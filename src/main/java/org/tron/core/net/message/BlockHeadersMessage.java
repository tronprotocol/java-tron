package org.tron.core.net.message;

import com.google.protobuf.InvalidProtocolBufferException;
import org.tron.protos.Protocal.Items;
import org.tron.protos.Protocal.BlockHeader;

import java.util.ArrayList;
import java.util.List;

public class BlockHeadersMessage extends Message {

    private List<BlockHeader> blockHeaders = new ArrayList<>();

    public BlockHeadersMessage(byte[] packed) {
        super(packed);
    }

    public BlockHeadersMessage(List<BlockHeader> blockHeaders) {
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

    public List<BlockHeader> getBlockHeaders() {
        unPack();
        return blockHeaders;
    }

    private synchronized void unPack() {
        if (unpacked) return;
        try {
            Items items = Items.parseFrom(data);
            if (items.getType() == Items.ItemType.BLOCKHEADER) {
                blockHeaders = items.getBlockHeadersList();
            }
        } catch (InvalidProtocolBufferException e) {
            logger.debug(e.getMessage());
        }
        unpacked = true;
    }

    private void pack() {
        Items.Builder itemsBuilder =  Items.newBuilder();
        itemsBuilder.setType(Items.ItemType.BLOCKHEADER);
        itemsBuilder.addAllBlockHeaders(this.blockHeaders);
        this.data = itemsBuilder.build().toByteArray();
    }

}
