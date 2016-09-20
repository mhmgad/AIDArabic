package mpi.keyphraseextraction;

import java.util.List;

import edu.stanford.nlp.util.StringUtils;

/**
 * 
 * A Noun Phrase in a sentence
 * 
 * @author nnakasho
 *
 */

public class NounPhrase {

  private int positionInDocument;
  
  private int sentenceInDocument;
  
  private int startInWordSequence;

  private int endInWordSequence;

  private String[] tokens;

  public NounPhrase(String[] nounphraseTokens, int start, int end, int docPosition, int sentenceInDocument) {
    this.tokens = nounphraseTokens;
    this.startInWordSequence = start;
    this.endInWordSequence = end;
    this.positionInDocument = docPosition;
    this.sentenceInDocument = sentenceInDocument;
  }
  
  public boolean overlapsWith(NounPhrase np) {

    if (startInWordSequence == np.startInWordSequence && np.endInWordSequence == np.endInWordSequence) return true;

    //check if one range includes another
    if (np.endInWordSequence >= startInWordSequence && np.endInWordSequence <= endInWordSequence) return true;

    return false;
  }

  public int startInWordSequence() {
    return startInWordSequence;
  }

  public int endInWordSequence() {
    return endInWordSequence;
  }

  public int getPositionInDocument() {
    return positionInDocument;
  }

  public String[] getTokens() {
    return tokens;
  }
  
  public int getSentenceInDocument() {
    return sentenceInDocument;
  }

  public String toString() {
    return StringUtils.join(tokens, " ");
  }

  public static NounPhrase[] sort(List<NounPhrase> nounphrases) {
    NounPhrase[] sorted = new NounPhrase[nounphrases.size()];
    for (int i = 0; i < sorted.length; i++)
      sorted[i] = new NounPhrase(new String[0], -1, 0, 0, 0);

    sorted[0] = nounphrases.remove(0);
    for (int i = 1; (nounphrases.size() > 0); i++) {
      NounPhrase current = nounphrases.remove(0);

      //find correct position in array
      boolean insertion = false;
      for (int j = 0; j < i; j++) {
        if (!insertion && current.startInWordSequence <= sorted[j].startInWordSequence) {

          while (j < i) {
            NounPhrase temp = sorted[j];
            sorted[j] = current;
            current = temp;
            j++;
          }
          if (current.startInWordSequence >= 0) sorted[j] = current;

          insertion = true;
        }

      }
      if (!insertion) sorted[i] = current;

    }
    return sorted;
  }
}
