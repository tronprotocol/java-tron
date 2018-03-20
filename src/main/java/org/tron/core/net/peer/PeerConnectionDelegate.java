package org.tron.core.net.peer;

import org.tron.common.utils.Sha256Hash;
import org.tron.common.overlay.message.Message;

public abstract class PeerConnectionDelegate extends P2pNode {

  public abstract void onMessage(PeerConnection peer, Message msg);

  public abstract Message getMessage(Sha256Hash msgId);

  public abstract void connectPeer(PeerConnection peer);

  public abstract void disconnectPeer(PeerConnection peer);

  public abstract PeerConnection getPeer(io.scalecube.transport.Message msg);

  //public abstract gvoid onConnectionClosed(PeerConnection peer);

}
