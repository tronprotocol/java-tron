package org.tron.plugins;

import com.google.common.primitives.Longs;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.ByteArray;
import org.tron.utils.db.DBInterface;
import org.tron.utils.db.DbTool;
import picocli.CommandLine;
import picocli.CommandLine.Option;

@Slf4j(topic = "query")
@CommandLine.Command(name = "query", sortOptions = false,
    description = "get entry from leveldb or rocksdb.")
public class DbQuery implements Callable<Integer> {

  @CommandLine.Spec
  CommandLine.Model.CommandSpec spec;

  @Option(names = {"-d", "--db-path"},
      order = 1,
      paramLabel = "PATH",
      required = true,
      description = "database directory")
  private String databaseDirectory;

  @Option(names = {"-k", "--key"},
      order = 2,
      required = true,
      description = "")
  private String key;

  @Option(names = {"-f", "--key-format"},
      order = 3,
      defaultValue = "utf8",
      description = "[utf8|hex]")
  private String keyFormat;

  @Option(names = {"-h", "--help"}, help = true, order = 4)
  private boolean help;

  @Override
  public Integer call() throws Exception {
    if (help) {
      spec.commandLine().usage(System.out);
      return 0;
    }

    File dbDirectory = new File(databaseDirectory);
    if (!dbDirectory.exists()) {
      spec.commandLine().getErr().format("Directory %s does not exist.",
          databaseDirectory).println();
      logger.error("Directory {} does not exist.", databaseDirectory);
      return 404;
    }

    if (!dbDirectory.isDirectory()) {
      spec.commandLine().getErr().format(" %s is not directory.",
          databaseDirectory).println();
      logger.error("{} is not directory.", databaseDirectory);
      return 405;
    }

    List<File> files = Arrays.stream(Objects.requireNonNull(dbDirectory.listFiles()))
        .collect(Collectors.toList());
    boolean isDbDir = false;
    for (File f: files) {
      if (f.getName().contains("MANIFEST")) {
        isDbDir = true;
        break;
      }
    }
    if (!isDbDir) {
      spec.commandLine().getErr().format(" %s is not a leveldb or rocksdb.",
          databaseDirectory).println();
      logger.error("{} is not a leveldb or rocksdb.", databaseDirectory);
      return 406;
    }

    if (!"utf8".equals(keyFormat) && !"hex".equals(keyFormat)) {
      spec.commandLine().getErr().format("key format illegal: %s", keyFormat).println();
      logger.error("key format illegal: {}", keyFormat);
      return 407;
    }

    DBInterface dbInterface = DbTool.getDB(databaseDirectory);

    byte[] keyBytes;
    if ("utf8".equals(keyFormat)) {
      keyBytes = ByteArray.fromString(key);
    } else {
      try {
        keyBytes = ByteArray.fromHexString(key);
      } catch (Exception e) {
        spec.commandLine().getErr().format("key is not a hex string: %s", key).println();
        logger.error("key is not a hex string: {}", key, e);
        return 408;
      }

    }

    byte[] value = dbInterface.get(keyBytes);
    spec.commandLine().getOut().println("key: " + key);
    spec.commandLine().getOut().println();
    spec.commandLine().getOut().println("value string: " + ByteArray.toStr(value));
    spec.commandLine().getOut().println("value hex string: " + ByteArray.toHexString(value));
    try {
      spec.commandLine().getOut().println("value long: " + Longs.fromByteArray(value));
    } catch (Exception e) {
      spec.commandLine().getOut().println("value long: " + "can not parse to long");
    }
    return 0;
  }

}