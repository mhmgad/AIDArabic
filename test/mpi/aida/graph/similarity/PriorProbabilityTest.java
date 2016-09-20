package mpi.aida.graph.similarity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import mpi.aida.AidaManager;
import mpi.aida.access.DataAccessForTesting;
import mpi.aida.config.AidaConfig;
import mpi.aida.data.Entity;
import mpi.aida.data.Mention;

import org.junit.Test;

public class PriorProbabilityTest {
  
  public PriorProbabilityTest() {
    AidaConfig.set("dataAccess", "testing");
    AidaConfig.set(AidaConfig.CACHE_WORD_DATA, "false");
    AidaManager.init();
  }
  
  @Test
  public void test() throws Exception {
    Set<Mention> mentions = new HashSet<>();
    Mention kashmirMention = new Mention();
    kashmirMention.setMention("Kashmir");
    
    Mention pageMention = new Mention();
    pageMention.setMention("Page");
    
    
    mentions.add(kashmirMention);
    mentions.add(pageMention);
    
    Entity kashmir = DataAccessForTesting.getTestEntity("Kashmir");
    Entity kashmirSong = DataAccessForTesting.getTestEntity("Kashmir_(song)");
    Entity jimmy = DataAccessForTesting.getTestEntity("Jimmy_Page");
    Entity larry = DataAccessForTesting.getTestEntity("Larry_Page");
    
    PriorProbability pp = new MaterializedPriorProbability(mentions);
    
    double ppKashmirKashmir = pp.getPriorProbability(kashmirMention, kashmir);
    double ppKashmirKashmirSong = pp.getPriorProbability(kashmirMention, kashmirSong);
        
    assertTrue(ppKashmirKashmir > ppKashmirKashmirSong);
    assertEquals(0.9, ppKashmirKashmir, 0.001);
    assertEquals(1.0, ppKashmirKashmir + ppKashmirKashmirSong, 0.001);

    double ppPageJimmy = pp.getPriorProbability(pageMention, jimmy);
    double ppPageLarry = pp.getPriorProbability(pageMention, larry);
    
    assertTrue(ppPageJimmy < ppPageLarry);
    assertEquals(0.3, ppPageJimmy, 0.001);
    assertEquals(1.0, ppPageJimmy + ppPageLarry, 0.001);
  }
}