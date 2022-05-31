package org.tron.core.capsule;

import com.google.common.base.Objects;
import com.google.common.math.LongMath;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.tron.protos.Protocol.OracleReward;

@Slf4j(topic = "capsule")
public class OracleRewardCapsule implements ProtoCapsule<OracleReward> {

  private OracleReward reward;


  public OracleRewardCapsule() {
    this.reward = OracleReward.getDefaultInstance();
  }

  /**
   * get reward from bytes data.
   */
  public OracleRewardCapsule(byte[] data) {
    try {
      this.reward = OracleReward.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage());
    }
  }

  /**
   * initial reward capsule.
   */
  public OracleRewardCapsule(OracleReward reward) {
    this(reward.getBalance(), reward.getAssetMap());
  }

  public OracleRewardCapsule(long balance, Map<String, Long> asset) {
    this.reward = OracleReward.newBuilder()
        .setBalance(balance)
        .putAllAsset(removeZeroAsset(asset))
        .build();
  }

  public OracleRewardCapsule add(OracleRewardCapsule plus) {
    long balance = LongMath.checkedAdd(this.getBalance(), plus.getBalance());
    Map<String, Long> asset = new HashMap<>(this.getAsset());
    plus.getAsset().forEach((k, v) -> asset.merge(k, v, LongMath::checkedAdd));
    return new OracleRewardCapsule(balance, asset);
  }

  public OracleRewardCapsule sub(OracleRewardCapsule sub) {
    long balance = LongMath.checkedSubtract(this.getBalance(), sub.getBalance());
    Map<String, Long> asset = new HashMap<>(this.getAsset());
    sub.getAsset().forEach((k, v) -> asset.merge(k, v, LongMath::checkedSubtract));
    return new OracleRewardCapsule(balance, asset);
  }

  @Override
  public byte[] getData() {
    return this.reward.toByteArray();
  }

  @Override
  public OracleReward getInstance() {
    return this.reward;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OracleRewardCapsule that = (OracleRewardCapsule) o;
    return Objects.equal(reward, that.reward);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(reward);
  }

  public long getBalance() {
    return this.reward.getBalance();
  }

  public Map<String, Long> getAsset() {
    return this.reward.getAssetMap();
  }


  private Map<String, Long> removeZeroAsset(Map<String, Long> asset) {
    return asset.entrySet().stream().filter(e -> e.getValue() != 0)
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  // IsZero returns whether all coins are zero
  public boolean isZero() {
    if (this.getBalance() != 0) {
      return false;
    }
    for (long v : this.getAsset().values()) {
      if (v != 0) {
        return false;
      }
    }
    return true;
  }

}
