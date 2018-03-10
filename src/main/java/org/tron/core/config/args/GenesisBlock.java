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
  private List<Witness> witnesses;
  private String timeStamp;
  private String parentHash;
  private String hash;
  private String number;

  /**
   * return default genesis block.
   */
  public static GenesisBlock getDefault() {
    final GenesisBlock genesisBlock = new GenesisBlock();
    genesisBlock.setNumber(DEFAULT_NUMBER);
    genesisBlock.setTimeStamp(DEFAULT_TIMESTAMP);
    genesisBlock.setHash(DEFAULT_HASH);
    genesisBlock.setParentHash(DEFAULT_PARENT_HASH);
    return genesisBlock;
  }

  public List<Account> getAssets() {
    return this.assets;
  }

  public void setAssets(final List<Account> assets) {
    this.assets = assets;
  }

  public String getTimeStamp() {
    return this.timeStamp;
  }

  public void setTimeStamp(final String timeStamp) {
    this.timeStamp = timeStamp;
  }

  public String getParentHash() {
    return this.parentHash;
  }

  public void setParentHash(final String parentHash) {
    this.parentHash = parentHash;
  }

  public String getHash() {
    return this.hash;
  }

  public void setHash(final String hash) {
    this.hash = hash;
  }

  public String getNumber() {
    return this.number;
  }

  public void setNumber(final String number) {
    this.number = number;
  }

  public List<Witness> getWitnesses() {
    return this.witnesses;
  }

  public void setWitnesses(final List<Witness> witnesses) {
    this.witnesses = witnesses;
  }
}
