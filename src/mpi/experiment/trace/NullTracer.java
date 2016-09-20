package mpi.experiment.trace;

import mpi.aida.data.Mention;
import mpi.experiment.trace.data.EntityTracer;
import mpi.experiment.trace.measures.MeasureTracer;

public class NullTracer extends Tracer {

  EntityEntityTracing nullEETracing = new NullEntityEntityTracing();
  
  public NullTracer() {
    super(null, null);
  }

  @Override
  public void addEntityForMention(Mention mention, int entity, EntityTracer entityTracer) {
  }

  @Override
  public void addMeasureForMentionEntity(Mention mention, int entity, MeasureTracer measure) {
  }

  @Override
  public void setMentionEntityTotalSimilarityScore(Mention mention, int entity, double score) {
  }

  @Override
  public void writeOutput(String resultFileName, boolean withYago, boolean relatedness) {
  }
  
  public EntityEntityTracing eeTracing() {
    return nullEETracing;
  }
}
