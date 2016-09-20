package mpi.aida.cachepreloader;

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mpi.aida.access.DataAccess;
import mpi.aida.data.Entities;
import mpi.aida.data.Entity;


public class EntityKeyphrasePreloader {
  
  private static final Logger logger = 
      LoggerFactory.getLogger(EntityKeyphrasePreloader.class);
  
  public static void cacheAllEntities() {
    logger.info("Loading all entities keyphrases...");
    int limit = 100000;
    int count = 0;
    int total = 0;
    Entities entities = DataAccess.getAllEntities();
    // This should pre-load all entity keyphrases in DataAccessSQLCache
    Entities tmpEntities = new Entities();
    for(Entity entity : entities.getEntities()) {
      tmpEntities.add(entity);
      if(++count >= limit) {
        total += count;
        logger.info("Cached " + total + " entities(with keyphrases)");
        DataAccess.getEntityKeyphrases(tmpEntities, new HashMap<String,Double>(), 0.0 ,0);
        tmpEntities = new Entities();
        count = 0;
      }
    }
    logger.info("Done loading cache. Entities loaded : "  +entities.size());
  }
}
