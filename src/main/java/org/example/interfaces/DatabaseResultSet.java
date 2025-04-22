package org.example.interfaces;

import java.sql.SQLException;

public interface DatabaseResultSet extends AutoCloseable {
    boolean next() throws SQLException;
    String getString(String columnLabel) throws SQLException;
    void close() throws SQLException;
}