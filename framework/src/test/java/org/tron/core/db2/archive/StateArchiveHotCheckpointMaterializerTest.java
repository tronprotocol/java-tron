package org.tron.core.db2.archive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.Collections;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tron.core.db2.core.CommonCheckpointCapture;
import org.tron.core.db2.core.CommonCheckpointFile;
import org.tron.core.db2.core.CommonCheckpointMaterializer;
import org.tron.core.db2.core.CommonCheckpointMaterializer.Authority;
import org.tron.core.db2.core.CommonCheckpointMaterializer.Status;
import org.tron.core.db2.core.CommonCheckpointPayload;
import org.tron.core.db2.core.CommonCheckpointRedoCoordinator;
import org.tron.core.db2.core.CommonCheckpointRedoCoordinator.RecoveryAction;
import org.tron.core.db2.core.CommonCheckpointTarget;
import org.tron.core.db2.stateroot.PathStateFlushTarget;
import org.tron.core.db2.stateroot.PathStateStoreManifest.Engine;

public class StateArchiveHotCheckpointMaterializerTest {

  @Rule
  public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void commonParticipantOnlyVerifiesPreparedHotBodiesThenPublishes() throws Exception {
    Path root = temporaryFolder.newFolder("hot-materializer").toPath();
    byte[] format = hash(7);
    StateArchiveHotStore store = open(root, format);
    BlockReverseDiff diff = diff();
    StateArchiveHotBatchDescriptor descriptor = store.planCheckpoint(
        Collections.singletonList(diff));
    CommonCheckpointPayload payload = payload(format, descriptor);
    CommonCheckpointTarget target = CommonCheckpointTarget.from(payload);
    CommonCheckpointCapture capture = CommonCheckpointCapture.create(payload,
        Collections.singletonList(diff), descriptor);
    StateArchiveHotCheckpointMaterializer materializer =
        new StateArchiveHotCheckpointMaterializer(store);
    try {
      assertEquals(Status.NEEDS_MATERIALIZATION, materializer.inspect(target));
      CommonCheckpointTarget foreignFormat = CommonCheckpointTarget.from(
          payload(hash(8), descriptor));
      assertThrows(ArchivePersistenceException.class,
          () -> materializer.inspect(foreignFormat));
      assertThrows(IllegalArgumentException.class,
          () -> materializer.prepare(target, Collections.singletonList(
              new BlockReverseDiff(BlockSnapshotMeta.forBlock(2, hash(2), hash(1), 6_000L),
                  Collections.emptyList()))));
      assertThrows(ArchivePersistenceException.class, () -> store.loadBlock(1));
      assertThrows(java.io.IOException.class,
          () -> materializer.materialize(payload, target));
      assertEquals(0, store.getMaterializedHead());

      assertEquals(target, materializer.prepare(capture));
      assertEquals(target, materializer.prepare(capture));
      assertEquals(Status.MATERIALIZED, materializer.inspect(target));
      assertEquals(1, store.getMaterializedHead());
      assertEquals(0, store.getCommittedHead());
      assertFalse(store.findOldValueAfter("code", new byte[]{1}, 0).isPresent());
      materializer.materialize(payload, target);
      materializer.publish(target);
      assertEquals(Status.PUBLISHED, materializer.inspect(target));
      assertEquals(1, store.loadBlock(1).getMeta().getBlockNumber());
    } finally {
      materializer.close();
    }

    StateArchiveHotStore reopenedStore = open(root, format);
    StateArchiveHotCheckpointMaterializer reopened =
        new StateArchiveHotCheckpointMaterializer(reopenedStore);
    try {
      assertEquals(Status.PUBLISHED, reopened.inspect(target));
      reopened.materialize(payload, target);
      reopened.publish(target);
      assertEquals(1, reopenedStore.getCommittedHead());
    } finally {
      reopened.close();
    }
  }

  @Test
  public void prepreparedHotCaptureCrossesExistingCoordinatorWithoutWalBody() throws Exception {
    Path root = temporaryFolder.newFolder("hot-v2-coordinator").toPath();
    byte[] format = hash(9);
    StateArchiveHotStore store = open(root.resolve("hot"), format);
    BlockReverseDiff diff = diff();
    StateArchiveHotBatchDescriptor descriptor = store.planCheckpoint(
        Collections.singletonList(diff));
    CommonCheckpointPayload payload = payload(format, descriptor);
    CommonCheckpointCapture capture = CommonCheckpointCapture.create(payload,
        Collections.singletonList(diff), descriptor);
    StateArchiveHotCheckpointMaterializer archive =
        new StateArchiveHotCheckpointMaterializer(store);
    CommonCheckpointTarget target = archive.prepare(capture);
    try (CommonCheckpointRedoCoordinator coordinator = new CommonCheckpointRedoCoordinator(
        new CommonCheckpointFile(root.resolve("wal")), new FakeMaterializer(Authority.CHAINBASE),
        new FakeMaterializer(Authority.PATH_STATE), archive)) {
      assertEquals(RecoveryAction.COMPLETED_REDO, coordinator.apply(payload));
      assertEquals(Status.PUBLISHED, archive.inspect(target));
      assertEquals(1, store.getCommittedHead());
      assertEquals(RecoveryAction.NO_CHECKPOINT, coordinator.recover());
    }
  }

  private StateArchiveHotStore open(Path root, byte[] format) throws java.io.IOException {
    return StateArchiveHotStore.openOrCreate(root, format, Engine.LEVELDB,
        0, hash(0), 3, 10, 1024 * 1024);
  }

  private static CommonCheckpointPayload payload(byte[] format,
      StateArchiveHotBatchDescriptor descriptor) {
    BlockSnapshotMeta meta = BlockSnapshotMeta.forBlock(1, hash(1), hash(0), 3_000L);
    PathStateFlushTarget.BlockBinding binding = mock(PathStateFlushTarget.BlockBinding.class);
    when(binding.getMeta()).thenReturn(meta);
    when(binding.getParentStateRoot()).thenReturn(hash(5));
    when(binding.getStateRoot()).thenReturn(hash(6));
    when(binding.getTransitionPayloadDigest()).thenReturn(hash(71));
    PathStateFlushTarget pathState = mock(PathStateFlushTarget.class);
    when(pathState.getBlocks()).thenReturn(Collections.singletonList(binding));
    when(pathState.getParentStateRoot()).thenReturn(hash(5));
    when(pathState.getStateRoot()).thenReturn(hash(6));
    when(pathState.getStores()).thenReturn(Collections.emptyList());
    when(pathState.getSuperNodeMutations()).thenReturn(Collections.emptyList());
    return CommonCheckpointPayload.createV2(format, pathState, descriptor,
        Collections.emptyList());
  }

  private static BlockReverseDiff diff() {
    return new BlockReverseDiff(BlockSnapshotMeta.forBlock(1, hash(1), hash(0), 3_000L),
        Collections.singletonList(new BlockReverseDiff.DbGroup("code",
            Collections.singletonList(new BlockReverseDiff.Entry(new byte[]{1},
                OldValue.absent())))));
  }

  private static byte[] hash(int marker) {
    byte[] hash = new byte[32];
    hash[31] = (byte) marker;
    return hash;
  }

  private static final class FakeMaterializer implements CommonCheckpointMaterializer {
    private final Authority authority;
    private Status status = Status.NEEDS_MATERIALIZATION;

    private FakeMaterializer(Authority authority) {
      this.authority = authority;
    }

    @Override
    public Authority authority() {
      return authority;
    }

    @Override
    public Status inspect(CommonCheckpointTarget target) {
      return status;
    }

    @Override
    public void materialize(CommonCheckpointPayload payload, CommonCheckpointTarget target) {
      status = Status.MATERIALIZED;
    }

    @Override
    public void publish(CommonCheckpointTarget target) {
      status = Status.PUBLISHED;
    }
  }
}
