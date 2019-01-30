package org.tron.program;

import static org.fusesource.leveldbjni.JniDBFactory.factory;

import java.io.File;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.rocksdb.BlockBasedTableConfig;
import org.rocksdb.BloomFilter;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.tron.common.utils.FileUtil;

@Slf4j
public class DBConvert {

  static {
    RocksDB.loadLibrary();
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

  public DB newLevelDB(String dir, String dbname) throws IOException {
    DB database = null;
    File file = new File(dir + dbname);
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
    options.setTargetFileSizeBase(256 * 1024 * 1024);
    options.setBaseBackgroundCompactions(Math.max(1, Runtime.getRuntime().availableProcessors()));
    options.setLevel0FileNumCompactionTrigger(4);
    final BlockBasedTableConfig tableCfg;
    options.setTableFormatConfig(tableCfg = new BlockBasedTableConfig());
    tableCfg.setBlockSize(64 * 1024);
    tableCfg.setBlockCacheSize(32 * 1024 * 1024);
    tableCfg.setCacheIndexAndFilterBlocks(true);
    tableCfg.setPinL0FilterAndIndexBlocksInCache(true);
    tableCfg.setFilter(new BloomFilter(10, false));
    return options;
  }

  public RocksDB newRocksDB(String dir, String dbname) {
    RocksDB database = null;
    try (Options options = newDefaultRocksDbOptions()) {
      database = RocksDB.open(options, dir + dbname);
    } catch (Exception ignore) {
      logger.error(ignore.getMessage());
    }
    return database;
  }

  public boolean convertLeveltoRocks(DB level, RocksDB rocks) {
    int count = 0;

    DBIterator iterator = level.iterator();
    try {
      for (iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
        byte[] key = iterator.peekNext().getKey();
        byte[] value = iterator.peekNext().getValue();
        count++;
        rocks.put(key, value);
      }
    } catch (RocksDBException e) {
      logger.error(e.getMessage());
      return false;
    } finally {
      try {
        iterator.close();
      } catch (IOException e1) {
        logger.error(e1.getMessage());
      }
    }
    logger.info("covert {} items from LevelDb to RocksDb", count);
    return true;
  }

  public boolean doConvert(String[] args) {
    String levelDbDir = "";
    String rocksDbDir = "";

    if (args.length < 2) {
      levelDbDir = "output-directory";
      rocksDbDir = "output-directory-rocks";
    } else {
      levelDbDir = args[0];
      rocksDbDir = args[1];
    }

    String srcDir = levelDbDir + File.separator + "database" + File.separator;
    String dstDir = rocksDbDir + File.separator + "database" + File.separator;

    File levels = new File(srcDir);
    if (!levels.exists()) {
      System.out.println(srcDir + " not exists.");
      return false;
    }

    File[] aa = levels.listFiles();
    if (aa == null || aa.length == 0) {
      return false;
    }
    for (File file : aa) {
      DB level = null;
      try {
        level = newLevelDB(srcDir, file.getName());
      } catch (IOException e) {
        e.printStackTrace();
      }

      FileUtil.createDirIfNotExists(dstDir + file.getName());

      RocksDB rocks = null;
      rocks = newRocksDB(dstDir, file.getName());

      if (convertLeveltoRocks(level, rocks)) {
        System.out.println("success");
      } else {
        System.out.println("failure");
      }
    }

    return true;
  }

  public static void main(String[] args) {
    DBConvert convert = new DBConvert();
    convert.doConvert(args);
  }
}