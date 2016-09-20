 package mpi.aida.graph.similarity.util;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import mpi.aida.config.AidaConfig;
import mpi.aida.data.Entities;
import mpi.aida.graph.similarity.context.EntitiesContext;
import mpi.aida.graph.similarity.context.EntitiesContextSettings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 
 * Caches entity contexts based on the context id and document id.
 * Assumes distinct document ids and caches up to ecc contexts.
 * 
 * DEPRECATED: Caching is now done on a per-entity granularity at the
 * DataAccess level.
 */
@Deprecated
public class EntitiesContextCreator {  
  private Logger logger_ = LoggerFactory.getLogger(EntitiesContextCreator.class);
  
  private int cacheSize = 1;
  
  /** Holds the cached EntityContexts. */
  private Map<String, EntitiesContext> cache = 
      new HashMap<String, EntitiesContext>();
  
  /** 
   * Keeps the order in which the EntityContexts were created for 
   * discarding the least recently used on cache overflow.
   */
  private List<String> cacheIds = new LinkedList<String>();

  /**
   * Synchronized the creation of different contexts. Allows the parallel
   * creation of contexts for distinct documents but blocks for requests
   * of the same context. 
   */
  private Map<String, Lock> contextCreationLocks = new HashMap<String, Lock>();
  
  private static class EntitiesContextCreatorHolder {
    public static EntitiesContextCreator ecc = new EntitiesContextCreator();
  }
  
  public static EntitiesContextCreator getEntitiesContextCache() {
    return EntitiesContextCreatorHolder.ecc;
  }
  
  public EntitiesContextCreator() {
    int size = AidaConfig.getAsInt(AidaConfig.ENTITIES_CONTEXT_CACHE_SIZE);
    // Has to be at least 1.
    if (size < 1) {
      logger_.warn("entitiesContextCacheSize must be at least 1, setting to 1.");
      size = 1;
    }
    cacheSize = size;
  }
    
  public EntitiesContext getEntitiesContext(
      String contextClassName, String docId, Entities entities, 
      EntitiesContextSettings entitiesContextSettings) 
          throws Exception {
    
    String id = getCacheId(contextClassName, docId, entitiesContextSettings);
    
    // Allow the parallel creation of distinct contexts but only
    // one creation per id.
    Lock contextLock = getContextCreationLock(id);
    contextLock.lock();
    EntitiesContext context = null;
    try {
      context = cache.get(id);
      
      if (context == null) {
        // Create context.
        context = 
            (EntitiesContext) 
            Class.forName(contextClassName).
              getDeclaredConstructor(
                  Entities.class, EntitiesContextSettings.class).newInstance(
                      entities, entitiesContextSettings);
        
        // Put it into the cache, deleting the oldest cache if the cache
        // size is exceeded.
        synchronized(cache) {
          cache.put(id, context);
          cacheIds.add(id);
          
          if (cacheIds.size() > cacheSize) {
            String removedId = cacheIds.get(0);
            cacheIds.remove(0);
            cache.remove(removedId);
          }
        }
      }
    } catch (Exception e) {
      throw e;    
    } finally {
      contextLock.unlock();
    }
    
    // Will be null if something goes wrong in the creation process.
    return context;
  }
  
  /**
   * Creates the id so that contexts will be reusable. MentionEntitySimilarity
   * and EntityEntitySimilarity contexts will not be shared as they might
   * have different configurations.
   * 
   * @param contextClassName
   * @param docId
   * @param entitiesContextSettings
   * @return
   */
  private String getCacheId(
      String contextClassName, String docId, 
      EntitiesContextSettings entitiesContextSettings) {
    return contextClassName + "\t" + docId + "\t" + 
           entitiesContextSettings.getEntitiesContextType();
  }

  private synchronized Lock getContextCreationLock(String id) {
    Lock lock = contextCreationLocks.get(id);
    if (lock == null) {
      lock = new ReentrantLock();
      contextCreationLocks.put(id, lock);
    }
    return lock;
  }
}
