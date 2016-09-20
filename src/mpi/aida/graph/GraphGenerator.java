package mpi.aida.graph;

import gnu.trove.iterator.TIntDoubleIterator;
import gnu.trove.map.hash.TIntDoubleHashMap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mpi.aida.AidaManager;
import mpi.aida.config.AidaConfig;
import mpi.aida.config.settings.DisambiguationSettings;
import mpi.aida.data.Context;
import mpi.aida.data.Entities;
import mpi.aida.data.Entity;
import mpi.aida.data.ExternalEntitiesContext;
import mpi.aida.data.Mention;
import mpi.aida.data.Mentions;
import mpi.aida.data.NullEntity;
import mpi.aida.graph.extraction.ExtractGraph;
import mpi.aida.graph.similarity.EnsembleEntityEntitySimilarity;
import mpi.aida.graph.similarity.EnsembleMentionEntitySimilarity;
import mpi.aida.graph.similarity.MaterializedPriorProbability;
import mpi.aida.util.CollectionUtils;
import mpi.aida.util.Counter;
import mpi.aida.util.timing.RunningTimer;
import mpi.experiment.trace.GraphTracer;
import mpi.experiment.trace.NullGraphTracer;
import mpi.experiment.trace.Tracer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Ordering;

public class GraphGenerator {
  
  public class MaximumGraphSizeExceededException extends Exception {
    private static final long serialVersionUID = -4159436792558733318L;
    public MaximumGraphSizeExceededException() { super(); }
    public MaximumGraphSizeExceededException(String message) { super(message); }
  }

  private static final Logger logger = 
      LoggerFactory.getLogger(GraphGenerator.class);
  
  private final Mentions mentions;
  
  private final Context context;
  
  private String docId;

  private DisambiguationSettings settings;
  
  private Tracer tracer = null;

  // set this to 20000 for web server so it can still run for web server
  private int maxNumCandidateEntitiesForGraph = 0;

  public GraphGenerator(Mentions mentions, Context context, String docId, DisambiguationSettings settings, Tracer tracer) {
    //		this.storePath = content.getStoreFile();
    this.mentions = mentions;
    this.context = context;
    this.docId = docId;
    this.settings = settings;
    this.tracer = tracer;
    try {
      if (AidaConfig.get(AidaConfig.MAX_NUM_CANDIDATE_ENTITIES_FOR_GRAPH) != null) {
        maxNumCandidateEntitiesForGraph = 
            Integer.parseInt(
                AidaConfig.get(AidaConfig.MAX_NUM_CANDIDATE_ENTITIES_FOR_GRAPH));
      }
    } catch (Exception e) {
      maxNumCandidateEntitiesForGraph = 0;
    }
  }

  public Graph run() throws Exception {
    Graph gData = null;
    gData = generateGraph();
    return gData;
  }

  private Graph generateGraph() throws Exception {
    int timerId = RunningTimer.recordStartTime("GraphGenerator");

    // gather candidate entities - and prepare tracing
    Integer id = RunningTimer.recordStartTime("GatherCandidateEntities");
    // TODO pass external entities here.
    Entities allEntities = AidaManager.getAllEntities(mentions, new ExternalEntitiesContext(), tracer);
    if (settings.isIncludeNullAsEntityCandidate()) {
      allEntities.setIncludesOokbeEntities(true);
    }
    RunningTimer.recordEndTime("GatherCandidateEntities", id);
    
    // Check if the number of candidates exceeds the threshold (for memory
    // issues).
    if (maxNumCandidateEntitiesForGraph != 0 && allEntities.size() > maxNumCandidateEntitiesForGraph) {
      throw new MaximumGraphSizeExceededException(
          "Maximum number of candidate entites for graph exceeded " + allEntities.size());
    }

    id = RunningTimer.recordStartTime("GG-LocalSimilarityCompute");
    
    logger.debug("Computing the mention-entity similarities...");
    
    // Counters for keeping track.
    int solvedByCoherenceRobustnessHeuristic = 0;
    int solvedByConfidenceThresholdHeuristic = 0;
    int solvedByEasyMentionsHeuristic = 0;
    int preGraphNullMentions = 0;
    int prunedMentionsCount = 0;

    Map<Mention, Double> mentionL1s = null;
    Integer timer = RunningTimer.recordStartTime("MentionPriorSimL1DistCompute");
    if (settings.getGraphSettings().shouldUseCoherenceRobustnessTest()) {
      mentionL1s = computeMentionPriorSimL1Distances(mentions, allEntities);
    }
    RunningTimer.recordEndTime("MentionPriorSimL1DistCompute", timer);
    
    timer = RunningTimer.recordStartTime("EnsembleMentionEntitySimInit");
    EnsembleMentionEntitySimilarity mentionEntitySimilarity = 
        new EnsembleMentionEntitySimilarity(
            mentions, allEntities, context,
                new ExternalEntitiesContext(), settings.getSimilaritySettings(), tracer);
    logger.debug("Computing the mention-entity similarities...");
    RunningTimer.recordEndTime("EnsembleMentionEntitySimInit", timer);
    // We might drop entities here, so we have to rebuild the list of unique
    // entities
    allEntities = new Entities();
    if (settings.isIncludeNullAsEntityCandidate()) {
      allEntities.setIncludesOokbeEntities(true);
    }
    // Keep the similarities for all mention-entity pairs, as some are
    // dropped later on.
    Map<Mention, TIntDoubleHashMap> mentionEntityLocalSims =
        new HashMap<Mention, TIntDoubleHashMap>();
    timer = RunningTimer.recordStartTime("CalcSimAndRobustnessForCandidateEntities");
    for (int i = 0; i < mentions.getMentions().size(); i++) {
      Counter.incrementCount("MENTIONS_TOTAL");
      Mention currentMention = mentions.getMentions().get(i);
      TIntDoubleHashMap entityLocalSims = new TIntDoubleHashMap();
      mentionEntityLocalSims.put(currentMention, entityLocalSims);
      Entities originalCandidateEntities = currentMention.getCandidateEntities();
      
      // Compute similarities for all candidates.   
      for (Entity candidate : originalCandidateEntities) {
        // Keyphrase-based mention/entity similarity.
        double similarity = mentionEntitySimilarity.calcSimilarity(currentMention, context, candidate);
        candidate.setMentionEntitySimilarity(similarity);
        entityLocalSims.put(candidate.getId(), similarity);
      }
      
      TIntDoubleHashMap normalizedEntityLocalSims = 
          CollectionUtils.normalizeValuesToSum(entityLocalSims);
      
      Entity bestEntity = null;

      // Do pre-graph algorithm null-mention discovery
      if (settings.getGraphSettings().isPreCoherenceNullMappingDiscovery()) {
        double max = CollectionUtils.getMaxValue(normalizedEntityLocalSims);
        if (max < settings.getGraphSettings().getPreCoherenceNullMappingDiscoveryThreshold()) {
          bestEntity = new NullEntity();
          ++preGraphNullMentions;
          GraphTracer.gTracer.addMentionToEasy(
              docId, currentMention.getMention(), currentMention.getCharOffset());
          Counter.incrementCount("PRE_GRAPH_NULL_MENTIONS");
        }
      }
      
      // If there are multiple candidates, try to determine the correct
      // entity before running the joint disambiguation according
      // to some heuristics.
      if (bestEntity == null && originalCandidateEntities.size() > 1) {
        if (bestEntity == null) {
          bestEntity = 
              doConfidenceThresholdCheck(currentMention, normalizedEntityLocalSims);
          if (bestEntity != null) {
            GraphTracer.gTracer.addMentionToConfidenceThresh(
                docId, currentMention.getMention(), currentMention.getCharOffset());  
            ++solvedByConfidenceThresholdHeuristic;
            Counter.incrementCount("MENTIONS_BY_CONFIDENCE_THRESHOLD_HEURISTIC");
          }
        } 
        
        if (bestEntity == null) {
          bestEntity = doEasyMentionsCheck(currentMention);
          if (bestEntity != null) {
            ++solvedByEasyMentionsHeuristic;
            Counter.incrementCount("MENTIONS_BY_EASY_MENTIONS_HEURISTIC");
            GraphTracer.gTracer.addMentionToEasy(
                docId, currentMention.getMention(), currentMention.getCharOffset());
          }
        }  
        
        if (bestEntity == null) {
          bestEntity = doCoherenceRobustnessCheck(
              currentMention, mentionL1s);
          if (bestEntity != null) {
            ++solvedByCoherenceRobustnessHeuristic;
            Counter.incrementCount("MENTIONS_BY_COHERENCE_ROBUSTNESS_HEURISTIC");
            GraphTracer.gTracer.addMentionToLocalOnly(
                docId, currentMention.getMention(), currentMention.getCharOffset());          
          }
        }                           
      }
      if (bestEntity != null) {
        Entities candidates = new Entities();
        candidates.add(bestEntity);
        currentMention.setCandidateEntities(candidates);
        allEntities.add(bestEntity);
      } else {
        // If all heuristics failed, prune candidates.
        Entities candidates = pruneCandidates(currentMention);
        if (candidates != null) {
          allEntities.addAll(candidates);
          currentMention.setCandidateEntities(candidates);
          ++prunedMentionsCount;
          Counter.incrementCount("MENTIONS_PRUNED");
          GraphTracer.gTracer.addMentionToPruned(
              docId, currentMention.getMention(), currentMention.getCharOffset());  
        } else {
          // Nothing changed from any heuristic/pruning.
          allEntities.addAll(originalCandidateEntities);
        }
      }
    }
    
    if (!(GraphTracer.gTracer instanceof NullGraphTracer)) {
      gatherL1stats(docId, mentionL1s);
      GraphTracer.gTracer.addStat(
          docId, "Number of fixed mention by coherence robustness check", 
          Integer.toString(solvedByCoherenceRobustnessHeuristic));
      GraphTracer.gTracer.addStat(
          docId, "Number of fixed mention by confidence threshold check", 
          Integer.toString(solvedByConfidenceThresholdHeuristic));
      GraphTracer.gTracer.addStat(
          docId, "Number of fixed mention by easy mentions check", 
          Integer.toString(solvedByEasyMentionsHeuristic));
      GraphTracer.gTracer.addStat(
          docId, "Number of mentions with pruned candidates", 
          Integer.toString(prunedMentionsCount));
      GraphTracer.gTracer.addStat(
          docId, "Number of mentions set to null before running the algorithm",
          Integer.toString(preGraphNullMentions));
    }
    
    RunningTimer.recordEndTime("CalcSimAndRobustnessForCandidateEntities", timer);
    RunningTimer.recordEndTime("GG-LocalSimilarityCompute", id);
    
    logger.debug("Building the graph...");
    EnsembleEntityEntitySimilarity eeSim = 
        new EnsembleEntityEntitySimilarity(
            allEntities, settings.getSimilaritySettings(), tracer);
    ExtractGraph egraph = 
        new ExtractGraph(
            docId, mentions, allEntities, eeSim, settings.getGraphSettings().getAlpha());
    Graph gData = egraph.generateGraph();
    gData.setMentionEntitySim(mentionEntityLocalSims);
    RunningTimer.recordEndTime("GraphGenerator", timerId);    
    return gData;
  }
  
  /**
   * Checks if the coherence robustness check fires. If local sim and prior
   * agree on an entity, use it.
   * 
   * @param mentionL1s
   * @param currentMention
   * @return best candidate or null if check failed.
   */
  private Entity doCoherenceRobustnessCheck(
      Mention currentMention, Map<Mention, Double> mentionL1s) {
    Entity bestCandidate = null;
    if (settings.getGraphSettings().shouldUseCoherenceRobustnessTest()) {
      if (mentionL1s.containsKey(currentMention) &&
          mentionL1s.get(currentMention) < 
          settings.getGraphSettings().getCohRobustnessThreshold()) {
        bestCandidate = getBestCandidate(currentMention);
      }
    }
    return bestCandidate;
  }

  /**
   * Checks if the confidence of a mention disambiguation by local sim alone is 
   * high enough to fix it.
   * 
   * @param mention
   * @return best candidate or null if check failed.
   */
  private Entity doConfidenceThresholdCheck(
      Mention mention, TIntDoubleHashMap normalizedEntityLocalSims) {
    Entity bestEntity = null;
    if (settings.getGraphSettings().shouldUseConfidenceThresholdTest()) {     
      double max = CollectionUtils.getMaxValue(normalizedEntityLocalSims);
      if (max > settings.getGraphSettings().getConfidenceTestThreshold()) {
        bestEntity = getBestCandidate(mention);
      }
    }      
    return bestEntity;
  }
  
  /**
   * For mentions with less than K candidates, assume that local similarity
   * is good enough to distinguish. K is given in graphSettings.
   * 
   * @param mention
   * @return best candidate or null if check failed.
   */
  private Entity doEasyMentionsCheck(Mention mention) {
    Entity bestEntity = null;
    if (settings.getGraphSettings().shouldUseEasyMentionsTest()) {
      Entities candidates = mention.getCandidateEntities();
      if (candidates.size() < settings.getGraphSettings().getEasyMentionsTestThreshold()) {
        bestEntity = getBestCandidate(mention);
      }
    }
    return bestEntity;
  }

  /**
   * Prunes candidates, keeping only the top K elements for each mention. K is
   * set in graphSettings.
   * 
   * @param mention 
   * 
   * @return
   */
  private Entities pruneCandidates(Mention mention) {
    Entities bestEntities = null;
    if (settings.getGraphSettings().shouldPruneCandidateEntities()) {
      int k = settings.getGraphSettings().getPruneCandidateThreshold();
      Entities candidates = mention.getCandidateEntities();
      if (candidates.size() > k) {
        Ordering<Entity> order = new Ordering<Entity>() {
          @Override
          public int compare(Entity e1, Entity e2) {
            return Double.compare(
                e1.getMentionEntitySimilarity(),
                e2.getMentionEntitySimilarity());
          }
        };
        List<Entity> topEntities = order.greatestOf(candidates, k);
        bestEntities = new Entities();
        bestEntities.addAll(topEntities);
      }
    }
    return bestEntities;
  }

  private Map<Mention, Double> computeMentionPriorSimL1Distances(
      Mentions mentions, Entities allEntities) throws Exception {
    // Precompute the l1 distances between each mentions
    // prior and keyphrase based similarity.
    Map<Mention, Double> l1s = new HashMap<Mention, Double>();
    Set<Mention> mentionObjects = new HashSet<>();
    for (Mention m : mentions.getMentions()) {
      mentionObjects.add(m);
    }    
    MaterializedPriorProbability pp = 
        new MaterializedPriorProbability(mentionObjects);
    EnsembleMentionEntitySimilarity keyphraseSimMeasure = 
        new EnsembleMentionEntitySimilarity(
            mentions, allEntities, context,
                new ExternalEntitiesContext(), settings.getGraphSettings().getCoherenceSimilaritySetting(),
            tracer);

    for (Mention mention : mentions.getMentions()) {
      // get prior distribution
      TIntDoubleHashMap priorDistribution = calcPriorDistribution(mention, pp);

      // get similarity distribution per UnnormCOMB (IDF+MI)
      TIntDoubleHashMap simDistribution = 
          calcSimDistribution(mention, keyphraseSimMeasure);

      // get L1 norm of both distributions, the graph algorithm can use
      // this for additional information. 
      // SOLVE_BY_LOCAL
      // otherwise, SOLVE_BY_COHERENCE
      double l1 = calcL1(priorDistribution, simDistribution);
      l1s.put(mention, l1);     
    }
        
    return l1s;
  }
  
  private void gatherL1stats(String docId, Map<Mention, Double> l1s) {
    double l1_total = 0.0;
    for (double l1 : l1s.values()) {
      l1_total += l1;
    }
    double l1_mean = l1_total / l1s.size();
    double varTemp = 0.0;
    for (double l1 : l1s.values()) {
      varTemp += Math.pow(Math.abs(l1_mean - l1), 2);
    }
    double variance = 0;
    if (l1s.size() > 1) {
      variance = varTemp / l1s.size();
    }
    GraphTracer.gTracer.addStat(docId, "L1 (prior-sim) Mean", Double.toString(l1_mean));
    GraphTracer.gTracer.addStat(docId, "L1 (prior-sim) StdDev", Double.toString(Math.sqrt(variance)));
  }

  private TIntDoubleHashMap calcPriorDistribution(
      Mention mention, MaterializedPriorProbability pp) {
    TIntDoubleHashMap priors = 
        new TIntDoubleHashMap();

    for (Entity entity : mention.getCandidateEntities()) {
      priors.put(entity.getId(), 
          pp.getPriorProbability(mention, entity));
    }

    return priors;
  }

  private TIntDoubleHashMap calcSimDistribution(
      Mention mention, EnsembleMentionEntitySimilarity combSimMeasure) throws Exception {
    TIntDoubleHashMap sims = new TIntDoubleHashMap();
    for (Entity e : mention.getCandidateEntities()) {
      sims.put(e.getId(),
              combSimMeasure.calcSimilarity(mention, context, e));
    }
    return CollectionUtils.normalizeValuesToSum(sims);
  }

  private double calcL1(TIntDoubleHashMap priorDistribution, 
      TIntDoubleHashMap simDistribution) {
    double l1 = 0.0;

    for (TIntDoubleIterator itr = priorDistribution.iterator(); 
        itr.hasNext(); ) {
      itr.advance();
      double prior = itr.value();
      double sim = simDistribution.get(itr.key());
      double diff = Math.abs(prior - sim);
      l1 += diff;
    }

    assert (l1 >= -0.00001 && l1 <= 2.00001) : "This cannot happen, L1 must be in [0,2]. Was: " + l1;
    return l1;
  }
  
  private Entity getBestCandidate(Mention m) {
    double bestSim = Double.NEGATIVE_INFINITY;
    Entity bestCandidate = null;
    for (Entity e : m.getCandidateEntities()) {
      if (e.getMentionEntitySimilarity() > bestSim) {
        bestSim = e.getMentionEntitySimilarity();
        bestCandidate = e;
      }
    }
    return bestCandidate;
  }
}
