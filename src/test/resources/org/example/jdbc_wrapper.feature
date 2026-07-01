Feature: Accessing a database through JdbcWrapper
  JdbcWrapper is a thin, safe wrapper over a JDBC Connection. It runs
  parameterised queries and updates, maps each result row to an object of your
  choosing, and cleans up its statements and result sets automatically.

  Every scenario below runs against a REAL in-memory H2 database used as a test
  "fake" (not a mock), so the SQL, the parameter binding, and the errors are all
  genuine. Read the scenarios top to bottom as living documentation of how the
  wrapper behaves.

  Background: a small users table to work against
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

  Scenario: An update changes the data and reports how many rows it touched
    When I rename the user with id 1 to "Jane Doe"
    Then 1 row is reported as updated
    And the name of the user with id 1 is now "Jane Doe"

  Scenario: Running a query with no parameters is allowed
    When I query the ids of all users passing no parameters
    Then the ids returned are:
      | 1 |
      | 2 |
      | 3 |

  Scenario: An invalid query surfaces a real database error
    When I run a query against a table that does not exist
    Then a SQLException is raised

  Scenario: Creating a wrapper around a null connection is rejected immediately
    When I try to create a JdbcWrapper with a null connection
    Then an IllegalArgumentException is raised saying "Connection cannot be null"

  Scenario: Closing the wrapper closes the underlying connection
    Given the wrapper's connection is open
    When I close the wrapper
    Then the underlying connection is closed
