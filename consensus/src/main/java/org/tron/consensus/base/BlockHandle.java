package org.tron.consensus.base;

import org.tron.consensus.base.Param.Miner;
import org.tron.core.capsule.BlockCapsule;

public interface BlockHandle {

  State getState();

  Object getLock();

  BlockCapsule produce(Miner miner, long blockTime, long timeout);

}