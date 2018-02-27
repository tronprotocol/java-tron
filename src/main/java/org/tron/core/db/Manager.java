package org.tron.core.db;

import com.carrotsearch.sizeof.RamUsageEstimator;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.utils.ByteArray;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.protos.Contract.VoteWitnessContract;
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
  private DynamicPropertiesStore dynamicPropertiesStore;

  public WitnessStore getWitnessStore() {
    return witnessStore;
  }

  private void setWitnessStore(WitnessStore witnessStore) {
    this.witnessStore = witnessStore;
  }

  public DynamicPropertiesStore getDynamicPropertiesStore() {
    return dynamicPropertiesStore;
  }

  public void setDynamicPropertiesStore(DynamicPropertiesStore dynamicPropertiesStore) {
    this.dynamicPropertiesStore = dynamicPropertiesStore;
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
    setDynamicPropertiesStore(DynamicPropertiesStore.create("properties"));

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

    if (!trxCap.validateSignature()) {
      return false;
    }

    Transaction trx = trxCap.getTransaction();

    switch (trx.getType()) {
      case Transfer:
        break;
      case VoteWitess:
        voteWitnessCount(trx);
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

  private void voteWitnessCount(Transaction trx) {
    try {
      if (trx.getParameterList() == null || trx.getParameterList().isEmpty()) {
        return;
      }
      Any parameter = trx.getParameterList().get(0);
      if (parameter.is(VoteWitnessContract.class)) {
        VoteWitnessContract voteContract = parameter.unpack(VoteWitnessContract.class);
        int voteAdd = voteContract.getCount();
        if (voteAdd > 0) {
          voteContract.getVoteAddressList().forEach(voteAddress -> {
            countvotewitness(voteAddress, voteAdd);
          });
        }
      }
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
    }
  }

  private void countvotewitness(ByteString voteAddress, int countAdd) {
    logger.info("voteAddress is {},voteAddCount is {}", voteAddress, countAdd);
    int count = 0;
    byte[] value = witnessStore.dbSource.getData(voteAddress.toByteArray());
    if (null != value) {
      count = ByteArray.toInt(value);
    }

    logger.info("voteAddress pre-voteCount is {}", count);
    count += countAdd;
    witnessStore.dbSource.putData(voteAddress.toByteArray(), ByteArray.fromInt(count));
    logger.info("voteAddress after-voteCount is {}", count);
  }

  /**
   * Generate a block.
   */
  public BlockCapsule generateBlock(WitnessCapsule witnessCapsule,
      long when, String privateKey) {

    final long timestamp = this.dynamicPropertiesStore.getLatestBlockHeaderTimestamp();

    final long number = this.dynamicPropertiesStore.getLatestBlockHeaderNumber();

    final ByteString preHash = this.dynamicPropertiesStore.getLatestBlockHeaderHash();

    // judge create block time
    if (when < timestamp) {
      throw new IllegalArgumentException("generate block timestamp is invalid.");
    }

    long currentTrxSize = 0;
    long postponedTrxCount = 0;

    BlockCapsule blockCapsule = new BlockCapsule(number + 1, preHash, when,
        witnessCapsule.getAddress());

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
        blockCapsule.addTransaction(trx);
        pendingTrxs.remove(trx);
      }
    }


    if (postponedTrxCount > 0) {
      logger.info("{} transactions over the block size limit", postponedTrxCount);
    }

    blockCapsule.setMerklerRoot();
    blockCapsule.sign(privateKey);
    blockCapsule.generatedByMyself = true;

    dynamicPropertiesStore.saveLatestBlockHeaderHash(blockCapsule.getBlockId().getByteString());
    dynamicPropertiesStore.saveLatestBlockHeaderNumber(blockCapsule.getNum());
    dynamicPropertiesStore.saveLatestBlockHeaderTimestamp(blockCapsule.getTimeStamp());
    return blockCapsule;
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
