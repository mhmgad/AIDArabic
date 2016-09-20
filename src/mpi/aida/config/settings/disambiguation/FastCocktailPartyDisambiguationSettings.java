package mpi.aida.config.settings.disambiguation;

import java.io.IOException;

import mpi.aida.graph.similarity.exception.MissingSettingException;

/**
 * Preconfigured settings for the {@see Disambiguator} using the mention-entity
 * prior, the keyphrase based similarity, and the MilneWitten Wikipedia link
 * based entity coherence.
 *
 * Improves the performance by pruning keyphrases based on weight and rank.
 *
 * Also prunes candidate entities by prior (top 20 are kept).
 *
 */
public class FastCocktailPartyDisambiguationSettings extends CocktailPartyDisambiguationSettings {
    
  private static final long serialVersionUID = 5867674989478781057L;

  public FastCocktailPartyDisambiguationSettings() throws MissingSettingException, NoSuchMethodException, ClassNotFoundException, IOException {
    super();
    getSimilaritySettings().setMaxEntityKeyphraseCount(1000);
    getSimilaritySettings().setMinimumEntityKeyphraseWeight(0.001);
    getGraphSettings().getCoherenceSimilaritySetting().setMaxEntityKeyphraseCount(1000);
    getGraphSettings().getCoherenceSimilaritySetting().setMinimumEntityKeyphraseWeight(0.001);
    setMaxCandidatesPerEntityByPrior(20);
  }
}