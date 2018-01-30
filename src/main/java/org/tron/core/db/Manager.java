package org.tron.core.db;

public class Manager {

  private AccountStore accountStore;
  private TransactionStore transactionStore;

  public void init() {
    accountStore = AccountStore.create("account");
    transactionStore = TransactionStore.create("trans");
  }

}
