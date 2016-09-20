package mpi.aida.protobufdmap;

public abstract class ProtobufDMapException extends Exception {
  public ProtobufDMapException(String msg) {
    super(msg);
  }

  public ProtobufDMapException() {
    super();
  }
}
