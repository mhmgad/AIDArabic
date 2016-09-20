package mpi.aida.config.settings.disambiguation;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import mpi.aida.access.DataAccess;
import mpi.aida.graph.similarity.exception.MissingSettingException;
import mpi.aida.graph.similarity.util.SimilaritySettings;

/**
 * Preconfigured settings for the {@see Disambiguator} using the mention-entity
 * prior, the keyphrase based similarity, and the KORE keyphrase based
 * entity coherence.
 */
public class CocktailPartyKOREDisambiguationSettings extends CocktailPartyDisambiguationSettings {
    
  private static final long serialVersionUID = 5867674989478781057L;

  public CocktailPartyKOREDisambiguationSettings() throws MissingSettingException, NoSuchMethodException, ClassNotFoundException, IOException {
    super();
    List<String[]> cohConfigs = new LinkedList<String[]>();
    cohConfigs.add(new String[] { "KOREEntityEntitySimilarity", "1.0" });

    SimilaritySettings switchedKPsettings = getSimilaritySettings();
    switchedKPsettings.setEntityEntitySimilarities(cohConfigs);
    switchedKPsettings.setEntityCohKeyphraseAlpha(1.0);
    switchedKPsettings.setEntityCohKeywordAlpha(0.0);
    switchedKPsettings.setShouldNormalizeCoherenceWeights(true);
    List<String[]> sourceWeightConfigs = new LinkedList<String[]>();
    sourceWeightConfigs.add(new String[] { DataAccess.KPSOURCE_INLINKTITLE, "0.0" });
    switchedKPsettings.setEntityEntityKeyphraseSourceWeights(sourceWeightConfigs);
  }
}