package mpi.aida.service.web.logger;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mpi.aida.AidaManager;
import mpi.aida.util.DBUtil;


public class WebCallLogger {
  
    private static final String AIDA_CALL_LOGGER = "call_logger";

    private static Logger logger = LoggerFactory.getLogger(WebCallLogger.class);

    public static void log(String input, String output, String prepSettings, String technique, String algo) {
      Connection loggerCon = null;
      try {
        loggerCon = 
            AidaManager.getConnectionForDatabase(AidaManager.DB_WEBSERVICE_LOGGER);
        loggerCon.setAutoCommit(false);
        PreparedStatement statementCallLog = DBUtil.getAutoExecutingPeparedStatement(loggerCon, "INSERT INTO " + AIDA_CALL_LOGGER + "(request_text, json_response, preparation_settings, disambiguation_technique, disambiguation_algorithm, processed_time) "
            + "VALUES(?, ?, ?, ?, ?, NOW())", 1);
        
        statementCallLog.setString(1, input);
        statementCallLog.setString(2, output);
        statementCallLog.setString(3, prepSettings);
        statementCallLog.setString(4, technique);
        statementCallLog.setString(5, algo);
        
        statementCallLog.addBatch();
        
        statementCallLog.executeBatch();
        loggerCon.commit();
        statementCallLog.close();
        logger.info("Logged Service Request..");
      } catch (Exception e) {
        logger.error(e.getLocalizedMessage());
        e.printStackTrace();
      } finally {
        AidaManager.releaseConnection(loggerCon);
      }

    }    
}
