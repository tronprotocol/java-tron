package org.tron.plugins;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.tron.utils.db.DBInterface;
import org.tron.utils.db.DbTool;
import picocli.CommandLine;
import picocli.CommandLine.Option;

@Slf4j(topic = "stat")
@CommandLine.Command(name = "stat", sortOptions = false,
    description = "db stat info.")
public class DbStat implements Callable<Integer> {

  @CommandLine.Spec
  CommandLine.Model.CommandSpec spec;

  @Option(names = {"-d", "--db-path"},
      paramLabel = "FILE",
      required = true,
      description = "database directory")
  private String databaseDirectory;

  @Option(names = {"-h", "--help"}, help = true)
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
      logger.info("Directory {} does not exist.", databaseDirectory);
      return 404;
    }

    if (!dbDirectory.isDirectory()) {
      spec.commandLine().getErr().format(" %s is not directory.",
          databaseDirectory).println();
      logger.info("{} is not directory.", databaseDirectory);
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
      logger.info("{} is not a leveldb or rocksdb.", databaseDirectory);
      return 406;
    }

    DBInterface dbInterface = DbTool.getDB(databaseDirectory);
    DbTool.DbType type = DbTool.getDbType(databaseDirectory);
    List<String> stat = dbInterface.getStats();
    spec.commandLine().getOut().println("DB stat");
    spec.commandLine().getOut().println("type: " + type.toString());
    if (type.toString().equals("rocksdb")) {
      spec.commandLine().getOut().println(" Level  Files  Size(MB)");
      spec.commandLine().getOut().println("-----------------------");
    } else {
      spec.commandLine().getOut().println(
          "Compactions Level Files Size(MB) Time(sec) Read(MB) Write(MB)");
      spec.commandLine().getOut().println(
          "----------------------------------------------------");
    }
    for (String item: stat) {
      spec.commandLine().getOut().println(item);
    }
    return 0;
  }

}