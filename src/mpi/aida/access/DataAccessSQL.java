package mpi.aida.access;

import edu.stanford.nlp.util.StringUtils;
import gnu.trove.iterator.TIntIntIterator;
import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.hash.TIntHashSet;
import mpi.aida.AidaManager;
import mpi.aida.access.DataAccessSQLCache.EntityKeyphraseData;
import mpi.aida.access.DataAccessSQLCache.EntityKeywordsData;
import mpi.aida.config.AidaConfig;
import mpi.aida.data.*;
import mpi.aida.graph.similarity.UnitType;
import mpi.aida.util.CollectionUtils;
import mpi.aida.util.Counter;
import mpi.aida.util.Util;
import mpi.aida.util.YagoUtil;
import mpi.aida.util.YagoUtil.Gender;
import mpi.aida.util.timing.RunningTimer;
import mpi.tools.javatools.datatypes.Pair;
import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class DataAccessSQL implements DataAccessInterface {
  private static final Logger logger = 
      LoggerFactory.getLogger(DataAccessSQL.class);
  
  public static final String ENTITY_KEYPHRASES = "entity_keyphrases";
  public static final String ENTITY_BIGRAMS = "entity_bigrams";
  public static final String ENTITY_IDS = "entity_ids";
  public static final String TYPE_IDS = "type_ids";
  public static final String NAME_IDS = "named_ids";
  public static final String WORD_IDS = "word_ids";
  public static final String WORD_EXPANSION = "word_expansion";
  public static final String KEYPHRASE_COUNTS = "keyphrase_counts";
  public static final String KEYPHRASES_SOURCES_WEIGHTS = "keyphrases_sources_weights";
  public static final String KEYPHRASES_SOURCE = "keyphrase_sources";
  public static final String KEYPHRASES_TOKENS = "keyphrase_tokens";
  public static final String KEYWORD_COUNTS = "keyword_counts";
  public static final String BIGRAM_COUNTS = "bigram_counts";
  public static final String ENTITY_COUNTS = "entity_counts";
  public static final String ENTITY_KEYWORDS = "entity_keywords";
  public static final String ENTITY_LSH_SIGNATURES = "entity_lsh_signatures_2000";
  public static final String ENTITY_INLINKS = "entity_inlinks";
  public static final String ENTITY_TYPES = "entity_types";
  public static final String TYPE_ENTITIES = "type_entities";
  public static final String TYPE_TAXONOMY = "type_taxonomy";
  public static final String ENTITY_RANK = "entity_rank";
  public static final String DICTIONARY = "dictionary";
  public static final String YAGO_FACTS = "facts";
  public static final String METADATA = "meta";
  public static final String ENTITY_METADATA = "entity_metadata";
  public static final String IMPORTANCE_COMPONENTS_INFO = "importance_components_info";
  
  @Override
  public DataAccess.type getAccessType() {
    return DataAccess.type.sql;
  }

  @Override
  public Map<String, Entities> getEntitiesForMentions(Collection<String> mentions, double maxEntityRank, int topByPrior) {
    Integer id = RunningTimer.recordStartTime("DataAccess:getEntitiesForMention");
    Map<String, Entities> candidates = new HashMap<String, Entities>(mentions.size(), 1.0f);
    if (mentions.size() == 0) {
      return candidates;
    }
    List<String> queryMentions = new ArrayList<String>(mentions.size());
    for (String m : mentions) {
      queryMentions.add(AidaManager.conflateToken(m));
      // Add an emtpy candidate set as default.
      candidates.put(m, new Entities());
    }
    Connection mentionEntityCon = null;
    Statement statement = null;
    Map<String, Map<Integer, Double>> queryMentionCandidates = new HashMap<>();
    try {
      mentionEntityCon = 
          AidaManager.getConnectionForDatabase(AidaManager.DB_AIDA);
      statement = mentionEntityCon.createStatement();
      String sql = null;
      String query = YagoUtil.getPostgresEscapedConcatenatedQuery(queryMentions);
      if (maxEntityRank < 1.0) {
        sql = "SELECT " + DICTIONARY + ".mention, " + 
            DICTIONARY + ".entity " + DICTIONARY + ".prior FROM " + DICTIONARY +
            " JOIN " + ENTITY_RANK +
            " ON " + DICTIONARY + ".entity=" + ENTITY_RANK + ".entity" +
            " WHERE mention IN (" + query + ")" +
            " AND rank<" + maxEntityRank;
      } else {
        sql = "SELECT mention, entity, prior FROM " + DICTIONARY +
              " WHERE mention IN (" + query + ")";
      }
      ResultSet r = statement.executeQuery(sql);
      while (r.next()) {
        String mention = r.getString(1);
        int entity = r.getInt(2);
        double prior = r.getDouble(3);
        Map<Integer, Double> entities = queryMentionCandidates.get(mention);
        if (entities == null) {
          entities = new HashMap<>();
          queryMentionCandidates.put(mention, entities);
        }
        entities.put(entity, prior);
      }
      r.close();
      statement.close();
      AidaManager.releaseConnection(mentionEntityCon);

      // Get the candidates for the original Strings.
      for (Entry<String, Entities> entry: candidates.entrySet()) {
        String queryMention = AidaManager.conflateToken(entry.getKey());
        Map<Integer, Double> entityPriors = queryMentionCandidates.get(queryMention);
        if (entityPriors != null) {
          Integer[] ids;
          if (topByPrior > 0) {
            List<Integer> topIds = CollectionUtils.getTopKeys(entityPriors, topByPrior);
            int droppedByPrior = entityPriors.size() - topIds.size();
            Counter.incrementCountByValue("CANDIDATES_DROPPED_BY_PRIOR", droppedByPrior);
            ids = topIds.toArray(new Integer[topIds.size()]);
          } else {
            ids = entityPriors.keySet().toArray(new Integer[entityPriors.size()]);
          }
          TIntObjectHashMap<KBIdentifiedEntity> yagoEntityIds = getKnowlegebaseEntitiesForInternalIds(ArrayUtils.toPrimitive(ids));
          Entities entities = entry.getValue();
          for (int i = 0; i < ids.length; ++i) {
            entities.add(new Entity(yagoEntityIds.get(ids[i]), ids[i]));
          }
        }
      }
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage());
      e.printStackTrace();
    }
    RunningTimer.recordEndTime("DataAccess:getEntitiesForMention", id);
    return candidates;
  }
  
  @Override
  public Entities getEntitiesForMentionByFuzzyMatching(String mention, double minSimilarity) {
    Integer id = RunningTimer.recordStartTime("FuzzyEntitiesForMention");
    String conflatedMention = AidaManager.conflateToken(mention);
    TIntHashSet entitiesIds = new TIntHashSet();
    Connection mentionEntityCon = null;
    Statement statement = null;
    try {
      mentionEntityCon = 
          AidaManager.getConnectionForDatabase(AidaManager.DB_AIDA);
      statement = mentionEntityCon.createStatement();
      String sql = null;
      String query = YagoUtil.getPostgresEscapedString(conflatedMention);
        sql = "SELECT mention, entity  FROM " + DICTIONARY + 
              " WHERE mention % '" + query + "'" +
              " AND similarity(mention, '" +  query + "') >= " + minSimilarity;
      ResultSet r = statement.executeQuery(sql);
      while (r.next()) {
        int entity = r.getInt(2);
        entitiesIds.add(entity);
      }
      r.close();
      statement.close();
      AidaManager.releaseConnection(mentionEntityCon);      
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage());
      e.printStackTrace();
    }
    int[] ids = entitiesIds.toArray();
    TIntObjectHashMap<KBIdentifiedEntity> yagoEntityIds = getKnowlegebaseEntitiesForInternalIds(ids);

    Entities entities = new Entities();
    for (int i = 0; i < ids.length; ++i) {
      entities.add(new Entity(yagoEntityIds.get(ids[i]), ids[i]));
    }
    RunningTimer.recordEndTime("FuzzyEntitiesForMention", id);
    return entities;
  }

  @Override
  public Keyphrases getEntityKeyphrases(
      Entities entities, Map<String, Double> keyphraseSourceWeights,
      double minKeyphraseWeight, int maxEntityKeyphraseCount) {
    Integer runId = RunningTimer.recordStartTime("DataAccess:getEntityKeyPhrases");
    boolean useSources = keyphraseSourceWeights != null && !keyphraseSourceWeights.isEmpty();
    
    TObjectIntHashMap<String> keyphraseSrcName2Id = DataAccessCache.singleton().getAllKeyphraseSources();
    
    KeytermsCache<EntityKeyphraseData> kpc = 
        DataAccessSQLCache.singleton().
        getEntityKeyphrasesCache(entities, keyphraseSrcName2Id, keyphraseSourceWeights, minKeyphraseWeight,
            maxEntityKeyphraseCount, useSources);
    
    // Create and fill return object with empty maps.
    Keyphrases keyphrases = new Keyphrases();
    
    TIntObjectHashMap<int[]> entityKeyphrases = 
        new TIntObjectHashMap<int[]>();
    TIntObjectHashMap<TIntDoubleHashMap> entity2keyphrase2mi = 
        new TIntObjectHashMap<TIntDoubleHashMap>();
    TIntObjectHashMap<TIntDoubleHashMap> entity2keyword2mi = 
        new TIntObjectHashMap<TIntDoubleHashMap>();
    
    // Fill the keyphrases object with all data.
    keyphrases.setEntityKeyphrases(entityKeyphrases);
    keyphrases.setEntityKeyphraseWeights(entity2keyphrase2mi);
    keyphrases.setEntityKeywordWeights(entity2keyword2mi);
    
    // All keyphrase tokens are preloaded, just use the full data
    keyphrases.setKeyphraseTokens(DataAccessCache.singleton().getAllKeyphraseTokens());
    if (useSources) {
      TIntObjectHashMap<TIntIntHashMap> entity2keyphrase2source =
          new TIntObjectHashMap<TIntIntHashMap>();
      keyphrases.setEntityKeyphraseSources(entity2keyphrase2source);
      keyphrases.setKeyphraseSource2id(keyphraseSrcName2Id);
      TIntDoubleHashMap keyphraseSourceId2weight =
          new TIntDoubleHashMap();
      keyphrases.setKeyphraseSourceWeights(keyphraseSourceId2weight);
    }
    
    if (entities == null || entities.size() == 0) {            
      return keyphrases;
    }

    TIntObjectHashMap<TIntHashSet> eKps = 
        new TIntObjectHashMap<TIntHashSet>();
    for (Entity e : entities) {
      eKps.put(e.getId(), new TIntHashSet());
    }
        
    for (Pair<Integer, EntityKeyphraseData> p : kpc) {
      int entity = p.first;
      EntityKeyphraseData ekd = p.second;
      int keyphrase = ekd.keyphrase;
      double keyphraseWeight = ekd.weight;
              
      // Add keyphrase.
      TIntHashSet kps = eKps.get(entity);
      if (kps == null) {
        kps = new TIntHashSet();
        eKps.put(entity, kps);
      }
      kps.add(keyphrase);
      
      // Add keyphrase weight.
      TIntDoubleHashMap keyphrase2mi = entity2keyphrase2mi.get(entity);
      if (keyphrase2mi == null) {
        keyphrase2mi = new TIntDoubleHashMap();
        entity2keyphrase2mi.put(entity, keyphrase2mi);
      }
      keyphrase2mi.put(keyphrase, keyphraseWeight);
      
      if (useSources) {
        int source = ekd.source;
        TIntIntHashMap keyphraseSources = 
            keyphrases.getEntityKeyphraseSources().get(entity);
        if (keyphraseSources == null) {
          keyphraseSources = new TIntIntHashMap();
          keyphrases.getEntityKeyphraseSources().put(entity, keyphraseSources);
        }
        keyphraseSources.put(keyphrase, source);
      }
    }

    // Transform eKps to entityKeyphrases.
    for (Entity e : entities) {
      entityKeyphrases.put(e.getId(), eKps.get(e.getId()).toArray());
    }
    
    // Retrieve entity keywords and weights.
    KeytermsCache<EntityKeywordsData> kwc = DataAccessSQLCache.singleton().getEntityKeywordsCache(entities, keyphraseSourceWeights, 
        keyphrases.getEntityKeyphrases(), keyphrases.getKeyphraseTokens(), minKeyphraseWeight, maxEntityKeyphraseCount);
 
    for (Pair<Integer, EntityKeywordsData> p : kwc) {
      int entity = p.first;
      EntityKeywordsData ewd = p.second;
      int keyword = ewd.keyword;
      double keywordWeight = ewd.weight;
     
      // Add keywords and weights.
      TIntDoubleHashMap keyword2mi = entity2keyword2mi.get(entity);
      if (keyword2mi == null) {
        keyword2mi = new TIntDoubleHashMap();
        entity2keyword2mi.put(entity, keyword2mi);
      }      
      keyword2mi.put(keyword, keywordWeight);
    }    
    
    RunningTimer.recordEndTime("DataAccess:getEntityKeyPhrases", runId);
    return keyphrases;
  }
  
  @Override
  public void getEntityKeyphraseTokens(
      Entities entities,
      TIntObjectHashMap<int[]> entityKeyphrases,
      TIntObjectHashMap<int[]> keyphraseTokens) {
    if (entities == null || entities.size() == 0) {
      return;
    }

    Connection con = null;
    Statement statement = null;

    try {
      con = AidaManager.getConnectionForDatabase(AidaManager.DB_AIDA);
      String entityQueryString = StringUtils.join(entities.getUniqueIds(), ",");
      statement = con.createStatement();

      TIntObjectHashMap<int[]> allKeyphraseTokens = getAllKeyphraseTokens();
      
      String sql = "SELECT entity,keyphrase " +
      		         " FROM " + ENTITY_KEYPHRASES +
      		         " WHERE entity IN (" + entityQueryString + ")";
      ResultSet rs = statement.executeQuery(sql);
      TIntObjectHashMap<TIntHashSet> eKps = 
          new TIntObjectHashMap<TIntHashSet>();
      for (Entity e : entities) {
        eKps.put(e.getId(), new TIntHashSet());
      }
      while (rs.next()) {
        int entity = rs.getInt("entity");
        int keyphrase = rs.getInt("keyphrase");
        TIntHashSet kps = eKps.get(entity);
        if (kps == null) {
          kps = new TIntHashSet();
          eKps.put(entity, kps);
        }
        kps.add(keyphrase);
        
        if (!keyphraseTokens.containsKey(keyphrase)) {
          int[] tokenIds = allKeyphraseTokens.get(keyphrase);
          keyphraseTokens.put(keyphrase, tokenIds);
        }
      }
      rs.close();
      statement.close();
      
      // Transform eKps to entityKeyphrases.
      for (Entity e : entities) {
        entityKeyphrases.put(e.getId(), eKps.get(e.getId()).toArray());
      }
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage());
    } finally {
      AidaManager.releaseConnection(con);
    }
  }

  @Override
  public TIntIntHashMap getEntitySuperdocSize(Entities entities) {
    TIntIntHashMap entitySuperDocSizes = new TIntIntHashMap();
    
    if (entities == null || entities.size() == 0) {
      return entitySuperDocSizes;
    }

    Connection con = null;
    Statement statement = null;
    try {
      con = AidaManager.getConnectionForDatabase(AidaManager.DB_AIDA);
      statement = con.createStatement();
      String entitiesQuery = StringUtils.join(entities.getUniqueIds(), ",");
      String sql = "SELECT entity, count" +
      		          " FROM " + ENTITY_COUNTS +
      		          " WHERE entity IN (" + entitiesQuery + ")";
      ResultSet r = statement.executeQuery(sql);
      while (r.next()) {
        int entity = r.getInt("entity");
        int entityDocCount = r.getInt("count");
        entitySuperDocSizes.put(entity, entityDocCount);
      }
      r.close();
      statement.close();
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage());
    } finally {
      AidaManager.releaseConnection(con);
    }
    
    return entitySuperDocSizes;
  }

  @Override
  public TIntObjectHashMap<TIntIntHashMap> getEntityKeywordIntersectionCount(Entities entities) {
    TIntObjectHashMap<TIntIntHashMap> entityKeywordIC = new TIntObjectHashMap<TIntIntHashMap>();
    for (Entity entity : entities) {
     TIntIntHashMap keywordsIC = new TIntIntHashMap();
      entityKeywordIC.put(entity.getId(), keywordsIC);
    }
    
    if (entities == null || entities.size() == 0) {
      return entityKeywordIC;
    }

    Connection con = null;
    Statement statement = null;
    try {
      con = AidaManager.getConnectionForDatabase(AidaManager.DB_AIDA);
      statement = con.createStatement();
      String entitiesQuery = StringUtils.join(entities.getUniqueIds(), ",");
      String sql = "SELECT entity, keyword, count" +
      		         " FROM " + ENTITY_KEYWORDS +
      		         " WHERE entity IN (" + entitiesQuery + ")";
      ResultSet r = statement.executeQuery(sql);
      while (r.next()) {
        int entity = r.getInt("entity");
        int keyword = r.getInt("keyword");
        int keywordCount = r.getInt("count");
        entityKeywordIC.get(entity).put(keyword, keywordCount);
      }
      r.close();
      statement.close();
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage());
    } finally {
      AidaManager.releaseConnection(con);
    }
    
    return entityKeywordIC;
  }

  @Override
  public TIntObjectHashMap<TIntIntHashMap> getEntityUnitIntersectionCount(Entities entities, UnitType unitType) {
    int timer = RunningTimer.recordStartTime("DataAccessSQL#getEntityUnitIntersectionCount()");
    TIntObjectHashMap<TIntIntHashMap> entityKeywordIC = new TIntObjectHashMap<TIntIntHashMap>();
    for (Entity entity : entities) {
      TIntIntHashMap keywordsIC = new TIntIntHashMap();
      entityKeywordIC.put(entity.getId(), keywordsIC);
    }

    if (entities == null || entities.size() == 0) {
      return entityKeywordIC;
    }

    Connection con = null;
    Statement statement = null;
    try {
      con = AidaManager.getConnectionForDatabase(AidaManager.DB_AIDA);
      statement = con.createStatement();
      String entitiesQuery = StringUtils.join(entities.getUniqueIds(), ",");
      String sql = "SELECT entity, " + unitType.getUnitName() + ", count" +
        " FROM " + unitType.getEntityUnitCooccurrenceTableName() +
        " WHERE entity IN (" + entitiesQuery + ")";
      ResultSet r = statement.executeQuery(sql);
      while (r.next()) {
        int entity = r.getInt(1);
        int keyword = r.getInt(2);
        int keywordCount = r.getInt(3);
        entityKeywordIC.get(entity).put(keyword, keywordCount);
      }
      r.close();
      statement.close();
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage());
    } finally {
      AidaManager.releaseConnection(con);
    }
    RunningTimer.recordEndTime("DataAccessSQL#getEntityUnitIntersectionCount()", timer);
    return entityKeywordIC;
  }

  @Override
  public TIntObjectHashMap<int[]> getInlinkNeighbors(Entities entities) {
    TIntObjectHashMap<int[]> neighbors = new TIntObjectHashMap<int[]>();
    if (entities.isEmpty()) {
      return neighbors;
    }
    
    for (int entityId : entities.getUniqueIds()) {
      neighbors.put(entityId, new int[0]);
    }

    Connection con = null;
    Statement statement = null;
    
    String entitiesQuery = StringUtils.join(entities.getUniqueIds(), ",");
    try {
      con = AidaManager.getConnectionForDatabase(AidaManager.DB_AIDA);
      statement = con.createStatement();
      String sql = "SELECT entity, inlinks FROM " + 
                   DataAccessSQL.ENTITY_INLINKS + 
                   " WHERE entity IN (" + entitiesQuery + ")";
      ResultSet rs = statement.executeQuery(sql);
      while (rs.next()) {
        Integer[] neigbors = (Integer[]) rs.getArray("inlinks").getArray();
        int entity = rs.getInt("entity");
        neighbors.put(entity, ArrayUtils.toPrimitive(neigbors));
      }
      rs.close();
      statement.close();
      return neighbors;
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage());
    } finally {
      AidaManager.releaseConnection(con);
    }
    return neighbors;
  }

  @Override
  public TIntObjectHashMap<Gender> getGenderForEntities(Entities entities) {
    TIntObjectHashMap<Gender> entityGenders = new TIntObjectHashMap<Gender>();
    Map<String, Gender> genders = new HashMap<String, Gender>();

    Connection con = null;
    Statement statement = null;
    try {
      con = AidaManager.getConnectionForDatabase(AidaManager.DB_YAGO2_FULL);
      statement = con.createStatement();
      String sql = "SELECT arg1,arg2 FROM " + YAGO_FACTS + 
                   " WHERE arg1 IN (" + YagoUtil.getPostgresEscapedConcatenatedQuery(entities.getUniqueNames()) + ") " + "AND relation='hasGender'";
      ResultSet rs = statement.executeQuery(sql);
      while (rs.next()) {
        String entity = rs.getString("arg1");
        String gender = rs.getString("arg2");

        Gender g = Gender.FEMALE;

        if (gender.equals("male")) {
          g = Gender.MALE;
        }

        genders.put(entity, g);
      }
      rs.close();
      statement.close();
      
      for (Entity e : entities) {
        entityGenders.put(e.getId(), genders.get(e.getIdentifierInKb()));
      }
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage());
    } finally {
      AidaManager.releaseConnection(con);
    }
    return entityGenders;
  }
 
  @Override
  public TIntIntHashMap getKeyphraseDocumentFrequencies(
      TIntHashSet keyphrases) {
    TIntIntHashMap keyphraseCounts = new TIntIntHashMap();

    if (keyphrases == null || keyphrases.size() == 0) {
      return keyphraseCounts;
    }

    Connection con = null;
    Statement statement = null;

    try {
      con = AidaManager.getConnectionForDatabase(AidaManager.DB_AIDA);
      statement = con.createStatement();

      String keyphraseQueryString = YagoUtil.getIdQuery(keyphrases);

      String sql = "SELECT keyphrase,count " + "FROM " + KEYPHRASE_COUNTS + 
                   " WHERE keyphrase IN (" + keyphraseQueryString + ")";
      ResultSet rs = statement.executeQuery(sql);

      while (rs.next()) {
        int keyphrase = rs.getInt("keyphrase");
        int count= rs.getInt("count");

        keyphraseCounts.put(keyphrase, count);
      }

      rs.close();
      statement.close();
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage());
    } finally {
      AidaManager.releaseConnection(con);
    }

    return keyphraseCounts;
  }

  /**
   * Retrieves all parent types for the given YAGO2 type (via the subClassOf relation).
   * 
   * @param TokenizationType  type (in YAGO2 format) to retrieve parent types for 
   * @return        List of types in YAGO2 format
   * @throws SQLException
   */
  @Override
  public List<String> getParentTypes(String queryType) {
    List<String> types = new LinkedList<String>();
    Connection con = null;
    try {
      con = AidaManager.getConnectionForDatabase(AidaManager.DB_YAGO2);
      Statement statement = con.createStatement();
      String sql = "SELECT arg2 FROM facts " + "WHERE arg1=E'" + YagoUtil.getPostgresEscapedString(queryType) + "' " + "AND relation='subclassOf'";
      ResultSet rs = statement.executeQuery(sql);
      while (rs.next()) {
        String type = rs.getString(1);
        types.add(type);
      }
      rs.close();
      statement.close();
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage());
    } finally {
      AidaManager.releaseConnection(con);
    }
    return types;
  }

  @Override
  public TIntObjectHashMap<int[]> getEntityLSHSignatures(Entities entities) {
    return getEntityLSHSignatures(entities, ENTITY_LSH_SIGNATURES);
  }

  @Override
  public TIntObjectHashMap<int[]> getEntityLSHSignatures(Entities entities, String table) {
//    Map<String, int[]> tmpEntitySignatures = new HashMap<String, int[]>();
    TIntObjectHashMap<int[]> entitySignatures = 
        new TIntObjectHashMap<int[]>();
    
    Connection con = null;
    Statement statement = null;

    try {
      con = AidaManager.getConnectionForDatabase(AidaManager.DB_AIDA);
      String entityQueryString = StringUtils.join(entities.getUniqueIds(), ",");
      statement = con.createStatement();
      
      String sql = "SELECT entity, signature FROM " + table + " WHERE entity IN (" + entityQueryString + ")";
      ResultSet rs = statement.executeQuery(sql);
      while (rs.next()) {
        int entity = rs.getInt("entity");
        int[] sig = org.apache.commons.lang.ArrayUtils.toPrimitive((Integer[]) rs.getArray("signature").getArray());
        entitySignatures.put(entity, sig);        
      }
      rs.close();
      statement.close();
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage());
    } finally {
      AidaManager.releaseConnection(con);
    }

    return entitySignatures;
  }  

  public TIntDoubleHashMap getEntityPriors(String mention) {
    mention = AidaManager.conflateToken(mention);
    TIntDoubleHashMap entityPriors = new TIntDoubleHashMap();
    Connection con = null;
    Statement statement = null;
    try {
      con = AidaManager.getConnectionForDatabase(AidaManager.DB_AIDA);
      statement = con.createStatement();
      String sql = "SELECT entity,prior FROM " + DICTIONARY +
          " WHERE mention=E'" + 
           YagoUtil.getPostgresEscapedString(
               mention) + "'";
      ResultSet rs = statement.executeQuery(sql);
      while (rs.next()) {
        int entity = rs.getInt("entity");
        double prior = rs.getDouble("prior");
        entityPriors.put(entity, prior);
      }
      rs.close();
      statement.close();
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage());
    } finally {
      AidaManager.releaseConnection(con);
    }
    return entityPriors;
  }

  @Override
  public TIntObjectHashMap<KBIdentifiedEntity> getKnowlegebaseEntitiesForInternalIds(int[] ids) {
    TIntObjectHashMap<KBIdentifiedEntity> entityIds = new TIntObjectHashMap<KBIdentifiedEntity>();
    if (ids.length == 0) {
      return entityIds;
    }
    Connection con = null;
    Statement stmt = null;
    try {
      con = AidaManager.getConnectionForDatabase(AidaManager.DB_AIDA);
      con.setAutoCommit(false);
      stmt = con.createStatement();
      stmt.setFetchSize(100000);
      String idQuery = YagoUtil.getIdQuery(ids);
      String sql = "SELECT entity, id, knowledgebase FROM " + DataAccessSQL.ENTITY_IDS + 
                   " WHERE id IN (" + idQuery + ")";
      ResultSet rs = stmt.executeQuery(sql);
      int read = 0;
      while (rs.next()) {
        String entity = rs.getString("entity");
        String kb = rs.getString("knowledgebase");
        int id = rs.getInt("id");
        entityIds.put(id, new KBIdentifiedEntity(entity, kb));

        if (++read % 1000000 == 0) {
          logger.info("Read " + read + " entity ids.");
        }
      }
      con.setAutoCommit(true);
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage());
    } finally {
      AidaManager.releaseConnection(con);
    }
    return entityIds;
  }

  @Override
  public TObjectIntHashMap<KBIdentifiedEntity> getInternalIdsForKBEntities(
      Collection<KBIdentifiedEntity> kbEntities) {
    //for performance, query by entities names only, and filter later by kb
    TObjectIntHashMap<KBIdentifiedEntity> allEntities = new TObjectIntHashMap<KBIdentifiedEntity>();
    TObjectIntHashMap<KBIdentifiedEntity> filteredEntities = new TObjectIntHashMap<KBIdentifiedEntity>();
    if(kbEntities.size() == 0) {
      return filteredEntities;
    }
    
    // TODO this looks like a bug, why is the knowledge base id dropped?
    // FIXME: Indeed a bug, the KB-part should be part of the query.
    // it should be part of the query.
    Set<String> entities = new HashSet<String>();
    for(KBIdentifiedEntity e: kbEntities) {
      entities.add(e.getIdentifier());
    }
    
       
    Connection con = null;
    Statement stmt = null;
    try {
      con = AidaManager.getConnectionForDatabase(AidaManager.DB_AIDA);
      con.setAutoCommit(false);
      stmt = con.createStatement();
      stmt.setFetchSize(100000);
      String idQuery = YagoUtil.getPostgresEscapedConcatenatedQuery(entities);
      String sql = "SELECT entity, id, knowledgebase FROM " + DataAccessSQL.ENTITY_IDS + 
                   " WHERE entity IN (" + idQuery + ")";
      ResultSet rs = stmt.executeQuery(sql);
      int read = 0;
      while (rs.next()) {
        String entity = rs.getString("entity");
        String kb = rs.getString("knowledgebase");
        int id = rs.getInt("id");
        allEntities.put(new KBIdentifiedEntity(entity, kb), id);

        if (++read % 1000000 == 0) {
          logger.info("Read " + read + " entity ids.");
        }
      }
      con.setAutoCommit(true);
      
      for(KBIdentifiedEntity e: kbEntities) { 
        filteredEntities.put(e, allEntities.get(e));
      }
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage());
    } finally {
      AidaManager.releaseConnection(con);
    }
    return filteredEntities;
  }

  @Override
  public TIntObjectHashMap<String> getWordsForIds(int[] ids) {
    TIntObjectHashMap<String> wordIds = new TIntObjectHashMap<String>();
    if (ids.length == 0) {
      return wordIds;
    }
    Connection con = null;
    Statement stmt = null;
    try {
      con = AidaManager.getConnectionForDatabase(AidaManager.DB_AIDA);
      con.setAutoCommit(false);
      stmt = con.createStatement();
      stmt.setFetchSize(100000);
      String idQuery = YagoUtil.getIdQuery(ids);
      String sql = "SELECT word, id FROM " + DataAccessSQL.WORD_IDS + 
                   " WHERE id IN (" + idQuery + ")";
      ResultSet rs = stmt.executeQuery(sql);
      int read = 0;
      while (rs.next()) {
        String word = rs.getString("word");
        int id = rs.getInt("id");
        wordIds.put(id, word);

        if (++read % 1000000 == 0) {
          logger.info("Read " + read + " word ids.");
        }
      }
      con.setAutoCommit(true);
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage());
    } finally {
      AidaManager.releaseConnection(con);
    }
    return wordIds;
  }

  @Override
  public TObjectIntHashMap<String> getIdsForWords(Collection<String> keywords) {
    TObjectIntHashMap<String> wordIds = new TObjectIntHashMap<String>();
    if (keywords.isEmpty()) {
      return wordIds;
    }
    Connection con = null;
    Statement stmt = null;
    try {
      con = AidaManager.getConnectionForDatabase(AidaManager.DB_AIDA);
      con.setAutoCommit(false);
      stmt = con.createStatement();
      stmt.setFetchSize(100000);
      String idQuery = YagoUtil.getPostgresEscapedConcatenatedQuery(keywords);
      String sql = "SELECT word, id FROM " + DataAccessSQL.WORD_IDS + 
                   " WHERE word IN (" + idQuery + ")";
      ResultSet rs = stmt.executeQuery(sql);
      int read = 0;
      while (rs.next()) {
        String word = rs.getString("word");
        int id = rs.getInt("id");
        wordIds.put(word, id);

        if (++read % 1000000 == 0) {
          logger.info("Read " + read + " word ids.");
        }
      }
      con.setAutoCommit(true);
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage());
    } finally {
      AidaManager.releaseConnection(con);
    }
    return wordIds;
  }

  @Override
  public TObjectIntHashMap<String> getAllKeyphraseSources() {
    Integer kpSrcTime = RunningTimer.recordStartTime("DataAccessSQL:getAllKPSrc");
    Connection con = null;
    Statement stmt = null;
    TObjectIntHashMap<String> keyphraseSources = new TObjectIntHashMap<String>();
    try{
      con = AidaManager.getConnectionForDatabase(AidaManager.DB_AIDA);
      con.setAutoCommit(false);
      stmt = con.createStatement();
      String sql = "SELECT source, source_id FROM " + DataAccessSQL.KEYPHRASES_SOURCE;
      ResultSet rs = stmt.executeQuery(sql);
      while(rs.next()) {
        int sourceId = rs.getInt("source_id");
        String sourceName = rs.getString("source");
        keyphraseSources.put(sourceName, sourceId);
      }
    }catch(Exception e){
      logger.error(e.getLocalizedMessage());
    }finally {
      AidaManager.releaseConnection(con);
    }
    RunningTimer.recordEndTime("DataAccessSQL:getAllKPSrc", kpSrcTime);
    return keyphraseSources;
  }
  
  @Override
  public TIntObjectHashMap<int[]> getAllKeyphraseTokens() {
    Connection con = null;
    Statement stmt = null;
    TIntObjectHashMap<int[]> keyphraseTokens = new TIntObjectHashMap<int[]>();    
    try {
      con = AidaManager.getConnectionForDatabase(AidaManager.DB_AIDA);
      stmt = con.createStatement();
      stmt.setFetchSize(1000000);
      String sql = 
          "SELECT keyphrase, token FROM " + 
              DataAccessSQL.KEYPHRASES_TOKENS +
              " ORDER BY keyphrase, position";
      ResultSet rs = stmt.executeQuery(sql);
      
      int prevKp = -1;
      int currentKp = -1;
      TIntArrayList currentTokens = new TIntArrayList();
      int read = 0;
      while(rs.next()) {
        currentKp = rs.getInt(1);
        if (prevKp != -1 && currentKp != prevKp) {
          keyphraseTokens.put(prevKp, currentTokens.toArray());
          currentTokens.clear();
          if (++read % 1000000 == 0) {
            logger.debug("Read " + read + " keyphrase tokens.");
          }
        }
        currentTokens.add(rs.getInt(2));
        prevKp = currentKp;
      }
      // Put in last keyphrase.
      keyphraseTokens.put(currentKp, currentTokens.toArray());
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage());
    } finally {
      AidaManager.releaseConnection(con);
    }
        
    return keyphraseTokens;
  }
  
  @Override
  public TObjectIntHashMap<KBIdentifiedEntity> getAllEntityIds() {
    Connection con = null;
    Statement stmt = null;
    TObjectIntHashMap<KBIdentifiedEntity> entityIds = new TObjectIntHashMap<KBIdentifiedEntity>();
    try {
      con = AidaManager.getConnectionForDatabase(AidaManager.DB_AIDA);
      con.setAutoCommit(false);
      stmt = con.createStatement();
      stmt.setFetchSize(100000);
      String sql = "SELECT entity, id, knowledgebase FROM " + DataAccessSQL.ENTITY_IDS +
                   " WHERE knowledgebase <> 'MENTION'";
      ResultSet rs = stmt.executeQuery(sql);
      int read = 0;
      while (rs.next()) {
        String entity = rs.getString("entity");
        String kb = rs.getString("knowledgebase");
        int id = rs.getInt("id");
        entityIds.put(new KBIdentifiedEntity(entity, kb), id);

        if (++read % 1000000 == 0) {
          logger.info("Read " + read + " entity ids.");
        }
      }
      con.setAutoCommit(true);
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage());
    } finally {
      AidaManager.releaseConnection(con);
    }
    return entityIds;
  }
  
  @Override
  public TObjectIntHashMap<Type> getAllTypeIds() {
    Connection con = null;
    Statement stmt = null;
    TObjectIntHashMap<Type> typeIds = new TObjectIntHashMap<Type>();
    try {
      con = AidaManager.getConnectionForDatabase(AidaManager.DB_AIDA);
      con.setAutoCommit(false);
      stmt = con.createStatement();
      stmt.setFetchSize(100000);
      String sql = "SELECT type, id, knowledgebase FROM " + DataAccessSQL.TYPE_IDS;
      ResultSet rs = stmt.executeQuery(sql);
      int read = 0;
      while (rs.next()) {
        String type = rs.getString("type");
        String kb = rs.getString("knowledgebase");
        int id = rs.getInt("id");
        typeIds.put(new Type(kb, type), id);

        if (++read % 1000000 == 0) {
          logger.info("Read " + read + " type ids.");
        }
      }
      con.setAutoCommit(true);
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage());
    } finally {
      AidaManager.releaseConnection(con);
    }
    return typeIds;
  }

  @Override
  public TIntDoubleHashMap getAllEntityRanks() {
    Connection con = null;
    Statement stmt = null;
    TIntDoubleHashMap entityRanks = new TIntDoubleHashMap();
    try {
      con = AidaManager.getConnectionForDatabase(AidaManager.DB_AIDA);
      con.setAutoCommit(false);
      stmt = con.createStatement();
      stmt.setFetchSize(100000);
      String sql = "SELECT entity, rank FROM " + DataAccessSQL.ENTITY_RANK;
      ResultSet rs = stmt.executeQuery(sql);
      int read = 0;
      while (rs.next()) {
        int entity = rs.getInt("entity");
        double rank = rs.getDouble("rank");
        entityRanks.put(entity, rank);

        if (++read % 1000000 == 0) {
          logger.info("Read " + read + " entity ranks.");
        }
      }
      con.setAutoCommit(true);
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage());
    } finally {
      AidaManager.releaseConnection(con);
    }
    return entityRanks;
  }

  @Override
  public TIntObjectHashMap<int[]> getAllEntityTypes() {
    Connection con = null;
    Statement stmt = null;
    TIntObjectHashMap<int[]> entityTypes = new TIntObjectHashMap<>();
    try {
      con = AidaManager.getConnectionForDatabase(AidaManager.DB_AIDA);
      con.setAutoCommit(false);
      stmt = con.createStatement();
      stmt.setFetchSize(100000);
      String sql = "SELECT entity, types FROM " + DataAccessSQL.ENTITY_TYPES;
      ResultSet rs = stmt.executeQuery(sql);
      int read = 0;
      while (rs.next()) {
        int entity = rs.getInt(1);
        Integer[] types = (Integer[]) rs.getArray(2).getArray();
        entityTypes.put(entity, ArrayUtils.toPrimitive(types));
        if (++read % 1000000 == 0) {
          logger.info("Read " + read + " entity types.");
        }
      }
      con.setAutoCommit(true);
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage());
    } finally {
      AidaManager.releaseConnection(con);
    }
    return entityTypes;
  }
  
  @Override
  public TIntObjectHashMap<int[]> getTaxonomy() {
    Connection con = null;
    Statement stmt = null;
    TIntObjectHashMap<int[]> taxonomy = new TIntObjectHashMap<>();
    try {
      con = AidaManager.getConnectionForDatabase(AidaManager.DB_AIDA);
      con.setAutoCommit(false);
      stmt = con.createStatement();
      stmt.setFetchSize(100000);
      String sql = "SELECT type, parents FROM " + DataAccessSQL.TYPE_TAXONOMY;
      ResultSet rs = stmt.executeQuery(sql);
      int read = 0;
      while (rs.next()) {
        int type = rs.getInt(1);
        Integer[] parents = (Integer[]) rs.getArray(2).getArray();
        taxonomy.put(type, ArrayUtils.toPrimitive(parents));
        if (++read % 1000000 == 0) {
          logger.info("Read " + read + " type parents.");
        }
      }
      con.setAutoCommit(true);
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage());
    } finally {
      AidaManager.releaseConnection(con);
    }
    return taxonomy;
  }

  @Override
  public TObjectIntHashMap<String> getAllWordIds() {
    Connection con = null;
    Statement stmt = null;
    TObjectIntHashMap<String> wordIds = new TObjectIntHashMap<String>(10000000);
    try {
      con = AidaManager.getConnectionForDatabase(AidaManager.DB_AIDA);
      con.setAutoCommit(false);
      stmt = con.createStatement();
      stmt.setFetchSize(100000);
      String sql = "SELECT word, id FROM " + DataAccessSQL.WORD_IDS;
      ResultSet rs = stmt.executeQuery(sql);
      int read = 0;
      while (rs.next()) {
        String word = rs.getString("word");
        int id = rs.getInt("id");
        wordIds.put(word, id);

        if (++read % 1000000 == 0) {
          logger.info("Read " + read + " word ids.");
        }
      }
      con.setAutoCommit(true);
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage());
    } finally {
      AidaManager.releaseConnection(con);
    }
    return wordIds;
  }

  @Override
  public Entities getAllEntities() {
    TObjectIntHashMap<KBIdentifiedEntity> entityIds = getAllEntityIds();
    Entities entities = new Entities();
    for (TObjectIntIterator<KBIdentifiedEntity> itr = entityIds.iterator(); 
        itr.hasNext(); ) {
      itr.advance();
      entities.add(new Entity(itr.key(), itr.value()));
    }
    return entities;
  }

  @Override
  public int[] getAllWordExpansions() {
    Connection con = null;
    Statement stmt = null;
    TIntIntHashMap wordExpansions = new TIntIntHashMap();
    int maxId = -1;
    try {
      logger.info("Reading word expansions.");
      con = AidaManager.getConnectionForDatabase(AidaManager.DB_AIDA);
      con.setAutoCommit(false);
      stmt = con.createStatement();
      stmt.setFetchSize(1000000);
      String sql = "SELECT word, expansion FROM " + WORD_EXPANSION;
      ResultSet rs = stmt.executeQuery(sql);
      int read = 0;
      while (rs.next()) {
        int word = rs.getInt("word");
        int expansion = rs.getInt("expansion");
        wordExpansions.put(word, expansion);
        if (word > maxId) {
          maxId = word;
        }

        if (++read % 1000000 == 0) {
         logger.debug("Read " + read + " word expansions.");
        }
      }
      con.setAutoCommit(true);
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage());
    } finally {
      AidaManager.releaseConnection(con);
    }
    
    // Transform hash to int array.
    int[] expansions = new int[maxId + 1];
    for (TIntIntIterator itr = wordExpansions.iterator(); itr.hasNext(); ) {
      itr.advance();
      assert itr.key() < expansions.length && itr.key() > 0;  // Ids start at 1.
      expansions[itr.key()] = itr.value();
    }
    return expansions;
  }

  @Override
  public int[] getAllWordContractions() {
    Connection con = null;
    Statement stmt = null;
    TIntIntHashMap wordContraction = new TIntIntHashMap();
    int maxId = -1;
    try {
      logger.info("Reading word contractions.");
      con = AidaManager.getConnectionForDatabase(AidaManager.DB_AIDA);
      con.setAutoCommit(false);
      stmt = con.createStatement();
      stmt.setFetchSize(1000000);
      String sql = "SELECT word, expansion FROM " + WORD_EXPANSION;
      ResultSet rs = stmt.executeQuery(sql);
      int read = 0;
      while (rs.next()) {
        int word = rs.getInt("word");
        int expansion = rs.getInt("expansion");
        wordContraction.put(expansion, word);
        if (expansion > maxId) {
          maxId = expansion;
        }

        if (++read % 1000000 == 0) {
          logger.debug("Read " + read + " word expansions.");
        }
      }
      con.setAutoCommit(true);
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage());
    } finally {
      AidaManager.releaseConnection(con);
    }

    // Transform hash to int array.
    int[] contractions = new int[maxId + 1];
    for (TIntIntIterator itr = wordContraction.iterator(); itr.hasNext(); ) {
      itr.advance();
      assert itr.key() < contractions.length && itr.key() > 0;  // Ids start at 1.
      contractions[itr.key()] = itr.value();
    }
    return contractions;
  }

  @Override
  public int getWordExpansion(int wordId) {
    Connection con = null;
    Statement stmt = null;
    int wordExpansion = 0;
    try {
      con = AidaManager.getConnectionForDatabase(AidaManager.DB_AIDA);
      stmt = con.createStatement();
      String sql = "SELECT expansion FROM " + WORD_EXPANSION + 
                   " WHERE word=" + wordId;
      ResultSet rs = stmt.executeQuery(sql);
      if (rs.next()) {
        wordExpansion = rs.getInt("expansion");
      }
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage());
    } finally {
      AidaManager.releaseConnection(con);
    }
    return wordExpansion;
  }

  @Override
  public boolean isYagoEntity(Entity entity) {
    Connection con = null;
    Statement stmt = null;
    boolean isYagoEntity = false;
    try {
      con = AidaManager.getConnectionForDatabase(AidaManager.DB_YAGO2);
      stmt = con.createStatement();
      String sql = "SELECT arg1 FROM facts WHERE arg1=E'" + 
                    YagoUtil.getPostgresEscapedString(entity.getIdentifierInKb()) + 
                    "' AND relation='hasWikipediaUrl'";
      ResultSet rs = stmt.executeQuery(sql);
      
      // if there is a result, it means it is a YAGO entity
      if (rs.next()) {
        isYagoEntity = true;
      } 
      rs.close();
      stmt.close();
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage());
    } finally {
      AidaManager.releaseConnection(con);
    }
    return isYagoEntity;
  }

  @Override
  public TIntObjectHashMap<int[]> getAllInlinks() {
    TIntObjectHashMap<int[]> inlinks = new TIntObjectHashMap<int[]>();
    Connection con = null;
    Statement statement = null;    
    try {
      con = AidaManager.getConnectionForDatabase(AidaManager.DB_AIDA);
      con.setAutoCommit(false);
      statement = con.createStatement();
      statement.setFetchSize(100000);
      int read = 0;
      String sql = "SELECT entity, inlinks FROM " + DataAccessSQL.ENTITY_INLINKS; 
      ResultSet rs = statement.executeQuery(sql);
      while (rs.next()) {
        Integer[] neigbors = (Integer[]) rs.getArray("inlinks").getArray();
        int entity = rs.getInt("entity");
        inlinks.put(entity, ArrayUtils.toPrimitive(neigbors));
        
        if (++read % 1000000 == 0) {
          logger.info("Read " + read + " entity inlinks.");
        }
      }
      rs.close();
      statement.close();
      con.setAutoCommit(true);
      return inlinks;
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage());
    } finally {
      AidaManager.releaseConnection(con);
    }
    return inlinks;
  }

  @Override
  public int getCollectionSize() {
    Connection con = null;
    Statement stmt = null;
    int collectionSize = 0;
    try {
      con = AidaManager.getConnectionForDatabase(AidaManager.DB_AIDA);
      stmt = con.createStatement();
      String sql = "SELECT value FROM " + METADATA +
      		         " WHERE key='collection_size'";
      ResultSet rs = stmt.executeQuery(sql);
      
      // if there is a result, it means it is a YAGO entity
      if (rs.next()) {
        String sizeString = rs.getString("value");
        collectionSize = Integer.parseInt(sizeString);        
      } 
      rs.close();
      stmt.close();
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage());
      logger.error("You might have an outdated entity repository, please " +
      		"download the latest version from the AIDA website. Also check " +
      		"above for other error messages, maybe the connection to the " +
      		"Postgres database is not working properly.");
      throw new IllegalStateException(
          "You might have an outdated entity repository, please " +
              "download the latest version from the AIDA website. Also check " +
              "above for other error messages, maybe the connection to the " +
              "Postgres database is not working properly.");
    } finally {
      AidaManager.releaseConnection(con);
    }
    return collectionSize;
  }

  @Override
  public int getMaximumEntityId() {
    Connection con = null;
    Statement stmt = null;
    int maxId = 0;
    try {
      con = AidaManager.getConnectionForDatabase(AidaManager.DB_AIDA);
      stmt = con.createStatement();
      String sql = "SELECT max(id) FROM " + ENTITY_IDS;
      ResultSet rs = stmt.executeQuery(sql);

      if (rs.next()) {
        maxId = rs.getInt(1);
      }
      rs.close();
      stmt.close();
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage());
    } finally {
      AidaManager.releaseConnection(con);
    }
    return maxId;
  }

  @Override
  public int getMaximumWordId() {
    Connection con = null;
    Statement stmt = null;
    int maxId = 0;
    try {
      con = AidaManager.getConnectionForDatabase(AidaManager.DB_AIDA);
      stmt = con.createStatement();
      String sql = "SELECT max(id) FROM " + WORD_IDS;
      ResultSet rs = stmt.executeQuery(sql);

      if (rs.next()) {
        maxId = rs.getInt(1);
      }
      rs.close();
      stmt.close();
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage());
    } finally {
      AidaManager.releaseConnection(con);
    }
    return maxId;
  }

  @Override
  public TIntObjectHashMap<Type> getTypesForIds(int[] ids) {
    TIntObjectHashMap<Type> typeNames = new TIntObjectHashMap<Type>();
    if (ids.length == 0) {
      return typeNames;
    }
    Connection con = null;
    Statement stmt = null;
    try {
      con = AidaManager.getConnectionForDatabase(AidaManager.DB_AIDA);
      con.setAutoCommit(false);
      stmt = con.createStatement();
      stmt.setFetchSize(100000);
      String idQuery = YagoUtil.getIdQuery(ids);
      String sql = "SELECT type, knowledgeBase, id FROM " + DataAccessSQL. TYPE_IDS + 
                   " WHERE id IN (" + idQuery + ")";
      ResultSet rs = stmt.executeQuery(sql);
      int read = 0;
      while (rs.next()) {
        String typeName = rs.getString("type");
        String knowledgeBase = rs.getString("knowledgeBase");
        int id = rs.getInt("id");
        typeNames.put(id, new Type(knowledgeBase,typeName));

        if (++read % 1000000 == 0) {
          logger.info("Read " + read + " type names.");
        }
      }
      con.setAutoCommit(true);
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage());
    } finally {
      AidaManager.releaseConnection(con);
    }
    return typeNames;
  }

  @Override
  public TObjectIntHashMap<String> getIdsForTypeNames(Collection<String> typeNames) {
    Connection con = null;
    Statement stmt = null;
    TObjectIntHashMap<String> typesIds = new TObjectIntHashMap<String>();
    try {
      con = AidaManager.getConnectionForDatabase(AidaManager.DB_AIDA);
      con.setAutoCommit(false);
      stmt = con.createStatement();
      stmt.setFetchSize(100000);
      String idQuery = YagoUtil.getPostgresEscapedConcatenatedQuery(typeNames);
      String sql = "SELECT type, id FROM " + DataAccessSQL.TYPE_IDS + 
                   " WHERE type IN (" + idQuery + ")";
      ResultSet rs = stmt.executeQuery(sql);
      int read = 0;
      while (rs.next()) {
        String type = rs.getString("type");
        int id = rs.getInt("id");
        typesIds.put(type, id);

        if (++read % 1000000 == 0) {
          logger.info("Read " + read + " types ids.");
        }
      }
      con.setAutoCommit(true);
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage());
    } finally {
      AidaManager.releaseConnection(con);
    }
    return typesIds;
  }

  @Override
  public TIntObjectHashMap<int[]> getTypesIdsForEntitiesIds(int[] entitiesIds) {
    Integer id = RunningTimer.recordStartTime("DataAccess:getTypesIdsForEntitiesIds");
    TIntObjectHashMap<int[]> typesIds = new TIntObjectHashMap<int[]>();
    if(entitiesIds.length == 0) {
      return typesIds;
    }
    
    for (int entityId : entitiesIds) {
      typesIds.put(entityId, new int[0]);
    }

    Connection con = null;
    Statement statement = null;
    
    
    String entitiesQuery = StringUtils.join(Util.asIntegerList(entitiesIds), ",");
    try {
      con = AidaManager.getConnectionForDatabase(AidaManager.DB_AIDA);
      statement = con.createStatement();
      String sql = "SELECT entity, types FROM " + 
                   DataAccessSQL.ENTITY_TYPES + 
                   " WHERE entity IN (" + entitiesQuery + ")";
      ResultSet rs = statement.executeQuery(sql);
      while (rs.next()) {
        Integer[] types = (Integer[]) rs.getArray("types").getArray();
        int entity = rs.getInt("entity");
        typesIds.put(entity, ArrayUtils.toPrimitive(types));
      }
      rs.close();
      statement.close();
      return typesIds;
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage());
    } finally {
      AidaManager.releaseConnection(con);
      RunningTimer.recordEndTime("DataAccess:getTypesIdsForEntitiesIds", id);
    }
    return typesIds;
  }

  @Override
  public TIntObjectHashMap<int[]> getEntitiesIdsForTypesIds(int[] typesIds) {
    TIntObjectHashMap<int[]> entitiesIds = new TIntObjectHashMap<int[]>();
    for (int typeId : typesIds) {
      entitiesIds.put(typeId, new int[0]);
    }

    Connection con = null;
    Statement statement = null;
    
    
    String typesQuery = StringUtils.join(Util.asIntegerList(typesIds), ",");
    try {
      con = AidaManager.getConnectionForDatabase(AidaManager.DB_AIDA);
      statement = con.createStatement();
      String sql = "SELECT type, entities FROM " + 
                   DataAccessSQL.ENTITY_TYPES + 
                   " WHERE type IN (" + typesQuery + ")";
      ResultSet rs = statement.executeQuery(sql);
      while (rs.next()) {
        Integer[] entities = (Integer[]) rs.getArray("entities").getArray();
        int type = rs.getInt("type");
        entitiesIds.put(type, ArrayUtils.toPrimitive(entities));
      }
      rs.close();
      statement.close();
      return entitiesIds;
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage());
    } finally {
      AidaManager.releaseConnection(con);
    }
    return entitiesIds;
  }

  @Override
  public Map<String, List<String>> getAllEntitiesMetaData(String startingWith){
    Map<String, List<String>> entitiesMetaData = new TreeMap<String, List<String>>();

    Connection con = null;
    Statement statement = null;
    
    try {
      con = AidaManager.getConnectionForDatabase(AidaManager.DB_AIDA);
      statement = con.createStatement();      
      StringBuilder sb = new StringBuilder();
      boolean useFilteringTypes = AidaConfig.getFilteringTypes() != null;
      sb.append("SELECT em.humanreadablererpresentation, em.url ");
      if (useFilteringTypes) {
        sb.append(", ti.type ");
      }
      sb.append("FROM entity_metadata em ");
      if (useFilteringTypes) {
        sb.append(", entity_types et, type_ids ti ");
      }
      sb.append("WHERE em.humanreadablererpresentation ILIKE '"+startingWith+"%' ");
      if (useFilteringTypes) {
        sb.append("AND em.entity = et.entity AND ti.id=ANY(et.types) ");
        sb.append("AND ti.type IN ( ");
        sb.append(
            Arrays.stream(
                AidaConfig.getFilteringTypes())
                  .map(Type::getName)
                  .map(n -> "'" + n + "'")
                  .collect(Collectors.joining(",")));
        sb.deleteCharAt(sb.length() - 1);
        sb.append(") ");
      }      
      sb.append("ORDER BY em.humanreadablererpresentation");
      ResultSet rs = statement.executeQuery(sb.toString());
      while (rs.next()) {
        String humanReadableRepresentation = rs.getString("humanreadablererpresentation");
        String url = rs.getString("url");
        if(entitiesMetaData.containsKey(humanReadableRepresentation)){
          entitiesMetaData.get(humanReadableRepresentation).add(url);
        }else{
          List<String> newList = new ArrayList<String>();
          newList.add(url);
          entitiesMetaData.put(humanReadableRepresentation, newList);
        }
      }
      rs.close();
      statement.close();
      return entitiesMetaData;
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage());
    } finally {
      AidaManager.releaseConnection(con);
    }
    return entitiesMetaData;
  }
  
  @Override
  public TIntObjectHashMap<EntityMetaData> getEntitiesMetaData(int[] entitiesIds) {
    TIntObjectHashMap<EntityMetaData> entitiesMetaData = new TIntObjectHashMap<EntityMetaData>();
    if (entitiesIds == null || entitiesIds.length == 0) {
      return entitiesMetaData;
    }

    Connection con = null;
    Statement statement = null;
    
    String entitiesQuery = StringUtils.join(Util.asIntegerList(entitiesIds), ",");
    try {
      con = AidaManager.getConnectionForDatabase(AidaManager.DB_AIDA);
      statement = con.createStatement();
      String sql = "SELECT entity, humanreadablererpresentation, url, "
          + "knowledgebase, depictionurl, description FROM " + 
                   DataAccessSQL.ENTITY_METADATA + 
                   " WHERE entity IN (" + entitiesQuery + ")";
      logger.debug("Getting metadata for " + entitiesIds.length + " entities");    
      ResultSet rs = statement.executeQuery(sql);
      while (rs.next()) {
        int entity = rs.getInt("entity");
        String humanReadableRepresentation = rs.getString("humanreadablererpresentation");
        String url = rs.getString("url");
        String knowledgebase = rs.getString("knowledgebase");
        String depictionurl = rs.getString("depictionurl");
        String description = rs.getString("description");
        entitiesMetaData.put(entity, new EntityMetaData(entity, 
            humanReadableRepresentation, url, knowledgebase, depictionurl, description));        
      }
      logger.debug("Getting metadata for " + entitiesIds.length + " entities DONE");
      rs.close();
      statement.close();
      return entitiesMetaData;
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage());
    } finally {
      AidaManager.releaseConnection(con);
    }
    return entitiesMetaData;
  }

  @Override
  public TIntDoubleHashMap getEntitiesImportances(int[] entitiesIds) {
    TIntDoubleHashMap entitiesImportances = new TIntDoubleHashMap();
    if (entitiesIds == null || entitiesIds.length == 0) {
      return entitiesImportances;
    }
    
    Connection con = null;
    Statement statement = null;
    
    String entitiesQuery = StringUtils.join(Util.asIntegerList(entitiesIds), ",");
    try {
      con = AidaManager.getConnectionForDatabase(AidaManager.DB_AIDA);
      statement = con.createStatement();
      String sql = "SELECT entity, rank FROM " + 
                   DataAccessSQL.ENTITY_RANK + 
                   " WHERE entity IN (" + entitiesQuery + ")";
      ResultSet rs = statement.executeQuery(sql);
      while (rs.next()) {
        int entity = rs.getInt("entity");
        double rank = rs.getDouble("rank");
        entitiesImportances.put(entity, rank);
      }
      rs.close();
      statement.close();
      return entitiesImportances;
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage());
    } finally {
      AidaManager.releaseConnection(con);
    }
    return entitiesImportances;
  }
 
  @Override
  public Map<String, Double> getKeyphraseSourceWeights(){
    Connection sourceWeightCon = null;
    Statement statement = null;
    Map<String, Double> querySourceWeights = new HashMap<String, Double>();
    try {
      sourceWeightCon = 
          AidaManager.getConnectionForDatabase(AidaManager.DB_AIDA);
      statement = sourceWeightCon.createStatement();
      String sql = "SELECT " + KEYPHRASES_SOURCE + ".source, " + 
        KEYPHRASES_SOURCES_WEIGHTS +".weight FROM " + 
        KEYPHRASES_SOURCE + "," + KEYPHRASES_SOURCES_WEIGHTS +
        " WHERE " + KEYPHRASES_SOURCE + ".source_id = " +
        KEYPHRASES_SOURCES_WEIGHTS + ".source";
      
      ResultSet r = statement.executeQuery(sql);
      while (r.next()) {
        String source = r.getString(1);
        double weight = r.getDouble(2);
        querySourceWeights.put(source, weight);
      }      
      r.close();
      statement.close();
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage());
      e.printStackTrace();
    } finally {
      AidaManager.releaseConnection(sourceWeightCon);
    }
    return querySourceWeights;
  }

  @Override
  public String getConfigurationName() {
    String confName = "";
    Connection confNameConn = null;
    Statement statement = null;
    try {
      confNameConn = AidaManager.getConnectionForDatabase(AidaManager.DB_AIDA);
      statement = confNameConn.createStatement();
      String sql = "SELECT " + METADATA + ".value FROM " + METADATA +
          " WHERE " + METADATA + ".key = 'confName'";

      ResultSet r = statement.executeQuery(sql);
      if (r.next()) {
        confName = r.getString(1);
      }
      r.close();
      statement.close();
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage());
      e.printStackTrace();
    } finally {
      AidaManager.releaseConnection(confNameConn);
    }
    return confName;
  }

  @Override
  public int[] getAllKeywordDocumentFrequencies() {
    Connection con = null;
    Statement stmt = null;
    TIntIntHashMap keywordCounts = new TIntIntHashMap();
    int maxId = -1;
    try {
      logger.info("Reading keyword counts.");
      con = AidaManager.getConnectionForDatabase(AidaManager.DB_AIDA);
      con.setAutoCommit(false);
      stmt = con.createStatement();
      stmt.setFetchSize(1000000);
      String sql = "SELECT keyword, count FROM " + KEYWORD_COUNTS;
      ResultSet rs = stmt.executeQuery(sql);
      int read = 0;
      while (rs.next()) {
        int keyword = rs.getInt("keyword");
        int count = rs.getInt("count");
        keywordCounts.put(keyword, count);
        if (keyword > maxId) {
          maxId = keyword;
        }

        if (++read % 1000000 == 0) {
         logger.debug("Read " + read + " keyword counts.");
        }
      }
      con.setAutoCommit(true);
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage());
    } finally {
      AidaManager.releaseConnection(con);
    }
    
    // Transform hash to int array. This will contain a lot of zeroes as 
    // the keyphrase ids are not part of this (but need to be considered).
    int[] counts = new int[maxId + 1];
    for (TIntIntIterator itr = keywordCounts.iterator(); itr.hasNext(); ) {
      itr.advance();
      int keywordId = itr.key();
      // assert keywordId < counts.length && keywordId > 0 : "Failed for " + keywordId;  // Ids start at 1.
      // actually, keywords should not contain a 0 id, but they do. TODO(mamir,jhoffart).
      assert keywordId < counts.length : "Failed for " + keywordId;  // Ids start at 1.
      counts[keywordId] = itr.value();
    }
    return counts;
  }

  @Override
  public int[] getAllUnitDocumentFrequencies(UnitType unitType) {
    Connection con = null;
    Statement stmt = null;
    TIntIntHashMap unitCounts = new TIntIntHashMap();
    int maxId = -1;
    try {
      logger.info("Reading " + unitType.getUnitName() + " counts.");
      con = AidaManager.getConnectionForDatabase(AidaManager.DB_AIDA);
      con.setAutoCommit(false);
      stmt = con.createStatement();
      stmt.setFetchSize(1000000);
      StringBuilder sql = new StringBuilder();
      sql.append("SELECT ").append(unitType.getUnitName()).append(", count FROM ").append(
        unitType.getUnitCountsTableName());
      ResultSet rs = stmt.executeQuery(sql.toString());
      int read = 0;
      while (rs.next()) {
        int unit = rs.getInt(1);
        int count = rs.getInt(2);
        unitCounts.put(unit, count);
        if (unit > maxId) {
          maxId = unit;
        }
        
        if (++read % 1000000 == 0) {
          logger.debug("Read " + read + " " + unitType.getUnitName() + " counts.");
        }
      }
      con.setAutoCommit(true);
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage());
    } finally {
      AidaManager.releaseConnection(con);
    }

    if (maxId == -1)
      return null;
    
    // Transform hash to int array. This will contain a lot of zeroes as 
    // the keyphrase ids are not part of this (but need to be considered).
    int[] counts = new int[maxId + 1];
    for (TIntIntIterator itr = unitCounts.iterator(); itr.hasNext(); ) {
      itr.advance();
      int unitId = itr.key();
      // assert unitId < counts.length && unitId > 0 : "Failed for " + unitId;  // Ids start at 1.
      // actually, units should not contain a 0 id, but they do. TODO(mamir,jhoffart).
      assert unitId < counts.length : "Failed for " + unitId;  // Ids start at 1.
      counts[unitId] = itr.value();
    }
    return counts;
  }

  @Override
  public TIntIntHashMap getGNDTripleCount(Entities entities) {
    return getEntityImportanceComponentValue(entities,  DatabaseNames.GND_TRIPLES_PER_ENTITY_COUNT_COUNTS_DB_NAME
        /*GNDTriplesCountEntitiesImportanceComponentProvider.getDBName()*/);
  }

  @Override
  public TIntIntHashMap getGNDTitleCount(Entities entities) {
    return getEntityImportanceComponentValue(entities, DatabaseNames.GND_TITLES_PER_ENTITY_COUNTS_DB_NAME
        /*GNDTitleDataEntitiesImportanceComponentProvider.getDBName()*/);
  }

  @Override
  public TIntIntHashMap getYagoOutlinkCount(Entities entities) {
    return getEntityImportanceComponentValue(entities, DatabaseNames.YAGO_PER_ENTITY_OUTLINKS_COUNTS_DB_NAME
        /*YagoOutlinksEntitiesImportanceComponentProvider.getDBName()*/);
  }
  
  private TIntIntHashMap getEntityImportanceComponentValue(Entities entities, String dbTableName) {
    TIntIntHashMap values = new TIntIntHashMap();
    if(entities.size() == 0) {
      return values;
    }

    Connection con = null;
    Statement statement = null;

    String entitiesQuery = StringUtils.join(entities.getUniqueIds(), ",");
    try {
      con = AidaManager.getConnectionForDatabase(AidaManager.DB_AIDA);
      statement = con.createStatement();
      String sql = "SELECT entity, value FROM " +  dbTableName
          + " WHERE entity IN ("
          + entitiesQuery + ")";
      ResultSet rs = statement.executeQuery(sql);
      while (rs.next()) {
        int value = rs.getInt("value");
        int entity = rs.getInt("entity");
        values.put(entity, value);
      }
      rs.close();
      statement.close();
      return values;
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage());
    } finally {
      AidaManager.releaseConnection(con);
    }
    return values;
  }

  @Override
  public Pair<Integer, Integer> getImportanceComponentMinMax(String importanceComponentId) {
    Pair<Integer, Integer> minMax = null;

    Connection con = null;
    Statement statement = null;

    try {
      con = AidaManager.getConnectionForDatabase(AidaManager.DB_AIDA);
      statement = con.createStatement();
      String sql = "SELECT min, max FROM " + DataAccessSQL.IMPORTANCE_COMPONENTS_INFO 
          + " WHERE component='" + importanceComponentId + "'";
      ResultSet rs = statement.executeQuery(sql);
      while (rs.next()) {
        int min = rs.getInt("min");
        int max = rs.getInt("max");
        minMax = new Pair<Integer, Integer>(min, max);
      }
      rs.close();
      statement.close();
      return minMax;
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage());
    } finally {
      AidaManager.releaseConnection(con);
    }
    return minMax;
  }

  @Override
  public Map<String, int[]> getDictionary() {
    Map<String, int[]> candidates = new HashMap<String, int[]>();
    Connection con = null;
    Statement statement = null;
    Map<String, TIntList> tempCandidates = new HashMap<String, TIntList>(DataAccess.getCollectionSize(), 1.0f);
    try {
      con = 
          AidaManager.getConnectionForDatabase(AidaManager.DB_AIDA);
      con.setAutoCommit(false);
      statement = con.createStatement();
      statement.setFetchSize(100000);
      String sql = "SELECT mention, entity FROM " + DICTIONARY;
      ResultSet r = statement.executeQuery(sql);
      int read = 0;
      while (r.next()) {
        if (++read % 1000000 == 0) {
          logger.info("Read " + read + " dictionary entries.");
        }
        String mention = r.getString(1);
        int entity = r.getInt(2);
        TIntList entities = tempCandidates.get(mention);
        if (entities == null) {
          entities = new TIntArrayList();
          tempCandidates.put(mention, entities);
        }
        entities.add(entity);
      }
      r.close();
      statement.close();
      AidaManager.releaseConnection(con);

      // Transform to arrays.
      candidates = new HashMap<String, int[]>(tempCandidates.size(), 1.0f);
      for (Entry<String, TIntList> entry: tempCandidates.entrySet()) {
        candidates.put(entry.getKey(), entry.getValue().toArray());
      }
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage());
      e.printStackTrace();
    }
    return candidates;
  }
}
