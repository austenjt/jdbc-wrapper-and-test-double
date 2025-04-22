package org.example.adapters;

import org.example.interfaces.DatabaseResultSet;
import org.example.interfaces.DatabaseStatement;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class JdbcStatementAdapter implements DatabaseStatement {

    private final PreparedStatement statement;

    public JdbcStatementAdapter(PreparedStatement statement) {
        this.statement = statement;
    }

    @Override
    public void setParameter(int index, Object value) throws SQLException {
        statement.setObject(index, value);
    }

    @Override
    public DatabaseResultSet executeQuery() throws SQLException {
        return new JdbcResultSetAdapter(statement.executeQuery());
    }

    @Override
    public int executeUpdate() throws SQLException {
        return statement.executeUpdate();
    }

    @Override
    public void close() throws SQLException {
        statement.close();
    }

}