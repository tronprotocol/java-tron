package org.tron.core.config.args;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Test;
import org.tron.common.math.StrictMathWrapper;

public class StorageConfigTest {

  private static Config withRef(String hocon) {
    return ConfigFactory.parseString(hocon).withFallback(ConfigFactory.defaultReference());
  }

  private static Config withRef() {
    return ConfigFactory.defaultReference();
  }

  @Test
  public void testDefaults() {
    Config empty = withRef();
    StorageConfig sc = StorageConfig.fromConfig(empty);
    assertEquals("LEVELDB", sc.getDb().getEngine());
    assertFalse(sc.getDb().isSync());
    assertEquals("database", sc.getDb().getDirectory());
    assertEquals("index", sc.getIndex().getDirectory());
    assertTrue(sc.isNeedToUpdateAsset());
    assertFalse(sc.getBackup().isEnable());
    assertEquals(10000, sc.getBackup().getFrequency());
    assertEquals(7, sc.getDbSettings().getLevelNumber());
    assertEquals(5000, sc.getDbSettings().getMaxOpenFiles());
  }

  @Test
  public void testFromConfig() {
    Config config = withRef(
        "storage { db { engine = ROCKSDB, sync = true, directory = mydb },"
            + " backup { enable = true, frequency = 5000 },"
            + " dbSettings { levelNumber = 5, maxOpenFiles = 3000 } }");
    StorageConfig sc = StorageConfig.fromConfig(config);
    assertEquals("ROCKSDB", sc.getDb().getEngine());
    assertTrue(sc.getDb().isSync());
    assertEquals("mydb", sc.getDb().getDirectory());
    assertTrue(sc.getBackup().isEnable());
    assertEquals(5000, sc.getBackup().getFrequency());
    assertEquals(5, sc.getDbSettings().getLevelNumber());
    assertEquals(3000, sc.getDbSettings().getMaxOpenFiles());
  }

  @Test
  public void testCheckpointDefaults() {
    Config empty = withRef();
    StorageConfig sc = StorageConfig.fromConfig(empty);
    assertEquals(1, sc.getCheckpoint().getVersion());
    assertTrue(sc.getCheckpoint().isSync());
  }

  @Test
  public void testDbSettingsDefaults() {
    // These defaults must match develop's Args.initRocksDbSettings() fallbacks so that
    // nodes with minimal configs retain the same RocksDB tuning. See
    // docs/plans/2026-04-21-001-fix-reference-conf-default-drift.md.
    Config empty = withRef();
    StorageConfig sc = StorageConfig.fromConfig(empty);
    StorageConfig.DbSettingsConfig ds = sc.getDbSettings();
    assertEquals(7, ds.getLevelNumber());
    // compactThreads default is 0 in reference.conf, auto-expanded by postProcess()
    assertEquals(StrictMathWrapper.max(Runtime.getRuntime().availableProcessors(), 1),
        ds.getCompactThreads());
    assertEquals(16, ds.getBlocksize());
    assertEquals(256, ds.getMaxBytesForLevelBase());
    assertEquals(10, ds.getMaxBytesForLevelMultiplier(), 0.01);
    assertEquals(2, ds.getLevel0FileNumCompactionTrigger());
    assertEquals(64, ds.getTargetFileSizeBase());
    assertEquals(1, ds.getTargetFileSizeMultiplier());
    assertEquals(5000, ds.getMaxOpenFiles());
  }

  @Test
  public void testCompactThreadsAutoExpand() {
    // compactThreads = 0 must be auto-expanded to availableProcessors (min 1)
    Config config = withRef("storage { dbSettings { compactThreads = 0 } }");
    StorageConfig sc = StorageConfig.fromConfig(config);
    assertEquals(StrictMathWrapper.max(Runtime.getRuntime().availableProcessors(), 1),
        sc.getDbSettings().getCompactThreads());
  }

  @Test
  public void testCompactThreadsExplicitPreserved() {
    // Non-zero compactThreads must be passed through untouched
    Config config = withRef("storage { dbSettings { compactThreads = 7 } }");
    StorageConfig sc = StorageConfig.fromConfig(config);
    assertEquals(7, sc.getDbSettings().getCompactThreads());
  }

  @Test
  public void testBalanceHistoryLookup() {
    Config config = withRef(
        "storage { balance { history { lookup = true } } }");
    StorageConfig sc = StorageConfig.fromConfig(config);
    assertTrue(sc.getBalance().getHistory().isLookup());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSnapshotMaxFlushCountZeroRejected() {
    StorageConfig.fromConfig(withRef("storage.snapshot.maxFlushCount = 0"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSnapshotMaxFlushCountNegativeRejected() {
    StorageConfig.fromConfig(withRef("storage.snapshot.maxFlushCount = -1"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSnapshotMaxFlushCountOver500Rejected() {
    StorageConfig.fromConfig(withRef("storage.snapshot.maxFlushCount = 501"));
  }

  @Test
  public void testTxCacheEstimatedClampedBelowMin() {
    StorageConfig sc = StorageConfig.fromConfig(
        withRef("storage.txCache.estimatedTransactions = 50"));
    assertEquals(100, sc.getTxCache().getEstimatedTransactions());
  }

  @Test
  public void testTxCacheEstimatedClampedAboveMax() {
    StorageConfig sc = StorageConfig.fromConfig(
        withRef("storage.txCache.estimatedTransactions = 99999"));
    assertEquals(10000, sc.getTxCache().getEstimatedTransactions());
  }

  @Test
  public void testTxCacheEstimatedWithinRangePreserved() {
    StorageConfig sc = StorageConfig.fromConfig(
        withRef("storage.txCache.estimatedTransactions = 5000"));
    assertEquals(5000, sc.getTxCache().getEstimatedTransactions());
  }
}
