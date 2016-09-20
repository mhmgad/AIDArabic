package mpi.aida.util.htmloutput;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mpi.aida.config.AidaConfig;
import mpi.aida.config.settings.PreparationSettings.LANGUAGE;

import org.apache.commons.lang.StringEscapeUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;


/**
 * Constructs HTML version of disambiguated results from JSON String.
 * Supports HTML construction from DisambiguationResults object as well.
 */
public class HtmlGenerator {

  /**
   * This method generates HTML from given JSON representation of disambiguated result
   *
   * @param docTitle Title of the HTML document.
   * @param jsonResult A JSONObject containing the disambiguation results.
   * @return HTML body as a string
   * @throws Exception
   */
  public String constructFromJson(String docTitle, JSONObject jsonResult) throws Exception {
    //GenerateWebHtml gen = new GenerateWebHtml();
    //String html = gen.processJSON(content, input, jsonRepr, false);
    StringBuilder sb = constructHTML(docTitle, jsonResult, null);
    sb.append("</body></html>");
    return sb.toString().replaceAll("\n", "<br />");
  }

  /**
   * This method generates Header Tag with given content and level.
   * @param content The string to be placed between heading tag
   * @param level The level of heading (1 to 6)
   * @return HTML String
   */
  public String generateHeading(String content, int level){
      String tagVal = "h"+level;
      String res = "<" + tagVal + ">";
      res = res.concat(content);
      res = res.concat("</" + tagVal + ">");
      return res;
  }

  /**
   * Generates HTML snippet of annotated text with all hyperlinks.
   * @return HTML snippet for annotated string.
   */
  public String getAnnotatedText(JSONObject jsonContent) {
    Map<String, String> url2rep = new HashMap<String, String>();
    JSONObject entities = (JSONObject) jsonContent.get("entityMetadata");
    for (Object o : entities.keySet()) {
      String id = (String) o;
      url2rep.put(
          (String) ((JSONObject) entities.get(id)).get("entityId"),
          (String) ((JSONObject) entities.get(id)).get("readableRepr"));
    }
    String annotatedText = (String)jsonContent.get("annotatedText");
    Pattern pattern = Pattern.compile("\\[\\[([^|]+)\\|([^]]+)\\]\\]");
    Matcher matcher = pattern.matcher(annotatedText);
    StringBuffer sb =  new StringBuffer();
    int current = 0;
    while (matcher.find()){
      sb.append(annotatedText.substring(current, matcher.start()));
      String kbId = matcher.group(1);
      String mention = matcher.group(2);
      String representation = kbId;
      String url = kbId;
      JSONObject entityMetadata = (JSONObject) entities.get(kbId);
      if (entityMetadata != null) {
        String repr = (String) entityMetadata.get("readableRepr");
        if (repr != null) {
          representation = repr;
        }
        String eurl = (String) entityMetadata.get("url");
        if(eurl != null) {
          url = eurl;
        }
      }
      sb.append("<span class='eq'>" + mention + "</span>");
      sb.append(" <small>[<a href=" + StringEscapeUtils.escapeHtml(url) + ">" + StringEscapeUtils.escapeHtml(representation) + "</a>]</small>");
      current = matcher.end();
    }
    sb.append(annotatedText.substring(current, annotatedText.length()));
    return sb.toString();
  }

  /**
   * Generates HTML list items based on the mention-entity result from json.
   * @return HTML snippet
   */
  @SuppressWarnings("rawtypes")
  public String generateMEHtml(JSONObject jsonContent){
    JSONArray mentions = (JSONArray) jsonContent.get("mentions");
    JSONObject entityMetadata = (JSONObject) jsonContent.get("entityMetadata");
    Iterator itMention = mentions.iterator();
    StringBuilder htmlList = new StringBuilder();
    //htmlList.append(generateHeading("Mappings", 2));
    htmlList.append("<ul>");
    while(itMention.hasNext()){
      htmlList.append("<li>");
      JSONObject tmpMention = (JSONObject)itMention.next();
//      htmlList.append("["+(String)jsonContent.get("docID")+"] ");
      htmlList.append("<strong>\"").append(tmpMention.get("name")).append("\"</strong>");
      htmlList.append(" <em>[").append(tmpMention.get("offset")).append("]</em>");
      htmlList.append("<ul>");
      JSONArray entities = (JSONArray)tmpMention.get("allEntities");
      for (int i = 0; i < entities.size(); ++i) {
        htmlList.append("<li>");
        JSONObject entity = (JSONObject) entities.get(i);
        String kbId = (String) entity.get("kbIdentifier");
        String url = (String) ((JSONObject) entityMetadata.get(kbId)).get("url");
        String hkbId = StringEscapeUtils.escapeHtml(kbId);
        if(url == null) {
          url = "";
        }
        htmlList.append("<a href=\"").append(url).append("\">").append(hkbId).append("</a>");
        htmlList.append(" (" + entity.get("disambiguationScore") + ")");
        htmlList.append("</li>");
      }
      htmlList.append("</ul>");
      htmlList.append("</li>");
    }
    htmlList.append("</ul>");
    return htmlList.toString();
  }

  private StringBuilder constructHTML(String docTitle, JSONObject jsonContent, String html) {
    StringBuilder sb = new StringBuilder();
    String htmlTag = "<html>";
    if(AidaConfig.getLanguage() == LANGUAGE.ar) {
      htmlTag = "<html dir='rtl' lang='ar'>";
    }
    sb.append(htmlTag + "<head><title>AIDA annotations for ").append(docTitle).append("</title>");
    sb.append("<meta http-equiv='content-type'");
    sb.append("CONTENT='text/html; charset=utf-8' />");
    sb.append("<style type='text/css'>");
    sb.append("html { font-family: sans-serif; } ");
    sb.append("body { padding: 10pt; } ");
    sb.append("a { background: 0 0 } ");
    sb.append("a:active, a:hover { outline: 0 } ");
    sb.append("b, strong { font-weight: 700 } ");
    sb.append("h1 { font-size: 2em; margin: .67em 0 } ");
    sb.append("small { font-size: 80% } ");
    sb.append("img { border: 0 } ");
    sb.append("hr { -moz-box-sizing: content-box; box-sizing: content-box; height: 0 } ");
    sb.append("table { border-collapse: collapse; border-spacing: 0 } ");
    sb.append("td, th { padding: 0 } ");
    sb.append(".eq { background-color:#87CEEB } ");
    sb.append("</style>").append("<body>");
    sb.append(generateHeading("AIDA annotations for " + docTitle, 1));
//    sb.append(generateHeading("AnotatedText", 2));
    sb.append("<div id='annotatedText'>");
    sb.append(getAnnotatedText(jsonContent));
    sb.append("</div>");
    sb.append("\n");
    if(html!=null)
      sb.append(html);
    else
    {
      sb.append("<h2>Mentions and Entities</h2>");
      sb.append(generateMEHtml(jsonContent));
    }
    return sb;
  }
}