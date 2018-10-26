package org.tron.core.net.messagehandler;

import org.tron.core.net.message.TronMessage;
import org.tron.core.net.peer.PeerConnection;

public interface TronMsgHandler {

  boolean processMessage(PeerConnection peer, TronMessage msg);

}
