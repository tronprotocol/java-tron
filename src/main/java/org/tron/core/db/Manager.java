package org.tron.core.db;

import static org.tron.core.config.Parameter.ChainConstant.SOLIDIFIED_THRESHOLD;
import static org.tron.core.config.Parameter.ChainConstant.WITNESS_PAY_PER_BLOCK;
import static org.tron.protos.Protocol.Transaction.Contract.ContractType.TransferAssetContract;
import static org.tron.protos.Protocol.Transaction.Contract.ContractType.TransferContract;

import com.carrotsearch.sizeof.RamUsageEstimator;
import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.crypto.ECKey;
import org.tron.common.overlay.discover.Node;
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
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.BalanceInsufficientException;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.HeaderNotFound;
import org.tron.core.exception.HighFreqException;
import org.tron.core.exception.ItemNotFoundException;
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


  // db store
  private AccountStore accountStore;
  private TransactionStore transactionStore;
  private BlockStore blockStore;
  private UtxoStore utxoStore;
  private WitnessStore witnessStore;
  private AssetIssueStore assetIssueStore;
  private DynamicPropertiesStore dynamicPropertiesStore;
  private BlockIndexStore blockIndexStore;
  private WitnessScheduleStore witnessScheduleStore;

  @Autowired
  private PeersStore peersStore;
  private BlockCapsule genesisBlock;


  private KhaosDatabase khaosDb;
  private RevokingDatabase revokingStore;
  @Getter
  private DialogOptional dialog = DialogOptional.instance();

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

  public WitnessScheduleStore getWitnessScheduleStore() {
    return this.witnessScheduleStore;
  }

  public void setWitnessScheduleStore(final WitnessScheduleStore witnessScheduleStore) {
    this.witnessScheduleStore = witnessScheduleStore;
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
  private List<TransactionCapsule> popedTransactions = Collections
      .synchronizedList(Lists.newArrayList());


  //for test only
  public List<ByteString> getWitnesses() {
    return witnessController.getActiveWitnesses();
  }

  //for test only
  public void addWitness(final ByteString address) {
    List<ByteString> witnessAddresses = witnessController.getActiveWitnesses();
    witnessAddresses.add(address);
    witnessController.setActiveWitnesses(witnessAddresses);
  }

  public BlockCapsule getHead() throws HeaderNotFound {
    try {
      return getBlockStore().get(getDynamicPropertiesStore().getLatestBlockHeaderHash().getBytes());
    } catch (ItemNotFoundException e) {
      logger.info(e.getMessage());
      throw new HeaderNotFound(e.getMessage());
    } catch (BadItemException e) {
      throw new HeaderNotFound(e.getMessage());
    }
  }


  public BlockId getHeadBlockId() {
    return new BlockId(getDynamicPropertiesStore().getLatestBlockHeaderHash(),
        getDynamicPropertiesStore().getLatestBlockHeaderNumber());
  }


  public long getHeadBlockNum() {
    return getDynamicPropertiesStore().getLatestBlockHeaderNumber();
  }


  public long getHeadBlockTimeStamp() {
    return getDynamicPropertiesStore().getLatestBlockHeaderTimestamp();
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

  // fot test only
  public void destory() {
    AccountStore.destroy();
    TransactionStore.destroy();
    BlockStore.destroy();
    WitnessStore.destory();
    AssetIssueStore.destroy();
    DynamicPropertiesStore.destroy();
    WitnessScheduleStore.destroy();
    BlockIndexStore.destroy();
  }

  /**
   * all db should be init here.
   */
  public void init() {
    revokingStore = RevokingStore.getInstance();
    revokingStore.disable();
    this.setAccountStore(AccountStore.create("account"));
    this.setTransactionStore(TransactionStore.create("trans"));
    this.setBlockStore(BlockStore.create("block"));
    this.setUtxoStore(UtxoStore.create("utxo"));
    this.setWitnessStore(WitnessStore.create("witness"));
    this.setAssetIssueStore(AssetIssueStore.create("asset-issue"));
    this.setDynamicPropertiesStore(DynamicPropertiesStore.create("properties"));
    this.setWitnessScheduleStore(WitnessScheduleStore.create("witness_schedule"));
    this.setWitnessController(WitnessController.createInstance(this));
    this.setBlockIndexStore(BlockIndexStore.create("block-index"));
    this.khaosDb = new KhaosDatabase("block" + "_KDB");
    this.pendingTransactions = Collections.synchronizedList(Lists.newArrayList());
    this.initGenesis();
    try {
      this.khaosDb.start(getBlockById(getDynamicPropertiesStore().getLatestBlockHeaderHash()));
    } catch (ItemNotFoundException e) {
      logger.error("Can not find Dynamic highest block from DB! \nnumber={} \nhash={}",
          getDynamicPropertiesStore().getLatestBlockHeaderNumber(),
          getDynamicPropertiesStore().getLatestBlockHeaderHash());
      logger.error("Please delete database directory({}) and restart",
          Args.getInstance().getOutputDirectory());
      System.exit(1);
    } catch (BadItemException e) {
      e.printStackTrace();
      logger.error("DB data broken!");
      logger.error("Please delete database directory({}) and restart",
          Args.getInstance().getOutputDirectory());
      System.exit(1);
    }
    revokingStore.enable();
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
        this.blockIndexStore.put(this.genesisBlock.getBlockId());

        logger.info("save block: " + this.genesisBlock);
        // init DynamicPropertiesStore
        this.dynamicPropertiesStore.saveLatestBlockHeaderNumber(0);
        this.dynamicPropertiesStore.saveLatestBlockHeaderHash(
            this.genesisBlock.getBlockId().getByteString());
        this.dynamicPropertiesStore.saveLatestBlockHeaderTimestamp(
            this.genesisBlock.getTimeStamp());
        this.initAccount();
        this.initWitness();
        this.witnessController.initWits();
        this.khaosDb.start(genesisBlock);
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
          ByteString.copyFrom(account.getAddress()),
          account.getAccountType(),
          account.getBalance());
      this.accountStore.put(account.getAddress(), accountCapsule);
    });
  }

  /**
   * save witnesses into database.
   */
  private void initWitness() {
    final Args args = Args.getInstance();
    final GenesisBlock genesisBlockArg = args.getGenesisBlock();
    genesisBlockArg.getWitnesses().forEach(key -> {
      byte[] keyAddress = key.getAddress();
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
      dialog.setValue(revokingStore.buildDialog());
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

  private void validateFreq(TransactionCapsule trx) throws HighFreqException {
    List<org.tron.protos.Protocol.Transaction.Contract> contracts = trx.getInstance().getRawData()
        .getContractList();
    for (Transaction.Contract contract : contracts) {
      if (contract.getType() == TransferContract
          || contract.getType() == TransferAssetContract) {
        byte[] address = TransactionCapsule.getOwner(contract);
        AccountCapsule accountCapsule = this.getAccountStore().get(address);
        long balance = accountCapsule.getBalance();
        long latestOperationTime = accountCapsule.getLatestOperationTime();
        if (latestOperationTime != 0) {
          doValidateFreq(balance, 0, latestOperationTime);
        }
        accountCapsule.setLatestOperationTime(Time.getCurrentMillis());
        this.getAccountStore().put(accountCapsule.createDbKey(),accountCapsule);
      }
    }
  }

  private void doValidateFreq(long balance, int transNumber, long latestOperationTime)
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
  public void eraseBlock() throws BadItemException, ItemNotFoundException {
    dialog.reset();
    BlockCapsule oldHeadBlock = getBlockStore()
        .get(getDynamicPropertiesStore().getLatestBlockHeaderHash().getBytes());
    try {
      revokingStore.pop();
    } catch (RevokingStoreIllegalStateException e) {
      logger.debug(e.getMessage(), e);
    }
    logger.info("erase block:" + oldHeadBlock);
    khaosDb.pop();
    popedTransactions.addAll(oldHeadBlock.getTransactions());
  }

  private void applyBlock(BlockCapsule block)
      throws ContractValidateException, ContractExeException, ValidateSignatureException {
    processBlock(block);
    this.blockStore.put(block.getBlockId().getBytes(), block);
    this.blockIndexStore.put(block.getBlockId());
  }

  private void switchFork(BlockCapsule newHead) {
    Pair<LinkedList<BlockCapsule>, LinkedList<BlockCapsule>> binaryTree = khaosDb
        .getBranch(newHead.getBlockId(), getDynamicPropertiesStore().getLatestBlockHeaderHash());

    if (CollectionUtils.isNotEmpty(binaryTree.getValue())) {
      while (!getDynamicPropertiesStore().getLatestBlockHeaderHash().equals(
          binaryTree.getValue().peekLast().getParentHash())) {
        try {
          eraseBlock();
        } catch (BadItemException e) {
          logger.info(e.getMessage());
        } catch (ItemNotFoundException e) {
          logger.info(e.getMessage());
        }
      }
    }

    if (CollectionUtils.isNotEmpty(binaryTree.getKey())) {
      LinkedList<BlockCapsule> branch = binaryTree.getKey();
      Collections.reverse(branch);
      branch.forEach(item -> {
        // todo  process the exception carefully later
        try (Dialog tmpDialog = revokingStore.buildDialog()) {
          applyBlock(item);
          tmpDialog.commit();
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

      // checkWitness
      if (!witnessController.validateWitnessSchedule(block)) {
        throw new ValidateScheduleException("validateWitnessSchedule error");
      }

      BlockCapsule newBlock = this.khaosDb.push(block);
      //DB don't need lower block
      if (getDynamicPropertiesStore().getLatestBlockHeaderHash() == null) {
        if (newBlock.getNum() != 0) {
          return;
        }
      } else {
        if (newBlock.getNum() <= getDynamicPropertiesStore().getLatestBlockHeaderNumber()) {
          return;
        }
        //switch fork
        if (!newBlock.getParentHash()
            .equals(getDynamicPropertiesStore().getLatestBlockHeaderHash())) {
          logger.warn("switch fork! new head num = {}, blockid = {}", newBlock.getNum(),
              newBlock.getBlockId());

          logger.error("******** before switchFork ******* push block: " + block
              + ", new block:" + newBlock
              + ", dynamic head num: " + dynamicPropertiesStore.getLatestBlockHeaderNumber()
              + ", dynamic head hash: " + dynamicPropertiesStore.getLatestBlockHeaderHash()
              + ", dynamic head timestamp: " + dynamicPropertiesStore
              .getLatestBlockHeaderTimestamp()
              + ", khaosDb head: " + khaosDb.getHead()
              + ", khaosDb miniStore size: " + khaosDb.getMiniStore().size()
              + ", khaosDb unlinkMiniStore size: " + khaosDb.getMiniUnlinkedStore().size()
          );

          switchFork(newBlock);
          logger.info("save block: " + newBlock);

          logger.error("******** after switchFork ******* push block: " + block
              + ", new block:" + newBlock
              + ", dynamic head num: " + dynamicPropertiesStore.getLatestBlockHeaderNumber()
              + ", dynamic head hash: " + dynamicPropertiesStore.getLatestBlockHeaderHash()
              + ", dynamic head timestamp: " + dynamicPropertiesStore
              .getLatestBlockHeaderTimestamp()
              + ", khaosDb head: " + khaosDb.getHead()
              + ", khaosDb miniStore size: " + khaosDb.getMiniStore().size()
              + ", khaosDb unlinkMiniStore size: " + khaosDb.getMiniUnlinkedStore().size()
          );

          return;
        }
        try (Dialog tmpDialog = revokingStore.buildDialog()) {
          applyBlock(newBlock);
          tmpDialog.commit();
        } catch (RevokingStoreIllegalStateException e) {
          logger.debug(e.getMessage(), e);
        }
      }
      logger.info("save block: " + newBlock);
    }
  }

  public void updateDynamicProperties(BlockCapsule block) {
    long slot = 1;
    if (block.getNum() != 1) {
      slot = witnessController.getSlotAtTime(block.getTimeStamp());
    }
    for (int i = 1; i < slot; ++i) {
      if (!witnessController.getScheduledWitness(i).equals(block.getWitnessAddress())) {
        WitnessCapsule w = this.witnessStore
            .get(StringUtil.createDbKey(witnessController.getScheduledWitness(i)));
        w.setTotalMissed(w.getTotalMissed() + 1);
        this.witnessStore.put(w.createDbKey(), w);
        logger.info("{} miss a block. totalMissed = {}",
            w.createReadableString(), w.getTotalMissed());
      }
      this.dynamicPropertiesStore.applyBlock(false);
    }
    this.dynamicPropertiesStore.applyBlock(true);

    if (slot <= 0) {
      logger.warn("missedBlocks [" + slot + "] is illegal");
    }

    logger.info("update head, num = {}", block.getNum());
    this.dynamicPropertiesStore
        .saveLatestBlockHeaderHash(block.getBlockId().getByteString());
    this.dynamicPropertiesStore.saveLatestBlockHeaderNumber(block.getNum());
    this.dynamicPropertiesStore.saveLatestBlockHeaderTimestamp(block.getTimeStamp());

    ((AbstractRevokingStore) revokingStore).setMaxSize((int) (
        dynamicPropertiesStore.getLatestBlockHeaderNumber()
            - dynamicPropertiesStore.getLatestSolidifiedBlockNum() + 1)
    );
  }

  /**
   * Get the fork branch.
   */
  public LinkedList<BlockId> getBlockChainHashesOnFork(final BlockId forkBlockHash) {
    final Pair<LinkedList<BlockCapsule>, LinkedList<BlockCapsule>> branch =
        this.khaosDb.getBranch(
            getDynamicPropertiesStore().getLatestBlockHeaderHash(),
            forkBlockHash);
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
    try {
      return this.khaosDb.containBlock(blockHash)
          || blockStore.get(blockHash.getBytes()) != null;
    } catch (ItemNotFoundException e) {
      return false;
    } catch (BadItemException e) {
      return false;
    }

  }

  public boolean containBlockInMainChain(BlockId blockId) {
    try {
      return blockStore.get(blockId.getBytes()) != null;
    } catch (ItemNotFoundException e) {
      return false;
    } catch (BadItemException e) {
      return false;
    }
  }

  public void setBlockReference(TransactionCapsule trans) {
    byte[] headHash = getDynamicPropertiesStore().getLatestBlockHeaderHash().getBytes();
    long headNum = getDynamicPropertiesStore().getLatestBlockHeaderNumber();
    trans.setReference(headNum, headHash);
  }

  /**
   * Get a BlockCapsule by id.
   */

  public BlockCapsule getBlockById(final Sha256Hash hash)
      throws BadItemException, ItemNotFoundException {
    return this.khaosDb.containBlock(hash) ? this.khaosDb.getBlock(hash)
        : blockStore.get(hash.getBytes());
  }

  /**
   * Delete a block.
   */
  @Deprecated
  public void deleteBlock(final Sha256Hash blockHash)
      throws BadItemException, ItemNotFoundException {
    final BlockCapsule block = this.getBlockById(blockHash);
    this.khaosDb.removeBlk(blockHash);
    blockStore.delete(blockHash.getBytes());
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
  public BlockId getBlockIdByNum(final long num)
      throws ItemNotFoundException {
    return this.blockIndexStore.get(num);
  }

  public BlockCapsule getBlockByNum(final long num)
      throws ItemNotFoundException, BadItemException {
    return getBlockById(getBlockIdByNum(num));
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
    final Sha256Hash preHash = this.dynamicPropertiesStore.getLatestBlockHeaderHash();

    // judge create block time
    if (when < timestamp) {
      throw new IllegalArgumentException("generate block timestamp is invalid.");
    }

    long currentTrxSize = 0;
    long postponedTrxCount = 0;

    final BlockCapsule blockCapsule = new BlockCapsule(number + 1, preHash, when,
        witnessCapsule.getAddress());

    dialog.reset();
    dialog.setValue(revokingStore.buildDialog());

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
    // todo set revoking db max size.
    this.updateDynamicProperties(block);
    this.updateSignedWitness(block);
    this.updateLatestSolidifiedBlock();

    for (TransactionCapsule transactionCapsule : block.getTransactions()) {
      processTransaction(transactionCapsule);
    }

    boolean needMaint = needMaintenance(block.getTimeStamp());
    if (needMaint) {
      if (block.getNum() == 1) {
        this.dynamicPropertiesStore.updateNextMaintenanceTime(block.getTimeStamp());
      } else {
        this.processMaintenance(block);
      }
    }
    updateMaintenanceState(needMaint);
    witnessController.updateWitnessSchedule();
  }

  /**
   * update the latest solidified block.
   */
  public void updateLatestSolidifiedBlock() {
    List<Long> numbers = witnessController.getActiveWitnesses().stream()
        .map(address -> witnessController.getWitnesseByAddress(address).getLatestBlockNum())
        .sorted()
        .collect(Collectors.toList());

    long size = witnessController.getActiveWitnesses().size();
    int solidifiedPosition = (int) (size * (1 - SOLIDIFIED_THRESHOLD));
    if (solidifiedPosition < 0) {
      logger.warn("updateLatestSolidifiedBlock error, solidifiedPosition:{},wits.size:{}",
          solidifiedPosition, size);
      return;
    }
    long latestSolidifiedBlockNum = numbers.get(solidifiedPosition);
    getDynamicPropertiesStore().saveLatestSolidifiedBlockNum(latestSolidifiedBlockNum);
    logger.info("update solid block, num = {}", latestSolidifiedBlockNum);
  }

  public long getSyncBeginNumber() {
    logger.info("headNumber:" + dynamicPropertiesStore.getLatestBlockHeaderNumber());
    logger.info(
        "syncBeginNumber:" + (dynamicPropertiesStore.getLatestBlockHeaderNumber() - revokingStore
            .size()));
    logger.info("solidBlockNumber:" + dynamicPropertiesStore.getLatestSolidifiedBlockNum());
    return dynamicPropertiesStore.getLatestBlockHeaderNumber() - revokingStore.size();
  }

  /**
   * Determine if the current time is maintenance time.
   */
  public boolean needMaintenance(long blockTime) {
    return this.dynamicPropertiesStore.getNextMaintenanceTime() <= blockTime;
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
    witnessCapsule.setTotalProduced(witnessCapsule.getTotalProduced() + 1);
    witnessCapsule.setLatestBlockNum(block.getNum());
    witnessCapsule.setLatestSlotNum(witnessController.getAbSlotAtTime(block.getTimeStamp()));

    //Update memory witness status
    WitnessCapsule wit = witnessController.getWitnesseByAddress(block.getWitnessAddress());
    if (wit != null) {
      wit.setTotalProduced(witnessCapsule.getTotalProduced() + 1);
      wit.setLatestBlockNum(block.getNum());
      wit.setLatestSlotNum(witnessController.getAbSlotAtTime(block.getTimeStamp()));
    }

    this.getWitnessStore().put(witnessCapsule.getAddress().toByteArray(), witnessCapsule);

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

    logger.debug("updateSignedWitness. witness address:{}, blockNum:{}, totalProduced:{}",
        witnessCapsule.createReadableString(), block.getNum(), witnessCapsule.getTotalProduced());

  }

  public void updateMaintenanceState(boolean needMaint) {
    if (needMaint) {
      getDynamicPropertiesStore().saveStateFlag(1);
    } else {
      getDynamicPropertiesStore().saveStateFlag(0);
    }
  }

  public boolean lastHeadBlockIsMaintenance() {
    return getDynamicPropertiesStore().getStateFlag() == 1;
  }

  // To be added
  public long getSkipSlotInMaintenance() {
    return getDynamicPropertiesStore().getMaintenanceSkipSlots();
  }

  public AssetIssueStore getAssetIssueStore() {
    return assetIssueStore;
  }

  public void setAssetIssueStore(AssetIssueStore assetIssueStore) {
    this.assetIssueStore = assetIssueStore;
  }

  public void setBlockIndexStore(BlockIndexStore indexStore) {
    this.blockIndexStore = indexStore;
  }
}
