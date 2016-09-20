package mpi.aida.access;

import gnu.trove.impl.Constants;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import mpi.aida.config.AidaConfig;
import mpi.aida.data.*;
import mpi.aida.graph.similarity.UnitType;
import mpi.aida.util.YagoUtil.Gender;
import mpi.aida.util.timing.RunningTimer;
import mpi.tools.javatools.datatypes.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class DataAccess {
  private static final Logger logger = 
      LoggerFactory.getLogger(DataAccess.class);

  private static DataAccessInterface dataAccess = null;

  /* Keyphrase sources */
  public static final String KPSOURCE_LINKANCHOR = "linkAnchor";

  public static final String KPSOURCE_INLINKTITLE = "inlinkTitle";

  public static final String KPSOURCE_CATEGORY = "wikipediaCategory";

  public static final String KPSOURCE_CITATION = "citationTitle";
  
  /** which type of data access*/
  public static enum type {
    sql, testing, dmap
  }

  private static synchronized void initDataAccess() {
    if (dataAccess != null) {
      return;
    }
    if (DataAccess.type.sql.toString().equalsIgnoreCase(AidaConfig.get(AidaConfig.DATAACCESS))) {
      dataAccess = new DataAccessSQL();
    } else if (DataAccess.type.testing.toString().equalsIgnoreCase(AidaConfig.get(AidaConfig.DATAACCESS))) {
      dataAccess = new DataAccessForTesting();
    } else if (type.dmap.toString().equalsIgnoreCase(AidaConfig.get(AidaConfig.DATAACCESS))) {
      dataAccess = new DataAccessDMap();
    } else {
      // Default is sql.
      logger.info("No dataAccess given in 'settings/aida.properties', " +
      		        "using 'sql' as default.");
      dataAccess = new DataAccessSQL();
    }
  }  

  private static DataAccessInterface getInstance() {
    if (dataAccess == null) {
      initDataAccess();
    }
    return dataAccess;
  }

  public static void init() {
    DataAccess.getInstance();
  }

  public static DataAccess.type getAccessType() {
    return DataAccess.getInstance().getAccessType();
  }

  /**
   * Returns candidate entities for the given mention.
   * The candidate space can be restricted globally by the maxEntityRank, and on a per mention
   * basis only the topK according to the prior can be returned.
   *
   * @param mention Mention to get candidates for
   * @param maxEntityRank Maximum rank of the candidate entity (according to global rank in [0.0,1.0] where 0.0 is the best rank.
   * @param topByPrior  How many candidates to return, according to the ranking by prior. Set to 0 to return all.
   * @return  Candidate entities for mention.
   */
  public static Entities getEntitiesForMention(String mention, double maxEntityRank, int topByPrior) {
    List<String> mentionAsList = new ArrayList<String>(1);
    mentionAsList.add(mention);
    Map<String, Entities> candidates = getEntitiesForMentions(mentionAsList, maxEntityRank, topByPrior);
    return candidates.get(mention);
  }
  
  public static Map<String, Entities> getEntitiesForMentions(Collection<String> mention, double maxEntityRank, int topByPrior) {
    return DataAccess.getInstance().getEntitiesForMentions(mention, maxEntityRank, topByPrior);
  }
  
  public static Entities getEntitiesForMentionByFuzzyMatcyhing(String mention, double minSimilarity) {
    return DataAccess.getInstance().getEntitiesForMentionByFuzzyMatching(mention, minSimilarity);
  }
  
  /**
   * @return The complete mention-entity dictionary.
   */
  public static Map<String, int[]> getDictionary() {
    return DataAccess.getInstance().getDictionary();
  }
  
  /**
   * Retrieves all the Keyphrases for the given entities. Does not return
   * keyphrases when the source has a weight of 0.0.
   * 
   * If keyphraseSourceWeights is not null, the return object will also
   * contain all keyphrase sources. This will increase the data transfer,
   * so use wisely.
   * 
   * If minKeyphraseWeight > 0.0, keyphrases with a weight lower than
   * minKeyphraseWeight will not be returned.
   * 
   * If maxEntityKeyphraseCount > 0, at max maxEntityKeyphraseCount will
   * be returned for each entity.
   * 
   * @param entities
   * @param keyphraseSourceWeights
   * @param minKeyphraseWeight
   * @param maxEntityKeyphraseCount
   * @return
   */
  public static Keyphrases getEntityKeyphrases(
      Entities entities, Map<String, Double> keyphraseSourceWeights, 
      double minKeyphraseWeight, int maxEntityKeyphraseCount) {
    return DataAccess.getInstance().
        getEntityKeyphrases(entities, keyphraseSourceWeights, 
            minKeyphraseWeight, maxEntityKeyphraseCount);
  }

  public static void getEntityKeyphraseTokens(
      Entities entities,
      TIntObjectHashMap<int[]> entityKeyphrases,
      TIntObjectHashMap<int[]> keyphraseTokens) {
    DataAccess.getInstance().getEntityKeyphraseTokens(
        entities, entityKeyphrases, keyphraseTokens);
  }

  public static int[] getInlinkNeighbors(Entity e) {
    Entities entities = new Entities();
    entities.add(e);
    TIntObjectHashMap<int[]> neighbors = getInlinkNeighbors(entities);
    return neighbors.get(e.getId());
  }

  public static TIntObjectHashMap<int[]> getInlinkNeighbors(Entities entities) {
    return DataAccess.getInstance().getInlinkNeighbors(entities);
  }

  public static int getInternalIdForKBEntity(KBIdentifiedEntity entity) {
    List<KBIdentifiedEntity> entities = new ArrayList<KBIdentifiedEntity>(1);
    entities.add(entity);
    return getInternalIdsForKBEntities(entities).get(entity);
  }

  public static TObjectIntHashMap<KBIdentifiedEntity> getInternalIdsForKBEntities(Collection<KBIdentifiedEntity> entities) {
    return DataAccess.getInstance().getInternalIdsForKBEntities(entities);
  }
  
  public static KBIdentifiedEntity getKnowlegebaseEntityForInternalId(int entity) {
    int[] entities = new int[1];
    entities[0] = entity;
    return getKnowlegebaseEntitiesForInternalIds(entities).get(entity);
  }
   public static TIntObjectHashMap<KBIdentifiedEntity> getKnowlegebaseEntitiesForInternalIds(int[] ids) {
    return DataAccess.getInstance().getKnowlegebaseEntitiesForInternalIds(ids);
  }
   
  public static Entities getAidaEntitiesForInternalIds(int[] internalIds) {
    TIntObjectHashMap<KBIdentifiedEntity> kbEntities = 
        DataAccess.getKnowlegebaseEntitiesForInternalIds(internalIds);
    Entities entities = new Entities();
    for (TIntObjectIterator<KBIdentifiedEntity> itr = kbEntities.iterator(); 
        itr.hasNext(); ) {
      itr.advance();
      entities.add(new Entity(itr.value(), itr.key()));
    }
    return entities;
  }
  
  public static Entities getAidaEntitiesForKBEntities(Set<KBIdentifiedEntity> entities) {
    TObjectIntHashMap<KBIdentifiedEntity> kbEntities = 
        DataAccess.getInternalIdsForKBEntities(entities);
    Entities aidaEntities = new Entities();
    for (TObjectIntIterator<KBIdentifiedEntity> itr = kbEntities.iterator(); 
        itr.hasNext(); ) {
      itr.advance();
      aidaEntities.add(new Entity(itr.key(), itr.value()));
    }
    return aidaEntities;
  }
   
  public static int getIdForTypeName(String typeName) {
    List<String> typeNames = new ArrayList<String>(1);
    typeNames.add(typeName);
    return getIdsForTypeNames(typeNames).get(typeName);
  }

  public static TObjectIntHashMap<String> getIdsForTypeNames(Collection<String> typeNames) {
    return DataAccess.getInstance().getIdsForTypeNames(typeNames);
  }
   
  public static Type getTypeForId(int typeId) {
    int[] types = new int[1];
    types[0] = typeId;
    return getTypesForIds(types).get(typeId);
  }
  
  public static TIntObjectHashMap<Type> getTypesForIds(int[] ids) {
    return DataAccess.getInstance().getTypesForIds(ids);
  }
  
  public static int[] getTypeIdsForEntityId(int entityId) {
    int[] entities = new int[1];
    entities[0] = entityId;
    return getTypesIdsForEntitiesIds(entities).get(entityId);
  }
  
  public static TIntObjectHashMap<int[]> getTypesIdsForEntitiesIds(int[] ids) {
    return DataAccess.getInstance().getTypesIdsForEntitiesIds(ids);
  }
  
  
  public static int[] getEntitiesIdsForTypeId(int typeId) {
    int[] types = new int[1];
    types[0] = typeId;
    return getEntitiesIdsForTypesIds(types).get(typeId);
  }
  
  public static TIntObjectHashMap<int[]> getEntitiesIdsForTypesIds(int[] ids) {
    return DataAccess.getInstance().getEntitiesIdsForTypesIds(ids);
  }


  public static String getWordForId(int wordId) {
    int[] words = new int[1];
    words[0] = wordId;
    return getWordsForIds(words).get(wordId);
  }

  public static int getIdForWord(String word) {
    List<String> words = new ArrayList<String>(1);
    words.add(word);
    return getIdsForWords(words).get(word);  
  }
  
  public static TIntObjectHashMap<String> getWordsForIds(int[] wordIds) {
    return DataAccess.getInstance().getWordsForIds(wordIds);
  }
  
  /**
   * Transform words into the token ids. Unknown words will be assigned
   * the id -1
   * 
   * @param   words
   * @return  Ids for words ranging from 1 to MAX, -1 for missing.
   */
  public static TObjectIntHashMap<String> getIdsForWords(
      Collection<String> words) {
    return DataAccess.getInstance().getIdsForWords(words);
  }

  public static TIntObjectHashMap<Gender> getGenderForEntities(Entities entities) {
    return getInstance().getGenderForEntities(entities);
  }

  public static TIntObjectHashMap<Set<Type>> getTypes(Entities entities) {
    TIntObjectHashMap<Set<Type>> entityTypes = new TIntObjectHashMap<Set<Type>>();
    TIntObjectHashMap<int[]> entitiesTypesIds = getTypesIdsForEntitiesIds(entities.getUniqueIdsAsArray());
    TIntSet allTypesIds = new TIntHashSet();
    for(TIntObjectIterator<int[]> itr = entitiesTypesIds.iterator(); 
        itr.hasNext(); ) {
      itr.advance();
      int[] types = itr.value();
      allTypesIds.addAll(types);
    }

    TIntObjectHashMap<Type> typeNamesMap = getTypesForIds(allTypesIds.toArray());
    
    for (Entity entity : entities) {
      int[] typesIds = entitiesTypesIds.get(entity.getId());
      Set<Type> types = new HashSet<Type>(typesIds.length, 1.0f);
      for(int typeId : typesIds) {
        types.add(typeNamesMap.get(typeId));
      }
      entityTypes.put(entity.getId(), types);
    }
    return entityTypes;
  }

  public static Set<Type> getTypes(Entity entity) {
    Entities entities = new Entities();
    entities.add(entity);
    TIntObjectHashMap<Set<Type>> entityTypes = getTypes(entities);
    return entityTypes.get(entity.getId());
  }

  @Deprecated
  public static Map<String, List<String>> getAllEntitiesMetaData(String startingWith){
    return getInstance().getAllEntitiesMetaData(startingWith);
  }
  
  public static TIntObjectHashMap<EntityMetaData> getEntitiesMetaData(int[] entitiesIds) {
    return getInstance().getEntitiesMetaData(entitiesIds);
  }

  public static EntityMetaData getEntityMetaData(int entityId) {
    int[] entitiesIds = new int[1];
    entitiesIds[0] = entityId;
    TIntObjectHashMap<EntityMetaData> results = getEntitiesMetaData(entitiesIds);
    return results.get(entityId);
  }
  
  public static TIntDoubleHashMap getEntitiesImportances(int[] entitiesIds) {
    return getInstance().getEntitiesImportances(entitiesIds);
  }

  public static double getEntityImportance(int entityId) {
    int[] entitiesIds = new int[1];
    entitiesIds[0] = entityId;
    TIntDoubleHashMap results = getEntitiesImportances(entitiesIds);
    return results.get(entityId);
  }
  
  public static TIntIntHashMap getKeyphraseDocumentFrequencies(TIntHashSet keyphrases) {    
    return getInstance().getKeyphraseDocumentFrequencies(keyphrases);
  }

  public static List<String> getParentTypes(String queryType) {
    return getInstance().getParentTypes(queryType);
  }

  public static TIntObjectHashMap<int[]> getEntityLSHSignatures(Entities entities, String table) {
    return getInstance().getEntityLSHSignatures(entities, table);
  }

  public static TIntObjectHashMap<int[]> getEntityLSHSignatures(Entities entities) {
    return getInstance().getEntityLSHSignatures(entities);
  }
  
  public static TIntDoubleHashMap getEntityPriors(String mention) {
    return getInstance().getEntityPriors(mention);
  }

  public static TIntIntHashMap getKeywordDocumentFrequencies(TIntSet keywords) {
    Integer runId = RunningTimer.recordStartTime("DataAccess:KWDocFreq");
    TIntIntHashMap keywordCounts = new TIntIntHashMap((int) (keywords.size() / Constants.DEFAULT_LOAD_FACTOR));
    for (TIntIterator itr = keywords.iterator(); itr.hasNext(); ) {
      int keywordId = itr.next();      
      int count = DataAccessCache.singleton().getKeywordCount(keywordId);
      keywordCounts.put(keywordId, count);
    }    
    RunningTimer.recordEndTime("DataAccess:KWDocFreq", runId);
    return keywordCounts;
  }

  public static TIntIntHashMap getUnitDocumentFrequencies(TIntSet keywords, UnitType unitType) {
    Integer runId = RunningTimer.recordStartTime("DataAccess:KWDocFreq");
    TIntIntHashMap keywordCounts = new TIntIntHashMap((int) (keywords.size() / Constants.DEFAULT_LOAD_FACTOR));
    for (TIntIterator itr = keywords.iterator(); itr.hasNext(); ) {
      int keywordId = itr.next();
      int count = DataAccessCache.singleton().getUnitCount(keywordId, unitType);
      keywordCounts.put(keywordId, count);
    }
    RunningTimer.recordEndTime("DataAccess:KWDocFreq", runId);
    return keywordCounts;
  }
  
  public static int getUnitDocumentFrequency(int unit, UnitType unitType) {
    int frequency = 0;
    try {
      frequency = DataAccessCache.singleton().getUnitCount(unit, unitType);
    } catch (Exception e) {
      logger.debug("Could not get frequency of " + unitType.getUnitName() + ": " + unit);
    }
    return frequency;
  }

  public static TIntIntHashMap getEntitySuperdocSize(Entities entities) {
    return getInstance().getEntitySuperdocSize(entities);
  }

  public static TIntObjectHashMap<TIntIntHashMap> getEntityUnitIntersectionCount(Entities entities, UnitType unitType) {
    return getInstance().getEntityUnitIntersectionCount(entities, unitType);
  }

  public static TObjectIntHashMap<KBIdentifiedEntity> getAllEntityIds() {
    return getInstance().getAllEntityIds();
  }
  
  public static TObjectIntHashMap<Type> getAllTypeIds() {
    return getInstance().getAllTypeIds();
  }
  
  public static TIntObjectHashMap<int[]> getAllEntityTypes() {
    return getInstance().getAllEntityTypes();
  }
  
  public static TIntObjectHashMap<int[]> getTaxonomy() {
    return getInstance().getTaxonomy();
  }
  
  public static TIntDoubleHashMap getAllEntityRanks() {
    return getInstance().getAllEntityRanks();
  }
  
  public static TObjectIntHashMap<String> getAllKeyphraseSources() {
    return getInstance().getAllKeyphraseSources();
  }
  
  public static TIntObjectHashMap<int[]> getAllKeyphraseTokens() {
    return getInstance().getAllKeyphraseTokens();
  }
  
  public static Entities getAllEntities() {
    return getInstance().getAllEntities();
  }

  // Will return an array where the index is the id of the word to be expanded
  // and the value is the id of the expanded word. Expansion is done by
  // fully uppercasing the original word behind the id.
  public static int[] getAllWordExpansions() {
    return getInstance().getAllWordExpansions();
  }
  
  public static int[] getAllWordContractions() {
    return getInstance().getAllWordContractions();
  }

  public static boolean isYagoEntity(Entity entity) {
    return getInstance().isYagoEntity(entity);
  }

  public static TIntObjectHashMap<int[]> getAllInlinks() {
    return getInstance().getAllInlinks();
  }
  
  public static TObjectIntHashMap<String> getAllWordIds() {
    return getInstance().getAllWordIds();
  }
  
  
  /**
   * Used for the weight computation. This returns the total number of 
   * documents in the collection for the computation of the keyword IDF weights.
   * In the original AIDA setting with YAGO-entities this is the number of 
   * Wikipedia entities.
   * @return  Collection Size.
   */
  public static int getCollectionSize() {
    return getInstance().getCollectionSize();
  }

  public static int getMaximumEntityId() {
    return getInstance().getMaximumEntityId();
  }

  public static int getMaximumWordId() {
    return getInstance().getMaximumWordId();
  }

  public static int getWordExpansion(int wordId) {
    return getInstance().getWordExpansion(wordId);
  }
  
  public static String getConfigurationName() {
    return getInstance().getConfigurationName();
  }

  public static int expandTerm(int wordId) {
    return DataAccessCache.singleton().expandTerm(wordId);
  }

  public static String expandTerm(String term) {
    return term.toUpperCase(Locale.ENGLISH);
  }
  
  public static int contractTerm(int wordId) {
    return DataAccessCache.singleton().contractTerm(wordId);
  }
  
  public static int[] getAllKeywordDocumentFrequencies() {
    return getInstance().getAllKeywordDocumentFrequencies();
  }

  public static int[] getAllUnitDocumentFrequencies(UnitType unitType) {
    return getInstance().getAllUnitDocumentFrequencies(unitType);
  }
  
  public static int getGNDTripleCount(Entity e) {
    Entities entities = new Entities();
    entities.add(e);
    TIntIntHashMap  counts = getGNDTripleCount(entities);
    return counts.get(e.getId());
  }

  public static TIntIntHashMap getGNDTripleCount(Entities entities) {
    return DataAccess.getInstance().getGNDTripleCount(entities);
  }
  
  public static int getGNDTitleCount(Entity e) {
    Entities entities = new Entities();
    entities.add(e);
    TIntIntHashMap  counts = getGNDTitleCount(entities);
    return counts.get(e.getId());
  }

  public static TIntIntHashMap getGNDTitleCount(Entities entities) {
    return DataAccess.getInstance().getGNDTitleCount(entities);
  }
  
  //use only for importance component computation 
  public static int getYagoOutlinkCount(Entity e) {
    Entities entities = new Entities();
    entities.add(e);
    TIntIntHashMap  counts = getYagoOutlinkCount(entities);
    return counts.get(e.getId());
  }

  public static TIntIntHashMap getYagoOutlinkCount(Entities entities) {
    return DataAccess.getInstance().getYagoOutlinkCount(entities);
  }
  
  public static Pair<Integer, Integer> getImportanceComponentMinMax(String importanceId) {
    return DataAccess.getInstance().getImportanceComponentMinMax(importanceId);
  }
  
  public static Map<String, Double> getKeyphraseSourceWeights() {
     return DataAccess.getInstance().getKeyphraseSourceWeights();
  }

  public static Map<KBIdentifiedEntity, EntityMetaData> getEntitiesMetaData(Set<KBIdentifiedEntity> entities) {
    TObjectIntHashMap<KBIdentifiedEntity> ids = getInternalIdsForKBEntities(entities);
    TIntObjectHashMap<EntityMetaData> metadata = getEntitiesMetaData(ids.values());
    Map<KBIdentifiedEntity, EntityMetaData> result = new HashMap<KBIdentifiedEntity, EntityMetaData>();
    for(TObjectIntIterator<KBIdentifiedEntity> itr = ids.iterator(); itr.hasNext();) {
      itr.advance();
      int id = itr.value();
      result.put(itr.key(), metadata.get(id));
    }
    return result;
  }
  
}
