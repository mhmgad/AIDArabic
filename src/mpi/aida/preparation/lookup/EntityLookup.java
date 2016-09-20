package mpi.aida.preparation.lookup;

import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import mpi.aida.AidaManager;
import mpi.aida.access.DataAccess;
import mpi.aida.config.AidaConfig;
import mpi.aida.data.CandidateDictionary;
import mpi.aida.data.Entities;
import mpi.aida.data.Entity;
import mpi.aida.data.Mention;
import mpi.aida.data.Mentions;
import mpi.aida.data.OokbEntity;
import mpi.aida.data.Type;
import mpi.aida.datapreparation.gnd.util.GNDYagoIdsMapper;
import mpi.aida.util.Counter;
import mpi.aida.util.MathUtil;
import mpi.aida.util.StringUtils;
import mpi.aida.util.YagoUtil.Gender;
import mpi.aida.util.timing.RunningTimer;
import mpi.lsh.LSH;
import mpi.lsh.LSHStringNgramFeatureExtractor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This package-level class retrieves list of candidate entities for given mentions.
 * The actual retrieval depends on the type of instance created during runtime.
 * Based on the runtime type, entities will be retrieved by database lookup or
 * dictionary lookup.
 */
abstract class EntityLookup {

  private Logger logger_ = LoggerFactory.getLogger(EntityLookup.class);

  private LSH<String> lshEntityLookup_;

  protected final Set<String> malePronouns = new HashSet<String>() {

    private static final long serialVersionUID = 2L;
    {
      add("He");
      add("he");
      add("Him");
      add("him");
      add("His");
      add("his");
    }
  };

  protected final Set<String> femalePronouns = new HashSet<String>() {

    private static final long serialVersionUID = 3L;
    {
      add("she");
      add("she");
      add("Her");
      add("her");
      add("Hers");
      add("hers");
    }
  };

  public EntityLookup() {
    // If LSH matching should be done, initialize the LSH datastructures.
    if (AidaConfig.getBoolean(AidaConfig.DICTIONARY_LSH_MATCHING)) {
      lshEntityLookup_ = createMentionLsh();
    }
  }

  private LSH<String> createMentionLsh() {
    logger_.info("Reading all entity names to create LSH representation.");
    Set<String> names = DataAccess.getDictionary().keySet();
    logger_.info("Creating LSH representation.");
    LSH<String> lsh = null;
    try {
      lsh = LSH.createLSH(names, new LSHStringNgramFeatureExtractor(), 4, 6, 1);
    } catch (InterruptedException e) {
      logger_.warn("Could not finish the creation of the mention LSH.");
      Thread.currentThread().interrupt();
    }
    return lsh;
  }

  public abstract Entities getEntitiesForMention(Mention mention, double maxEntityRank, int topByPrior, boolean mentionIsPrefix) throws IOException;

  public void fillInCandidateEntities(Mentions mentions, 
      CandidateDictionary externalDictionary, 
      boolean includeNullEntityCandidates, boolean includeContextMentions, 
      double maxEntityRank, int topByPrior, boolean mentionIsPrefix)
          throws SQLException, IOException {
    String logMsg = "Retrieving candidates with max global rank of " + maxEntityRank + ".";
    if (topByPrior > 0) {
      logMsg += " Restricting to top " + topByPrior + " candidates per mention.";
    }
    logger_.debug(logMsg);
    //flag to be used when having entities from different knowledge bases
    //and some of them are linked by a sameAs relation
    //currently applicable only for the configuration GND_PLUS_YAGO
    Integer id = RunningTimer.recordStartTime("EntityLookup:fillInCandidates");
    boolean removeDuplicateEntities = false;
    if(DataAccess.getConfigurationName().equals("GND_PLUS_YAGO")) {
      removeDuplicateEntities = true;
    }

    Set<Type> filteringTypes = mentions.getEntitiesTypes();
    //TODO This method shouldn't be doing one DB call per mention!
    for (int i = 0; i < mentions.getMentions().size(); i++) {
      Mention m = mentions.getMentions().get(i);
      Entities mentionCandidateEntities;
      if (malePronouns.contains(m.getMention()) || femalePronouns.contains(m.getMention())) {
        // TODO If we want pronouns, we need to enable this again.
        // setCandiatesFromPreviousMentions(mentions, i);
        // TODO For now, just set empty candidates.
        m.setCandidateEntities(new Entities());
      } else {
        mentionCandidateEntities = getEntitiesForMention(m, maxEntityRank, topByPrior, mentionIsPrefix);
        // Check for fallback options when no candidate was found using direct lookup.
        if(mentionCandidateEntities.size() == 0) {
          Counter.incrementCount("MENTION_WITHOUT_CANDIDATE");
          mentionCandidateEntities = getEntitiesByFuzzy(m, maxEntityRank, topByPrior);
        }
        
        if (externalDictionary != null) {
          mentionCandidateEntities.addAll(externalDictionary.getEntities(m));
        }

        int candidateCount = mentionCandidateEntities.size();
        mentionCandidateEntities = filterEntitiesByType(mentionCandidateEntities, filteringTypes);
        int filteredCandidateCount = mentionCandidateEntities.size();
        if (filteredCandidateCount < candidateCount) {
          Counter.incrementCountByValue("MENTION_CANDIDATE_FILTERED_BY_TYPE", candidateCount - filteredCandidateCount);
        }
        if (includeNullEntityCandidates) {
          Entity nmeEntity = new OokbEntity(m.getMention());

          // add surrounding mentions as context
          if (includeContextMentions) {
            List<String> surroundingMentionsNames = new LinkedList<String>();
            int begin = Math.max(i - 2, 0);
            int end = Math.min(i + 3, mentions.getMentions().size());

            for (int s = begin; s < end; s++) {
              if (s == i) continue; // skip mention itself
              surroundingMentionsNames.add(mentions.getMentions().get(s).getMention());
            }
            nmeEntity.setSurroundingMentionNames(surroundingMentionsNames);
          }

          mentionCandidateEntities.add(nmeEntity);
        }
        if(removeDuplicateEntities) {
          removeDuplicateEntities(mentionCandidateEntities);
        }
        m.setCandidateEntities(mentionCandidateEntities);
      }
    }
    RunningTimer.recordEndTime("EntityLookup:fillInCandidates", id);
  }


  private Entities getEntitiesByFuzzy(Mention m, double maxEntityRank, int topByPrior) {
    Entities mentionCandidateEntities = new Entities();
    boolean doDictionaryFuzzyMatching = AidaConfig.getBoolean(AidaConfig.DICTIONARY_FUZZY_MATCHING);
    boolean doLshMatching = AidaConfig.getBoolean(AidaConfig.DICTIONARY_LSH_MATCHING);
    if (doDictionaryFuzzyMatching) {
      double minSim = AidaConfig.getDouble(AidaConfig.DICTIONARY_FUZZY_MATCHING_MIN_SIM);
      mentionCandidateEntities = DataAccess.getEntitiesForMentionByFuzzyMatcyhing(m.getMention(), minSim);
      if (mentionCandidateEntities.size() > 0) {
        Counter.incrementCount("MENTION_CANDIDATE_BY_PG_FUZZY");
      }
    } else if (doLshMatching) {
      mentionCandidateEntities = getEntitiesByLsh(m.getMention(), maxEntityRank, topByPrior);
      if (mentionCandidateEntities.size() > 0) {
        Counter.incrementCount("MENTION_CANDIDATE_BY_LSH_FUZZY");
      }
    }
    return mentionCandidateEntities;
  }

  private void setCandiatesFromPreviousMentions(Mentions mentions, int mentionIndex) {
    Mention mention = mentions.getMentions().get(mentionIndex);
    Entities allPrevCandidates = new Entities();
    if (mentionIndex == 0) {
      mention.setCandidateEntities(allPrevCandidates);
      return;
    }

    for (int i = 0; i < mentionIndex; i++) {
      Mention m = mentions.getMentions().get(i);
      for (Entity e : m.getCandidateEntities()) {
        allPrevCandidates.add(e);
      }
    }

    TIntObjectHashMap<Gender> entitiesGenders = AidaManager.getGenderForEntities(allPrevCandidates);

    Gender targetGender = null;
    if (malePronouns.contains(mention.getMention())) targetGender = Gender.MALE;
    else if (femalePronouns.contains(mention.getMention())) targetGender = Gender.FEMALE;

    Entities filteredCandidates = new Entities();
    for (Entity e : allPrevCandidates) {
      if (entitiesGenders != null && entitiesGenders.containsKey(e.getId())
          && entitiesGenders.get(e.getId()) == targetGender) filteredCandidates
          .add(e);
    }
    mention.setCandidateEntities(filteredCandidates);
  }

  private Entities getEntitiesByLsh(
      String mention, double maxEntityRank, int topByPrior) {
    Entities candidates = new Entities();
    String conflatedMention = AidaManager.conflateToken(mention);
    Set<String> names = lshEntityLookup_.getSimilarItemsForFeature(conflatedMention);

    // Check for a sufficiently high Jaccard overlap to avoid false positives.
    Set<String> similarNames = new HashSet<>();
    Set<String> mentionTrigrams = StringUtils.getNgrams(conflatedMention, 3);
    for (String candidate : names) {
      Set<String> candTrigrams = StringUtils.getNgrams(candidate, 3);
      double jSim = MathUtil.computeJaccardSimilarity(mentionTrigrams, candTrigrams);
      if (jSim > AidaConfig.getDouble(AidaConfig.DICTIONARY_LSH_MATCHING_MIN_SIM)) {
        similarNames.add(candidate);
      }
    }

    Collection<Entities> mentionCandidates =
        DataAccess.getEntitiesForMentions(similarNames, maxEntityRank, topByPrior).values();

    for (Entities cands : mentionCandidates) {
      candidates.addAll(cands);
    }
    return candidates;
  }

  /**
   * Filters the entity candidates against the given list of types
   * 
   * @param entities Entities to filter'
   * @param filteringTypes Set of types to filter the entities against
   * @return filtered entities
   */
  private Entities filterEntitiesByType(Entities entities, Set<Type> filteringTypes) {
    if(filteringTypes == null) {
      return entities;
    }
    Entities filteredEntities = new Entities();
    TIntObjectHashMap<Set<Type>> entitiesTypes = DataAccess.getTypes(entities);
    for (TIntObjectIterator<Set<Type>> itr = entitiesTypes.iterator();
        itr.hasNext(); ) {
      itr.advance();
      int id = itr.key();
      Set<Type> entityTypes = itr.value();
      for (Type t : entityTypes) {
        if (filteringTypes.contains(t)) {
          filteredEntities.add(entities.getEntityById(id));
          break;
        }
      }
    }
    return filteredEntities;
  }

  private Entities removeDuplicateEntities(Entities mentionCandidateEntities) {
    //for now only remove YAGO entities that are also GND entities
    GNDYagoIdsMapper gndYagoIdsMapper = GNDYagoIdsMapper.getInstance(true);

    Entities filteredEntities = new Entities();

    for (Entity entity : mentionCandidateEntities) {
      //try to get a GND id assuming this is a yago Entity
      int gndId = gndYagoIdsMapper.mapFromYagoId(entity.getId());
      //if there exist a gnd counter part, and it's one of the candidates,
      //ignore this entity
      if(gndId > 0 && mentionCandidateEntities.contains(gndId)) {
        continue;
      }
      filteredEntities.add(entity);
    }
    return filteredEntities;
  }
}