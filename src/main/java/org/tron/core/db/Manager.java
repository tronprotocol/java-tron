package org.tron.core.db;

import static org.tron.core.config.Parameter.ChainConstant.SOLIDIFIED_THRESHOLD;
import static org.tron.protos.Protocol.Transaction.Contract.ContractType.TransferAssetContract;
import static org.tron.protos.Protocol.Transaction.Contract.ContractType.TransferContract;

import com.carrotsearch.sizeof.RamUsageEstimator;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import javafx.util.Pair;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.crypto.ECKey;
import org.tron.common.overlay.discover.Node;
import org.tron.common.storage.leveldb.LevelDbDataSourceImpl;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.DialogOptional;
import org.tron.common.utils.RandomGenerator;
import org.tron.common.utils.Sha256Hash;
import org.tron.common.utils.Time;
import org.tron.core.actuator.Actuator;
import org.tron.core.actuator.ActuatorFactory;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.capsule.utils.BlockUtil;
import org.tron.core.config.args.Args;
import org.tron.core.config.args.GenesisBlock;
import org.tron.core.db.AbstractRevokingStore.Dialog;
import org.tron.core.exception.BalanceInsufficientException;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.HighFreqException;
import org.tron.core.exception.RevokingStoreIllegalStateException;
import org.tron.core.exception.UnLinkedBlockException;
import org.tron.core.exception.ValidateSignatureException;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Transaction;

@Slf4j
@Component
public class Manager {

  private static final long BLOCK_INTERVAL_SEC = 1;
  private static final int MAX_ACTIVE_WITNESS_NUM = 21;
  private static final long TRXS_SIZE = 2_000_000; // < 2MiB
  public static final long LOOP_INTERVAL = 5000L; // ms,produce block period, must be divisible by 60. millisecond

  private AccountStore accountStore;
  private TransactionStore transactionStore;
  private BlockStore blockStore;
  private UtxoStore utxoStore;
  private WitnessStore witnessStore;
  private AssetIssueStore assetIssueStore;
  private DynamicPropertiesStore dynamicPropertiesStore;

  @Autowired
  private PeersStore peersStore;
  private BlockCapsule genesisBlock;


  private LevelDbDataSourceImpl numHashCache;
  private KhaosDatabase khaosDb;
  @Getter
  private BlockCapsule head;
  private RevokingDatabase revokingStore;
  private DialogOptional<Dialog> dialog = DialogOptional.empty();

  @Getter
  @Setter
  private boolean isSyncMode;

  @Getter
  @Setter
  protected List<WitnessCapsule> shuffledWitnessStates;


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

  volatile private List<WitnessCapsule> wits = new ArrayList<>();
  private ReadWriteLock witsLock = new ReentrantReadWriteLock();
  private Lock witsRead = witsLock.readLock();
  private Lock witsWrite = witsLock.writeLock();

  // witness

  public List<WitnessCapsule> getWitnesses() {
    witsRead.lock();
    try {
      return this.wits;
    } finally {
      witsRead.unlock();
    }

  }

  public void setWitnesses(List<WitnessCapsule> wits) {
    witsWrite.lock();
    try {
      this.wits = wits;
    } finally {
      witsWrite.unlock();
    }
  }

  public void addWitness(final WitnessCapsule witnessCapsule) {
    witsWrite.lock();
    try {
      this.wits.add(witnessCapsule);
    } finally {
      witsWrite.unlock();
    }
  }

  public BlockId getHeadBlockId() {
    return head.getBlockId();
    //return Sha256Hash.wrap(this.dynamicPropertiesStore.getLatestBlockHeaderHash());
  }

  public long getHeadBlockNum() {
    return this.head.getNum();
  }

  public long getHeadBlockTimeStamp() {
    return this.head.getTimeStamp();
  }


  /**
   * get ScheduledWitness by slot.
   */
  public ByteString getScheduledWitness(final long slot) {

    final long currentSlot = getHeadSlot() + slot;

    if (currentSlot < 0) {
      throw new RuntimeException("currentSlot should be positive.");
    }
    final List<WitnessCapsule> currentShuffledWitnesses = this.getShuffledWitnessStates();
    if (CollectionUtils.isEmpty(currentShuffledWitnesses)) {
      throw new RuntimeException("ShuffledWitnesses is null.");
    }
    final int witnessIndex = (int) currentSlot % currentShuffledWitnesses.size();

    final ByteString scheduledWitness = currentShuffledWitnesses.get(witnessIndex).getAddress();
    //logger.info("scheduled_witness:" + scheduledWitness.toStringUtf8() + ",slot:" + currentSlot);

    return scheduledWitness;
  }

  private long getHeadSlot() {
    return (head.getTimeStamp() - genesisBlock.getTimeStamp()) / blockInterval();
  }

  public int calculateParticipationRate() {
    return 100 * this.dynamicPropertiesStore.getBlockFilledSlots().calculateFilledSlotsCount()
        / BlockFilledSlots.SLOT_NUMBER;
  }

  public PeersStore getPeersStore() {
    return peersStore;
  }

  public void setPeersStore(PeersStore peersStore) {
    this.peersStore = peersStore;
  }

  public Node getHomeNode() {
    final Args args = Args.getInstance();
    Set<Node> nodes = this.peersStore.get("home".getBytes());
    if (nodes.size() > 0) {
      return nodes.stream().findFirst().get();
    } else {
      Node node = new Node(new ECKey().getNodeId(), args.getNodeExternalIp(),
          args.getNodeListenPort());
      nodes.add(node);
      this.peersStore.put("home".getBytes(), nodes);
      return node;
    }
  }

  public void clearAndWriteNeighbours(Set<Node> nodes) {
    this.peersStore.put("neighbours".getBytes(), nodes);
  }

  public Set<Node> readNeighbours() {
    return this.peersStore.get("neighbours".getBytes());
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
    } catch (UnLinkedBlockException e) {
      logger.error(e.getMessage());
      System.exit(-1);
    }
    this.updateWits();
    this.setShuffledWitnessStates(getWitnesses());
    this.initHeadBlock(Sha256Hash.wrap(this.dynamicPropertiesStore.getLatestBlockHeaderHash()));
    this.khaosDb.start(head);
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
      throws ContractValidateException, ContractExeException,
      ValidateSignatureException, UnLinkedBlockException {
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

        //this.pushBlock(this.genesisBlock);
        blockStore.put(this.genesisBlock.getBlockId().getBytes(), this.genesisBlock);
        this.numHashCache.putData(ByteArray.fromLong(this.genesisBlock.getNum()),
            this.genesisBlock.getBlockId().getBytes());
        //refreshHead(newBlock);
        logger.info("save block: " + this.genesisBlock);

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
          ByteString.copyFrom(account.getAddressBytes()),
          account.getAccountType(),
          account.getBalance());
      this.accountStore.put(account.getAddressBytes(), accountCapsule);
    });
  }

  /**
   * save witnesses into database.
   */
  private void initWitness() {
    final Args args = Args.getInstance();
    final GenesisBlock genesisBlockArg = args.getGenesisBlock();
    genesisBlockArg.getWitnesses().forEach(key -> {
      byte[] keyAddress = ByteArray.fromHexString(key.getAddress());
      ByteString address = ByteString.copyFrom(keyAddress);

      if (!this.accountStore.has(keyAddress)) {
        final AccountCapsule accountCapsule = new AccountCapsule(
            ByteString.EMPTY, address, AccountType.AssetIssue, 0L);
        this.accountStore.put(keyAddress, accountCapsule);
      }

      final WitnessCapsule witnessCapsule = new WitnessCapsule(
          address, key.getVoteCount(), key.getUrl());
      witnessCapsule.setIsJobs(true);
      this.witnessStore.put(keyAddress, witnessCapsule);

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
  public synchronized boolean pushTransactions(final TransactionCapsule trx)
      throws ValidateSignatureException, ContractValidateException,
      ContractExeException, HighFreqException {
    logger.info("push transaction");
    if (!trx.validateSignature()) {
      throw new ValidateSignatureException("trans sig validate failed");
    }

    validateFreq(trx);

    if (!dialog.valid()) {
      dialog = DialogOptional.of(revokingStore.buildDialog());
    }

    try (
        RevokingStore.Dialog tmpDialog = revokingStore.buildDialog()) {
      processTransaction(trx);
      pendingTrxs.add(trx);

      tmpDialog.merge();
    } catch (RevokingStoreIllegalStateException e) {
      e.printStackTrace();
    }
    return true;
  }

  void validateFreq(TransactionCapsule trx) throws HighFreqException {
    List<org.tron.protos.Protocol.Transaction.Contract> contracts = trx.getInstance().getRawData()
        .getContractList();
    for (Transaction.Contract contract : contracts) {
      if (contract.getType() == TransferContract
          || contract.getType() == TransferAssetContract) {
        byte[] address = TransactionCapsule.getOwner(contract);
        AccountCapsule accountCapsule = this.getAccountStore().get(address);
        long balacne = accountCapsule.getBalance();
        long latestOperationTime = accountCapsule.getLatestOperationTime();
        int latstTransNumberInBlock = this.head.getTransactions().size();
        doValidateFreq(balacne, latstTransNumberInBlock, latestOperationTime);
      }
    }
  }

  void doValidateFreq(long balance, int transNumber, long latestOperationTime)
      throws HighFreqException {
    long now = Time.getCurrentMillis();
    // todo: avoid ddos, design more smoothly formula later.
    if (balance < 1000000 * 1000) {
      if (now - latestOperationTime < 5 * 60 * 1000) {
        throw new HighFreqException("try later");
      }
    }
  }

  /**
   * when switch fork need erase blocks on fork branch.
   */
  public void eraseBlock() {
    dialog.reset();
    BlockCapsule oldHeadBlock = getBlockStore().get(head.getBlockId().getBytes());
    try {
      revokingStore.pop();
      head = getBlockStore().get(getBlockIdByNum(oldHeadBlock.getNum() - 1).getBytes());
    } catch (RevokingStoreIllegalStateException e) {
      e.printStackTrace();
    }
    khaosDb.pop();
    // todo process the trans in the poped block.

  }

  private void switchFork(BlockCapsule newHead) {
    Pair<LinkedList<BlockCapsule>, LinkedList<BlockCapsule>> binaryTree = khaosDb
        .getBranch(newHead.getBlockId(), head.getBlockId());

    if (CollectionUtils.isNotEmpty(binaryTree.getValue())) {
      while (!head.getBlockId().equals(binaryTree.getValue().peekLast().getParentHash())) {
        eraseBlock();
      }
    }

    if (CollectionUtils.isNotEmpty(binaryTree.getKey())) {
      LinkedList<BlockCapsule> branch = binaryTree.getKey();
      Collections.reverse(branch);
      branch.forEach(item -> {
        // todo  process the exception carefully later
        try (Dialog tmpDialog = revokingStore.buildDialog()) {
          processBlock(item);
          blockStore.put(item.getBlockId().getBytes(), item);
          this.numHashCache
              .putData(ByteArray.fromLong(item.getNum()), item.getBlockId().getBytes());
          tmpDialog.commit();
          head = item;
        } catch (ValidateSignatureException e) {
          e.printStackTrace();
        } catch (ContractValidateException e) {
          e.printStackTrace();
        } catch (ContractExeException e) {
          e.printStackTrace();
        } catch (RevokingStoreIllegalStateException e) {
          e.printStackTrace();
        }
      });
      return;
    }
  }

  //TODO: if error need to rollback.


  /**
   * validate witness schedule.
   */
  private boolean validateWitnessSchedule(BlockCapsule block) {

    ByteString witnessAddress = block.getInstance().getBlockHeader().getRawData()
        .getWitnessAddress();
    //to deal with other condition later
    if (head.getNum() != 0 && head.getBlockId().equals(block.getParentHash())) {
      long slot = getSlotAtTime(block.getTimeStamp());
      final ByteString scheduledWitness = getScheduledWitness(slot);
      if (!scheduledWitness.equals(witnessAddress)) {
        logger.warn(
            "Witness is out of order, scheduledWitness[{}],blockWitnessAddress[{}],blockTimeStamp[{}],slot[{}]",
            ByteArray.toHexString(scheduledWitness.toByteArray()),
            ByteArray.toHexString(witnessAddress.toByteArray()), new DateTime(block.getTimeStamp()),
            slot);
        return false;
      }
    }

    logger.debug("Validate witnessSchedule successfully,scheduledWitness:{}",
        ByteArray.toHexString(witnessAddress.toByteArray()));
    return true;
  }

  private synchronized void filterPendingTrx(List<TransactionCapsule> listTrx) {

  }

  /**
   * save a block.
   */
  public void pushBlock(final BlockCapsule block)
      throws ValidateSignatureException, ContractValidateException,
      ContractExeException, UnLinkedBlockException {

    List<TransactionCapsule> pendingTrxsTmp = new ArrayList<>(pendingTrxs);
    //TODO: optimize performance here.
    pendingTrxs.clear();
    dialog.reset();

    //todo: check block's validity
    if (!block.generatedByMyself) {
      if (!block.validateSignature()) {
        logger.info("The siganature is not validated.");
        //TODO: throw exception here.
        return;
      }

      if (!block.calcMerkleRoot().equals(block.getMerkleRoot())) {
        logger.info("The merkler root doesn't match, Calc result is " + block.calcMerkleRoot()
            + " , the headers is " + block.getMerkleRoot());
        //TODO: throw exception here.
        return;
      }
    }

    try {
      validateWitnessSchedule(block); // direct return ,need test
    } catch (Exception ex) {
      logger.error("validateWitnessSchedule error", ex);
    }

    BlockCapsule newBlock = this.khaosDb.push(block);
    //DB don't need lower block
    if (head == null) {
      if (newBlock.getNum() != 0) {
        return;
      }
    } else {
      if (newBlock.getNum() <= head.getNum()) {
        return;
      }
      //switch fork
      if (!newBlock.getParentHash().equals(head.getBlockId())) {
        switchFork(newBlock);
      }

      try (Dialog tmpDialog = revokingStore.buildDialog()) {
        this.processBlock(newBlock);
        tmpDialog.commit();
      } catch (RevokingStoreIllegalStateException e) {
        e.printStackTrace();
      }
    }

    //filter trxs
    pendingTrxsTmp.stream()
        .filter(trx -> transactionStore.get(trx.getTransactionId().getBytes()) == null)
        .forEach(trx -> {
          try {
            pushTransactions(trx);
          } catch (ValidateSignatureException e) {
            e.printStackTrace();
          } catch (ContractValidateException e) {
            e.printStackTrace();
          } catch (ContractExeException e) {
            e.printStackTrace();
          } catch (HighFreqException e) {
            e.printStackTrace();
          }
        });

    blockStore.put(block.getBlockId().getBytes(), block);
    this.numHashCache.putData(ByteArray.fromLong(block.getNum()), block.getBlockId().getBytes());
    //refreshHead(newBlock);
    logger.info("save block: " + newBlock);
  }

  public void updateDynamicProperties(BlockCapsule block) {
    this.head = block;
    this.dynamicPropertiesStore
        .saveLatestBlockHeaderHash(block.getBlockId().getByteString());
    this.dynamicPropertiesStore.saveLatestBlockHeaderNumber(block.getNum());
    this.dynamicPropertiesStore.saveLatestBlockHeaderTimestamp(block.getTimeStamp());
    updateWitnessSchedule();
  }

  @Deprecated
  private void refreshHead(BlockCapsule block) {
    this.head = block;
    this.dynamicPropertiesStore
        .saveLatestBlockHeaderHash(block.getBlockId().getByteString());
    this.dynamicPropertiesStore.saveLatestBlockHeaderNumber(block.getNum());
    this.dynamicPropertiesStore.saveLatestBlockHeaderTimestamp(block.getTimeStamp());
    updateWitnessSchedule();
  }


  /**
   * Get the fork branch.
   */
  public LinkedList<BlockId> getBlockChainHashesOnFork(final BlockId forkBlockHash) {
    final Pair<LinkedList<BlockCapsule>, LinkedList<BlockCapsule>> branch =
        this.khaosDb.getBranch(this.head.getBlockId(), forkBlockHash);
    return branch.getValue().stream()
        .map(blockCapsule -> blockCapsule.getBlockId())
        .collect(Collectors.toCollection(LinkedList::new));
  }

  /**
   * judge id.
   *
   * @param blockHash blockHash
   */
  public boolean containBlock(final Sha256Hash blockHash) {
    return this.khaosDb.containBlock(blockHash)
        || blockStore.get(blockHash.getBytes()) != null;
  }

  public boolean containBlockInMainChain(BlockId blockId) {
    return blockStore.get(blockId.getBytes()) != null;
  }

  /**
   * find a block packed data by id.
   */
  public byte[] findBlockByHash(final Sha256Hash hash) {
    return this.khaosDb.containBlock(hash) ? this.khaosDb.getBlock(hash).getData()
        : blockStore.get(hash.getBytes()).getData();
  }

  /**
   * Get a BlockCapsule by id.
   */

  public BlockCapsule getBlockById(final Sha256Hash hash) {
    return this.khaosDb.containBlock(hash) ? this.khaosDb.getBlock(hash)
        : blockStore.get(hash.getBytes());
  }

  /**
   * Delete a block.
   */

  public void deleteBlock(final Sha256Hash blockHash) {
    final BlockCapsule block = this.getBlockById(blockHash);
    this.khaosDb.removeBlk(blockHash);
    blockStore.delete(blockHash.getBytes());
    this.numHashCache.deleteData(ByteArray.fromLong(block.getNum()));
    this.head = this.khaosDb.getHead();
  }

  /**
   * judge has blocks.
   */
  public boolean hasBlocks() {
    return blockStore.dbSource.allKeys().size() > 0 || this.khaosDb.hasData();
  }

  /**
   * Process transaction.
   */
  public boolean processTransaction(final TransactionCapsule trxCap)
      throws ValidateSignatureException, ContractValidateException, ContractExeException {

    TransactionResultCapsule transRet;
    if (trxCap == null || !trxCap.validateSignature()) {
      return false;
    }
    final List<Actuator> actuatorList = ActuatorFactory.createActuator(trxCap, this);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    for (Actuator act : actuatorList) {

      act.validate();
      act.execute(ret);
      trxCap.setResult(ret);
    }
    transactionStore.put(trxCap.getTransactionId().getBytes(), trxCap);
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
    final byte[] blockByte = blockStore.get(hash.getBytes()).getData();
    return ArrayUtils.isNotEmpty(blockByte) ? new BlockCapsule(blockByte).getNum() : 0;
  }


  public void initHeadBlock(final Sha256Hash id) {
    this.head = this.getBlockById(id);
  }

  /**
   * Generate a block.
   */
  public synchronized BlockCapsule generateBlock(final WitnessCapsule witnessCapsule,
      final long when, final byte[] privateKey)
      throws ValidateSignatureException, ContractValidateException,
      ContractExeException, UnLinkedBlockException {

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

    blockCapsule.setMerkleRoot();
    blockCapsule.sign(privateKey);
    blockCapsule.generatedByMyself = true;
    this.pushBlock(blockCapsule);
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
    }

    // todo set reverking db max size.
    this.updateDynamicProperties(block);
    this.updateSignedWitness(block);
    this.updateLatestSolidifiedBlock();

    if (needMaintenance(block.getTimeStamp())) {
      if (block.getNum() == 1) {
        this.dynamicPropertiesStore.updateNextMaintenanceTime(block.getTimeStamp());
      } else {
        this.processMaintenance(block);
      }
    }

  }

  /**
   * update the latest solidified block.
   */
  public void updateLatestSolidifiedBlock() {
    List<Long> numbers = getWitnesses().stream()
        .map(wit -> wit.getLatestBlockNum())
        .sorted()
        .collect(Collectors.toList());

    int solidifiedPosition = (int) (wits.size() * (1 - SOLIDIFIED_THRESHOLD)) - 1;
    if (solidifiedPosition < 0) {
      logger.warn("updateLatestSolidifiedBlock error,solidifiedPosition:{},wits.size:{}",
          solidifiedPosition, wits.size());
      return;
    }

    long latestSolidifiedBlockNum = numbers.get(solidifiedPosition);

    getDynamicPropertiesStore().saveLatestSolidifiedBlockNum(latestSolidifiedBlockNum);
  }

  /**
   * Determine if the current time is maintenance time.
   */
  public boolean needMaintenance(long blockTime) {
    return this.dynamicPropertiesStore.getNextMaintenanceTime().getMillis() <= blockTime;
  }

  /**
   * Perform maintenance.
   */
  private void processMaintenance(BlockCapsule block) {
    this.updateWitness();
    this.dynamicPropertiesStore.updateNextMaintenanceTime(block.getTimeStamp());
  }

  /**
   * @param block the block update signed witness.  set witness who signed block the 1. the latest
   * block num 2. pay the trx to witness. 3. (TODO)the latest slot num.
   */

  public void updateSignedWitness(BlockCapsule block) {
    //TODO: add verification
    WitnessCapsule witnessCapsule = witnessStore
        .get(block.getInstance().getBlockHeader().getRawData().getWitnessAddress().toByteArray());
    long latestSlotNum = 0;
    witnessCapsule.getInstance().toBuilder().setLatestBlockNum(block.getNum())
        .setLatestSlotNum(latestSlotNum)
        .build();
    AccountCapsule sun = accountStore.getSun();

    try {
      adjustBalance(sun.getAddress().toByteArray(), -3);
    } catch (BalanceInsufficientException e) {

    }
    try {
      adjustBalance(witnessCapsule.getAddress().toByteArray(), 3);
    } catch (BalanceInsufficientException e) {
      e.printStackTrace();
    }

  }

  private long blockInterval() {
    return LOOP_INTERVAL; // millisecond todo getFromDb
  }

  /**
   * get slot at time.
   */
  public long getSlotAtTime(long when) {
    long firstSlotTime = getSlotTime(1);
    if (when < firstSlotTime) {
      return 0;
    }
    logger
        .debug("nextFirstSlotTime:[{}],when[{}]", new DateTime(firstSlotTime), new DateTime(when));
    return (when - firstSlotTime) / blockInterval() + 1;
  }

  /**
   * get absolute Slot At Time
   */
  public long getAbSlotAtTime(long when) {
    return (when - getGenesisBlock().getTimeStamp()) / blockInterval();
  }

  /**
   * get slot time.
   */
  public long getSlotTime(long slotNum) {
    if (slotNum == 0) {
      return Time.getCurrentMillis();
    }
    long interval = blockInterval();

    if (getHeadBlockNum() == 0) {
      return getGenesisBlock().getTimeStamp() + slotNum * interval;
    }

    if (lastHeadBlockIsMaintenance()) {
      slotNum += getSkipSlotInMaintenance();
    }

    long headSlotTime = getHeadBlockTimestamp();
    headSlotTime = headSlotTime
        - ((headSlotTime - getGenesisBlock().getTimeStamp()) % interval);

    return headSlotTime + interval * slotNum;
  }

  private boolean lastHeadBlockIsMaintenance() {
    return getDynamicPropertiesStore().getStateFlag() == 1;
  }

  private long getHeadBlockTimestamp() {
    return head.getTimeStamp();
  }


  // To be added
  private long getSkipSlotInMaintenance() {
    return 0;
  }

  /**
   * update witness.
   */
  public void updateWitness() {
    List<WitnessCapsule> currentWits = getWitnesses();

    final Map<ByteString, Long> countWitness = Maps.newHashMap();
    final List<AccountCapsule> accountList = this.accountStore.getAllAccounts();
    logger.info("there is account List size is {}", accountList.size());
    accountList.forEach(account -> {
      logger.info("there is account ,account address is {}",
          account.createReadableString());

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
              "account" + account.createReadableString() + ",share[" + account.getShare()
                  + "] > voteSum["
                  + sum.get() + "]");
        }
      }
    });

    witnessStore.getAllWitnesses().forEach(witnessCapsule -> {
      witnessCapsule.setVoteCount(0);
      witnessCapsule.setIsJobs(false);
      this.witnessStore.put(witnessCapsule.createDbKey(), witnessCapsule);
    });
    final List<WitnessCapsule> witnessCapsuleList = Lists.newArrayList();
    logger.info("countWitnessMap size is {}", countWitness.keySet().size());

    //Only possible during the initialization phase
    if (countWitness.size() == 0) {
      witnessCapsuleList.addAll(this.witnessStore.getAllWitnesses());
    }

    countWitness.forEach((address, voteCount) -> {
      final WitnessCapsule witnessCapsule = this.witnessStore.get(createDbKey(address));
      if (null == witnessCapsule) {
        logger.warn("witnessCapsule is null.address is {}", createReadableString(address));
        return;
      }

      ByteString witnessAddress = witnessCapsule.getInstance().getAddress();
      AccountCapsule witnessAccountCapsule = accountStore.get(createDbKey(witnessAddress));
      if (witnessAccountCapsule == null) {
        logger.warn("witnessAccount[" + createReadableString(witnessAddress) + "] not exists");
      } else {
        if (witnessAccountCapsule.getBalance() < WitnessCapsule.MIN_BALANCE) {
          logger.warn("witnessAccount[" + createReadableString(witnessAddress) + "] has balance["
              + witnessAccountCapsule
              .getBalance() + "] < MIN_BALANCE[" + WitnessCapsule.MIN_BALANCE + "]");
        } else {
          witnessCapsule.setVoteCount(witnessCapsule.getVoteCount() + voteCount);
          witnessCapsule.setIsJobs(false);
          witnessCapsuleList.add(witnessCapsule);
          this.witnessStore.put(witnessCapsule.createDbKey(), witnessCapsule);
          logger.info("address is {}  ,countVote is {}", witnessCapsule.createReadableString(),
              witnessCapsule.getVoteCount());
        }
      }
    });
    sortWitness(witnessCapsuleList);
    if (witnessCapsuleList.size() > MAX_ACTIVE_WITNESS_NUM) {
      setWitnesses(witnessCapsuleList.subList(0, MAX_ACTIVE_WITNESS_NUM));
    } else {
      setWitnesses(witnessCapsuleList);
    }

    getWitnesses().forEach(witnessCapsule -> {
      witnessCapsule.setIsJobs(true);
      this.witnessStore.put(witnessCapsule.createDbKey(), witnessCapsule);
    });

    logger.info(
        "updateWitness,before:{} ",
        getWitnessStringList(currentWits) + ",\nafter:{} " + getWitnessStringList(
            getWitnesses()));
  }

  private byte[] createDbKey(ByteString string) {
    return string.toByteArray();
  }

  public String createReadableString(ByteString string) {
    return ByteArray.toHexString(string.toByteArray());
  }

  /**
   * update wits sync to store.
   */
  public void updateWits() {
    getWitnesses().clear();
    witnessStore.getAllWitnesses().forEach(witnessCapsule -> {
      if (witnessCapsule.getIsJobs()) {
        addWitness(witnessCapsule);
      }
    });
    sortWitness();
  }

  private void sortWitness() {
    witsWrite.lock();
    try {
      sortWitness(wits);
    } finally {
      witsWrite.unlock();
    }

  }

  private void sortWitness(List<WitnessCapsule> list) {
    list.sort((a, b) -> {
      if (b.getVoteCount() != a.getVoteCount()) {
        return (int) (b.getVoteCount() - a.getVoteCount());
      } else {
        return Long.compare(b.getAddress().hashCode(),a.getAddress().hashCode());
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
   * shuffle witnesses
   */
  private void updateWitnessSchedule() {
    if (CollectionUtils.isEmpty(getWitnesses())) {
      throw new RuntimeException("Witnesses is empty");
    }

    List<String> currentWitsAddress = getWitnessStringList(getWitnesses());
    // TODO  what if the number of witness is not same in different slot.
    if (getHeadBlockNum() != 0 && getHeadBlockNum() % getWitnesses().size() == 0) {
      logger.info("updateWitnessSchedule number:{},HeadBlockTimeStamp:{}", getHeadBlockNum(),
          getHeadBlockTimeStamp());
      setShuffledWitnessStates(new RandomGenerator<WitnessCapsule>()
          .shuffle(getWitnesses(), getHeadBlockTimeStamp()));

      logger.info(
          "updateWitnessSchedule,before:{} ", currentWitsAddress
              + ",\nafter:{} " + getWitnessStringList(getShuffledWitnessStates()));
    }
  }

  private List<String> getWitnessStringList(List<WitnessCapsule> witnessStates) {
    return witnessStates.stream()
        .map(witnessCapsule -> ByteArray.toHexString(witnessCapsule.getAddress().toByteArray()))
        .collect(Collectors.toList());
  }
}
