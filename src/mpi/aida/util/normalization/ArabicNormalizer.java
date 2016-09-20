package mpi.aida.util.normalization;

import mpi.aida.util.arabic.ArabicTextNormalizerUtil;


public class ArabicNormalizer implements TextNormalizer {

  @Override
  public String normalize(String input) {
    
    String text = ArabicTextNormalizerUtil.normalizeDiacritics(input);
    text = ArabicTextNormalizerUtil.normalizeTatweel(text);
    return text;
  }

}
