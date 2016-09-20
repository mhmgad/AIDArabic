package mpi.ner;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mpi.aida.data.Mentions;
import mpi.ner.config.NERConfig;
import mpi.tokenizer.data.TokenizerManager;
import mpi.tokenizer.data.Tokens;
import org.junit.Test;

public class NERManagerTest {

  public NERManagerTest() {
    NERConfig.set(NERConfig.TAGGERS, "Manual");
  }

  @Test
  public void testFilterAnnotations() {
    NERConfig.set(NERConfig.MENTION_FILTER_REMOVESINGLECHAR, "true");
    NERConfig.set(NERConfig.MENTION_FILTER_REMOVEIFBEGINNINGOFSENTENCE, "true");

    String text = "Mention at the beginning. Mention at the End. This A should be gone.";
    Tokens tokens = TokenizerManager.tokenize(text);
    Map<String, List<Name>> annotations = new HashMap<>();
    List<Name> names = new ArrayList<>();
    annotations.put("test", names);
    names.add(new Name("Mention", 0));
    names.add(new Name("Mention", 26));
    names.add(new Name("End", 41));
    names.add(new Name("A", 51));

    List<Name> filteredAnnotations = NERManager.singleton().filterAnnotations(annotations, tokens).get("test");
    assertEquals(1, filteredAnnotations.size());
    assertEquals("End", filteredAnnotations.get(0).getName());
  }

	@Test
	public void testReconcileAnnotations() throws Exception {
		NERManager manager = NERManager.singleton();
		Map<String, List<Name>> annotations = new HashMap<String, List<Name>>();
		List<Name> ann = new ArrayList<Name>();
		ann.add(new Name("pablo", 5));
		ann.add(new Name("pablo picasso", 5));
		ann.add(new Name("picasso test", 10));
		ann.add(new Name("diego maradona", 50));
		annotations.put("test", ann);
		annotations.put("test1", ann);
		List<Name> names = manager.reconcileAnnotations(annotations, null);
		assertEquals(2, names.size());
		assertEquals("pablo picasso", names.get(0).getName());
		assertEquals(5, names.get(0).getStart());
		assertEquals("diego maradona", names.get(1).getName());
		assertEquals(50, names.get(1).getStart());

	}

	@Test
	public void testReconcileAnnotations2() throws Exception {
		NERManager manager = NERManager.singleton();
		Map<String, List<Name>> annotations = new HashMap<String, List<Name>>();
		List<Name> ann = new ArrayList<Name>();
		List<Name> ann1 = new ArrayList<Name>();

		ann.add(new Name("pablo", 5));
		ann1.add(new Name("pablo picasso", 5));
		ann.add(new Name("picasso test", 10));
		ann1.add(new Name("diego maradona", 50));
		annotations.put("test", ann);
		annotations.put("test1", ann1);
		List<Name> names = manager.reconcileAnnotations(annotations, null);
		assertEquals(2, names.size());
		assertEquals("pablo picasso", names.get(0).getName());
		assertEquals(5, names.get(0).getStart());
		assertEquals("diego maradona", names.get(1).getName());
		assertEquals(50, names.get(1).getStart());

	}

	@Test
	public void testReconcileAnnotations3() throws Exception {
		NERManager manager = NERManager.singleton();
		Map<String, List<Name>> annotations = new HashMap<String, List<Name>>();
		List<Name> ann = new ArrayList<Name>();
		List<Name> ann1 = new ArrayList<Name>();

		ann.add(new Name("pablo", 5));
		ann1.add(new Name("pablo picasso", 5));
		ann.add(new Name("picasso test", 10));
		ann1.add(new Name("diego maradona", 50));
		ann.add(new Name("the player diego maradona goleador", 40));
		ann.add(new Name("maradona", 40));
		annotations.put("test", ann);
		annotations.put("test1", ann1);
		List<Name> names = manager.reconcileAnnotations(annotations, null);
		assertEquals(2, names.size());
		assertEquals("pablo picasso", names.get(0).getName());
		assertEquals(5, names.get(0).getStart());
		assertEquals("the player diego maradona goleador", names.get(1)
				.getName());
		assertEquals(40, names.get(1).getStart());

	}
}
