package org.tron.plugins;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.ReadOptions;
import org.iq80.leveldb.WriteBatch;
import org.tron.plugins.utils.DBUtils;
import org.tron.plugins.utils.FileUtils;
import picocli.CommandLine;


@CommandLine.Command(name = "expand",
    description = "expand db size .")
@Slf4j(topic = "expand")
public class DbExpand implements Callable<Integer> {
  private static final int BATCH = 1024;
  public static final byte ADD_PRE_FIX_BYTE_MAINNET = (byte) 0x41;

  @CommandLine.Spec
  CommandLine.Model.CommandSpec spec;

  @CommandLine.Option(names = {"-d", "--database"},
      defaultValue = "output-directory/database",
      description = "database directory path. Default: ${DEFAULT-VALUE}")
  private Path database;

  @CommandLine.Option(names = {"--target-database"},
      defaultValue = "target/database")
  private Path targetDatabase;

  @CommandLine.Option(names = {"--target-db"},
      defaultValue = "account",
      description = " expend db target db name")
  private String targetDb;

  @CommandLine.Option(names = {"--target-type"},
      defaultValue = "0",
      description = "0 reWrite 1 Cold Data + Warm Data 2. Warm Data + Cold Data")
  private int targetType;

  @CommandLine.Option(names = {"--expend-rate"},
      defaultValue = "3",
      description = " expend rate")
  private int expendRate;

  @CommandLine.Option(names = {"-h", "--help"}, help = true, description = "display a help message")
  boolean help;

  final Random random = new Random();
  SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
  long blockExpandBegin = 1_000_000_000L;

  @Override
  public Integer call() throws Exception {
    if (help) {
      spec.commandLine().usage(System.out);
      return 0;
    }
    long start = System.currentTimeMillis();
    this.run();
    long cost = System.currentTimeMillis() - start;
    spec.commandLine().getOut().println(String.format("Expand %s done,cost:%s seconds", targetDb,
        cost / 1000));
    return 0;
  }

  private void run() throws Exception {
    final Path sourcePath = Paths.get(database.toString(), targetDb);
    Path targetPath = Paths.get(this.targetDatabase.toString(), targetDb);
    FileUtils.createDirIfNotExists(targetPath.toString());
    DB source = DBUtils.newLevelDb(Paths.get(database.toString(), targetDb));
    if (targetType == 0) {
      final DB target = DBUtils.newLevelDb(Paths.get(targetDatabase.toString(), targetDb));
      logger.info("Rewrite db {} start", targetDb);
      spec.commandLine().getOut().println(String.format("%s Rewrite db %s start",
          dateFormat.format(new Date()), targetDb));
      logger.info("DB size: {} M", getStats(source));
      spec.commandLine().getOut().println(String.format("%s DB size: %s M",
          dateFormat.format(new Date()), getStats(source)));
      merge(source, target);
      logger.info("Rewrite db {} done", targetDb);
      spec.commandLine().getOut().println(String.format("%s Rewrite db %s done",
          dateFormat.format(new Date()), targetDb));
      return;
    }

    logger.info("Expand db {} start", targetDb);
    spec.commandLine().getOut().println(String.format("%s Expand db %s start",
        dateFormat.format(new Date()),  targetDb));
    logger.info("DB size: {} M , expend rate: {}", getStats(source), expendRate);
    spec.commandLine().getOut().println(String.format("%s DB size: %s M , expend rate: %s",
        dateFormat.format(new Date()), getStats(source), expendRate));

    if (targetType == 2) {
      copy(database, targetDatabase, targetDb);
    }
    DB target = DBUtils.newLevelDb(Paths.get(targetDatabase.toString(), targetDb));
    if (targetType == 1) {
      // generate Cold Data
      logger.info("Generate Cold Data start in path {}", targetPath);
      spec.commandLine().getOut().println(String.format("%s Generate Cold Data start in path %s",
          dateFormat.format(new Date()), targetPath));
      generateColdData(source, target, expendRate);
      logger.info("Generate Cold Data done in path {}", targetPath);
      spec.commandLine().getOut().println(String.format("%s Generate Cold Data done in path %s",
          dateFormat.format(new Date()), targetPath));
      // merge Warm Data to Cold Data
      logger.info("Merge Warm Data {} to Cold Data {} start", sourcePath, targetPath);
      spec.commandLine().getOut().println(String.format(
          "%s Merge Warm Data %s to Cold Data %s start",
          dateFormat.format(new Date()), sourcePath, targetPath));
      merge(source, target);
      logger.info("Merge Warm Data {} to Cold Data {} done", sourcePath, targetPath);
      spec.commandLine().getOut().println(String.format(
          "%s Merge Warm Data %s to Cold Data %s done",
          dateFormat.format(new Date()), sourcePath, targetPath));
      source.close();
    } else if (targetType == 2) {
      // generate Cold Data
      Path coldPath = Paths.get(targetDatabase.toString(), targetDb + "_cold");
      DB coldData = DBUtils.newLevelDb(coldPath);
      logger.info("Generate Cold Data start in path {}", coldPath);
      spec.commandLine().getOut().println(String.format("%s Generate Cold Data start in path %s",
          dateFormat.format(new Date()), coldPath));
      generateColdData(source, coldData, expendRate);
      logger.info("Generate Cold Data done in path {}", coldPath);
      spec.commandLine().getOut().println(String.format("%s Generate Cold Data done in path %s",
          dateFormat.format(new Date()), coldPath));
      // merge Cold Data to Warn Data
      logger.info("Merge Cold Data {} to Warm Data {} start", coldPath, targetPath);
      spec.commandLine().getOut().println(String.format(
          "%s Merge Cold Data %s to Warm Data %s start",
          dateFormat.format(new Date()), coldPath, targetPath));
      merge(coldData, target);
      logger.info("Merge Cold Data {} to Warm Data {} done", coldPath, targetPath);
      spec.commandLine().getOut().println(String.format(
          "%s Merge Cold Data %s to Warm Data %s done",
          dateFormat.format(new Date()), coldPath, targetPath));
      coldData.close();
      FileUtils.deleteDir(coldPath.toFile());
      source.close();
    }

    logger.info("Expand db {} done", targetDb);
    spec.commandLine().getOut().println(String.format("%s Expand db %s done",
        dateFormat.format(new Date()), targetDb));
    logger.info("Expand DB size: {} M", getStats(target));
    target.close();
  }

  public void copy(Path source, Path dest, String db) {
    FileUtils.createDirIfNotExists(Paths.get(dest.toString(), db).toString());
    logger.info("Copy database {} start", db);
    FileUtils.copyDir(source, dest, db);
    logger.info("Copy database {} end", db);
  }

  public double getStats(DB db) {
    return Arrays.stream(db.getProperty("leveldb.stats").split("\n"))
        .skip(3)
        .map(s -> s.trim().replaceAll(" +", ",").split(",")[2])
        .mapToLong(Long::parseLong)
        .sum();
  }

  public void merge(DB source, DB target) throws Exception {
    List<byte[]> keys = new ArrayList<>(BATCH);
    List<byte[]> values = new ArrayList<>(BATCH);
    JniDBFactory.pushMemoryPool(2048 * 2048);
    try (
        DBIterator levelIterator = source.iterator(new ReadOptions().fillCache(false))) {
      levelIterator.seekToFirst();
      while (levelIterator.hasNext()) {
        Map.Entry<byte[], byte[]> entry = levelIterator.next();
        byte[] key = entry.getKey();
        byte[] value = entry.getValue();
        keys.add(key);
        values.add(value);
        if (keys.size() >= BATCH) {
          insertToLevelDb(target, keys, values);
        }
      }
      // clear
      if (!keys.isEmpty()) {
        insertToLevelDb(target, keys, values);
      }
    } finally {
      JniDBFactory.popMemoryPool();
    }
  }

  private void generateColdData(DB source, DB coldData, int expendRate) {
    JniDBFactory.pushMemoryPool(2048 * 2048);
    try {
      IntStream.range(0, expendRate - 1).parallel().forEach(i -> {
        List<byte[]> keys = new ArrayList<>(BATCH);
        List<byte[]> values = new ArrayList<>(BATCH);
        try (DBIterator levelIterator = source.iterator(
            new org.iq80.leveldb.ReadOptions().fillCache(false))) {
          levelIterator.seekToFirst();
          while (levelIterator.hasNext()) {
            Map.Entry<byte[], byte[]> entry = levelIterator.next();
            byte[] key = generateAddress();
            keys.add(key);
            values.add(entry.getValue());
            if (keys.size() >= BATCH) {
              insertToLevelDb(coldData, keys, values);
            }
          }
          if (!keys.isEmpty()) {
            insertToLevelDb(coldData, keys, values);
          }
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });

    } finally {
      JniDBFactory.popMemoryPool();
    }
  }

  private byte[] generateAddress() {
    // generate the random number
    if ("account".equalsIgnoreCase(targetDb)) {
      byte[] result = new byte[21];
      random.nextBytes(result);
      result[0] = ADD_PRE_FIX_BYTE_MAINNET;
      return result;
    }
    if ("storage-row".equalsIgnoreCase(targetDb)) {
      byte[] result = new byte[32];
      random.nextBytes(result);
      return result;
    }
    //block  trans transRet
    if ("block".equalsIgnoreCase(targetDb)) {
      byte[] result = new byte[32];
      random.nextBytes(result);
      writeLongToBytes(blockExpandBegin,result,true);
      blockExpandBegin++;
      return result;
    }
    if ("trans".equalsIgnoreCase(targetDb)) {
      byte[] result = new byte[32];
      random.nextBytes(result);
      return result;
    }
    if ("tranRet".equalsIgnoreCase(targetDb)) {
      byte[] result = new byte[8];
      random.nextBytes(result);
      writeLongToBytes(blockExpandBegin,result,true);
      blockExpandBegin++;
      return result;
    }

    throw new IllegalArgumentException("Unsupported db type: " + targetDb);
  }
  /**
   * 将Long值写入字节数组的前8字节
   * @param value 要写入的Long值
   * @param target 目标字节数组（长度必须≥8）
   * @param bigEndian 是否使用大端序（true=大端序，false=小端序）
   */
  public static void writeLongToBytes(long value, byte[] target, boolean bigEndian) {
    if (target.length < 8) {
      throw new IllegalArgumentException("目标数组长度必须≥8");
    }

    ByteBuffer buffer = ByteBuffer.allocate(8)
        .order(bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN)
        .putLong(value);

    System.arraycopy(buffer.array(), 0, target, 0, 8);
  }
  private void insertToLevelDb(DB db, List<byte[]> keys, List<byte[]> values)
      throws IOException {
    try (WriteBatch batch = db.createWriteBatch()) {
      for (int i = 0; i < keys.size(); i++) {
        byte[] k = keys.get(i);
        byte[] v = values.get(i);
        batch.put(k, v);
      }
      db.write(batch);
      keys.clear();
      values.clear();
    }
  }
}
