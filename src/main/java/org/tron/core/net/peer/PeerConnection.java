package org.tron.core.net.peer;

import org.tron.core.net.message.Message;

public class PeerConnection {

    private PeerConnectionDelegate peerDel;

    public void onMessage(PeerConnection peerConnection, Message msg) {
        peerDel.onMessage(peerConnection, msg);
    }

    public Message getMessage(byte[] item_hash) {
        return peerDel.getMessage(item_hash);
    }

    public void sendMessage(Message msg) {

    }



}
