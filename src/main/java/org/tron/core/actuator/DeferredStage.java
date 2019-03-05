package org.tron.core.actuator;

import org.tron.core.Constant;

public class DeferredStage {
  int stage = Constant.NORMALTRANSACTION;
  long delaySeconds = 0;

  DeferredStage(int stage, long delaySeconds) {
    this.stage = stage;
    this.delaySeconds = delaySeconds;
  }

}