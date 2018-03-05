package org.tron.core.config.args;

import java.io.Serializable;
import java.util.List;

public class GenesisBlock implements Serializable {

  private static final long serialVersionUID = 3559533002594201715L;

  private static final String DEFAULT_NUMBER = "0";
  private static final String DEFAULT_TIMESTAMP = "0";
  private static final String DEFAULT_HASH = "0";
  private static final String DEFAULT_PARENT_HASH = "0";

  private List<Account> assets;
  private String timeStamp;
  private String parentHash;
  private String hash;
  private String number;

  /**
   * return default genesis block.
   */
  public static GenesisBlock getDefault() {
    GenesisBlock genesisBlock = new GenesisBlock();
    genesisBlock.setNumber(DEFAULT_NUMBER);
    genesisBlock.setTimeStamp(DEFAULT_TIMESTAMP);
    genesisBlock.setHash(DEFAULT_HASH);
    genesisBlock.setParentHash(DEFAULT_PARENT_HASH);
    return genesisBlock;
  }

  public List<Account> getAssets() {
    return assets;
  }

  public void setAssets(List<Account> assets) {
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
