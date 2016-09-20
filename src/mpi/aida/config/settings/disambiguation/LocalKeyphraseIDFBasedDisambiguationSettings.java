package mpi.aida.config.settings.disambiguation;

import java.io.IOException;
import java.util.Properties;

import mpi.aida.config.settings.DisambiguationSettings;
import mpi.aida.config.settings.Settings.TECHNIQUE;
import mpi.aida.graph.similarity.exception.MissingSettingException;
import mpi.aida.graph.similarity.util.SimilaritySettings;
import mpi.aida.util.ClassPathUtils;

/**
 * Preconfigured settings for the {@see Disambiguator} using only the 
 * keyphrase based similarity based only on idf counts.
 */
public class LocalKeyphraseIDFBasedDisambiguationSettings extends DisambiguationSettings {

  private static final long serialVersionUID = -6391627336407534940L;

  public LocalKeyphraseIDFBasedDisambiguationSettings() throws MissingSettingException, NoSuchMethodException, ClassNotFoundException, IOException {
    setDisambiguationTechnique(TECHNIQUE.LOCAL);  
    
    Properties prop = ClassPathUtils.getPropertiesFromClasspath("similarity/conll/KeyphraseIDF.properties");
    SimilaritySettings localIDFPsettings = new SimilaritySettings(prop, "KeyphraseIDF");
    setSimilaritySettings(localIDFPsettings);
  }
}
