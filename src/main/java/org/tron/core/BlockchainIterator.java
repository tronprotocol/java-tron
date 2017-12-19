package org.tron.core;

import com.google.protobuf.InvalidProtocolBufferException;
import org.tron.protos.core.TronBlock;

import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Consumer;

public class BlockchainIterator implements Iterator {
    private Blockchain blockchain;
    private byte[] index;

    public BlockchainIterator(Blockchain blockchain) {
        this.blockchain = blockchain;
        index = new byte[blockchain.getCurrentHash().length];
        index = Arrays.copyOf(blockchain.getCurrentHash(), blockchain
                .getCurrentHash().length);
    }

    @Override
    public boolean hasNext() {
        return (index == null || index.length == 0);
    }

    @Override
    public Object next() {
        TronBlock.Block block = null;
        if (hasNext()) {
            byte[] value = blockchain.getBlockDB().get(index);
            try {
                block = TronBlock.Block.parseFrom(value);
                index = block.getBlockHeader().getParentHash()
                        .toByteArray();
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
            }
        }

        return block;
    }

    @Override
    public void remove() {

    }

    @Override
    public void forEachRemaining(Consumer action) {

    }
}
