package mpi.aida.config.settings.disambiguation;

import mpi.aida.config.AidaConfig;
import mpi.aida.config.settings.DisambiguationSettings;
import mpi.aida.config.settings.Settings;
import mpi.aida.graph.similarity.util.SimilaritySettings;
import mpi.aida.util.ClassPathUtils;
import mpi.experiment.trace.GraphTracer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Preconfigured settings for the {@see Disambiguator} using the mention-entity
 * prior, the language model context similarity, and the MilneWitten Wikipedia link
 * based entity coherence.
 *
 * It applies additional heuristics to fix entities before the graph algorithm
 * is run, and a threshold to assign Entity.OOKBE to null mentions.
 *
 * Use this for running on "real world" documents to get the best results.
 */
public class CocktailPartyLangaugeModelDefaultDisambiguationSettings extends DisambiguationSettings {

  public CocktailPartyLangaugeModelDefaultDisambiguationSettings() throws IOException, NoSuchMethodException, ClassNotFoundException {
    setTracingTarget(GraphTracer.TracingTarget.WEB_INTERFACE);

    setDisambiguationTechnique(Settings.TECHNIQUE.GRAPH);
    setDisambiguationAlgorithm(Settings.ALGORITHM.COCKTAIL_PARTY_SIZE_CONSTRAINED);

    getGraphSettings().setUseExhaustiveSearch(true);
    getGraphSettings().setUseNormalizedObjective(true);

    String trainingCorpus = AidaConfig.get(AidaConfig.TRAINING_CORPUS);

    Properties switchedUnitProp = ClassPathUtils.getPropertiesFromClasspath("similarity/" + trainingCorpus + "/SwitchedUnit.properties");
    SimilaritySettings switchedUnitSettings = new SimilaritySettings(switchedUnitProp, "SwitchedUnit");
    List<String[]> cohConfigs = new ArrayList<>();
    cohConfigs.add(new String[] { "MilneWittenEntityEntitySimilarity", "1.0" });
    switchedUnitSettings.setEntityEntitySimilarities(cohConfigs);
    setSimilaritySettings(switchedUnitSettings);

    Properties cohRobProp = ClassPathUtils.getPropertiesFromClasspath("similarity/" + trainingCorpus +"/SwitchedUnit_cohrob.properties");
    SimilaritySettings unnormalizedKPsettings = new SimilaritySettings(cohRobProp, "CoherenceRobustnessTest");
    getGraphSettings().setCoherenceSimilaritySetting(unnormalizedKPsettings);

    setComputeConfidence(true);

    // Default hyperparameters are trained on CoNLL.
    double alpha = 0.65;
    double cohRobThresh = 1.05;
    double confTestThresh = 0.55;
    double nullThresh = 0.075;

    switch (trainingCorpus) {
      case "spiegel":
        alpha = 1.0;
        cohRobThresh = 0.91;
        confTestThresh = 0.65;
        nullThresh = 0.073;
    }

    getGraphSettings().setAlpha(alpha);
    getGraphSettings().setUseCoherenceRobustnessTest(true);
    getGraphSettings().setCohRobustnessThreshold(cohRobThresh);
    getGraphSettings().setUseEasyMentionsTest(true);
    getGraphSettings().setEasyMentionsTestThreshold(5);
    getGraphSettings().setUseConfidenceThresholdTest(true);
    getGraphSettings().setConfidenceTestThreshold(confTestThresh);
    getGraphSettings().setPruneCandidateEntities(true);
    getGraphSettings().setPruneCandidateThreshold(25);
    setNullMappingThreshold(nullThresh);
  }
}
