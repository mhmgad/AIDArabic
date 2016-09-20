package mpi.aida.data;

import static org.junit.Assert.assertEquals;
import mpi.aida.AidaManager;
import mpi.aida.Disambiguator;
import mpi.aida.config.AidaConfig;
import mpi.aida.config.settings.DisambiguationSettings;
import mpi.aida.config.settings.JsonSettings.JSONTYPE;
import mpi.aida.config.settings.PreparationSettings;
import mpi.aida.config.settings.disambiguation.CocktailPartyDisambiguationSettings;
import mpi.aida.preparation.mentionrecognition.MentionsDetector.type;
import mpi.aida.preparator.Preparator;

import org.json.simple.JSONObject;
import org.junit.Test;


public class ResultProcessorTest {
  
  public static final double DEFAULT_ALPHA = 0.6;
  public static final double DEFAULT_COH_ROBUSTNESS = 0.9;
  public static final int DEFAULT_SIZE = 5;

  public ResultProcessorTest() {
    AidaConfig.set("dataAccess", "testing");
    AidaConfig.set(AidaConfig.CACHE_WORD_DATA, "false");
    AidaManager.init();
  }
  
  @Test
  public void test() throws Exception {
    Preparator p = new Preparator();

    String docId = "testPageKashmir1";
    String content = "[[Page]] played [[Kashmir]] at [[Knebworth]].";
    PreparationSettings prepSettings = new PreparationSettings();
    prepSettings.setMentionsDetectionType(type.MANUAL);

    PreparedInput preparedInput = p.prepare(docId, content, prepSettings);

    DisambiguationSettings settings = new CocktailPartyDisambiguationSettings();
    settings.getGraphSettings().setAlpha(DEFAULT_ALPHA);
    settings.getGraphSettings().setCohRobustnessThreshold(DEFAULT_COH_ROBUSTNESS);
    settings.getGraphSettings().setEntitiesPerMentionConstraint(DEFAULT_SIZE);
    settings.setIncludeNullAsEntityCandidate(false);

    Disambiguator d = new Disambiguator(preparedInput, settings);

    DisambiguationResults results = d.disambiguate();
   
    ResultProcessor rp = new ResultProcessor(results, docId, preparedInput);
    JSONObject json = rp.process(JSONTYPE.ANNOTATED_TEXT);
    // This line actually relies on correct disambiguation as well.
    assertEquals("[[TESTING:Jimmy_Page|Page]] played [[TESTING:Kashmir_(song)|Kashmir]] at [[TESTING:Knebworth_Festival|Knebworth]].", json.get("annotatedText"));
    assertEquals("Page played Kashmir at Knebworth.", json.get("originalText"));
  }
}