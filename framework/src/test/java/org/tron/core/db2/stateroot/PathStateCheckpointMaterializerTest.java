package org.tron.core.db2.stateroot;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tron.core.db2.archive.BlockReverseDiff;
import org.tron.core.db2.archive.BlockSnapshotMeta;
import org.tron.core.db2.core.CommonCheckpointMaterializer.Status;
import org.tron.core.db2.core.CommonCheckpointPayload;
import org.tron.core.db2.core.CommonCheckpointTarget;
import org.tron.core.db2.stateroot.PathStateStoreManifest.Engine;

public class PathStateCheckpointMaterializerTest {

  @Rule
  public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void separatesSyncedMaterializationFromCurrentPublicationAcrossReopen()
      throws Exception {
    Fixture fixture = fixture("normal", null);
    try {
      assertEquals(Status.NEEDS_MATERIALIZATION,
          fixture.materializer.inspect(fixture.target));
      fixture.materializer.materialize(fixture.payload, fixture.target);
      assertEquals(Status.MATERIALIZED, fixture.materializer.inspect(fixture.target));
      assertFalse(Files.exists(fixture.root.resolve(
          PathStateCheckpointMaterializer.CURRENT_FILE)));
      assertArrayEquals(new byte[]{2}, fixture.stores.participant("account")
          .getFlat(new byte[]{1}));
      assertArrayEquals(new byte[]{4}, fixture.stores.participant("account")
          .nodeStore().get(new byte[]{3}));
      assertArrayEquals(new byte[]{6}, fixture.stores.superStore().nodeStore()
          .get(new byte[]{5}));
      long accountBatches = fixture.stores.participant("account").getSyncedWriteBatchCalls();
      long superBatches = fixture.stores.superStore().getSyncedWriteBatchCalls();
      fixture.materializer.materialize(fixture.payload, fixture.target);
      assertEquals(accountBatches,
          fixture.stores.participant("account").getSyncedWriteBatchCalls());
      assertEquals(superBatches, fixture.stores.superStore().getSyncedWriteBatchCalls());
      fixture.materializer.publish(fixture.target);
      assertEquals(Status.PUBLISHED, fixture.materializer.inspect(fixture.target));
    } finally {
      fixture.stores.close();
    }

    try (PathStatePhysicalStoreSet reopened = PathStatePhysicalStoreSet.openExisting(fixture.root,
        fixture.scope, Engine.ROCKSDB)) {
      PathStateCheckpointMaterializer recovered = new PathStateCheckpointMaterializer(reopened,
          fixture.scope, fixture.formatIdentity);
      assertEquals(Status.PUBLISHED, recovered.inspect(fixture.target));
      recovered.publish(fixture.target);

      CommonCheckpointPayload child = payload(fixture.formatIdentity, 2, hash(1), hash(2),
          hash(11), hash(12));
      CommonCheckpointTarget childTarget = CommonCheckpointTarget.from(child);
      assertEquals(Status.NEEDS_MATERIALIZATION, recovered.inspect(childTarget));
      recovered.materialize(child, childTarget);
      recovered.publish(childTarget);
      assertEquals(Status.PUBLISHED, recovered.inspect(childTarget));
    }
  }

  @Test
  public void resumesEveryStoreAndMarkerBoundaryWithoutRepeatingExactBatches()
      throws Exception {
    for (PathStateCheckpointMaterializer.Stage stage
        : PathStateCheckpointMaterializer.Stage.values()) {
      Fixture fixture = fixture("fault-" + stage, stage);
      try {
        if (stage == PathStateCheckpointMaterializer.Stage.AFTER_CURRENT) {
          fixture.materializer.materialize(fixture.payload, fixture.target);
          assertThrows(IOException.class, () -> fixture.materializer.publish(fixture.target));
        } else {
          assertThrows(IOException.class,
              () -> fixture.materializer.materialize(fixture.payload, fixture.target));
        }
      } finally {
        fixture.stores.close();
      }

      try (PathStatePhysicalStoreSet reopened = PathStatePhysicalStoreSet.openExisting(fixture.root,
          fixture.scope, Engine.ROCKSDB)) {
        PathStateCheckpointMaterializer recovered = new PathStateCheckpointMaterializer(reopened,
            fixture.scope, fixture.formatIdentity);
        Status status = recovered.inspect(fixture.target);
        if (status == Status.NEEDS_MATERIALIZATION) {
          recovered.materialize(fixture.payload, fixture.target);
        }
        recovered.publish(fixture.target);
        assertEquals(Status.PUBLISHED, recovered.inspect(fixture.target));
      }
    }
  }

  @Test
  public void rejectsForeignFormatCorruptCurrentAndNonParentTarget() throws Exception {
    Fixture fixture = fixture("reject", null);
    try {
      CommonCheckpointPayload foreign = payload(hash(9), 1, hash(0), hash(1), hash(10), hash(11));
      assertThrows(IOException.class, () -> fixture.materializer.materialize(foreign,
          CommonCheckpointTarget.from(foreign)));

      fixture.materializer.materialize(fixture.payload, fixture.target);
      fixture.materializer.publish(fixture.target);
      CommonCheckpointPayload nonChild = payload(fixture.formatIdentity, 3, hash(8), hash(9),
          hash(12), hash(13));
      assertThrows(IOException.class,
          () -> fixture.materializer.inspect(CommonCheckpointTarget.from(nonChild)));

      byte[] corrupt = Files.readAllBytes(fixture.root.resolve(
          PathStateCheckpointMaterializer.CURRENT_FILE));
      corrupt[corrupt.length - 1] ^= 1;
      Files.write(fixture.root.resolve(PathStateCheckpointMaterializer.CURRENT_FILE), corrupt);
      assertThrows(IOException.class, () -> fixture.materializer.inspect(fixture.target));
      assertTrue(Files.isRegularFile(fixture.root.resolve(
          PathStateCheckpointMaterializer.CURRENT_FILE)));
    } finally {
      fixture.stores.close();
    }
  }

  private Fixture fixture(String name, PathStateCheckpointMaterializer.Stage failedStage)
      throws Exception {
    Path root = temporaryFolder.newFolder(name).toPath();
    PathStateParticipantScope scope = new PathStateCanonicalizer().participantScope();
    PathStatePhysicalStoreSet stores = PathStatePhysicalStoreSet.open(root, scope, Engine.ROCKSDB);
    byte[] formatIdentity = hash(7);
    CommonCheckpointPayload payload = payload(formatIdentity, 1, hash(0), hash(1),
        hash(10), hash(11));
    PathStateCheckpointMaterializer materializer = new PathStateCheckpointMaterializer(stores,
        scope, formatIdentity, failAt(failedStage));
    return new Fixture(root, scope, stores, formatIdentity, payload, materializer);
  }

  private static PathStateCheckpointMaterializer.FaultHook failAt(
      PathStateCheckpointMaterializer.Stage failedStage) {
    return (stage, storeId) -> {
      if (stage == failedStage) {
        throw new IOException("injected " + stage + " at " + storeId);
      }
    };
  }

  private static CommonCheckpointPayload payload(byte[] formatIdentity, long blockNumber,
      byte[] parentHash, byte[] blockHash, byte[] parentRoot, byte[] stateRoot) {
    BlockSnapshotMeta meta = BlockSnapshotMeta.forBlock(blockNumber, blockHash, parentHash,
        blockNumber * 3_000L);
    PathStateFlushTarget.BlockBinding binding = mock(PathStateFlushTarget.BlockBinding.class);
    when(binding.getMeta()).thenReturn(meta);
    when(binding.getParentStateRoot()).thenReturn(parentRoot);
    when(binding.getStateRoot()).thenReturn(stateRoot);
    when(binding.getTransitionPayloadDigest()).thenReturn(hash((int) blockNumber + 30));

    PathStateFlushTarget.StoreTarget store = mock(PathStateFlushTarget.StoreTarget.class);
    when(store.getStoreId()).thenReturn(4);
    when(store.getDbName()).thenReturn("account");
    when(store.getStoreRoot()).thenReturn(hash((int) blockNumber + 40));
    when(store.getFlatMutations()).thenReturn(Collections.singletonList(
        new PathStateSnapshotDelta.Mutation(new byte[]{1}, new byte[]{2})));
    when(store.getNodeMutations()).thenReturn(Collections.singletonList(
        new PathStateSnapshotDelta.Mutation(new byte[]{3}, new byte[]{4})));

    PathStateFlushTarget target = mock(PathStateFlushTarget.class);
    when(target.getBlocks()).thenReturn(Collections.singletonList(binding));
    when(target.getParentStateRoot()).thenReturn(parentRoot);
    when(target.getStateRoot()).thenReturn(stateRoot);
    when(target.getStores()).thenReturn(Collections.singletonList(store));
    when(target.getSuperNodeMutations()).thenReturn(Collections.singletonList(
        new PathStateSnapshotDelta.Mutation(new byte[]{5}, new byte[]{6})));
    return CommonCheckpointPayload.create(formatIdentity, target,
        Collections.singletonList(new BlockReverseDiff(meta, Collections.emptyList())),
        Collections.emptyList());
  }

  private static byte[] hash(int seed) {
    byte[] hash = new byte[32];
    for (int index = 0; index < hash.length; index++) {
      hash[index] = (byte) (seed + index);
    }
    return hash;
  }

  private static final class Fixture {

    private final Path root;
    private final PathStateParticipantScope scope;
    private final PathStatePhysicalStoreSet stores;
    private final byte[] formatIdentity;
    private final CommonCheckpointPayload payload;
    private final CommonCheckpointTarget target;
    private final PathStateCheckpointMaterializer materializer;

    private Fixture(Path root, PathStateParticipantScope scope,
        PathStatePhysicalStoreSet stores, byte[] formatIdentity,
        CommonCheckpointPayload payload, PathStateCheckpointMaterializer materializer) {
      this.root = root;
      this.scope = scope;
      this.stores = stores;
      this.formatIdentity = formatIdentity;
      this.payload = payload;
      this.target = CommonCheckpointTarget.from(payload);
      this.materializer = materializer;
    }
  }
}
