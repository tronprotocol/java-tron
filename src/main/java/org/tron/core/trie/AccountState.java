package org.tron.core.trie;

import java.util.HashMap;
import java.util.Map;

public class AccountState {

  private byte[] address;
  private byte[] name;
  private byte[] id;
  private long balance;
  private Map<String, Long> assetMap = new HashMap<>();

  public byte[] getAddress() {
    return address;
  }

  public AccountState setAddress(byte[] address) {
    this.address = address;
    return this;
  }

  public byte[] getName() {
    return name;
  }

  public AccountState setName(byte[] name) {
    this.name = name;
    return this;
  }

  public byte[] getId() {
    return id;
  }

  public AccountState setId(byte[] id) {
    this.id = id;
    return this;
  }

  public long getBalance() {
    return balance;
  }

  public AccountState setBalance(long balance) {
    this.balance = balance;
    return this;
  }

  public Map<String, Long> getAssetMap() {
    return assetMap;
  }

  public AccountState setAssetMap(Map<String, Long> assetMap) {
    this.assetMap = assetMap;
    return this;
  }


}
