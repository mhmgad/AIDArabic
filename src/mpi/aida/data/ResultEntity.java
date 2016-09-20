package mpi.aida.data;

import java.io.Serializable;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Entity the was assigned to a ResultMention. 
 *
 */
public class ResultEntity implements Comparable<ResultEntity>, Serializable {

  private static final long serialVersionUID = -7062155406718136994L;

  private KBIdentifiedEntity kbEntity;

  /** Score assigned to the entity */
  private double disambiguationScore;

  public ResultEntity(String identifier, String knowledgebase, double disambiguationScore) {
    super();
    this.kbEntity = new KBIdentifiedEntity(identifier, knowledgebase);
    this.disambiguationScore = disambiguationScore;
  }
  
  public ResultEntity(Entity entity, double disambiguationScore) {
    super();
    if (entity instanceof NullEntity) {
      this.kbEntity = new KBIdentifiedEntity(Entity.OOKBE, "AIDA");
      this.disambiguationScore = 0.0;
    } else {
      this.kbEntity = new KBIdentifiedEntity(entity.getIdentifierInKb(), 
          entity.getKnowledgebase());
      this.disambiguationScore = disambiguationScore;
    }
  }

  public static ResultEntity getNoMatchingEntity() {
    return new ResultEntity(Entity.OOKBE, "AIDA", 0.0);
  }

  public static List<ResultEntity> getResultEntityAsList(ResultEntity re) {
    List<ResultEntity> res = new ArrayList<ResultEntity>(1);
    res.add(re);
    return res;
  }

  /**
   * @return  original knowledgebase identifier of the entity
   */
  public String getEntity() {
    return kbEntity.getIdentifier();
  }

  public String getKnowledgebase() {
    return kbEntity.getKnowledgebase();
  }
  
  public KBIdentifiedEntity getKbEntity() {
    return kbEntity;
  }
  
  public double getDisambiguationScore() {
    return disambiguationScore;
  }

  public void setDisambiguationScore(double disambiguationScore) {
    this.disambiguationScore = disambiguationScore;
  }
  
  public boolean isNoMatchingEntity() {
    return kbEntity.getIdentifier().equals(Entity.OOKBE);
  }

  @Override
  public int compareTo(ResultEntity re) {
    // natural ordering for ResultEntities is descending
    return new Double(new Double(re.getDisambiguationScore())).compareTo(disambiguationScore);
  }
  
  public String toString() {
    NumberFormat df = NumberFormat.getInstance(Locale.ENGLISH);
    df.setMaximumFractionDigits(5);
    return kbEntity + " (" + df.format(disambiguationScore) + ")";
  }
}
