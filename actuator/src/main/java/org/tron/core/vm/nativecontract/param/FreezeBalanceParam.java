package org.tron.core.vm.nativecontract.param;

import org.tron.protos.contract.Common;

public class FreezeBalanceParam {

  private byte[] ownerAddress;

  private byte[] receiverAddress;

  private long frozenBalance;

  private long frozenDuration;

  private Common.ResourceCode resourceType;

  private boolean isDelegating;

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

  public long getFrozenBalance() {
    return frozenBalance;
  }

  public void setFrozenBalance(long frozenBalance) {
    this.frozenBalance = frozenBalance;
  }

  public long getFrozenDuration() {
    return frozenDuration;
  }

  public void setFrozenDuration(long frozenDuration) {
    this.frozenDuration = frozenDuration;
  }

  public Common.ResourceCode getResourceType() {
    return resourceType;
  }

  public void setResourceType(Common.ResourceCode resourceType) {
    this.resourceType = resourceType;
  }

  public boolean isDelegating() {
    return isDelegating;
  }

  public void setDelegating(boolean delegating) {
    isDelegating = delegating;
  }
}
