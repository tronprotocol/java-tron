package org.tron.core.net.service.keepalive;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tron.core.net.message.MessageTypes;
import org.tron.core.net.message.TronMessage;
import org.tron.core.net.message.keepalive.PongMessage;
import org.tron.core.net.peer.PeerConnection;

@Slf4j(topic = "net")
@Component
public class KeepAliveService {

  public void processMessage(PeerConnection peer, TronMessage message) {
    if (message.getType().equals(MessageTypes.P2P_PING)) {
      peer.sendMessage(new PongMessage());
    }
  }
}
