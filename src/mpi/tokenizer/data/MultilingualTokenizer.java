package mpi.tokenizer.data;

import java.io.IOException;
import java.io.StringReader;

import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.util.Version;

public class MultilingualTokenizer {

  public Tokens tokenize(String text) {
    Tokens tokens = new Tokens();

    StandardTokenizer tokenizer = new StandardTokenizer(Version.LUCENE_46, new StringReader(text));
    try {
      tokenizer.reset();
      int id = 0;
      while (tokenizer.incrementToken()) {

        OffsetAttribute offset = tokenizer.getAttribute(OffsetAttribute.class);

        allocateBranch(id++, offset.startOffset(), offset.endOffset(), tokens, text);

      }
      tokenizer.close();

    } catch (IOException e) {
      e.printStackTrace();
    }

    return tokens;
  }

  private void allocateBranch(int id, int beginPosition, int endPosition, Tokens tokens, String text) {
    String word = text.substring(beginPosition, endPosition);
    int lastTokenId = tokens.size() - 1;
    if (lastTokenId >= 0) {
      int spacesStartIndex = tokens.getToken(lastTokenId).getEndIndex();
      String spaces = text.substring(spacesStartIndex, beginPosition);
      tokens.getToken(lastTokenId).setOriginalEnd(spaces);
    } else {
      tokens.setOriginalStart(text.substring(0, beginPosition));
    }

    Token token = new Token(id, word, beginPosition, endPosition, 0);

    tokens.addToken(token);
  }

}
