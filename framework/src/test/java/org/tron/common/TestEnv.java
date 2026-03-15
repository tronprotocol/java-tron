package org.tron.common;

import static org.junit.Assume.assumeFalse;

import java.util.Arrays;
import org.tron.common.arch.Arch;

public class TestEnv {

  /** Default test config: LEVELDB engine, minimal settings. */
  public static final String TEST_CONF = "config-test.conf";

  /** Production config: full mainnet settings, RocksDB tuning, 27 witnesses. */
  public static final String NET_CONF = "config.conf";

  /** Mainnet-like config: P2P connection management (maxConnections, minActive). */
  public static final String MAINNET_CONF = "config-test-mainnet.conf";

  /** DB backup config: RocksDB engine, backup enabled with frequency/path settings. */
  public static final String DBBACKUP_CONF = "config-test-dbbackup.conf";

  /** Local test config: custom port 6666, full HTTP/RPC services, single local witness. */
  public static final String LOCAL_CONF = "config-localtest.conf";

  /** Storage test config: per-database property tuning (compression, cache sizes). */
  public static final String STORAGE_CONF = "config-test-storagetest.conf";

  /** Index test config: minimal setup for transaction history index testing. */
  public static final String INDEX_CONF = "config-test-index.conf";

  /**
   * Skips the current test on ARM64 where LevelDB JNI is unavailable.
   */
  public static void assumeLevelDbAvailable() {
    assumeFalse("LevelDB JNI unavailable on ARM64", Arch.isArm64());
  }

  /**
   * Appends --storage-db-engine override if the system property tron.test.db.engine is set.
   * Used by Gradle testWithRocksDb task to run tests with RocksDB engine.
   */
  public static String[] withDbEngineOverride(String... args) {
    String engineOverride = System.getProperty("tron.test.db.engine");
    if (engineOverride != null) {
      String[] extra = {"--storage-db-engine", engineOverride};
      String[] result = Arrays.copyOf(args, args.length + extra.length);
      System.arraycopy(extra, 0, result, args.length, extra.length);
      return result;
    }
    return args;
  }
}
