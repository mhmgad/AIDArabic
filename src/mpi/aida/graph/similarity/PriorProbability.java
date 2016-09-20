package mpi.aida.graph.similarity;

import gnu.trove.iterator.TIntDoubleIterator;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;

import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

import mpi.aida.data.Entity;
import mpi.aida.data.Mention;
import mpi.aida.util.timing.RunningTimer;

/**
 * This class calculates the prior probability of a mention
 * being associated with a given entity. The prior probability is based
 * on the occurrence count of links (and their anchor text as mention) with
 * a given Wikipedia/YAGO entity as target.
 * 
 * The calculation is done on the fly, so it is a bit slow. For a faster implementation,
 * use {@link MaterializedPriorProbability}.
 * 
 * It uses the 'hasInternalWikipediaLinkTo' and 'hasAnchorText' relations
 * in the YAGO2 database.
 * 
 *
 */
public abstract class PriorProbability {
 
  protected Map<Mention, TIntDoubleHashMap> priors;
  
  protected TObjectDoubleHashMap<Mention> bestPriors;
  
  private double weight;
  
  public PriorProbability(Set<Mention> mentions) throws SQLException {
    setupMentions(mentions);
  }
  
  public double getWeight() {
    return weight;
  }

  public void setWeight(double weight) {
    this.weight = weight;
  }
  
  protected abstract void setupMentions(Set<Mention> mentions) throws SQLException;

  /**
   * Returns the prior probability for the given mention-entity pair.
   * If smoothing is true, it will return the lowest prior among all entities if
   * there is no real prior.
   * 
   * @param mention
   * @param entity
   * @param smoothing
   * @return
   */
  public double getPriorProbability(
      Mention mention, Entity entity, boolean smoothing) {
    Integer id = RunningTimer.recordStartTime("PriorProbability");
    TIntDoubleHashMap allMentionPriors = priors.get(mention);    
    double entityPrior = allMentionPriors.get(entity.getId());
    
    if (smoothing && entityPrior == 0.0) {
      double smallestPrior = 1.0;
      
      for (TIntDoubleIterator it = allMentionPriors.iterator(); it.hasNext();) {
        it.advance();
        double currentPrior = it.value(); 
        if (currentPrior < smallestPrior) {
          smallestPrior = currentPrior;
        }
      }      
      entityPrior = smallestPrior;
    }
    RunningTimer.recordEndTime("PriorProbability", id);
    return entityPrior;
  }
  
  public double getBestPrior(Mention mention) {
    return bestPriors.get(mention);
  }
  
  public double getPriorProbability(Mention mention, Entity entity) {
    return getPriorProbability(mention, entity, false);
  }
} 
