Feature: Querying users through JdbcWrapper
  JdbcWrapper runs parameterised SELECTs and maps each result row to an object
  of your choosing. These scenarios read against a real in-memory H2 database
  used as a test "fake" (not a mock), so the SQL and parameter binding are
  genuine.

  Background: a small users table to query
    Given a database containing these users:
      | id | name        | age |
      | 1  | John Doe    | 25  |
      | 2  | Alice Smith | 30  |
      | 3  | Bob Jones   | 18  |

  Scenario: Selecting rows that match a bound parameter
    When I query the names of users older than 20
    Then the names returned are:
      | Alice Smith |
      | John Doe    |

  Scenario: Mapping several columns of different types from each row
    When I query the id, name and age of every user
    Then the mapped rows are:
      | 1:John Doe:25    |
      | 2:Alice Smith:30 |
      | 3:Bob Jones:18   |

  Scenario: A query that matches nothing returns an empty list
    When I query the names of users older than 100
    Then no rows are returned

  Scenario Outline: Filtering by age returns the expected number of people
    When I query the names of users older than <age>
    Then <count> names are returned

    Examples:
      | age | count |
      | 17  | 3     |
      | 20  | 2     |
      | 29  | 1     |
      | 30  | 0     |

  Scenario: Running a query with no parameters is allowed
    When I query the ids of all users passing no parameters
    Then the ids returned are:
      | 1 |
      | 2 |
      | 3 |
