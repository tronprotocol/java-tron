package org.tron.common.runtime.vm.cache;

import org.tron.common.runtime.vm.DataWord;
import org.tron.common.runtime.vm.program.Storage;
import org.tron.common.storage.Deposit;
import org.tron.core.capsule.*;
import org.tron.core.db.Manager;
import org.tron.protos.Protocol;

public class WriteCachedDepositImpl implements Deposit {


//    WriteCache<>

    @Override
    public Manager getDbManager() {
        return null;
    }

    @Override
    public AccountCapsule createAccount(byte[] address, Protocol.AccountType type) {
        return null;
    }

    @Override
    public AccountCapsule createAccount(byte[] address, String accountName, Protocol.AccountType type) {
        return null;
    }

    @Override
    public AccountCapsule getAccount(byte[] address) {
        return null;
    }

    @Override
    public WitnessCapsule getWitness(byte[] address) {
        return null;
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

    }

    @Override
    public void createContract(byte[] address, ContractCapsule contractCapsule) {

    }

    @Override
    public ContractCapsule getContract(byte[] address) {
        return null;
    }

    @Override
    public void saveCode(byte[] codeHash, byte[] code) {

    }

    @Override
    public byte[] getCode(byte[] codeHash) {
        return new byte[0];
    }

    @Override
    public void putStorageValue(byte[] address, DataWord key, DataWord value) {

    }

    @Override
    public DataWord getStorageValue(byte[] address, DataWord key) {
        return null;
    }

    @Override
    public Storage getStorage(byte[] address) {
        return null;
    }

    @Override
    public long getBalance(byte[] address) {
        return 0;
    }

    @Override
    public long addBalance(byte[] address, long value) {
        return 0;
    }

    @Override
    public Deposit newDepositChild() {
        return null;
    }

    @Override
    public void setParent(Deposit deposit) {

    }

    @Override
    public void flush() {

    }

    @Override
    public void commit() {

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
        return null;
    }

    @Override
    public BlockCapsule getBlock(byte[] blockHash) {
        return null;
    }
}
