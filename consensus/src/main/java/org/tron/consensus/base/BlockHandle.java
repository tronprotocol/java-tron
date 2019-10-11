package org.tron.consensus.base;

import org.tron.protos.Protocol.Block;

public interface BlockHandle {

  State getState();

  Object getLock();

  Block produce(long timeout);

  void complete(Block block);

}