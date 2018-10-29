package org.tron.common.runtime.vm;

import com.google.protobuf.ByteString;
import lombok.Getter;
import org.tron.common.runtime.vm.cache.CachedSource;
import org.tron.common.runtime.vm.cache.MemoryCache;
import org.tron.common.runtime.vm.cache.ReadWriteCapsuleCache;
import org.tron.common.runtime.vm.cache.WriteCapsuleCache;
import org.tron.common.runtime.vm.program.Storage;
import org.tron.common.utils.ByteArrayMap;
import org.tron.common.utils.ByteArraySet;
import org.tron.core.capsule.*;
import org.tron.core.db.Manager;
import org.tron.protos.Protocol;

public class DepositImpl implements Deposit {

  private Manager manager;
  private Deposit parent;

  @Getter
  private CachedSource<byte[], AccountCapsule> accountCache;
  @Getter
  private CachedSource<byte[], ContractCapsule> contractCache;
  @Getter
  private CachedSource<byte[], BlockCapsule> blockCache;
  @Getter
  private CachedSource<byte[], TransactionCapsule> transactionCache;
  @Getter
  private CachedSource<byte[], WitnessCapsule> witnessCache;
  @Getter
  private CachedSource<byte[], CodeCapsule> codeCache;

  @Getter
  private CachedSource<byte[], StorageRowCapsule> storageRowCache;
  private ByteArrayMap<Storage> storageMap = new ByteArrayMap<>();
  @Getter
  private ByteArraySet storageKeysToDelete = new ByteArraySet();

  public static Deposit createRoot(Manager manager) {
    return new DepositImpl(manager);
  }

  // for deposit root
  private DepositImpl(Manager manager) {
    this.manager = manager;
    if (manager == null) {
      // just for mock
      accountCache = new MemoryCache<>();
      contractCache = new MemoryCache<>();
      transactionCache = new MemoryCache<>();
      witnessCache = new MemoryCache<>();
      codeCache = new MemoryCache<>();
      blockCache = new MemoryCache<>();
    } else {
      accountCache = new ReadWriteCapsuleCache<>(manager.getAccountStore());
      contractCache = new ReadWriteCapsuleCache<>(manager.getContractStore());
      transactionCache = new ReadWriteCapsuleCache<>(manager.getTransactionStore());
      witnessCache = new ReadWriteCapsuleCache<>(manager.getWitnessStore());
      codeCache = new ReadWriteCapsuleCache<>(manager.getCodeStore());
      blockCache = new ReadWriteCapsuleCache<>(manager.getBlockStore());
      storageRowCache = new ReadWriteCapsuleCache<>(manager.getStorageRowStore());
    }
  }

  // for deposit child
  private DepositImpl(Manager manager, DepositImpl parent) {
    this.manager = manager;
    this.parent = parent;

    accountCache = new WriteCapsuleCache<>(parent.getAccountCache());
    contractCache = new WriteCapsuleCache<>(parent.getContractCache());
    transactionCache = new WriteCapsuleCache<>(parent.getTransactionCache());
    witnessCache = new WriteCapsuleCache<>(parent.getWitnessCache());
    codeCache = new WriteCapsuleCache<>(parent.getCodeCache());
    blockCache = new WriteCapsuleCache<>(parent.getBlockCache());
    storageRowCache = new WriteCapsuleCache<>(parent.getStorageRowCache());
  }

  @Override
  public Deposit newDepositChild() {
    return new DepositImpl(manager, this);
  }

  @Override
  public Manager getDbManager() {
    return manager;
  }

  @Override
  public AccountCapsule createAccount(byte[] address, Protocol.AccountType type) {
    AccountCapsule account = new AccountCapsule(ByteString.copyFrom(address), type);
    accountCache.put(address, account);
    return account;
  }

  @Override
  public AccountCapsule createAccount(byte[] address, String accountName, Protocol.AccountType type) {
    AccountCapsule account = new AccountCapsule(ByteString.copyFrom(address),
        ByteString.copyFromUtf8(accountName),
        type);
    accountCache.put(address, account);
    return account;
  }

  @Override
  public AccountCapsule getAccount(byte[] address) {
    return accountCache.get(address);
  }

  @Override
  public WitnessCapsule getWitness(byte[] address) {
    return witnessCache.get(address);
  }

  @Override
  public void deleteContract(byte[] address) {
    this.manager.getCodeStore().delete(address);
    this.manager.getAccountStore().delete(address);
    this.manager.getContractStore().delete(address);
  }

  @Override
  public void createContract(byte[] address, ContractCapsule contractCapsule) {
    contractCache.put(address, contractCapsule);
  }

  @Override
  public ContractCapsule getContract(byte[] address) {
    return contractCache.get(address);
  }

  @Override
  public void saveCode(byte[] codeHash, byte[] code) {
    codeCache.put(codeHash, new CodeCapsule(code));
  }

  @Override
  public byte[] getCode(byte[] codeHash) {
    return codeCache.get(codeHash).getData();
  }

  @Override
  public void putStorageValue(byte[] address, DataWord key, DataWord value) {
    getStorage(address).put(key, value);
  }

  @Override
  public DataWord getStorageValue(byte[] address, DataWord key) {
    return getStorage(address).getValue(key);
  }

  @Override
  public Storage getStorage(byte[] address) {
    if (storageMap.containsKey(address)) {
      return storageMap.get(address);
    }
    Storage storage = new Storage(address, this.storageRowCache);
    storageMap.put(address,storage);
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

    long balance = accountCapsule.getBalance();
    if (value == 0) {
      return balance;
    }

    accountCapsule.setBalance(Math.addExact(balance, value));
    accountCache.put(address, accountCapsule);
    return accountCapsule.getBalance();
  }

  @Override
  public void commit() {
    this.getAccountCache().commit();
    this.getTransactionCache().commit();
    this.getCodeCache().commit();
    this.getContractCache().commit();
    this.getWitnessCache().commit();
    this.getBlockCache().commit();
    commitStorageCache();
  }

  @Override
  public TransactionCapsule getTransaction(byte[] trxHash) {
    return this.getTransactionCache().get(trxHash);
  }

  @Override
  public BlockCapsule getBlock(byte[] blockHash) {
    return this.blockCache.get(blockHash);
  }

  @Override
  public void putStorage(byte[] key, Storage storage) {
    storageMap.put(key, storage);
  }

  @Override
  public ByteArraySet getStorageKeysToDelete() {
    return this.storageKeysToDelete;
  }

  private void commitStorageCache() {
      storageMap.forEach((key, value) -> {
        if (this.parent != null) {
          this.parent.getStorageKeysToDelete().addAll(value.getKeysToDelete());
        } else {
          this.getStorageKeysToDelete().addAll(value.getKeysToDelete());
        }
      });

    if (this.parent == null) {
      storageKeysToDelete.forEach(key -> {
        storageRowCache.put(key, null);
      });
    }
    storageRowCache.commit();
  }
}
