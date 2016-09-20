package mpi.aida.util.normalization;

import mpi.aida.config.AidaConfig;


public class TextNormalizerManager {

  private static class Holder {
    static final TextNormalizerManager INSTANCE = new TextNormalizerManager();
  }

  private static TextNormalizerManager getInstance() {
    return Holder.INSTANCE;
  }
  
  private TextNormalizer normalizer;
  
  private TextNormalizerManager() {
    switch(AidaConfig.getLanguage()) {
      case ar:
        normalizer = new ArabicNormalizer();
        break;
      default:
        normalizer = new DummyNormalizer();
        break;
    }
    
  }
  
  public static String normalize(String text) {
    return TextNormalizerManager.getInstance().normalizer.normalize(text);
  }



}
