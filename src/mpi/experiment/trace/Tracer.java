package mpi.experiment.trace;

import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.hash.TIntHashSet;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import mpi.aida.access.DataAccess;
import mpi.aida.data.EntityMetaData;
import mpi.aida.data.Mention;
import mpi.experiment.trace.data.EntityTracer;
import mpi.experiment.trace.data.MentionTracer;
import mpi.experiment.trace.measures.MeasureTracer;
import mpi.tools.javatools.parsers.Char;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Tracer {
  private static final Logger logger = 
      LoggerFactory.getLogger(Tracer.class);
  
  private EntityEntityTracing eeTracing = new NullEntityEntityTracing();
  
  private Map<Mention, MentionTracer> mentions = null;

  private String path = null;

  private String docId = null;

  private List<MentionTracer> mentionsList = new LinkedList<MentionTracer>();

  private TIntObjectHashMap<EntityMetaData> entitiesMetaData = null;
  
  /**
   *  Use only when the tracing output isn't to be stored on disk, but returned
   *  instead (e.g. for the web interface)
   * @param docId
   */
  public Tracer(String docId) {
    this.docId = docId;
    mentions = new HashMap<Mention, MentionTracer>();
  }

  public Tracer(String path, String docId) {
    this.docId = docId;
    this.path = path;
    mentions = new HashMap<Mention, MentionTracer>();
  }

  public void addMention(Mention m, MentionTracer mt) {
    if (mentionsList == null) {
      mentionsList = new LinkedList<MentionTracer>();
    }
    mentionsList.add(mt);
    mentions.put(m, mt);
  }

  public void addEntityForMention(Mention mention, int entity, EntityTracer entityTracer) {
    MentionTracer mt = mentions.get(mention);
    mt.addEntityTracer(entity, entityTracer);
  }

  public void addMeasureForMentionEntity(Mention mention, int entity, MeasureTracer measure) {
    MentionTracer mt = mentions.get(mention);
    EntityTracer et = mt.getEntityTracer(entity);
    et.addMeasureTracer(measure);
  }

  public void setMentionEntityTotalSimilarityScore(Mention mention, int entity, double score) {
    MentionTracer mt = mentions.get(mention);
    EntityTracer et = mt.getEntityTracer(entity);
    et.setTotalScore(score);
  }

  public void writeOutput(String resultFileName, boolean withYago, boolean relatedness) throws InterruptedException {
    loadEntitiesMetaData();
    String outputPath;
    if (withYago) {
      outputPath = path + "/html/yago/" + resultFileName + "/" + docId + "/";
    } else if (relatedness) {
      outputPath = path + "/relatedness_html/all/" + resultFileName + "/" + docId + "/";
    } else {
      outputPath = path + "/html/all/" + resultFileName + "/" + docId + "/";
    }
    
    File outputDir = new File(outputPath);
    if (!outputDir.exists()) {
      outputDir.mkdirs();
    }
    
    try {
      File entitiesFile = new File(outputPath + "entities.html");
      BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(entitiesFile), "UTF-8"));
      writer.write("<html><head>");
      writer.write(generateCss());
      writer.write(generateScript());
      writer.write("</head><body>");
      writer.write(eeTracing.getHtml(entitiesMetaData));
      writer.write("</body></html>");
      writer.flush();
      writer.close();
    } catch (IOException e) {
      logger.warn("Couldn't write '" + docId + "/entities.html', skipping ...");
    }
    
    for (MentionTracer m : mentionsList) {
      File outFile = new File(outputPath + m.getOffset() + ".html");
      String out = getMentionOutput(m, false);
      try {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile), "UTF-8"));
        writer.write(out);
        writer.flush();
        writer.close();
      } catch (IOException e) {
        for (int i = 0; i < 2; i++) {
          try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile), "UTF-8"));
            writer.write(out);
            writer.flush();
            writer.close();
          } catch (IOException ioe) {
            // simply retry in the outer for loop
          }
        }

        logger.warn("Couldn't write '" + docId + "/" + m.getName() + "', skipping ...");
        // if it didn't work, try writing the next file
        continue;
      }
    }
  }

  private String getMentionOutput(MentionTracer m, boolean isWebInterface) {
    StringBuilder sb = new StringBuilder();
    if(!isWebInterface) {
      sb.append("<script language='JavaScript'> function setVisibility(id, visibility) {document.getElementById(id).style.display = visibility;}</script> ");
      sb.append("<h1>" + m.getName() + "</h1>");
    }

    if (m.getEntityTracers().size() > 0) {
      sb.append("<table border='1'><tr>");

      sb.append("<th>Entity</th>");

      List<EntityTracer> es = new LinkedList<EntityTracer>(m.getEntityTracers());
      Collections.sort(es);

      // write table header
      for (MeasureTracer mt : es.get(0).getMeasureTracers()) {
        sb.append("<th>" + mt.getName() + "</th>");
      }

      sb.append("</tr>");

      // write single entities in order of decreasing score
      for (EntityTracer e : es) {
        sb.append("<tr><td valign='top'>" + buildEntityUriAnchor(e.getEntityId())
            + "<br /> <strong>" + e.getTotalScore() + " </strong> <br />"
            + "<a target='_blank' href='entity.jsp?entity=" + e.getEntityId() + "'>Info</a></td>");

        for (MeasureTracer mt : e.getMeasureTracers()) {
          sb.append("<td valign='top'>" + mt.getOutput() + "</td>");
        }
      }

      sb.append("</table>");
    }
    return sb.toString();
  }

  private String buildEntityUriAnchor(int entityId) {
    String uriString = "NO_METADATA";
    String displayString = new Integer(entityId).toString();
    if (entitiesMetaData != null && entitiesMetaData.size() > 0) {
      EntityMetaData md = entitiesMetaData.get(entityId);
      if (md != null) {
        uriString = entitiesMetaData.get(entityId).getUrl();
        displayString = Char.toHTML(entitiesMetaData.get(entityId).getHumanReadableRepresentation());
        displayString += "<br /><span font-size='small'>[" + uriString + "]</span>";
      }
    }
    String entityAnchor = "<a class='entityAnchor' target='_blank' href='" + uriString + "'>" + displayString + "</a>";
    return entityAnchor;
  }

  public void enableEETracing() {
    eeTracing = new EntityEntityTracing();
  }
  
  public void enableEETracing(boolean doDocumentEETracing) {
    eeTracing = new EntityEntityTracing(doDocumentEETracing);
  }
  
  public EntityEntityTracing eeTracing() {
    return eeTracing;
  }
  
  public String generateScript() {
    StringBuilder sb = new StringBuilder();
    sb.append("<script type='text/javascript'>\n");
    sb.append("function showHide(id) {\n");
    sb.append("var checkboxElement = document.getElementById( id + '-checkbox');\n");
    sb.append("var divElement = document.getElementById( id + '-div');\n");
    sb.append("if (checkboxElement.checked) {\n");
    sb.append("divElement.style.display = 'block';\n");
    sb.append("} else {\n");
    sb.append("divElement.style.display = 'none';\n");
    sb.append("}\n");
    sb.append("}\n");
    sb.append("function setVisibility(id, visibility) {document.getElementById(id).style.display = visibility;}");
    sb.append("</script>\n");
    return sb.toString();
  }

	private String generateCss() {
	  StringBuilder sb = new StringBuilder();
	  sb.append("<style type='text/css'>");
	  sb.append(".mmTable { border:1px solid gray }");
	  sb.append(".mmTable tr { border:1px solid gray }");
	  sb.append(".mmTable td { border:1px solid gray }");
	  sb.append("</style>");
	  return sb.toString();
  }

  public String getHtmlOutputForWebInterface() {
    loadEntitiesMetaData();
		StringBuilder sb = new StringBuilder();
		for (MentionTracer m : mentionsList) {
			sb.append("<h3><a href=\"#\">"  + m.getMentionStr() + "</a></h3>");
			sb.append("<div>");
			sb.append(getMentionOutput(m, true));
			sb.append("</div>");
		}
		return sb.toString();
	}
  
  private void loadEntitiesMetaData() {
    TIntHashSet entities = new TIntHashSet();
    for (MentionTracer m : mentionsList) { 
        for(EntityTracer e : m.getEntityTracers()) {
          entities.add(e.getEntityId());
        }
    }
    entitiesMetaData = DataAccess.getEntitiesMetaData(entities.toArray());
  }
}
  