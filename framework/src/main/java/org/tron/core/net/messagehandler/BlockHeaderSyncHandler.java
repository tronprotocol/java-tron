package org.tron.core.net.messagehandler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tron.core.net.message.TronMessage;
import org.tron.core.net.peer.PeerConnection;

@Slf4j(topic = "net")
@Component
public class BlockHeaderSyncHandler {

  public void HandleUpdatedNotice(PeerConnection peer, TronMessage msg) {

  }

  public void handleRequest(PeerConnection peer, TronMessage msg) {

  }

  public void handleInventory(PeerConnection peer, TronMessage msg) {

  }

  public void handleSrList(PeerConnection peer, TronMessage msg) {

  }

  public void handleEpoch(PeerConnection peer, TronMessage msg) {

  }
}
