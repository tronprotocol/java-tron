package org.tron.core.db;

import static org.tron.common.crypto.Hash.sha3;

import com.carrotsearch.sizeof.RamUsageEstimator;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.protos.Protocal;
import org.tron.protos.Protocal.Block;
import org.tron.protos.Protocal.Block.Builder;
import org.tron.protos.Protocal.BlockHeader;
import org.tron.protos.Protocal.Transaction;

public class Manager {

  private static final Logger logger = LoggerFactory.getLogger("Manager");

  private static final long BLOCK_INTERVAL_SEC = 1;
  private static final long TRXS_SIZE = 2_000_000; // < 2MiB

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

  /**
   * get witnessCapsule List.
   */
  public List<WitnessCapsule> getWitnesses() {
    List<WitnessCapsule> wits = new ArrayList<WitnessCapsule>();
    wits.add(new WitnessCapsule(ByteString.copyFromUtf8("0x11")));
    wits.add(new WitnessCapsule(ByteString.copyFromUtf8("0x12")));
    wits.add(new WitnessCapsule(ByteString.copyFromUtf8("0x13")));
    wits.add(new WitnessCapsule(ByteString.copyFromUtf8("0x14")));
    return wits;
  }

  public List<WitnessCapsule> getCurrentShuffledWitnesses() {
    return getWitnesses();
  }


  /**
   * get ScheduledWitness by slot.
   */
  public ByteString getScheduledWitness(long slot) {
    long currentSlot = blockStore.currentASlot() + slot;

    if (currentSlot < 0) {
      throw new RuntimeException("currentSlot should be positive.");
    }
    List<WitnessCapsule> currentShuffledWitnesses = getShuffledWitnesses();
    if (currentShuffledWitnesses == null || currentShuffledWitnesses.size() == 0) {
      throw new RuntimeException("ShuffledWitnesses is null.");
    }
    int witnessIndex = (int) currentSlot % currentShuffledWitnesses.size();

    ByteString scheduledWitness = currentShuffledWitnesses.get(witnessIndex).getAddress();

    logger.info("scheduled_witness:" + scheduledWitness.toStringUtf8() + ",slot:" + currentSlot);

    return scheduledWitness;
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
  public boolean processTrx(TransactionCapsule trxCap) {

    if (!trxCap.validate()) {
      return false;
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

    return true;
  }

  /**
   * Generate a block.
   */
  public Protocal.Block generateBlock(WitnessCapsule witnessCapsule,
      long when) {
    long latestBlockNum = witnessCapsule.getLatestBlockNum();

    if (latestBlockNum == 0) {
      //throw new IllegalArgumentException("latest block num (" + latestBlockNum + ") is invalid.");
      //TODO: Handle exception
      return Block.getDefaultInstance();
    }

    byte[] parentBlockHash = blockStore.getBlockHashByNum(latestBlockNum).getBytes();
    Block parentBlock = null;
    try {
      parentBlock = Block.parseFrom(blockStore.findBlockByHash(parentBlockHash));
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
      return null;
    }
    // judge create block time
    if (when < parentBlock.getBlockHeader().getTimestamp()) {
      throw new IllegalArgumentException("generate block timestamp is invalid.");
    }

    long currentTrxSize = 0;
    long postponedTrxCount = 0;
    Builder blockBuilder = Block.newBuilder();

    for (Transaction trx : pendingTrxs) {
      currentTrxSize += RamUsageEstimator.sizeOf(trx);
      // judge block size
      if (currentTrxSize > TRXS_SIZE) {
        postponedTrxCount++;
        continue;
      }

      // apply transaction
      if (processTrx(new TransactionCapsule(trx))) {
        // push into block
        blockBuilder.addTransactions(trx);
      }
    }

    if (postponedTrxCount > 0) {
      logger.info("postponed {} transactions due to block size limit", postponedTrxCount);
    }

    // generate block
    BlockHeader.Builder blockHeaderBuilder = BlockHeader.newBuilder()
        .setNumber(parentBlock.getBlockHeader().getNumber() + 1)
        .setParentHash(parentBlock.getBlockHeader().getHash())
        .setTimestamp(when)
        .setWitnessAddress(witnessCapsule.getAddress());

    blockBuilder.setBlockHeader(blockHeaderBuilder.build());

    blockHeaderBuilder.setHash(ByteString.copyFrom(sha3(blockBuilder.build().toByteArray())));

    blockBuilder.setBlockHeader(blockHeaderBuilder.build());

    return blockBuilder.build();
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
