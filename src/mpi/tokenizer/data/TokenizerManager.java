package mpi.tokenizer.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TokenizerManager {
  /** type of tokenizer */
  public static enum TokenizationType {
    //Types without language prefix, read the system configuration 
    //to decide the language
    TOKEN, POS, PARSE,
    ENGLISH_TOKENS, ENGLISH_POS, ENGLISH_PARSE, 
    GERMAN_TOKENS, GERMAN_POS, 
    ENGLISH_CASELESS_POS,ENGLISH_CASELESS_PARSE, 
    ARABIC_TOKENS, ARABIC_POS, ENGLISH_CASELESS_TOKENS,
    MULTILINGUAL, ENGLISH_WHITESPACE_TOKENIZER
  }

  private Logger logger_ = LoggerFactory.getLogger(TokenizerManager.class);
  
  private static TokenizerManager manager = null;

  public static void init() {
    TokenizerManager.getInstance();
  }

  public static Tokens tokenize(String text) {
    return TokenizerManager.getInstance().parseText(text, TokenizerManager.TokenizationType.TOKEN, false);
  }
  
  public static Tokens tokenize(String text, TokenizerManager.TokenizationType type, boolean lemmatize) {
    return TokenizerManager.getInstance().parseText(text, type, lemmatize);
  }

  private static TokenizerManager getInstance() {
    if (manager == null) {
      manager = new TokenizerManager();
    }
    return manager;
  }

  private StanfordCoreNLPTokenizer tokenizer = null;
  
  private StanfordCoreNLPTokenizer tokenizerPOS = null;

  private StanfordCoreNLPTokenizer tokenizerParse = null;
  
  private MultilingualTokenizer multilingualTokenizer = null;

  private TokenizerManager() {

  }

  private Tokens parseText(String text, TokenizerManager.TokenizationType type, boolean lemmatize) {
    synchronized (manager) {      
      loadTokenizerForType(type);
    }
    switch (type) {
      case ENGLISH_TOKENS:
      case GERMAN_TOKENS:
      case ARABIC_TOKENS:
      case ENGLISH_WHITESPACE_TOKENIZER:
      case TOKEN:
        return tokenizer.parse(text, lemmatize);
      case ENGLISH_POS:
      case GERMAN_POS:
      case ENGLISH_CASELESS_POS:
      case ARABIC_POS:
      case POS:
        return tokenizerPOS.parse(text, lemmatize);
      case ENGLISH_PARSE:
      case ENGLISH_CASELESS_PARSE:
      case PARSE:
        return tokenizerParse.parse(text, lemmatize);
      case MULTILINGUAL:
        return multilingualTokenizer.tokenize(text);
      default:
        return null;
    }
  }

  private void loadTokenizerForType(TokenizerManager.TokenizationType type) {

    switch (type) {
      case MULTILINGUAL:
        multilingualTokenizer = new MultilingualTokenizer();
        break;
      case ENGLISH_TOKENS:
      case GERMAN_TOKENS:
      case ARABIC_TOKENS:
      case ENGLISH_WHITESPACE_TOKENIZER:
      case TOKEN:
        if (tokenizer == null) {
          logger_.info("Loading tokenizer of type: " + type.name());
          tokenizer = new StanfordCoreNLPTokenizer(type);
        }
        break;
      case ENGLISH_POS:
      case GERMAN_POS:
      case ENGLISH_CASELESS_POS:
      case ARABIC_POS:
      case POS:
        if (tokenizerPOS == null) {
          logger_.info("Loading tokenizer of type: " + type.name());
          tokenizerPOS = new StanfordCoreNLPTokenizer(type);
        }
        break;
      case ENGLISH_PARSE:
      case ENGLISH_CASELESS_PARSE:
      case PARSE:
        if (tokenizerParse == null) {
          logger_.info("Loading tokenizer of type: " + type.name());
          tokenizerParse = new StanfordCoreNLPTokenizer(type);
        }
        break;
      default:
        break;
    }
  }

}
