package org.tron.common.runtime.vm.cache;

import static org.tron.common.runtime.utils.MUtil.convertToTronAddress;

import com.google.protobuf.ByteString;
import java.util.HashMap;
import lombok.Getter;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.runtime.vm.program.Storage;
import org.tron.common.storage.Deposit;
import org.tron.common.utils.ByteArrayMap;
import org.tron.core.capsule.*;
import org.tron.core.db.Manager;
import org.tron.protos.Protocol;

public class CachedDepositImpl implements Deposit {

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

//  private HashMap<ByteArrayWrapper, Storage> storageCache = new HashMap<>();
  private HashMap<Key, Storage> storageCache = new HashMap<>();

  public static Deposit createRoot(Manager manager) {
    return new CachedDepositImpl(manager);
  }

  // for deposit root
  private CachedDepositImpl(Manager manager) {
    this.manager = manager;
    accountCache = new ReadWriteCapsuleCache<>(manager.getAccountStore());
    contractCache = new ReadWriteCapsuleCache<>(manager.getContractStore());
    transactionCache = new ReadWriteCapsuleCache<>(manager.getTransactionStore());
    witnessCache = new ReadWriteCapsuleCache<>(manager.getWitnessStore());
    codeCache = new ReadWriteCapsuleCache<>(manager.getCodeStore());
    blockCache = new ReadWriteCapsuleCache<>(manager.getBlockStore());
  }

  // for deposit child
  private CachedDepositImpl(Manager manager, CachedDepositImpl parent) {
    this.manager = manager;
    this.parent = parent;

    accountCache = new WriteCapsuleCache<>(parent.getAccountCache());
    contractCache = new WriteCapsuleCache<>(parent.getContractCache());
    transactionCache = new WriteCapsuleCache<>(parent.getTransactionCache());
    witnessCache = new WriteCapsuleCache<>(parent.getWitnessCache());
    codeCache = new WriteCapsuleCache<>(parent.getCodeCache());
    blockCache = new WriteCapsuleCache<>(parent.getBlockCache());
  }

  @Override
  public Deposit newDepositChild() {
    return new CachedDepositImpl(manager, this);
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
  public VotesCapsule getVotesCapsule(byte[] address) {
    return null;
  }

  @Override
  public ProposalCapsule getProposalCapsule(byte[] id) {
    return null;
  }

  @Override
  public BytesCapsule getDynamic(byte[] bytesKey) {
    return null;
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
    address = convertToTronAddress(address);
    if (getAccount(address) == null) {
      return;
    }
//    ByteArrayWrapper addressKey = new ByteArrayWrapper(address);
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
  public DataWord getStorageValue(byte[] address, DataWord key) {
    address = convertToTronAddress(address);
    if (getAccount(address) == null) {
      return null;
    }
    Key addressKey = Key.create(address);
//    ByteArrayWrapper addressKey = new ByteArrayWrapper(address);

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
  public Storage getStorage(byte[] address) {
//    ByteArrayWrapper key = new ByteArrayWrapper(address);
    Key key = Key.create(address);
    if (storageCache.containsKey(key)) {
      return storageCache.get(key);
    }

    Storage storage;
    if (this.parent != null) {
      storage = parent.getStorage(address);
    } else {
      storage = new Storage(address, this.manager.getStorageRowStore());
    }
    storageCache.put(key, storage);
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
  public void setParent(Deposit deposit) {
    this.parent = deposit;
  }

  @Override
  public void flush() {

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
  public void putAccount(Key key, Value value) {

  }

  @Override
  public void putTransaction(Key key, Value value) {

  }

  @Override
  public void putBlock(Key key, Value value) {

  }

  @Override
  public void putWitness(Key key, Value value) {

  }

  @Override
  public void putCode(Key key, Value value) {

  }

  @Override
  public void putContract(Key key, Value value) {

  }

  @Override
  public void putStorage(Key key, Storage cache) {

  }

  @Override
  public void putVotes(Key key, Value value) {

  }

  @Override
  public void putProposal(Key key, Value value) {

  }

  @Override
  public void putDynamicProperties(Key key, Value value) {

  }

  @Override
  public void putAccountValue(byte[] address, AccountCapsule accountCapsule) {

  }

  @Override
  public void putVoteValue(byte[] address, VotesCapsule votesCapsule) {

  }

  @Override
  public void putProposalValue(byte[] address, ProposalCapsule proposalCapsule) {

  }

  @Override
  public void putDynamicPropertiesWithLatestProposalNum(long num) {

  }

  @Override
  public long getLatestProposalNum() {
    return 0;
  }

  @Override
  public long getWitnessAllowanceFrozenTime() {
    return 0;
  }

  @Override
  public long getMaintenanceTimeInterval() {
    return 0;
  }

  @Override
  public long getNextMaintenanceTime() {
    return 0;
  }

  @Override
  public TransactionCapsule getTransaction(byte[] trxHash) {
    return this.getTransactionCache().get(trxHash);
  }

  @Override
  public BlockCapsule getBlock(byte[] blockHash) {
    return this.blockCache.get(blockHash);
  }

  private void commitStorageCache() {
    storageCache.forEach((key, value) -> {
      if (this.parent != null) {
        // write to parent cache
        this.parent.putStorage(key, value);
      } else {
        // persistence
        value.commit();
      }
    });

  }
}
