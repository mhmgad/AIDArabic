package mpi.aida.graph.similarity;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import mpi.aida.AidaManager;
import mpi.aida.access.DataAccessForTesting;
import mpi.aida.config.AidaConfig;
import mpi.aida.data.Context;
import mpi.aida.data.Entities;
import mpi.aida.data.Entity;
import mpi.aida.data.ExternalEntitiesContext;
import mpi.aida.data.Mention;
import mpi.aida.data.Mentions;
import mpi.aida.graph.similarity.util.SimilaritySettings;
import mpi.aida.preparation.lookup.EntityLookupManager;
import mpi.experiment.trace.NullTracer;
import mpi.experiment.trace.Tracer;

import mpi.tokenizer.data.Tokens;
import org.junit.Test;


public class EnsembleMentionEntitySimilarityTest {

  public EnsembleMentionEntitySimilarityTest() {
    AidaConfig.set("dataAccess", "testing");
    AidaConfig.set(AidaConfig.CACHE_WORD_DATA, "false");
    AidaManager.init();
  }

  @Test
  public void test() throws Exception {
    // All caps PLAYED to check if term expansion is working.
    String text =
        "When Page PLAYED Kashmir at Knebworth , his Les Paul was uniquely tuned .";

    Context context = new Context(new Tokens(Arrays.asList(text.split(" "))));

    String n1 = "Kashmir";
    String n2 = "Kashmir_(song)";
    String n3 = "Jimmy_Page";

    Entity e1 = DataAccessForTesting.getTestEntity(n1);
    Entity e2 = DataAccessForTesting.getTestEntity(n2);
    Entity e3 = DataAccessForTesting.getTestEntity(n3);

    Entities entities = new Entities();
    entities.add(e1);
    entities.add(e2);
    entities.add(e3);

    Tracer tracer = new NullTracer();

    List<SimilaritySettings.MentionEntitySimilarityRaw> simConfigsnNoPrior = new LinkedList<>();
    simConfigsnNoPrior.add(new SimilaritySettings.MentionEntitySimilarityRaw("UnnormalizedKeyphrasesBasedMISimilarity", "KeyphrasesContext", 0.95, false));
    simConfigsnNoPrior.add(new SimilaritySettings.MentionEntitySimilarityRaw("UnnormalizedKeyphrasesBasedIDFSimilarity", "KeyphrasesContext", 0.05, false));
    List<SimilaritySettings.MentionEntitySimilarityRaw> simConfigsWithPrior = new LinkedList<>();
    simConfigsWithPrior.add(new SimilaritySettings.MentionEntitySimilarityRaw("UnnormalizedKeyphrasesBasedMISimilarity", "KeyphrasesContext", 0.475, false));
    simConfigsWithPrior.add(new SimilaritySettings.MentionEntitySimilarityRaw("UnnormalizedKeyphrasesBasedIDFSimilarity", "KeyphrasesContext", 0.025, false));

    List<String[]> eeSimConfigs = new LinkedList<>();

    eeSimConfigs.add(new String[] { "MilneWittenEntityEntitySimilarity", "1.0" });

    double priorWeight = 0.5;

    SimilaritySettings settings =
        new SimilaritySettings(
          simConfigsWithPrior, eeSimConfigs, priorWeight);
    settings.setPriorThreshold(0.8);
    settings.setMentionEntitySimilaritiesNoPrior(simConfigsnNoPrior);

    EntityLookupManager entityLookup = EntityLookupManager.singleton();

    Mentions ms = new Mentions();
    Mention m1 = new Mention();
    m1.setMention("Page");
    m1.setStartToken(1);
    m1.setEndToken(1);
    ms.addMention(m1);
    Mention m2 = new Mention();
    m2.setMention("Kashmir");
    m2.setStartToken(3);
    m2.setEndToken(3);
    ms.addMention(m2);
    entityLookup.fillInCandidateEntities(ms);
    for (Mention m : ms.getMentions()) {
      entities.addAll(m.getCandidateEntities());
    }

    EnsembleMentionEntitySimilarity emes = new EnsembleMentionEntitySimilarity(ms, entities, context, new ExternalEntitiesContext(), settings, tracer);

    double simPage = emes.calcSimilarity(m1, context, e3);
    double simKashmir = emes.calcSimilarity(m2, context, e2);

    assertEquals(1.0, simPage, 0.000000001);
    assertEquals(0.12748, simKashmir, 0.00001);
  }
}
