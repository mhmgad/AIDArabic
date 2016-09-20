package mpi.aida.datapreparation.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;

import mpi.aida.AidaManager;
import mpi.aida.util.DBUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Given a YAGO facts table, and a set of interesting entities, it produces a trimmed version by keeping only 
 * the relations the concern those interesting entities as well as all meta relations
 *
 */
public class YagoTrimmer {

  private static final Logger logger = LoggerFactory.getLogger(YagoTrimmer.class);

  private Set<String> interestingEntities;

  public YagoTrimmer(Set<String> interestingEntities) {
    this.interestingEntities = interestingEntities;
  }

  public void run(String fullTableName, String trimmedTableName) {
    Connection con = null;
    try {
      con = AidaManager.getConnectionForDatabase(AidaManager.DB_AIDA);

      logger.info("Creating the table:" + trimmedTableName);
      Statement stmt = con.createStatement();
      String sql = "CREATE TABLE " + trimmedTableName + " (id character varying, relation character varying, arg1 text, arg2 text )";
      stmt.execute(sql);
      stmt.close();
          
      String insertSql = "INSERT INTO " + trimmedTableName + "(id, relation, arg1, arg2) " + "VALUES(?, ?, ?, ?)";
      PreparedStatement insertStmt = DBUtil.getAutoExecutingPeparedStatement(con, insertSql, 10000);    
      
      Statement selectStatement = con.createStatement();
      con.setAutoCommit(false);
      selectStatement.setFetchSize(1000000);     
      
      sql = "select id, relation, arg1, arg2 from " + fullTableName;

      ResultSet r;
      r = selectStatement.executeQuery(sql);
      while (r.next()) {
        String id = r.getString(1);
        String relation = r.getString(2);
        String arg1 = r.getString(3);
        String arg2 = r.getString(4);

        boolean shouldInclude = false;

        if (arg1.startsWith("#")) {
          shouldInclude = true;
        } else if (interestingEntities.contains(arg1) || interestingEntities.contains(arg2)) {
          shouldInclude = true;
        }

        if (shouldInclude) {
          insertStmt.setString(1, id);
          insertStmt.setString(2, relation);
          insertStmt.setString(3, arg1);
          insertStmt.setString(4, arg2);
          insertStmt.addBatch();
        }
      }

      insertStmt.executeBatch();
      con.commit();
      insertStmt.close();
      
      r.close();
      selectStatement.close();
      con.setAutoCommit(true);

      logger.info("Creating Index.");
      stmt = con.createStatement();
      sql = "CREATE INDEX " + trimmedTableName + "arg1arg2_idx " + "ON " + trimmedTableName + "(arg1, arg2)";
      stmt.execute(sql);

      sql = "CREATE INDEX " + trimmedTableName + "arg1relation_idx " + "ON " + trimmedTableName + "(arg1, relation)";
      stmt.execute(sql);

      sql = "CREATE INDEX " + trimmedTableName + "arg2arg1_idx " + "ON " + trimmedTableName + "(arg2, arg1)";
      stmt.execute(sql);

      sql = "CREATE INDEX " + trimmedTableName + "arg2relation_idx " + "ON " + trimmedTableName + "(arg2, relation)";
      stmt.execute(sql);
      stmt.close();

      logger.info("Creating Index DONE.");

    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      AidaManager.releaseConnection(con);
    }

  }

}
