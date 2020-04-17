package org.tron.core.store;

import java.util.List;
import org.iq80.leveldb.Options;
import org.rocksdb.ComparatorOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.common.utils.ByteUtil;
import org.tron.common.utils.MarketOrderPriceComparatorForLevelDB;
import org.tron.common.utils.MarketOrderPriceComparatorForRockDB;
import org.tron.common.utils.StorageUtils;
import org.tron.core.capsule.MarketPriceLinkedListCapsule;
import org.tron.core.db.TronStoreWithRevoking;
import org.tron.core.exception.ItemNotFoundException;

@Component
public class MarketPairToPriceStore extends TronStoreWithRevoking<MarketPriceLinkedListCapsule> {

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

  @Override
  public MarketPriceLinkedListCapsule get(byte[] key) throws ItemNotFoundException {
    byte[] value = revokingDB.get(key);
    return new MarketPriceLinkedListCapsule(value);
  }


  public byte[] getNextKey(byte[] key) {
    //contain the key
    List<byte[]> keysNext = revokingDB.getKeysNext(key, 2);
    return ByteUtil.equals(keysNext.get(0), key) ? keysNext.get(1) : keysNext.get(0);
  }
}