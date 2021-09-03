package org.tron.tool.litefullnode;

import static org.fusesource.leveldbjni.JniDBFactory.factory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.extern.slf4j.Slf4j;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.rocksdb.BlockBasedTableConfig;
import org.rocksdb.BloomFilter;
import org.rocksdb.ComparatorOptions;
import org.rocksdb.RocksDBException;
import org.tron.common.utils.MarketOrderPriceComparatorForLevelDB;
import org.tron.common.utils.MarketOrderPriceComparatorForRockDB;
import org.tron.common.utils.PropUtil;
import org.tron.tool.litefullnode.db.LevelDBImpl;
import org.tron.tool.litefullnode.db.RocksDBImpl;
import org.tron.tool.litefullnode.db.TronDB;

@Slf4j(topic = "tool")
public class DbTool {

  private static final String KEY_ENGINE = "ENGINE";
  private static final String ENGINE_FILE = "engine.properties";
  private static final String FILE_SEPARATOR = File.separator;
  private static final String ROCKSDB = "ROCKSDB";

  enum DbType {
    LevelDB,
    RocksDB
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
  public static TronDB getDB(String sourceDir, String dbName)
          throws IOException, RocksDBException {
    Path path = Paths.get(sourceDir, dbName);
    if (TronDB.containsDB(path.toString())) {
      return TronDB.getDB(path.toString());
    }
    DbType type = getDbType(sourceDir, dbName);
    TronDB db;
    switch (type) {
      case LevelDB:
        db = openLevelDb(sourceDir, dbName);
        break;
      case RocksDB:
        db = openRocksDb(sourceDir, dbName);
        break;
      default:
        throw new IllegalStateException("Unexpected value: " + type);
    }
    return db;
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
    TronDB db = TronDB.removeDB(path.toString());
    if (db != null) {
      try {
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
    TronDB.closeAll();
  }

  private static DbType getDbType(String sourceDir, String dbName) {
    String engineFile = String.format("%s%s%s%s%s", sourceDir, FILE_SEPARATOR,
            dbName, FILE_SEPARATOR, ENGINE_FILE);
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

  private static LevelDBImpl openLevelDb(String sourceDir, String name) throws IOException {
    DB database;
    Options options = getLevelDbOptions();
    Path path = Paths.get(sourceDir, name);
    try {
      if ("market_pair_price_to_order".equalsIgnoreCase(name)) {
        options.comparator(new MarketOrderPriceComparatorForLevelDB());
      }
      database = factory.open(path.toFile(), options);
    } catch (IOException e) {
      if (e.getMessage().contains("Corruption:")) {
        factory.repair(path.toFile(), options);
        database = factory.open(path.toFile(), options);
      } else {
        throw e;
      }
    }
    return new LevelDBImpl(sourceDir, database, name);
  }

  private static RocksDBImpl openRocksDb(String sourceDir, String name) throws RocksDBException {
    org.rocksdb.RocksDB database;
    Path path = Paths.get(sourceDir, name);
    try (org.rocksdb.Options options = newDefaultRocksDbOptions()) {
      if ("market_pair_price_to_order".equalsIgnoreCase(name)) {
        options.setComparator(new MarketOrderPriceComparatorForRockDB(new ComparatorOptions()));
      }
      database = org.rocksdb.RocksDB.open(options, path.toString());
    } catch (Exception e) {
      throw e;
    }
    return new RocksDBImpl(sourceDir, database, name);
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
    int defaultWriteBufferSize = 64 * 1024 * 1024;
    long defaultCacheSize = 32 * 1024 * 1024L;
    int defaultMaxOpenFiles = 5000;

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
}
