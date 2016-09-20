package mpi.aida.graph.similarity.measure;

import java.util.HashMap;

import mpi.aida.data.Entity;
import mpi.aida.graph.similarity.context.EntitiesContext;
import mpi.aida.graph.similarity.context.LSHContext;
import mpi.experiment.trace.Tracer;
import mpi.experiment.trace.measures.KeytermEntityEntityMeasureTracer;
import mpi.experiment.trace.measures.TermTracer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LSHEntityEntitySimilarityMeasure extends EntityEntitySimilarityMeasure {
  private static final Logger logger = 
      LoggerFactory.getLogger(LSHEntityEntitySimilarityMeasure.class);

  EntityEntitySimilarityMeasure realSim;
  
  public LSHEntityEntitySimilarityMeasure(Tracer tracer) {    
    super(tracer);
    
    realSim = new KOREEntityEntitySimilarityMeasure(tracer);
  }

  @Override
  public double calcSimilarity(Entity a, Entity b, EntitiesContext context) {
    LSHContext ctx = (LSHContext) context;
    
    double sim = 0.0; // default sim is 0
    
    if (ctx.isRelated(a, b)) {
      sim = calcRealSimilarity(a,b,ctx);
      logger.debug("Compared '" + a.getIdentifierInKb() + "' - '" + b.getIdentifierInKb()+ "'");
    } else {
      logger.debug("SKIPPED '" + a.getIdentifierInKb() + "' - '" + b.getIdentifierInKb() + "'");

      
      KeytermEntityEntityMeasureTracer mt = new KeytermEntityEntityMeasureTracer("PartialKeyphraseSim", 0.0, new HashMap<Integer, Double>(), new HashMap<Integer, TermTracer>());
      mt.setScore(sim);
      tracer.eeTracing().addEntityEntityMeasureTracer(a.getId(), b.getId(), mt);

      KeytermEntityEntityMeasureTracer mt2 = new KeytermEntityEntityMeasureTracer("PartialKeyphraseSim", 0.0, new HashMap<Integer, Double>(), new HashMap<Integer, TermTracer>());
      mt2.setScore(sim);
      tracer.eeTracing().addEntityEntityMeasureTracer(b.getId(), a.getId(), mt2);
    }
    
    return sim;
  }

  private double calcRealSimilarity(Entity a, Entity b, LSHContext context) {
    double sim = realSim.calcSimilarity(a, b, context);
    
    return sim;   
  }
}
