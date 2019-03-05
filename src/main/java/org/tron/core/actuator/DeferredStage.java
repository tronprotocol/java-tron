package org.tron.core.actuator;

public class DeferredStage {
  int stage;
  long delaySeconds;
  DeferredStage(int stage, long delaySeconds) {
    this.stage = stage;
    this.delaySeconds = delaySeconds;
  }
}