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
  private final int opCode;

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

  public MessageCall(int opCode, DataWord energy, DataWord codeAddress,
      DataWord endowment, DataWord inDataOffs, DataWord inDataSize, DataWord tokenId,
      boolean isTokenTransferMsg) {
    this.opCode = opCode;
    this.energy = energy;
    this.codeAddress = codeAddress;
    this.endowment = endowment;
    this.inDataOffs = inDataOffs;
    this.inDataSize = inDataSize;
    this.tokenId = tokenId;
    this.isTokenTransferMsg = isTokenTransferMsg;
  }

  public MessageCall(int opCode, DataWord energy, DataWord codeAddress,
      DataWord endowment, DataWord inDataOffs, DataWord inDataSize,
      DataWord outDataOffs, DataWord outDataSize, DataWord tokenId, boolean isTokenTransferMsg) {
    this(opCode, energy, codeAddress, endowment, inDataOffs, inDataSize, tokenId, isTokenTransferMsg);
    this.outDataOffs = outDataOffs;
    this.outDataSize = outDataSize;
  }

  public int getOpCode() {
    return opCode;
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
