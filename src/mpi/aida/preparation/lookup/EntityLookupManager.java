package mpi.aida.preparation.lookup;

import mpi.aida.config.AidaConfig;
import mpi.aida.data.CandidateDictionary;
import mpi.aida.data.Entities;
import mpi.aida.data.Mention;
import mpi.aida.data.Mentions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Provides entity candidate lookup. Uses a database or ternarytree/dmap backend, the actual implementation
 * is done in a subclass of {@see mpi.aida.preparation.EntityLookup}.
 */
public class EntityLookupManager {

  private Logger logger_ = LoggerFactory.getLogger(EntityLookupManager.class);

  private EntityLookup lookupInst;

  private static class EntityLookupManagerHolder {

    public static EntityLookupManager entityLookupManager = new EntityLookupManager();
  }

  public static EntityLookupManager singleton() {
    return EntityLookupManagerHolder.entityLookupManager;
  }

  private EntityLookupManager() {
    switch (AidaConfig.getEntityLookupSource()) {
      case DATABASE:
        lookupInst = new DbLookup();
        break;
      // Not in use right now - needs to be fixed for prefix-lookup.
//      case DICTIONARY:
//        try {
//          String dicPath = AidaConfig.get(AidaConfig.CANDIDATE_ENTITY_LOOKUP_DICTIONARY_PATH);
//          File dicDir = null;
//          if (dicPath == null) {
//            try {
//              dicDir = TernaryTreeDictionaryBuilder.getDefaultDictionaryDirectory();
//            } catch (IOException e) {
//              logger_.warn("Could not determine default AIDA database for loading the default dictionary. Please " +
//                      "specify 'lookup.dictionary.path' in 'settings/aida.properties'.");
//            }
//          } else {
//            dicDir = new File(dicPath);
//          }
//          lookupInst = new DictionaryLookup(dicDir);
//        } catch (IOException e) {
//          logger_.error("Could not create DictionaryLookup: " + e.getLocalizedMessage());
//          e.printStackTrace();
//        }
//        break;
    }
  }

  public Entities getEntitiesForMention(Mention mention) throws IOException {
    return getEntitiesForMention(mention, 1.0, 0, false);
  }

  public Entities getEntitiesForMention(Mention mention, double maxEntityRank) throws IOException {
    return getEntitiesForMention(mention, maxEntityRank, 0, false);
  }

  public Entities getEntitiesForMention(Mention mention, double maxEntityRank, int topByPrior) throws IOException {
    return getEntitiesForMention(mention, maxEntityRank, topByPrior, false);
  }

  /**
   * Returns the potential entity candidates for a mention (from AIDA dictionary)
   *
   * @param mention
   *            Mention to get entity candidates for
   * @param maxEntityRank Retrieve entities up to a global rank, where rank is
   * between 0.0 (best) and 1.0 (worst). Setting to 1.0 will retrieve all entities.
   * @param topByPrior  Retrieve only the best entities according to the prior.
   * @param mentionIsPrefix Treat the mention string as prefix, gathering all candidates that have at least one label
   *                        with mention as prefix.
   * @return Candidate entities for this mention.
   *
   */
  public Entities getEntitiesForMention(Mention mention, double maxEntityRank, int topByPrior, boolean mentionIsPrefix) throws IOException {
    return lookupInst.getEntitiesForMention(mention, maxEntityRank, topByPrior, mentionIsPrefix);
  }

  public void fillInCandidateEntities(Mentions mentions) throws SQLException, IOException {
    fillInCandidateEntities(mentions, null, false, false, 1.0, 0, false);
  }

  public void fillInCandidateEntities(Mentions mentions, boolean includeNullEntityCandidates, boolean includeContextMentions, double maxEntityRank) throws SQLException, IOException {
    fillInCandidateEntities(mentions, null, includeNullEntityCandidates, includeContextMentions, maxEntityRank, 0, false);
  }

  /**
   * Retrieves all the candidate entities for the given mentions.
   *
   * @param mentions  All mentions in the input doc.
   * @param externalDictionary Additional mention-entity dictionary for lookups.
   * Can be used to supplement the actual entity repository, pass null to ignore.
   * @param includeNullEntityCandidates Set to true to include mentions flagged
   * as NME in the ground-truth data.
   * @param includeContextMentions  Include mentions as context.
   * @param maxEntityRank Fraction of entities to include. Between 0.0 (none)
   * and 1.0 (all). The ranks are taken from the entity_rank table.
   * @param topByPrior  How many candidates to return, according to the ranking by prior. Set to 0 to return all.
   * @param mentionIsPrefix Treat the mention string as prefix, gathering all candidates that have at least one label
   *                        with mention as prefix.
   * @throws SQLException
   */
  public void fillInCandidateEntities(Mentions mentions, CandidateDictionary externalDictionary,
                                      boolean includeNullEntityCandidates, boolean includeContextMentions,
                                      double maxEntityRank, int topByPrior, boolean mentionIsPrefix)
          throws SQLException, IOException {
    lookupInst.fillInCandidateEntities(
            mentions, externalDictionary, includeNullEntityCandidates, includeContextMentions,
            maxEntityRank, topByPrior, mentionIsPrefix);
  }
}
