package org.tron.db;


import org.tron.protos.core.TronBlock;

public interface BlockStore {

    TronBlock.Block getBestBlock();

    long getMaxNumber();

    TronBlock.Block getChainBlockByNumber(long blockNumber);

    boolean isBlockExist(byte[] hash);
    TronBlock.Block getBlockByHash(byte[] hash);

}
