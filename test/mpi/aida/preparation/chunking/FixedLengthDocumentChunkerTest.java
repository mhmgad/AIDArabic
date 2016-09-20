package mpi.aida.preparation.chunking;

import static org.junit.Assert.assertEquals;
import mpi.aida.AidaManager;
import mpi.aida.config.AidaConfig;
import mpi.aida.config.settings.PreparationSettings;
import mpi.aida.config.settings.preparation.StanfordHybridPreparationSettings;
import mpi.aida.data.PreparedInput;
import mpi.aida.preparation.documentchunking.DocumentChunker;
import mpi.aida.preparation.documentchunking.FixedLengthDocumentChunker;
import mpi.aida.preparator.Preparator;

import org.junit.Test;

public class FixedLengthDocumentChunkerTest {
  
  public FixedLengthDocumentChunkerTest() {
    AidaConfig.set("dataAccess", "testing");
    AidaConfig.set(AidaConfig.CACHE_WORD_DATA, "false");
    AidaManager.init();
  }
  
  @Test
  public void splitTextIntoFixedLengthChunks() throws Exception {
    String text = "Albert Einstein was born in Ulm, in the Kingdom of Württemberg in the German Empire on 14 March 1879."
        + " His father was Hermann Einstein, a salesman and engineer."
        + " His mother was Pauline Einstein (née Koch)."
        + " In 1880, the family moved to Munich, where his father and his uncle founded Elektrotechnische Fabrik J. Einstein & Cie, a company that manufactured electrical equipment based on direct current."
        + " After graduating, Einstein spent almost two frustrating years searching for a teaching post. "
        + " He acquired Swiss citizenship in February 1901. "
        + " Einstein was awarded a PhD by the University of Zurich."
        + " Einstein became an American citizen in 1940.";
  
    Preparator p = new Preparator();
    PreparationSettings prepSetting = new StanfordHybridPreparationSettings();
    PreparedInput pInp = p.prepare(text, prepSetting);
    DocumentChunker chunker = new FixedLengthDocumentChunker(2);
    pInp = chunker.process("test", text, pInp.getTokens(), pInp.getMentions());
    assertEquals(4, pInp.getChunksCount());
    assertEquals(text, pInp.getTokens().toText());
    assertEquals(text, pInp.getOriginalText());
    
    chunker = new FixedLengthDocumentChunker(3);
    pInp = chunker.process("test", text, pInp.getTokens(), pInp.getMentions());
    assertEquals(3, pInp.getChunksCount());
    assertEquals(text, pInp.getTokens().toText());
    assertEquals(text, pInp.getOriginalText());

    chunker = new FixedLengthDocumentChunker(4);
    pInp = chunker.process("test", text, pInp.getTokens(), pInp.getMentions());
    assertEquals(2, pInp.getChunksCount());
    assertEquals(text, pInp.getTokens().toText());
    assertEquals(text, pInp.getOriginalText());

    chunker = new FixedLengthDocumentChunker(1);
    pInp = chunker.process("test", text, pInp.getTokens(), pInp.getMentions());
    assertEquals(8, pInp.getChunksCount());
    assertEquals(text, pInp.getTokens().toText());
    assertEquals(text, pInp.getOriginalText());
  }
}
