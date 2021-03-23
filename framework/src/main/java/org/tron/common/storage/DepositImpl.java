package org.tron.common.storage;

import static org.tron.core.db.TransactionTrace.convertToTronAddress;

import com.google.common.primitives.Longs;
import com.google.protobuf.ByteString;
import java.util.HashMap;
import lombok.extern.slf4j.Slf4j;
import org.spongycastle.util.Strings;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.crypto.Hash;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.ByteUtil;
import org.tron.common.utils.Commons;
import org.tron.common.utils.StorageUtils;
import org.tron.common.utils.StringUtil;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.capsule.ContractCapsule;
import org.tron.core.capsule.ProposalCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.capsule.VotesCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.db.BlockStore;
import org.tron.core.db.Manager;
import org.tron.core.db.TransactionStore;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.core.store.AccountStore;
import org.tron.core.store.CodeStore;
import org.tron.core.store.ContractStore;
import org.tron.core.store.DelegatedResourceStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.store.ProposalStore;
import org.tron.core.store.VotesStore;
import org.tron.core.store.WitnessStore;
import org.tron.core.vm.config.VMConfig;
import org.tron.core.vm.program.Storage;
import org.tron.core.vm.repository.Key;
import org.tron.core.vm.repository.Type;
import org.tron.core.vm.repository.Value;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.AccountType;

@Slf4j(topic = "deposit")
public class DepositImpl implements Deposit {

  private static final byte[] LATEST_PROPOSAL_NUM = "LATEST_PROPOSAL_NUM".getBytes();
  private static final byte[] WITNESS_ALLOWANCE_FROZEN_TIME = "WITNESS_ALLOWANCE_FROZEN_TIME"
      .getBytes();
  private static final byte[] MAINTENANCE_TIME_INTERVAL = "MAINTENANCE_TIME_INTERVAL".getBytes();
  private static final byte[] NEXT_MAINTENANCE_TIME = "NEXT_MAINTENANCE_TIME".getBytes();

  private Manager dbManager;
  private Deposit parent = null;

  private HashMap<Key, Value> accountCache = new HashMap<>();
  private HashMap<Key, Value> transactionCache = new HashMap<>();
  private HashMap<Key, Value> blockCache = new HashMap<>();
  private HashMap<Key, Value> witnessCache = new HashMap<>();
  private HashMap<Key, Value> codeCache = new HashMap<>();
  private HashMap<Key, Value> contractCache = new HashMap<>();

  private HashMap<Key, Value> votesCache = new HashMap<>();
  private HashMap<Key, Value> proposalCache = new HashMap<>();
  private HashMap<Key, Value> dynamicPropertiesCache = new HashMap<>();
  private HashMap<Key, Storage> storageCache = new HashMap<>();
  private HashMap<Key, Value> assetIssueCache = new HashMap<>();

  private DepositImpl(Manager dbManager, DepositImpl parent) {
    init(dbManager, parent);
  }

  public static DepositImpl createRoot(Manager dbManager) {
    return new DepositImpl(dbManager, null);
  }

  protected void init(Manager dbManager, DepositImpl parent) {
    this.dbManager = dbManager;
    this.parent = parent;
  }

  @Override
  public Manager getDbManager() {
    return dbManager;
  }

  private BlockStore getBlockStore() {
    return dbManager.getBlockStore();
  }

  private TransactionStore getTransactionStore() {
    return dbManager.getTransactionStore();
  }

  private ContractStore getContractStore() {
    return dbManager.getContractStore();
  }

  private WitnessStore getWitnessStore() {
    return dbManager.getWitnessStore();
  }

  private VotesStore getVotesStore() {
    return dbManager.getVotesStore();
  }

  private ProposalStore getProposalStore() {
    return dbManager.getProposalStore();
  }

  private DynamicPropertiesStore getDynamicPropertiesStore() {
    return dbManager.getDynamicPropertiesStore();
  }

  private AccountStore getAccountStore() {
    return dbManager.getAccountStore();
  }

  private CodeStore getCodeStore() {
    return dbManager.getCodeStore();
  }

  private DelegatedResourceStore getDelegatedResourceStore() {
    return dbManager.getDelegatedResourceStore();
  }

  @Override
  public Deposit newDepositChild() {
    return new DepositImpl(dbManager, this);
  }

  @Override
  public synchronized AccountCapsule createAccount(byte[] address, Protocol.AccountType type) {
    Key key = new Key(address);
    AccountCapsule account = new AccountCapsule(ByteString.copyFrom(address), type);
    accountCache.put(key, new Value(account.getData(), Type.VALUE_TYPE_CREATE));
    return account;
  }

  @Override
  public AccountCapsule createAccount(byte[] address, String accountName, AccountType type) {
    Key key = new Key(address);
    AccountCapsule account = new AccountCapsule(ByteString.copyFrom(address),
        ByteString.copyFromUtf8(accountName),
        type);

    accountCache.put(key, new Value(account.getData(), Type.VALUE_TYPE_CREATE));
    return account;
  }

  @Override
  public synchronized AccountCapsule getAccount(byte[] address) {
    Key key = new Key(address);
    if (accountCache.containsKey(key)) {
      return accountCache.get(key).getAccount();
    }

    AccountCapsule accountCapsule;
    if (parent != null) {
      accountCapsule = parent.getAccount(address);
    } else {
      accountCapsule = getAccountStore().get(address);
    }

    if (accountCapsule != null) {
      accountCache.put(key, Value.create(accountCapsule.getData()));
    }
    return accountCapsule;
  }

  @Override
  public byte[] getBlackHoleAddress() {
    // using dbManager directly, black hole address should not be changed
    // when executing smart contract.
    return getAccountStore().getBlackholeAddress();
  }

  @Override
  public WitnessCapsule getWitness(byte[] address) {
    Key key = new Key(address);
    if (witnessCache.containsKey(key)) {
      return witnessCache.get(key).getWitness();
    }

    WitnessCapsule witnessCapsule;
    if (parent != null) {
      witnessCapsule = parent.getWitness(address);
    } else {
      witnessCapsule = getWitnessStore().get(address);
    }

    if (witnessCapsule != null) {
      witnessCache.put(key, Value.create(witnessCapsule.getData()));
    }
    return witnessCapsule;
  }

  @Override
  public synchronized VotesCapsule getVotesCapsule(byte[] address) {
    Key key = new Key(address);
    if (votesCache.containsKey(key)) {
      return votesCache.get(key).getVotes();
    }

    VotesCapsule votesCapsule;
    if (parent != null) {
      votesCapsule = parent.getVotesCapsule(address);
    } else {
      votesCapsule = getVotesStore().get(address);
    }

    if (votesCapsule != null) {
      votesCache.put(key, Value.create(votesCapsule.getData()));
    }
    return votesCapsule;
  }

  @Override
  public synchronized ProposalCapsule getProposalCapsule(byte[] id) {
    Key key = new Key(id);
    if (proposalCache.containsKey(key)) {
      return proposalCache.get(key).getProposal();
    }

    ProposalCapsule proposalCapsule;
    if (parent != null) {
      proposalCapsule = parent.getProposalCapsule(id);
    } else {
      try {
        proposalCapsule = getProposalStore().get(id);
      } catch (ItemNotFoundException e) {
        logger.warn("proposal not found, id:" + Hex.toHexString(id));
        proposalCapsule = null;
      }
    }

    if (proposalCapsule != null) {
      proposalCache.put(key, Value.create(proposalCapsule.getData()));
    }
    return proposalCapsule;
  }

  // just for depositRoot
  @Override
  public void deleteContract(byte[] address) {
    getCodeStore().delete(address);
    getAccountStore().delete(address);
    getContractStore().delete(address);
  }

  @Override
  public synchronized void createContract(byte[] address, ContractCapsule contractCapsule) {
    Key key = Key.create(address);
    Value value = Value.create(contractCapsule.getData(), Type.VALUE_TYPE_CREATE);
    contractCache.put(key, value);
  }

  @Override
  public void updateContract(byte[] address, ContractCapsule contractCapsule) {
    Key key = Key.create(address);
    Value value = Value.create(contractCapsule.getData(), Type.VALUE_TYPE_DIRTY);
    contractCache.put(key, value);
  }

  @Override
  public void updateAccount(byte[] address, AccountCapsule accountCapsule) {
    Key key = Key.create(address);
    Value value = Value.create(accountCapsule.getData(), Type.VALUE_TYPE_DIRTY);
    accountCache.put(key, value);
  }

  @Override
  public synchronized ContractCapsule getContract(byte[] address) {
    Key key = Key.create(address);
    if (contractCache.containsKey(key)) {
      return contractCache.get(key).getContract();
    }

    ContractCapsule contractCapsule;
    if (parent != null) {
      contractCapsule = parent.getContract(address);
    } else {
      contractCapsule = getContractStore().get(address);
    }

    if (contractCapsule != null) {
      contractCache.put(key, Value.create(contractCapsule.getData()));
    }
    return contractCapsule;
  }

  @Override
  public synchronized void saveCode(byte[] address, byte[] code) {
    Key key = Key.create(address);
    Value value = Value.create(code, Type.VALUE_TYPE_CREATE);
    codeCache.put(key, value);

    if (VMConfig.allowTvmConstantinople()) {
      ContractCapsule contract = getContract(address);
      byte[] codeHash = Hash.sha3(code);
      contract.setCodeHash(codeHash);
      updateContract(address, contract);
    }
  }

  @Override
  public synchronized byte[] getCode(byte[] address) {
    Key key = Key.create(address);
    if (codeCache.containsKey(key)) {
      return codeCache.get(key).getCode().getData();
    }

    byte[] code;
    if (parent != null) {
      code = parent.getCode(address);
    } else {
      if (null == getCodeStore().get(address)) {
        code = null;
      } else {
        code = getCodeStore().get(address).getData();
      }
    }
    if (code != null) {
      codeCache.put(key, Value.create(code));
    }
    return code;
  }

  @Override
  public synchronized Storage getStorage(byte[] address) {
    Key key = Key.create(address);
    if (storageCache.containsKey(key)) {
      return storageCache.get(key);
    }
    Storage storage;
    if (this.parent != null) {
      Storage parentStorage = parent.getStorage(address);
      if (StorageUtils.getEnergyLimitHardFork()) {
        // deep copy
        storage = new Storage(parentStorage);
      } else {
        storage = parentStorage;
      }
    } else {
      storage = new Storage(address, dbManager.getStorageRowStore());
    }
    ContractCapsule contract = getContract(address);
    if (contract != null && !ByteUtil.isNullOrZeroArray(contract.getTrxHash())) {
      storage.generateAddrHash(contract.getTrxHash());
    }
    return storage;
  }

  @Override
  public synchronized AssetIssueCapsule getAssetIssue(byte[] tokenId) {
    byte[] tokenIdWithoutLeadingZero = ByteUtil.stripLeadingZeroes(tokenId);
    Key key = Key.create(tokenIdWithoutLeadingZero);
    if (assetIssueCache.containsKey(key)) {
      return assetIssueCache.get(key).getAssetIssue();
    }

    AssetIssueCapsule assetIssueCapsule;
    if (this.parent != null) {
      assetIssueCapsule = parent.getAssetIssue(tokenIdWithoutLeadingZero);
    } else {
      assetIssueCapsule = Commons.getAssetIssueStoreFinal(dbManager.getDynamicPropertiesStore(),
          dbManager.getAssetIssueStore(), dbManager.getAssetIssueV2Store())
          .get(tokenIdWithoutLeadingZero);
    }
    if (assetIssueCapsule != null) {
      assetIssueCache.put(key, Value.create(assetIssueCapsule.getData()));
    }
    return assetIssueCapsule;
  }

  @Override
  public synchronized void putStorageValue(byte[] address, DataWord key, DataWord value) {
    address = convertToTronAddress(address);
    if (getAccount(address) == null) {
      return;
    }
    Key addressKey = Key.create(address);
    Storage storage;
    if (storageCache.containsKey(addressKey)) {
      storage = storageCache.get(addressKey);
    } else {
      storage = getStorage(address);
      storageCache.put(addressKey, storage);
    }
    storage.put(key, value);
  }

  @Override
  public synchronized DataWord getStorageValue(byte[] address, DataWord key) {
    address = convertToTronAddress(address);
    if (getAccount(address) == null) {
      return null;
    }
    Key addressKey = Key.create(address);
    Storage storage;
    if (storageCache.containsKey(addressKey)) {
      storage = storageCache.get(addressKey);
    } else {
      storage = getStorage(address);
      storageCache.put(addressKey, storage);
    }
    return storage.getValue(key);
  }

  @Override
  public synchronized long getBalance(byte[] address) {
    AccountCapsule accountCapsule = getAccount(address);
    return accountCapsule == null ? 0L : accountCapsule.getBalance();
  }

  @Override
  public synchronized long addTokenBalance(byte[] address, byte[] tokenId, long value) {
    byte[] tokenIdWithoutLeadingZero = ByteUtil.stripLeadingZeroes(tokenId);
    AccountCapsule accountCapsule = getAccount(address);
    if (accountCapsule == null) {
      accountCapsule = createAccount(address, AccountType.Normal);
    }
    long balance = accountCapsule.getAssetMapV2()
        .getOrDefault(new String(tokenIdWithoutLeadingZero), new Long(0));
    if (value == 0) {
      return balance;
    }

    if (value < 0 && balance < -value) {
      throw new RuntimeException(
          StringUtil.createReadableString(accountCapsule.createDbKey())
              + " insufficient balance");
    }
    if (value >= 0) {
      accountCapsule.addAssetAmountV2(tokenIdWithoutLeadingZero, value,
          this.dbManager.getDynamicPropertiesStore(), this.dbManager.getAssetIssueStore());
    } else {
      accountCapsule.reduceAssetAmountV2(tokenIdWithoutLeadingZero, -value,
          this.dbManager.getDynamicPropertiesStore(), this.dbManager.getAssetIssueStore());
    }

    Key key = Key.create(address);
    Value V = Value.create(accountCapsule.getData(),
        Type.VALUE_TYPE_DIRTY | accountCache.get(key).getType().getType());
    accountCache.put(key, V);
    return accountCapsule.getAssetMapV2().get(new String(tokenIdWithoutLeadingZero));
  }

  @Override
  public synchronized long addBalance(byte[] address, long value) {
    AccountCapsule accountCapsule = getAccount(address);
    if (accountCapsule == null) {
      accountCapsule = createAccount(address, Protocol.AccountType.Normal);
    }

    long balance = accountCapsule.getBalance();
    if (value == 0) {
      return balance;
    }

    if (value < 0 && balance < -value) {
      throw new RuntimeException(
          StringUtil.createReadableString(accountCapsule.createDbKey())
              + " insufficient balance");
    }
    accountCapsule.setBalance(Math.addExact(balance, value));
    Key key = Key.create(address);
    Value val = Value.create(accountCapsule.getData(),
        Type.VALUE_TYPE_DIRTY | accountCache.get(key).getType().getType());
    accountCache.put(key, val);
    return accountCapsule.getBalance();
  }

  /**
   * @param address address
   * @param tokenId tokenIdstr in assetV2map is a string like "1000001". So before using this
   * function, we need to do some conversion. usually we will use a DataWord as input. so the byte
   * tokenId should be like DataWord.shortHexWithoutZeroX().getbytes().
   */
  @Override
  public synchronized long getTokenBalance(byte[] address, byte[] tokenId) {
    AccountCapsule accountCapsule = getAccount(address);
    if (accountCapsule == null) {
      return 0;
    }
    String tokenStr = new String(ByteUtil.stripLeadingZeroes(tokenId));
    return accountCapsule.getAssetMapV2().getOrDefault(tokenStr, 0L);
  }

  @Override
  public TransactionCapsule getTransaction(byte[] trxHash) {
    Key key = Key.create(trxHash);
    if (transactionCache.containsKey(key)) {
      return transactionCache.get(key).getTransaction();
    }

    TransactionCapsule transactionCapsule;
    if (parent != null) {
      transactionCapsule = parent.getTransaction(trxHash);
    } else {
      try {
        transactionCapsule = getTransactionStore().get(trxHash);
      } catch (BadItemException e) {
        transactionCapsule = null;
      }
    }

    if (transactionCapsule != null) {
      transactionCache.put(key, Value.create(transactionCapsule.getData()));
    }
    return transactionCapsule;
  }

  @Override
  public BlockCapsule getBlock(byte[] blockHash) {
    Key key = Key.create(blockHash);
    if (blockCache.containsKey(key)) {
      return blockCache.get(key).getBlock();
    }

    BlockCapsule ret;
    try {
      if (parent != null) {
        ret = parent.getBlock(blockHash);
      } else {
        ret = getBlockStore().get(blockHash);
      }
    } catch (Exception e) {
      ret = null;
    }

    if (ret != null) {
      blockCache.put(key, Value.create(ret.getData()));
    }
    return ret;
  }

  @Override
  public void putAccount(Key key, Value value) {
    accountCache.put(key, value);
  }

  @Override
  public void putTransaction(Key key, Value value) {
    transactionCache.put(key, value);
  }

  @Override
  public void putBlock(Key key, Value value) {
    blockCache.put(key, value);
  }

  @Override
  public void putWitness(Key key, Value value) {
    witnessCache.put(key, value);
  }

  @Override
  public void putCode(Key key, Value value) {
    codeCache.put(key, value);
  }

  @Override
  public void putContract(Key key, Value value) {
    contractCache.put(key, value);
  }

  @Override
  public void putStorage(Key key, Storage cache) {
    storageCache.put(key, cache);
  }

  @Override
  public void putVotes(Key key, Value value) {
    votesCache.put(key, value);
  }

  @Override
  public void putProposal(Key key, Value value) {
    proposalCache.put(key, value);
  }

  @Override
  public void putDynamicProperties(Key key, Value value) {
    dynamicPropertiesCache.put(key, value);
  }

  @Override
  public long getLatestProposalNum() {
    return Longs.fromByteArray(getDynamic(LATEST_PROPOSAL_NUM).getData());
  }

  @Override
  public long getWitnessAllowanceFrozenTime() {
    byte[] frozenTime = getDynamic(WITNESS_ALLOWANCE_FROZEN_TIME).getData();
    if (frozenTime.length >= 8) {
      return Longs.fromByteArray(getDynamic(WITNESS_ALLOWANCE_FROZEN_TIME).getData());
    }

    byte[] result = new byte[8];
    System.arraycopy(frozenTime, 0, result, 8 - frozenTime.length, frozenTime.length);
    return Longs.fromByteArray(result);

  }

  @Override
  public long getMaintenanceTimeInterval() {
    return Longs.fromByteArray(getDynamic(MAINTENANCE_TIME_INTERVAL).getData());
  }

  @Override
  public long getNextMaintenanceTime() {
    return Longs.fromByteArray(getDynamic(NEXT_MAINTENANCE_TIME).getData());
  }

  public BytesCapsule getDynamic(byte[] word) {
    Key key = Key.create(word);
    if (dynamicPropertiesCache.containsKey(key)) {
      return dynamicPropertiesCache.get(key).getDynamicProperties();
    }

    BytesCapsule bytesCapsule;
    if (parent != null) {
      bytesCapsule = parent.getDynamic(word);
    } else {
      try {
        bytesCapsule = getDynamicPropertiesStore().get(word);
      } catch (BadItemException | ItemNotFoundException e) {
        logger.warn("Dynamic property not found:" + Strings.fromUTF8ByteArray(word));
        bytesCapsule = null;
      }
    }

    if (bytesCapsule != null) {
      dynamicPropertiesCache.put(key, Value.create(bytesCapsule.getData()));
    }
    return bytesCapsule;
  }

  private void commitAccountCache(Deposit deposit) {
    accountCache.forEach((key, value) -> {
      if (value.getType().isCreate() || value.getType().isDirty()) {
        if (deposit != null) {
          deposit.putAccount(key, value);
        } else {
          getAccountStore().put(key.getData(), value.getAccount());
        }
      }
    });
  }

  private void commitTransactionCache(Deposit deposit) {
    transactionCache.forEach((key, value) -> {
      if (value.getType().isDirty() || value.getType().isCreate()) {
        if (deposit != null) {
          deposit.putTransaction(key, value);
        } else {
          getTransactionStore().put(key.getData(), value.getTransaction());
        }
      }
    });
  }

  private void commitBlockCache(Deposit deposit) {
    blockCache.forEach(((key, value) -> {
      if (value.getType().isDirty() || value.getType().isCreate()) {
        if (deposit != null) {
          deposit.putBlock(key, value);
        } else {
          getBlockStore().put(key.getData(), value.getBlock());
        }
      }
    }));
  }

  private void commitWitnessCache(Deposit deposit) {
    witnessCache.forEach(((key, value) -> {
      if (value.getType().isDirty() || value.getType().isCreate()) {
        if (deposit != null) {
          deposit.putWitness(key, value);
        } else {
          getWitnessStore().put(key.getData(), value.getWitness());
        }
      }
    }));
  }

  private void commitCodeCache(Deposit deposit) {
    codeCache.forEach(((key, value) -> {
      if (value.getType().isDirty() || value.getType().isCreate()) {
        if (deposit != null) {
          deposit.putCode(key, value);
        } else {
          getCodeStore().put(key.getData(), value.getCode());
        }
      }
    }));
  }

  private void commitContractCache(Deposit deposit) {
    contractCache.forEach(((key, value) -> {
      if (value.getType().isDirty() || value.getType().isCreate()) {
        if (deposit != null) {
          deposit.putContract(key, value);
        } else {
          getContractStore().put(key.getData(), value.getContract());
        }
      }
    }));
  }

  private void commitStorageCache(Deposit deposit) {
    storageCache.forEach((Key address, Storage storage) -> {
      if (deposit != null) {
        // write to parent cache
        deposit.putStorage(address, storage);
      } else {
        // persistence
        storage.commit();
      }
    });

  }

  private void commitVoteCache(Deposit deposit) {
    votesCache.forEach(((key, value) -> {
      if (value.getType().isDirty() || value.getType().isCreate()) {
        if (deposit != null) {
          deposit.putVotes(key, value);
        } else {
          getVotesStore().put(key.getData(), value.getVotes());
        }
      }
    }));
  }

  private void commitProposalCache(Deposit deposit) {
    proposalCache.forEach(((key, value) -> {
      if (value.getType().isDirty() || value.getType().isCreate()) {
        if (deposit != null) {
          deposit.putProposal(key, value);
        } else {
          getProposalStore().put(key.getData(), value.getProposal());
        }
      }
    }));
  }

  private void commitDynamicPropertiesCache(Deposit deposit) {
    dynamicPropertiesCache.forEach(((key, value) -> {
      if (value.getType().isDirty() || value.getType().isCreate()) {
        if (deposit != null) {
          deposit.putDynamicProperties(key, value);
        } else {
          getDynamicPropertiesStore().put(key.getData(), value.getDynamicProperties());
        }
      }
    }));
  }

  @Override
  public void putAccountValue(byte[] address, AccountCapsule accountCapsule) {
    Key key = new Key(address);
    accountCache.put(key, new Value(accountCapsule.getData(), Type.VALUE_TYPE_CREATE));
  }

  @Override
  public void putVoteValue(byte[] address, VotesCapsule votesCapsule) {
    Key key = new Key(address);
    votesCache.put(key, new Value(votesCapsule.getData(), Type.VALUE_TYPE_CREATE));
  }

  @Override
  public void putProposalValue(byte[] address, ProposalCapsule proposalCapsule) {
    Key key = new Key(address);
    proposalCache.put(key, new Value(proposalCapsule.getData(), Type.VALUE_TYPE_CREATE));
  }

  @Override
  public void putDynamicPropertiesWithLatestProposalNum(long num) {
    Key key = new Key(LATEST_PROPOSAL_NUM);
    dynamicPropertiesCache.put(key,
        new Value(new BytesCapsule(ByteArray.fromLong(num)).getData(), Type.VALUE_TYPE_CREATE));
  }

  @Override
  public synchronized void commit() {
    Deposit deposit = null;
    if (parent != null) {
      deposit = parent;
    }

    commitAccountCache(deposit);
    commitTransactionCache(deposit);
    commitBlockCache(deposit);
    commitWitnessCache(deposit);
    commitCodeCache(deposit);
    commitContractCache(deposit);
    commitStorageCache(deposit);
    commitVoteCache(deposit);
    commitProposalCache(deposit);
    commitDynamicPropertiesCache(deposit);
  }

  @Override
  public void setParent(Deposit deposit) {
    parent = deposit;
  }

  @Override
  public AccountCapsule createNormalAccount(byte[] address) {
    boolean withDefaultPermission =
        getDynamicPropertiesStore().getAllowMultiSign() == 1;
    Key key = new Key(address);
    AccountCapsule account = new AccountCapsule(ByteString.copyFrom(address), AccountType.Normal,
        getDynamicPropertiesStore().getLatestBlockHeaderTimestamp(), withDefaultPermission,
        getDynamicPropertiesStore());

    accountCache.put(key, new Value(account.getData(), Type.VALUE_TYPE_CREATE));
    return account;
  }

}

