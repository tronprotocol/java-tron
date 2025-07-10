package org.tron.common.exit;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadFactory;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.exception.TronError;

@Slf4j(topic = "Exit")
public class ExitManager {

  private static final ThreadFactory exitThreadFactory = r -> {
    Thread thread = new Thread(r, "System-Exit-Thread");
    thread.setDaemon(true);
    return thread;
  };

  private ExitManager() {

  }

  public static void initExceptionHandler() {
    Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
      findTronError(e).ifPresent(ExitManager::logAndExit);
      logger.error("Uncaught exception", e);
    });
  }

  public static Optional<TronError> findTronError(Throwable e) {
    if (e == null) {
      return Optional.empty();
    }

    Set<Throwable> seen = new HashSet<>();

    while (e != null && !seen.contains(e)) {
      if (e instanceof TronError) {
        return Optional.of((TronError) e);
      }
      seen.add(e);
      e = e.getCause();
    }
    return Optional.empty();
  }

  public static void logAndExit(TronError exit) {
    final int code = exit.getErrCode().getCode();
    logger.error("Shutting down with code: {}, reason: {}", exit.getErrCode(), exit.getMessage());
    Thread exitThread = exitThreadFactory.newThread(() -> System.exit(code));
    exitThread.start();
  }
}