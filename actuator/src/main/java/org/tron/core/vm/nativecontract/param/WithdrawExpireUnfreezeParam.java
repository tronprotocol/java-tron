package org.tron.core.vm.nativecontract.param;

public class WithdrawExpireUnfreezeParam {

  // Account address which want to withdraw its reward
  private byte[] ownerAddress;

  public byte[] getOwnerAddress() {
    return ownerAddress;
  }

  public void setOwnerAddress(byte[] ownerAddress) {
    this.ownerAddress = ownerAddress;
  }
}
