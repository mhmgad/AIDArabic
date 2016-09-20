package mpi.aida.graph.similarity;

import gnu.trove.iterator.TIntDoubleIterator;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Set;

import mpi.aida.AidaManager;
import mpi.aida.access.DataAccess;
import mpi.aida.data.Mention;
import mpi.aida.util.CollectionUtils;

/**
 * This class calculates the prior probability of a mention
 * being associated with a given entity. The prior probability is based
 * on the occurrence count of links (and their anchor text as mention) with
 * a given Wikipedia/YAGO entity as target.
 * 
 * It is faster than {@link PriorProbability} because it uses a table with 
 * all the priors materialized. To get the table, run the {@link MaterializedPriorProbability}
 * main method, it will create another table in the YAGO2 database which can
 * then be used by this class. 
 *    
 *
 */
public class MaterializedPriorProbability extends PriorProbability {

  public MaterializedPriorProbability(Set<Mention> mentions) throws SQLException {
    super(mentions);
  }

  public void setupMentions(Set<Mention> mentions) throws SQLException {
    // Get the prior for mention-entity pairs (aggregate normalized mentions).
    // Precompute the best prior per mention as well.
    priors = new HashMap<Mention, TIntDoubleHashMap>();
    bestPriors = new TObjectDoubleHashMap<Mention>();
    for (Mention mention : mentions) {
      if (mention.getNormalizedMention().size() == 1) {
        String normalizedMention = 
            mention.getNormalizedMention().iterator().next();
        normalizedMention = AidaManager.conflateToken(normalizedMention);
        TIntDoubleHashMap entityPriors = 
            DataAccess.getEntityPriors(normalizedMention);
        priors.put(mention, entityPriors);
        bestPriors.put(mention, CollectionUtils.getMaxValue(entityPriors));
      } else {    
        TIntDoubleHashMap allMentionPriors = new TIntDoubleHashMap();
        priors.put(mention, allMentionPriors);
        for(String normalizedMention: mention.getNormalizedMention()) {
          normalizedMention = AidaManager.conflateToken(normalizedMention);
          TIntDoubleHashMap entityPriors = 
              DataAccess.getEntityPriors(normalizedMention);  
          for (TIntDoubleIterator it = entityPriors.iterator(); it.hasNext();) {
            it.advance();
            int e = it.key();
            double prior = it.value();
            if (allMentionPriors.containsKey(e)) {
              allMentionPriors.put(e, Math.max(allMentionPriors.get(e), prior));
            } else {
              allMentionPriors.put(e, prior);
            }
          }
        }
        bestPriors.put(mention, CollectionUtils.getMaxValue(allMentionPriors));
      }
    }
  }
}