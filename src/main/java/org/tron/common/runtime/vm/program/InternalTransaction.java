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
import static org.tron.common.utils.ByteUtil.toHexString;

import com.google.common.primitives.Longs;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import org.tron.common.crypto.ECKey;
import org.tron.common.crypto.ECKey.ECDSASignature;
import org.tron.common.crypto.Hash;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.utils.ByteUtil;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.protos.Protocol.Transaction;

public class InternalTransaction {

  private Transaction transaction;
  private byte[] hash;
  private byte[] parentHash;
  /* the amount of trx to transfer (calculated as sun) */
  private long value;

  /* the address of the destination account
   * In creation transaction the receive address is - 0 */
  private byte[] receiveAddress;

  /* An unlimited size byte array specifying
   * input [data] of the message call or
   * Initialization code for a new contract */
  private byte[] data;

  protected byte[] sendAddress;
  private int deep;
  private int index;
  private boolean rejected = false;
  private String note;
  private boolean parsed;
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
  public InternalTransaction(Transaction trx) {
    this.transaction = trx;
    this.protoEncoded = new TransactionCapsule(trx).getData();
    this.parsed = false;
  }

  /**
   * Construct an encoded InternalTransaction
   */

  public InternalTransaction(byte[] parentHash, int deep, int index,
      byte[] sendAddress, byte[] receiveAddress, long value, byte[] data, String note) {

    this.parentHash = parentHash;
    this.deep = deep;
    this.index = index;
    this.note = note;
    this.sendAddress = nullToEmpty(sendAddress);
    this.receiveAddress = nullToEmpty(receiveAddress);
    this.value = value;
    this.data = nullToEmpty(data);
    this.parsed = true;
  }

  public Transaction getTransaction() {
    return transaction;
  }

  public void setTransaction(Transaction transaction) {
    this.transaction = transaction;
  }

  public void reject() {
    this.rejected = true;
  }


  public int getDeep() {
    protoParse();
    return deep;
  }

  public int getIndex() {
    protoParse();
    return index;
  }

  public boolean isRejected() {
    protoParse();
    return rejected;
  }

  public String getNote() {
    protoParse();
    return note;
  }

  public byte[] getSender() {
    protoParse();
    return sendAddress;
  }

  public byte[] getParentHash() {
    protoParse();
    return parentHash;
  }

  public long getValue() {
    protoParse();
    return value;
  }

  public byte[] getData() {
    protoParse();
    return data.clone();
  }

  protected void setValue(long value) {
    this.value = value;
    parsed = true;
  }

  public byte[] getReceiveAddress() {
    protoParse();
    return receiveAddress.clone();
  }

  private void protoParse() {
    if (parsed) {
      return;
    }
    try {
      this.hash = Hash.sha3(protoEncoded);
      this.parsed = true;
    } catch (Exception e) {
      throw new RuntimeException("Error on parsing proto", e);
    }
  }

  public byte[] getHash() {
    if (!isEmpty(hash)) {
      return Arrays.copyOf(hash, hash.length);
    }

    protoParse();
    byte[] plainMsg = this.getEncoded();
    return Hash.sha3(plainMsg);
  }


  public byte[] getEncoded() {

    if (protoEncoded != null) {
      if (null == this.hash) {
        this.hash = Hash.sha3(protoEncoded);
      }
      return protoEncoded.clone();
    }

    byte[] valueByte = Longs.toByteArray(this.value);
    byte[] raw = new byte[this.receiveAddress.length + this.data.length + valueByte.length];
    System.arraycopy(this.receiveAddress, 0, raw, 0, this.receiveAddress.length);
    System.arraycopy(this.data, 0, raw, this.receiveAddress.length, this.data.length);
    System.arraycopy(valueByte, 0, raw, this.data.length, valueByte.length);
    this.protoEncoded = raw;
    this.hash = Hash.sha3(protoEncoded);

    return protoEncoded.clone();
  }

  @Override
  public String toString() {
    return "TransactionData [" +
        "  parentHash=" + toHexString(getParentHash()) +
        ", hash=" + toHexString(getHash()) +
        ", sendAddress=" + toHexString(getSender()) +
        ", receiveAddress=" + toHexString(getReceiveAddress()) +
        ", value=" + getValue() +
        ", data=" + toHexString(getData()) +
        ", note=" + getNote() +
        ", deep=" + getDeep() +
        ", index=" + getIndex() +
        ", rejected=" + isRejected() +
        "]";
  }
}
