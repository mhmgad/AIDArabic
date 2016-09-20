package mpi.aida.config.settings.disambiguation;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import mpi.aida.config.AidaConfig;
import mpi.aida.config.settings.DisambiguationSettings;
import mpi.aida.config.settings.Settings.ALGORITHM;
import mpi.aida.config.settings.Settings.TECHNIQUE;
import mpi.aida.graph.similarity.exception.MissingSettingException;
import mpi.aida.graph.similarity.util.SimilaritySettings;
import mpi.aida.util.ClassPathUtils;
import mpi.experiment.trace.GraphTracer.TracingTarget;

/**
 * Preconfigured settings for the {@see Disambiguator} using the mention-entity
 * prior, the keyphrase based similarity, and the MilneWitten Wikipedia link
 * based entity coherence.
 * 
 * This gives the best quality and should be used in comparing results against
 * AIDA. 
 */
public class CocktailPartyDisambiguationSettings extends DisambiguationSettings {
    
  private static final long serialVersionUID = 5867674989478781057L;
  
  public CocktailPartyDisambiguationSettings() throws MissingSettingException, NoSuchMethodException, ClassNotFoundException, IOException {
    getGraphSettings().setAlpha(0.6);
    setTracingTarget(TracingTarget.WEB_INTERFACE);
     
    setDisambiguationTechnique(TECHNIQUE.GRAPH);
    setDisambiguationAlgorithm(ALGORITHM.COCKTAIL_PARTY_SIZE_CONSTRAINED);   
    
    getGraphSettings().setUseExhaustiveSearch(true);
    getGraphSettings().setUseNormalizedObjective(true);
    getGraphSettings().setEntitiesPerMentionConstraint(5);
    getGraphSettings().setUseCoherenceRobustnessTest(true);
    getGraphSettings().setCohRobustnessThreshold(0.9);

    String trainingCorpus = AidaConfig.get(AidaConfig.TRAINING_CORPUS);

    Properties switchedKpProp = ClassPathUtils.getPropertiesFromClasspath("similarity/" + trainingCorpus + "/SwitchedKP.properties");
    SimilaritySettings switchedKPsettings = new SimilaritySettings(switchedKpProp, "SwitchedKP");
    List<String[]> cohConfigs = new LinkedList<String[]>();
    cohConfigs.add(new String[] { "MilneWittenEntityEntitySimilarity", "1.0" });
    switchedKPsettings.setEntityEntitySimilarities(cohConfigs);
    setSimilaritySettings(switchedKPsettings);

    Properties cohRobProp = ClassPathUtils.getPropertiesFromClasspath("similarity/" +trainingCorpus + "/SwitchedKP_cohrob.properties");
    SimilaritySettings unnormalizedKPsettings = new SimilaritySettings(cohRobProp, "CoherenceRobustnessTest");
    getGraphSettings().setCoherenceSimilaritySetting(unnormalizedKPsettings);
  }
}