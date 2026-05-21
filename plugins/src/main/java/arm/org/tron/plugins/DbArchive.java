package org.tron.plugins;

import java.util.concurrent.Callable;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.arch.Arch;
import picocli.CommandLine;
import picocli.CommandLine.Option;

/**
 *  ARM architecture only supports RocksDB,
 *  which does not require manifest rebuilding (manifest rebuilding is a LevelDB-only feature).
 *  This command is not supported but retained for compatibility.
 **/
@Slf4j(topic = "archive")
@CommandLine.Command(name = "archive", description = "A helper to rewrite leveldb manifest.")
public class DbArchive implements Callable<Integer> {

  @CommandLine.Spec
  CommandLine.Model.CommandSpec spec;
  @Option(names = {"-d", "--database-directory"},
      defaultValue = "output-directory/database",
      description = "java-tron database directory. Default: ${DEFAULT-VALUE}")
  private String databaseDirectory;

  @Option(names = {"-b", "--batch-size"},
      defaultValue = "80000",
      description = "deal manifest batch size. Default: ${DEFAULT-VALUE}")
  private int maxBatchSize;

  @Option(names = {"-m", "--manifest-size"},
      defaultValue = "0",
      description = "manifest min size(M) to archive. Default: ${DEFAULT-VALUE}")
  private int maxManifestSize;

  @Option(names = {"-h", "--help"})
  private boolean help;


  @Override
  public Integer call() throws Exception {
    String tips = String.format(
        "%s architecture only supports RocksDB, which does not require manifest rebuilding "
            + "(manifest rebuilding is a LevelDB-only feature).",
        Arch.getOsArch());
    spec.commandLine().getErr().println(spec.commandLine().getColorScheme().errorText(tips));
    logger.warn(tips);
    return 0;
  }

}
