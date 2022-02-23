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
   * The check times before shutdown.  default is 50
   */
  private Integer check_times = 50;

  public TronLogShutdownHook() {
  }

  @Override
  public void run() {
    try {
      for (int i = 0; i < check_times; i++) {
        if (FullNode.shutDownSign) {
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
