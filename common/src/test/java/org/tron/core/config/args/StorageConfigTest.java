package org.tron.core.config.args;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
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
  public void testStateArchiveDefaultsAndOverrides() {
    StorageConfig defaults = StorageConfig.fromConfig(withRef());
    assertFalse(defaults.getStateArchive().isEnabled());
    assertEquals("state-archive", defaults.getStateArchive().getDirectory());
    assertEquals(1073741824L, defaults.getStateArchive().getMaxSegmentSize());
    assertEquals(256, defaults.getStateArchive().getQueueCapacity());
    assertEquals("ROCKSDB", defaults.getStateArchive().getServingIndexEngine());
    assertFalse(defaults.getStateArchive().getHotStore().isEnabled());
    assertEquals("ROCKSDB", defaults.getStateArchive().getHotStore().getEngine());
    assertEquals(10000L, defaults.getStateArchive().getHotStore().getMaxBlocks());
    assertEquals(2147483648L,
        defaults.getStateArchive().getHotStore().getMaxEncodedBytes());
    assertEquals(8, defaults.getStateArchive().getHotStore().getMaxFrozenGenerations());
    assertEquals(4, defaults.getStateArchive().getHotStore().getYellowFrozenGenerations());
    assertEquals(7, defaults.getStateArchive().getHotStore().getRedFrozenGenerations());
    assertFalse(defaults.getStateArchive().getAppendFile().isEnabled());
    assertEquals(3, defaults.getStateArchive().getAppendFile().getFormatVersion());
    assertEquals(2000000000L,
        defaults.getStateArchive().getAppendFile().getSegmentTargetBytes());

    StorageConfig configured = StorageConfig.fromConfig(withRef(
        "storage.stateArchive { enabled = true, directory = archive-test, "
            + "maxSegmentSize = 134217728, queueCapacity = 8, "
            + "servingIndexEngine = leveldb, hotStore.engine = leveldb }"));
    assertTrue(configured.getStateArchive().isEnabled());
    assertEquals("archive-test", configured.getStateArchive().getDirectory());
    assertEquals(134217728L, configured.getStateArchive().getMaxSegmentSize());
    assertEquals(8, configured.getStateArchive().getQueueCapacity());
    assertEquals("LEVELDB", configured.getStateArchive().getServingIndexEngine());
    assertEquals("LEVELDB", configured.getStateArchive().getHotStore().getEngine());
  }

  @Test
  public void testArchiveNativeDatabaseProfileDefaultsAndOverrides() {
    StorageConfig defaults = StorageConfig.fromConfig(withRef());
    assertEquals(67108864,
        defaults.getStateArchive().getServingIndex().getWriteBufferSize());
    assertEquals(33554432L,
        defaults.getStateArchive().getServingIndex().getCacheSize());
    assertNotSame(defaults.getStateArchive().getServingIndex(),
        defaults.getStateArchive().getHotStore().getDbSettings());
    assertEquals(16777216,
        defaults.getPathStateRoot().getDbSettings().getSmall().getWriteBufferSize());
    assertEquals(67108864,
        defaults.getPathStateRoot().getDbSettings().getGiant().getWriteBufferSize());
    assertEquals(67108864L,
        defaults.getPathStateRoot().getDbSettings().getGiant().getCacheSize());

    StorageConfig configured = StorageConfig.fromConfig(withRef(
        "storage.pathStateRoot.dbSettings.small.cacheSize = 1048576\n"
            + "storage.pathStateRoot.dbSettings.giant.maxOpenFiles = 321\n"
            + "storage.stateArchive.servingIndex.writeBufferSize = 8388608\n"
            + "storage.stateArchive.hotStore.dbSettings.writeBufferSize = 4194304"));
    assertEquals(1048576L,
        configured.getPathStateRoot().getDbSettings().getSmall().getCacheSize());
    assertEquals(321,
        configured.getPathStateRoot().getDbSettings().getGiant().getMaxOpenFiles());
    assertEquals(8388608,
        configured.getStateArchive().getServingIndex().getWriteBufferSize());
    assertEquals(4194304,
        configured.getStateArchive().getHotStore().getDbSettings().getWriteBufferSize());
    assertEquals(67108864,
        defaults.getStateArchive().getHotStore().getDbSettings().getWriteBufferSize());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testHotStoreRejectsInvalidFrozenWatermarks() {
    StorageConfig.fromConfig(withRef(
        "storage.stateArchive.hotStore.yellowFrozenGenerations = 7\n"
            + "storage.stateArchive.hotStore.redFrozenGenerations = 7"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testArchiveNativeDatabaseProfileRejectsInvalidValues() {
    StorageConfig.fromConfig(withRef(
        "storage.pathStateRoot.dbSettings.large.maxOpenFiles = 0"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testStateArchiveRejectsSmallSegments() {
    StorageConfig.fromConfig(withRef("storage.stateArchive.maxSegmentSize = 1024"));
  }

  @Test
  public void testCommonCheckpointDefaultsAndAdmission() {
    StorageConfig defaults = StorageConfig.fromConfig(withRef());
    assertFalse(defaults.getCommonCheckpoint().isEnabled());
    assertEquals("common-checkpoint", defaults.getCommonCheckpoint().getDirectory());

    StorageConfig configured = StorageConfig.fromConfig(withRef(
        "storage.stateArchive.enabled = true\n"
            + "storage.pathStateRoot.enabled = true\n"
            + "storage.commonCheckpoint { enabled = true, directory = common-test }"));
    assertTrue(configured.getCommonCheckpoint().isEnabled());
    assertEquals("common-test", configured.getCommonCheckpoint().getDirectory());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCommonCheckpointRequiresBothAuthorities() {
    StorageConfig.fromConfig(withRef("storage.commonCheckpoint.enabled = true"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testHotStoreRequiresCommonCheckpoint() {
    StorageConfig.fromConfig(withRef(
        "storage.stateArchive.enabled = true\n"
            + "storage.stateArchive.hotStore.enabled = true"));
  }

  @Test
  public void testHotStoreAdmitsOnlyWithCommonCheckpointAuthorities() {
    StorageConfig configured = StorageConfig.fromConfig(withRef(
        "storage.stateArchive.enabled = true\n"
            + "storage.stateArchive.hotStore.enabled = true\n"
            + "storage.pathStateRoot.enabled = true\n"
            + "storage.commonCheckpoint.enabled = true"));
    assertTrue(configured.getStateArchive().getHotStore().isEnabled());
  }

  @Test
  public void testAppendFileAdmitsOnlyWithCommonCheckpointAuthorities() {
    StorageConfig configured = StorageConfig.fromConfig(withRef(
        "storage.stateArchive.enabled = true\n"
            + "storage.stateArchive.appendFile.enabled = true\n"
            + "storage.pathStateRoot.enabled = true\n"
            + "storage.commonCheckpoint.enabled = true"));
    assertTrue(configured.getStateArchive().getAppendFile().isEnabled());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAppendFileRequiresCommonCheckpoint() {
    StorageConfig.fromConfig(withRef(
        "storage.stateArchive.enabled = true\n"
            + "storage.stateArchive.appendFile.enabled = true"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAppendFileRejectsHotStoreCombination() {
    StorageConfig.fromConfig(withRef(
        "storage.stateArchive.enabled = true\n"
            + "storage.stateArchive.appendFile.enabled = true\n"
            + "storage.stateArchive.hotStore.enabled = true\n"
            + "storage.pathStateRoot.enabled = true\n"
            + "storage.commonCheckpoint.enabled = true"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCommonCheckpointRejectsBenchmarkMode() {
    StorageConfig.fromConfig(withRef(
        "storage.stateArchive.enabled = true\n"
            + "storage.pathStateRoot.enabled = true\n"
            + "storage.pathStateRoot.volatileSnapshotBenchmark = true\n"
            + "storage.commonCheckpoint.enabled = true"));
  }

  @Test
  public void testPathStateRootDefaultsAndOverrides() {
    StorageConfig defaults = StorageConfig.fromConfig(withRef());
    assertFalse(defaults.getPathStateRoot().isEnabled());
    assertEquals("shadow", defaults.getPathStateRoot().getMode());
    assertEquals("path-state-root", defaults.getPathStateRoot().getDirectory());
    assertEquals("ROCKSDB", defaults.getPathStateRoot().getEngine());
    assertEquals(1, defaults.getPathStateRoot().getFormatVersion());
    assertEquals(128, defaults.getPathStateRoot().getReversibleLayerLimit());
    assertEquals(2147483648L, defaults.getPathStateRoot().getReversibleLayerBytes());
    assertEquals(268435456L, defaults.getPathStateRoot().getWriteBufferBytes());
    assertEquals(268435456L, defaults.getPathStateRoot().getNodeCacheBytes());
    assertEquals(4, defaults.getPathStateRoot().getParticipantThreads());
    assertEquals(8, defaults.getPathStateRoot().getBranchThreads());
    assertFalse(defaults.getPathStateRoot().isRebuildFromGenesis());
    assertTrue(defaults.getPathStateRoot().isVerifyEveryBlock());
    assertFalse(defaults.getPathStateRoot().isVolatileSnapshotBenchmark());
    assertFalse(defaults.getPathStateRoot().isAsyncPrepareBenchmark());

    StorageConfig configured = StorageConfig.fromConfig(withRef(
        "storage.pathStateRoot { enabled = true, engine = leveldb, mode = shadow, "
            + "directory = root-test, "
            + "formatVersion = 1, reversibleLayerLimit = 8, reversibleLayerBytes = 4096, "
            + "writeBufferBytes = 1024, nodeCacheBytes = 2048, participantThreads = 2, "
            + "branchThreads = 3, rebuildFromGenesis = false, "
            + "verifyEveryBlock = true, volatileSnapshotBenchmark = true, "
            + "asyncPrepareBenchmark = true }"));
    assertTrue(configured.getPathStateRoot().isEnabled());
    assertEquals("root-test", configured.getPathStateRoot().getDirectory());
    assertEquals("LEVELDB", configured.getPathStateRoot().getEngine());
    assertEquals(8, configured.getPathStateRoot().getReversibleLayerLimit());
    assertEquals(4096L, configured.getPathStateRoot().getReversibleLayerBytes());
    assertEquals(1024L, configured.getPathStateRoot().getWriteBufferBytes());
    assertEquals(2048L, configured.getPathStateRoot().getNodeCacheBytes());
    assertEquals(2, configured.getPathStateRoot().getParticipantThreads());
    assertEquals(3, configured.getPathStateRoot().getBranchThreads());
    assertTrue(configured.getPathStateRoot().isVolatileSnapshotBenchmark());
    assertTrue(configured.getPathStateRoot().isAsyncPrepareBenchmark());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testPathStateRootRejectsInvalidPrepareThreads() {
    StorageConfig.fromConfig(withRef("storage.pathStateRoot.participantThreads = 0"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testPathStateRootRejectsUnsupportedMode() {
    StorageConfig.fromConfig(withRef("storage.pathStateRoot.mode = consensus"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRejectsUnsupportedAuxiliaryDatabaseEngine() {
    StorageConfig.fromConfig(withRef("storage.stateArchive.servingIndexEngine = memory"));
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
