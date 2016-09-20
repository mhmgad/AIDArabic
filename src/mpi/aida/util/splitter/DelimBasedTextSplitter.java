package mpi.aida.util.splitter;

import java.util.ArrayList;
import java.util.List;

public class DelimBasedTextSplitter {
  /**
   * Returns a list of strings split based on the given delimiter
   * This method assumes the delimiter appears at the beginning of the document
   * (e.g xml root tag / custom element denoting a start of document)
   * 
   * @param content
   * @param delim
   * @return A list of strings representing text (along with the delimiters)
   */
  public static List<String> split(String content, String delim) {
    List<String> lstText = new ArrayList<String>();
    if(content == null || delim == null) {
      return lstText;
    }

    int start = content.indexOf(delim);    
    while(start >= 0) {
      int end = content.indexOf(delim, start + 1);
      if(end < 0) {
        if(start < content.length()) {
          lstText.add(content.substring(start, content.length()));
        }
        break;
      }
      String extractedText = content.substring(start, end);
      if(extractedText.length() > 0) {
        lstText.add(extractedText);
      }
      start = end; 
    }
    return lstText;
  }
}
