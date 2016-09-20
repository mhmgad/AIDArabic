package mpi.aida.config.settings.disambiguation;

import java.io.IOException;

import mpi.aida.graph.similarity.exception.MissingSettingException;

/**
 * Preconfigured settings for the {@see Disambiguator} using the mention-entity
 * prior, the keyphrase based similarity, and the KORE keyphrase based
 * entity coherence.
 * 
 * Also does thresholding to determine out-of-knowledge-base / null entities. 
 */
public class CocktailPartyKOREDisambiguationWithNullSettings extends CocktailPartyKOREDisambiguationSettings {
    
  private static final long serialVersionUID = 5867674989478781057L;

  public CocktailPartyKOREDisambiguationWithNullSettings() throws MissingSettingException, NoSuchMethodException, ClassNotFoundException, IOException {
    super();
    setComputeConfidence(true);
    setNullMappingThreshold(0.05);
  }
}