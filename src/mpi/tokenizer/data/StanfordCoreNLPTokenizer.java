package mpi.tokenizer.data;

import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import mpi.aida.config.AidaConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetBeginAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetEndAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.WordTag;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.process.Morphology;
import edu.stanford.nlp.util.CoreMap;

public class StanfordCoreNLPTokenizer {
  private static final Logger logger = 
      LoggerFactory.getLogger(StanfordCoreNLPTokenizer.class);

  public static final String TEXT = "TEXT";

  public static final String NUMBER = "NUMBER";

  public static final String REST = "REST";

  public static final String NPWORD = "NPWORD";

  private HashSet<Character> newline = null;

  private HashSet<Character> whitespace = null;

  private StanfordCoreNLP stanfordCoreNLP = null;

  private Pattern p = Pattern.compile(".*[\\n\\r]+.*[\\n\\r]+.*");
  
  // German Models
  private final String GERMAN_NER_HGC = 
      "resources/corenlp/germanmodels/ner/hgc_175m_600.crf.ser.gz";
  private final String GERMAN_POS_HGC = 
      "resources/corenlp/germanmodels/pos/german-hgc.tagger";
  
  private final String ARABIC_MODELS = "resources/corenlp/arabicmodels/arabicFactored.ser.gz";
  
  //CASESLESS English Models (e.g. for tweets)
  private final String ENGLISH_CASELESS_POS_MODEL = "resources/corenlp/stanford-corenlp-caseless-2013-11-12-models/edu/stanford/nlp/models/pos-tagger/english-caseless-left3words-distsim.tagger";
  private final String ENGLISH_CASELESS_PARSE_MODEL = "resources/corenlp/stanford-corenlp-caseless-2013-11-12-models/edu/stanford/nlp/models/lexparser/englishPCFG.caseless.ser.gz";
  
  public StanfordCoreNLPTokenizer(TokenizerManager.TokenizationType type) {
    Properties props = buildProperties(type);
    stanfordCoreNLP = new StanfordCoreNLP(props, true);
    init();
  }

  
  //Should be called only if type is (POS, TOKENS, PARSE) without language prefix
  private Properties buildPropertiesUsingFrameworkLanguage(TokenizerManager.TokenizationType type) {
    switch (AidaConfig.getLanguage()) {
      case en:
        switch (type) {
          case TOKEN: return buildProperties(TokenizerManager.TokenizationType.ENGLISH_TOKENS);
          case POS: return buildProperties(TokenizerManager.TokenizationType.ENGLISH_POS);
          case PARSE: return buildProperties(TokenizerManager.TokenizationType.ENGLISH_PARSE);
          default: return null;
        }
      case de:
        switch (type) {
          case TOKEN: return buildProperties(TokenizerManager.TokenizationType.GERMAN_TOKENS);
          case POS: return buildProperties(TokenizerManager.TokenizationType.GERMAN_POS);
          default: return null;
        }
      case ar:
        switch (type) {
          case TOKEN: return buildProperties(TokenizerManager.TokenizationType.ARABIC_TOKENS);
          case POS: return buildProperties(TokenizerManager.TokenizationType.ARABIC_POS);
          default: return null;
        }
      default:
        return null;
        
    }
  }
  
  private Properties buildProperties(TokenizerManager.TokenizationType type) {
    Properties props = new Properties();
    props.put("tokenize.options", "untokenizable=noneDelete");
    switch(type) {
      case TOKEN:
      case POS:
      case PARSE:
        props = buildPropertiesUsingFrameworkLanguage(type);
        break;
      case ENGLISH_TOKENS:
        props.put("annotators", "tokenize, ssplit");
        break;
      case ENGLISH_POS:
        props.put("annotators", "tokenize, ssplit, pos, lemma");
        break;
      case ENGLISH_PARSE:
        props.put("annotators", "tokenize, ssplit, parse, pos, lemma");
        break;
      case GERMAN_TOKENS:
        props.put("annotators", "tokenize, ssplit");
        break;
      case GERMAN_POS:
        props.put("annotators", "tokenize, ssplit, pos");
        props.put("pos.model", GERMAN_POS_HGC);
        props.put("ner.model", GERMAN_NER_HGC);
        props.put("ner.useSUTime", "false"); //false not for english
        props.put("ner.applyNumericClassifiers", "false"); //false not for english
        break;
      case ENGLISH_CASELESS_TOKENS:
        props.put("annotators", "tokenize, ssplit");
        break;
      case ENGLISH_CASELESS_POS:
        props.put("annotators", "tokenize, ssplit, pos, lemma");
        props.put("pos.model", ENGLISH_CASELESS_POS_MODEL);
        break;
      case ENGLISH_CASELESS_PARSE:
        props.put("annotators", "tokenize, ssplit, parse, pos, lemma");
        props.put("pos.model", ENGLISH_CASELESS_POS_MODEL);
        props.put("parse.model", ENGLISH_CASELESS_PARSE_MODEL);
        break;
      case ARABIC_TOKENS:
        props.put("annotators", "tokenize, ssplit");
        props.put("pos.model", ARABIC_MODELS);
        break;
      case ARABIC_POS:
        props.put("annotators", "tokenize, ssplit, pos");
        props.put("pos.model", ARABIC_MODELS);
        break;
      case ENGLISH_WHITESPACE_TOKENIZER:
        //does regular Stanford NER, but tokenizes the inputs on Whitespace only
        //and breaks the sentences on newlines.
        //useful for preprocessed input (e.g. tokenized input)
        props.put("annotators", "tokenize, ssplit, pos, lemma, ner");
        // separates words only when whitespace is encountered.
        props.put("tokenize.whitespace", "true");
        // split sentences on newlines. Works well in conjunction with 
        // "-tokenize.whitespace true", in which case StanfordCoreNLP will treat 
        // the input as one sentence per line, only separating words on whitespace.
        props.put("ssplit.eolonly", "true");
        break;
      default:
          break;
    }
    return props;
  }
  
  public Tokens parse(String text, boolean lemmatize) {
    Tokens tokens = new Tokens();
    parse(tokens, text, lemmatize);
    return tokens;
  }
  
  private void parse(Tokens tokens, String text, boolean lemmatize) {
    try {
      if (text.trim().length() == 0) {
        return;
      }
      Annotation document = new Annotation(text);
      stanfordCoreNLP.annotate(document);
      List<CoreMap> sentences = document.get(SentencesAnnotation.class);
      Wrapper wrapper = new Wrapper();
      Morphology morphology = null;
      if (lemmatize) {
        morphology = new Morphology();
      }
      for (CoreMap sentence : sentences) {
        wrapper.sentence(wrapper.sentence() + 1);
        for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
          wrapper.setStandfordId(wrapper.getStandfordId() + 1);
          String ne = token.get(NamedEntityTagAnnotation.class);
          String pos = token.get(PartOfSpeechAnnotation.class);
          int start = token.get(CharacterOffsetBeginAnnotation.class);
          int end = token.get(CharacterOffsetEndAnnotation.class);
          
          
          allocateBranch(start, end, ne, pos, tokens, wrapper, text, lemmatize, morphology);
        }
      }
      if (tokens.size() > 0) {
        int lastCharIndex = tokens.getToken(tokens.size() - 1).getEndIndex();
        if (lastCharIndex < text.length()) {
          tokens.setOriginalEnd(text.substring(lastCharIndex));
        }
      } else {
        tokens.setOriginalEnd(text);
      }
    } catch (Exception e) {
      logger.error("Parser failed: " + e.getLocalizedMessage());
      e.printStackTrace();
    }
  }

  private void allocateBranch(int beginPosition, int endPosition, String ne, String pos, Tokens tokens, Wrapper wrapper, String text, boolean lemmatize, Morphology morphology) {
    String word = text.substring(beginPosition, endPosition);
    int lastTokenId = tokens.size() - 1;
    if (lastTokenId >= 0) {
      int spacesStartIndex = tokens.getToken(lastTokenId).getEndIndex();
      String spaces = text.substring(spacesStartIndex, beginPosition);
      tokens.getToken(lastTokenId).setOriginalEnd(spaces);
      if (p.matcher(spaces).matches()) {
        wrapper.setParagraph(wrapper.paragraph() + 1);
      }
    } else {
      tokens.setOriginalStart(text.substring(0, beginPosition));
    }
    Token token = new Token(wrapper.getStandfordId(), word, beginPosition, endPosition, wrapper.paragraph());
    token.setSentence(wrapper.sentence());
    token.setNE(ne);
    if (lemmatize) {
      String subword = token.getOriginal();
      WordTag wordtag = new WordTag(subword, pos);
      token.setLemma(morphology.lemmatize(wordtag).lemma());
    }    
    token.setPOS(pos);
    
    tokens.addToken(token);
  }

  private void init() {
    newline = new HashSet<Character>();
    newline.add(new Character('\n'));
    newline.add(new Character('\r'));
    whitespace = new HashSet<Character>();
    whitespace.add(' ');
    whitespace.add('\t');
  }
}
