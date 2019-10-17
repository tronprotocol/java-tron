package org.tron.consensus.base;

import org.tron.core.capsule.BlockCapsule;

public interface ConsensusInterface {

  void start(Param param);

  void stop();

  void receiveBlock(BlockCapsule block);

  boolean validBlock(BlockCapsule block);

  boolean applyBlock(BlockCapsule block);

}