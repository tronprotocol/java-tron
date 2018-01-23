package org.tron.core.net.message;

import com.google.protobuf.InvalidProtocolBufferException;

public class BlocksMessage  extends  Message{

    private org.tron.protos.core.Tron.Blocks blocks;

    public BlocksMessage() {
        super();
    }

    public BlocksMessage(byte[] packed) {
        super(packed);
    }

    public BlocksMessage(org.tron.protos.core.Tron.Blocks blocks) {
        this.blocks = blocks;
        unpacked = true;
    }

    @Override
    public byte[] getData() {
        return new byte[0];
    }

    @Override
    public String toString() {
        return null;
    }

    @Override
    public MessageTypes getType() {
        return MessageTypes.BLOCKS;
    }

    private synchronized void unPack() {
        if(unpacked) return;

        try {
            this.blocks = org.tron.protos.core.Tron.Blocks.parseFrom(data);
        } catch (InvalidProtocolBufferException e) {
            logger.debug(e.getMessage());
        }

        unpacked = true;
    }

    private void pack() {
        this.data = this.blocks.toByteArray();
    }
}
