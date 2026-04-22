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
   * a log flush. Each pool managed by ExecutorServiceManager.shutdownAndAwait-
   * Termination() can take up to 120 s in the worst case (60 s await +
   * shutdownNow + 60 s await). 180 s is therefore not a hard upper bound, but
   * a pragmatic headroom that assumes the many pools in the node shut down
   * largely in parallel; in pathological cases trailing shutdown logs may
   * still be truncated. In practice 180 s of shutdown output is also enough
   * to diagnose most stalls — if a pool is still alive past that window the
   * earlier logs already carry the stack/trace context needed to locate the
   * offender, so truncating the tail is an acceptable trade-off against
   * holding JVM exit open indefinitely.
   */
  private static final long MAX_WAIT_MS = 3 * 60 * 1000;

  private static final long CHECK_TIMES =
      MAX_WAIT_MS / CHECK_SHUTDOWN_DELAY.getMilliseconds();

  // if true, shutdown hook will be executed, for example, 'java -jar FullNode.jar -[v|h]'.
  public static volatile boolean shutDown = true;

  public TronLogShutdownHook() {
  }

  @Override
  public void run() {
    try {
      for (long i = 0; i < CHECK_TIMES; i++) {
        if (shutDown) {
          break;
        }
        if (i % 100 == 0) {
          long elapsedSeconds = i * CHECK_SHUTDOWN_DELAY.getMilliseconds() / 1000;
          addInfo("Waiting for application shutdown... elapsed=" + elapsedSeconds + "s");
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
