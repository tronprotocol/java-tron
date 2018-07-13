package org.tron.core.net.node;

import static org.tron.core.config.Parameter.ChainConstant.BLOCK_PRODUCED_INTERVAL;
import static org.tron.core.config.Parameter.ChainConstant.BLOCK_SIZE;

import com.google.common.primitives.Longs;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.overlay.message.Message;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.Parameter.NodeConstant;
import org.tron.core.db.Manager;
import org.tron.core.exception.AccountResourceInsufficientException;
import org.tron.core.exception.BadBlockException;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.BadNumberBlockException;
import org.tron.core.exception.BadTransactionException;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractSizeNotEqualToOneException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.DupTransactionException;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.core.exception.NonCommonBlockException;
import org.tron.core.exception.StoreException;
import org.tron.core.exception.TaposException;
import org.tron.core.exception.TooBigTransactionException;
import org.tron.core.exception.TransactionExpirationException;
import org.tron.core.exception.TronException;
import org.tron.core.exception.UnLinkedBlockException;
import org.tron.core.exception.ValidateScheduleException;
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

  @Override
  public synchronized LinkedList<Sha256Hash> handleBlock(BlockCapsule block, boolean syncMode)
      throws BadBlockException, UnLinkedBlockException, InterruptedException, NonCommonBlockException {

    // syncMode 是否同步模式 True：采用同步模式处理收到的区块 False：采用广播模式处理收到的区块，此时返回交易信息
    if (block.getInstance().getSerializedSize() > BLOCK_SIZE + 100) {
      throw new BadBlockException("block size over limit");
    }

    // TODO timestamp should be consistent.
    long gap = block.getTimeStamp() - System.currentTimeMillis();
    if (gap >= BLOCK_PRODUCED_INTERVAL) {
      throw new BadBlockException("block time error");
    }
    try {
      dbManager.preValidateTransactionSign(block);
      dbManager.pushBlock(block);
      if (!syncMode) {  //广播模式
        List<TransactionCapsule> trx = null;
        trx = block.getTransactions();
        return trx.stream()
            .map(TransactionCapsule::getTransactionId)
            .collect(Collectors.toCollection(LinkedList::new));
      } else {
        return null;
      }

    } catch (AccountResourceInsufficientException e) {
      throw new BadBlockException("AccountResourceInsufficientException," + e.getMessage());
    } catch (ValidateScheduleException e) {
      throw new BadBlockException("validate schedule exception," + e.getMessage());
    } catch (ValidateSignatureException e) {
      throw new BadBlockException("validate signature exception," + e.getMessage());
    } catch (ContractValidateException e) {
      throw new BadBlockException("ContractValidate exception," + e.getMessage());
    } catch (ContractExeException e) {
      throw new BadBlockException("Contract Execute exception," + e.getMessage());
    } catch (TaposException e) {
      throw new BadBlockException("tapos exception," + e.getMessage());
    } catch (DupTransactionException e) {
      throw new BadBlockException("DupTransaction exception," + e.getMessage());
    } catch (TooBigTransactionException e) {
      throw new BadBlockException("TooBigTransaction exception," + e.getMessage());
    } catch (TransactionExpirationException e) {
      throw new BadBlockException("Expiration exception," + e.getMessage());
    } catch (BadNumberBlockException e) {
      throw new BadBlockException("bad number exception," + e.getMessage());
    }

  }


  @Override
  public boolean handleTransaction(TransactionCapsule trx) throws BadTransactionException {
    logger.debug("handle transaction");
    if (dbManager.getTransactionIdCache().getIfPresent(trx.getTransactionId()) != null) {
      logger.warn("This transaction has been processed");
      return false;
    } else {
      dbManager.getTransactionIdCache().put(trx.getTransactionId(), true);
    }
    try {
      dbManager.pushTransactions(trx);
    } catch (ContractSizeNotEqualToOneException e){
      logger.info("Contract validate failed" + e.getMessage());
      throw new BadTransactionException();
    } catch (ContractValidateException e) {
      logger.info("Contract validate failed" + e.getMessage());
      //throw new BadTransactionException();
      return false;
    } catch (ContractExeException e) {
      logger.info("Contract execute failed" + e.getMessage());
      //throw new BadTransactionException();
      return false;
    } catch (ValidateSignatureException e) {
      logger.info("ValidateSignatureException" + e.getMessage());
      throw new BadTransactionException();
    } catch (AccountResourceInsufficientException e) {
      logger.info("AccountResourceInsufficientException" + e.getMessage());
      return false;
    } catch (DupTransactionException e) {
      logger.info("dup trans" + e.getMessage());
      return false;
    } catch (TaposException e) {
      logger.info("tapos error" + e.getMessage());
      return false;
    } catch (TooBigTransactionException e) {
      logger.info("too big transaction" + e.getMessage());
      return false;
    } catch (TransactionExpirationException e) {
      logger.info("expiration transaction" + e.getMessage());
      return false;
    }
    return true;
  }

  @Override
  public LinkedList<BlockId> getLostBlockIds(List<BlockId> blockChainSummary)
      throws StoreException {
    //找到请求节点缺失的区块，从区块清单的最后一个找，查找本地节点和请求节点共同拥有的最高的块，
    //返回本地节点最高块之后的所有块（但是不超过2000的limit）
    //todo: return the remain block count.
    //todo: return the blocks it should be have.
    if (dbManager.getHeadBlockNum() == 0) {
      return new LinkedList<>();
    }

    BlockId unForkedBlockId;

    if (blockChainSummary.isEmpty() ||
        (blockChainSummary.size() == 1
            && blockChainSummary.get(0).equals(dbManager.getGenesisBlockId()))) {
      unForkedBlockId = dbManager.getGenesisBlockId();
    } else if (blockChainSummary.size() == 1
        && blockChainSummary.get(0).getNum() == 0) {
      return new LinkedList(Arrays.asList(dbManager.getGenesisBlockId()));
    } else {
      //todo: find a block we all know between the summary and my db.
      //只是在主链中进行查找是否存在summary的块
      Collections.reverse(blockChainSummary);
      unForkedBlockId = blockChainSummary.stream()
          .filter(blockId -> containBlockInMainChain(blockId))
          .findFirst().orElse(null);
      if (unForkedBlockId == null) {
        return new LinkedList<>();
      }
      //todo: can not find any same block form peer's summary and my db.
    }

    //todo: limit the count of block to send peer by one time.
    long unForkedBlockIdNum = unForkedBlockId.getNum();
    long len = Longs
        .min(dbManager.getHeadBlockNum(), unForkedBlockIdNum + NodeConstant.SYNC_FETCH_BATCH_NUM);

    //返回至多2000个块id
    LinkedList<BlockId> blockIds = new LinkedList<>();
    for (long i = unForkedBlockIdNum; i <= len; i++) {
      BlockId id = dbManager.getBlockIdByNum(i);
      blockIds.add(id);
    }
    return blockIds;
  }

  @Override
  public Deque<BlockId> getBlockChainSummary(BlockId beginBlockId, Deque<BlockId> blockIdsToFetch)
      throws TronException {
    //blockIdsToFetch：从节点将要抓取的区块
    Deque<BlockId> retSummary = new LinkedList<>();
    List<BlockId> blockIds = new ArrayList<>(blockIdsToFetch);
    long highBlkNum;
    long highNoForkBlkNum;
    long syncBeginNumber = dbManager.getSyncBeginNumber();
    long lowBlkNum = syncBeginNumber < 0 ? 0 : syncBeginNumber;  //lowBlkNum设置成syncBeginNumber  将固化块作为同步的起点

    LinkedList<BlockId> forkList = new LinkedList<>();

    if (!beginBlockId.equals(getGenesisBlock().getBlockId())) { //共有头块不是创世块
      if (containBlockInMainChain(beginBlockId)) {  //共同块在主链
        highBlkNum = beginBlockId.getNum();
        if (highBlkNum == 0) {
          throw new TronException(
              "This block don't equal my genesis block hash, but it is in my DB, the block id is :"
                  + beginBlockId.getString());
        }
        highNoForkBlkNum = highBlkNum;  // 将未分叉的最高块也设置成为最高块
        if (beginBlockId.getNum() < lowBlkNum) { //从其他节点同步了块，导致lowBlkNum高于共有块的高度
          lowBlkNum = beginBlockId.getNum();
        }
      } else {  // 本地主链没有包含共有块
        forkList = dbManager.getBlockChainHashesOnFork(beginBlockId);  //共同块在分叉链上，查找分叉的区块链
        if (forkList.isEmpty()) {
          throw new UnLinkedBlockException(
              "We want to find forkList of this block: " + beginBlockId.getString()
                  + " ,but in KhasoDB we can not find it, It maybe a very old beginBlockId, we are sync once,"
                  + " we switch and pop it after that time. ");
        }
        highNoForkBlkNum = forkList.peekLast().getNum();  // 找到分叉链，设置未分叉的最高块
        forkList.pollLast(); // 删除分叉链的最低块
        Collections.reverse(forkList);  // 逆序
        highBlkNum = highNoForkBlkNum + forkList.size();  // 设置最高块为没有分叉的块 + 分叉链的大小（已经删掉了分叉的那一块）
        if (highNoForkBlkNum < lowBlkNum) {
          throw new UnLinkedBlockException(
              "It is a too old block that we take it as a forked block long long ago"
                  + "\n lowBlkNum:" + lowBlkNum
                  + "\n highNoForkBlkNum" + highNoForkBlkNum);
        }
      }
    } else { // 共有块是创世块
      highBlkNum = dbManager.getHeadBlockNum();
      highNoForkBlkNum = highBlkNum;
    }

    if (!blockIds.isEmpty() && highBlkNum != blockIds.get(0).getNum() - 1) {
      logger.error("Check ERROR: highBlkNum:" + highBlkNum + ",blockIdToSyncFirstNum is "
          + blockIds.get(0).getNum() + ",blockIdToSyncEnd is " + blockIds.get(blockIds.size() - 1)
          .getNum());
    }

    long realHighBlkNum = highBlkNum + blockIds.size();

    do {
      if (lowBlkNum <= highNoForkBlkNum) { //如果没有分叉，从主链上一直能找到区块
        retSummary.offer(dbManager.getBlockIdByNum(lowBlkNum));
      } else if (lowBlkNum <= highBlkNum) { //如果有分叉，从highNoForkBlkNum这块之后，需要到分叉的区块上寻找
        retSummary.offer(forkList.get((int) (lowBlkNum - highNoForkBlkNum - 1)));
      } else { //有一些区块还没有同步过来，但是已经放在了待从该节点同步的集合里，从集合中找出来
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
        try {
          return new BlockMessage(dbManager.getBlockById(hash));
        } catch (BadItemException e) {
          logger.debug(e.getMessage());
        } catch (ItemNotFoundException e) {
          logger.debug(e.getMessage());
        } catch (Exception e) {
          logger.error("new BlockMessage fail", e);
        }
        return null;
      case TRX:
        try {
          return new TransactionMessage(
              dbManager.getTransactionStore().get(hash.getBytes()).getData());
        } catch (Exception e) {
          logger.error("new TransactionMessage fail", e);
        }
        return null;
      default:
        logger.info("message type not block or trx.");
        return null;
    }
  }

  @Override
  public void syncToCli(long unSyncNum) {
    logger.info("There are " + unSyncNum + " blocks we need to sync.");
    if (unSyncNum == 0) {
      logger.info("Sync Block Completed !!!");
    }
    dbManager.setSyncMode(unSyncNum == 0);
    //TODO: notify cli know how many block we need to sync
  }

  @Override
  public long getBlockTime(BlockId id) {
    try {
      return dbManager.getBlockById(id).getTimeStamp();
    } catch (BadItemException e) {
      return dbManager.getGenesisBlock().getTimeStamp();
    } catch (ItemNotFoundException e) {
      return dbManager.getGenesisBlock().getTimeStamp();
    }
  }

  @Override
  public BlockId getHeadBlockId() {
    return dbManager.getHeadBlockId();
  }

  @Override
  public BlockId getSolidBlockId() {
    return dbManager.getSolidBlockId();
  }

  @Override
  public long getHeadBlockTimeStamp() {
    return dbManager.getHeadBlockTimeStamp();
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

  //  @Override
//  public long getLatestSolidifiedBlockNum() {
//    return dbManager.getDynamicPropertiesStore().getLatestSolidifiedBlockNum();
//  }
//
//  @Override
//  public long getSyncBeginNumber() {
//    return dbManager.getSyncBeginNumber();
//  }
//
  @Override
  public boolean canChainRevoke(long num) {
    return num >= dbManager.getSyncBeginNumber();
  }
}
