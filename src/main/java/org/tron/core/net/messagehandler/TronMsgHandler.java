package org.tron.core.net.messagehandler;

import org.tron.core.net.message.TronMessage;
import org.tron.core.net.peer.PeerConnection;

public interface TronMsgHandler {

  void processMessage(PeerConnection peer, TronMessage msg) throws Exception;

}
