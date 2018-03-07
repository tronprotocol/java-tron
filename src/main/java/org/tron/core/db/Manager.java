package org.tron.core.db;

import com.carrotsearch.sizeof.RamUsageEstimator;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javafx.util.Pair;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.storage.leveldb.LevelDbDataSourceImpl;
import org.tron.common.utils.ByteArray;
import org.tron.core.Sha256Hash;
import org.tron.core.actuator.Actuator;
import org.tron.core.actuator.ActuatorFactory;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.capsule.utils.BlockUtil;
import org.tron.core.config.args.Args;
import org.tron.core.config.args.GenesisBlock;
import org.tron.core.config.args.InitialWitness;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.ValidateSignatureException;
import org.tron.protos.Protocal.AccountType;

public class Manager {

  private static final Logger logger = LoggerFactory.getLogger("Manager");

  private static final long BLOCK_INTERVAL_SEC = 1;
  private static final int MAX_ACTIVE_WITNESS_NUM = 21;
  private static final long TRXS_SIZE = 2_000_000; // < 2MiB

  private AccountStore accountStore;
  private TransactionStore transactionStore;
  private BlockStore blockStore;
  private UtxoStore utxoStore;
  private WitnessStore witnessStore;
  private DynamicPropertiesStore dynamicPropertiesStore;
  private BlockCapsule genesisBlock;


  private LevelDbDataSourceImpl numHashCache;
  private KhaosDatabase khaosDb;
  private BlockCapsule head;

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

  public List<TransactionCapsule> getPendingTrxs() {
    return pendingTrxs;
  }


  // transaction cache
  private List<TransactionCapsule> pendingTrxs;

  private List<WitnessCapsule> wits = new ArrayList<>();

  // witness

  public List<WitnessCapsule> getWitnesses() {
    return wits;
  }

  public Sha256Hash getHeadBlockId() {
    return Sha256Hash.wrap(this.dynamicPropertiesStore.getLatestBlockHeaderHash());
  }

  /**
   * TODO: should get this list from Database. get witnessCapsule List.
   */

  public void initialWitnessList() {
    List<InitialWitness.ActiveWitness> activeWitnessList = Args.getInstance().getInitialWitness()
        .getActiveWitnessList();
    activeWitnessList.forEach(activeWitness -> {
      wits.add(new WitnessCapsule(ByteString.copyFromUtf8(activeWitness.getPublicKey()),
          activeWitness.getUrl()));
    });
  }


  public void addWitness(WitnessCapsule witnessCapsule) {
    this.wits.add(witnessCapsule);
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
    if (CollectionUtils.isEmpty(currentShuffledWitnesses)) {
      throw new RuntimeException("ShuffledWitnesses is null.");
    }
    int witnessIndex = (int) currentSlot % currentShuffledWitnesses.size();

    ByteString scheduledWitness = currentShuffledWitnesses.get(witnessIndex).getAddress();
    //logger.info("scheduled_witness:" + scheduledWitness.toStringUtf8() + ",slot:" + currentSlot);

    return scheduledWitness;
  }

  public int calculateParticipationRate() {
    return 100 * dynamicPropertiesStore.getBlockFilledSlots().calculateFilledSlotsCount()
        / BlockFilledSlots.SLOT_NUMBER;
  }

  /**
   * get shuffled witnesses.
   */
  public List<WitnessCapsule> getShuffledWitnesses() {
    List<WitnessCapsule> shuffleWits = getWitnesses();
    //Collections.shuffle(shuffleWits);
    return shuffleWits;
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

    numHashCache = new LevelDbDataSourceImpl(
        Args.getInstance().getOutputDirectory(), "block" + "_NUM_HASH");
    numHashCache.initDB();
    khaosDb = new KhaosDatabase("block" + "_KDB");

    pendingTrxs = new ArrayList<>();
    initGenesis();
    initHeadBlock(Sha256Hash.wrap(this.dynamicPropertiesStore.getLatestBlockHeaderHash()));
  }

  /**
   * init genesis block.
   */
  public void initGenesis() {
    genesisBlock = BlockUtil.newGenesisBlockCapsule();
    if (containBlock(genesisBlock.getBlockId())) {
      Args.getInstance().setChainId(genesisBlock.getBlockId().toString());
    } else {
      if (hasBlocks()) {
        logger.error("genesis block modify, please delete database directory({}) and restart",
            Args.getInstance().getOutputDirectory());
        System.exit(1);
      } else {
        logger.info("create genesis block");
        Args.getInstance().setChainId(genesisBlock.getBlockId().toString());
        try {
          pushBlock(genesisBlock);
        } catch (ValidateSignatureException e) {
          e.printStackTrace();
        }
        this.dynamicPropertiesStore.saveLatestBlockHeaderNumber(0);
        this.dynamicPropertiesStore.saveLatestBlockHeaderHash(
            genesisBlock.getBlockId().getByteString());
        this.dynamicPropertiesStore.saveLatestBlockHeaderTimestamp(
            genesisBlock.getTimeStamp());
        initAccount();
      }
    }
  }

  /**
   * save account into database.
   */
  public void initAccount() {
    Args args = Args.getInstance();
    GenesisBlock genesisBlockArg = args.getGenesisBlock();
    genesisBlockArg.getAssets().forEach(key -> {
      AccountCapsule accountCapsule = new AccountCapsule(AccountType.AssetIssue,
          ByteString.copyFrom(ByteArray.fromHexString(key.getAddress())),
          Long.valueOf(key.getBalance()));

      this.accountStore.putAccount(accountCapsule);
    });
  }

  public AccountStore getAccountStore() {
    return accountStore;
  }

  /**
   * push transaction into db.
   */
  public boolean pushTransactions(TransactionCapsule trx)
      throws ValidateSignatureException, ContractValidateException, ContractExeException {
    logger.info("push transaction");
    if (!trx.validateSignature()) {
      throw new ValidateSignatureException("trans sig validate failed");
    }
    processTransaction(trx);
    pendingTrxs.add(trx);
    getTransactionStore().dbSource.putData(trx.getTransactionId().getBytes(), trx.getData());
    return true;
  }


  /**
   * save a block.
   */
  public void pushBlock(BlockCapsule block) throws ValidateSignatureException {
    khaosDb.push(block);
    //todo: check block's validity
    if (!block.generatedByMyself) {
      if (!block.validateSignature()) {
        logger.info("The siganature is not validated.");
        return;
      }

      if (!block.calcMerklerRoot().equals(block.getMerklerRoot())) {
        logger.info("The merkler root doesn't match, Calc result is " + block.calcMerklerRoot()
            + " , the headers is " + block.getMerklerRoot());
        return;
      }
      // block
      processBlock(block);
      //todo: In some case it need to switch the branch
    }
    getBlockStore().dbSource.putData(block.getBlockId().getBytes(), block.getData());
    logger.info("save block, Its ID is " + block.getBlockId() + ", Its num is " + block.getNum());
    numHashCache.putData(ByteArray.fromLong(block.getNum()), block.getBlockId().getBytes());
    head = khaosDb.getHead();
    // blockDbDataSource.putData(blockHash, blockData);
  }


  /**
   * Get the fork branch.
   */
  public ArrayList<BlockId> getBlockChainHashesOnFork(BlockId forkBlockHash) {
    Pair<ArrayList<BlockCapsule>, ArrayList<BlockCapsule>> branch =
        khaosDb.getBranch(head.getBlockId(), forkBlockHash);
    return branch.getValue().stream()
        .map(blockCapsule -> blockCapsule.getBlockId())
        .collect(Collectors.toCollection(ArrayList::new));
  }

  /**
   * judge id.
   *
   * @param blockHash blockHash
   */
  public boolean containBlock(Sha256Hash blockHash) {
    //TODO: check it from levelDB
    return khaosDb.containBlock(blockHash)
        || getBlockStore().dbSource.getData(blockHash.getBytes()) != null;
  }

  /**
   * find a block packed data by id.
   */
  public byte[] findBlockByHash(Sha256Hash hash) {
    return khaosDb.containBlock(hash) ? khaosDb.getBlock(hash).getData()
        : getBlockStore().dbSource.getData(hash.getBytes());
  }

  /**
   * Get a BlockCapsule by id.
   */
  public BlockCapsule getBlockByHash(Sha256Hash hash) {
    return khaosDb.containBlock(hash) ? khaosDb.getBlock(hash)
        : new BlockCapsule(getBlockStore().dbSource.getData(hash.getBytes()));
  }

  /**
   * Delete a block.
   */
  public void deleteBlock(Sha256Hash blockHash) {
    BlockCapsule block = getBlockByHash(blockHash);
    khaosDb.removeBlk(blockHash);
    getBlockStore().dbSource.deleteData(blockHash.getBytes());
    numHashCache.deleteData(ByteArray.fromLong(block.getNum()));
    head = khaosDb.getHead();
  }

  /**
   * judge has blocks.
   */
  public boolean hasBlocks() {
    return getBlockStore().dbSource.allKeys().size() > 0 || khaosDb.hasData();
  }

  /**
   * Process transaction.
   */
  public boolean processTransaction(TransactionCapsule trxCap)
      throws ValidateSignatureException, ContractExeException, ContractValidateException {

    if (trxCap == null || !trxCap.validateSignature()) {
      return false;
    }
    List<Actuator> actuatorList = ActuatorFactory.createActuator(trxCap, this);
    assert actuatorList != null;
    for (Actuator act : actuatorList) {
      act.validate();
      act.execute();

    }
    return true;
  }

  /**
   * Get the block id from the number.
   */
  public BlockId getBlockIdByNum(long num) {
    byte[] hash = numHashCache.getData(ByteArray.fromLong(num));
    return ArrayUtils.isNotEmpty(hash)
        ? genesisBlock.getBlockId()
        : new BlockId(Sha256Hash.wrap(hash), num);
  }

  /**
   * Get number of block by the block id.
   */
  public long getBlockNumById(Sha256Hash hash) {
    if (khaosDb.containBlock(hash)) {
      return khaosDb.getBlock(hash).getNum();
    }

    //TODO: optimize here
    byte[] blockByte = getBlockStore().dbSource.getData(hash.getBytes());
    return ArrayUtils.isNotEmpty(blockByte) ? new BlockCapsule(blockByte).getNum() : 0;
  }

  public void initHeadBlock(Sha256Hash id) {
    head = getBlockByHash(id);
  }

  /**
   * Generate a block.
   */
  public BlockCapsule generateBlock(WitnessCapsule witnessCapsule,
      long when, byte[] privateKey)
      throws ValidateSignatureException {

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

    for (TransactionCapsule trx : pendingTrxs) {
      currentTrxSize += RamUsageEstimator.sizeOf(trx);
      // judge block size
      if (currentTrxSize > TRXS_SIZE) {
        postponedTrxCount++;
        continue;
      }

      // apply transaction
      try {
        if (processTransaction(trx)) {
          // push into block
          blockCapsule.addTransaction(trx);
          pendingTrxs.remove(trx);
        }
      } catch (ContractExeException e) {
        e.printStackTrace();
      } catch (ContractValidateException e) {
        e.printStackTrace();
      }
    }

    if (postponedTrxCount > 0) {
      logger.info("{} transactions over the block size limit", postponedTrxCount);
    }

    blockCapsule.setMerklerRoot();
    blockCapsule.sign(privateKey);
    blockCapsule.generatedByMyself = true;
    pushBlock(blockCapsule);
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

  /**
   * process block.
   */
  public void processBlock(BlockCapsule block) throws ValidateSignatureException {
    for (TransactionCapsule transactionCapsule : block.getTransactions()) {
      try {
        processTransaction(transactionCapsule);
      } catch (ContractExeException e) {
        e.printStackTrace();
      } catch (ContractValidateException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * update witness.
   */
  public void updateWitness() {
    //TODO validate maint needed
    Map<ByteString, Long> countWitness = Maps.newHashMap();
    List<AccountCapsule> accountList = accountStore.getAllAccounts();
    accountList.forEach(account -> {
      account.getVotesList().forEach(vote -> {
        //TODO validate witness //active_witness
        if (countWitness.containsKey(vote.getVoteAddress())) {
          countWitness.put(vote.getVoteAddress(),
              countWitness.get(vote.getVoteAddress()) + vote.getVoteCount());
        } else {
          countWitness.put(vote.getVoteAddress(), vote.getVoteCount());
        }
      });
    });
    List<WitnessCapsule> witnessCapsuleList = Lists.newArrayList();
    countWitness.forEach((address, voteCount) -> {
      WitnessCapsule witnessCapsule = witnessStore.getWitness(address);
      if (null == witnessCapsule) {
        logger.warn("winessSouece is null.address is {}", address);
      }
      witnessCapsule.setVoteCount(voteCount);
      witnessCapsuleList.add(witnessCapsule);
      witnessStore.putWitness(witnessCapsule);
    });
    witnessCapsuleList.sort((a, b) -> {
      return (int) (a.getVoteCount() - b.getVoteCount());
    });
    wits = witnessCapsuleList.subList(0, MAX_ACTIVE_WITNESS_NUM);
  }
}
