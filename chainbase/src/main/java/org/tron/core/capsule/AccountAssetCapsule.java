package org.tron.core.capsule;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.AccountAsset;
import org.tron.protos.Protocol.AccountAsset.Frozen;

import java.util.List;
import java.util.Map;

@Slf4j(topic = "capsule")
public class AccountAssetCapsule implements ProtoCapsule<AccountAsset> {

  private AccountAsset accountAsset;

  /**
   * get accountAssetIssue from bytes data.
   */
  public AccountAssetCapsule(byte[] data) {
    try {
      this.accountAsset = AccountAsset.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage());
    }
  }

  public AccountAssetCapsule(ByteString address) {
    this.accountAsset = AccountAsset.newBuilder()
            .setAddress(address)
            .build();
  }

  public AccountAssetCapsule(AccountAsset accountAsset) {
    this.accountAsset = accountAsset;
  }

  @Override
  public byte[] getData() {
    return this.accountAsset.toByteArray();
  }

  @Override
  public AccountAsset getInstance() {
    return this.accountAsset;
  }

  public ByteString getAddress() {
    return this.accountAsset.getAddress();
  }

  public byte[] createDbKey() {
    return getAddress().toByteArray();
  }

  public void setInstance(AccountAsset accountAsset) {
    this.accountAsset = accountAsset;
  }

  public Map<String, Long> getAssetMap() {
    return this.accountAsset.getAssetMap();
  }

  public ByteString getAssetIssuedID() {
    return getInstance().getAssetIssuedID();
  }

  public ByteString getAssetIssuedName() {
    return getInstance().getAssetIssuedName();
  }

  public Map<String, Long> getAllFreeAssetNetUsage() {
    return this.accountAsset.getFreeAssetNetUsageMap();
  }

  public Map<String, Long> getLatestAssetOperationTimeMap() {
    return this.accountAsset.getLatestAssetOperationTimeMap();
  }

  public Map<String, Long> getAssetMapV2() {
    return this.accountAsset.getAssetV2Map();
  }

  public Map<String, Long> getAllFreeAssetNetUsageV2() {
    return this.accountAsset.getFreeAssetNetUsageV2Map();
  }

  public Map<String, Long> getLatestAssetOperationTimeMapV2() {
    return this.accountAsset.getLatestAssetOperationTimeV2Map();
  }

  public List<Frozen> getFrozenSupplyList() {
    return getInstance().getFrozenSupplyList();
  }

  @Override
  public String toString() {
    return this.accountAsset.toString();
  }

}
