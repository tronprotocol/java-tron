package org.tron.common.log;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.jul.LevelChangePropagator;
import ch.qos.logback.classic.spi.LoggerContextListener;
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.core.joran.spi.JoranException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.LoggerFactory;
import org.tron.core.exception.TronError;

/**
 * Verifies that {@link LogService#load(String)} keeps the Logback<->JUL level
 * bridge working even when the active configuration does not declare a
 * {@code LevelChangePropagator} itself.
 */
public class LogServiceTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @After
  public void restoreDefaultLogbackConfig() {
    LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
    lc.reset();
    try {
      new ContextInitializer(lc).autoConfig();
    } catch (JoranException e) {
      Assert.fail("failed to restore default logback config: " + e.getMessage());
    }
  }

  @Test
  public void propagatorIsInstalledWhenCustomConfigOmitsIt() throws IOException {
    Path xml = writeLogbackXml("DEBUG", false);

    LogService.load(xml.toString());

    LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
    assertEquals(1, countLevelChangePropagators(lc));

    // LevelChangePropagator maps Logback DEBUG -> JUL FINE.
    Level julLevel = Logger.getLogger("io.grpc").getLevel();
    assertNotNull("JUL level for io.grpc should be synced from Logback", julLevel);
    assertEquals(Level.FINE, julLevel);
  }

  @Test
  public void propagatorIsNotDuplicatedWhenCustomConfigDeclaresIt() throws IOException {
    Path xml = writeLogbackXml("INFO", true);

    LogService.load(xml.toString());

    LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
    assertEquals("XML-declared propagator should not be duplicated",
        1, countLevelChangePropagators(lc));
  }

  @Test
  public void propagatorIsEnsuredWhenNoLogConfigIsSupplied() {
    LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
    // Drop whatever the default logback-test.xml registered so we can observe the
    // fall-through path (no --log-config) installing the propagator on its own.
    removeLevelChangePropagators(lc);
    assertEquals(0, countLevelChangePropagators(lc));

    // Empty path == no --log-config passed; must keep classpath default AND
    // still install the propagator so JUL sync works.
    LogService.load("");

    assertEquals("ensureLevelChangePropagator should run on the default context",
        1, countLevelChangePropagators(lc));
  }

  @Test
  public void nonEmptyInvalidPathFailsFast() {
    // A non-empty --log-config that cannot be read must surface loudly instead
    // of silently falling back to the classpath default.
    TronError thrown = assertThrows(TronError.class,
        () -> LogService.load("definitely-not-a-real-path.xml"));
    assertEquals(TronError.ErrCode.LOG_LOAD, thrown.getErrCode());
  }

  private Path writeLogbackXml(String level,
                               boolean includePropagator) throws IOException {
    StringBuilder sb = new StringBuilder();
    sb.append("<configuration>\n");
    if (includePropagator) {
      sb.append("  <contextListener "
          + "class=\"ch.qos.logback.classic.jul.LevelChangePropagator\">\n");
      sb.append("    <resetJUL>true</resetJUL>\n");
      sb.append("  </contextListener>\n");
    }
    sb.append("  <appender name=\"STDOUT\" "
        + "class=\"ch.qos.logback.core.ConsoleAppender\">\n");
    sb.append("    <encoder><pattern>%m%n</pattern></encoder>\n");
    sb.append("  </appender>\n");
    sb.append("  <root level=\"WARN\"><appender-ref ref=\"STDOUT\"/></root>\n");
    sb.append("  <logger level=\"").append(level).append("\" name=\"")
        .append("io.grpc").append("\"/>\n");
    sb.append("</configuration>\n");
    Path path = temporaryFolder.newFile("logback.xml").toPath();
    Files.write(path, sb.toString().getBytes(StandardCharsets.UTF_8));
    return path;
  }

  private static int countLevelChangePropagators(LoggerContext lc) {
    int count = 0;
    for (LoggerContextListener listener : lc.getCopyOfListenerList()) {
      if (listener instanceof LevelChangePropagator) {
        count++;
      }
    }
    return count;
  }

  private static void removeLevelChangePropagators(LoggerContext lc) {
    for (LoggerContextListener listener : lc.getCopyOfListenerList()) {
      if (listener instanceof LevelChangePropagator) {
        lc.removeListener(listener);
      }
    }
  }
}
