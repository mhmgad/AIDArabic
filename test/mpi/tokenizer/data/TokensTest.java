package mpi.tokenizer.data;

import static org.junit.Assert.assertEquals;

import java.util.List;

import mpi.aida.AidaManager;
import mpi.aida.config.AidaConfig;
import mpi.aida.config.settings.preparation.ManualPreparationSettings;
import mpi.aida.data.PreparedInput;
import mpi.aida.preparator.Preparator;

import org.junit.Test;


public class TokensTest {

  public TokensTest() {
    AidaConfig.set("dataAccess", "testing");
    AidaConfig.set(AidaConfig.CACHE_WORD_DATA, "false");
    AidaManager.init();
  }
  
  @Test
  public void sentencesTokensTest() {
    Preparator p = new Preparator();
    PreparedInput input;
    try {
      input = p.prepare(
          "temp",
          "This is a sentence. And another one. Just for the kicks.",
          new ManualPreparationSettings());

      Tokens ts = input.getTokens();
      List<List<Token>> sentences = ts.getSentenceTokens();
      assertEquals(3, sentences.size());
      assertEquals("This", sentences.get(0).get(0).getOriginal());
      assertEquals("sentence", sentences.get(0).get(3).getOriginal());
      assertEquals(".", sentences.get(0).get(4).getOriginal());
      assertEquals("another", sentences.get(1).get(1).getOriginal());
      assertEquals("kicks", sentences.get(2).get(3).getOriginal());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
