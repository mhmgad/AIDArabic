package mpi.aida.util;

import gnu.trove.set.hash.TIntHashSet;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import mpi.aida.AidaManager;
import mpi.aida.access.DataAccess;
import mpi.aida.data.Entities;
import mpi.aida.data.Entity;
import mpi.aida.data.KBIdentifiedEntity;
import mpi.tools.basics2.Normalize;

import org.apache.commons.lang.StringUtils;

/**
 * This class contains some convenience wrappers for accessing YAGO data.
 * It has to use DataAccess and MUST NOT access the DB directly!
 * 
 *
 */
public class YagoUtil {
   
  public enum Gender {
    FEMALE, MALE;
  }
 
  /**
   * Checks whether the given String is an entity in YAGO
   * 
   * @param entity  Entity to check.
   * @return        true if the entity is in YAGO
   * @throws SQLException
   */
  public static boolean isYagoEntity(Entity entity) throws SQLException {
    return DataAccess.isYagoEntity(entity);
  }
  
  /**
   * Formats a given mention string properly to query a yago database.
   * 
   * It will first transform the string into a YAGO string (with "" and
   * UTF-8 with backslash encoding), and then escape the string properly
   * for a Postgres query.
   * 
   * @param mention Mention to format
   * @return        Mention in YAGO2/Postgres format
   */
  public static String getYagoMentionStringPostgresEscaped(String mention) {
    return getPostgresEscapedString(mention);
  }
  
  public static String getPostgresEscapedString(String input) {
    return input.replace("'", "''").replace("\\", "\\\\");
  }
  
  public static String getPostgresEscapedConcatenatedQuery(Collection<String> entities) {
    List<String> queryTerms = new LinkedList<String>();

    for (String term : entities) {
      StringBuilder sb = new StringBuilder();
      sb.append("E'").append(YagoUtil.getPostgresEscapedString(term)).append("'");
      queryTerms.add(sb.toString());
    }

    return StringUtils.join(queryTerms, ",");
  }
  
  public static String getIdQuery(TIntHashSet ids) {
    int[] conv = ids.toArray();
    return getIdQuery(conv);
  }
  
  public static String getIdQuery(int[] ids) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < ids.length; ++i) {
      sb.append(ids[i]);
      if (i < ids.length - 1) {
        sb.append(",");
      }
    }
    return sb.toString();
  }

  public static Entity getEntityForYagoId(String targetEntity) {
    return AidaManager.getEntity(new KBIdentifiedEntity(targetEntity, "YAGO"));
  }
  
  public static Entity getEntityForYago3Id(String targetEntity) {
    return AidaManager.getEntity(new KBIdentifiedEntity(targetEntity, "YAGO3"));
  }
  
  
  public static Entities getEntityForYagoId(Set<String> targetEntities) {
    Set<KBIdentifiedEntity> kbEntities = new HashSet<KBIdentifiedEntity>();
    for(String e: targetEntities) {
      kbEntities.add(new KBIdentifiedEntity(e, "YAGO"));
    }
    return AidaManager.getEntities(kbEntities);
  }
  
  public static boolean isNamedEntity(String entity) {
    if (Normalize.unWordNetEntity(entity) == null && Normalize.unWikiCategory(entity) == null && Normalize.unGeonamesClass(entity) == null
        && Normalize.unGeonamesEntity(entity) == null && !entity.equals("male") && !entity.equals("female")) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Creates a url-encoded part from the entity id (without KB-prefix)
   * @param entity  Entity ID without KB prefix in YAGO2 format.
   * @return  entity id as url-encoded part.
   */
  public static String getEntityAsUrlPart(String entity) throws UnsupportedEncodingException {
    return URLEncoder.encode(Normalize.unEntity(entity), "UTF-8").replace("+", "%20");
  }

  public static final String YAGO2_HAS_CITATIONS_TITLE_RELATION = "hasCitationTitle";
  public static final String YAGO2_HAS_WIKIPEDIA_CATEGORY_RELATION = "hasWikipediaCategory";
  public static final String YAGO2_HAS_WIKIPEDIA_ANCHOR_TEXT_RELATION = "hasWikipediaAnchorText";
  public static final String YAGO2_HAS_INTERNAL_WIKIPEDIA_LINK_TO_RELATION = "hasInternalWikipediaLinkTo";
  public static final String YAGO2_TYPE_RELATION = "type";
  public static final String YAGO2_SUBCLASSOF_RELATION = "subclassOf";
}