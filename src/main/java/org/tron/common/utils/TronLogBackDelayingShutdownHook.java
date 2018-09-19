package org.tron.common.utils;

import ch.qos.logback.core.hook.DelayingShutdownHook;
import ch.qos.logback.core.util.Duration;

public class TronLogBackDelayingShutdownHook extends DelayingShutdownHook {
  public TronLogBackDelayingShutdownHook() {
    super.setDelay(Duration.buildBySeconds(2d));
  }
}
