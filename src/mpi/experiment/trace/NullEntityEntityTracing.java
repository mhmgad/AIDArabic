package mpi.experiment.trace;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.Collection;

import mpi.aida.data.EntityMetaData;
import mpi.experiment.trace.measures.MeasureTracer;


public class NullEntityEntityTracing extends EntityEntityTracing {

  @Override
  public String generateOutput(TIntObjectHashMap<EntityMetaData> emd) {
    return "";
  }

  @Override
  public void addEntityEntityMeasureTracer(int e1, int e2, MeasureTracer mt) {
  }

  @Override
  public void setCorrectEntities(Collection<Integer> correctEntities) {
  }
}
