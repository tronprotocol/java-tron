package org.tron.plugins;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.extern.slf4j.Slf4j;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBComparator;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.impl.FileMetaData;
import org.iq80.leveldb.impl.InternalKeyComparator;
import org.iq80.leveldb.impl.InternalUserComparator;
import org.iq80.leveldb.impl.TableCache;
import org.iq80.leveldb.impl.VersionSet;
import org.iq80.leveldb.table.BytewiseComparator;
import org.iq80.leveldb.table.CustomUserComparator;
import org.iq80.leveldb.table.UserComparator;
import org.tron.plugins.utils.ByteArray;
import org.tron.plugins.utils.DBUtils;
import picocli.CommandLine;

@CommandLine.Command(name = "stat",
    description = "stat db .")
@Slf4j(topic = "stat")
public class DbStat implements Callable<Integer> {

  @CommandLine.Spec
  CommandLine.Model.CommandSpec spec;

  @CommandLine.Option(names = {"-d", "--database"},
      defaultValue = "output-directory/database",
      description = "database directory path. Default: ${DEFAULT-VALUE}")
  private Path database;


  @CommandLine.Option(names = {"--db"},
      defaultValue = "account",
      description = "db name")
  private List<String> dbNames;

  @CommandLine.Option(names = {"--print-meta"},
      defaultValue = "false",
      description = "print meta info")

  private boolean printMeta;
  @CommandLine.Option(names = {"--print-value-distribute"},
      defaultValue = "false",
      description = "print value distribute")
  private boolean printValueDistribute;

  @CommandLine.Option(names = {"-h", "--help"}, help = true, description = "display a help message")
  boolean help;

  SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
  //concurrentMap
  private ConcurrentMap<Integer,Integer > valueSizeMap = new ConcurrentHashMap<>();

  @Override
  public Integer call() throws Exception {
    if (help) {
      spec.commandLine().usage(System.out);
      return 0;
    }
    this.run();
    return 0;
  }

  private void run() {
    dbNames.stream().parallel().forEach(name -> {
      final Path sourcePath = Paths.get(database.toString(), name);
      logger.info("stat info ----: {}", sourcePath);
      spec.commandLine().getOut().println(
          String.format("%s stat info ----: %s", dateFormat.format(new Date()), sourcePath));
      try {
        printMeta(name);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      try (DB db = DBUtils.newLevelDb(sourcePath);
           DBIterator iterator = db.iterator(
               new org.iq80.leveldb.ReadOptions().fillCache(false))) {
        String stats = db.getProperty("leveldb.stats");
        double totalSize = getStats(stats);
        logger.info("DB size: {} M", totalSize);
        spec.commandLine().getOut().println(String.format("%s DB size: %s M",
            dateFormat.format(new Date()), totalSize));
        logger.info("{}", stats);
        spec.commandLine().getOut().println(stats);
        iterator.seekToFirst();
        long count = 0;
        double keySize = 0.0; // in M
        double valueSize = 0.0; // in M
        while (iterator.hasNext()) {
          Map.Entry<byte[], byte[]> entry = iterator.next();
          byte[] key = entry.getKey();
          keySize += key.length / 1024.0 / 1024;
          byte[] value = entry.getValue();
          if (printValueDistribute) {
            valueSizeMap.put(value.length, valueSizeMap.getOrDefault(value.length, 0) + 1);
          }
          valueSize += value.length / 1024.0 / 1024;
          count++;
        }
        logger.info("count: {}, key size: {} M, value size: {} M", count, keySize, valueSize);
        spec.commandLine().getOut().println(
            String.format("%s count: %s, key size: %s M, value size: %s M",
            dateFormat.format(new Date()), count, keySize, valueSize));
        if (printValueDistribute) {
          spec.commandLine().getOut()
              .println("--------------value size distribution------------------ ");
          //按数量从大到小输出，valueSizeMap
          valueSizeMap.entrySet().stream().sorted(Map.Entry.comparingByValue()).forEach(entry -> {
            spec.commandLine().getOut().println(entry.getKey() + " : " + entry.getValue());
          });
        }

      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
  }


  public double getStats(String stats) {
    return Arrays.stream(stats.split("\n"))
        .skip(3)
        .map(s -> s.trim().replaceAll(" +", ",").split(",")[2])
        .mapToLong(Long::parseLong)
        .sum();
  }

  private void printMeta(String dbName) throws IOException {
    if (!printMeta) {
      return;
    }
    Options options = DBUtils.newDefaultLevelDbOptions(dbName);
    File databaseDir = new File(database.toString(), dbName);
    int tableCacheSize = options.maxOpenFiles() - 10;
    InternalKeyComparator internalKeyComparator;
    //use custom comparator if set
    DBComparator comparator = options.comparator();
    UserComparator userComparator;
    if (comparator != null) {
      userComparator = new CustomUserComparator(comparator);
    } else {
      userComparator = new BytewiseComparator();
    }
    internalKeyComparator = new InternalKeyComparator(userComparator);
    TableCache tableCache = new TableCache(databaseDir, tableCacheSize,
        new InternalUserComparator(internalKeyComparator), options.verifyChecksums());
    VersionSet versions = new VersionSet(databaseDir, tableCache, options, internalKeyComparator);
    // load  (and recover) current version
    versions.recover();
    versions.getCurrent().getFiles().forEach(this::printFileMetaData);
  }

  private void printFileMetaData(long level, FileMetaData meta) {
    String small = ByteArray.toHexString(meta.getSmallest().getUserKey().getBytes());
    String max = ByteArray.toHexString(meta.getLargest().getUserKey().getBytes());
    long size = meta.getFileSize();
    long num = meta.getNumber();
    long seek = meta.getAllowedSeeks();
    spec.commandLine().getOut().format(
        "level: %d, num: %d, seek: %d, size: %d, small: %s, max: %s",
        level, num, seek, size, small, max).println();
    logger.info("level: {}, num: {}, seek: {}, size: {}, small: {}, max: {}",
        level, num, seek, size, small, max);
  }
}
