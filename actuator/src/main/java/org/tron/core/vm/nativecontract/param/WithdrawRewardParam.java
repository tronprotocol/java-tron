package org.tron.core.vm.nativecontract.param;

public class WithdrawRewardParam {

  private byte[] targetAddress;

  public byte[] getTargetAddress() {
    return targetAddress;
  }

  public void setTargetAddress(byte[] targetAddress) {
    this.targetAddress = targetAddress;
  }
}
