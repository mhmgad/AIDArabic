package mpi.aida.resultreconciliation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mpi.aida.data.ResultEntity;
import mpi.aida.data.ResultMention;
import mpi.aida.util.CollectionUtils;
import mpi.aida.util.timing.RunningTimer;


public class ResultsReconciler {
  
  private Logger logger_ = LoggerFactory.getLogger(ResultsReconciler.class);
  
  private int chunksCount;
  
  @SuppressWarnings("unused")
  private List<ResultMention> resultMentions;
  
  // result entities from all chunks
  private List<ResultEntity> allResultEntities;
  
  private Map<ResultMention, List<ResultEntity>> mappings;
  
  // surface name to identified entities count
  private Map<String, Map<String, Double>> surfaceFormEntityAggregatedScore;
  
  // entity name to result entity for lookup
  private Map<String, ResultEntity> entityMap;
  
  private void init() {
    // resultMentions = new ArrayList<ResultMention>();
    allResultEntities = new ArrayList<ResultEntity>();
    mappings = new HashMap<ResultMention, List<ResultEntity>>();
    surfaceFormEntityAggregatedScore = 
        new HashMap<String, Map<String, Double>>();
    entityMap = new HashMap<String, ResultEntity>();
  }
  
  public ResultsReconciler() {
    init();
  }
  
  public ResultsReconciler(int count) {
    init();
    chunksCount = count;
  }
  
  public void setTotalChunkCount(int count) {
    chunksCount = count;
  }
  /**
   * This method caches the result mentions and result entities from all chunk results.
   * 
   * @param rm ResultMention
   * @param res List of ResultEntity objects
   */
  public void addMentionEntityListPair(ResultMention rm, List<ResultEntity> res) {
   // resultMentions.add(rm);
   allResultEntities.addAll(res);
   mappings.put(rm, res);
   
   for(ResultEntity re : res) {
     updateSurfaceNameEntityPair(rm.getMention(), re);
   }   
  }
  
  /**
   * Groups all result mention with same surface forms. Combines all identified entities
   * and returns a single entities list sorted based on maximum aggregated score.
   * 
   * @return A Map of ResultMention to List of ResultEntity
   */
  public Map<ResultMention, List<ResultEntity>> reconcile() {
    Integer runId = RunningTimer.recordStartTime("reconcile");
    if(chunksCount <= 1) {
      logger_.debug("Single chunk : Returning existing mapping.");
      RunningTimer.recordEndTime("reconcile", runId);
      return mappings;
    }
    
    generateEntityMap();    
    for(ResultMention rm : mappings.keySet()) {
      List<ResultEntity> res = new ArrayList<ResultEntity>();
      Map<String, Double> aggregatedEntityScore = surfaceFormEntityAggregatedScore.get(rm.getMention());      
      aggregatedEntityScore = CollectionUtils.sortMapByValue(aggregatedEntityScore, true);      
      Iterator<Entry<String, Double>> it = aggregatedEntityScore.entrySet().iterator();      
      while(it.hasNext()) {
        res.add(entityMap.get(it.next().getKey()));
      }           
      mappings.put(rm, res);
    }
    logger_.debug("Reconciled Results for " + chunksCount + " chunks.");
    RunningTimer.recordEndTime("reconcile", runId);
    return mappings;
  }

  private void generateEntityMap() {
    for(ResultEntity re : allResultEntities) {
      if(!entityMap.containsKey(re.getEntity())) {
        entityMap.put(re.getEntity(), re);
      }
    }
  }
  
  private void updateSurfaceNameEntityPair(String mentionName, ResultEntity re) {
    Map<String, Double> entityScore;
    String entity = re.getEntity();
    double score = re.getDisambiguationScore();
    if(surfaceFormEntityAggregatedScore.containsKey(mentionName)) {      
      entityScore = surfaceFormEntityAggregatedScore.get(mentionName);
      if(entityScore.containsKey(entity)) {
        entityScore.put(entity, entityScore.get(entity)+score);
      }else{
        entityScore.put(entity, score);
      }                
    }else{      
      entityScore = new HashMap<String, Double>();
      surfaceFormEntityAggregatedScore.put(mentionName, entityScore);
      entityScore.put(entity, score);
    }
  }  
}