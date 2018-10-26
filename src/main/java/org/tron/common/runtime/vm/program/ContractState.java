/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.tron.common.runtime.vm.program;

import org.tron.common.runtime.vm.DataWord;
import org.tron.common.runtime.vm.program.invoke.ProgramInvoke;
import org.tron.common.runtime.vm.program.listener.ProgramListener;
import org.tron.common.runtime.vm.program.listener.ProgramListenerAware;
import org.tron.common.runtime.vm.Deposit;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.ContractCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.db.Manager;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.AccountType;

public class ContractState implements Deposit, ProgramListenerAware {

  private Deposit deposit;
  private final DataWord address;  // contract address
  private ProgramListener programListener;

  public ContractState(ProgramInvoke programInvoke) {
    this.address = programInvoke.getContractAddress(); // contract address
    this.deposit = programInvoke.getDeposit();
  }

  @Override
  public Manager getDbManager() {
    return deposit.getDbManager();
  }

  @Override
  public void setProgramListener(ProgramListener listener) {
    this.programListener = listener;
  }

  @Override
  public AccountCapsule createAccount(byte[] addr, Protocol.AccountType type) {
    return deposit.createAccount(addr, type);
  }

  @Override
  public AccountCapsule createAccount(byte[] address, String accountName, AccountType type) {
    return deposit.createAccount(address, accountName, type);
  }


  @Override
  public AccountCapsule getAccount(byte[] addr) {
    return deposit.getAccount(addr);
  }

  @Override
  public WitnessCapsule getWitness(byte[] address) {
    return deposit.getWitness(address);
  }

  @Override
  public void deleteContract(byte[] address) {
    deposit.deleteContract(address);
  }

  @Override
  public void createContract(byte[] codeHash, ContractCapsule contractCapsule) {
    deposit.createContract(codeHash, contractCapsule);
  }

  @Override
  public ContractCapsule getContract(byte[] codeHash) {
    return deposit.getContract(codeHash);
  }

  @Override
  public void saveCode(byte[] addr, byte[] code) {
    deposit.saveCode(addr, code);
  }

  @Override
  public byte[] getCode(byte[] addr) {
    return deposit.getCode(addr);
  }

  @Override
  public void putStorageValue(byte[] addr, DataWord key, DataWord value) {
    if (canListenTrace(addr)) {
      programListener.onStoragePut(key, value);
    }
    deposit.putStorageValue(addr, key, value);
  }

  private boolean canListenTrace(byte[] address) {
    return (programListener != null) && this.address.equals(new DataWord(address));
  }

  @Override
  public DataWord getStorageValue(byte[] addr, DataWord key) {
    return deposit.getStorageValue(addr, key);
  }

  @Override
  public long getBalance(byte[] addr) {
    return deposit.getBalance(addr);
  }

  @Override
  public long addBalance(byte[] addr, long value) {
    return deposit.addBalance(addr, value);
  }

  @Override
  public Deposit newDepositChild() {
    return deposit.newDepositChild();
  }

  @Override
  public void commit() {
    deposit.commit();
  }

  @Override
  public Storage getStorage(byte[] address) {
    return deposit.getStorage(address);
  }

  @Override
  public void putStorage(byte[] key, Storage cache) {
    deposit.putStorage(key, cache);
  }

  @Override
  public TransactionCapsule getTransaction(byte[] trxHash) {
    return this.deposit.getTransaction(trxHash);
  }

  @Override
  public BlockCapsule getBlock(byte[] blockHash) {
    return this.deposit.getBlock(blockHash);
  }

}
