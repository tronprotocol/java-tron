package org.tron.core.metrics.blockchain;

public class Witness {
  private String address;
  private int version;

  public Witness(String address, int version) {
    this.address = address;
    this.version = version;
  }

  public String getAddress() {
    return this.address;
  }

  public Witness setAddress(String address) {
    this.address = address;
    return this;
  }

  public int getVersion() {
    return this.version;
  }

  public Witness setVersion(int version) {
    this.version = version;
    return this;
  }
}
