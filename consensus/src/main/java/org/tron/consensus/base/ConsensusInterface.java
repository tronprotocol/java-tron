package org.tron.consensus.base;

import org.tron.protos.Protocol.Block;

public interface ConsensusInterface {

  void start(Param param);

  void stop();

  boolean validBlock(Block block);

  boolean applyBlock(Block block);

}