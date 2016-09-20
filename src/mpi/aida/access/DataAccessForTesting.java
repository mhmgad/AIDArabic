package mpi.aida.access;

import gnu.trove.iterator.TIntIntIterator;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.hash.TIntHashSet;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import mpi.aida.AidaManager;
import mpi.aida.access.DataAccess.type;
import mpi.aida.data.Entities;
import mpi.aida.data.Entity;
import mpi.aida.data.EntityMetaData;
import mpi.aida.data.KBIdentifiedEntity;
import mpi.aida.data.Keyphrases;
import mpi.aida.data.Type;
import mpi.aida.graph.similarity.UnitType;
import mpi.aida.graph.similarity.measure.WeightComputation;
import mpi.aida.util.YagoUtil.Gender;
import mpi.tools.javatools.datatypes.Pair;

public class DataAccessForTesting implements DataAccessInterface {

  private Map<String, Integer> entity2id = new HashMap<String, Integer>();
  private Map<Integer, String> id2entity = new HashMap<Integer, String>();
  
  private Map<String, Integer> word2id = new HashMap<String, Integer>();
  private Map<Integer, String> id2word = new HashMap<Integer, String>();
  
  private TIntIntHashMap wordExpansions = new TIntIntHashMap();
  
  private int entityId = 1;
  private int wordId = 1;
  
  private final int TOTAL_ENTITY_COUNT = 2651987;
      
  private static final String TESTING = "TESTING";
  
  /**
   * All entities with keyphrases and count. Format is:
   * entity, kp1, count1, kp2, count2, ...
   * kpN is space separated tokens.
   */
  private String[][] allEntityKeyphrases = new String[][] {
      new String[] { "Larry_Page", "Google", "2" },
      new String[] { "Jimmy_Page", "played", "10", "Les Paul", "4", "tuned", "1", "Led Zeppelin", "5", "Robert Plant", "9", "Rock music", "2"},
      new String[] { "Nomatching_Page", "Page", "5" },
      new String[] { "Stopword_Page", "and the", "2" },
      new String[] { "Kashmir", "China", "10" },
      new String[] { "Kashmir_(song)", "Jimmy Page", "5", "festival", "2", "Led Zeppelin", "3", "Robert Plant", "5" },
      new String[] { "Knebworth_Festival", "festival", "1", "Rock music", "2" },
  };
  
  /**
   * All entity superdoc sizes. Format is:
   * entity, size
   */
  private String[][] allEntitySizes = new String[][] {
      new String[] { "Larry_Page", "20" },
      new String[] { "Jimmy_Page", "10" },
      new String[] { "Nomatching_Page", "5" },
      new String[] { "Stopword_Page", "2" },
      new String[] { "Kashmir", "15" },
      new String[] { "Kashmir_(song)", "5" },
      new String[] { "Knebworth_Festival", "2" }
  };
  
  private String[] orderedEntities = new String[] {
      "Larry_Page", "Jimmy_Page", "Nomatching_Page", "Stopword_Page",
      "Kashmir", "Kashmir_(song)", "Knebworth_Festival" };
  
  /**
   * All keyphrase superdoc frequencies. Format is:
   * keyphrase, frequency
   */
  private String[][] allKeyphraseFrequencies = new String[][] {
      new String[] { "Google", "50" },
      new String[] { "played", "100" },
      new String[] { "Les Paul", "80" },
      new String[] { "tuned", "20" },
      new String[] { "China", "200" },
      new String[] { "Jimmy Page", "30" },
      new String[] { "festival", "10" },
      new String[] { "Led Zeppelin", "40" },
      new String[] { "Robert Plant", "25" },
      new String[] { "Rock music", "30" },
      new String[] { "and the", "5" },
  };
  
  /** All entity inlinks */
  private String[][] allInlinks = new String[][] {
      new String[] { "Larry_Page", "Google" },
      new String[] { "Jimmy_Page", "Led_Zeppelin", "Robert_Plant", "Rock", "Les_Paul" },
      new String[] { "Kashmir", "China", "India", "Pakistan" },
      new String[] { "Kashmir_(song)", "Led_Zeppelin", "Robert_Plant", "Jimmy_Page" },
      new String[] { "Knebworth_Festival", "England", "Music_Festival", "Led_Zeppelin" },
  };      
      
  @Override
  public void getEntityKeyphraseTokens(
      Entities entities,
      TIntObjectHashMap<int[]> entityKeyphrases,
      TIntObjectHashMap<int[]> keyphraseTokens) {
    for (String[] eKps : allEntityKeyphrases) {
      int entity = DataAccess.getInternalIdForKBEntity(getTestKBEntity(eKps[0]));
      int[] keyphrases = new int[(eKps.length - 1) / 2];
      if (eKps.length > 1) {
        for (int i = 1; i < eKps.length; ++i) {
          if (i % 2 == 1) {
            int kp = DataAccess.getIdForWord(eKps[i]);
            keyphrases[(i - 1) / 2] = kp;
            
            // Add tokens.
            String[] tokens = eKps[i].split(" ");
            int[] tokenIds = new int[tokens.length];
            for (int j = 0; j < tokens.length; ++j) {
              tokenIds[j] = DataAccess.getIdForWord(tokens[j]);
            }
            keyphraseTokens.put(kp, tokenIds);
          }
        }
      }
      entityKeyphrases.put(entity, keyphrases);
    }
  }

  public static KBIdentifiedEntity getTestKBEntity(String entity) {
    return new  KBIdentifiedEntity(entity, TESTING);
  }
  
  public static Entity getTestEntity(String entity) {
    KBIdentifiedEntity kbEntity = getTestKBEntity(entity);
    return AidaManager.getEntity(kbEntity);
  }

  public TIntObjectHashMap<TIntIntHashMap> getEntityKeyphraseIntersectionCount(
      Entities entities) {
    TIntObjectHashMap<TIntIntHashMap> isec = new TIntObjectHashMap<TIntIntHashMap>();
    for (String[] eKps : allEntityKeyphrases) {
      int entity = DataAccess.getInternalIdForKBEntity(getTestKBEntity(eKps[0]));
      TIntIntHashMap counts = new TIntIntHashMap();
      isec.put(entity, counts);
      
      if (eKps.length > 1) {
        int currentKp = -1;
        for (int i = 1; i < eKps.length; ++i) {
          if (i % 2 == 1) {
            currentKp = DataAccess.getIdForWord(eKps[i]);
          } else {
            int count = Integer.parseInt(eKps[i]);
            counts.put(currentKp, count);
          }
        }
      }
    }
    return isec;
  }

  public DataAccessForTesting() {
    addEntity(Entity.OOKBE);
    
    for (String[] entity : allEntityKeyphrases) {
      addEntity(entity[0]);
    }

    for (String[] inlinks : allInlinks) {
      for (int i = 1; i < inlinks.length; ++i) {
        addEntity(inlinks[i]);
      }
    }
    
    for (Entry<String, Integer> e : entity2id.entrySet()) {
      id2entity.put(e.getValue(), e.getKey());
    }
        
    for (String[] entity : allEntityKeyphrases) {
      for (int i = 1; i < entity.length; ++i) {
        if (i % 2 == 1) {     
          addWord(entity[i]);
          
          String[] tokens = entity[i].split(" ");
          for (String token : tokens) {
            addWord(token);
          }
        }
      }
    }

    for (Entry<String, Integer> e : word2id.entrySet()) {
      id2word.put(e.getValue(), e.getKey());
    }
  }
  
  private void addEntity(String name) {
    entity2id.put(name, entityId++);
  }
  
  private void addWord(String word) {
    // Don't add twice.=
    if (word2id.containsKey(word)) {
      return;
    }
    
    int id = wordId;
    ++wordId;
    word2id.put(word, id);
    String wordUpper = DataAccess.expandTerm(word);
    int upper = wordId;
    if (word2id.containsKey(wordUpper)) {
      upper = word2id.get(wordUpper);
    } else {
      word2id.put(wordUpper, upper);
      ++wordId;
    }
    wordExpansions.put(id, upper);
  }
  
  @Override
  public type getAccessType() {
    return DataAccess.type.testing;
  }

  @Override
  public Map<String, Entities> getEntitiesForMentions(Collection<String> mentions, double maxEntityRank, int topByPrior) {
    Map<String, Entities> allCandidates = new HashMap<String, Entities>();
    Entities pageEntities = new Entities();
    Entity e1 = getTestEntity("Jimmy_Page");
    if (getEntityRank(e1) <= maxEntityRank) { pageEntities.add(e1); }
    Entity e2 = getTestEntity("Larry_Page");
    if (getEntityRank(e2) <= maxEntityRank) { pageEntities.add(e2); }
    allCandidates.put("Page", pageEntities);
    Entities kashmirEntities = new Entities();
    e1 = getTestEntity("Kashmir");
    if (getEntityRank(e1) <= maxEntityRank) { kashmirEntities.add(e1); }
    e2 = getTestEntity("Kashmir_(song)");
    if (getEntityRank(e2) <= maxEntityRank) { kashmirEntities.add(e2); }
    allCandidates.put("Kashmir", kashmirEntities);
    Entities knebworthEntities = new Entities();
    e1 = getTestEntity("Knebworth_Festival");
    if (getEntityRank(e1) <= maxEntityRank) { knebworthEntities.add(e1); } 
    allCandidates.put("Knebworth", knebworthEntities);
    allCandidates.put("Les Paul", new Entities());
    
    Map<String, Entities> candidates = new HashMap<>();
    for (String m : mentions) {
      Entities entities = allCandidates.get(m);
      if (entities == null) {
        entities = new Entities();
      }
      candidates.put(m, entities);
    }
    
    return candidates;
  }
  
  public double getEntityRank(Entity e) {
    int offset = 0;
    for (String rank : orderedEntities) {
      if (rank.equals(e.getIdentifierInKb())) {
        break;
      } else {
        ++offset;
      }
    }
    
    if (offset == orderedEntities.length) {
      System.err.println("No rank for entity: " + e);
    }
    
    return (double) offset / (double) orderedEntities.length;
  }
  
  @Override
  public Keyphrases getEntityKeyphrases(Entities entities,
      Map<String, Double> keyphraseSourceWeights, double minKeyphraseWeight,
      int maxEntityKeyphraseCount) {
    Keyphrases keyphrases = new Keyphrases();
    TIntObjectHashMap<int[]> eKps = new TIntObjectHashMap<int[]>();
    TIntObjectHashMap<int[]> kpTokens = new TIntObjectHashMap<int[]>();
    getEntityKeyphraseTokens(
        entities, eKps, kpTokens);
    keyphrases.setEntityKeyphrases(eKps);
    keyphrases.setKeyphraseTokens(kpTokens);
    
    // TODO(jhoffart) add keyphrase mi weights.
    TIntObjectHashMap<TIntDoubleHashMap> e2kw2mi = 
        new TIntObjectHashMap<TIntDoubleHashMap>();
    keyphrases.setEntityKeywordWeights(e2kw2mi);
    TIntObjectHashMap<TIntDoubleHashMap> e2kp2mi = 
        new TIntObjectHashMap<TIntDoubleHashMap>();
    keyphrases.setEntityKeyphraseWeights(e2kp2mi);    
  
    for (Entity entity : entities) {
      int eId = entity.getId();
      Entities singleEntity = new Entities();
      singleEntity.add(entity);
      int entityCount = getEntitySuperdocSize(singleEntity).get(eId);
      TIntDoubleHashMap kp2mi = new TIntDoubleHashMap();
      e2kp2mi.put(entity.getId(), kp2mi);
      TIntDoubleHashMap kw2mi = new TIntDoubleHashMap();
      e2kw2mi.put(entity.getId(), kw2mi);
      for (int kp : eKps.get(eId)) {
        TIntHashSet singleKp = new TIntHashSet();
        singleKp.add(kp);
        int kpCount = getKeyphraseDocumentFrequencies(singleKp).get(kp);
        int eKpIcCount = 
            getEntityKeyphraseIntersectionCount(singleEntity).get(eId).get(kp);
        kp2mi.put(kp, 
            WeightComputation.computeNPMI(
                entityCount, kpCount, eKpIcCount, 
                TOTAL_ENTITY_COUNT));
        
        for (int kw : kpTokens.get(kp)) {
          TIntHashSet singleKw = new TIntHashSet();
          singleKw.add(kw);
          int kwCount = getKeywordDocumentFrequencies(singleKw).get(kw);
          int eKwIcCount = 
              getEntityKeywordIntersectionCount(singleEntity).get(eId).get(kw);
          kw2mi.put(kw, 
              WeightComputation.computeMI(
                  entityCount, kwCount, eKwIcCount, 
                  TOTAL_ENTITY_COUNT, false));
        }
      }
    }
    return keyphrases;
  }
 
  @Override
  public TIntObjectHashMap<int[]> getInlinkNeighbors(Entities entities) {
    TIntObjectHashMap<int[]> inlinks = new TIntObjectHashMap<int[]>();
    
    for (Entity e : entities) {
      inlinks.put(e.getId(), new int[0]);
    }
    
    for (String[] entityInlinks : allInlinks) {
      int eId = DataAccess.getInternalIdForKBEntity(getTestKBEntity(entityInlinks[0]));
      int[] inlinkIds = new int[entityInlinks.length - 1];
      for (int i = 1; i < entityInlinks.length; ++i) {
        inlinkIds[i-1] = DataAccess.getInternalIdForKBEntity(getTestKBEntity(entityInlinks[i]));
      }
      Arrays.sort(inlinkIds);
      inlinks.put(eId, inlinkIds);
    }
    
    return inlinks;
  }

  @Override
  public TObjectIntHashMap<KBIdentifiedEntity> getInternalIdsForKBEntities(
      Collection<KBIdentifiedEntity> entities) {
    TObjectIntHashMap<KBIdentifiedEntity> ids = 
        new TObjectIntHashMap<KBIdentifiedEntity>(entities.size());
    for (KBIdentifiedEntity entity : entities) {
      if (!entity2id.containsKey(entity.getIdentifier())) {
        throw new IllegalArgumentException(entity + " not in testing");
      } else {
        ids.put(entity, entity2id.get(entity.getIdentifier()));
      }
    }
    return ids;
  }

  @Override
  public TIntObjectHashMap<KBIdentifiedEntity> getKnowlegebaseEntitiesForInternalIds(int[] ids) {
    TIntObjectHashMap<KBIdentifiedEntity> entities = new TIntObjectHashMap<KBIdentifiedEntity>();
    for (int i = 0; i < ids.length; ++i) {
      if (!id2entity.containsKey(ids[i])) {
        throw new IllegalArgumentException(ids[i] + " not in testing");
      } else {
        entities.put(ids[i], getTestKBEntity(id2entity.get(ids[i])));
      }
      ++i;
    }
    return entities;
  }

  @Override
  public TIntObjectHashMap<String> getWordsForIds(int[] ids) {
    TIntObjectHashMap<String> words = new TIntObjectHashMap<String>();
    for (int i = 0; i < ids.length; ++i) {
      if (!id2word.containsKey(ids[i])) {
        throw new IllegalArgumentException(ids[i] + " not in testing");
      } else {
        words.put(ids[i], id2word.get(ids[i]));
      }
    }
    return words;
  }

  @Override
  public TObjectIntHashMap<String> getIdsForWords(Collection<String> words) {
    TObjectIntHashMap<String> ids = new TObjectIntHashMap<String>(words.size());
    for (String word : words) {
      int id = ids.getNoEntryValue();
      if (word2id.containsKey(word)) {
        id = word2id.get(word);
      }
      ids.put(word, id);
    }
    return ids;
  }
 
  @Override
  public TIntObjectHashMap<int[]> getEntityLSHSignatures(Entities entities, String table) {
    System.err.println("Accessed " + getMethodName());
    return null;
  }

  @Override
  public TIntDoubleHashMap getEntityPriors(String mention) {    
    if (mention.equals("PAGE")) {
      TIntDoubleHashMap pagePriors = 
          new TIntDoubleHashMap();
      pagePriors.put(DataAccess.getInternalIdForKBEntity(getTestKBEntity("Jimmy_Page")), 0.3);
      pagePriors.put(DataAccess.getInternalIdForKBEntity(getTestKBEntity("Larry_Page")), 0.7);
      return pagePriors;
    } else if (mention.equals("KASHMIR")) {
      TIntDoubleHashMap kashmirPriors =
          new TIntDoubleHashMap();
      kashmirPriors.put(DataAccess.getInternalIdForKBEntity(getTestKBEntity("Kashmir")), 0.9);
      kashmirPriors.put(DataAccess.getInternalIdForKBEntity(getTestKBEntity("Kashmir_(song)")), 0.1);
      return kashmirPriors;
    } else if (mention.equals("KNEBWORTH")) {
      TIntDoubleHashMap knebworthPriors = 
          new TIntDoubleHashMap();
      knebworthPriors.put(DataAccess.getInternalIdForKBEntity(getTestKBEntity("Knebworth_Festival")), 1.0);
      return knebworthPriors;
    } else if (mention.equals("LES PAUL")) {
      return new TIntDoubleHashMap();
    } else {
      return new TIntDoubleHashMap();
    }
  }

  private TIntIntHashMap getKeywordDocumentFrequencies(TIntHashSet keywords) {
    TIntIntHashMap freqs = new TIntIntHashMap();
    for (String[] kpF : allKeyphraseFrequencies) {
      String[] tokens = kpF[0].split(" ");
      int freq = Integer.parseInt(kpF[1]);
      for (String token : tokens) {
        freqs.put(DataAccess.getIdForWord(token), freq);
      }
    }
    
    for (int kw : keywords.toArray()) {
      if (!freqs.containsKey(kw)) {
        System.err.println(
            "allKeyphraseFrequencies do not contain token '" + 
            DataAccess.getWordForId(kw) + "'");
      }     
    }
    
    return freqs;
  }

  @Override
  public TIntIntHashMap getEntitySuperdocSize(Entities entities) {
    TIntIntHashMap sizes = new TIntIntHashMap();
    for (String entity[] : allEntitySizes) {
      int id = DataAccess.getInternalIdForKBEntity(getTestKBEntity(entity[0]));
      int size = Integer.parseInt(entity[1]);
      sizes.put(id, size);
    }

    for (Entity e : entities) {
      if (!sizes.containsKey(e.getId())) {
        System.err.println(
            "allEntitySizes does not contain '" +  e );
      }    
    }
    
    return sizes;
  }

  @Override
  public TIntObjectHashMap<TIntIntHashMap> getEntityKeywordIntersectionCount(Entities entities) {
    TIntObjectHashMap<TIntIntHashMap> isec = 
        new TIntObjectHashMap<TIntIntHashMap>();
    for (String[] eKps : allEntityKeyphrases) {
      int entity = DataAccess.getInternalIdForKBEntity(getTestKBEntity(eKps[0]));
      TIntIntHashMap counts = new TIntIntHashMap();
      isec.put(entity, counts);
      
      if (eKps.length > 1) {
        String[] tokens = null;
        for (int i = 1; i < eKps.length; ++i) {
          if (i % 2 == 1) {
            tokens = eKps[i].split(" ");
          } else {
            int count = Integer.parseInt(eKps[i]);
            for (String token : tokens) {
              counts.adjustOrPutValue(DataAccess.getIdForWord(token), count, count); 
            }
          }
        }
      }
    }
    return isec;
  }

  @Override
  public TIntObjectHashMap<TIntIntHashMap> getEntityUnitIntersectionCount(Entities entities, UnitType unitType) {
    if (unitType == UnitType.KEYWORD)
      return getEntityKeywordIntersectionCount(entities);
    return null;
  }

  private static String getMethodName()
  {
    StackTraceElement[] stacktrace = Thread.currentThread().getStackTrace();
    StackTraceElement e = stacktrace[2];
    String methodName = e.getMethodName();
    return methodName;
  }

  @Override
  public TIntIntHashMap getKeyphraseDocumentFrequencies(TIntHashSet keyphrases) {
    TIntIntHashMap freqs = new TIntIntHashMap();
    for (String[] kpF : allKeyphraseFrequencies) {
      int keyphrase = DataAccess.getIdForWord(kpF[0]);
      int freq = Integer.parseInt(kpF[1]);
      freqs.put(keyphrase, freq);
    }
    
    for (int kp : keyphrases.toArray()) {
      if (!freqs.containsKey(kp)) {
        System.err.println(
            "allKeyphraseFrequencies does not contain '" + 
            DataAccess.getWordForId(kp) + "'");
      }
    }
    
    return freqs;
  }

  @Override
  public TObjectIntHashMap<KBIdentifiedEntity> getAllEntityIds() {
    System.err.println("Accessed " + getMethodName());
    return null;
  }

  @Override
  public Entities getAllEntities() {
    Entities entities = new Entities();
    for (String eName : orderedEntities) {
      entities.add(getTestEntity(eName));
    }
    return entities;
  }

  @Override
  public int[] getAllWordExpansions() {
    int max = -1;
    for (int i : wordExpansions.keys()) {
      if (i > max) {
        max = i;
      }
    }
    
    int[] expansions = new int[max + 1];
    for (int i : wordExpansions.keys()) {
      expansions[i] = wordExpansions.get(i);
    }
    return expansions;
  }

  @Override
  public int[] getAllWordContractions() {
    int max = -1;
    for (int i : wordExpansions.values()) {
      if (i > max) {
        max = i;
      }
    }

    int[] contractions = new int[max + 1];
    for (int i : wordExpansions.keys()) {
      contractions[wordExpansions.get(i)] = i;
    }
    return contractions;
  }

  @Override
  public boolean isYagoEntity(Entity entity) {
    System.err.println("Accessed " + getMethodName());
    return false;
  }


  @Override
  public TIntObjectHashMap<int[]> getAllInlinks() {
    System.err.println("Accessed " + getMethodName());
    return null;
  }
  
  public TObjectIntHashMap<String> getAllWordIds() {
    System.err.println("Accessed " + getMethodName());
    return null;
  }


  @Override
  public int getCollectionSize() {
    return TOTAL_ENTITY_COUNT;
  }


  @Override
  public int getWordExpansion(int wordId) {
    return getAllWordExpansions()[wordId];
  }

  @Override
  public String getConfigurationName() {
    return "YAGO";
  }


  @Override
  public int[] getAllKeywordDocumentFrequencies() {
    TIntHashSet keywords = new TIntHashSet();
    int max = 0;
    for (String[] kpF : allKeyphraseFrequencies) {
      String[] tokens = kpF[0].split(" ");
      for (String token : tokens) {
        int id = DataAccess.getIdForWord(token);
        keywords.add(id);
        if (id > max) { max = id; }
      }
    }
    TIntIntHashMap keywordCounts = getKeywordDocumentFrequencies(keywords);
    int[] counts = new int[max + 1];
    for (TIntIntIterator itr = keywordCounts.iterator(); itr.hasNext(); ) {
      itr.advance();
      int keywordId = itr.key();     
      counts[keywordId] = itr.value();
    }
    
    return counts;
  }

  @Override
  public int[] getAllUnitDocumentFrequencies(UnitType unitType) {
    if (unitType == UnitType.KEYWORD)
      return getAllKeywordDocumentFrequencies();
    return null;
  }

  @Override
  public TObjectIntHashMap<String> getIdsForTypeNames(Collection<String> typeNames) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public TIntObjectHashMap<Type> getTypesForIds(int[] ids) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public TIntObjectHashMap<int[]> getTypesIdsForEntitiesIds(int[] ids) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public TIntObjectHashMap<int[]> getEntitiesIdsForTypesIds(int[] ids) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public TIntObjectHashMap<Gender> getGenderForEntities(Entities entities) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Map<String, List<String>> getAllEntitiesMetaData(String startingWith) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public TIntObjectHashMap<EntityMetaData> getEntitiesMetaData(int[] entitiesIds) {
    TIntObjectHashMap<EntityMetaData> md = new TIntObjectHashMap<>();
    
    Entities entities = getAllEntities();
    for (Entity e : entities) {
      EntityMetaData emd = 
          new EntityMetaData(e.getId(), e.getIdentifierInKb(),
              "http://" + e.getIdentifierInKb(), e.getKnowledgebase(), "", "");
      md.put(e.getId(), emd);
    }
    
    return md;
  }

  @Override
  public TIntDoubleHashMap getEntitiesImportances(int[] entitiesIds) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<String> getParentTypes(String queryType) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public TIntObjectHashMap<int[]> getEntityLSHSignatures(Entities entities) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public TIntIntHashMap getGNDTripleCount(Entities entities) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public TIntIntHashMap getGNDTitleCount(Entities entities) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public TIntIntHashMap getYagoOutlinkCount(Entities entities) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Pair<Integer, Integer> getImportanceComponentMinMax(String importanceId) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Map<String, Double> getKeyphraseSourceWeights() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public TIntObjectHashMap<int[]> getAllEntityTypes() {
    // TODO Auto-generated method stub
    return null;
  }
  

  @Override
  public TIntDoubleHashMap getAllEntityRanks() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Entities getEntitiesForMentionByFuzzyMatching(String mention, double minSimilarity) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public TObjectIntHashMap<String> getAllKeyphraseSources() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public TIntObjectHashMap<int[]> getAllKeyphraseTokens() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public TObjectIntHashMap<Type> getAllTypeIds() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public TIntObjectHashMap<int[]> getTaxonomy() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Map<String, int[]> getDictionary() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public int getMaximumEntityId() {
    int maxId = 0;
    for (Integer i : id2entity.keySet()) {
      if (i > maxId) {
        maxId = i;
      }
    }
    return maxId;
  }

  @Override
  public int getMaximumWordId() {
    int maxId = 0;
    for (Integer i : id2word.keySet()) {
      if (i > maxId) {
        maxId = i;
      }
    }
    return maxId;
  }
}
