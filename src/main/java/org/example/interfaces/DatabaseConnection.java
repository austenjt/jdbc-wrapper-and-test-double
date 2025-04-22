package org.example.interfaces;

import java.sql.SQLException;

public interface DatabaseConnection extends AutoCloseable {
    DatabaseStatement prepareStatement(String query) throws SQLException;
    void close() throws SQLException;
    boolean isClosed() throws SQLException;
}