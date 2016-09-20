package mpi.aida.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;


public class TimeUtil {
  private static DateFormat df;
  static {
    TimeZone tz = TimeZone.getTimeZone("UTC");
    df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
    df.setTimeZone(tz);
  }
  
  public static String getCurrentISO8601Date() {
    String now = df.format(new Date());
    return now;
  }
}
