package mpi.aida.access;

import gnu.trove.impl.Constants;
import gnu.trove.iterator.TIntIntIterator;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.hash.TIntHashSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mpi.aida.AidaManager;
import mpi.aida.access.compiledprotos.BigramCountsBigram;
import mpi.aida.access.compiledprotos.DictionaryMention;
import mpi.aida.access.compiledprotos.EntityBigramsEntity;
import mpi.aida.access.compiledprotos.EntityIdsEntityKnowledgebase;
import mpi.aida.access.compiledprotos.EntityIdsId;
import mpi.aida.access.compiledprotos.EntityInlinksEntity;
import mpi.aida.access.compiledprotos.EntityKeyphrasesEntity;
import mpi.aida.access.compiledprotos.EntityKeywordsEntity;
import mpi.aida.access.compiledprotos.EntityMetadataEntity;
import mpi.aida.access.compiledprotos.EntityRankEntity;
import mpi.aida.access.compiledprotos.EntityTypesEntity;
import mpi.aida.access.compiledprotos.KeyphraseSourcesSourceId;
import mpi.aida.access.compiledprotos.KeyphraseTokensKeyphrase;
import mpi.aida.access.compiledprotos.KeyphrasesSourcesWeightsSource;
import mpi.aida.access.compiledprotos.KeywordCountsKeyword;
import mpi.aida.access.compiledprotos.MetaKey;
import mpi.aida.access.compiledprotos.TypeIdsId;
import mpi.aida.access.compiledprotos.WordExpansionWord;
import mpi.aida.access.compiledprotos.WordIdsWord;
import mpi.aida.data.Entities;
import mpi.aida.data.Entity;
import mpi.aida.data.EntityMetaData;
import mpi.aida.data.KBIdentifiedEntity;
import mpi.aida.data.Keyphrases;
import mpi.aida.data.Type;
import mpi.aida.graph.similarity.UnitType;
import mpi.aida.protobufdmap.ProtobufDMapNotLoadedException;
import mpi.aida.util.CollectionUtils;
import mpi.aida.util.Counter;
import mpi.aida.util.YagoUtil;
import mpi.aida.util.timing.RunningTimer;
import mpi.tools.javatools.datatypes.Pair;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.jhoff.dmap.DMap;

public class DataAccessDMap implements DataAccessInterface {
  private static final Logger logger =
    LoggerFactory.getLogger(DataAccessDMap.class);

  // the load factor for all trove maps
  private static final float troveLoadFactor = Constants.DEFAULT_LOAD_FACTOR;
  
  public DataAccessDMap() {
    DataAccessDMapHandler.singleton();
  }

  public DataAccess.type getAccessType() {
    return DataAccess.type.dmap;
  }

  public TIntDoubleHashMap getEntityPriors(String mention) {
    TIntDoubleHashMap entityPriors;
    try {
      mention = AidaManager.conflateToken(mention);
      DMap dMap = DataAccessDMapHandler.singleton().getDMap(DatabaseDMap.DICTIONARY_MENTION);
      byte[] resultBytes = dMap.get(DictionaryMention.Key.newBuilder().setMention(mention).build().toByteArray());
      if (resultBytes != null) {
        DictionaryMention.Values result = DictionaryMention.Values.parseFrom(resultBytes);
        entityPriors = new TIntDoubleHashMap(getCapacity(result.getValuesCount()), troveLoadFactor);
        for (DictionaryMention.Values.Value value : result.getValuesList()) {
          entityPriors.put(value.getEntity(), value.getPrior());
        }
      } else entityPriors = new TIntDoubleHashMap();
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage());
      entityPriors = new TIntDoubleHashMap();
    }
    return entityPriors;
  }

  public TIntObjectHashMap<KBIdentifiedEntity> getKnowlegebaseEntitiesForInternalIds(int[] ids) {
    TIntObjectHashMap<KBIdentifiedEntity> entityIds = new TIntObjectHashMap<>(getCapacity(ids.length), troveLoadFactor);
    if (ids.length == 0) {
      return entityIds;
    }
    try {
      DMap dMap = DataAccessDMapHandler.singleton().getDMap(DatabaseDMap.ENTITY_IDS_ID);
      EntityIdsId.Key.Builder keyBuilder = EntityIdsId.Key.newBuilder();
      for (int id : ids) {
        byte[] resultBytes = dMap.get(keyBuilder.setId(id).build().toByteArray());
        if (resultBytes == null) continue;
        EntityIdsId.Values result = EntityIdsId.Values.parseFrom(resultBytes);
        for (EntityIdsId.Values.Value value : result.getValuesList()) {
          entityIds.put(id, new KBIdentifiedEntity(value.getEntity(), value.getKnowledgebase()));
        }
      }
    } catch (Exception e) {
      logger.error(e.getClass().getName() + ": " + e.getLocalizedMessage());
      e.printStackTrace();
    }
    return entityIds;
  }

  public TObjectIntHashMap<String> getIdsForWords(Collection<String> keywords) {
    TObjectIntHashMap<String> wordIds = new TObjectIntHashMap<>(getCapacity(keywords.size()), troveLoadFactor);
    if (keywords.isEmpty()) {
      return wordIds;
    }
    try {
      DMap dMap = DataAccessDMapHandler.singleton().getDMap(DatabaseDMap.WORD_IDS_WORD);
      int read = 0;
      for (String word : keywords) {
        byte[] resultBytes = dMap.get(WordIdsWord.Key.newBuilder().setWord(word).build().toByteArray());
        if (resultBytes == null) continue;
        WordIdsWord.Values result = WordIdsWord.Values.parseFrom(resultBytes);
        for (WordIdsWord.Values.Value value : result.getValuesList()) {
          wordIds.put(word, value.getId());

          if (++read % 1000000 == 0) {
            logger.info("Read " + read + " word ids.");
          }
        }
      }
    } catch (Exception e) {
      logger.error(e.getClass().getName() + ": " + e.getLocalizedMessage());
    }
    return wordIds;
  }

  public String getConfigurationName() {
    String confName = "";
    try {
      DMap dMap = DataAccessDMapHandler.singleton().getDMap(DatabaseDMap.METADATA_KEY);
      byte[] resultBytes = dMap.get(MetaKey.Key.newBuilder().setKey("confName").build().toByteArray());
      if (resultBytes != null) {
        MetaKey.Values result = MetaKey.Values.parseFrom(resultBytes);
        for (MetaKey.Values.Value value : result.getValuesList()) {
          confName = value.getValue();
          break;
        }
      }
    } catch (Exception e) {
      logger.error(e.getClass().getName() + ": " + e.getLocalizedMessage());
    }
    return confName;
  }

  public int getCollectionSize() {
    int collectionSize = 0;
    try {
      DMap dMap = DataAccessDMapHandler.singleton().getDMap(DatabaseDMap.METADATA_KEY);
      byte[] resultBytes = dMap.get(MetaKey.Key.newBuilder().setKey("collection_size").build().toByteArray());
      if (resultBytes != null) {
        MetaKey.Values result = MetaKey.Values.parseFrom(resultBytes);
        for (MetaKey.Values.Value value : result.getValuesList()) {
          String sizeString = value.getValue();
          collectionSize = Integer.parseInt(sizeString);
          break;
        }
      }
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage());
      logger.error("You might have an outdated entity repository, please " +
        "download the latest version from the AIDA website. Also check " +
        "above for other error messages, maybe the DMap files are not  " +
        "Postgres database is not working properly.");
      throw new IllegalStateException(
        "You might have an outdated entity repository, please " +
          "download the latest version from the AIDA website. Also check " +
          "above for other error messages, maybe the connection to the " +
          "Postgres database is not working properly.");
    }
    return collectionSize;
  }

  public Keyphrases getEntityKeyphrases(Entities entities, Map<String, Double> keyphraseSourceWeights,
                                        double minKeyphraseWeight, int maxEntityKeyphraseCount) {
    boolean useSources = keyphraseSourceWeights != null && !keyphraseSourceWeights.isEmpty();
    
    // Create and fill return object with empty maps.
    Keyphrases keyphrases = new Keyphrases();

    int entitiesSize = entities == null ? 0 : entities.size();
    
    TIntObjectHashMap<TIntIntHashMap> entity2keyphrase2source = null;
    TObjectIntHashMap<String> keyphraseSrcName2Id = null;
    TIntDoubleHashMap keyphraseSourceId2weight = null;
    if (useSources) {
      entity2keyphrase2source = new TIntObjectHashMap<>(getCapacity(entitiesSize), troveLoadFactor);
      keyphraseSrcName2Id = new TObjectIntHashMap<>();
      keyphraseSourceId2weight = new TIntDoubleHashMap();
      keyphrases.setEntityKeyphraseSources(entity2keyphrase2source);
      keyphrases.setKeyphraseSource2id(keyphraseSrcName2Id);
      keyphrases.setKeyphraseSourceWeights(keyphraseSourceId2weight);
    }

    
    TIntObjectHashMap<int[]> entityKeyphrases = new TIntObjectHashMap<>(getCapacity(entitiesSize), troveLoadFactor);
    TIntObjectHashMap<TIntDoubleHashMap> entity2keyphrase2mi = new TIntObjectHashMap<>(getCapacity(entitiesSize), troveLoadFactor);
    TIntObjectHashMap<TIntDoubleHashMap> entity2keyword2mi = new TIntObjectHashMap<>(getCapacity(entitiesSize), troveLoadFactor);
    
    // Fill the keyphrases object with all data.
    // we do it here because if the entities are empty
    // we return it right away and we don't want to have nulls in there
    keyphrases.setEntityKeyphrases(entityKeyphrases);
    keyphrases.setEntityKeyphraseWeights(entity2keyphrase2mi);
    keyphrases.setEntityKeywordWeights(entity2keyword2mi);
    
    if (entitiesSize == 0) {
      keyphrases.setKeyphraseTokens(new TIntObjectHashMap<int[]>());
      return keyphrases;
    }

    
    try {
      EntityKeyphrasesEntity.Key.Builder entityKeyphrasesEntityKeyBuilder = EntityKeyphrasesEntity.Key.newBuilder();
      DMap entityKeyphrasesEntityDMap = DataAccessDMapHandler.singleton().getDMap(DatabaseDMap.ENTITY_KEYPHRASES_ENTITY);
      EntityKeywordsEntity.Key.Builder entityKeywordsEntityKeyBuilder = EntityKeywordsEntity.Key.newBuilder();
      DMap entityKeywordsEntityDMap = DataAccessDMapHandler.singleton().getDMap(DatabaseDMap.ENTITY_KEYWORDS_ENTITY);
      final KeyphraseTokensKeyphrase.Key.Builder keyphraseTokensKeyphraseKeyBuilder = KeyphraseTokensKeyphrase.Key.newBuilder();
      final DMap keyphraseTokensKeyphraseDMap = DataAccessDMapHandler.singleton().getDMap(DatabaseDMap.KEYPHRASE_TOKENS_KEYPHRASE);

      int keyphraseKeywordTimer = RunningTimer.recordStartTime("RequestKeyphrasesAndKeywords");
      int keyphrasesCountEstimate = 0;
      for (Entity entity : entities) {
        int keyphraseTimer = RunningTimer.recordStartTime("RequestKeyphrases");
        try {
          byte[] resultBytes = entityKeyphrasesEntityDMap.get(entityKeyphrasesEntityKeyBuilder.setEntity(entity.getId()).build().toByteArray());
          if (resultBytes != null) {
            EntityKeyphrasesEntity.Values result = EntityKeyphrasesEntity.Values.parseFrom(resultBytes);
            keyphrasesCountEstimate += result.getValuesCount();
            int[] curEntityKeyphrases = new int[result.getValuesCount()];
            TIntDoubleHashMap curEntity2keyphrase2mi = new TIntDoubleHashMap(getCapacity(result.getValuesCount()), troveLoadFactor);
            TIntIntHashMap curEntity2keyphrase2source = null;
            if (useSources) curEntity2keyphrase2source = new TIntIntHashMap(getCapacity(result.getValuesCount()), troveLoadFactor);

            for (int i = 0; i < result.getValuesCount(); i++) {
              int keyphrase = result.getValues(i).getKeyphrase();
              curEntityKeyphrases[i] = keyphrase;
              curEntity2keyphrase2mi.put(keyphrase, result.getValues(i).getWeight());

              if (curEntity2keyphrase2source != null) { // don't know if its better to use 'useSources'
                curEntity2keyphrase2source.put(keyphrase, result.getValues(i).getSource());
              }
            }
            entityKeyphrases.put(entity.getId(), curEntityKeyphrases);
            entity2keyphrase2mi.put(entity.getId(), curEntity2keyphrase2mi);
            if (curEntity2keyphrase2source != null)   // don't know if its better to use 'useSources'
              entity2keyphrase2source.put(entity.getId(), curEntity2keyphrase2source);
          }
        } catch (Exception e) {
          logger.error("Error loading keyphrases for entity " + entity + ": " + e.getLocalizedMessage());
        }
        RunningTimer.recordEndTime("RequestKeyphrases", keyphraseTimer);

        int keywordTimer = RunningTimer.recordStartTime("RequestKeywords");
        try {
          byte[] resultBytes = entityKeywordsEntityDMap.get(entityKeywordsEntityKeyBuilder.setEntity(entity.getId()).build().toByteArray());
          if (resultBytes != null) {
            EntityKeywordsEntity.Values result = EntityKeywordsEntity.Values.parseFrom(resultBytes);

            TIntDoubleHashMap curEntity2keyword2mi = new TIntDoubleHashMap(getCapacity(result.getValuesCount()), troveLoadFactor);

            for (EntityKeywordsEntity.Values.Value value : result.getValuesList()) {
              curEntity2keyword2mi.put(value.getKeyword(), value.getWeight());
            }

            entity2keyword2mi.put(entity.getId(), curEntity2keyword2mi);
          }
        } catch (Exception e) {
          logger.error("Error loading keywords for entity " + entity + ": " + e.getLocalizedMessage());
        }
        RunningTimer.recordEndTime("RequestKeywords", keywordTimer);
      }
      RunningTimer.recordEndTime("RequestKeyphrasesAndKeywords", keyphraseKeywordTimer);
      
      TIntObjectHashMap<int[]> keyphraseTokens = new TIntObjectHashMap<>(getCapacity(keyphrasesCountEstimate), troveLoadFactor);
      int tokenTimer = RunningTimer.recordStartTime("RequestTokens");
      for (int[] keyphrasesPerEntity : entityKeyphrases.valueCollection()) {
        for (int keyphrase : keyphrasesPerEntity) {
          if (keyphraseTokens.containsKey(keyphrase)) continue;
          try {
            byte[] resultBytes = keyphraseTokensKeyphraseDMap.get(
              keyphraseTokensKeyphraseKeyBuilder.setKeyphrase(keyphrase).build().toByteArray());
            if (resultBytes != null) {
              KeyphraseTokensKeyphrase.Values result = KeyphraseTokensKeyphrase.Values.parseFrom(resultBytes);

              int[] curKeyphraseTokens = new int[result.getValuesCount()];
              for (int i = 0; i < result.getValuesCount(); i++) {
                curKeyphraseTokens[i] = result.getValues(i).getToken();
              }

              keyphraseTokens.put(keyphrase, curKeyphraseTokens);
            }
          } catch (Exception e) {
            logger.error("Error loading tokens for keyphrase " + keyphrase + ": " + e.getLocalizedMessage());
          }
        }
      }
      RunningTimer.recordEndTime("RequestTokens", tokenTimer);
      keyphrases.setKeyphraseTokens(keyphraseTokens);
      
      if (keyphraseSrcName2Id != null) {
        DMap.EntryIterator entryIterator = DataAccessDMapHandler.singleton()
          .getDMap(DatabaseDMap.KEYPHRASES_SOURCE_SOURCE_ID).entryIterator();
        while (entryIterator.hasNext()) {
          DMap.Entry entry = entryIterator.next();
          int sourceId = KeyphraseSourcesSourceId.Key.parseFrom(entry.getKey()).getSourceId();
          String sourceName = KeyphraseSourcesSourceId.Values.parseFrom(entry.getValue()).getValues(0).getSource();
          keyphraseSrcName2Id.put(sourceName, sourceId);
        }
      }
      
      if (keyphraseSourceId2weight != null) {
        DMap.EntryIterator entryIterator = DataAccessDMapHandler.singleton()
          .getDMap(DatabaseDMap.KEYPHRASES_SOURCES_WEIGHT_SOURCE).entryIterator();
        while (entryIterator.hasNext()) {
          DMap.Entry entry = entryIterator.next();
          int sourceId = KeyphrasesSourcesWeightsSource.Key.parseFrom(entry.getKey()).getSource();
          double sourceWeight = KeyphrasesSourcesWeightsSource.Values.parseFrom(entry.getValue()).getValues(0).getWeight();
          keyphraseSourceId2weight.put(sourceId, sourceWeight);
        }
      }
    } catch (Exception e) {
      keyphrases.setKeyphraseTokens(new TIntObjectHashMap<int[]>());
      logger.error("Error loading Entity Keyphrases");
    }
    return keyphrases;
  }

  public Map<String, Entities> getEntitiesForMentions(Collection<String> mentions, double maxEntityRank,
                                                      int topByPrior) {
    Map<String, Entities> candidates = new HashMap<>(mentions.size(), 1.0f);
    if (mentions.isEmpty()) {
      return candidates;
    }
    List<String> queryMentions = new ArrayList<>(mentions.size());
    for (String m : mentions) {
      queryMentions.add(AidaManager.conflateToken(m));
      // Add an emtpy candidate set as default.
      candidates.put(m, new Entities());
    }
    Map<String, Map<Integer, Double>> queryMentionCandidates = new HashMap<>(queryMentions.size(), 1.0f);
    try {
      DMap dMap = DataAccessDMapHandler.singleton().getDMap(DatabaseDMap.DICTIONARY_MENTION);
      for (String queryMention : queryMentions) {
        byte[] resultBytes = dMap.get(DictionaryMention.Key.newBuilder().setMention(queryMention).build().toByteArray());
        if (resultBytes != null) {
          DictionaryMention.Values result = DictionaryMention.Values.parseFrom(resultBytes);
          Map<Integer, Double> entities = queryMentionCandidates.get(queryMention);
          if (entities == null)
            queryMentionCandidates.put(queryMention, (entities = new HashMap<>(result.getValuesCount(), 1.0f)));
          for (DictionaryMention.Values.Value value : result.getValuesList()) {
            entities.put(value.getEntity(), value.getPrior());
          }
        }
      }
      // Get the candidates for the original Strings.
      for (Map.Entry<String, Entities> entry: candidates.entrySet()) {
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
          TIntObjectHashMap<KBIdentifiedEntity> yagoEntityIds = getKnowlegebaseEntitiesForInternalIds(
            ArrayUtils.toPrimitive(ids));
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
    return candidates;
  }

  public TIntObjectHashMap<int[]> getInlinkNeighbors(Entities entities) {
    if (entities.isEmpty()) {
      return new TIntObjectHashMap<>();
    }

    TIntObjectHashMap<int[]> neighbors = new TIntObjectHashMap<>(getCapacity(entities.size()), troveLoadFactor);
    try {
      DMap dMap = DataAccessDMapHandler.singleton().getDMap(DatabaseDMap.ENTITY_INLINKS_ENTITY);
      for (Entity entity : entities) {
        int entityId = entity.getId();
        byte[] resultBytes = dMap.get(EntityInlinksEntity.Key.newBuilder().setEntity(entityId).build().toByteArray());
        if (resultBytes == null)
          neighbors.put(entityId, new int[0]);
        else {
          EntityInlinksEntity.Values result = EntityInlinksEntity.Values.parseFrom(resultBytes);
          int[] curNeighbors = new int[result.getValuesCount()];
          for (int i = 0; i < result.getValuesCount(); i++) {
            curNeighbors[i] = result.getValues(i).getInlink();
          }
          neighbors.put(entityId, curNeighbors);
        }
      }
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage());
    }
    return neighbors;
  }

  public TObjectIntHashMap<KBIdentifiedEntity> getInternalIdsForKBEntities(Collection<KBIdentifiedEntity> kbEntities) {
    if(kbEntities.isEmpty()) {
      return new TObjectIntHashMap<>();
    }

    TObjectIntHashMap<KBIdentifiedEntity> entities = new TObjectIntHashMap<>(getCapacity(kbEntities.size()), troveLoadFactor);
    try {
      DMap dMap = DataAccessDMapHandler.singleton().getDMap(DatabaseDMap.ENTITY_IDS_ENTITY_KNOWLEDGEBASE);
      for (KBIdentifiedEntity kbEntity : kbEntities) {
        byte[] resultBytes = dMap.get(EntityIdsEntityKnowledgebase.Key.newBuilder()
          .setEntity(kbEntity.getIdentifier()).setKnowledgebase(kbEntity.getKnowledgebase()).build().toByteArray());
        if (resultBytes != null) {
          EntityIdsEntityKnowledgebase.Values result = EntityIdsEntityKnowledgebase.Values.parseFrom(resultBytes);
          if (result.getValuesCount() > 0)
            entities.put(kbEntity, result.getValues(0).getId());
        }
      }
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage());
    }
    return entities;
  }

  public TIntObjectHashMap<EntityMetaData> getEntitiesMetaData(int[] entitiesIds) {
    if (entitiesIds == null || entitiesIds.length == 0) {
      return new TIntObjectHashMap<>();
    }

    TIntObjectHashMap<EntityMetaData> entitiesMetaData = new TIntObjectHashMap<>(getCapacity(entitiesIds.length), troveLoadFactor);
    try {
      DMap dMap = DataAccessDMapHandler.singleton().getDMap(DatabaseDMap.ENTITY_METADATA_ENTITY);
      for (int entitiesId : entitiesIds) {
        byte[] resultBytes = dMap.get(EntityMetadataEntity.Key.newBuilder().setEntity(entitiesId).build().toByteArray());
        if (resultBytes != null) {
          EntityMetadataEntity.Values result = EntityMetadataEntity.Values.parseFrom(resultBytes);
          if (result.getValuesCount() > 0) {
            EntityMetadataEntity.Values.Value value = result.getValues(0);
            entitiesMetaData.put(entitiesId, new EntityMetaData(entitiesId,
              value.getHumanreadablererpresentation(), 
              value.getUrl(),
              value.getKnowledgebase(), 
              value.hasDepictionurl() ? value.getDepictionurl() : null, value.getDescription()));
          }
        }
      }
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage());
    }
    return entitiesMetaData;
  }

  public TIntObjectHashMap<int[]> getTypesIdsForEntitiesIds(int[] entitiesIds) {
    if(entitiesIds.length == 0) {
      return new TIntObjectHashMap<>();
    }

    TIntObjectHashMap<int[]> typesIds = new TIntObjectHashMap<>(getCapacity(entitiesIds.length), troveLoadFactor);
    try {
      DMap dMap = DataAccessDMapHandler.singleton().getDMap(DatabaseDMap.ENTITY_TYPES_ENTITY);
      for (int entityId : entitiesIds) {
        byte[] resultBytes = dMap.get(EntityTypesEntity.Key.newBuilder().setEntity(entityId).build().toByteArray());
        if (resultBytes == null)
          typesIds.put(entityId, new int[0]);
        else {
          EntityTypesEntity.Values result = EntityTypesEntity.Values.parseFrom(resultBytes);
          int[] curTypesIds = new int[result.getValuesCount()];
          for (int i = 0; i < result.getValuesCount(); i++) {
            curTypesIds[i] = result.getValues(i).getType();
          }
          typesIds.put(entityId, curTypesIds);
        }
      }
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage());
    }
    return typesIds;
  }

  public TIntObjectHashMap<Type> getTypesForIds(int[] ids) {
    if (ids.length == 0)
      return new TIntObjectHashMap<>();
    
    TIntObjectHashMap<Type> typeNames = new TIntObjectHashMap<>(getCapacity(ids.length), troveLoadFactor);
    try {
      DMap dMap = DataAccessDMapHandler.singleton().getDMap(DatabaseDMap.TYPE_IDS_ID);
      for (int id : ids) {
        byte[] resultBytes = dMap.get(TypeIdsId.Key.newBuilder().setId(id).build().toByteArray());
        if (resultBytes != null) {
          TypeIdsId.Values result = TypeIdsId.Values.parseFrom(resultBytes);
          if (result.getValuesCount() > 0) {
            TypeIdsId.Values.Value curValue = result.getValues(0);
            typeNames.put(id, new Type(curValue.getKnowledgebase(), curValue.getType()));
          }
        }
      }
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage());
    }
    return typeNames;
  }

  public TIntDoubleHashMap getEntitiesImportances(int[] entitiesIds) {
    if (entitiesIds == null || entitiesIds.length == 0) {
      return new TIntDoubleHashMap();
    }
    
    TIntDoubleHashMap entitiesImportances = new TIntDoubleHashMap(getCapacity(entitiesIds.length), troveLoadFactor);
    try {
      DMap dMap = DataAccessDMapHandler.singleton().getDMap(DatabaseDMap.ENTITY_RANK_ENTITY);
      for (int entitiesId : entitiesIds) {
        byte[] resultBytes = dMap.get(EntityRankEntity.Key.newBuilder().setEntity(entitiesId).build().toByteArray());
        if (resultBytes != null) {
          EntityRankEntity.Values result = EntityRankEntity.Values.parseFrom(resultBytes);
          if (result.getValuesCount() > 0) {
            entitiesImportances.put(entitiesId, result.getValues(0).getRank());
          }
        }
      }
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage());
    }
    return entitiesImportances;
  }
  
  public int[] getAllWordExpansions() {
    TIntIntHashMap wordExpansions = new TIntIntHashMap();
    int maxId = -1;
    try {
      logger.info("Reading word expansions.");
      DMap dMap = DataAccessDMapHandler.singleton().getDMap(DatabaseDMap.WORD_EXPANSION_WORD);
      DMap.EntryIterator iterator = dMap.entryIterator();
      int read = 0;
      int word, expansion;
      while (iterator.hasNext()) {
        DMap.Entry entry = iterator.next();
        word = WordExpansionWord.Key.parseFrom(entry.getKey()).getWord();
        expansion = WordExpansionWord.Values.parseFrom(entry.getValue()).getValues(0).getExpansion();
        wordExpansions.put(word, expansion);
        if (word > maxId) {
          maxId = word;
        }

        if (++read % 1000000 == 0) {
          logger.debug("Read " + read + " word expansions.");
        }
      }
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage());
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

  public int[] getAllWordContractions() {
    TIntIntHashMap wordContraction = new TIntIntHashMap();
    int maxId = -1;
    try {
      logger.info("Reading word expansions.");
      DMap dMap = DataAccessDMapHandler.singleton().getDMap(DatabaseDMap.WORD_EXPANSION_WORD);
      DMap.EntryIterator iterator = dMap.entryIterator();
      int read = 0;
      int word, expansion;
      while (iterator.hasNext()) {
        DMap.Entry entry = iterator.next();
        word = WordExpansionWord.Key.parseFrom(entry.getKey()).getWord();
        expansion = WordExpansionWord.Values.parseFrom(entry.getValue()).getValues(0).getExpansion();
        wordContraction.put(expansion, word);
        if (expansion > maxId) {
          maxId = expansion;
        }

        if (++read % 1000000 == 0) {
          logger.debug("Read " + read + " word expansions.");
        }
      }
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage());
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
  
  public int[] getAllUnitDocumentFrequencies(UnitType unitType) {
    TIntIntHashMap unitCounts = new TIntIntHashMap();
    int maxId = -1;
    try {
      logger.info("Reading " + unitType.getUnitName() + " counts.");
      DMap dMap = DataAccessDMapHandler.singleton().getDMap(unitType.getUnitCountsDMap());
      DMap.EntryIterator iterator = dMap.entryIterator();
      int read = 0;
      int unit = 0, count = 0;
      while (iterator.hasNext()) {
        DMap.Entry entry = iterator.next();
        if (unitType == UnitType.KEYWORD) {
          unit = KeywordCountsKeyword.Key.parseFrom(entry.getKey()).getKeyword();
          count = KeywordCountsKeyword.Values.parseFrom(entry.getValue()).getValues(0).getCount();
        } else if (unitType == UnitType.BIGRAM) {
          unit = BigramCountsBigram.Key.parseFrom(entry.getKey()).getBigram();
          count = BigramCountsBigram.Values.parseFrom(entry.getValue()).getValues(0).getCount();
        }
        unitCounts.put(unit, count);
        if (unit > maxId) {
          maxId = unit;
        }

        if (++read % 1000000 == 0) {
          logger.debug("Read " + read + " " + unitType.getUnitName() + " counts.");
        }
      }
    } catch (ProtobufDMapNotLoadedException e) {
      return null;
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage());
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

  public TIntObjectHashMap<int[]> getAllKeyphraseTokens() {
    TIntObjectHashMap<int[]> keyphraseTokens = new TIntObjectHashMap<int[]>();
    try {
      DMap dMap = DataAccessDMapHandler.singleton().getDMap(DatabaseDMap.KEYPHRASE_TOKENS_KEYPHRASE);
      DMap.EntryIterator iterator = dMap.entryIterator();
      int read = 0;
      while(iterator.hasNext()) {
        DMap.Entry entry = iterator.next();
        int keyphrse = KeyphraseTokensKeyphrase.Key.parseFrom(entry.getKey()).getKeyphrase();
        KeyphraseTokensKeyphrase.Values values = KeyphraseTokensKeyphrase.Values.parseFrom(entry.getValue());
        int[] tokens = new int[values.getValuesCount()];
        for (int i = 0; i < values.getValuesCount(); i++) {
          tokens[i] = values.getValues(i).getToken();
          if (++read % 1000000 == 0) {
            logger.debug("Read " + read + " keyphrase tokens.");
          }
        }
        keyphraseTokens.put(keyphrse, tokens);
      }
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage());
    }

    return keyphraseTokens;
  }

  public TObjectIntHashMap<String> getAllKeyphraseSources() {
    TObjectIntHashMap<String> keyphraseSources = new TObjectIntHashMap<String>();
    try {
      DMap.EntryIterator entryIterator = DataAccessDMapHandler.singleton()
        .getDMap(DatabaseDMap.KEYPHRASES_SOURCE_SOURCE_ID).entryIterator();
      while (entryIterator.hasNext()) {
        DMap.Entry entry = entryIterator.next();
        int sourceId = KeyphraseSourcesSourceId.Key.parseFrom(entry.getKey()).getSourceId();
        String sourceName = KeyphraseSourcesSourceId.Values.parseFrom(entry.getValue()).getValues(0).getSource();
        keyphraseSources.put(sourceName, sourceId);
      }
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage());
    }
    return keyphraseSources;
  }

  public TIntObjectHashMap<TIntIntHashMap> getEntityUnitIntersectionCount(Entities entities, UnitType unitType) {
    TIntObjectHashMap<TIntIntHashMap> entityKeywordIC = new TIntObjectHashMap<TIntIntHashMap>();

    if (entities == null || entities.size() == 0) {
      return entityKeywordIC;
    }
    
    try {
      DMap dMap = DataAccessDMapHandler.singleton().getDMap(unitType.getEntityUnitCooccurrenceDMap());
      if (unitType == UnitType.KEYWORD) {
        EntityKeywordsEntity.Key.Builder keyBuilder = EntityKeywordsEntity.Key.newBuilder();
        for (Entity entity : entities) {
          byte[] resultBytes = dMap.get(keyBuilder.setEntity(entity.getId()).build().toByteArray());
          if (resultBytes != null) {
            EntityKeywordsEntity.Values values = EntityKeywordsEntity.Values.parseFrom(resultBytes);
            TIntIntHashMap curEntityUnitCounts = new TIntIntHashMap(getCapacity(values.getValuesCount()), troveLoadFactor);
            entityKeywordIC.put(entity.getId(), curEntityUnitCounts);
            for (int i = 0; i < values.getValuesCount(); i++) {
              EntityKeywordsEntity.Values.Value curValue = values.getValues(i);
              curEntityUnitCounts.put(curValue.getKeyword(), curValue.getCount());
            }
          } else {
            entityKeywordIC.put(entity.getId(), new TIntIntHashMap());
          }
        }
      } else if (unitType == UnitType.BIGRAM) {
        for (Entity entity : entities) {
          EntityBigramsEntity.Key.Builder keyBuilder = EntityBigramsEntity.Key.newBuilder();
          byte[] resultBytes = dMap.get(keyBuilder.setEntity(entity.getId()).build().toByteArray());
          if (resultBytes != null) {
            EntityBigramsEntity.Values values = EntityBigramsEntity.Values.parseFrom(resultBytes);
            TIntIntHashMap curEntityUnitCounts = new TIntIntHashMap(getCapacity(values.getValuesCount()), troveLoadFactor);
            entityKeywordIC.put(entity.getId(), curEntityUnitCounts);
            for (int i = 0; i < values.getValuesCount(); i++) {
              EntityBigramsEntity.Values.Value curValue = values.getValues(i);
              curEntityUnitCounts.put(curValue.getBigram(), curValue.getCount());
            }
          } else {
            entityKeywordIC.put(entity.getId(), new TIntIntHashMap());
          }
        }
      }
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage());
    }
    
    return entityKeywordIC;
  }
  
  public Map<String, int[]> getDictionary() {
    Map<String, int[]> candidates = new HashMap<>();
    try {
      DMap.EntryIterator entryIterator = DataAccessDMapHandler.singleton()
        .getDMap(DatabaseDMap.DICTIONARY_MENTION).entryIterator();
      int read = 0;
      while (entryIterator.hasNext()) {
        DMap.Entry entry = entryIterator.next();
        String mention = DictionaryMention.Key.parseFrom(entry.getKey()).getMention();
        DictionaryMention.Values values = DictionaryMention.Values.parseFrom(entry.getValue());
        int[] entities = new int[values.getValuesCount()];
        for (int i = 0; i < values.getValuesCount(); i++) {
          entities[i] = values.getValues(i).getEntity();
        }
        candidates.put(mention, entities);
        if (++read % 1000000 == 0) {
          logger.info("Read " + read + " dictionary entries.");
        }
      }
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage());
      e.printStackTrace();
    }
    return candidates;
  }

  public static int getCapacity(int numElements) {
    return (int) (numElements/troveLoadFactor);
  }

  // =========================
  // === DEPRECATED METHODS
  // =
  public Entities getEntitiesForMentionByFuzzyMatching(String mention, double minSimilarity) {
    throw new NotImplementedException("getEntitiesForMentionByFuzzyMatching()  is not implemented in DataAccessDMap.");    
  }

  public void getEntityKeyphraseTokens(Entities entities, TIntObjectHashMap<int[]> entityKeyphrases,
                                       TIntObjectHashMap<int[]> keyphraseTokens) {
    throw new NotImplementedException("getEntityKeyphraseTokens() is not implemented in DataAccessDMap.");
  }

  public TIntIntHashMap getEntitySuperdocSize(Entities entities) {
    throw new NotImplementedException("getEntitySuperdocSize()  is not implemented in DataAccessDMap.");
  }

  public TIntObjectHashMap<TIntIntHashMap> getEntityKeywordIntersectionCount(Entities entities) {
    throw new NotImplementedException("getEntityKeywordIntersectionCount()  is not implemented in DataAccessDMap.");
  }

  public TIntObjectHashMap<YagoUtil.Gender> getGenderForEntities(Entities entities) {
    throw new NotImplementedException("getGenderForEntities()  is not implemented in DataAccessDMap.");
  }

  public TIntIntHashMap getKeyphraseDocumentFrequencies(TIntHashSet keyphrases) {
    throw new NotImplementedException("getKeyphraseDocumentFrequencies()  is not implemented in DataAccessDMap.");
  }

  public List<String> getParentTypes(String queryType) {
    throw new NotImplementedException("getParentTypes()  is not implemented in DataAccessDMap.");
  }

  public TIntObjectHashMap<int[]> getEntityLSHSignatures(Entities entities) {
    throw new NotImplementedException("getEntityLSHSignatures()  is not implemented in DataAccessDMap.");
  }

  public TIntObjectHashMap<int[]> getEntityLSHSignatures(Entities entities, String table) {
    throw new NotImplementedException("getEntityLSHSignatures()  is not implemented in DataAccessDMap.");
  }

  public TIntObjectHashMap<String> getWordsForIds(int[] ids) {
    throw new NotImplementedException("getWordsForIds()  is not implemented in DataAccessDMap.");
  }

  public TObjectIntHashMap<KBIdentifiedEntity> getAllEntityIds() {
    throw new NotImplementedException("getAllEntityIds()  is not implemented in DataAccessDMap.");
  }

  public TObjectIntHashMap<Type> getAllTypeIds() {
    throw new NotImplementedException("getAllTypeIds()  is not implemented in DataAccessDMap.");
  }

  public TIntDoubleHashMap getAllEntityRanks() {
    throw new NotImplementedException("getAllEntityRanks()  is not implemented in DataAccessDMap.");
  }

  public TIntObjectHashMap<int[]> getAllEntityTypes() {
    throw new NotImplementedException("getAllEntityTypes()  is not implemented in DataAccessDMap.");
  }

  public TIntObjectHashMap<int[]> getTaxonomy() {
    throw new NotImplementedException("getTaxonomy()  is not implemented in DataAccessDMap.");
  }

  public TObjectIntHashMap<String> getAllWordIds() {
    throw new NotImplementedException("getAllWordIds()  is not implemented in DataAccessDMap.");
  }

  public Entities getAllEntities() {
    throw new NotImplementedException("getAllEntities()  is not implemented in DataAccessDMap.");
  }

  public int getWordExpansion(int wordId) {
    throw new NotImplementedException("getWordExpansion()  is not implemented in DataAccessDMap.");
  }

  public boolean isYagoEntity(Entity entity) {
    throw new NotImplementedException("isYagoEntity()  is not implemented in DataAccessDMap.");
  }

  public TIntObjectHashMap<int[]> getAllInlinks() {
    throw new NotImplementedException("getAllInlinks()  is not implemented in DataAccessDMap.");
  }

  public int getMaximumEntityId() {
    throw new NotImplementedException("getMaximumEntityId()  is not implemented in DataAccessDMap.");
  }

  public int getMaximumWordId() {
    throw new NotImplementedException("getMaximumWordId()  is not implemented in DataAccessDMap.");
  }

  public TObjectIntHashMap<String> getIdsForTypeNames(Collection<String> typeNames) {
    throw new NotImplementedException("getIdsForTypeNames()  is not implemented in DataAccessDMap.");
  }

  public TIntObjectHashMap<int[]> getEntitiesIdsForTypesIds(int[] typesIds) {
    throw new NotImplementedException("getEntitiesIdsForTypesIds()  is not implemented in DataAccessDMap.");
  }

  public Map<String, List<String>> getAllEntitiesMetaData(String startingWith) {
    throw new NotImplementedException("getAllEntitiesMetaData()  is not implemented in DataAccessDMap.");
  }

  public Map<String, Double> getKeyphraseSourceWeights() {
    throw new NotImplementedException("getKeyphraseSourceWeights()  is not implemented in DataAccessDMap.");
  }

  public int[] getAllKeywordDocumentFrequencies() {
    throw new NotImplementedException("getAllKeywordDocumentFrequencies()  is not implemented in DataAccessDMap.");
  }

  public TIntIntHashMap getGNDTripleCount(Entities entities) {
    throw new NotImplementedException("getGNDTripleCount()  is not implemented in DataAccessDMap.");
  }

  public TIntIntHashMap getGNDTitleCount(Entities entities) {
    throw new NotImplementedException("getGNDTitleCount()  is not implemented in DataAccessDMap.");
  }

  public TIntIntHashMap getYagoOutlinkCount(Entities entities) {
    throw new NotImplementedException("getYagoOutlinkCount()  is not implemented in DataAccessDMap.");
  }

  public Pair<Integer, Integer> getImportanceComponentMinMax(String importanceComponentId) {
    throw new NotImplementedException("getImportanceComponentMinMax()  is not implemented in DataAccessDMap.");
  }
}
