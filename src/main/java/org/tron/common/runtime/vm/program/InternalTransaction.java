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

import static org.apache.commons.lang3.ArrayUtils.isEmpty;
import static org.apache.commons.lang3.ArrayUtils.nullToEmpty;
import static org.tron.common.utils.ByteUtil.EMPTY_BYTE_ARRAY;

import com.google.common.primitives.Longs;
import java.util.Arrays;
import org.tron.common.crypto.Hash;
import org.tron.core.Wallet;
import org.tron.core.capsule.ContractCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.protos.Contract.CreateSmartContract;
import org.tron.protos.Contract.TriggerSmartContract;
import org.tron.protos.Protocol.Transaction;

public class InternalTransaction {

  private Transaction transaction;
  private byte[] hash;
  private byte[] parentHash;
  /* the amount of trx to transfer (calculated as sun) */
  private long value;

  /* the address of the destination account (for message)
   * In creation transaction the receive address is - 0 */
  private byte[] receiveAddress;

  /* An unlimited size byte array specifying
   * input [data] of the message call or
   * Initialization code for a new contract */
  private byte[] data;
  private long nonce;
  private byte[] transferToAddress;

  /*  Message sender address */
  protected byte[] sendAddress;
  private int deep;
  private int index;
  private boolean rejected;
  private String note;
  private byte[] protoEncoded;


  public enum TrxType {
    TRX_PRECOMPILED_TYPE,
    TRX_CONTRACT_CREATION_TYPE,
    TRX_CONTRACT_CALL_TYPE,
    TRX_UNKNOWN_TYPE,
  }

  public enum ExecutorType {
    ET_PRE_TYPE,
    ET_NORMAL_TYPE,
    ET_CONSTANT_TYPE,
    ET_UNKNOWN_TYPE,
  }


  /**
   * Construct an un-encoded InternalTransaction
   */
  public InternalTransaction(Transaction trx,  InternalTransaction.TrxType trxType) {
    this.transaction = trx;
    TransactionCapsule trxCap = new TransactionCapsule(trx);
    this.protoEncoded = trxCap.getData();
    this.nonce = 0;
    // outside transaction should not have deep, so use -1 to mark it is root.
    // It will not count in vm trace. But this deep will be shown in program result.
    this.deep = -1;
    if (trxType == TrxType.TRX_CONTRACT_CREATION_TYPE) {
      CreateSmartContract contract = ContractCapsule.getSmartContractFromTransaction(trx);
      this.sendAddress = contract.getOwnerAddress().toByteArray();
      this.receiveAddress = EMPTY_BYTE_ARRAY;
      this.transferToAddress = Wallet.generateContractAddress(trx);
      this.note = "create";
      this.value = contract.getNewContract().getCallValue();
      this.data = contract.getNewContract().getBytecode().toByteArray();
    } else if(trxType == TrxType.TRX_CONTRACT_CALL_TYPE) {
      TriggerSmartContract contract = ContractCapsule.getTriggerContractFromTransaction(trx);
      this.sendAddress = contract.getOwnerAddress().toByteArray();
      this.receiveAddress = contract.getContractAddress().toByteArray();
      this.transferToAddress = this.receiveAddress.clone();
      this.note = "call";
      this.value = contract.getCallValue();
      this.data = contract.getData().toByteArray();
    } else {
      // TODO: Should consider unknown type?
    }
    this.hash = trxCap.getTransactionId().getBytes();
  }

  /**
   * Construct an encoded InternalTransaction
   */

  public InternalTransaction(byte[] parentHash, int deep, int index,
      byte[] sendAddress, byte[] transferToAddress,  long value, byte[] data, String note, long nonce) {
    this.parentHash = parentHash.clone();
    this.deep = deep;
    this.index = index;
    this.note = note;
    this.sendAddress = nullToEmpty(sendAddress);
    this.transferToAddress = nullToEmpty(transferToAddress);
    if("create".equalsIgnoreCase(note)){
      this.receiveAddress = EMPTY_BYTE_ARRAY;
    } else {
      this.receiveAddress = nullToEmpty(transferToAddress);
    }
    this.value = value;
    this.data = nullToEmpty(data);
    this.nonce = nonce;
    this.hash = getHash();
  }

  public Transaction getTransaction() {
    return transaction;
  }

  public void setTransaction(Transaction transaction) {
    this.transaction = transaction;
  }

  public byte[] getTransferToAddress() {
    return transferToAddress.clone();
  }

  public void reject() {
    this.rejected = true;
  }


  public int getDeep() {
    return deep;
  }

  public int getIndex() {
    return index;
  }

  public boolean isRejected() {
    return rejected;
  }

  public String getNote() {
    if (note == null) {
      return "";
    }
    return note;
  }

  public byte[] getSender() {
    if (sendAddress == null) {
      return EMPTY_BYTE_ARRAY;
    }
    return sendAddress;
  }

  public byte[] getParentHash() {
    if (parentHash == null) {
      return EMPTY_BYTE_ARRAY;
    }
    return parentHash.clone();
  }

  public long getValue() {
    return value;
  }

  public byte[] getData() {
    if (data == null) {
      return EMPTY_BYTE_ARRAY;
    }
    return data.clone();
  }

  protected void setValue(long value) {
    this.value = value;
  }

  public byte[] getReceiveAddress() {
    if (receiveAddress == null) {
      return EMPTY_BYTE_ARRAY;
    }
    return receiveAddress.clone();
  }

  public final byte[] getHash() {
    if (!isEmpty(hash)) {
      return Arrays.copyOf(hash, hash.length);
    }

    byte[] plainMsg = this.getEncoded();
    byte[] nonceByte;
    nonceByte = Longs.toByteArray(nonce);
    byte[] forHash = new byte[plainMsg.length + nonceByte.length];
    System.arraycopy(plainMsg, 0, forHash, 0, plainMsg.length);
    System.arraycopy(nonceByte, 0, forHash, plainMsg.length, nonceByte.length);
    this.hash = Hash.sha3(forHash);
    return Arrays.copyOf(hash, hash.length);
  }

  public long getNonce() {
    return nonce;
  }

  public byte[] getEncoded() {
    if (protoEncoded != null) {
      return protoEncoded.clone();
    }
    byte[] parentHashArray = parentHash.clone();

    if (parentHashArray == null){
      parentHashArray = EMPTY_BYTE_ARRAY;
    }
    byte[] valueByte = Longs.toByteArray(this.value);
    byte[] raw = new byte[parentHashArray.length + this.receiveAddress.length + this.data.length + valueByte.length];
    System.arraycopy(parentHashArray, 0, raw, 0, parentHashArray.length);
    System.arraycopy(this.receiveAddress, 0, raw, parentHashArray.length, this.receiveAddress.length);
    System.arraycopy(this.data, 0, raw, parentHashArray.length + this.receiveAddress.length, this.data.length);
    System.arraycopy(valueByte, 0, raw, parentHashArray.length + this.receiveAddress.length + this.data.length,
        valueByte.length);
    this.protoEncoded = raw;
    return protoEncoded.clone();
  }

}
