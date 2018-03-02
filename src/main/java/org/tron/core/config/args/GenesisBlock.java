package org.tron.core.config.args;

import java.util.List;

public class GenesisBlock {

  private List<SeedNodeAddress> assets;
  private String timeStamp;
  private String parentHash;
  private String hash;
  private String number;

  public List<SeedNodeAddress> getAssets() {
    return assets;
  }

  public void setAssets(List<SeedNodeAddress> assets) {
    this.assets = assets;
  }

  public String getTimeStamp() {
    return timeStamp;
  }

  public void setTimeStamp(String timeStamp) {
    this.timeStamp = timeStamp;
  }

  public String getParentHash() {
    return parentHash;
  }

  public void setParentHash(String parentHash) {
    this.parentHash = parentHash;
  }

  public String getHash() {
    return hash;
  }

  public void setHash(String hash) {
    this.hash = hash;
  }

  public String getNumber() {
    return number;
  }

  public void setNumber(String number) {
    this.number = number;
  }
}
