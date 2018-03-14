package org.tron.core.db;

import com.carrotsearch.sizeof.RamUsageEstimator;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javafx.util.Pair;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.storage.leveldb.LevelDbDataSourceImpl;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.DialogOptional;
import org.tron.common.utils.Sha256Hash;
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
import org.tron.core.db.AbstractRevokingStore.Dialog;
import org.tron.core.exception.BalanceInsufficientException;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.RevokingStoreIllegalStateException;
import org.tron.core.exception.ValidateSignatureException;
import org.tron.protos.Protocol.AccountType;

public class Manager {

  private static final Logger logger = LoggerFactory.getLogger("Manager");

  private static final long BLOCK_INTERVAL_SEC = 1;
  private static final int MAX_ACTIVE_WITNESS_NUM = 21;
  private static final long TRXS_SIZE = 2_000_000; // < 2MiB
  public static final long LOOP_INTERVAL = Args.getInstance().getBlockInterval(); // millisecond

  private AccountStore accountStore;
  private TransactionStore transactionStore;
  private BlockStore blockStore;
  private UtxoStore utxoStore;
  private WitnessStore witnessStore;
  private AssetIssueStore assetIssueStore;
  private DynamicPropertiesStore dynamicPropertiesStore;
  private BlockCapsule genesisBlock;


  private LevelDbDataSourceImpl numHashCache;
  private KhaosDatabase khaosDb;
  private BlockCapsule head;
  private RevokingDatabase revokingStore;
  private DialogOptional<Dialog> dialog = DialogOptional.empty();

  public WitnessStore getWitnessStore() {
    return this.witnessStore;
  }

  private void setWitnessStore(final WitnessStore witnessStore) {
    this.witnessStore = witnessStore;
  }

  public DynamicPropertiesStore getDynamicPropertiesStore() {
    return this.dynamicPropertiesStore;
  }

  public void setDynamicPropertiesStore(final DynamicPropertiesStore dynamicPropertiesStore) {
    this.dynamicPropertiesStore = dynamicPropertiesStore;
  }

  public List<TransactionCapsule> getPendingTrxs() {
    return this.pendingTrxs;
  }


  // transaction cache
  private List<TransactionCapsule> pendingTrxs;

  private List<WitnessCapsule> wits = new ArrayList<>();

  // witness

  public List<WitnessCapsule> getWitnesses() {
    return this.wits;
  }

  public BlockId getHeadBlockId() {
    return head.getBlockId();
    //return Sha256Hash.wrap(this.dynamicPropertiesStore.getLatestBlockHeaderHash());
  }

  public long getHeadBlockNum() {
    return this.head.getNum();
  }

  public void addWitness(final WitnessCapsule witnessCapsule) {
    this.wits.add(witnessCapsule);
  }

  public List<WitnessCapsule> getCurrentShuffledWitnesses() {
    return this.getWitnesses();
  }


  /**
   * get ScheduledWitness by slot.
   */
  public ByteString getScheduledWitness(final long slot) {
    final long currentSlot = this.blockStore.currentASlot() + slot;

    if (currentSlot < 0) {
      throw new RuntimeException("currentSlot should be positive.");
    }
    final List<WitnessCapsule> currentShuffledWitnesses = this.getShuffledWitnesses();
    if (CollectionUtils.isEmpty(currentShuffledWitnesses)) {
      throw new RuntimeException("ShuffledWitnesses is null.");
    }
    final int witnessIndex = (int) currentSlot % currentShuffledWitnesses.size();

    final ByteString scheduledWitness = currentShuffledWitnesses.get(witnessIndex).getAddress();
    //logger.info("scheduled_witness:" + scheduledWitness.toStringUtf8() + ",slot:" + currentSlot);

    return scheduledWitness;
  }

  public int calculateParticipationRate() {
    return 100 * this.dynamicPropertiesStore.getBlockFilledSlots().calculateFilledSlotsCount()
        / BlockFilledSlots.SLOT_NUMBER;
  }

  /**
   * get shuffled witnesses.
   */
  public List<WitnessCapsule> getShuffledWitnesses() {
    final List<WitnessCapsule> shuffleWits = this.getWitnesses();
    //Collections.shuffle(shuffleWits);
    return shuffleWits;
  }


  /**
   * all db should be init here.
   */
  public void init() {
    this.setAccountStore(AccountStore.create("account"));
    this.setTransactionStore(TransactionStore.create("trans"));
    this.setBlockStore(BlockStore.create("block"));
    this.setUtxoStore(UtxoStore.create("utxo"));
    this.setWitnessStore(WitnessStore.create("witness"));
    this.setAssetIssueStore(AssetIssueStore.create("asset-issue"));
    this.setDynamicPropertiesStore(DynamicPropertiesStore.create("properties"));

    revokingStore = RevokingStore.getInstance();
    revokingStore.enable();

    this.numHashCache = new LevelDbDataSourceImpl(
        Args.getInstance().getOutputDirectory(), "block" + "_NUM_HASH");
    this.numHashCache.initDB();
    this.khaosDb = new KhaosDatabase("block" + "_KDB");

    this.pendingTrxs = new ArrayList<>();
    try {
      this.initGenesis();
    } catch (ContractValidateException e) {
      logger.error(e.getMessage());
      System.exit(-1);
    } catch (ContractExeException e) {
      logger.error(e.getMessage());
      System.exit(-1);
    } catch (ValidateSignatureException e) {
      logger.error(e.getMessage());
      System.exit(-1);
    }
    this.initHeadBlock(Sha256Hash.wrap(this.dynamicPropertiesStore.getLatestBlockHeaderHash()));
  }

  public BlockId getGenesisBlockId() {
    return this.genesisBlock.getBlockId();
  }

  public BlockCapsule getGenesisBlock() {
    return genesisBlock;
  }

  /**
   * init genesis block.
   */
  public void initGenesis()
      throws ContractValidateException, ContractExeException, ValidateSignatureException {
    this.genesisBlock = BlockUtil.newGenesisBlockCapsule();
    if (this.containBlock(this.genesisBlock.getBlockId())) {
      Args.getInstance().setChainId(this.genesisBlock.getBlockId().toString());
    } else {
      if (this.hasBlocks()) {
        logger.error("genesis block modify, please delete database directory({}) and restart",
            Args.getInstance().getOutputDirectory());
        System.exit(1);
      } else {
        logger.info("create genesis block");
        Args.getInstance().setChainId(this.genesisBlock.getBlockId().toString());

        this.pushBlock(this.genesisBlock);

        this.dynamicPropertiesStore.saveLatestBlockHeaderNumber(0);
        this.dynamicPropertiesStore.saveLatestBlockHeaderHash(
            this.genesisBlock.getBlockId().getByteString());
        this.dynamicPropertiesStore.saveLatestBlockHeaderTimestamp(
            this.genesisBlock.getTimeStamp());
        this.initAccount();
        this.initWitness();

      }
    }
  }

  /**
   * save account into database.
   */
  public void initAccount() {
    final Args args = Args.getInstance();
    final GenesisBlock genesisBlockArg = args.getGenesisBlock();
    genesisBlockArg.getAssets().forEach(account -> {
      account.setAccountType("Normal");//to be set in conf
      final AccountCapsule accountCapsule = new AccountCapsule(account.getAccountName(),
          account.getAccountType(),
          ByteString.copyFrom(account.getAddressBytes()),
          account.getBalance());
      this.accountStore.put(account.getAddressBytes(), accountCapsule);
    });
  }

  private void initWitness() {
    final Args args = Args.getInstance();
    final GenesisBlock genesisBlockArg = args.getGenesisBlock();
    genesisBlockArg.getWitnesses().forEach(key -> {
      final AccountCapsule accountCapsule = new AccountCapsule(ByteString.EMPTY,
          AccountType.AssetIssue,
          ByteString.copyFrom(ByteArray.fromHexString(key.getAddress())),
          Long.valueOf(0));
      final WitnessCapsule witnessCapsule = new WitnessCapsule(
          ByteString.copyFrom(ByteArray.fromHexString(key.getAddress())),
          key.getVoteCount(), key.getUrl());
      witnessCapsule.setIsJobs(true);
      this.accountStore.put(ByteArray.fromHexString(key.getAddress()), accountCapsule);
      this.witnessStore.put(ByteArray.fromHexString(key.getAddress()), witnessCapsule);
      this.wits.add(witnessCapsule);
    });
  }

  public AccountStore getAccountStore() {
    return this.accountStore;
  }

  /**
   * judge balance.
   */
  public void adjustBalance(byte[] accountAddress, long amount)
      throws BalanceInsufficientException {
    AccountCapsule account = getAccountStore().get(accountAddress);
    long balance = account.getBalance();
    if (amount == 0) {
      return;
    }

    if (amount < 0 && balance < -amount) {
      throw new BalanceInsufficientException(accountAddress + " Insufficient");
    }
    account.setBalance(balance + amount);
    this.getAccountStore().put(account.getAddress().toByteArray(), account);
  }

  /**
   * push transaction into db.
   */
  public boolean pushTransactions(final TransactionCapsule trx)
      throws ValidateSignatureException, ContractValidateException, ContractExeException {
    logger.info("push transaction");
    if (!trx.validateSignature()) {
      throw new ValidateSignatureException("trans sig validate failed");
    }

    if (!dialog.valid()) {
      dialog = DialogOptional.of(revokingStore.buildDialog());
    }

    try (RevokingStore.Dialog tmpDialog = revokingStore.buildDialog()) {
      processTransaction(trx);
      pendingTrxs.add(trx);
      tmpDialog.merge();
    } catch (RevokingStoreIllegalStateException e) {
      e.printStackTrace();
    }
    getTransactionStore().dbSource.putData(trx.getTransactionId().getBytes(), trx.getData());
    return true;
  }


  /**
   * save a block.
   */
  public void pushBlock(final BlockCapsule block)
      throws ValidateSignatureException, ContractValidateException, ContractExeException {
    this.khaosDb.push(block);
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

      //todo: In some case it need to switch the branch
    }

    if (block.getNum() != 0) {
      try (Dialog tmpDialog = revokingStore.buildDialog()) {
        this.processBlock(block);
        tmpDialog.commit();
      } catch (RevokingStoreIllegalStateException e) {
        e.printStackTrace();
      }
    }

    this.getBlockStore().dbSource.putData(block.getBlockId().getBytes(), block.getData());
    logger.info("save block, Its ID is " + block.getBlockId() + ", Its num is " + block.getNum());
    this.numHashCache.putData(ByteArray.fromLong(block.getNum()), block.getBlockId().getBytes());
    this.head = this.khaosDb.getHead();
    // blockDbDataSource.putData(blockHash, blockData);
  }


  /**
   * Get the fork branch.
   */
  public ArrayList<BlockId> getBlockChainHashesOnFork(final BlockId forkBlockHash) {
    final Pair<ArrayList<BlockCapsule>, ArrayList<BlockCapsule>> branch =
        this.khaosDb.getBranch(this.head.getBlockId(), forkBlockHash);
    return branch.getValue().stream()
        .map(blockCapsule -> blockCapsule.getBlockId())
        .collect(Collectors.toCollection(ArrayList::new));
  }

  /**
   * judge id.
   *
   * @param blockHash blockHash
   */
  public boolean containBlock(final Sha256Hash blockHash) {
    return this.khaosDb.containBlock(blockHash)
        || this.getBlockStore().dbSource.getData(blockHash.getBytes()) != null;
  }

  public boolean containBlockInMainChain(BlockId blockId) {
    return getBlockStore().dbSource.getData(blockId.getBytes()) != null;
  }

  /**
   * find a block packed data by id.
   */
  public byte[] findBlockByHash(final Sha256Hash hash) {
    return this.khaosDb.containBlock(hash) ? this.khaosDb.getBlock(hash).getData()
        : this.getBlockStore().dbSource.getData(hash.getBytes());
  }

  /**
   * Get a BlockCapsule by id.
   */

  public BlockCapsule getBlockById(final Sha256Hash hash) {
    return this.khaosDb.containBlock(hash) ? this.khaosDb.getBlock(hash)
        : new BlockCapsule(this.getBlockStore().dbSource.getData(hash.getBytes()));
  }

  /**
   * Delete a block.
   */

  public void deleteBlock(final Sha256Hash blockHash) {
    final BlockCapsule block = this.getBlockById(blockHash);
    this.khaosDb.removeBlk(blockHash);
    this.getBlockStore().dbSource.deleteData(blockHash.getBytes());
    this.numHashCache.deleteData(ByteArray.fromLong(block.getNum()));
    this.head = this.khaosDb.getHead();
  }

  /**
   * judge has blocks.
   */
  public boolean hasBlocks() {
    return this.getBlockStore().dbSource.allKeys().size() > 0 || this.khaosDb.hasData();
  }

  /**
   * Process transaction.
   */
  public boolean processTransaction(final TransactionCapsule trxCap)
      throws ValidateSignatureException, ContractValidateException, ContractExeException {

    if (trxCap == null || !trxCap.validateSignature()) {
      return false;
    }
    final List<Actuator> actuatorList = ActuatorFactory.createActuator(trxCap, this);

    for (Actuator act : actuatorList) {
      act.validate();
      act.execute();
    }

    return true;
  }

  /**
   * Get the block id from the number.
   */
  public BlockId getBlockIdByNum(final long num) {
    final byte[] hash = this.numHashCache.getData(ByteArray.fromLong(num));
    return ArrayUtils.isEmpty(hash)
        ? this.genesisBlock.getBlockId()
        : new BlockId(Sha256Hash.wrap(hash), num);
  }

  /**
   * Get number of block by the block id.
   */
  public long getBlockNumById(final Sha256Hash hash) {
    if (this.khaosDb.containBlock(hash)) {
      return this.khaosDb.getBlock(hash).getNum();
    }

    //TODO: optimize here
    final byte[] blockByte = this.getBlockStore().dbSource.getData(hash.getBytes());
    return ArrayUtils.isNotEmpty(blockByte) ? new BlockCapsule(blockByte).getNum() : 0;
  }


  public void initHeadBlock(final Sha256Hash id) {
    this.head = this.getBlockById(id);
  }

  /**
   * Generate a block.
   */
  public BlockCapsule generateBlock(final WitnessCapsule witnessCapsule,
      final long when, final byte[] privateKey)
      throws ValidateSignatureException, ContractValidateException, ContractExeException {

    final long timestamp = this.dynamicPropertiesStore.getLatestBlockHeaderTimestamp();
    final long number = this.dynamicPropertiesStore.getLatestBlockHeaderNumber();
    final ByteString preHash = this.dynamicPropertiesStore.getLatestBlockHeaderHash();

    // judge create block time
    if (when < timestamp) {
      throw new IllegalArgumentException("generate block timestamp is invalid.");
    }

    long currentTrxSize = 0;
    long postponedTrxCount = 0;

    final BlockCapsule blockCapsule = new BlockCapsule(number + 1, preHash, when,
        witnessCapsule.getAddress());

    dialog.reset();
    dialog = DialogOptional.of(revokingStore.buildDialog());

    Iterator iterator = pendingTrxs.iterator();
    while (iterator.hasNext()) {
      TransactionCapsule trx = (TransactionCapsule) iterator.next();
      currentTrxSize += RamUsageEstimator.sizeOf(trx);
      // judge block size
      if (currentTrxSize > TRXS_SIZE) {
        postponedTrxCount++;
        continue;
      }
      // apply transaction
      try (Dialog tmpDialog = revokingStore.buildDialog()) {
        processTransaction(trx);
        tmpDialog.merge();
        // push into block
        blockCapsule.addTransaction(trx);
        iterator.remove();
      } catch (ContractExeException e) {
        logger.info("contract not processed during execute");
        e.printStackTrace();
      } catch (ContractValidateException e) {
        logger.info("contract not processed during validate");
        e.printStackTrace();
      } catch (RevokingStoreIllegalStateException e) {
        e.printStackTrace();
      }
    }

    dialog.reset();

    if (postponedTrxCount > 0) {
      logger.info("{} transactions over the block size limit", postponedTrxCount);
    }

    logger.info("postponedTrxCount[" + postponedTrxCount + "],TrxLeft[" + pendingTrxs.size() + "]");

    blockCapsule.setMerklerRoot();
    blockCapsule.sign(privateKey);
    blockCapsule.generatedByMyself = true;
    this.pushBlock(blockCapsule);
    this.dynamicPropertiesStore
        .saveLatestBlockHeaderHash(blockCapsule.getBlockId().getByteString());
    this.dynamicPropertiesStore.saveLatestBlockHeaderNumber(blockCapsule.getNum());
    this.dynamicPropertiesStore.saveLatestBlockHeaderTimestamp(blockCapsule.getTimeStamp());
    return blockCapsule;
  }

  private void setAccountStore(final AccountStore accountStore) {
    this.accountStore = accountStore;
  }

  public TransactionStore getTransactionStore() {
    return this.transactionStore;
  }

  private void setTransactionStore(final TransactionStore transactionStore) {
    this.transactionStore = transactionStore;
  }

  public BlockStore getBlockStore() {
    return this.blockStore;
  }

  private void setBlockStore(final BlockStore blockStore) {
    this.blockStore = blockStore;
  }

  public UtxoStore getUtxoStore() {
    return this.utxoStore;
  }

  private void setUtxoStore(final UtxoStore utxoStore) {
    this.utxoStore = utxoStore;
  }

  /**
   * process block.
   */
  public void processBlock(BlockCapsule block)
      throws ValidateSignatureException, ContractValidateException, ContractExeException {
    for (TransactionCapsule transactionCapsule : block.getTransactions()) {
      processTransaction(transactionCapsule);
      this.updateDynamicProperties(block);
      this.updateSignedWitness(block);
      if (this.dynamicPropertiesStore.getNextMaintenanceTime().getMillis() <= block
          .getTimeStamp()) {
        this.processMaintenance();
      }
    }
  }

  private void updateDynamicProperties(final BlockCapsule block) {

  }

  private void processMaintenance() {
    this.updateWitness();
    this.dynamicPropertiesStore.updateMaintenanceTime();
  }


  /**
   * update signed witness.
   */
  public void updateSignedWitness(BlockCapsule block) {
    //TODO: add verification
    WitnessCapsule witnessCapsule = witnessStore
        .get(block.getInstance().getBlockHeader().getRawData().getWitnessAddress().toByteArray());

    long latestSlotNum = 0L;

    //    dynamicPropertiesStore.current_aslot + getSlotAtTime(new DateTime(block.getTimeStamp()));

    witnessCapsule.getInstance().toBuilder().setLatestBlockNum(block.getNum())
        .setLatestSlotNum(latestSlotNum)
        .build();

    processFee();
  }

  private void processFee() {

  }

  private long blockInterval() {
    return LOOP_INTERVAL; // millisecond todo getFromDb
  }

  /**
   * get slot at time.
   */
  public long getSlotAtTime(DateTime when) {
    DateTime firstSlotTime = getSlotTime(1);
    if (when.isBefore(firstSlotTime)) {
      return 0;
    }
    return (when.getMillis() - firstSlotTime.getMillis()) / blockInterval() + 1;
  }


  /**
   * get slot time.
   */
  public DateTime getSlotTime(long slotNum) {
    if (slotNum == 0) {
      return DateTime.now();
    }
    long interval = blockInterval();
    BlockStore blockStore = getBlockStore();
    DateTime genesisTime = blockStore.getGenesisTime();
    if (blockStore.getHeadBlockNum() == 0) {
      return genesisTime.plus(slotNum * interval);
    }

    if (lastHeadBlockIsMaintenance()) {
      slotNum += getSkipSlotInMaintenance();
    }

    DateTime headSlotTime = blockStore.getHeadBlockTime();

    //align slot time
    headSlotTime = headSlotTime
        .minus((headSlotTime.getMillis() - genesisTime.getMillis()) % interval);

    return headSlotTime.plus(interval * slotNum);
  }


  private boolean lastHeadBlockIsMaintenance() {
    return getDynamicPropertiesStore().getStateFlag() == 1;
  }

  private long getSkipSlotInMaintenance() {
    return 0;
  }

  /**
   * update witness.
   */
  public void updateWitness() {
    final Map<ByteString, Long> countWitness = Maps.newHashMap();
    final List<AccountCapsule> accountList = this.accountStore.getAllAccounts();
    logger.info("there is account List size is {}", accountList.size());
    accountList.forEach(account -> {
      logger.info("there is account ,account address is {}",
          ByteArray.toHexString(account.getAddress().toByteArray()));

      Optional<Long> sum = account.getVotesList().stream().map(vote -> vote.getVoteCount())
          .reduce((a, b) -> a + b);
      if (sum.isPresent()) {
        if (sum.get() <= account.getShare()) {
          account.getVotesList().forEach(vote -> {
            //TODO validate witness //active_witness
            ByteString voteAddress = vote.getVoteAddress();
            long voteCount = vote.getVoteCount();
            if (countWitness.containsKey(voteAddress)) {
              countWitness.put(voteAddress, countWitness.get(voteAddress) + voteCount);
            } else {
              countWitness.put(voteAddress, voteCount);
            }
          });
        } else {
          logger.info(
              "account" + account.getAddress() + ",share[" + account.getShare() + "] > voteSum["
                  + sum.get() + "]");
        }
      }
    });

    witnessStore.getAllWitnesses().forEach(witnessCapsule -> {
      witnessCapsule.setVoteCount(0);
      witnessCapsule.setIsJobs(false);
      this.witnessStore.putWitness(witnessCapsule);
    });
    final List<WitnessCapsule> witnessCapsuleList = Lists.newArrayList();
    logger.info("countWitnessMap size is {}", countWitness.keySet().size());
    countWitness.forEach((address, voteCount) -> {
      final WitnessCapsule witnessCapsule = this.witnessStore.getWitness(address);
      if (null == witnessCapsule) {
        logger.warn("witnessCapsule is null.address is {}", address);
        return;
      }

      ByteString witnessAddress = witnessCapsule.getInstance().getAddress();
      AccountCapsule witnessAccountCapsule = accountStore.get(witnessAddress.toByteArray());
      if (witnessAccountCapsule == null) {
        logger.warn("witnessAccount[" + witnessAddress + "] not exists");
      }

      if (witnessAccountCapsule.getBalance() < WitnessCapsule.MIN_BALANCE) {
        logger.warn("witnessAccount[" + witnessAddress + "] has balance[" + witnessAccountCapsule
            .getBalance() + "] < MIN_BALANCE[" + WitnessCapsule.MIN_BALANCE + "]");
      }

      witnessCapsule.setVoteCount(witnessCapsule.getVoteCount() + voteCount);
      witnessCapsule.setIsJobs(false);
      witnessCapsuleList.add(witnessCapsule);
      this.witnessStore.putWitness(witnessCapsule);
      logger.info("address is {}  ,countVote is {}", witnessCapsule.getAddress().toStringUtf8(),
          witnessCapsule.getVoteCount());
    });
    witnessCapsuleList.sort((a, b) -> (int) (a.getVoteCount() - b.getVoteCount()));
    if (this.wits.size() > MAX_ACTIVE_WITNESS_NUM) {
      this.wits = witnessCapsuleList.subList(0, MAX_ACTIVE_WITNESS_NUM);
    }

    witnessCapsuleList.forEach(witnessCapsule -> {
      witnessCapsule.setIsJobs(true);
      this.witnessStore.putWitness(witnessCapsule);
    });
  }

  public void updateWits() {
    wits.clear();
    witnessStore.getAllWitnesses().forEach(witnessCapsule -> {
      if (witnessCapsule.getIsJobs()) {
        wits.add(witnessCapsule);
      }
    });
  }

  public AssetIssueStore getAssetIssueStore() {
    return assetIssueStore;
  }

  public void setAssetIssueStore(AssetIssueStore assetIssueStore) {
    this.assetIssueStore = assetIssueStore;
  }

  /**
   * Close database.
   */
  public void close() {
    if (this.accountStore != null) {
      this.accountStore.close();
    }

    if (this.transactionStore != null) {
      this.transactionStore.close();
    }

    if (this.blockStore != null) {
      this.blockStore.close();
    }

    if (this.utxoStore != null) {
      this.utxoStore.close();
    }

    if (this.witnessStore != null) {
      this.witnessStore.close();
    }

    if (this.assetIssueStore != null) {
      this.assetIssueStore.close();
    }

    if (this.dynamicPropertiesStore != null) {
      this.dynamicPropertiesStore.close();
    }

    if (this.numHashCache != null) {
      this.numHashCache.closeDB();
    }
  }
}
