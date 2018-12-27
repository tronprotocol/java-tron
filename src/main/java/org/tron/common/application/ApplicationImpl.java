package org.tron.common.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.config.args.Args;
import org.tron.core.db.BlockStore;
import org.tron.core.db.Manager;
import org.tron.core.net.node.Node;
import org.tron.core.net.node.NodeDelegate;
import org.tron.core.net.node.NodeDelegateImpl;
import org.tron.core.net.node.NodeImpl;

@Slf4j
@Component
public class ApplicationImpl implements Application {

  @Autowired
  private NodeImpl p2pNode;

  private BlockStore blockStoreDb;
  private ServiceContainer services;
  private NodeDelegate nodeDelegate;

  @Autowired
  private Manager dbManager;

  private boolean isProducer;


  private void resetP2PNode() {
    p2pNode.listen();
    p2pNode.syncFrom(null);
  }

  @Override
  public void setOptions(Args args) {
    // not used
  }

  @Override
  @Autowired
  public void init(Args args) {
    blockStoreDb = dbManager.getBlockStore();
    services = new ServiceContainer();
    nodeDelegate = new NodeDelegateImpl(dbManager);
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
    p2pNode.setNodeDelegate(nodeDelegate);
    resetP2PNode();
  }

  @Override
  public void shutdown() {
    logger.info("******** begin to shutdown ********");
    synchronized (dbManager.getRevokingStore()) {
      closeRevokingStore();
      closeAllStore();
    }
    closeConnection();
    dbManager.stopRepushThread();
    logger.info("******** end to shutdown ********");
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

  private void closeConnection() {
    logger.info("******** begin to shutdown connection ********");
    try {
      p2pNode.close();
    } catch (Exception e) {
      logger.info("failed to close p2pNode. " + e);
    } finally {
      logger.info("******** end to shutdown connection ********");
    }
  }

  private void closeRevokingStore() {
    dbManager.getRevokingStore().shutdown();
  }

  private void closeAllStore() {
//    if (dbManager.getRevokingStore().getClass() == SnapshotManager.class) {
//      ((SnapshotManager) dbManager.getRevokingStore()).getDbs().forEach(IRevokingDB::close);
//    } else {
//      dbManager.closeAllStore();
//    }
    dbManager.closeAllStore();
  }

}
