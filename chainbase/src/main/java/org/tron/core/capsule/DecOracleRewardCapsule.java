package org.tron.core.capsule;

import com.google.protobuf.InvalidProtocolBufferException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.Pair;
import org.tron.protos.Protocol.BigInt;
import org.tron.protos.Protocol.DecOracleReward;
import org.tron.protos.Protocol.OracleReward;

@Slf4j(topic = "capsule")
public class DecOracleRewardCapsule implements ProtoCapsule<DecOracleReward> {
  public static final BigInteger DECIMAL_OF_ORACLE_REWARD = BigInteger.valueOf(10).pow(18);
  public static final BigInteger DECIMAL_OF_BROKERAGE = BigInteger.valueOf(10).pow(2);


  private DecOracleReward decReward;
  @Getter
  private BigInteger balance = BigInteger.ZERO;
  @Getter
  private Map<String, BigInteger> asset = new HashMap<>();


  public DecOracleRewardCapsule() {
    this.decReward = DecOracleReward.getDefaultInstance();
  }

  public DecOracleRewardCapsule(byte[] data) {
    try {
      this.decReward = DecOracleReward.parseFrom(data);
      this.balance = new BigInteger(this.decReward.getBalance().getData().toByteArray());
      this.decReward.getAssetMap().forEach((k, v) ->
          this.asset.put(k, new BigInteger(v.getData().toByteArray())));
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage());
    }
  }

  public DecOracleRewardCapsule(DecOracleReward decReward) {
    this.decReward = decReward;
    this.balance = new BigInteger(this.decReward.getBalance().getData().toByteArray());
    this.decReward.getAssetMap().forEach((k, v) ->
        this.asset.put(k, new BigInteger(v.getData().toByteArray())));
  }

  public DecOracleRewardCapsule(OracleRewardCapsule oracleReward) {
    this(oracleReward.getInstance());
  }

  public DecOracleRewardCapsule(OracleReward oracleReward) {
    BigInteger balance = BigInteger.valueOf(oracleReward.getBalance())
        .multiply(DecOracleRewardCapsule.DECIMAL_OF_ORACLE_REWARD);
    Map<String, BigInteger> asset = new HashMap<>();
    oracleReward.getAssetMap().forEach((k, v) -> asset.put(k,
        BigInteger.valueOf(v).multiply(DecOracleRewardCapsule.DECIMAL_OF_ORACLE_REWARD)));
    try {
      this.balance = balance;
      this.asset = removeZeroAsset(asset);
      this.decReward = DecOracleReward.newBuilder()
          .setBalance(BigInt.parseFrom(balance.toByteArray()))
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
      this.balance = balance;
      this.asset = removeZeroAsset(asset);
      this.decReward = DecOracleReward.newBuilder()
          .setBalance(BigInt.parseFrom(balance.toByteArray()))
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
    return operate(plus, BigInteger::add);
  }

  public DecOracleRewardCapsule subtract(DecOracleRewardCapsule sub) {
    return operate(sub, BigInteger::subtract);
  }

  public DecOracleRewardCapsule multiply(long d) {
    return operate(BigInteger.valueOf(d), BigInteger::multiply);
  }

  public DecOracleRewardCapsule divide(long d) {
    return operate(BigInteger.valueOf(d), BigInteger::divide);
  }

  private DecOracleRewardCapsule operate(DecOracleRewardCapsule other,
                                         BiFunction<BigInteger, BigInteger, BigInteger> op) {
    BigInteger balance = op.apply(this.getBalance(), other.getBalance());
    Map<String, BigInteger> asset = new HashMap<>(this.getAsset());
    other.getAsset().forEach((k, v) -> asset.merge(k, v, op));
    return new DecOracleRewardCapsule(balance, asset);

  }

  private DecOracleRewardCapsule operate(BigInteger number,
                                         BiFunction<BigInteger, BigInteger, BigInteger> op) {
    BigInteger balance = op.apply(this.getBalance(), number);
    Map<String, BigInteger> asset = new HashMap<>();
    this.getAsset().forEach((k, v) -> asset.put(k, op.apply(v, number)));
    return new DecOracleRewardCapsule(balance, asset);

  }

  public Pair<DecOracleRewardCapsule, DecOracleRewardCapsule> divideAndRemainder(long d) {
    BigInteger dec = BigInteger.valueOf(d);
    BigInteger[] truncatedBalance = this.getBalance().divideAndRemainder(dec);
    Map<String, BigInteger> truncatedAsset = new HashMap<>();
    Map<String, BigInteger> remainedAsset = new HashMap<>();
    this.getAsset().forEach((k, v) -> {
      BigInteger[] truncatedAssets = v.divideAndRemainder(dec);
      truncatedAsset.put(k, truncatedAssets[0]);
      remainedAsset.put(k, truncatedAssets[1]);
    });
    DecOracleRewardCapsule truncate = new DecOracleRewardCapsule(
        truncatedBalance[0], truncatedAsset);
    DecOracleRewardCapsule remainder = new DecOracleRewardCapsule(
        truncatedBalance[1], remainedAsset);
    return new Pair<>(truncate, remainder);
  }

  public OracleRewardCapsule truncateDecimal() {
    OracleReward.Builder oracleReward = OracleReward.newBuilder();
    DecOracleRewardCapsule decOracleReward = divide(DECIMAL_OF_ORACLE_REWARD.longValue());
    oracleReward.setBalance(decOracleReward.getBalance().longValueExact());
    decOracleReward.getAsset().forEach((k, v) ->
        oracleReward.putAsset(k, v.longValueExact()));

    return new OracleRewardCapsule(oracleReward.build());
  }

  public Pair<OracleReward, DecOracleRewardCapsule> truncateDecimalAndRemainder() {
    OracleReward.Builder oracleReward = OracleReward.newBuilder();

    Pair<DecOracleRewardCapsule, DecOracleRewardCapsule> pair =
        divideAndRemainder(DECIMAL_OF_ORACLE_REWARD.longValue());

    DecOracleRewardCapsule truncate = pair.getKey();
    oracleReward.setBalance(truncate.getBalance().longValueExact());
    truncate.getAsset().forEach((k, v) -> oracleReward.putAsset(k, v.longValueExact()));
    return new Pair<>(oracleReward.build(), pair.getValue());
  }

  // IsZero returns whether all coins are zero
  public boolean isZero() {
    if (this.getBalance().signum() != 0) {
      return false;
    }
    for (BigInteger v : this.getAsset().values()) {
      if (v.signum() != 0) {
        return false;
      }
    }
    return true;
  }

  private Map<String, BigInteger> removeZeroAsset(Map<String, BigInteger> asset) {
    return asset.entrySet().stream().filter(e -> e.getValue().signum() != 0)
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
}