package org.tron.core.db.api;

import com.googlecode.cqengine.IndexedCollection;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.tron.core.db.AccountStore;
import org.tron.core.db.AssetIssueStore;
import org.tron.core.db.BlockStore;
import org.tron.core.db.TransactionStore;
import org.tron.core.db.WitnessStore;
import org.tron.core.db.api.index.AbstractIndex;
import org.tron.protos.Contract.AssetIssueContract;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Witness;

@Slf4j
public class IndexHelper {

  @Getter
  @Resource
  private IndexedCollection<Transaction> transactionIndex;
  @Getter
  @Resource
  private IndexedCollection<Block> blockIndex;
  @Getter
  @Resource
  private IndexedCollection<Witness> witnessIndex;
  @Getter
  @Resource
  private IndexedCollection<Account> accountIndex;
  @Getter
  @Resource
  private IndexedCollection<AssetIssueContract> assetIssueIndex;

  private BlockStore blockStore;
  private WitnessStore witnessStore;
  private AccountStore accountStore;
  private TransactionStore transactionStore;
  private AssetIssueStore assetIssueStore;

  /**
   * init index
   */
  @PostConstruct
  public void init() {
    long start = System.currentTimeMillis();
    logger.info("begin to initialize indexHelper");
    blockStore.forEach(b -> blockIndex.add(b.getValue().getInstance()));
    witnessStore.forEach(w -> witnessIndex.add(w.getValue().getInstance()));
    transactionStore.forEach(t -> transactionIndex.add(t.getValue().getInstance()));
    accountStore.forEach(a -> accountIndex.add(a.getValue().getInstance()));
    assetIssueStore.forEach(a -> assetIssueIndex.add(a.getValue().getInstance()));
    logger.info("end to initialize indexHelper. cost:{}s", System.currentTimeMillis() - start);
  }

  private <T> void add(IndexedCollection<T> index, T t) {
    index.add(t);
  }

  public void add(Transaction t) {
    add(transactionIndex, t);
  }

  public void add(Block b) {
    add(blockIndex, b);
  }

  public void add(Witness w) {
    add(witnessIndex, w);
  }

  public void add(Account a) {
    add(accountIndex, a);
  }

  public void add(AssetIssueContract a) {
    add(assetIssueIndex, a);
  }

  public <T> void update(IndexedCollection<T> index, T t) {
    ((AbstractIndex<T>) index).update(t);
  }

  public void update(Transaction t) {
    update(transactionIndex, t);
  }

  public void update(Block b) {
    update(blockIndex, b);
  }

  public void update(Witness w) {
    update(witnessIndex, w);
  }

  public void update(Account a) {
    update(accountIndex, a);
  }

  public void update(AssetIssueContract a) {
    update(assetIssueIndex, a);
  }

  private <T> void remove(IndexedCollection<T> index, T t) {
    index.remove(t);
  }

  public void remove(Transaction t) {
    remove(transactionIndex, t);
  }

  public void remove(Block b) {
    remove(blockIndex, b);
  }

  public void remove(Witness w) {
    remove(witnessIndex, w);
  }

  public void remove(Account a) {
    remove(accountIndex, a);
  }

  public void remove(AssetIssueContract a) {
    remove(assetIssueIndex, a);
  }

  @Autowired
  public void setBlockStore(BlockStore blockStore) {
    this.blockStore = blockStore;
  }

  @Autowired
  public void setWitnessStore(WitnessStore witnessStore) {
    this.witnessStore = witnessStore;
  }

  @Autowired
  public void setAccountStore(AccountStore accountStore) {
    this.accountStore = accountStore;
  }

  @Autowired
  public void setTransactionStore(TransactionStore transactionStore) {
    this.transactionStore = transactionStore;
  }

  @Autowired
  public void setAssetIssueStore(AssetIssueStore assetIssueStore) {
    this.assetIssueStore = assetIssueStore;
  }
}
