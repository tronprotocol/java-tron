package org.tron.core.net;

import java.util.LinkedList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.overlay.message.Message;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.db.Manager;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.core.exception.StoreException;
import org.tron.core.net.message.BlockMessage;
import org.tron.core.net.message.MessageTypes;
import org.tron.core.net.message.TransactionMessage;

@Slf4j
@Component
public class TronProxy {

  @Autowired
  private Manager dbManager;

  public long getSyncBeginNumber() {
    return dbManager.getSyncBeginNumber();
  }

  public long getBlockTime(BlockId id) {
    try {
      return dbManager.getBlockById(id).getTimeStamp();
    } catch (BadItemException e) {
      return dbManager.getGenesisBlock().getTimeStamp();
    } catch (ItemNotFoundException e) {
      return dbManager.getGenesisBlock().getTimeStamp();
    }
  }

  public BlockId getHeadBlockId() {
    return dbManager.getHeadBlockId();
  }

  public BlockId getSolidBlockId() {
    return dbManager.getSolidBlockId();
  }

  public BlockId getGenesisBlockId() {
    return dbManager.getGenesisBlockId();
  }

  public BlockId getBlockIdByNum(long num) throws Exception {return dbManager.getBlockIdByNum(num);}

  public BlockCapsule getGenesisBlock() {
    return dbManager.getGenesisBlock();
  }
  public long getHeadBlockTimeStamp() {
    return dbManager.getHeadBlockTimeStamp();
  }

  public boolean containBlock(BlockId id) {
    return dbManager.containBlock(id);
  }

  public boolean containBlockInMainChain(BlockId id) {
    return dbManager.containBlockInMainChain(id);
  }

  public LinkedList<BlockId> getBlockChainHashesOnFork(BlockId forkBlockHash) throws Exception {
    return dbManager.getBlockChainHashesOnFork(forkBlockHash);
  }

  public boolean canChainRevoke(long num) {
    return num >= dbManager.getSyncBeginNumber();
  }

  public boolean contain(Sha256Hash hash, MessageTypes type) {
    if (type.equals(MessageTypes.BLOCK)) {
      return dbManager.containBlock(hash);
    } else if (type.equals(MessageTypes.TRX)) {
      return dbManager.getTransactionStore().has(hash.getBytes());
    }
    return false;
  }

  public Message getData(Sha256Hash hash, MessageTypes type) throws StoreException {
    switch (type) {
      case BLOCK:
        return new BlockMessage(dbManager.getBlockById(hash));
      case TRX:
        TransactionCapsule tx = dbManager.getTransactionStore().get(hash.getBytes());
        if (tx != null) {
          return new TransactionMessage(tx.getData());
        }
        throw new ItemNotFoundException("transaction is not found");
      default:
        throw new BadItemException("message type not block or trx.");
    }
  }

}
