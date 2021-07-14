package org.tron.plugins;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.iq80.leveldb.impl.Iq80DBFactory.factory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.impl.Filename;

@Slf4j
public class PreOpenLevelDB implements Callable<Boolean> {


  private static final String KEY_ENGINE = "ENGINE";
  private static final String LEVELDB = "LEVELDB";

  private final Path srcDbPath;
  private final String name;
  private final Options options;
  private final long startTime;


  private static final int CPUS  = Runtime.getRuntime().availableProcessors();

  private static final ThreadPoolExecutor esDb = new ThreadPoolExecutor(
      CPUS, 16 * CPUS, 1, TimeUnit.MINUTES,
      new ArrayBlockingQueue<>(CPUS, true), Executors.defaultThreadFactory(),
      new ThreadPoolExecutor.CallerRunsPolicy());

  static {
    esDb.allowCoreThreadTimeOut(true);
  }

  public PreOpenLevelDB(String src, String name, boolean fast,
                        int maxManifestSize, int maxBatchSize) {
    this.name = name;
    this.srcDbPath = Paths.get(src, name);
    this.startTime = System.currentTimeMillis();
    this.options = newDefaultLevelDbOptions();
    this.options.maxManifestSize(maxManifestSize);
    this.options.maxBatchSize(maxBatchSize);
    this.options.fast(fast);
  }

  @Override
  public Boolean call() throws Exception {
    return doPreOpen();
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
    String dbSrc = "output-directory/database";
    boolean fast = false;
    int maxManifestSize = 128;
    int maxBatchSize = 52_000;
    if (args.length >= 4) {
      dbSrc = args[0];
      fast = Boolean.parseBoolean(args[1]);
      try {
        maxManifestSize = Integer.parseInt(args[2]);
      } catch (NumberFormatException e) {
        maxManifestSize = 128;
      }
      try {
        maxBatchSize = Integer.parseInt(args[3]);
      } catch (NumberFormatException e) {
        maxBatchSize = 52_000;
      }
    }
    File dbDirectory = new File(dbSrc);
    if (!dbDirectory.exists()) {
      logger.info(" {} does not exist.", dbSrc);
      return;
    }


    List<File> files = Arrays.stream(Objects.requireNonNull(dbDirectory.listFiles()))
        .filter(File::isDirectory).collect(
            Collectors.toList());

    if (files.isEmpty()) {
      logger.info("{} does not contain any database.", dbSrc);
      return;
    }
    long time = System.currentTimeMillis();
    final List<Future<Boolean>> res = new ArrayList<>();
    int finalMaxManifestSize = maxManifestSize;
    int finalMaxBatchSize = maxBatchSize;
    String finalDbSrc = dbSrc;
    boolean finalFast = fast;
    files.forEach(f -> res.add(esDb.submit(new PreOpenLevelDB(finalDbSrc, f.getName(), finalFast,
        finalMaxManifestSize, finalMaxBatchSize))));
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
    logger.info("dbSrc:{}, fast:{}, maxManifestSize:{}, maxBatchSize:{}," +
            "database reopen use {} seconds total."
        , dbSrc, fast, maxManifestSize, maxBatchSize,
        (System.currentTimeMillis() - time) / 1000);
    if (fails > 0) {
      logger.error("failed!!!!!!!!!!!!!!!!!!!!!!!! size:{}", fails);
    }
    System.exit(fails);
  }

  public void openLevelDb() throws IOException {
    DB database = factory.open(this.srcDbPath.toFile(), this.options);
    database.close();

  }

  public boolean checkManifest(String dir) throws IOException {
    // Read "CURRENT" file, which contains a pointer to the current manifest file
    File currentFile = new File(dir, Filename.currentFileName());
    if (!currentFile.exists()) {
      return false;
    }

    String currentName = com.google.common.io.Files.asCharSource(currentFile, UTF_8).read();
    if (currentName.isEmpty() || currentName.charAt(currentName.length() - 1) != '\n') {
      return false;
    }
    currentName = currentName.substring(0, currentName.length() - 1);
    File current = new File(dir, currentName);
    if (!current.isFile()) {
      return false;
    }
    long maxSize = options.maxManifestSize();
    if (maxSize < 0) {
      return false;
    }
    logger.info("currentName {}/{},size {} kb", dir, currentName, current.length() / 1024);
    if ("market_pair_price_to_order".equalsIgnoreCase(this.name)) {
      logger.info("database {} ignore", this.name);
      return false;
    }
    return current.length() >= maxSize * 1024 * 1024;
  }

  public boolean doPreOpen() throws IOException {
    File levelDbFile = srcDbPath.toFile();
    if (!levelDbFile.exists()) {
      logger.info("{},does not exist ignore.", srcDbPath.toString());
      return true;
    }
    if (!checkEngine()) {
      logger.info("{},not leveldb ignore.", this.name);
      return true;
    }
    if (!checkManifest(levelDbFile.toString())) {
      logger.info("{},no need ignore.", levelDbFile.toString());
      return true;
    }
    openLevelDb();
    logger.info("{} reopen use {} ms.",this.name, (System.currentTimeMillis() - startTime));
    return true;
  }

  public boolean checkEngine() {
    String dir = this.srcDbPath.toString();
    String enginePath = dir + File.separator + "engine.properties";

    // for the first init engine
    String engine = readProperty(enginePath, KEY_ENGINE);

    return LEVELDB.equals(engine);
  }

  public static String readProperty(String file, String key) {
    InputStream is = null;
    FileInputStream fis = null;
    Properties prop;
    try {
      prop = new Properties();
      fis = new FileInputStream(file);
      is = new BufferedInputStream(fis);
      prop.load(is);
      return new String(prop.getProperty(key, "").getBytes(StandardCharsets.ISO_8859_1),
          UTF_8);
    } catch (Exception e) {
      logger.error("{}", e);
      return "";
    } finally {
      //fis
      try {
        if (fis != null) {
          fis.close();
        }
      } catch (Exception e) {
        logger.warn("{}", e);
      }
      //is
      try {
        if (is != null) {
          is.close();
        }
      } catch (Exception e) {
        logger.error("{}", e);
      }
    }
  }
}