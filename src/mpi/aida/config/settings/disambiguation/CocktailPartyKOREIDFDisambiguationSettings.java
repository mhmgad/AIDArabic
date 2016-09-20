package mpi.aida.config.settings.disambiguation;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import mpi.aida.access.DataAccess;
import mpi.aida.graph.similarity.exception.MissingSettingException;
import mpi.aida.graph.similarity.util.SimilaritySettings;
import mpi.aida.graph.similarity.util.SimilaritySettings.ImportanceAggregationStrategy;
import mpi.aida.util.ClassPathUtils;

/**
 * Preconfigured settings for the {@see Disambiguator} using the mention-entity
 * the keyphrase based similarity using idf scores only, and the KORE keyphrase based
 * entity coherence.
 */
public class CocktailPartyKOREIDFDisambiguationSettings extends CocktailPartyDisambiguationSettings {
    
  private static final long serialVersionUID = 5867674989478781057L;
 
  public CocktailPartyKOREIDFDisambiguationSettings() throws MissingSettingException, NoSuchMethodException, ClassNotFoundException, IOException {
    super();
    getGraphSettings().setUseCoherenceRobustnessTest(false);
    
    List<String[]> cohConfigs = new LinkedList<String[]>();
    cohConfigs.add(new String[] { "KOREEntityEntitySimilarity", "1.0" });

    Properties switchedKpProp = ClassPathUtils.getPropertiesFromClasspath("similarity/conll/KeyphraseIDF.properties");
    SimilaritySettings settings = new SimilaritySettings(switchedKpProp, "KeyphraseIDF");
    settings.setEntityEntitySimilarities(cohConfigs);
    settings.setEntityCohKeyphraseAlpha(0.0);
    settings.setEntityCohKeywordAlpha(0.0);
    settings.setShouldNormalizeCoherenceWeights(true);
    setSimilaritySettings(settings);
  }
}