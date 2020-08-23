package org.tron.core.store;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.iq80.leveldb.Options;
import org.rocksdb.ComparatorOptions;
import org.rocksdb.DirectComparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.common.utils.ByteUtil;
import org.tron.common.utils.MarketOrderPriceComparatorForLevelDB;
import org.tron.common.utils.MarketOrderPriceComparatorForRockDB;
import org.tron.common.utils.StorageUtils;
import org.tron.core.capsule.MarketOrderIdListCapsule;
import org.tron.core.capsule.utils.MarketUtils;
import org.tron.core.db.TronStoreWithRevoking;
import org.tron.core.exception.ItemNotFoundException;

@Slf4j(topic = "DB")
@Component
public class MarketPairPriceToOrderStore extends TronStoreWithRevoking<MarketOrderIdListCapsule> {

  @Autowired
  protected MarketPairPriceToOrderStore(@Value("market_pair_price_to_order") String dbName) {
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
  protected DirectComparator getDirectComparator() {
    ComparatorOptions comparatorOptions = new ComparatorOptions();
    return new MarketOrderPriceComparatorForRockDB(comparatorOptions);
  }

  @Override
  public MarketOrderIdListCapsule get(byte[] key) throws ItemNotFoundException {
    byte[] value = revokingDB.get(key);
    return new MarketOrderIdListCapsule(value);
  }

  public List<byte[]> getKeysNext(byte[] key, long limit) {
    if (limit <= 0) {
      return Collections.emptyList();
    }

    return revokingDB.getKeysNext(key, limit);
  }

  public List<byte[]> getPriceKeysList(byte[] sellTokenId, byte[] buyTokenId, long count) {
    byte[] headKey = MarketUtils.getPairPriceHeadKey(sellTokenId, buyTokenId);
    return getPriceKeysList(headKey, count, count, true);
  }

  /**
   * Note: when skip is true, neither count nor totalCount includes the headKey.
   *   The limit should be smaller than the max int.
   * number: want to get
   * totalCount: largest count
   *
   * */
  public List<byte[]> getPriceKeysList(byte[] headKey, long count, long totalCount, boolean skip) {
    List<byte[]> result = new ArrayList<>();

    logger.info(String.format("[getPriceKeysList] headKey is %s, count: %d, totalCount: %d",
        new String(headKey), count, totalCount));

    if (has(headKey)) {
      long limit = count > totalCount ? totalCount : count;
      logger.info(String.format("[getPriceKeysList] limit: %d", limit));
      if (skip) {
        // need to get one more
        result = getKeysNext(headKey, limit + 1);
        logger.info(String.format("[getPriceKeysList] allResult num is %d, is %s,",
            result.size(), Arrays.toString(result.toArray())));
        result = result.subList(1, (int)(limit + 1));
      } else {
        result = getKeysNext(headKey, limit);
      }
    }

    return result;
  }

  public byte[] getNextKey(byte[] key) {
    // contain the key
    List<byte[]> keysNext = revokingDB.getKeysNext(key, 2);
    if (keysNext.size() < 2) {
      return new byte[0];
    } else {
      return ByteUtil.equals(keysNext.get(0), key) ? keysNext.get(1) : keysNext.get(0);
    }
  }
}