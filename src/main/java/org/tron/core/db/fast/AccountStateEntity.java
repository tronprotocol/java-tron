package org.tron.core.db.fast;

import com.alibaba.fastjson.JSON;
import java.util.Map;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Account;

public class AccountStateEntity {

  private String address;
  private long balance;
  private Map<String, Long> asset;
  private Map<String, Long> assetV2;
  private long allowance;

  public AccountStateEntity() {
  }

  public AccountStateEntity(Account account) {
    address = Wallet.encode58Check(account.getAddress().toByteArray());
    balance = account.getBalance();
    asset = account.getAssetMap();
    assetV2 = account.getAssetV2Map();
    allowance = account.getAllowance();
  }

  public String getAddress() {
    return address;
  }

  public AccountStateEntity setAddress(String address) {
    this.address = address;
    return this;
  }

  public long getBalance() {
    return balance;
  }

  public AccountStateEntity setBalance(long balance) {
    this.balance = balance;
    return this;
  }

  public Map<String, Long> getAsset() {
    return asset;
  }

  public AccountStateEntity setAsset(Map<String, Long> asset) {
    this.asset = asset;
    return this;
  }

  public Map<String, Long> getAssetV2() {
    return assetV2;
  }

  public AccountStateEntity setAssetV2(Map<String, Long> assetV2) {
    this.assetV2 = assetV2;
    return this;
  }

  public long getAllowance() {
    return allowance;
  }

  public AccountStateEntity setAllowance(long allowance) {
    this.allowance = allowance;
    return this;
  }

  public byte[] toByteArrays() {
    return JSON.toJSONBytes(this);
  }

  public static AccountStateEntity parse(byte[] data) {
    return JSON.parseObject(data, AccountStateEntity.class);
  }

  @Override
  public String toString() {
    return JSON.toJSONString(this);
  }
}
