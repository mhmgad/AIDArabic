package mpi.aida.graph.algorithms;

import java.util.List;
import java.util.Map;

import mpi.aida.config.settings.DisambiguationSettings;
import mpi.aida.data.ExternalEntitiesContext;
import mpi.aida.data.PreparedInputChunk;
import mpi.aida.data.ResultEntity;
import mpi.aida.data.ResultMention;
import mpi.experiment.trace.Tracer;


public abstract class DisambiguationAlgorithm {
  
  protected PreparedInputChunk input_;

  protected ExternalEntitiesContext externalContext_;

  protected DisambiguationSettings settings_;

  protected Tracer tracer_;
  
  
  public DisambiguationAlgorithm(PreparedInputChunk input, ExternalEntitiesContext externalContext,
                                 DisambiguationSettings settings, Tracer tracer) {
    input_ = input;
    externalContext_ = externalContext;
    settings_ = settings;
    tracer_ = tracer;
  }

  public abstract Map<ResultMention, List<ResultEntity>> disambiguate() throws Exception;
  
}
