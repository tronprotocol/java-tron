package org.tron.core.db2.archive;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tron.core.config.args.StorageConfig.NativeDbConfig;
import org.tron.core.config.args.StorageConfig.StateArchiveHotStoreConfig;
import org.tron.core.db2.archive.BlockReverseDiff.DbGroup;
import org.tron.core.db2.archive.BlockReverseDiff.Entry;
import org.tron.core.db2.stateroot.PathStateStoreManifest.Engine;

public class StateArchiveHotStoreTest {

  @Rule
  public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void appendsSealsQueriesAndReopensAcrossBothEngines() throws Exception {
    for (Engine engine : Engine.values()) {
      Path root = temporaryFolder.newFolder("hot-" + engine.name()).toPath();
      byte[] format = hash(90);
      try (StateArchiveHotStore store = open(root, format, engine, 0, hash(0), 3, 2)) {
        store.appendSolidified(Arrays.asList(
            diff(1, 0, "code", new byte[]{1}, OldValue.absent()),
            diff(2, 1, "code", new byte[]{1}, OldValue.present(new byte[0]))));
        assertTrue(store.shouldSealCurrent());
        assertLookup(store.findOldValueAfter("code", new byte[]{1}, 0), 1,
            OldValue.absent());
        assertLookup(store.findOldValueAfter("code", new byte[]{1}, 1), 2,
            OldValue.present(new byte[0]));
        assertEquals(0, store.sealCurrent());
        assertEquals(Collections.singletonList(0L), store.getFrozenGenerationIds());
        assertEquals(1, store.getCurrentGenerationId());
        store.appendSolidified(Collections.singletonList(
            diff(3, 2, "account", new byte[]{3}, OldValue.present(new byte[]{33}))));
      }

      try (StateArchiveHotStore reopened = open(root, format, engine, 0, hash(0), 3, 2)) {
        assertEquals(3, reopened.getCommittedHead());
        assertEquals(1, reopened.getCurrentGenerationId());
        assertEquals(Collections.singletonList(0L), reopened.getFrozenGenerationIds());
        assertEquals(1, reopened.loadBlock(1).getMeta().getBlockNumber());
        assertArrayEquals(hash(61), reopened.loadBlock(1).getMutationViewDigest());
        assertLookup(reopened.findOldValueAfter("account", new byte[]{3}, 2), 3,
            OldValue.present(new byte[]{33}));
        assertFalse(reopened.findOldValueAfter("missing", new byte[]{1}, 0).isPresent());
      }

      assertTrue(Files.isRegularFile(root.resolve(StateArchiveHotStore.CURRENT)));
      assertTrue(Files.isDirectory(root.resolve(StateArchiveHotStore.GENERATIONS)
          .resolve("00000000000000000000").resolve(StateArchiveHotStore.DATABASE)));
    }
  }

  @Test
  public void recoversEveryRotationPublicationBoundary() throws Exception {
    for (StateArchiveHotStore.Stage failedStage : Arrays.asList(
        StateArchiveHotStore.Stage.AFTER_SEAL,
        StateArchiveHotStore.Stage.AFTER_NEW_GENERATION,
        StateArchiveHotStore.Stage.AFTER_CURRENT)) {
      Path root = temporaryFolder.newFolder("fault-" + failedStage).toPath();
      byte[] format = hash(91);
      StateArchiveHotStore failed = StateArchiveHotStore.openOrCreate(root, format,
          Engine.LEVELDB, 0, hash(0), 2, 1, 1024 * 1024,
          stage -> {
            if (stage == failedStage) {
              throw new IOException("injected " + stage);
            }
          });
      failed.appendSolidified(Collections.singletonList(
          diff(1, 0, "code", new byte[]{1}, OldValue.present(new byte[]{9}))));
      assertThrows(IOException.class, failed::sealCurrent);
      failed.close();

      try (StateArchiveHotStore recovered = open(root, format, Engine.LEVELDB, 0, hash(0),
          2, 1)) {
        assertEquals(1, recovered.getCurrentGenerationId());
        assertEquals(Collections.singletonList(0L), recovered.getFrozenGenerationIds());
        assertLookup(recovered.findOldValueAfter("code", new byte[]{1}, 0), 1,
            OldValue.present(new byte[]{9}));
      }
    }
  }

  @Test
  public void reconcilesOnlyCurrentPreparedTailAcrossBothEngines() throws Exception {
    for (Engine engine : Engine.values()) {
      Path root = temporaryFolder.newFolder("reconcile-" + engine.name()).toPath();
      byte[] format = hash(94);
      try (StateArchiveHotStore store = open(root, format, engine, 0, hash(0), 3, 10)) {
        store.appendSolidified(Arrays.asList(
            diff(1, 0, "code", new byte[]{1}, OldValue.absent()),
            diff(2, 1, "code", new byte[]{2}, OldValue.absent())));
        store.prepareCheckpoint(hash(110), Collections.singletonList(
            diff(3, 2, "code", new byte[]{3}, OldValue.absent())));
        assertEquals(2, store.getCommittedHead());
        assertEquals(3, store.getMaterializedHead());
        assertThrows(ArchivePersistenceException.class,
            () -> store.reconcilePreparedTail(meta(2, 1, 99)));
        assertEquals(1, store.reconcilePreparedTail(meta(2, 1, 2)));
        assertEquals(2, store.getCommittedHead());
        assertThrows(ArchivePersistenceException.class, () -> store.loadBlock(3));
        assertFalse(store.findOldValueAfter("code", new byte[]{3}, 0).isPresent());
      }
      try (StateArchiveHotStore reopened = open(root, format, engine, 0, hash(0), 3, 10)) {
        assertEquals(2, reopened.getCommittedHead());
        assertEquals(2, reopened.loadBlock(2).getMeta().getBlockNumber());
        assertThrows(ArchivePersistenceException.class, () -> reopened.loadBlock(3));
      }
    }
  }

  @Test
  public void resumesEveryPreparedTailTruncateBoundary() throws Exception {
    for (StateArchiveHotStore.Stage failedStage : Arrays.asList(
        StateArchiveHotStore.Stage.AFTER_TRUNCATE_INTENT,
        StateArchiveHotStore.Stage.AFTER_TRUNCATE_DELETE_BATCH,
        StateArchiveHotStore.Stage.AFTER_TRUNCATE_METADATA)) {
      Path root = temporaryFolder.newFolder("truncate-" + failedStage).toPath();
      byte[] format = hash(95);
      StateArchiveHotStore failed = StateArchiveHotStore.openOrCreate(root, format,
          Engine.LEVELDB, 0, hash(0), 3, 10, 1024 * 1024,
          stage -> {
            if (stage == failedStage) {
              throw new IOException("injected " + stage);
            }
          });
      failed.appendSolidified(Arrays.asList(
          diff(1, 0, "code", new byte[]{1}, OldValue.absent())));
      failed.prepareCheckpoint(hash(111), Collections.singletonList(
          diff(2, 1, "code", new byte[]{2}, OldValue.absent())));
      assertThrows(IOException.class, () -> failed.reconcilePreparedTail(meta(1, 0, 1)));
      failed.close();

      try (StateArchiveHotStore recovered = open(root, format, Engine.LEVELDB, 0, hash(0),
          3, 10)) {
        assertEquals(1, recovered.getCommittedHead());
        assertEquals(1, recovered.loadBlock(1).getMeta().getBlockNumber());
        assertThrows(ArchivePersistenceException.class, () -> recovered.loadBlock(2));
      }
    }
  }

  @Test
  public void preparesPublishesAndReopensAcrossBothEngines() throws Exception {
    for (Engine engine : Engine.values()) {
      Path root = temporaryFolder.newFolder("publication-" + engine.name()).toPath();
      byte[] format = hash(97);
      byte[] target = hash(112);
      BlockReverseDiff block = diff(1, 0, "code", new byte[]{1}, OldValue.absent());
      StateArchiveHotBatchDescriptor descriptor;
      try (StateArchiveHotStore store = open(root, format, engine, 0, hash(0), 3, 10)) {
        descriptor = store.planCheckpoint(Collections.singletonList(block));
        assertEquals(StateArchiveHotStore.HotCheckpointStatus.NEEDS_MATERIALIZATION,
            store.inspectCheckpoint(target));
        store.prepareCheckpoint(target, Collections.singletonList(block));
        assertEquals(StateArchiveHotStore.HotCheckpointStatus.MATERIALIZED,
            store.inspectCheckpoint(target, descriptor));
        assertEquals(0, store.getCommittedHead());
        assertEquals(1, store.getMaterializedHead());
        assertEquals(0, store.getStatistics().getPublishedBlock());
        assertTrue(store.getStatistics().hasPreparedCheckpoint());
        assertThrows(ArchivePersistenceException.class, () -> store.loadBlock(1));
        assertFalse(store.findOldValueAfter("code", new byte[]{1}, 0).isPresent());
        assertThrows(ArchivePersistenceException.class, store::sealCurrent);
      }

      try (StateArchiveHotStore store = open(root, format, engine, 0, hash(0), 3, 10)) {
        assertEquals(StateArchiveHotStore.HotCheckpointStatus.MATERIALIZED,
            store.inspectCheckpoint(target, descriptor));
        assertEquals(0, store.getCommittedHead());
        assertThrows(ArchivePersistenceException.class, () -> store.loadBlock(1));
        store.prepareCheckpoint(target, descriptor, Collections.singletonList(block));
        store.publishCheckpoint(target, descriptor);
        store.publishCheckpoint(target, descriptor);
        assertEquals(StateArchiveHotStore.HotCheckpointStatus.PUBLISHED,
            store.inspectCheckpoint(target, descriptor));
        assertEquals(1, store.getCommittedHead());
        assertEquals(1, store.loadBlock(1).getMeta().getBlockNumber());
        assertFalse(store.getStatistics().hasPreparedCheckpoint());
        store.sealCurrent();
      }

      try (StateArchiveHotStore store = open(root, format, engine, 0, hash(0), 3, 10)) {
        assertEquals(StateArchiveHotStore.HotCheckpointStatus.PUBLISHED,
            store.inspectCheckpoint(target, descriptor));
        assertEquals(1, store.getCommittedHead());
      }
    }
  }

  @Test
  public void recoversPrepareAndPublishNativeBoundaries() throws Exception {
    Path preparedRoot = temporaryFolder.newFolder("fault-prepare").toPath();
    byte[] format = hash(98);
    byte[] preparedTarget = hash(113);
    StateArchiveHotStore failedPrepare = StateArchiveHotStore.openOrCreate(preparedRoot, format,
        Engine.LEVELDB, 0, hash(0), 3, 10, 1024 * 1024,
        stage -> {
          if (stage == StateArchiveHotStore.Stage.AFTER_PREPARE) {
            throw new IOException("injected prepare failure");
          }
        });
    assertThrows(IOException.class, () -> failedPrepare.prepareCheckpoint(preparedTarget,
        Collections.singletonList(diff(1, 0, "code", new byte[]{1}, OldValue.absent()))));
    failedPrepare.close();
    try (StateArchiveHotStore recovered = open(preparedRoot, format, Engine.LEVELDB,
        0, hash(0), 3, 10)) {
      assertEquals(StateArchiveHotStore.HotCheckpointStatus.MATERIALIZED,
          recovered.inspectCheckpoint(preparedTarget));
      assertEquals(0, recovered.getCommittedHead());
    }

    Path publishedRoot = temporaryFolder.newFolder("fault-publish").toPath();
    byte[] publishedTarget = hash(114);
    StateArchiveHotStore failedPublish = StateArchiveHotStore.openOrCreate(publishedRoot, format,
        Engine.LEVELDB, 0, hash(0), 3, 1, 1024 * 1024,
        stage -> {
          if (stage == StateArchiveHotStore.Stage.AFTER_PUBLISH) {
            throw new IOException("injected publish failure");
          }
        });
    failedPublish.prepareCheckpoint(publishedTarget, Collections.singletonList(
        diff(1, 0, "code", new byte[]{1}, OldValue.absent())));
    assertThrows(IOException.class, () -> failedPublish.publishCheckpoint(publishedTarget));
    failedPublish.close();
    try (StateArchiveHotStore recovered = open(publishedRoot, format, Engine.LEVELDB,
        0, hash(0), 3, 1)) {
      assertEquals(StateArchiveHotStore.HotCheckpointStatus.PUBLISHED,
          recovered.inspectCheckpoint(publishedTarget));
      assertEquals(1, recovered.getCommittedHead());
      assertEquals(1, recovered.loadBlock(1).getMeta().getBlockNumber());
      recovered.publishCheckpoint(publishedTarget);
      assertEquals(1, recovered.getCurrentGenerationId());
      assertEquals(Collections.singletonList(0L), recovered.getFrozenGenerationIds());
    }
  }

  @Test
  public void rejectsPreparedRotationBeforeWritingWhenFrozenBacklogIsFull() throws Exception {
    Path root = temporaryFolder.newFolder("prepare-full-backlog").toPath();
    try (StateArchiveHotStore store = open(root, hash(99), Engine.LEVELDB,
        0, hash(0), 1, 1)) {
      store.appendSolidified(Collections.singletonList(
          diff(1, 0, "code", new byte[]{1}, OldValue.absent())));
      store.sealCurrent();
      assertThrows(ArchivePersistenceException.class, () -> store.prepareCheckpoint(hash(115),
          Collections.singletonList(
              diff(2, 1, "code", new byte[]{2}, OldValue.absent()))));
      assertEquals(1, store.getMaterializedHead());
      assertEquals(StateArchiveHotStore.HotCheckpointStatus.NEEDS_MATERIALIZATION,
          store.inspectCheckpoint(hash(115)));
    }
  }

  @Test
  public void persistsAndRevalidatesExactPreparedDescriptorAcrossBothEngines()
      throws Exception {
    for (Engine engine : Engine.values()) {
      Path root = temporaryFolder.newFolder("descriptor-" + engine.name()).toPath();
      byte[] format = hash(100);
      BlockReverseDiff original = diff(1, 0, "code", new byte[]{1}, OldValue.absent());
      StateArchiveHotBatchDescriptor descriptor;
      try (StateArchiveHotStore store = open(root, format, engine, 0, hash(0), 3, 10)) {
        descriptor = store.planCheckpoint(Collections.singletonList(original));
        store.prepareCheckpoint(hash(116), descriptor, Collections.singletonList(original));
      }
      try (StateArchiveHotStore reopened = open(root, format, engine, 0, hash(0), 3, 10)) {
        assertEquals(StateArchiveHotStore.HotCheckpointStatus.MATERIALIZED,
            reopened.inspectCheckpoint(hash(116), descriptor));
      }

      Path scratch = temporaryFolder.newFolder("descriptor-other-" + engine.name()).toPath();
      StateArchiveHotBatchDescriptor different;
      try (StateArchiveHotStore store = open(scratch, format, engine, 0, hash(0), 3, 10)) {
        BlockReverseDiff changed = diff(1, 0, "code", new byte[]{1},
            OldValue.present(new byte[]{9}));
        different = store.planCheckpoint(Collections.singletonList(changed));
      }
      Path database = root.resolve(StateArchiveHotStore.GENERATIONS)
          .resolve("00000000000000000000").resolve(StateArchiveHotStore.DATABASE);
      try (StateArchiveIndexDatabase.Writer writer = StateArchiveIndexDatabase.openWriter(
          database, engine, NativeDbConfig.large())) {
        writer.write(Collections.singletonList(StateArchiveIndexDatabase.put(
            "meta/prepared-descriptor".getBytes(StandardCharsets.US_ASCII),
            new StateArchiveHotBatchDescriptorCodec().encode(different))), true);
      }
      assertThrows(ArchivePersistenceException.class,
          () -> open(root, format, engine, 0, hash(0), 3, 10));
    }
  }

  @Test
  public void recoveryCeilingNeverTruncatesFrozenHistory() throws Exception {
    Path root = temporaryFolder.newFolder("truncate-frozen").toPath();
    try (StateArchiveHotStore store = open(root, hash(96), Engine.LEVELDB,
        0, hash(0), 3, 1)) {
      store.appendSolidified(Collections.singletonList(
          diff(1, 0, "code", new byte[]{1}, OldValue.absent())));
      store.sealCurrent();
      store.appendSolidified(Collections.singletonList(
          diff(2, 1, "code", new byte[]{2}, OldValue.absent())));
      assertThrows(ArchivePersistenceException.class,
          () -> store.reconcilePreparedTail(meta(0, 0, 0)));
      assertEquals(2, store.getCommittedHead());
      assertEquals(Collections.singletonList(0L), store.getFrozenGenerationIds());
    }
  }

  @Test
  public void rejectsGapsParentDriftIdentityDriftAndFrozenOverflow() throws Exception {
    Path root = temporaryFolder.newFolder("reject").toPath();
    byte[] format = hash(92);
    try (StateArchiveHotStore store = open(root, format, Engine.LEVELDB, 0, hash(0), 1, 1)) {
      assertThrows(IllegalArgumentException.class, () -> store.appendSolidified(
          Collections.singletonList(diff(2, 0, "code", new byte[]{1}, OldValue.absent()))));
      store.appendSolidified(Collections.singletonList(
          diff(1, 0, "code", new byte[]{1}, OldValue.absent())));
      store.sealCurrent();
      store.appendSolidified(Collections.singletonList(
          diff(2, 1, "code", new byte[]{2}, OldValue.present(new byte[]{1}))));
      assertThrows(ArchivePersistenceException.class, store::sealCurrent);
    }

    assertThrows(ArchivePersistenceException.class,
        () -> open(root, hash(99), Engine.LEVELDB, 0, hash(0), 1, 1));
    assertThrows(ArchivePersistenceException.class,
        () -> open(root, format, Engine.ROCKSDB, 0, hash(0), 1, 1));
  }

  @Test
  public void dedicatedConfigurationControlsRotationAndBacklogStatistics() throws Exception {
    Path disabledRoot = temporaryFolder.newFolder("hot-disabled").toPath();
    StateArchiveHotStoreConfig config = new StateArchiveHotStoreConfig();
    assertThrows(IllegalStateException.class, () -> StateArchiveHotStore.openOrCreate(
        disabledRoot, hash(93), Engine.LEVELDB, 0, hash(0), config));

    config.setEnabled(true);
    config.setMaxBlocks(1);
    config.setMaxEncodedBytes(1024 * 1024);
    config.setMaxFrozenGenerations(3);
    config.setYellowFrozenGenerations(1);
    config.setRedFrozenGenerations(2);
    Path root = temporaryFolder.newFolder("hot-configured").toPath();
    try (StateArchiveHotStore store = StateArchiveHotStore.openOrCreate(
        root, hash(93), Engine.LEVELDB, 0, hash(0), config)) {
      StateArchiveHotStore.Statistics empty = store.getStatistics();
      assertEquals(StateArchiveHotStore.BacklogLevel.GREEN, empty.getBacklogLevel());
      assertEquals(0, empty.getFrozenGenerations());
      assertEquals(10_000L, new StateArchiveHotStoreConfig().getMaxBlocks());

      store.appendSolidified(Collections.singletonList(
          diff(1, 0, "code", new byte[]{1}, OldValue.absent())));
      assertTrue(store.getStatistics().isRotationDue());
      store.sealCurrent();
      StateArchiveHotStore.Statistics yellow = store.getStatistics();
      assertEquals(StateArchiveHotStore.BacklogLevel.YELLOW, yellow.getBacklogLevel());
      assertEquals(1, yellow.getFrozenGenerations());
      assertEquals(1, yellow.getFrozenBlocks());
      assertTrue(yellow.getFrozenEncodedBytes() > 0);
      assertEquals(3, yellow.getMaxFrozenGenerations());
      assertEquals(1, yellow.getYellowFrozenGenerations());
      assertEquals(2, yellow.getRedFrozenGenerations());

      store.appendSolidified(Collections.singletonList(
          diff(2, 1, "code", new byte[]{2}, OldValue.absent())));
      store.sealCurrent();
      assertEquals(StateArchiveHotStore.BacklogLevel.RED,
          store.getStatistics().getBacklogLevel());
    }
  }

  private StateArchiveHotStore open(Path root, byte[] format, Engine engine, long baseBlock,
      byte[] baseHash, int maxFrozen, long maxBlocks) throws IOException {
    return StateArchiveHotStore.openOrCreate(root, format, engine, baseBlock, baseHash,
        maxFrozen, maxBlocks, 1024 * 1024);
  }

  private static BlockReverseDiff diff(long block, int parent, String dbName, byte[] key,
      OldValue oldValue) {
    return new BlockReverseDiff(BlockSnapshotMeta.forBlock(block, hash((int) block), hash(parent),
        block * 3_000), Collections.singletonList(new DbGroup(dbName,
        Collections.singletonList(new Entry(key, oldValue)))), hash(60 + (int) block));
  }

  private static BlockSnapshotMeta meta(long block, int parent, int hashMarker) {
    return BlockSnapshotMeta.forBlock(block, hash(hashMarker), hash(parent), block * 3_000);
  }

  private static void assertLookup(Optional<StateArchiveHotStore.HotLookup> found,
      long block, OldValue oldValue) {
    assertTrue(found.isPresent());
    assertEquals(block, found.get().getBlockNumber());
    assertEquals(oldValue, found.get().getOldValue());
  }

  private static byte[] hash(int marker) {
    byte[] hash = new byte[32];
    hash[31] = (byte) marker;
    return hash;
  }
}
