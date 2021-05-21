package org.tron.core.metrics.blockchain;

public class DupWitnessInfo {

  private String address;
  private long blockNum;
  private int count;

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public long getBlockNum() {
    return blockNum;
  }

  public void setBlockNum(long blockNum) {
    this.blockNum = blockNum;
  }

  public int getCount() {
    return count;
  }

  public void setCount(int count) {
    this.count = count;
  }
}
