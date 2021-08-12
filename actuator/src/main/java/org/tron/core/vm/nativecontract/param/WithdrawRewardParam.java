package org.tron.core.vm.nativecontract.param;

/**
 * Param used by WithdrawRewardProcessor
 */
public class WithdrawRewardParam {

  // Account address which want to withdraw its reward
  private byte[] ownerAddress;

  // Latest block timestamp
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
