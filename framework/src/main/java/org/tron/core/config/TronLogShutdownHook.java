package org.tron.core.config;

import ch.qos.logback.core.hook.ShutdownHookBase;
import ch.qos.logback.core.util.Duration;
import org.tron.program.FullNode;

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
   * The check times before shutdown.  default is 60000/100 = 600 times.
   */
  private final long  check_times = 60 * 1000 / CHECK_SHUTDOWN_DELAY.getMilliseconds();

  // if true, shutdown hook will be executed , for example, 'java -jar FullNode.jar -[v|h]'.
  public static volatile boolean shutDown = true;

  public TronLogShutdownHook() {
  }

  @Override
  public void run() {
    try {
      for (int i = 0; i < check_times; i++) {
        if (shutDown) {
          break;
        }
        addInfo("Sleeping for " + CHECK_SHUTDOWN_DELAY);
        Thread.sleep(CHECK_SHUTDOWN_DELAY.getMilliseconds());
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      addInfo("TronLogShutdownHook run error :" + e.getMessage());
    }
    super.stop();
  }

}
