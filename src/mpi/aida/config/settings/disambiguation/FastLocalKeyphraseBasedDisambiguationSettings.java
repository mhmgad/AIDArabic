package mpi.aida.config.settings.disambiguation;

import java.io.IOException;

import mpi.aida.graph.similarity.exception.MissingSettingException;

/**
 * Preconfigured settings for the {@see Disambiguator} using only the 
 * mention-entity prior and the keyphrase based similarity.
 * 
 * Prunes the keyphrases used for disambiguation based on weight and count.
 *  
 * Prunes the candidate entities according to prior (keeps top 20);
 */
public class FastLocalKeyphraseBasedDisambiguationSettings extends LocalKeyphraseBasedDisambiguationSettings {
    
  private static final long serialVersionUID = -1943862223862927646L;

  public FastLocalKeyphraseBasedDisambiguationSettings() throws MissingSettingException, NoSuchMethodException, ClassNotFoundException, IOException {
    super();
    getSimilaritySettings().setMaxEntityKeyphraseCount(1000);
    getSimilaritySettings().setMinimumEntityKeyphraseWeight(0.001);
    setMaxCandidatesPerEntityByPrior(20);
  }
}
