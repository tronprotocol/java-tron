package org.tron.core.config.args;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public class GenesisBlock implements Serializable {

  private static final long serialVersionUID = 3559533002594201715L;

  public static final String DEFAULT_NUMBER = "0";
  public static final String DEFAULT_TIMESTAMP = "0";
  public static final String DEFAULT_PARENT_HASH = "0";

  private List<Account> assets;
  private List<Witness> witnesses;
  private String timestamp;
  private String parentHash;
  private String number;

  public GenesisBlock() {
    this.number = "0";
  }

  /**
   * return default genesis block.
   */
  public static GenesisBlock getDefault() {
    final GenesisBlock genesisBlock = new GenesisBlock();
    List<Account> assets = Collections.emptyList();
    genesisBlock.setAssets(assets);
    List<Witness> witnesses = Collections.emptyList();
    genesisBlock.setWitnesses(witnesses);
    genesisBlock.setNumber(DEFAULT_NUMBER);
    genesisBlock.setTimestamp(DEFAULT_TIMESTAMP);
    genesisBlock.setParentHash(DEFAULT_PARENT_HASH);
    return genesisBlock;
  }

  public List<Account> getAssets() {
    return this.assets;
  }

  /**
   * Empty assets.
   */
  public void setAssets(final List<Account> assets) {
    this.assets = assets;

    if (assets == null) {
      this.assets = Collections.EMPTY_LIST;
    }
  }

  public String getTimestamp() {
    return this.timestamp;
  }

  /**
   * Timestamp >= 0.
   */
  public void setTimestamp(final String timestamp) {
    this.timestamp = timestamp;

    if (this.timestamp == null) {
      this.timestamp = DEFAULT_TIMESTAMP;
    }

    try {
      long l = Long.parseLong(this.timestamp);
      if (l < 0) {
        throw new IllegalArgumentException("Timestamp(" + timestamp + ") must be Long type.");
      }
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Timestamp(" + timestamp + ") must be Long type.");
    }
  }

  public String getParentHash() {
    return this.parentHash;
  }

  /**
   * Set parent hash.
   */
  public void setParentHash(final String parentHash) {
    this.parentHash = parentHash;

    if (this.parentHash == null) {
      this.parentHash = DEFAULT_PARENT_HASH;
    }
  }

  public String getNumber() {
    return this.number;
  }

  public void setNumber(final String number) {
    this.number = "0";
  }

  public List<Witness> getWitnesses() {
    return this.witnesses;
  }

  /**
   * Empty witnesses.
   */
  public void setWitnesses(final List<Witness> witnesses) {
    this.witnesses = witnesses;

    if (witnesses == null) {
      this.witnesses = Collections.EMPTY_LIST;
    }
  }
}
