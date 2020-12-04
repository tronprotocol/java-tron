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
package org.tron.core.vm.program;


import org.tron.common.runtime.vm.DataWord;
import org.tron.common.utils.ByteArray;
import org.tron.core.capsule.*;
import org.tron.core.store.*;
import org.tron.core.vm.program.invoke.ProgramInvoke;
import org.tron.core.vm.program.listener.ProgramListener;
import org.tron.core.vm.program.listener.ProgramListenerAware;
import org.tron.core.vm.repository.Key;
import org.tron.core.vm.repository.Repository;
import org.tron.core.vm.repository.Value;
import org.tron.protos.Protocol.AccountType;

import java.util.Optional;

public class ContractState implements Repository, ProgramListenerAware {

  // contract address
  private final DataWord address;
  private Repository repository;
  private ProgramListener programListener;

  ContractState(ProgramInvoke programInvoke) {
    this.address = programInvoke.getContractAddress();
    this.repository = programInvoke.getDeposit();
  }

  @Override
  public void setProgramListener(ProgramListener listener) {
    this.programListener = listener;
  }

  @Override
  public AssetIssueCapsule getAssetIssue(byte[] tokenId) {
    return repository.getAssetIssue(tokenId);
  }

  @Override
  public AssetIssueV2Store getAssetIssueV2Store() {
    return repository.getAssetIssueV2Store();
  }

  @Override
  public AssetIssueStore getAssetIssueStore() {
    return repository.getAssetIssueStore();
  }

  @Override
  public DynamicPropertiesStore getDynamicPropertiesStore() {
    return repository.getDynamicPropertiesStore();
  }

  @Override
  public AccountCapsule createAccount(byte[] addr, AccountType type) {
    return repository.createAccount(addr, type);
  }

  @Override
  public AccountCapsule createAccount(byte[] address, String accountName, AccountType type) {
    return repository.createAccount(address, accountName, type);
  }


  @Override
  public AccountCapsule getAccount(byte[] addr) {
    return repository.getAccount(addr);
  }

  public BytesCapsule getDynamic(byte[] bytesKey) {
    return repository.getDynamic(bytesKey);
  }

  @Override
  public WitnessCapsule getWitnessCapsule(byte[] address) {
    return repository.getWitnessCapsule(address);
  }

  @Override
  public void deleteContract(byte[] address) {
    repository.deleteContract(address);
  }

  @Override
  public void createContract(byte[] codeHash, ContractCapsule contractCapsule) {
    repository.createContract(codeHash, contractCapsule);
  }

  @Override
  public ContractCapsule getContract(byte[] codeHash) {
    return repository.getContract(codeHash);
  }

  @Override
  public void updateContract(byte[] address, ContractCapsule contractCapsule) {
    repository.updateContract(address, contractCapsule);
  }

  @Override
  public void updateAccount(byte[] address, AccountCapsule accountCapsule) {
    repository.updateAccount(address, accountCapsule);
  }

  @Override
  public void saveCode(byte[] address, byte[] code) {
    repository.saveCode(address, code);
  }

  @Override
  public byte[] getCode(byte[] address) {
    return repository.getCode(address);
  }

  @Override
  public void putStorageValue(byte[] addr, DataWord key, DataWord value) {
    if (canListenTrace(addr)) {
      programListener.onStoragePut(key, value);
    }
    repository.putStorageValue(addr, key, value);
  }

  private boolean canListenTrace(byte[] address) {
    return (programListener != null) && this.address.equals(new DataWord(address));
  }

  @Override
  public DataWord getStorageValue(byte[] addr, DataWord key) {
    return repository.getStorageValue(addr, key);
  }

  @Override
  public long getBalance(byte[] addr) {
    return repository.getBalance(addr);
  }

  @Override
  public long addBalance(byte[] addr, long value) {
    return repository.addBalance(addr, value);
  }

  @Override
  public Repository newRepositoryChild() {
    return repository.newRepositoryChild();
  }

  @Override
  public void setParent(Repository repository) {
    this.repository.setParent(repository);
  }

  @Override
  public void commit() {
    repository.commit();
  }

  @Override
  public void putAccount(Key key, Value value) {
    repository.putAccount(key, value);
  }

  @Override
  public void putCode(Key key, Value value) {
    repository.putCode(key, value);
  }


  @Override
  public void putContract(Key key, Value value) {
    repository.putContract(key, value);
  }


  public void putStorage(Key key, Storage cache) {
    repository.putStorage(key, cache);
  }


  @Override
  public Storage getStorage(byte[] address) {
    return repository.getStorage(address);
  }

  @Override
  public void putAccountValue(byte[] address, AccountCapsule accountCapsule) {
    this.repository.putAccountValue(address, accountCapsule);
  }

  @Override
  public void putAssetIssue(Key key, Value value) {
    repository.putAssetIssue(key, value);
  }

  @Override
  public void putAssetIssueValue(byte[] tokenId, AssetIssueCapsule assetIssueCapsule) {
    repository.putAssetIssueValue(tokenId, assetIssueCapsule);
  }

  @Override
  public void putDelegation(Key key, Value value) {
    repository.putDelegation(key, value);
  }

  @Override
  public long addTokenBalance(byte[] address, byte[] tokenId, long value) {
    return repository.addTokenBalance(address, tokenId, value);
  }

  @Override
  public long getTokenBalance(byte[] address, byte[] tokenId) {
    return repository.getTokenBalance(address, tokenId);
  }

  @Override
  public long getAccountLeftEnergyFromFreeze(AccountCapsule accountCapsule) {
    return repository.getAccountLeftEnergyFromFreeze(accountCapsule);
  }

  @Override
  public long calculateGlobalEnergyLimit(AccountCapsule accountCapsule) {
    return repository.calculateGlobalEnergyLimit(accountCapsule);
  }

  @Override
  public byte[] getBlackHoleAddress() {
    return repository.getBlackHoleAddress();
  }

  @Override
  public BlockCapsule getBlockByNum(long num) {
    return repository.getBlockByNum(num);
  }

  @Override
  public AccountCapsule createNormalAccount(byte[] address) {
    return repository.createNormalAccount(address);
  }

  @Override
  public void saveTokenIdNum(long num) {
    this.updateDynamic(DynamicPropertiesStore.getTOKEN_ID_NUM(),
            new BytesCapsule(ByteArray.fromLong(num)));
  }

  @Override
  public long getTokenIdNum() {
    return Optional.ofNullable(this.getDynamic(DynamicPropertiesStore.getTOKEN_ID_NUM()))
            .map(BytesCapsule::getData)
            .map(ByteArray::toLong)
            .orElseThrow(
                    () -> new IllegalArgumentException("error in contract not found TOKEN_ID_NUM"));
  }

  @Override
  public DelegationStore getDelegationStore() {
    return repository.getDelegationStore();
  }

  @Override
  public WitnessStore getWitnessStore() {
    return repository.getWitnessStore();
  }

  @Override
  public VotesCapsule getVotesCapsule(byte[] address) {
    return repository.getVotesCapsule(address);
  }

  @Override
  public long getBeginCycle(byte[] address) {
    return repository.getBeginCycle(address);
  }

  @Override
  public long getEndCycle(byte[] address) {
    return repository.getEndCycle(address);
  }

  @Override
  public AccountCapsule getAccountVote(long cycle, byte[] address) {
    return repository.getAccountVote(cycle, address);
  }

  @Override
  public BytesCapsule getDelegationCache(Key key) {
    return repository.getDelegationCache(key);
  }

  @Override
  public void updateDynamic(byte[] word, BytesCapsule bytesCapsule) {
    repository.updateDynamic(word, bytesCapsule);
  }

  @Override
  public void updateVotesCapsule(byte[] word, VotesCapsule votesCapsule) {
    repository.updateVotesCapsule(word, votesCapsule);
  }

  @Override
  public void updateBeginCycle(byte[] word, long cycle) {
    repository.updateBeginCycle(word, cycle);
  }

  @Override
  public void updateEndCycle(byte[] word, long cycle) {
    repository.updateEndCycle(word, cycle);
  }

  @Override
  public void updateAccountVote(byte[] word, long cycle, AccountCapsule accountCapsule) {
    repository.updateAccountVote(word, cycle, accountCapsule);
  }

  @Override
  public void updateRemark(byte[] word, long cycle) {
    repository.updateRemark(word, cycle);
  }

  @Override
  public void updateDelegation(byte[] word, BytesCapsule bytesCapsule) {
    repository.updateDelegation(word, bytesCapsule);
  }

  @Override
  public void updateLastWithdrawCycle(byte[] address, long cycle) {
    repository.updateLastWithdrawCycle(address, cycle);
  }

  @Override
  public void putDynamic(Key key, Value value) {
    repository.putDynamic(key, value);
  }

  @Override
  public void putVotesCapsule(Key key, Value value) {
    repository.putVotesCapsule(key, value);
  }

  @Override
  public void addTotalNetWeight(long amount) {
    repository.addTotalNetWeight(amount);
  }

  @Override
  public void saveTotalNetWeight(long totalNetWeight) {
    repository.saveTotalNetWeight(totalNetWeight);
  }

  @Override
  public long getTotalNetWeight() {
    return repository.getTotalNetWeight();
  }
}
