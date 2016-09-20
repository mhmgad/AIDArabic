package mpi.ner.config;

import java.io.IOException;
import java.util.Properties;

import mpi.aida.util.ClassPathUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration class for NER. Read from 'ner.properties' on classpath.
 */
public class NERConfig {
  
  private Properties prop_;
  
  private static Logger logger_ = LoggerFactory.getLogger(NERConfig.class);

  public static final String TAGGERS = "taggers";

  public static final String MENTION_FILTER_REMOVESINGLECHAR = "mention.filter.removesinglechar";

  public static final String MENTION_FILTER_REMOVEIFBEGINNINGOFSENTENCE = "mention.filter.removeifbeginningofsentence";
    
  private static class NERConfigHolder {
    public static NERConfig config = new NERConfig();
  }

  public static NERConfig singleton() {
    return NERConfigHolder.config;
  }
  
  private NERConfig() {
    String nerSettingsFile = "ner.properties";
    try {
      prop_ = ClassPathUtils.getPropertiesFromClasspath(nerSettingsFile);
    } catch (IOException e) {
      logger_.error("Couldn't load NER Configuration from: "
          + nerSettingsFile);
      e.printStackTrace();
    }
  }
  
  public static String get(String key) {
    String value = null;
    if (singleton().hasKey(key)) {
      value = singleton().getValue(key);
    } else {
      if (key.equals(MENTION_FILTER_REMOVESINGLECHAR)) {
        return "false";
      } else if (key.equals(MENTION_FILTER_REMOVEIFBEGINNINGOFSENTENCE)) {
        return "false";
      } else {
        logger_.error("Missing key in ner.properties file with no default value: " + key);
      }
    }
    return value;
  }

  public boolean hasKey(String key) {
    return prop_.containsKey(key);
  }

  public String getValue(String key) {
    return prop_.getProperty(key);
  }

  public static boolean getBoolean(String key) {
    return Boolean.parseBoolean(get(key));
  }

  public static String get(String key, String def) {
    return singleton().prop_.getProperty(key, def);
  }

  public static void set(String key, String value) {
    singleton().prop_.setProperty(key, value);
  }
}
