package org.example.steps;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.example.JdbcWrapper;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Step definitions for {@code jdbc_wrapper.feature}.
 *
 * <p>These bind the plain-English steps to a real in-memory H2 database (in
 * PostgreSQL compatibility mode), which acts as the test double. There are no
 * mocks: every {@code When} runs actual SQL through {@link JdbcWrapper}.
 */
public class JdbcWrapperSteps {

    private Connection connection;
    private JdbcWrapper wrapper;

    private List<String> stringResults;
    private List<Integer> intResults;
    private int rowsUpdated;
    private Exception thrown;

    @After
    void closeConnection() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    // --- Given -----------------------------------------------------------

    @Given("a database containing these users:")
    public void a_database_containing_these_users(DataTable table) throws SQLException {
        String url = "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
        connection = DriverManager.getConnection(url, "sa", "");
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR(255), age INT)");
            for (Map<String, String> row : table.asMaps()) {
                stmt.execute(String.format(
                        "INSERT INTO users (id, name, age) VALUES (%s, '%s', %s)",
                        row.get("id"), row.get("name"), row.get("age")));
            }
        }
        wrapper = new JdbcWrapper(connection);
    }

    @Given("the wrapper's connection is open")
    public void the_wrapper_s_connection_is_open() throws SQLException {
        assertFalse(connection.isClosed(), "connection should start open");
    }

    // --- When ------------------------------------------------------------

    @When("I query the names of users older than {int}")
    public void i_query_the_names_of_users_older_than(int age) throws SQLException {
        stringResults = wrapper.executeQuery(
                "SELECT name FROM users WHERE age > ? ORDER BY name",
                List.of(age),
                rs -> rs.getString("name"));
    }

    @When("I query the id, name and age of every user")
    public void i_query_the_id_name_and_age_of_every_user() throws SQLException {
        stringResults = wrapper.executeQuery(
                "SELECT id, name, age FROM users ORDER BY id",
                List.of(),
                rs -> rs.getInt("id") + ":" + rs.getString("name") + ":" + rs.getInt("age"));
    }

    @When("I rename the user with id {int} to {string}")
    public void i_rename_the_user_with_id_to(int id, String newName) throws SQLException {
        rowsUpdated = wrapper.executeUpdate(
                "UPDATE users SET name = ? WHERE id = ?",
                List.of(newName, id));
    }

    @When("I query the ids of all users passing no parameters")
    public void i_query_the_ids_of_all_users_passing_no_parameters() throws SQLException {
        intResults = wrapper.executeQuery(
                "SELECT id FROM users ORDER BY id",
                null,
                rs -> rs.getInt("id"));
    }

    @When("I run a query against a table that does not exist")
    public void i_run_a_query_against_a_table_that_does_not_exist() {
        thrown = assertThrows(Exception.class, () -> wrapper.executeQuery(
                "SELECT name FROM table_that_does_not_exist",
                List.of(),
                rs -> rs.getString("name")));
    }

    @When("I try to create a JdbcWrapper with a null connection")
    public void i_try_to_create_a_jdbc_wrapper_with_a_null_connection() {
        thrown = assertThrows(Exception.class, () -> new JdbcWrapper(null));
    }

    @When("I close the wrapper")
    public void i_close_the_wrapper() throws SQLException {
        wrapper.close();
    }

    // --- Then ------------------------------------------------------------

    @Then("the names returned are:")
    public void the_names_returned_are(DataTable table) {
        assertEquals(firstColumn(table), stringResults);
    }

    @Then("the mapped rows are:")
    public void the_mapped_rows_are(DataTable table) {
        assertEquals(firstColumn(table), stringResults);
    }

    @Then("no rows are returned")
    public void no_rows_are_returned() {
        assertTrue(stringResults.isEmpty(), "expected an empty result but got: " + stringResults);
    }

    @Then("{int} names are returned")
    public void names_are_returned(int expectedCount) {
        assertEquals(expectedCount, stringResults.size());
    }

    @Then("{int} row is reported as updated")
    public void row_is_reported_as_updated(int expected) {
        assertEquals(expected, rowsUpdated);
    }

    @Then("the name of the user with id {int} is now {string}")
    public void the_name_of_the_user_with_id_is_now(int id, String expectedName) throws SQLException {
        List<String> names = wrapper.executeQuery(
                "SELECT name FROM users WHERE id = ?",
                List.of(id),
                rs -> rs.getString("name"));
        assertEquals(List.of(expectedName), names);
    }

    @Then("the ids returned are:")
    public void the_ids_returned_are(DataTable table) {
        List<Integer> expected = new ArrayList<>();
        for (String cell : firstColumn(table)) {
            expected.add(Integer.valueOf(cell));
        }
        assertEquals(expected, intResults);
    }

    @Then("a SQLException is raised")
    public void a_sql_exception_is_raised() {
        assertNotNull(thrown, "expected an exception to have been thrown");
        assertInstanceOf(SQLException.class, thrown,
                "expected a SQLException but got " + thrown.getClass().getName());
    }

    @Then("an IllegalArgumentException is raised saying {string}")
    public void an_illegal_argument_exception_is_raised_saying(String message) {
        assertNotNull(thrown, "expected an exception to have been thrown");
        assertInstanceOf(IllegalArgumentException.class, thrown,
                "expected an IllegalArgumentException but got " + thrown.getClass().getName());
        assertEquals(message, thrown.getMessage());
    }

    @Then("the underlying connection is closed")
    public void the_underlying_connection_is_closed() throws SQLException {
        assertTrue(connection.isClosed(), "the underlying connection should be closed");
    }

    // --- helpers ---------------------------------------------------------

    /** Reads a single-column DataTable into a trimmed list of strings. */
    private static List<String> firstColumn(DataTable table) {
        List<String> values = new ArrayList<>();
        for (List<String> row : table.asLists()) {
            values.add(row.get(0).trim());
        }
        return values;
    }
}
