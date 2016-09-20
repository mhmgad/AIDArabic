package mpi.aida.config;

import mpi.aida.access.DatabaseDMap;
import mpi.aida.util.ClassPathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Properties;

/**
 * Configuration for the DMap based dataaccess.
 */
public class DMapConfig {
  private static final Logger logger =
    LoggerFactory.getLogger(DMapConfig.class);
  
  public static final String DIRECTORY = "directory";
  public static final String DIRECTORY_DEFAULT = "dMaps";

  public static final String MAPS_TO_LOAD = "mapsToLoad";
  public static final String MAPS_TO_LOAD_DEFAULT = "all";

  public static final String PRELOAD_KEYS_POSTFIX = "preloadKeys";
  public static final boolean PRELOAD_KEYS_DEFAULT = true;

  public static final String PRELOAD_VALUES_POSTFIX = "preloadValues";
  public static final boolean PRELOAD_VALUES_DEFAULT = false;
  
  public static final String DEFAULT_PREFIX = "default";

  private Properties properties;

  public static final String PATH = "dmap_aida.properties";

  private static DMapConfig config = null;

  private DMapConfig() {
    try {
      properties = ClassPathUtils.getPropertiesFromClasspath(PATH);
    } catch (Exception e) {
      properties = new Properties();
      logger.error("DMap settings file missing. " +
        "Copy 'sample_settings/dmap_aida.properties' to the 'settings/' " +
        "directory and adjust it.");
    }
  }

  private static DMapConfig getInstance() {
    if (config == null) {
      config = new DMapConfig();
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

  public static File getDirectory() {
    if (DMapConfig.getInstance().hasKey(DIRECTORY)) {
      return new File(DMapConfig.getInstance().getValue(DIRECTORY));
    }
    logger.warn("Key '" + DIRECTORY + "' was not found in '" + PATH + "' " +
      "fallback to default value: " + DIRECTORY_DEFAULT);
    return new File(DIRECTORY_DEFAULT);
  }
  
  public static boolean shouldPreloadKeys(DatabaseDMap databaseDMap) {
    String key = databaseDMap.getName() + "." + PRELOAD_KEYS_POSTFIX;
    if (DMapConfig.getInstance().hasKey(key)) {
      return Boolean.parseBoolean(DMapConfig.getInstance().getValue(key));
    }
    String defaultKey = DEFAULT_PREFIX + "." + PRELOAD_KEYS_POSTFIX;
    if (DMapConfig.getInstance().hasKey(defaultKey)){
      return Boolean.parseBoolean(DMapConfig.getInstance().getValue(defaultKey));
    }
    logger.warn("Key '" + key + "' or '" + defaultKey + "' was not found in '" + PATH + "' " +
      "fallback to default value: " + PRELOAD_KEYS_DEFAULT);
    return PRELOAD_KEYS_DEFAULT;
  }

  public static boolean shouldPreloadValues(DatabaseDMap databaseDMap) {
    String key = databaseDMap.getName() + "." + PRELOAD_VALUES_POSTFIX;
    if (DMapConfig.getInstance().hasKey(key)) {
      return Boolean.parseBoolean(DMapConfig.getInstance().getValue(key));
    }
    String defaultKey = DEFAULT_PREFIX + "." + PRELOAD_VALUES_POSTFIX;
    if (DMapConfig.getInstance().hasKey(defaultKey)){
      return Boolean.parseBoolean(DMapConfig.getInstance().getValue(defaultKey));
    }
    logger.warn("Key '" + key + "' or '" + defaultKey + "' was not found in '" + PATH + "' " +
      "fallback to default value: " + PRELOAD_VALUES_DEFAULT);
    return PRELOAD_VALUES_DEFAULT;
  }

  public static boolean shouldLoadMap(DatabaseDMap databaseDMap) {
    String mapsToLoad;
    if (DMapConfig.getInstance().hasKey(MAPS_TO_LOAD))
      mapsToLoad = DMapConfig.getInstance().getValue(MAPS_TO_LOAD);
    else mapsToLoad = MAPS_TO_LOAD_DEFAULT;
    if (mapsToLoad.equals("all"))
      return true;
    else {
      String[] mapNames = mapsToLoad.split(" *, *");
      for (String curMapName : mapNames) {
        if (curMapName.equals(databaseDMap.getName()))
          return true;
      }
      return false;
    }
  }
}
