package org.tron.common.application;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.tron.core.Sha256Hash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.core.db.BlockStore;
import org.tron.core.db.Manager;
import org.tron.core.net.message.BlockMessage;
import org.tron.core.net.message.Message;
import org.tron.core.net.message.TransactionMessage;
import org.tron.core.net.node.Node;
import org.tron.core.net.node.NodeDelegate;
import org.tron.core.net.node.NodeImpl;
import org.tron.program.Args;
import org.tron.protos.Protocal;

public class ApplicationImpl implements Application, NodeDelegate {

  private static final Logger logger = LoggerFactory.getLogger("ApplicationImpl");
  private Node p2pNode;
  private BlockStore blockStoreDb;
  private ServiceContainer services;

  private Manager dbManager;

  @Override
  public List<Sha256Hash> getBlockIds(List<Sha256Hash> blockChainSummary) {
    //todo: return the blocks it should be have.

    List<Sha256Hash> retBlockHashes = new ArrayList<>();
    Sha256Hash lastKnownBlkHash = Sha256Hash.ZERO_HASH;

    if (!blockChainSummary.isEmpty()) {
      //todo: find a block we all know between the summary and my db.
      Collections.reverse(blockChainSummary);
      for (Sha256Hash hash : blockChainSummary) {
        if (blockStoreDb.isIncludeBlock(hash)) {
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
  public List<Sha256Hash> getBlockChainSynopsis(Sha256Hash refPoint, int num) {

    List<Sha256Hash> retSummary = new ArrayList<>();
    long highBlkNum = 0;
    long highNoForkBlkNum = 0;
    long lowBlkNum = 0; //TODO：get this from db.

    List<Sha256Hash> forkList = new ArrayList<>();

    if (refPoint != Sha256Hash.ZERO_HASH) {
      //todo: get db's head num to check local db's block status.
      if (blockStoreDb.isIncludeBlock(refPoint)) {
        highBlkNum = blockStoreDb.getBlockNumByHash(refPoint);
        highNoForkBlkNum = highBlkNum;
      } else {
        //todo: set highNoForkBlkNum and push fork block to fork list.
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
    p2pNode.listenOn("endpoint");
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
  public void handleBlock(BlockMessage blkMsg) {
    logger.info("handle block");
    blockStoreDb.saveBlock("".getBytes(), blkMsg.getData());
  }

  @Override
  public void handleTransation(TransactionMessage trxMsg) {
    logger.info("handle transaction");
    blockStoreDb.pushTransactions(trxMsg.getTransaction());
  }

//  @Override
//  public boolean isIncludedBlock(Sha256Hash hash) {
//    return blockStoreDb.isIncludeBlock(hash);
//  }


  @Override
  public Message getData(byte[] hash) {
    //Block
    return new BlockMessage(blockStoreDb.findBlockByHash(hash));
    //todo
    //Trx
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
    services.init();
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
  public void startServies() {
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

}
