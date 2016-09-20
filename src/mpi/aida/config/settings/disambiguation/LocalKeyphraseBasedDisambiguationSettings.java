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
 * mention-entity prior and the keyphrase based similarity.
 */
public class LocalKeyphraseBasedDisambiguationSettings extends DisambiguationSettings {

  private static final long serialVersionUID = -1943862223862927646L;
  
  public LocalKeyphraseBasedDisambiguationSettings() throws MissingSettingException, NoSuchMethodException, ClassNotFoundException, IOException {
    setDisambiguationTechnique(TECHNIQUE.LOCAL);
    
    Properties prop = ClassPathUtils.getPropertiesFromClasspath("similarity/conll/SwitchedKP.properties");
    SimilaritySettings switchedKPsettings = new SimilaritySettings(prop, "SwitchedKP");
    setSimilaritySettings(switchedKPsettings);
  }
}
