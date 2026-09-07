package org.tron.core.db2.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tron.core.db2.archive.BlockReverseDiff;
import org.tron.core.db2.archive.BlockSnapshotMeta;
import org.tron.core.db2.archive.StateArchiveHotBatchDescriptor;
import org.tron.core.db2.archive.StateArchiveHotCheckpointMaterializer;
import org.tron.core.db2.archive.StateArchiveHotStore;
import org.tron.core.db2.core.CommonCheckpointHotRecovery.PersistentDynamicHead;
import org.tron.core.db2.core.CommonCheckpointMaterializer.Authority;
import org.tron.core.db2.stateroot.PathStateFlushTarget;
import org.tron.core.db2.stateroot.PathStateStoreManifest.Engine;

public class CommonCheckpointHotRecoveryTest {

  private static final byte[] NUMBER_KEY = bytes("latest_block_header_number");
  private static final byte[] HASH_KEY = bytes("latest_block_header_hash");

  @Rule
  public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void validWalSelectsTargetBeforeChainbaseBlockExists() throws Exception {
    Path root = temporaryFolder.newFolder("wal-authority").toPath();
    BlockSnapshotMeta target = meta(1);
    CommonCheckpointFile file = new CommonCheckpointFile(root.resolve("wal"));
    file.publish(payload(root.resolve("plan"), target, dynamic(target), Engine.LEVELDB));
    AtomicBoolean dynamicRead = new AtomicBoolean();
    AtomicBoolean reconciled = new AtomicBoolean();
    CommonCheckpointHotRecovery recovery = new CommonCheckpointHotRecovery(file,
        () -> {
          dynamicRead.set(true);
          return new PersistentDynamicHead(0, hash(0));
        }, block -> null, authority -> {
      assertEquals(target, authority);
      reconciled.set(true);
      return 2;
    });

    CommonCheckpointHotRecovery.Result result = recovery.reconcileBeforeCommonRedo();

    assertEquals(CommonCheckpointHotRecovery.Source.COMMON_WAL, result.getSource());
    assertEquals(target, result.getAuthority());
    assertEquals(2, result.getRemovedBlocks());
    assertFalse(dynamicRead.get());
    assertTrue(reconciled.get());
  }

  @Test
  public void walRejectsDynamicAndExistingBlockIdentityDrift() throws Exception {
    Path root = temporaryFolder.newFolder("wal-drift").toPath();
    BlockSnapshotMeta target = meta(1);
    CommonCheckpointFile wrongDynamicFile = new CommonCheckpointFile(
        root.resolve("wrong-dynamic"));
    wrongDynamicFile.publish(payload(root.resolve("plan-dynamic"), target,
        dynamic(target.getBlockNumber(), hash(9)), Engine.LEVELDB));
    AtomicBoolean reconciled = new AtomicBoolean();
    CommonCheckpointHotRecovery wrongDynamic = new CommonCheckpointHotRecovery(
        wrongDynamicFile, () -> new PersistentDynamicHead(0, hash(0)), block -> null,
        authority -> {
          reconciled.set(true);
          return 0;
        });
    assertThrows(IOException.class, wrongDynamic::reconcileBeforeCommonRedo);
    assertFalse(reconciled.get());

    CommonCheckpointFile wrongBlockFile = new CommonCheckpointFile(root.resolve("wrong-block"));
    wrongBlockFile.publish(payload(root.resolve("plan-block"), target, dynamic(target),
        Engine.LEVELDB));
    BlockSnapshotMeta driftedMeta = BlockSnapshotMeta.forBlock(1, hash(1), hash(8), 3_000L);
    CommonCheckpointHotRecovery wrongBlock = new CommonCheckpointHotRecovery(wrongBlockFile,
        () -> new PersistentDynamicHead(0, hash(0)), block -> driftedMeta, authority -> 0);
    assertThrows(IOException.class, wrongBlock::reconcileBeforeCommonRedo);
  }

  @Test
  public void absentWalUsesPersistedDynamicAndTruncatesOrphanIdempotently() throws Exception {
    Path root = temporaryFolder.newFolder("dynamic-authority").toPath();
    byte[] format = hash(70);
    BlockSnapshotMeta block = meta(1);
    BlockReverseDiff diff = new BlockReverseDiff(block, Collections.emptyList(), hash(40));
    try (StateArchiveHotStore hot = StateArchiveHotStore.openOrCreate(root.resolve("hot"),
        format, Engine.LEVELDB, 0, hash(0), 3, 10, 1024 * 1024)) {
      StateArchiveHotBatchDescriptor descriptor = hot.planCheckpoint(
          Collections.singletonList(diff));
      hot.prepareCheckpoint(hash(90), descriptor, Collections.singletonList(diff));
      BlockSnapshotMeta persisted = BlockSnapshotMeta.forBlock(0, hash(0), hash(-1), 0);
      CommonCheckpointHotRecovery recovery = new CommonCheckpointHotRecovery(
          new CommonCheckpointFile(root.resolve("wal")),
          () -> new PersistentDynamicHead(0, hash(0)), ignored -> persisted,
          hot::reconcilePreparedTail);

      CommonCheckpointHotRecovery.Result first = recovery.reconcileBeforeCommonRedo();
      CommonCheckpointHotRecovery.Result second = recovery.reconcileBeforeCommonRedo();

      assertEquals(CommonCheckpointHotRecovery.Source.PERSISTED_DYNAMIC, first.getSource());
      assertEquals(1, first.getRemovedBlocks());
      assertEquals(0, second.getRemovedBlocks());
      assertEquals(0, hot.getCommittedHead());
      assertEquals(0, hot.getMaterializedHead());
    }
  }

  @Test
  public void runtimeInvokesOptionalHotRecoveryBeforeCommonRedo() throws Exception {
    Path root = temporaryFolder.newFolder("runtime").toPath();
    CommonCheckpointFile file = new CommonCheckpointFile(root.resolve("wal"));
    AtomicBoolean reconciled = new AtomicBoolean();
    BlockSnapshotMeta persisted = BlockSnapshotMeta.forBlock(0, hash(0), hash(-1), 0);
    StateArchiveHotStore hotStore = StateArchiveHotStore.openOrCreate(root.resolve("hot"),
        hash(70), Engine.LEVELDB, 0, hash(0), 3, 10, 1024 * 1024);
    StateArchiveHotCheckpointMaterializer hotMaterializer =
        new StateArchiveHotCheckpointMaterializer(hotStore);
    CommonCheckpointHotRecovery hotRecovery = new CommonCheckpointHotRecovery(file,
        () -> new PersistentDynamicHead(0, hash(0)), ignored -> persisted, authority -> {
      reconciled.set(true);
      return hotMaterializer.reconcilePreparedTail(authority);
    });
    CommonCheckpointRedoCoordinator coordinator = new CommonCheckpointRedoCoordinator(file,
        materializer(Authority.CHAINBASE), materializer(Authority.PATH_STATE),
        hotMaterializer);
    CommonCheckpointRuntime runtime = new CommonCheckpointRuntime(
        new CommonCheckpointRuntimeOwner(coordinator),
        Collections.singletonList(mock(Chainbase.class)), root.resolve("archive"), hash(70),
        Engine.LEVELDB, (blockNumber, blockHash) -> {
      throw new IOException("latest state is intentionally unavailable");
    }, target -> () -> { }, hotMaterializer, hotRecovery);

    assertEquals(CommonCheckpointRedoCoordinator.RecoveryAction.NO_CHECKPOINT,
        runtime.recoverBeforeServing());
    assertTrue(reconciled.get());
    assertEquals(CommonCheckpointRuntimeOwner.State.READY, runtime.getState());
    runtime.close();
  }

  @Test
  public void absentWalRejectsMissingOrDriftedBlockStoreIdentity() throws Exception {
    Path root = temporaryFolder.newFolder("dynamic-drift").toPath();
    CommonCheckpointFile file = new CommonCheckpointFile(root.resolve("wal"));
    CommonCheckpointHotRecovery missing = new CommonCheckpointHotRecovery(file,
        () -> new PersistentDynamicHead(1, hash(1)), ignored -> null, authority -> 0);
    assertThrows(IOException.class, missing::reconcileBeforeCommonRedo);

    CommonCheckpointHotRecovery drifted = new CommonCheckpointHotRecovery(file,
        () -> new PersistentDynamicHead(1, hash(9)), ignored -> meta(1), authority -> 0);
    assertThrows(IOException.class, drifted::reconcileBeforeCommonRedo);
  }

  private CommonCheckpointPayload payload(Path hotPath, BlockSnapshotMeta meta,
      List<CommonCheckpointPayload.StoreMutations> stores, Engine engine) throws Exception {
    byte[] viewDigest = hash(40);
    BlockReverseDiff diff = new BlockReverseDiff(meta, Collections.emptyList(), viewDigest);
    try (StateArchiveHotStore hot = StateArchiveHotStore.openOrCreate(hotPath, hash(70), engine,
        0, hash(0), 3, 10, 1024 * 1024)) {
      return CommonCheckpointPayload.createV2(hash(70), pathState(meta, viewDigest),
          hot.planCheckpoint(Collections.singletonList(diff)), stores);
    }
  }

  private static List<CommonCheckpointPayload.StoreMutations> dynamic(BlockSnapshotMeta meta) {
    return dynamic(meta.getBlockNumber(), meta.getBlockHash());
  }

  private static List<CommonCheckpointPayload.StoreMutations> dynamic(long number, byte[] hash) {
    return Collections.singletonList(new CommonCheckpointPayload.StoreMutations("properties",
        Arrays.asList(new CommonCheckpointPayload.Mutation(NUMBER_KEY,
                ByteBuffer.allocate(Long.BYTES).putLong(number).array()),
            new CommonCheckpointPayload.Mutation(HASH_KEY, hash))));
  }

  private static PathStateFlushTarget pathState(BlockSnapshotMeta meta, byte[] viewDigest) {
    PathStateFlushTarget.BlockBinding binding = mock(PathStateFlushTarget.BlockBinding.class);
    when(binding.getMeta()).thenReturn(meta);
    when(binding.getParentStateRoot()).thenReturn(hash(50));
    when(binding.getStateRoot()).thenReturn(hash(51));
    when(binding.getTransitionPayloadDigest()).thenReturn(hash(52));
    when(binding.getMutationViewDigest()).thenReturn(viewDigest);
    PathStateFlushTarget pathState = mock(PathStateFlushTarget.class);
    when(pathState.getBlocks()).thenReturn(Collections.singletonList(binding));
    when(pathState.getParentStateRoot()).thenReturn(hash(50));
    when(pathState.getStateRoot()).thenReturn(hash(51));
    when(pathState.getStores()).thenReturn(Collections.emptyList());
    when(pathState.getSuperNodeMutations()).thenReturn(Collections.emptyList());
    return pathState;
  }

  private static CommonCheckpointMaterializer materializer(Authority authority) {
    CommonCheckpointMaterializer materializer = mock(CommonCheckpointMaterializer.class);
    when(materializer.authority()).thenReturn(authority);
    return materializer;
  }

  private static BlockSnapshotMeta meta(long block) {
    return BlockSnapshotMeta.forBlock(block, hash((int) block), hash((int) block - 1),
        block * 3_000L);
  }

  private static byte[] bytes(String value) {
    return value.getBytes(StandardCharsets.UTF_8);
  }

  private static byte[] hash(int marker) {
    byte[] value = new byte[32];
    value[31] = (byte) marker;
    return value;
  }
}
