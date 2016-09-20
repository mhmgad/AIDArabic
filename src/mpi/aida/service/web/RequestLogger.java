package mpi.aida.service.web;

import mpi.aida.config.settings.Settings.ALGORITHM;
import mpi.aida.config.settings.Settings.TECHNIQUE;
import mpi.aida.data.PreparedInput;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RequestLogger {
  private static Logger sLogger_ = LoggerFactory.getLogger(RequestLogger.class); 
  
  public static  void logProcess(
      String callerId, PreparedInput p, String preparationClass, 
      TECHNIQUE technique, ALGORITHM algorithm, long dur) {
    StringBuilder sb = new StringBuilder();
    sb.append("DISAMBIGUATE ");
    sb.append(callerId).append(" ");
    sb.append(p.getDocId()).append(" ");
    sb.append(p.getChunksCount()).append(" ");
    sb.append(p.getTokens().size()).append(" ");
    sb.append(p.getMentionSize()).append(" ");
    sb.append(preparationClass).append(" ");
    if (technique != null) {
      sb.append("TECHNIQUE:").append(technique).append(" ");
    }
    if (algorithm != null) {
      sb.append("ALGORITHM:").append(algorithm).append(" ");
    }
    sb.append(dur).append("ms ");
    sLogger_.info(sb.toString());
  }
}
