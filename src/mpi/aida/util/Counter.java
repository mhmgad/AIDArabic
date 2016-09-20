package mpi.aida.util;

import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.hash.TObjectIntHashMap;


public class Counter {
  private static TObjectIntHashMap<String> counts = 
      new TObjectIntHashMap<String>();
  
  public static void incrementCount(String counterName) {
    synchronized (counts) {
      counts.adjustOrPutValue(counterName, 1, 1);      
    }
  }
  
  public static void incrementCountByValue(String counterName, int value) {
    synchronized (counts) {     
      counts.adjustOrPutValue(counterName, value, value);
    }
  }
  
  public static String getOverview() {
    StringBuilder sb = new StringBuilder();
    sb.append("COUNTER_NAME\tCOUNTER_VALUE\n");
    for (TObjectIntIterator<String> itr = counts.iterator(); itr.hasNext(); ) {
      itr.advance();
      sb.append(itr.key()).append("\t").append(itr.value()).append("\n");
    }
    return sb.toString();
  }
}
