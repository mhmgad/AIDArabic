package mpi.aida.graph.similarity.importance;

import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntIntHashMap;

import java.sql.SQLException;

import mpi.aida.data.Entities;
import mpi.aida.data.Entity;
import mpi.aida.util.MathUtil;
import mpi.tools.javatools.datatypes.Pair;


public abstract class CountBasedImportance extends EntityImportance {
  
  protected TIntDoubleHashMap importances;
  
  private int min;
  private int max;


  public CountBasedImportance(Entities entities) throws SQLException {
    super(entities);
  }
  
  @Override
  protected void setupEntities(Entities entities) throws SQLException {
    importances = new TIntDoubleHashMap();
    Pair<Integer, Integer> minMax = loadMinMax();
    min = minMax.first();
    max = minMax.second();
    TIntIntHashMap counts = loadCounts(entities);
    for (int eId : entities.getUniqueIds()) {
      if(counts.containsKey(eId)) {
        int count = counts.get(eId);
        double importance =  MathUtil.rescale(count, min, max);
        importances.put(eId, importance);
      }
    }
  }
  
  @Override
  public double getImportance(Entity entity) {
    int id = entity.getId();
    if(importances.contains(id)) {
      return importances.get(id);
    } else {
      // Do not differentiate between a missing entity and a 0 score.
      return 0.0;
    }
  }


  protected abstract Pair<Integer, Integer> loadMinMax();
  protected abstract TIntIntHashMap loadCounts(Entities entities);

}
