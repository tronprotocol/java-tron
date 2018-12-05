package org.tron.core.net.message;

public class NodeDataMessage extends TronMessage {

    public NodeDataMessage(byte[] encoded) {
        super(encoded);
        this.type = MessageTypes.NODE_DATA.asByte();
    }

    @Override
    public Class<?> getAnswerMessage() {
        return null;
    }

    @Override
    public String toString() {
        return "";
    }
}
