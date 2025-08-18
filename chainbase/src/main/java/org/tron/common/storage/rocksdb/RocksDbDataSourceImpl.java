package org.tron.common.storage.rocksdb;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
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
import org.rocksdb.Checkpoint;
import org.rocksdb.InfoLogLevel;
import org.rocksdb.Logger;
import org.rocksdb.Options;
import org.rocksdb.ReadOptions;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.rocksdb.Status;
import org.rocksdb.WriteBatch;
import org.rocksdb.WriteOptions;
import org.slf4j.LoggerFactory;
import org.tron.common.error.TronDBException;
import org.tron.common.setting.RocksDbSettings;
import org.tron.common.storage.WriteOptionsWrapper;
import org.tron.common.storage.metric.DbStat;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.PropUtil;
import org.tron.core.db.common.DbSourceInter;
import org.tron.core.db.common.iterator.RockStoreIterator;
import org.tron.core.db2.common.Instance;
import org.tron.core.db2.common.WrappedByteArray;
import org.tron.core.exception.TronError;


@Slf4j(topic = "DB")
@NoArgsConstructor
public class RocksDbDataSourceImpl extends DbStat implements DbSourceInter<byte[]>,
    Iterable<Map.Entry<byte[], byte[]>>, Instance<RocksDbDataSourceImpl> {

  private String dataBaseName;
  private RocksDB database;
  private Options options;
  private volatile boolean alive;
  private String parentPath;
  private ReadWriteLock resetDbLock = new ReentrantReadWriteLock();
  private static final String KEY_ENGINE = "ENGINE";
  private static final String ROCKSDB = "ROCKSDB";
  private static final org.slf4j.Logger rocksDbLogger = LoggerFactory.getLogger(ROCKSDB);

  public RocksDbDataSourceImpl(String parentPath, String name, Options options) {
    this.dataBaseName = name;
    this.parentPath = parentPath;
    this.options = options;
    initDB();
  }

  @VisibleForTesting
  public RocksDbDataSourceImpl(String parentPath, String name) {
    this.parentPath = parentPath;
    this.dataBaseName = name;
    this.options = RocksDbSettings.getOptionsByDbName(name);
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

  private void throwIfNotAlive() {
    if (!isAlive()) {
      throw new TronDBException("DB " + this.getDBName() + " is closed.");
    }
  }

  /** copy from {@link org.fusesource.leveldbjni.internal#checkArgNotNull} */
  private static void checkArgNotNull(Object value, String name) {
    if (value == null) {
      throw new IllegalArgumentException("The " + name + " argument cannot be null");
    }
  }

  @Deprecated
  @VisibleForTesting
  @Override
  public Set<byte[]> allKeys() throws RuntimeException {
    resetDbLock.readLock().lock();
    try (final RocksIterator iter = getRocksIterator()) {
      Set<byte[]> result = Sets.newHashSet();
      for (iter.seekToFirst(); iter.isValid(); iter.next()) {
        result.add(iter.key());
      }
      return result;
    } finally {
      resetDbLock.readLock().unlock();
    }
  }

  @Deprecated
  @VisibleForTesting
  @Override
  public Set<byte[]> allValues() throws RuntimeException {
    resetDbLock.readLock().lock();
    try (final RocksIterator iter = getRocksIterator()) {
      Set<byte[]> result = Sets.newHashSet();
      for (iter.seekToFirst(); iter.isValid(); iter.next()) {
        result.add(iter.value());
      }
      return result;
    } finally {
      resetDbLock.readLock().unlock();
    }
  }

  @Deprecated
  @VisibleForTesting
  @Override
  public long getTotal() throws RuntimeException {
    resetDbLock.readLock().lock();
    try (final RocksIterator iter = getRocksIterator()) {
      long total = 0;
      for (iter.seekToFirst(); iter.isValid(); iter.next()) {
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
    this.dataBaseName = name;
  }

  public boolean checkOrInitEngine() {
    String dir = getDbPath().toString();
    String enginePath = dir + File.separator + "engine.properties";
    File currentFile = new File(dir, "CURRENT");
    if (currentFile.exists() && !Paths.get(enginePath).toFile().exists()) {
      // if the CURRENT file exists, but the engine.properties file does not exist, it is LevelDB
      logger.error(" You are trying to open a LevelDB database with RocksDB engine.");
      return false;
    }
    if (FileUtil.createDirIfNotExists(dir)) {
      if (!FileUtil.createFileIfNotExists(enginePath)) {
        return false;
      }
    } else {
      return false;
    }

    // for the first init engine
    String engine = PropUtil.readProperty(enginePath, KEY_ENGINE);
    if (Strings.isNullOrEmpty(engine) && !PropUtil.writeProperty(enginePath, KEY_ENGINE, ROCKSDB)) {
      return false;
    }
    engine = PropUtil.readProperty(enginePath, KEY_ENGINE);

    return ROCKSDB.equals(engine);
  }

  public void initDB() {
    if (!checkOrInitEngine()) {
      throw new RuntimeException(String.format(
          "Cannot open LevelDB database '%s' with RocksDB engine."
              + " Set db.engine=LEVELDB or use RocksDB database. ", dataBaseName));
    }
    resetDbLock.writeLock().lock();
    try {
      if (isAlive()) {
        return;
      }
      if (dataBaseName == null) {
        throw new IllegalArgumentException("No name set to the dbStore");
      }
      options.setLogger(new Logger(options) {
        @Override
        protected void log(InfoLogLevel infoLogLevel, String logMsg) {
          rocksDbLogger.info("{} {}", dataBaseName, logMsg);
        }
      });

      try {
        logger.debug("Opening database {}.", dataBaseName);
        final Path dbPath = getDbPath();

        if (!Files.isSymbolicLink(dbPath.getParent())) {
          Files.createDirectories(dbPath.getParent());
        }

        try {
          database = RocksDB.open(options, dbPath.toString());
        } catch (RocksDBException e) {
          if (Objects.equals(e.getStatus().getCode(), Status.Code.Corruption)) {
            logger.error("Database {} corrupted, please delete database directory({}) "
                + "and restart.", dataBaseName, parentPath, e);
          } else {
            logger.error("Open Database {} failed", dataBaseName, e);
          }
          throw new TronError(e, TronError.ErrCode.ROCKSDB_INIT);
        }

        alive = true;
      } catch (IOException ioe) {
        throw new RuntimeException(
            String.format("failed to init database: %s", dataBaseName), ioe);
      }

      logger.debug("Init DB {} done.", dataBaseName);
    } finally {
      resetDbLock.writeLock().unlock();
    }
  }

  @Override
  public void putData(byte[] key, byte[] value) {
    resetDbLock.readLock().lock();
    try {
      throwIfNotAlive();
      checkArgNotNull(key, "key");
      checkArgNotNull(value, "value");
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
      throwIfNotAlive();
      checkArgNotNull(key, "key");
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
      throwIfNotAlive();
      checkArgNotNull(key, "key");
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

  private void updateByBatchInner(Map<byte[], byte[]> rows, WriteOptions options)
      throws Exception {
    try (WriteBatch batch = new WriteBatch()) {
      for (Map.Entry<byte[], byte[]> entry : rows.entrySet()) {
        checkArgNotNull(entry.getKey(), "key");
        if (entry.getValue() == null) {
          batch.delete(entry.getKey());
        } else {
          batch.put(entry.getKey(), entry.getValue());
        }
      }
      throwIfNotAlive();
      database.write(options, batch);
    }
  }

  @Override
  public void updateByBatch(Map<byte[], byte[]> rows, WriteOptionsWrapper optionsWrapper) {
    this.updateByBatch(rows, optionsWrapper.rocks);
  }

  @Override
  public void updateByBatch(Map<byte[], byte[]> rows) {
    this.updateByBatch(rows, new WriteOptions());
  }

  private void updateByBatch(Map<byte[], byte[]> rows, WriteOptions options) {
    resetDbLock.readLock().lock();
    try {
      updateByBatchInner(rows, options);
    } catch (Exception e) {
      try {
        updateByBatchInner(rows, options);
      } catch (Exception e1) {
        throw new RuntimeException(dataBaseName, e1);
      }
    } finally {
      resetDbLock.readLock().unlock();
    }
  }

  public List<byte[]> getKeysNext(byte[] key, long limit) {
    if (limit <= 0) {
      return new ArrayList<>();
    }
    resetDbLock.readLock().lock();
    try (RocksIterator iter = getRocksIterator()) {
      List<byte[]> result = new ArrayList<>();
      long i = 0;
      for (iter.seek(key); iter.isValid() && i < limit; iter.next(), i++) {
        result.add(iter.key());
      }
      return result;
    } finally {
      resetDbLock.readLock().unlock();
    }
  }

  public Map<byte[], byte[]> getNext(byte[] key, long limit) {
    if (limit <= 0) {
      return Collections.emptyMap();
    }
    resetDbLock.readLock().lock();
    try (RocksIterator iter = getRocksIterator()) {
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

  @Override
  public Map<WrappedByteArray, byte[]> prefixQuery(byte[] key) {
    resetDbLock.readLock().lock();
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
    } finally {
      resetDbLock.readLock().unlock();
    }
  }

  public Set<byte[]> getlatestValues(long limit) {
    if (limit <= 0) {
      return Sets.newHashSet();
    }
    resetDbLock.readLock().lock();
    try (RocksIterator iter = getRocksIterator()) {
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

  public Set<byte[]> getValuesNext(byte[] key, long limit) {
    if (limit <= 0) {
      return Sets.newHashSet();
    }
    resetDbLock.readLock().lock();
    try (RocksIterator iter = getRocksIterator()) {
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

  public void backup(String dir) throws RocksDBException {
    throwIfNotAlive();
    Checkpoint cp = Checkpoint.create(database);
    cp.createCheckpoint(dir + this.getDBName());
  }

  private RocksIterator getRocksIterator() {
    try ( ReadOptions readOptions = new ReadOptions().setFillCache(false)) {
      throwIfNotAlive();
      return  database.newIterator(readOptions);
    }
  }

  public boolean deleteDbBakPath(String dir) {
    return FileUtil.deleteDir(new File(dir + this.getDBName()));
  }

  @Override
  public RocksDbDataSourceImpl newInstance() {
    return new RocksDbDataSourceImpl(parentPath, dataBaseName,
        this.options);
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
