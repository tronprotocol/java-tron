package org.tron.plugins;

import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBar;
import org.rocksdb.RocksDBException;
import org.tron.plugins.utils.db.DbTool;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@Slf4j(topic = "compact")
@CommandLine.Command(name = "compact", description = "A helper to compact db.")
public class DbCompact implements Callable<Integer> {

  @CommandLine.Spec
  CommandLine.Model.CommandSpec spec;

  @Option(names = {"-d", "--database-directory"},
      defaultValue = "output-directory/database",
      description = "java-tron database directory. Default: ${DEFAULT-VALUE}")
  private String databaseDirectory;

  @Option(names = { "--name"},
      description = "db name for compact. Default: ${DEFAULT-VALUE}")
  private String name;

  @Option(names = {"-h", "--help"})
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
        .filter(File::isDirectory).filter( file -> name == null || name.isEmpty() ||
            name.equals(file.getName())).collect(
            Collectors.toList());

    if (files.isEmpty()) {
      spec.commandLine().getErr().format("Directory %s does not contain any database.",
          databaseDirectory).println();
      logger.info("Directory {} does not contain any database.", databaseDirectory);
      return 0;
    }
    final long time = System.currentTimeMillis();
    List<Compact> services = new ArrayList<>();
    files.forEach(f -> services.add(new Compactor(databaseDirectory, f.getName())));
    ProgressBar.wrap(services.stream(), "compact task").parallel().forEach(Compact::doCompact);
    spec.commandLine().getOut().println("compact db done.");

    logger.info("DatabaseDirectory:{}, database compact use {} seconds total.",
        databaseDirectory, (System.currentTimeMillis() - time) / 1000);

    return 0;
  }


  interface Compact {
    default void doCompact() {
    }
  }

  static class Compactor implements Compact {

    private final Path srcDbPath;
    private final String name;
    private final long startTime;

    public Compactor(String src, String name) {
      this.name = name;
      this.srcDbPath = Paths.get(src);
      this.startTime = System.currentTimeMillis();
    }
    @Override
    public void doCompact() {
      try {
        DbTool.getDB(srcDbPath, name).compactRange();
      } catch (RocksDBException | IOException e) {
        throw new RuntimeException(e);
      }
      logger.info("Db {} compact use {} ms.", this.name, (System.currentTimeMillis() - startTime));
    }
  }
}