package org.tron.core.net.peer;

import org.tron.common.overlay.message.Message;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.net.message.TronMessage;

public abstract class PeerConnectionDelegate {

  public abstract void onMessage(PeerConnection peer, TronMessage msg);

  public abstract Message getMessage(Sha256Hash msgId);

  public abstract void connectPeer(PeerConnection peer);

  public abstract void disconnectPeer(PeerConnection peer);

  public abstract PeerConnection getPeer(io.scalecube.transport.Message msg);

  //public abstract gvoid onConnectionClosed(PeerConnection peer);

}
