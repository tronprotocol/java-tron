package org.tron.core.vm.nativecontract.param;

import org.tron.protos.contract.Common;

public class ExpireFreezeV2BalanceParam {

  private byte[] ownerAddress;

  private Common.ResourceCode resourceType;

  public byte[] getOwnerAddress() {
    return ownerAddress;
  }

  public void setOwnerAddress(byte[] ownerAddress) {
    this.ownerAddress = ownerAddress;
  }

  public Common.ResourceCode getResourceType() {
    return resourceType;
  }

  public void setResourceType(Common.ResourceCode resourceType) {
    this.resourceType = resourceType;
  }
}
