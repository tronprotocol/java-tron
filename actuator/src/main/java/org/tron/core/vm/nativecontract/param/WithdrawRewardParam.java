package org.tron.core.vm.nativecontract.param;

public class WithdrawRewardParam {

  private byte[] targetAddress;

  private long nowInMs;

  public byte[] getTargetAddress() {
    return targetAddress;
  }

  public void setTargetAddress(byte[] targetAddress) {
    this.targetAddress = targetAddress;
  }

  public long getNowInMs() {
    return nowInMs;
  }

  public void setNowInMs(long nowInMs) {
    this.nowInMs = nowInMs;
  }
}
