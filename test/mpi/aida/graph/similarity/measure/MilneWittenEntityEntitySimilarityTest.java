package mpi.aida.graph.similarity.measure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import mpi.aida.AidaManager;
import mpi.aida.access.DataAccessForTesting;
import mpi.aida.config.AidaConfig;
import mpi.aida.data.Entities;
import mpi.aida.data.Entity;
import mpi.aida.graph.similarity.EntityEntitySimilarity;
import mpi.experiment.trace.NullTracer;

import org.junit.Test;

public class MilneWittenEntityEntitySimilarityTest {

  public MilneWittenEntityEntitySimilarityTest() {
    AidaConfig.set("dataAccess", "testing");
    AidaConfig.set(AidaConfig.CACHE_WORD_DATA, "false");
    AidaManager.init();
  }
  
  @Test
  public void mwTest() throws Exception {
    Entity a = DataAccessForTesting.getTestEntity("Kashmir_(song)");
    Entity b = DataAccessForTesting.getTestEntity("Jimmy_Page");
    Entity c = DataAccessForTesting.getTestEntity("Larry_Page");
    Entity d = DataAccessForTesting.getTestEntity("Knebworth_Festival");
    
    Entities entities = new Entities();
    entities.add(a);
    entities.add(b);
    entities.add(c);
    entities.add(d);

    EntityEntitySimilarity mwSim = 
        EntityEntitySimilarity.getMilneWittenSimilarity(
            entities, new NullTracer());

    double simAB = mwSim.calcSimilarity(a, b);
    double simAC = mwSim.calcSimilarity(a, c);
    double simBD = mwSim.calcSimilarity(b, d);
    double simCD = mwSim.calcSimilarity(c, d);
    double simAD = mwSim.calcSimilarity(a, d);
    
    assertTrue(simAB > simAC);
    assertTrue(simAD < simAB);
    assertTrue(simBD > simCD);
    assertEquals(0.9493, simAB, 0.0001);
    assertEquals(0.8987, simBD, 0.0001);
    assertEquals(0.9197, simAD, 0.0001);
    assertEquals(0.0, simCD, 0.001);
  }
}
