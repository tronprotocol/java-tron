package org.tron.program;

import static org.fusesource.leveldbjni.JniDBFactory.factory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.extern.slf4j.Slf4j;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.rocksdb.BlockBasedTableConfig;
import org.rocksdb.BloomFilter;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.PropUtil;

@Slf4j
public class DBConvert {

  static {
    RocksDB.loadLibrary();
  }

  private String srcDir;
  private String dstDir;
  private String dbName;
  private Path srcDbPath;
  private Path dstDbPath;

  private int srcDbKeyCount = 0;
  private int dstDbKeyCount = 0;
  private int srcDbKeySum = 0;
  private int dstDbKeySum = 0;
  private int srcDbValueSum = 0;
  private int dstDbValueSum = 0;

  public DBConvert(String src, String dst, String name) {
    this.srcDir = src;
    this.dstDir = dst;
    this.dbName = name;
    this.srcDbPath = Paths.get(this.srcDir, name);
    this.dstDbPath = Paths.get(this.dstDir, name);
  }

  private static org.iq80.leveldb.Options newDefaultLevelDbOptions() {
    org.iq80.leveldb.Options dbOptions = new org.iq80.leveldb.Options();
    dbOptions.createIfMissing(true);
    dbOptions.paranoidChecks(true);
    dbOptions.verifyChecksums(true);
    dbOptions.compressionType(CompressionType.SNAPPY);
    dbOptions.blockSize(4 * 1024);
    dbOptions.writeBufferSize(10 * 1024 * 1024);
    dbOptions.cacheSize(10 * 1024 * 1024L);
    dbOptions.maxOpenFiles(100);
    return dbOptions;
  }

  public static void main(String[] args) {
    String dbSrc;
    String dbDst;
    if (args.length < 2) {
      dbSrc = "output-directory/database";
      dbDst = "output-directory-dst/database";
    } else {
      dbSrc = args[0];
      dbDst = args[1];
    }
    File dbDirectory = new File(dbSrc);
    if (!dbDirectory.exists()) {
      System.out.println(dbSrc + "does not exist.");
      return;
    }
    File[] files = dbDirectory.listFiles();

    if (files == null || files.length == 0) {
      System.out.println(dbSrc + "does not contain any database.");
      return;
    }
    long time = System.currentTimeMillis();
    for (File file : files) {
      if (!file.isDirectory()) {
        System.out.println(file.getName() + " is not a database directory, ignore it.");
        continue;
      }
      try {
        DBConvert convert = new DBConvert(dbSrc, dbDst, file.getName());
        if (convert.doConvert()) {
          System.out.println(String
              .format(
                  "Convert database %s successful with %s key-value. keySum: %d, valueSum: %d",
                  convert.dbName,
                  convert.srcDbKeyCount, convert.dstDbKeySum, convert.dstDbValueSum));
        } else {
          System.out.println(String.format("Convert database %s failure", convert.dbName));
        }
      } catch (Exception e) {
        System.out.println(e.getMessage());
        return;
      }
    }
    System.out.println(String
        .format("database convert use %d seconds total.",
            (System.currentTimeMillis() - time) / 1000));
  }

  public DB newLevelDb(Path db) throws IOException {
    DB database;
    File file = db.toFile();
    org.iq80.leveldb.Options dbOptions = newDefaultLevelDbOptions();
    try {
      database = factory.open(file, dbOptions);
    } catch (IOException e) {
      if (e.getMessage().contains("Corruption:")) {
        factory.repair(file, dbOptions);
        database = factory.open(file, dbOptions);
      } else {
        throw e;
      }
    }
    return database;
  }

  private Options newDefaultRocksDbOptions() {
    Options options = new Options();
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
    final BlockBasedTableConfig tableCfg;
    options.setTableFormatConfig(tableCfg = new BlockBasedTableConfig());
    tableCfg.setBlockSize(64 * 1024);
    tableCfg.setBlockCacheSize(32 * 1024 * 1024);
    tableCfg.setCacheIndexAndFilterBlocks(true);
    tableCfg.setPinL0FilterAndIndexBlocksInCache(true);
    tableCfg.setFilter(new BloomFilter(10, false));
    return options;
  }

  public RocksDB newRocksDb(Path db) {
    RocksDB database = null;
    try (Options options = newDefaultRocksDbOptions()) {
      database = RocksDB.open(options, db.toString());
    } catch (Exception ignore) {
      logger.error(ignore.getMessage());
    }
    return database;
  }

  public boolean convertLevelToRocks(DB level, RocksDB rocks) {
    // convert
    DBIterator levelIterator = level.iterator();
    try {
      for (levelIterator.seekToFirst(); levelIterator.hasNext(); levelIterator.next()) {
        byte[] key = levelIterator.peekNext().getKey();
        byte[] value = levelIterator.peekNext().getValue();
        srcDbKeyCount++;
        srcDbKeySum = byteArrayToIntWithOne(srcDbKeySum, key);
        srcDbValueSum = byteArrayToIntWithOne(srcDbValueSum, value);
        rocks.put(key, value);
      }
    } catch (RocksDBException e) {
      logger.error(e.getMessage());
      return false;
    } finally {
      try {
        levelIterator.close();
      } catch (IOException e1) {
        logger.error(e1.getMessage());
      }
    }

    // check
    try (final RocksIterator rocksIterator = rocks.newIterator()) {
      for (rocksIterator.seekToLast(); rocksIterator.isValid(); rocksIterator.prev()) {
        byte[] key = rocksIterator.key();
        byte[] value = rocksIterator.value();
        dstDbKeyCount++;
        dstDbKeySum = byteArrayToIntWithOne(dstDbKeySum, key);
        dstDbValueSum = byteArrayToIntWithOne(dstDbValueSum, value);
      }
    }

    return dstDbKeyCount == srcDbKeyCount && dstDbKeySum == srcDbKeySum
        && dstDbValueSum == srcDbValueSum;
  }

  public boolean createEngine(String dir) {
    String enginePath = dir + File.separator + "engine.properties";

    if (!FileUtil.createFileIfNotExists(enginePath)) {
      return false;
    }

    return PropUtil.writeProperty(enginePath, "ENGINE", "ROCKSDB");
  }

  public boolean doConvert() {

    File levelDbFile = srcDbPath.toFile();
    if (!levelDbFile.exists()) {
      System.out.println(srcDbPath.toString() + "does not exist.");
      return false;
    }

    DB level = null;
    try {
      level = newLevelDb(srcDbPath);
    } catch (IOException e) {
      logger.error("{}", e);
    }

    FileUtil.createDirIfNotExists(dstDir);
    RocksDB rocks = newRocksDb(dstDbPath);

    return convertLevelToRocks(level, rocks) && createEngine(dstDbPath.toString());
  }

  public int byteArrayToIntWithOne(int sum, byte[] b) {
    for (byte oneByte : b) {
      sum += (int) oneByte;
    }
    return sum;
  }
}