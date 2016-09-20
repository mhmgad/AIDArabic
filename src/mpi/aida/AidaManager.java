package mpi.aida;

import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.sql.DataSource;

import mpi.aida.access.DataAccess;
import mpi.aida.data.Entities;
import mpi.aida.data.Entity;
import mpi.aida.data.ExternalEntitiesContext;
import mpi.aida.data.KBIdentifiedEntity;
import mpi.aida.data.Mention;
import mpi.aida.data.Mentions;
import mpi.aida.util.ClassPathUtils;
import mpi.aida.util.YagoUtil.Gender;
import mpi.aida.util.timing.RunningTimer;
import mpi.experiment.trace.Tracer;
import mpi.experiment.trace.data.EntityTracer;
import mpi.experiment.trace.data.MentionTracer;
import mpi.tokenizer.data.TokenizerManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class AidaManager {

  static {
    // Always need to do this.
    init();
  }

  private static Logger slogger_ = LoggerFactory.getLogger(AidaManager.class);

  // This is more couple to SQL than it should be. Works for now.
  public static final String DB_AIDA = "DatabaseAida";

  public static final String DB_YAGO2 = "DatabaseYago";

  public static final String DB_YAGO3 = "DatabaseYago3";

  public static final String DB_YAGO2_FULL = "DatabaseYago2Full";

  public static final String DB_YAGO2_SPOTLX = "DatabaseYago2SPOTLX";

  public static final String DB_RMI_LOGGER = "DatabaseRMILogger";

  public static final String DB_HYENA = "DatabaseHYENA";

  public static final String DB_WEBSERVICE_LOGGER = "DatabaseWSLogger";

  public static String databaseAidaConfig = "database_aida.properties";

  private static String databaseYago2Config = "database_yago2.properties";

  public static String databaseYago3Config = "database_yago3.properties";

  private static String databaseYAGO2SPOTLXConfig = "database_yago2spotlx.properties";

  private static String databaseRMILoggerConfig = "databaseRmiLogger.properties";

  private static String databaseWSLoggerConfig = "databaseWsLogger.properties";

  private static String databaseHYENAConfig = "database_hyena.properties";

  public static final String WIKIPEDIA_PREFIX = "http://en.wikipedia.org/wiki/";

  public static final String YAGO_PREFIX = "http://yago-knowledge.org/resource/";

  private static Map<String, String> dbIdToConfig = new HashMap<String, String>();

  private static Map<String, Properties> dbIdToProperties = new HashMap<String, Properties>();

  private static Map<String, DataSource> dbIdToDataSource = new HashMap<>();

  static {
    dbIdToConfig.put(DB_AIDA, databaseAidaConfig);
    dbIdToConfig.put(DB_YAGO2, databaseYago2Config);
    dbIdToConfig.put(DB_YAGO3, databaseYago3Config);
    dbIdToConfig.put(DB_YAGO2_SPOTLX, databaseYAGO2SPOTLXConfig);
    dbIdToConfig.put(DB_RMI_LOGGER, databaseRMILoggerConfig);
    dbIdToConfig.put(DB_HYENA, databaseHYENAConfig);
    dbIdToConfig.put(DB_WEBSERVICE_LOGGER, databaseWSLoggerConfig);
  }

  private static AidaManager tasks = null;

  public static enum language {
    english, german
  }

  public static void init() {
    getTasksInstance();
  }

  private static synchronized AidaManager getTasksInstance() {
    if (tasks == null) {
      tasks = new AidaManager();
    }
    return tasks;
  }  

  public static String getAidaDbIdentifier() throws SQLException {
    // Make sure properties are loaded.
    Connection con = getConnectionForDatabase(DB_AIDA);
    releaseConnection(con);

    // Get database name
    Properties prop = dbIdToProperties.get(DB_AIDA);
    return prop.getProperty("dataSource.databaseName");
  }

  public static Connection getConnectionForDatabase(String dbId) throws SQLException {
    Properties prop = dbIdToProperties.get(dbId);
    if (prop == null) {
      try {
        prop = ClassPathUtils.getPropertiesFromClasspath(dbIdToConfig.get(dbId));
        dbIdToProperties.put(dbId, prop);
      } catch (IOException e) {
        throw new SQLException(e);
      }
    }
    return getConnectionForNameAndProperties(dbId, prop);
  }

  public static Connection getConnectionForNameAndProperties(String dbId, Properties prop) throws SQLException {
    DataSource ds = null;
    synchronized (dbIdToDataSource) {
      ds = dbIdToDataSource.get(dbId);
      if (ds == null) {
        try {
          String serverName = prop.getProperty("dataSource.serverName");
          String database = prop.getProperty("dataSource.databaseName");
          String username = prop.getProperty("dataSource.user");
          String port = prop.getProperty("dataSource.portNumber");
          if (port == null) {
            port = "5432"; // Standard postgres port.
          }
          slogger_.info("Connecting to database " + username + "@" + serverName + ":" + port + "/" + database);

          HikariConfig config = new HikariConfig(prop);
          ds = new HikariDataSource(config);
          dbIdToDataSource.put(dbId, ds);
        } catch (Exception e) {
          slogger_.error("Error connecting to the database: " + e.getLocalizedMessage());
          e.printStackTrace();
        }
      }
    }
    if (ds == null) {
      slogger_.error("Could not connect to the database. " +
          "Please check the settings in '" + dbIdToConfig.get(dbId) +
          "' and make sure the Postgres server is up and running.");
      return null;
    }
    Integer id = RunningTimer.recordStartTime("getConnection");
    Connection connection = ds.getConnection();
    RunningTimer.recordEndTime("getConnection", id);
    return connection;
  }

  public static Properties getDatabaseProperties(
      String hostname, Integer port, String username, String password,
      Integer maxCon, String database) {
    Properties prop = new Properties();
    prop.put("dataSourceClassName", "org.postgresql.ds.PGSimpleDataSource");
    prop.put("maximumPoolSize", maxCon);
    prop.put("dataSource.user", username);
    prop.put("dataSource.password", password);
    prop.put("dataSource.databaseName", database);
    prop.put("dataSource.serverName", hostname);
    prop.put("dataSource.portNumber", port);
    return prop;
  }

  public static void releaseConnection(Connection con) {
    try {
      con.close();
    } catch (SQLException e) {
      slogger_.error("Could not release connection: " + e.getLocalizedMessage());
    }
  }

  /**
   * Gets an AIDA entity for the given entity id.
   * This is slow, as it accesses the DB for each call.
   * Do in batch using DataAccess directly for a larger number
   * of entities.
   * 
   * @return              AIDA Entity
   */
  public static Entity getEntity(KBIdentifiedEntity entity) {
    int id = DataAccess.getInternalIdForKBEntity(entity);
    return new Entity(entity, id);
  }

  public static Entities getEntities(Set<KBIdentifiedEntity> kbEntities) {
    TObjectIntHashMap<KBIdentifiedEntity> ids =
        DataAccess.getInternalIdsForKBEntities(kbEntities);
    Entities entities = new Entities();
    for(TObjectIntIterator<KBIdentifiedEntity> itr = ids.iterator(); itr.hasNext(); ) {
      itr.advance();
      entities.add(new Entity(itr.key(), itr.value()));
    }
    return entities;
  }

  /**
   * Gets an AIDA entity for the given AIDA entity id.
   * This is slow, as it accesses the DB for each call.
   * Do in batch using DataAccess directly for a larger number
   * of entities.
   * 
   * @param entityId  Internal AIDA int ID
   * @return          AIDA Entity
   */
  public static Entity getEntity(int entityId) {
    KBIdentifiedEntity kbEntity = DataAccess.getKnowlegebaseEntityForInternalId(entityId);
    return new Entity(kbEntity, entityId);
  }

  /**
   * Extracts all candidate entities from a Mentions collection, not including the ones that have been
   * passed as ExternalEntitiesContext. It also adds tracers if a Tracer has been passed.
   *
   * @param mentions mentions to extract candidates from.
   * @param externalContext Context that has been passed externally.
   * @param tracer  Tracer to trace mention-entity objects.
   * @return All (non-external) candidate entities of the input.
   */
  public static Entities getAllEntities(
          Mentions mentions, ExternalEntitiesContext externalContext, Tracer tracer) {
    Entities entities = new Entities();
    for (Mention mention : mentions.getMentions()) {
      MentionTracer mt = new MentionTracer(mention);
      tracer.addMention(mention, mt);
      for (Entity entity : mention.getCandidateEntities()) {
        EntityTracer et = new EntityTracer(entity.getId());
        tracer.addEntityForMention(mention, entity.getId(), et);
      }
      for (Entity entity : mention.getCandidateEntities()) {
        if (!externalContext.contains(entity)) {
          entities.add(entity);
        }
      }
    }
    return entities;
  }

  public static TIntObjectHashMap<Gender> getGenderForEntities(Entities entities) {
    return DataAccess.getGenderForEntities(entities);
  }

  private AidaManager() {
    TokenizerManager.init();
  }

  /**
   * Uppercases a token of more than 4 characters. This is
   * used as pre-processing method during name recognition
   * and dictionary matching mainly.
   *
   * @param token Token to check for conflation.
   * @return  ALL-UPPERCASE token if token is longer than 4 characters.
   */
  public static String conflateToken(String token) {
    if (token.length() >= 4) {
      token = token.toUpperCase(Locale.ENGLISH);
    }

    return token;
  }

}
