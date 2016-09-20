package mpi.aida.graph.similarity.measure;

import gnu.trove.set.hash.TIntHashSet;

import java.util.HashMap;
import java.util.Map;

import mpi.aida.access.DataAccess;
import mpi.aida.data.Entity;
import mpi.aida.graph.similarity.context.EntitiesContext;
import mpi.aida.graph.similarity.context.FastWeightedKeyphrasesContext;
import mpi.aida.util.CollectionUtils;
import mpi.experiment.trace.Tracer;
import mpi.experiment.trace.measures.KeytermEntityEntityMeasureTracer;
import mpi.experiment.trace.measures.TermTracer;

public class JaccardEntityEntitySimilarityMeasure extends EntityEntitySimilarityMeasure {

  public JaccardEntityEntitySimilarityMeasure(Tracer tracer) {
    super(tracer);
  }

  @Override
  public double calcSimilarity(Entity a, Entity b, EntitiesContext context) {   
    TIntHashSet contextA = new TIntHashSet(context.getContext(a));
    TIntHashSet contextB = new TIntHashSet(context.getContext(b));

    TIntHashSet union = getUnion(contextA, contextB);
    TIntHashSet intersection = getIntersection(contextA, contextB);

    double jaccardSim = (double) intersection.size() / (double) union.size();   
    return jaccardSim;
  }

  private TIntHashSet getIntersection(TIntHashSet contextA, TIntHashSet contextB) {
    TIntHashSet is = new TIntHashSet();

    for (int a : contextA.toArray()) {
      if (contextB.contains(a) || contextB.contains(DataAccess.expandTerm(a))) {
        is.add(a);
      }
    }

    return is;
  }

  private TIntHashSet getUnion(TIntHashSet contextA, TIntHashSet contextB) {
    TIntHashSet union = new TIntHashSet();

    for (int a : contextB.toArray()) {
      union.add(a);
    }

    for (int a : contextA.toArray()) {
      if (!union.contains(a) && !union.contains(DataAccess.expandTerm(a))) {
        union.add(a);
      }
    }

    return union;
  }
  
  @SuppressWarnings("unused")
  private void collectTracingInfo(Entity a, Entity b, int[] kpsA, int[] kpsB, double sim, Map<Integer, TermTracer> matches, FastWeightedKeyphrasesContext kwc) {
    Map<Integer, Double> e1keyphrases = new HashMap<Integer, Double>();     
    for (int kp : kpsA) {
      if (kwc.getCombinedKeyphraseMiIdfWeight(a, kp) > 0.0) {
        e1keyphrases.put(kp, kwc.getCombinedKeyphraseMiIdfWeight(a, kp));
      }
    }     
    e1keyphrases = CollectionUtils.sortMapByValue(e1keyphrases, true);    

    Map<Integer, Double> e2keyphrases = new HashMap<Integer, Double>();
    for (int kp : kpsB) {
      if (kwc.getCombinedKeyphraseMiIdfWeight(b, kp) > 0.0) {
        e2keyphrases.put(kp, kwc.getCombinedKeyphraseMiIdfWeight(b, kp));
      }
    }  
    e2keyphrases = CollectionUtils.sortMapByValue(e2keyphrases, true);

    tracer.eeTracing().addEntityContext(a.getId(), e1keyphrases);
    tracer.eeTracing().addEntityContext(b.getId(), e2keyphrases);

    KeytermEntityEntityMeasureTracer mt = new KeytermEntityEntityMeasureTracer("PartialKeyphraseSim", 0.0, e2keyphrases, matches);
    mt.setScore(sim);
    tracer.eeTracing().addEntityEntityMeasureTracer(a.getId(), b.getId(), mt);

    KeytermEntityEntityMeasureTracer mt2 = new KeytermEntityEntityMeasureTracer("PartialKeyphraseSim", 0.0, e1keyphrases, matches);
    mt2.setScore(sim);
    tracer.eeTracing().addEntityEntityMeasureTracer(b.getId(), a.getId(), mt2);
  }
}
