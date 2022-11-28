package org.tron.core.vm.repository;

import static java.lang.Long.max;
import static org.tron.core.config.Parameter.ChainConstant.BLOCK_PRODUCED_INTERVAL;
import static org.tron.core.config.Parameter.ChainConstant.TRX_PRECISION;

import com.google.protobuf.ByteString;
import java.util.HashMap;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.tuweni.bytes.Bytes32;
import org.bouncycastle.util.Strings;
import org.bouncycastle.util.encoders.Hex;
import org.tron.common.crypto.Hash;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.ByteUtil;
import org.tron.common.utils.StorageUtils;
import org.tron.common.utils.StringUtil;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.capsule.CodeCapsule;
import org.tron.core.capsule.ContractCapsule;
import org.tron.core.capsule.ContractStateCapsule;
import org.tron.core.capsule.DelegatedResourceAccountIndexCapsule;
import org.tron.core.capsule.DelegatedResourceCapsule;
import org.tron.core.capsule.VotesCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.config.Parameter;
import org.tron.core.db.TransactionTrace;
import org.tron.core.state.WorldStateQueryInstance;
import org.tron.core.store.AccountStore;
import org.tron.core.store.AssetIssueStore;
import org.tron.core.store.AssetIssueV2Store;
import org.tron.core.store.DelegationStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.store.StoreFactory;
import org.tron.core.vm.config.VMConfig;
import org.tron.core.vm.program.Storage;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.DelegatedResource;
import org.tron.protos.Protocol.Votes;
import org.tron.protos.contract.AssetIssueContractOuterClass.AssetIssueContract;
import org.tron.protos.contract.Common;
import org.tron.protos.contract.SmartContractOuterClass;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;

@Slf4j(topic = "Repository")
public class RepositoryStateImpl implements Repository {

  private final long precision = Parameter.ChainConstant.PRECISION;

  private Bytes32 rootHash;
  private StoreFactory storeFactory;

  @Getter
  private final WorldStateQueryInstance worldStateQueryInstance;

  private Repository parent = null;

  private final HashMap<Key, Value<Account>> accountCache = new HashMap<>();
  private final HashMap<Key, Value<byte[]>> codeCache = new HashMap<>();
  private final HashMap<Key, Value<SmartContract>> contractCache = new HashMap<>();
  private final HashMap<Key, Value<SmartContractOuterClass.ContractState>> contractStateCache
          = new HashMap<>();
  private final HashMap<Key, Storage> storageCache = new HashMap<>();

  private final HashMap<Key, Value<AssetIssueContract>> assetIssueCache = new HashMap<>();
  private final HashMap<Key, Value<byte[]>> dynamicPropertiesCache = new HashMap<>();
  private final HashMap<Key, Value<DelegatedResource>> delegatedResourceCache = new HashMap<>();
  private final HashMap<Key, Value<Votes>> votesCache = new HashMap<>();
  private final HashMap<Key, Value<byte[]>> delegationCache = new HashMap<>();
  private final HashMap<Key, Value<Protocol.DelegatedResourceAccountIndex>>
          delegatedResourceAccountIndexCache = new HashMap<>();

  public RepositoryStateImpl(StoreFactory storeFactory, RepositoryStateImpl repository,
                             Bytes32 rootHash) {
    this.rootHash = rootHash;
    this.worldStateQueryInstance = new WorldStateQueryInstance(rootHash,
            storeFactory.getChainBaseManager());
    init(storeFactory, repository);
  }

  public static RepositoryStateImpl createRoot(StoreFactory storeFactory, Bytes32 rootHash) {
    return new RepositoryStateImpl(storeFactory, null, rootHash);
  }

  protected void init(StoreFactory storeFactory, RepositoryStateImpl parent) {
    if (storeFactory != null) {
      this.storeFactory = storeFactory;
    }
    this.parent = parent;
  }

  @Override
  public Repository newRepositoryChild() {
    return new RepositoryStateImpl(storeFactory, this, this.rootHash);
  }

  @Override
  public long getAccountLeftEnergyFromFreeze(AccountCapsule accountCapsule) {
    long now = getHeadSlot();

    long energyUsage = accountCapsule.getEnergyUsage();
    long latestConsumeTime = accountCapsule.getAccountResource().getLatestConsumeTimeForEnergy();
    long energyLimit = calculateGlobalEnergyLimit(accountCapsule);

    long windowSize = accountCapsule.getWindowSize(Common.ResourceCode.ENERGY);
    long newEnergyUsage = recover(energyUsage, latestConsumeTime, now, windowSize);

    return max(energyLimit - newEnergyUsage, 0); // us
  }

  @Override
  public long getAccountEnergyUsage(AccountCapsule accountCapsule) {
    long now = getHeadSlot();
    long energyUsage = accountCapsule.getEnergyUsage();
    long latestConsumeTime = accountCapsule.getAccountResource().getLatestConsumeTimeForEnergy();

    long accountWindowSize = accountCapsule.getWindowSize(Common.ResourceCode.ENERGY);

    return recover(energyUsage, latestConsumeTime, now, accountWindowSize);
  }

  @Override
  public Pair<Long, Long> getAccountEnergyUsageBalanceAndRestoreSeconds(AccountCapsule accountCapsule) {
    long now = getHeadSlot();

    long energyUsage = accountCapsule.getEnergyUsage();
    long latestConsumeTime = accountCapsule.getAccountResource().getLatestConsumeTimeForEnergy();
    long accountWindowSize = accountCapsule.getWindowSize(Common.ResourceCode.ENERGY);

    if (now >= latestConsumeTime + accountWindowSize) {
      return Pair.of(0L, 0L);
    }

    long restoreSlots = latestConsumeTime + accountWindowSize - now;

    long newEnergyUsage = recover(energyUsage, latestConsumeTime, now, accountWindowSize);

    long totalEnergyLimit = getDynamicPropertiesStore().getTotalEnergyCurrentLimit();
    long totalEnergyWeight = getTotalEnergyWeight();

    long balance = (long) ((double) newEnergyUsage * totalEnergyWeight / totalEnergyLimit * TRX_PRECISION);

    return Pair.of(balance, restoreSlots * BLOCK_PRODUCED_INTERVAL / 1_000);
  }

  @Override
  public Pair<Long, Long> getAccountNetUsageBalanceAndRestoreSeconds(AccountCapsule accountCapsule) {
    long now = getHeadSlot();

    long netUsage = accountCapsule.getNetUsage();
    long latestConsumeTime = accountCapsule.getLatestConsumeTime();
    long accountWindowSize = accountCapsule.getWindowSize(Common.ResourceCode.BANDWIDTH);

    if (now >= latestConsumeTime + accountWindowSize) {
      return Pair.of(0L, 0L);
    }

    long restoreSlots = latestConsumeTime + accountWindowSize - now;

    long newNetUsage = recover(netUsage, latestConsumeTime, now, accountWindowSize);

    long totalNetLimit = getDynamicPropertiesStore().getTotalNetLimit();
    long totalNetWeight = getTotalNetWeight();

    long balance = (long) ((double) newNetUsage * totalNetWeight / totalNetLimit * TRX_PRECISION);

    return Pair.of(balance, restoreSlots * BLOCK_PRODUCED_INTERVAL / 1_000);
  }

  @Override
  public AssetIssueCapsule getAssetIssue(byte[] tokenId) {
    byte[] tokenIdWithoutLeadingZero = ByteUtil.stripLeadingZeroes(tokenId);
    Key key = Key.create(tokenIdWithoutLeadingZero);
    if (assetIssueCache.containsKey(key)) {
      return new AssetIssueCapsule(assetIssueCache.get(key).getValue());
    }

    AssetIssueCapsule assetIssueCapsule;
    if (this.parent != null) {
      assetIssueCapsule = parent.getAssetIssue(tokenIdWithoutLeadingZero);
    } else {
      assetIssueCapsule = worldStateQueryInstance.getAssetIssue(tokenIdWithoutLeadingZero);
    }
    if (assetIssueCapsule != null) {
      assetIssueCache.put(key, Value.create(assetIssueCapsule));
    }
    return assetIssueCapsule;
  }

  @Override
  public AssetIssueV2Store getAssetIssueV2Store() {
    throw new UnsupportedOperationException();
  }

  @Override
  public AssetIssueStore getAssetIssueStore() {
    throw new UnsupportedOperationException();
  }

  @Override
  public DynamicPropertiesStore getDynamicPropertiesStore() {
    throw new UnsupportedOperationException();
  }

  @Override
  public DelegationStore getDelegationStore() {
    throw new UnsupportedOperationException();
  }

  @Override
  public AccountCapsule createAccount(byte[] address, Protocol.AccountType type) {
    Key key = new Key(address);
    AccountCapsule account = new AccountCapsule(ByteString.copyFrom(address), type);
    accountCache.put(key, Value.create(account, Type.CREATE));
    return account;
  }

  @Override
  public AccountCapsule createAccount(byte[] address, String accountName,
                                      Protocol.AccountType type) {
    Key key = new Key(address);
    AccountCapsule account = new AccountCapsule(ByteString.copyFrom(address),
        ByteString.copyFromUtf8(accountName),
        type);
    accountCache.put(key, Value.create(account, Type.CREATE));
    return account;
  }

  @Override
  public AccountCapsule getAccount(byte[] address) {
    Key key = new Key(address);
    if (accountCache.containsKey(key)) {
      return new AccountCapsule(accountCache.get(key).getValue());
    }

    AccountCapsule accountCapsule;
    if (parent != null) {
      accountCapsule = parent.getAccount(address);
    } else {
      accountCapsule = worldStateQueryInstance.getAccount(address);
    }

    if (accountCapsule != null) {
      accountCache.put(key, Value.create(accountCapsule));
    }
    return accountCapsule;
  }

  @Override
  public BytesCapsule getDynamicProperty(byte[] word) {
    Key key = Key.create(word);
    if (dynamicPropertiesCache.containsKey(key)) {
      return new BytesCapsule(dynamicPropertiesCache.get(key).getValue());
    }

    BytesCapsule bytesCapsule;
    if (parent != null) {
      bytesCapsule = parent.getDynamicProperty(word);
    } else {
      try {
        bytesCapsule = worldStateQueryInstance.getDynamicProperty(word);
      } catch (IllegalArgumentException e) {
        logger.warn("Not found dynamic property:" + Strings.fromUTF8ByteArray(word));
        bytesCapsule = null;
      }
    }

    if (bytesCapsule != null) {
      dynamicPropertiesCache.put(key, Value.create(bytesCapsule.getData()));
    }
    return bytesCapsule;
  }

  @Override
  public DelegatedResourceCapsule getDelegatedResource(byte[] key) {
    Key cacheKey = new Key(key);
    if (delegatedResourceCache.containsKey(cacheKey)) {
      return new DelegatedResourceCapsule(delegatedResourceCache.get(cacheKey).getValue());
    }

    DelegatedResourceCapsule delegatedResourceCapsule;
    if (parent != null) {
      delegatedResourceCapsule = parent.getDelegatedResource(key);
    } else {
      delegatedResourceCapsule = worldStateQueryInstance.getDelegatedResource(key);
    }

    if (delegatedResourceCapsule != null) {
      delegatedResourceCache.put(cacheKey, Value.create(delegatedResourceCapsule));
    }
    return delegatedResourceCapsule;
  }

  @Override
  public VotesCapsule getVotes(byte[] address) {
    Key cacheKey = new Key(address);
    if (votesCache.containsKey(cacheKey)) {
      return new VotesCapsule(votesCache.get(cacheKey).getValue());
    }

    VotesCapsule votesCapsule;
    if (parent != null) {
      votesCapsule = parent.getVotes(address);
    } else {
      votesCapsule = worldStateQueryInstance.getVotes(address);
    }

    if (votesCapsule != null) {
      votesCache.put(cacheKey, Value.create(votesCapsule));
    }
    return votesCapsule;
  }

  @Override
  public WitnessCapsule getWitness(byte[] address) {
    return worldStateQueryInstance.getWitness(address);
  }

  @Override
  public long getBeginCycle(byte[] address) {
    Key cacheKey = new Key(address);
    BytesCapsule bytesCapsule = getDelegation(cacheKey);
    return bytesCapsule == null ? 0 : ByteArray.toLong(bytesCapsule.getData());
  }

  @Override
  public long getEndCycle(byte[] address) {
    byte[] key = ("end-" + Hex.toHexString(address)).getBytes();
    Key cacheKey = new Key(key);
    BytesCapsule bytesCapsule = getDelegation(cacheKey);
    return bytesCapsule == null ? DelegationStore.REMARK : ByteArray.toLong(bytesCapsule.getData());
  }

  @Override
  public AccountCapsule getAccountVote(long cycle, byte[] address) {
    byte[] key = (cycle + "-" + Hex.toHexString(address) + "-account-vote").getBytes();
    Key cacheKey = new Key(key);
    BytesCapsule bytesCapsule = getDelegation(cacheKey);
    if (bytesCapsule == null) {
      return null;
    } else {
      return new AccountCapsule(bytesCapsule.getData());
    }
  }

  @Override
  public BytesCapsule getDelegation(Key key) {
    if (delegationCache.containsKey(key)) {
      return new BytesCapsule(delegationCache.get(key).getValue());
    }
    BytesCapsule bytesCapsule;
    if (parent != null) {
      bytesCapsule = parent.getDelegation(key);
    } else {
      bytesCapsule = worldStateQueryInstance.getDelegation(key.getData());
    }
    if (bytesCapsule != null) {
      delegationCache.put(key, Value.create(bytesCapsule.getData()));
    }
    return bytesCapsule;
  }

  @Override
  public DelegatedResourceAccountIndexCapsule getDelegatedResourceAccountIndex(byte[] key) {
    Key cacheKey = new Key(key);
    if (delegatedResourceAccountIndexCache.containsKey(cacheKey)) {
      return new DelegatedResourceAccountIndexCapsule(
              delegatedResourceAccountIndexCache.get(cacheKey).getValue());
    }

    DelegatedResourceAccountIndexCapsule delegatedResourceAccountIndexCapsule;
    if (parent != null) {
      delegatedResourceAccountIndexCapsule = parent.getDelegatedResourceAccountIndex(key);
    } else {
      delegatedResourceAccountIndexCapsule = worldStateQueryInstance
              .getDelegatedResourceAccountIndex(key);
    }

    if (delegatedResourceAccountIndexCapsule != null) {
      delegatedResourceAccountIndexCache.put(
              cacheKey, Value.create(delegatedResourceAccountIndexCapsule));
    }
    return delegatedResourceAccountIndexCapsule;
  }


  @Override
  public void deleteContract(byte[] address) {
    throw new UnsupportedOperationException();

  }

  @Override
  public void createContract(byte[] address, ContractCapsule contractCapsule) {
    contractCache.put(Key.create(address),
        Value.create(contractCapsule, Type.CREATE));
  }

  @Override
  public ContractCapsule getContract(byte[] address) {
    Key key = Key.create(address);
    if (contractCache.containsKey(key)) {
      return new ContractCapsule(contractCache.get(key).getValue());
    }

    ContractCapsule contractCapsule;
    if (parent != null) {
      contractCapsule = parent.getContract(address);
    } else {
      contractCapsule = worldStateQueryInstance.getContract(address);
    }

    if (contractCapsule != null) {
      contractCache.put(key, Value.create(contractCapsule));
    }
    return contractCapsule;
  }

  @Override
  public ContractStateCapsule getContractState(byte[] address) {
    Key key = Key.create(address);
    if (contractStateCache.containsKey(key)) {
      return new ContractStateCapsule(contractStateCache.get(key).getValue());
    }

    ContractStateCapsule contractStateCapsule;
    if (parent != null) {
      contractStateCapsule = parent.getContractState(address);
    } else {
      contractStateCapsule = worldStateQueryInstance.getContractState(address);
    }

    if (contractStateCapsule != null) {
      contractStateCache.put(key, Value.create(contractStateCapsule));
    }
    return contractStateCapsule;
  }

  @Override
  public void updateContract(byte[] address, ContractCapsule contractCapsule) {
    contractCache.put(Key.create(address),
        Value.create(contractCapsule, Type.DIRTY));
  }

  @Override
  public void updateContractState(byte[] address, ContractStateCapsule contractStateCapsule) {
    contractStateCache.put(Key.create(address),
            Value.create(contractStateCapsule, Type.DIRTY));
  }

  @Override
  public void updateAccount(byte[] address, AccountCapsule accountCapsule) {
    accountCache.put(Key.create(address),
        Value.create(accountCapsule, Type.DIRTY));
  }

  @Override
  public void updateDynamicProperty(byte[] word, BytesCapsule bytesCapsule) {
    dynamicPropertiesCache.put(Key.create(word),
        Value.create(bytesCapsule.getData(), Type.DIRTY));
  }

  @Override
  public void updateDelegatedResource(byte[] word,
                                      DelegatedResourceCapsule delegatedResourceCapsule) {
    delegatedResourceCache.put(Key.create(word),
        Value.create(delegatedResourceCapsule, Type.DIRTY));
  }

  @Override
  public void updateVotes(byte[] word, VotesCapsule votesCapsule) {
    votesCache.put(Key.create(word),
        Value.create(votesCapsule, Type.DIRTY));
  }

  @Override
  public void updateBeginCycle(byte[] word, long cycle) {
    updateDelegation(word, new BytesCapsule(ByteArray.fromLong(cycle)));
  }

  @Override
  public void updateEndCycle(byte[] word, long cycle) {
    BytesCapsule bytesCapsule = new BytesCapsule(ByteArray.fromLong(cycle));
    byte[] key = ("end-" + Hex.toHexString(word)).getBytes();
    updateDelegation(key, bytesCapsule);
  }

  @Override
  public void updateAccountVote(byte[] word, long cycle, AccountCapsule accountCapsule) {
    BytesCapsule bytesCapsule = new BytesCapsule(accountCapsule.getData());
    byte[] key = (cycle + "-" + Hex.toHexString(word) + "-account-vote").getBytes();
    updateDelegation(key, bytesCapsule);
  }

  @Override
  public void updateDelegation(byte[] word, BytesCapsule bytesCapsule) {
    delegationCache.put(Key.create(word),
        Value.create(bytesCapsule.getData(), Type.DIRTY));
  }

  @Override
  public void updateDelegatedResourceAccountIndex(byte[] word,
                 DelegatedResourceAccountIndexCapsule delegatedResourceAccountIndexCapsule) {
    delegatedResourceAccountIndexCache.put(
            Key.create(word), Value.create(delegatedResourceAccountIndexCapsule, Type.DIRTY));
  }

  @Override
  public void saveCode(byte[] address, byte[] code) {
    codeCache.put(Key.create(address), Value.create(code, Type.CREATE));

    if (VMConfig.allowTvmConstantinople()) {
      ContractCapsule contract = getContract(address);
      byte[] codeHash = Hash.sha3(code);
      contract.setCodeHash(codeHash);
      updateContract(address, contract);
    }
  }

  @Override
  public byte[] getCode(byte[] address) {
    Key key = Key.create(address);
    if (codeCache.containsKey(key)) {
      return codeCache.get(key).getValue();
    }

    byte[] code;
    if (parent != null) {
      code = parent.getCode(address);
    } else {
      CodeCapsule c = worldStateQueryInstance.getCode(address);
      code = null == c ? null : c.getData();
    }
    if (code != null) {
      codeCache.put(key, Value.create(code));
    }
    return code;
  }

  @Override
  public void putStorageValue(byte[] address, DataWord key, DataWord value) {
    Storage storage = getStorageInternal(address);
    if (storage != null) {
      storage.put(key, value);
    }
  }

  @Override
  public DataWord getStorageValue(byte[] address, DataWord key) {
    Storage storage = getStorageInternal(address);
    return storage == null ? null : storage.getValue(key);
  }

  private Storage getStorageInternal(byte[] address) {
    address = TransactionTrace.convertToTronAddress(address);
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
    return storage;
  }

  @Override
  public Storage getStorage(byte[] address) {
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
      storage = new Storage(address, worldStateQueryInstance);
    }
    ContractCapsule contract = getContract(address);
    if (contract != null) {
      storage.setContractVersion(contract.getContractVersion());
      if (!ByteUtil.isNullOrZeroArray(contract.getTrxHash())) {
        storage.generateAddrHash(contract.getTrxHash());
      }
    }
    return storage;
  }

  @Override
  public long getBalance(byte[] address) {
    AccountCapsule accountCapsule = getAccount(address);
    return accountCapsule == null ? 0L : accountCapsule.getBalance();
  }

  @Override
  public long addBalance(byte[] address, long value) {
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
    accountCache.put(key, Value.create(accountCapsule,
        accountCache.get(key).getType().addType(Type.DIRTY)));
    return accountCapsule.getBalance();
  }

  @Override
  public void setParent(Repository repository) {
    parent = repository;
  }

  @Override
  public void commit() {
    Repository repository = null;
    if (parent != null) {
      repository = parent;
    }
    commitAccountCache(repository);
    commitCodeCache(repository);
    commitContractCache(repository);
    commitContractStateCache(repository);
    commitStorageCache(repository);
    commitDynamicCache(repository);
    commitDelegatedResourceCache(repository);
    commitVotesCache(repository);
    commitDelegationCache(repository);
    commitDelegatedResourceAccountIndexCache(repository);
  }

  @Override
  public void putAccount(Key key, Value value) {
    accountCache.put(key, value);
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
  public void putContractState(Key key, Value value) {
    contractStateCache.put(key, value);
  }

  @Override
  public void putStorage(Key key, Storage cache) {
    storageCache.put(key, cache);
  }

  @Override
  public void putAccountValue(byte[] address, AccountCapsule accountCapsule) {
    accountCache.put(new Key(address),
        Value.create(accountCapsule, Type.CREATE));
  }

  @Override
  public void putDynamicProperty(Key key, Value value) {
    dynamicPropertiesCache.put(key, value);
  }

  @Override
  public void putDelegatedResource(Key key, Value value) {
    delegatedResourceCache.put(key, value);
  }

  @Override
  public void putVotes(Key key, Value value) {
    votesCache.put(key, value);
  }

  @Override
  public void putDelegation(Key key, Value value) {
    delegationCache.put(key, value);
  }

  @Override
  public void putDelegatedResourceAccountIndex(Key key, Value value) {
    delegatedResourceAccountIndexCache.put(key, value);
  }

  @Override
  public long addTokenBalance(byte[] address, byte[] tokenId, long value) {
    byte[] tokenIdWithoutLeadingZero = ByteUtil.stripLeadingZeroes(tokenId);
    AccountCapsule accountCapsule = getAccount(address);
    if (accountCapsule == null) {
      accountCapsule = createAccount(address, Protocol.AccountType.Normal);
    }
    long balance = accountCapsule.getAssetV2(new String(tokenIdWithoutLeadingZero));
    if (value == 0) {
      return balance;
    }

    if (value < 0 && balance < -value) {
      throw new RuntimeException(
          StringUtil.createReadableString(accountCapsule.createDbKey())
              + " insufficient balance");
    }
    if (value >= 0) {
      accountCapsule.addAssetAmountV2(tokenIdWithoutLeadingZero, value, getDynamicPropertiesStore(),
          getAssetIssueStore());
    } else {
      accountCapsule
          .reduceAssetAmountV2(tokenIdWithoutLeadingZero, -value, getDynamicPropertiesStore(),
              getAssetIssueStore());
    }
    Key key = Key.create(address);
    accountCache.put(key, Value.create(accountCapsule,
        accountCache.get(key).getType().addType(Type.DIRTY)));
    return accountCapsule.getAssetV2(new String(tokenIdWithoutLeadingZero));
  }

  @Override
  public long getTokenBalance(byte[] address, byte[] tokenId) {
    AccountCapsule accountCapsule = getAccount(address);
    if (accountCapsule == null) {
      return 0;
    }
    String tokenStr = new String(ByteUtil.stripLeadingZeroes(tokenId));
    return accountCapsule.getAssetV2(tokenStr);
  }

  @Override
  public byte[] getBlackHoleAddress() {
    return AccountStore.getBlackholeAddress();
  }

  @Override
  public BlockCapsule getBlockByNum(long num) {
    throw new UnsupportedOperationException();
  }

  // new recover method, use personal window size.
  private long recover(long lastUsage, long lastTime, long now, long personalWindowSize) {
    return increase(lastUsage, 0, lastTime, now, personalWindowSize);
  }

  private long increase(long lastUsage, long usage, long lastTime, long now, long windowSize) {
    long averageLastUsage = divideCeil(lastUsage * precision, windowSize);
    long averageUsage = divideCeil(usage * precision, windowSize);

    if (lastTime != now) {
      assert now > lastTime;
      if (lastTime + windowSize > now) {
        long delta = now - lastTime;
        double decay = (windowSize - delta) / (double) windowSize;
        averageLastUsage = Math.round(averageLastUsage * decay);
      } else {
        averageLastUsage = 0;
      }
    }
    averageLastUsage += averageUsage;
    return getUsage(averageLastUsage, windowSize);
  }

  private long divideCeil(long numerator, long denominator) {
    return (numerator / denominator) + ((numerator % denominator) > 0 ? 1 : 0);
  }

  private long getUsage(long usage, long windowSize) {
    return usage * windowSize / precision;
  }

  @Override
  public long calculateGlobalEnergyLimit(AccountCapsule accountCapsule) {
    long frozeBalance = accountCapsule.getAllFrozenBalanceForEnergy();
    if (frozeBalance < 1_000_000L) {
      return 0;
    }
    long energyWeight = frozeBalance / 1_000_000L;
    long totalEnergyLimit = getWorldStateQueryInstance().getTotalEnergyCurrentLimit();
    long totalEnergyWeight = getWorldStateQueryInstance().getTotalEnergyWeight();

    assert totalEnergyWeight > 0;

    return (long) (energyWeight * ((double) totalEnergyLimit / totalEnergyWeight));
  }

  @Override
  public long getHeadSlot() {
    return getSlotByTimestampMs(getWorldStateQueryInstance().getLatestBlockHeaderTimestamp());
  }

  @Override
  public long getSlotByTimestampMs(long timestamp) {
    return (timestamp - Long.parseLong(CommonParameter.getInstance()
            .getGenesisBlock().getTimestamp()))
            / BLOCK_PRODUCED_INTERVAL;
  }

  private void commitAccountCache(Repository deposit) {
    accountCache.forEach((key, value) -> {
      if (value.getType().isCreate() || value.getType().isDirty()) {
        if (deposit != null) {
          deposit.putAccount(key, value);
        } else {
          throw new UnsupportedOperationException();
        }
      }
    });
  }

  private void commitCodeCache(Repository deposit) {
    codeCache.forEach(((key, value) -> {
      if (value.getType().isDirty() || value.getType().isCreate()) {
        if (deposit != null) {
          deposit.putCode(key, value);
        } else {
          throw new UnsupportedOperationException();
        }
      }
    }));
  }

  private void commitContractCache(Repository deposit) {
    contractCache.forEach(((key, value) -> {
      if (value.getType().isDirty() || value.getType().isCreate()) {
        if (deposit != null) {
          deposit.putContract(key, value);
        } else {
          throw new UnsupportedOperationException();
        }
      }
    }));
  }

  private void commitContractStateCache(Repository deposit) {
    contractStateCache.forEach(((key, value) -> {
      if (value.getType().isDirty() || value.getType().isCreate()) {
        if (deposit != null) {
          deposit.putContractState(key, value);
        } else {
         throw new UnsupportedOperationException();
        }
      }
    }));
  }

  private void commitStorageCache(Repository deposit) {
    storageCache.forEach((Key address, Storage storage) -> {
      if (deposit != null) {
        // write to parent cache
        deposit.putStorage(address, storage);
      } else {
        throw new UnsupportedOperationException();
      }
    });

  }

  private void commitDynamicCache(Repository deposit) {
    dynamicPropertiesCache.forEach(((key, value) -> {
      if (value.getType().isDirty() || value.getType().isCreate()) {
        if (deposit != null) {
          deposit.putDynamicProperty(key, value);
        } else {
          throw new UnsupportedOperationException();
        }
      }
    }));
  }

  private void commitDelegatedResourceCache(Repository deposit) {
    delegatedResourceCache.forEach(((key, value) -> {
      if (value.getType().isDirty() || value.getType().isCreate()) {
        if (deposit != null) {
          deposit.putDelegatedResource(key, value);
        } else {
          throw new UnsupportedOperationException();
        }
      }
    }));
  }

  private void commitVotesCache(Repository deposit) {
    votesCache.forEach(((key, value) -> {
      if (value.getType().isDirty() || value.getType().isCreate()) {
        if (deposit != null) {
          deposit.putVotes(key, value);
        } else {
          throw new UnsupportedOperationException();
        }
      }
    }));
  }

  private void commitDelegationCache(Repository deposit) {
    delegationCache.forEach((key, value) -> {
      if (value.getType().isDirty() || value.getType().isCreate()) {
        if (deposit != null) {
          deposit.putDelegation(key, value);
        } else {
          throw new UnsupportedOperationException();
        }
      }
    });
  }

  private void commitDelegatedResourceAccountIndexCache(Repository deposit) {
    delegatedResourceAccountIndexCache.forEach(((key, value) -> {
      if (value.getType().isDirty() || value.getType().isCreate()) {
        if (deposit != null) {
          deposit.putDelegatedResourceAccountIndex(key, value);
        } else {
          throw new UnsupportedOperationException();
        }
      }
    }));
  }

  // todo: check whether replace getDynamicPropertiesStore to worldStateQuery
  @Override
  public AccountCapsule createNormalAccount(byte[] address) {
    boolean withDefaultPermission =
        getDynamicPropertiesStore().getAllowMultiSign() == 1;
    Key key = new Key(address);
    AccountCapsule account = new AccountCapsule(ByteString.copyFrom(address), AccountType.Normal,
        getDynamicPropertiesStore().getLatestBlockHeaderTimestamp(), withDefaultPermission,
        getDynamicPropertiesStore());

    accountCache.put(key, Value.create(account, Type.CREATE));
    return account;
  }

  //The unit is trx
  @Override
  public void addTotalNetWeight(long amount) {
    long totalNetWeight = getTotalNetWeight();
    totalNetWeight += amount;
    saveTotalNetWeight(totalNetWeight);
  }

  //The unit is trx
  @Override
  public void addTotalEnergyWeight(long amount) {
    long totalEnergyWeight = getTotalEnergyWeight();
    totalEnergyWeight += amount;
    saveTotalEnergyWeight(totalEnergyWeight);
  }

  @Override
  public void addTotalTronPowerWeight(long amount) {
    long totalTronPowerWeight = getTotalTronPowerWeight();
    totalTronPowerWeight += amount;
    saveTotalTronPowerWeight(totalTronPowerWeight);
  }

  @Override
  public void saveTotalNetWeight(long totalNetWeight) {
    updateDynamicProperty(DynamicPropertiesStore.DynamicResourceProperties.TOTAL_NET_WEIGHT,
        new BytesCapsule(ByteArray.fromLong(totalNetWeight)));
  }

  @Override
  public void saveTotalEnergyWeight(long totalEnergyWeight) {
    updateDynamicProperty(DynamicPropertiesStore.DynamicResourceProperties.TOTAL_ENERGY_WEIGHT,
        new BytesCapsule(ByteArray.fromLong(totalEnergyWeight)));
  }

  @Override
  public void saveTotalTronPowerWeight(long totalTronPowerWeight) {
    updateDynamicProperty(DynamicPropertiesStore.DynamicResourceProperties.TOTAL_TRON_POWER_WEIGHT,
            new BytesCapsule(ByteArray.fromLong(totalTronPowerWeight)));
  }

  @Override
  public long getTotalNetWeight() {
    return worldStateQueryInstance.getTotalNetWeight();
  }

  @Override
  public long getTotalEnergyWeight() {
    return worldStateQueryInstance.getTotalEnergyWeight();
  }

  @Override
  public long getTotalTronPowerWeight() {
    return worldStateQueryInstance.getTotalTronPowerWeight();
  }

}
