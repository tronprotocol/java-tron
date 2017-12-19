package org.tron.core;


import org.tron.db.BlockStore;
import org.tron.protos.core.TronBlock;

import java.math.BigInteger;

public interface TronBlockChain {

    BlockStore getBlockStore();

    /**
     * @return - last added block from blockchain
     */
    TronBlock.Block getBestBlock();


    BigInteger getTotalDifficulty();
}
