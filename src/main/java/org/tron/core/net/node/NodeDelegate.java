package org.tron.core.net.node;

import java.util.List;
import org.tron.core.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.net.message.Message;
import org.tron.core.net.message.MessageTypes;

public interface NodeDelegate {

  void handleBlock(BlockCapsule block);

  void handleTransaction(TransactionCapsule trx);

  List<BlockId> getLostBlockIds(List<BlockId> blockChainSummary);

  List<BlockId> getBlockChainSummary(BlockId beginBLockId, List<BlockId> blockIds);

  Message getData(Sha256Hash msgId, MessageTypes type);

  void syncToCli();

  void getBlockNum(byte[] hash);

  void getBlockTime(byte[] hash);

  byte[] getHeadBlockId();

  boolean contain(Sha256Hash hash, MessageTypes type);

  BlockId getGenesisBlock();

}
