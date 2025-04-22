package org.example.adapters;

import org.example.interfaces.DatabaseConnection;
import org.example.interfaces.DatabaseStatement;

import java.sql.Connection;
import java.sql.SQLException;

public class JdbcConnectionAdapter implements DatabaseConnection {

    private final Connection connection;

    public JdbcConnectionAdapter(Connection connection) {
        this.connection = connection;
    }

    @Override
    public DatabaseStatement prepareStatement(String query) throws SQLException {
        return new JdbcStatementAdapter(connection.prepareStatement(query));
    }

    @Override
    public void close() throws SQLException {
        connection.close();
    }

    @Override
    public boolean isClosed() throws SQLException {
        return connection.isClosed();
    }

}