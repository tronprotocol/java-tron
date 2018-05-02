package org.tron.core.db.api;

import javax.annotation.Resource;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.ProtoCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.db.api.index.AbstractIndex;
import org.tron.core.db.api.index.AccountIndex;
import org.tron.core.db.api.index.AssetIssueIndex;
import org.tron.core.db.api.index.BlockIndex;
import org.tron.core.db.api.index.TransactionIndex;
import org.tron.core.db.api.index.WitnessIndex;
import org.tron.protos.Contract.AssetIssueContract;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Witness;

@Slf4j
public class IndexHelper {

  @Getter
  @Resource
  private TransactionIndex transactionIndex;
  @Getter
  @Resource
  private BlockIndex blockIndex;
  @Getter
  @Resource
  private WitnessIndex witnessIndex;
  @Getter
  @Resource
  private AccountIndex accountIndex;
  @Getter
  @Resource
  private AssetIssueIndex assetIssueIndex;

  private <T> void add(AbstractIndex<? extends ProtoCapsule, T> index, byte[] bytes) {
    index.add(bytes);
  }

  public void add(Transaction t) {
    add(transactionIndex, getKey(t));
  }

  public void add(Block b) {
    add(blockIndex, getKey(b));
  }

  public void add(Witness w) {
    add(witnessIndex, getKey(w));
  }

  public void add(Account a) {
    add(accountIndex, getKey(a));
  }

  public void add(AssetIssueContract a) {
    add(assetIssueIndex, getKey(a));
  }

  private <T> void update(AbstractIndex<? extends ProtoCapsule, T> index, byte[] bytes) {
    index.update(bytes);
  }

  public void update(Transaction t) {
    update(transactionIndex, getKey(t));
  }

  public void update(Block b) {
    update(blockIndex, getKey(b));
  }

  public void update(Witness w) {
    update(witnessIndex, getKey(w));
  }

  public void update(Account a) {
    update(accountIndex, getKey(a));
  }

  public void update(AssetIssueContract a) {
    update(assetIssueIndex, getKey(a));
  }

  private <T> void remove(AbstractIndex<? extends ProtoCapsule, T> index, byte[] bytes) {
    index.remove(bytes);
  }

  public void remove(Transaction t) {
    remove(transactionIndex, getKey(t));
  }

  public void remove(Block b) {
    remove(blockIndex, getKey(b));
  }

  public void remove(Witness w) {
    remove(witnessIndex, getKey(w));
  }

  public void remove(Account a) {
    remove(accountIndex, getKey(a));
  }

  public void remove(AssetIssueContract a) {
    remove(assetIssueIndex, getKey(a));
  }

  private byte[] getKey(Transaction t) {
    return new TransactionCapsule(t).getTransactionId().getBytes();
  }

  private byte[] getKey(Block b) {
    return new BlockCapsule(b).getBlockId().getBytes();
  }

  private byte[] getKey(Witness w) {
    return new WitnessCapsule(w).createDbKey();
  }

  private byte[] getKey(Account a) {
    return new AccountCapsule(a).getAddress().toByteArray();
  }

  private byte[] getKey(AssetIssueContract a) {
    return new AssetIssueCapsule(a).getName().toByteArray();
  }

}
