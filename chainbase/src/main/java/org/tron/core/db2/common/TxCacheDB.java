package org.tron.core.db2.common;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.google.common.primitives.Longs;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.bouncycastle.util.encoders.Hex;
import org.iq80.leveldb.WriteOptions;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.prometheus.MetricKeys;
import org.tron.common.prometheus.Metrics;
import org.tron.common.storage.leveldb.LevelDbDataSourceImpl;
import org.tron.common.storage.rocksdb.RocksDbDataSourceImpl;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.JsonUtil;
import org.tron.common.utils.StorageUtils;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.db.RecentTransactionItem;
import org.tron.core.db.RecentTransactionStore;
import org.tron.core.db.common.iterator.DBIterator;

@Slf4j(topic = "DB")
public class TxCacheDB implements DB<byte[], byte[]>, Flusher {

  // > 65_536(= 2^16) blocks, that is the number of the reference block
  private static final long MAX_BLOCK_SIZE = 65536;
  // estimated number transactions in one block
  private final int TRANSACTION_COUNT;

  private static final long INVALID_BLOCK = -1;

  // Since the filter cannot query for specific record information,
  // FAKE_TRANSACTION represent the record presence.
  private final byte[] FAKE_TRANSACTION = ByteArray.fromLong(0);

  // a pair of bloom filters record the recent transactions
  private BloomFilter<byte[]>[] bloomFilters = new BloomFilter[2];
  // filterStartBlock record the start block of the active filter
  private volatile long filterStartBlock = INVALID_BLOCK;
  // currentFilterIndex records the index of the active filter
  private volatile int currentFilterIndex = 0;

  // record the last metric block to avoid duplication
  private long lastMetricBlock = 0;

  private final String name;

  // add a persistent storage, the store name is: trans-cache
  // when fullnode startup, transactionCache initializes transactions from this store
  private DB<byte[], byte[]> persistentStore;

  // replace persistentStore and optimizes startup performance
  private RecentTransactionStore recentTransactionStore;

  public TxCacheDB(String name, RecentTransactionStore recentTransactionStore) {
    this.name = name;
    this.TRANSACTION_COUNT =
        CommonParameter.getInstance().getStorage().getEstimatedBlockTransactions();
    this.recentTransactionStore = recentTransactionStore;
    int dbVersion = CommonParameter.getInstance().getStorage().getDbVersion();
    String dbEngine = CommonParameter.getInstance().getStorage().getDbEngine();
    if (dbVersion == 2) {
      if ("LEVELDB".equals(dbEngine.toUpperCase())) {
        this.persistentStore = new LevelDB(
            new LevelDbDataSourceImpl(StorageUtils.getOutputDirectoryByDbName(name),
                name, StorageUtils.getOptionsByDbName(name),
                new WriteOptions().sync(CommonParameter.getInstance()
                    .getStorage().isDbSync())));
      } else if ("ROCKSDB".equals(dbEngine.toUpperCase())) {
        String parentPath = Paths
            .get(StorageUtils.getOutputDirectoryByDbName(name), CommonParameter
                .getInstance().getStorage().getDbDirectory()).toString();

        this.persistentStore = new RocksDB(
            new RocksDbDataSourceImpl(parentPath,
                name, CommonParameter.getInstance()
                .getRocksDBCustomSettings()));
      } else {
        throw new RuntimeException("db type is not supported.");
      }
    } else {
      throw new RuntimeException("db version is not supported.");
    }
    this.bloomFilters[0] = BloomFilter.create(Funnels.byteArrayFunnel(),
        MAX_BLOCK_SIZE * TRANSACTION_COUNT);
    this.bloomFilters[1] = BloomFilter.create(Funnels.byteArrayFunnel(),
        MAX_BLOCK_SIZE * TRANSACTION_COUNT);

    init();
  }

  /**
   * this method only used for init, put all data in tran-cache into the two maps.
   */
  private void initCache() {
    long start = System.currentTimeMillis();
    DBIterator iterator = (DBIterator) persistentStore.iterator();
    long persistentSize = 0;
    while (iterator.hasNext()) {
      Entry<byte[], byte[]> entry = iterator.next();
      if (ArrayUtils.isEmpty(entry.getKey()) || ArrayUtils.isEmpty(entry.getValue())) {
        return;
      }
      bloomFilters[1].put(entry.getKey());
      persistentSize++;
    }
    logger.info("load transaction cache from persistentStore "
            + "db-size:{}, filter-size:{}, filter-fpp:{}, cost:{}ms",
        persistentSize,
        bloomFilters[1].approximateElementCount(), bloomFilters[1].expectedFpp(),
        System.currentTimeMillis() - start);
  }

  private void init() {
    long size = recentTransactionStore.size();
    if (size != MAX_BLOCK_SIZE) {
      // 0. load from persistentStore
      initCache();
    }

    // 1. load from recentTransactionStore
    long start = System.currentTimeMillis();
    for (Entry<byte[], BytesCapsule> bytesCapsuleEntry : recentTransactionStore) {
      byte[] data = bytesCapsuleEntry.getValue().getData();
      RecentTransactionItem trx =
          JsonUtil.json2Obj(new String(data), RecentTransactionItem.class);

      trx.getTransactionIds().forEach(tid -> bloomFilters[1].put(Hex.decode(tid)));
    }

    logger.info("load transaction cache from recentTransactionStore"
            + " filter-size:{}, filter-fpp:{}, cost:{}ms",
        bloomFilters[1].approximateElementCount(), bloomFilters[1].expectedFpp(),
        System.currentTimeMillis() - start);
  }

  @Override
  public byte[] get(byte[] key) {
    if (!bloomFilters[0].mightContain(key) && !bloomFilters[1].mightContain(key)) {
      return null;
    }
    // this means exist
    return FAKE_TRANSACTION;
  }

  @Override
  public void put(byte[] key, byte[] value) {
    if (key == null || value == null) {
      return;
    }

    long blockNum = Longs.fromByteArray(value);
    if (filterStartBlock == INVALID_BLOCK) {
      // init active filter start block
      filterStartBlock = blockNum;
      currentFilterIndex = 0;
      logger.info("init tx cache bloomFilters at {}", blockNum);
    } else if (blockNum - filterStartBlock > MAX_BLOCK_SIZE) {
      // active filter is full
      logger.info("active bloomFilters is full (size={} fpp={}), create a new one (start={})",
          bloomFilters[currentFilterIndex].approximateElementCount(),
          bloomFilters[currentFilterIndex].expectedFpp(),
          blockNum);

      if (currentFilterIndex == 0) {
        currentFilterIndex = 1;
      } else {
        currentFilterIndex = 0;
      }

      filterStartBlock = blockNum;
      bloomFilters[currentFilterIndex] =
          BloomFilter.create(Funnels.byteArrayFunnel(),
              MAX_BLOCK_SIZE * TRANSACTION_COUNT);
    }
    bloomFilters[currentFilterIndex].put(key);

    if (lastMetricBlock != blockNum) {
      lastMetricBlock = blockNum;
      Metrics.gaugeSet(MetricKeys.Gauge.TX_CACHE,
          bloomFilters[currentFilterIndex].approximateElementCount(), "count");
      Metrics.gaugeSet(MetricKeys.Gauge.TX_CACHE,
          bloomFilters[currentFilterIndex].expectedFpp(), "fpp");
    }
  }

  @Override
  public long size() {
    throw new UnsupportedOperationException("TxCacheDB size");
  }

  @Override
  public boolean isEmpty() {
    throw new UnsupportedOperationException("TxCacheDB isEmpty");
  }

  @Override
  public void remove(byte[] key) {
    throw new UnsupportedOperationException("TxCacheDB remove");
  }

  @Override
  public String getDbName() {
    return name;
  }

  @Override
  public Iterator<Entry<byte[], byte[]>> iterator() {
    throw new UnsupportedOperationException("TxCacheDB iterator");
  }

  @Override
  public void flush(Map<WrappedByteArray, WrappedByteArray> batch) {
    batch.forEach((k, v) -> this.put(k.getBytes(), v.getBytes()));
  }

  @Override
  public void close() {
    reset();
    bloomFilters[0] = null;
    bloomFilters[1] = null;
    persistentStore.close();
  }

  @Override
  public void reset() {
  }

  @Override
  public TxCacheDB newInstance() {
    return new TxCacheDB(name, recentTransactionStore);
  }

  @Override
  public void stat() {
  }
}

