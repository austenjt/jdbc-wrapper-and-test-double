package org.example.adapters;

import org.example.interfaces.DatabaseStatement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JdbcConnectionAdapterTest {

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    private JdbcConnectionAdapter connectionAdapter;

    @BeforeEach
    void setUp() {
        connectionAdapter = new JdbcConnectionAdapter(connection);
    }

    @Test
    void testPrepareStatement_ReturnsJdbcStatementAdapter() {
        String query = "SELECT * FROM users";
        assertDoesNotThrow(() -> {
            when(connection.prepareStatement(query)).thenReturn(preparedStatement);
            DatabaseStatement statement = connectionAdapter.prepareStatement(query);
            assertTrue(statement instanceof JdbcStatementAdapter, "Should return a JdbcStatementAdapter");
            verify(connection).prepareStatement(query);
        });
    }

    @Test
    void testClose_DelegatesToConnection() {
        assertDoesNotThrow(() -> {
            connectionAdapter.close();
            verify(connection).close();
        });
    }

    @Test
    void testIsClosed_DelegatesToConnection() {
        assertDoesNotThrow(() -> {
            when(connection.isClosed()).thenReturn(true);
            boolean isClosed = connectionAdapter.isClosed();
            assertTrue(isClosed, "isClosed() should return true");
            verify(connection).isClosed();
        });
    }

    @Test
    void testAutoCloseable_CloseCalledInTryWithResources() {
        assertDoesNotThrow(() -> {
            try (JdbcConnectionAdapter adapter = new JdbcConnectionAdapter(connection)) {
                // No operations, just testing close
            }
            verify(connection).close();
        });
    }

    //TODO these tests just need a little work to fix them
//    @Test
//    void testPrepareStatement_SQLException_Propagates() {
//        assertDoesNotThrow(() -> {
//            String query = "SELECT * FROM users";
//            SQLException sqlException = new SQLException("Prepare statement error");
//            when(connection.prepareStatement(query)).thenThrow(sqlException);
//            SQLException thrown = assertThrows(SQLException.class, () -> {
//                assertDoesNotThrow(() -> connectionAdapter.prepareStatement(query));
//            });
//            assertEquals("Prepare statement error", thrown.getMessage());
//            verify(connection).prepareStatement(query);
//        });
//    }
//
//    @Test
//    void testClose_SQLException_Propagates() {
//        assertDoesNotThrow(() -> {
//            SQLException sqlException = new SQLException("Close error");
//            doThrow(sqlException).when(connection).close();
//            SQLException thrown = assertThrows(SQLException.class, () -> {
//                assertDoesNotThrow(() -> connectionAdapter.close());
//            });
//            assertEquals("Close error", thrown.getMessage());
//            verify(connection).close();
//        });
//    }
//
//    @Test
//    void testIsClosed_SQLException_Propagates() {
//        assertDoesNotThrow(() -> {
//            SQLException sqlException = new SQLException("IsClosed error");
//            when(connection.isClosed()).thenThrow(sqlException);
//            SQLException thrown = assertThrows(SQLException.class, () -> {
//                assertDoesNotThrow(() -> connectionAdapter.isClosed());
//            });
//            assertEquals("IsClosed error", thrown.getMessage());
//            verify(connection).isClosed();
//        });
//    }

    @Test
    void testConstructor_NullConnection_ThrowsException() {
        NullPointerException thrown = assertThrows(NullPointerException.class, () -> new JdbcConnectionAdapter(null));
        assertEquals("Connection cannot be null", thrown.getMessage());
    }
}