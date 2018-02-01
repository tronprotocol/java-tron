package org.tron.core.db;

import java.util.ArrayList;
import java.util.List;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.protos.Protocal.Transaction;


public class Manager {

  private AccountStore accountStore;
  private TransactionStore transactionStore;
  private BlockStore blockStore;
  private UtxoStore utxoStore;

  // transaction cache
  private List<Transaction> pendingTrxs;

  /**
   * all db should be init here.
   */
  public void init() {
    setAccountStore(AccountStore.create("account"));
    setTransactionStore(TransactionStore.create("trans"));
    setBlockStore(BlockStore.create("block"));
    setUtxoStore(UtxoStore.create("utxo"));

    pendingTrxs = new ArrayList<>();

  }

  public AccountStore getAccountStore() {
    return accountStore;
  }

  public void pushTrx(Transaction trx) {
    this.pendingTrxs.add(trx);
  }

  /**
   *
   * @param trxCap
   */
  public void processTrx(TransactionCapsule trxCap) {

    if (!trxCap.validate()) {
      return;
    }

    Transaction trx = trxCap.getTransaction();

    switch (trx.getType()) {
      case Transfer:
        break;
      case VoteWitess:
        break;
      case CreateAccount:
        break;
      case DeployContract:
        break;
    }
  }

  public void generateBlock() {

    for (Transaction trx : pendingTrxs) {

    }
  }

  private void setAccountStore(AccountStore accountStore) {
    this.accountStore = accountStore;
  }

  public TransactionStore getTransactionStore() {
    return transactionStore;
  }

  private void setTransactionStore(TransactionStore transactionStore) {
    this.transactionStore = transactionStore;
  }

  public BlockStore getBlockStore() {
    return blockStore;
  }

  private void setBlockStore(BlockStore blockStore) {
    this.blockStore = blockStore;
  }

  public UtxoStore getUtxoStore() {
    return utxoStore;
  }

  private void setUtxoStore(UtxoStore utxoStore) {
    this.utxoStore = utxoStore;
  }
}
