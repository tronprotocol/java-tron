package org.tron.common.logsfilter.trigger;

import lombok.Getter;
import lombok.Setter;

public class SolidityTrigger extends Trigger {
  @Getter
  @Setter
  private long latestSolidifiedBlockNumber;

  @Override
  public String toString() {
    return new StringBuilder().append("triggerName: ").append(getTriggerName())
        .append("timestamp: ")
        .append(timeStamp)
        .append(", latestSolidifiedBlockNumber: ")
        .append(latestSolidifiedBlockNumber).toString();
  }

  public SolidityTrigger() {
    setTriggerName(Trigger.SOLIDITY_TRIGGER_NAME);
  }
}