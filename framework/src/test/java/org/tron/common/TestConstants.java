package org.tron.common;

import static org.junit.Assume.assumeFalse;

import org.tron.common.arch.Arch;

/**
 * Centralized test environment constants and utilities.
 *
 * <h3>DB engine override for dual-engine testing</h3>
 * Gradle tasks ({@code test} on ARM64, {@code testWithRocksDb}) set the JVM system property
 * {@code -Dstorage.db.engine=ROCKSDB}. Because test config files are loaded from the classpath
 * via {@code ConfigFactory.load(fileName)}, Typesafe Config automatically merges system
 * properties with higher priority than the config file values. This means the config's
 * {@code storage.db.engine = "LEVELDB"} is overridden transparently, without any code changes
 * in individual tests.
 *
 * <p><b>IMPORTANT:</b> Config files MUST be classpath resources (in {@code src/test/resources/}),
 * NOT standalone files in the working directory. If a config file exists on disk,
 * {@code Configuration.getByFileName} falls back to {@code ConfigFactory.parseFile()},
 * which does NOT merge system properties, and the engine override will silently fail.
 */
public class TestConstants {

  public static final String TEST_CONF = "config-test.conf";
  public static final String SHIELD_CONF = "config-shield.conf";

  /**
   * Skips the current test on ARM64 where LevelDB JNI is unavailable.
   */
  public static void assumeLevelDbAvailable() {
    assumeFalse("LevelDB JNI unavailable on ARM64", Arch.isArm64());
  }
}
