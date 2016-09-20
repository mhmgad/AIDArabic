package mpi.keyphraseextraction;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import edu.stanford.nlp.util.StringUtils;

public class KeyphraseExtractorTest {

	@Test
	public void testFindNames() throws Exception {
	    String text = "The World Health Organization (WHO) says there has been a decline in the spread of Ebola in Liberia, the country hardest hit in the outbreak. The WHO's Bruce Aylward said it was confident the response to the virus was now gaining the upper hand. But he warned against any suggestion that the crisis was over. He said the new number of cases globally was 13,703 and that the death toll, to be published later on Wednesday, would probably pass 5,000.";
	    
	    KeyphraseExtractor ke = new KeyphraseExtractor();
	    List<NounPhrase> keyphrases = ke.findKeyphrases(text);
	    
	    assertEquals("World Health Organization", StringUtils.join(keyphrases.get(0).getTokens()));
	    assertEquals("Ebola", StringUtils.join(keyphrases.get(1).getTokens()));
	    assertEquals("Liberia", StringUtils.join(keyphrases.get(2).getTokens()));
	    assertEquals("Bruce Aylward", StringUtils.join(keyphrases.get(3).getTokens()));
	    assertEquals("upper hand", StringUtils.join(keyphrases.get(4).getTokens()));
	}
}
