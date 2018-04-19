package org.tron.common.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.config.args.Args;
import org.tron.core.db.AbstractRevokingStore;
import org.tron.core.db.BlockStore;
import org.tron.core.db.Manager;
import org.tron.core.db.RevokingStore;
import org.tron.core.db.TronDatabase;
import org.tron.core.exception.RevokingStoreIllegalStateException;
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

  private Manager dbManager;

  private boolean isProducer;

  private void resetP2PNode() {
    p2pNode.listen();
    //p2pNode.connectToP2PNetWork();
    p2pNode.syncFrom(blockStoreDb.getHeadBlockId());
  }

  @Override
  public void setOptions(Args args) {

  }

  @Override
  @Autowired
  public void init(Args args) {
    //p2pNode = new NodeImpl();
    //p2pNode = ctx.getBean(NodeImpl.class);
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
    System.err.println("******** begin to shutdown ********");
    closeConnection();
    synchronized (RevokingStore.getInstance()) {
      closeRevokingStore();
      closeAllStore();
    }
    System.err.println("******** end to shutdown ********");
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
    System.err.println("******** begin to shutdown connection ********");
    try {
      p2pNode.close();
    } catch (Exception e) {
      System.err.println("faild to close p2pNode. " + e);
    } finally {
      System.err.println("******** end to shutdown connection ********");
    }
  }

  private void closeRevokingStore() {
    System.err.println("******** begin to pop revokingDb ********");
    System.err.println("******** before revokingDb size:" + RevokingStore.getInstance().size());
    try {
      RevokingStore.getInstance().disable();
      boolean exit = false;
      while (!exit) {
        try {
          RevokingStore.getInstance().commit();
        } catch (RevokingStoreIllegalStateException e) {
          exit = true;
        }
      }

      while (true) {
        try {
          RevokingStore.getInstance().pop();
        } catch (RevokingStoreIllegalStateException e) {
          break;
        }
      }
    } catch (Exception e) {
      System.err.println("******** faild to pop revokingStore. " + e);
    } finally {
      System.err.println("******** after revokingDb size:" + RevokingStore.getInstance().size());
      System.err.println("******** after revokingDb contains:"
          + ((AbstractRevokingStore) RevokingStore.getInstance()).getStack());
      System.err.println("******** end to pop revokingDb ********");
    }
  }

  private void closeAllStore() {
    System.err.println("******** begin to close db ********");
    closeOneStore(dbManager.getAccountStore());
    closeOneStore(dbManager.getBlockStore());
    closeOneStore(dbManager.getWitnessStore());
    closeOneStore(dbManager.getAssetIssueStore());
    closeOneStore(dbManager.getDynamicPropertiesStore());
    closeOneStore(dbManager.getTransactionStore());
    closeOneStore(dbManager.getUtxoStore());
    System.err.println("******** end to close db ********");
  }

  private void closeOneStore(TronDatabase database) {
    System.err.println("******** begin to close " + database.getName() + " ********");
    try {
      database.close();
    } catch (Exception e) {
      System.err.println("faild to close  " + database.getName() + ". " + e);
    } finally {
      System.err.println("******** end to close " + database.getName() + " ********");
    }
  }

}
