package org.tron.common.log;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.util.StatusPrinter;
import java.io.File;
import org.slf4j.LoggerFactory;
import org.tron.core.exception.TronError;

public class LogService {

  public static void load(String path) {
    LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
    try {
      File file = new File(path);
      if (!file.exists() || !file.isFile() || !file.canRead()) {
        return;
      }
      JoranConfigurator configurator = new JoranConfigurator();
      configurator.setContext(lc);
      lc.reset();
      configurator.doConfigure(file);
    } catch (Exception e) {
      throw new TronError(e, TronError.ErrCode.LOG_LOAD);
    } finally {
      StatusPrinter.printInCaseOfErrorsOrWarnings(lc);
    }
  }
}
