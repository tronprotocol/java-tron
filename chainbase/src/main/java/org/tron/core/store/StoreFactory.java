package org.tron.core.store;

import org.tron.core.ChainBaseManager;

public class StoreFactory {

  private static final StoreFactory INSTANCE = new StoreFactory();

  private ChainBaseManager chainBaseManager;

  public StoreFactory() {

  }


  public static StoreFactory getInstance() {
    return INSTANCE;
  }


  public ChainBaseManager getChainBaseManager() {
    return chainBaseManager;
  }

  public StoreFactory setChainBaseManager(ChainBaseManager chainBaseManager) {
    this.chainBaseManager = chainBaseManager;
    return this;
  }
}
