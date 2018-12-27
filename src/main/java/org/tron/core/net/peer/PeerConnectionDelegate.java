package org.tron.core.net.peer;

import org.tron.common.overlay.message.Message;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.net.message.TronMessage;

public abstract class PeerConnectionDelegate {

  public abstract void onMessage(PeerConnection peer, TronMessage msg) throws Exception;

  public abstract Message getMessage(Sha256Hash msgId);

  public abstract void onConnectPeer(PeerConnection peer);

  public abstract void onDisconnectPeer(PeerConnection peer);

}
