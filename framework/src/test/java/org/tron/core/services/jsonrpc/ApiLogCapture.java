package org.tron.core.services.jsonrpc;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.LoggerFactory;

/**
 * Captures events of every level logged to the shared {@code API} logger by the thread that
 * creates it, and restores the logger when closed. Create it inside the test method: a test with
 * a timeout runs its body on a different thread than its setup.
 */
public final class ApiLogCapture implements AutoCloseable {

  private final Logger logger = (Logger) LoggerFactory.getLogger("API");
  private final Thread owner = Thread.currentThread();
  private final List<ILoggingEvent> events = new ArrayList<>();
  private final AppenderBase<ILoggingEvent> appender = new AppenderBase<ILoggingEvent>() {
    @Override
    protected void append(ILoggingEvent event) {
      if (Thread.currentThread() == owner) {
        events.add(event);
      }
    }
  };
  private final Level originalLevel;

  public ApiLogCapture() {
    originalLevel = logger.getLevel();
    logger.setLevel(Level.TRACE);
    appender.start();
    logger.addAppender(appender);
  }

  public List<ILoggingEvent> events() {
    return new ArrayList<>(events);
  }

  @Override
  public void close() {
    logger.setLevel(originalLevel);
    logger.detachAppender(appender);
    appender.stop();
  }
}
