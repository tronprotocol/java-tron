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
package org.tron.common.runtime;

import static org.apache.commons.lang3.ArrayUtils.isEmpty;
import static org.tron.common.utils.ByteUtil.EMPTY_BYTE_ARRAY;

import com.google.common.primitives.Longs;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import org.apache.commons.lang3.ArrayUtils;
import org.tron.common.crypto.Hash;
import org.tron.common.utils.WalletUtil;
import org.tron.core.capsule.ContractCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.contract.SmartContractOuterClass.CreateSmartContract;
import org.tron.protos.contract.SmartContractOuterClass.TriggerSmartContract;

public class InternalTransaction {

  private Transaction transaction;
  private byte[] hash;
  private byte[] parentHash;
  /* the amount of trx to transfer (calculated as sun) */
  private long value;

  private Map<String, Long> tokenInfo = new HashMap<>();

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
  private byte[] sendAddress;
  @Getter
  private int deep;
  @Getter
  private int index;
  private boolean rejected;
  private String note;
  private byte[] protoEncoded;


  /**
   * Construct a root InternalTransaction
   */
  public InternalTransaction(Transaction trx, InternalTransaction.TrxType trxType)
      throws ContractValidateException {
    this.transaction = trx;
    TransactionCapsule trxCap = new TransactionCapsule(trx);
    this.protoEncoded = trxCap.getData();
    this.nonce = 0;
    // outside transaction should not have deep, so use -1 to mark it is root.
    // It will not count in vm trace. But this deep will be shown in program result.
    this.deep = -1;
    if (trxType == TrxType.TRX_CONTRACT_CREATION_TYPE) {
      CreateSmartContract contract = ContractCapsule.getSmartContractFromTransaction(trx);
      if (contract == null) {
        throw new ContractValidateException("Invalid CreateSmartContract Protocol");
      }
      this.sendAddress = contract.getOwnerAddress().toByteArray();
      this.receiveAddress = EMPTY_BYTE_ARRAY;
      this.transferToAddress = WalletUtil.generateContractAddress(trx);
      this.note = "create";
      this.value = contract.getNewContract().getCallValue();
      this.data = contract.getNewContract().getBytecode().toByteArray();
      this.tokenInfo.put(String.valueOf(contract.getTokenId()), contract.getCallTokenValue());
    } else if (trxType == TrxType.TRX_CONTRACT_CALL_TYPE) {
      TriggerSmartContract contract = ContractCapsule.getTriggerContractFromTransaction(trx);
      if (contract == null) {
        throw new ContractValidateException("Invalid TriggerSmartContract Protocol");
      }
      this.sendAddress = contract.getOwnerAddress().toByteArray();
      this.receiveAddress = contract.getContractAddress().toByteArray();
      this.transferToAddress = this.receiveAddress.clone();
      this.note = "call";
      this.value = contract.getCallValue();
      this.data = contract.getData().toByteArray();
      this.tokenInfo.put(String.valueOf(contract.getTokenId()), contract.getCallTokenValue());
    } else {
      // do nothing, just for running byte code
    }
    this.hash = trxCap.getTransactionId().getBytes();
  }

  /**
   * Construct a child InternalTransaction
   */

  public InternalTransaction(byte[] parentHash, int deep, int index,
      byte[] sendAddress, byte[] transferToAddress, long value, byte[] data, String note,
      long nonce, Map<String, Long> tokenInfo) {
    this.parentHash = parentHash.clone();
    this.deep = deep;
    this.index = index;
    this.note = note;
    this.sendAddress = ArrayUtils.nullToEmpty(sendAddress);
    this.transferToAddress = ArrayUtils.nullToEmpty(transferToAddress);
    if ("create".equalsIgnoreCase(note)) {
      this.receiveAddress = EMPTY_BYTE_ARRAY;
    } else {
      this.receiveAddress = ArrayUtils.nullToEmpty(transferToAddress);
    }
    // in this case, value also can represent a tokenValue when tokenId is not null, otherwise it is a trx callvalue.
    this.value = value;
    this.data = ArrayUtils.nullToEmpty(data);
    this.nonce = nonce;
    this.hash = getHash();
    // in a contract call contract case, only one value should be used. trx or a token. can't be both. We should avoid using
    // tokenValue in this case.
    if (tokenInfo != null) {
      this.tokenInfo.putAll(tokenInfo);
    }
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

  public boolean isRejected() {
    return rejected;
  }

  public String getNote() {
    if (note == null) {
      return "";
    }
    return note;
  }

  public Map<String, Long> getTokenInfo() {
    return tokenInfo;
  }

  public byte[] getSender() {
    if (sendAddress == null) {
      return EMPTY_BYTE_ARRAY;
    }
    return sendAddress.clone();
  }

  public byte[] getReceiveAddress() {
    if (receiveAddress == null) {
      return EMPTY_BYTE_ARRAY;
    }
    return receiveAddress.clone();
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

    if (parentHashArray == null) {
      parentHashArray = EMPTY_BYTE_ARRAY;
    }
    byte[] valueByte = Longs.toByteArray(this.value);
    byte[] raw = new byte[parentHashArray.length + this.receiveAddress.length + this.data.length
        + valueByte.length];
    System.arraycopy(parentHashArray, 0, raw, 0, parentHashArray.length);
    System
        .arraycopy(this.receiveAddress, 0, raw, parentHashArray.length, this.receiveAddress.length);
    System.arraycopy(this.data, 0, raw, parentHashArray.length + this.receiveAddress.length,
        this.data.length);
    System.arraycopy(valueByte, 0, raw,
        parentHashArray.length + this.receiveAddress.length + this.data.length,
        valueByte.length);
    this.protoEncoded = raw;
    return protoEncoded.clone();
  }

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

}
