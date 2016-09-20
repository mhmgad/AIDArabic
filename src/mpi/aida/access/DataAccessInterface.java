package mpi.aida.access;

import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.hash.TIntHashSet;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import mpi.aida.data.Entities;
import mpi.aida.data.Entity;
import mpi.aida.data.EntityMetaData;
import mpi.aida.data.KBIdentifiedEntity;
import mpi.aida.data.Keyphrases;
import mpi.aida.data.Type;
import mpi.aida.graph.similarity.UnitType;
import mpi.aida.util.YagoUtil.Gender;
import mpi.tools.javatools.datatypes.Pair;


public interface DataAccessInterface {

  public DataAccess.type getAccessType();

  public Map<String, Entities> getEntitiesForMentions(Collection<String> mention, double maxEntityRank, int topByPrior);

  public Keyphrases getEntityKeyphrases(Entities entities, Map<String, Double> keyphraseSourceWeights, double minKeyphraseWeight,
      int maxEntityKeyphraseCount);

  public void getEntityKeyphraseTokens(Entities entities, TIntObjectHashMap<int[]> entityKeyphrases, TIntObjectHashMap<int[]> keyphraseTokens);

  public TIntObjectHashMap<int[]> getInlinkNeighbors(Entities entities);

  public TObjectIntHashMap<KBIdentifiedEntity> getInternalIdsForKBEntities(Collection<KBIdentifiedEntity> entities);

  public TIntObjectHashMap<KBIdentifiedEntity> getKnowlegebaseEntitiesForInternalIds(int[] ids);

  public TObjectIntHashMap<String> getIdsForTypeNames(Collection<String> typeNames);

  public TIntObjectHashMap<Type> getTypesForIds(int[] ids);

  public TIntObjectHashMap<int[]> getTypesIdsForEntitiesIds(int[] ids);

  public TIntObjectHashMap<int[]> getEntitiesIdsForTypesIds(int[] ids);

  public TIntObjectHashMap<String> getWordsForIds(int[] wordIds);

  public TObjectIntHashMap<String> getIdsForWords(Collection<String> words);

  public TIntObjectHashMap<Gender> getGenderForEntities(Entities entities);

  public Map<String, List<String>> getAllEntitiesMetaData(String startingWith);

  public TIntObjectHashMap<EntityMetaData> getEntitiesMetaData(int[] entitiesIds);

  public TIntDoubleHashMap getEntitiesImportances(int[] entitiesIds);

  public TIntIntHashMap getKeyphraseDocumentFrequencies(TIntHashSet keyphrases);

  public List<String> getParentTypes(String queryType);

  public TIntObjectHashMap<int[]> getEntityLSHSignatures(Entities entities, String table);

  public TIntObjectHashMap<int[]> getEntityLSHSignatures(Entities entities);

  public TIntDoubleHashMap getEntityPriors(String mention);

  public TIntIntHashMap getEntitySuperdocSize(Entities entities);

  public TIntObjectHashMap<TIntIntHashMap> getEntityKeywordIntersectionCount(Entities entities);

  public TIntObjectHashMap<TIntIntHashMap> getEntityUnitIntersectionCount(Entities entities, UnitType unitType);

  public TObjectIntHashMap<KBIdentifiedEntity> getAllEntityIds();

  public TObjectIntHashMap<Type> getAllTypeIds();

  public Entities getAllEntities();

  public int[] getAllWordExpansions();
  
  public int[] getAllWordContractions();

  public boolean isYagoEntity(Entity entity);

  public TIntObjectHashMap<int[]> getAllInlinks();

  public TObjectIntHashMap<String> getAllWordIds();

  public int getCollectionSize();

  public int getWordExpansion(int wordId);

  public String getConfigurationName();

  public int[] getAllKeywordDocumentFrequencies();

  public int[] getAllUnitDocumentFrequencies(UnitType unitType);

  public TIntIntHashMap getGNDTripleCount(Entities entities);

  public TIntIntHashMap getGNDTitleCount(Entities entities);

  public TIntIntHashMap getYagoOutlinkCount(Entities entities);

  public Pair<Integer, Integer> getImportanceComponentMinMax(String importanceId);

  public Map<String, Double> getKeyphraseSourceWeights();

  public TIntObjectHashMap<int[]> getAllEntityTypes();

  public TIntDoubleHashMap getAllEntityRanks();
  
  public Entities getEntitiesForMentionByFuzzyMatching(String mention, double minSimilarity);

  public TObjectIntHashMap<String> getAllKeyphraseSources();

  public TIntObjectHashMap<int[]> getAllKeyphraseTokens();

  public TIntObjectHashMap<int[]> getTaxonomy();

  public Map<String, int[]> getDictionary();

  public int getMaximumEntityId();

  public int getMaximumWordId();
}
