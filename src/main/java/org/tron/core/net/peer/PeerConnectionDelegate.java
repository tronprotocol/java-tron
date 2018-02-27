package org.tron.core.net.peer;

import org.tron.common.utils.Sha256Hash;
import org.tron.core.net.message.Message;

public abstract class PeerConnectionDelegate {

  public abstract void onMessage(PeerConnection peer, Message msg);

    public abstract Message getMessage(Sha256Hash msgId);

    //public abstract gvoid onConnectionClosed(PeerConnection peer);

}
