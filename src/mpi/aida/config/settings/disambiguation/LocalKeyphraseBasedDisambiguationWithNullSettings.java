package mpi.aida.config.settings.disambiguation;

import java.io.IOException;

import mpi.aida.graph.similarity.exception.MissingSettingException;

/**
 * Preconfigured settings for the {@see Disambiguator} using only the 
 * mention-entity prior and the keyphrase based similarity.
 *  
 * Also does thresholding to determine out-of-knowledge-base / null entities.
 */
public class LocalKeyphraseBasedDisambiguationWithNullSettings extends LocalKeyphraseBasedDisambiguationSettings {
    
  public static final Double priorWeight = 0.5650733990091601;
    
  private static final long serialVersionUID = -1943862223862927646L;

  public LocalKeyphraseBasedDisambiguationWithNullSettings() throws MissingSettingException, NoSuchMethodException, ClassNotFoundException, IOException {
    super();
    setComputeConfidence(true);
    setNullMappingThreshold(0.05);
  }
}
