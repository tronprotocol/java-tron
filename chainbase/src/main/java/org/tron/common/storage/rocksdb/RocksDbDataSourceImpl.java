package org.tron.common.storage.rocksdb;

import com.google.common.collect.Sets;
import com.google.common.primitives.Bytes;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.BlockBasedTableConfig;
import org.rocksdb.BloomFilter;
import org.rocksdb.Checkpoint;
import org.rocksdb.DirectComparator;
import org.rocksdb.InfoLogLevel;
import org.rocksdb.Logger;
import org.rocksdb.Options;
import org.rocksdb.ReadOptions;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.rocksdb.Statistics;
import org.rocksdb.WriteBatch;
import org.rocksdb.WriteOptions;
import org.slf4j.LoggerFactory;
import org.tron.common.setting.RocksDbSettings;
import org.tron.common.storage.WriteOptionsWrapper;
import org.tron.common.storage.metric.DbStat;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.PropUtil;
import org.tron.core.db.common.DbSourceInter;
import org.tron.core.db.common.iterator.RockStoreIterator;
import org.tron.core.db2.common.Instance;
import org.tron.core.db2.common.WrappedByteArray;


@Slf4j(topic = "DB")
@NoArgsConstructor
public class RocksDbDataSourceImpl extends DbStat implements DbSourceInter<byte[]>,
    Iterable<Map.Entry<byte[], byte[]>>, Instance<RocksDbDataSourceImpl> {

  ReadOptions readOpts;
  private String dataBaseName;
  private RocksDB database;
  private volatile boolean alive;
  private String parentPath;
  private ReadWriteLock resetDbLock = new ReentrantReadWriteLock();
  private static final String KEY_ENGINE = "ENGINE";
  private static final String ROCKSDB = "ROCKSDB";
  private DirectComparator comparator;
  private static final org.slf4j.Logger rocksDbLogger = LoggerFactory.getLogger(ROCKSDB);

  public RocksDbDataSourceImpl(String parentPath, String name, RocksDbSettings settings,
      DirectComparator comparator) {
    this.dataBaseName = name;
    this.parentPath = parentPath;
    this.comparator = comparator;
    RocksDbSettings.setRocksDbSettings(settings);
    initDB();
  }

  public RocksDbDataSourceImpl(String parentPath, String name, RocksDbSettings settings) {
    this.dataBaseName = name;
    this.parentPath = parentPath;
    RocksDbSettings.setRocksDbSettings(settings);
    initDB();
  }

  public RocksDbDataSourceImpl(String parentPath, String name) {
    this.parentPath = parentPath;
    this.dataBaseName = name;
  }

  public Path getDbPath() {
    return Paths.get(parentPath, dataBaseName);
  }

  public RocksDB getDatabase() {
    return database;
  }

  public boolean isAlive() {
    return alive;
  }

  @Override
  public void closeDB() {
    resetDbLock.writeLock().lock();
    try {
      if (!isAlive()) {
        return;
      }
      database.close();
      alive = false;
    } catch (Exception e) {
      logger.error("Failed to find the dbStore file on the closeDB: {}.", dataBaseName, e);
    } finally {
      resetDbLock.writeLock().unlock();
    }
  }

  @Override
  public void resetDb() {
    resetDbLock.writeLock().lock();
    try {
      closeDB();
      FileUtil.recursiveDelete(getDbPath().toString());
      initDB();
    } finally {
      resetDbLock.writeLock().unlock();
    }
  }

  private boolean quitIfNotAlive() {
    if (!isAlive()) {
      logger.warn("DB {} is not alive.", dataBaseName);
    }
    return !isAlive();
  }

  @Override
  public Set<byte[]> allKeys() throws RuntimeException {
    resetDbLock.readLock().lock();
    try {
      if (quitIfNotAlive()) {
        return null;
      }
      Set<byte[]> result = Sets.newHashSet();
      try (final RocksIterator iter = getRocksIterator()) {
        for (iter.seekToFirst(); iter.isValid(); iter.next()) {
          result.add(iter.key());
        }
        return result;
      }
    } finally {
      resetDbLock.readLock().unlock();
    }
  }

  @Override
  public Set<byte[]> allValues() throws RuntimeException {
    return null;
  }

  @Override
  public long getTotal() throws RuntimeException {
    return 0;
  }

  @Override
  public String getDBName() {
    return this.dataBaseName;
  }

  @Override
  public void setDBName(String name) {
  }

  public boolean checkOrInitEngine() {
    String dir = getDbPath().toString();
    String enginePath = dir + File.separator + "engine.properties";

    if (FileUtil.createDirIfNotExists(dir)) {
      if (!FileUtil.createFileIfNotExists(enginePath)) {
        return false;
      }
    } else {
      return false;
    }

    // for the first init engine
    String engine = PropUtil.readProperty(enginePath, KEY_ENGINE);
    if (engine.isEmpty() && !PropUtil.writeProperty(enginePath, KEY_ENGINE, ROCKSDB)) {
      return false;
    }
    engine = PropUtil.readProperty(enginePath, KEY_ENGINE);

    return ROCKSDB.equals(engine);
  }

  public void initDB() {
    if (!checkOrInitEngine()) {
      throw new RuntimeException(
          String.format("failed to check database: %s, engine do not match", dataBaseName));
    }
    initDB(RocksDbSettings.getSettings());
  }

  public void initDB(RocksDbSettings settings) {
    resetDbLock.writeLock().lock();
    try {
      if (isAlive()) {
        return;
      }
      if (dataBaseName == null) {
        throw new IllegalArgumentException("No name set to the dbStore");
      }

      try (Options options = new Options()) {

        // most of these options are suggested by https://github.com/facebook/rocksdb/wiki/Set-Up-Options

        // general options
        if (settings.isEnableStatistics()) {
          options.setStatistics(new Statistics());
          options.setStatsDumpPeriodSec(60);
        }
        options.setCreateIfMissing(true);
        options.setIncreaseParallelism(1);
        options.setLevelCompactionDynamicLevelBytes(true);
        options.setMaxOpenFiles(settings.getMaxOpenFiles());

        // general options supported user config
        options.setNumLevels(settings.getLevelNumber());
        options.setMaxBytesForLevelMultiplier(settings.getMaxBytesForLevelMultiplier());
        options.setMaxBytesForLevelBase(settings.getMaxBytesForLevelBase());
        options.setMaxBackgroundCompactions(settings.getCompactThreads());
        options.setLevel0FileNumCompactionTrigger(settings.getLevel0FileNumCompactionTrigger());
        options.setTargetFileSizeMultiplier(settings.getTargetFileSizeMultiplier());
        options.setTargetFileSizeBase(settings.getTargetFileSizeBase());
        if (comparator != null) {
          options.setComparator(comparator);
        }
        options.setLogger(new Logger(options) {
          @Override
          protected void log(InfoLogLevel infoLogLevel, String logMsg) {
            rocksDbLogger.info("{} {}", dataBaseName, logMsg);
          }
        });

        // table options
        final BlockBasedTableConfig tableCfg;
        options.setTableFormatConfig(tableCfg = new BlockBasedTableConfig());
        tableCfg.setBlockSize(settings.getBlockSize());
        tableCfg.setBlockCache(RocksDbSettings.getCache());
        tableCfg.setCacheIndexAndFilterBlocks(true);
        tableCfg.setPinL0FilterAndIndexBlocksInCache(true);
        tableCfg.setFilter(new BloomFilter(10, false));

        // read options
        readOpts = new ReadOptions();
        readOpts = readOpts.setPrefixSameAsStart(true)
            .setVerifyChecksums(false);

        try {
          logger.debug("Opening database {}.", dataBaseName);
          final Path dbPath = getDbPath();

          if (!Files.isSymbolicLink(dbPath.getParent())) {
            Files.createDirectories(dbPath.getParent());
          }

          try {
            database = RocksDB.open(options, dbPath.toString());
          } catch (RocksDBException e) {
            throw new RuntimeException(
                String.format("failed to open database: %s", dataBaseName), e);
          }

          alive = true;
        } catch (IOException ioe) {
          throw new RuntimeException(
          String.format("failed to init database: %s", dataBaseName), ioe);
        }

        logger.debug("Init DB {} done.", dataBaseName);
      }
    } finally {
      resetDbLock.writeLock().unlock();
    }
  }

  @Override
  public void putData(byte[] key, byte[] value) {
    resetDbLock.readLock().lock();
    try {
      if (quitIfNotAlive()) {
        return;
      }
      database.put(key, value);
    } catch (RocksDBException e) {
      throw new RuntimeException(dataBaseName, e);
    } finally {
      resetDbLock.readLock().unlock();
    }
  }

  @Override
  public byte[] getData(byte[] key) {
    resetDbLock.readLock().lock();
    try {
      if (quitIfNotAlive()) {
        return null;
      }
      return database.get(key);
    } catch (RocksDBException e) {
      throw new RuntimeException(dataBaseName, e);
    } finally {
      resetDbLock.readLock().unlock();
    }
  }

  @Override
  public void deleteData(byte[] key) {
    resetDbLock.readLock().lock();
    try {
      if (quitIfNotAlive()) {
        return;
      }
      database.delete(key);
    } catch (RocksDBException e) {
      throw new RuntimeException(dataBaseName, e);
    } finally {
      resetDbLock.readLock().unlock();
    }
  }

  @Override
  public boolean flush() {
    return false;
  }

  @Override
  public org.tron.core.db.common.iterator.DBIterator iterator() {
    return new RockStoreIterator(getRocksIterator());
  }

  private void updateByBatchInner(Map<byte[], byte[]> rows) throws Exception {
    if (quitIfNotAlive()) {
      return;
    }
    try (WriteBatch batch = new WriteBatch()) {
      for (Map.Entry<byte[], byte[]> entry : rows.entrySet()) {
        if (entry.getValue() == null) {
          batch.delete(entry.getKey());
        } else {
          batch.put(entry.getKey(), entry.getValue());
        }
      }
      database.write(new WriteOptions(), batch);
    }
  }

  private void updateByBatchInner(Map<byte[], byte[]> rows, WriteOptions options)
      throws Exception {
    if (quitIfNotAlive()) {
      return;
    }
    try (WriteBatch batch = new WriteBatch()) {
      for (Map.Entry<byte[], byte[]> entry : rows.entrySet()) {
        if (entry.getValue() == null) {
          batch.delete(entry.getKey());
        } else {
          batch.put(entry.getKey(), entry.getValue());
        }
      }
      database.write(options, batch);
    }
  }

  @Override
  public void updateByBatch(Map<byte[], byte[]> rows, WriteOptionsWrapper optionsWrapper) {
    resetDbLock.readLock().lock();
    try {
      if (quitIfNotAlive()) {
        return;
      }
      updateByBatchInner(rows, optionsWrapper.rocks);
    } catch (Exception e) {
      try {
        updateByBatchInner(rows);
      } catch (Exception e1) {
        throw new RuntimeException(dataBaseName, e1);
      }
    } finally {
      resetDbLock.readLock().unlock();
    }
  }

  @Override
  public void updateByBatch(Map<byte[], byte[]> rows) {
    resetDbLock.readLock().lock();
    try {
      if (quitIfNotAlive()) {
        return;
      }
      updateByBatchInner(rows);
    } catch (Exception e) {
      try {
        updateByBatchInner(rows);
      } catch (Exception e1) {
        throw new RuntimeException(dataBaseName, e1);
      }
    } finally {
      resetDbLock.readLock().unlock();
    }
  }

  public List<byte[]> getKeysNext(byte[] key, long limit) {
    resetDbLock.readLock().lock();
    try {
      if (quitIfNotAlive()) {
        return new ArrayList<>();
      }
      if (limit <= 0) {
        return new ArrayList<>();
      }

      try (RocksIterator iter = getRocksIterator()) {
        List<byte[]> result = new ArrayList<>();
        long i = 0;
        for (iter.seek(key); iter.isValid() && i < limit; iter.next(), i++) {
          result.add(iter.key());
        }
        return result;
      }
    } finally {
      resetDbLock.readLock().unlock();
    }
  }

  public Map<byte[], byte[]> getNext(byte[] key, long limit) {
    resetDbLock.readLock().lock();
    try {
      if (quitIfNotAlive()) {
        return null;
      }
      if (limit <= 0) {
        return Collections.emptyMap();
      }
      try (RocksIterator iter = getRocksIterator()) {
        Map<byte[], byte[]> result = new HashMap<>();
        long i = 0;
        for (iter.seek(key); iter.isValid() && i < limit; iter.next(), i++) {
          result.put(iter.key(), iter.value());
        }
        return result;
      }
    } finally {
      resetDbLock.readLock().unlock();
    }
  }

  @Override
  public Map<WrappedByteArray, byte[]> prefixQuery(byte[] key) {
    resetDbLock.readLock().lock();
    try {
      if (quitIfNotAlive()) {
        return null;
      }
      try (RocksIterator iterator = getRocksIterator()) {
        Map<WrappedByteArray, byte[]> result = new HashMap<>();
        for (iterator.seek(key); iterator.isValid(); iterator.next()) {
          if (Bytes.indexOf(iterator.key(), key) == 0) {
            result.put(WrappedByteArray.of(iterator.key()), iterator.value());
          } else {
            return result;
          }
        }
        return result;
      }
    } finally {
      resetDbLock.readLock().unlock();
    }
  }

  public Set<byte[]> getlatestValues(long limit) {
    resetDbLock.readLock().lock();
    try {
      if (quitIfNotAlive()) {
        return null;
      }
      if (limit <= 0) {
        return Sets.newHashSet();
      }
      try (RocksIterator iter = getRocksIterator()) {
        Set<byte[]> result = Sets.newHashSet();
        long i = 0;
        for (iter.seekToLast(); iter.isValid() && i < limit; iter.prev(), i++) {
          result.add(iter.value());
        }
        return result;
      }
    } finally {
      resetDbLock.readLock().unlock();
    }
  }


  public Set<byte[]> getValuesNext(byte[] key, long limit) {
    resetDbLock.readLock().lock();
    try {
      if (quitIfNotAlive()) {
        return null;
      }
      if (limit <= 0) {
        return Sets.newHashSet();
      }
      try (RocksIterator iter = getRocksIterator()) {
        Set<byte[]> result = Sets.newHashSet();
        long i = 0;
        for (iter.seek(key); iter.isValid() && i < limit; iter.next(), i++) {
          result.add(iter.value());
        }
        return result;
      }
    } finally {
      resetDbLock.readLock().unlock();
    }
  }

  public void backup(String dir) throws RocksDBException {
    Checkpoint cp = Checkpoint.create(database);
    cp.createCheckpoint(dir + this.getDBName());
  }

  private RocksIterator getRocksIterator() {
    try ( ReadOptions readOptions = new ReadOptions().setFillCache(false)) {
      return  database.newIterator(readOptions);
    }
  }

  public boolean deleteDbBakPath(String dir) {
    return FileUtil.deleteDir(new File(dir + this.getDBName()));
  }

  @Override
  public RocksDbDataSourceImpl newInstance() {
    return new RocksDbDataSourceImpl(parentPath, dataBaseName, RocksDbSettings.getSettings());
  }



  /**
   * Level Files Size(MB)
   * --------------------
   *   0        5       10
   *   1      134      254
   *   2     1311     2559
   *   3     1976     4005
   *   4        0        0
   *   5        0        0
   *   6        0        0
   */
  @Override
  public List<String> getStats() throws Exception {
    resetDbLock.readLock().lock();
    try {
      if (!isAlive()) {
        return Collections.emptyList();
      }
      String stat = database.getProperty("rocksdb.levelstats");
      String[] stats = stat.split("\n");
      return Arrays.stream(stats).skip(2).collect(Collectors.toList());
    } finally {
      resetDbLock.readLock().unlock();
    }
  }

  @Override
  public String getEngine() {
    return ROCKSDB;
  }

  @Override
  public String getName() {
    return this.dataBaseName;
  }

  @Override public void stat() {
    this.statProperty();
  }
}
