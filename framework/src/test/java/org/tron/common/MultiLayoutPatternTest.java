package org.tron.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.tron.common.log.layout.MultiLayoutPattern;

public class MultiLayoutPatternTest {

  private MultiLayoutPattern multiLayoutPattern;
  private LoggerContext context;

  @Before
  public void setUp() {
    context = new LoggerContext();
    multiLayoutPattern = new MultiLayoutPattern();
    multiLayoutPattern.setContext(context);

    MultiLayoutPattern.Rule rule1 = new MultiLayoutPattern.Rule();
    rule1.setLogger("com.example.app1");
    assertNotNull(rule1.getLogger());
    rule1.setPattern("%date [%thread] %-5level %logger{36} - %msg%n");
    assertNotNull(rule1.getPattern());
    rule1.setOutputPatternAsHeader(true);
    assertTrue(rule1.isOutputPatternAsHeader());
    multiLayoutPattern.addRule(rule1);

    MultiLayoutPattern.Rule rule2 = new MultiLayoutPattern.Rule();
    rule2.setLogger("com.example.app2");
    rule2.setPattern("%msg%n");
    multiLayoutPattern.addRule(rule2);

    multiLayoutPattern.start();
  }

  @Test
  public void testEncodeForSpecificLogger() {
    ILoggingEvent event1 = createLoggingEvent("com.example.app1", "Test message 1");
    byte[] encoded1 = multiLayoutPattern.encode(event1);
    String result1 = new String(encoded1);
    assertTrue(result1.contains("Test message 1"));

    ILoggingEvent event2 = createLoggingEvent("com.example.app2", "Test message 2");
    byte[] encoded2 = multiLayoutPattern.encode(event2);
    String result2 = new String(encoded2);
    assertEquals("Test message 2\n", result2);
  }

  @Test
  public void testEncodeForRootLogger() {
    ILoggingEvent event = createLoggingEvent(Logger.ROOT_LOGGER_NAME, "Root logger message");
    byte[] encoded = multiLayoutPattern.encode(event);
    String result = new String(encoded);
    assertFalse(result.contains("Root logger message"));
  }

  private ILoggingEvent createLoggingEvent(String loggerName, String message) {
    Logger logger = (Logger) LoggerFactory.getLogger(loggerName);
    return new LoggingEvent(loggerName, logger, Level.INFO, message, null, null);
  }

}
