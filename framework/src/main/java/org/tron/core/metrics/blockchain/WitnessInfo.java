package org.tron.core.metrics.blockchain;

public class WitnessInfo {

  private String address;
  private int version;

  public WitnessInfo(String address, int version) {
    this.address = address;
    this.version = version;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }
}
