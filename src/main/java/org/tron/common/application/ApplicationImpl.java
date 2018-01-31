package org.tron.common.application;

import java.util.ArrayList;
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
  public Protocal.Inventory getBlockIds(Protocal.Inventory inv) {
    //todo: return the blocks it should be have.
    return inv;
  }

  @Override
  public ArrayList<byte[]> getBlockChainSynopsis(byte[] refPoint, int num) {
    return new ArrayList<>();
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

  @Override
  public boolean isIncludedBlock(byte[] hash) {
    return blockStoreDb.isIncludeBlock(hash);
  }


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
