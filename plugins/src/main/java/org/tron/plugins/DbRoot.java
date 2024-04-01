package org.tron.plugins;

import com.google.common.collect.Streams;
import com.google.common.primitives.Bytes;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBar;
import org.rocksdb.RocksDBException;
import org.tron.plugins.utils.MerkleRoot;
import org.tron.plugins.utils.Sha256Hash;
import org.tron.plugins.utils.db.DBInterface;
import org.tron.plugins.utils.db.DBIterator;
import org.tron.plugins.utils.db.DbTool;
import picocli.CommandLine;

@Slf4j(topic = "db-root")
@CommandLine.Command(name = "root",
    description = "compute merkle root for tiny db. NOTE: large db may GC overhead limit exceeded.",
    exitCodeListHeading = "Exit Codes:%n",
    exitCodeList = {
        "0:Successful",
        "n:query failed,please check toolkit.log"})
public class DbRoot implements Callable<Integer> {

  @CommandLine.Spec
  CommandLine.Model.CommandSpec spec;
  @CommandLine.Parameters(index = "0", defaultValue = "output-directory/database",
      description = "Input path. Default: ${DEFAULT-VALUE}")
  private Path db;

  @CommandLine.Option(names = { "--db"},
      description = "db name for show root")
  private List<String> dbs;

  @CommandLine.Option(names = {"-h", "--help"}, help = true, description = "display a help message")
  private boolean help;


  @Override
  public Integer call() throws Exception {
    if (help) {
      spec.commandLine().usage(System.out);
      return 0;
    }
    if (!db.toFile().exists()) {
      logger.info(" {} does not exist.", db);
      spec.commandLine().getErr().println(spec.commandLine().getColorScheme()
          .errorText(String.format("%s does not exist.", db)));
      return 404;
    }

    // remove not exit
    if (dbs != null) {
      dbs.removeIf(s -> !Paths.get(db.toString(), s).toFile().exists());
    }

    if (dbs == null || dbs.isEmpty()) {
      logger.info("Specify at least one exit database: --db dbName.");
      spec.commandLine().getErr().println(spec.commandLine().getColorScheme()
          .errorText("Specify at least one exit database: --db dbName."));
      return 404;
    }
    List<Ret> task = ProgressBar.wrap(dbs.stream(), "root task").parallel()
        .map(this::calcMerkleRoot).collect(Collectors.toList());
    task.forEach(this::printInfo);
    int code = (int) task.stream().filter(r -> r.code == 1).count();
    if (code > 0) {
      spec.commandLine().getErr().println(spec.commandLine().getColorScheme()
          .errorText("There are some errors, please check toolkit.log for detail."));
    }
    spec.commandLine().getOut().println("root task done.");
    return code;
  }

  private Ret calcMerkleRoot(String name) {
    Ret info = new Ret();
    try (DBInterface database = DbTool.getDB(this.db, name)) {
      DBIterator iterator = database.iterator();
      iterator.seekToFirst();
      ArrayList<Sha256Hash> ids = Streams.stream(iterator)
          .map(this::getHash)
          .collect(Collectors.toCollection(ArrayList::new));
      Sha256Hash root = MerkleRoot.root(ids);
      logger.info("db: {},root: {}", database.getName(), root);
      info.code = 0;
      info.msg = String.format("db: %s,root: %s", database.getName(), root);
    } catch (RocksDBException | IOException e) {
      logger.error("calc db {} fail", name, e);
      info.code = 1;
      info.msg = String.format("db: %s,fail: %s",
          name, e.getMessage());
    }
    return info;
  }

  private Sha256Hash getHash(Map.Entry<byte[], byte[]> entry) {
    return Sha256Hash.of(true,
        Bytes.concat(entry.getKey(), entry.getValue()));
  }

  private void printInfo(Ret ret) {
    if (ret.code == 0) {
      spec.commandLine().getOut().println(ret.msg);
    } else {
      spec.commandLine().getErr().println(spec.commandLine().getColorScheme()
          .errorText(ret.msg));
    }
  }

  private static class Ret {
    private int code;
    private String msg;
  }
}
