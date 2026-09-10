package org.tron.core.db2.archive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tron.core.db2.archive.BlockReverseDiff.DbGroup;
import org.tron.core.db2.archive.BlockReverseDiff.Entry;
import org.tron.core.db2.archive.StateArchiveFiveLaneSegmentWriterV3.SyncStage;
import org.tron.core.db2.core.CommonCheckpointCapture;
import org.tron.core.db2.core.CommonCheckpointFile;
import org.tron.core.db2.core.CommonCheckpointMaterializer;
import org.tron.core.db2.core.CommonCheckpointMaterializer.Authority;
import org.tron.core.db2.core.CommonCheckpointMaterializer.Status;
import org.tron.core.db2.core.CommonCheckpointPayload;
import org.tron.core.db2.core.CommonCheckpointRedoCoordinator;
import org.tron.core.db2.core.CommonCheckpointTarget;
import org.tron.core.db2.stateroot.PathStateFlushTarget;
import org.tron.core.db2.stateroot.PathStateStoreManifest.Engine;

public class StateArchiveAppendCheckpointMaterializerV3Test {

  @Rule
  public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void preparesBeforeWalPublishesAndReopensExactTarget() throws Exception {
    Path root = temporaryFolder.newFolder("append-materializer").toPath();
    byte[] format = hash(70);
    byte[] baseline = hash(80);
    BlockReverseDiff diff = diff(1, 64);
    CommonCheckpointPayload payload;
    CommonCheckpointTarget target;
    try (StateArchiveAppendCheckpointMaterializerV3 archive = materializer(
        root, format, baseline, 10_000)) {
      StateArchiveHotBatchDescriptor descriptor = archive.planCheckpoint(
          Collections.singletonList(diff));
      payload = payload(format, Collections.singletonList(diff), descriptor);
      target = CommonCheckpointTarget.from(payload);
      CommonCheckpointCapture capture = CommonCheckpointCapture.create(payload,
          Collections.singletonList(diff), descriptor);

      assertEquals(Status.NEEDS_MATERIALIZATION, archive.inspect(target));
      assertThrows(java.io.IOException.class, () -> archive.materialize(payload, target));
      assertEquals(target, archive.prepare(capture));
      assertEquals(target, archive.prepare(capture));
      assertEquals(Status.MATERIALIZED, archive.inspect(target));
      assertFalse(Files.exists(root.resolve(StateArchiveCheckpointMaterializer.READABLE_FILE)));
      archive.materialize(payload, target);
      archive.publish(target);
      assertEquals(Status.PUBLISHED, archive.inspect(target));
    }

    try (StateArchiveAppendCheckpointMaterializerV3 reopened = materializer(
        root, format, baseline, 10_000)) {
      assertEquals(Status.PUBLISHED, reopened.inspect(target));
      assertEquals(1, reopened.servingIndexStatus().getIndexedThrough());
      assertEquals(StateArchiveServingIndexBuildCoordinatorV3.Mode.BULK_CATCH_UP,
          reopened.servingIndexStatus().getMode());
      reopened.materialize(payload, target);
      reopened.publish(target);
    }
  }

  @Test
  public void crossesExistingCoordinatorWithoutArchiveBodiesInWal() throws Exception {
    Path root = temporaryFolder.newFolder("append-coordinator").toPath();
    byte[] format = hash(71);
    BlockReverseDiff diff = diff(1, 32);
    StateArchiveAppendCheckpointMaterializerV3 archive = materializer(
        root.resolve("history"), format, hash(81), 10_000);
    StateArchiveHotBatchDescriptor descriptor = archive.planCheckpoint(
        Collections.singletonList(diff));
    CommonCheckpointPayload payload = payload(format, Collections.singletonList(diff),
        descriptor);
    CommonCheckpointTarget target = archive.prepare(CommonCheckpointCapture.create(payload,
        Collections.singletonList(diff), descriptor));

    try (CommonCheckpointRedoCoordinator coordinator = new CommonCheckpointRedoCoordinator(
        new CommonCheckpointFile(root.resolve("wal")), new FakeMaterializer(Authority.CHAINBASE),
        new FakeMaterializer(Authority.PATH_STATE), archive)) {
      assertEquals(CommonCheckpointRedoCoordinator.RecoveryAction.COMPLETED_REDO,
          coordinator.apply(payload));
      assertEquals(Status.PUBLISHED, archive.inspect(target));
      assertEquals(CommonCheckpointRedoCoordinator.RecoveryAction.NO_CHECKPOINT,
          coordinator.recover());
    }
  }

  @Test
  public void persistsRotationSpanningSap3DuringPrepare() throws Exception {
    Path root = temporaryFolder.newFolder("append-materializer-rotation").toPath();
    byte[] format = hash(72);
    byte[] baseline = hash(82);
    List<BlockReverseDiff> diffs = Arrays.asList(diff(1, 1_400), diff(2, 0));
    try (StateArchiveAppendCheckpointMaterializerV3 archive = materializer(
        root, format, baseline, 1_500)) {
      StateArchiveHotBatchDescriptor descriptor = archive.planCheckpoint(diffs);
      CommonCheckpointPayload payload = payload(format, diffs, descriptor);
      CommonCheckpointTarget target = CommonCheckpointTarget.from(payload);
      archive.prepare(CommonCheckpointCapture.create(payload, diffs, descriptor));
      StateArchiveFiveLaneSegmentWriterV3.ArchiveDurabilityProof proof =
          StateArchiveFiveLaneDurabilityProofV3.decode(Files.readAllBytes(
              root.resolve(StateArchiveFiveLaneDurabilityProofV3.FILE_NAME)));
      assertEquals(6, proof.getFileTails().size());
      assertEquals(Status.MATERIALIZED, archive.inspect(target));
    }
  }

  @Test
  public void advancesTwoPublishedTargetsAndPreservesFreshHotBindingBytes() throws Exception {
    Path root = temporaryFolder.newFolder("append-materializer-sequential").toPath();
    byte[] format = hash(73);
    byte[] baseline = hash(83);
    BlockReverseDiff first = diff(1, 48);
    try (StateArchiveAppendCheckpointMaterializerV3 archive = materializer(
        root.resolve("history"), format, baseline, 10_000)) {
      StateArchiveHotBatchDescriptor firstDescriptor = archive.planCheckpoint(
          Collections.singletonList(first));
      assertEquals(StateArchiveHotStore.planCheckpointDescriptor(Engine.LEVELDB,
          0, hash(0), new byte[StateArchiveFileFormatV3.HASH_LENGTH],
          Collections.singletonList(first)), firstDescriptor);
      CommonCheckpointPayload firstPayload = payload(format,
          Collections.singletonList(first), firstDescriptor);
      CommonCheckpointTarget firstTarget = archive.prepare(CommonCheckpointCapture.create(
          firstPayload, Collections.singletonList(first), firstDescriptor));
      archive.publish(firstTarget);
      assertEquals(Status.PUBLISHED, archive.inspect(firstTarget));
      assertEquals(StateArchiveServingIndexBuildCoordinatorV3.Mode.BULK_CATCH_UP,
          archive.servingIndexStatus().getMode());
      assertEquals(1, archive.servingIndexStatus().getPendingBlocks());
      archive.completeServingInitialSync(firstTarget);
      assertEquals(StateArchiveServingIndexBuildCoordinatorV3.Mode.LIVE_IMMEDIATE,
          archive.servingIndexStatus().getMode());
      assertEquals(1, archive.servingIndexStatus().getIndexedThrough());

      BlockReverseDiff second = diff(2, 16);
      StateArchiveHotBatchDescriptor secondDescriptor = archive.planCheckpoint(
          Collections.singletonList(second));
      CommonCheckpointPayload secondPayload = payload(format,
          Collections.singletonList(second), secondDescriptor);
      CommonCheckpointTarget secondTarget = archive.prepare(CommonCheckpointCapture.create(
          secondPayload, Collections.singletonList(second), secondDescriptor));
      assertEquals(Status.MATERIALIZED, archive.inspect(secondTarget));
      archive.publish(secondTarget);
      assertEquals(Status.PUBLISHED, archive.inspect(secondTarget));
      assertEquals(2, archive.servingIndexStatus().getIndexedThrough());
      assertEquals(0, archive.servingIndexStatus().getPendingBlocks());
      assertThrows(java.io.IOException.class, () -> archive.inspect(firstTarget));
    }
  }

  @Test
  public void resumesEveryRotationMarkerPhaseBeforeSap3Publication() throws Exception {
    for (SyncStage stage : new SyncStage[]{SyncStage.MARKER_WRITTEN,
        SyncStage.DATA_FORCED, SyncStage.MARKER_VERIFIED}) {
      Path root = temporaryFolder.newFolder("append-rotation-resume-" + stage).toPath();
      byte[] format = hash(74);
      byte[] baseline = hash(84);
      List<BlockReverseDiff> diffs = Arrays.asList(diff(1, 1_400), diff(2, 0));
      CommonCheckpointCapture capture = capture(root, format, baseline, diffs, 1_500);
      CommonCheckpointTarget target = CommonCheckpointTarget.from(capture.getPayload());
      StateArchiveFiveLaneBlockCodecV3 codec = new StateArchiveFiveLaneBlockCodecV3();
      StateArchiveFiveLaneBlockCodecV3.EncodedBundle first = codec.encode(
          diffs.get(0), baseline, StateArchiveFileFormatV3.COMPRESSION_NONE);
      StateArchiveFiveLaneBlockCodecV3.EncodedBundle second = codec.encode(
          diffs.get(1), first.getResultHistoryDigest(),
          StateArchiveFileFormatV3.COMPRESSION_NONE);
      try (StateArchiveFiveLaneSegmentWriterV3 writer =
          new StateArchiveFiveLaneSegmentWriterV3(root, baseline,
              StateArchiveFileFormatV3.COMPRESSION_NONE, 1_500)) {
        writer.appendForCheckpoint(first, 2, target.getPayloadDigest());
        assertThrows(java.io.IOException.class,
            () -> writer.appendForCheckpoint(second, 2, target.getPayloadDigest(),
                (actual, laneId) -> {
                  if (actual == stage && laneId == 0) {
                    throw new java.io.IOException("rotation crash at " + stage);
                  }
                }));
      }
      try (StateArchiveAppendCheckpointMaterializerV3 recovered = materializer(
          root, format, baseline, 1_500)) {
        recovered.prepare(capture);
        assertEquals(Status.MATERIALIZED, recovered.inspect(target));
        assertEquals(6, StateArchiveFiveLaneDurabilityProofV3.decode(Files.readAllBytes(
            root.resolve(StateArchiveFiveLaneDurabilityProofV3.FILE_NAME)))
            .getFileTails().size());
      }
    }
  }

  @Test
  public void resumesEveryFinalBarrierPhaseBeforeSap3Publication() throws Exception {
    for (SyncStage stage : SyncStage.values()) {
      Path root = temporaryFolder.newFolder("append-sync-resume-" + stage).toPath();
      byte[] format = hash(75);
      byte[] baseline = hash(85);
      List<BlockReverseDiff> diffs = Collections.singletonList(diff(1, 24));
      CommonCheckpointCapture capture = capture(root, format, baseline, diffs, 10_000);
      CommonCheckpointTarget target = CommonCheckpointTarget.from(capture.getPayload());
      StateArchiveFiveLaneBlockCodecV3.EncodedBundle bundle =
          new StateArchiveFiveLaneBlockCodecV3().encode(diffs.get(0), baseline,
              StateArchiveFileFormatV3.COMPRESSION_NONE);
      try (StateArchiveFiveLaneSegmentWriterV3 writer =
          new StateArchiveFiveLaneSegmentWriterV3(root, baseline,
              StateArchiveFileFormatV3.COMPRESSION_NONE, 10_000)) {
        writer.appendForCheckpoint(bundle, 1, target.getPayloadDigest());
        assertThrows(java.io.IOException.class,
            () -> writer.sync(1, point(bundle), target.getPayloadDigest(),
                (actual, laneId) -> {
                  if (actual == stage) {
                    throw new java.io.IOException("sync crash at " + stage);
                  }
                }));
      }
      try (StateArchiveAppendCheckpointMaterializerV3 recovered = materializer(
          root, format, baseline, 10_000)) {
        recovered.prepare(capture);
        assertEquals(Status.MATERIALIZED, recovered.inspect(target));
      }
    }
  }

  private static StateArchiveAppendCheckpointMaterializerV3 materializer(Path root,
      byte[] format, byte[] baseline, long rotationTarget) throws Exception {
    return new StateArchiveAppendCheckpointMaterializerV3(root, format, Engine.LEVELDB,
        baseline, StateArchiveFileFormatV3.COMPRESSION_NONE, rotationTarget);
  }

  private static CommonCheckpointCapture capture(Path root, byte[] format, byte[] baseline,
      List<BlockReverseDiff> diffs, long rotationTarget) throws Exception {
    StateArchiveHotBatchDescriptor descriptor;
    try (StateArchiveAppendCheckpointMaterializerV3 planner = materializer(
        root, format, baseline, rotationTarget)) {
      descriptor = planner.planCheckpoint(diffs);
    }
    CommonCheckpointPayload payload = payload(format, diffs, descriptor);
    return CommonCheckpointCapture.create(payload, diffs, descriptor);
  }

  private static StateArchiveFiveLaneRecoveryIntentV3.RecoveryPoint point(
      StateArchiveFiveLaneBlockCodecV3.EncodedBundle bundle) {
    BlockSnapshotMeta meta = bundle.getDiff().getMeta();
    return new StateArchiveFiveLaneRecoveryIntentV3.RecoveryPoint(meta.getEpoch(),
        meta.getBlockNumber(), meta.getTimestamp(), meta.getBlockHash(), meta.getParentHash(),
        bundle.getResultHistoryDigest());
  }

  private static CommonCheckpointPayload payload(byte[] format, List<BlockReverseDiff> diffs,
      StateArchiveHotBatchDescriptor descriptor) {
    List<PathStateFlushTarget.BlockBinding> bindings = new ArrayList<>();
    for (BlockReverseDiff diff : diffs) {
      BlockSnapshotMeta meta = diff.getMeta();
      PathStateFlushTarget.BlockBinding binding = mock(PathStateFlushTarget.BlockBinding.class);
      when(binding.getMeta()).thenReturn(meta);
      when(binding.getParentStateRoot()).thenReturn(hash(30 + (int) meta.getBlockNumber()));
      when(binding.getStateRoot()).thenReturn(hash(31 + (int) meta.getBlockNumber()));
      when(binding.getTransitionPayloadDigest()).thenReturn(hash(90));
      bindings.add(binding);
    }
    PathStateFlushTarget path = mock(PathStateFlushTarget.class);
    byte[] parentStateRoot = bindings.get(0).getParentStateRoot();
    byte[] stateRoot = bindings.get(bindings.size() - 1).getStateRoot();
    when(path.getBlocks()).thenReturn(bindings);
    when(path.getParentStateRoot()).thenReturn(parentStateRoot);
    when(path.getStateRoot()).thenReturn(stateRoot);
    when(path.getStores()).thenReturn(Collections.emptyList());
    when(path.getSuperNodeMutations()).thenReturn(Collections.emptyList());
    return CommonCheckpointPayload.createV2(format, path, descriptor, Collections.emptyList());
  }

  private static BlockReverseDiff diff(int blockNumber, int valueLength) {
    List<DbGroup> groups;
    if (valueLength == 0) {
      groups = Collections.emptyList();
    } else {
      byte[] value = new byte[valueLength];
      Arrays.fill(value, (byte) blockNumber);
      groups = Collections.singletonList(new DbGroup("code", Collections.singletonList(
          new Entry(new byte[]{1}, OldValue.present(value)))));
    }
    return new BlockReverseDiff(BlockSnapshotMeta.forBlock(blockNumber, hash(blockNumber),
        hash(blockNumber - 1), blockNumber * 3_000L), groups);
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
