package org.tron.plugins;

import lombok.extern.slf4j.Slf4j;
import org.tron.common.arch.Arch;

/**
 *  ARM architecture only supports RocksDB,
 *  which does not require manifest rebuilding (manifest rebuilding is a LevelDB-only feature).
 *  This command is not supported but retained for compatibility.
 **/
@Slf4j(topic = "archive")
public class ArchiveManifest  {

  public static void main(String[] args) {
    int exitCode = run(args);
    System.exit(exitCode);
  }

  public static int run(String[] args) {
    String tips = String.format(
        "%s architecture only supports RocksDB, which does not require manifest rebuilding "
            + "(manifest rebuilding is a LevelDB-only feature).",
        Arch.getOsArch());
    System.out.println(tips);
    logger.warn(tips);
    return 0;
  }

}
