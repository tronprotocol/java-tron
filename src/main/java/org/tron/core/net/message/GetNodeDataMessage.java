package org.tron.core.net.message;

import java.util.List;

public class GetNodeDataMessage extends TronMessage {

    private List<byte[]> nodeKeys;

    public GetNodeDataMessage(byte[] packed) throws Exception {
        super(packed);
        this.type = MessageTypes.GET_NODE_DATA.asByte();
    }

    public GetNodeDataMessage(List<byte[]> nodeKeys){
        this.nodeKeys = nodeKeys;
        this.type = MessageTypes.GET_NODE_DATA.asByte();
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
