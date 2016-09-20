package mpi.aida;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import mpi.aida.config.AidaConfig;
import mpi.aida.config.settings.DisambiguationSettings;
import mpi.aida.data.ChunkDisambiguationResults;
import mpi.aida.data.Entities;
import mpi.aida.data.ExternalEntitiesContext;
import mpi.aida.data.PreparedInputChunk;
import mpi.aida.data.ResultEntity;
import mpi.aida.data.ResultMention;
import mpi.aida.disambiguationtechnique.LocalDisambiguation;
import mpi.aida.graph.algorithms.CocktailParty;
import mpi.aida.graph.algorithms.CocktailPartySizeConstrained;
import mpi.aida.graph.algorithms.DisambiguationAlgorithm;
import mpi.aida.graph.algorithms.SimpleGreedy;
import mpi.aida.preparation.lookup.EntityLookupManager;
import mpi.aida.resultreconciliation.PersonMerger;
import mpi.aida.util.timing.RunningTimer;
import mpi.experiment.trace.GraphTracer;
import mpi.experiment.trace.GraphTracer.TracingTarget;
import mpi.experiment.trace.Tracer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main class for running the disambiguation. Is thread-safe and can be
 * run in parallel.
 * 
 */
public class ChunkDisambiguator implements Callable<ChunkDisambiguationResults> {
  private static final Logger logger_ =
      LoggerFactory.getLogger(ChunkDisambiguator.class);

  private final PreparedInputChunk input_;
  
  private final ExternalEntitiesContext externalContext_;

  private final DisambiguationSettings settings_;

  private final Tracer tracer_;

  private final EntityLookupManager entityLookupMgr = EntityLookupManager.singleton();

  public ChunkDisambiguator(PreparedInputChunk input, 
      ExternalEntitiesContext eec, DisambiguationSettings settings,
      Tracer tracer) {
    this.input_ = input;
    this.externalContext_ = eec;
    this.settings_ = settings;
    this.tracer_ = tracer;
  }

  public ChunkDisambiguationResults disambiguate() throws Exception {
    Integer timerId = RunningTimer.recordStartTime("ChunkDisambiguator");
    entityLookupMgr.fillInCandidateEntities(input_.getMentions(),
        externalContext_.getDictionary(),
        settings_.isIncludeNullAsEntityCandidate(),
        settings_.isIncludeContextMentions(),
        settings_.getMaxEntityRank(),
        settings_.getMaxCandidatesPerEntityByPrior(),
        settings_.isMentionLookupPrefix());

    Map<ResultMention, List<ResultEntity>> mentionMappings = null;

    DisambiguationAlgorithm da = null;
    switch (settings_.getDisambiguationTechnique()) {
      case LOCAL:
        da = new LocalDisambiguation(input_, externalContext_, settings_, tracer_);
        break;
        // TODO outdated, adjust.
        //      case LOCAL_ITERATIVE:
        //        mentionMappings = runLocalDisambiguationIterative(input, settings, tracer);
        //        break;
      case GRAPH:
        switch(settings_.getDisambiguationAlgorithm()) {
          case COCKTAIL_PARTY:
            da = new CocktailParty(input_, settings_, tracer_);
            break;
          case COCKTAIL_PARTY_SIZE_CONSTRAINED:
            da = new CocktailPartySizeConstrained(input_, settings_, tracer_);
            break;
          case SIMPLE_GREEDY:
            da = new SimpleGreedy(input_, settings_, tracer_);
            break;
            //          case RANDOM_WALK:
            //            da = new RandomWalk(input_, settings_, tracer_);
            //            break;
          default:
            logger_.warn("Unsupported graph algorithm.");
            // TODO(fkeller): shouldn't there be a return instead of a break?
            break;
        }
        break;
        // TODO outdated, adjust.
        //      case CHAKRABARTI:
        //        mentionMappings = runChakrabartiDisambiguation(input, settings);
      default:
        // TODO(fkeller): shouldn't there be a return instead of a break?
        break;
    }
    mentionMappings = da.disambiguate();
    RunningTimer.recordEndTime("ChunkDisambiguator", timerId);

    if (mentionMappings == null) {
      mentionMappings = new HashMap<ResultMention, List<ResultEntity>>();
    }
    if (settings_.isIncludeNullAsEntityCandidate()) {
      adjustOokbeEntityNames(mentionMappings);
    }

    // do the tracing
    String tracerHtml = null;  //tracer.getHtmlOutput();
    TracingTarget target = settings_.getTracingTarget();

    if (GraphTracer.gTracer.canGenerateHtmlFor(input_.getChunkId())) {
      tracerHtml = GraphTracer.gTracer.generateHtml(input_.getChunkId(), target);
      GraphTracer.gTracer.removeDocId(input_.getChunkId());
    } else if (GraphTracer.gTracer.canGenerateHtmlFor(Integer.toString(input_.getChunkId().hashCode()))) {
      tracerHtml = GraphTracer.gTracer.generateHtml(Integer.toString(input_.getChunkId().hashCode()), target);
      GraphTracer.gTracer.removeDocId(Integer.toString(input_.getChunkId().hashCode()));
    }

    ChunkDisambiguationResults disambiguationResults =
        new ChunkDisambiguationResults(mentionMappings, tracerHtml);

    boolean doPersonMerging =
        AidaConfig.getBoolean(AidaConfig.RECONCILER_PERSON_MERGE);
    if (doPersonMerging) {
      PersonMerger pm = new PersonMerger();
      pm.reconcile(disambiguationResults);
    }

    if (settings_.getNullMappingThreshold() >= 0.0) {
      double threshold = settings_.getNullMappingThreshold();
      logger_.debug(
          "Dropping all entities below the score threshold of " + threshold);

      // drop anything below the threshold
      for (ResultMention rm : disambiguationResults.getResultMentions()) {
        double score = disambiguationResults.getBestEntity(rm).getDisambiguationScore();

        if (score < threshold) {
          logger_.debug("Dropping entity:" +
              disambiguationResults.getBestEntity(rm) + " for mention:" + rm );
          List<ResultEntity> nme = new ArrayList<ResultEntity>(1);
          nme.add(ResultEntity.getNoMatchingEntity());
          disambiguationResults.setResultEntities(rm, nme);
        }
      }
    }

    return disambiguationResults;
  }
  
  // TODO: this seems to be broken
  private void adjustOokbeEntityNames(Map<ResultMention, List<ResultEntity>> solutions) {
    // Replace name-OOKBE placeholders by plain OOKBE placeholders.
    Map<ResultMention, List<ResultEntity>> nmeCleanedResults =
        new HashMap<ResultMention, List<ResultEntity>>();

    for (Entry<ResultMention, List<ResultEntity>> e : solutions.entrySet()) {
      if (Entities.isOokbeName(e.getValue().get(0).getEntity())) {
        List<ResultEntity> nme = new ArrayList<ResultEntity>(1);
        nme.add(ResultEntity.getNoMatchingEntity());
        nmeCleanedResults.put(e.getKey(), nme);
      } else {
        nmeCleanedResults.put(e.getKey(), e.getValue());
      }
    }
  }


  @Override
  public ChunkDisambiguationResults call() throws Exception {
    ChunkDisambiguationResults result = disambiguate();
    return result;
  }
}