package org.tron.core.net.node;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import org.tron.common.overlay.message.Message;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.exception.BadBlockException;
import org.tron.core.exception.BadTransactionException;
import org.tron.core.exception.NonCommonBlockException;
import org.tron.core.exception.StoreException;
import org.tron.core.exception.TronException;
import org.tron.core.exception.UnLinkedBlockException;
import org.tron.core.net.message.MessageTypes;

public interface NodeDelegate {

  LinkedList<Sha256Hash> handleBlock(BlockCapsule block, boolean syncMode)
      throws BadBlockException, UnLinkedBlockException, InterruptedException, NonCommonBlockException;

  boolean handleTransaction(TransactionCapsule trx) throws BadTransactionException;

  LinkedList<BlockId> getLostBlockIds(List<BlockId> blockChainSummary) throws StoreException;

  Deque<BlockId> getBlockChainSummary(BlockId beginBLockId, Deque<BlockId> blockIds)
      throws TronException;

  Message getData(Sha256Hash msgId, MessageTypes type);

  void syncToCli(long unSyncNum);

  long getBlockTime(BlockId id);

  BlockId getHeadBlockId();

  BlockId getSolidBlockId();

  boolean contain(Sha256Hash hash, MessageTypes type);

  boolean containBlock(BlockId id);

  long getHeadBlockTimeStamp();

  boolean containBlockInMainChain(BlockId id);

  BlockCapsule getGenesisBlock();

  boolean canChainRevoke(long num);

  boolean forkOrNot(TransactionCapsule transactionCapsule);
}
