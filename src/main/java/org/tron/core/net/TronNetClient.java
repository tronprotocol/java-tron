package org.tron.core.net;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.overlay.message.Message;
import org.tron.core.net.peer.PeerAdv;

@Component
public class TronNetClient {

  @Autowired
  private PeerAdv peerAdv;

  public void broadcast(Message msg) {
    peerAdv.broadcast(msg);
  }

}
