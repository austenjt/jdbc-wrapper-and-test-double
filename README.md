# jdbc-wrapper-and-test-double

A small example showing an elegant way to unit test JDBC code: use a real
in-memory database (H2) as a **test double** instead of mocking the JDBC API.

## The idea

`JdbcWrapper` is a thin wrapper over a `java.sql.Connection` that exposes a
tidy `executeQuery` / `executeUpdate` API with a row-mapper lambda:

```java
try (Connection conn = DriverManager.getConnection(url, user, pass);
     JdbcWrapper wrapper = new JdbcWrapper(conn)) {

    List<String> names = wrapper.executeQuery(
            "SELECT name FROM users WHERE age > ?",
            List.of(20),
            rs -> rs.getString("name"));
}
```

## How it works

### The wrapper

`JdbcWrapper` holds a single `java.sql.Connection` and adds two methods on top
of it. Nothing more — it owns no connection pool, no driver, no configuration.

`executeQuery(sql, params, mapper)` does the full read cycle for you:

1. Opens a `PreparedStatement` for the SQL in a try-with-resources block.
2. Binds each element of `params` in order via `stmt.setObject(i + 1, value)`
   (JDBC parameters are 1-based; a `null` or empty list means no parameters).
3. Runs `executeQuery()` and walks the `ResultSet` row by row.
4. Calls your `mapper` once per row and collects the results into a `List<T>`.
5. Closes the `ResultSet` and `PreparedStatement` automatically, even on error.

`executeUpdate(sql, params)` is the write path: same prepare-and-bind step,
then `executeUpdate()`, returning the affected-row count.

The `mapper` is a small functional interface:

```java
@FunctionalInterface
public interface ResultSetMapper<T> {
    T map(ResultSet rs) throws SQLException;
}
```

Because it receives the **real** `java.sql.ResultSet`, you have the entire JDBC
surface available inside the lambda — `getInt`, `getString`, `getTimestamp`,
`getBigDecimal`, and so on — without the wrapper needing to expose or forward
any of them. You call the wrapper for the boilerplate (prepare, bind, iterate,
close) and keep full control over how each row becomes an object.

`JdbcWrapper` implements `AutoCloseable`: `close()` closes the underlying
connection if it isn't already closed, so it slots into a try-with-resources
alongside the `Connection`.

### The test double

The test never mocks any of the above. Instead it hands `JdbcWrapper` a
connection to a **real** database — H2 running in memory:

```java
String url = "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
connection = DriverManager.getConnection(url, "sa", "");
```

Each piece of that URL is doing a job:

- `mem:<random-uuid>` — an in-memory database with a unique name, so every test
  gets its own isolated schema and they can't interfere with each other.
- `MODE=PostgreSQL` — H2 emulates PostgreSQL's SQL dialect, keeping the SQL you
  test close to a production Postgres (or SQL Server) target.
- `DB_CLOSE_DELAY=-1` — keeps the in-memory database alive for the lifetime of
  the connection rather than discarding it after the first statement.

A Cucumber `Background` step opens that connection, creates a small `users`
table and seeds a few rows; an `@After` hook closes the connection, which drops
the in-memory database. From there each scenario calls the wrapper exactly as
production code would and asserts on the results. Real SQL runs, real parameters
bind, and a bad query (`SELECT ... FROM table_that_does_not_exist`) throws a
real `SQLException` — no `when(...).thenReturn(...)` staging required.

## Why a fake, not a mock

An earlier version of this project hand-wrote a parallel set of interfaces
(`DatabaseConnection`, `DatabaseStatement`, `DatabaseResultSet`) plus adapter
classes, purely so the JDBC calls could be mocked with Mockito. That was a lot
of boilerplate, and the resulting tests mostly re-asserted their own
`when(...).thenReturn(...)` wiring — they never proved any SQL was actually
correct.

Two facts make that layer unnecessary:

1. `java.sql.Connection`, `PreparedStatement`, and `ResultSet` are **already
   interfaces** — nothing extra is needed to make them substitutable.
2. The most faithful substitute for a database is **a database**. H2 runs
   in-memory, starts in milliseconds, needs no Docker, and speaks real SQL — so
   the tests exercise real parameter binding, real result sets, and real
   `SQLException`s.

So `JdbcWrapper` now wraps `java.sql.Connection` directly, and the mapper
receives the real `ResultSet` (giving full access to `getInt`, `getString`,
etc. with no adapter plumbing to extend).

## The tests are behaviour specifications (BDD with Cucumber)

Instead of example-based JUnit methods, the behaviour of `JdbcWrapper` is
described in plain English in a Gherkin feature file, which doubles as living
documentation:

```
src/test/resources/org/example/jdbc_wrapper.feature   # the spec (Given/When/Then)
src/test/java/org/example/steps/JdbcWrapperSteps.java  # the Java glue behind each step
src/test/java/org/example/RunCucumberTest.java         # JUnit Platform runner
src/test/resources/junit-platform.properties           # Cucumber config
```

A scenario reads like documentation and executes like a test:

```gherkin
Scenario: An update changes the data and reports how many rows it touched
  When I rename the user with id 1 to "Jane Doe"
  Then 1 row is reported as updated
  And the name of the user with id 1 is now "Jane Doe"
```

Every scenario runs against the real in-memory H2 fake described above. A
`Scenario Outline` covers the age-filter boundaries with a table of examples.

## Requirements

- JDK 25
- Maven

Key library versions: Cucumber **7.34.4** (`cucumber-bom`), JUnit Platform
**6.1.1** (`junit-bom`), H2 2.2.224, PostgreSQL driver 42.7.3.

## Run

```bash
mvn test          # run the behaviour scenarios against the H2 fake
mvn compile exec:java -Dexec.mainClass=org.example.Main   # or run the demo
```

## HTML report

The report is generated automatically by `mvn test` — the
`maven-cucumber-reporting` plugin is bound to the `test` phase.

- **Rich, browsable report** (feature overview, per-scenario steps, pass/fail,
  timings, tags): open
  `target/cucumber-html-reports/overview-features.html`.
- **Self-contained single file** (produced by Cucumber's built-in `html`
  plugin, handy for emailing): `target/cucumber-reports/cucumber.html`.

Because the report is driven by Gherkin, it reads as documentation of what
`JdbcWrapper` does — not just a list of green checkmarks.

## Where to go next: higher-fidelity integration tests

H2-in-PostgreSQL-mode is excellent for fast unit-level tests, but it is not
byte-for-byte identical to your production database. When you need that last
mile of fidelity (vendor-specific SQL, types, or behavior), add a second tier of
tests using [Testcontainers](https://testcontainers.com/) to run the real
engine (PostgreSQL or SQL Server) in Docker. Same `JdbcWrapper`, same tests —
only the `Connection` source changes.
