package mpi.aida.util;

import mpi.tools.javatools.util.FileUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper methods for JSON.
 *
 */
public class JsonUtils {
  /**
   * Returns all JSON documents from AIDA results file. AIDA can write single- and multi-document results files.
   *
   * @param resultsFile AIDA JSON results file.
   * @return  List of AIDA processed documents. Empty if there was a problem processing the file.
   */
  public static List<JSONObject> getJsonObjectsFromAIDAResultsFile(File resultsFile) throws IOException {
    String jsonStr = FileUtils.getFileContent(resultsFile);
    Object json = JSONValue.parse(jsonStr);
    List<JSONObject> documents = new ArrayList<>();
    // Check if the json is a multi-document json.
    if (json instanceof JSONArray) {
      JSONArray a = (JSONArray) json;
      for (int i = 0; i < a.size(); i++) {
        documents.add((JSONObject) a.get(i));
      }
    } else if (json instanceof JSONObject) {
      documents.add((JSONObject) json);
    }
    return documents;
  }
}