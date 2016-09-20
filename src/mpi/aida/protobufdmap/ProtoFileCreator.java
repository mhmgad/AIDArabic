package mpi.aida.protobufdmap;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.FieldDescriptor.Type;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label;
import com.google.protobuf.ProtocolStringList;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/*
  Can also be created using this piece of c code

 #include <google/protobuf/descriptor.h>
 #include <google/protobuf/descriptor.pb.h>
 #include <iostream>

 int main() {
   google::protobuf::FileDescriptorProto fileProto;
   fileProto.ParseFromFileDescriptor(0);
   google::protobuf::DescriptorPool pool;
   const google::protobuf::FileDescriptor* desc =
    pool.BuildFile(fileProto);
   std::cout << desc->DebugString() << std::endl;
   return 0;
 }
 */
public class ProtoFileCreator {
  public static final String SEMI_NEW_LINE = ";\n";
  public static final String FILE_NAME_PREFIX = "//";
  public static final String PACKAGE_PREFIX = "package";
  public static final String OPTIONS_PREFIX = "option";
  public static final String WEAK_DEPENDENCY_PREFIX = "import";
  public static final String PUBLIC_DEPENDENCY_PREFIX = "import public";
  
  public static final String ENUM_PREFIX = "enum";
  public static final String MSG_PREFIX = "message";
  public static final String EXTEND_PREFIX = "extend";
  public static final String EXTENDSIONS_PREFIX = "entensions";
  
  private int depth;
  private StringBuilder result;
  private DescriptorProtos.FileDescriptorProto fileDescriptorProto;
  public ProtoFileCreator(DescriptorProtos.FileDescriptorProto fileDescriptorProto) {
    this.fileDescriptorProto = fileDescriptorProto;
    result = new StringBuilder();
    depth = 0;
  }
  
  public void writeTo(OutputStream os) throws IOException {
    String toWrite = fileToString(fileDescriptorProto);
    os.write(toWrite.getBytes());
  }
  
  public String getAsString() {
    return fileToString(fileDescriptorProto);
  }
  
  private String fileToString(DescriptorProtos.FileDescriptorProto fileDescriptorProto) {
    String name  = fileDescriptorProto.getName();
    String packageName = fileDescriptorProto.getPackage();
    DescriptorProtos.FileOptions options = fileDescriptorProto.getOptions();
    ProtocolStringList dependencyList = fileDescriptorProto.getDependencyList();
//    List<Integer> weakDependencyList = fileDescriptorProto.getWeakDependencyList();
    List<Integer> publicDependencyList = fileDescriptorProto.getPublicDependencyList();
    List<DescriptorProtos.EnumDescriptorProto> enumTypeList = fileDescriptorProto.getEnumTypeList();
    List<DescriptorProtos.DescriptorProto> messageTypeList = fileDescriptorProto.getMessageTypeList();
//    List<DescriptorProtos.ServiceDescriptorProto> serviceList = fileDescriptorProto.getServiceList();
    List<DescriptorProtos.FieldDescriptorProto> extensionList = fileDescriptorProto.getExtensionList();
    
    boolean newline = true;
    
    if (fileDescriptorProto.hasName()) {
      appendisnl(FILE_NAME_PREFIX, " ", name);
      newline = false;
    }
    if (fileDescriptorProto.hasPackage()) {
      appendisnl(PACKAGE_PREFIX, " ", packageName);
      newline = false;
    }
    if (!newline && (newline = true)) appendnl();
    for (Map.Entry<Descriptors.FieldDescriptor, Object> option : options.getAllFields().entrySet()) {
      appendisnl(OPTIONS_PREFIX, " ", option.getKey().getName(), " = ",
        formatValue(option.getValue(), option.getKey().getType()));
      newline = false;
    }
    if (!newline && (newline = true)) appendnl();
    for (int i = 0; i < dependencyList.size(); i++) {
      if (!publicDependencyList.contains(i)) {
        appendisnl(WEAK_DEPENDENCY_PREFIX, " ", formatValue(dependencyList.get(i), Type.STRING));
        newline = false;
      }
    }
    if (!newline && (newline = true)) appendnl();
    for (Integer dependencyId : publicDependencyList) {
      appendisnl(PUBLIC_DEPENDENCY_PREFIX, " ", formatValue(dependencyList.get(dependencyId), Type.STRING));
      newline = false;
    }
    if (!newline && (newline = extensionList.size() == 0)) appendnl();
    appendExtensionListToString(extensionList);
    if (!newline && (newline = true)) appendnl();
    for (DescriptorProtos.EnumDescriptorProto enumDescriptorProto : enumTypeList) {
      appendEnumToString(enumDescriptorProto);
      newline = false;
    }
    if (!newline && (newline = true)) appendnl();
    for (DescriptorProtos.DescriptorProto descriptorProto : messageTypeList) {
      appendMsgToString(descriptorProto);
    }
    
    return result.toString();
  }
  
  private void appendEnumToString(DescriptorProtos.EnumDescriptorProto enumDescriptorProto) {
    String name = enumDescriptorProto.getName();
    DescriptorProtos.EnumOptions options = enumDescriptorProto.getOptions();
    List<DescriptorProtos.EnumValueDescriptorProto> valueList = enumDescriptorProto.getValueList();
    
    appendinl(ENUM_PREFIX, " ", name, " {");
    depth++;
    for (Map.Entry<Descriptors.FieldDescriptor, Object> option : options.getAllFields().entrySet()) {
      appendisnl(OPTIONS_PREFIX, " ", option.getKey().getName(), " = ", formatValue(option.getValue(), option.getKey().getType()));
    }
    for (DescriptorProtos.EnumValueDescriptorProto enumValueDescriptorProto : valueList) {
      appendisnl(enumValueDescriptorProto.getName(), " = ", enumValueDescriptorProto.getNumber());
    }
    depth--;
    appendinl("}");
  }

  private void appendMsgToString(DescriptorProtos.DescriptorProto descriptorProto) {
    String name = descriptorProto.getName();
    DescriptorProtos.MessageOptions options = descriptorProto.getOptions();
    List<DescriptorProtos.EnumDescriptorProto> enumTypeList = descriptorProto.getEnumTypeList();
    List<DescriptorProtos.FieldDescriptorProto> extensionList = descriptorProto.getExtensionList();
    List<DescriptorProtos.DescriptorProto.ExtensionRange> extensionRangeList = descriptorProto.getExtensionRangeList();
//    List<DescriptorProtos.OneofDescriptorProto> oneofDeclList = descriptorProto.getOneofDeclList();
    List<DescriptorProtos.FieldDescriptorProto> fieldList = descriptorProto.getFieldList();
    List<DescriptorProtos.DescriptorProto> nestedTypeList = descriptorProto.getNestedTypeList();
    
    appendinl(MSG_PREFIX, " ", name, " {");
    depth++;
    for (Map.Entry<Descriptors.FieldDescriptor, Object> option : options.getAllFields().entrySet()) {
      appendisnl(OPTIONS_PREFIX, " ", option.getKey().getName(), " = ",
        formatValue(option.getValue(), option.getKey().getType()));
    }
    for (DescriptorProtos.DescriptorProto.ExtensionRange extensionRange : extensionRangeList) {
      appendExtensionRange(extensionRange);
    }
    appendExtensionListToString(extensionList);
    for (DescriptorProtos.EnumDescriptorProto enumDescriptorProto : enumTypeList) {
      appendEnumToString(enumDescriptorProto);
    }
    for (DescriptorProtos.FieldDescriptorProto fieldDescriptorProto : fieldList) {
      appendisnl(getLabelString(fieldDescriptorProto.getLabel()), " ", 
        fieldDescriptorProto.hasTypeName() ? fieldDescriptorProto.getTypeName() : getTypeString(fieldDescriptorProto.getType()), 
        " ", fieldDescriptorProto.getName(), " = ", fieldDescriptorProto.getNumber(), buildFieldOptions(fieldDescriptorProto));
    }
    for (DescriptorProtos.DescriptorProto proto : nestedTypeList) {
      appendMsgToString(proto);
    }
    depth--;
    appendinl("}");
  }
  
  private String buildFieldOptions(DescriptorProtos.FieldDescriptorProto fieldDescriptorProto) {
    StringBuilder sb =  new StringBuilder();
    if (fieldDescriptorProto.hasDefaultValue())
      sb.append("default = ").append(formatValue(fieldDescriptorProto.getDefaultValue(), fieldDescriptorProto.getType()));
    DescriptorProtos.FieldOptions options = fieldDescriptorProto.getOptions();
    for (Map.Entry<Descriptors.FieldDescriptor, Object> option : options.getAllFields().entrySet()) {
      if (sb.length() != 0) sb.append(", ");
      sb.append(option.getKey().getName()).append(" = ").append(formatValue(option.getValue(), option.getKey().getType()));
    }
    if (sb.length() == 0) return "";
    else return " [" + sb.toString() + "]";
  }
  
  private void appendExtensionListToString(List<DescriptorProtos.FieldDescriptorProto> extensionList) {
    Multimap<String, DescriptorProtos.FieldDescriptorProto> mapping = HashMultimap.create();
    for (DescriptorProtos.FieldDescriptorProto fieldDescriptorProto : extensionList) {
      mapping.put(fieldDescriptorProto.getExtendee(), fieldDescriptorProto);
    }
    for (String extendee : mapping.keySet()) {
      appendinl(EXTEND_PREFIX, " ", extendee, " {");
      depth++;
      for (DescriptorProtos.FieldDescriptorProto fieldDescriptorProto : mapping.get(extendee)) {
        appendisnl(getLabelString(fieldDescriptorProto.getLabel()), " ", getTypeString(fieldDescriptorProto.getType()),
          " ", fieldDescriptorProto.getName(), " = ", fieldDescriptorProto.getNumber());
      }
      depth--;
      appendinl("}");
    }
  }
  
  private void appendExtensionRange(DescriptorProtos.DescriptorProto.ExtensionRange extensionRange) {
    appendisnl(EXTENDSIONS_PREFIX, " ", extensionRange.getStart(), " to ", extensionRange.getEnd());
  }

  private void append(Object... values) {
    for (Object value : values) {
      result.append(value);
    }
  }
  
  private void appendnl(Object... values) {
    append(values);
    result.append("\n");
  }
  
  private void appendsnl(Object... values) {
    append(values);
    result.append(SEMI_NEW_LINE);
  }
  
  private void appendinl(Object... values) {
    result.append(StringUtils.repeat("\t", depth));
    appendnl(values);
  }
  
  private void appendisnl(Object... values) {
    result.append(StringUtils.repeat("\t", depth));
    appendsnl(values);
  }
  
  private String formatValue(Object v, Type type) {
    switch (type) {
      case STRING:
        return '\"' + v.toString() + '\"';
      default:
        return v.toString();
    }
  }


  private String formatValue(Object v, DescriptorProtos.FieldDescriptorProto.Type type) {
    switch (type) {
      case TYPE_STRING:
        return '\"' + v.toString() + '\"';
      default:
        return v.toString();
    }
  }
  
  private String getLabelString(Label label) {
    return label.getValueDescriptor().getName().substring("LABEL_".length()).toLowerCase();
  }
  
  private String getTypeString(DescriptorProtos.FieldDescriptorProto.Type type) {
    return type.getValueDescriptor().getName().substring("TYPE_".length()).toLowerCase();
  }
}
