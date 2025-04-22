package org.example.adapters;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JdbcResultSetAdapterTest {

    @Mock
    private ResultSet resultSet;

    private JdbcResultSetAdapter resultSetAdapter;

    @BeforeEach
    void setUp() {
        resultSetAdapter = new JdbcResultSetAdapter(resultSet);
    }

    @Test
    void testNext_DelegatesToResultSet() {
        assertDoesNotThrow(() -> {
            when(resultSet.next()).thenReturn(true);
            boolean hasNext = resultSetAdapter.next();
            assertTrue(hasNext, "next() should return true");
            verify(resultSet).next();
        });
    }

    @Test
    void testGetString_DelegatesToResultSet() {
        String columnLabel = "name";
        assertDoesNotThrow(() -> {
            when(resultSet.getString(columnLabel)).thenReturn("John Doe");
            String value = resultSetAdapter.getString(columnLabel);
            assertEquals("John Doe", value, "getString() should return the correct value");
            verify(resultSet).getString(columnLabel);
        });
    }

    @Test
    void testClose_DelegatesToResultSet() {
        assertDoesNotThrow(() -> {
            resultSetAdapter.close();
            verify(resultSet).close();
        });
    }

    @Test
    void testAutoCloseable_CloseCalledInTryWithResources() {
        assertDoesNotThrow(() -> {
            try (JdbcResultSetAdapter adapter = new JdbcResultSetAdapter(resultSet)) {
                // No operations, just testing close
            }
            verify(resultSet).close();
        });
    }

    @Test
    void testNext_SQLException_Propagates() {
        assertDoesNotThrow(() -> {
            SQLException sqlException = new SQLException("Next error");
            when(resultSet.next()).thenThrow(sqlException);

            // Act & Assert
            SQLException thrown = assertThrows(SQLException.class, () -> resultSetAdapter.next());
            assertEquals("Next error", thrown.getMessage());
            verify(resultSet).next();
        });
    }

    @Test
    void testGetString_SQLException_Propagates() {
        assertDoesNotThrow(() -> {
            String columnLabel = "name";
            SQLException sqlException = new SQLException("GetString error");
            when(resultSet.getString(columnLabel)).thenThrow(sqlException);

            // Act & Assert
            SQLException thrown = assertThrows(SQLException.class, () -> resultSetAdapter.getString(columnLabel));
            assertEquals("GetString error", thrown.getMessage());
            verify(resultSet).getString(columnLabel);
        });
    }

    @Test
    void testClose_SQLException_Propagates() {
        assertDoesNotThrow(() -> {
            SQLException sqlException = new SQLException("Close error");
            doThrow(sqlException).when(resultSet).close();

            // Act & Assert
            SQLException thrown = assertThrows(SQLException.class, () -> resultSetAdapter.close());
            assertEquals("Close error", thrown.getMessage());
            verify(resultSet).close();
        });
    }

    @Test
    void testConstructor_NullResultSet_ThrowsException() {
        NullPointerException thrown = assertThrows(NullPointerException.class, () -> new JdbcResultSetAdapter(null));
        assertEquals("ResultSet cannot be null", thrown.getMessage());
    }

}