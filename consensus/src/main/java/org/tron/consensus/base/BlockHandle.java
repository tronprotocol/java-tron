package org.tron.consensus.base;

import org.tron.consensus.base.Param.Miner;
import org.tron.protos.Protocol.Block;

public interface BlockHandle {

  State getState();

  Object getLock();

  Block produce(Miner miner, long timeout);

  void complete(Block block);

}