package mpi.aida.preparator.inputformat.xml;

import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;


public class XmlUtil {
  public static final String NEW_LINE = "\n";

  public static final String EMPTY_STR = " ";

  // Byte Order Mark - should be cleaned before parsing xml.
  private static final String BOM_IDENTIFIER = "^([\\W]+)<";

  private static final String XML_TAG_START = "<";

  private static final Pattern pattern = Pattern.compile("(&\\w+;)");
  /*
   * Removes any special characters encoded before the beginning of XML content
   */
  public static String cleanXmlString(String xmlContent) {
    return xmlContent.replaceFirst(BOM_IDENTIFIER, XML_TAG_START);
  }

  public static Element getXmlRootElement(String content) throws Exception {
    // Step 1: Clean XML to remove junk characters
    StringReader sr = new StringReader(cleanXmlString(content));
    SAXBuilder sBuilder = new SAXBuilder();

    // Step 2: Build XML object model
    Document xmlBook = sBuilder.build(sr);
    return xmlBook.getRootElement();
  }
  
  public static boolean containsEncodedText(String text) {
    Matcher matcher = pattern.matcher(text);
    int count = 0;
    while(matcher.find()) {
      count++;
    }    
    return count > 0;
  }
  
  
  public static int computeAdditionalEmptySpacesLength(String text) {    
    Matcher matcher = pattern.matcher(text);
    
    int len = 0;
    while(matcher.find()) {
      // encoded strings represent characters of length "1".
      // (TODO: need to check for any encoded string that represent a character with length more than 1) 
      len += (matcher.group(0).length() - 1); 
    }
    return len;
  }
}
