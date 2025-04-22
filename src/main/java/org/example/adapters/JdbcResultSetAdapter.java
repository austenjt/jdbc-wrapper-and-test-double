package org.example.adapters;

import org.example.interfaces.DatabaseResultSet;

import java.sql.ResultSet;
import java.sql.SQLException;

public class JdbcResultSetAdapter implements DatabaseResultSet {

    private final ResultSet resultSet;

    public JdbcResultSetAdapter(ResultSet resultSet) {
        this.resultSet = resultSet;
    }

    @Override
    public boolean next() throws SQLException {
        return resultSet.next();
    }

    @Override
    public String getString(String columnLabel) throws SQLException {
        return resultSet.getString(columnLabel);
    }

    @Override
    public void close() throws SQLException {
        resultSet.close();
    }

}