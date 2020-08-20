package org.tron.core.vm.nativecontract.param;

public class UpdateAssetParam {

  private byte[] ownerAddress;

  private byte[] newUrl;

  private byte[] newDesc;

  public byte[] getOwnerAddress() {
    return ownerAddress;
  }

  public void setOwnerAddress(byte[] ownerAddress) {
    this.ownerAddress = ownerAddress;
  }

  public byte[] getNewUrl() {
    return newUrl;
  }

  public void setNewUrl(byte[] newUrl) {
    this.newUrl = newUrl;
  }

  public byte[] getNewDesc() {
    return newDesc;
  }

  public void setNewDesc(byte[] newDesc) {
    this.newDesc = newDesc;
  }
}
