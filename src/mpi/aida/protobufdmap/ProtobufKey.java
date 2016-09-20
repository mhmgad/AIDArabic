package mpi.aida.protobufdmap;

import com.google.protobuf.*;

/**
 * Represents a dynamically creatable key for the dMap in protobuf format.
 */
public class ProtobufKey {
  /**
   * The name of the KeyMessage in the proto file
   */
  public static final String MESSAGE_NAME = "Key";

  // holds teh message Descriptor to reuse it
  private final Descriptors.Descriptor msgDescriptor;
  
  private ProtobufKey(DescriptorProtos.DescriptorProto msgDescriptorProto) throws Descriptors.DescriptorValidationException {
    // prepare the main message
    DescriptorProtos.FileDescriptorProto fileDescP = DescriptorProtos.FileDescriptorProto.newBuilder()
      .addMessageType(msgDescriptorProto).build();
    Descriptors.FileDescriptor[] fileDescs = new Descriptors.FileDescriptor[0];
    Descriptors.FileDescriptor dynamicDescriptor = Descriptors.FileDescriptor.buildFrom(fileDescP, fileDescs);
    msgDescriptor = dynamicDescriptor.findMessageTypeByName(msgDescriptorProto.getName());
  }

  // returns the Descriptor of the 
  protected DescriptorProtos.DescriptorProto getMsgDescriptorProto() {
    return msgDescriptor.toProto();
  }

  // returns a new builder for adding seting the values of the field
  protected MsgBuilder newMsgBuilder() {
    return new MsgBuilder(msgDescriptor);
  }

  /**
   * Allows you to build one single message by setting the fields of the descriptor using there names or numbers.
   */
  protected static class MsgBuilder {
    private final DynamicMessage.Builder dmBuilder;
    private final Descriptors.Descriptor msgDescriptor;
    
    private MsgBuilder(Descriptors.Descriptor msgDescriptor) {
      this.msgDescriptor = msgDescriptor;
      dmBuilder = DynamicMessage.newBuilder(msgDescriptor);
    }

    // sets the value of a field by its number
    protected MsgBuilder setField(int number, Object value) {
      dmBuilder.setField(msgDescriptor.findFieldByNumber(number), value);
      return this;
    }

    // sets the value of a field by its name
    protected MsgBuilder setField(String name, Object value) {
      dmBuilder.setField(msgDescriptor.findFieldByName(name), value);
      return this;
    }

    // clears teh values of the message bulder
    protected MsgBuilder clear() {
      dmBuilder.clear();
      return this;
    }

    // returns the built message
    protected Message getMessage() {
      return dmBuilder.build();
    }
  }

  // returns a new descriptor builder to add fields
  protected static DescBuilder newDescBuilder() {
    return new DescBuilder();
  }

  // parses a descriptor out of a given byte array
  protected static ProtobufKey parseDescFrom(byte[] keyDescriptorBytes) throws InvalidProtocolBufferException, Descriptors.DescriptorValidationException {
    return new ProtobufKey(DescriptorProtos.DescriptorProto.parseFrom(keyDescriptorBytes));
  }

  // uses a given descriptor for the message
  protected static ProtobufKey buildDescFrom(DescriptorProtos.DescriptorProto descriptorProto) throws Descriptors.DescriptorValidationException {
    return new ProtobufKey(descriptorProto);
  }

  /**
   * Allows you to build an key Descriptor dynamically
   */
  protected static class DescBuilder {
    // build the descriptor
    private final DescriptorProtos.DescriptorProto.Builder desBuilder;

    private DescBuilder() {
      desBuilder = DescriptorProtos.DescriptorProto.newBuilder();
      desBuilder.setName(MESSAGE_NAME);
    }

    // adds a field with name, number, and type
    protected DescBuilder addField(String name, int index, DescriptorProtos.FieldDescriptorProto.Type type,
                                   DescriptorProtos.FieldDescriptorProto.Label label, String defaultValue) {
      DescriptorProtos.FieldDescriptorProto.Builder fieldBuilder =
        DescriptorProtos.FieldDescriptorProto.newBuilder()
          .setName(name).setNumber(index).setType(type);
      if (label != null) fieldBuilder.setLabel(label);
      if (defaultValue != null) fieldBuilder.setDefaultValue(defaultValue);
      desBuilder.addField(fieldBuilder.build());
      return this;
    }

    // returns a ProtobufKey using the built descriptor
    protected ProtobufKey build() throws Descriptors.DescriptorValidationException {
      return new ProtobufKey(desBuilder.build());
    }
  }
}
