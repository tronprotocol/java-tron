package org.tron.core.config.args;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.util.List;
import org.junit.Test;
import org.tron.common.math.StrictMathWrapper;
import org.tron.core.config.args.StorageConfig.PropertyConfig;

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
    assertTrue(sc.isNeedToUpdateAsset());
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

  // ---- readProperties() ----

  private static List<PropertyConfig> props(String storageProperties) {
    return StorageConfig.fromConfig(withRef(storageProperties)).getProperties();
  }

  @Test
  public void testPropertiesDefaultEmpty() {
    // reference.conf sets storage.properties = []
    assertTrue(StorageConfig.fromConfig(withRef()).getProperties().isEmpty());
    assertTrue(props("storage.properties = []").isEmpty());
  }

  @Test
  public void testPropertiesNameAndPathOnly() {
    // All LevelDB options omitted: name/path set, the four boxed fields stay null so
    // they inherit the per-tier defaults applied later by newDefaultDbOptions.
    List<PropertyConfig> list = props(
        "storage.properties = [ { name = account, path = some_path } ]");
    assertEquals(1, list.size());
    PropertyConfig p = list.get(0);
    assertEquals("account", p.getName());
    assertEquals("some_path", p.getPath());
    assertNull(p.getBlockSize());
    assertNull(p.getWriteBufferSize());
    assertNull(p.getCacheSize());
    assertNull(p.getMaxOpenFiles());
  }

  @Test
  public void testPropertiesNameOnlyKeepsEmptyPath() {
    PropertyConfig p = props("storage.properties = [ { name = account } ]").get(0);
    assertEquals("account", p.getName());
    assertEquals("", p.getPath());
  }

  @Test
  public void testPropertiesFullOverrideParsed() {
    PropertyConfig p = props(
        "storage.properties = [ { name = foo, path = bar,"
        + " blockSize = 2, writeBufferSize = 3, cacheSize = 4, maxOpenFiles = 5 } ]").get(0);
    assertEquals(Integer.valueOf(2), p.getBlockSize());
    assertEquals(Integer.valueOf(3), p.getWriteBufferSize());
    assertEquals(Long.valueOf(4L), p.getCacheSize());
    assertEquals(Integer.valueOf(5), p.getMaxOpenFiles());
  }

  @Test
  public void testPropertiesPartialOverrideLeavesOthersNull() {
    // Only blockSize is set; the other three stay null (inherit defaults).
    PropertyConfig p = props(
        "storage.properties = [ { name = foo, path = bar, blockSize = 8192 } ]").get(0);
    assertEquals(Integer.valueOf(8192), p.getBlockSize());
    assertNull(p.getWriteBufferSize());
    assertNull(p.getCacheSize());
    assertNull(p.getMaxOpenFiles());
  }

  @Test
  public void testPropertiesMultipleEntriesInOrder() {
    List<PropertyConfig> list = props(
        "storage.properties = ["
        + " { name = first, path = p1 },"
        + " { name = second, path = p2, maxOpenFiles = 7 } ]");
    assertEquals(2, list.size());
    assertEquals("first", list.get(0).getName());
    assertNull(list.get(0).getMaxOpenFiles());
    assertEquals("second", list.get(1).getName());
    assertEquals(Integer.valueOf(7), list.get(1).getMaxOpenFiles());
  }

  @Test
  public void testPropertiesMissingNameKeepsEmpty() {
    // readProperties does not require name (validation is deferred to Storage); name stays "".
    PropertyConfig p = props("storage.properties = [ { path = bar } ]").get(0);
    assertEquals("", p.getName());
    assertEquals("bar", p.getPath());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testPropertiesInvalidIntegerRejected() {
    props("storage.properties = [ { name = foo, blockSize = not_a_number } ]");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testPropertiesInvalidLongRejected() {
    props("storage.properties = [ { name = foo, cacheSize = not_a_number } ]");
  }
}
