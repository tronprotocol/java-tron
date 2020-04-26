package org.tron.core.store;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;
import org.iq80.leveldb.Options;
import org.rocksdb.ComparatorOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.ByteUtil;
import org.tron.common.utils.MarketOrderPriceComparatorForLevelDB;
import org.tron.common.utils.MarketOrderPriceComparatorForRockDB;
import org.tron.common.utils.StorageUtils;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.capsule.utils.MarketUtils;
import org.tron.core.db.TronStoreWithRevoking;

@Component
public class MarketPairToPriceStore extends TronStoreWithRevoking<BytesCapsule> {
  public static final int MARKET_PRICE_COUNT_LIMIT_MAX = 1000;

  @Autowired
  protected MarketPairToPriceStore(@Value("market_pair_to_price") String dbName) {
    super(dbName);
  }

  @Override
  protected Options getOptionsByDbNameForLevelDB(String dbName) {
    Options options = StorageUtils.getOptionsByDbName(dbName);
    options.comparator(new MarketOrderPriceComparatorForLevelDB());
    return options;
  }

  //todo: to test later
  @Override
  protected org.rocksdb.Options getOptionsForRockDB() {
    ComparatorOptions comparatorOptions = new ComparatorOptions();
    org.rocksdb.Options options = new org.rocksdb.Options();
    options.setComparator(new MarketOrderPriceComparatorForRockDB(comparatorOptions));
    return options;
  }

  // @Override
  // public MarketPriceLinkedListCapsule get(byte[] key) throws ItemNotFoundException {
  //   byte[] value = revokingDB.get(key);
  //   return new MarketPriceLinkedListCapsule(value);
  // }

  @Override
  public BytesCapsule get(byte[] key) {
    byte[] value = revokingDB.getUnchecked(key);
    return ArrayUtils.isEmpty(value) ? null : new BytesCapsule(value);
  }

  public void putPrice(byte[] key) {
    put(key, new BytesCapsule(ByteArray.fromLong(0)));
  }

  public long getPriceNum(byte[] key) {
    BytesCapsule bytesCapsule = get(key);
    if (bytesCapsule != null) {
      return ByteArray.toLong(bytesCapsule.getData());
    } else {
      return 0L;
    }
  }

  public long getPriceNum(byte[] sellTokenId, byte[] buyTokenId) {
    return getPriceNum(getHeadKey(sellTokenId, buyTokenId));
  }

  public void setPriceNum(byte[] key, long number) {
    put(key, new BytesCapsule(ByteArray.fromLong(number)));
  }

  public void setPriceNum(byte[] sellTokenId, byte[] buyTokenId, long number) {
    setPriceNum(getHeadKey(sellTokenId, buyTokenId), number);
  }

  public boolean hasPrice(byte[] key) {
    return has(key);
  }

  public boolean hasPrice(byte[] sellTokenId, byte[] buyTokenId) {
    return hasPrice(getHeadKey(sellTokenId, buyTokenId));
  }

  public byte[] getNextKey(byte[] key) {
    // contain the key
    List<byte[]> keysNext = revokingDB.getKeysNext(key, 2);
    return ByteUtil.equals(keysNext.get(0), key) ? keysNext.get(1) : keysNext.get(0);
  }

  /**
   * The first price key of one token
   * Because using the price compare, we can set the smallest price as the first one.
   * */
  public byte[] getHeadKey(byte[] sellTokenId, byte[] buyTokenId) {
    return MarketUtils.createPairPriceKey(sellTokenId, buyTokenId, 0L, 0L);
  }

  /**
   * Get the price key list
   * The first key of token is sellToken + buyToken + 0 + 0
   *
   * @param number it should not be large than the max int
   * @param insertHead if True, insert the head key
   * */
  public List<byte[]> getPriceKeysList(byte[] sellTokenId, byte[] buyTokenId,
      long number, boolean insertHead) {
    // getKeysNext will contain the input key which is used as placeholder, so it need get one more
    long limit = number + 1;
    List<byte[]> result = new ArrayList<>();

    byte[] headKey = getHeadKey(sellTokenId, buyTokenId);
    if (hasPrice(headKey)) {
      long priceNum = getPriceNum(headKey);
      if (priceNum > 0) {
        // skip the head key
        long fetchNum = priceNum + 1;
        long end = fetchNum > limit ? limit : fetchNum;
        result = getKeysNext(headKey, fetchNum).subList(1, (int)end);
      }
    } else if (insertHead) {
      setPriceNum(headKey, 0L);
    }

    return result;
  }

  public List<byte[]> getPriceKeysList(byte[] sellTokenId, byte[] buyTokenId) {
    return getPriceKeysList(sellTokenId, buyTokenId, MARKET_PRICE_COUNT_LIMIT_MAX, false);
  }

  public List<byte[]> getKeysNext(byte[] key, long limit) {
    return revokingDB.getKeysNext(key, limit);
  }

  /**
   * put priceKey to store, modify headKey's value which indicate the price number
   * put priceCapsule to MarketPriceStore
   * */
  public void addPriceKey(byte[] sellTokenId, byte[] buyTokenId,
      long sellTokenQuantity, long buyTokenQuantity) {
    byte[] headKey = getHeadKey(sellTokenId, buyTokenId);
    byte[] newPriceKey = MarketUtils
        .createPairPriceKey(sellTokenId, buyTokenId, sellTokenQuantity, buyTokenQuantity);
    long number;

    // check if already exited
    if (has(newPriceKey)) {
      return;
    }

    // add new key
    if (!hasPrice(headKey)) {
      number = 1;
    } else {
      number = getPriceNum(headKey) + 1;
    }

    // update DB
    setPriceNum(headKey, number);
    putPrice(newPriceKey);
  }

  public void deletePriceKey(byte[] sellTokenId, byte[] buyTokenId,
      byte[] priceKey) {
    byte[] headKey = getHeadKey(sellTokenId, buyTokenId);
    long number;

    // check if not exited
    if (!has(priceKey)) {
      return;
    }

    // delete key
    if (!hasPrice(headKey)) {
      // should never happened TODO add raise
      number = 0;
    } else {
      number = getPriceNum(headKey) - 1;
    }

    // update DB
    setPriceNum(headKey, number);
    delete(priceKey);
  }
}