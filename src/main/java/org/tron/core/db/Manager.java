package org.tron.core.db;

import static org.tron.core.config.Parameter.ChainConstant.SOLIDIFIED_THRESHOLD;
import static org.tron.core.config.Parameter.ChainConstant.WITNESS_PAY_PER_BLOCK;
import static org.tron.protos.Protocol.Transaction.Contract.ContractType.TransferAssetContract;
import static org.tron.protos.Protocol.Transaction.Contract.ContractType.TransferContract;

import com.carrotsearch.sizeof.RamUsageEstimator;
import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javafx.util.Pair;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.crypto.ECKey;
import org.tron.common.overlay.discover.Node;
import org.tron.common.storage.leveldb.LevelDbDataSourceImpl;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.DialogOptional;
import org.tron.common.utils.Sha256Hash;
import org.tron.common.utils.StringUtil;
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
import org.tron.core.exception.ValidateScheduleException;
import org.tron.core.exception.ValidateSignatureException;
import org.tron.core.witness.WitnessController;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Transaction;

@Slf4j
@Component
public class Manager {

  private static final long BLOCK_INTERVAL_SEC = 1;
  public static final int MAX_ACTIVE_WITNESS_NUM = 21;
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
  @Getter
  private DialogOptional<Dialog> dialog = DialogOptional.empty();

  @Getter
  @Setter
  private boolean isSyncMode;

  @Getter
  @Setter
  private String netType;

  @Getter
  @Setter
  private WitnessController witnessController;


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

  public List<TransactionCapsule> getPendingTransactions() {
    return this.pendingTransactions;
  }

  public List<TransactionCapsule> getPoppedTransactions() {
    return this.popedTransactions;
  }


  // transactions cache
  private List<TransactionCapsule> pendingTransactions;

  // transactions popped
  private List<TransactionCapsule> popedTransactions = new ArrayList<>();


  //for test only
  public List<WitnessCapsule> getWitnesses() {
    return witnessController.getWitnesses();
  }

  //for test only
  public void addWitness(final WitnessCapsule witnessCapsule) {
    witnessController.addWitness(witnessCapsule);
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

  public void destory() {
    getAccountStore().destroy();
    getTransactionStore().destroy();
    getBlockStore().destroy();
    getWitnessStore().destory();
    getAssetIssueStore().destroy();
    getDynamicPropertiesStore().destroy();
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
    this.setWitnessController(WitnessController.createInstance(this));

    revokingStore = RevokingStore.getInstance();
    revokingStore.enable();
    this.numHashCache = new LevelDbDataSourceImpl(
        Args.getInstance().getOutputDirectory(), "block" + "_NUM_HASH");
    this.numHashCache.initDB();
    this.khaosDb = new KhaosDatabase("block" + "_KDB");
    this.pendingTransactions = new ArrayList<>();
    this.initGenesis();
    this.witnessController.initWits();
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
  public void initGenesis() {
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
      pendingTransactions.add(trx);

      tmpDialog.merge();
    } catch (RevokingStoreIllegalStateException e) {
      logger.debug(e.getMessage(), e);
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
        if (latestOperationTime != 0) {
          int latstTransNumberInBlock = this.head.getTransactions().size();
          doValidateFreq(balacne, latstTransNumberInBlock, latestOperationTime);
        } else {
          accountCapsule.setLatestOperationTime(Time.getCurrentMillis());
        }
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
  private void eraseBlock() {
    dialog.reset();
    BlockCapsule oldHeadBlock = getBlockStore().get(head.getBlockId().getBytes());
    try {
      revokingStore.pop();
      head = getBlockStore().get(getBlockIdByNum(oldHeadBlock.getNum() - 1).getBytes());
    } catch (RevokingStoreIllegalStateException e) {
      logger.debug(e.getMessage(), e);
    }
    khaosDb.pop();
    popedTransactions.addAll(oldHeadBlock.getTransactions());
    // todo process the trans in the poped block.

  }

  private void switchFork(BlockCapsule newHead) {
    Pair<LinkedList<BlockCapsule>, LinkedList<BlockCapsule>> binaryTree = khaosDb
        .getBranch(newHead.getBlockId(), head.getBlockId());

    if (CollectionUtils.isNotEmpty(binaryTree.getValue())) {
      while (!head.getBlockId().equals(binaryTree.getValue().peekLast().getParentHash())) {
        logger.info("erase block. num = {}", head.getNum());
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
          logger.debug(e.getMessage(), e);
        } catch (ContractValidateException e) {
          logger.debug(e.getMessage(), e);
        } catch (ContractExeException e) {
          logger.debug(e.getMessage(), e);
        } catch (RevokingStoreIllegalStateException e) {
          logger.debug(e.getMessage(), e);
        }
      });
      return;
    }
  }

  //TODO: if error need to rollback.

  private synchronized void filterPendingTrx(List<TransactionCapsule> listTrx) {

  }

  /**
   * save a block.
   */
  public void pushBlock(final BlockCapsule block)
      throws ValidateSignatureException, ContractValidateException,
      ContractExeException, UnLinkedBlockException, ValidateScheduleException {

    try (PendingManager pm = new PendingManager(this)) {
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
          // TODO:throw exception here.
          return;
        }
      }

      if (!witnessController.validateWitnessSchedule(block)) {
        throw new ValidateScheduleException("validateWitnessSchedule error");
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
          logger.warn("switch fork! new head num = {}, blockid = {}", newBlock.getNum(),
              newBlock.getBlockId());
          switchFork(newBlock);
          logger.info("save block: " + newBlock);
          return;
        }
        try (Dialog tmpDialog = revokingStore.buildDialog()) {
          this.processBlock(newBlock);
          tmpDialog.commit();
        } catch (RevokingStoreIllegalStateException e) {
          logger.debug(e.getMessage(), e);
        }
      }
      blockStore.put(block.getBlockId().getBytes(), block);
      this.numHashCache.putData(ByteArray.fromLong(block.getNum()), block.getBlockId().getBytes());
      //refreshHead(newBlock);
      logger.info("save block: " + newBlock);
    }
  }

  public void updateDynamicProperties(BlockCapsule block) {
    long slot = 1;
    if (block.getNum() != 1){
      slot = witnessController.getSlotAtTime(block.getTimeStamp());
    }
    for (int i = 1; i < slot; ++i){
      if (!witnessController.getScheduledWitness(i).equals(block.getWitnessAddress())) {
        WitnessCapsule w = this.witnessStore
            .get(StringUtil.createDbKey(witnessController.getScheduledWitness(i)));
        w.setTotalMissed(w.getTotalMissed()+1);
        this.witnessStore.put(w.createDbKey(), w);
        logger.info("{} miss a block. totalMissed = {}",
            w.createReadableString(), w.getTotalMissed());
      }
    }

    long missedBlocks = witnessController.getSlotAtTime(block.getTimeStamp()) - 1;
    if (missedBlocks >= 0) {
      while (missedBlocks-- > 0) {
        this.dynamicPropertiesStore.getBlockFilledSlots().applyBlock(false);
      }
      this.dynamicPropertiesStore.getBlockFilledSlots().applyBlock(true);
    } else {
      logger.warn("missedBlocks [" + missedBlocks + "] is illegal");
    }

    this.head = block;
    logger.info("update head, num = {}", block.getNum());
    this.dynamicPropertiesStore
        .saveLatestBlockHeaderHash(block.getBlockId().getByteString());
    this.dynamicPropertiesStore.saveLatestBlockHeaderNumber(block.getNum());
    this.dynamicPropertiesStore.saveLatestBlockHeaderTimestamp(block.getTimeStamp());
    witnessController.updateWitnessSchedule();


  }

  @Deprecated
  private void refreshHead(BlockCapsule block) {
    this.head = block;
    this.dynamicPropertiesStore
        .saveLatestBlockHeaderHash(block.getBlockId().getByteString());
    this.dynamicPropertiesStore.saveLatestBlockHeaderNumber(block.getNum());
    this.dynamicPropertiesStore.saveLatestBlockHeaderTimestamp(block.getTimeStamp());
    witnessController.updateWitnessSchedule();
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
      ContractExeException, UnLinkedBlockException, ValidateScheduleException {

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

    Iterator iterator = pendingTransactions.iterator();
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
        logger.debug(e.getMessage(), e);
      } catch (ContractValidateException e) {
        logger.info("contract not processed during validate");
        logger.debug(e.getMessage(), e);
      } catch (RevokingStoreIllegalStateException e) {
        logger.debug(e.getMessage(), e);
      }
    }

    dialog.reset();

    if (postponedTrxCount > 0) {
      logger.info("{} transactions over the block size limit", postponedTrxCount);
    }

    logger.info(
        "postponedTrxCount[" + postponedTrxCount + "],TrxLeft[" + pendingTransactions.size() + "]");

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
   * get the latest solidified block num from witness.
   */
  public long getLatestSolidifiedBlockNumFromWitness() {
    List<Long> numbers = witnessController.getWitnesses().stream()
        .map(wit -> wit.getLatestBlockNum())
        .sorted()
        .collect(Collectors.toList());

    long size = witnessController.getWitnesses().size();
    int solidifiedPosition = (int) (size * (1 - SOLIDIFIED_THRESHOLD)) - 1;
    if (solidifiedPosition < 0) {
      logger.warn("getLatestSolidifiedBlockNumFromWitness error,solidifiedPosition:{},wits.size:{}",
          solidifiedPosition, size);
      return 0;
    }

    long latestSolidifiedBlockNum = numbers.get(solidifiedPosition);
    return latestSolidifiedBlockNum;
  }

  /**
   * update the latest solidified block.
   */
  public void updateLatestSolidifiedBlock() {
    long latestSolidifiedBlockNum = getLatestSolidifiedBlockNumFromWitness();
    if (latestSolidifiedBlockNum > 0) {
      getDynamicPropertiesStore().saveLatestSolidifiedBlockNum(latestSolidifiedBlockNum);
    }
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
    witnessController.updateWitness();
    this.dynamicPropertiesStore.updateNextMaintenanceTime(block.getTimeStamp());
  }

  /**
   * @param block the block update signed witness.  set witness who signed block the 1. the latest
   * block num 2. pay the trx to witness. 3. the latest slot num.
   */

  public void updateSignedWitness(BlockCapsule block) {
    //TODO: add verification
    WitnessCapsule witnessCapsule = witnessStore
        .get(block.getInstance().getBlockHeader().getRawData().getWitnessAddress().toByteArray());
    witnessCapsule.setTotalProduced(witnessCapsule.getTotalProduced()+1);
    witnessCapsule.setLatestBlockNum(block.getNum());
    witnessCapsule.setLatestSlotNum(witnessController.getAbSlotAtTime(block.getTimeStamp()));

    this.getWitnessStore().put(witnessCapsule.getAddress().toByteArray(),witnessCapsule);

    AccountCapsule sun = accountStore.getSun();
    try {
      adjustBalance(sun.getAddress().toByteArray(), -WITNESS_PAY_PER_BLOCK);
    } catch (BalanceInsufficientException e) {
      logger.debug(e.getMessage(), e);
    }
    try {
      adjustBalance(witnessCapsule.getAddress().toByteArray(), WITNESS_PAY_PER_BLOCK);
    } catch (BalanceInsufficientException e) {
      logger.debug(e.getMessage(), e);
    }

    logger.info("updateSignedWitness. witness address:{}, blockNum:{}, totalProduced:{}",
        witnessCapsule.createReadableString(), block.getNum(), witnessCapsule.getTotalProduced());

  }

  public boolean lastHeadBlockIsMaintenance() {
    return getDynamicPropertiesStore().getStateFlag() == 1;
  }

  // To be added
  public long getSkipSlotInMaintenance() {
    return 0;
  }

  public AssetIssueStore getAssetIssueStore() {
    return assetIssueStore;
  }

  public void setAssetIssueStore(AssetIssueStore assetIssueStore) {
    this.assetIssueStore = assetIssueStore;
  }

}
