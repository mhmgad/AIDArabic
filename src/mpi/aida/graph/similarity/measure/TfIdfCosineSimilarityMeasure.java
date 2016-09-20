package mpi.aida.graph.similarity.measure;

import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.hash.TIntHashSet;
import mpi.aida.access.DataAccess;
import mpi.aida.data.Context;
import mpi.aida.data.Entity;
import mpi.aida.data.Mention;
import mpi.aida.graph.similarity.context.EntitiesContext;
import mpi.experiment.trace.Tracer;

/**
 * Calculates the similarity of two contexts by the cosine similarity
 * of their tf.idf weighted term vectors.
 * 
 *
 */
public class TfIdfCosineSimilarityMeasure extends MentionEntitySimilarityMeasure {

  private int collectionSize_;
  
  public TfIdfCosineSimilarityMeasure(Tracer tracer) {
    super(tracer);
    collectionSize_ = DataAccess.getCollectionSize();
  }

  @Override
  public double calcSimilarity(Mention mention, Context context, Entity entity, EntitiesContext entitiesContext) {
    TIntDoubleHashMap contextVec = getTfIdfVector(context.getTokenIds());
    TIntDoubleHashMap entityVec = getTfIdfVector(entitiesContext.getContext(entity));

    double sim = calcCosine(entityVec, contextVec);
    return sim;
  }

  protected double calcCosine(TIntDoubleHashMap entityVec, TIntDoubleHashMap contextVec) {
    double dotProduct = 0.0;

    for (int termA : entityVec.keys()) {
      int expandedA = DataAccess.expandTerm(termA);
      if (contextVec.containsKey(termA)) {
        double tempProduct = entityVec.get(termA) * contextVec.get(termA);
        dotProduct += tempProduct;
      }
      if (contextVec.containsKey(expandedA)) {
        double tempProduct = entityVec.get(termA) * contextVec.get(expandedA);
        dotProduct += tempProduct;
      }
    }

    double normA = 0.0;
    for (double weightA : entityVec.values()) {
      normA += weightA * weightA;
    }
    normA = Math.sqrt(normA);

    double normB = 0.0;
    for (double weightB : contextVec.values()) {
      normB += weightB * weightB;
    }
    normB = Math.sqrt(normB);

    double sim = 0.0;

    if (normA * normB != 0) {
      sim = dotProduct / (normA * normB);
    }

    return sim;
  }

  private TIntDoubleHashMap getTfIdfVector(int[] is) {
    TIntDoubleHashMap vector = new TIntDoubleHashMap();

    TIntIntHashMap tfs = new TIntIntHashMap();

    for (int term : is) {
      tfs.adjustOrPutValue(term, 1, 1);
    }

    TIntIntHashMap termDFs =
        DataAccess.getKeywordDocumentFrequencies(new TIntHashSet(is));
    
    for (int term : new TIntHashSet(is).toArray()) {
      int tf = tfs.get(term);
      int df = termDFs.get(term);
      if (df == 0) df = collectionSize_; // default smoothing

      double tfIdf = 
          (double) tf 
          * log2((double) collectionSize_ / (double) df);

      vector.put(term, tfIdf);
    }

    return vector;
  }

  public static double log2(double x) {
    return Math.log(x) / Math.log(2);
  }
}
