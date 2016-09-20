package mpi.aida.preparator.inputformat.xml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

public class SpiegelPreparatorInputFormat extends XMLPreparatorInputFormat {

  private Logger logger_ = LoggerFactory.getLogger(SpiegelPreparatorInputFormat.class);

  public static final String SPIEGEL_ID_ELEMENT = "artikel-id";
  public static final Set<String> TEXT_ELEMENTS =
          new HashSet<>(Arrays.asList(
                  new String[] { "absatz", "vorspann", "kurztitel", "titel", "beschriftung" } ));

  private Map<String, Boolean> insideFlags_;

  public SpiegelPreparatorInputFormat() {
    insideFlags_ = new HashMap<>();
    for (String e : TEXT_ELEMENTS) {
      insideFlags_.put(e, false);
    }
  }

  @Override
  protected TextPart determineTextPartForElement(String element) {
    TextPart textPart = TextPart.NONE;
    if (insideFlags_.containsKey(element) &&
            insideFlags_.get(element)) {
      if (element.equals("titel")) {
        textPart = TextPart.TITLE;
      } else if (element.equals("absatz")) {
        textPart = TextPart.NEW_PARAGRAPH;
      } else if (TEXT_ELEMENTS.contains(element)) {
        textPart = TextPart.TEXT;
      }
    }
    return textPart;
  }

  @Override
  protected boolean isDocumentIdElement(String element) {
    return element.equals(SPIEGEL_ID_ELEMENT);
  }

    @Override
  protected void signalStartElement(String elementName) {
    if (insideFlags_.containsKey(elementName)) {
      if (insideFlags_.get(elementName)) {
        logger_.warn("Encountered nested <" + elementName + ">.");
      }
      insideFlags_.put(elementName, true);
    }
  }

  @Override
  protected void signalEndElement(String elementName) {
    if (insideFlags_.containsKey(elementName)) {
      if (!insideFlags_.get(elementName)) {
        logger_.warn("Encountered nested <" + elementName + ">.");
      }
      insideFlags_.put(elementName, false);
    }
  }

  @Override
  protected boolean shouldCompactText() {
    return true;
  }

  @Override
  protected String getDocumentId(String element, XMLStreamReader reader) throws XMLStreamException {
    if (element.equals(SPIEGEL_ID_ELEMENT)) {
      return reader.getElementText();
    } else {
      throw new IllegalArgumentException("Element is not correct for extracting docId.");
    }
  }
}

