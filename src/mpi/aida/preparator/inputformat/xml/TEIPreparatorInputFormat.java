package mpi.aida.preparator.inputformat.xml;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mpi.aida.config.settings.PreparationSettings;
import mpi.aida.data.ExternalEntitiesContext;
import mpi.aida.data.PreparedInput;
import mpi.aida.preparator.Preparator;
import mpi.aida.preparator.inputformat.PreparatorInputFormatInterface;

import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.Namespace;


public class TEIPreparatorInputFormat implements PreparatorInputFormatInterface {

  /*
   * TEI related xml elements 
   */

  private static final String TEI_NDB = "ndb";

  // TEI attribute names
  private static final String TEI_ATTRIBUTE_TYPE = "type";

  private static final String TEI_ATTRIBUTE_SUBTYPE = "subtype";

  // private static final String TEI_ATTRIBUTE_REF = "n";

  // TEI element names
  private static final String TEI_ELEMENT_TEXT = "text";

  private static final String TEI_ELEMENT_BODY = "body";

  private static final String TEI_ELEMENT_DIV = "div";

  private static final String TEI_ELEMENT_PARA = "p";

  private static final String TEI_ELEMENT_CHOICE_ABBR = "abbr";

  private static final String TEI_ELEMENT_REF_TARGET = "ref";

  private static final String TEI_ELEMENT_PERSON_NAME = "persName";

  private static final String TEI_ELEMENT_SEGMENT = "seg";

  // Top level divs type names
  private static final String TEI_ENTRY_HEAD = "kopf";

  private static final String TEI_ENTRY_GENEAL = "geneal";

  private static final String TEI_ENTRY_LIFE = "leben";

  private static Map<String, String> stringToGroundTruth = new HashMap<String, String>();

  /**
   * Parses the TEI xml format and extracts the biographie text
   * @param docId
   * @param content
   * @param prepSettings
   * @return
   * @throws Exception
   */
  @Override
  public PreparedInput prepare(String docId, String content, PreparationSettings prepSettings, ExternalEntitiesContext entitiesContext) throws Exception {
    stringToGroundTruth.clear();
    String text = extractTeiText(content);
    PreparedInput pInp = Preparator.prepareInputData(text, docId, entitiesContext, prepSettings);

    return pInp;
  }

  public Map<String, String> getExtractedGroundTruth() {
    return stringToGroundTruth;
  }

  private String extractTeiText(String content) throws Exception {
    String result = "";
    Element root = XmlUtil.getXmlRootElement(content);
    Element body = root.getChild(TEI_ELEMENT_TEXT, root.getNamespace()).getChild(TEI_ELEMENT_BODY, root.getNamespace());
    for (Element entry : body.getChildren()) {
      if (TEI_NDB.equalsIgnoreCase(entry.getAttributeValue(TEI_ATTRIBUTE_SUBTYPE))) {
        result = getCompleteBiographie(entry, root.getNamespace());
        return cleanExtractedText(result);
      }
    }

    return result;
  }

  private static Element removeElement(Element parent, String elementToRemove, Namespace ns) {
    List<Element> children = parent.getChildren();
    if (children.isEmpty()) {
      return parent;
    }

    for (Element child : children) {
      removeElement(child, elementToRemove, ns);
    }

    switch (elementToRemove) {
      case TEI_ELEMENT_REF_TARGET:
        // if parent is name and ref type = "n"
        for (Element child : parent.getChildren()) {
          Element refChild = child.getChild(TEI_ELEMENT_REF_TARGET, ns);
          if (refChild == null) {
            //System.out.println("No Ref Child for : " + child.getName());
            continue;
          }

          String typeAttr = refChild.getAttributeValue("type");
          refChild.getParentElement().getTextTrim();
          refChild.getTextTrim();
          if (typeAttr != null && typeAttr.equalsIgnoreCase("n")) {
            //System.out.println("Store the target url : " + refChild.getAttributeValue("target"));
            Pattern pattern = Pattern.compile("\\W(.+) \\W");
            Matcher matcher = pattern.matcher(child.getValue());
            if (matcher.find()) {
              stringToGroundTruth.put(matcher.group(1), refChild.getAttributeValue("target"));
            }
          }
          child.removeChild(refChild.getName(), ns);                
        }
        //parent.removeChildren(elementToRemove, ns);
        break;
      case TEI_ELEMENT_CHOICE_ABBR:
        Element ele = parent.getChild(elementToRemove, ns);
        if (ele != null) {
          ele.setText(XmlUtil.EMPTY_STR);
        }
        break;
      default:
        break;
    }
    return parent;
  }

  private static Element cleanup(Element parent, Namespace ns) {
    // removes "->" link symbol from text
    parent = removeElement(parent, TEI_ELEMENT_REF_TARGET, ns);
    // replaces <abbr> element of choice by empty str
    parent = removeElement(parent, TEI_ELEMENT_CHOICE_ABBR, ns);
    return parent;
  }

  private static String cleanExtractedText(String extractedText) {
    extractedText = extractedText.replaceAll("[ ]+", " ");
    extractedText = extractedText.substring(0, extractedText.length() - 1);
    return extractedText;
  }

  private static String getInfo(Element element, String divType, Namespace ns) {
    StringBuffer info = new StringBuffer();
    Element para = element.getChild(TEI_ELEMENT_PARA, ns);
    boolean addNewLineAtEnd = true;

    switch (divType) {
      case TEI_ENTRY_HEAD:
        Element tmpEle = para.getChild(TEI_ELEMENT_PERSON_NAME, ns);
        if (tmpEle != null) {
          StringBuffer tmpNameBuff = new StringBuffer();
          //tmpNameBuff.append("Start : ");
          List<Element> children = tmpEle.getChildren();
          //size = 2 (fN, lN)
          for(int i=children.size()-1;i>=0;i--){
            tmpNameBuff.append(children.get(i).getValue());
            if(i!=0) {
              tmpNameBuff.append(XmlUtil.EMPTY_STR);
            }
          }          
          info.append(tmpNameBuff.toString()).append(XmlUtil.EMPTY_STR + ":");
        }
        tmpEle = para.getChild(TEI_ELEMENT_SEGMENT, ns);

        if (tmpEle != null) {
          info.append(cleanup(tmpEle, ns).getValue()).append(XmlUtil.NEW_LINE);
        }
        break;
      case TEI_ENTRY_GENEAL:
      case TEI_ENTRY_LIFE:
        if (para == null) {
          info.append(cleanup(element, ns).getValue()).append(XmlUtil.NEW_LINE);
        } else {
          info.append(cleanup(para, ns).getValue()).append(XmlUtil.NEW_LINE);
        }
        break;
      default:
        addNewLineAtEnd = false;
        break;
    }

    if (addNewLineAtEnd) {
      info.append(XmlUtil.NEW_LINE);
    }

    return info.toString();
  }

  private static String getCompleteBiographie(Element divEntry, Namespace ns) {
    StringBuffer sb = new StringBuffer();

    for (Element child : divEntry.getChildren()) {
      if (!child.getName().equals(TEI_ELEMENT_DIV) || !child.hasAttributes()) continue;

      Attribute attr = child.getAttribute(TEI_ATTRIBUTE_TYPE);
      if (attr != null) {
        sb.append(getInfo(child, attr.getValue(), ns));
      }

    }
    return sb.toString();
  }
}
