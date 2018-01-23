package org.tron.core.net.node;

import org.tron.common.overlay.gossip.LocalNode;
import org.tron.core.net.message.BlockMessage;
import org.tron.core.net.message.Message;
import org.tron.core.net.message.MessageTypes;
import org.tron.core.net.message.TransationMessage;

import java.io.UnsupportedEncodingException;

public class Node {

    private NodeDelegate nodeDel;
    private LocalNode localNode;

    public void setNodeDelegate(NodeDelegate nodeDel) {
        this.nodeDel = nodeDel;
    }

    public Node() {
        localNode = LocalNode.getInstance();
    }

    public void start() {
        localNode.getGossipManager().registerSharedDataSubscriber((key, oldValue, newValue) -> {
            byte[] newValueBytes = null;
            try {
                newValueBytes = newValue.toString().getBytes("ISO-8859-1");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            recieve(key, newValueBytes);
        });
    }

    public void recieve(String key, byte[] msgStr) {
        switch (MessageTypes.valueOf(key)) {
            case BLOCK:
                handleBlock(new BlockMessage(msgStr));
                break;
            case TRX:
                handleTranscation(new TransationMessage(msgStr));
                break;
            default:
                throw new IllegalArgumentException("No such message");
        }
    }

    public void broadcast(Message msg) {
        localNode.broadcast(msg);
    }

    private void handleBlock(BlockMessage msg) {
        nodeDel.handleBlock(msg);
    }

    private void handleTranscation(TransationMessage msg) {
        nodeDel.handleTransation(msg);
    }


}
