package org.tron.core.vm.nativecontract.param;

import org.tron.protos.contract.Common;

public class FreezeBalanceV2Param {

  private byte[] ownerAddress;

  private long frozenBalance;

  private Common.ResourceCode resourceType;

  public byte[] getOwnerAddress() {
    return ownerAddress;
  }

  public void setOwnerAddress(byte[] ownerAddress) {
    this.ownerAddress = ownerAddress;
  }

  public long getFrozenBalance() {
    return frozenBalance;
  }

  public void setFrozenBalance(long frozenBalance) {
    this.frozenBalance = frozenBalance;
  }

  public Common.ResourceCode getResourceType() {
    return resourceType;
  }

  public void setResourceType(Common.ResourceCode resourceType) {
    this.resourceType = resourceType;
  }
}
