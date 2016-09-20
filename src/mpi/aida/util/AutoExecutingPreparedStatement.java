package mpi.aida.util;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;

/**
 *  Wrapper around a PreparedStatement that automatically executes
 *  after a certain number of batches have been added.
 */
public class AutoExecutingPreparedStatement implements PreparedStatement {
  
  private PreparedStatement stmt_;
  
  private int count_;
  
  private int batchSize_;
  
  public AutoExecutingPreparedStatement(PreparedStatement stmt, int batchSize) {
    stmt_ = stmt;
    batchSize_ = batchSize;
  }

  @Override
  public ResultSet executeQuery(String sql) throws SQLException {
    return stmt_.executeQuery(sql);
  }

  @Override
  public int executeUpdate(String sql) throws SQLException {
    return stmt_.executeUpdate(sql);
  }

  @Override
  public void close() throws SQLException {
    stmt_.close();
  }

  @Override
  public int getMaxFieldSize() throws SQLException {
    return stmt_.getMaxFieldSize();
  }

  @Override
  public void setMaxFieldSize(int max) throws SQLException {
    stmt_.setMaxFieldSize(max);
  }

  @Override
  public int getMaxRows() throws SQLException {
    return stmt_.getMaxRows();
  }

  @Override
  public void setMaxRows(int max) throws SQLException {
    stmt_.setMaxRows(max);
  }

  @Override
  public void setEscapeProcessing(boolean enable) throws SQLException {
    stmt_.setEscapeProcessing(enable);
  }

  @Override
  public int getQueryTimeout() throws SQLException {
    return stmt_.getQueryTimeout();
  }

  @Override
  public void setQueryTimeout(int seconds) throws SQLException {
    stmt_.setQueryTimeout(seconds);    
  }

  @Override
  public void cancel() throws SQLException {
    stmt_.cancel();
  }

  @Override
  public SQLWarning getWarnings() throws SQLException {
    return stmt_.getWarnings();
  }

  @Override
  public void clearWarnings() throws SQLException {
    stmt_.clearWarnings();
  }

  @Override
  public void setCursorName(String name) throws SQLException {
    stmt_.setCursorName(name);
  }

  @Override
  public boolean execute(String sql) throws SQLException {
    return stmt_.execute(sql);
  }

  @Override
  public ResultSet getResultSet() throws SQLException {
    return stmt_.getResultSet();
  }

  @Override
  public int getUpdateCount() throws SQLException {
    return stmt_.getUpdateCount();
  }

  @Override
  public boolean getMoreResults() throws SQLException {
    return stmt_.getMoreResults();
  }

  @Override
  public void setFetchDirection(int direction) throws SQLException {
    stmt_.setFetchDirection(direction);
  }

  @Override
  public int getFetchDirection() throws SQLException {
    return stmt_.getFetchDirection();
  }

  @Override
  public void setFetchSize(int rows) throws SQLException {
    stmt_.setFetchSize(rows);    
  }

  @Override
  public int getFetchSize() throws SQLException {
    return stmt_.getFetchSize();
  }

  @Override
  public int getResultSetConcurrency() throws SQLException {
    return stmt_.getResultSetConcurrency();
  }

  @Override
  public int getResultSetType() throws SQLException {
    return stmt_.getResultSetType();
  }

  @Override
  public void addBatch(String sql) throws SQLException {
    stmt_.addBatch(sql);
    ++count_;
    if (count_ >= batchSize_) {
      stmt_.executeBatch();
      count_ = 0;
    }
  }

  @Override
  public void clearBatch() throws SQLException {
    stmt_.clearBatch();
  }

  @Override
  public int[] executeBatch() throws SQLException {    
    return stmt_.executeBatch();
  }

  @Override
  public Connection getConnection() throws SQLException {
    return stmt_.getConnection();
  }

  @Override
  public boolean getMoreResults(int current) throws SQLException {
    return stmt_.getMoreResults(current);
  }

  @Override
  public ResultSet getGeneratedKeys() throws SQLException {
     return stmt_.getGeneratedKeys();
  }

  @Override
  public int executeUpdate(String sql, int autoGeneratedKeys)
      throws SQLException {
    return stmt_.executeUpdate(sql, autoGeneratedKeys);
  }

  @Override
  public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
    return stmt_.executeUpdate(sql, columnIndexes);
  }

  @Override
  public int executeUpdate(String sql, String[] columnNames)
      throws SQLException {
    return stmt_.executeUpdate(sql, columnNames);
  }

  @Override
  public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
    return stmt_.execute(sql, autoGeneratedKeys);
  }

  @Override
  public boolean execute(String sql, int[] columnIndexes) throws SQLException {
    return stmt_.execute(sql, columnIndexes);
  }

  @Override
  public boolean execute(String sql, String[] columnNames) throws SQLException {
    return stmt_.execute(sql, columnNames);
  }

  @Override
  public int getResultSetHoldability() throws SQLException {
    return stmt_.getResultSetHoldability();
  }

  @Override
  public boolean isClosed() throws SQLException {
    return stmt_.isClosed();
  }

  @Override
  public void setPoolable(boolean poolable) throws SQLException {
    stmt_.setPoolable(poolable);
  }

  @Override
  public boolean isPoolable() throws SQLException {
    return stmt_.isPoolable();
  }

  @Override
  public void closeOnCompletion() throws SQLException {
    stmt_.closeOnCompletion();
  }

  @Override
  public boolean isCloseOnCompletion() throws SQLException {
    return stmt_.isCloseOnCompletion();
  }

  @Override
  public <T> T unwrap(Class<T> iface) throws SQLException {
    return stmt_.unwrap(iface);
  }

  @Override
  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    return stmt_.isWrapperFor(iface);
  }

  @Override
  public ResultSet executeQuery() throws SQLException {
    return stmt_.executeQuery();
  }

  @Override
  public int executeUpdate() throws SQLException {
    return stmt_.executeUpdate();
  }

  @Override
  public void setNull(int parameterIndex, int sqlType) throws SQLException {
    stmt_.setNull(parameterIndex, sqlType);
  }

  @Override
  public void setBoolean(int parameterIndex, boolean x) throws SQLException {
    stmt_.setBoolean(parameterIndex, x);    
  }

  @Override
  public void setByte(int parameterIndex, byte x) throws SQLException {
    stmt_.setByte(parameterIndex, x);
  }

  @Override
  public void setShort(int parameterIndex, short x) throws SQLException {
    stmt_.setShort(parameterIndex, x);
  }

  @Override
  public void setInt(int parameterIndex, int x) throws SQLException {
    stmt_.setInt(parameterIndex, x);
  }

  @Override
  public void setLong(int parameterIndex, long x) throws SQLException {
    stmt_.setLong(parameterIndex, x);   
  }

  @Override
  public void setFloat(int parameterIndex, float x) throws SQLException {
    stmt_.setFloat(parameterIndex, x);    
  }

  @Override
  public void setDouble(int parameterIndex, double x) throws SQLException {
    stmt_.setDouble(parameterIndex, x);
  }

  @Override
  public void setBigDecimal(int parameterIndex, BigDecimal x)
      throws SQLException {
    stmt_.setBigDecimal(parameterIndex, x);
  }

  @Override
  public void setString(int parameterIndex, String x) throws SQLException {
    stmt_.setString(parameterIndex, x);
  }

  @Override
  public void setBytes(int parameterIndex, byte[] x) throws SQLException {
    stmt_.setBytes(parameterIndex, x);
  }

  @Override
  public void setDate(int parameterIndex, Date x) throws SQLException {
    stmt_.setDate(parameterIndex, x);    
  }

  @Override
  public void setTime(int parameterIndex, Time x) throws SQLException {
    stmt_.setTime(parameterIndex, x);
  }

  @Override
  public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
    stmt_.setTimestamp(parameterIndex, x);    
  }

  @Override
  public void setAsciiStream(int parameterIndex, InputStream x, int length)
      throws SQLException {
    stmt_.setAsciiStream(parameterIndex, x, length);    
  }

  @SuppressWarnings("deprecation")
  @Override
  public void setUnicodeStream(int parameterIndex, InputStream x, int length)
      throws SQLException {
    stmt_.setUnicodeStream(parameterIndex, x, length);
  }

  @Override
  public void setBinaryStream(int parameterIndex, InputStream x, int length)
      throws SQLException {
    stmt_.setBinaryStream(parameterIndex, x, length);
  }

  @Override
  public void clearParameters() throws SQLException {
    stmt_.clearParameters();
  }

  @Override
  public void setObject(int parameterIndex, Object x, int targetSqlType)
      throws SQLException {
    stmt_.setObject(parameterIndex, x, targetSqlType);    
  }

  @Override
  public void setObject(int parameterIndex, Object x) throws SQLException {
    stmt_.setObject(parameterIndex, x);
  }

  @Override
  public boolean execute() throws SQLException {
    return stmt_.execute();
  }

  @Override
  public void addBatch() throws SQLException {
    stmt_.addBatch();
    ++count_;
    if (count_ >= batchSize_) {
      stmt_.executeBatch();
      count_ = 0;
    }    
  }

  @Override
  public void setCharacterStream(int parameterIndex, Reader reader, int length)
      throws SQLException {
    stmt_.setCharacterStream(parameterIndex, reader, length);    
  }

  @Override
  public void setRef(int parameterIndex, Ref x) throws SQLException {
    stmt_.setRef(parameterIndex, x);
  }

  @Override
  public void setBlob(int parameterIndex, Blob x) throws SQLException {
    stmt_.setBlob(parameterIndex, x);    
  }

  @Override
  public void setClob(int parameterIndex, Clob x) throws SQLException {
    stmt_.setClob(parameterIndex, x);
  }

  @Override
  public void setArray(int parameterIndex, Array x) throws SQLException {
    stmt_.setArray(parameterIndex, x);
  }

  @Override
  public ResultSetMetaData getMetaData() throws SQLException {
    return stmt_.getMetaData();
  }

  @Override
  public void setDate(int parameterIndex, Date x, Calendar cal)
      throws SQLException {
    stmt_.setDate(parameterIndex, x, cal);
  }

  @Override
  public void setTime(int parameterIndex, Time x, Calendar cal)
      throws SQLException {
    stmt_.setTime(parameterIndex, x, cal);    
  }

  @Override
  public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal)
      throws SQLException {
    stmt_.setTimestamp(parameterIndex, x, cal);    
  }

  @Override
  public void setNull(int parameterIndex, int sqlType, String typeName)
      throws SQLException {
    stmt_.setNull(parameterIndex, sqlType, typeName);    
  }

  @Override
  public void setURL(int parameterIndex, URL x) throws SQLException {
    stmt_.setURL(parameterIndex, x);    
  }

  @Override
  public ParameterMetaData getParameterMetaData() throws SQLException {
    return stmt_.getParameterMetaData();
  }

  @Override
  public void setRowId(int parameterIndex, RowId x) throws SQLException {
    stmt_.setRowId(parameterIndex, x);
  }

  @Override
  public void setNString(int parameterIndex, String value) throws SQLException {
    stmt_.setNString(parameterIndex, value);
  }

  @Override
  public void setNCharacterStream(int parameterIndex, Reader value, long length)
      throws SQLException {
    stmt_.setNCharacterStream(parameterIndex, value, length);
  }

  @Override
  public void setNClob(int parameterIndex, NClob value) throws SQLException {
    stmt_.setNClob(parameterIndex, value);    
  }

  @Override
  public void setClob(int parameterIndex, Reader reader, long length)
      throws SQLException {
    stmt_.setClob(parameterIndex, reader, length);    
  }

  @Override
  public void setBlob(int parameterIndex, InputStream inputStream, long length)
      throws SQLException {
    stmt_.setBlob(parameterIndex, inputStream, length);
  }

  @Override
  public void setNClob(int parameterIndex, Reader reader, long length)
      throws SQLException {
    stmt_.setNClob(parameterIndex, reader, length);
  }

  @Override
  public void setSQLXML(int parameterIndex, SQLXML xmlObject)
      throws SQLException {
    stmt_.setSQLXML(parameterIndex, xmlObject);    
  }

  @Override
  public void setObject(int parameterIndex, Object x, int targetSqlType,
      int scaleOrLength) throws SQLException {
    stmt_.setObject(parameterIndex, x, targetSqlType, scaleOrLength);
  }

  @Override
  public void setAsciiStream(int parameterIndex, InputStream x, long length)
      throws SQLException {
    stmt_.setAsciiStream(parameterIndex, x, length);
  }

  @Override
  public void setBinaryStream(int parameterIndex, InputStream x, long length)
      throws SQLException {
    stmt_.setBinaryStream(parameterIndex, x, length);
  }

  @Override
  public void setCharacterStream(int parameterIndex, Reader reader, long length)
      throws SQLException {
    stmt_.setCharacterStream(parameterIndex, reader, length);
  }

  @Override
  public void setAsciiStream(int parameterIndex, InputStream x)
      throws SQLException {
    stmt_.setAsciiStream(parameterIndex, x);
  }

  @Override
  public void setBinaryStream(int parameterIndex, InputStream x)
      throws SQLException {
    stmt_.setBinaryStream(parameterIndex, x);
  }

  @Override
  public void setCharacterStream(int parameterIndex, Reader reader)
      throws SQLException {
    stmt_.setCharacterStream(parameterIndex, reader);
  }

  @Override
  public void setNCharacterStream(int parameterIndex, Reader value)
      throws SQLException {
    stmt_.setNCharacterStream(parameterIndex, value);    
  }

  @Override
  public void setClob(int parameterIndex, Reader reader) throws SQLException {
    stmt_.setClob(parameterIndex, reader);
  }

  @Override
  public void setBlob(int parameterIndex, InputStream inputStream)
      throws SQLException {
    stmt_.setBlob(parameterIndex, inputStream);
  }

  @Override
  public void setNClob(int parameterIndex, Reader reader) throws SQLException {
    stmt_.setNClob(parameterIndex, reader);
  }

}
