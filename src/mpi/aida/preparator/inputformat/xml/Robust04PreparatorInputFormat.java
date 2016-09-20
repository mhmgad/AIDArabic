package mpi.aida.preparator.inputformat.xml;

import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import mpi.aida.config.settings.PreparationSettings;
import mpi.aida.data.ExternalEntitiesContext;
import mpi.aida.data.PreparedInput;
import mpi.aida.preparator.Preparator;
import mpi.aida.preparator.inputformat.PreparatorInputFormatInterface;


public class Robust04PreparatorInputFormat implements PreparatorInputFormatInterface {

  private static final Object ROBUST_DOCNO = "DOCNO";
  private String docId;
  
  @SuppressWarnings("unused")
  private String extractText(String xmlText) throws XMLStreamException {
    XMLInputFactory factory = XMLInputFactory.newInstance();
    XMLStreamReader reader = factory.createXMLStreamReader(new StringReader(xmlText));
    StringBuilder sb = new StringBuilder();
    while(reader.hasNext()) {
      if(reader.isStartElement() && reader.getLocalName().equals(ROBUST_DOCNO)) {
        docId = reader.getElementText();
        sb.append(docId);
      } else if(reader.isCharacters()) {
        if(reader.getText().length() > 0) {
          sb.append(reader.getText());
        }        
      }
      reader.next();
    }
    return sb.toString();
  }
  
  private String extractTextUsingRegex(String xmlText) {
    Pattern pattern = Pattern.compile("<DOCNO>(.*?)</DOCNO>");
    Matcher matcher = pattern.matcher(xmlText);
    if(matcher.find()) {
      docId = matcher.group(1);
    }
    return xmlText.replaceAll("<.*?>", "");    
  }
  
  @Override
  public PreparedInput prepare(String docId, String text, PreparationSettings prepSettings, ExternalEntitiesContext externalContext) throws Exception {
    text = XmlUtil.cleanXmlString(text);
    String extractedContent = extractTextUsingRegex(text);
    // override the docid by DOCNO value
    return Preparator.prepareInputData(extractedContent, this.docId, externalContext, prepSettings);
  }
}