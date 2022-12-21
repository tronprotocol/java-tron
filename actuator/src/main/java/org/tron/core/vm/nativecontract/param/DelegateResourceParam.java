package org.tron.core.vm.nativecontract.param;

import org.tron.protos.contract.Common;

public class DelegateResourceParam {

  private byte[] ownerAddress;

  private byte[] receiverAddress;

  private long delegateBalance;

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

  public long getDelegateBalance() {
    return delegateBalance;
  }

  public void setDelegateBalance(long delegateBalance) {
    this.delegateBalance = delegateBalance;
  }

  public Common.ResourceCode getResourceType() {
    return resourceType;
  }

  public void setResourceType(Common.ResourceCode resourceType) {
    this.resourceType = resourceType;
  }
}
