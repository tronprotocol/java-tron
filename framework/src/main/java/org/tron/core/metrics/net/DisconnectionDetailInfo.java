package org.tron.core.metrics.net;

public class DisconnectionDetailInfo {

  private String reason;
  private int count;

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public int getCount() {
    return count;
  }

  public void setCount(int count) {
    this.count = count;
  }
}
