package org.tron.plugins;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.iq80.leveldb.impl.Iq80DBFactory.factory;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
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
import picocli.CommandLine;
import picocli.CommandLine.Option;

@Slf4j(topic = "archive")
/*
  a helper to rewrite leveldb manifest.
 */
public class ArchiveManifest implements Callable<Boolean> {


  private static final String KEY_ENGINE = "ENGINE";
  private static final String LEVELDB = "LEVELDB";

  private final Path srcDbPath;
  private final String name;
  private final Options options;
  private final long startTime;


  private static final int CPUS  = Runtime.getRuntime().availableProcessors();

  public ArchiveManifest(String src, String name, int maxManifestSize, int maxBatchSize) {
    this.name = name;
    this.srcDbPath = Paths.get(src, name);
    this.startTime = System.currentTimeMillis();
    this.options = newDefaultLevelDbOptions();
    this.options.maxManifestSize(maxManifestSize);
    this.options.maxBatchSize(maxBatchSize);
  }

  @Override
  public Boolean call() throws Exception {
    return doArchive();
  }

  public static org.iq80.leveldb.Options newDefaultLevelDbOptions() {
    org.iq80.leveldb.Options dbOptions = new org.iq80.leveldb.Options();
    dbOptions.createIfMissing(true);
    dbOptions.paranoidChecks(true);
    dbOptions.verifyChecksums(true);
    dbOptions.compressionType(CompressionType.SNAPPY);
    dbOptions.blockSize(4 * 1024);
    dbOptions.writeBufferSize(10 * 1024 * 1024);
    dbOptions.cacheSize(10 * 1024 * 1024L);
    dbOptions.maxOpenFiles(1000);
    dbOptions.maxBatchSize(64_000);
    dbOptions.maxManifestSize(128);
    return dbOptions;
  }

  public static void main(String[] args) {
    int code = run(args);
    logger.info("exit code {}.", code);
    System.out.printf("exit code %d.\n", code);
    System.exit(code);
  }

  public static int run(String[] args) {
    Args parameters = new Args();
    CommandLine commandLine = new CommandLine(parameters);
    commandLine.parseArgs(args);
    if (parameters.help) {
      commandLine.usage(System.out);
      return 0;
    }

    File dbDirectory = new File(parameters.databaseDirectory);
    if (!dbDirectory.exists()) {
      logger.info("Directory {} does not exist.", parameters.databaseDirectory);
      return 404;
    }

    if (!dbDirectory.isDirectory()) {
      logger.info("{} is not directory.", parameters.databaseDirectory);
      return 405;
    }

    List<File> files = Arrays.stream(Objects.requireNonNull(dbDirectory.listFiles()))
        .filter(File::isDirectory).collect(
            Collectors.toList());

    if (files.isEmpty()) {
      logger.info("Directory {} does not contain any database.", parameters.databaseDirectory);
      return 0;
    }
    final long time = System.currentTimeMillis();
    final List<Future<Boolean>> res = new ArrayList<>();
    final ThreadPoolExecutor executor = new ThreadPoolExecutor(
        CPUS, 16 * CPUS, 1, TimeUnit.MINUTES,
        new ArrayBlockingQueue<>(CPUS, true), Executors.defaultThreadFactory(),
        new ThreadPoolExecutor.CallerRunsPolicy());

    executor.allowCoreThreadTimeOut(true);

    files.forEach(f -> res.add(
        executor.submit(new ArchiveManifest(parameters.databaseDirectory, f.getName(),
            parameters.maxManifestSize, parameters.maxBatchSize))));
    int fails = res.size();

    for (Future<Boolean> re : res) {
      try {
        if (Boolean.TRUE.equals(re.get())) {
          fails--;
        }
      } catch (InterruptedException e) {
        logger.error("{}", e);
        Thread.currentThread().interrupt();
      } catch (ExecutionException e) {
        logger.error("{}", e);
      }
    }

    executor.shutdown();
    logger.info("DatabaseDirectory:{}, maxManifestSize:{}, maxBatchSize:{},"
            + "database reopen use {} seconds total.",
        parameters.databaseDirectory, parameters.maxManifestSize, parameters.maxBatchSize,
        (System.currentTimeMillis() - time) / 1000);
    if (fails > 0) {
      logger.error("Failed!!!!!!!!!!!!!!!!!!!!!!!! size:{}", fails);
    }
    return fails;
  }

  public void open() throws IOException {
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
    logger.info("CurrentName {}/{},size {} kb.", dir, currentName, current.length() / 1024);
    if ("market_pair_price_to_order".equalsIgnoreCase(this.name)) {
      logger.info("Db {} ignored.", this.name);
      return false;
    }
    return current.length() >= maxSize * 1024 * 1024;
  }

  public boolean doArchive() throws IOException {
    File levelDbFile = srcDbPath.toFile();
    if (!levelDbFile.exists()) {
      logger.info("File {},does not exist, ignored.", srcDbPath.toString());
      return true;
    }
    if (!checkManifest(levelDbFile.toString())) {
      logger.info("Db {},no need, ignored.", levelDbFile.toString());
      return true;
    }
    if (!checkEngine()) {
      logger.info("Db {},not leveldb, ignored.", this.name);
      return true;
    }
    open();
    logger.info("Db {} archive use {} ms.", this.name, (System.currentTimeMillis() - startTime));
    return true;
  }

  public boolean checkEngine() {
    String dir = this.srcDbPath.toString();
    String enginePath = dir + File.separator + "engine.properties";
    if (!new File(enginePath).exists() && !writeProperty(enginePath, KEY_ENGINE, LEVELDB)) {
      return false;
    }
    String engine = readProperty(enginePath, KEY_ENGINE);
    return LEVELDB.equals(engine);
  }

  public static String readProperty(String file, String key) {
    try (FileInputStream fileInputStream = new FileInputStream(file);
         InputStream inputStream = new BufferedInputStream(fileInputStream)) {
      Properties prop = new Properties();
      prop.load(inputStream);
      return new String(prop.getProperty(key, "").getBytes(StandardCharsets.ISO_8859_1),
          UTF_8);
    } catch (Exception e) {
      logger.error("{}", e);
      return "";
    }
  }

  public static boolean writeProperty(String file, String key, String value) {
    try (OutputStream out = new FileOutputStream(file);
         FileInputStream fis = new FileInputStream(file);
         BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(out, UTF_8))) {
      BufferedReader bf = new BufferedReader(new InputStreamReader(fis, UTF_8));
      Properties properties = new Properties();
      properties.load(bf);
      properties.setProperty(key, value);
      properties.store(bw, "Generated by the application.  PLEASE DO NOT EDIT! ");
    } catch (Exception e) {
      logger.warn("{}", e);
      return false;
    }
    return true;
  }

  public static class Args {

    @Option(names = {"-d", "--database-directory"},
        defaultValue = "output-directory/database",
        description = "java-tron database directory. Default: ${DEFAULT-VALUE}")
    private  String databaseDirectory;

    @Option(names = {"-b", "--batch-size" },
        defaultValue = "80000",
        description = "deal manifest batch size. Default: ${DEFAULT-VALUE}")
    private  int maxBatchSize;

    @Option(names = {"-m", "--manifest-size" },
        defaultValue = "0",
        description = "manifest  min size(M) to archive. Default: ${DEFAULT-VALUE}")
    private  int maxManifestSize;

    @Option(names = {"-h", "--help"}, help = true)
    private  boolean help;
  }
}