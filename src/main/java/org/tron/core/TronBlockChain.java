package org.tron.core;


import org.tron.dbStore.BlockStoreInter;
import org.tron.protos.core.TronBlock;

import java.math.BigInteger;

public interface TronBlockChain {

    BlockStoreInter getBlockStoreInter();

    /**
     * @return - last added block from blockchain
     */
    TronBlock.Block getBestBlock();


    BigInteger getTotalDifficulty();
}
