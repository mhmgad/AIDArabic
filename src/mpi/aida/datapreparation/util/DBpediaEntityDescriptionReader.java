package mpi.aida.datapreparation.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import mpi.tools.javatools.filehandlers.FileLines;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides entity descriptions from DBpedia.
 */
public class DBpediaEntityDescriptionReader {

  private static final Logger logger_ =
      LoggerFactory.getLogger(DBpediaEntityDescriptionReader.class);

  private static final String prefixToStrip = "http://dbpedia.org/resource/";
  
  /**
   * File containing Wikipedia descriptions for entities.
   * The file format is ntriples, only subject and object are used.
   * 
   * The file can be downloaded from http://www.dbpedia.org.
   */
  private static final String descriptionFile = "/GW/D5data-3/DBpedia/3.9/short_abstracts_en.nt";
  
  public static Map<String, String> getEntityDescriptions() {
    Map<String, String> descriptions = new HashMap<>();
    try {
      for (String line : new FileLines(descriptionFile)) {
        if (line.startsWith("#")) {
          continue;
        }
        String entity = getEntity(line.substring(0, line.indexOf('>') + 1));
        String description = line.substring(line.indexOf('"') + 1, line.lastIndexOf('"'));
        descriptions.put(entity, description);
      }
    } catch (IOException e) {
      logger_.error("Error reading description file: " + e);
      e.printStackTrace();
    }
    return descriptions;
  }
  
  private static String getEntity(String encoded) throws UnsupportedEncodingException {
    String entity = 
        // Decode URL encoding to UTF-8.
        URLDecoder.decode(
            // Strip <>.
            encoded.substring(1, encoded.length() - 1)
            // Replace DBpedia namespace.
            .replace(prefixToStrip, ""),
        "UTF-8");
    return entity;
  }
}
