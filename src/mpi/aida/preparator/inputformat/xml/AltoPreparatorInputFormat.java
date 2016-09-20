package mpi.aida.preparator.inputformat.xml;

import java.util.ArrayList;
import java.util.List;

import mpi.aida.config.settings.PreparationSettings;
import mpi.aida.data.ExternalEntitiesContext;
import mpi.aida.data.Mention;
import mpi.aida.data.Mentions;
import mpi.aida.data.PreparedInput;
import mpi.aida.data.PreparedInputChunk;
import mpi.aida.preparation.documentchunking.DocumentChunker;
import mpi.aida.preparator.Preparator;
import mpi.aida.preparator.inputformat.PreparatorInputFormatInterface;
import mpi.tokenizer.data.Token;
import mpi.tokenizer.data.Tokens;

import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AltoPreparatorInputFormat implements PreparatorInputFormatInterface {

  private final Logger logger = LoggerFactory.getLogger(AltoPreparatorInputFormat.class);

  /**
   * Parses the ALTO xml format and process each page creating a prepared input.
   * @param contentTEI_ATTRIBUTE_TYPE
   * @param docId
   * @param prepSettings
   * @return
   * @throws Exception
   */
  @SuppressWarnings("unused")
  @Override
  public PreparedInput prepare(String docId, String content, PreparationSettings prepSettings, ExternalEntitiesContext context) throws Exception {

    Element root = XmlUtil.getXmlRootElement(content);
    Element layout = root.getChild("Layout", root.getNamespace());

    // Step 3: Process all extracted pages, generatin<ref target="http://www.deutsche-biographie.de/sfz11652.html" type="n">→</ref>g Prepared Input Chunks
    Tokens allTokens = new Tokens();
    Mentions allMentions = new Mentions();
    int lastTokenStartPos = 0;
    int lastTokenEndPos = 0;
    boolean isAnyPageProcessed = false;
    List<PreparedInputChunk> chunks = new ArrayList<PreparedInputChunk>();
    for (Element page : layout.getChildren("Page", layout.getNamespace())) {
      int pageNumber = Integer.parseInt(page.getAttributeValue("ID").substring(4));
      logger.debug("Processing page : " + pageNumber);
      String text = constructTextFromPageElement(page);
      PreparedInput pInp = Preparator.prepareInputData(text, docId, context, prepSettings);
      Tokens currPageTokens = pInp.getTokens();
      Mentions currPageMentions = pInp.getMentions();
      currPageTokens.setPageNumber(pageNumber);
      //pInp.getMentions().setPageNumber(pageNumber);      

      if (isAnyPageProcessed) {
        for (Token token : currPageTokens) {
          int currTokenStartPos = lastTokenEndPos + 1; // assuming the original end of previous token is taken care of
          int currTokenEndPos = currTokenStartPos + token.getOriginal().length() + token.getOriginalEnd().length();
          Token newToken = new Token(token.getStandfordId(), token.getOriginal(), token.getOriginalEnd(), currTokenStartPos, currTokenEndPos,
              token.getSentence(), token.getParagraph(), token.getPOS(), token.getNE());
          // UPDATE MENTION Mention mention = new Me
          if (currPageMentions.containsOffset(token.getBeginIndex())) {
            Mention mention = currPageMentions.getMentionForOffset(token.getBeginIndex());
            Mention newMention = new Mention(mention.getMention(), mention.getStartToken(), mention.getEndToken(), mention.getStartStanford(),
                mention.getEndStanford(), mention.getSentenceId());
            newMention.setCharOffset(newToken.getBeginIndex());
            newMention.setCharLength(mention.getCharLength());
            allMentions.addMention(newMention);
          }
          newToken.setPageNumber(token.getPageNumber());
          allTokens.addToken(newToken);
          lastTokenEndPos = currTokenEndPos;
        }
      } else {
        for (Token token : currPageTokens) {
          lastTokenStartPos = token.getBeginIndex();
          lastTokenEndPos = token.getEndIndex();
          allTokens.addToken(token);
          if (currPageMentions.containsOffset(token.getBeginIndex())) {
            allMentions.addMention(currPageMentions.getMentionForOffset(token.getBeginIndex()));
          }
        }
        isAnyPageProcessed = true;
      }
    }// end of loop over pages

    DocumentChunker docChunker = prepSettings.getDocumentChunker();
    PreparedInput fullPrepInp = docChunker.process(docId, content, allTokens, allMentions);

    return fullPrepInp;
  }

  /*
   * Process the smallest alto xml unit <String> 
   */
  private String processLine(Element line) {
    // String, SP , HYP
    StringBuffer content = new StringBuffer();
    List<Element> lstElements = line.getChildren();
    for (Element element : lstElements) {
      if ("String".equalsIgnoreCase(element.getName())) {
        content.append(element.getAttributeValue("CONTENT"));
      } else if ("SP".equalsIgnoreCase(element.getName())) {
        content.append(" ");
      } else if ("HYP".equalsIgnoreCase(element.getName())) {
        //FIXME check!
        content.append("");
      }
    }
    return content.toString();
  }

  /*
   * Extracts all the TextLine elements within the given text block and constructs a string. 
   */
  private String processTextBlock(Element block) {
    StringBuffer textContent = new StringBuffer();
    List<Element> lstLines = block.getChildren("TextLine", block.getNamespace());
    for (Element line : lstLines) {
      textContent.append(processLine(line)).append("\n");
    }
    return textContent.toString();
  }

  /*
   * Process the page element to extract atomic string units from xml
   */
  private String constructTextFromPageElement(Element page) {
    StringBuffer content = new StringBuffer();
    Element printSpace = page.getChild("PrintSpace", page.getNamespace());
    for (Element ele : printSpace.getChildren()) {
      if ("ComposedBlock".equalsIgnoreCase(ele.getName())) {
        List<Element> lstChildren = ele.getChildren();
        for (Element child : lstChildren) {
          if ("TextBlock".equalsIgnoreCase(child.getName())) {
            content.append(processTextBlock(child));
          } else {
            // TextLine
            content.append(processLine(child)).append("\n");
          }
        }
      } else if ("TextBlock".equalsIgnoreCase(ele.getName())) {
        content.append(processTextBlock(ele));
      }
    }
    return content.toString();
  }
}
