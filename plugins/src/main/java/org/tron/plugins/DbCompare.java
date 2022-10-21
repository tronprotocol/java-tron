package org.tron.plugins;

import com.google.common.collect.Lists;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBar;
import org.tron.common.utils.ByteArray;
import org.tron.utils.db.DBInterface;
import org.tron.utils.db.DBIterator;
import org.tron.utils.db.DbTool;
import picocli.CommandLine;
import picocli.CommandLine.Option;

@Slf4j(topic = "compare")
@CommandLine.Command(name = "compare", sortOptions = false,
    description = "check the consistency of db / db-set. \n"
        + "Demo: java -jar Toolkit.jar db check "
        + "-s ./srcDatabase -d ./destDatabase -rl -e common -e abi")
public class DbCompare implements Callable<Integer> {

  private static final String CHECKPOINT = "checkpoint";
  private static final String TMP = "tmp";
  private static final String MARKET_PAIR_PRICE_TO_ORDER = "market_pair_price_to_order";
  private static final String OUTPUT_DIR = "./compare_" + System.currentTimeMillis();  // dir name
  private static final String SUMMARY = "summary";  // file name

  @CommandLine.Spec
  CommandLine.Model.CommandSpec spec;

  @Option(names = {"-s", "--src-path"},
      order = 1,
      paramLabel = "PATH",
      required = true,
      description = "src database directory or parent directory")
  private String srcDir;

  @Option(names = {"-d", "--dest-path"},
      order = 2,
      paramLabel = "PATH",
      required = true,
      description = "dest database directory or parent directory")
  private String destDir;

  @Option(names = {"-r", "--recurse"},
      order = 3,
      description = "single db compare or multi dbs compare")
  private boolean recurse;

  @Option(names = {"-e", "--exclude"},
      order = 4,
      paramLabel = "dbName",
      description = "db which not need to compare")
  private List<String> excludeList = Lists.newArrayList();

  @Option(names = {"-l", "--detail"},
      order = 5,
      description = "whether print the detail of the difference")
  private boolean detail;

  @Option(names = {"-h", "--help"}, help = true, order = 6)
  private boolean help;

  @Override
  public Integer call() throws Exception {
    if (help) {
      spec.commandLine().usage(System.out);
      return 0;
    }

    File srcDirectory = new File(srcDir);
    File destDirectory = new File(destDir);
    if (!srcDirectory.exists()) {
      spec.commandLine().getErr().format("src directory %s does not exist.",
          srcDir).println();
      logger.error("src directory {} does not exist.", srcDir);
      return 404;
    }
    if (!destDirectory.exists()) {
      spec.commandLine().getErr().format("dest directory %s does not exist.",
          destDir).println();
      logger.error("dest directory {} does not exist.", destDir);
      return 404;
    }
    if (!srcDirectory.isDirectory()) {
      spec.commandLine().getErr().format(" %s is not directory.",
          srcDir).println();
      logger.error("{} is not directory.", srcDir);
      return 405;
    }
    if (!destDirectory.isDirectory()) {
      spec.commandLine().getErr().format(" %s is not directory.",
          destDir).println();
      logger.error("{} is not directory.", destDir);
      return 405;
    }

    List<String> srcDbs = Lists.newArrayList();
    List<String> destDbs = Lists.newArrayList();
    if (!recurse) {
      srcDbs.add(srcDir);
      destDbs.add(destDir);
    } else {
      excludeList.add(CHECKPOINT);
      excludeList.add(TMP);
      excludeList.add(MARKET_PAIR_PRICE_TO_ORDER);
      srcDbs = Arrays.stream(Objects.requireNonNull(srcDirectory.listFiles()))
          .filter(File::isDirectory)
          .filter(f -> !excludeList.contains(f.getName()))
          .map(File::getAbsolutePath)
          .collect(Collectors.toList());
      destDbs = Arrays.stream(Objects.requireNonNull(destDirectory.listFiles()))
          .filter(File::isDirectory)
          .filter(f -> !excludeList.contains(f.getName()))
          .map(File::getAbsolutePath)
          .collect(Collectors.toList());
      List<String> srcDbNames = srcDbs.stream().map(File::new)
          .map(File::getName).sorted().collect(Collectors.toList());
      List<String> destDbNames = destDbs.stream().map(File::new)
          .map(File::getName).sorted().collect(Collectors.toList());
      if (!srcDbNames.equals(destDbNames)) {
        spec.commandLine().getErr().format("dbs in src is not equal to dest.").println();
        logger.error("dbs in src is not equal to dest.");
        return 406;
      }
    }

    if (srcDbs.size() == 0) {
      return 0;
    }

    List<String> allDbs = Lists.newArrayList();
    allDbs.addAll(srcDbs);
    allDbs.addAll(destDbs);
    for (String db: allDbs) {
      if (!DbTool.isLevelOrRocksDB(db)) {
        spec.commandLine().getErr().format("not a db, path: %s", db).println();
        logger.error("not a db, path: {}", db);
        return 404;
      }
    }

    File output = new File(OUTPUT_DIR);
    if (!output.exists()) {
      Files.createDirectory(output.toPath());
    }

    final List<String> finalDestDbs = destDbs;
    List<String> diffDbs = Collections.synchronizedList(new ArrayList<>());
    ProgressBar.wrap(srcDbs.stream(), "compare task").parallel().forEach(db -> {
      try {
        DBInterface srcDb = DbTool.getDB(db);
        String dbName = new File(db).getName();
        String destDbPath = getDestDbPath(finalDestDbs, dbName);
        if (destDbPath == null) {
          spec.commandLine().getErr().format("dest db not exist, %s", destDbPath).println();
          logger.error("dest db not exist, {}.", destDbPath);
          return;
        }
        DBInterface destDb = DbTool.getDB(destDbPath);
        if (!compareDb(dbName, srcDb, destDb, detail)) {
          diffDbs.add(dbName);
        }
      } catch (Exception e) {
        spec.commandLine().getErr().format("compare db failed, db: %s, err: %s",
            db, e.getMessage()).println();
        logger.error("compare db failed, db: {}", db, e);
      }
    });

    String result = String.format("These dbs are not consistent: \n%s\n"
        + "more details in the compare dir.", diffDbs);
    writeSummary(Paths.get(OUTPUT_DIR, SUMMARY).toString(), result);
    spec.commandLine().getOut().format(result).println();

    DbTool.close();
    return 0;
  }

  private static synchronized void writeSummary(String file, String content)
      throws IOException {
    File f = new File(file);
    if (!f.exists()) {
      f.createNewFile();
    }
    FileWriter fw = new FileWriter(f.getAbsoluteFile(), true);
    BufferedWriter bw = new BufferedWriter(fw);
    bw.write(content);
    bw.flush();
    bw.close();
  }

  private String getDestDbPath(List<String> dbs, String name) {
    for (String db: dbs) {
      File f = new File(db);
      if (f.getName().equals(name)) {
        return db;
      }
    }
    return null;
  }

  /**
   * @return true if src/dest is equal
   */
  private static boolean compareDb(
      String name, DBInterface src, DBInterface dest, boolean details)
      throws IOException {
    String summary = "";
    long srcTotal = 0;
    long srcDiffCount = 0;
    long destTotal = 0;
    long destDiffCount = 0;

    BufferedWriter bw = null;
    if (details) {
      if (!new File(OUTPUT_DIR, "diff_detail").exists()) {
        new File(OUTPUT_DIR, "diff_detail").mkdirs();
      }
      File f = new File(Paths.get(OUTPUT_DIR, "diff_detail", name).toString());
      if (!f.exists()) {
        f.createNewFile();
      }
      FileWriter fw = new FileWriter(f.getAbsoluteFile());
      bw = new BufferedWriter(fw);
    }

    try (DBIterator sourceIterator = src.iterator();
         DBIterator destIterator = dest.iterator()) {
      long start = System.currentTimeMillis();
      for (sourceIterator.seekToFirst(); sourceIterator.hasNext(); sourceIterator.next()) {
        byte[] key = sourceIterator.getKey();
        byte[] value = sourceIterator.getValue();
        byte[] destvalue = dest.get(key);
        if (ByteArray.isEmpty(destvalue) && ByteArray.isEmpty(value)) {
          continue;
        }
        if (!Arrays.equals(value, destvalue)) {
          if (details) {
            String diffContent = String.format("--SRC-- key: %s, src value: %s, dest value: %s",
                ByteArray.toHexString(key), ByteArray.toHexString(value),
                ByteArray.toHexString(destvalue));
            bw.write(diffContent);
            bw.newLine();
          }
          srcDiffCount++;
        }
        srcTotal++;
      }

      for (destIterator.seekToFirst(); destIterator.hasNext(); destIterator.next()) {
        byte[] key = destIterator.getKey();
        byte[] value = destIterator.getValue();
        byte[] srcValue = src.get(key);
        if (ByteArray.isEmpty(srcValue) && ByteArray.isEmpty(value)) {
          continue;
        }
        if (!Arrays.equals(value, srcValue)) {
          if (details) {
            String diffContent = String.format("--DEST-- key: %s, dest value: %s, src value: %s",
                ByteArray.toHexString(key), ByteArray.toHexString(value),
                ByteArray.toHexString(srcValue));
            bw.write(diffContent);
            bw.newLine();
          }
          destDiffCount++;
        }
        destTotal++;
      }
      if (details) {
        bw.close();
      }
      summary = String.format("db: %s\n", name)
          + String.format("there are total [%d] diffs between src and dest db\n", srcDiffCount)
          + String.format("there are total [%d] diffs between dest and src db\n", destDiffCount)
          + String.format("there are total [%d] entry in src db\n", srcTotal)
          + String.format("there are total [%d] entry in dest db\n", destTotal)
          + String.format("cost: %d\n\n", System.currentTimeMillis() - start);

      writeSummary(Paths.get(OUTPUT_DIR, SUMMARY).toString(), summary);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return srcDiffCount == 0 & destDiffCount == 0;
  }
}