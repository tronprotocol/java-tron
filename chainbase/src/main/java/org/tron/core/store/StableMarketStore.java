package org.tron.core.store;

import com.google.common.primitives.Longs;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.common.entity.Dec;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.ByteUtil;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.capsule.StableCoinCapsule;
import org.tron.core.capsule.StableCoinInfoCapsule;
import org.tron.core.db.TronStoreWithRevoking;
import org.tron.protos.contract.AssetIssueContractOuterClass;
import org.tron.protos.contract.StableMarketContractOuterClass;
import org.tron.protos.contract.StableMarketContractOuterClass.StableCoinInfo;
import org.tron.protos.contract.StableMarketContractOuterClass.StableCoinContract;

import java.util.List;


@Slf4j(topic = "DB")
@Component
public class StableMarketStore extends TronStoreWithRevoking<BytesCapsule> {

  private static final int TOBIN_FEE_DECIMAL = 3;

  private static final byte[] TOBIN_FEE_PREFIX = "tobin_".getBytes();
  private static final byte[] STABLE_COIN_PREFIX = "stable_".getBytes();
  private static final byte[] BASE_POOL = "basepool".getBytes();
  private static final byte[] POOL_RECOVERY_PERIOD = "pool_recovery_period".getBytes();
  private static final byte[] POOL_DELTA = "delta".getBytes();
  private static final byte[] MIN_SPREAD = "min_spread".getBytes();
  private static final byte[] TRX_EXCHANGE_AMOUNT = "trx_exchange_amount".getBytes();
  private static final byte[] EXCHANGE_RATE = "exchange_rate".getBytes();

  @Autowired
  AssetIssueV2Store assetIssueV2Store;

  @Autowired
  private StableMarketStore(@Value("stable-market") String dbName) {
    super(dbName);
  }

  // todo
  public StableMarketContractOuterClass.StableCoinInfoList getStableCoinList() {
    return null;
  }

  public StableCoinInfo getStableCoinById(byte[] tokenId) {
    BytesCapsule data = getUnchecked(buildKey(STABLE_COIN_PREFIX, tokenId));
    if (data != null && !ByteUtil.isNullOrZeroArray(data.getData())) {
      AssetIssueCapsule assetIssueContract = assetIssueV2Store.get(tokenId);
      // todo: optimize
      Dec tobinFee = getTobinFee(tokenId);
      StableCoinInfo stableCoinInfo =
           StableCoinInfo.newBuilder()
               .setAssetIssue(assetIssueContract.getInstance())
               .setTobinFee(tobinFee.roundLong())  // todo check
               .build();
      return stableCoinInfo;
    } else {
      return null;
    }
  }

  public void setStableCoin(byte[] tokenId, long tobinFee) {
    Dec fee = Dec.newDecWithPrec(tobinFee, TOBIN_FEE_DECIMAL);
    StableCoinContract stableCoin = StableCoinContract.newBuilder()
            .setAssetIssueId(ByteArray.toStr(tokenId))
            .setTobinFee(fee.toString())
            .build();
    this.put(buildKey(STABLE_COIN_PREFIX, tokenId), new BytesCapsule(stableCoin.toByteArray()));
  }

  public Dec getTobinFee(byte[] tokenId) {
    BytesCapsule data = getUnchecked(buildKey(STABLE_COIN_PREFIX, tokenId));
    try {
      if (data != null && !ByteUtil.isNullOrZeroArray(data.getData())) {
        StableCoinContract stableCoin = StableCoinContract.parseFrom(data.getData());
        return Dec.newDec(stableCoin.getTobinFee());
      } else {
        return null;
      }
    } catch (InvalidProtocolBufferException e) {
      return null;
    }
  }

  public void setTobinFee(byte[] tokenId, long tobinFee) {
    Dec fee = Dec.newDecWithPrec(tobinFee, TOBIN_FEE_DECIMAL);
    BytesCapsule data = getUnchecked(buildKey(STABLE_COIN_PREFIX, tokenId));
    try {
      if (data != null && !ByteUtil.isNullOrZeroArray(data.getData())) {
        StableCoinContract stableCoin = StableCoinContract.parseFrom(data.getData());
        stableCoin = stableCoin.toBuilder().setTobinFee(fee.toString()).build();
        this.put(buildKey(STABLE_COIN_PREFIX, tokenId), new BytesCapsule(stableCoin.toByteArray()));
      } else {
        // todo: optimize
        throw new RuntimeException("set tobin fee failed, data is null");
      }
    } catch (InvalidProtocolBufferException e) {
      throw new RuntimeException("set tobin fee failed, " + e.getMessage());
    }
  }

  public Dec getBasePool() {
    BytesCapsule data = getUnchecked(BASE_POOL);
    if (data != null && !ByteUtil.isNullOrZeroArray(data.getData())) {
      return Dec.newDec(data.getData());
    } else {
      return null;
    }
  }

  public void setBasePool(Dec basePool) {
    this.put(BASE_POOL, new BytesCapsule(basePool.toByteArray()));
  }

  public Long getPoolRecoveryPeriod() {
    BytesCapsule data = getUnchecked(POOL_RECOVERY_PERIOD);
    if (data != null && !ByteUtil.isNullOrZeroArray(data.getData())) {
      return ByteArray.toLong(data.getData());
    } else {
      return null;
    }
  }

  public void setPoolRecoveryPeriod(long poolRecoveryPeriod) {
    this.put(POOL_RECOVERY_PERIOD, new BytesCapsule(ByteArray.fromLong(poolRecoveryPeriod)));
  }

  public Dec getPoolDelta() {
    BytesCapsule data = getUnchecked(POOL_DELTA);
    if (data != null && !ByteUtil.isNullOrZeroArray(data.getData())) {
      return Dec.newDec(data.getData());
    } else {
      return null;
    }
  }

  public void setPoolDelta(Dec delta) {
    this.put(POOL_DELTA, new BytesCapsule(delta.toByteArray()));
  }

  public Dec getMinSpread() {
    BytesCapsule data = getUnchecked(MIN_SPREAD);
    if (data != null && !ByteUtil.isNullOrZeroArray(data.getData())) {
      return Dec.newDec(data.getData());
    } else {
      return null;
    }
  }

  public void setMinSpread(Dec spread) {
    this.put(MIN_SPREAD, new BytesCapsule(spread.toByteArray()));
  }

  public Long getTrxExchangeAmount() {
    BytesCapsule data = getUnchecked(TRX_EXCHANGE_AMOUNT);
    if (data != null && !ByteUtil.isNullOrZeroArray(data.getData())) {
      return ByteArray.toLong(data.getData());
    } else {
      return null;
    }
  }

  public void setTrxExchangeAmount(long amount) {
    this.put(TRX_EXCHANGE_AMOUNT, new BytesCapsule(ByteArray.fromLong(amount)));
  }

  public Dec getOracleExchangeRate(byte[] tokenId) {
    BytesCapsule data = getUnchecked(buildKey(EXCHANGE_RATE, tokenId));
    if (data != null && !ByteUtil.isNullOrZeroArray(data.getData())) {
      return Dec.newDec(data.getData());
    } else {
      return null;
    }
  }

  public void setOracleExchangeRate(byte[] tokenId, Dec rate) {
    this.put(buildKey(EXCHANGE_RATE, tokenId), new BytesCapsule(rate.toByteArray()));
  }

  @Override
  public void close() {
    super.close();
  }

  private byte[] buildKey(byte[] a, byte[] b) {
    byte[] c = new byte[a.length + b.length];
    System.arraycopy(a, 0, c, 0, a.length);
    System.arraycopy(b, 0, c, a.length, b.length);
    return c;
  }
}
