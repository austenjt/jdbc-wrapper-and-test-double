Feature: Updating users through JdbcWrapper
  JdbcWrapper runs parameterised UPDATE/INSERT/DELETE statements and returns the
  number of rows affected. These scenarios modify a real in-memory H2 database
  and then read the data back to prove the change was actually persisted.

  Background: a small users table to modify
    Given a database containing these users:
      | id | name        | age |
      | 1  | John Doe    | 25  |
      | 2  | Alice Smith | 30  |
      | 3  | Bob Jones   | 18  |

  Scenario: An update changes the data and reports how many rows it touched
    When I rename the user with id 1 to "Jane Doe"
    Then 1 row is reported as updated
    And the name of the user with id 1 is now "Jane Doe"
