package org.tron.core.net.node;

import org.tron.common.overlay.message.Message;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.net.message.TronMessage;
import org.tron.core.net.peer.PeerConnection;

public abstract class PeerConnectionDelegate {

  public abstract void onMessage(PeerConnection peer, TronMessage msg);

  public abstract Message getMessage(Sha256Hash msgId);

  public abstract void onConnectPeer(PeerConnection peer);

  public abstract void onDisconnectPeer(PeerConnection peer);

}
