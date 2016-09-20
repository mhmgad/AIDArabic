package mpi.aida.data;

import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.ArrayList;
import java.util.List;

import mpi.aida.access.DataAccess;
import mpi.tokenizer.data.Token;
import mpi.tokenizer.data.Tokens;

/**
 * Holds the input document as context representation.
 */
public class Context {

  private List<String> tokenStrings_;
  private int[] tokenIds_;

  public Context(Tokens tokens) {
    List<String> ts = new ArrayList<>(tokens.size());
    for (Token token : tokens) {
      ts.add(token.getOriginal());
    }
    tokenStrings_ = new ArrayList<>(ts);
    TObjectIntHashMap<String> token2ids = 
        DataAccess.getIdsForWords(tokenStrings_);
    tokenIds_ = new int[tokens.size()];
    for (int i = 0; i < tokens.size(); ++i) {
      String token = tokenStrings_.get(i);
      int tokenId = token2ids.get(token);
      if (tokenId == token2ids.getNoEntryValue()) {
        tokenId = tokens.getIdForTransientToken(token);
      }
      tokenIds_[i] = tokenId;
    }
  }
  
  public List<String> getTokens() {
    return tokenStrings_;
  }
  
  public int[] getTokenIds() {
    return tokenIds_;
  }
  
  public int getTokenCount() {
    return tokenIds_.length;
  }
}
