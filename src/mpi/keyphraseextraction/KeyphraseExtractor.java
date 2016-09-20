package mpi.keyphraseextraction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mpi.tokenizer.data.Token;
import mpi.tokenizer.data.TokenizerManager;
import mpi.tokenizer.data.Tokens;
import mpi.tools.javatools.administrative.D;
import edu.stanford.nlp.ling.TaggedWord;

/**
 * 
 * Identifies noun phrases in text. After a part-of-speech tagger is applied,
 * a series of heuristic regular expressions capture what constitues a noun phrase.
 *
 * @author nnakasho
 *
 */

public class KeyphraseExtractor {

  public static final String NOUN = "NNS?\\s?";
  
  public static final String ADJ = "JJ\\s";
  
  public static final String PREP = "IN\\s";

  //	 Potentially A PROPER noun
  public static final String nounPhrase = "(NNP\\s?){1,}(NNPS\\s?){0,}(NNS\\s?){0,}(NN\\s?){0,}(NNP\\s?){0,}(NNPS\\s?){0,}(NNS\\s?){0,}";

  //  Special case:  e.g University of X
  //	 Potentially A PROPER noun
  public static final String nounPhraseIN = "(NNP\\s){1,}(IN\\s?){1}(NNP\\s?){1,}";

  //	 Potentially A PROPER noun
  public static final String nounPhraseJJ2 = "(JJ\\s){1}(NNP\\s?){1,}";

  // Special case: e.g. book title, A Journey
  // Potentiall A PROPER noun
  public static final String nounPhraseDT = "((?=[^A-Z])DT\\s){1}(NNP\\s?){1,}";

  // Technical terminology (according to Justeson & Katz 1995)
  public static final String nounPhraseTT = "(("+ADJ+"|"+NOUN+")+|(("+ADJ+"|"+NOUN+")*("+NOUN+PREP+")?)("+ADJ+"|"+NOUN+")+)NNS?";
  
  // Regexes for dates
  public static final String month = "(Jan|January|Feb|February|Febr|Mar|March|Apr|April|May|Jun|June|Jul|July|Aug|August|Sep|September|Sept"
      + "|Oct|October|Nov|November|Dec|December)";

  public static final String day = "(\\d{0,2})";

  public static final String year = "(\\d{4})";


  List<String> nounPhrases = new ArrayList<>(Arrays.asList(new String[] { nounPhrase }));

  List<String> nounPhrases_list = new ArrayList<>(Arrays.asList(new String[] { nounPhraseIN }));

  List<String> nounPhrasesJJ_list = new ArrayList<>(Arrays.asList(new String[] { nounPhraseJJ2 }));

  List<String> nounPhrasesDT_list = new ArrayList<>(Arrays.asList(new String[] { nounPhraseDT }));

  List<String> nounPhrasesTT_list = new ArrayList<>(Arrays.asList(new String[] { nounPhraseTT }));

  /**
   * Extracts keyphrases from the given text. Keyphrases are extracted
   * using regular expressions over PoS tags, including both proper nouns
   * and technical terms (Justeson and Katz, 1995).
   * 
   * @param text  Plain text to extract keyphrases from.
   * @return  A list of keyphrases from the text (may contain duplicates).
   * @throws IOException
   */
  public List<NounPhrase> findKeyphrases(String text) throws IOException {
    if (text == null || text.isEmpty()) {
      return new ArrayList<>();
    }
       
    // tokenize text
    Tokens tokens = TokenizerManager.tokenize(text, TokenizerManager.TokenizationType.ENGLISH_POS, false);
    int currentSentence = -1;
    List<List<Token>> sentences = new ArrayList<>();
    List<Token> sentenceTokens = new ArrayList<>();
    for (Token token : tokens) {
      if (token.getSentence() != currentSentence) {
        if (currentSentence != -1) {
          sentences.add(sentenceTokens);
        }
        sentenceTokens = new ArrayList<>();
        currentSentence = token.getSentence();
      }
      sentenceTokens.add(token);
    }

    return gatherKeyphrases(sentences);
  }

  protected List<NounPhrase> gatherKeyphrases(List<List<Token>> sentences) {
    List<NounPhrase> keyphrases = new ArrayList<>();

    int sentenceNum = 0;
    int sentenceStartWordNum = 0;
    // Run regular expressions on each sentence
    for (List<Token> sentence : sentences) {
      WordSequence wordSequence = new WordSequence();
      List<NounPhrase> nounPhraseList = new ArrayList<>();
      
      List<TaggedWord> tSentence = new LinkedList<>();
      
      for (Token t : sentence) {
        tSentence.add(new TaggedWord(t.getOriginal(), t.getPOS()));
      }
      
      for (TaggedWord tw : tSentence) {
        if (tw.word().compareTo("-RRB-") == 0 || tw.word().compareTo("-LRB-") == 0) continue;
        wordSequence.appendWord(tw.word());
        wordSequence.appendTag(tw.tag());
      }

      // ignore lines that are too long
//      if (wordSequence.WordtoString().length() > 300) continue;
      
      for (String nounphrase : nounPhrases) {
        Pattern familyNameSuffixPattern = Pattern.compile(nounphrase);
        Matcher matcher = familyNameSuffixPattern.matcher(wordSequence.TagtoString());

        while (matcher.find()) {
          int start = matcher.start();
          int end = matcher.end();
          String phrase = wordSequence.TagtoString().substring(start, end);
          int count = new StringTokenizer(phrase).countTokens();
          int wordSeqStart = wordSequence.TagStringPosToSequencePos.get(start);
          String entityPhrase = "";
          while (count > 0) {
            entityPhrase = entityPhrase + wordSequence.words.get(wordSeqStart++) + " ";
            count--;
          }
          int startInSentence = wordSequence.TagStringPosToSequencePos.get(start);
          int posInDocument = startInSentence + sentenceStartWordNum;
          nounPhraseList.add(new NounPhrase(wordSequence.getSubSeq(startInSentence, wordSeqStart), startInSentence, (wordSeqStart), posInDocument, sentenceNum));
        }
      }
      
      // Technical terminology
      for (String nounphrase : nounPhrasesTT_list) {
        Pattern familyNameSuffixPattern = Pattern.compile(nounphrase);
        Matcher matcher = familyNameSuffixPattern.matcher(wordSequence.TagtoString());

        while (matcher.find()) {
          int start = matcher.start();
          int end = matcher.end();
          String phrase = wordSequence.TagtoString().substring(start, end);
          int count = new StringTokenizer(phrase).countTokens();
          int wordSeqStart = wordSequence.TagStringPosToSequencePos.get(start);
          String entityPhrase = "";
          while (count > 0) {
            entityPhrase = entityPhrase + wordSequence.words.get(wordSeqStart++) + " ";
            count--;
          }
          int startInSentence = wordSequence.TagStringPosToSequencePos.get(start);
          int posInDocument = startInSentence + sentenceStartWordNum;
          nounPhraseList.add(new NounPhrase(wordSequence.getSubSeq(startInSentence, wordSeqStart), startInSentence, (wordSeqStart), posInDocument, sentenceNum));
        }
      }

      // adjectives JJ
      for (String nounphraseJJ : nounPhrasesJJ_list) {

        Pattern familyNameSuffixPattern = Pattern.compile(nounphraseJJ);
        Matcher matcher = familyNameSuffixPattern.matcher(wordSequence.TagtoString());
        while (matcher.find()) {
          int start = matcher.start();
          int end = matcher.end();
          String phrase = wordSequence.TagtoString().substring(start, end);
          int count = new StringTokenizer(phrase).countTokens();
          int wordSeqStart = wordSequence.TagStringPosToSequencePos.get(start);

          if (phrase.startsWith("JJ") && Character.isUpperCase(wordSequence.words.get(wordSeqStart).charAt(0))) {
            String entityPhrase = "";
            while (count > 0) {
              entityPhrase = entityPhrase + wordSequence.words.get(wordSeqStart++) + " ";
              count--;
            }
            int startInSentence = wordSequence.TagStringPosToSequencePos.get(start);
            int posInDocument = startInSentence + sentenceStartWordNum;
            nounPhraseList.add(new NounPhrase(wordSequence.getSubSeq(startInSentence, wordSeqStart), startInSentence, (wordSeqStart), posInDocument, sentenceNum));
          }
        }
      }

      // determiners
      for (String nounphraseDT : nounPhrasesDT_list) {
        Pattern familyNameSuffixPattern = Pattern.compile(nounphraseDT);
        Matcher matcher = familyNameSuffixPattern.matcher(wordSequence.TagtoString());
        while (matcher.find()) {
          int start = matcher.start() + 1; //becuase we are macthing only DT not say WDT and so need space in regex
          int end = matcher.end();
          String phrase = wordSequence.TagtoString().substring(start, end);

          int count = new StringTokenizer(phrase).countTokens();
          int wordSeqStart = wordSequence.TagStringPosToSequencePos.get(start);

          if (start > 0 && phrase.startsWith("DT") && Character.isUpperCase(wordSequence.words.get(wordSeqStart).charAt(0))) {
            String entityPhrase = "";
            while (count > 0) {
              entityPhrase = entityPhrase + wordSequence.words.get(wordSeqStart++) + " ";
              count--;
            }
            int startInSentence = wordSequence.TagStringPosToSequencePos.get(start);
            int posInDocument = startInSentence + sentenceStartWordNum;
            nounPhraseList.add(new NounPhrase(wordSequence.getSubSeq(startInSentence, wordSeqStart), startInSentence, (wordSeqStart), posInDocument, sentenceNum));
          }
        }
      }

      for (String nounphraseDT : nounPhrases_list) {
        Pattern familyNameSuffixPattern = Pattern.compile(nounphraseDT);
        Matcher matcher = familyNameSuffixPattern.matcher(wordSequence.TagtoString());
        while (matcher.find()) {
          int start = matcher.start();
          int end = matcher.end();
          String phrase = wordSequence.TagtoString().substring(start, end);
          int count = new StringTokenizer(phrase).countTokens();
          int wordSeqStart = wordSequence.TagStringPosToSequencePos.get(start);

          //String entityPhrase = "PN:";
          String entityPhrase = "";
          while (count > 0) {
            entityPhrase = entityPhrase + wordSequence.words.get(wordSeqStart++) + " ";
            count--;
          }
          if (entityPhrase.indexOf("of") > 0) {
            int startInSentence = wordSequence.TagStringPosToSequencePos.get(start);
            int posInDocument = startInSentence + sentenceStartWordNum;
            nounPhraseList.add(new NounPhrase(wordSequence.getSubSeq(startInSentence, wordSeqStart), startInSentence, (wordSeqStart), posInDocument, sentenceNum));
          }
        }
      }
              
      for (NounPhrase np : nounPhraseList) {
        if (isValid(np)) {
          // normalize to single line
          keyphrases.add(np);
        }
      }
      
      sentenceNum++;
      sentenceStartWordNum += wordSequence.size();
    }
    
    return keyphrases;
  }

  private boolean isValid(NounPhrase np) {   
    String[] tokens = np.getTokens();
    
    if (tokens.length > 10) {
      return false;
    }
    
    boolean valid = true;
    
    for (String token : tokens) {
      if (token.length() > 30) {
        valid = false;
        break;
      }
    }
    
    return valid;
  }

  public static void main(String[] args) throws IOException {
    KeyphraseExtractor kpe = new KeyphraseExtractor();
    while (true) {
      D.p(kpe.findKeyphrases(D.r()));
    }
  }
}
