package org.tron.core.events;

import org.tron.overlay.Net;
import org.tron.protos.core.TronBlock;

public interface BlockchainListener {

  /**
   * New block added to blockchain
   */
  void addBlock(TronBlock.Block block);

  /**
   * New block added to blockchain
   * includes net reference
   */
  void addBlockNet(TronBlock.Block block, Net net);

  /**
   * Genesis block added to blockchain
   */
  void addGenesisBlock(TronBlock.Block block);
}
