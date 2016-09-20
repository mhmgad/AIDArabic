package mpi.ner.normalizers;

import mpi.ner.MentionNormalizer;


public class WhiteSpaceNormalizer implements MentionNormalizer {

  @Override
  public String normalize(String mention) {
    return mention.replaceAll("\\s+", " ");
  }

}
