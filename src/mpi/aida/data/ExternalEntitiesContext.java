package mpi.aida.data;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.TIntSet;
import mpi.aida.access.DataAccess;
import mpi.tokenizer.data.Token;
import mpi.tokenizer.data.TokenizerManager;
import mpi.tokenizer.data.Tokens;

import java.util.*;


public class ExternalEntitiesContext {
  
  private TIntObjectHashMap<int[]> entityKeyphrases_ = new TIntObjectHashMap<>();

  private TIntObjectHashMap<int[]> keyphraseTokens_ = new TIntObjectHashMap<>();

  private TIntObjectHashMap<String> transientId2token_ = new TIntObjectHashMap<>();

  private TIntIntHashMap transientWordExpansions_ = new TIntIntHashMap();

  private TObjectIntHashMap<String> transientToken2Id_ = new TObjectIntHashMap<>();

  private TObjectIntHashMap<String> existingToken2Id_ = new TObjectIntHashMap<>();

  private CandidateDictionary dictionary_ = new CandidateDictionary();
  
  public ExternalEntitiesContext() {
    // Placeholder when no external context is present.
  }

  public ExternalEntitiesContext(
      Map<String, List<KBIdentifiedEntity>> mentionEntityDictionary,
      Map<KBIdentifiedEntity, List<String>> entityKeyphrases) {
    dictionary_ = new CandidateDictionary(mentionEntityDictionary);
    keyphraseTokens_ = buildKeyphraseTokens(entityKeyphrases);
    entityKeyphrases_ = buildEntityKeyphrases(entityKeyphrases);
  }

  public TIntObjectHashMap<int[]> getEntityKeyphrases() {
    return entityKeyphrases_;
  }

  public TIntObjectHashMap<int[]> getKeyphraseTokens() {
    return keyphraseTokens_;
  }

  /**
   * @return All ids of words that have been created transiently.
   */
  public TIntSet getTransientWordIds() {
    return transientId2token_.keySet();
  }

  public TObjectIntHashMap<String> getTransientTokenIds() {
    return transientToken2Id_;
  }

  public int getIdForWord(String word) {
    return transientToken2Id_.get(word);
  }

  public TIntIntMap getTransientWordExpansions() {
    return transientWordExpansions_;
  }

  private TIntObjectHashMap<int[]> buildKeyphraseTokens(
      Map<KBIdentifiedEntity, List<String>> entityKeyphrases) {
    // Collect all strings to map them to IDs.
    Set<String> allKeyphrases = new HashSet<>();
    Set<String> allKeywords = new HashSet<>();
    Map<String, List<String>> keyphraseTokenStrings = new HashMap<>();
    for (List<String> keyphrases : entityKeyphrases.values()) {
      for (String keyphrase : keyphrases) {
        allKeyphrases.add(keyphrase);
        // Create term expansions for matching upper case tokens.
        allKeyphrases.add(DataAccess.expandTerm(keyphrase));
        Tokens tokens = TokenizerManager.tokenize(keyphrase, TokenizerManager.TokenizationType.ENGLISH_TOKENS, false);
        List<String> keywords = new ArrayList<>(tokens.size());
        for (Token token : tokens.getTokens()) {
          allKeywords.add(token.getOriginal());
          allKeywords.add(DataAccess.expandTerm(token.getOriginal()));
          keywords.add(token.getOriginal());
        }
        keyphraseTokenStrings.put(keyphrase, keywords);
      }
    }

    // Map to IDs, assign transient IDs to tokens that are not in the main database.
    Set<String> allWords = new HashSet<>(allKeyphrases);
    allWords.addAll(allKeywords);
    existingToken2Id_ = DataAccess.getIdsForWords(allWords);
    int nextWordId = DataAccess.getMaximumWordId() + 1;

    // Create ID-based keyphraseTokens
    TIntObjectHashMap<int[]> keyphraseTokens = new TIntObjectHashMap<>();
    for (Map.Entry<String, List<String>> entry : keyphraseTokenStrings.entrySet()) {
      String keyphrase = entry.getKey();
      List<String> keywords = entry.getValue();
      int kpId = existingToken2Id_.get(keyphrase);
      if (kpId == existingToken2Id_.getNoEntryValue()) {
        kpId = transientToken2Id_.get(keyphrase);
        if (kpId == transientToken2Id_.getNoEntryValue()) {
          kpId = nextWordId;
          nextWordId = createTransientWord(keyphrase, kpId);
        }
      }
      int[] kwIds = new int[keywords.size()];
      for (int i = 0; i < keywords.size(); i++) {
        String keyword = keywords.get(i);
        int kwId = existingToken2Id_.get(keyword);
        if (kwId == existingToken2Id_.getNoEntryValue()) {
          kwId = transientToken2Id_.get(keyword);
          if (kwId == transientToken2Id_.getNoEntryValue()) {
            kwId = nextWordId;
            nextWordId = createTransientWord(keyword, kwId);
          }
        }
        kwIds[i] = kwId;
      }
      keyphraseTokens.put(kpId, kwIds);
    }
    return keyphraseTokens;
  }

  /**
   * Assumes the word does NOT exist in the database.
   *
   * @param word
   * @param wordId
   * @return  ID to assign to the next word.
   */
  private int createTransientWord(String word, int wordId) {
    int newWordId = wordId;
    transientId2token_.put(wordId, word);
    transientToken2Id_.put(word, wordId);
    String expanded = DataAccess.expandTerm(word);
    int expandedId = existingToken2Id_.get(expanded);
    if (expandedId == existingToken2Id_.getNoEntryValue()) {
      expandedId = transientToken2Id_.get(expanded);
      if (expandedId == transientToken2Id_.getNoEntryValue()) {
        expandedId = ++newWordId;
        transientId2token_.put(expandedId, expanded);
        transientToken2Id_.put(expanded, expandedId);
      }
    }
    transientWordExpansions_.put(wordId, expandedId);
    return ++newWordId;
  }


  private TIntObjectHashMap<int[]> buildEntityKeyphrases(
          Map<KBIdentifiedEntity, List<String>> entityKeyphrases) {
    TIntObjectHashMap<int[]> entityKeyphrasesIds = new TIntObjectHashMap<>();
    for (Map.Entry<KBIdentifiedEntity, List<String>> entry : entityKeyphrases.entrySet()) {
      KBIdentifiedEntity entityKbId = entry.getKey();
      List<String> keyphrases = entry.getValue();
      int[] keyphraseIds = new int[keyphrases.size()];
      for (int i = 0; i < keyphrases.size(); i++) {
        String keyphrase = keyphrases.get(i);
        int kpId = existingToken2Id_.get(keyphrase);
        if (kpId == existingToken2Id_.getNoEntryValue()) {
          kpId = transientToken2Id_.get(keyphrase);
        }
        keyphraseIds[i] = kpId;
      }
      int entityId = dictionary_.getEntityId(entityKbId);
      entityKeyphrasesIds.put(entityId, keyphraseIds);
    }
    return entityKeyphrasesIds;
  }

  public CandidateDictionary getDictionary() {
    return dictionary_;
  }

  public boolean contains(Entity entity) {
    return dictionary_.contains(entity);
  }
}
