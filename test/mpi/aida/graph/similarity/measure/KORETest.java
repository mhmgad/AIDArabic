package mpi.aida.graph.similarity.measure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import mpi.aida.AidaManager;
import mpi.aida.access.DataAccessForTesting;
import mpi.aida.config.AidaConfig;
import mpi.aida.data.Entities;
import mpi.aida.data.Entity;
import mpi.aida.graph.similarity.EntityEntitySimilarity;
import mpi.aida.graph.similarity.context.EntitiesContextSettings;
import mpi.aida.graph.similarity.context.EntitiesContextSettings.EntitiesContextType;
import mpi.experiment.trace.NullTracer;

import org.junit.Test;


public class KORETest {
  public KORETest() {
    AidaConfig.set("dataAccess", "testing");
    AidaConfig.set(AidaConfig.CACHE_WORD_DATA, "false");
    AidaManager.init();
  }
  
  @Test
  public void koreTest() throws Exception {
    Entity a = DataAccessForTesting.getTestEntity("Kashmir_(song)");
    Entity b = DataAccessForTesting.getTestEntity("Jimmy_Page");
    Entity c = DataAccessForTesting.getTestEntity("Larry_Page");
    Entity d = DataAccessForTesting.getTestEntity("Knebworth_Festival");
    
    Entities entities = new Entities();
    entities.add(a);
    entities.add(b);
    entities.add(c);
    entities.add(d);

    EntitiesContextSettings ecs = new EntitiesContextSettings();
    ecs.setEntityCoherenceKeyphraseAlpha(1.0);
    ecs.setEntityCoherenceKeywordAlpha(0.0);
    ecs.setShouldNormalizeWeights(true);
    ecs.setEntitiesContextType(EntitiesContextType.ENTITY_ENTITY);
    Map<String, Double> keyphraseSourceWeights = new HashMap<String, Double>();
    ecs.setEntityEntityKeyphraseSourceWeights(keyphraseSourceWeights);
    EntityEntitySimilarity kore = 
        EntityEntitySimilarity.getKOREEntityEntitySimilarity(
            entities, ecs, new NullTracer());

    double simAB = kore.calcSimilarity(a, b);
    double simAC = kore.calcSimilarity(a, c);
    double simBD = kore.calcSimilarity(b, d);
    double simCD = kore.calcSimilarity(c, d);
    double simAD = kore.calcSimilarity(a, d);
    
    assertTrue(simAB > simAC);
    assertTrue(simAD < simAB);
    assertTrue(simBD > simCD);
    assertEquals(0.2091, simAB, 0.0001);
    assertEquals(0.1125, simBD, 0.0001);
    assertEquals(0.1613, simAD, 0.0001);
    assertEquals(0.0, simCD, 0.001);
  }
}
