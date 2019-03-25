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

  public TriggerConfig() {
    triggerName = "";
    enabled = false;
    topic = "";
  }
}