package mpi.aida;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import mpi.aida.config.settings.DisambiguationSettings;
import mpi.aida.data.ChunkDisambiguationResults;
import mpi.aida.data.DisambiguationResults;
import mpi.aida.data.ExternalEntitiesContext;
import mpi.aida.data.PreparedInput;
import mpi.aida.data.PreparedInputChunk;
import mpi.aida.data.ResultEntity;
import mpi.aida.data.ResultMention;
import mpi.aida.resultreconciliation.ResultsReconciler;
import mpi.aida.util.Counter;
import mpi.aida.util.DocumentCounter;
import mpi.aida.util.timing.RunningTimer;
import mpi.experiment.trace.NullTracer;
import mpi.experiment.trace.Tracer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Disambiguator implements Callable<DisambiguationResults> {
  
  private Logger logger_ = LoggerFactory.getLogger(Disambiguator.class);
  
  private PreparedInput preparedInput_;
  private DisambiguationSettings settings_;
  private ExternalEntitiesContext externalContext_;
  private DocumentCounter documentCounter_;
  private Tracer tracer_;
   
  /** 
   * Common init.
   */
  private void init(PreparedInput input, DisambiguationSettings settings,
                  Tracer tracer, ExternalEntitiesContext eec) {
    preparedInput_ = input;
    settings_ = settings;
    externalContext_ = eec;
    tracer_ = tracer;
  }
  
  /**
   * Use this when calling Disambiguator in parallel.
   * 
   * @param input
   * @param settings
   * @param tracer
   * @param dc 
   */
  public Disambiguator(PreparedInput input, DisambiguationSettings settings, 
      Tracer tracer, DocumentCounter dc) {
    this(input, settings, tracer);
    documentCounter_ = dc;
  }
    
  /**
   * tracer is set to NullTracer();
   * @param input
   * @param settings
   */
  public Disambiguator(PreparedInput input, DisambiguationSettings settings) {
    init(input, settings, new NullTracer(), new ExternalEntitiesContext());
  }

  public Disambiguator(PreparedInput input, DisambiguationSettings settings, ExternalEntitiesContext eec) {
    init(input, settings, new NullTracer(), eec);
  }

  public Disambiguator(PreparedInput input, DisambiguationSettings settings, Tracer tracer) {
    init(input, settings, tracer, new ExternalEntitiesContext());
  }

  public Disambiguator(PreparedInput input, DisambiguationSettings settings,
                       Tracer tracer, ExternalEntitiesContext eec) {
    init(input, settings, tracer, eec);
  }

  public DisambiguationResults disambiguate() throws Exception {
    logger_.debug("Disambiguating '" + preparedInput_.getDocId() + "' with " + 
        preparedInput_.getChunksCount() + " chunks and " +
        preparedInput_.getMentionSize() + " mentions."); 
    Integer runningId = RunningTimer.recordStartTime("Disambiguator");
    long startTime = System.currentTimeMillis();
    Map<PreparedInputChunk, ChunkDisambiguationResults> chunkResults =
        disambiguateChunks(preparedInput_, externalContext_);
    DisambiguationResults results = 
        aggregateChunks(preparedInput_, chunkResults);
    RunningTimer.recordEndTime("Disambiguator", runningId);
    double runTime = System.currentTimeMillis() - startTime;
    logger_.info("Document '" + preparedInput_.getDocId() + "' done in " + 
                runTime + "ms (" + 
                preparedInput_.getChunksCount() + " chunks, " +
                preparedInput_.getMentionSize() + " mentions).");
    RunningTimer.trackDocumentTime(preparedInput_.getDocId(), runTime);
    Counter.incrementCount("DOCUMENTS_PROCESSED");
    return results;
  }

  private Map<PreparedInputChunk, ChunkDisambiguationResults> disambiguateChunks(
      PreparedInput preparedInput, ExternalEntitiesContext eec) throws Exception {
    Map<PreparedInputChunk, ChunkDisambiguationResults> chunkResults =
        new HashMap<PreparedInputChunk, ChunkDisambiguationResults>();
    ExecutorService es = Executors.newFixedThreadPool(settings_.getNumChunkThreads());
    Map<PreparedInputChunk, Future<ChunkDisambiguationResults>> futureResults = 
        new HashMap<PreparedInputChunk, Future<ChunkDisambiguationResults>>();
    for (PreparedInputChunk c : preparedInput) {
      ChunkDisambiguator cd = new ChunkDisambiguator(c, eec, settings_, tracer_);
      Future<ChunkDisambiguationResults> result = es.submit(cd);
      futureResults.put(c, result);
    }
    for (PreparedInputChunk c : preparedInput) {
      chunkResults.put(c, futureResults.get(c).get());
    }
    es.shutdown();
    es.awaitTermination(1, TimeUnit.DAYS);
    return chunkResults;
  }
  
  /**
   * For the time being, just put everything together.
   * It should take into account potential conflicts across chunks (e.g.
   * the same mention string pointing to different entities, which is unlikely
   * in the same document).
   * 
   * @param preparedInput
   * @param chunkResults
   * @return
   */
  private DisambiguationResults aggregateChunks(PreparedInput preparedInput,
      Map<PreparedInputChunk, ChunkDisambiguationResults> chunkResults) {
    Integer runId = RunningTimer.recordStartTime("aggregateChunks");
    Map<ResultMention, List<ResultEntity>> mentionMappings = 
        new HashMap<ResultMention, List<ResultEntity>>();
    
    StringBuilder gtracerHtml = new StringBuilder();
    ResultsReconciler recon = new ResultsReconciler(chunkResults.size());
    for (Entry<PreparedInputChunk,ChunkDisambiguationResults> e : chunkResults.entrySet()) {
      PreparedInputChunk p = e.getKey();
      ChunkDisambiguationResults cdr = e.getValue();
      gtracerHtml.append("<div>");
      gtracerHtml.append(cdr.getgTracerHtml());
      gtracerHtml.append("</div>");
      gtracerHtml.append("<div style='font-size:8pt;color:#DDDDDD'>chunkid: ").append(p.getChunkId()).append("</p>");
      for (ResultMention rm : cdr.getResultMentions()) {
        List<ResultEntity> res = cdr.getResultEntities(rm);
        rm.setDocId(preparedInput.getDocId());
        recon.addMentionEntityListPair(rm, res);
      }
    }
    
    mentionMappings = recon.reconcile();
    DisambiguationResults results = 
        new DisambiguationResults(mentionMappings, gtracerHtml.toString());
    results.setTracer(tracer_);
    RunningTimer.recordEndTime("aggregateChunks", runId);
    return results;
  }
  
  @Override
  public DisambiguationResults call() throws Exception {
    DisambiguationResults result = disambiguate();
    result.setTracer(tracer_);
    if (documentCounter_ != null) {
      // This provides a means of knowing where we are
      // and how long it took until now.
      documentCounter_.oneDone();
    }
    return result;
  }
}
