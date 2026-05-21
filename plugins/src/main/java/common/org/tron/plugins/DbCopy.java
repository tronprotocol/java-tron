package org.tron.plugins;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBar;
import org.tron.plugins.utils.DBUtils;
import org.tron.plugins.utils.FileUtils;
import picocli.CommandLine;


@Slf4j(topic = "copy")
@CommandLine.Command(name = "cp", aliases = "copy",
    description = "Quick copy leveldb or rocksdb data.",
    exitCodeListHeading = "Exit Codes:%n",
    exitCodeList = {
        "0:Successful",
        "n:Internal error: exception occurred,please check toolkit.log"})
public class DbCopy implements Callable<Integer> {


  @CommandLine.Spec
  CommandLine.Model.CommandSpec spec;
  @CommandLine.Parameters(index = "0", defaultValue = "output-directory/database",
      description = "Input path. Default: ${DEFAULT-VALUE}")
  private File src;
  @CommandLine.Parameters(index = "1", defaultValue = "output-directory-cp/database",
      description = "Output path. Default: ${DEFAULT-VALUE}")
  private File dest;

  @CommandLine.Option(names = {"-h", "--help"})
  private boolean help;


  @Override
  public Integer call() throws Exception {
    if (help) {
      spec.commandLine().usage(System.out);
      return 0;
    }
    if (dest.exists()) {
      logger.info(" {} exist, please delete it first.", dest);
      spec.commandLine().getErr().println(spec.commandLine().getColorScheme()
          .errorText(String.format("%s exist, please delete it first.", dest)));
      return 402;
    }
    if (!src.exists()) {
      logger.info(" {} does not exist.", src);
      spec.commandLine().getErr().println(spec.commandLine().getColorScheme()
          .errorText(String.format("%s does not exist.", src)));
      return 404;
    }

    if (!src.isDirectory()) {
      logger.info(" {} is not a directory.", src);
      spec.commandLine().getErr().println(spec.commandLine().getColorScheme()
          .errorText(String.format("%s is not a directory.", src)));
      return 403;
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
    List<Copier> services = new ArrayList<>();
    files.forEach(f -> services.add(
        new DbCopier(src.getPath(), dest.getPath(), f.getName())));
    cpList.forEach(f -> services.add(
        new DbCopier(
            Paths.get(src.getPath(), DBUtils.CHECKPOINT_DB_V2).toString(),
            Paths.get(dest.getPath(), DBUtils.CHECKPOINT_DB_V2).toString(),
            f.getName())));
    List<String> fails = ProgressBar.wrap(services.stream(), "copy task").parallel().map(
        dbCopier -> {
          try {
            return dbCopier.doCopy() ? null : dbCopier.name();
          } catch (Exception e) {
            logger.error("{}", e);
            spec.commandLine().getErr().println(spec.commandLine().getColorScheme()
                .errorText(e.getMessage()));
            return dbCopier.name();
          }
        }).filter(Objects::nonNull).collect(Collectors.toList());
    // copy info.properties if lite need
    Arrays.stream(Objects.requireNonNull(src.listFiles()))
        .filter(File::isFile).forEach(f -> FileUtils.copy(Paths.get(src.toString(), f.getName()),
            Paths.get(dest.toString(), f.getName())));
    long during = (System.currentTimeMillis() - time) / 1000;
    spec.commandLine().getOut().format("copy db done, fails: %s, take %d s.",
        fails, during).println();
    logger.info("database copy use {} seconds total, fails: {}.", during, fails);
    return fails.size();
  }

  interface Copier {

    boolean doCopy();

    String name();
  }

  static class DbCopier implements Copier {
    private final String srcDir;
    private final String dstDir;
    private final String dbName;
    private final Path srcDbPath;
    private final Path dstDbPath;

    public DbCopier(String srcDir, String dstDir, String name) {
      this.srcDir = srcDir;
      this.dstDir = dstDir;
      this.dbName = name;
      this.srcDbPath = Paths.get(this.srcDir, name);
      this.dstDbPath = Paths.get(this.dstDir, name);
    }

    @Override
    public boolean doCopy() {
      File srcDb = srcDbPath.toFile();
      if (!srcDb.exists()) {
        logger.info(" {} does not exist.", srcDb);
        return true;
      }
      FileUtils.createDirIfNotExists(dstDir);
      logger.info("Copy database {} start", this.dbName);
      FileUtils.copyDir(Paths.get(srcDir), Paths.get(dstDir), dbName);
      logger.info("Copy database {} end", this.dbName);
      return true;
    }

    @Override
    public String name() {
      return dbName;
    }
  }

}
