package mpi.aida.protobufdmap;

public class WrongProtoFileFormatException extends ProtobufDMapException {
  public WrongProtoFileFormatException(String msg) {
    super(msg);
  }

  public WrongProtoFileFormatException() {
  }
}
