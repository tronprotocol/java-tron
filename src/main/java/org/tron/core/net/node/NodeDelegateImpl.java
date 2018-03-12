package org.tron.core.net.node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.db.BlockStore;
import org.tron.core.db.DynamicPropertiesStore;
import org.tron.core.db.Manager;
import org.tron.core.exception.BadBlockException;
import org.tron.core.exception.BadTransactionException;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.UnReachBlockException;
import org.tron.core.exception.ValidateSignatureException;
import org.tron.core.net.message.BlockMessage;
import org.tron.core.net.message.Message;
import org.tron.core.net.message.MessageTypes;
import org.tron.core.net.message.TransactionMessage;


public class NodeDelegateImpl implements NodeDelegate {

  private static final Logger logger = LoggerFactory.getLogger("NodeDelegateImpl");

  private Manager dbManager;

  public NodeDelegateImpl(Manager dbManager) {
    this.dbManager = dbManager;
  }

  protected BlockStore getBlockStoreDb() {
    return dbManager.getBlockStore();
  }

  @Override
  public LinkedList<Sha256Hash> handleBlock(BlockCapsule block, boolean syncMode)
      throws BadBlockException {
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
    } //TODO:Add a unlinked exception here

    DynamicPropertiesStore dynamicPropertiesStore = dbManager.getDynamicPropertiesStore();
    dynamicPropertiesStore.saveLatestBlockHeaderNumber(block.getNum());
    //TODO: get block's TRXs here and return
    return new LinkedList<>();
  }


  @Override
  public void handleTransaction(TransactionCapsule trx) throws BadTransactionException {
    logger.info("handle transaction");
    try {
      dbManager.pushTransactions(trx);
    } catch (ContractValidateException e) {
      e.printStackTrace();
      logger.info("Contract validate failed");
      throw new BadTransactionException();
      // TODO stores failed trans in db for inquiry.
    } catch (ContractExeException e) {
      logger.info("Contract execute failed");
      e.printStackTrace();
      throw new BadTransactionException();
      // TODO stores failed trans in db for inquiry.
    } catch (ValidateSignatureException e) {
      throw new BadTransactionException();
    }

  }

  @Override

  public LinkedList<BlockId> getLostBlockIds(List<BlockId> blockChainSummary)
      throws UnReachBlockException {
    //todo: return the remain block count.
    //todo: return the blocks it should be have.
    LinkedList<BlockId> retBlockIds = new LinkedList<>();
    if (dbManager.getHeadBlockNum() == 0) {
      return retBlockIds;
    }

    BlockId unForkedBlockId = null;

    if (blockChainSummary.isEmpty() || blockChainSummary.size() == 1) {
      unForkedBlockId = dbManager.getGenesisBlockId();
    }

    if (!blockChainSummary.isEmpty()) {
      //todo: find a block we all know between the summary and my db.
      Collections.reverse(blockChainSummary);
      for (BlockId blockId : blockChainSummary) {
        if (dbManager.containBlock(blockId)) {
          unForkedBlockId = blockId;
          break;
        }
      }

      if (unForkedBlockId == null) {
        throw new UnReachBlockException();
        //todo: can not find any same block form peer's summary and my db.
      }
    }

    //todo: limit the count of block to send peer by one time.
    for (long num = unForkedBlockId.getNum();
        num <= dbManager.getHeadBlockNum(); ++num) {
      if (num > 0) {
        retBlockIds.add(dbManager.getBlockIdByNum(num));
      }
    }
    return retBlockIds;
  }

  @Override
  public Deque<BlockId> getBlockChainSummary(BlockId beginBLockId, List<BlockId> blockIds) {

    Deque<BlockId> retSummary = new LinkedList<>();
    long highBlkNum = 0;
    long highNoForkBlkNum;
    long lowBlkNum = 0; //TODO：get this from db.

    List<BlockId> forkList = new ArrayList<>();

    if (beginBLockId != Sha256Hash.ZERO_HASH) {
      //todo: get db's head num to check local db's block status.
      if (dbManager.containBlock(beginBLockId)) {
        highBlkNum = beginBLockId.getNum();
        highNoForkBlkNum = highBlkNum;
      } else {
        forkList = dbManager.getBlockChainHashesOnFork(beginBLockId);
        highNoForkBlkNum = dbManager.getBlockNumById(forkList.get(forkList.size() - 1));
        forkList.remove(forkList.get(forkList.size() - 1));
        highBlkNum = highNoForkBlkNum + forkList.size();
      }
    } else {
      highBlkNum = getBlockStoreDb().getHeadBlockNum();
      highNoForkBlkNum = highBlkNum;
      if (highBlkNum == 0) {
        return retSummary;
      }
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
            dbManager.getTransactionStore().findTransactionByHash(hash.getBytes()));
      default:
        logger.info("message type not block or trx.");
        return null;
    }
  }

  @Override
  public void syncToCli() {

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
      return false;
    }
    return false;
  }

  @Override
  public BlockCapsule getGenesisBlock() {
    //TODO return a genissBlock
    return dbManager.getGenesisBlock();
  }
}
