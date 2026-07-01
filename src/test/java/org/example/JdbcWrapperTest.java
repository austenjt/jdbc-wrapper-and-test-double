package org.example;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests {@link JdbcWrapper} against a real in-memory H2 database used as a
 * <em>test double</em> (a "fake"), rather than a mocked JDBC stack.
 *
 * <p>Why a fake instead of Mockito mocks: the mock-based approach forced us to
 * hand-write parallel interfaces and adapters purely so they could be stubbed,
 * and the resulting tests only re-asserted our own {@code when(...).thenReturn(...)}
 * wiring &mdash; they never proved a single line of SQL was valid. Running against
 * a real database exercises real SQL, real parameter binding, and real
 * {@link SQLException}s, with no boilerplate. H2 runs in-memory so it is fast
 * enough for the unit-test loop and needs no Docker.
 *
 * <p>H2 runs in PostgreSQL compatibility mode so the SQL stays close to a
 * production Postgres/SQL-Server target. Each test gets its own uniquely-named
 * in-memory database for full isolation.
 */
@DisplayName("JdbcWrapper (backed by an in-memory H2 fake)")
class JdbcWrapperTest {

    private Connection connection;
    private JdbcWrapper wrapper;

    @BeforeEach
    void setUp() throws SQLException {
        // Unique DB name per test => no cross-test bleed. DB_CLOSE_DELAY=-1 keeps
        // the in-memory DB alive for the lifetime of this connection.
        String url = "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
        connection = DriverManager.getConnection(url, "sa", "");
        seed(connection);
        wrapper = new JdbcWrapper(connection);
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    private static void seed(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR(255), age INT)");
            stmt.execute("INSERT INTO users (id, name, age) VALUES (1, 'John Doe', 25)");
            stmt.execute("INSERT INTO users (id, name, age) VALUES (2, 'Alice Smith', 30)");
            stmt.execute("INSERT INTO users (id, name, age) VALUES (3, 'Bob Jones', 18)");
        }
    }

    @Test
    @DisplayName("executeQuery returns rows matching the bound parameter")
    void executeQuery_returnsMatchingRows() throws SQLException {
        List<String> names = wrapper.executeQuery(
                "SELECT name FROM users WHERE age > ? ORDER BY name",
                List.of(20),
                rs -> rs.getString("name"));

        assertEquals(List.of("Alice Smith", "John Doe"), names);
    }

    @Test
    @DisplayName("executeQuery can map multiple columns of different types")
    void executeQuery_mapsMultipleColumns() throws SQLException {
        // Demonstrates the payoff of the refactor: the mapper sees the real
        // ResultSet, so getInt/getString/etc. are all available with no adapter
        // plumbing to extend.
        List<String> rows = wrapper.executeQuery(
                "SELECT id, name, age FROM users ORDER BY id",
                List.of(),
                rs -> rs.getInt("id") + ":" + rs.getString("name") + ":" + rs.getInt("age"));

        assertEquals(List.of("1:John Doe:25", "2:Alice Smith:30", "3:Bob Jones:18"), rows);
    }

    @Test
    @DisplayName("executeQuery returns an empty list when nothing matches")
    void executeQuery_noMatches_returnsEmptyList() throws SQLException {
        List<String> names = wrapper.executeQuery(
                "SELECT name FROM users WHERE age > ?",
                List.of(100),
                rs -> rs.getString("name"));

        assertTrue(names.isEmpty());
    }

    @Test
    @DisplayName("executeUpdate persists changes and reports the affected row count")
    void executeUpdate_persistsAndReturnsRowCount() throws SQLException {
        int rowsAffected = wrapper.executeUpdate(
                "UPDATE users SET name = ? WHERE id = ?",
                List.of("Jane Doe", 1));

        assertEquals(1, rowsAffected);

        // Verify the change actually hit the database, not just a mock's memory.
        List<String> name = wrapper.executeQuery(
                "SELECT name FROM users WHERE id = ?",
                List.of(1),
                rs -> rs.getString("name"));
        assertEquals(List.of("Jane Doe"), name);
    }

    @Test
    @DisplayName("null parameter list is treated as no parameters")
    void executeQuery_nullParameters_isAllowed() throws SQLException {
        List<Integer> ids = wrapper.executeQuery(
                "SELECT id FROM users ORDER BY id",
                null,
                rs -> rs.getInt("id"));

        assertEquals(List.of(1, 2, 3), ids);
    }

    @Test
    @DisplayName("a genuinely invalid query surfaces a real SQLException")
    void executeQuery_invalidSql_throwsSQLException() {
        // No mock needed to simulate a failure: the real database rejects it.
        assertThrows(SQLException.class, () -> wrapper.executeQuery(
                "SELECT name FROM table_that_does_not_exist",
                List.of(),
                rs -> rs.getString("name")));
    }

    @Test
    @DisplayName("constructor rejects a null connection")
    void constructor_nullConnection_throws() {
        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> new JdbcWrapper(null));
        assertEquals("Connection cannot be null", ex.getMessage());
    }

    @Test
    @DisplayName("close() closes the underlying connection")
    void close_closesUnderlyingConnection() throws SQLException {
        assertFalse(connection.isClosed());
        wrapper.close();
        assertTrue(connection.isClosed());
    }

    @Test
    @DisplayName("close() is idempotent when the connection is already closed")
    void close_alreadyClosed_isNoOp() throws SQLException {
        connection.close();
        assertDoesNotThrow(() -> wrapper.close());
    }
}
