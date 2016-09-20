package mpi.aida.data;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EntityMetaData implements Serializable {
  
  private static Logger logger = LoggerFactory.getLogger(EntityMetaData.class);
  
  private static final long serialVersionUID = -5254220574529910760L;

  private int id;

  private String humanReadableRepresentation;

  private String url;
  
  private String knowledgebase;
  
  private String depictionurl;
  
  private String description;
  
  public EntityMetaData(int id, String humanReadableRepresentation, String url, 
      String knowledgebase, String depictionurl, String description) {
    super();
    this.id = id;
    this.humanReadableRepresentation = humanReadableRepresentation;
    this.url = url;
    this.knowledgebase = knowledgebase;
    this.depictionurl = depictionurl;
    this.description = description;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getHumanReadableRepresentation() {
    return humanReadableRepresentation;
  }

  public void setHumanReadableRepresentation(String humanReadableRepresentation) {
    this.humanReadableRepresentation = humanReadableRepresentation;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  
  public String getKnowledgebase() {
    return knowledgebase;
  }

  
  public void setKnowledgebase(String knowledgebase) {
    this.knowledgebase = knowledgebase;
  }

  
  public String getDepictionurl() {
    return depictionurl;
  }

  
  public void setDepictionurl(String depictionurl) {
    this.depictionurl = depictionurl;
  }

  public String getDepictionthumbnailurl() {
    return getDepictionthumbnailurl(200);
  }
  
  
  
  
  public String getDescription() {
    return description;
  }

  
  public void setDescription(String description) {
    this.description = description;
  }

  public String getDepictionthumbnailurl(int widthInPixels) {
    if (depictionurl == null) {
      return null;
    }
    String thumbnailUrl = depictionurl;
    
    int insertIndex = -1; 
    if (thumbnailUrl.contains("/commons")) {
      insertIndex = depictionurl.indexOf("/commons") + "/commons".length();
    } else if (thumbnailUrl.contains("/en")) {
      insertIndex = depictionurl.indexOf("/en") + "/en".length();
    }
    
    if (insertIndex != -1) {
      thumbnailUrl = depictionurl.substring(0, insertIndex);
      thumbnailUrl += "/thumb";
      thumbnailUrl += depictionurl.substring(insertIndex + "/thumb".length());
      
      // Add the last part twice
      String imageName = depictionurl.substring(depictionurl.lastIndexOf('/') + 1);
      thumbnailUrl += "/" + widthInPixels + "px-" + imageName;
      return thumbnailUrl;
    } else {
      // URL does not conform to expected schema.
      logger.warn("DepictionUrl does not conform to expected schema: '" + 
          depictionurl + "'.");
      return null;
    }
  }
}
