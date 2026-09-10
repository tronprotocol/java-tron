package org.tron.core.db2.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tron.common.TestConstants;
import org.tron.core.config.args.Args;
import org.tron.core.db2.archive.ArchiveReadSnapshot.PinnedLatestState;
import org.tron.core.db2.archive.BlockReverseDiff;
import org.tron.core.db2.archive.BlockReverseDiff.DbGroup;
import org.tron.core.db2.archive.BlockReverseDiff.Entry;
import org.tron.core.db2.archive.BlockSnapshotMeta;
import org.tron.core.db2.archive.HistoricalRangeOverlay;
import org.tron.core.db2.archive.OldValue;
import org.tron.core.db2.archive.StateArchiveAppendCheckpointMaterializerV3;
import org.tron.core.db2.archive.StateArchiveCheckpointMaterializer;
import org.tron.core.db2.archive.StateArchiveCheckpointReadSnapshot;
import org.tron.core.db2.archive.StateArchiveFileFormatV3;
import org.tron.core.db2.archive.StateArchiveFiveLaneDurabilityProofV3;
import org.tron.core.db2.archive.StateArchiveHotCheckpointMaterializer;
import org.tron.core.db2.archive.StateArchiveHotStore;
import org.tron.core.db2.common.DB;
import org.tron.core.db2.common.Flusher;
import org.tron.core.db2.common.WrappedByteArray;
import org.tron.core.db2.core.CommonCheckpointMaterializer.Authority;
import org.tron.core.db2.core.CommonCheckpointMaterializer.Status;
import org.tron.core.db2.stateroot.PathStateCanonicalizer;
import org.tron.core.db2.stateroot.PathStateCheckpointMaterializer;
import org.tron.core.db2.stateroot.PathStateFlushTarget;
import org.tron.core.db2.stateroot.PathStateParticipantScope;
import org.tron.core.db2.stateroot.PathStatePhysicalStoreSet;
import org.tron.core.db2.stateroot.PathStateSnapshotDelta;
import org.tron.core.db2.stateroot.PathStateStoreManifest.Engine;

public class ChainbaseCheckpointMaterializerTest {

  @Rule
  public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  @BeforeClass
  public static void configure() {
    Args.setParam(new String[]{}, TestConstants.TEST_CONF);
  }

  @AfterClass
  public static void clearConfiguration() {
    Args.clearParam();
  }

  @Test
  public void appliesEachStoreWithSyncBeforePublishingCurrentAndReopens() throws Exception {
    Fixture fixture = fixture("normal", null);
    fixture.code.put(new byte[]{9}, new byte[]{9});

    assertEquals(Status.NEEDS_MATERIALIZATION,
        fixture.materializer.inspect(fixture.target));
    fixture.materializer.materialize(fixture.payload, fixture.target);
    assertEquals(Status.MATERIALIZED, fixture.materializer.inspect(fixture.target));
    assertFalse(java.nio.file.Files.exists(fixture.root.resolve(
        ChainbaseCheckpointMaterializer.CURRENT_FILE)));
    assertArrayEquals(new byte[]{2}, fixture.code.get(new byte[]{1}));
    assertNull(fixture.code.get(new byte[]{9}));
    assertArrayEquals(new byte[]{4}, fixture.storage.get(new byte[]{3}));
    assertEquals(1, fixture.code.syncedFlushes);
    assertEquals(1, fixture.storage.syncedFlushes);

    fixture.materializer.materialize(fixture.payload, fixture.target);
    assertEquals(1, fixture.code.syncedFlushes);
    fixture.materializer.publish(fixture.target);
    assertEquals(Status.PUBLISHED, fixture.materializer.inspect(fixture.target));

    ChainbaseCheckpointMaterializer reopened = new ChainbaseCheckpointMaterializer(fixture.root,
        fixture.format, fixture.databases);
    assertEquals(Status.PUBLISHED, reopened.inspect(fixture.target));
    CommonCheckpointPayload child = payload(fixture.format, 2, hash(1), hash(2), hash(11),
        hash(12));
    CommonCheckpointTarget childTarget = CommonCheckpointTarget.from(child);
    reopened.materialize(child, childTarget);
    reopened.publish(childTarget);
    assertEquals(Status.PUBLISHED, reopened.inspect(childTarget));
  }

  @Test
  public void resumesEveryStoreAndMarkerBoundaryUsingCheckpointRedo() throws Exception {
    for (ChainbaseCheckpointMaterializer.Stage stage
        : ChainbaseCheckpointMaterializer.Stage.values()) {
      Fixture fixture = fixture("fault-" + stage, stage);
      if (stage == ChainbaseCheckpointMaterializer.Stage.AFTER_CURRENT) {
        fixture.materializer.materialize(fixture.payload, fixture.target);
        assertThrows(IOException.class, () -> fixture.materializer.publish(fixture.target));
      } else {
        assertThrows(IOException.class,
            () -> fixture.materializer.materialize(fixture.payload, fixture.target));
      }

      ChainbaseCheckpointMaterializer recovered = new ChainbaseCheckpointMaterializer(
          fixture.root, fixture.format, fixture.databases);
      if (recovered.inspect(fixture.target) == Status.NEEDS_MATERIALIZATION) {
        recovered.materialize(fixture.payload, fixture.target);
      }
      recovered.publish(fixture.target);
      assertEquals(Status.PUBLISHED, recovered.inspect(fixture.target));
      assertArrayEquals(new byte[]{2}, fixture.code.get(new byte[]{1}));
      assertArrayEquals(new byte[]{4}, fixture.storage.get(new byte[]{3}));
    }
  }

  @Test
  public void admitsLegacyMarkerOnlyUntilCentralSlotExists() throws Exception {
    Fixture fixture = fixture("legacy-central", null);
    fixture.materializer.materialize(fixture.payload, fixture.target);
    fixture.materializer.publish(fixture.target);
    CommonCheckpointMaterializedStore store = new CommonCheckpointMaterializedStore(
        fixture.root.resolve("common"));
    ChainbaseCheckpointMaterializer migrated = new ChainbaseCheckpointMaterializer(
        fixture.root, fixture.format, fixture.databases, null, store);

    assertEquals(Status.PUBLISHED, migrated.inspect(fixture.target));
    store.replace(Authority.CHAINBASE, new byte[]{1});
    assertThrows(IOException.class, () -> migrated.inspect(fixture.target));
  }

  @Test
  public void rejectsUnknownStoreForeignFormatAndNonParentTarget() throws Exception {
    Fixture fixture = fixture("reject", null);
    CommonCheckpointPayload unknown = payload(fixture.format, 1, hash(0), hash(1), hash(10),
        hash(11), "unknown");
    assertThrows(IOException.class, () -> fixture.materializer.materialize(unknown,
        CommonCheckpointTarget.from(unknown)));

    CommonCheckpointPayload foreign = payload(hash(99), 1, hash(0), hash(1), hash(10),
        hash(11));
    assertThrows(IOException.class, () -> fixture.materializer.materialize(foreign,
        CommonCheckpointTarget.from(foreign)));

    fixture.materializer.materialize(fixture.payload, fixture.target);
    fixture.materializer.publish(fixture.target);
    CommonCheckpointPayload nonChild = payload(fixture.format, 4, hash(8), hash(9), hash(20),
        hash(21));
    assertThrows(IOException.class,
        () -> fixture.materializer.inspect(CommonCheckpointTarget.from(nonChild)));
  }

  @Test
  public void payloadFactoryCoalescesSnapshotMutationsWithoutDurableReads() throws Exception {
    MemoryDb code = new MemoryDb("code");
    MemoryDb storage = new MemoryDb("storage-row");
    Chainbase codeChainbase = new Chainbase(new SnapshotRoot(code));
    Chainbase storageChainbase = new Chainbase(new SnapshotRoot(storage));
    List<Chainbase> databases = Arrays.asList(codeChainbase, storageChainbase);

    for (int number = 1; number <= 2; number++) {
      BlockSnapshotMeta meta = BlockSnapshotMeta.forBlock(number, hash(number), hash(number - 1),
          number * 3_000L);
      byte[] parentRoot = hash(10 + number - 1);
      byte[] stateRoot = hash(10 + number);
      PathStateSnapshotDelta path = mock(PathStateSnapshotDelta.class);
      when(path.getMeta()).thenReturn(meta);
      when(path.getParentStateRoot()).thenReturn(parentRoot);
      when(path.getStateRoot()).thenReturn(stateRoot);
      when(path.getTransitionPayloadDigest()).thenReturn(hash(50 + number));
      when(path.getStores()).thenReturn(Collections.emptyList());
      when(path.getSuperNodeMutations()).thenReturn(Collections.emptyList());
      BlockReverseDiff archive = new BlockReverseDiff(meta, Collections.emptyList());

      SnapshotImpl codeLayer = append(codeChainbase, meta, archive, path);
      SnapshotImpl storageLayer = append(storageChainbase, meta, archive, path);
      codeLayer.put(new byte[]{1}, new byte[]{(byte) number});
      codeLayer.put(new byte[]{(byte) (10 + number)}, new byte[]{(byte) (20 + number)});
      if (number == 1) {
        storageLayer.put(new byte[]{3}, new byte[]{3});
      } else {
        storageLayer.remove(new byte[]{3});
      }
    }

    CommonCheckpointPayload captured = new CommonCheckpointPayloadFactory().capture(hash(80),
        databases, 2);
    assertEquals(2, captured.getBlocks().size());
    assertEquals(2, captured.getChainbaseStores().size());
    CommonCheckpointPayload.StoreMutations codeStore = captured.getChainbaseStores().get(0);
    assertEquals("code", codeStore.getDbName());
    assertEquals(3, codeStore.getMutations().size());
    CommonCheckpointPayload.Mutation overwritten = codeStore.getMutations().stream()
        .filter(mutation -> Arrays.equals(new byte[]{1}, mutation.getKey()))
        .findFirst().orElseThrow(AssertionError::new);
    assertArrayEquals(new byte[]{2}, overwritten.getValue());
    CommonCheckpointPayload.StoreMutations storageStore =
        captured.getChainbaseStores().get(1);
    assertEquals("storage-row", storageStore.getDbName());
    assertEquals(1, storageStore.getMutations().size());
    assertEquals(true, storageStore.getMutations().get(0).isDelete());
    assertEquals(0, code.getCalls);
    assertEquals(0, storage.getCalls);

    java.nio.file.Path hotRoot = temporaryFolder.newFolder("capture-v2-hot").toPath();
    try (StateArchiveHotStore hotStore = StateArchiveHotStore.openOrCreate(hotRoot, hash(80),
        Engine.LEVELDB, 0, hash(0), 3, 10, 1024 * 1024)) {
      CommonCheckpointCapture capture = new CommonCheckpointPayloadFactory().captureV2(hash(80),
          databases, 2, new StateArchiveHotCheckpointMaterializer(hotStore));
      assertEquals(CommonCheckpointPayload.COORDINATION_FORMAT_VERSION,
          capture.getPayload().getVersion());
      assertEquals(2, capture.getArchiveDiffs().size());
      assertEquals(2, capture.getArchiveBinding().getBlockCount());
      assertEquals(0, hotStore.getMaterializedHead());
      assertThrows(IllegalStateException.class,
          () -> capture.getPayload().getBlocks().get(0).getArchiveDiff());
      CommonCheckpointPayloadCodec codec = new CommonCheckpointPayloadCodec();
      assertEquals(capture.getArchiveBinding(),
          codec.decode(codec.encode(capture.getPayload())).getArchiveBinding());
    }
  }

  @Test
  public void realThreeAuthorityCoordinatorCrossesBothBarriersThenRetiresWal()
      throws Exception {
    java.nio.file.Path root = temporaryFolder.newFolder("three-authority").toPath();
    byte[] format = hash(88);
    MemoryDb code = new MemoryDb("code");
    Chainbase codeChainbase = new Chainbase(new SnapshotRoot(code));
    List<Chainbase> databases = Collections.singletonList(codeChainbase);
    PathStateParticipantScope scope = new PathStateCanonicalizer().participantScope();
    CommonCheckpointPayload payload = integratedPayload(format, scope);
    CommonCheckpointTarget target = CommonCheckpointTarget.from(payload);
    CommonCheckpointMaterializedStore materializedStore =
        new CommonCheckpointMaterializedStore(root.resolve("wal"));

    try (PathStatePhysicalStoreSet pathStores = PathStatePhysicalStoreSet.open(
        root.resolve("path-state"), scope, Engine.ROCKSDB)) {
      ChainbaseCheckpointMaterializer chainbase = new ChainbaseCheckpointMaterializer(
          root.resolve("chainbase"), format, databases, null, materializedStore);
      PathStateCheckpointMaterializer pathState = new PathStateCheckpointMaterializer(pathStores,
          scope, format, null, materializedStore);
      StateArchiveCheckpointMaterializer archive = new StateArchiveCheckpointMaterializer(
          root.resolve("archive"), format, null, Engine.LEVELDB, materializedStore);
      CommonCheckpointFile file = new CommonCheckpointFile(root.resolve("wal"));
      CommonCheckpointRedoCoordinator coordinator = new CommonCheckpointRedoCoordinator(file,
          chainbase, pathState, archive);

      assertEquals(CommonCheckpointRedoCoordinator.RecoveryAction.COMPLETED_REDO,
          coordinator.apply(payload));
      assertEquals(Status.PUBLISHED, chainbase.inspect(target));
      assertEquals(Status.PUBLISHED, pathState.inspect(target));
      assertEquals(Status.PUBLISHED, archive.inspect(target));
      assertEquals(CommonCheckpointRedoCoordinator.RecoveryAction.NO_CHECKPOINT,
          coordinator.recover());
      assertArrayEquals(new byte[]{2}, code.get(new byte[]{1}));
      assertArrayEquals(new byte[]{4}, pathStores.participant("account").getFlat(
          new byte[]{3}));
      assertFalse(java.nio.file.Files.exists(root.resolve("wal").resolve(
          CommonCheckpointFile.FILE_NAME)));
      try (java.util.stream.Stream<java.nio.file.Path> markers = java.nio.file.Files.list(
          root.resolve("wal/materialized"))) {
        assertEquals(3L, markers.count());
      }
      assertFalse(java.nio.file.Files.exists(
          root.resolve("chainbase/chainbase-checkpoint-materialized")));
      assertFalse(java.nio.file.Files.exists(
          root.resolve("path-state/checkpoint-materialized")));
      try (java.util.stream.Stream<java.nio.file.Path> paths = java.nio.file.Files.walk(
          root.resolve("archive/checkpoint-targets"))) {
        assertFalse(paths.anyMatch(path -> "MATERIALIZED".equals(
            path.getFileName().toString())));
      }
      coordinator.close();
      CommonCheckpointMaterializedStore reopenedStore =
          new CommonCheckpointMaterializedStore(root.resolve("wal"));
      ChainbaseCheckpointMaterializer reopenedChainbase =
          new ChainbaseCheckpointMaterializer(root.resolve("chainbase"), format, databases,
              null, reopenedStore);
      PathStateCheckpointMaterializer reopenedPathState =
          new PathStateCheckpointMaterializer(pathStores, scope, format, null, reopenedStore);
      StateArchiveCheckpointMaterializer reopenedArchive =
          new StateArchiveCheckpointMaterializer(root.resolve("archive"), format, null,
              Engine.LEVELDB, reopenedStore);
      assertEquals(Status.PUBLISHED, reopenedChainbase.inspect(target));
      assertEquals(Status.PUBLISHED, reopenedPathState.inspect(target));
      assertEquals(Status.PUBLISHED, reopenedArchive.inspect(target));
      reopenedArchive.close();
    }
  }

  @Test
  public void snapshotRebaserDropsOnlyMaterializedPrefixWithoutSecondStoreWrite()
      throws Exception {
    java.nio.file.Path root = temporaryFolder.newFolder("snapshot-rebase").toPath();
    MemoryDb code = new MemoryDb("code");
    MemoryDb storage = new MemoryDb("storage-row");
    Chainbase codeChainbase = new Chainbase(new SnapshotRoot(code));
    Chainbase storageChainbase = new Chainbase(new SnapshotRoot(storage));
    List<Chainbase> databases = Arrays.asList(codeChainbase, storageChainbase);
    for (int number = 1; number <= 3; number++) {
      BlockSnapshotMeta meta = BlockSnapshotMeta.forBlock(number, hash(number), hash(number - 1),
          number * 3_000L);
      PathStateSnapshotDelta path = pathDelta(meta, hash(10 + number - 1), hash(10 + number),
          hash(40 + number));
      BlockReverseDiff archive = new BlockReverseDiff(meta, Collections.emptyList());
      append(codeChainbase, meta, archive, path).put(new byte[]{1},
          new byte[]{(byte) number});
      append(storageChainbase, meta, archive, path).put(new byte[]{3},
          new byte[]{(byte) number});
    }
    byte[] format = hash(80);
    CommonCheckpointPayload payload = new CommonCheckpointPayloadFactory().capture(format,
        databases, 2);
    CommonCheckpointTarget target = CommonCheckpointTarget.from(payload);
    ChainbaseCheckpointMaterializer materializer = new ChainbaseCheckpointMaterializer(root,
        format, databases);
    materializer.materialize(payload, target);
    materializer.publish(target);
    assertEquals(1, code.syncedFlushes);
    assertEquals(1, storage.syncedFlushes);

    new CommonCheckpointSnapshotRebaser().rebase(databases, target, 2);
    assertEquals(1, code.syncedFlushes);
    assertEquals(1, storage.syncedFlushes);
    assertArrayEquals(new byte[]{2}, code.get(new byte[]{1}));
    assertArrayEquals(new byte[]{2}, storage.get(new byte[]{3}));
    assertArrayEquals(new byte[]{3}, codeChainbase.getUnchecked(new byte[]{1}));
    assertArrayEquals(new byte[]{3}, storageChainbase.getUnchecked(new byte[]{3}));
    assertSame(codeChainbase.getHead().getRoot(), codeChainbase.getHead().getPrevious());
    assertSame(storageChainbase.getHead().getRoot(),
        storageChainbase.getHead().getPrevious());
  }

  @Test
  public void snapshotRebaserPrevalidatesEveryStoreBeforeChangingAnyChain() {
    MemoryDb code = new MemoryDb("code");
    MemoryDb storage = new MemoryDb("storage-row");
    Chainbase codeChainbase = new Chainbase(new SnapshotRoot(code));
    Chainbase storageChainbase = new Chainbase(new SnapshotRoot(storage));
    CommonCheckpointTarget target = CommonCheckpointTarget.from(payload(hash(80), 1, hash(0),
        hash(1), hash(10), hash(11)));
    SnapshotImpl codeLayer = (SnapshotImpl) codeChainbase.getHead().advance();
    codeLayer.attachBlockArtifacts(target.getFirstBlock(), null, null);
    codeChainbase.setHead(codeLayer);
    SnapshotImpl storageLayer = (SnapshotImpl) storageChainbase.getHead().advance();
    storageLayer.attachBlockArtifacts(BlockSnapshotMeta.forBlock(1, hash(9), hash(0), 3_000L),
        null, null);
    storageChainbase.setHead(storageLayer);

    assertThrows(IOException.class, () -> new CommonCheckpointSnapshotRebaser().rebase(
        Arrays.asList(codeChainbase, storageChainbase), target, 1));
    assertSame(codeLayer, codeChainbase.getHead());
    assertSame(storageLayer, storageChainbase.getHead());
  }

  @Test
  public void memoryRebaseFailureLeavesEverySnapshotPointerUnchanged() throws Exception {
    java.nio.file.Path root = temporaryFolder.newFolder("memory-rebase-failure").toPath();
    byte[] format = hash(94);
    MemoryDb code = new MemoryDb("code");
    Chainbase database = new Chainbase(new SnapshotRoot(code));
    List<Chainbase> databases = Collections.singletonList(database);
    BlockSnapshotMeta meta = BlockSnapshotMeta.forBlock(1, hash(1), hash(0), 3_000L);
    byte[] view = hash(41);
    PathStateSnapshotDelta path = pathDelta(meta, hash(10), hash(11), view);
    BlockReverseDiff archiveBlock = new BlockReverseDiff(meta, Collections.emptyList());
    SnapshotImpl layer = append(database, meta, archiveBlock, path);
    layer.put(new byte[]{1}, new byte[]{2});

    ChainbaseCheckpointMaterializer chainbase = new ChainbaseCheckpointMaterializer(
        root.resolve("chainbase"), format, databases);
    PublishingMaterializer pathState = new PublishingMaterializer(Authority.PATH_STATE);
    StateArchiveCheckpointMaterializer archive = new StateArchiveCheckpointMaterializer(
        root.resolve("archive"), format);
    CommonCheckpointRedoCoordinator coordinator = new CommonCheckpointRedoCoordinator(
        new CommonCheckpointFile(root.resolve("wal")), chainbase, pathState, archive);
    CommonCheckpointRuntime runtime = new CommonCheckpointRuntime(
        new CommonCheckpointRuntimeOwner(coordinator), databases, root.resolve("archive"),
        format, Engine.LEVELDB,
        (blockNumber, blockHash) -> new TestLatest(code, blockNumber, blockHash),
        target -> {
          throw new IOException("injected memory rebase prepare failure");
        });

    runtime.recoverBeforeServing();
    IOException failure = assertThrows(IOException.class, () -> runtime.checkpointAndRebase(1));
    assertEquals("injected memory rebase prepare failure", failure.getMessage());
    assertSame(layer, database.getHead());
    assertSame(layer.getRoot(), layer.getPrevious());
    assertEquals(CommonCheckpointRuntimeOwner.State.FAILED, runtime.getState());
    assertFalse(java.nio.file.Files.exists(root.resolve("wal").resolve(
        CommonCheckpointFile.FILE_NAME)));
  }

  @Test
  public void runtimeComposesStartupCheckpointRebaseAndPointQuery() throws Exception {
    java.nio.file.Path root = temporaryFolder.newFolder("composed-runtime").toPath();
    byte[] format = hash(93);
    MemoryDb code = new MemoryDb("code");
    Chainbase database = new Chainbase(new SnapshotRoot(code));
    List<Chainbase> databases = Collections.singletonList(database);
    BlockSnapshotMeta meta = BlockSnapshotMeta.forBlock(1, hash(1), hash(0), 3_000L);
    byte[] view = hash(41);
    PathStateSnapshotDelta path = pathDelta(meta, hash(10), hash(11), view);
    BlockReverseDiff archiveBlock = new BlockReverseDiff(meta,
        Collections.singletonList(new DbGroup("code", Collections.singletonList(
            new Entry(new byte[]{1}, OldValue.present(new byte[]{0}))))));
    append(database, meta, archiveBlock, path).put(new byte[]{1}, new byte[]{2});

    ChainbaseCheckpointMaterializer chainbase = new ChainbaseCheckpointMaterializer(
        root.resolve("chainbase"), format, databases);
    PublishingMaterializer pathState = new PublishingMaterializer(Authority.PATH_STATE);
    StateArchiveCheckpointMaterializer archive = new StateArchiveCheckpointMaterializer(
        root.resolve("archive"), format);
    CommonCheckpointRedoCoordinator coordinator = new CommonCheckpointRedoCoordinator(
        new CommonCheckpointFile(root.resolve("wal")), chainbase, pathState, archive);
    AtomicLong clock = new AtomicLong();
    List<CommonCheckpointRuntime.Timing> timings = new ArrayList<>();
    CommonCheckpointRuntime runtime = new CommonCheckpointRuntime(
        new CommonCheckpointRuntimeOwner(coordinator), databases, root.resolve("archive"),
        format, Engine.LEVELDB,
        (blockNumber, blockHash) -> new TestLatest(code, blockNumber, blockHash),
        target -> () -> { }, () -> clock.addAndGet(1_000L), timings::add);

    assertEquals(CommonCheckpointRedoCoordinator.RecoveryAction.NO_CHECKPOINT,
        runtime.recoverBeforeServing());
    CommonCheckpointTarget target = runtime.checkpointAndRebase(1);
    assertEquals(meta, target.getLastBlock());
    assertThrows(IllegalStateException.class, target::getArchiveBinding);
    assertEquals(1, timings.size());
    CommonCheckpointRuntime.Timing timing = timings.get(0);
    assertEquals(1, timing.getHead());
    assertEquals(1, timing.getBlocks());
    assertEquals(1, timing.getPayloadCaptureUs());
    assertEquals(0, timing.getHotPrepareUs());
    assertTrue(timing.getOwnerApplyUs() > 0);
    assertEquals(1, timing.getChainbaseRebasePrepareUs());
    assertEquals(1, timing.getPathStateRebasePrepareUs());
    assertEquals(1, timing.getChainbaseRebaseApplyUs());
    assertEquals(1, timing.getPathStateRebaseApplyUs());
    assertTrue(timing.getTotalUs() >= timing.getOwnerApplyUs());
    assertSame(database.getHead().getRoot(), database.getHead());
    assertEquals(1, code.syncedFlushes);
    try (StateArchiveCheckpointReadSnapshot snapshot = runtime.pinPoint(0)) {
      assertArrayEquals(new byte[]{0}, snapshot.get("code", new byte[]{1}).getValue());
    }
    try (StateArchiveCheckpointReadSnapshot snapshot = runtime.pinPoint(1)) {
      assertArrayEquals(new byte[]{2}, snapshot.get("code", new byte[]{1}).getValue());
    }
    java.nio.file.Files.delete(root.resolve("archive/checkpoint-serving-index/ENGINE"));
    try (StateArchiveCheckpointReadSnapshot snapshot = runtime.pinPoint(1)) {
      assertArrayEquals(new byte[]{2}, snapshot.get("code", new byte[]{1}).getValue());
    }
    assertThrows(IOException.class, () -> StateArchiveCheckpointMaterializer.loadPublishedTarget(
        root.resolve("archive"), format, Engine.LEVELDB));
    runtime.close();
    assertEquals(CommonCheckpointRuntimeOwner.State.CLOSED, runtime.getState());
  }

  @Test
  public void hotRuntimePreparesV2BeforeWalAndCompletesBothBarriers() throws Exception {
    java.nio.file.Path root = temporaryFolder.newFolder("hot-runtime-v2").toPath();
    byte[] format = hash(94);
    V2Snapshots snapshots = new V2Snapshots();
    StateArchiveHotStore hotStore = StateArchiveHotStore.openOrCreate(root.resolve("hot"),
        format, Engine.LEVELDB, 0, hash(0), 3, 10, 1024 * 1024);
    List<CommonCheckpointRuntime.Timing> timings = new ArrayList<>();
    CommonCheckpointRuntime runtime = hotRuntime(root,
        new CommonCheckpointFile(root.resolve("wal")), snapshots, hotStore, format, timings);

    assertEquals(CommonCheckpointRedoCoordinator.RecoveryAction.NO_CHECKPOINT,
        runtime.recoverBeforeServing());
    CommonCheckpointTarget target = runtime.checkpointAndRebase(1);

    assertEquals(snapshots.meta, target.getLastBlock());
    assertEquals(1, target.getArchiveBinding().getBlockCount());
    assertEquals(1, hotStore.getMaterializedHead());
    assertEquals(1, hotStore.getCommittedHead());
    assertFalse(java.nio.file.Files.exists(root.resolve("wal")
        .resolve(CommonCheckpointFile.FILE_NAME)));
    assertSame(snapshots.codeDatabase.getHead().getRoot(), snapshots.codeDatabase.getHead());
    assertSame(snapshots.propertiesDatabase.getHead().getRoot(),
        snapshots.propertiesDatabase.getHead());
    assertEquals(1, timings.size());
    assertEquals(1, timings.get(0).getHotPrepareUs());
    assertThrows(IOException.class, () -> runtime.pinPoint(1));
    runtime.close();
  }

  @Test
  public void appendRuntimePreparesSap3BeforeWalAndReopensPublishedTarget() throws Exception {
    java.nio.file.Path root = temporaryFolder.newFolder("append-runtime-v3").toPath();
    java.nio.file.Path history = root.resolve("history");
    byte[] format = hash(96);
    byte[] baselineHistory = hash(70);
    V2Snapshots snapshots = new V2Snapshots();
    StateArchiveAppendCheckpointMaterializerV3 append =
        new StateArchiveAppendCheckpointMaterializerV3(history, format, Engine.LEVELDB,
            baselineHistory, StateArchiveFileFormatV3.COMPRESSION_NONE, 10_000);
    CommonCheckpointRedoCoordinator coordinator = new CommonCheckpointRedoCoordinator(
        new CommonCheckpointFile(root.resolve("wal")),
        new ChainbaseCheckpointMaterializer(root.resolve("chainbase"), format,
            snapshots.databases),
        new PublishingMaterializer(Authority.PATH_STATE), append);
    AtomicLong clock = new AtomicLong();
    List<CommonCheckpointRuntime.Timing> timings = new ArrayList<>();
    CommonCheckpointRuntime runtime = new CommonCheckpointRuntime(
        new CommonCheckpointRuntimeOwner(coordinator), snapshots.databases, history,
        format, Engine.LEVELDB,
        (blockNumber, blockHash) -> new TestLatest(snapshots.code, blockNumber, blockHash),
        target -> () -> { }, null, append, null, () -> clock.addAndGet(1_000L),
        timings::add);

    assertEquals(CommonCheckpointRedoCoordinator.RecoveryAction.NO_CHECKPOINT,
        runtime.recoverBeforeServing());
    CommonCheckpointTarget target = runtime.checkpointAndRebase(1);

    assertEquals(snapshots.meta, target.getLastBlock());
    assertEquals(1, target.getArchiveBinding().getBlockCount());
    assertEquals(Status.PUBLISHED, append.inspect(target));
    assertEquals(760, java.nio.file.Files.size(
        history.resolve(StateArchiveFiveLaneDurabilityProofV3.FILE_NAME)));
    assertFalse(java.nio.file.Files.exists(
        root.resolve("wal").resolve(CommonCheckpointFile.FILE_NAME)));
    assertSame(snapshots.codeDatabase.getHead().getRoot(), snapshots.codeDatabase.getHead());
    assertSame(snapshots.propertiesDatabase.getHead().getRoot(),
        snapshots.propertiesDatabase.getHead());
    assertEquals(1, timings.size());
    assertEquals(1, timings.get(0).getHotPrepareUs());
    assertThrows(IOException.class, () -> runtime.pinPoint(1));
    runtime.close();

    try (StateArchiveAppendCheckpointMaterializerV3 reopened =
        new StateArchiveAppendCheckpointMaterializerV3(history, format, Engine.LEVELDB,
            baselineHistory, StateArchiveFileFormatV3.COMPRESSION_NONE, 10_000)) {
      assertEquals(Status.PUBLISHED, reopened.inspect(target));
    }
  }

  @Test
  public void hotRuntimeRetriesAfterPrepareButBeforeWalPublication() throws Exception {
    java.nio.file.Path root = temporaryFolder.newFolder("hot-runtime-retry").toPath();
    byte[] format = hash(95);
    V2Snapshots snapshots = new V2Snapshots();
    StateArchiveHotStore firstHot = StateArchiveHotStore.openOrCreate(root.resolve("hot"),
        format, Engine.LEVELDB, 0, hash(0), 3, 10, 1024 * 1024);
    AtomicBoolean failedOnce = new AtomicBoolean();
    CommonCheckpointFile interruptedFile = new CommonCheckpointFile(root.resolve("wal"),
        CommonCheckpointPayloadCodec.DEFAULT_MAX_ENCODED_LENGTH, (stage, path) -> {
      if (stage == CommonCheckpointFile.Stage.AFTER_TEMPORARY_FORCE
          && failedOnce.compareAndSet(false, true)) {
        throw new IOException("injected failure after Hot prepare and temporary WAL force");
      }
    });
    CommonCheckpointRuntime interrupted = hotRuntime(root, interruptedFile, snapshots, firstHot,
        format, new ArrayList<>());
    interrupted.recoverBeforeServing();

    assertThrows(IOException.class, () -> interrupted.checkpointAndRebase(1));
    assertEquals(CommonCheckpointRuntimeOwner.State.FAILED, interrupted.getState());
    assertFalse(java.nio.file.Files.exists(root.resolve("wal")
        .resolve(CommonCheckpointFile.FILE_NAME)));
    assertTrue(java.nio.file.Files.exists(root.resolve("wal")
        .resolve(CommonCheckpointFile.TEMPORARY_FILE_NAME)));
    assertSame(snapshots.codeLayer, snapshots.codeDatabase.getHead());

    StateArchiveHotStore recoveredHot = StateArchiveHotStore.openOrCreate(root.resolve("hot"),
        format, Engine.LEVELDB, 0, hash(0), 3, 10, 1024 * 1024);
    assertEquals(1, recoveredHot.getMaterializedHead());
    assertEquals(0, recoveredHot.getCommittedHead());
    CommonCheckpointRuntime recovered = hotRuntime(root,
        new CommonCheckpointFile(root.resolve("wal")), snapshots, recoveredHot, format,
        new ArrayList<>());
    assertEquals(CommonCheckpointRedoCoordinator.RecoveryAction.NO_CHECKPOINT,
        recovered.recoverBeforeServing());
    assertEquals(0, recoveredHot.getMaterializedHead());

    CommonCheckpointTarget target = recovered.checkpointAndRebase(1);
    assertEquals(snapshots.meta, target.getLastBlock());
    assertEquals(1, recoveredHot.getCommittedHead());
    assertFalse(java.nio.file.Files.exists(root.resolve("wal")
        .resolve(CommonCheckpointFile.TEMPORARY_FILE_NAME)));
    recovered.close();
  }

  private Fixture fixture(String name, ChainbaseCheckpointMaterializer.Stage failedStage)
      throws Exception {
    java.nio.file.Path root = temporaryFolder.newFolder(name).toPath();
    MemoryDb code = new MemoryDb("code");
    MemoryDb storage = new MemoryDb("storage-row");
    List<Chainbase> databases = Arrays.asList(
        new Chainbase(new SnapshotRoot(code)), new Chainbase(new SnapshotRoot(storage)));
    byte[] format = hash(80);
    CommonCheckpointPayload payload = payload(format, 1, hash(0), hash(1), hash(10), hash(11));
    ChainbaseCheckpointMaterializer materializer = new ChainbaseCheckpointMaterializer(root,
        format, databases, failAt(failedStage));
    return new Fixture(root, code, storage, databases, format, payload, materializer);
  }

  private static CommonCheckpointRuntime hotRuntime(java.nio.file.Path root,
      CommonCheckpointFile file, V2Snapshots snapshots, StateArchiveHotStore hotStore,
      byte[] format, List<CommonCheckpointRuntime.Timing> timings) {
    StateArchiveHotCheckpointMaterializer hotMaterializer =
        new StateArchiveHotCheckpointMaterializer(hotStore);
    BlockSnapshotMeta persisted = BlockSnapshotMeta.forBlock(0, hash(0), hash(-1), 0);
    CommonCheckpointHotRecovery recovery = new CommonCheckpointHotRecovery(file,
        () -> new CommonCheckpointHotRecovery.PersistentDynamicHead(0, hash(0)),
        ignored -> persisted, hotMaterializer::reconcilePreparedTail);
    CommonCheckpointRedoCoordinator coordinator = new CommonCheckpointRedoCoordinator(file,
        new ChainbaseCheckpointMaterializer(root.resolve("chainbase"), format,
            snapshots.databases),
        new PublishingMaterializer(Authority.PATH_STATE), hotMaterializer);
    AtomicLong clock = new AtomicLong();
    return new CommonCheckpointRuntime(new CommonCheckpointRuntimeOwner(coordinator),
        snapshots.databases, root.resolve("legacy-archive"), format, Engine.LEVELDB,
        (blockNumber, blockHash) -> new TestLatest(snapshots.code, blockNumber, blockHash),
        target -> () -> { }, hotMaterializer, recovery, () -> clock.addAndGet(1_000L),
        timings::add);
  }

  private static ChainbaseCheckpointMaterializer.FaultHook failAt(
      ChainbaseCheckpointMaterializer.Stage failedStage) {
    return (stage, dbName) -> {
      if (stage == failedStage) {
        throw new IOException("injected " + stage + " at " + dbName);
      }
    };
  }

  private static CommonCheckpointPayload payload(byte[] format, long blockNumber,
      byte[] parentHash, byte[] blockHash, byte[] parentRoot, byte[] stateRoot,
      String... storeOverride) {
    BlockSnapshotMeta meta = BlockSnapshotMeta.forBlock(blockNumber, blockHash, parentHash,
        blockNumber * 3_000L);
    PathStateFlushTarget.BlockBinding binding = mock(PathStateFlushTarget.BlockBinding.class);
    when(binding.getMeta()).thenReturn(meta);
    when(binding.getParentStateRoot()).thenReturn(parentRoot);
    when(binding.getStateRoot()).thenReturn(stateRoot);
    when(binding.getTransitionPayloadDigest()).thenReturn(hash(50 + (int) blockNumber));
    PathStateFlushTarget pathState = mock(PathStateFlushTarget.class);
    when(pathState.getBlocks()).thenReturn(Collections.singletonList(binding));
    when(pathState.getParentStateRoot()).thenReturn(parentRoot);
    when(pathState.getStateRoot()).thenReturn(stateRoot);
    when(pathState.getStores()).thenReturn(Collections.emptyList());
    when(pathState.getSuperNodeMutations()).thenReturn(Collections.emptyList());

    List<CommonCheckpointPayload.StoreMutations> stores = new ArrayList<>();
    String firstName = storeOverride.length == 0 ? "code" : storeOverride[0];
    stores.add(new CommonCheckpointPayload.StoreMutations(firstName, Arrays.asList(
        new CommonCheckpointPayload.Mutation(new byte[]{1}, new byte[]{2}),
        new CommonCheckpointPayload.Mutation(new byte[]{9}, null))));
    if (storeOverride.length == 0) {
      stores.add(new CommonCheckpointPayload.StoreMutations("storage-row",
          Collections.singletonList(
              new CommonCheckpointPayload.Mutation(new byte[]{3}, new byte[]{4}))));
    }
    BlockReverseDiff archive = new BlockReverseDiff(meta, Collections.emptyList());
    return CommonCheckpointPayload.create(format, pathState,
        Collections.singletonList(archive), stores);
  }

  private static byte[] hash(int seed) {
    byte[] hash = new byte[32];
    for (int index = 0; index < hash.length; index++) {
      hash[index] = (byte) (seed + index);
    }
    return hash;
  }

  private static CommonCheckpointPayload integratedPayload(byte[] format,
      PathStateParticipantScope scope) {
    long blockNumber = 1;
    BlockSnapshotMeta meta = BlockSnapshotMeta.forBlock(blockNumber, hash(1), hash(0), 3_000L);
    PathStateFlushTarget.BlockBinding binding = mock(PathStateFlushTarget.BlockBinding.class);
    when(binding.getMeta()).thenReturn(meta);
    when(binding.getParentStateRoot()).thenReturn(hash(10));
    when(binding.getStateRoot()).thenReturn(hash(11));
    when(binding.getTransitionPayloadDigest()).thenReturn(hash(51));
    PathStateFlushTarget.StoreTarget account = mock(PathStateFlushTarget.StoreTarget.class);
    when(account.getStoreId()).thenReturn(scope.require("account").getStoreId());
    when(account.getDbName()).thenReturn("account");
    when(account.getStoreRoot()).thenReturn(hash(61));
    PathStateSnapshotDelta.Mutation flat = mockMutation(new byte[]{3}, new byte[]{4});
    PathStateSnapshotDelta.Mutation node = mockMutation(new byte[]{5}, new byte[]{6});
    PathStateSnapshotDelta.Mutation superNode = mockMutation(new byte[]{7}, new byte[]{8});
    when(account.getFlatMutations()).thenReturn(Collections.singletonList(flat));
    when(account.getNodeMutations()).thenReturn(Collections.singletonList(node));
    PathStateFlushTarget pathState = mock(PathStateFlushTarget.class);
    when(pathState.getBlocks()).thenReturn(Collections.singletonList(binding));
    when(pathState.getParentStateRoot()).thenReturn(hash(10));
    when(pathState.getStateRoot()).thenReturn(hash(11));
    when(pathState.getStores()).thenReturn(Collections.singletonList(account));
    when(pathState.getSuperNodeMutations()).thenReturn(Collections.singletonList(superNode));
    List<CommonCheckpointPayload.StoreMutations> chainbase = Collections.singletonList(
        new CommonCheckpointPayload.StoreMutations("code", Collections.singletonList(
            new CommonCheckpointPayload.Mutation(new byte[]{1}, new byte[]{2}))));
    return CommonCheckpointPayload.create(format, pathState,
        Collections.singletonList(new BlockReverseDiff(meta, Collections.emptyList())),
        chainbase);
  }

  private static PathStateSnapshotDelta.Mutation mockMutation(byte[] key, byte[] value) {
    PathStateSnapshotDelta.Mutation mutation = mock(PathStateSnapshotDelta.Mutation.class);
    when(mutation.getKey()).thenReturn(key);
    when(mutation.getValue()).thenReturn(value);
    return mutation;
  }

  private static SnapshotImpl append(Chainbase database, BlockSnapshotMeta meta,
      BlockReverseDiff archive, PathStateSnapshotDelta path) {
    SnapshotImpl layer = (SnapshotImpl) database.getHead().advance();
    layer.attachBlockArtifacts(meta, archive, path);
    database.setHead(layer);
    return layer;
  }

  private static PathStateSnapshotDelta pathDelta(BlockSnapshotMeta meta, byte[] parentRoot,
      byte[] stateRoot, byte[] view) {
    PathStateSnapshotDelta path = mock(PathStateSnapshotDelta.class);
    when(path.getMeta()).thenReturn(meta);
    when(path.getParentStateRoot()).thenReturn(parentRoot);
    when(path.getStateRoot()).thenReturn(stateRoot);
    when(path.getTransitionPayloadDigest()).thenReturn(hash(50 + (int) meta.getBlockNumber()));
    when(path.getStores()).thenReturn(Collections.emptyList());
    when(path.getSuperNodeMutations()).thenReturn(Collections.emptyList());
    return path;
  }

  private static final class Fixture {

    private final java.nio.file.Path root;
    private final MemoryDb code;
    private final MemoryDb storage;
    private final List<Chainbase> databases;
    private final byte[] format;
    private final CommonCheckpointPayload payload;
    private final CommonCheckpointTarget target;
    private final ChainbaseCheckpointMaterializer materializer;

    private Fixture(java.nio.file.Path root, MemoryDb code, MemoryDb storage,
        List<Chainbase> databases, byte[] format, CommonCheckpointPayload payload,
        ChainbaseCheckpointMaterializer materializer) {
      this.root = root;
      this.code = code;
      this.storage = storage;
      this.databases = databases;
      this.format = format;
      this.payload = payload;
      this.target = CommonCheckpointTarget.from(payload);
      this.materializer = materializer;
    }
  }

  private static final class V2Snapshots {

    private final MemoryDb code = new MemoryDb("code");
    private final MemoryDb properties = new MemoryDb("properties");
    private final Chainbase codeDatabase = new Chainbase(new SnapshotRoot(code));
    private final Chainbase propertiesDatabase = new Chainbase(new SnapshotRoot(properties));
    private final List<Chainbase> databases = Arrays.asList(codeDatabase, propertiesDatabase);
    private final BlockSnapshotMeta meta = BlockSnapshotMeta.forBlock(1, hash(1), hash(0),
        3_000L);
    private final SnapshotImpl codeLayer;

    private V2Snapshots() {
      byte[] view = hash(41);
      PathStateSnapshotDelta path = pathDelta(meta, hash(10), hash(11), view);
      BlockReverseDiff archive = new BlockReverseDiff(meta,
          Collections.singletonList(new DbGroup("code", Collections.singletonList(
              new Entry(new byte[]{1}, OldValue.present(new byte[]{0}))))));
      codeLayer = append(codeDatabase, meta, archive, path);
      SnapshotImpl propertiesLayer = append(propertiesDatabase, meta, archive, path);
      codeLayer.put(new byte[]{1}, new byte[]{2});
      propertiesLayer.put("latest_block_header_number".getBytes(StandardCharsets.UTF_8),
          ByteBuffer.allocate(Long.BYTES).putLong(1).array());
      propertiesLayer.put("latest_block_header_hash".getBytes(StandardCharsets.UTF_8), hash(1));
    }
  }

  private static final class PublishingMaterializer implements CommonCheckpointMaterializer {

    private final Authority authority;
    private Status status = Status.NEEDS_MATERIALIZATION;
    private CommonCheckpointTarget target;

    private PublishingMaterializer(Authority authority) {
      this.authority = authority;
    }

    @Override
    public Authority authority() {
      return authority;
    }

    @Override
    public Status inspect(CommonCheckpointTarget expected) throws IOException {
      if (target != null && !target.equals(expected)) {
        throw new IOException("test materializer target mismatch");
      }
      return status;
    }

    @Override
    public void materialize(CommonCheckpointPayload payload, CommonCheckpointTarget expected) {
      target = expected;
      status = Status.MATERIALIZED;
    }

    @Override
    public void publish(CommonCheckpointTarget expected) throws IOException {
      if (status != Status.MATERIALIZED || !expected.equals(target)) {
        throw new IOException("test materializer publish without materialization");
      }
      status = Status.PUBLISHED;
    }
  }

  private static final class TestLatest implements PinnedLatestState {

    private final MemoryDb database;
    private final long blockNumber;
    private final byte[] blockHash;

    private TestLatest(MemoryDb database, long blockNumber, byte[] blockHash) {
      this.database = database;
      this.blockNumber = blockNumber;
      this.blockHash = Arrays.copyOf(blockHash, blockHash.length);
    }

    @Override
    public long getBlockNumber() {
      return blockNumber;
    }

    @Override
    public byte[] getBlockHash() {
      return Arrays.copyOf(blockHash, blockHash.length);
    }

    @Override
    public OldValue get(String dbName, byte[] physicalRawKey) {
      if (!"code".equals(dbName)) {
        throw new IllegalArgumentException("unexpected test Store " + dbName);
      }
      return OldValue.fromNullable(database.get(physicalRawKey));
    }

    @Override
    public List<HistoricalRangeOverlay.Entry> range(String dbName, byte[] lowerInclusive,
        byte[] upperExclusive, int maxEntries) {
      throw new UnsupportedOperationException("point-only test latest");
    }

    @Override
    public void close() {
    }
  }

  private static final class MemoryDb implements DB<byte[], byte[]>, Flusher {

    private final String name;
    private final Map<WrappedByteArray, byte[]> values = new LinkedHashMap<>();
    private int syncedFlushes;
    private int getCalls;

    private MemoryDb(String name) {
      this.name = name;
    }

    @Override
    public byte[] get(byte[] key) {
      getCalls++;
      return values.get(WrappedByteArray.of(key));
    }

    @Override
    public void put(byte[] key, byte[] value) {
      values.put(WrappedByteArray.of(key), value);
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
      Map<byte[], byte[]> copy = new LinkedHashMap<>();
      values.forEach((key, value) -> copy.put(key.getBytes(), value));
      return copy.entrySet().iterator();
    }

    @Override
    public void close() {
    }

    @Override
    public String getDbName() {
      return name;
    }

    @Override
    public void stat() {
    }

    @Override
    public DB<byte[], byte[]> newInstance() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void flush(Map<WrappedByteArray, WrappedByteArray> batch) {
      apply(batch);
    }

    @Override
    public void flushSynced(Map<WrappedByteArray, WrappedByteArray> batch) {
      syncedFlushes++;
      apply(batch);
    }

    @Override
    public void reset() {
      values.clear();
    }

    private void apply(Map<WrappedByteArray, WrappedByteArray> batch) {
      batch.forEach((key, value) -> {
        if (value.getBytes() == null) {
          values.remove(key);
        } else {
          values.put(key, value.getBytes());
        }
      });
    }
  }
}
