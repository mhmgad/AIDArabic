package mpi.aida.graph.algorithms;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import mpi.aida.AidaManager;
import mpi.aida.access.DataAccessForTesting;
import mpi.aida.config.AidaConfig;
import mpi.aida.config.settings.DisambiguationSettings;
import mpi.aida.config.settings.PreparationSettings;
import mpi.aida.config.settings.disambiguation.CocktailPartyDisambiguationSettings;
import mpi.aida.config.settings.preparation.StanfordHybridPreparationSettings;
import mpi.aida.data.Entities;
import mpi.aida.data.Entity;
import mpi.aida.data.PreparedInput;
import mpi.aida.data.PreparedInputChunk;
import mpi.aida.data.ResultEntity;
import mpi.aida.data.ResultMention;
import mpi.aida.preparation.lookup.EntityLookupManager;
import mpi.aida.preparator.Preparator;
import mpi.experiment.trace.NullTracer;
import mpi.experiment.trace.Tracer;

import org.junit.Test;

public class CocktailPartySizeConstrainedTest {

  public CocktailPartySizeConstrainedTest() {
    AidaConfig.set("dataAccess", "testing");
    AidaConfig.set(AidaConfig.CACHE_WORD_DATA, "false");
    AidaManager.init();
  }

  @Test
  public void testCocktailParty() throws Exception {

    String text = "When [[Page]] played Kashmir at Knebworth, his Les Paul was uniquely tuned.";

    String e1 = "Kashmir";
    String e2 = "Kashmir_(song)";
    String e3 = "Jimmy_Page";

    Entities entities = new Entities();
    entities.add(DataAccessForTesting.getTestEntity(e1));
    entities.add(DataAccessForTesting.getTestEntity(e2));
    entities.add(DataAccessForTesting.getTestEntity(e3));

    PreparationSettings prepSettings = new StanfordHybridPreparationSettings();

    Tracer tracer = new NullTracer();

    Preparator p = new Preparator();
    PreparedInput input = p.prepare("test", text, prepSettings);
    PreparedInputChunk chunk = input.iterator().next();

    DisambiguationSettings disSettings = new CocktailPartyDisambiguationSettings();
    disSettings.setComputeConfidence(false);

    EntityLookupManager entityLookup = EntityLookupManager.singleton();
    entityLookup.fillInCandidateEntities(chunk.getMentions(),
        disSettings.isIncludeNullAsEntityCandidate(),
        disSettings.isIncludeContextMentions(), disSettings.getMaxEntityRank());

    DisambiguationAlgorithm da = new CocktailPartySizeConstrained(chunk, disSettings, tracer);

    Map<ResultMention, List<ResultEntity>> results = da.disambiguate();
    Map<String, ResultEntity> mappings = repackageMappings(results);

    String mapped = mappings.get("Page").getEntity();
    double score = mappings.get("Page").getDisambiguationScore();
    assertEquals("Jimmy_Page", mapped);
    assertEquals(0.6, score, 0.00001);

    mapped = mappings.get("Kashmir").getEntity();
    score = mappings.get("Kashmir").getDisambiguationScore();
    assertEquals("Kashmir_(song)", mapped);
    assertEquals(0.08787, score, 0.00001);

    mapped = mappings.get("Knebworth").getEntity();
    score = mappings.get("Knebworth").getDisambiguationScore();
    assertEquals("Knebworth_Festival", mapped);
    assertEquals(0.33910, score, 0.00001);

    mapped = mappings.get("Les Paul").getEntity();
    score = mappings.get("Les Paul").getDisambiguationScore();
    assertEquals(Entity.OOKBE, mapped);
    assertEquals(0.0, score, 0.00001);

  }

  private Map<String, ResultEntity> repackageMappings(Map<ResultMention, List<ResultEntity>> results) {
    Map<String, ResultEntity> repack = new HashMap<String, ResultEntity>();

    for(Entry<ResultMention, List<ResultEntity>> entry: results.entrySet()) {
      repack.put(entry.getKey().getMention(), entry.getValue().get(0));
    }
    return repack;
  }

}
