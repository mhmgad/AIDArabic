package mpi.ner.taggers;

import de.mpii.ternarytree.Match;
import de.mpii.ternarytree.TernaryTriePrimitive;
import de.mpii.ternarytree.TrieBuilder;
import mpi.aida.AidaManager;
import mpi.aida.util.TernaryTreeDictionaryUtil;
import mpi.ner.NER;
import mpi.ner.Name;
import mpi.ner.config.NERConfig;
import mpi.tokenizer.data.Token;
import mpi.tokenizer.data.TokenizerManager;
import mpi.tokenizer.data.Tokens;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * Mention recognition method that uses a TernaryTree for name lookups - matching all names in the text that
 * are part of the dictionary. Optionally, mentions that do not contain proper nouns are filtered.
 */
public class TernaryTreeDictionary implements NER {

  private static Logger logger_ = LoggerFactory.getLogger(TernaryTreeDictionary.class);

  private TernaryTriePrimitive trie_;

  /**
   * If set to true, filters matched names that do not contain
   * proper nouns as part of speech.
   */
  private boolean useProperNounFilter_;


  /**
   * Convenience constructor
   *
   * @throws IOException
   */
  public TernaryTreeDictionary() throws IOException {
    this(getDictionaryFilePath(), Boolean.parseBoolean(NERConfig.get("dictionary.useposfilter", "false")));
  }

  private static File getDictionaryFilePath() throws FileNotFoundException {
    String dictionaryPath = NERConfig.get("dictionary.triefile");
    File dictionary;
    if (dictionaryPath == null) {
      logger_.debug("No 'dictionary.triefile' set in 'ner.properties', using default location.");
      try {
        dictionary = new File(TernaryTreeDictionaryUtil.getDefaultDictionaryDirectory(), "dictionary.trie");
      } catch (IOException e) {
        throw new FileNotFoundException("Wrapping SQL Exception: '" + e.getMessage() + "'. TODO: Getting the DB id " +
                "should be implemented for DMap as well.");
      }
    } else {
      dictionary = new File(dictionaryPath);
    }
    return dictionary;
  }

  /**
   * Convenience constructor
   *
   * @throws IOException
   */
  public TernaryTreeDictionary(File dictionaryFile) throws IOException {
    this(dictionaryFile, Boolean.parseBoolean(NERConfig.get("dictionary.useposfilter", "false")));
  }

  /**
   *
   * @param dictionaryFile .trie File constructed using the {@see mpi.aida.datapreparation.ternarytree.TernaryTreeDictionaryBuilder}
   * @param useProperNounFilter  Set to true to filter out mentions that do not contain proper nouns.
   * @throws IOException
   */
  public TernaryTreeDictionary(File dictionaryFile, boolean useProperNounFilter) throws IOException {
    TrieBuilder tb = new TrieBuilder();
    trie_ = tb.loadTernaryTriePrimitive(dictionaryFile);
    logger_.debug("Loaded Ternary Trie dictionary with " + trie_.getTotalNodes() + " nodes from '" + dictionaryFile + "'.");
    useProperNounFilter_ = useProperNounFilter;
    if (useProperNounFilter_) {
      logger_.debug("Filtering proper nouns");
    }
  }
  
  @Override
  public List<Name> findNames(String docId, String text) {
    List<Name> names = new ArrayList<>();

    TokenizerManager.TokenizationType tokenizerType = TokenizerManager.TokenizationType.TOKEN;
    if (useProperNounFilter_) {
      tokenizerType = TokenizerManager.TokenizationType.POS;
    }

    Tokens ts = TokenizerManager.tokenize(text, tokenizerType, false);
    String[] tokens = new String[ts.getTokens().size()];
    BitSet posMap = null;
    if (useProperNounFilter_) {
      posMap = new BitSet();
    }
    int i = 0;
    for (Token t : ts.getTokens()) {
      String token = t.getOriginal();
      String conflatedToken = AidaManager.conflateToken(token);
      tokens[i] = conflatedToken;
      if (useProperNounFilter_) {
        // Keep track of all proper nouns, singular and plural.
        if (t.isProperNoun()) {
          posMap.set(i);
        }
      }
      ++i;
    }
   
    List<Match> allMatches = trie_.getAllMatches(tokens);
    for (Match match : allMatches) {
      Token start = ts.getToken(match.getTokenOffset());
      Token end = ts.getToken(match.getTokenOffset() + match.getTokenCount() - 1);
      if (useProperNounFilter_) {
        boolean keepName = false;
        // Check if token range contains at least one proper noun.
        for (int j = start.getId(); j <= end.getId(); ++j) {
          if (posMap.get(j)) {
            keepName = true;
            break;
          }
        }
        if (!keepName) {
          continue;
        }
      }
      int charOffset = start.getBeginIndex();
      String nameText = text.substring(start.getBeginIndex(), end.getEndIndex());
      Name n = new Name(nameText, charOffset);
      names.add(n);
    }
    
    return names;
  }

  @Override
  public String getId() {
    return "TernaryTreeDictionary";
  }
}
