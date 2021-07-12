package org.tron.program;

import static org.fusesource.leveldbjni.JniDBFactory.factory;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.rocksdb.BlockBasedTableConfig;
import org.rocksdb.BloomFilter;
import org.rocksdb.ComparatorOptions;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.MarketOrderPriceComparatorForLevelDB;
import org.tron.common.utils.MarketOrderPriceComparatorForRockDB;
import org.tron.common.utils.PropUtil;

@Slf4j
public class DBConvert implements Callable<Boolean> {

  static {
    RocksDB.loadLibrary();
  }

  private final String srcDir;
  private final String dstDir;
  private final String dbName;
  private final Path srcDbPath;
  private final Path dstDbPath;

  private long srcDbKeyCount = 0L;
  private long dstDbKeyCount = 0L;
  private long srcDbKeySum = 0L;
  private long dstDbKeySum = 0L;
  private long srcDbValueSum = 0L;
  private long dstDbValueSum = 0L;
  private final long startTime;
  private static final int CPUS  = Runtime.getRuntime().availableProcessors();
  private static final int BATCH  = 256;
  private static final DateTimeFormatter formatter = DateTimeFormatter
      .ofPattern("yyyy-MM-dd HH:mm:ss SS");
  private static final ThreadPoolExecutor esDb = new ThreadPoolExecutor(
      CPUS, 16 * CPUS, 1, TimeUnit.MINUTES,
      new ArrayBlockingQueue<>(CPUS, true), Executors.defaultThreadFactory(),
      new ThreadPoolExecutor.CallerRunsPolicy());

  static {
    esDb.allowCoreThreadTimeOut(true);
  }

  @Override
  public Boolean call() throws Exception {
    return doConvert();
  }

  public DBConvert(String src, String dst, String name) {
    this.srcDir = src;
    this.dstDir = dst;
    this.dbName = name;
    this.srcDbPath = Paths.get(this.srcDir, name);
    this.dstDbPath = Paths.get(this.dstDir, name);
    this.startTime = System.currentTimeMillis();
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
    dbOptions.maxOpenFiles(1000);
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
      printf(" %s does not exist.", dbSrc);
      return;
    }
    List<File> files = Arrays.stream(Objects.requireNonNull(dbDirectory.listFiles()))
        .filter(File::isDirectory).collect(
            Collectors.toList());

    if (files.isEmpty()) {
      printf("%s does not contain any database.", dbSrc);
      return;
    }
    final long time = System.currentTimeMillis();
    final List<Future<Boolean>> res = new ArrayList<>();

    files.forEach(f -> res.add(esDb.submit(new DBConvert(dbSrc, dbDst, f.getName()))));

    int fails = res.size();

    for (Future<Boolean> re : res) {
      try {
        if (re.get()) {
          fails--;
        }
      } catch (InterruptedException e) {
        logger.error(e.getMessage(), e);
        Thread.currentThread().interrupt();
      } catch (ExecutionException e) {
        logger.error(e.getMessage(), e);
      }
    }

    esDb.shutdown();
    printf("database convert use %d seconds total.",
        (System.currentTimeMillis() - time) / 1000);
    if (fails > 0) {
      printf("failed!!!!!!!!!!!!!!!!!!!!!!!! size:%d", fails);
    }
    System.exit(fails);
  }

  public DB newLevelDb(Path db) throws Exception {
    DB database;
    File file = db.toFile();
    org.iq80.leveldb.Options dbOptions = newDefaultLevelDbOptions();
    if ("market_pair_price_to_order".equalsIgnoreCase(this.dbName)) {
      dbOptions.comparator(new MarketOrderPriceComparatorForLevelDB());
    }
    database = factory.open(file, dbOptions);
    return database;
  }

  private Options newDefaultRocksDbOptions() {
    Options options = new Options();
    options.setCreateIfMissing(true);
    options.setIncreaseParallelism(1);
    options.setNumLevels(7);
    options.setMaxOpenFiles(5000);
    options.setTargetFileSizeBase(64 * 1024 * 1024);
    options.setTargetFileSizeMultiplier(1);
    options.setMaxBytesForLevelBase(512 * 1024 * 1024);
    options.setMaxBackgroundCompactions(Math.max(1, Runtime.getRuntime().availableProcessors()));
    options.setLevel0FileNumCompactionTrigger(4);
    options.setLevelCompactionDynamicLevelBytes(true);
    if ("market_pair_price_to_order".equalsIgnoreCase(this.dbName)) {
      options.setComparator(new MarketOrderPriceComparatorForRockDB(new ComparatorOptions()));
    }
    final BlockBasedTableConfig tableCfg;
    options.setTableFormatConfig(tableCfg = new BlockBasedTableConfig());
    tableCfg.setBlockSize(64 * 1024);
    tableCfg.setBlockCacheSize(32 * 1024 * 1024);
    tableCfg.setCacheIndexAndFilterBlocks(true);
    tableCfg.setPinL0FilterAndIndexBlocksInCache(true);
    tableCfg.setFilter(new BloomFilter(10, false));
    options.prepareForBulkLoad();
    return options;
  }

  public RocksDB newRocksDb(Path db) {
    RocksDB database = null;
    try (Options options = newDefaultRocksDbOptions()) {
      database = RocksDB.open(options, db.toString());
    } catch (Exception e) {
      logger.error(e.getMessage());
    }
    return database;
  }

  public boolean convertLevelToRocks(DB level, RocksDB rocks) {
    // convert
    try (DBIterator levelIterator = level.iterator()) {
      JniDBFactory.pushMemoryPool(1024 * 1024);
      for (levelIterator.seekToFirst(); levelIterator.hasNext(); levelIterator.next()) {
        byte[] key = levelIterator.peekNext().getKey();
        byte[] value = levelIterator.peekNext().getValue();
        srcDbKeyCount++;
        srcDbKeySum = byteArrayToIntWithOne(srcDbKeySum, key);
        srcDbValueSum = byteArrayToIntWithOne(srcDbValueSum, value);
        rocks.put(key, value);
      }
      // check
      check(rocks);
    } catch (Exception e) {
      logger.error(e.getMessage());
      return false;
    } finally {
      try {
        level.close();
        rocks.close();
        JniDBFactory.popMemoryPool();
      } catch (Exception e1) {
        logger.error(e1.getMessage());
      }
    }
    return dstDbKeyCount == srcDbKeyCount && dstDbKeySum == srcDbKeySum
        && dstDbValueSum == srcDbValueSum;
  }


  private void batchInsert(RocksDB rocks, List<byte[]> keys, List<byte[]> values)
      throws Exception {
    try (org.rocksdb.WriteBatch batch = new org.rocksdb.WriteBatch()) {
      for (int i = 0; i < keys.size(); i++) {
        byte[] k = keys.get(i);
        byte[] v = values.get(i);
        batch.put(k, v);
      }
      write(rocks, batch);
    }
    keys.clear();
    values.clear();
  }

  private void write(RocksDB rocks, org.rocksdb.WriteBatch batch) throws Exception {
    try {
      rocks.write(new org.rocksdb.WriteOptions(), batch);
    } catch (RocksDBException e) {
      // retry
      if (e.getMessage() != null && "Write stall".equalsIgnoreCase(e.getMessage())) {
        TimeUnit.MILLISECONDS.sleep(1);
        write(rocks, batch);
      } else {
        throw e;
      }
    }
  }

  /**
   * https://github.com/facebook/rocksdb/wiki/RocksDB-FAQ .
   *  What's the fastest way to load data into RocksDB?
   * @param level leveldb
   * @param rocks rocksdb
   * @return if ok
   */
  public boolean convertLevelToRocksBatchIterator(DB level, RocksDB rocks) {
    // convert
    List<byte[]> keys = new ArrayList<>(BATCH);
    List<byte[]> values = new ArrayList<>(BATCH);
    try (DBIterator levelIterator = level.iterator(
        new org.iq80.leveldb.ReadOptions().fillCache(false))) {

      JniDBFactory.pushMemoryPool(1024 * 1024);
      levelIterator.seekToFirst();

      while (levelIterator.hasNext()) {
        Map.Entry<byte[], byte[]> entry = levelIterator.next();
        byte[] key = entry.getKey();
        byte[] value = entry.getValue();
        srcDbKeyCount++;
        srcDbKeySum = byteArrayToIntWithOne(srcDbKeySum, key);
        srcDbValueSum = byteArrayToIntWithOne(srcDbValueSum, value);
        keys.add(key);
        values.add(value);
        if (keys.size() >= BATCH) {
          try {
            batchInsert(rocks, keys, values);
          } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;
          }
        }
      }

      if (!keys.isEmpty()) {
        try {
          batchInsert(rocks, keys, values);
        } catch (Exception e) {
          logger.error(e.getMessage(), e);
          return false;
        }
      }
      // check
      check(rocks);
    }  catch (Exception e) {
      logger.error(e.getMessage());
      return false;
    } finally {
      try {
        level.close();
        rocks.close();
        JniDBFactory.popMemoryPool();
      } catch (Exception e1) {
        logger.error(e1.getMessage());
      }
    }
    return dstDbKeyCount == srcDbKeyCount && dstDbKeySum == srcDbKeySum
        && dstDbValueSum == srcDbValueSum;
  }

  private void check(RocksDB rocks) throws RocksDBException {
    printf("check database %s start", this.dbName);
    // manually call CompactRange()
    printf("compact database %s start", this.dbName);
    rocks.compactRange();
    printf("compact database %s end", this.dbName);
    // check
    try (org.rocksdb.ReadOptions r = new org.rocksdb.ReadOptions().setFillCache(false);
         RocksIterator rocksIterator = rocks.newIterator(r)) {
      for (rocksIterator.seekToFirst(); rocksIterator.isValid(); rocksIterator.next()) {
        byte[] key = rocksIterator.key();
        byte[] value = rocksIterator.value();
        dstDbKeyCount++;
        dstDbKeySum = byteArrayToIntWithOne(dstDbKeySum, key);
        dstDbValueSum = byteArrayToIntWithOne(dstDbValueSum, value);
      }
    }
    printf("check database %s end", this.dbName);
  }

  public boolean createEngine(String dir) {
    String enginePath = dir + File.separator + "engine.properties";

    if (!FileUtil.createFileIfNotExists(enginePath)) {
      return false;
    }

    return PropUtil.writeProperty(enginePath, "ENGINE", "ROCKSDB");
  }

  public boolean checkDone(String dir) {
    String enginePath = dir + File.separator + "engine.properties";
    return FileUtil.isExists(enginePath);

  }

  public boolean doConvert() throws Exception {

    if (checkDone(this.dstDbPath.toString())) {
      printf(" %s is done, skip it.", this.dbName);
      // delete leveldb for space
      // TODO
      /*if (this.srcDbPath.toFile().exists()) {
        printf(" %s begin to delete database directory", this.dbName);
        FileUtil.deleteDir(this.srcDbPath.toFile());
        printf(" %s clear database directory done.", this.dbName);
      }*/
      return true;
    }

    File levelDbFile = srcDbPath.toFile();
    if (!levelDbFile.exists()) {
      printf(" %s does not exist.", srcDbPath.toString());
      return false;
    }

    DB level = newLevelDb(srcDbPath);

    if (this.dstDbPath.toFile().exists()) {
      printf(" %s begin to clear exist database directory", this.dbName);
      FileUtil.deleteDir(this.dstDbPath.toFile());
      printf(" %s clear exist database directory done.", this.dbName);
    }

    FileUtil.createDirIfNotExists(dstDir);
    RocksDB rocks = newRocksDb(dstDbPath);

    printf("Convert database %s start", this.dbName);
    // convertLevelToRocksBatchIterator convertLevelToRocks
    boolean result  = convertLevelToRocksBatchIterator(level, rocks)
        && createEngine(dstDbPath.toString());
    long etime = System.currentTimeMillis();

    if (result) {
      printf("Convert database %s successful end with %d key-value cost %(,.2f minutes",
          this.dbName, this.srcDbKeyCount, (etime - this.startTime) / 1000.0 / 60);
      // TODO
      /*if (this.srcDbPath.toFile().exists()) {
        printf(" %s begin to delete database directory", this.srcDbPath.toFile().getName());
        FileUtil.deleteDir(this.srcDbPath.toFile());
        printf(" %s clear database directory done.", this.srcDbPath.toFile().getName());
      }*/
    } else {
      printf("Convert database %s failure", this.dbName);
      if (this.dstDbPath.toFile().exists()) {
        printf(" %s begin to clear exist database directory", this.dbName);
        FileUtil.deleteDir(this.dstDbPath.toFile());
        printf(" %s clear exist database directory done.", this.dbName);
      }
    }
    return result;
  }

  public static String getTime() {
    LocalDateTime now = LocalDateTime.now();
    return  "[" + formatter.format(now) + "] ";
  }

  public static void printf(String reg, Object... args) {
    reg = reg + "%n";
    System.out.printf("%s", getTime());
    System.out.printf(reg,  args);
  }

  public long byteArrayToIntWithOne(long sum, byte[] b) {
    for (byte oneByte : b) {
      sum += oneByte;
    }
    return sum;
  }
}