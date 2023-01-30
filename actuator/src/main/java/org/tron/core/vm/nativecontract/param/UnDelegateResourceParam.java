package org.tron.core.vm.nativecontract.param;

import org.tron.protos.contract.Common;

public class UnDelegateResourceParam {

  private byte[] ownerAddress;

  private byte[] receiverAddress;

  private long unDelegateBalance;

  private Common.ResourceCode resourceType;

  public byte[] getOwnerAddress() {
    return ownerAddress;
  }

  public void setOwnerAddress(byte[] ownerAddress) {
    this.ownerAddress = ownerAddress;
  }

  public byte[] getReceiverAddress() {
    return receiverAddress;
  }

  public void setReceiverAddress(byte[] receiverAddress) {
    this.receiverAddress = receiverAddress;
  }

  public long getUnDelegateBalance() {
    return unDelegateBalance;
  }

  public void setUnDelegateBalance(long unDelegateBalance) {
    this.unDelegateBalance = unDelegateBalance;
  }

  public Common.ResourceCode getResourceType() {
    return resourceType;
  }

  public void setResourceType(Common.ResourceCode resourceType) {
    this.resourceType = resourceType;
  }
}
