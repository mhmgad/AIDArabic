package mpi.aida.util.htmloutput;

public class ResultMention {

  private String dataSetId = null;

  private int offset;

  private int length;

  private String mention;

  private String entity;

  private double confidence;
  

  public ResultMention(String dataSetId, int offset, int length, String mention, String entity, double confidence) {
    this.dataSetId = dataSetId;
    this.offset = offset;
    this.length = length;
    this.mention = mention;
    this.entity = entity;
    this.confidence = confidence;
  }

  public int getOffset() {
    return offset;
  }

  public int getLength() {
    return length;
  }

  public String getMention() {
    return mention;
  }

  public String getEntity() {
    return entity;
  }

  public double getConfidence() {
    return confidence;
  }
  
  public String getDataSetId() {
    return dataSetId;
  }

  public String toString() {
    return offset + "\t" + length + "\t" + mention + "\t" + entity;
  }
}
