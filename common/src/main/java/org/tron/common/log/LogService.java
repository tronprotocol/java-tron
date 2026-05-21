package org.tron.common.log;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.jul.LevelChangePropagator;
import ch.qos.logback.classic.spi.LoggerContextListener;
import ch.qos.logback.core.util.StatusPrinter;
import java.io.File;
import org.slf4j.LoggerFactory;
import org.tron.core.exception.TronError;

public class LogService {

  public static void load(String path) {
    LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
    try {
      // Fail fast rather than silently falling back to the classpath default —
      // that legacy behavior misled operators into thinking their custom
      // --log-config was active.
      if (path != null && !path.isEmpty()) {
        File file = new File(path);
        if (!file.exists() || !file.isFile() || !file.canRead()) {
          throw new IllegalArgumentException(
              "logback config is not a readable file: " + path);
        }
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(lc);
        lc.reset();
        configurator.doConfigure(file);
      }
      // Whether we loaded a custom config via --log-config or kept the classpath
      // default, make sure Logback level changes are propagated back to JUL so
      // gRPC loggers actually honor the levels declared in the XML. If
      // the active config already registered a LevelChangePropagator we leave
      // it alone.
      ensureLevelChangePropagator(lc);
    } catch (Exception e) {
      throw new TronError(e, TronError.ErrCode.LOG_LOAD);
    } finally {
      StatusPrinter.printInCaseOfErrorsOrWarnings(lc);
    }
  }

  private static void ensureLevelChangePropagator(LoggerContext lc) {
    for (LoggerContextListener listener : lc.getCopyOfListenerList()) {
      if (listener instanceof LevelChangePropagator) {
        return;
      }
    }
    LevelChangePropagator propagator = new LevelChangePropagator();
    propagator.setContext(lc);
    propagator.setResetJUL(true);
    propagator.start();
    lc.addListener(propagator);
  }
}
