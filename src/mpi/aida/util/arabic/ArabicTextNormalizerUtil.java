package mpi.aida.util.arabic;

import java.util.HashMap;
import java.util.Map;

public class ArabicTextNormalizerUtil {
  
  private static class Holder {
    static final ArabicTextNormalizerUtil INSTANCE = new ArabicTextNormalizerUtil();
  }

  private static ArabicTextNormalizerUtil getInstance() {
    return Holder.INSTANCE;
  }

  private static Map<String, String> junkCharatcers;

  private static Map<String, String> digits;

  private static Map<String, String> punctuation;

  private static Map<String, String> alif;

  private static Map<String, String> ya;

  private static Map<String, String> diacritics;

  private static Map<String, String> tatweel;

  private static Map<String, String> quranSymbols;

  private static Map<String, String> abtEscpaing;

  private ArabicTextNormalizerUtil() {
    setupNormalizationMap();
  }


  public static String normalizeJunkCharacters(String text) {
    return ArabicTextNormalizerUtil.getInstance().normalize(text, junkCharatcers);

  }

  public static String normalizeDigits(String text) {
    return ArabicTextNormalizerUtil.getInstance().normalize(text, digits);
  }

  public static String normalizePunctuation(String text) {
    return ArabicTextNormalizerUtil.getInstance().normalize(text, punctuation);
  }

  public static String normalizeAlif(String text) {
    return ArabicTextNormalizerUtil.getInstance().normalize(text, alif);
  }

  public static String normalizeYa(String text) {
    return ArabicTextNormalizerUtil.getInstance().normalize(text, ya);
  }

  public static String normalizeDiacritics(String text) {
    return ArabicTextNormalizerUtil.getInstance().normalize(text, diacritics);
  }

  public static String normalizeTatweel(String text) {
    return ArabicTextNormalizerUtil.getInstance().normalize(text, tatweel);
  }

  public static String normalizeQuranCharatcers(String text) {
    return ArabicTextNormalizerUtil.getInstance().normalize(text, quranSymbols);
  }

  public static String normalizeABTEscaping(String text) {
    return ArabicTextNormalizerUtil.getInstance().normalize(text, abtEscpaing);
  }
  
  private String normalize(String text, Map<String, String> replacementMap) {
    int len = text.length();
    StringBuilder sb = new StringBuilder(len);
    for (int i = 0; i < len; ++i) {
      String thisChar = String.valueOf(text.charAt(i));
      if (replacementMap.containsKey(thisChar)) {
        thisChar = replacementMap.get(thisChar);
      }
      if (thisChar.length() > 0) {
        sb.append(thisChar);
      }
    }
    return sb.toString();
  }

  private void setupNormalizationMap() {

    // Junk characters that we always remove
    junkCharatcers = new HashMap<>();
    junkCharatcers.put("\u0600", "#");
    junkCharatcers.put("\u0601", "");
    junkCharatcers.put("\u0602", "");
    junkCharatcers.put("\u0603", "");
    junkCharatcers.put("\u0606", "\u221B");
    junkCharatcers.put("\u0607", "\u221C");
    junkCharatcers.put("\u0608", "");
    junkCharatcers.put("\u0609", "%");
    junkCharatcers.put("\u060A", "%");
    junkCharatcers.put("\u060B", "");
    junkCharatcers.put("\u060E", "");
    junkCharatcers.put("\u060F", "");
    junkCharatcers.put("\u066E", "\u0628");
    junkCharatcers.put("\u066F", "\u0642");
    junkCharatcers.put("\u06CC", "\u0649");
    junkCharatcers.put("\u06D6", "");
    junkCharatcers.put("\u06D7", "");
    junkCharatcers.put("\u06D8", "");
    junkCharatcers.put("\u06D9", "");
    junkCharatcers.put("\u06DA", "");
    junkCharatcers.put("\u06DB", "");
    junkCharatcers.put("\u06DC", "");
    junkCharatcers.put("\u06DD", "");
    junkCharatcers.put("\u06DE", "");
    junkCharatcers.put("\u06DF", "");
    junkCharatcers.put("\u06E0", "");
    junkCharatcers.put("\u06E1", "");
    junkCharatcers.put("\u06E2", "");
    junkCharatcers.put("\u06E3", "");
    junkCharatcers.put("\u06E4", "");
    junkCharatcers.put("\u06E5", "");
    junkCharatcers.put("\u06E6", "");
    junkCharatcers.put("\u06E7", "");
    junkCharatcers.put("\u06E8", "");
    junkCharatcers.put("\u06E9", "");
    junkCharatcers.put("\u06EA", "");
    junkCharatcers.put("\u06EB", "");
    junkCharatcers.put("\u06EC", "");
    junkCharatcers.put("\u06ED", "");

    digits = new HashMap<>();
    digits.put("\u0660", "0");
    digits.put("\u0661", "1");
    digits.put("\u0662", "2");
    digits.put("\u0663", "3");
    digits.put("\u0664", "4");
    digits.put("\u0665", "5");
    digits.put("\u0666", "6");
    digits.put("\u0667", "7");
    digits.put("\u0668", "8");
    digits.put("\u0669", "9");
    digits.put("\u06F0", "0");
    digits.put("\u06F1", "1");
    digits.put("\u06F2", "2");
    digits.put("\u06F3", "3");
    digits.put("\u06F4", "4");
    digits.put("\u06F5", "5");
    digits.put("\u06F6", "6");
    digits.put("\u06F7", "7");
    digits.put("\u06F8", "8");
    digits.put("\u06F9", "9");

    punctuation = new HashMap<>();
    punctuation.put("\u00BB", "\"");
    punctuation.put("\u00AB", "\"");
    punctuation.put("\u060C", ",");
    punctuation.put("\u060D", ",");
    punctuation.put("\u061B", ";");
    punctuation.put("\u061E", ".");
    punctuation.put("\u061F", "?");
    punctuation.put("\u066A", "%");
    punctuation.put("\u066B", ",");
    punctuation.put("\u066C", "\u0027");
    punctuation.put("\u066F", "*");
    punctuation.put("\u06DF", ".");

    alif = new HashMap<>();
    alif.put("\u0622", "\u0627");
    alif.put("\u0623", "\u0627");
    alif.put("\u0625", "\u0627");
    alif.put("\u0671", "\u0627");
    alif.put("\u0672", "\u0627");
    alif.put("\u0673", "\u0627");

    ya = new HashMap<>();
    ya.put("\u064A", "\u0649");

    diacritics = new HashMap<>();
    diacritics.put("\u064B", "");
    diacritics.put("\u064C", "");
    diacritics.put("\u064D", "");
    diacritics.put("\u064E", "");
    diacritics.put("\u064F", "");
    diacritics.put("\u0650", "");
    diacritics.put("\u0651", "");
    diacritics.put("\u0652", "");
    diacritics.put("\u0653", "");
    diacritics.put("\u0654", "");
    diacritics.put("\u0655", "");
    diacritics.put("\u0656", "");
    diacritics.put("\u0657", "");
    diacritics.put("\u0658", "");
    diacritics.put("\u0659", "");
    diacritics.put("\u065A", "");
    diacritics.put("\u065B", "");
    diacritics.put("\u065C", "");
    diacritics.put("\u065D", "");
    diacritics.put("\u065E", "");
    diacritics.put("\u0670", "");

    tatweel = new HashMap<>();
    tatweel.put("\u0640", "");
    tatweel.put("_", "");

    quranSymbols = new HashMap<>();
    // Arabic honorifics
    quranSymbols.put("\u0610", "");
    quranSymbols.put("\u0611", "");
    quranSymbols.put("\u0612", "");
    quranSymbols.put("\u0613", "");
    quranSymbols.put("\u0614", "");
    quranSymbols.put("\u0615", "");
    quranSymbols.put("\u0616", "");
    quranSymbols.put("\u0617", "");
    quranSymbols.put("\u0618", "");
    quranSymbols.put("\u0619", "");
    quranSymbols.put("\u061A", "");

    abtEscpaing = new HashMap<>();
    abtEscpaing.put("(", "-LRB-");
    abtEscpaing.put(")", "-RRB-");
  }

}
