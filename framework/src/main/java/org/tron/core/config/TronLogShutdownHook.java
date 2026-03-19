package org.tron.core.config;

import ch.qos.logback.core.hook.ShutdownHookBase;
import ch.qos.logback.core.util.Duration;

/**
 * @author kiven
 * tron log shutdown hock
 */
public class TronLogShutdownHook extends ShutdownHookBase {

  /**
   * The default shutdown delay check unit.
   */
  private static final Duration CHECK_SHUTDOWN_DELAY = Duration.buildByMilliseconds(100);

  /**
   * Maximum time to wait for a graceful application shutdown before forcing
   * log flush. The thread pool managed by ExecutorServiceManager will be
   * forcibly shut down after at most 60 seconds, so 180 s gives enough
   * headroom for all shutdown phases to complete before logs are flushed.
   */
  private static final long MAX_WAIT_MS = 3 * 60 * 1000;

  private final long checkTimes = MAX_WAIT_MS / CHECK_SHUTDOWN_DELAY.getMilliseconds();

  // if true, shutdown hook will be executed, for example, 'java -jar FullNode.jar -[v|h]'.
  public static volatile boolean shutDown = true;

  public TronLogShutdownHook() {
  }

  @Override
  public void run() {
    try {
      for (long i = 0; i < checkTimes; i++) {
        if (shutDown) {
          break;
        }
        if (i % 100 == 0) {
          addInfo("Waiting for application shutdown... elapsed=" + (i / 10) + "s");
        }
        Thread.sleep(CHECK_SHUTDOWN_DELAY.getMilliseconds());
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      addInfo("TronLogShutdownHook interrupted: " + e.getMessage());
    }
    super.stop();
  }

}
