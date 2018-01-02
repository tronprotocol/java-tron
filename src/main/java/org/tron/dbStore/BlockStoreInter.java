package org.tron.dbStore;


import org.tron.protos.core.TronBlock;

public interface BlockStoreInter {

    TronBlock.Block getBestBlock();

    long getMaxNumber();

    TronBlock.Block getChainBlockByNumber(long blockNumber);

    boolean isBlockExist(byte[] hash);
    TronBlock.Block getBlockByHash(byte[] hash);

}
