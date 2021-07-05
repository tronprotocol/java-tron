package org.tron.core.vm.nativecontract.param;

public class WithdrawRewardParam {

  private byte[] ownerAddress;

  private long nowInMs;

  public byte[] getOwnerAddress() {
    return ownerAddress;
  }

  public void setOwnerAddress(byte[] ownerAddress) {
    this.ownerAddress = ownerAddress;
  }

  public long getNowInMs() {
    return nowInMs;
  }

  public void setNowInMs(long nowInMs) {
    this.nowInMs = nowInMs;
  }
}
