package org.tron.core.capsule;

import com.google.common.collect.Maps;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.tron.protos.Protocol.AccountAssetIssue;
import org.tron.protos.Protocol.AccountAssetIssue.Frozen;

import java.util.List;
import java.util.Map;

@Slf4j(topic = "capsule")
public class AccountAssetIssueCapsule implements ProtoCapsule<AccountAssetIssue> {

  private AccountAssetIssue accountAssetIssue;

  /**
   * get accountAssetIssue from bytes data.
   */
  public AccountAssetIssueCapsule(byte[] data) {
    try {
      this.accountAssetIssue = AccountAssetIssue.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage());
    }
  }

  public AccountAssetIssueCapsule(AccountAssetIssue accountAssetIssue) {
    this.accountAssetIssue = accountAssetIssue;
  }

  @Override
  public byte[] getData() {
    return this.accountAssetIssue.toByteArray();
  }

  @Override
  public AccountAssetIssue getInstance() {
    return this.accountAssetIssue;
  }

  public ByteString getAddress() {
    return this.accountAssetIssue.getAddress();
  }

  public byte[] createDbKey() {
    return getAddress().toByteArray();
  }

  public void setInstance(AccountAssetIssue accountAssetIssue) {
    this.accountAssetIssue = accountAssetIssue;
  }

  public Map<String, Long> getAssetMap() {
    return this.accountAssetIssue.getAssetMap();
  }

  public ByteString getAssetIssuedID() {
    return getInstance().getAssetIssuedID();
  }

  public ByteString getAssetIssuedName() {
    return getInstance().getAssetIssuedName();
  }

  public Map<String, Long> getAllFreeAssetNetUsage() {
    return this.accountAssetIssue.getFreeAssetNetUsageMap();
  }

  public Map<String, Long> getLatestAssetOperationTimeMap() {
    return this.accountAssetIssue.getLatestAssetOperationTimeMap();
  }

  public Map<String, Long> getAssetMapV2() {
    return this.accountAssetIssue.getAssetV2Map();
  }

  public Map<String, Long> getAllFreeAssetNetUsageV2() {
    return this.accountAssetIssue.getFreeAssetNetUsageV2Map();
  }

  public Map<String, Long> getLatestAssetOperationTimeMapV2() {
    return this.accountAssetIssue.getLatestAssetOperationTimeV2Map();
  }

  public List<Frozen> getFrozenSupplyList() {
    return getInstance().getFrozenSupplyList();
  }

  @Override
  public String toString() {
    return this.accountAssetIssue.toString();
  }

}
