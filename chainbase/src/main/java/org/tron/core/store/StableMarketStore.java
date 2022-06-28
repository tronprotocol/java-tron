package org.tron.core.store;

import static org.tron.core.config.Parameter.ChainSymbol.TRX_SYMBOL;
import static org.tron.core.config.Parameter.ChainSymbol.TRX_SYMBOL_BYTES;

import com.google.protobuf.InvalidProtocolBufferException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.common.entity.Dec;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.ByteUtil;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.capsule.DecOracleRewardCapsule;
import org.tron.core.capsule.OraclePrevoteCapsule;
import org.tron.core.db.TronStoreWithRevoking;
import org.tron.core.db2.common.WrappedByteArray;
import org.tron.protos.Protocol.OracleVote;
import org.tron.protos.contract.StableMarketContractOuterClass.StableCoinContract;
import org.tron.protos.contract.StableMarketContractOuterClass.StableCoinInfo;
import org.tron.protos.contract.StableMarketContractOuterClass.StableCoinInfoList;

@Slf4j(topic = "DB")
@Component
public class StableMarketStore extends TronStoreWithRevoking<BytesCapsule> {

  private static final int TOBIN_FEE_DECIMAL = 3;
  private static final Dec DEFAULT_BASE_POOL = Dec.newDec("25000000000000");
  private static final long DEFAULT_POOL_RECOVERY_PERIOD = 10;
  private static final Dec DEFAULT_MIN_SPREAD = Dec.newDecWithPrec(5, 3);

  private static final String TOBIN_FEE_SET_FAILED = "set tobin fee failed, ";

  private static final byte[] STABLE_COIN_PREFIX = "stable_".getBytes();
  private static final byte[] BASE_POOL = "basepool".getBytes();
  private static final byte[] POOL_RECOVERY_PERIOD = "pool_recovery_period".getBytes();
  private static final byte[] POOL_DELTA = "delta".getBytes();
  private static final byte[] MIN_SPREAD = "min_spread".getBytes();
  private static final byte[] TRX_EXCHANGE_AMOUNT = "trx_exchange_amount".getBytes();
  private static final byte[] SDR_TOKEN_ID = "sdr_token_id".getBytes();

  // for oracle module begin
  private static final byte[] EXCHANGE_RATE = "exchange_rate".getBytes();
  private static final byte[] ORACLE_FEEDER = "oracle_feeder".getBytes();
  private static final byte[] ORACLE_VOTE = "oracle_vote".getBytes();
  private static final byte[] ORACLE_PREVOTE = "oracle_prevote".getBytes();
  private static final byte[] ORACLE_TOBIN_TAX = "oracle_tobin_tax".getBytes();
  private static final byte[] ORACLE_MISS = "oracle_miss".getBytes();
  private static final byte[] REWARD_POOL = "reward_pool".getBytes();
  private static final byte[] REWARD_TOTAL = "reward_total".getBytes();
  // for oracle module end

  @Autowired
  private AssetIssueV2Store assetIssueV2Store;

  @Autowired
  private StableMarketStore(@Value("stable-market") String dbName) {
    super(dbName);
  }

  public StableCoinInfoList getStableCoinList() {
    StableCoinInfoList.Builder result = StableCoinInfoList.newBuilder();
    Map<WrappedByteArray, byte[]> allStableCoins = prefixQuery(STABLE_COIN_PREFIX);
    if (allStableCoins == null) {
      return result.build();
    }
    allStableCoins.forEach((key, data) -> {
      int tokenLength = key.getBytes().length - STABLE_COIN_PREFIX.length;
      byte[] tokenId = new byte[tokenLength];
      System.arraycopy(key.getBytes(), STABLE_COIN_PREFIX.length, tokenId, 0, tokenLength);

      try {
        if (!ByteUtil.isNullOrZeroArray(data)) {
          AssetIssueCapsule assetIssueContract = assetIssueV2Store.get(tokenId);
          StableCoinContract stableCoin = StableCoinContract.parseFrom(data);
          result.addStableCoinInfo(
              StableCoinInfo.newBuilder()
                  .setAssetIssue(assetIssueContract.getInstance())
                  .setTobinFee(Dec.newDec(stableCoin.getTobinFee()).toString())// todo check
                  .build()
          );
        } else {
          // todo: optimize
          throw new RuntimeException(TOBIN_FEE_SET_FAILED + "data is null");
        }
      } catch (InvalidProtocolBufferException e) {
        throw new RuntimeException(TOBIN_FEE_SET_FAILED + e.getMessage());
      }
    });
    return result.build();
  }

  public StableCoinInfo getStableCoinInfoById(byte[] tokenId) {
    BytesCapsule data = getUnchecked(buildKey(STABLE_COIN_PREFIX, tokenId));
    if (data != null && !ByteUtil.isNullOrZeroArray(data.getData())) {
      AssetIssueCapsule assetIssueContract = assetIssueV2Store.get(tokenId);
      if (assetIssueContract == null || assetIssueContract.getData() == null
          || assetIssueContract.getData().length == 0) {
        throw new RuntimeException("fatal: stable asset not exist");
      }
      // todo: optimize
      Dec tobinTax = getProposalTobinFee(tokenId);
      return StableCoinInfo.newBuilder()
          .setAssetIssue(assetIssueContract.getInstance())
          .setTobinFee(tobinTax.toString())  // todo check
          .build();
    } else {
      return null;
    }
  }

  public byte[] getSDRTokenId() {
    // todo: replace
//    BytesCapsule data = getUnchecked(SDR_TOKEN_ID);
//    if (data == null || ByteUtil.isNullOrZeroArray(data.getData())) {
//      return null;
//    }
//    return data.getData();
    return "1000001".getBytes();
  }

  public void setSDRTokenId(byte[] tokenId) {
    this.put(SDR_TOKEN_ID, new BytesCapsule(tokenId));
  }

  public void setStableCoin(byte[] tokenId, long tobinFee) {
    Dec fee = Dec.newDecWithPrec(tobinFee, TOBIN_FEE_DECIMAL);
    StableCoinContract stableCoin = StableCoinContract.newBuilder()
        .setAssetIssueId(ByteArray.toStr(tokenId))
        .setTobinFee(fee.toString())
        .build();
    this.put(buildKey(STABLE_COIN_PREFIX, tokenId), new BytesCapsule(stableCoin.toByteArray()));
  }

  public Dec getProposalTobinFee(byte[] tokenId) {
    BytesCapsule data = getUnchecked(buildKey(STABLE_COIN_PREFIX, tokenId));
    try {
      if (data != null && !ByteUtil.isNullOrZeroArray(data.getData())) {
        StableCoinContract stableCoinContract = StableCoinContract.parseFrom(data.getData());
        return Dec.newDec(stableCoinContract.getTobinFee());
      } else {
        // todo: optimize
        throw new RuntimeException("get stable coin failed, data is null");
      }
    } catch (InvalidProtocolBufferException e) {
      throw new RuntimeException(TOBIN_FEE_SET_FAILED + e.getMessage());
    }
  }

  public void setProposalTobinFee(byte[] tokenId, long tobinFee) {
    Dec fee = Dec.newDecWithPrec(tobinFee, TOBIN_FEE_DECIMAL);
    BytesCapsule data = getUnchecked(buildKey(STABLE_COIN_PREFIX, tokenId));
    try {
      if (data != null && !ByteUtil.isNullOrZeroArray(data.getData())) {
        StableCoinContract stableCoin = StableCoinContract.parseFrom(data.getData());
        stableCoin = stableCoin.toBuilder().setTobinFee(fee.toString()).build();
        this.put(buildKey(STABLE_COIN_PREFIX, tokenId), new BytesCapsule(stableCoin.toByteArray()));
      } else {
        // todo: optimize
        throw new RuntimeException(TOBIN_FEE_SET_FAILED + "data is null");
      }
    } catch (InvalidProtocolBufferException e) {
      throw new RuntimeException(TOBIN_FEE_SET_FAILED + e.getMessage());
    }
  }

  public Dec getBasePool() {
    BytesCapsule data = getUnchecked(BASE_POOL);
    if (data != null && !ByteUtil.isNullOrZeroArray(data.getData())) {
      return Dec.newDec(data.getData());
    } else {
      return DEFAULT_BASE_POOL;
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
      return DEFAULT_POOL_RECOVERY_PERIOD;
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
      // todo: whether should return NULL?
      return Dec.zeroDec();
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
      return DEFAULT_MIN_SPREAD;
    }
  }

  public void setMinSpread(Dec spread) {
    this.put(MIN_SPREAD, new BytesCapsule(spread.toByteArray()));
  }

  public long getTrxExchangeAmount() {
    BytesCapsule data = getUnchecked(TRX_EXCHANGE_AMOUNT);
    if (data != null && !ByteUtil.isNullOrZeroArray(data.getData())) {
      return ByteArray.toLong(data.getData());
    } else {
      return 0;
    }
  }

  public void setTrxExchangeAmount(long amount) {
    this.put(TRX_EXCHANGE_AMOUNT, new BytesCapsule(ByteArray.fromLong(amount)));
  }

  public Dec getOracleExchangeRate(byte[] tokenId) {
    if (Arrays.equals(TRX_SYMBOL_BYTES, tokenId)) {
      return Dec.oneDec();
    }
    BytesCapsule data = getUnchecked(buildKey(EXCHANGE_RATE, tokenId));
    if (data != null && !ByteUtil.isNullOrZeroArray(data.getData())) {
      return Dec.newDec(data.getData());
    } else {
      return null;
    }
  }

  public void setOracleExchangeRate(byte[] tokenId, Dec rate) {
    // todo: change to debug?
    logger.info(String.format(
        "update exchange rate, tokenid: %s, rate: %s", ByteArray.toStr(tokenId), rate.toString()));
    this.put(buildKey(EXCHANGE_RATE, tokenId), new BytesCapsule(rate.toByteArray()));
  }

  public void clearAllOracleExchangeRates() {
    Map<WrappedByteArray, byte[]> allExchangeRates = prefixQuery(EXCHANGE_RATE);
    allExchangeRates.forEach((key, value) -> {
      delete(key.getBytes());
    });
  }

  public void setVote(byte[] srAddress, OracleVote vote) {
    byte[] key = buildKey(ORACLE_VOTE, srAddress);
    put(key, new BytesCapsule(vote.toByteArray()));
  }

  public OracleVote getVote(byte[] srAddress) {
    BytesCapsule vote = getUnchecked(buildKey(ORACLE_VOTE, srAddress));
    if (vote.getData() == null) {
      return null;
    }
    OracleVote oracleVote;
    try {
      oracleVote = OracleVote.parseFrom(vote.getData());
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      return null;
    }
    return oracleVote;
  }

  public void clearPrevoteAndVotes(long blockNum, long votePeriod) {
    Map<WrappedByteArray, byte[]> prevotes = prefixQuery(ORACLE_PREVOTE);
    prevotes.forEach((key, value) -> {
      OraclePrevoteCapsule prevote = new OraclePrevoteCapsule(value);
      if (blockNum > prevote.getInstance().getBlockNum() + votePeriod) {
        delete(key.getBytes());
      }
    });
    Map<WrappedByteArray, byte[]> votes = prefixQuery(ORACLE_VOTE);
    votes.forEach((key, value) -> {
      delete(key.getBytes());
    });
  }

  public Map<String, Dec> getAllTobinTax() {
    Map<String, Dec> result = new HashMap<>();
    Map<WrappedByteArray, byte[]> allTobinTax = prefixQuery(ORACLE_TOBIN_TAX);
    allTobinTax.forEach((key, value) -> {
      int tokenLength = key.getBytes().length - ORACLE_TOBIN_TAX.length;
      byte[] tokenID = new byte[tokenLength];
      System.arraycopy(key.getBytes(), ORACLE_TOBIN_TAX.length, tokenID, 0, tokenLength);
      result.put(new String(tokenID), Dec.newDec(value));
    });
    return result;
  }

  public void clearAllTobinTax() {
    Map<WrappedByteArray, byte[]> allTobinTax = prefixQuery(ORACLE_TOBIN_TAX);
    allTobinTax.forEach((key, value) -> {
      delete(key.getBytes());
    });
  }

  public Dec getTobinFee(byte[] tokenId) {
    BytesCapsule data = getUnchecked(buildKey(ORACLE_TOBIN_TAX, tokenId));
    if (data == null || ByteUtil.isNullOrZeroArray(data.getData())) {
      return null;
    }
    return Dec.newDec(data.getData());
  }

  public void updateTobinTax(Map<String, Dec> oracleTobinTaxMap) {
    // check is there any update in proposal
    boolean needUpdate = false;
    StableCoinInfoList stableCoinList = getStableCoinList();
    if (stableCoinList == null) {

      if (oracleTobinTaxMap != null) {
        clearAllTobinTax();
      }
      return;
    } else if (oracleTobinTaxMap == null) {
      needUpdate = true;
    } else if (stableCoinList.getStableCoinInfoList().size() != oracleTobinTaxMap.size()) {
      needUpdate = true;
    } else {
      for (StableCoinInfo stableCoinInfo : stableCoinList.getStableCoinInfoList()) {
        Dec oracleTobin = oracleTobinTaxMap.get(stableCoinInfo.getAssetIssue().getId());
        if (oracleTobin == null || !oracleTobin.eq(Dec.newDec(stableCoinInfo.getTobinFee()))) {
          needUpdate = true;
          break;
        }
      }
    }
    if (needUpdate) {
      logger.info("tobin tax need to update");
      clearAllTobinTax();
      for (StableCoinInfo stableCoinInfo : stableCoinList.getStableCoinInfoList()) {
        Dec tobinTax = Dec.newDec(stableCoinInfo.getTobinFee());
        byte[] key = buildKey(ORACLE_TOBIN_TAX,
            Objects.requireNonNull(ByteArray.fromString(stableCoinInfo.getAssetIssue().getId())));
        put(key, new BytesCapsule(tobinTax.toByteArray()));
      }
    }
  }

  public void setFeeder(byte[] srAddress, byte[] feederAddress) {
    byte[] key = buildKey(ORACLE_FEEDER, srAddress);

    // if feeder == sr or fedder is empty, delete sr feeder from db
    if (Arrays.equals(srAddress, feederAddress) || ArrayUtils.isEmpty(feederAddress)) {
      delete(key);
      return;
    }

    put(key, new BytesCapsule(feederAddress));
  }

  public byte[] getFeeder(byte[] srAddress) {
    BytesCapsule feeder = getUnchecked(buildKey(ORACLE_FEEDER, srAddress));
    return feeder != null ? feeder.getData() : null;
  }

  public void setPrevote(byte[] srAddress, OraclePrevoteCapsule oraclePrevoteCapsule) {
    byte[] key = buildKey(ORACLE_PREVOTE, srAddress);
    put(key, new BytesCapsule(oraclePrevoteCapsule.getData()));
  }

  public OraclePrevoteCapsule getPrevote(byte[] srAddress) {
    BytesCapsule prevote = getUnchecked(buildKey(ORACLE_PREVOTE, srAddress));
    if (prevote.getData() != null) {
      return new OraclePrevoteCapsule(prevote.getData());
    }
    return null;
  }

  public void setWitnessMissCount(byte[] address, long value) {
    put(buildKey(ORACLE_MISS, address), new BytesCapsule(ByteArray.fromLong(value)));
  }

  public long getWitnessMissCount(byte[] address) {
    BytesCapsule bytesCapsule = getUnchecked(buildKey(ORACLE_MISS, address));
    if (bytesCapsule == null) {
      return 0;
    } else {
      return ByteArray.toLong(bytesCapsule.getData());
    }
  }

  public Map<byte[], Long> getAllWitnessMissCount() {
    Map<byte[], Long> result = new HashMap<>();
    Map<WrappedByteArray, byte[]> allWitnessMissCount = prefixQuery(ORACLE_MISS);
    allWitnessMissCount.forEach((key, value) -> {
      int tokenLength = key.getBytes().length - ORACLE_MISS.length;
      byte[] address = new byte[tokenLength];
      System.arraycopy(key.getBytes(), ORACLE_MISS.length, address, 0, tokenLength);
      result.put(address, ByteArray.toLong(value));
    });
    return result;
  }

  public void clearAllWitnessMissCount() {
    Map<WrappedByteArray, byte[]> allWitnessMissCount = prefixQuery(ORACLE_MISS);
    allWitnessMissCount.forEach((key, value) -> {
      delete(key.getBytes());
    });
  }

  public void deleteWitnessMissCount(byte[] address) {
    delete(buildKey(ORACLE_MISS, address));
  }

  public void addOracleRewardPool(String denom, Dec amount) {
    Dec balance = Dec.zeroDec();
    Map<String, Dec> asset = new HashMap<>();
    if (TRX_SYMBOL.equals(denom)) {
      balance = balance.add(amount);
    }else{
      asset.put(denom, amount);
    }
    DecOracleRewardCapsule reward = new DecOracleRewardCapsule(balance, asset);

    BytesCapsule bytesCapsule = getUnchecked(REWARD_POOL);
    if (bytesCapsule == null || bytesCapsule.getData() == null) {
      put(REWARD_POOL, new BytesCapsule(reward.getData()));
    } else {
      put(REWARD_POOL, new BytesCapsule(new DecOracleRewardCapsule(bytesCapsule.getData())
          .add(reward).getData()));
    }
  }

  public void addDistributedReward(DecOracleRewardCapsule reward) {
    BytesCapsule bytesCapsule = getUnchecked(REWARD_TOTAL);
    if (bytesCapsule == null || bytesCapsule.getData() == null) {
      put(REWARD_TOTAL, new BytesCapsule(reward.getData()));
    } else {
      put(REWARD_TOTAL, new BytesCapsule(new DecOracleRewardCapsule(bytesCapsule.getData())
          .add(reward).getData()));
    }
  }

  public DecOracleRewardCapsule getOracleRewardPool() {
    BytesCapsule bytesCapsule = getUnchecked(REWARD_POOL);
    if (bytesCapsule == null || bytesCapsule.getData() == null) {
      return new DecOracleRewardCapsule();
    }
    return new DecOracleRewardCapsule(bytesCapsule.getData());
  }

  public DecOracleRewardCapsule getDistributedReward() {
    BytesCapsule bytesCapsule = getUnchecked(REWARD_TOTAL);
    if (bytesCapsule == null || bytesCapsule.getData() == null) {
      return new DecOracleRewardCapsule();
    }
    return new DecOracleRewardCapsule(bytesCapsule.getData());
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

  public static Map<String, Dec> parseExchangeRateTuples(String exchangeRatesStr) {
    exchangeRatesStr = exchangeRatesStr.trim();
    if (exchangeRatesStr.isEmpty()) {
      throw new RuntimeException("exchange rate string cannot be empty");
    }

    Map<String, Dec> ex = new HashMap<>();
    String[] exchangeRateList = exchangeRatesStr.split(",");
    for (String e : exchangeRateList) {
      String[] exchangeRatePair = e.split(":");
      if (exchangeRatePair.length != 2) {
        throw new RuntimeException("exchange rate pair length error");
      }
      String asset = exchangeRatePair[0];
      String exchangeRate = exchangeRatePair[1];
      ex.put(asset, Dec.newDec(exchangeRate));
    }
    return ex;
  }
}
