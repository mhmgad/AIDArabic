package mpi.aida.preparation.mentionrecognition;

import java.io.Serializable;

import mpi.aida.config.settings.PreparationSettings;
import mpi.aida.data.Mentions;
import mpi.ner.NERManager;
import mpi.tokenizer.data.TokenizerManager;
import mpi.tokenizer.data.Tokens;
import mpi.tools.javatools.datatypes.Pair;

public class MentionsDetector implements Serializable {
  
  private static final long serialVersionUID = 6260499966421708963L;

  private ManualFilter manualFilter = null;

  private HybridFilter hybridFilter = null;

  public MentionsDetector() {
    manualFilter = new ManualFilter();
    hybridFilter = new HybridFilter();
  }

  public static enum type {
    AUTOMATIC, MANUAL, AUTOMATIC_AND_MANUAL
  }
  
  public MentionDetectionResults filter(String docId, String text, TokenizerManager.TokenizationType tokenizerType, MentionsDetector.type mentionDetectionType, PreparationSettings.LANGUAGE language) {
    Mentions mentions = null;
    Mentions manualMentions = null;

    // The input text is changed only when manual markup is included.
    String filteredText = text;
    Tokens tokens = null;

    //manual case handled separately
    if (mentionDetectionType == type.MANUAL) {
      Pair<String, Mentions> filteredTextMentions = manualFilter.filter(text);
      filteredText = filteredTextMentions.first;
      mentions = filteredTextMentions.second();
      tokens = manualFilter.tokenize(filteredTextMentions.first(), tokenizerType, mentions, language);
      return new MentionDetectionResults(tokens, mentions, filteredText);
    }
    
    //if hybrid mention detection, use manual filter to get the tokens
    //and then pass them the appropriate ner 
    if (mentionDetectionType == type.AUTOMATIC_AND_MANUAL) {
      Pair<String, Mentions> filteredTextMentions = manualFilter.filter(text);
      tokens = manualFilter.tokenize(filteredTextMentions.first(), tokenizerType, filteredTextMentions.second(), language);
      manualMentions = filteredTextMentions.second();
      filteredText = filteredTextMentions.first();
    } else { //otherwise tokenize in AUTOMATIC mode.
      tokens = TokenizerManager.tokenize(filteredText, tokenizerType, false);
    }
    
    mentions = NERManager.singleton().findMentions(docId, filteredText, tokens);
    
    //if hybrid mention detection, merge both types mentions
    if (mentionDetectionType == type.AUTOMATIC_AND_MANUAL) {
      mentions = hybridFilter.parse(manualMentions, mentions);
    }

    return new MentionDetectionResults(tokens, mentions, filteredText);
  }
}