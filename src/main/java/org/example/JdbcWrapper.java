package org.example;

import org.example.interfaces.DatabaseConnection;
import org.example.interfaces.DatabaseResultSet;
import org.example.interfaces.DatabaseStatement;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class JdbcWrapper implements AutoCloseable {

    private final DatabaseConnection connection;

    public JdbcWrapper(DatabaseConnection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("Connection cannot be null");
        }
        this.connection = connection;
    }

    public <T> List<T> executeQuery(String query, List<Object> parameters, ResultSetMapper<T> mapper) throws SQLException {
        List<T> results = new ArrayList<>();
        try (DatabaseStatement stmt = connection.prepareStatement(query)) {
            setParameters(stmt, parameters);
            try (DatabaseResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(mapper.map(rs));
                }
            }
        }
        return results;
    }

    public int executeUpdate(String query, List<Object> parameters) throws SQLException {
        try (DatabaseStatement stmt = connection.prepareStatement(query)) {
            setParameters(stmt, parameters);
            return stmt.executeUpdate();
        }
    }

    public void close() throws SQLException {
        if (!connection.isClosed()) {
            connection.close();
        }
    }

    private void setParameters(DatabaseStatement stmt, List<Object> parameters) throws SQLException {
        if (parameters != null) {
            for (int i = 0; i < parameters.size(); i++) {
                stmt.setParameter(i + 1, parameters.get(i));
            }
        }
    }

    @FunctionalInterface
    public interface ResultSetMapper<T> {
        T map(DatabaseResultSet rs) throws SQLException;
    }

}