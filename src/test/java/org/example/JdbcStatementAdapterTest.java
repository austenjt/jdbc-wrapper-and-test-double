package org.example;

import org.example.adapters.JdbcResultSetAdapter;
import org.example.adapters.JdbcStatementAdapter;
import org.example.interfaces.DatabaseResultSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JdbcStatementAdapterTest {

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    private JdbcStatementAdapter statementAdapter;

    @BeforeEach
    void setUp() {
        try {
            statementAdapter = new JdbcStatementAdapter(preparedStatement);
        } catch (Exception e) {
            // do nothing
        }
    }

    @Test
    void testSetParameter_DelegatesToPreparedStatement() {
        int index = 1;
        Object value = "test";
        assertDoesNotThrow(() -> {
            statementAdapter.setParameter(index, value);
            verify(preparedStatement).setObject(index, value);
        });
    }

    @Test
    void testExecuteQuery_ReturnsJdbcResultSetAdapter() {
        assertDoesNotThrow(() -> {
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            DatabaseResultSet result = statementAdapter.executeQuery();
            assertTrue(result instanceof JdbcResultSetAdapter, "Result should be a JdbcResultSetAdapter");
            verify(preparedStatement).executeQuery();
        });
    }

    @Test
    void testExecuteUpdate_DelegatesToPreparedStatement() {
        assertDoesNotThrow(() -> {
            when(preparedStatement.executeUpdate()).thenReturn(1);
            int rowsAffected = statementAdapter.executeUpdate();
            assertEquals(1, rowsAffected);
            verify(preparedStatement).executeUpdate();
        });
    }

    @Test
    void testClose_DelegatesToPreparedStatement() {
        assertDoesNotThrow(() -> {
            statementAdapter.close();
            verify(preparedStatement).close();
        });
    }

    @Test
    void testAutoCloseable_CloseCalledInTryWithResources() {
        Assertions.assertDoesNotThrow(() -> {
            try (JdbcStatementAdapter adapter = new JdbcStatementAdapter(preparedStatement)) {
                // No operations, just testing close
            }
            verify(preparedStatement).close();
        });
    }

    @Test
    void testSetParameter_SQLException_Propagates() {
        assertDoesNotThrow(() -> {
            SQLException sqlException = new SQLException("Database error");
            doThrow(sqlException).when(preparedStatement).setObject(anyInt(), any());

            SQLException thrown = assertThrows(SQLException.class, () -> statementAdapter.setParameter(1, "test"));
            assertEquals("Database error", thrown.getMessage());
        });
    }

    @Test
    void testExecuteQuery_SQLException_Propagates() {
        assertDoesNotThrow(() -> {
            SQLException sqlException = new SQLException("Query error");
            when(preparedStatement.executeQuery()).thenThrow(sqlException);

            SQLException thrown = assertThrows(SQLException.class, () -> statementAdapter.executeQuery());
            assertEquals("Query error", thrown.getMessage());
        });
    }

    @Test
    void testExecuteUpdate_SQLException_Propagates() {
        assertDoesNotThrow(() -> {
            SQLException sqlException = new SQLException("Update error");
            when(preparedStatement.executeUpdate()).thenThrow(sqlException);

            SQLException thrown = assertThrows(SQLException.class, () -> statementAdapter.executeUpdate());
            assertEquals("Update error", thrown.getMessage());
        });
    }

    @Test
    void testClose_SQLException_Propagates() {
       assertDoesNotThrow(() -> {
           SQLException sqlException = new SQLException("Close error");
           doThrow(sqlException).when(preparedStatement).close();

           SQLException thrown = assertThrows(SQLException.class, () -> statementAdapter.close());
           assertEquals("Close error", thrown.getMessage());
       });
    }

    @Test
    void testConstructor_NullPreparedStatement_ThrowsException() {
        NullPointerException exception = assertThrows(NullPointerException.class, () -> new JdbcStatementAdapter(null));
        assertEquals("Cannot initialize JdbcStatementAdapter with a null PreparedStatement", exception.getMessage());
    }

}