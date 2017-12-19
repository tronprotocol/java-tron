package org.tron.facade;


import org.tron.protos.core.TronBlock;

public interface TronBlockChain {

    /**
     * @return - last added block from blockchain
     */
    TronBlock.Block getBestBlock();
}
