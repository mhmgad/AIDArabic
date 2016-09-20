package mpi.aida.config.settings.disambiguation;

import java.io.IOException;

import mpi.aida.graph.similarity.exception.MissingSettingException;

/**
 * Preconfigured settings for the {@see Disambiguator} using the mention-entity
 * prior, the keyphrase based similarity, and the MilneWitten Wikipedia link
 * based entity coherence.
 * 
 * It applies additional heuristics to fix entities before the graph algorithm
 * is run, and a threshold to assign Entity.OOKBE to null mentions.
 * 
 * Use this for running on "real world" documents to get the best results. 
 */
public class CocktailPartyWithHeuristicsDisambiguationWithNullSettings extends CocktailPartyDisambiguationWithNullSettings {

  private static final long serialVersionUID = 5867674989478781057L;
    
  public CocktailPartyWithHeuristicsDisambiguationWithNullSettings() throws MissingSettingException, NoSuchMethodException, ClassNotFoundException, IOException {
    super();
    setComputeConfidence(true);

    getGraphSettings().setUseCoherenceRobustnessTest(true);
    getGraphSettings().setCohRobustnessThreshold(1.15);
    getGraphSettings().setUseEasyMentionsTest(true);
    getGraphSettings().setEasyMentionsTestThreshold(5);
    getGraphSettings().setUseConfidenceThresholdTest(true);
    getGraphSettings().setConfidenceTestThreshold(0.9);
    getGraphSettings().setPruneCandidateEntities(true);
    getGraphSettings().setPruneCandidateThreshold(25);
    
    setNullMappingThreshold(0.075);
  }
}