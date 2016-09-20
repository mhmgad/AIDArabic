package mpi.ner;

import mpi.ner.normalizers.WhiteSpaceNormalizer;
import mpi.ner.taggers.StanfordNER;
import mpi.ner.taggers.StanfordNERWhiteSpaceTokenization;
import mpi.ner.taggers.StanfordPOS;
import mpi.ner.taggers.TernaryTreeDictionary;

import java.io.IOException;

public class NERFactory {

  public static NER createNER(NERTaggerName taggerName) throws IOException {
    switch (taggerName) {
      case StanfordNER:
        return new StanfordNER();
      case StanfordNERWhiteSpaceTokenizer:
    	  return new StanfordNERWhiteSpaceTokenization();
      case DICTIONARY:
        return new TernaryTreeDictionary();
      case StanfordPOS:
        return new StanfordPOS();
//      case LingPipeNER:
//        return new LingPipeNER();
//      case LingPipePOS:
//        return new LingPipePOS();
//      case DEXTER:
//        return new DexterDictionary();
//      case BOLDYREV:
//        return new BoldyrevDictionary();
      default:
        throw new UnsupportedOperationException("Unknown NER: " + taggerName);

    }
  }

  public static MentionNormalizer createMentionNormalizer(MentionNormalizerName normalizerName) {
    switch (normalizerName) {
      case WhiteSpaceNormalizer:
        return new WhiteSpaceNormalizer();
      default:
        throw new UnsupportedOperationException("Unknown Normalizer: " + normalizerName);
    }
  }

}
