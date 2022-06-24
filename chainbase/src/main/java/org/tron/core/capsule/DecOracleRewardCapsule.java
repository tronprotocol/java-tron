package org.tron.core.capsule;

import com.google.common.base.Objects;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.entity.Dec;
import org.tron.common.utils.Pair;
import org.tron.protos.Protocol.BigInt;
import org.tron.protos.Protocol.DecOracleReward;
import org.tron.protos.Protocol.OracleReward;

@Slf4j(topic = "capsule")
public class DecOracleRewardCapsule implements ProtoCapsule<DecOracleReward> {


  private DecOracleReward decReward;
  @Getter
  private Dec balance = Dec.zeroDec();
  @Getter
  private Map<String, Dec> asset = new HashMap<>();


  public DecOracleRewardCapsule() {
    this.decReward = DecOracleReward.getDefaultInstance();
  }

  public DecOracleRewardCapsule(byte[] data) {
    try {
      this.decReward = DecOracleReward.parseFrom(data);
      this.balance = Dec.newDec(this.decReward.getBalance().getData().toByteArray());
      this.decReward.getAssetMap().forEach((k, v) ->
          this.asset.put(k, Dec.newDec(v.getData().toByteArray())));
      valid();
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage());
    }
  }

  public DecOracleRewardCapsule(DecOracleReward decReward) {
    this(Dec.newDec(decReward.getBalance().getData().toByteArray()),
        decReward.getAssetMap().entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey,
                e -> Dec.newDec(e.getValue().getData().toByteArray()))));
  }

  public DecOracleRewardCapsule(OracleRewardCapsule oracleReward) {
    this(oracleReward.getInstance());
  }

  public DecOracleRewardCapsule(OracleReward oracleReward) {
    this(Dec.newDec(oracleReward.getBalance()), oracleReward.getAssetMap().entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> Dec.newDec(e.getValue()))));
  }

  public DecOracleRewardCapsule(BigInteger balance, Map<String, BigInteger> asset) {
    this(Dec.newDec(balance), asset.entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> Dec.newDec(e.getValue()))));
  }

  public DecOracleRewardCapsule(Dec balance, Map<String, Dec> asset) {
    this.balance = Dec.newDec(balance.toByteArray());
    this.asset = removeZeroAsset(asset);
    this.decReward = DecOracleReward.newBuilder()
        .setBalance(buildFromDec(this.balance))
        .putAllAsset(this.asset.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
            e -> buildFromDec(e.getValue())))).build();
    valid();
  }


  public DecOracleRewardCapsule add(DecOracleRewardCapsule plus) {
    return operate(plus, Dec::add);
  }

  public DecOracleRewardCapsule sub(DecOracleRewardCapsule sub) {
    return operate(sub, Dec::sub);
  }

  public DecOracleRewardCapsule mul(Dec d) {
    return operate(d, Dec::mul);
  }

  public DecOracleRewardCapsule mul(long d) {
    return mul(Dec.newDec(d));
  }

  public DecOracleRewardCapsule mulTruncate(long d) {
    return mulTruncate(Dec.newDec(d));
  }

  public DecOracleRewardCapsule mulTruncate(Dec d) {
    return operate(d, Dec::mulTruncate);
  }

  public DecOracleRewardCapsule quo(Dec d) {
    return operate(d, Dec::quo);
  }

  public DecOracleRewardCapsule quo(long d) {
    return quo(Dec.newDec(d));
  }

  public DecOracleRewardCapsule quoTruncate(Dec d) {
    return operate(d, Dec::quoTruncate);
  }

  public DecOracleRewardCapsule quoTruncate(long d) {
    return quoTruncate(Dec.newDec(d));
  }

  // Intersect will return a new set of coins which contains the minimum DecCoin
  // for common denoms found in both `coins` and `coinsB`. For denoms not common
  // to both `coins` and `coinsB` the minimum is considered to be 0, thus they
  // are not added to the final set.In other words, trim any denom amount from
  // coin which exceeds that of coinB, such that (coin.Intersect(coinB)).IsLTE(coinB).
  public DecOracleRewardCapsule intersect(DecOracleRewardCapsule coinsB) {
    Dec balance = Dec.minDec(this.balance, coinsB.balance);
    Map<String, Dec> asset = new HashMap<>();
    this.asset.forEach((k, v) -> asset.put(k, Dec.minDec(v,
        coinsB.asset.getOrDefault(k, Dec.zeroDec()))));
    return new DecOracleRewardCapsule(balance, asset);
  }

  private DecOracleRewardCapsule operate(DecOracleRewardCapsule other,
                                         BiFunction<Dec, Dec, Dec> op) {
    Dec balance = op.apply(this.getBalance(), other.getBalance());
    Map<String, Dec> asset = new HashMap<>(this.getAsset());
    other.getAsset().forEach((k, v) -> asset.merge(k, v, op));
    return new DecOracleRewardCapsule(balance, asset);

  }

  private DecOracleRewardCapsule operate(Dec number,
                                         BiFunction<Dec, Dec, Dec> op) {
    Dec balance = op.apply(this.getBalance(), number);
    Map<String, Dec> asset = new HashMap<>();
    this.getAsset().forEach((k, v) -> asset.put(k, op.apply(v, number)));
    return new DecOracleRewardCapsule(balance, asset);

  }

  public OracleRewardCapsule truncateDecimal() {
    OracleReward.Builder oracleReward = OracleReward.newBuilder();
    long balance = this.balance.truncateLong();
    oracleReward.setBalance(balance);
    this.getAsset().forEach((k, v) ->
        oracleReward.putAsset(k, v.truncateLong()));

    return new OracleRewardCapsule(oracleReward.build());
  }

  public Pair<OracleRewardCapsule, DecOracleRewardCapsule> truncateDecimalAndRemainder() {
    long balance = this.balance.truncateLong();
    Dec remBalance = this.balance.sub(Dec.newDec(balance));
    Map<String, Long> asset = new HashMap<>();
    Map<String, Dec> remAsset = new HashMap<>();

    this.getAsset().forEach((k, v) -> {
          long amount = v.truncateLong();
          asset.put(k, amount);
          remAsset.put(k, v.sub(Dec.newDec(amount)));
        }
    );

    return new Pair<>(new OracleRewardCapsule(balance, asset),
        new DecOracleRewardCapsule(remBalance, remAsset));
  }

  // IsZero returns whether all coins are zero
  public boolean isZero() {
    if (!this.getBalance().isZero()) {
      return false;
    }
    for (Dec v : this.getAsset().values()) {
      if (!v.isZero()) {
        return false;
      }
    }
    return true;
  }

  /**
   * check whether all coins are greater than or equal zero
   */
  public void valid() {
    if (this.getBalance().isNegative()) {
      throw new IllegalArgumentException("balance:" + this.getBalance());
    }
    for (Map.Entry<String, Dec> v : this.getAsset().entrySet()) {
      if (v.getValue().isNegative()) {
        throw new IllegalArgumentException(v.getKey() + ":" + v.getValue());
      }
    }
  }

  private Map<String, Dec> removeZeroAsset(Map<String, Dec> asset) {
    return asset.entrySet().stream().filter(e -> !e.getValue().isZero())
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  @Override
  public byte[] getData() {
    return this.decReward.toByteArray();
  }

  @Override
  public DecOracleReward getInstance() {
    return this.decReward;
  }

  @Override
  public String toString() {
    return "DecOracleRewardCapsule{" + "balance:" + balance + ", asset:" + asset + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DecOracleRewardCapsule that = (DecOracleRewardCapsule) o;
    return Objects.equal(balance, that.balance)
        && Objects.equal(asset, that.asset);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(balance, asset);
  }

  private BigInt buildFromDec(Dec dec) {
    return BigInt.newBuilder().setData(ByteString.copyFrom(dec.toByteArray())).build();
  }
}