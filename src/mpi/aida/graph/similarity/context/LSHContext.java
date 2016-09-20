package mpi.aida.graph.similarity.context;

import edu.stanford.nlp.util.StringUtils;
import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.hash.TIntHashSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import mpi.aida.AidaManager;
import mpi.aida.access.DataAccess;
import mpi.aida.data.Entities;
import mpi.aida.data.Entity;
import mpi.aida.graph.similarity.context.lsh.IntHashTable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LSHContext extends FastWeightedKeyphrasesContext {
  private static final Logger logger = 
      LoggerFactory.getLogger(LSHContext.class);
  
  public Integer relcount = 0;
  
  private IntHashTable intHashTable;
//  private HashTable hashTable;
  
  private TIntObjectHashMap<TIntHashSet> relatedEntities;
  
  public LSHContext(Entities entities, EntitiesContextSettings settings) throws Exception {
    super(entities, settings);
  }

  @Override
  protected void setupEntities(Entities entities) throws Exception {
    super.setupEntities(entities);
    
    // get the entity-representation
    TIntObjectHashMap<int[]> sigs = null;
    if (settings != null) {
      sigs = DataAccess.getEntityLSHSignatures(entities, settings.getLshDatabaseTable());
    } else {
      sigs = DataAccess.getEntityLSHSignatures(entities);
    }
    
    // setup the LSH table
    int lshBandSize = 2;
    int lshBandCount = 100;
    
    if (settings != null) {
      lshBandSize = settings.getLshBandSize();
      lshBandCount = settings.getLshBandCount();
    }
        
    long start = System.currentTimeMillis();
    intHashTable = new IntHashTable(sigs, lshBandSize, lshBandCount);
    
    // generate the LSH table from all entities in this context 
    intHashTable.put(entities.getUniqueIds());
    System.out.println("#entities: " + entities.size());
    System.out.println("#ids: " + entities.getUniqueIds().size());

    // populate related entities
    relatedEntities = intHashTable.getAllRelatedPairs();
    long duration = System.currentTimeMillis() - start;
    for (TIntObjectIterator<TIntHashSet> itr = relatedEntities.iterator();
        itr.hasNext(); ) {
      itr.advance();
      Entity key = AidaManager.getEntity(itr.key());
      List<String> names = new ArrayList<String>();
      for (int e : itr.value().toArray()) {
        names.add(AidaManager.getEntity(e).toString());
      }
      Collections.sort(names);
      logger.info("Entity: " + key + "\n" + "Related: " + StringUtils.join(names, ", "));
    }
    logger.debug("SETUP TIME:" + duration);
  }
  
  /**
   * Checks if two entities are related using their LSH signature
   * 
   * @param a First entity
   * @param b Second entity
   * @return  true if LSH signature overlaps, false otherwise
   */
  public boolean isRelated(Entity a, Entity b) {
    // map is ordered
    int oneId = a.getId();
    int twoId = b.getId();
    
    if (twoId < oneId) {
      int tmp = oneId;
      oneId = twoId;
      twoId = tmp;
    }
    
    TIntHashSet related = relatedEntities.get(oneId);
    
    if (related != null) {
      synchronized (relcount) {
        ++relcount;
        System.out.println("COUNT " + relcount);
      }
      return related.contains(twoId);
    } else {
      return false;
    }
  }
}
