package mpi.aida.config.settings.disambiguation;

import java.io.IOException;

import mpi.aida.graph.similarity.exception.MissingSettingException;

/**
 * Preconfigured settings for the {@see Disambiguator} using only the 
 * keyphrase based similarity based only on idf counts.
 * 
 * Also does thresholding to determine out-of-knowledge-base / null entities.
 */
public class LocalKeyphraseIDFBasedDisambiguationWithNullSettings extends LocalKeyphraseIDFBasedDisambiguationSettings {

  private static final long serialVersionUID = -8458216249693970790L;

  public LocalKeyphraseIDFBasedDisambiguationWithNullSettings() throws MissingSettingException, NoSuchMethodException, ClassNotFoundException, IOException {
    super();
    setComputeConfidence(true);
    setNullMappingThreshold(0.05);
  }
}
