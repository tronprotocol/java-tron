package org.tron.core.db2.archive;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.hash.Hashing;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.storage.leveldb.LevelDbDataSourceImpl;
import org.tron.common.storage.rocksdb.RocksDbDataSourceImpl;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.config.args.Storage;
import org.tron.core.config.args.StorageConfig;
import org.tron.core.db.Manager;
import org.tron.core.db.TronDatabase;
import org.tron.core.db.common.DbSourceInter;
import org.tron.core.db2.ISession;
import org.tron.core.db2.archive.LatestStateGenerationAdapter.SnapshotCapableStore;
import org.tron.core.db2.archive.LatestStateGenerationAdapter.StoreSnapshot;
import org.tron.core.db2.archive.StateArchiveRuntimeOwner.ReadableStateStage;
import org.tron.core.db2.archive.StateArchiveRuntimeOwner.ServingIndexStage;
import org.tron.core.db2.archive.StateArchiveRuntimeOwner.State;
import org.tron.core.db2.common.DB;
import org.tron.core.db2.common.Flusher;
import org.tron.core.db2.common.LevelDB;
import org.tron.core.db2.common.RocksDB;
import org.tron.core.db2.common.WrappedByteArray;
import org.tron.core.db2.core.Chainbase;
import org.tron.core.db2.core.SnapshotManager;
import org.tron.core.db2.core.SnapshotRoot;
import org.tron.core.db2.stateroot.PathStateStoreManifest.Engine;
import org.tron.core.store.AccountAssetStore;
import org.tron.core.store.CheckTmpStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.protos.Protocol.Account;

public class StateArchiveManagerStartupIntegrationTest {

  private static final List<String> PARTICIPANTS = participants();

  static {
    org.rocksdb.RocksDB.loadLibrary();
  }

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void automaticOverflowFlushAdvancesHistoryAndAllStoreServingIndex() throws Exception {
    Path output = temporaryFolder.newFolder("overflow-manager").toPath();
    Path archive = output.resolve("state-archive");
    HistoryCommitMarker head = initializeRecoverableTail(archive, "ROCKSDB");
    SnapshotFixture fixture = snapshotFixture();
    SnapshotManager snapshots = fixture.snapshots;
    installRecoveredBinding(snapshots, archive, 6, 6);
    snapshots.setMaxSize(1);
    Manager manager = manager(snapshots, head);

    withArchiveConfig(output, "ROCKSDB", true, () -> invoke(manager, "initStateArchive"));

    byte[] key = new byte[]{1, 6, 1, 8};
    for (int epoch = 7; epoch <= 9; epoch++) {
      BlockSnapshotMeta target = new BlockSnapshotMeta(epoch, epoch, hash(epoch),
          hash(epoch - 1), epoch * 1_000L);
      try (ISession block = snapshots.buildSession()) {
        fixture.databases.get("proposal").put(key, new byte[]{(byte) epoch});
        block.commit(target);
      }
      if (epoch == 9) {
        BlockSnapshotMeta durable = new BlockSnapshotMeta(7, 7, hash(7), hash(6), 7_000L);
        assertEquals(durable, manager.getStateArchiveRuntime().verifyNormalWriteFixedPoint());
        assertServingFixedPoint(archive, durable, 5);
      }
    }

    invoke(manager, "closeStateArchive");
    snapshots.shutdown();
  }

  @Test
  public void batchedAutomaticOverflowFlushAdvancesHistoryAndAllStoreServingIndex()
      throws Exception {
    Path output = temporaryFolder.newFolder("batched-overflow-manager").toPath();
    Path archive = output.resolve("state-archive");
    HistoryCommitMarker head = initializeRecoverableTail(archive, "ROCKSDB");
    SnapshotFixture fixture = snapshotFixture();
    SnapshotManager snapshots = fixture.snapshots;
    installRecoveredBinding(snapshots, archive, 6, 6);
    snapshots.setMaxSize(1);
    snapshots.setMaxFlushCount(2);
    Manager manager = manager(snapshots, head);

    withArchiveConfig(output, "ROCKSDB", true, () -> invoke(manager, "initStateArchive"));

    byte[] key = new byte[]{1, 6, 1, 9};
    for (int epoch = 7; epoch <= 10; epoch++) {
      BlockSnapshotMeta target = new BlockSnapshotMeta(epoch, epoch, hash(epoch),
          hash(epoch - 1), epoch * 1_000L);
      try (ISession block = snapshots.buildSession()) {
        fixture.databases.get("proposal").put(key, new byte[]{(byte) epoch});
        block.commit(target);
      }
    }

    BlockSnapshotMeta durable = new BlockSnapshotMeta(8, 8, hash(8), hash(7), 8_000L);
    assertEquals(durable, manager.getStateArchiveRuntime().verifyNormalWriteFixedPoint());
    assertServingFixedPoint(archive, durable, 5);
    invoke(manager, "closeStateArchive");
    snapshots.shutdown();
  }

  @Test
  public void servingIndexPublicationFailuresRetryWithoutResubmittingHistoryAndRestartCleanly()
      throws Exception {
    for (ServingIndexStage failureStage : ServingIndexStage.values()) {
      Path output = temporaryFolder.newFolder(
          "serving-failure-" + failureStage.name().toLowerCase()).toPath();
      Path archive = output.resolve("state-archive");
      HistoryCommitMarker head = initializeRecoverableTail(archive, "ROCKSDB");
      SnapshotFixture fixture = snapshotFixture();
      SnapshotManager snapshots = fixture.snapshots;
      installRecoveredBinding(snapshots, archive, 6, 6);
      Manager manager = manager(snapshots, head);
      AtomicReference<ServingIndexStage> armed = new AtomicReference<>();
      setField(manager, "stateArchiveServingIndexFaultHook",
          (StateArchiveRuntimeOwner.ServingIndexFaultHook) stage -> {
            if (stage == armed.get() && armed.compareAndSet(stage, null)) {
              throw new IOException("injected serving-index failure at " + stage);
            }
          });
      withArchiveConfig(output, "ROCKSDB", true,
          () -> invoke(manager, "initStateArchive"));

      BlockSnapshotMeta target = new BlockSnapshotMeta(7, 7, hash(7), hash(6), 7_000L);
      try (ISession block = snapshots.buildSession()) {
        fixture.databases.get("proposal").put(new byte[]{7, 7}, new byte[]{7});
        block.commit(target);
      }
      setField(snapshots, "flushCount", 1);
      armed.set(failureStage);

      assertThrows(org.tron.core.exception.TronError.class, snapshots::flushPending);
      assertEquals(target, manager.getArchiveHistoryWriter().committedHeadMeta());
      assertEquals(6, snapshots.getArchiveReadableEpoch());
      if (failureStage == ServingIndexStage.CURRENT_PUBLISHED) {
        assertThrows(ArchivePersistenceException.class,
            () -> manager.getArchiveAccountBalance(6,
                new byte[HistoricalAccountBalanceReader.ADDRESS_LENGTH]));
      } else {
        assertFalse(manager.getArchiveAccountBalance(6,
            new byte[HistoricalAccountBalanceReader.ADDRESS_LENGTH]).isPresent());
      }
      assertThrows(ArchivePersistenceException.class, manager::inspectArchiveServingIndex);
      assertTrue(fixture.databases.values().stream().allMatch(database ->
          failureStage == ServingIndexStage.BEFORE_BUILD
              ? database.getHead() instanceof org.tron.core.db2.core.SnapshotImpl
              : database.getHead() instanceof SnapshotRoot));

      if (failureStage == ServingIndexStage.BEFORE_BUILD) {
        snapshots.flushPending();
        assertEquals(target, manager.getStateArchiveRuntime().verifyNormalWriteFixedPoint());
        assertEquals(target.getEpoch(), snapshots.getArchiveReadableEpoch());
        assertFalse(manager.getArchiveAccountBalance(7,
            new byte[HistoricalAccountBalanceReader.ADDRESS_LENGTH]).isPresent());
        assertServingFixedPoint(archive, target, 5);
        assertEquals(1, countGenerationDirectories(archive));
        assertTrue(fixture.databases.values().stream()
            .allMatch(database -> database.getHead() instanceof SnapshotRoot));
      }
      @SuppressWarnings("unchecked")
      ArgumentCaptor<Map<byte[], byte[]>> checkpoints = ArgumentCaptor.forClass(Map.class);
      verify(fixture.checkpoint, atLeastOnce()).updateByBatch(checkpoints.capture());
      Map<byte[], byte[]> recoveredCheckpoint = checkpoints.getAllValues()
          .get(checkpoints.getAllValues().size() - 1);

      invoke(manager, "closeStateArchive");
      snapshots.shutdown();

      SnapshotFixture restarted = snapshotFixture(recoveredCheckpoint);
      invokeCheckpointRecovery(restarted.snapshots, restarted.checkpoint);
      Manager restartedManager = manager(restarted.snapshots, target);
      withArchiveConfig(output, "ROCKSDB", true,
          () -> invoke(restartedManager, "initStateArchive"));
      assertEquals(target,
          restartedManager.getStateArchiveRuntime().verifyNormalWriteFixedPoint());
      assertServingFixedPoint(archive, target, 5);
      assertEquals(1, countGenerationDirectories(archive));
      invoke(restartedManager, "closeStateArchive");
      restarted.snapshots.shutdown();
    }
  }

  @Test
  public void managerBootstrapsFreshBaseAndContinuesNormalFlush() throws Exception {
    for (String engine : Arrays.asList("LEVELDB", "ROCKSDB")) {
      Path output = temporaryFolder.newFolder("fresh-manager-" + engine.toLowerCase()).toPath();
      Path archive = output.resolve("state-archive");
      BlockSnapshotMeta head = new BlockSnapshotMeta(6, 6, hash(6), hash(5), 6_000L);
      SnapshotFixture fixture = snapshotFixture();
      SnapshotManager snapshots = fixture.snapshots;
      Manager manager = manager(snapshots, head);

      withArchiveConfig(output, engine, true, () -> invoke(manager, "initStateArchive"));

      assertEquals(State.RUNNING, manager.getStateArchiveRuntime().getState());
      assertEquals(head, manager.getStateArchiveRuntime().getRecoveredHead());
      assertEquals(0, manager.getStateArchiveRuntime().getStartupRecoveryActionCount());
      assertServingFixedPoint(archive, head, 6);
      try (PersistentServingKeyIndexCatalog catalog =
          PersistentServingKeyIndexCatalog.open(archive.resolve("serving-index"));
          PersistentServingKeyIndexGeneration serving = catalog.pin()) {
        assertEquals(Engine.valueOf(engine), serving.getEngine());
      }
      assertEquals(head.getEpoch(), snapshots.getArchiveReadableEpoch());
      assertTrue(Files.isRegularFile(archive.resolve("MANIFEST")));
      assertFalse(Files.exists(archive.resolve("participants")));
      assertFalse(Files.exists(archive.resolve("progress")));

      byte[] key = new byte[]{2, 7, 1, 8};
      BlockSnapshotMeta target = new BlockSnapshotMeta(7, 7, hash(7), hash(6), 7_000L);
      try (ISession block = snapshots.buildSession()) {
        fixture.databases.get("proposal").put(key, new byte[]{7});
        block.commit(target);
      }
      setField(snapshots, "flushCount", 1);
      snapshots.flushPending();
      assertEquals(target, manager.getStateArchiveRuntime().verifyNormalWriteFixedPoint());
      assertServingFixedPoint(archive, target, 6);
      try (PersistentServingKeyIndexGeneration serving = manager.getArchiveHistoryWriter()
          .buildServingGeneration(output.resolve("serving-" + engine.toLowerCase()), "fresh")) {
        assertEquals(6, serving.getIndexedFrom());
        assertEquals(7, serving.getIndexedThrough());
      }

      invoke(manager, "closeStateArchive");
      assertEquals(-1, snapshots.getArchiveReadableEpoch());
      assertFalse(Files.exists(archive.resolve("participants")));
      assertFalse(Files.exists(archive.resolve("progress")));
      snapshots.shutdown();
    }
  }

  @Test
  public void managerReadsRequestOwnedHistoricalAccountsAndDrainsBeforeRestart()
      throws Exception {
    Path output = temporaryFolder.newFolder("historical-account-manager").toPath();
    Path archive = output.resolve("state-archive");
    BlockSnapshotMeta head = new BlockSnapshotMeta(6, 6, hash(6), hash(5), 6_000L);
    SnapshotFixture fixture = snapshotFixture();
    SnapshotManager snapshots = fixture.snapshots;
    Manager manager = manager(snapshots, head);
    withArchiveConfig(output, "ROCKSDB", true, () -> invoke(manager, "initStateArchive"));

    byte[] address = new byte[HistoricalAccountBalanceReader.ADDRESS_LENGTH];
    address[0] = 0x41;
    assertFalse(manager.getArchiveAccountBalance(6, address).isPresent());

    BlockSnapshotMeta target = null;
    for (int epoch = 7; epoch <= 9; epoch++) {
      target = new BlockSnapshotMeta(epoch, epoch, hash(epoch), hash(epoch - 1), epoch * 1_000L);
      try (ISession block = snapshots.buildSession()) {
        if (epoch == 7) {
          fixture.databases.get("account").put(address, account(address, 10));
        } else if (epoch == 8) {
          fixture.databases.get("account").put(address, account(address, 20));
        } else {
          fixture.databases.get("account").delete(address);
        }
        block.commit(target);
      }
      setField(snapshots, "flushCount", 1);
      snapshots.flushPending();
    }

    assertFalse(manager.getArchiveAccountBalance(6, address).isPresent());
    assertEquals(10, manager.getArchiveAccountBalance(7, address).getBalance());
    assertEquals(20, manager.getArchiveAccountBalance(8, address).getBalance());
    assertFalse(manager.getArchiveAccountBalance(9, address).isPresent());

    try (ArchiveRuntimeQueryGate.Lease lease =
        manager.getStateArchiveRuntime().pinHistoricalState(7)) {
      assertThrows(IllegalStateException.class, () -> invoke(manager, "closeStateArchive"));
      assertEquals(10,
          HistoricalAccountBalanceReader.read(lease.getSnapshot(), address).getBalance());
      assertThrows(IllegalStateException.class,
          () -> manager.getStateArchiveRuntime().pinHistoricalState(7));
    }
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<byte[], byte[]>> checkpoints = ArgumentCaptor.forClass(Map.class);
    verify(fixture.checkpoint, times(3)).updateByBatch(checkpoints.capture());
    Map<byte[], byte[]> recoveredCheckpoint = checkpoints.getAllValues().get(2);
    invoke(manager, "closeStateArchive");
    snapshots.shutdown();

    SnapshotFixture restarted = snapshotFixture(recoveredCheckpoint);
    invokeCheckpointRecovery(restarted.snapshots, restarted.checkpoint);
    Manager restartedManager = manager(restarted.snapshots, target);
    withArchiveConfig(output, "ROCKSDB", true,
        () -> invoke(restartedManager, "initStateArchive"));
    assertEquals(10, restartedManager.getArchiveAccountBalance(7, address).getBalance());
    assertEquals(20, restartedManager.getArchiveAccountBalance(8, address).getBalance());
    assertFalse(restartedManager.getArchiveAccountBalance(9, address).isPresent());
    invoke(restartedManager, "closeStateArchive");
    restarted.snapshots.shutdown();
  }

  @Test
  public void managerReadsExactPointHistoryAcrossEveryStateStoreAndRestart() throws Exception {
    Path output = temporaryFolder.newFolder("historical-exact-27-manager").toPath();
    Path archive = output.resolve("state-archive");
    BlockSnapshotMeta head = new BlockSnapshotMeta(6, 6, hash(6), hash(5), 6_000L);
    SnapshotFixture fixture = snapshotFixture();
    SnapshotManager snapshots = fixture.snapshots;
    Manager manager = manager(snapshots, head);
    seedExact27State(fixture.databases, 6);
    withArchiveConfig(output, "ROCKSDB", true, () -> invoke(manager, "initStateArchive"));

    assertExact27History(manager, 6);
    BlockSnapshotMeta target = null;
    for (int epoch = 7; epoch <= 8; epoch++) {
      target = new BlockSnapshotMeta(epoch, epoch, hash(epoch), hash(epoch - 1),
          epoch * 1_000L);
      try (ISession block = snapshots.buildSession()) {
        int storeIndex = 0;
        for (String dbName : PARTICIPANTS) {
          Chainbase database = fixture.databases.get(dbName);
          database.put(exact27Key(1, storeIndex), exact27Value(epoch, storeIndex));
          database.put(exact27Key(3, storeIndex), exact27Value(epoch, storeIndex));
          if (epoch == 7) {
            database.delete(exact27Key(4, storeIndex));
            database.put(exact27Key(5, storeIndex), new byte[0]);
            database.put(exact27Key(6, storeIndex), exact27Value(6, storeIndex));
          }
          storeIndex++;
        }
        block.commit(target);
      }
      setField(snapshots, "flushCount", 1);
      snapshots.flushPending();
      assertEquals(target, manager.getStateArchiveRuntime().verifyNormalWriteFixedPoint());
      assertExact27History(manager, epoch);
    }

    assertExact27History(manager, 6);
    assertExact27History(manager, 7);
    assertExact27ServingIndex(archive);
    assertThrows(IllegalArgumentException.class,
        () -> manager.getArchiveStateValue(8, "block", new byte[]{1}));
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<byte[], byte[]>> checkpoints = ArgumentCaptor.forClass(Map.class);
    verify(fixture.checkpoint, times(2)).updateByBatch(checkpoints.capture());
    Map<byte[], byte[]> recoveredCheckpoint = checkpoints.getAllValues().get(1);
    invoke(manager, "closeStateArchive");
    snapshots.shutdown();

    SnapshotFixture restarted = snapshotFixture(recoveredCheckpoint);
    Manager restartedManager = manager(restarted.snapshots, target);
    seedExact27State(restarted.databases, 8);
    invokeCheckpointRecovery(restarted.snapshots, restarted.checkpoint);
    withArchiveConfig(output, "ROCKSDB", true,
        () -> invoke(restartedManager, "initStateArchive"));
    assertExact27History(restartedManager, 6);
    assertExact27History(restartedManager, 7);
    assertExact27History(restartedManager, 8);
    assertExact27ServingIndex(archive);
    invoke(restartedManager, "closeStateArchive");
    restarted.snapshots.shutdown();
  }

  @Test
  public void managerResolvesP66AccountAssetHistoryAndRejectsInvalidLayoutsAfterRestart()
      throws Exception {
    Path output = temporaryFolder.newFolder("historical-p66-manager").toPath();
    BlockSnapshotMeta head = new BlockSnapshotMeta(6, 6, hash(6), hash(5), 6_000L);
    SnapshotFixture fixture = snapshotFixture();
    SnapshotManager snapshots = fixture.snapshots;
    Manager manager = manager(snapshots, head);
    byte[] address = archiveAddress(1);
    byte[] absentAddress = archiveAddress(2);
    byte[] orphanAddress = archiveAddress(3);
    byte[] mixedAddress = archiveAddress(4);
    String tokenId = "1000001";
    P66AccountAssetCodec codec = new P66AccountAssetCodec();
    byte[] directKey = codec.assetPhysicalKey(address, tokenId);
    byte[] orphanKey = codec.assetPhysicalKey(orphanAddress, tokenId);
    byte[] mixedKey = codec.assetPhysicalKey(mixedAddress, tokenId);
    fixture.databases.get("properties").put(
        HistoricalAccountAssetBalanceResolver.proposal66PhysicalKey(), longValue(0));
    fixture.databases.get("account").put(address,
        assetAccount(address, false, tokenId, 20));
    withArchiveConfig(output, "ROCKSDB", true, () -> invoke(manager, "initStateArchive"));

    assertAccountAsset(manager, 6, address, tokenId,
        P66AccountAssetCodec.Phase.P66_OFF, true, 20);
    assertAccountAsset(manager, 6, absentAddress, tokenId,
        P66AccountAssetCodec.Phase.P66_OFF, false, 0);

    BlockSnapshotMeta target = null;
    for (int epoch = 7; epoch <= 11; epoch++) {
      target = new BlockSnapshotMeta(epoch, epoch, hash(epoch), hash(epoch - 1),
          epoch * 1_000L);
      try (ISession block = snapshots.buildSession()) {
        if (epoch == 7) {
          fixture.databases.get("properties").put(
              HistoricalAccountAssetBalanceResolver.proposal66PhysicalKey(), longValue(1));
          fixture.databases.get("account").put(address,
              assetAccount(address, true, null, 0));
          fixture.databases.get("account-asset").put(directKey, longValue(30));
        } else if (epoch == 8) {
          fixture.databases.get("account-asset").put(directKey, longValue(40));
        } else if (epoch == 9) {
          fixture.databases.get("account").delete(address);
          fixture.databases.get("account-asset").delete(directKey);
        } else if (epoch == 10) {
          fixture.databases.get("account-asset").put(orphanKey, longValue(5));
          fixture.databases.get("account").put(mixedAddress,
              assetAccount(mixedAddress, false, tokenId, 7));
          fixture.databases.get("account-asset").put(mixedKey, longValue(7));
        } else {
          fixture.databases.get("account-asset").delete(orphanKey);
          fixture.databases.get("account").delete(mixedAddress);
          fixture.databases.get("account-asset").delete(mixedKey);
        }
        block.commit(target);
      }
      setField(snapshots, "flushCount", 1);
      snapshots.flushPending();
      assertEquals(target, manager.getStateArchiveRuntime().verifyNormalWriteFixedPoint());
    }

    assertP66History(manager, address, absentAddress, tokenId);
    assertThrows(ArchivePersistenceException.class,
        () -> manager.getArchiveAccountAssetBalance(10, orphanAddress, tokenId));
    assertThrows(ArchivePersistenceException.class,
        () -> manager.getArchiveAccountAssetBalance(10, mixedAddress, tokenId));
    assertThrows(ArchivePersistenceException.class,
        () -> manager.getArchiveAccountAssets(10, orphanAddress, prefixLimits()));
    assertThrows(ArchivePersistenceException.class,
        () -> manager.getArchiveAccountAssets(10, mixedAddress, prefixLimits()));
    assertAccountAsset(manager, 11, orphanAddress, tokenId,
        P66AccountAssetCodec.Phase.P66_ON, false, 0);
    assertAccountAsset(manager, 11, mixedAddress, tokenId,
        P66AccountAssetCodec.Phase.P66_ON, false, 0);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<byte[], byte[]>> checkpoints = ArgumentCaptor.forClass(Map.class);
    verify(fixture.checkpoint, times(5)).updateByBatch(checkpoints.capture());
    Map<byte[], byte[]> recoveredCheckpoint = checkpoints.getAllValues().get(4);
    invoke(manager, "closeStateArchive");
    snapshots.shutdown();

    SnapshotFixture restarted = snapshotFixture(recoveredCheckpoint);
    Manager restartedManager = manager(restarted.snapshots, target);
    restarted.databases.get("properties").put(
        HistoricalAccountAssetBalanceResolver.proposal66PhysicalKey(), longValue(1));
    invokeCheckpointRecovery(restarted.snapshots, restarted.checkpoint);
    withArchiveConfig(output, "ROCKSDB", true,
        () -> invoke(restartedManager, "initStateArchive"));
    assertP66History(restartedManager, address, absentAddress, tokenId);
    assertThrows(ArchivePersistenceException.class,
        () -> restartedManager.getArchiveAccountAssetBalance(10, orphanAddress, tokenId));
    assertThrows(ArchivePersistenceException.class,
        () -> restartedManager.getArchiveAccountAssetBalance(10, mixedAddress, tokenId));
    assertThrows(ArchivePersistenceException.class,
        () -> restartedManager.getArchiveAccountAssets(10, orphanAddress, prefixLimits()));
    assertThrows(ArchivePersistenceException.class,
        () -> restartedManager.getArchiveAccountAssets(10, mixedAddress, prefixLimits()));
    invoke(restartedManager, "closeStateArchive");
    restarted.snapshots.shutdown();
  }

  @Test
  public void managerProjectsP66HistoryFromNativeSupplementalAccountAssetAndRestart()
      throws Exception {
    for (String engine : Arrays.asList("LEVELDB", "ROCKSDB")) {
      Path output = temporaryFolder.newFolder(
          "supplemental-p66-" + engine.toLowerCase()).toPath();
      withArchiveConfig(output, engine, true,
          () -> runSupplementalP66Scenario(output, engine));
    }
  }

  @Test
  public void postRefreshFailuresReopenNativeSupplementalP66FixedPoint() throws Exception {
    for (String engine : Arrays.asList("LEVELDB", "ROCKSDB")) {
      for (ReadableStateStage failureStage : ReadableStateStage.values()) {
        Path output = temporaryFolder.newFolder("post-refresh-p66-"
            + engine.toLowerCase() + "-" + failureStage.name().toLowerCase()).toPath();
        withArchiveConfig(output, engine, true,
            () -> runPostRefreshP66Failure(output, engine, failureStage));
      }
    }
  }

  private void runPostRefreshP66Failure(Path output, String engine,
      ReadableStateStage failureStage) throws Exception {
    Path archive = output.resolve("state-archive");
    BlockSnapshotMeta base = new BlockSnapshotMeta(6, 6, hash(6), hash(5), 6_000L);
    SnapshotFixture fixture = snapshotFixtureWithoutAccountAsset(Collections.emptyMap());
    AtomicLong targetOptimization = new AtomicLong();
    AtomicReference<ReadableStateStage> armed = new AtomicReference<>();
    TestAccountAssetStore accountAssetStore = new TestAccountAssetStore();
    Manager manager = manager(fixture.snapshots, base, accountAssetStore, targetOptimization);
    setField(manager, "stateArchiveReadableStateFaultHook",
        (StateArchiveRuntimeOwner.ReadableStateFaultHook) stage -> {
          if (stage == armed.get() && armed.compareAndSet(stage, null)) {
            throw new IOException("injected readable-state failure at " + stage);
          }
        });
    byte[] address = archiveAddress(12);
    String tokenId = "1000012";
    byte[] directKey = new P66AccountAssetCodec().assetPhysicalKey(address, tokenId);
    fixture.databases.get("properties").put(
        HistoricalAccountAssetBalanceResolver.proposal66PhysicalKey(), longValue(0));
    fixture.databases.get("account").put(address,
        assetAccount(address, false, tokenId, 20));
    invoke(manager, "initStateArchive");

    targetOptimization.set(1);
    BlockSnapshotMeta activation = new BlockSnapshotMeta(7, 7, hash(7), hash(6), 7_000L);
    try (ISession block = fixture.snapshots.buildSession()) {
      fixture.databases.get("properties").put(
          HistoricalAccountAssetBalanceResolver.proposal66PhysicalKey(), longValue(1));
      fixture.databases.get("account").put(address,
          assetAccount(address, false, tokenId, 30));
      block.commit(activation);
    }
    setField(fixture.snapshots, "flushCount", 1);
    fixture.snapshots.flushPending();
    assertEquals(activation, manager.getStateArchiveRuntime().verifyNormalWriteFixedPoint());

    BlockSnapshotMeta target = new BlockSnapshotMeta(8, 8, hash(8), hash(7), 8_000L);
    try (ISession block = fixture.snapshots.buildSession()) {
      fixture.databases.get("account").put(address,
          assetAccount(address, true, tokenId, 40));
      block.commit(target);
    }
    setField(fixture.snapshots, "flushCount", 1);
    armed.set(failureStage);
    assertThrows(org.tron.core.exception.TronError.class, fixture.snapshots::flushPending);

    assertEquals(target, manager.getArchiveHistoryWriter().committedHeadMeta());
    assertEquals(activation.getEpoch(), fixture.snapshots.getArchiveReadableEpoch());
    assertArrayEquals(longValue(40), accountAssetStore.get(directKey));
    assertTrue(fixture.databases.values().stream()
        .allMatch(database -> database.getHead() instanceof SnapshotRoot));
    if (failureStage == ReadableStateStage.CANONICAL_REFRESHED) {
      assertAccountAsset(manager, 7, address, tokenId,
          P66AccountAssetCodec.Phase.P66_ON, true, 30);
      assertThrows(IllegalArgumentException.class,
          () -> manager.getArchiveAccountAssetBalance(8, address, tokenId));
    } else {
      assertThrows(ArchivePersistenceException.class,
          () -> manager.getArchiveAccountAssetBalance(7, address, tokenId));
      assertThrows(ArchivePersistenceException.class,
          () -> manager.getArchiveAccountAssetBalance(8, address, tokenId));
    }
    Map<String, byte[]> historyAuthority = historyAuthoritySnapshot(archive);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<byte[], byte[]>> checkpoints = ArgumentCaptor.forClass(Map.class);
    verify(fixture.checkpoint, times(2)).updateByBatch(checkpoints.capture());
    Map<byte[], byte[]> recoveredCheckpoint = checkpoints.getAllValues().get(1);
    invoke(manager, "closeStateArchive");
    fixture.snapshots.shutdown();
    accountAssetStore.getDbSource().closeDB();
    assertHistoryAuthorityEquals(historyAuthority, archive);

    SnapshotFixture restarted = snapshotFixtureWithoutAccountAsset(recoveredCheckpoint);
    TestAccountAssetStore reopenedAccountAssetStore = new TestAccountAssetStore();
    Manager restartedManager = manager(restarted.snapshots, target, reopenedAccountAssetStore,
        targetOptimization);
    invokeCheckpointRecovery(restarted.snapshots, restarted.checkpoint);
    invoke(restartedManager, "initStateArchive");
    assertEquals(0, restartedManager.getStateArchiveRuntime().getStartupRecoveryActionCount());
    assertEquals(target,
        restartedManager.getStateArchiveRuntime().verifyNormalWriteFixedPoint());
    assertAccountAsset(restartedManager, 6, address, tokenId,
        P66AccountAssetCodec.Phase.P66_OFF, true, 20);
    assertAccountAsset(restartedManager, 7, address, tokenId,
        P66AccountAssetCodec.Phase.P66_ON, true, 30);
    assertAccountAsset(restartedManager, 8, address, tokenId,
        P66AccountAssetCodec.Phase.P66_ON, true, 40);
    assertArrayEquals(longValue(40), reopenedAccountAssetStore.get(directKey));
    assertEquals(1, countGenerationDirectories(archive));
    assertHistoryAuthorityEquals(historyAuthority, archive);

    ArchiveRuntimeQueryGate.Lease active =
        restartedManager.getStateArchiveRuntime().pinHistoricalState(8);
    assertThrows(IllegalStateException.class,
        () -> invoke(restartedManager, "closeStateArchive"));
    active.close();
    invoke(restartedManager, "closeStateArchive");
    restarted.snapshots.shutdown();
    reopenedAccountAssetStore.getDbSource().closeDB();

    SnapshotFixture secondRestart = snapshotFixtureWithoutAccountAsset(recoveredCheckpoint);
    TestAccountAssetStore secondAccountAssetStore = new TestAccountAssetStore();
    Manager secondManager = manager(secondRestart.snapshots, target, secondAccountAssetStore,
        targetOptimization);
    invokeCheckpointRecovery(secondRestart.snapshots, secondRestart.checkpoint);
    invoke(secondManager, "initStateArchive");
    assertEquals(0, secondManager.getStateArchiveRuntime().getStartupRecoveryActionCount());
    assertEquals(target, secondManager.getStateArchiveRuntime().verifyNormalWriteFixedPoint());
    assertAccountAsset(secondManager, 8, address, tokenId,
        P66AccountAssetCodec.Phase.P66_ON, true, 40);
    assertHistoryAuthorityEquals(historyAuthority, archive);
    assertEquals("LEVELDB".equals(engine), secondAccountAssetStore.getDbSource()
        instanceof org.tron.common.storage.leveldb.LevelDbDataSourceImpl);
    assertEquals("ROCKSDB".equals(engine), secondAccountAssetStore.getDbSource()
        instanceof org.tron.common.storage.rocksdb.RocksDbDataSourceImpl);
    invoke(secondManager, "closeStateArchive");
    secondRestart.snapshots.shutdown();
    secondAccountAssetStore.getDbSource().closeDB();
  }

  @Test
  public void allNativeExact27StoresReopenWithStableIdentityAndHistory() throws Exception {
    for (String engine : Arrays.asList("LEVELDB", "ROCKSDB")) {
      Path output = temporaryFolder.newFolder("all-native-exact27-"
          + engine.toLowerCase()).toPath();
      withArchiveConfig(output, engine, true,
          () -> runAllNativeExact27Scenario(output, engine));
    }
  }

  private void runAllNativeExact27Scenario(Path output, String engine) throws Exception {
    Path archive = output.resolve("state-archive");
    BlockSnapshotMeta base = new BlockSnapshotMeta(6, 6, hash(6), hash(5), 6_000L);
    SnapshotFixture fixture = nativeSnapshotFixture(output, engine, Collections.emptyMap());
    AtomicLong targetOptimization = new AtomicLong();
    TestAccountAssetStore accountAssetStore = new TestAccountAssetStore();
    Manager manager = manager(fixture.snapshots, base, accountAssetStore, targetOptimization);
    byte[] address = archiveAddress(13);
    String tokenId = "1000013";
    byte[] directKey = new P66AccountAssetCodec().assetPhysicalKey(address, tokenId);
    seedNativeExact27(fixture.databases, 6);
    fixture.databases.get("properties").put(
        HistoricalAccountAssetBalanceResolver.proposal66PhysicalKey(), longValue(0));
    fixture.databases.get("account").put(address,
        assetAccount(address, false, tokenId, 20));
    Map<String, String> sourceIdentities = sourceIdentities(fixture, accountAssetStore);
    invoke(manager, "initStateArchive");

    BlockSnapshotMeta target = null;
    for (int epoch = 7; epoch <= 8; epoch++) {
      targetOptimization.set(1);
      target = new BlockSnapshotMeta(epoch, epoch, hash(epoch), hash(epoch - 1),
          epoch * 1_000L);
      try (ISession block = fixture.snapshots.buildSession()) {
        mutateNativeExact27(fixture.databases, epoch);
        if (epoch == 7) {
          fixture.databases.get("properties").put(
              HistoricalAccountAssetBalanceResolver.proposal66PhysicalKey(), longValue(1));
          fixture.databases.get("account").put(address,
              assetAccount(address, false, tokenId, 30));
        } else {
          fixture.databases.get("account").put(address,
              assetAccount(address, true, tokenId, 40));
        }
        block.commit(target);
      }
      setField(fixture.snapshots, "flushCount", 1);
      fixture.snapshots.flushPending();
      assertEquals(target, manager.getStateArchiveRuntime().verifyNormalWriteFixedPoint());
    }

    assertNativeExact27History(manager, address, directKey);
    assertAccountAsset(manager, 6, address, tokenId,
        P66AccountAssetCodec.Phase.P66_OFF, true, 20);
    assertAccountAsset(manager, 7, address, tokenId,
        P66AccountAssetCodec.Phase.P66_ON, true, 30);
    assertAccountAsset(manager, 8, address, tokenId,
        P66AccountAssetCodec.Phase.P66_ON, true, 40);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<byte[], byte[]>> checkpoints = ArgumentCaptor.forClass(Map.class);
    verify(fixture.checkpoint, times(2)).updateByBatch(checkpoints.capture());
    Map<byte[], byte[]> recoveredCheckpoint = checkpoints.getAllValues().get(1);
    invoke(manager, "closeStateArchive");
    fixture.snapshots.shutdown();
    closeNativeStores(fixture);
    accountAssetStore.getDbSource().closeDB();

    SnapshotFixture restarted = nativeSnapshotFixture(output, engine, recoveredCheckpoint);
    TestAccountAssetStore reopenedAccountAssetStore = new TestAccountAssetStore();
    assertEquals(sourceIdentities, sourceIdentities(restarted, reopenedAccountAssetStore));
    Manager restartedManager = manager(restarted.snapshots, target, reopenedAccountAssetStore,
        targetOptimization);
    invokeCheckpointRecovery(restarted.snapshots, restarted.checkpoint);
    invoke(restartedManager, "initStateArchive");
    assertEquals(0, restartedManager.getStateArchiveRuntime().getStartupRecoveryActionCount());
    assertEquals(target,
        restartedManager.getStateArchiveRuntime().verifyNormalWriteFixedPoint());
    assertNativeExact27History(restartedManager, address, directKey);
    assertAccountAsset(restartedManager, 6, address, tokenId,
        P66AccountAssetCodec.Phase.P66_OFF, true, 20);
    assertAccountAsset(restartedManager, 7, address, tokenId,
        P66AccountAssetCodec.Phase.P66_ON, true, 30);
    assertAccountAsset(restartedManager, 8, address, tokenId,
        P66AccountAssetCodec.Phase.P66_ON, true, 40);
    invoke(restartedManager, "closeStateArchive");
    restarted.snapshots.shutdown();
    closeNativeStores(restarted);
    reopenedAccountAssetStore.getDbSource().closeDB();

    SnapshotFixture secondRestart = nativeSnapshotFixture(output, engine, recoveredCheckpoint);
    TestAccountAssetStore secondAccountAssetStore = new TestAccountAssetStore();
    assertEquals(sourceIdentities, sourceIdentities(secondRestart, secondAccountAssetStore));
    Manager secondManager = manager(secondRestart.snapshots, target, secondAccountAssetStore,
        targetOptimization);
    invokeCheckpointRecovery(secondRestart.snapshots, secondRestart.checkpoint);
    invoke(secondManager, "initStateArchive");
    assertEquals(0, secondManager.getStateArchiveRuntime().getStartupRecoveryActionCount());
    assertEquals(target, secondManager.getStateArchiveRuntime().verifyNormalWriteFixedPoint());
    assertNativeExact27History(secondManager, address, directKey);
    invoke(secondManager, "closeStateArchive");
    secondRestart.snapshots.shutdown();
    closeNativeStores(secondRestart);
    secondAccountAssetStore.getDbSource().closeDB();
  }

  private void runSupplementalP66Scenario(Path output, String engine) throws Exception {
    Path archive = output.resolve("state-archive");
    BlockSnapshotMeta head = new BlockSnapshotMeta(6, 6, hash(6), hash(5), 6_000L);
    SnapshotFixture fixture = snapshotFixtureWithoutAccountAsset(Collections.emptyMap());
    AtomicLong targetOptimization = new AtomicLong();
    TestAccountAssetStore accountAssetStore = new TestAccountAssetStore();
    Manager manager = manager(fixture.snapshots, head, accountAssetStore, targetOptimization);
    byte[] address = archiveAddress(11);
    String tokenId = "1000011";
    byte[] directKey = new P66AccountAssetCodec().assetPhysicalKey(address, tokenId);
    fixture.databases.get("properties").put(
        HistoricalAccountAssetBalanceResolver.proposal66PhysicalKey(), longValue(0));
    fixture.databases.get("account").put(address,
        assetAccount(address, false, tokenId, 20));
    invoke(manager, "initStateArchive");
    assertAccountAsset(manager, 6, address, tokenId,
        P66AccountAssetCodec.Phase.P66_OFF, true, 20);
    assertAccountAssetPrefix(manager, 6, address, tokenId,
        P66AccountAssetCodec.Phase.P66_OFF, true, 20);
    assertFalse(accountAssetStore.has(directKey));

    BlockSnapshotMeta target = null;
    for (int epoch = 7; epoch <= 9; epoch++) {
      targetOptimization.set(1);
      target = new BlockSnapshotMeta(epoch, epoch, hash(epoch), hash(epoch - 1),
          epoch * 1_000L);
      if (epoch == 8) {
        accountAssetStore.failNextPrefixQuery();
        BlockSnapshotMeta failedTarget = target;
        assertThrows(ArchivePersistenceException.class, () -> {
          try (ISession block = fixture.snapshots.buildSession()) {
            fixture.databases.get("account").put(address,
                assetAccount(address, true, tokenId, 40));
            block.commit(failedTarget);
          }
        });
        assertEquals(7, manager.getArchiveHistoryWriter().committedHeadMeta().getEpoch());
        assertEquals(7, fixture.snapshots.getArchiveReadableEpoch());
        assertArrayEquals(longValue(30), accountAssetStore.get(directKey));
      }
      try (ISession block = fixture.snapshots.buildSession()) {
        if (epoch == 7) {
          fixture.databases.get("properties").put(
              HistoricalAccountAssetBalanceResolver.proposal66PhysicalKey(), longValue(1));
          fixture.databases.get("account").put(address,
              assetAccount(address, false, tokenId, 30));
        } else if (epoch == 8) {
          fixture.databases.get("account").put(address,
              assetAccount(address, true, tokenId, 40));
        } else {
          fixture.databases.get("account").delete(address);
        }
        block.commit(target);
      }
      setField(fixture.snapshots, "flushCount", 1);
      fixture.snapshots.flushPending();
      assertEquals(target, manager.getStateArchiveRuntime().verifyNormalWriteFixedPoint());
      if (epoch < 9) {
        assertArrayEquals(longValue(epoch == 7 ? 30 : 40), accountAssetStore.get(directKey));
        assertAccountAssetPrefix(manager, epoch, address, tokenId,
            P66AccountAssetCodec.Phase.P66_ON, true, epoch == 7 ? 30 : 40);
      } else {
        assertFalse(accountAssetStore.has(directKey));
      }
    }

    assertAccountAsset(manager, 6, address, tokenId,
        P66AccountAssetCodec.Phase.P66_OFF, true, 20);
    assertAccountAsset(manager, 7, address, tokenId,
        P66AccountAssetCodec.Phase.P66_ON, true, 30);
    assertAccountAsset(manager, 8, address, tokenId,
        P66AccountAssetCodec.Phase.P66_ON, true, 40);
    assertAccountAsset(manager, 9, address, tokenId,
        P66AccountAssetCodec.Phase.P66_ON, false, 0);
    assertAccountAssetPrefix(manager, 6, address, tokenId,
        P66AccountAssetCodec.Phase.P66_OFF, true, 20);
    assertAccountAssetPrefix(manager, 7, address, tokenId,
        P66AccountAssetCodec.Phase.P66_ON, true, 30);
    assertAccountAssetPrefix(manager, 8, address, tokenId,
        P66AccountAssetCodec.Phase.P66_ON, true, 40);
    assertAccountAssetPrefix(manager, 9, address, tokenId,
        P66AccountAssetCodec.Phase.P66_ON, false, 0);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<byte[], byte[]>> checkpoints = ArgumentCaptor.forClass(Map.class);
    verify(fixture.checkpoint, times(3)).updateByBatch(checkpoints.capture());
    Map<byte[], byte[]> recoveredCheckpoint = checkpoints.getAllValues().get(2);
    Path legacyShadow = output.resolve("legacy-v3-shadow");
    try (PersistentServingKeyIndexGeneration ignored = manager.getArchiveHistoryWriter()
        .buildServingGeneration(legacyShadow, "legacy-v3")) {
      // Published after the runtime releases its catalog ownership.
    }
    downgradeGenerationToV3(legacyShadow);
    invoke(manager, "closeStateArchive");
    fixture.snapshots.shutdown();
    accountAssetStore.getDbSource().closeDB();
    assertEquals(1, countGenerationDirectories(archive));
    try (PersistentServingKeyIndexCatalog catalog =
        PersistentServingKeyIndexCatalog.open(archive.resolve("serving-index"))) {
      assertTrue(catalog.publish(catalog.getCurrentGenerationId(), legacyShadow));
    }

    SnapshotFixture interrupted = snapshotFixtureWithoutAccountAsset(recoveredCheckpoint);
    TestAccountAssetStore interruptedAccountAssetStore = new TestAccountAssetStore();
    Manager interruptedManager = manager(interrupted.snapshots, target,
        interruptedAccountAssetStore, targetOptimization);
    interrupted.databases.get("properties").put(
        HistoricalAccountAssetBalanceResolver.proposal66PhysicalKey(), longValue(1));
    invokeCheckpointRecovery(interrupted.snapshots, interrupted.checkpoint);
    AtomicReference<ServingIndexStage> upgradeFailure =
        new AtomicReference<>(ServingIndexStage.GENERATION_INSTALLED);
    setField(interruptedManager, "stateArchiveServingIndexFaultHook",
        (StateArchiveRuntimeOwner.ServingIndexFaultHook) stage -> {
          if (stage == upgradeFailure.get() && upgradeFailure.compareAndSet(stage, null)) {
            throw new IOException("injected v3 range-index upgrade failure");
          }
        });
    assertThrows(IllegalStateException.class,
        () -> invoke(interruptedManager, "initStateArchive"));
    assertNull(interruptedManager.getStateArchiveRuntime());
    interrupted.snapshots.shutdown();
    interruptedAccountAssetStore.getDbSource().closeDB();
    assertEquals(2, countGenerationDirectories(archive));

    SnapshotFixture restarted = snapshotFixtureWithoutAccountAsset(recoveredCheckpoint);
    targetOptimization.set(1);
    TestAccountAssetStore reopenedAccountAssetStore = new TestAccountAssetStore();
    Manager restartedManager = manager(restarted.snapshots, target, reopenedAccountAssetStore,
        targetOptimization);
    restarted.databases.get("properties").put(
        HistoricalAccountAssetBalanceResolver.proposal66PhysicalKey(), longValue(1));
    invokeCheckpointRecovery(restarted.snapshots, restarted.checkpoint);
    invoke(restartedManager, "initStateArchive");
    assertAccountAsset(restartedManager, 6, address, tokenId,
        P66AccountAssetCodec.Phase.P66_OFF, true, 20);
    assertAccountAsset(restartedManager, 7, address, tokenId,
        P66AccountAssetCodec.Phase.P66_ON, true, 30);
    assertAccountAsset(restartedManager, 8, address, tokenId,
        P66AccountAssetCodec.Phase.P66_ON, true, 40);
    assertAccountAsset(restartedManager, 9, address, tokenId,
        P66AccountAssetCodec.Phase.P66_ON, false, 0);
    assertAccountAssetPrefix(restartedManager, 6, address, tokenId,
        P66AccountAssetCodec.Phase.P66_OFF, true, 20);
    assertAccountAssetPrefix(restartedManager, 7, address, tokenId,
        P66AccountAssetCodec.Phase.P66_ON, true, 30);
    assertAccountAssetPrefix(restartedManager, 8, address, tokenId,
        P66AccountAssetCodec.Phase.P66_ON, true, 40);
    assertAccountAssetPrefix(restartedManager, 9, address, tokenId,
        P66AccountAssetCodec.Phase.P66_ON, false, 0);
    assertFalse(reopenedAccountAssetStore.has(directKey));
    assertServingGenerationSupportsRange(archive);
    assertEquals("LEVELDB".equals(engine), reopenedAccountAssetStore.getDbSource()
        instanceof org.tron.common.storage.leveldb.LevelDbDataSourceImpl);
    assertEquals("ROCKSDB".equals(engine), reopenedAccountAssetStore.getDbSource()
        instanceof org.tron.common.storage.rocksdb.RocksDbDataSourceImpl);
    invoke(restartedManager, "closeStateArchive");
    restarted.snapshots.shutdown();
    reopenedAccountAssetStore.getDbSource().closeDB();
  }

  @Test
  public void managerRunsTwoNormalFlushTargetsThroughExact27FixedPoint() throws Exception {
    for (String engine : Arrays.asList("LEVELDB", "ROCKSDB")) {
      Path output = temporaryFolder.newFolder("manager-" + engine.toLowerCase()).toPath();
      Path archive = output.resolve("state-archive");
      HistoryCommitMarker head = initializeRecoverableTail(archive, engine);
      SnapshotFixture fixture = snapshotFixture();
      SnapshotManager snapshots = fixture.snapshots;
      installRecoveredBinding(snapshots, archive, 6, 6);
      Manager manager = manager(snapshots, head);

      withArchiveConfig(output, engine, true, () -> invoke(manager, "initStateArchive"));
      Path runtimeBuilder = archive.resolve(
          StateArchiveRuntimeOwner.SERVING_INDEX_RUNTIME_DIRECTORY).resolve("keys");
      Engine selectedEngine = Engine.valueOf(engine);

      assertEquals(State.RUNNING, manager.getStateArchiveRuntime().getState());
      assertEquals(1,
          StateArchiveIndexDatabase.openReferenceCount(runtimeBuilder, selectedEngine));
      assertEquals(0, manager.getStateArchiveRuntime().getStartupRecoveryActionCount());
      assertEquals(head.getMeta(), manager.getStateArchiveRuntime().getRecoveredHead());
      assertNotNull(manager.getArchiveHistoryWriter());
      assertEquals(6, snapshots.getArchiveReadableEpoch());

      byte[] key = new byte[]{3, 1, 4};
      for (int epoch = 7; epoch <= 8; epoch++) {
        BlockSnapshotMeta target = new BlockSnapshotMeta(epoch, epoch, hash(epoch),
            hash(epoch - 1), epoch * 1_000L);
        try (ISession block = snapshots.buildSession()) {
          fixture.databases.get("proposal").put(key, new byte[]{(byte) epoch});
          block.commit(target);
        }
        setField(snapshots, "flushCount", 1);
        snapshots.flushPending();
        assertEquals(target, manager.getStateArchiveRuntime().verifyNormalWriteFixedPoint());
        assertServingFixedPoint(archive, target, 5);
        StateArchiveRuntimeOwner.ServingIndexInspection inspection =
            manager.inspectArchiveServingIndex();
        assertEquals(target, inspection.getReadableHead());
        assertEquals(epoch - 1, inspection.getLastApply().getIndexedFrom());
        assertEquals(epoch, inspection.getLastApply().getIndexedThrough());
        assertEquals(1, inspection.getLastApply().getValidatedCommitCount());
        assertEquals(Long.valueOf(1), inspection.getLastApply().getChangedEntriesByStore()
            .get("proposal"));
        assertEquals(Long.valueOf(0), inspection.getLastApply().getChangedEntriesByStore()
            .get("abi"));
        assertTrue(inspection.getLastApply().isGenerationCreated());
        assertTrue(inspection.getLastApply().getElapsedNanos() > 0);
        assertEquals(1, inspection.getLastApply().getChangedEntryCount());
        assertTrue(inspection.getLastApply().isThroughputAvailable());
        assertTrue(inspection.getLastApply().getChangedEntriesPerSecond() > 0);
        assertTrue(inspection.getLastApply().getValidatedCommitsPerSecond() > 0);
        assertEquals(0, inspection.getHistoryToServingBacklog());
        assertEquals(27, inspection.getGeneration().getStores().size());
        assertEquals(epoch, inspection.getGeneration().getIndexedThrough());
        assertTrue(inspection.getGeneration().getApparentBytes() > 0);
        assertEquals("ROCKSDB".equals(engine), inspection.getGeneration().getEngine()
            .getEstimatedLiveDataBytes().isAvailable());
        assertEquals("ROCKSDB".equals(engine), inspection.getGeneration().getEngine()
            .getTotalSstBytes().isAvailable());
        assertEquals("ROCKSDB".equals(engine), inspection.getGeneration().getEngine()
            .getPendingCompactionBytes().isAvailable());
        assertEquals(1,
            StateArchiveIndexDatabase.openReferenceCount(runtimeBuilder, selectedEngine));
        setField(snapshots, "size", 0);
      }

      invoke(manager, "closeStateArchive");
      assertEquals(0,
          StateArchiveIndexDatabase.openReferenceCount(runtimeBuilder, selectedEngine));
      assertEquals(-1, snapshots.getArchiveReadableEpoch());
      assertNull(manager.getStateArchiveRuntime());
      assertNull(manager.getArchiveHistoryWriter());
      assertFalse(Files.exists(archive.resolve("participants")));
      assertFalse(Files.exists(archive.resolve("progress")));
      snapshots.shutdown();
    }
  }

  @Test
  public void managerRunsMultiTargetNormalFlushThroughExact27FixedPoint() throws Exception {
    for (String engine : Arrays.asList("LEVELDB", "ROCKSDB")) {
      Path output = temporaryFolder.newFolder("multi-target-" + engine.toLowerCase()).toPath();
      Path archive = output.resolve("state-archive");
      HistoryCommitMarker head = initializeRecoverableTail(archive, engine);
      SnapshotFixture fixture = snapshotFixture();
      SnapshotManager snapshots = fixture.snapshots;
      installRecoveredBinding(snapshots, archive, 6, 6);
      Manager manager = manager(snapshots, head);

      withArchiveConfig(output, engine, true, () -> invoke(manager, "initStateArchive"));

      byte[] key = new byte[]{3, 1, 5};
      BlockSnapshotMeta target = null;
      for (int epoch = 7; epoch <= 8; epoch++) {
        target = new BlockSnapshotMeta(epoch, epoch, hash(epoch),
            hash(epoch - 1), epoch * 1_000L);
        try (ISession block = snapshots.buildSession()) {
          fixture.databases.get("proposal").put(key, new byte[]{(byte) epoch});
          fixture.databases.get("properties").put(new byte[]{1}, new byte[]{(byte) epoch});
          fixture.checkpointOnly.get("block").put(new byte[]{2}, new byte[]{(byte) epoch});
          fixture.checkpointOnly.get("block-index").put(new byte[]{3},
              new byte[]{(byte) epoch});
          block.commit(target);
        }
      }
      setField(snapshots, "flushCount", 2);

      snapshots.flushPending();

      assertEquals(target, manager.getStateArchiveRuntime().verifyNormalWriteFixedPoint());
      assertServingFixedPoint(archive, target, 5);
      StateArchiveRuntimeOwner.ServingIndexInspection publishedInspection =
          manager.inspectArchiveServingIndex();
      String publishedGeneration = publishedInspection.getGeneration().getGenerationId();
      ArchiveWalBinding binding = snapshots.getLatestArchiveWalBinding();
      assertNotNull(binding);
      assertEquals(7, binding.getFirst().getEpoch());
      assertEquals(8, binding.getLast().getEpoch());
      assertEquals(6, binding.getPredecessorEpoch());
      assertArrayEquals(hash(6), binding.getPredecessorHash());
      @SuppressWarnings("unchecked")
      ArgumentCaptor<Map<byte[], byte[]>> checkpointBatch =
          ArgumentCaptor.forClass(Map.class);
      verify(fixture.checkpoint).updateByBatch(checkpointBatch.capture());
      ArchiveWalBinding persisted = ArchiveWalBinding.fromCheckpointBatch(
          checkpointBatch.getValue());
      assertNotNull(persisted);
      assertArrayEquals(binding.getBatchDigest(), persisted.getBatchDigest());
      Set<String> checkpointDatabases = checkpointDatabases(checkpointBatch.getValue());
      assertTrue(checkpointDatabases.contains("properties"));
      assertTrue(checkpointDatabases.contains("proposal"));
      assertTrue(checkpointDatabases.contains("block"));
      assertTrue(checkpointDatabases.contains("block-index"));
      assertTrue(checkpointDatabases.contains(ArchiveWalBinding.CHECKPOINT_DATABASE));
      SnapshotFixture recovered = snapshotFixture(checkpointBatch.getValue());
      invokeCheckpointRecovery(recovered.snapshots, recovered.checkpoint);
      assertNotNull(recovered.snapshots.getRecoveredArchiveWalBinding());
      assertArrayEquals(binding.getBatchDigest(),
          recovered.snapshots.getRecoveredArchiveWalBinding().getBatchDigest());
      assertArrayEquals(new byte[]{8}, recovered.databases.get("proposal").get(key));
      assertArrayEquals(new byte[]{8}, recovered.databases.get("properties").get(new byte[]{1}));
      assertArrayEquals(new byte[]{8}, recovered.checkpointOnly.get("block").get(new byte[]{2}));
      assertArrayEquals(new byte[]{8},
          recovered.checkpointOnly.get("block-index").get(new byte[]{3}));
      invokeCheckpointRecovery(recovered.snapshots, recovered.checkpoint);
      assertArrayEquals(binding.getBatchDigest(),
          recovered.snapshots.getRecoveredArchiveWalBinding().getBatchDigest());
      recovered.snapshots.shutdown();
      assertTrue(fixture.databases.values().stream()
          .allMatch(database -> database.getHead() instanceof SnapshotRoot));
      invoke(manager, "closeStateArchive");
      assertFalse(Files.exists(archive.resolve("participants")));
      assertFalse(Files.exists(archive.resolve("progress")));
      snapshots.shutdown();

      SnapshotFixture restarted = snapshotFixture(checkpointBatch.getValue());
      invokeCheckpointRecovery(restarted.snapshots, restarted.checkpoint);
      BlockSnapshotMeta restartHead = target;
      Manager restartedManager = manager(restarted.snapshots, restartHead);
      withArchiveConfig(output, engine, true,
          () -> invoke(restartedManager, "initStateArchive"));
      assertEquals(restartHead, restartedManager.getStateArchiveRuntime().getRecoveredHead());
      assertEquals(0,
          restartedManager.getStateArchiveRuntime().getStartupRecoveryActionCount());
      assertEquals(restartHead,
          restartedManager.getStateArchiveRuntime().verifyNormalWriteFixedPoint());
      assertServingFixedPoint(archive, restartHead, 5);
      StateArchiveRuntimeOwner.ServingIndexInspection restartedInspection =
          restartedManager.inspectArchiveServingIndex();
      assertEquals(publishedGeneration,
          restartedInspection.getGeneration().getGenerationId());
      assertEquals(restartHead.getEpoch(),
          restartedInspection.getLastApply().getIndexedFrom());
      assertEquals(restartHead.getEpoch(),
          restartedInspection.getLastApply().getIndexedThrough());
      assertEquals(0, restartedInspection.getLastApply().getValidatedCommitCount());
      assertTrue(restartedInspection.getLastApply().getChangedEntriesByStore().values()
          .stream().allMatch(changes -> changes == 0));
      assertFalse(restartedInspection.getLastApply().isGenerationCreated());
      assertEquals(0, restartedInspection.getHistoryToServingBacklog());
      assertFalse(restartedInspection.getLastApply().isThroughputAvailable());
      assertThrows(IllegalStateException.class,
          () -> restartedInspection.getLastApply().getChangedEntriesPerSecond());
      assertEngineMeasurementEquals(publishedInspection.getGeneration().getEngine()
              .getEstimatedLiveDataBytes(),
          restartedInspection.getGeneration().getEngine().getEstimatedLiveDataBytes());
      assertEngineMeasurementEquals(publishedInspection.getGeneration().getEngine()
              .getTotalSstBytes(),
          restartedInspection.getGeneration().getEngine().getTotalSstBytes());
      assertEngineMeasurementEquals(publishedInspection.getGeneration().getEngine()
              .getPendingCompactionBytes(),
          restartedInspection.getGeneration().getEngine().getPendingCompactionBytes());
      invoke(restartedManager, "closeStateArchive");
      restarted.snapshots.shutdown();
    }
  }

  @Test
  public void newRuntimeDoesNotOpenLegacyParticipantEvidence() throws Exception {
    for (String engine : Arrays.asList("LEVELDB", "ROCKSDB")) {
      Path output = temporaryFolder.newFolder("partial-" + engine.toLowerCase()).toPath();
      Path archive = output.resolve("state-archive");
      HistoryCommitMarker head = initializeHistory(archive, 7);
      SnapshotManager snapshots = snapshotFixture().snapshots;
      installRecoveredBinding(snapshots, archive, 7, 7);
      Path failurePath = archive.resolve("participants").resolve(PARTICIPANTS.get(3));
      Files.createDirectories(failurePath.getParent());
      byte[] evidence = new byte[]{4, 5, 6, 7};
      Files.write(failurePath, evidence);
      Manager manager = manager(snapshots, head);

      withArchiveConfig(output, engine, true, () -> invoke(manager, "initStateArchive"));

      assertNotNull(manager.getStateArchiveRuntime());
      assertArrayEquals(evidence, Files.readAllBytes(failurePath));
      invoke(manager, "closeStateArchive");
      snapshots.shutdown();
    }
  }

  @Test
  public void missingWalBindingFailsBeforeNormalWriterAttachment() throws Exception {
    Path output = temporaryFolder.newFolder("missing-wal-binding").toPath();
    Path archive = output.resolve("state-archive");
    HistoryCommitMarker head = initializeRecoverableTail(archive, "LEVELDB");
    SnapshotFixture fixture = snapshotFixture();
    Manager manager = manager(fixture.snapshots, head);

    assertThrows(IllegalStateException.class,
        () -> withArchiveConfig(output, "LEVELDB", true,
            () -> invoke(manager, "initStateArchive")));

    assertNull(manager.getStateArchiveRuntime());
    assertFalse(Files.exists(archive.resolve("participants")));
    fixture.snapshots.shutdown();
  }

  @Test
  public void substitutedWalBindingFailsBeforeNormalWriterAttachment() throws Exception {
    Path output = temporaryFolder.newFolder("substituted-wal-binding").toPath();
    Path archive = output.resolve("state-archive");
    HistoryCommitMarker head = initializeRecoverableTail(archive, "ROCKSDB");
    SnapshotFixture fixture = snapshotFixture();
    ArchiveWalBinding valid = binding(archive, 6, 6);
    byte[] substitutedRefs = valid.getHistoryRefsDigest();
    substitutedRefs[0] ^= 1;
    ArchiveWalBinding substituted = new ArchiveWalBinding(valid.getFirst(), valid.getLast(),
        valid.getPredecessorEpoch(), valid.getPredecessorHash(), valid.getBatchDigest(),
        valid.getStoreScopeDigest(), substitutedRefs, valid.getBlockIndexRefsDigest());
    setField(fixture.snapshots, "recoveredArchiveWalBinding", substituted);
    Manager manager = manager(fixture.snapshots, head);

    assertThrows(IllegalStateException.class,
        () -> withArchiveConfig(output, "ROCKSDB", true,
            () -> invoke(manager, "initStateArchive")));

    assertNull(manager.getStateArchiveRuntime());
    assertFalse(Files.exists(archive.resolve("participants")));
    fixture.snapshots.shutdown();
  }

  @Test
  public void substitutedAllStoreServingIndexFailsBeforeRuntimeAttachment() throws Exception {
    Path output = temporaryFolder.newFolder("substituted-serving-index").toPath();
    Path archive = output.resolve("state-archive");
    HistoryCommitMarker head = initializeRecoverableTail(archive, "ROCKSDB");
    SnapshotFixture initial = snapshotFixture();
    installRecoveredBinding(initial.snapshots, archive, 6, 6);
    Manager initialManager = manager(initial.snapshots, head);
    withArchiveConfig(output, "ROCKSDB", true,
        () -> invoke(initialManager, "initStateArchive"));
    invoke(initialManager, "closeStateArchive");
    initial.snapshots.shutdown();

    Path foreignArchive = temporaryFolder.newFolder("foreign-serving-history").toPath();
    Path foreignShadow = output.resolve("foreign-serving-shadow");
    withArchiveConfig(output, "ROCKSDB", true, () -> {
      try (ArchiveHistoryWriter foreign = new ArchiveHistoryWriter(
          foreignArchive, 4096, ArchiveStoreScope.getStateDatabases())) {
        foreign.accept(new BlockReverseDiff(head.getMeta(), Collections.singletonList(
            new BlockReverseDiff.DbGroup("proposal", Collections.singletonList(
                new BlockReverseDiff.Entry(new byte[]{9, 9}, OldValue.absent()))))));
        try (PersistentServingKeyIndexGeneration ignored =
            foreign.buildServingGeneration(foreignShadow, "foreign")) {
          // Catalog publication reopens the generation after this build handle is closed.
        }
      }
    });
    try (PersistentServingKeyIndexCatalog catalog =
        PersistentServingKeyIndexCatalog.open(archive.resolve("serving-index"))) {
      assertTrue(catalog.publish(catalog.getCurrentGenerationId(), foreignShadow));
    }

    SnapshotFixture restarted = snapshotFixture();
    installRecoveredBinding(restarted.snapshots, archive, 6, 6);
    Manager restartedManager = manager(restarted.snapshots, head);
    assertThrows(IllegalStateException.class,
        () -> withArchiveConfig(output, "ROCKSDB", true,
            () -> invoke(restartedManager, "initStateArchive")));
    assertNull(restartedManager.getStateArchiveRuntime());
    restarted.snapshots.shutdown();
  }

  @Test
  public void disabledManagerControlDoesNotInspectOrCreateArchiveRuntime() throws Exception {
    Path output = temporaryFolder.newFolder("disabled-manager").toPath();
    Path archive = output.resolve("state-archive");
    Files.createDirectories(archive);
    byte[] evidence = new byte[]{8, 9, 10};
    Files.write(archive.resolve("unexpected-evidence"), evidence);
    Manager manager = new Manager();

    withArchiveConfig(output, "LEVELDB", false, () -> invoke(manager, "initStateArchive"));

    assertNull(manager.getStateArchiveRuntime());
    assertArrayEquals(evidence, Files.readAllBytes(archive.resolve("unexpected-evidence")));
    assertFalse(Files.exists(archive.resolve("participants")));
  }

  private static Manager manager(SnapshotManager snapshots, HistoryCommitMarker head)
      throws Exception {
    return manager(snapshots, head.getMeta());
  }

  private static Manager manager(SnapshotManager snapshots, BlockSnapshotMeta head)
      throws Exception {
    return manager(snapshots, head, null, null);
  }

  private static Manager manager(SnapshotManager snapshots, BlockSnapshotMeta head,
      AccountAssetStore accountAssetStore, AtomicLong targetOptimization) throws Exception {
    DynamicPropertiesStore properties = mock(DynamicPropertiesStore.class);
    when(properties.getLatestBlockHeaderNumber()).thenReturn(head.getBlockNumber());
    when(properties.getLatestBlockHeaderHash())
        .thenReturn(Sha256Hash.wrap(head.getBlockHash()));
    ChainBaseManager chainBase = mock(ChainBaseManager.class);
    when(chainBase.getDynamicPropertiesStore()).thenReturn(properties);
    if (accountAssetStore != null) {
      when(chainBase.getAccountAssetStore()).thenReturn(accountAssetStore);
      when(properties.getAllowAccountAssetOptimizationFromRoot())
          .thenAnswer(ignored -> targetOptimization.get());
      when(properties.supportAllowAssetOptimization())
          .thenAnswer(ignored -> targetOptimization.get() == 1);
    }
    BlockCapsule headBlock = mock(BlockCapsule.class);
    when(headBlock.getParentHash()).thenReturn(Sha256Hash.wrap(head.getParentHash()));
    when(headBlock.getTimeStamp()).thenReturn(head.getTimestamp());
    when(chainBase.getBlockByNum(head.getBlockNumber())).thenReturn(headBlock);
    ChainBaseManager.init(chainBase);
    Manager manager = new Manager();
    setField(manager, "revokingStore", snapshots);
    setField(manager, "chainBaseManager", chainBase);
    return manager;
  }

  private static SnapshotFixture snapshotFixture() {
    return snapshotFixture(Collections.emptyMap());
  }

  private static SnapshotFixture snapshotFixture(Map<byte[], byte[]> checkpointEntries) {
    return snapshotFixture(checkpointEntries, true);
  }

  private static SnapshotFixture snapshotFixture(Map<byte[], byte[]> checkpointEntries,
      boolean includeAccountAsset) {
    if (CommonParameter.getInstance().getStorage() == null) {
      CommonParameter.getInstance().storage = new Storage();
    }
    SnapshotManager snapshots = new SnapshotManager("");
    Map<String, Chainbase> databases = new LinkedHashMap<>();
    for (String participant : PARTICIPANTS) {
      if (!includeAccountAsset
          && AccountAssetArchiveProjector.ACCOUNT_ASSET_DB.equals(participant)) {
        continue;
      }
      Chainbase database = new Chainbase(new SnapshotRoot(new MemoryDb(participant)));
      snapshots.add(database);
      databases.put(participant, database);
    }
    Map<String, Chainbase> checkpointOnly = new LinkedHashMap<>();
    for (String name : Arrays.asList("block", "block-index")) {
      Chainbase database = new Chainbase(new SnapshotRoot(new MemoryDb(name)));
      snapshots.add(database);
      checkpointOnly.put(name, database);
    }
    snapshots.enable();
    snapshots.setUnChecked(false);
    CheckTmpStore checkpoint = mock(CheckTmpStore.class);
    @SuppressWarnings("unchecked")
    DbSourceInter<byte[]> checkpointDb = mock(DbSourceInter.class);
    when(checkpointDb.iterator()).thenAnswer(
        ignored -> checkpointEntries.entrySet().iterator());
    when(checkpoint.getDbSource()).thenReturn(checkpointDb);
    snapshots.setCheckTmpStore(checkpoint);
    return new SnapshotFixture(snapshots, databases, checkpointOnly, checkpoint,
        Collections.emptyMap());
  }

  private static SnapshotFixture nativeSnapshotFixture(Path output, String engine,
      Map<byte[], byte[]> checkpointEntries) {
    if (CommonParameter.getInstance().getStorage() == null) {
      CommonParameter.getInstance().storage = new Storage();
    }
    SnapshotManager snapshots = new SnapshotManager("");
    Map<String, Chainbase> databases = new LinkedHashMap<>();
    Map<String, SnapshotCapableStore> nativeStores = new LinkedHashMap<>();
    for (String participant : PARTICIPANTS) {
      if (AccountAssetArchiveProjector.ACCOUNT_ASSET_DB.equals(participant)) {
        continue;
      }
      DB<byte[], byte[]> nativeStore = openNativeStore(output, engine, participant);
      Chainbase database = new Chainbase(new SnapshotRoot(nativeStore));
      snapshots.add(database);
      databases.put(participant, database);
      nativeStores.put(participant, (SnapshotCapableStore) nativeStore);
    }
    Map<String, Chainbase> checkpointOnly = new LinkedHashMap<>();
    for (String name : Arrays.asList("block", "block-index")) {
      DB<byte[], byte[]> nativeStore = openNativeStore(output, engine, name);
      Chainbase database = new Chainbase(new SnapshotRoot(nativeStore));
      snapshots.add(database);
      checkpointOnly.put(name, database);
      nativeStores.put(name, (SnapshotCapableStore) nativeStore);
    }
    snapshots.enable();
    snapshots.setUnChecked(false);
    CheckTmpStore checkpoint = mock(CheckTmpStore.class);
    @SuppressWarnings("unchecked")
    DbSourceInter<byte[]> checkpointDb = mock(DbSourceInter.class);
    when(checkpointDb.iterator()).thenAnswer(
        ignored -> checkpointEntries.entrySet().iterator());
    when(checkpoint.getDbSource()).thenReturn(checkpointDb);
    snapshots.setCheckTmpStore(checkpoint);
    return new SnapshotFixture(snapshots, databases, checkpointOnly, checkpoint, nativeStores);
  }

  private static DB<byte[], byte[]> openNativeStore(Path output, String engine, String name) {
    if ("LEVELDB".equals(engine)) {
      return new LevelDB(new LevelDbDataSourceImpl(output.toString(), name));
    }
    if ("ROCKSDB".equals(engine)) {
      return new RocksDB(new RocksDbDataSourceImpl(
          output.resolve("database").toString(), name));
    }
    throw new IllegalArgumentException("Unsupported native fixture engine: " + engine);
  }

  private static SnapshotFixture snapshotFixtureWithoutAccountAsset(
      Map<byte[], byte[]> checkpointEntries) {
    return snapshotFixture(checkpointEntries, false);
  }

  private static HistoryCommitMarker initializeRecoverableTail(Path archive, String engine)
      throws Exception {
    return initializeHistory(archive, 6);
  }

  private static void assertServingFixedPoint(Path archive, BlockSnapshotMeta expected,
      long expectedFrom) throws Exception {
    try (PersistentServingKeyIndexCatalog catalog =
        PersistentServingKeyIndexCatalog.open(archive.resolve("serving-index"));
        PersistentServingKeyIndexGeneration generation = catalog.pin()) {
      assertEquals(expectedFrom, generation.getIndexedFrom());
      assertEquals(expected.getEpoch(), generation.getIndexedThrough());
      assertArrayEquals(expected.getBlockHash(), generation.getHeadHash());
      assertEquals(PARTICIPANTS, generation.getParticipatingDatabases());
      assertTrue(generation.isLatestSourceIdentityBound());
      assertTrue(generation.isExactOnlyFormat());
      assertFalse(generation.supportsRangeQueries());
      for (String participant : PARTICIPANTS) {
        assertEquals(expected.getEpoch(), generation.getPersistentStoreCoverage(participant)
            .getIndexedThrough());
      }
    }
  }

  private static void assertEngineMeasurementEquals(
      PersistentServingKeyIndexGeneration.LongPropertyMeasurement expected,
      PersistentServingKeyIndexGeneration.LongPropertyMeasurement actual) {
    assertEquals(expected.isAvailable(), actual.isAvailable());
    if (expected.isAvailable()) {
      assertEquals(expected.getValue(), actual.getValue());
    }
  }

  private static long countGenerationDirectories(Path archive) throws IOException {
    try (java.util.stream.Stream<Path> entries =
        Files.list(archive.resolve("serving-index").resolve("generations"))) {
      return entries.filter(Files::isDirectory).count();
    }
  }

  private static void downgradeGenerationToV3(Path generation) throws IOException {
    Path manifest = generation.resolve("generation.meta");
    byte[] encoded = Files.readAllBytes(manifest);
    ByteBuffer.wrap(encoded).putShort(4, (short) 3);
    int payloadLength = encoded.length - Integer.BYTES;
    int checksum = Hashing.crc32c().hashBytes(encoded, 0, payloadLength).asInt();
    ByteBuffer.wrap(encoded, payloadLength, Integer.BYTES).putInt(checksum);
    Files.write(manifest, encoded);
  }

  private static void assertServingGenerationSupportsRange(Path archive) throws IOException {
    try (PersistentServingKeyIndexCatalog catalog =
        PersistentServingKeyIndexCatalog.open(archive.resolve("serving-index"));
        PersistentServingKeyIndexGeneration generation = catalog.pin()) {
      assertTrue(generation.isExactOnlyFormat());
      assertFalse(generation.supportsRangeQueries());
    }
  }

  private static Map<String, byte[]> historyAuthoritySnapshot(Path archive) throws IOException {
    Map<String, byte[]> snapshot = new LinkedHashMap<>();
    for (String relative : Arrays.asList("MANIFEST", "bootstrap.anchor",
        "history.scan-anchor", "state_history.idx", "commits/commit.log")) {
      Path file = archive.resolve(relative);
      snapshot.put(relative, Files.readAllBytes(file));
    }
    try (java.util.stream.Stream<Path> segments = Files.list(archive.resolve("history"))) {
      Iterator<Path> ordered = segments.filter(Files::isRegularFile).sorted().iterator();
      while (ordered.hasNext()) {
        Path file = ordered.next();
        snapshot.put("history/" + file.getFileName(), Files.readAllBytes(file));
      }
    }
    return snapshot;
  }

  private static void assertHistoryAuthorityEquals(Map<String, byte[]> expected, Path archive)
      throws IOException {
    Map<String, byte[]> actual = historyAuthoritySnapshot(archive);
    assertEquals(expected.keySet(), actual.keySet());
    for (String relative : expected.keySet()) {
      assertArrayEquals(relative, expected.get(relative), actual.get(relative));
    }
  }

  private static void installRecoveredBinding(SnapshotManager snapshots, Path archive,
      long firstEpoch, long lastEpoch) throws Exception {
    setField(snapshots, "recoveredArchiveWalBinding",
        binding(archive, firstEpoch, lastEpoch));
  }

  private static ArchiveWalBinding binding(Path archive, long firstEpoch, long lastEpoch)
      throws Exception {
    List<HistoryCommitMarker> markers = new ArrayList<>();
    try (ArchiveHistoryWriter writer = new ArchiveHistoryWriter(
        archive, 4096, ArchiveStoreScope.getStateDatabases())) {
      for (long epoch = firstEpoch; epoch <= lastEpoch; epoch++) {
        markers.add(writer.get(epoch));
      }
    }
    return ArchiveWalBinding.fromMarkers(markers);
  }

  private static HistoryCommitMarker initializeHistory(Path archive, int epoch)
      throws Exception {
    HistoryCommitMarker head;
    try (ArchiveHistoryWriter writer = new ArchiveHistoryWriter(
        archive, 4096, ArchiveStoreScope.getStateDatabases())) {
      writer.accept(new BlockReverseDiff(
          new BlockSnapshotMeta(epoch, epoch, hash(epoch), hash(epoch - 1), epoch * 1_000L),
          Collections.emptyList()));
      head = writer.committedHead();
    }
    return head;
  }

  private static void withArchiveConfig(Path output, String engine, boolean enabled,
      ThrowingRunnable action) throws Exception {
    CommonParameter args = CommonParameter.getInstance();
    Storage oldStorage = args.getStorage();
    Storage storage = oldStorage == null ? new Storage() : oldStorage;
    args.storage = storage;
    String oldOutput = args.outputDirectory;
    String oldDirectory = storage.getStateArchiveDirectory();
    String oldDbDirectory = storage.getDbDirectory();
    String oldEngine = storage.getDbEngine();
    long oldSegmentSize = storage.getStateArchiveMaxSegmentSize();
    int oldQueueCapacity = storage.getStateArchiveQueueCapacity();
    boolean oldEnabled = storage.isStateArchiveEnabled();
    try {
      args.outputDirectory = output.toString();
      storage.setStateArchiveDirectory("state-archive");
      storage.setDbDirectory("database");
      storage.setDbEngine(engine);
      storage.setDefaultDbOptions(new StorageConfig());
      storage.setStateArchiveMaxSegmentSize(4096);
      storage.setStateArchiveQueueCapacity(4);
      storage.setStateArchiveEnabled(enabled);
      action.run();
    } finally {
      args.outputDirectory = oldOutput;
      storage.setStateArchiveDirectory(oldDirectory);
      storage.setDbDirectory(oldDbDirectory);
      storage.setDbEngine(oldEngine);
      storage.setStateArchiveMaxSegmentSize(oldSegmentSize);
      storage.setStateArchiveQueueCapacity(oldQueueCapacity);
      storage.setStateArchiveEnabled(oldEnabled);
      args.storage = oldStorage;
    }
  }

  private static void setField(Object target, String name, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }

  private static void invoke(Manager manager, String name) throws Exception {
    Method method = Manager.class.getDeclaredMethod(name);
    method.setAccessible(true);
    try {
      method.invoke(manager);
    } catch (InvocationTargetException failure) {
      Throwable cause = failure.getCause();
      if (cause instanceof Exception) {
        throw (Exception) cause;
      }
      throw failure;
    }
  }

  private static void invokeCheckpointRecovery(SnapshotManager snapshots,
      TronDatabase<byte[]> checkpoint) throws Exception {
    Method method = SnapshotManager.class.getDeclaredMethod("recover", TronDatabase.class);
    method.setAccessible(true);
    try {
      method.invoke(snapshots, checkpoint);
    } catch (InvocationTargetException failure) {
      Throwable cause = failure.getCause();
      if (cause instanceof Exception) {
        throw (Exception) cause;
      }
      throw failure;
    }
  }

  private static List<String> participants() {
    List<String> participants = new ArrayList<>(ArchiveStoreScope.getStateDatabases());
    Collections.sort(participants);
    return Collections.unmodifiableList(participants);
  }

  private static byte[] hash(int suffix) {
    byte[] hash = new byte[32];
    hash[31] = (byte) suffix;
    return hash;
  }

  private static byte[] account(byte[] address, long balance) {
    return Account.newBuilder().setAddress(ByteString.copyFrom(address)).setBalance(balance)
        .build().toByteArray();
  }

  private static byte[] assetAccount(byte[] address, boolean optimized, String tokenId,
      long balance) {
    Account.Builder builder = Account.newBuilder().setAddress(ByteString.copyFrom(address))
        .setAssetOptimized(optimized);
    if (tokenId != null) {
      builder.putAssetV2(tokenId, balance);
    }
    return builder.build().toByteArray();
  }

  private static void assertP66History(Manager manager, byte[] address, byte[] absentAddress,
      String tokenId) {
    assertAccountAsset(manager, 6, address, tokenId,
        P66AccountAssetCodec.Phase.P66_OFF, true, 20);
    assertAccountAsset(manager, 6, absentAddress, tokenId,
        P66AccountAssetCodec.Phase.P66_OFF, false, 0);
    assertAccountAsset(manager, 7, address, tokenId,
        P66AccountAssetCodec.Phase.P66_ON, true, 30);
    assertAccountAsset(manager, 8, address, tokenId,
        P66AccountAssetCodec.Phase.P66_ON, true, 40);
    assertAccountAsset(manager, 9, address, tokenId,
        P66AccountAssetCodec.Phase.P66_ON, false, 0);
    assertAccountAssetPrefix(manager, 6, address, tokenId,
        P66AccountAssetCodec.Phase.P66_OFF, true, 20);
    assertAccountAssetPrefix(manager, 6, absentAddress, tokenId,
        P66AccountAssetCodec.Phase.P66_OFF, false, 0);
    assertAccountAssetPrefix(manager, 7, address, tokenId,
        P66AccountAssetCodec.Phase.P66_ON, true, 30);
    assertAccountAssetPrefix(manager, 8, address, tokenId,
        P66AccountAssetCodec.Phase.P66_ON, true, 40);
    assertAccountAssetPrefix(manager, 9, address, tokenId,
        P66AccountAssetCodec.Phase.P66_ON, false, 0);
  }

  private static void assertAccountAssetPrefix(Manager manager, int targetEpoch, byte[] address,
      String tokenId, P66AccountAssetCodec.Phase phase, boolean present, long balance) {
    assertNotNull(tokenId);
    assertNotNull(phase);
    assertTrue(present || balance == 0);
    assertThrows(ArchivePersistenceException.class,
        () -> manager.getArchiveAccountAssets(targetEpoch, address, prefixLimits()));
  }

  private static HistoricalAccountAssetPrefixResolver.Limits prefixLimits() {
    return new HistoricalAccountAssetPrefixResolver.Limits(10, 10, 10, 64, 8, 1_000);
  }

  private static void assertAccountAsset(Manager manager, int targetEpoch, byte[] address,
      String tokenId, P66AccountAssetCodec.Phase phase, boolean present, long balance) {
    HistoricalAccountAssetBalanceResolver.Result result =
        manager.getArchiveAccountAssetBalance(targetEpoch, address, tokenId);
    assertEquals(targetEpoch, result.getBlockNumber());
    assertArrayEquals(address, result.getAddress());
    assertEquals(tokenId, result.getTokenId());
    assertEquals(phase, result.getPhase());
    assertEquals(present, result.isAccountPresent());
    if (present) {
      assertEquals(balance, result.getBalance());
    }
  }

  private static byte[] archiveAddress(int suffix) {
    byte[] address = new byte[HistoricalAccountBalanceReader.ADDRESS_LENGTH];
    address[0] = 0x41;
    address[address.length - 1] = (byte) suffix;
    return address;
  }

  private static byte[] longValue(long value) {
    return ByteBuffer.allocate(Long.BYTES).putLong(value).array();
  }

  private static void seedExact27State(Map<String, Chainbase> databases, int latestEpoch) {
    int storeIndex = 0;
    for (String dbName : PARTICIPANTS) {
      Chainbase database = databases.get(dbName);
      database.put(exact27Key(1, storeIndex), exact27Value(latestEpoch, storeIndex));
      database.put(exact27Key(2, storeIndex), exact27Value(6, storeIndex));
      if (latestEpoch == 6) {
        database.put(exact27Key(4, storeIndex), exact27Value(6, storeIndex));
      } else {
        database.put(exact27Key(3, storeIndex), exact27Value(latestEpoch, storeIndex));
        database.put(exact27Key(5, storeIndex), new byte[0]);
      }
      database.put(exact27Key(6, storeIndex), exact27Value(6, storeIndex));
      storeIndex++;
    }
  }

  private static void seedNativeExact27(Map<String, Chainbase> databases, int epoch) {
    int storeIndex = 0;
    for (String dbName : PARTICIPANTS) {
      if (!AccountAssetArchiveProjector.ACCOUNT_ASSET_DB.equals(dbName)
          && !"account".equals(dbName)) {
        databases.get(dbName).put(nativeExactKey(dbName, storeIndex),
            nativeExactValue(epoch, storeIndex));
      }
      storeIndex++;
    }
  }

  private static void mutateNativeExact27(Map<String, Chainbase> databases, int epoch) {
    int storeIndex = 0;
    for (String dbName : PARTICIPANTS) {
      if (!AccountAssetArchiveProjector.ACCOUNT_ASSET_DB.equals(dbName)
          && !"account".equals(dbName)) {
        Chainbase database = databases.get(dbName);
        if (epoch == 7 || storeIndex % 3 == 0) {
          database.put(nativeExactKey(dbName, storeIndex), nativeExactValue(epoch, storeIndex));
        } else if (storeIndex % 3 == 1) {
          database.delete(nativeExactKey(dbName, storeIndex));
        } else {
          database.put(nativeExactKey(dbName, storeIndex), nativeExactValue(7, storeIndex));
        }
      }
      storeIndex++;
    }
  }

  private static void assertNativeExact27History(Manager manager, byte[] address,
      byte[] directKey) {
    for (int epoch = 6; epoch <= 8; epoch++) {
      int storeIndex = 0;
      for (String dbName : PARTICIPANTS) {
        if (!AccountAssetArchiveProjector.ACCOUNT_ASSET_DB.equals(dbName)
            && !"account".equals(dbName)) {
          byte[] expected;
          if (epoch == 6) {
            expected = nativeExactValue(6, storeIndex);
          } else if (epoch == 7 || storeIndex % 3 == 0) {
            expected = nativeExactValue(epoch, storeIndex);
          } else if (storeIndex % 3 == 1) {
            expected = null;
          } else {
            expected = nativeExactValue(7, storeIndex);
          }
          assertHistoricalValue(manager, epoch, dbName, nativeExactKey(dbName, storeIndex),
              expected);
        }
        storeIndex++;
      }
      assertTrue(manager.getArchiveStateValue(epoch, "account", address).isPresent());
      assertHistoricalValue(manager, epoch, AccountAssetArchiveProjector.ACCOUNT_ASSET_DB,
          directKey, epoch == 6 ? null : longValue(epoch == 7 ? 30 : 40));
    }
  }

  private static byte[] nativeExactKey(String dbName, int storeIndex) {
    if ("market_pair_price_to_order".equals(dbName)) {
      return org.tron.core.capsule.utils.MarketUtils.createPairPriceKey(
          "100".getBytes(StandardCharsets.UTF_8),
          "200".getBytes(StandardCharsets.UTF_8), 1000L + storeIndex, 2000L);
    }
    return new byte[]{(byte) 0xc1, (byte) storeIndex};
  }

  private static byte[] nativeExactValue(int epoch, int storeIndex) {
    return new byte[]{(byte) 0xd1, (byte) epoch, (byte) storeIndex};
  }

  private static Map<String, String> sourceIdentities(SnapshotFixture fixture,
      AccountAssetStore accountAssetStore) {
    Map<String, String> identities = new LinkedHashMap<>();
    for (Map.Entry<String, SnapshotCapableStore> entry : fixture.nativeStores.entrySet()) {
      identities.put(entry.getKey(), entry.getValue().getSourceIdentity());
    }
    DbSourceInter<byte[]> source = accountAssetStore.getDbSource();
    if (source instanceof LevelDbDataSourceImpl) {
      identities.put(AccountAssetArchiveProjector.ACCOUNT_ASSET_DB,
          ((LevelDbDataSourceImpl) source).getSnapshotSourceIdentity());
    } else if (source instanceof RocksDbDataSourceImpl) {
      identities.put(AccountAssetArchiveProjector.ACCOUNT_ASSET_DB,
          ((RocksDbDataSourceImpl) source).getSnapshotSourceIdentity());
    } else {
      throw new IllegalArgumentException("AccountAsset Store is not native");
    }
    return identities;
  }

  private static void closeNativeStores(SnapshotFixture fixture) {
    for (SnapshotCapableStore store : fixture.nativeStores.values()) {
      if (store instanceof LevelDB) {
        ((LevelDB) store).getDb().closeDB();
      } else if (store instanceof RocksDB) {
        ((RocksDB) store).getDb().closeDB();
      }
    }
  }

  private static void assertExact27History(Manager manager, int targetEpoch) {
    int storeIndex = 0;
    for (String dbName : PARTICIPANTS) {
      assertHistoricalValue(manager, targetEpoch, dbName, exact27Key(1, storeIndex),
          exact27Value(targetEpoch, storeIndex));
      assertHistoricalValue(manager, targetEpoch, dbName, exact27Key(2, storeIndex),
          exact27Value(6, storeIndex));
      assertHistoricalValue(manager, targetEpoch, dbName, exact27Key(3, storeIndex),
          targetEpoch == 6 ? null : exact27Value(targetEpoch, storeIndex));
      assertHistoricalValue(manager, targetEpoch, dbName, exact27Key(4, storeIndex),
          targetEpoch == 6 ? exact27Value(6, storeIndex) : null);
      assertHistoricalValue(manager, targetEpoch, dbName, exact27Key(5, storeIndex),
          targetEpoch == 6 ? null : new byte[0]);
      assertHistoricalValue(manager, targetEpoch, dbName, exact27Key(6, storeIndex),
          exact27Value(6, storeIndex));
      storeIndex++;
    }
  }

  private static void assertHistoricalValue(Manager manager, int targetEpoch, String dbName,
      byte[] physicalRawKey, byte[] expected) {
    OldValue actual = manager.getArchiveStateValue(targetEpoch, dbName, physicalRawKey);
    assertEquals(expected != null, actual.isPresent());
    assertEquals(expected != null,
        manager.hasArchiveStateValue(targetEpoch, dbName, physicalRawKey));
    if (expected != null) {
      assertArrayEquals(expected, actual.getValue());
    }
  }

  private static void assertExact27ServingIndex(Path archive) throws IOException {
    try (PersistentServingKeyIndexCatalog catalog =
        PersistentServingKeyIndexCatalog.open(archive.resolve("serving-index"));
        PersistentServingKeyIndexGeneration generation = catalog.pin()) {
      int storeIndex = 0;
      for (String dbName : PARTICIPANTS) {
        for (int changedKind : Arrays.asList(1, 3, 4, 5)) {
          assertEquals(7, generation.firstChangeAfter(dbName,
              exact27Key(changedKind, storeIndex), 6, 8).getAsLong());
        }
        assertFalse(generation.firstChangeAfter(dbName,
            exact27Key(2, storeIndex), 6, 8).isPresent());
        assertFalse(generation.firstChangeAfter(dbName,
            exact27Key(6, storeIndex), 6, 8).isPresent());
        storeIndex++;
      }
    }
  }

  private static byte[] exact27Key(int kind, int storeIndex) {
    return new byte[]{(byte) 0xa1, (byte) kind, (byte) storeIndex};
  }

  private static byte[] exact27Value(int epoch, int storeIndex) {
    return new byte[]{(byte) 0xb1, (byte) epoch, (byte) storeIndex};
  }

  private static Set<String> checkpointDatabases(Map<byte[], byte[]> batch) {
    Set<String> databases = new LinkedHashSet<>();
    for (byte[] key : batch.keySet()) {
      ByteBuffer input = ByteBuffer.wrap(key);
      int length = input.getInt();
      byte[] database = new byte[length];
      input.get(database);
      databases.add(new String(database, StandardCharsets.UTF_8));
    }
    return databases;
  }

  private static final class SnapshotFixture {
    private final SnapshotManager snapshots;
    private final Map<String, Chainbase> databases;
    private final Map<String, Chainbase> checkpointOnly;
    private final CheckTmpStore checkpoint;
    private final Map<String, SnapshotCapableStore> nativeStores;

    private SnapshotFixture(SnapshotManager snapshots, Map<String, Chainbase> databases,
        Map<String, Chainbase> checkpointOnly, CheckTmpStore checkpoint,
        Map<String, SnapshotCapableStore> nativeStores) {
      this.snapshots = snapshots;
      this.databases = databases;
      this.checkpointOnly = checkpointOnly;
      this.checkpoint = checkpoint;
      this.nativeStores = nativeStores;
    }
  }

  private static final class TestAccountAssetStore extends AccountAssetStore {
    private boolean failPrefixQuery;

    private TestAccountAssetStore() {
      super(AccountAssetArchiveProjector.ACCOUNT_ASSET_DB);
    }

    private void failNextPrefixQuery() {
      failPrefixQuery = true;
    }

    @Override
    public Map<WrappedByteArray, byte[]> prefixQuery(byte[] key) {
      if (failPrefixQuery) {
        failPrefixQuery = false;
        throw new ArchivePersistenceException("injected supplemental prefix failure");
      }
      return super.prefixQuery(key);
    }
  }

  private static final class MemoryDb implements DB<byte[], byte[]>, Flusher,
      SnapshotCapableStore {
    private final String name;
    private final Map<WrappedByteArray, byte[]> values = new LinkedHashMap<>();

    private MemoryDb(String name) {
      this.name = name;
    }

    @Override
    public byte[] get(byte[] key) {
      byte[] value = values.get(WrappedByteArray.of(key));
      return value == null ? null : Arrays.copyOf(value, value.length);
    }

    @Override
    public void put(byte[] key, byte[] value) {
      values.put(WrappedByteArray.copyOf(key), Arrays.copyOf(value, value.length));
    }

    @Override
    public long size() {
      return values.size();
    }

    @Override
    public boolean isEmpty() {
      return values.isEmpty();
    }

    @Override
    public void remove(byte[] key) {
      values.remove(WrappedByteArray.of(key));
    }

    @Override
    public Iterator<Map.Entry<byte[], byte[]>> iterator() {
      List<Map.Entry<byte[], byte[]>> entries = new ArrayList<>();
      values.forEach((key, value) -> entries.add(new AbstractMap.SimpleImmutableEntry<>(
          key.getBytes(), Arrays.copyOf(value, value.length))));
      return entries.iterator();
    }

    @Override
    public void close() {
      values.clear();
    }

    @Override
    public void flush(Map<WrappedByteArray, WrappedByteArray> batch) {
      batch.forEach((key, value) -> {
        if (value == null || value.getBytes() == null) {
          values.remove(key);
        } else {
          values.put(WrappedByteArray.copyOf(key.getBytes()), value.getBytes());
        }
      });
    }

    @Override
    public void reset() {
      values.clear();
    }

    @Override
    public String getDbName() {
      return name;
    }

    @Override
    public String getSourceIdentity() {
      return "memory:" + name;
    }

    @Override
    public StoreSnapshot pin(long blockNumber, byte[] blockHash) {
      Map<WrappedByteArray, byte[]> pinned = new LinkedHashMap<>();
      values.forEach((key, value) -> pinned.put(WrappedByteArray.copyOf(key.getBytes()),
          Arrays.copyOf(value, value.length)));
      byte[] pinnedHash = Arrays.copyOf(blockHash, blockHash.length);
      return new StoreSnapshot() {
        @Override
        public String getDbName() {
          return name;
        }

        @Override
        public String getSourceIdentity() {
          return MemoryDb.this.getSourceIdentity();
        }

        @Override
        public long getBlockNumber() {
          return blockNumber;
        }

        @Override
        public byte[] getBlockHash() {
          return Arrays.copyOf(pinnedHash, pinnedHash.length);
        }

        @Override
        public byte[] get(byte[] physicalRawKey) {
          byte[] value = pinned.get(WrappedByteArray.of(physicalRawKey));
          return value == null ? null : Arrays.copyOf(value, value.length);
        }

        @Override
        public List<Map.Entry<byte[], byte[]>> range(byte[] lowerInclusive,
            byte[] upperExclusive, int maxEntries) {
          if (lowerInclusive == null || maxEntries <= 0) {
            throw new IllegalArgumentException("Invalid memory snapshot range");
          }
          List<Map.Entry<byte[], byte[]>> entries = new ArrayList<>();
          pinned.forEach((key, value) -> entries.add(new AbstractMap.SimpleImmutableEntry<>(
              key.getBytes(), Arrays.copyOf(value, value.length))));
          entries.sort((left, right) ->
              BlockReverseDiff.compareUnsigned(left.getKey(), right.getKey()));
          List<Map.Entry<byte[], byte[]>> result = new ArrayList<>();
          for (Map.Entry<byte[], byte[]> entry : entries) {
            if (BlockReverseDiff.compareUnsigned(entry.getKey(), lowerInclusive) < 0) {
              continue;
            }
            if (upperExclusive != null
                && BlockReverseDiff.compareUnsigned(entry.getKey(), upperExclusive) >= 0) {
              break;
            }
            if (result.size() == maxEntries) {
              break;
            }
            result.add(entry);
          }
          return Collections.unmodifiableList(result);
        }

        @Override
        public void close() {
          pinned.clear();
        }
      };
    }

    @Override
    public void stat() {
    }

    @Override
    public DB<byte[], byte[]> newInstance() {
      return new MemoryDb(name);
    }
  }

  @FunctionalInterface
  private interface ThrowingRunnable {
    void run() throws Exception;
  }
}
