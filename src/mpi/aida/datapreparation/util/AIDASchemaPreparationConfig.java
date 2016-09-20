package mpi.aida.datapreparation.util;

import mpi.aida.access.DatabaseDMap;
import mpi.aida.util.ClassPathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public class AIDASchemaPreparationConfig {

  private static final Logger logger = LoggerFactory.getLogger(AIDASchemaPreparationConfig.class);

  public static final String CONFIGURAITON_NAME = "configurationName";
  
  public static final String YAGO3_FILE_LOCATION = "yago3FileLocation";
  
  public static final String TARGET_LANGUAGES = "targetLanguages";
  
  /**
   * The directory where the .dmap and .proto files are saved
   * => default: dMaps
   */
  public static final String DMAP_TARGET_DIRECTORY = "dMapTargetDirectory";

  /**
   * The package where the classes from the .proto files should go
   * => required
   */
  public static final String DMAP_PROTO_CLASSES_TARGET_PACKAGE = "dMapProtoClassesTargetPackage";

  /**
   * The root directory for the package path
   * => default: src
   */
  public static final String AIDA_SOURCE_FOLDER = "aidaSourceFolder";

  /**
   * Path to the protoc executable
   * => default: protoc
   */
  public static final String PROTOC_PATH = "protocPath";

  /**
   * Defines the compress behaviour
   * => default: true
   */
  public static final String COMPRESS_VALUES_POSTFIX = "compressValues";
  public static final String DEFAULT_PREFIX = "default";
  
  /**
   * Flag to create the entity_keywords and keyword_counts table
   * => default: true
   */
  public static final String CREATE_KEYWORDS = "createKeywords";

  /**
   * Flag to create the entity_bigrams and bigram_counts table
   * => default: true
   */
  public static final String CREATE_BIGRAMS = "createBigrams";

  /**
   * The maximum number of keyphrases that should be taken for unit creation
   * => default: 0 (all)
   */
  public static final String UNIT_CREATION_THRESHOLD_TOPK = "unitCreationThresholdTopk";

  /**
   * The maximum weight of the keyphrases that should be taken for unit creation
   * => default: 0.0 (all)
   */
  public static final String UNIT_CREATION_THRESHOLD_MIN_WEIGHT = "unitCreationThresholdMinWeight";

  public static final String DICTIONARY_MENTION_PREFIX_THRESHOLD = "dictionary.mention.prefixthreshold";

  /**
   * Minimum count an anchor has to link to an entity to be included in the dictionary.
   */
  public static final String DICTIONARY_ANCHORS_MINOCCURRENCE = "dictionary.anchors.minoccurrence";

  private Properties properties;

  private String path = "preparation.properties";

  private static AIDASchemaPreparationConfig config = null;

  private AIDASchemaPreparationConfig() {
    properties = new Properties();
    try {
      properties = ClassPathUtils.getPropertiesFromClasspath(path);
    } catch (Exception e) {
      properties = new Properties();
      logger.error("Schema Preparation config file missing. " + "Copy 'sample_settings/preparation.properties' to the 'settings/' "
          + "directory and adjust it.");
    }
  }

  private static AIDASchemaPreparationConfig getInstance() {
    if (config == null) {
      config = new AIDASchemaPreparationConfig();
    }
    return config;
  }

  private String getValue(String key) {
    return (String) properties.get(key);
  }

  private void setValue(String key, String value) {
    properties.setProperty(key, value);
  }

  private boolean hasKey(String key) {
    return properties.containsKey(key);
  }

  public static String get(String key) {
    String value = null;
    if (AIDASchemaPreparationConfig.getInstance().hasKey(key)) {
      value = AIDASchemaPreparationConfig.getInstance().getValue(key);
    } else {//default values go here
      switch (key) {
        case DMAP_TARGET_DIRECTORY:
          return "dMaps";
        case AIDA_SOURCE_FOLDER:
          return "src";
        case PROTOC_PATH:
          return "protoc";
        case CREATE_KEYWORDS:
          return "true";
        case CREATE_BIGRAMS:
          return "true";
        case DICTIONARY_MENTION_PREFIX_THRESHOLD:
          return "1.0";
        case DEFAULT_PREFIX + "." + COMPRESS_VALUES_POSTFIX:
          return "true";
        case DICTIONARY_ANCHORS_MINOCCURRENCE:
          return "0";
        case UNIT_CREATION_THRESHOLD_TOPK:
          return "0";
        case UNIT_CREATION_THRESHOLD_MIN_WEIGHT:
          return "0.0";
      }
    }
    return value;
  }

  public static boolean getBoolean(String key) {
    return Boolean.parseBoolean(get(key));
  }

  public static Integer getInteger(String key) {
    return Integer.parseInt(get(key));
  }
  
  public static Double getDouble(String key) {
    return Double.parseDouble(get(key));
  }
  
  public static void set(String key, String value) {
    AIDASchemaPreparationConfig.getInstance().setValue(key, value);
  }

  public static String getConfigurationName() {
    return get(CONFIGURAITON_NAME);
  }

  public static Set<String> getTargetLanguages() {
    String languages = get(TARGET_LANGUAGES);
    String languageArray[] = languages.split(",");
    Set<String> targetLanguages = new HashSet<>();
    for (String lang : languageArray) {
      targetLanguages.add(lang);
    }
    return targetLanguages;
  }

  public static boolean shouldCompressValues(DatabaseDMap databaseDMap) {
    String value = get(databaseDMap.getName() + "." + COMPRESS_VALUES_POSTFIX);
    if (value == null)
      value = get(DEFAULT_PREFIX + "." + COMPRESS_VALUES_POSTFIX);
    return Boolean.parseBoolean(value);
  }
}
