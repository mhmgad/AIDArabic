package mpi.aida.config.settings.disambiguation;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import mpi.aida.graph.similarity.exception.MissingSettingException;


public class CocktailPartyJaccardDisambiguationSettings extends
    CocktailPartyDisambiguationSettings {

  private static final long serialVersionUID = 6766852737067667775L;

  public CocktailPartyJaccardDisambiguationSettings()
    throws MissingSettingException, NoSuchMethodException, ClassNotFoundException, IOException {
    super();
    List<String[]> cohConfigs = new LinkedList<String[]>();
    cohConfigs.add(new String[] { "InlinkOverlapEntityEntitySimilarity", "1.0" });
    getSimilaritySettings().setEntityEntitySimilarities(cohConfigs);
  }  
}
