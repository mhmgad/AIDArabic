package mpi.aida.protobufdmap;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label;
import com.google.protobuf.Descriptors;
import de.jhoff.dmap.DMapBuilder;
import mpi.aida.util.YagoUtil;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

/**
 * Class to read data from a Database and writes it into a DMap
 */
public class SqlToProtobufDMap {
  private static final Logger logger =
    LoggerFactory.getLogger(SqlToProtobufDMap.class);
  
  public static int FETCH_SIZE = 100000;
  public static int BLOCK_SIZE = 1<<23;   // 1<<23 = 8388608
  private static int VIEW_NUMBER = 0;

  public static final String DMAP_FILETYPE = "dmap";
  public static final String PROTODESC_FILETYPE = "protodesc";
  public static final String PROTO_FILETYPE = "proto";

  /**
   * Same functionality as {@link mpi.aida.protobufdmap.SqlToProtobufDMap#requestAndWriteDataFromTable(java.sql.Connection, String, java.util.Set, boolean, java.io.File, boolean, boolean, String, String, String)}
   * @param selectStatement a Select statement what will be used instead of a table.
   * @throws SQLException
   * @throws Descriptors.DescriptorValidationException
   * @throws IOException
   * @throws InvalidTypeException
   */
  public static boolean requestAndWriteDataFromSQL(Connection connection, String selectStatement, Set<String> keySet,
                                                boolean sorted, File file, boolean reuse, boolean compressValues, 
                                                String protocPath, String aidaSrcDir, String protoClassesPackage)
    throws SQLException, Descriptors.DescriptorValidationException, IOException, InvalidTypeException, InterruptedException {
    Statement statement = connection.createStatement();
    String viewName =  "temp_" + StringUtils.join(keySet.toArray(), "_") + "_" + ++VIEW_NUMBER;
    statement.execute("CREATE TEMP VIEW " + viewName + " AS " + selectStatement + ";");
    boolean result = 
      requestAndWriteDataFromTable(connection, viewName, keySet, sorted, file, reuse, compressValues, protocPath, aidaSrcDir, protoClassesPackage);
    statement.close();
    return result;
  }

  /**
   * Requests the data from a given database table, writes it into a dMap, 
   * creates the .proto schema file, and compiles it.
   * 
   * @param connection The Connection to the database
   * @param tableName the name of the table we work with
   * @param keySet a set of the keys we use for the transforamtion
   * @param sorted true if the given table is already sorted correctly (by keys)
   * @param file the dMap file (with or without the '.dmap' postfix)
   * @param reuse true if we don't want to overwrite existing DMaps
   * @param compressValues true if we want to compress the values
   * @param protocPath path to the protoc executable
   * @param aidaSrcDir path to the aida src path (the root dir for the package path)
   * @param protoClassesPackage the package of Java classes generated from the proto files
   * @return true if a new dMap was created false otherwise
   * @throws SQLException
   * @throws IOException
   * @throws Descriptors.DescriptorValidationException
   * @throws InvalidTypeException
   */
  public static boolean requestAndWriteDataFromTable(Connection connection, String tableName, Set<String> keySet,
                                                     boolean sorted, File file, boolean reuse, boolean compressValues, 
                                                     String protocPath, String aidaSrcDir, String protoClassesPackage)
    throws SQLException, IOException, Descriptors.DescriptorValidationException, InvalidTypeException, InterruptedException {
    File dMapFile = getFile(DMAP_FILETYPE, file);
    File protoFile = getFile(PROTO_FILETYPE, file);
    File directory = file.getParentFile();

    Process p = Runtime.getRuntime().exec(protocPath);
    if (p.waitFor() >= 2) {
      throw new FileNotFoundException("'protoc' executable not found");
    }
    
    if (!directory.exists() || !directory.isDirectory()) {
      logger.info("Directory '" + directory + "' does not exist. Creating...");
      directory.mkdirs();
    }
    if (dMapFile.exists() && protoFile.exists() && reuse) 
      return false;
    if (dMapFile.exists() && !dMapFile.delete())
      throw new IOException("Could not delete DMap: " + dMapFile);
    if (protoFile.exists() && !protoFile.delete())
      throw new IOException("Could not delete ProtoFile: " + protoFile);
      
    
    Set<String> keys = new HashSet<>();
    Set<String> values = new HashSet<>();
    Statement statement = connection.createStatement();

    ProtobufKey protobufKey;
    {
      ResultSet resultSetKeys = statement.executeQuery(
        "SELECT column_name, ordinal_position, udt_name, is_nullable, column_default " +
        "FROM information_schema.columns " +
        "WHERE table_name='" + tableName.toLowerCase() + "' " +
        "AND column_name IN (" + YagoUtil.getPostgresEscapedConcatenatedQuery(keySet) + ");");
      ProtobufKey.DescBuilder protobufKeyDescBuilder = ProtobufKey.newDescBuilder();
      while (resultSetKeys.next()) {
        String colName = resultSetKeys.getString(1);
        Label label = resultSetKeys.getString(4).equals("NO") ? Label.LABEL_REQUIRED : Label.LABEL_OPTIONAL;
        protobufKeyDescBuilder.addField(colName, resultSetKeys.getInt(2), 
          SQLType2ProtobufType(resultSetKeys.getString(3)), label, resultSetKeys.getString(5));
        keys.add(colName);
      }
      protobufKey = protobufKeyDescBuilder.build();
      resultSetKeys.close();
    }

    ProtobufValues protobufValues;
    {
      ResultSet resultSetValues = statement.executeQuery(
        "SELECT column_name, ordinal_position, udt_name, is_nullable, column_default " +
        "FROM information_schema.columns " +
        "WHERE table_name='" + tableName.toLowerCase() + "' " +
        "AND column_name NOT IN (" + YagoUtil.getPostgresEscapedConcatenatedQuery(keySet) + ");");
      ProtobufValues.Builder protobufValuesBuilder = ProtobufValues.newBuilder();
      while (resultSetValues.next()) {
        String colName = resultSetValues.getString(1);
        Label label = resultSetValues.getString(4).equals("NO") ? Label.LABEL_REQUIRED : Label.LABEL_OPTIONAL;
        protobufValuesBuilder.addField(colName, resultSetValues.getInt(2), 
          SQLType2ProtobufType(resultSetValues.getString(3)), label, resultSetValues.getString(5));
        values.add(colName);
      }
      protobufValues = protobufValuesBuilder.build();
      resultSetValues.close();
    }

    {
      DescriptorProtos.FileOptions fileOptions =
        DescriptorProtos.FileOptions.newBuilder().setJavaPackage(protoClassesPackage).build();
      DescriptorProtos.FileDescriptorProto fileDescriptorProto =
        DescriptorProtos.FileDescriptorProto.newBuilder()
          .setName(getFile("", file).getName() + "_message")
          .setOptions(fileOptions)
          .addMessageType(protobufKey.getMsgDescriptorProto())
          .addMessageType(protobufValues.getMsgDescriptorProto())
          .build();
      FileOutputStream fos = new FileOutputStream(protoFile);
      ProtoFileCreator pfc = new ProtoFileCreator(fileDescriptorProto);
      pfc.writeTo(fos);
      fos.flush();
      fos.close();
      
      Process process = Runtime.getRuntime().exec(
        new String[]{protocPath, "--java_out=" + aidaSrcDir,
          "--proto_path=" + protoFile.getParentFile().getAbsolutePath(), protoFile.getAbsolutePath()});
      IOUtils.copy(process.getErrorStream(), System.err);
    }
    
    long rowCountEstimated = 0;
    {
      ResultSet rowCountResultSet = statement.executeQuery(
        "SELECT reltuples::bigint AS estimate FROM pg_class where relname='" + tableName.toLowerCase() + "';");
      if (rowCountResultSet.next()) rowCountEstimated = rowCountResultSet.getLong(1);
      rowCountResultSet.close();
    }
    
    List<String> colList = new ArrayList<>();
    StringBuilder sql = new StringBuilder();
    StringBuilder keysStringBuilder = new StringBuilder();
    for (String key : keys) {
      if (keysStringBuilder.length() != 0)
        keysStringBuilder.append(", ");
      keysStringBuilder.append(key);
      colList.add(key);
    }
    String keyString = keysStringBuilder.toString();
    
    sql.append("SELECT ").append(keyString);
    for (String value : values) {
      sql.append(", ").append(value);
      colList.add(value);
    }
    sql.append(" FROM ").append(tableName.toLowerCase());
    if (!sorted)
      sql.append(" ORDER BY ").append(keyString);
    sql.append(";");

    
    connection.setAutoCommit(false);
    statement.setFetchSize(FETCH_SIZE);
    logger.info("Requesting database: " + sql.toString());
    ResultSet rs = statement.executeQuery(sql.toString());

    DMapBuilder dMapBuilder = new DMapBuilder(dMapFile, BLOCK_SIZE, compressValues);
    
    ProtobufValues.Value value = protobufValues.value();
    Object[] curKeys = new Object[keys.size()];
    logger.info("Reading Database (estimated rows: " + rowCountEstimated + ")");
    ProtobufKey.MsgBuilder keyMsgBuilder = protobufKey.newMsgBuilder();
    int count = 0;
    while (rs.next()) {
      for (int i = 0; i < curKeys.length; i++) {
        if (!rs.getObject(i + 1).equals(curKeys[i])) {
          if (protobufValues.numValues() > 0) {
            dMapBuilder.add(keyMsgBuilder.getMessage().toByteArray(), protobufValues.getMessage().toByteArray());
          }
          keyMsgBuilder.clear();
          protobufValues.clear();
          for (int j = 0; j < curKeys.length; j++) {
            curKeys[j] = rs.getObject(j + 1);
            keyMsgBuilder.setField(colList.get(j), curKeys[j]);
          }
          break;
        }
      }
      value.clear();
      for (int i = keys.size(); i < colList.size(); i++) {
        value.setField(colList.get(i), rs.getObject(i + 1));
      }
      protobufValues.addValue(value);
      if (++count % 1_000_000 == 0)
        logger.info("Read " + count/1_000_000 + " mio rows");
    }
    if (protobufValues.numValues() > 0) {
      dMapBuilder.add(keyMsgBuilder.getMessage().toByteArray(), protobufValues.getMessage().toByteArray());
    }
    statement.close();
    logger.info("Finished reading " + count + " rows.");
    dMapBuilder.build();
    return true;
  }
  
  // converts a SQL type into a protobuf type
  private static DescriptorProtos.FieldDescriptorProto.Type SQLType2ProtobufType(String sqlType) throws InvalidTypeException {
    switch (sqlType) {
      case "int2":
      case "int4":
        return Type.TYPE_INT32;
      case "int8":
        return Type.TYPE_INT64;
      case "float8":
        return Type.TYPE_DOUBLE;
      case "float4":
        return Type.TYPE_FLOAT;
      case "varchar":
      case "bpchar":
      case "text":
        return Type.TYPE_STRING;
      case "bool":
      case "bit":
        return Type.TYPE_BOOL;
      case "bytea":
        return Type.TYPE_BYTES;
      default:
        throw new InvalidTypeException(sqlType + " is an invalid type");
    }
  }
  
  public static class InvalidTypeException extends Exception {
    public InvalidTypeException() {
      super();
    }
    
    public InvalidTypeException(String message) {
      super(message);
    }
  }

  /**
   * @param fileType the type of the file e.g. 'proto' or 'dmap'
   * @param initFile the initial file.
   * @return the new file
   */
  public static File getFile(String fileType, File initFile) {
    return new File(initFile.getAbsolutePath().replaceFirst(
      "(\\." + PROTODESC_FILETYPE + "$)|" +
        "(\\." + DMAP_FILETYPE + "$)|" +
        "(\\." + PROTO_FILETYPE + ")|$",
      fileType.length() == 0 ? "" : "." + fileType));
  }
}
