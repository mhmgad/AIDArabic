package mpi.aida.protobufdmap;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a dynamically creatable Value List for the dMap in protobuf format.
 */
public class ProtobufValues {
  public static final String MESSAGE_NAME = "Values";
  public static final String SUB_MESSAGE_NAME = "Value";
  public static final int SUB_MESSAGE_NUMBER = 1;
  public static final String FIELD_NAME = "values";

  private List<Message> values;
  private Descriptors.Descriptor subMsgDescriptor;
  private DynamicMessage.Builder subDmBuilder;
  private DescriptorProtos.DescriptorProto msgDescriptorProto;
  private Descriptors.Descriptor msgDescriptor;

  private ProtobufValues(DescriptorProtos.DescriptorProto.Builder subDesBuilder) throws Descriptors.DescriptorValidationException {
    // prepare the nested type
    DescriptorProtos.FileDescriptorProto subFileDescP = DescriptorProtos.FileDescriptorProto.newBuilder()
      .addMessageType(subDesBuilder.build()).build();
    Descriptors.FileDescriptor[] subFileDescs = new Descriptors.FileDescriptor[0];
    Descriptors.FileDescriptor subDynamicDescriptor = Descriptors.FileDescriptor.buildFrom(subFileDescP, subFileDescs);
    subMsgDescriptor = subDynamicDescriptor.findMessageTypeByName(SUB_MESSAGE_NAME);
    subDmBuilder = DynamicMessage.newBuilder(subMsgDescriptor);
    
    // assemble the main massage
    DescriptorProtos.DescriptorProto.Builder desBuilder = DescriptorProtos.DescriptorProto.newBuilder();
    desBuilder.setName(MESSAGE_NAME);
    desBuilder.addNestedType(subDesBuilder);
    desBuilder.addField(DescriptorProtos.FieldDescriptorProto.newBuilder()
      .setNumber(SUB_MESSAGE_NUMBER)
      .setType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_MESSAGE)
      .setTypeName(SUB_MESSAGE_NAME)
      .setName(FIELD_NAME)
      .setLabel(DescriptorProtos.FieldDescriptorProto.Label.LABEL_REPEATED)
      .build());

    // prepare the main message
    msgDescriptorProto = desBuilder.build();
    DescriptorProtos.FileDescriptorProto fileDescP = DescriptorProtos.FileDescriptorProto.newBuilder()
      .addMessageType(msgDescriptorProto).build();
    Descriptors.FileDescriptor[] fileDescs = new Descriptors.FileDescriptor[0];
    Descriptors.FileDescriptor dynamicDescriptor = Descriptors.FileDescriptor.buildFrom(fileDescP, fileDescs);
    msgDescriptor = dynamicDescriptor.findMessageTypeByName(MESSAGE_NAME);
    
    values = new ArrayList<>();
  }

  // adds a value
  protected ProtobufValues addValue(Value value) {
    subDmBuilder.clear();
    for (Map.Entry<Descriptors.FieldDescriptor, Object> fieldDescriptorValueEntry : value.fieldValues.entrySet()) {
      if (fieldDescriptorValueEntry.getValue() != null)
        subDmBuilder.setField(fieldDescriptorValueEntry.getKey(), fieldDescriptorValueEntry.getValue());
    }
    values.add(subDmBuilder.build());
    return this;
  }

  protected int numValues() {
    return values.size();
  }

  // builds a message out of the added values
  protected Message getMessage() throws Descriptors.DescriptorValidationException {
    DynamicMessage.Builder dmBuilder = DynamicMessage.newBuilder(msgDescriptor);
    dmBuilder.setField(msgDescriptor.findFieldByNumber(SUB_MESSAGE_NUMBER), values);
    
    return dmBuilder.build();
  }

  protected DescriptorProtos.DescriptorProto getMsgDescriptorProto() {
    return msgDescriptorProto;
  }

  protected void clear() {
    values.clear();
  }

  protected static Builder newBuilder() {
    return new Builder();
  }

  /**
   * A Builder to add fields and set there types
   */
  protected static class Builder {
    // prepare the nested type
    private DescriptorProtos.DescriptorProto.Builder subDesBuilder;
    
    private Builder() {
      subDesBuilder = DescriptorProtos.DescriptorProto.newBuilder();
      subDesBuilder.setName(SUB_MESSAGE_NAME);
    }

    protected Builder addField(String name, int index, DescriptorProtos.FieldDescriptorProto.Type type, 
                               DescriptorProtos.FieldDescriptorProto.Label label, String defaultValue) {
      DescriptorProtos.FieldDescriptorProto.Builder fieldBuilder = 
        DescriptorProtos.FieldDescriptorProto.newBuilder()
          .setName(name).setNumber(index).setType(type);
      if (label != null) fieldBuilder.setLabel(label);
      if (defaultValue != null) fieldBuilder.setDefaultValue(defaultValue);
      subDesBuilder.addField(fieldBuilder.build());
      return this;
    }

    protected ProtobufValues build() throws Descriptors.DescriptorValidationException {
      return new ProtobufValues(subDesBuilder);
    }
  }

  protected Value value() {
    return new Value();
  }

  /**
   * Represents a single Value,
   */
  protected class Value {
    private Map<Descriptors.FieldDescriptor, Object> fieldValues;
    
    private Value() {
      fieldValues = new HashMap<>();
    }

    protected Value setField(int index, Object value) {
      fieldValues.put(subMsgDescriptor.findFieldByNumber(index), value);
      return this;
    }

    protected Value setField(String name, Object value) {
      fieldValues.put(subMsgDescriptor.findFieldByName(name), value);
      return this;
    }

    protected Value clear() {
      fieldValues.clear();
      return this;
    }
  }
}
