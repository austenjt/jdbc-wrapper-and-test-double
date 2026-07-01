package org.example;

import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

/**
 * Entry point that lets Maven Surefire run the Cucumber feature files through
 * the JUnit Platform. Surefire discovers this class (its name ends in "Test"),
 * and the Cucumber engine executes every {@code .feature} found on the
 * classpath under {@code org/example}.
 *
 * <p>Cucumber options (glue package and report outputs) live in
 * {@code src/test/resources/junit-platform.properties}.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("org/example")
public class RunCucumberTest {
}
