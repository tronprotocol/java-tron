package org.tron.utils.db;

import static org.fusesource.leveldbjni.JniDBFactory.factory;

import com.google.common.collect.Maps;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.rocksdb.BlockBasedTableConfig;
import org.rocksdb.BloomFilter;
import org.rocksdb.RocksDBException;
import org.tron.common.utils.PropUtil;

@Slf4j(topic = "tool")
public class DbTool {

  private static final String KEY_ENGINE = "ENGINE";
  private static final String ENGINE_FILE = "engine.properties";
  private static final String FILE_SEPARATOR = File.separator;
  private static final String ROCKSDB = "ROCKSDB";

  private static Map<String, DBInterface> dbMap = Maps.newHashMap();

  public enum DbType {
    LevelDB("leveldb"),
    RocksDB("rocksdb");

    private final String value;

    DbType(String value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return this.value;
    }
  }

  /**
   * Get the DB object according to the specified path,
   * create db object when not exists, otherwise get it from the dbMap.
   *
   * @param sourceDir the parent path of db
   * @param dbName db dir name
   *
   * @return db object
   *
   * @throws IOException IOException
   * @throws RocksDBException RocksDBException
   */
  public static DBInterface getDB(String sourceDir, String dbName)
          throws IOException, RocksDBException {
    Path path = Paths.get(sourceDir, dbName);
    if (dbMap.containsKey(path.toString())) {
      return dbMap.get(path.toString());
    }
    DbType type = getDbType(sourceDir, dbName);
    DBInterface db;
    switch (type) {
      case LevelDB:
        db = openLevelDb(path);
        dbMap.put(path.toString(), db);
        break;
      case RocksDB:
        db = openRocksDb(path);
        dbMap.put(path.toString(), db);
        break;
      default:
        throw new IllegalStateException("Unexpected value: " + type);
    }
    return db;
  }

  public static DBInterface getDB(String dir)
      throws IOException, RocksDBException {
    Path path = Paths.get(dir);
    String parentDir = path.getParent().toString();
    String dbName = path.getFileName().toString();
    return getDB(parentDir, dbName);
  }


  /**
   * Close db.
   * @param sourceDir db parentPath
   * @param dbName db dirname
   * @throws IOException IOException
   */
  public static void closeDB(String sourceDir, String dbName)
          throws IOException {
    Path path = Paths.get(sourceDir, dbName);
    DBInterface db = dbMap.get(path.toString());
    if (db != null) {
      try {
        dbMap.remove(path.toString());
        db.close();
      } catch (IOException e) {
        logger.error("close db {} error: {}", path, e);
        throw e;
      }
    }
  }

  /**
   * Close all dbs.
   */
  public static void close() {
    Iterator<Map.Entry<String, DBInterface>> iterator = dbMap.entrySet().iterator();
    while (iterator.hasNext()) {
      Map.Entry<String, DBInterface> next = iterator.next();
      try {
        next.getValue().close();
      } catch (IOException e) {
        logger.error("close db failed, db: {}", next.getKey(), e);
      }
      iterator.remove();
    }
  }

  public static DbType getDbType(String sourceDir, String dbName) {
    String path = Paths.get(sourceDir, dbName).toString();
    return getDbType(path);
  }

  public static DbType getDbType(String path) {
    String engineFile = String.format("%s%s%s", path, FILE_SEPARATOR, ENGINE_FILE);
    if (!new File(engineFile).exists()) {
      return DbType.LevelDB;
    }
    String engine = PropUtil.readProperty(engineFile, KEY_ENGINE);
    if (engine.equalsIgnoreCase(ROCKSDB)) {
      return DbType.RocksDB;
    } else {
      return DbType.LevelDB;
    }
  }

  public static LevelDBImpl openLevelDb(Path db) throws IOException {
    DB database;
    Options options = getLevelDbOptions();
    try {
      database = factory.open(db.toFile(), options);
    } catch (IOException e) {
      if (e.getMessage().contains("Corruption:")) {
        factory.repair(db.toFile(), options);
        database = factory.open(db.toFile(), options);
      } else {
        throw e;
      }
    }
    return new LevelDBImpl(database);
  }

  public static RocksDBImpl openRocksDb(Path db) throws RocksDBException {
    org.rocksdb.RocksDB database;
    try (org.rocksdb.Options options = newDefaultRocksDbOptions()) {
      database = org.rocksdb.RocksDB.open(options, db.toString());
    } catch (Exception e) {
      throw e;
    }
    return new RocksDBImpl(database);
  }

  private static org.rocksdb.Options newDefaultRocksDbOptions() {
    org.rocksdb.Options options = new org.rocksdb.Options();

    options.setCreateIfMissing(true);
    options.setIncreaseParallelism(1);
    options.setNumLevels(7);
    options.setMaxOpenFiles(-1);
    options.setTargetFileSizeBase(64 * 1024 * 1024);
    options.setTargetFileSizeMultiplier(1);
    options.setMaxBytesForLevelBase(512 * 1024 * 1024);
    options.setMaxBackgroundCompactions(Math.max(1, Runtime.getRuntime().availableProcessors()));
    options.setLevel0FileNumCompactionTrigger(4);
    options.setLevelCompactionDynamicLevelBytes(true);

    BlockBasedTableConfig tableCfg = new BlockBasedTableConfig();
    tableCfg.setBlockSize(64 * 1024);
    tableCfg.setBlockCacheSize(32 * 1024 * 1024);
    tableCfg.setCacheIndexAndFilterBlocks(true);
    tableCfg.setPinL0FilterAndIndexBlocksInCache(true);
    tableCfg.setFilter(new BloomFilter(10, false));

    options.setTableFormatConfig(tableCfg);
    return options;
  }

  private static Options getLevelDbOptions() {
    CompressionType defaultCompressionType = CompressionType.SNAPPY;
    int defaultBlockSize = 4 * 1024;
    int defaultWriteBufferSize = 10 * 1024 * 1024;
    long defaultCacheSize = 10 * 1024 * 1024L;
    int defaultMaxOpenFiles = 100;

    Options dbOptions = new Options();

    dbOptions.createIfMissing(true);
    dbOptions.paranoidChecks(true);
    dbOptions.verifyChecksums(true);

    dbOptions.compressionType(defaultCompressionType);
    dbOptions.blockSize(defaultBlockSize);
    dbOptions.writeBufferSize(defaultWriteBufferSize);
    dbOptions.cacheSize(defaultCacheSize);
    dbOptions.maxOpenFiles(defaultMaxOpenFiles);

    return dbOptions;
  }

  public static boolean isLevelOrRocksDB(String path) {
    List<File> files = Arrays.stream(Objects.requireNonNull(new File(path).listFiles()))
        .collect(Collectors.toList());
    for (File f: files) {
      if (f.getName().contains("MANIFEST")) {
        return true;
      }
    }
    return false;
  }
}
