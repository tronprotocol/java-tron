package org.tron.common.application;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.tron.core.db.BlockStore;
import org.tron.core.db.Manager;
import org.tron.core.net.node.Node;
import org.tron.core.net.node.NodeDelegate;
import org.tron.core.net.node.NodeDelegateImpl;
import org.tron.core.net.node.NodeImpl;
import org.tron.core.config.args.Args;

public class ApplicationImpl implements Application {

  private static final Logger logger = LoggerFactory.getLogger("ApplicationImpl");
  private Node p2pNode;
  private BlockStore blockStoreDb;
  private ServiceContainer services;
  private NodeDelegate nodeDelegate;
  private Args args;

  private Manager dbManager;

  private boolean isProducer;

  private Injector injector;

  public ApplicationImpl(Injector injector) {
    this.injector = injector;
  }

  private void resetP2PNode() {
    p2pNode.listen();
    p2pNode.connectToP2PNetWork();
    p2pNode.syncFrom(blockStoreDb.getHeadBlockId());
  }
  
  @Override
  public void setArgs(Args args) {
    this.args = args;
  }

  @Override
  public void init(String path, Args args) {
    p2pNode = new NodeImpl();
    dbManager = new Manager();
    dbManager.init();
    blockStoreDb = dbManager.getBlockStore();
    services = new ServiceContainer();
    nodeDelegate = new NodeDelegateImpl(dbManager);
  }

  @Override
  public void addService(Service service) {
    services.add(service);
  }

  @Override
  public void initServices() {
    services.init();
  }

  @Override
  public Args getArgs() {
    return args;
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

  @Override
  public Injector getInjector() {
    return injector;
  }

  public boolean isProducer() {
    return isProducer;
  }

  public void setIsProducer(boolean producer) {
    isProducer = producer;
  }

}
