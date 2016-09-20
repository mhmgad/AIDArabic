package mpi.aida.preparator.inputformat.xml;

import mpi.aida.config.settings.PreparationSettings;
import mpi.aida.data.ExternalEntitiesContext;
import mpi.aida.data.PreparedInput;
import mpi.aida.preparator.Preparator;
import mpi.aida.preparator.inputformat.PreparatorInputFormatInterface;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.StringReader;

import com.google.common.collect.Range;

public abstract class XMLPreparatorInputFormat implements PreparatorInputFormatInterface {

  enum TextPart { NONE, TITLE, NEW_PARAGRAPH, TEXT }

  private String uniqueId = null;

  private XMLExtraction extractInfo(String content) throws XMLStreamException {
    XMLExtraction results = new XMLExtraction();

    StringBuilder sb = new StringBuilder();

    XMLInputFactory factory = XMLInputFactory.newInstance();
    factory.setProperty(XMLInputFactory.IS_VALIDATING, false);
    factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, false);
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    factory.setProperty(XMLInputFactory.IS_COALESCING, true);
//    factory.setProperty("http://java.sun.com/xml/stream/properties/ignore-external-dtd", Boolean.TRUE);
    XMLStreamReader reader = factory.createXMLStreamReader(new StringReader(content));

    int startOffset = 0;
    int endOffset = 0;
    String currentElement = null;
    // number of empty spaces introduced by xml stream reader as a result of encoded string processing
    int spacesToDelete = 0;
    int paragraphCount = -1;
    while (reader.hasNext()) {
      reader.next();
      if (reader.isStartElement()) {
        currentElement = reader.getLocalName();
        signalStartElement(currentElement);
        if (isDocumentIdElement(currentElement)) {
          uniqueId = getDocumentId(currentElement, reader);
        }
      } else if (reader.isEndElement()) {
        signalEndElement(reader.getLocalName());
      } else if (currentElement != null && reader.isCharacters()) {
        // reader.getLocation() is at the END of the text element.
        startOffset = reader.getLocation().getCharacterOffset();

        // Fill whitespaces if necessary.
        if (!shouldCompactText()) {
          int fillLength = startOffset - endOffset;
          sb.append(generateEmptyString(fillLength));
        }

        TextPart textPart = determineTextPartForElement(currentElement);
        if (textPart != TextPart.NONE) {
          int start = sb.length();
          String text = reader.getText();
          if (shouldCompactText()) {
            text = text.replaceAll("[\\s]+", " ");
          }
          sb.append(text);
          int end = sb.length();
          if (textPart == TextPart.TITLE) {
            results.setTitle(text);
            // Range is with respect to the processed text.
            results.setTitleRange(Range.closedOpen(start, end));
          } else if (textPart == TextPart.NEW_PARAGRAPH && paragraphCount == -1) {
            // Range is with respect to the processed text.
            results.setAbstractRange(Range.closedOpen(start, end));
            ++paragraphCount;
          }
          if (shouldCompactText()) {
            // Compaction will loose spaces between elements, replace by newline.
            sb.append("\n");
          }
        } else if (!shouldCompactText()) {
          sb.append(generateEmptyString(reader.getTextLength()));
        }

        if (spacesToDelete > 0) {
          sb = sb.delete(sb.length() - spacesToDelete, sb.length());
        }

        if (!shouldCompactText()) {
          endOffset = startOffset + reader.getTextLength();
          spacesToDelete = XmlUtil.computeAdditionalEmptySpacesLength(content.substring(startOffset, endOffset));
        }
      }
    }

    results.setContent(sb.toString());
    return results;
  }

  private String generateEmptyString(int n) {
    StringBuilder sb = new StringBuilder();
    for(int i=0;i<n;i++) {
      sb.append(" ");
    }
    return sb.toString();
  }
  
  protected abstract TextPart determineTextPartForElement(String element);
  
  protected abstract boolean isDocumentIdElement(String element);

  protected void signalStartElement(String elementName) {
    // nothing - can be overriden.
  }

  protected void signalEndElement(String elementName) {
    // nothing - can be overriden.
  }

  protected boolean shouldCompactText() {
    return false;
  }

  protected abstract String getDocumentId(String element, XMLStreamReader reader) throws XMLStreamException;

  @Override
  public PreparedInput prepare(String docid, String content, PreparationSettings prepSettings, ExternalEntitiesContext externalContext) throws Exception {
    XMLExtraction xmlExtraction = extractInfo(content);
    // TODO below does not work - however offset mappings are necessary for original offsets in XML containing &...; elements
//    boolean needsOffsetMapping = false;
//    SimpleOffsetRealigner mapper = null;
//    if (XmlUtil.containsEncodedText(content)) {
//      needsOffsetMapping = true;
//      mapper = new SimpleOffsetRealigner();
//      mapper.process(text);
//    }
    PreparedInput pInp = Preparator.prepareInputData(
            xmlExtraction.getContent(), uniqueId, externalContext, prepSettings);
    pInp.setTitle(xmlExtraction.getTitle());
    pInp.setTitleRange(xmlExtraction.getTitleRange());
    pInp.setAbstractRange(xmlExtraction.getAbstractRange());

//    if (needsOffsetMapping) {
//      pInp = mapper.reAlign(pInp);
//    }
    return pInp;
  }

  class XMLExtraction {
    private String title;
    private String content;
    private Range titleRange;
    private Range abstractRange;

    public String getTitle() {
      return title;
    }

    public void setTitle(String title) {
      this.title = title;
    }

    public String getContent() {
      return content;
    }

    public void setContent(String content) {
      this.content = content;
    }

    public Range getTitleRange() {
      return titleRange;
    }

    public void setTitleRange(Range<Integer> titleRange) {
      this.titleRange = titleRange;
    }

    public Range getAbstractRange() {
      return abstractRange;
    }

    public void setAbstractRange(Range<Integer> abstractRange) {
      this.abstractRange = abstractRange;
    }
  }
}