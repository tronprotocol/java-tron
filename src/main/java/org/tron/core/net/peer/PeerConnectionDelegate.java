package org.tron.core.net.peer;

import org.tron.core.net.message.Message;

public abstract class PeerConnectionDelegate {

    public abstract void onMessage(PeerConnection peer, Message msg);

    public abstract Message getMessage(byte[] item_hash);

    public abstract void onConnectionClosed(PeerConnection peer);

}
