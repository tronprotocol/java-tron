package org.tron.common.runtime.vm;

import org.tron.common.runtime.vm.program.Storage;
import org.tron.common.utils.ByteArraySet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.ContractCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.db.Manager;
import org.tron.protos.Protocol;

public interface Deposit {

  Manager getDbManager();

  AccountCapsule createAccount(byte[] address, Protocol.AccountType type);

  AccountCapsule createAccount(byte[] address, String accountName, Protocol.AccountType type);

  AccountCapsule getAccount(byte[] address);

  WitnessCapsule getWitness(byte[] address);

  void deleteContract(byte[] address);

  void createContract(byte[] address, ContractCapsule contractCapsule);

  ContractCapsule getContract(byte[] address);

  void saveCode(byte[] codeHash, byte[] code);

  byte[] getCode(byte[] codeHash);

  void putStorageValue(byte[] address, DataWord key, DataWord value);

  DataWord getStorageValue(byte[] address, DataWord key);

  Storage getStorage(byte[] address);

  long getBalance(byte[] address);

  long addBalance(byte[] address, long value);

  Deposit newDepositChild();

  void commit();

  TransactionCapsule getTransaction(byte[] trxHash);

  BlockCapsule getBlock(byte[] blockHash);

  void putStorage(byte[] key, Storage value);

  ByteArraySet getStorageKeysToDelete();
}
