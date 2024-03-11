package org.tron.core.db2.common;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;
import com.google.common.primitives.Longs;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.Getter;
import lombok.Setter;
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
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.JsonUtil;
import org.tron.common.utils.StorageUtils;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.db.RecentTransactionItem;
import org.tron.core.db.RecentTransactionStore;
import org.tron.core.db.common.iterator.DBIterator;
import org.tron.core.store.DynamicPropertiesStore;

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
  @Getter
  private DB<byte[], byte[]> persistentStore;

  // replace persistentStore and optimizes startup performance
  private RecentTransactionStore recentTransactionStore;

  private DynamicPropertiesStore dynamicPropertiesStore;

  private final Path cacheFile0;
  private final Path cacheFile1;
  private String crc32c0;
  private String crc32c1;
  private final Path cacheProperties;
  private final Path cacheDir;
  private AtomicBoolean isValid = new AtomicBoolean(false);
  private boolean txCacheInitOptimization;

  @Getter
  @Setter
  private volatile boolean alive;

  public TxCacheDB(String name, RecentTransactionStore recentTransactionStore,
                   DynamicPropertiesStore dynamicPropertiesStore) {
    this.name = name;
    this.TRANSACTION_COUNT =
        CommonParameter.getInstance().getStorage().getEstimatedBlockTransactions();
    this.recentTransactionStore = recentTransactionStore;
    this.dynamicPropertiesStore = dynamicPropertiesStore;
    String dbEngine = CommonParameter.getInstance().getStorage().getDbEngine();
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
      throw new RuntimeException(String.format("db type: %s is not supported", dbEngine));
    }
    this.bloomFilters[0] = BloomFilter.create(Funnels.byteArrayFunnel(),
        MAX_BLOCK_SIZE * TRANSACTION_COUNT);
    this.bloomFilters[1] = BloomFilter.create(Funnels.byteArrayFunnel(),
        MAX_BLOCK_SIZE * TRANSACTION_COUNT);
    cacheDir = Paths.get(CommonParameter.getInstance().getOutputDirectory(), ".cache");
    this.cacheFile0 = Paths.get(cacheDir.toString(), "bloomFilters_0");
    this.cacheFile1 = Paths.get(cacheDir.toString(), "bloomFilters_1");
    this.cacheProperties = Paths.get(cacheDir.toString(), "txCache.properties");
    this.txCacheInitOptimization = CommonParameter.getInstance()
        .getStorage().isTxCacheInitOptimization();

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
    logger.info("Load cache from persistentStore, db: {}, filter: {}, filter-fpp: {}, cost: {} ms.",
        persistentSize,
        bloomFilters[1].approximateElementCount(), bloomFilters[1].expectedFpp(),
        System.currentTimeMillis() - start);
  }

  public void init() {
    if (recovery()) {
      isValid.set(true);
      setAlive(true);
      return;
    }
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

    logger.info("Load cache from recentTransactionStore, filter: {}, filter-fpp: {}, cost: {} ms.",
        bloomFilters[1].approximateElementCount(), bloomFilters[1].expectedFpp(),
        System.currentTimeMillis() - start);
    isValid.set(true);
    setAlive(true);
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
      logger.info("Init tx cache bloomFilters at {}.", blockNum);
    } else if (blockNum - filterStartBlock > MAX_BLOCK_SIZE) {
      // active filter is full
      logger.info(
          "Active bloomFilters is full (size = {} fpp = {}), create a new one (start = {}).",
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
  public synchronized void flush(Map<WrappedByteArray, WrappedByteArray> batch) {
    isValid.set(false);
    batch.forEach((k, v) -> this.put(k.getBytes(), v.getBytes()));
    isValid.set(true);
  }

  @Override
  public void close() {
    if (!isAlive()) {
      return;
    }
    dump();
    bloomFilters[0] = null;
    bloomFilters[1] = null;
    persistentStore.close();
    setAlive(false);
  }

  @Override
  public void reset() {
  }

  private boolean recovery() {
    if (!txCacheInitOptimization) {
      logger.info("txCache init optimization is disabled, skip fast recovery mode.");
      logger.info("If you want fast recovery mode,"
          + " please set `storage.txCache.initOptimization = true` in config.conf.");
      return false;
    }
    FileUtil.createDirIfNotExists(this.cacheDir.toString());
    logger.info("recovery bloomFilters start.");
    CompletableFuture<Boolean> loadProperties = CompletableFuture.supplyAsync(this::loadProperties);
    CompletableFuture<Boolean> tk0 = loadProperties.thenApplyAsync(
        v -> recovery(0, this.cacheFile0));
    CompletableFuture<Boolean> tk1 = loadProperties.thenApplyAsync(
        v -> recovery(1, this.cacheFile1));

    try {
      return CompletableFuture.allOf(tk0, tk1).thenApply(v -> {
        logger.info("recovery bloomFilters success.");
        return true;
      }).exceptionally(this::handleException).join();
    } finally {
      clearCrc32c();
    }
  }

  private boolean recovery(int index, Path file) {
    checkCrc32c(index, file);
    try (InputStream in = new BufferedInputStream(Files.newInputStream(file,
        StandardOpenOption.READ, StandardOpenOption.DELETE_ON_CLOSE))) {
      logger.info("recovery bloomFilter[{}] from file.", index);
      long start = System.currentTimeMillis();
      bloomFilters[index] = BloomFilter.readFrom(in, Funnels.byteArrayFunnel());
      logger.info("recovery bloomFilter[{}] from file done,filter: {}, filter-fpp: {}, cost {} ms.",
          index, bloomFilters[index].approximateElementCount(), bloomFilters[index].expectedFpp(),
          System.currentTimeMillis() - start);
      return true;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private boolean handleException(Throwable e) {
    bloomFilters[0] = BloomFilter.create(Funnels.byteArrayFunnel(),
        MAX_BLOCK_SIZE * TRANSACTION_COUNT);
    bloomFilters[1] = BloomFilter.create(Funnels.byteArrayFunnel(),
        MAX_BLOCK_SIZE * TRANSACTION_COUNT);
    try {
      Files.deleteIfExists(this.cacheFile0);
      Files.deleteIfExists(this.cacheFile1);
    } catch (Exception ignored) {

    }
    logger.info("recovery bloomFilters failed. {}", e.getMessage());
    logger.info("rollback to previous mode.");
    return false;
  }

  private void dump() {
    if (!isValid.get()) {
      logger.info("bloomFilters is not valid.");
      return;
    }
    FileUtil.createDirIfNotExists(this.cacheDir.toString());
    logger.info("dump bloomFilters start.");
    CompletableFuture<Void> task0 = CompletableFuture.runAsync(
        () -> dump(0, this.cacheFile0));
    CompletableFuture<Void> task1 = CompletableFuture.runAsync(
        () -> dump(1, this.cacheFile1));
    try {
      CompletableFuture.allOf(task0, task1).thenRun(() -> {
        writeProperties();
        logger.info("dump bloomFilters done.");
      }).exceptionally(e -> {
        logger.info("dump bloomFilters to file failed. {}", e.getMessage());
        return null;
      }).join();
    } finally {
      clearCrc32c();
    }
  }

  private void dump(int index, Path file) {
    logger.info("dump bloomFilters[{}] to file.", index);
    long start = System.currentTimeMillis();
    try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(file))) {
      bloomFilters[index].writeTo(out);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    try {
      String crc32c = getCrc32c(file);
      if (index == 0) {
        this.crc32c0 = crc32c;
      } else {
        this.crc32c1 = crc32c;
      }
      logger.info("dump bloomFilters[{}] to file done,filter: {}, filter-fpp: {}, "
              + "crc32c: {}, cost {} ms.",
          index, bloomFilters[index].approximateElementCount(), bloomFilters[index].expectedFpp(),
          crc32c, System.currentTimeMillis() - start);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private boolean loadProperties() {
    try (Reader r = new InputStreamReader(new BufferedInputStream(Files.newInputStream(
        this.cacheProperties, StandardOpenOption.READ, StandardOpenOption.DELETE_ON_CLOSE)),
        StandardCharsets.UTF_8)) {
      Properties properties = new Properties();
      properties.load(r);
      filterStartBlock = Long.parseLong(properties.getProperty("filterStartBlock"));
      long currentBlockNum = Long.parseLong(properties.getProperty("currentBlockNum"));
      long currentBlockNumFromDB = dynamicPropertiesStore.getLatestBlockHeaderNumberFromDB();
      currentFilterIndex = Integer.parseInt(properties.getProperty("currentFilterIndex"));
      if (currentBlockNum != currentBlockNumFromDB) {
        throw new IllegalStateException(
            String.format("currentBlockNum not match. filter: %d, db: %d",
            currentBlockNum, currentBlockNumFromDB));
      }
      this.crc32c0 = properties.getProperty("crc32c0");
      this.crc32c1 = properties.getProperty("crc32c1");
      logger.info("filterStartBlock: {}, currentBlockNum: {}, currentFilterIndex: {}, load done.",
          filterStartBlock, currentBlockNum, currentFilterIndex);
      return true;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void writeProperties() {
    try (Writer w = Files.newBufferedWriter(this.cacheProperties, StandardCharsets.UTF_8)) {
      Properties properties = new Properties();
      long currentBlockNum = dynamicPropertiesStore.getLatestBlockHeaderNumberFromDB();
      properties.setProperty("filterStartBlock", String.valueOf(filterStartBlock));
      properties.setProperty("currentBlockNum", String.valueOf(currentBlockNum));
      properties.setProperty("currentFilterIndex", String.valueOf(currentFilterIndex));
      properties.setProperty("crc32c0", this.crc32c0);
      properties.setProperty("crc32c1", this.crc32c1);
      properties.store(w, "Generated by the application.  PLEASE DO NOT EDIT! ");
      logger.info("filterStartBlock: {}, currentBlockNum: {}, currentFilterIndex: {}, write done.",
          filterStartBlock, currentBlockNum, currentFilterIndex);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private String getCrc32c(Path file) throws IOException {
    ByteSource byteSource = com.google.common.io.Files.asByteSource(file.toFile());
    HashCode hc = byteSource.hash(Hashing.crc32c());
    return hc.toString();
  }

  private void checkCrc32c(int index, Path file) {
    try {
      String actual = getCrc32c(file);
      String expect = index == 0 ? this.crc32c0 : this.crc32c1;
      if (!Objects.equals(actual, expect)) {
        throw new IllegalStateException("crc32c not match. index: " + index + ", expect: " + expect
            + ", actual: " + actual);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void clearCrc32c() {
    this.crc32c0 = null;
    this.crc32c1 = null;
  }

  @Override
  public TxCacheDB newInstance() {
    return new TxCacheDB(name, recentTransactionStore, dynamicPropertiesStore);
  }

  @Override
  public void stat() {
  }
}

