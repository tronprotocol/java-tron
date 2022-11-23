package org.tron.plugins;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBar;
import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.rocksdb.Status;
import org.tron.plugins.utils.DBUtils;
import org.tron.plugins.utils.FileUtils;
import picocli.CommandLine;


@Slf4j(topic = "convert")
@CommandLine.Command(name = "convert",
    description = "Covert leveldb to rocksdb.",
    exitCodeListHeading = "Exit Codes:%n",
    exitCodeList = {
        "0:Successful",
        "n:Internal error: exception occurred,please check toolkit.log"})
public class DbConvert implements Callable<Integer> {

  static {
    RocksDB.loadLibrary();
  }

  private static final int BATCH  = 256;

  @CommandLine.Spec
  CommandLine.Model.CommandSpec spec;
  @CommandLine.Parameters(index = "0", defaultValue = "output-directory/database",
      description = " Input path for leveldb. Default: ${DEFAULT-VALUE}")
  private File src;
  @CommandLine.Parameters(index = "1", defaultValue = "output-directory-dst/database",
      description = "Output path for rocksdb. Default: ${DEFAULT-VALUE}")
  private File dest;

  @CommandLine.Option(names = {"--safe"},
      description = "In safe mode, read data from leveldb then put rocksdb."
          + "If not, just change engine.properties from leveldb to rocksdb,"
          + "rocksdb is compatible with leveldb for current version."
          + "This may not be the case in the future."
          + "Default: ${DEFAULT-VALUE}")
  private boolean safe;

  @CommandLine.Option(names = {"-h", "--help"})
  private boolean help;


  @Override
  public Integer call() throws Exception {
    if (help) {
      spec.commandLine().usage(System.out);
      return 0;
    }
    if (!src.exists()) {
      logger.info(" {} does not exist.", src);
      spec.commandLine().getErr().println(spec.commandLine().getColorScheme()
          .errorText(String.format("%s does not exist.", src)));
      return 404;
    }
    List<File> files = Arrays.stream(Objects.requireNonNull(src.listFiles()))
        .filter(File::isDirectory)
        .filter(e -> !DBUtils.CHECKPOINT_DB_V2.equals(e.getName()))
        .collect(Collectors.toList());

    // add checkpoint v2 convert
    File cpV2Dir = new File(Paths.get(src.toString(), DBUtils.CHECKPOINT_DB_V2).toString());
    List<File> cpList = new ArrayList<>();
    if (cpV2Dir.exists()) {
      cpList = Arrays.stream(Objects.requireNonNull(cpV2Dir.listFiles()))
          .filter(File::isDirectory)
          .collect(Collectors.toList());
    }

    if (files.isEmpty()) {
      logger.info("{} does not contain any database.", src);
      spec.commandLine().getOut().format("%s does not contain any database.", src).println();
      return 0;
    }
    final long time = System.currentTimeMillis();
    List<Converter> services = new ArrayList<>();
    files.forEach(f -> services.add(
        new DbConverter(src.getPath(), dest.getPath(), f.getName(), safe)));
    cpList.forEach(f -> services.add(
        new DbConverter(
            Paths.get(src.getPath(), DBUtils.CHECKPOINT_DB_V2).toString(),
            Paths.get(dest.getPath(), DBUtils.CHECKPOINT_DB_V2).toString(),
            f.getName(), safe)));
    List<String> fails = ProgressBar.wrap(services.stream(), "convert task").parallel().map(
        dbConverter -> {
          try {
            return dbConverter.doConvert() ? null : dbConverter.name();
          } catch (Exception e) {
            logger.error("{}", e);
            spec.commandLine().getErr().println(spec.commandLine().getColorScheme()
                .errorText(e.getMessage()));
            return dbConverter.name();
          }
        }).filter(Objects::nonNull).collect(Collectors.toList());
    long during = (System.currentTimeMillis() - time) / 1000;
    spec.commandLine().getOut().format("convert db done, fails: %s, take %d s.",
        fails, during).println();
    logger.info("database convert use {} seconds total, fails: {}.", during, fails);
    return fails.size();
  }

  interface Converter {

    boolean doConvert() throws Exception;

    String name();
  }

  static class DbConverter implements Converter {
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

    private boolean safe;

    public DbConverter(String srcDir, String dstDir, String name, boolean safe) {
      this.srcDir = srcDir;
      this.dstDir = dstDir;
      this.dbName = name;
      this.srcDbPath = Paths.get(this.srcDir, name);
      this.dstDbPath = Paths.get(this.dstDir, name);
      this.safe = safe;
    }

    @Override
    public boolean doConvert() throws Exception {

      if (checkDone(this.dstDbPath.toString())) {
        logger.info(" {} is done, skip it.", this.dbName);
        return true;
      }

      File levelDbFile = srcDbPath.toFile();
      if (!levelDbFile.exists()) {
        logger.info(" {} does not exist.", srcDbPath);
        return true;
      }
      if (!FileUtils.isLevelDBEngine(srcDbPath)) {
        logger.info("Db {},not leveldb, ignored.", this.dbName);
        return true;
      }
      long startTime = System.currentTimeMillis();
      if (this.dstDbPath.toFile().exists()) {
        logger.info(" {} begin to clear exist database directory", this.dbName);
        FileUtils.deleteDir(this.dstDbPath.toFile());
        logger.info(" {} clear exist database directory done.", this.dbName);
      }

      FileUtils.createDirIfNotExists(dstDir);

      logger.info("Convert database {} start", this.dbName);
      if (safe) {
        convertLevelToRocks();
        compact();
      } else {
        FileUtils.copyDir(Paths.get(srcDir), Paths.get(dstDir), dbName);
      }
      boolean result = check() && createEngine(dstDbPath.toString());
      long etime = System.currentTimeMillis();

      if (result) {
        if (safe) {
          logger.info("Convert database {} successful end with {} key-value {} minutes",
              this.dbName, this.srcDbKeyCount, (etime - startTime) / 1000.0 / 60);
        } else {
          logger.info("Convert database {} successful end  {} minutes",
              this.dbName, (etime - startTime) / 1000.0 / 60);
        }

      } else {
        logger.info("Convert database {} failure", this.dbName);
        if (this.dstDbPath.toFile().exists()) {
          logger.info(" {} begin to clear exist database directory", this.dbName);
          FileUtils.deleteDir(this.dstDbPath.toFile());
          logger.info(" {} clear exist database directory done.", this.dbName);
        }
      }
      return result;
    }

    @Override
    public String name() {
      return dbName;
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

    /**
     * https://github.com/facebook/rocksdb/issues/6625.
     *
     * @param rocks db
     * @param batch write batch
     * @throws Exception RocksDBException
     */
    private void write(RocksDB rocks, org.rocksdb.WriteBatch batch) throws Exception {
      try {
        rocks.write(new org.rocksdb.WriteOptions(), batch);
      } catch (RocksDBException e) {
        // retry
        if (maybeRetry(e)) {
          TimeUnit.MILLISECONDS.sleep(1);
          write(rocks, batch);
        } else {
          throw e;
        }
      }
    }

    private boolean maybeRetry(RocksDBException e) {
      boolean retry = false;
      if (e.getStatus() != null) {
        retry = e.getStatus().getCode() == Status.Code.TryAgain
            || e.getStatus().getCode() == Status.Code.Busy
            || e.getStatus().getCode() == Status.Code.Incomplete;
      }
      return retry || (e.getMessage() != null && ("Write stall".equalsIgnoreCase(e.getMessage())
          || ("Incomplete").equalsIgnoreCase(e.getMessage())));
    }

    /**
     * https://github.com/facebook/rocksdb/wiki/RocksDB-FAQ .
     *  What's the fastest way to load data into RocksDB?
     *
     * @return if ok
     */
    public void convertLevelToRocks() throws Exception {
      List<byte[]> keys = new ArrayList<>(BATCH);
      List<byte[]> values = new ArrayList<>(BATCH);
      JniDBFactory.pushMemoryPool(1024 * 1024);
      try (
          DB level = DBUtils.newLevelDb(srcDbPath);
          RocksDB rocks = DBUtils.newRocksDbForBulkLoad(dstDbPath);
          DBIterator levelIterator = level.iterator(
              new org.iq80.leveldb.ReadOptions().fillCache(false))) {

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
            batchInsert(rocks, keys, values);
          }
        }
        // clear
        if (!keys.isEmpty()) {
          batchInsert(rocks, keys, values);
        }
      } finally {
        JniDBFactory.popMemoryPool();
      }
    }

    private void compact() throws RocksDBException {
      if (DBUtils.MARKET_PAIR_PRICE_TO_ORDER.equalsIgnoreCase(this.dbName)) {
        return;
      }
      try (RocksDB rocks  = DBUtils.newRocksDb(this.dstDbPath)) {
        logger.info("compact database {} start", this.dbName);
        rocks.compactRange();
        logger.info("compact database {} end", this.dbName);
      }
    }

    private boolean check() throws RocksDBException {
      if (!safe) {
        return true;
      }
      try (
          RocksDB rocks  = DBUtils.newRocksDbReadOnly(this.dstDbPath);
          org.rocksdb.ReadOptions r = new org.rocksdb.ReadOptions().setFillCache(false);
          RocksIterator rocksIterator = rocks.newIterator(r)) {

        // check
        logger.info("check database {} start", this.dbName);
        for (rocksIterator.seekToFirst(); rocksIterator.isValid(); rocksIterator.next()) {
          byte[] key = rocksIterator.key();
          byte[] value = rocksIterator.value();
          dstDbKeyCount++;
          dstDbKeySum = byteArrayToIntWithOne(dstDbKeySum, key);
          dstDbValueSum = byteArrayToIntWithOne(dstDbValueSum, value);
        }
        logger.info("Check database {} end,dstDbKeyCount {}, dstDbKeySum {}, dstDbValueSum {},"
                + "srcDbKeyCount {}, srcDbKeySum {}, srcDbValueSum {}",
            dbName, dstDbKeyCount, dstDbKeySum, dstDbValueSum,
            srcDbKeyCount, srcDbKeySum, srcDbValueSum);
        return dstDbKeyCount == srcDbKeyCount && dstDbKeySum == srcDbKeySum
            && dstDbValueSum == srcDbValueSum;
      }
    }
  }

  private static boolean createEngine(String dir) {
    String enginePath = dir + File.separator + DBUtils.FILE_ENGINE;
    if (!FileUtils.createFileIfNotExists(enginePath)) {
      return false;
    }
    return FileUtils.writeProperty(enginePath, DBUtils.KEY_ENGINE, DBUtils.ROCKSDB);
  }

  private static boolean  checkDone(String dir) {
    String enginePath = dir + File.separator + DBUtils.FILE_ENGINE;
    return FileUtils.isExists(enginePath);
  }

  private static long byteArrayToIntWithOne(long sum, byte[] b) {
    for (byte oneByte : b) {
      sum += oneByte;
    }
    return sum;
  }

}
