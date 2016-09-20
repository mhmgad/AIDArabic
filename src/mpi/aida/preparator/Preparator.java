package mpi.aida.preparator;

import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.ArrayList;
import java.util.List;

import mpi.aida.config.settings.PreparationSettings;
import mpi.aida.config.settings.PreparationSettings.DOCUMENT_INPUT_FORMAT;
import mpi.aida.data.ExternalEntitiesContext;
import mpi.aida.data.Mention;
import mpi.aida.data.Mentions;
import mpi.aida.data.PreparedInput;
import mpi.aida.preparation.documentchunking.DocumentChunker;
import mpi.aida.preparation.mentionrecognition.MentionDetectionResults;
import mpi.aida.preparation.mentionrecognition.MentionsDetector;
import mpi.aida.preparator.inputformat.PlainPreparatorInputFormat;
import mpi.aida.preparator.inputformat.PreparatorInputFormatInterface;
import mpi.aida.preparator.inputformat.sgml.GigawordPrepratorInputFormat;
import mpi.aida.preparator.inputformat.xml.AltoPreparatorInputFormat;
import mpi.aida.preparator.inputformat.xml.JSONPreparatorInputFormat;
import mpi.aida.preparator.inputformat.xml.NYTPreparatorInputFormat;
import mpi.aida.preparator.inputformat.xml.Robust04PreparatorInputFormat;
import mpi.aida.preparator.inputformat.xml.SpiegelPreparatorInputFormat;
import mpi.aida.preparator.inputformat.xml.TEIPreparatorInputFormat;
import mpi.aida.util.normalization.TextNormalizerManager;
import mpi.tokenizer.data.Tokens;
import mpi.tools.javatools.datatypes.Pair;


public class Preparator {

  private PreparatorInputFormatInterface inpFormatPreparator;

  private static MentionsDetector filterMention = new MentionsDetector();

  /**
   * Returns prepared input for the given text.
   * 
   * @param content The text to be disambiguated
   * @param prepSettings  Preparation settings
   * @return  PreparedInput instance.
   * @throws Exception
   */
  public PreparedInput prepare(String content, PreparationSettings prepSettings) throws Exception {
    int signature = content.substring(0, Math.min(content.length(), 100)).hashCode();
    return prepare(signature + "_" + System.currentTimeMillis(), content, prepSettings, new ExternalEntitiesContext());
  }

  /**
   * Returns prepared input for the given text.
   * 
   * @param docId The document id associated with the text
   * @param content The text to be disambiguated
   * @param prepSettings  Preparation settings
   * @return  PreparedInput instance.
   * @throws Exception
   */
  public PreparedInput prepare(String docId, String content, PreparationSettings prepSettings) throws Exception {
    return prepare(docId, content, prepSettings, new ExternalEntitiesContext());
  }

  /**
   * Returns prepared input for the given text.
   * 
   * @param content The text to be disambiguated
   * @param docId The document id associated with the text
   * @param prepSettings  Preparation settings
   * @param externalContext External Entities Context
   * @return  PreparedInput instance.
   * @throws Exception
   */
  public PreparedInput prepare(String docId, String content, PreparationSettings prepSettings, ExternalEntitiesContext externalContext) throws Exception {
    content = TextNormalizerManager.normalize(content);
    DOCUMENT_INPUT_FORMAT docInpFormat = prepSettings.getDocumentInputFormat();
    switch (docInpFormat) {
      case NYT:
        inpFormatPreparator = new NYTPreparatorInputFormat();
        break;
      case ALTO:
        inpFormatPreparator = new AltoPreparatorInputFormat();
        break;
      case TEI:
        inpFormatPreparator = new TEIPreparatorInputFormat();
        break;
      case SPIEGEL:
        inpFormatPreparator = new SpiegelPreparatorInputFormat();
        break;
      case ROBUST04:
        inpFormatPreparator = new Robust04PreparatorInputFormat();
        break;
      case JSON:
        inpFormatPreparator = new JSONPreparatorInputFormat();
        break;
      case GIGAWORD:
          inpFormatPreparator = new GigawordPrepratorInputFormat();
          break;
      default:
        inpFormatPreparator = new PlainPreparatorInputFormat();
        break;
    }

    return inpFormatPreparator.prepare(docId, content, prepSettings, externalContext);
  }


  public static PreparedInput prepareInputData(
      String text, String docId, ExternalEntitiesContext externalContext,
      PreparationSettings settings) {
    text = TextNormalizerManager.normalize(text);
    MentionDetectionResults mdr = filterMention.filter(docId, text,
        settings.getTokenizerType(), settings.getMentionsDetectionType(), settings.getLanguage());

    // Drop mentions below min occurrence count.
    if (settings.getMinMentionOccurrenceCount() > 1) {
      dropMentionsBelowOccurrenceCount(mdr.getMentions(), settings.getMinMentionOccurrenceCount());
    }

    DocumentChunker chunker = settings.getDocumentChunker();

    Tokens tokens = mdr.getTokens();
    // Add external context word id mappings so that
    // external words will be transformed as well.
    tokens.setTransientTokenIds(externalContext.getTransientTokenIds());
    Mentions mentions = mdr.getMentions();

    PreparedInput preparedInput = 
        chunker.process(docId, mdr.getText(), tokens, mentions);

    return preparedInput;
  }

  private static void dropMentionsBelowOccurrenceCount(Mentions docMentions,
      int minMentionOccurrenceCount) {
    TObjectIntHashMap<String> mentionCounts = new TObjectIntHashMap<String>();
    for (Mention m : docMentions.getMentions()) {
      mentionCounts.adjustOrPutValue(m.getMention(), 1, 1);
    }
    List<Mention> mentionsToRemove = new ArrayList<Mention>();
    for (Mention m : docMentions.getMentions()) {
      if (mentionCounts.get(m.getMention()) < minMentionOccurrenceCount) {
        mentionsToRemove.add(m);
      }
    }
    for (Mention m : mentionsToRemove) {
      docMentions.remove(m);
    }
  }
}
