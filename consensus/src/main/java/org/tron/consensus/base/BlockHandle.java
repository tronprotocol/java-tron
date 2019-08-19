
package org.tron.consensus.base;

import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.BlockHeader;

public interface BlockHandle {

  State getState();

  Object getLock();

  Block produce();

  void complete(Block block);

}