package mpi.aida.util.timing.document;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import mpi.experiment.timing.docstats.DocumentRunTimeStats;

public class DocumentTimeTracker {
  
  private Map<String, Double> documentCompletionTime = new ConcurrentHashMap<String, Double>();
    
  public void recordDocumentRunTime(String docid, double totTime) {
    if(!documentCompletionTime.containsKey(docid)) {
      documentCompletionTime.put(docid, totTime);
    }
  }

  public String getTrackedDocInfo() {
    return getTrackedDocInfo(false);
  }
  
  public String getTrackedDocInfo(boolean descOrderTotalTime) {
    return new DocumentRunTimeStats(documentCompletionTime, descOrderTotalTime)
    .computeMedian()
    .computePercentile()
    .generateStats(); 
  }
  
  public void clearTrackedInfo() {
    documentCompletionTime.clear();
  }
}
