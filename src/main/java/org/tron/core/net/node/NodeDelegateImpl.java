package org.tron.core.net.node;

import com.google.common.primitives.Longs;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.overlay.message.Message;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.Parameter.NodeConstant;
import org.tron.core.db.BlockStore;
import org.tron.core.db.Manager;
import org.tron.core.exception.BadBlockException;
import org.tron.core.exception.BadTransactionException;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.HighFreqException;
import org.tron.core.exception.UnLinkedBlockException;
import org.tron.core.exception.UnReachBlockException;
import org.tron.core.exception.ValidateSignatureException;
import org.tron.core.net.message.BlockMessage;
import org.tron.core.net.message.MessageTypes;
import org.tron.core.net.message.TransactionMessage;

@Slf4j
public class NodeDelegateImpl implements NodeDelegate {

  private Manager dbManager;

  public NodeDelegateImpl(Manager dbManager) {
    this.dbManager = dbManager;
  }

  protected BlockStore getBlockStore() {
    return dbManager.getBlockStore();
  }

  @Override
  public synchronized LinkedList<Sha256Hash> handleBlock(BlockCapsule block, boolean syncMode)
      throws BadBlockException, UnLinkedBlockException {
    // TODO timestamp shouble be consistent.
    long gap = System.currentTimeMillis() - block.getTimeStamp();
    if (gap / 1000 < -6000) {
      throw new BadBlockException("block time error");
    }
    try {
      dbManager.pushBlock(block);
    } catch (ValidateSignatureException e) {
      throw new BadBlockException("validate signature exception");
    } catch (ContractValidateException e) {
      throw new BadBlockException("ContractValidate exception");
    } catch (ContractExeException e) {
      throw new BadBlockException("Contract Exectute exception");
    }
    if (!syncMode) {
      List<TransactionCapsule> trx = dbManager.getBlockById(block.getBlockId()).getTransactions();
      return trx.stream()
          .map(TransactionCapsule::getHash)
          .collect(Collectors.toCollection(LinkedList::new));
    } else {
      return null;
    }
  }


  @Override
  public void handleTransaction(TransactionCapsule trx) throws BadTransactionException {
    logger.info("handle transaction");
    try {
      dbManager.pushTransactions(trx);
    } catch (ContractValidateException e) {
      logger.info("Contract validate failed");
      logger.debug(e.getMessage(), e);
      throw new BadTransactionException();
    } catch (ContractExeException e) {
      logger.info("Contract execute failed");
      logger.debug(e.getMessage(), e);
      throw new BadTransactionException();
    } catch (ValidateSignatureException e) {
      throw new BadTransactionException();
    } catch (HighFreqException e) {
      logger.info(e.getMessage());
    }
  }

  @Override
  public LinkedList<BlockId> getLostBlockIds(List<BlockId> blockChainSummary)
      throws UnReachBlockException {
    //todo: return the remain block count.
    //todo: return the blocks it should be have.
    if (dbManager.getHeadBlockNum() == 0) {
      return new LinkedList<>();
    }

    BlockId unForkedBlockId = null;

    if (blockChainSummary.isEmpty() || blockChainSummary.size() == 1) {
      unForkedBlockId = dbManager.getGenesisBlockId();
    } else {
      //todo: find a block we all know between the summary and my db.
      Collections.reverse(blockChainSummary);
      unForkedBlockId = blockChainSummary.stream()
          .filter(blockId -> dbManager.containBlock(blockId))
          .findFirst()
          .orElseThrow(UnReachBlockException::new);
      //todo: can not find any same block form peer's summary and my db.
    }

    //todo: limit the count of block to send peer by one time.
    long unForkedBlockIdNum = unForkedBlockId.getNum();
    long len = Longs
        .min(dbManager.getHeadBlockNum(), unForkedBlockIdNum + NodeConstant.SYNC_FETCH_BATCH_NUM);
    return LongStream.rangeClosed(unForkedBlockIdNum, len)
        .filter(num -> num > 0)
        .mapToObj(num -> dbManager.getBlockIdByNum(num))
        .collect(Collectors.toCollection(LinkedList::new));
  }

  @Override
  public Deque<BlockId> getBlockChainSummary(BlockId beginBLockId, List<BlockId> blockIds) {

    Deque<BlockId> retSummary = new LinkedList<>();
    long highBlkNum;
    long highNoForkBlkNum;
    long lowBlkNum = 0;

    LinkedList<BlockId> forkList = new LinkedList<>();

    if (!beginBLockId.equals(getGenesisBlock().getBlockId())) {
      if (dbManager.containBlock(beginBLockId)) {
        highBlkNum = beginBLockId.getNum();
        highNoForkBlkNum = highBlkNum;
      } else {
        forkList = dbManager.getBlockChainHashesOnFork(beginBLockId);
        highNoForkBlkNum = forkList.peekLast().getNum();
        forkList.pollLast();
        Collections.reverse(forkList);
        highBlkNum = highNoForkBlkNum + forkList.size();
        logger.info("highNum: " + highBlkNum);
        logger.info("forkLastNum: " + forkList.peekLast().getNum());
      }
    } else {
      highBlkNum = dbManager.getHeadBlockNum();
      highNoForkBlkNum = highBlkNum;
//      if (highBlkNum == 0) {
//        return retSummary;
//      }
    }

    long realHighBlkNum = highBlkNum + blockIds.size();
    do {
      if (lowBlkNum <= highNoForkBlkNum) {
        retSummary.offer(dbManager.getBlockIdByNum(lowBlkNum));
      } else if (lowBlkNum <= highBlkNum) {
        retSummary.offer(forkList.get((int) (lowBlkNum - highNoForkBlkNum - 1)));
      } else {
        retSummary.offer(blockIds.get((int) (lowBlkNum - highBlkNum - 1)));
      }
      lowBlkNum += (realHighBlkNum - lowBlkNum + 2) / 2;
    } while (lowBlkNum <= realHighBlkNum);
    return retSummary;
  }

  @Override
  public Message getData(Sha256Hash hash, MessageTypes type) {
    switch (type) {
      case BLOCK:
        return new BlockMessage(dbManager.findBlockByHash(hash));
      case TRX:
        return new TransactionMessage(
            dbManager.getTransactionStore().get(hash.getBytes()).getData());
      default:
        logger.info("message type not block or trx.");
        return null;
    }
  }

  @Override
  public void syncToCli(long unSyncNum) {
    logger.info("There are " + unSyncNum + " blocks we need to sync.");
    dbManager.setSyncMode(unSyncNum == 0);
    //TODO: notify cli know how many block we need to sync
  }

  @Override
  public long getBlockTime(BlockId id) {
    return dbManager.containBlock(id)
        ? dbManager.getBlockById(id).getTimeStamp()
        : dbManager.getGenesisBlock().getTimeStamp();
  }

  @Override
  public BlockId getHeadBlockId() {
    return dbManager.getHeadBlockId();
  }

  @Override
  public boolean containBlock(BlockId id) {
    return dbManager.containBlock(id);
  }

  @Override
  public boolean containBlockInMainChain(BlockId id) {
    return dbManager.containBlockInMainChain(id);
  }

  @Override
  public boolean contain(Sha256Hash hash, MessageTypes type) {
    if (type.equals(MessageTypes.BLOCK)) {
      return dbManager.containBlock(hash);
    } else if (type.equals(MessageTypes.TRX)) {
      //TODO: check it
      return dbManager.getTransactionStore().has(hash.getBytes());
    }
    return false;
  }

  @Override
  public BlockCapsule getGenesisBlock() {
    //TODO return a genesisBlock
    return dbManager.getGenesisBlock();
  }
}
