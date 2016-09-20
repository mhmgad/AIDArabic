package mpi.aida.util.freebase;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import mpi.tools.basics2.Normalize;
import mpi.tools.basics3.FactComponent;
import mpi.tools.javatools.util.FileUtils;

/**
 * Utility methods for Freebase.
 */
public class FreebaseUtil {
  // TODO this should match the actual dump used to create YAGO - make sure to keep this consistent!
  // Make this part of YAGO!
  private static final String curIdToYagoIdsMapFileName = "/GW/D5data-3/wikipedia_dumps/en/20140102/entity_ids.txt";
  
  private static final String freebaseFileName = "/GW/aida/work/data/freebase-rdf-2013-03-24-00-00.gz";
  
  private enum YAGO {YAGO2, YAGO3};
  
  public static Map<String, String> loadFreebaseToYago2IdMap() throws IOException {
    return loadFreebaseToYagoIdMap(YAGO.YAGO2);
  }
  
  public static Map<String, String> loadFreebaseToYago3IdMap() throws IOException {
   return loadFreebaseToYagoIdMap(YAGO.YAGO3); 
  }
  
  private static Map<String, String> loadFreebaseToYagoIdMap(YAGO yagoVersion) throws IOException {
    Map<String, String> curIdToYagoIdMap = loadCurIdToYagoIdMap();

    Map<String, String> map = new HashMap<String, String>();
    InputStream in = new GZIPInputStream(new FileInputStream(freebaseFileName));

    BufferedReader bReader = FileUtils.getBufferedUTF8Reader(in);

    String line;

    while ((line = bReader.readLine()) != null) {

      String[] lineParts = line.split("\t");

      if (lineParts.length != 3) continue;

      if (lineParts[1].equals("ns:common.topic.topic_equivalent_webpage")) {
        String freebaseId = lineParts[0];
        String wikipediaUrl = lineParts[2];
        if (wikipediaUrl.startsWith("<http://en.wikipedia.org/wiki/index.html?curid")) {
          int idStart = wikipediaUrl.indexOf("=");
          int idEnd = wikipediaUrl.indexOf(">");
          String curId = wikipediaUrl.substring(idStart + 1, idEnd);
          String yagoId = curIdToYagoIdMap.get(curId);
          if (yagoId == null) {
            continue;
          }
          if(yagoVersion == YAGO.YAGO3) {
            yagoId = mapYago2ToYago3(yagoId);
          }
          map.put(freebaseId, yagoId);              
        }
      }
    }

    bReader.close();
    return map;
  }
  

  private static String mapYago2ToYago3(String yagoId) {
    return FactComponent.forYagoEntity(Normalize.unEntity(yagoId));
  }

  public static String getFreebaseFileName() {
    return freebaseFileName;
  }
  
  
  private static Map<String, String> loadCurIdToYagoIdMap() throws IOException {
    Map<String, String> map = new HashMap<String, String>();

    BufferedReader reader = FileUtils.getBufferedUTF8Reader(curIdToYagoIdsMapFileName);

    String line;

    while ((line = reader.readLine()) != null) {
      if (line.equals("")) continue;
      String[] lineParts = line.split("\t");
      String yagoId = lineParts[0];
      String id = lineParts[1];
      map.put(id, yagoId);
    }

    reader.close();
    return map;
  }
}
