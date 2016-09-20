package mpi.aida;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mpi.aida.config.AidaConfig;
import mpi.aida.config.settings.DisambiguationSettings;
import mpi.aida.config.settings.PreparationSettings;
import mpi.aida.config.settings.disambiguation.CocktailPartyDisambiguationSettings;
import mpi.aida.config.settings.disambiguation.LocalKeyphraseBasedDisambiguationSettings;
import mpi.aida.data.DisambiguationResults;
import mpi.aida.data.Entity;
import mpi.aida.data.ExternalEntitiesContext;
import mpi.aida.data.KBIdentifiedEntity;
import mpi.aida.data.PreparedInput;
import mpi.aida.data.ResultEntity;
import mpi.aida.data.ResultMention;
import mpi.aida.preparation.mentionrecognition.MentionsDetector.type;
import mpi.aida.preparator.Preparator;
import mpi.tokenizer.data.TokenizerManager;

import org.junit.Test;

/**
 * Testing against the predefined DataAccessForTesting.
 * 
 */
public class DisambiguatorTest {
  public static final double DEFAULT_ALPHA = 0.6;
  public static final double DEFAULT_COH_ROBUSTNESS = 0.9;
  public static final int DEFAULT_SIZE = 5;

  public DisambiguatorTest() {
    AidaConfig.set("dataAccess", "testing");
    AidaConfig.set(AidaConfig.CACHE_WORD_DATA, "false");
    AidaConfig.set(AidaConfig.RECONCILER_PERSON_MERGE, "false");
    AidaManager.init();
  }

  @Test
  public void testPageKashmir() throws Exception {
    Preparator p = new Preparator();

    String docId = "testPageKashmir1";
    String content = "When [[Page]] played Kashmir at Knebworth, his Les Paul was uniquely tuned.";
    PreparationSettings prepSettings = new PreparationSettings();
    prepSettings.setTokenizerType(TokenizerManager.TokenizationType.ENGLISH_TOKENS);
    prepSettings.setMentionsDetectionType(type.AUTOMATIC_AND_MANUAL);

    PreparedInput preparedInput = p.prepare(docId, content, new PreparationSettings());

    DisambiguationSettings settings = new CocktailPartyDisambiguationSettings();
    settings.getGraphSettings().setAlpha(DEFAULT_ALPHA);
    settings.getGraphSettings().setCohRobustnessThreshold(DEFAULT_COH_ROBUSTNESS);
    settings.getGraphSettings().setEntitiesPerMentionConstraint(DEFAULT_SIZE);
    settings.setIncludeNullAsEntityCandidate(false);

    Disambiguator d = new Disambiguator(preparedInput, settings);

    DisambiguationResults results = d.disambiguate();

    Map<String, String> mappings = repackageMappings(results);

    String mapped = mappings.get("Page");
    assertEquals("Jimmy_Page", mapped);

    mapped = mappings.get("Kashmir");
    assertEquals("Kashmir_(song)", mapped);

    mapped = mappings.get("Knebworth");
    assertEquals("Knebworth_Festival", mapped);

    mapped = mappings.get("Les Paul");
    assertEquals(Entity.OOKBE, mapped);
  }

  @Test
  public void testNoMaxEntityRank() throws Exception {
    Preparator p = new Preparator();

    String docId = "testPageKashmir2";
    String content = "When [[Page]] played Kashmir at Knebworth, his Les Paul was uniquely tuned.";
    PreparationSettings prepSettings = new PreparationSettings();
    prepSettings.setTokenizerType(TokenizerManager.TokenizationType.ENGLISH_TOKENS);
    prepSettings.setMentionsDetectionType(type.AUTOMATIC_AND_MANUAL);
    
    PreparedInput preparedInput = p.prepare(docId, content, new PreparationSettings());

    DisambiguationSettings settings = new CocktailPartyDisambiguationSettings();
    settings.setIncludeNullAsEntityCandidate(false);
    settings.getGraphSettings().setAlpha(DEFAULT_ALPHA);
    settings.getGraphSettings().setCohRobustnessThreshold(DEFAULT_COH_ROBUSTNESS);
    settings.getGraphSettings().setEntitiesPerMentionConstraint(DEFAULT_SIZE);
    settings.setMaxEntityRank(-0.1);

    Disambiguator d = new Disambiguator(preparedInput, settings);

    DisambiguationResults results = d.disambiguate();

    Map<String, String> mappings = repackageMappings(results);

    String mapped = mappings.get("Page");
    assertEquals(Entity.OOKBE, mapped);

    mapped = mappings.get("Kashmir");
    assertEquals(Entity.OOKBE, mapped);

    mapped = mappings.get("Knebworth");
    assertEquals(Entity.OOKBE, mapped);

    mapped = mappings.get("Les Paul");
    assertEquals(Entity.OOKBE, mapped);
  }

  @Test
  public void testTopMaxEntityRank() throws Exception {
    Preparator p = new Preparator();

    String docId = "testPageKashmir3";
    String content = "When [[Page]] played Kashmir at Knebworth, his Les Paul was uniquely tuned.";
    PreparationSettings prepSettings = new PreparationSettings();
    prepSettings.setTokenizerType(TokenizerManager.TokenizationType.ENGLISH_TOKENS);
    prepSettings.setMentionsDetectionType(type.AUTOMATIC_AND_MANUAL);

    PreparedInput preparedInput = p.prepare(docId, content, new PreparationSettings());

    DisambiguationSettings settings = new CocktailPartyDisambiguationSettings();
    settings.getGraphSettings().setAlpha(DEFAULT_ALPHA);
    settings.getGraphSettings().setCohRobustnessThreshold(DEFAULT_COH_ROBUSTNESS);
    settings.getGraphSettings().setEntitiesPerMentionConstraint(DEFAULT_SIZE);
    settings.setMaxEntityRank(0.8);
    settings.setIncludeNullAsEntityCandidate(false);

    Disambiguator d = new Disambiguator(preparedInput, settings);

    DisambiguationResults results = d.disambiguate();

    Map<String, String> mappings = repackageMappings(results);

    String mapped = mappings.get("Page");
    assertEquals("Jimmy_Page", mapped);

    mapped = mappings.get("Kashmir");
    assertEquals("Kashmir_(song)", mapped);

    mapped = mappings.get("Knebworth");
    assertEquals(Entity.OOKBE, mapped);

    mapped = mappings.get("Les Paul");
    assertEquals(Entity.OOKBE, mapped);
  }

  @Test
  public void testExternalEntitiesDictionary() throws Exception {
    Preparator p = new Preparator();

    String docId = "testPageKashmir4";
    String content = "When [[Page]] played [[Kashmir]] at [[Knebworth]], his Les Paul was uniquely tuned.";
    PreparationSettings prepSettings = new PreparationSettings();
    prepSettings.setMentionsDetectionType(type.MANUAL);

    PreparedInput preparedInput = p.prepare(docId, content, new PreparationSettings());

    DisambiguationSettings settings = new LocalKeyphraseBasedDisambiguationSettings();

    Map<String, List<KBIdentifiedEntity>> dictionary = new HashMap<>();
    List<KBIdentifiedEntity> pageCands = new ArrayList<>();
    pageCands.add(new KBIdentifiedEntity("page1", "EXTERNAL"));
    pageCands.add(new KBIdentifiedEntity("page2", "EXTERNAL"));
    dictionary.put("Page", pageCands);
    List<KBIdentifiedEntity> kashmirCands = new ArrayList<>();
    kashmirCands.add(new KBIdentifiedEntity("kashmir1", "EXTERNAL"));
    dictionary.put("Kashmir", kashmirCands);

    ExternalEntitiesContext externalContext =
        new ExternalEntitiesContext(dictionary, new HashMap<KBIdentifiedEntity, List<String>>());

    Disambiguator d = new Disambiguator(preparedInput, settings, externalContext);
    DisambiguationResults results = d.disambiguate();

    for (ResultMention rm : results.getResultMentions()) {
      if (rm.getMention().equals("Page")) {
        int count = 0;
        List<ResultEntity> pages = results.getResultEntities(rm);
        for (ResultEntity re : pages) {
          if (re.getKbEntity().getIdentifier().equals("page1") ||
              re.getKbEntity().getIdentifier().equals("page2")) {
            ++count;
          }
        }
        assertEquals(2, count);
      }
      if (rm.getMention().equals("Kashmir")) {
        int count = 0;
        List<ResultEntity> pages = results.getResultEntities(rm);
        for (ResultEntity re : pages) {
          if (re.getKbEntity().getIdentifier().equals("kashmir1")) {
            ++count;
          }
        }
        assertEquals(1, count);
      }
      if (rm.getMention().equals("Knebworth")) {
        int count = 0;
        List<ResultEntity> pages = results.getResultEntities(rm);
        for (ResultEntity re : pages) {
          if (re.getKbEntity().getIdentifier().equals("page1") ||
              re.getKbEntity().getIdentifier().equals("page2") ||
              re.getKbEntity().getIdentifier().equals("kashmir1")) {
            ++count;
          }
        }
        assertEquals(0, count);
      }
    }
  }

  @Test
  public void testExternalEntitiesKeyphrases() throws Exception {
    Preparator p = new Preparator();

    String docId = "testPageKashmir5";
    String content = "[[AIDA]] is the entity disambiguation software by MPI, not the musical.";
    PreparationSettings prepSettings = new PreparationSettings();
    prepSettings.setMentionsDetectionType(type.MANUAL);

    Map<String, List<KBIdentifiedEntity>> dictionary = new HashMap<>();
    List<KBIdentifiedEntity> aidaCands = new ArrayList<>();
    KBIdentifiedEntity aidaMPI = new KBIdentifiedEntity("AIDA-MPI", "EXTERNAL");
    KBIdentifiedEntity aidaWrong = new KBIdentifiedEntity("AIDA-WRONG", "EXTERNAL");
    KBIdentifiedEntity aidaNoMatch = new KBIdentifiedEntity("AIDA-NOMATCH", "EXTERNAL");
    aidaCands.add(aidaMPI);
    aidaCands.add(aidaWrong);
    aidaCands.add(aidaNoMatch);
    dictionary.put("AIDA", aidaCands);

    Map<KBIdentifiedEntity, List<String>> entityKeyphrases =
        new HashMap<>();
        List<String> aidaMPIKeyphrases = Arrays.asList(
            new String[]{"Google", "entity disambiguation framework",
                "MPI", "software"});
        entityKeyphrases.put(aidaMPI, aidaMPIKeyphrases);
        List<String> aidaWrongKeyphrases = Arrays.asList(
            new String[]{"musical"});
        entityKeyphrases.put(aidaWrong, aidaWrongKeyphrases);
        List<String> aidaNoMatchKeyphrases = new ArrayList<>();
        entityKeyphrases.put(aidaNoMatch, aidaNoMatchKeyphrases);

        ExternalEntitiesContext externalContext =
            new ExternalEntitiesContext(dictionary, entityKeyphrases);

        PreparedInput preparedInput = p.prepare(docId, content, new PreparationSettings(), externalContext);

        DisambiguationSettings settings = new LocalKeyphraseBasedDisambiguationSettings();

        Disambiguator d = new Disambiguator(preparedInput, settings, externalContext);
        DisambiguationResults results = d.disambiguate();

        for (ResultMention rm : results.getResultMentions()) {
          if (rm.getMention().equals("AIDA")) {
            int count = 0;
            List<ResultEntity> pages = results.getResultEntities(rm);
            ResultEntity best = null;
            for (ResultEntity re : pages) {
              if (re.getKbEntity().getIdentifier().equals("AIDA-MPI") ||
                  re.getKbEntity().getIdentifier().equals("AIDA-WRONG") ||
                  re.getKbEntity().getIdentifier().equals("AIDA-NOMATCH")) {
                ++count;
              }
              if (re.getKbEntity().getIdentifier().equals("AIDA-MPI")) {
                best = re;
              }
              if (re.getKbEntity().getIdentifier().equals("AIDA-WRONG")) {
                double score = re.getDisambiguationScore();
                assertTrue(score > 0.0);
              }
              if (re.getKbEntity().getIdentifier().equals("AIDA-NOMATCH")) {
                double score = re.getDisambiguationScore();
                assertEquals(0.0, score, 0.001);
              }
            }
            assertEquals("AIDA-MPI", best.getEntity());
            assertEquals(3, count);
          }
        }
  }

  public static Map<String, String> repackageMappings(DisambiguationResults results) {
    Map<String, String> repack = new HashMap<String, String>();

    for (ResultMention rm : results.getResultMentions()) {
      repack.put(rm.getMention(), results.getBestEntity(rm).getEntity());
    }

    return repack;
  }
}
