package org.tron.core.vm.nativecontract.param;

import org.tron.protos.contract.Common;

public class UnfreezeBalanceV2Param {

  private byte[] ownerAddress;

  private long unfreezeBalance;

  private Common.ResourceCode resourceType;

  public byte[] getOwnerAddress() {
    return ownerAddress;
  }

  public void setOwnerAddress(byte[] ownerAddress) {
    this.ownerAddress = ownerAddress;
  }

  public long getUnfreezeBalance() {
    return unfreezeBalance;
  }

  public void setUnfreezeBalance(long unfreezeBalance) {
    this.unfreezeBalance = unfreezeBalance;
  }

  public Common.ResourceCode getResourceType() {
    return resourceType;
  }

  public void setResourceType(Common.ResourceCode resourceType) {
    this.resourceType = resourceType;
  }
}
