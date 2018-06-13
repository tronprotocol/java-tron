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

import com.google.protobuf.ByteString;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.runtime.vm.program.invoke.ProgramInvoke;
import org.tron.common.runtime.vm.program.listener.ProgramListener;
import org.tron.common.runtime.vm.program.listener.ProgramListenerAware;
import org.tron.core.capsule.*;
import org.tron.core.db.Manager;
import org.tron.core.exception.BalanceInsufficientException;
import org.tron.protos.Protocol;

public class Storage implements ProgramListenerAware {
    private Manager dbManager;
    private final DataWord address;
    private ProgramListener programListener;

    public Storage(ProgramInvoke programInvoke) {
        this.address = programInvoke.getOwnerAddress();
        this.dbManager = programInvoke.getDbManager();
    }

    @Override
    public void setProgramListener(ProgramListener listener) {
        this.programListener = listener;
    }

    public AccountCapsule createAccount(byte[] addr, Protocol.AccountType type) {
        AccountCapsule accountCapsule = new AccountCapsule(ByteString.copyFrom(addr), type);
        dbManager.getAccountStore().put(addr, accountCapsule);
        return accountCapsule;
    }

    public AccountCapsule getAccount(byte[] addr) {
        return dbManager.getAccountStore().get(addr);
    }

    public void createContract(byte[] codeHash, ContractCapsule contractCapsule) {
        dbManager.getContractStore().put(codeHash, contractCapsule);
    }

    public ContractCapsule getContract(byte[] codeHash) {
        return dbManager.getContractStore().get(codeHash);
    }

    public void saveCode(byte[] addr, byte[] code) {
        dbManager.getCodeStore().put(addr, new CodeCapsule(code));
    }

    public byte[] getCode(byte[] addr) {
        return dbManager.getCodeStore().get(addr).getData();
    }

    public byte[] getCodeHash(byte[] addr) {
        return dbManager.getAccountStore().get(addr).getCodeHash();
    }

    public void addStorageValue(byte[] addr, DataWord key, DataWord value) {
        if (canListenTrace(addr)) programListener.onStoragePut(key, value);
        StorageCapsule storageCapsule = dbManager.getStorageStore().get(addr);
        storageCapsule.put(key, value);
        dbManager.getStorageStore().put(addr, storageCapsule);
    }

    private boolean canListenTrace(byte[] address) {
        return (programListener != null) && this.address.equals(new DataWord(address));
    }

    public DataWord getStorageValue(byte[] addr, DataWord key) {
        StorageCapsule storageCapsule = dbManager.getStorageStore().get(addr);
        return storageCapsule.get(key);
    }

    public long getBalance(byte[] addr) {
        return dbManager.getAccountStore().get(addr).getBalance();
    }

    public void addBalance(byte[] addr, long value) throws BalanceInsufficientException {
        dbManager.adjustBalance(addr, value);
    }

    public StorageCapsule getStorage(byte[] address) {
        return dbManager.getStorageStore().get(address);
    }
}
