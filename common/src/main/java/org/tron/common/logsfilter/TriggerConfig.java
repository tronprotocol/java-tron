package org.tron.common.logsfilter;

import lombok.Getter;
import lombok.Setter;

public class TriggerConfig {

  @Getter
  @Setter
  private String triggerName;

  @Getter
  @Setter
  private boolean enabled;

  @Getter
  @Setter
  private String topic;

  @Getter
  @Setter
  private boolean redundancy;

  @Getter
  @Setter
  private boolean ethCompatible;

  @Getter
  @Setter
  private boolean solidified;

  public TriggerConfig() {
    triggerName = "";
    enabled = false;
    topic = "";
    redundancy = false; // event will also write to log
    ethCompatible = false; // add eth compatible fields, just for transaction now
    solidified = false; // just write solidified data, just for block and transaction now
  }
}