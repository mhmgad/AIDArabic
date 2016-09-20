package mpi.aida.graph.similarity.context.lsh.preprocessing;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import mpi.aida.AidaManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectionTest {
  private static final Logger logger = 
      LoggerFactory.getLogger(ConnectionTest.class);
  
	public static void main(String args[]){
	  AidaManager.init();
	  
		Connection con = null;
		try {
			String ENTITY_KEYPHRASES= "entity_keyphrases";
			con = AidaManager.getConnectionForDatabase(AidaManager.DB_AIDA);
			Statement statement = con.createStatement();
			con.setAutoCommit(false);
			statement = con.createStatement();
			statement.setFetchSize(100000);

			String sql = "SELECT entity,keyphrase FROM " + ENTITY_KEYPHRASES;
			ResultSet rs = statement.executeQuery(sql);

			while (rs.next()) {
				String e = rs.getString("entity");
			    String kp = rs.getString("keyphrase");
			    // YOUR CODE GOES HERE
			    System.out.println(e + ": " + kp);
			}

			rs.close();
			statement.close();
			} catch (Exception e) {
				logger.error(e.getLocalizedMessage());
			} finally {
				AidaManager.releaseConnection(con);
			}
	}
}
