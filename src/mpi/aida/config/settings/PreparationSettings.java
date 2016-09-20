package mpi.aida.config.settings;

import java.io.Serializable;

import mpi.aida.config.AidaConfig;
import mpi.aida.data.Type;
import mpi.aida.preparation.documentchunking.DocumentChunker;
import mpi.aida.preparation.documentchunking.FixedLengthDocumentChunker;
import mpi.aida.preparation.documentchunking.PageBasedDocumentChunker;
import mpi.aida.preparation.documentchunking.SingleChunkDocumentChunker;
import mpi.aida.preparation.mentionrecognition.MentionsDetector;
import mpi.tokenizer.data.TokenizerManager;

/**
 * Settings for the preparator. Predefined settings are available in
 * {@see mpi.aida.config.settings.preparation}.
 */
public class PreparationSettings implements Serializable {

  private static final long serialVersionUID = -2825720730925914648L;

  private MentionsDetector.type mentionsDetectionType = MentionsDetector.type.AUTOMATIC_AND_MANUAL;
  
  private TokenizerManager.TokenizationType tokenizerType = TokenizerManager.TokenizationType.TOKEN;

  /** 
   * Minimum number of mention occurrence to be considered in disambiguation.
   * Default is to consider all mentions.
   */
  private int minMentionOccurrenceCount = 1;

  private Type[] filteringTypes = AidaConfig.getFilteringTypes();
  
  //default to the language in AIDA configuration
  private LANGUAGE language = AidaConfig.getLanguage();
  
  private DOCUMENT_CHUNK_STRATEGY docChunkStrategy = AidaConfig.getDocumentChunkStrategy();
  
  // default input format is PLAIN
  private DOCUMENT_INPUT_FORMAT documentInputFormat = DOCUMENT_INPUT_FORMAT.PLAIN;

  private String encoding = "UTF-8";
  
  private String documentId;
  private String documentTitle;
  private String documentField;
  
  public static enum DOCUMENT_CHUNK_STRATEGY {
    SINGLE, PAGEBASED, MULTIPLE_FIXEDLENGTH
  }
  
  public static enum DOCUMENT_INPUT_FORMAT {
    PLAIN, NYT, ALTO, TEI, SPIEGEL, ROBUST04, JSON, GIGAWORD
  }
  
  public static enum LANGUAGE {
    en, de, ar, multi
  }
  
  public MentionsDetector.type getMentionsDetectionType() {
    return mentionsDetectionType;
  }

  public void setMentionsDetectionType(MentionsDetector.type mentionsDetectionType) {
    this.mentionsDetectionType = mentionsDetectionType;
  }

  
  public TokenizerManager.TokenizationType getTokenizerType() {
    return tokenizerType;
  }

  
  public void setTokenizerType(TokenizerManager.TokenizationType tokenizerType) {
    this.tokenizerType = tokenizerType;
  }

  public Type[] getFilteringTypes() {
    return filteringTypes;
  }

  public void setFilteringTypes(Type[] filteringTypes) {
    this.filteringTypes = filteringTypes;
  }
  
  public LANGUAGE getLanguage() {
    return language;
  }
  
  public void setLanguage(LANGUAGE language) {
    this.language = language;
  }


  public int getMinMentionOccurrenceCount() {
    return minMentionOccurrenceCount;
  }

  public void setMinMentionOccurrenceCount(int minMentionOccurrenceCount) {
    this.minMentionOccurrenceCount = minMentionOccurrenceCount;
  }

  public void setDocumentInputFormat(DOCUMENT_INPUT_FORMAT docInpFormat) {
    this.documentInputFormat = docInpFormat;
  }

  public DOCUMENT_INPUT_FORMAT getDocumentInputFormat() {
    return this.documentInputFormat;
  }

//  public Map<String, Object> getAsMap() {
//    Map<String, Object> s = new HashMap<String, Object>();
//    s.put("mentionsFilter", mentionsFilter.toString());
//    s.put("language", language.toString());
//    s.put("minMentionOccurrenceCounts", String.valueOf(minMentionOccurrenceCount));
//    s.put("useHybridMentionDetection", String.valueOf(useHybridMentionDetection));
//    s.put("docChunkStrategy", docChunkStrategy.toString());
//    s.put("docInputFormat", documentInputFormat.toString());
//    return s;
//  }
  
  public DocumentChunker getDocumentChunker() {
    DocumentChunker chunker = null;
    switch (docChunkStrategy) {
      case SINGLE:
        chunker = new SingleChunkDocumentChunker();
        break;
      case PAGEBASED:
        chunker = new PageBasedDocumentChunker();
        break;
      case MULTIPLE_FIXEDLENGTH:
        chunker = new FixedLengthDocumentChunker();
        break;
    }
    return chunker;
  }

  public String getEncoding() {
    return encoding;
  }

  public void setEncoding(String encoding) {
    this.encoding = encoding;
  }

  
  public String getDocumentId() {
    return documentId;
  }

  
  public void setDocumentId(String documentId) {
    this.documentId = documentId;
  }

  
  public String getDocumentTitle() {
    return documentTitle;
  }

  
  public void setDocumentTitle(String documentTitle) {
    this.documentTitle = documentTitle;
  }

  
  public String getDocumentField() {
    return documentField;
  }

  
  public void setDocumentField(String documentField) {
    this.documentField = documentField;
  }
  
  
}
