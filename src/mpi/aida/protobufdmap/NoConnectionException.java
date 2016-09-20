package mpi.aida.protobufdmap;

public class NoConnectionException extends ProtobufDMapException {
  public NoConnectionException(String msg) {
    super(msg);
  }

  public NoConnectionException() {
  }
}
