package org.tron.core.capsule;

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
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage());
    }
  }

  public DecOracleRewardCapsule(DecOracleReward decReward) {
    this.decReward = decReward;
    this.balance = Dec.newDec(this.decReward.getBalance().getData().toByteArray());
    this.decReward.getAssetMap().forEach((k, v) ->
        this.asset.put(k, Dec.newDec(v.getData().toByteArray())));
  }

  public DecOracleRewardCapsule(OracleRewardCapsule oracleReward) {
    this(oracleReward.getInstance());
  }

  public DecOracleRewardCapsule(OracleReward oracleReward) {
    Map<String, Dec> asset = new HashMap<>();
    oracleReward.getAssetMap().forEach((k, v) -> asset.put(k, Dec.newDec(v)));
    try {
      this.balance = Dec.newDec(oracleReward.getBalance());
      this.asset = removeZeroAsset(asset);
      this.decReward = DecOracleReward.newBuilder()
          .setBalance(BigInt.parseFrom(this.balance.toByteArray()))
          .putAllAsset(this.asset.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
              e -> {
                try {
                  return BigInt.parseFrom(e.getValue().toByteArray());
                } catch (InvalidProtocolBufferException ex) {
                  logger.debug(ex.getMessage(), e);
                }
                return BigInt.getDefaultInstance();
              }))).build();
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
    }
  }

  public DecOracleRewardCapsule(Dec balance, Map<String, Dec> asset) {
    try {
      this.balance = balance;
      this.asset = removeZeroAsset(asset);
      this.decReward = DecOracleReward.newBuilder()
          .setBalance(BigInt.parseFrom(this.balance.toByteArray()))
          .putAllAsset(this.asset.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
              e -> {
                try {
                  return BigInt.parseFrom(e.getValue().toByteArray());
                } catch (InvalidProtocolBufferException ex) {
                  logger.debug(ex.getMessage(), e);
                }
                return BigInt.getDefaultInstance();
              }))).build();
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
    }
  }

  public DecOracleRewardCapsule(BigInteger balance, Map<String, BigInteger> asset) {
    try {
      Map<String, Dec> decAsset = new HashMap<>();
      asset.forEach((k, v) -> decAsset.put(k, Dec.newDec(v)));
      this.balance = Dec.newDec(balance);
      this.asset = removeZeroAsset(decAsset);
      this.decReward = DecOracleReward.newBuilder()
          .setBalance(BigInt.parseFrom(this.balance.toByteArray()))
          .putAllAsset(this.asset.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
              e -> {
                try {
                  return BigInt.parseFrom(e.getValue().toByteArray());
                } catch (InvalidProtocolBufferException ex) {
                  logger.debug(ex.getMessage(), e);
                }
                return BigInt.getDefaultInstance();
              }))).build();
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
    }
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


  public DecOracleRewardCapsule intersect(DecOracleRewardCapsule coinsB) {
    Dec balance = Dec.minDec(this.balance, coinsB.balance);
    Map<String, Dec> asset = new HashMap<>();
    this.asset.forEach((k, v) -> asset.put(k, Dec.maxDec(v,
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
      if (!this.getBalance().isZero()) {
        return false;
      }
    }
    return true;
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
    return "DecOracleRewardCapsule{" +
        "balance:" + balance + ", asset:" + asset +
        '}';
  }
}