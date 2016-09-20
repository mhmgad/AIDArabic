package mpi.aida.preparator.offsetmapping.realign;

import mpi.aida.data.PreparedInput;


public class NoopOffsetRealigner implements OffsetRealignInterface {

  @Override
  public void process(String text) {
    // do nothing.
  }

  @Override
  public PreparedInput reAlign(PreparedInput pInp) {
    return pInp;
  }

  @Override
  public int getMappedOffset(int offset) {
    return offset;
  }

}
