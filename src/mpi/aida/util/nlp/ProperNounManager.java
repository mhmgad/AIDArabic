package mpi.aida.util.nlp;

import mpi.aida.config.AidaConfig;
import mpi.aida.util.ClassPathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility methods for dealing with language-specific proper nouns.
 */
public class ProperNounManager {
  Logger logger_ = LoggerFactory.getLogger(ProperNounManager.class);

  /**
   * Singleton stuff.
   */
  private static class ProperNounManagerHolder {
    public static ProperNounManager pnm = new ProperNounManager();
  }

  /**
   * Singleton stuff.
   */
  public static ProperNounManager singleton() {
    return ProperNounManagerHolder.pnm;
  }

  private Set<String> properNounTags_;

  public ProperNounManager() {
    String path = "tokens/pos/propernoun/" + AidaConfig.getLanguage().name() + ".txt";
    try {
      List<String> properNouns = ClassPathUtils.getContent(path);
      properNounTags_ = new HashSet<>(properNouns);
    } catch (IOException e) {
      logger_.warn("Could not load proper noun tags for language '" + AidaConfig.getLanguage().name() +
              "' from '" + path + "'.");
    }
  }

  public boolean isProperNounTag(String tag) {
    return properNounTags_.contains(tag);
  }
}
