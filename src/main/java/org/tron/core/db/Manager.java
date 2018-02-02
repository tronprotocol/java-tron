package org.tron.core.db;

import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.List;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.protos.Protocal;
import org.tron.protos.Protocal.Transaction;


public class Manager {

  private AccountStore accountStore;
  private TransactionStore transactionStore;
  private BlockStore blockStore;
  private UtxoStore utxoStore;
  private WitnessStore witnessStore;

  public WitnessStore getWitnessStore() {
    return witnessStore;
  }

  private void setWitnessStore(WitnessStore witnessStore) {
    this.witnessStore = witnessStore;
  }

  public List<Transaction> getPendingTrxs() {
    return pendingTrxs;
  }


  // transaction cache
  private List<Transaction> pendingTrxs;

  // witness

  public List<WitnessCapsule> getWitnesses() {
    List<WitnessCapsule> wits = new ArrayList<WitnessCapsule>();
    wits.add(new WitnessCapsule(ByteString.copyFromUtf8("0x12")));
    wits.add(new WitnessCapsule(ByteString.copyFromUtf8("0x11")));
    return wits;
  }

  public List<WitnessCapsule> getCurrentShuffledWitnesses() {
    return getWitnesses();
  }


  public ByteString getScheduledWitness(long slot) {
    if (slot < 0) {
      throw new RuntimeException("currentSlot should be positive.");
    }
    List<WitnessCapsule> currentShuffledWitnesses = getShuffledWitnesses();
    if (currentShuffledWitnesses == null || currentShuffledWitnesses.size() == 0) {
      throw new RuntimeException("ShuffledWitnesses is null.");
    }
    int witnessIndex = (int) slot % currentShuffledWitnesses.size();
    return currentShuffledWitnesses.get(witnessIndex).getAddress();
  }

  public List<WitnessCapsule> getShuffledWitnesses() {
    return getWitnesses();
  }


  /**
   * all db should be init here.
   */
  public void init() {
    setAccountStore(AccountStore.create("account"));
    setTransactionStore(TransactionStore.create("trans"));
    setBlockStore(BlockStore.create("block"));
    setUtxoStore(UtxoStore.create("utxo"));
    setWitnessStore(WitnessStore.create("witness"));

    pendingTrxs = new ArrayList<>();

  }

  public AccountStore getAccountStore() {
    return accountStore;
  }

  public void pushTrx(Transaction trx) {
    this.pendingTrxs.add(trx);
  }

  /**
   * Process transaction.
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
      default:
        break;
    }
  }

  /**
   * Generate a block.
   */
  public Protocal.Block generateBlock() {

    for (Transaction trx : pendingTrxs) {

    }
    //todo
    return Protocal.Block.getDefaultInstance();
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
