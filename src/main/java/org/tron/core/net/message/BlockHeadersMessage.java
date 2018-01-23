package org.tron.core.net.message;

import com.google.protobuf.InvalidProtocolBufferException;

public class BlockHeadersMessage extends Message {

    private org.tron.protos.core.Tron.BlockHeaders blockHeaders;

    public BlockHeadersMessage(byte[] packed) {
        super(packed);
    }

    public BlockHeadersMessage(org.tron.protos.core.Tron.BlockHeaders blockHeaders) {
        this.blockHeaders = blockHeaders;
        unpacked = true;
    }

    @Override
    public String toString() {
        return null;
    }

    @Override
    public MessageTypes getType() {
        return MessageTypes.BLOCKHEADERS;
    }

    @Override
    public byte[] getData() {
        if (data == null) pack();
        return data;
    }

    private synchronized void unPack() {
        if (unpacked) return;
        try {
            this.blockHeaders = org.tron.protos.core.Tron.BlockHeaders.parseFrom(data);
        } catch (InvalidProtocolBufferException e) {
            logger.debug(e.getMessage());
        }
        unpacked = true;
    }

    private void pack() {
        this.data = this.blockHeaders.toByteArray();
    }

}
