package org.example;

import org.example.interfaces.DatabaseConnection;
import org.example.interfaces.DatabaseResultSet;
import org.example.interfaces.DatabaseStatement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JdbcWrapperTest {

    @Mock
    private DatabaseConnection connection;

    @Mock
    private DatabaseStatement statement;

    @Mock
    private DatabaseResultSet resultSet;

    private JdbcWrapper jdbcWrapper;

    @BeforeEach
    void setUp() {
        jdbcWrapper = new JdbcWrapper(connection);
    }

    @Test
    void testExecuteQuery_Success() throws SQLException {
        // Arrange
        String query = "SELECT * FROM users WHERE id = ?";
        List<Object> parameters = Arrays.asList(1);
        JdbcWrapper.ResultSetMapper<String> mapper = rs -> rs.getString("name");

        when(connection.prepareStatement(query)).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getString("name")).thenReturn("John Doe");

        // Act
        List<String> results = jdbcWrapper.executeQuery(query, parameters, mapper);

        // Assert
        assertEquals(1, results.size());
        assertEquals("John Doe", results.get(0));
        verify(statement).setParameter(1, 1);
        verify(statement).executeQuery();
        verify(statement).close();
        verify(resultSet).close();
    }

    @Test
    void testExecuteUpdate_Success() throws SQLException {
        // Arrange
        String query = "UPDATE users SET name = ? WHERE id = ?";
        List<Object> parameters = Arrays.asList("Jane Doe", 1);

        when(connection.prepareStatement(query)).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1);

        // Act
        int rowsAffected = jdbcWrapper.executeUpdate(query, parameters);

        // Assert
        assertEquals(1, rowsAffected);
        verify(statement).setParameter(1, "Jane Doe");
        verify(statement).setParameter(2, 1);
        verify(statement).executeUpdate();
        verify(statement).close();
    }

    @Test
    void testAutoCloseable_CloseCalledInTryWithResources() throws SQLException {
        // Arrange
        when(connection.isClosed()).thenReturn(false);

        // Act
        try (JdbcWrapper wrapper = new JdbcWrapper(connection)) {
            // No operations, just testing close
        }

        // Assert
        verify(connection).close();
    }

    @Test
    void testAutoCloseable_ConnectionAlreadyClosed_NoCloseCalled() throws SQLException {
        // Arrange
        when(connection.isClosed()).thenReturn(true);

        // Act
        try (JdbcWrapper wrapper = new JdbcWrapper(connection)) {
            // No operations
        }

        // Assert
        verify(connection, never()).close();
    }

    @Test
    void testConstructor_NullConnection_ThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new JdbcWrapper(null));
        assertEquals("Connection cannot be null", exception.getMessage());
    }

    @Test
    void testExecuteQuery_SQLException_Propagates() throws SQLException {
        // Arrange
        String query = "SELECT * FROM users WHERE id = ?";
        List<Object> parameters = Arrays.asList(1);
        JdbcWrapper.ResultSetMapper<String> mapper = rs -> rs.getString("name");

        when(connection.prepareStatement(query)).thenThrow(new SQLException("Database error"));

        // Act & Assert
        SQLException exception = assertThrows(SQLException.class, () -> jdbcWrapper.executeQuery(query, parameters, mapper));
        assertEquals("Database error", exception.getMessage());
    }

}
