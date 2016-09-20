package mpi.aida.preparator.offsetmapping.realign;

import mpi.aida.data.PreparedInput;


public interface OffsetRealignInterface {
  public void process(String text);
  public PreparedInput reAlign(PreparedInput pInp);
  public int getMappedOffset(int offset);
}
