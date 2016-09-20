package mpi.aida.graph.similarity.measure;

import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.map.hash.TIntObjectHashMap;
import mpi.aida.access.DataAccess;
import mpi.aida.data.Entities;
import mpi.aida.data.Entity;
import mpi.aida.graph.similarity.EntityEntitySimilarity;
import mpi.aida.graph.similarity.context.EntitiesContext;
import org.roaringbitmap.RoaringBitmap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;

/**
 * Similarity of two entities is the number of common inlinks
 * 
 *
 */
public class InlinkOverlapEntityEntitySimilarity extends EntityEntitySimilarity {
  private static final Logger logger = 
      LoggerFactory.getLogger(InlinkOverlapEntityEntitySimilarity.class);

  private TIntObjectHashMap<RoaringBitmap> entity2vector;

  Connection con;

  public InlinkOverlapEntityEntitySimilarity(EntityEntitySimilarityMeasure similarityMeasure, EntitiesContext entityContext) throws Exception {
    // not needed - uses entites directly
    super(similarityMeasure, entityContext);

    setupEntities(entityContext.getEntities());
  }

  private void setupEntities(Entities entities) throws Exception {
    if (entities.size() == 0) {
      logger.debug("Skipping initialization of InlinkEntityEntitySimilarity for " + entities.size() + " entities");
      return;
    }

    logger.debug("Initializing InlinkEntityEntitySimilarity for " + entities.size() + " entities");

    entity2vector = new TIntObjectHashMap<>();

    TIntObjectHashMap<int[]> entityInlinks = 
        DataAccess.getInlinkNeighbors(entities);
    
    for (TIntObjectIterator<int[]> itr = entityInlinks.iterator();
        itr.hasNext(); ) {
      itr.advance();
      int entity = itr.key();
      int[] inLinks = itr.value();

      RoaringBitmap bs = new RoaringBitmap();
      for (int l : inLinks) {
        bs.add(l);
      }
      entity2vector.put(entity, bs);
    }

    logger.debug("Done initializing InlinkEntityEntitySimilarity");
  }

  @Override
  public double calcSimilarity(Entity a, Entity b) throws Exception {
    RoaringBitmap bsA = entity2vector.get(a.getId());
    RoaringBitmap bsB = entity2vector.get(b.getId());
    
    int isecCount = RoaringBitmap.and(bsA, bsB).getCardinality();
    int unionCount = RoaringBitmap.or(bsA, bsB).getCardinality();

    if (isecCount == 0 || unionCount == 0) {
      return 0.0; // cannot calc
    }

    double sim = (double) isecCount
                 / (double) unionCount;
    
    return sim;
  }
  
  public String toString() {
    return "InlinkOverlapEntityEntitySimilarity";
  }
}
