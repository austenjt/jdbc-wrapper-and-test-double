package org.example;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * A thin, ergonomic wrapper over a JDBC {@link Connection}.
 *
 * <p>Because {@code java.sql.Connection}, {@code PreparedStatement}, and
 * {@code ResultSet} are already interfaces, there is no need for a hand-rolled
 * adapter layer to make this testable. The recommended test double is a real
 * in-memory database (H2) rather than a mock &mdash; see {@code JdbcWrapperTest}.
 */
public class JdbcWrapper implements AutoCloseable {

    private final Connection connection;

    public JdbcWrapper(Connection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("Connection cannot be null");
        }
        this.connection = connection;
    }

    public <T> List<T> executeQuery(String query, List<Object> parameters, ResultSetMapper<T> mapper) throws SQLException {
        List<T> results = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            setParameters(stmt, parameters);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(mapper.map(rs));
                }
            }
        }
        return results;
    }

    public int executeUpdate(String query, List<Object> parameters) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            setParameters(stmt, parameters);
            return stmt.executeUpdate();
        }
    }

    @Override
    public void close() throws SQLException {
        if (!connection.isClosed()) {
            connection.close();
        }
    }

    private void setParameters(PreparedStatement stmt, List<Object> parameters) throws SQLException {
        if (parameters != null) {
            for (int i = 0; i < parameters.size(); i++) {
                stmt.setObject(i + 1, parameters.get(i));
            }
        }
    }

    /** Maps a single row of a {@link ResultSet} to a domain object. */
    @FunctionalInterface
    public interface ResultSetMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

}
