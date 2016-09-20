package mpi.aida.util.timing.formatter;

import java.text.ParseException;

import mpi.aida.util.timing.data.TimingInfo;


public interface TimingInfoFormatter {
  public String format(TimingInfo timingInfo);
  public TimingInfo parse(String content) throws ParseException;
}
