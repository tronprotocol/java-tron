package org.tron.tool.litefullnode;

import static org.fusesource.leveldbjni.JniDBFactory.factory;

import com.google.common.collect.Maps;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.WriteOptions;
import org.rocksdb.BlockBasedTableConfig;
import org.rocksdb.BloomFilter;
import org.rocksdb.RocksDBException;
import org.tron.common.utils.PropUtil;
import org.tron.tool.litefullnode.db.DBInterface;
import org.tron.tool.litefullnode.db.LevelDBImpl;
import org.tron.tool.litefullnode.db.RocksDBImpl;

@Slf4j(topic = "tool")
public class DbTool {

  private static final String KEY_ENGINE = "ENGINE";
  private static final String ENGINE_FILE = "engine.properties";

  private static Map<String, DBInterface> dbMap = Maps.newHashMap();
  private static DbType type;

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
  public static DBInterface getDB(String sourceDir, String dbName)
          throws IOException, RocksDBException {
    Path path = Paths.get(sourceDir, dbName);
    if (dbMap.containsKey(path.toString())) {
      return dbMap.get(path.toString());
    }
    type = getDbType(sourceDir, dbName);
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
   * Close the dbs.
   */
  public static void close() {
    dbMap.forEach((key, value) -> {
      try {
        value.close();
      } catch (IOException ex) {
        ex.printStackTrace();
      }
    });
  }

  private static DbType getDbType(String sourceDir, String dbName) {
    String engineFile = String.format("%s%s%s%s%s", sourceDir, File.separator,
            dbName, File.separator, ENGINE_FILE);
    if (!new File(engineFile).exists()) {
      return DbType.LevelDB;
    }
    String engine = PropUtil.readProperty(engineFile, KEY_ENGINE);
    if (engine.equalsIgnoreCase("ROCKSDB")) {
      return DbType.RocksDB;
    } else {
      return DbType.LevelDB;
    }
  }

  private static LevelDBImpl openLevelDb(Path db) throws IOException {
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

  private static RocksDBImpl openRocksDb(Path db) throws RocksDBException {
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
}
