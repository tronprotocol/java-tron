package org.tron.common.exit;

import static org.junit.Assert.assertTrue;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.tron.core.exception.TronError;

public class ExitManagerTest {

  @Test
  public void logAndExitSkipsErrorWhoseExitWasRequested() {
    Logger exitLogger = (Logger) LoggerFactory.getLogger("Exit");
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    exitLogger.addAppender(appender);
    try {
      TronError error = new TronError("already exiting", TronError.ErrCode.DB_FLUSH);
      error.markExitRequested();

      ExitManager.logAndExit(error);

      assertTrue(error.isExitRequested());
      assertTrue(appender.list.isEmpty());
    } finally {
      exitLogger.detachAppender(appender);
    }
  }
}
