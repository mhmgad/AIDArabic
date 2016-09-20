package mpi.aida.graph.algorithms;

import java.io.IOException;

import mpi.aida.config.settings.DisambiguationSettings;
import mpi.aida.data.PreparedInputChunk;
import mpi.aida.graph.Graph;
import mpi.experiment.trace.Tracer;

/**
 * Simplified version of the CocktailParty algorithm that 
 * - does no initial pruning (assumes graph is pruned beforehand in GraphGenerator).
 * - TODO should not do a final solving step but just take the last remaining candidate per mention
 * 
 */
public class SimpleGreedy extends CocktailParty {

  public SimpleGreedy(PreparedInputChunk input,
      DisambiguationSettings settings, Tracer tracer) throws Exception {
    super(input, settings, tracer);
  }

  @Override
  protected int getDiameter() throws IOException {
    return -1;
  }

  @Override
  protected void removeInitialEntitiesByDistance(Graph graph) {
    // Don't prune, assume graph as is.
    return;
  }
}
