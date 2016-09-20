package mpi.aida.preparator.inputformat.xml;

import javax.xml.stream.XMLStreamReader;

public class NYTPreparatorInputFormat extends XMLPreparatorInputFormat {

  private static final String NYT_PUBDATA = "pubdata";
  private static final String NYT_PUBDATA_EXREF = "ex-ref";
  private static final String NYT_PUBDATA_RES = "res=";

  @Override
  protected TextPart determineTextPartForElement(String element) {
    return TextPart.TEXT;
  }

  @Override
  protected boolean isDocumentIdElement(String element) {
    return element.equals(NYT_PUBDATA);
  }

  @Override
  protected String getDocumentId(String element, XMLStreamReader reader) {
    String id = null;
    if (reader.getAttributeCount() > 0) {
      for (int i = 0; i < reader.getAttributeCount(); i++) {
        String attrName = reader.getAttributeLocalName(i);
        String attrVal = reader.getAttributeValue(i);
        // retrieve unique id for pubdata tag
        if (reader.getLocalName().equals(NYT_PUBDATA) && attrName.equalsIgnoreCase(NYT_PUBDATA_EXREF)) {
          int idx = attrVal.indexOf(NYT_PUBDATA_RES);
          id = attrVal.substring(idx + 4);
        }
      }
    }
    return id;
  }
}

