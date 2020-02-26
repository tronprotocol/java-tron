package org.tron.core.metrics.net;

public class DisconnectionDetailInfo {
  private String reason;
  private int count;

  public String getReason() {
    return this.reason;
  }

  public DisconnectionDetailInfo setReason(String reason) {
    this.reason = reason;
    return this;
  }

  public int getCount() {
    return this.count;
  }

  public DisconnectionDetailInfo setCount(int count) {
    this.count = count;
    return this;
  }
}
