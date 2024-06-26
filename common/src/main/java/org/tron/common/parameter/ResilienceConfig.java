package org.tron.common.parameter;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "net")
public class ResilienceConfig {
  @Getter
  @Setter
  private boolean enabled = false;

  @Getter
  @Setter
  private int checkInterval = 60;

  @Getter
  @Setter
  private int peerNotActiveTime = 600;

  @Getter
  @Setter
  private int blockNotChangeTime = 300;

  @Getter
  @Setter
  private int disconnectNumber = 1;

}
