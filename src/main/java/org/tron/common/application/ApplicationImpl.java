package org.tron.common.application;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.core.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.db.BlockStore;
import org.tron.core.db.DynamicPropertiesStore;
import org.tron.core.db.Manager;
import org.tron.core.net.message.BlockMessage;
import org.tron.core.net.message.Message;
import org.tron.core.net.message.MessageTypes;
import org.tron.core.net.message.TransactionMessage;
import org.tron.core.net.node.Node;
import org.tron.core.net.node.NodeDelegate;
import org.tron.core.net.node.NodeImpl;
import org.tron.program.Args;

public class ApplicationImpl implements Application, NodeDelegate {

  private static final Logger logger = LoggerFactory.getLogger("ApplicationImpl");
  private Node p2pNode;
  private BlockStore blockStoreDb;
  private ServiceContainer services;

  private Manager dbManager;

  private boolean isProducer;

  @Override
  public List<Sha256Hash> getBlockHashes(List<Sha256Hash> blockChainSummary) {
    //todo: return the blocks it should be have.

    List<Sha256Hash> retBlockHashes = new ArrayList<>();
    Sha256Hash lastKnownBlkHash = Sha256Hash.ZERO_HASH;

    if (!blockChainSummary.isEmpty()) {
      //todo: find a block we all know between the summary and my db.
      Collections.reverse(blockChainSummary);
      for (Sha256Hash hash : blockChainSummary) {
        if (blockStoreDb.containBlock(hash)) {
          lastKnownBlkHash = hash;
          break;
        }
      }
      if (lastKnownBlkHash == Sha256Hash.ZERO_HASH) {
        //todo: can not find any same block form peer's summary and my db.
      }
    }

    for (long num = blockStoreDb.getBlockNumByHash(lastKnownBlkHash);
        num <= blockStoreDb.getHeadBlockNum(); ++num) {
      if (num > 0) {
        retBlockHashes.add(blockStoreDb.getBlockHashByNum(num));
      }
    }
    return retBlockHashes;
  }

  @Override
  public List<Sha256Hash> getBlockChainSummary(Sha256Hash refPoint, int num) {

    List<Sha256Hash> retSummary = new ArrayList<>();
    long highBlkNum = 0;
    long highNoForkBlkNum;
    long lowBlkNum = 0; //TODO：get this from db.

    List<Sha256Hash> forkList = new ArrayList<>();

    if (refPoint != Sha256Hash.ZERO_HASH) {
      //todo: get db's head num to check local db's block status.
      if (blockStoreDb.containBlock(refPoint)) {
        highBlkNum = blockStoreDb.getBlockNumByHash(refPoint);
        highNoForkBlkNum = highBlkNum;
      } else {
        forkList = blockStoreDb.getBlockChainHashesOnFork(refPoint);
        highNoForkBlkNum = blockStoreDb.getBlockNumByHash(forkList.get(forkList.size() - 1));
        forkList.remove(forkList.get(forkList.size() - 1));
      }

    } else {
      highBlkNum = blockStoreDb.getHeadBlockNum();
      highNoForkBlkNum = highBlkNum;
      if (highBlkNum == 0) {
        return retSummary;
      }
    }

    long realHighBlkNum = highBlkNum + num;
    do {
      if (lowBlkNum <= highNoForkBlkNum) {
        retSummary.add(blockStoreDb.getBlockHashByNum(lowBlkNum));
      } else {
        retSummary.add(forkList.get((int) (lowBlkNum - highNoForkBlkNum - 1)));
      }
      lowBlkNum += (realHighBlkNum - lowBlkNum + 2) / 2;
    } while (lowBlkNum <= highBlkNum);
    return retSummary;
  }

  private void resetP2PNode() {
    p2pNode.listen();
    p2pNode.connectToP2PNetWork();
    p2pNode.syncFrom(blockStoreDb.getHeadBlockHash());
  }

  private void resetRpcServer() {

  }

  private void resetWebSocket() {

  }

  private void initalConfig(String configPathFile) {

  }

  //NodeDelegate

  @Override
  public void handleBlock(BlockCapsule block) {
    logger.info("handle block");
    blockStoreDb.saveBlock(block.getHash(), block);

    DynamicPropertiesStore dynamicPropertiesStore = dbManager.getDynamicPropertiesStore();

    //dynamicPropertiesStore.saveLatestBlockHeaderTimestamp(block.get);
    dynamicPropertiesStore.saveLatestBlockHeaderNumber(block.getNum());
    //dynamicPropertiesStore.saveLatestBlockHeaderHash(block.getHash());
  }

  @Override
  public void handleTransaction(TransactionCapsule trx) {
    logger.info("handle transaction");
    blockStoreDb.pushTransactions(trx.getTransaction());
  }


  @Override
  public Message getData(Sha256Hash hash, MessageTypes type) {
    switch (type) {
      case BLOCK:
        return new BlockMessage(blockStoreDb.findBlockByHash(hash.getBytes()));
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
    //sync to cli or gui
  }

  @Override
  public void getBlockNum(byte[] hash) {

  }

  @Override
  public void getBlockTime(byte[] hash) {

  }

  @Override
  public byte[] getHeadBlockId() {
    return new byte[0];
  }

  @Override
  public boolean hasItem(byte[] hash) {
    return false;
  }

  //IApplication

  @Override
  public void setOptions(Args args) {
  }

  @Override
  public void init(String path, Args args) {
    p2pNode = new NodeImpl();
    dbManager = new Manager();
    dbManager.init();
    blockStoreDb = dbManager.getBlockStore();
    services = new ServiceContainer();
  }

  @Override
  public void addService(Service service) {
    services.add(service);
  }

  @Override
  public void initServices(Args args) {
    services.init(args);
  }

  /**
   * start up the app.
   */
  public void startup() {
    p2pNode.setNodeDelegate(this);
    resetP2PNode();
    resetRpcServer();
    resetWebSocket();
  }

  @Override
  public void shutdown() {

  }

  @Override
  public void startServices() {
    services.start();
  }

  @Override
  public void shutdownServices() {
    services.stop();
  }

  @Override
  public Node getP2pNode() {
    return p2pNode;
  }

  @Override
  public BlockStore getBlockStoreS() {
    return blockStoreDb;
  }

  @Override
  public Manager getDbManager() {
    return dbManager;
  }

  public boolean isProducer() {
    return isProducer;
  }

  public void setIsProducer(boolean producer) {
    isProducer = producer;
  }

}
