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
package org.tron.core.vm;

import org.tron.common.runtime.vm.DataWord;

/**
 * A wrapper for a message call from a contract to another account. This can either be a normal
 * CALL, CALLCODE, DELEGATECALL or POST call.
 */
public class MessageCall {

  /**
   * Type of internal call. Either CALL, CALLCODE or POST
   */
  private final OpCode type;

  /**
   * energy to pay for the call, remaining energy will be refunded to the caller
   */
  private final DataWord energy;
  /**
   * address of account which code to call
   */
  private final DataWord codeAddress;
  /**
   * the value that can be transfer along with the code execution
   */
  private final DataWord endowment;
  /**
   * start of memory to be input data to the call
   */
  private final DataWord inDataOffs;
  /**
   * size of memory to be input data to the call
   */
  private final DataWord inDataSize;
  /**
   * start of memory to be output of the call
   */
  private DataWord outDataOffs;
  /**
   * size of memory to be output data to the call
   */
  private DataWord outDataSize;

  private DataWord tokenId;

  private boolean isTokenTransferMsg;

  public MessageCall(OpCode type, DataWord energy, DataWord codeAddress,
      DataWord endowment, DataWord inDataOffs, DataWord inDataSize, DataWord tokenId,
      boolean isTokenTransferMsg) {
    this.type = type;
    this.energy = energy;
    this.codeAddress = codeAddress;
    this.endowment = endowment;
    this.inDataOffs = inDataOffs;
    this.inDataSize = inDataSize;
    this.tokenId = tokenId;
    this.isTokenTransferMsg = isTokenTransferMsg;
  }

  public MessageCall(OpCode type, DataWord energy, DataWord codeAddress,
      DataWord endowment, DataWord inDataOffs, DataWord inDataSize,
      DataWord outDataOffs, DataWord outDataSize, DataWord tokenId, boolean isTokenTransferMsg) {
    this(type, energy, codeAddress, endowment, inDataOffs, inDataSize, tokenId, isTokenTransferMsg);
    this.outDataOffs = outDataOffs;
    this.outDataSize = outDataSize;
  }

  public OpCode getType() {
    return type;
  }

  public DataWord getEnergy() {
    return energy;
  }

  public DataWord getCodeAddress() {
    return codeAddress;
  }

  public DataWord getEndowment() {
    return endowment;
  }

  public DataWord getInDataOffs() {
    return inDataOffs;
  }

  public DataWord getInDataSize() {
    return inDataSize;
  }

  public DataWord getOutDataOffs() {
    return outDataOffs;
  }

  public DataWord getOutDataSize() {
    return outDataSize;
  }

  public DataWord getTokenId() {
    return tokenId;
  }

  public boolean isTokenTransferMsg() {
    return isTokenTransferMsg;
  }
}
