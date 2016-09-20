package mpi.aida.config.settings.disambiguation;

import java.io.IOException;

import mpi.aida.graph.similarity.exception.MissingSettingException;

/**
 * Preconfigured settings for the {@see Disambiguator} using only the 
 * mention-entity prior and the keyphrase based similarity.
 *  
 * Prunes the keyphrases used for disambiguation based on weight and count.
 *  
 * Does thresholding to determine out-of-knowledge-base / null entities.
 *
 * Prunes the candidate entities according to prior (keeps top 20);
 */
public class FastLocalKeyphraseBasedDisambiguationWithNullSettings extends FastLocalKeyphraseBasedDisambiguationSettings {
    
  public static final Double priorWeight = 0.5650733990091601;
    
  private static final long serialVersionUID = -1943862223862927646L;

  public FastLocalKeyphraseBasedDisambiguationWithNullSettings() throws MissingSettingException, NoSuchMethodException, ClassNotFoundException, IOException {
    super();
    setComputeConfidence(true);
    setNullMappingThreshold(0.2);
  }
}
