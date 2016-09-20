package mpi.aida.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mpi.aida.AidaManager;
import mpi.aida.config.AidaConfig;

import org.junit.Test;


public class ExternalEntitiesContextTest {

  public ExternalEntitiesContextTest() {
    AidaConfig.set("dataAccess", "testing");
    AidaConfig.set(AidaConfig.CACHE_WORD_DATA, "false");
    AidaManager.init();
  }
  
  @Test
  public void test() {
    Map<String, List<KBIdentifiedEntity>> dictionary = new HashMap<>();
    List<KBIdentifiedEntity> aidaCands = new ArrayList<>();
    KBIdentifiedEntity aida = new KBIdentifiedEntity("AIDA-MPI", "EXTERNAL");
    aidaCands.add(aida);
    dictionary.put("AIDA", aidaCands);

    Map<KBIdentifiedEntity, List<String>> entityKeyphrases =
            new HashMap<>();
    List<String> keyphrases = Arrays.asList(
            new String[]{"Google", "entity disambiguation framework",
                    "MPI", "software"});
    entityKeyphrases.put(aida, keyphrases);

    ExternalEntitiesContext externalContext =
            new ExternalEntitiesContext(dictionary, entityKeyphrases);

    Mention aidaMention = new Mention();
    aidaMention.setMention("AIDA");
    Entities cands = externalContext.getDictionary().getEntities(aidaMention);
    assertEquals(1, cands.size());
    Entity e = cands.getEntities().iterator().next();
    assertEquals(1, externalContext.getEntityKeyphrases().size());
    int[] kpIds = externalContext.getEntityKeyphrases().get(e.getId());
    assertEquals(4, kpIds.length);
    for (int kpId : kpIds) {
      int[] kwIds = externalContext.getKeyphraseTokens().get(kpId);
      assertTrue(kwIds.length == 1 || kwIds.length == 3);
    }
  }
}
