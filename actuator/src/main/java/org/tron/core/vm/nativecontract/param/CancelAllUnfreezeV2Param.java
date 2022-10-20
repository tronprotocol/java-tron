package org.tron.core.vm.nativecontract.param;

public class CancelAllUnfreezeV2Param {

  private byte[] ownerAddress;

  public byte[] getOwnerAddress() {
    return ownerAddress;
  }

  public void setOwnerAddress(byte[] ownerAddress) {
    this.ownerAddress = ownerAddress;
  }
}
