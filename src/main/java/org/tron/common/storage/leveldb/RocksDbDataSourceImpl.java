package org.tron.common.storage.leveldb;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.rocksdb.*;
import org.tron.common.storage.DbSourceInter;
import org.tron.common.storage.RocksDbSettings;
import org.tron.common.storage.WriteOptionsWrapper;
import org.tron.common.utils.ByteUtil;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.PropUtil;
import org.tron.core.db.common.iterator.RockStoreIterator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Slf4j
@NoArgsConstructor
public class RocksDbDataSourceImpl implements DbSourceInter<byte[]>,
    Iterable<Map.Entry<byte[], byte[]>> {
  private static final String ENGINE = "ENGINE";

  private String dataBaseName;
  private RocksDB database;
  private boolean alive;
  private String parentName;
  private ReadOptions readOpts;

  private ReadWriteLock resetDbLock = new ReentrantReadWriteLock();

  public RocksDbDataSourceImpl(String parentName, String name) {
    this.dataBaseName = name;
    this.parentName = parentName;
  }

  public Path getDbPath() {
    return Paths.get(parentName, dataBaseName);
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
    } finally {
      resetDbLock.writeLock().unlock();
    }
  }

  @Override
  public void resetDb() {
    closeDB();
    FileUtil.recursiveDelete(getDbPath().toString());
    initDB();
  }

  private boolean quitIfNotAlive() {
    if (!isAlive()) {
      logger.warn("db is not alive");
    }
    return !isAlive();
  }

  @Override
  public Set<byte[]> allKeys() throws RuntimeException {
    if (quitIfNotAlive()) {
      return null;
    }
    resetDbLock.readLock().lock();
    Set<byte[]> result = Sets.newHashSet();
    try (final RocksIterator iter = database.newIterator()) {
      for (iter.seekToFirst(); iter.isValid(); iter.next()) {
        result.add(iter.key());
      }
      return result;
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
    if (quitIfNotAlive()) {
      return 0;
    }
    resetDbLock.readLock().lock();
    try (RocksIterator iterator = database.newIterator()) {
      long total = 0;
      for (iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
        total++;
      }
      return total;
    } finally {
      resetDbLock.readLock().unlock();
    }
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
    String engine = PropUtil.readProperty(enginePath, ENGINE);
    if (StringUtils.isEmpty(engine) && !PropUtil.writeProperty(enginePath, ENGINE, "ROCKSDB")) {
      return false;
    }
    engine = PropUtil.readProperty(enginePath, ENGINE);
    return "ROCKSDB".equals(engine);
  }

  public void initDB() {
    if (!checkOrInitEngine()) {
      logger.error("database engine do not match");
      throw new RuntimeException("Failed to initialize database");
    }
    initDB(RocksDbSettings.getSettings());
  }

  public void initDB(RocksDbSettings settings) {
    resetDbLock.writeLock().lock();
    try {
      if (isAlive()) {
        return;
      }

      Preconditions.checkNotNull(dataBaseName, "no name set to the dbStore");

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

        // table options
        final BlockBasedTableConfig tableCfg;
        options.setTableFormatConfig(tableCfg = new BlockBasedTableConfig());
        tableCfg.setBlockSize(settings.getBlockSize());
        tableCfg.setBlockCacheSize(32 * 1024 * 1024);
        tableCfg.setCacheIndexAndFilterBlocks(true);
        tableCfg.setPinL0FilterAndIndexBlocksInCache(true);
        tableCfg.setFilter(new BloomFilter(10, false));

        // read options
        readOpts = new ReadOptions();
        readOpts = readOpts.setPrefixSameAsStart(true)
            .setVerifyChecksums(false);

        try {
          logger.debug("Opening database");
          final Path dbPath = getDbPath();
          if (!Files.isSymbolicLink(dbPath.getParent())) {
            Files.createDirectories(dbPath.getParent());
          }

          try {
            database = RocksDB.open(options, dbPath.toString());
          } catch (RocksDBException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException("Failed to initialize database", e);
          }

          alive = true;

        } catch (IOException ioe) {
          logger.error(ioe.getMessage(), ioe);
          throw new RuntimeException("Failed to initialize database", ioe);
        }

        logger.debug("<~ RocksDbDataSource.initDB(): " + dataBaseName);
      }
    } finally {
      resetDbLock.writeLock().unlock();
    }
  }

  @Override
  public void putData(byte[] key, byte[] value) {
    if (quitIfNotAlive()) {
      return;
    }
    resetDbLock.readLock().lock();
    try {
      database.put(key, value);
    } catch (RocksDBException e) {
      logger.error(e.getMessage(), e);
    } finally {
      resetDbLock.readLock().unlock();
    }
  }

  @Override
  public void putData(byte[] key, byte[] value, WriteOptionsWrapper optionsWrapper) {
    if (quitIfNotAlive()) {
      return;
    }
    resetDbLock.readLock().lock();
    try {
      database.put(optionsWrapper.getRocks(), key, value);
    } catch (RocksDBException e) {
      logger.error(e.getMessage(), e);
    } finally {
      resetDbLock.readLock().unlock();
    }
  }

  @Override
  public byte[] getData(byte[] key) {
    if (quitIfNotAlive()) {
      return null;
    }
    resetDbLock.readLock().lock();
    try {
      return database.get(key);
    } catch (RocksDBException e) {
      logger.error(e.getMessage(), e);
    } finally {
      resetDbLock.readLock().unlock();
    }
    return null;
  }

  @Override
  public void deleteData(byte[] key) {
    if (quitIfNotAlive()) {
      return;
    }
    resetDbLock.readLock().lock();
    try {
      database.delete(key);
    } catch (RocksDBException e) {
      logger.error(e.getMessage(), e);
    } finally {
      resetDbLock.readLock().unlock();
    }
  }

  @Override
  public void deleteData(byte[] key, WriteOptionsWrapper optionsWrapper) {
    if (quitIfNotAlive()) {
      return;
    }
    resetDbLock.readLock().lock();
    try {
      database.delete(optionsWrapper.getRocks(), key);
    } catch (RocksDBException e) {
      logger.error(e.getMessage(), e);
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
    return new RockStoreIterator(database.newIterator());
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
      database.write(new WriteOptions(), batch);
    }
  }

  @Override
  public void updateByBatch(Map<byte[], byte[]> rows) {
    if (quitIfNotAlive()) {
      return;
    }
    resetDbLock.readLock().lock();
    try {
      updateByBatchInner(rows);
    } catch (Exception e) {
      try {
        updateByBatchInner(rows);
      } catch (Exception e1) {
        throw new RuntimeException(e);
      }
    } finally {
      resetDbLock.readLock().unlock();
    }
  }

  @Override
  public void updateByBatch(Map<byte[], byte[]> rows, WriteOptionsWrapper optionsWrapper) {
    if (quitIfNotAlive()) {
      return;
    }
    resetDbLock.readLock().lock();
    try {
      updateByBatchInner(rows, optionsWrapper.getRocks());
    } catch (Exception e) {
      try {
        updateByBatchInner(rows);
      } catch (Exception e1) {
        throw new RuntimeException(e);
      }
    } finally {
      resetDbLock.readLock().unlock();
    }
  }

  public Map<byte[], byte[]> getNext(byte[] key, long limit) {
    if (quitIfNotAlive()) {
      return null;
    }
    if (limit <= 0) {
      return Collections.emptyMap();
    }
    resetDbLock.readLock().lock();
    try (RocksIterator iter = database.newIterator()) {
      Map<byte[], byte[]> result = new HashMap<>();
      long i = 0;
      for (iter.seek(key); iter.isValid() && i < limit; iter.next(), i++) {
        result.put(iter.key(), iter.value());
      }
      return result;
    } finally {
      resetDbLock.readLock().unlock();
    }
  }

  public Set<byte[]> getlatestValues(long limit) {
    if (quitIfNotAlive()) {
      return null;
    }
    if (limit <= 0) {
      return Sets.newHashSet();
    }
    resetDbLock.readLock().lock();
    try (RocksIterator iter = database.newIterator()) {
      Set<byte[]> result = Sets.newHashSet();
      long i = 0;
      for (iter.seekToLast(); iter.isValid() && i < limit; iter.prev(), i++) {
        result.add(iter.value());
      }
      return result;
    } finally {
      resetDbLock.readLock().unlock();
    }
  }

  public Set<byte[]> getValuesPrev(byte[] key, long limit) {
    if (quitIfNotAlive()) {
      return null;
    }
    if (limit <= 0) {
      return Sets.newHashSet();
    }
    resetDbLock.readLock().lock();
    try (RocksIterator iter = database.newIterator()) {
      Set<byte[]> result = Sets.newHashSet();
      long i = 0;
      byte[] data = getData(key);
      if (Objects.nonNull(data)) {
        result.add(data);
        i++;
      }
      for (iter.seekForPrev(key); iter.isValid() && i < limit; iter.prev(), i++) {
        result.add(iter.value());
      }
      return result;
    } finally {
      resetDbLock.readLock().unlock();
    }
  }

  public Set<byte[]> getValuesNext(byte[] key, long limit) {
    if (quitIfNotAlive()) {
      return null;
    }
    if (limit <= 0) {
      return Sets.newHashSet();
    }
    resetDbLock.readLock().lock();
    try (RocksIterator iter = database.newIterator()) {
      Set<byte[]> result = Sets.newHashSet();
      long i = 0;
      for (iter.seek(key); iter.isValid() && i < limit; iter.next(), i++) {
        result.add(iter.value());
      }
      return result;
    } finally {
      resetDbLock.readLock().unlock();
    }
  }

  public Map<byte[], byte[]> getPrevious(byte[] key, long limit, int precision) {
    if (quitIfNotAlive()) {
      return null;
    }
    if (limit <= 0 || key.length < precision) {
      return Collections.emptyMap();
    }
    resetDbLock.readLock().lock();
    try (RocksIterator iterator = database.newIterator()) {
      Map<byte[], byte[]> result = new HashMap<>();
      long i = 0;
      for (iterator.seekToFirst(); iterator.isValid() && i++ < limit; iterator.next()) {

        if (iterator.key().length >= precision) {
          if (ByteUtil.less(ByteUtil.parseBytes(key, 0, precision),
              ByteUtil.parseBytes(iterator.key(), 0, precision))) {
            break;
          }
          result.put(iterator.key(), iterator.value());
        }
      }
      return result;
    } finally {
      resetDbLock.readLock().unlock();
    }
  }

  public void backup(String dir) throws RocksDBException {
    Checkpoint cp = Checkpoint.create(database);
    cp.createCheckpoint(dir + this.getDBName());
  }

  public boolean deleteDbBakPath(String dir) {
    return FileUtil.deleteDir(new File(dir + this.getDBName()));
  }
}