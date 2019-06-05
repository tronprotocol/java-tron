package org.tron.common.overlay.message;

import org.springframework.stereotype.Component;

@Component
public class StaticMessages {

  public static final PingMessage PING_MESSAGE = new PingMessage();
  public static final PongMessage PONG_MESSAGE = new PongMessage();
}
