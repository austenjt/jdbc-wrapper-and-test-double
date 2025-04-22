package org.example;

import org.example.adapters.JdbcConnectionAdapter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {

    public static void main(String[] args) {
        // JDBC URL for an in-memory H2 database
        String jdbcUrl = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1";
        String username = "sa";
        String password = "";

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             JdbcWrapper wrapper = new JdbcWrapper(new JdbcConnectionAdapter(conn))) {
            // Initialize the database: create a table and insert sample data
            initializeDatabase(conn);

            // Execute a SELECT query
            String selectQuery = "SELECT name FROM users WHERE age > ?";
            List<Object> selectParams = Arrays.asList(20);
            List<String> names = wrapper.executeQuery(selectQuery, selectParams, rs -> rs.getString("name"));

            System.out.println("Users with age > 20:");
            names.forEach(System.out::println);

            // Execute an UPDATE query
            String updateQuery = "UPDATE users SET name = ? WHERE id = ?";
            List<Object> updateParams = Arrays.asList("Jane Doe", 1);
            int rowsAffected = wrapper.executeUpdate(updateQuery, updateParams);

            System.out.println("Rows updated: " + rowsAffected);

            // Verify the update
            List<String> updatedNames = wrapper.executeQuery(selectQuery, selectParams, rs -> rs.getString("name"));
            System.out.println("Users with age > 20 after update:");
            updatedNames.forEach(System.out::println);

        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void initializeDatabase(Connection conn) throws SQLException {
        try (var stmt = conn.createStatement()) {
            // Create users table
            stmt.execute("CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR(255), age INT)");
            // Insert sample data
            stmt.execute("INSERT INTO users (id, name, age) VALUES (1, 'John Doe', 25)");
            stmt.execute("INSERT INTO users (id, name, age) VALUES (2, 'Alice Smith', 30)");
            stmt.execute("INSERT INTO users (id, name, age) VALUES (3, 'Bob Jones', 18)");
        }
    }

}