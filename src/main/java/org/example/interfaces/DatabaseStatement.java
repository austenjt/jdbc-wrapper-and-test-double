package org.example.interfaces;

import java.sql.SQLException;

public interface DatabaseStatement extends AutoCloseable {
    void setParameter(int index, Object value) throws SQLException;
    DatabaseResultSet executeQuery() throws SQLException;
    int executeUpdate() throws SQLException;
    void close() throws SQLException;
}