package org.tron.core.net.peer;

import org.tron.core.Sha256Hash;
import org.tron.core.net.message.Message;

public abstract class PeerConnectionDelegate {

  public abstract void onMessage(PeerConnection peer, Message msg);

    public abstract Message getMessage(Sha256Hash msgId);

    public abstract void connectPeer(PeerConnection peer);

    public abstract void disconnectPeer(PeerConnection peer);

    public abstract PeerConnection getPeer(io.scalecube.transport.Message msg);


  //public abstract gvoid onConnectionClosed(PeerConnection peer);

}
