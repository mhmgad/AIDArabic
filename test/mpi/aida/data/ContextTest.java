package mpi.aida.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import mpi.aida.AidaManager;
import mpi.aida.access.DataAccess;
import mpi.aida.config.AidaConfig;
import mpi.tokenizer.data.Tokens;

import org.junit.Test;


public class ContextTest {

  public ContextTest() {
    AidaConfig.set("dataAccess", "testing");
    AidaConfig.set(AidaConfig.CACHE_WORD_DATA, "false");
    AidaManager.init();
  }
  
  @Test
  public void test() {
    List<String> text = new LinkedList<String>();
    
    text.add("Jimmy");
    text.add("played");
    text.add("Les");
    text.add("Paul");
    text.add("played");    
    
    Context context = new Context(new Tokens(text));
    assertEquals(text, context.getTokens());    
    TIntObjectHashMap<String> id2word =
        DataAccess.getWordsForIds(context.getTokenIds());
    
    for (int i = 0; i < text.size(); ++i) {
      assertEquals(text.get(i), id2word.get(context.getTokenIds()[i]));
    }
  }

  @Test
  public void testExternalContext() {
    List<String> text = new LinkedList<String>();

    text.add("Jimmy");
    text.add("played");
    text.add("Les");
    text.add("Paul");
    text.add("played");
    text.add("external");

    Map<String, List<KBIdentifiedEntity>> dictionary = new HashMap<>();
    List<KBIdentifiedEntity> aidaCands = new ArrayList<>();
    KBIdentifiedEntity aidaMPI = new KBIdentifiedEntity("TEST", "EXTERNAL");
    aidaCands.add(aidaMPI);
    dictionary.put("TEST", aidaCands);

    Map<KBIdentifiedEntity, List<String>> entityKeyphrases =
            new HashMap<>();
    List<String> aidaMPIKeyphrases = Arrays.asList(
            new String[] {"external"});
    entityKeyphrases.put(aidaMPI, aidaMPIKeyphrases);

    ExternalEntitiesContext externalContext =
            new ExternalEntitiesContext(dictionary, entityKeyphrases);

    Tokens tokens = new Tokens(text);
    tokens.setTransientTokenIds(externalContext.getTransientTokenIds());

    Context context = new Context(tokens);
    assertEquals(text, context.getTokens());

    int[] textAsId = context.getTokenIds();
    int[] existingIds = new int[textAsId.length - 1];
    for (int i = 0; i < textAsId.length - 1; i++) {
        existingIds[i] = textAsId[i];
    }

    List<String> existingTokens = new ArrayList<>(text);
    existingTokens.remove(existingTokens.size() - 1);

    TIntObjectHashMap<String> id2word =
            DataAccess.getWordsForIds(existingIds);

    for (int i = 0; i < text.size() - 1; ++i) {
      assertEquals(text.get(i), id2word.get(context.getTokenIds()[i]));
      assertFalse(externalContext.getTransientWordIds().contains(context.getTokenIds()[i]));
    }

    // Last word is not part of the database.
    int last = text.size() - 1;
    assertEquals(textAsId[last], externalContext.getIdForWord(text.get(last)));
  }
}
