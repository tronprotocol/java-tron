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
package org.tron.core.vm.program.invoke;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.runtime.vm.DataWord;
import org.tron.core.vm.repository.Repository;

@Slf4j
public class ProgramInvokeImpl implements ProgramInvoke {

  /*****************/
  /* NOTE: In the protocol there is no restriction on the maximum message data,
   * However msgData here is a byte[] and this can't hold more than 2^32-1
   */
  private static final BigInteger MAX_MSG_DATA = BigInteger.valueOf(Integer.MAX_VALUE);
  /* TRANSACTION  env*/
  private final DataWord address;
  private final DataWord origin;
  private final DataWord caller;
  private final DataWord balance;
  private final DataWord callValue;
  private final DataWord tokenValue;
  private final DataWord tokenId;
  /* BLOCK  env **/
  private final DataWord prevHash;
  private final DataWord coinbase;
  private final DataWord timestamp;
  private final DataWord number;
  private byte[] msgData;
  private long vmStartInUs;
  private long vmShouldEndInUs;
  private long energyLimit;
  private Repository deposit;
  private boolean byTransaction = true;
  private boolean byTestingSuite = false;
  private int callDeep = 0;
  private boolean isStaticCall = false;
  private boolean isConstantCall = false;

  public ProgramInvokeImpl(DataWord address, DataWord origin, DataWord caller, DataWord balance,
      DataWord callValue, DataWord tokenValue, DataWord tokenId, byte[] msgData,
      DataWord lastHash, DataWord coinbase, DataWord timestamp, DataWord number,
      DataWord difficulty,
      Repository deposit, int callDeep, boolean isStaticCall, boolean byTestingSuite,
      long vmStartInUs, long vmShouldEndInUs, long energyLimit) {
    this.address = address;
    this.origin = origin;
    this.caller = caller;
    this.balance = balance;
    this.callValue = callValue;
    this.tokenValue = tokenValue;
    this.tokenId = tokenId;
    if (Objects.nonNull(msgData)) {
      this.msgData = Arrays.copyOf(msgData, msgData.length);
    }

    // last Block env
    this.prevHash = lastHash;
    this.coinbase = coinbase;
    this.timestamp = timestamp;
    this.number = number;
    this.callDeep = callDeep;

    this.deposit = deposit;
    this.byTransaction = false;
    this.isStaticCall = isStaticCall;
    this.byTestingSuite = byTestingSuite;
    this.vmStartInUs = vmStartInUs;
    this.vmShouldEndInUs = vmShouldEndInUs;
    this.energyLimit = energyLimit;

  }

  public ProgramInvokeImpl(byte[] address, byte[] origin, byte[] caller, long balance,
      long callValue, long tokenValue, long tokenId, byte[] msgData,
      byte[] lastHash, byte[] coinbase, long timestamp, long number, Repository deposit,
      long vmStartInUs, long vmShouldEndInUs, boolean byTestingSuite, long energyLimit) {
    this(address, origin, caller, balance, callValue, tokenValue, tokenId, msgData, lastHash,
        coinbase,
        timestamp, number, deposit, vmStartInUs, vmShouldEndInUs, energyLimit);
    this.byTestingSuite = byTestingSuite;
  }

  public ProgramInvokeImpl(byte[] address, byte[] origin, byte[] caller, long balance,
      long callValue, long tokenValue, long tokenId, byte[] msgData, byte[] lastHash,
      byte[] coinbase, long timestamp,
      long number, Repository deposit, long vmStartInUs, long vmShouldEndInUs, long energyLimit) {

    // Transaction env
    this.address = new DataWord(address);
    this.origin = new DataWord(origin);
    this.caller = new DataWord(caller);
    this.balance = new DataWord(balance);
    this.callValue = new DataWord(callValue);
    this.tokenValue = new DataWord(tokenValue);
    this.tokenId = new DataWord(tokenId);
    this.msgData = Arrays.copyOf(msgData, msgData.length);

    // last Block env
    this.prevHash = new DataWord(lastHash);
    this.coinbase = new DataWord(coinbase);
    this.timestamp = new DataWord(timestamp);
    this.number = new DataWord(number);
    this.deposit = deposit;

    // calc should end time
    this.vmStartInUs = vmStartInUs;
    this.vmShouldEndInUs = vmShouldEndInUs;
    this.energyLimit = energyLimit;
  }

  /*           ADDRESS op         */
  public DataWord getContractAddress() {
    return address;
  }

  /*           BALANCE op         */
  public DataWord getBalance() {
    return balance;
  }

  /*           ORIGIN op         */
  public DataWord getOriginAddress() {
    return origin;
  }

  /*           CALLER op         */
  public DataWord getCallerAddress() {
    return caller;
  }

  /*          CALLVALUE op    */
  public DataWord getCallValue() {
    return callValue;
  }

  /*          TOKENVALUE op     */
  public DataWord getTokenValue() {
    return tokenValue;
  }

  /*****************/
  /***  msg data ***/

  /*          TOKENID op     */
  public DataWord getTokenId() {
    return tokenId;
  }

  /*     CALLDATALOAD  op   */
  public DataWord getDataValue(DataWord indexData) {

    BigInteger tempIndex = indexData.value();
    int index = tempIndex.intValue(); // possible overflow is caught below
    int size = 32; // maximum datavalue size

    if (msgData == null || index >= msgData.length
        || tempIndex.compareTo(MAX_MSG_DATA) > 0) {
      return new DataWord();
    }
    if (index + size > msgData.length) {
      size = msgData.length - index;
    }

    byte[] data = new byte[32];
    System.arraycopy(msgData, index, data, 0, size);
    return new DataWord(data);
  }

  /*  CALLDATASIZE */
  public DataWord getDataSize() {

    if (msgData == null || msgData.length == 0) {
      return DataWord.ZERO;
    }
    int size = msgData.length;
    return new DataWord(size);
  }

  /*  CALLDATACOPY */
  public byte[] getDataCopy(DataWord offsetData, DataWord lengthData) {

    int offset = offsetData.intValueSafe();
    int length = lengthData.intValueSafe();

    byte[] data = new byte[length];

    if (msgData == null) {
      return data;
    }
    if (offset > msgData.length) {
      return data;
    }
    if (offset + length > msgData.length) {
      length = msgData.length - offset;
    }

    System.arraycopy(msgData, offset, data, 0, length);

    return data;
  }


  /*     PREVHASH op    */
  public DataWord getPrevHash() {
    return prevHash;
  }

  /*     COINBASE op    */
  public DataWord getCoinbase() {
    return coinbase;
  }

  /*     TIMESTAMP op    */
  public DataWord getTimestamp() {
    return timestamp;
  }

  /*     NUMBER op    */
  public DataWord getNumber() {
    return number;
  }

  /*     DIFFICULTY op    */
  public DataWord getDifficulty() {
    return new DataWord(0);
  }

  public long getVmShouldEndInUs() {
    return vmShouldEndInUs;
  }

  public Repository getDeposit() {
    return deposit;
  }

  @Override
  public boolean isStaticCall() {
    return isStaticCall;
  }

  @Override
  public boolean isConstantCall() {
    return isConstantCall;
  }

  @Override
  public boolean byTestingSuite() {
    return byTestingSuite;
  }

  @Override
  public long getVmStartInUs() {
    return vmStartInUs;
  }


  @Override
  public int getCallDeep() {
    return this.callDeep;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ProgramInvokeImpl that = (ProgramInvokeImpl) o;

    if (byTestingSuite != that.byTestingSuite) {
      return false;
    }
    if (byTransaction != that.byTransaction) {
      return false;
    }
    if (address != null ? !address.equals(that.address) : that.address != null) {
      return false;
    }
    if (balance != null ? !balance.equals(that.balance) : that.balance != null) {
      return false;
    }
    if (callValue != null ? !callValue.equals(that.callValue) : that.callValue != null) {
      return false;
    }
    if (caller != null ? !caller.equals(that.caller) : that.caller != null) {
      return false;
    }
    if (coinbase != null ? !coinbase.equals(that.coinbase) : that.coinbase != null) {
      return false;
    }
    if (!Arrays.equals(msgData, that.msgData)) {
      return false;
    }
    if (number != null ? !number.equals(that.number) : that.number != null) {
      return false;
    }
    if (origin != null ? !origin.equals(that.origin) : that.origin != null) {
      return false;
    }
    if (prevHash != null ? !prevHash.equals(that.prevHash) : that.prevHash != null) {
      return false;
    }
    if (deposit != null ? !deposit.equals(that.deposit) : that.deposit != null) {
      return false;
    }
    return timestamp != null ? timestamp.equals(that.timestamp) : that.timestamp == null;
  }

  @Override
  public int hashCode() {
    return new Integer(Boolean.valueOf(byTestingSuite).hashCode()
        + Boolean.valueOf(byTransaction).hashCode()
        + address.hashCode()
        + balance.hashCode()
        + callValue.hashCode()
        + caller.hashCode()
        + coinbase.hashCode()
        + Arrays.hashCode(msgData)
        + number.hashCode()
        + origin.hashCode()
        + prevHash.hashCode()
        + deposit.hashCode()
        + timestamp.hashCode()
    ).hashCode();
  }

  @Override
  public String toString() {
    return "ProgramInvokeImpl{" +
        "address=" + address +
        ", origin=" + origin +
        ", caller=" + caller +
        ", balance=" + balance +
        ", callValue=" + callValue +
        ", msgData=" + Arrays.toString(msgData) +
        ", prevHash=" + prevHash +
        ", coinbase=" + coinbase +
        ", timestamp=" + timestamp +
        ", number=" + number +
        ", byTransaction=" + byTransaction +
        ", byTestingSuite=" + byTestingSuite +
        ", callDeep=" + callDeep +
        '}';
  }

  public long getEnergyLimit() {
    return energyLimit;
  }

  @Override
  public void setConstantCall() {
    isConstantCall = true;
  }


}
