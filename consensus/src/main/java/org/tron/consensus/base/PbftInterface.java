package org.tron.consensus.base;

import org.tron.consensus.pbft.message.PbftBaseMessage;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.ItemNotFoundException;

public interface PbftInterface {

  boolean isSyncing();

  void forwardMessage(PbftBaseMessage message);

  BlockCapsule getBlock(long blockNum) throws Exception;

}
