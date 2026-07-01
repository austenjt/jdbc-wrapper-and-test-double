Feature: Connection handling and error propagation
  Beyond queries and updates, JdbcWrapper is careful about lifecycle and
  failures: it rejects a null connection up front, lets real database errors
  surface instead of swallowing them, and closes the underlying connection when
  it is closed. These scenarios need only a connection, so they start from an
  empty database.

  Background: an open wrapper over an empty database
    Given an open connection to an empty database

  Scenario: An invalid query surfaces a real database error
    When I run a query against a table that does not exist
    Then a SQLException is raised

  Scenario: Creating a wrapper around a null connection is rejected immediately
    When I try to create a JdbcWrapper with a null connection
    Then an IllegalArgumentException is raised saying "Connection cannot be null"

  Scenario: Closing the wrapper closes the underlying connection
    When I close the wrapper
    Then the underlying connection is closed
