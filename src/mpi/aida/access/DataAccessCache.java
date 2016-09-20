package mpi.aida.access;

import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

import mpi.aida.AidaManager;
import mpi.aida.config.AidaConfig;
import mpi.aida.config.DMapConfig;
import mpi.aida.graph.similarity.UnitType;
import mpi.aida.util.ClassPathUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


class DataAccessCache {
  private static final Logger logger = 
      LoggerFactory.getLogger(DataAccessCache.class);
  
  private static final String DATABASE_AIDA_CONFIG_CACHE = "database_aida.cache";
  private static final String DMAP_AIDA_CONFIG_CACHE = "dmap_aida.cache";

  private DataAccessIntIntCacheTarget wordExpansion;  
  private DataAccessIntIntCacheTarget wordContraction;
  private DataAccessIntIntCacheTarget[] unitCounts;
  private DataAccessKeyphraseTokensCacheTarget keyphraseTokens;
  private DataAccessKeyphraseSourcesCacheTarget keyphraseSources;
  
  private static class DataAccessCacheHolder {
    public static DataAccessCache cache = new DataAccessCache();
  }
  
  public static DataAccessCache singleton() {
    return DataAccessCacheHolder.cache;
  }
  
  private DataAccessCache() {
    List<DataAccessCacheTarget> cacheTargets = new ArrayList<>();
    wordExpansion = new DataAccessWordExpansionCacheTarget();
    cacheTargets.add(wordExpansion);
    wordContraction = new DataAccessWordContractionCacheTarget();
    cacheTargets.add(wordContraction);
    unitCounts = new DataAccessUnitCountCacheTarget[UnitType.values().length];
    for (UnitType unitType : UnitType.values()) {
      DataAccessUnitCountCacheTarget target = new DataAccessUnitCountCacheTarget(unitType);
      unitCounts[unitType.ordinal()] = target;
      cacheTargets.add(target);
    }
    keyphraseTokens = new DataAccessKeyphraseTokensCacheTarget();
    keyphraseSources = new DataAccessKeyphraseSourcesCacheTarget();
    // dmaps don't need these caches so we don't need to load them if we use dmaps
    if (!DataAccess.getAccessType().equals(DataAccess.type.dmap)) {
      cacheTargets.add(keyphraseTokens);
      cacheTargets.add(keyphraseSources);
    }
    
    logger.info("Loading word caches.");
        
    if (AidaConfig.getBoolean(AidaConfig.CACHE_WORD_DATA)) {
      // Determine cache state.
      boolean needsCacheCreation = true; 
      try {
        needsCacheCreation = determineCacheCreation();
      } catch (FileNotFoundException e1) {
        logger.error("Did not find file: " + e1.getLocalizedMessage());
        e1.printStackTrace();
      } catch (IOException e1) {
        logger.error("Exception reading file: " + e1.getLocalizedMessage());
        e1.printStackTrace();
      }
      for (DataAccessCacheTarget target : cacheTargets) {
        try {          
          target.createAndLoadCache(needsCacheCreation);
        } catch (IOException e) {
          target.loadFromDb();
          logger.warn("Could not read cache file, reading from DB.", e);
        }        
      }
      if (needsCacheCreation) {
        try {
          Properties currentConfig = null;
          File cachedConfigFile = null;
          switch (DataAccess.getAccessType()) {
            case testing:
            case sql:
              currentConfig = ClassPathUtils.getPropertiesFromClasspath(AidaManager.databaseAidaConfig);
              cachedConfigFile = new File(DATABASE_AIDA_CONFIG_CACHE);
              break;
            case dmap:
              currentConfig = ClassPathUtils.getPropertiesFromClasspath(DMapConfig.PATH);
              cachedConfigFile = new File(DMAP_AIDA_CONFIG_CACHE);
              break;
          }
          currentConfig.store(new BufferedOutputStream(new FileOutputStream(cachedConfigFile)), "cached aida data config");
        } catch (IOException e ) {
          logger.error("Could not write config: " + e.getLocalizedMessage());
          e.printStackTrace();
        }
      }
    } else {
      logger.info("Loading data from DB.");
      for (DataAccessCacheTarget target : cacheTargets) {
        target.loadFromDb();
      }
    }
    logger.info("Done loading caches.");
  }

  private boolean determineCacheCreation() throws IOException {
    File cachedDBConfigFile = new File(DATABASE_AIDA_CONFIG_CACHE);
    File cachedDMAPConfigFile = new File(DMAP_AIDA_CONFIG_CACHE);
    if (!cachedDBConfigFile.exists() && (DataAccess.getAccessType() == DataAccess.type.sql 
      || DataAccess.getAccessType() == DataAccess.type.testing)) {
      cachedDMAPConfigFile.delete();
      return true;
    } else if (!cachedDMAPConfigFile.exists() && DataAccess.getAccessType() == DataAccess.type.dmap) {
      cachedDBConfigFile.delete();
      return true;
    }
    Properties currentConfig = null;
    Properties cachedConfig = new Properties();
    switch (DataAccess.getAccessType()) {
      case testing:
      case sql:
        currentConfig = ClassPathUtils.getPropertiesFromClasspath(AidaManager.databaseAidaConfig);
        cachedConfig.load(new BufferedInputStream(new FileInputStream(cachedDBConfigFile)));
        break;
      case dmap:
        currentConfig = ClassPathUtils.getPropertiesFromClasspath(DMapConfig.PATH);
        cachedConfig.load(new BufferedInputStream(new FileInputStream(cachedDMAPConfigFile)));
        break;
      default:
        return true;
    }
    if (!currentConfig.equals(cachedConfig)) {
      logger.info("Cache files exist, but config has been changed since it was created; data access is unavoidable!");
      //there is a change in the DB config
      // do a clean up and require a DB access
      cachedDBConfigFile.delete();
      cachedDMAPConfigFile.delete();
      return true;
    }
    return false;
  }

  public int expandTerm(int wordId) {
    return wordExpansion.getData(wordId);
  }
  
  public int contractTerm(int wordId) {
    return wordContraction.getData(wordId);
  }
  
  public int getKeywordCount(int wordId) {
    return unitCounts[UnitType.KEYWORD.ordinal()].getData(wordId);
  }
  
  public int getUnitCount(int unitId, UnitType unitType) {
    return unitCounts[unitType.ordinal()].getData(unitId);
  }
  
  public int[] getKeyphraseTokens(int wordId) {
    return keyphraseTokens.getData(wordId);
  }
  
  public int getKeyphraseSourceId(String source) {
    return keyphraseSources.getData(source);
  }
  
  public TIntObjectHashMap<int[]> getAllKeyphraseTokens() {
    return keyphraseTokens.getAllData();
  }
  
  public TObjectIntHashMap<String> getAllKeyphraseSources() {
    return keyphraseSources.getAllData();
  }
}
