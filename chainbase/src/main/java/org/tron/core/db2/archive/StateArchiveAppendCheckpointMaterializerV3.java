package org.tron.core.db2.archive;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.tron.core.db2.archive.StateArchiveFiveLaneBlockCodecV3.EncodedBundle;
import org.tron.core.db2.archive.StateArchiveFiveLaneRecoveryIntentV3.RecoveryPoint;
import org.tron.core.db2.archive.StateArchiveFiveLaneSegmentWriterV3.ArchiveDurabilityProof;
import org.tron.core.db2.core.CommonCheckpointCapture;
import org.tron.core.db2.core.CommonCheckpointMaterializer;
import org.tron.core.db2.core.CommonCheckpointPayload;
import org.tron.core.db2.core.CommonCheckpointTarget;
import org.tron.core.db2.stateroot.PathStateStoreManifest.Engine;

/** Default-off Common participant backed by the five-lane append-file v3 authority. */
public final class StateArchiveAppendCheckpointMaterializerV3
    implements CommonCheckpointMaterializer, StateArchiveCheckpointPlanner {

  private final Path directory;
  private final byte[] commonFormatIdentity;
  private final Engine bindingEngine;
  private final short compressionId;
  private final StateArchiveFiveLaneBlockCodecV3 codec =
      new StateArchiveFiveLaneBlockCodecV3();
  private final StateArchiveFiveLaneSegmentWriterV3 writer;
  private final StateArchiveServingWorkerV3 servingWorker;
  private boolean closed;

  public StateArchiveAppendCheckpointMaterializerV3(Path directory,
      byte[] commonFormatIdentity, Engine bindingEngine, byte[] baselineHistoryDigest,
      short compressionId) throws IOException {
    this(directory, commonFormatIdentity, bindingEngine, baselineHistoryDigest,
        compressionId, StateArchiveFileFormatV3.SEGMENT_TARGET_BYTES);
  }

  public StateArchiveAppendCheckpointMaterializerV3(Path directory,
      byte[] commonFormatIdentity, Engine bindingEngine, byte[] baselineHistoryDigest,
      short compressionId, long rotationTargetBytes) throws IOException {
    this(directory, commonFormatIdentity, bindingEngine, baselineHistoryDigest, compressionId,
        rotationTargetBytes, () -> { });
  }

  StateArchiveAppendCheckpointMaterializerV3(Path directory,
      byte[] commonFormatIdentity, Engine bindingEngine, byte[] baselineHistoryDigest,
      short compressionId, long rotationTargetBytes, Runnable beforeServingBuild) throws IOException {
    this.directory = Objects.requireNonNull(directory, "directory");
    this.commonFormatIdentity = requireDigest(commonFormatIdentity, "Common format identity");
    this.bindingEngine = Objects.requireNonNull(bindingEngine, "bindingEngine");
    this.compressionId = compressionId;
    this.writer = new StateArchiveFiveLaneSegmentWriterV3(directory,
        baselineHistoryDigest, compressionId, rotationTargetBytes);
    this.servingWorker = new StateArchiveServingWorkerV3(
        () -> new StateArchiveServingIndexBuildCoordinatorV3(directory, bindingEngine, 1_000),
        writer, beforeServingBuild);
  }

  @Override
  public Authority authority() {
    return Authority.STATE_ARCHIVE;
  }

  /** Builds the existing v2 transient-body binding without writing append-file bytes. */
  @Override
  public synchronized StateArchiveHotBatchDescriptor planCheckpoint(
      List<BlockReverseDiff> diffs) throws IOException {
    requireOpen();
    List<BlockReverseDiff> admitted = admittedDiffs(diffs);
    BlockSnapshotMeta first = admitted.get(0).getMeta();
    long lastBlock = admitted.get(admitted.size() - 1).getMeta().getBlockNumber();
    BlockSnapshotMeta head = writer.getAppendHead();
    if (head != null && (head.getBlockNumber() < first.getBlockNumber() - 1
        || head.getBlockNumber() > lastBlock)) {
      throw new IOException("Append-file Archive checkpoint is not the current successor or retry");
    }
    return StateArchiveHotStore.planCheckpointDescriptor(bindingEngine,
        first.getBlockNumber() - 1, first.getParentHash(),
        new byte[StateArchiveFileFormatV3.HASH_LENGTH], admitted);
  }

  /** Appends, group-forces, rereads and persists SAP3 before the Common WAL is published. */
  @Override
  public synchronized CommonCheckpointTarget prepare(CommonCheckpointCapture capture)
      throws IOException {
    requireOpen();
    CommonCheckpointCapture admitted = Objects.requireNonNull(capture, "capture");
    CommonCheckpointTarget target = requireTarget(
        CommonCheckpointTarget.from(admitted.getPayload()));
    Status status = inspect(target);
    if (status != Status.NEEDS_MATERIALIZATION) {
      return target;
    }
    List<BlockReverseDiff> diffs = admittedDiffs(admitted.getArchiveDiffs());
    if (!admitted.getArchiveBinding().equals(planCheckpoint(diffs))) {
      throw new IOException("Append-file Archive checkpoint binding differs");
    }
    ArchiveDurabilityProof recoveredProof = writer.getLastDurabilityProof();
    if (recoveredProof != null && matches(recoveredProof, target)) {
      ArchiveDurabilityProof forced = writer.sync(recoveredProof.getCheckpointSequence(),
          recoveredProof.getTarget(), target.getPayloadDigest());
      StateArchiveFiveLaneDurabilityProofV3.publish(directory, forced);
      return target;
    }
    byte[] previousHistory = writer.getResultHistoryDigest();
    int firstMissing = firstMissing(diffs, writer.getAppendHead());
    for (int index = firstMissing; index < diffs.size(); index++) {
      BlockReverseDiff diff = diffs.get(index);
      EncodedBundle bundle = codec.encode(diff, previousHistory,
          writerCompressionId());
      writer.appendForCheckpoint(bundle, target.getLastBlock().getBlockNumber(),
          target.getPayloadDigest());
      previousHistory = bundle.getResultHistoryDigest();
    }
    BlockSnapshotMeta meta = target.getLastBlock();
    RecoveryPoint point = new RecoveryPoint(meta.getEpoch(), meta.getBlockNumber(),
        meta.getTimestamp(), meta.getBlockHash(), meta.getParentHash(),
        writer.getResultHistoryDigest());
    ArchiveDurabilityProof proof = writer.sync(target.getLastBlock().getBlockNumber(),
        point, target.getPayloadDigest());
    StateArchiveFiveLaneDurabilityProofV3.publish(directory, proof);
    if (inspect(target) != Status.MATERIALIZED) {
      throw new IOException("Append-file Archive SAP3 materialization is not exact");
    }
    return target;
  }

  @Override
  public synchronized Status inspect(CommonCheckpointTarget target) throws IOException {
    requireOpen();
    CommonCheckpointTarget admitted = requireTarget(target);
    Optional<CommonCheckpointTarget> readable =
        StateArchiveCheckpointMaterializer.loadReadableTargetIfPresent(directory);
    if (readable.isPresent() && readable.get().equals(admitted)) {
      requireExactProof(admitted);
      return Status.PUBLISHED;
    }
    if (readable.isPresent()) {
      requireParent(readable.get(), admitted);
    }
    if (!Files.isRegularFile(directory.resolve(StateArchiveFiveLaneDurabilityProofV3.FILE_NAME),
        LinkOption.NOFOLLOW_LINKS)) {
      return Status.NEEDS_MATERIALIZATION;
    }
    ArchiveDurabilityProof proof = StateArchiveFiveLaneDurabilityProofV3.loadAndVerify(
        directory, writer);
    if (matches(proof, admitted)) {
      return Status.MATERIALIZED;
    }
    if (!readable.isPresent()) {
      throw new IOException("Append-file Archive has an uncommitted different SAP3 target");
    }
    return Status.NEEDS_MATERIALIZATION;
  }

  /** Loads and revalidates the append-file target made readable by the Common publish barrier. */
  public synchronized Optional<CommonCheckpointTarget> loadPublishedTargetIfPresent()
      throws IOException {
    requireOpen();
    Optional<CommonCheckpointTarget> target =
        StateArchiveCheckpointMaterializer.loadReadableTargetIfPresent(directory);
    if (target.isPresent() && inspect(target.get()) != Status.PUBLISHED) {
      throw new IOException("Append-file Archive readable target is not fully published");
    }
    return target;
  }

  @Override
  public synchronized void materialize(CommonCheckpointPayload payload,
      CommonCheckpointTarget target) throws IOException {
    CommonCheckpointPayload admittedPayload = Objects.requireNonNull(payload, "payload");
    CommonCheckpointTarget admittedTarget = requireTarget(target);
    if (admittedPayload.getVersion() != CommonCheckpointPayload.COORDINATION_FORMAT_VERSION
        || !admittedTarget.equals(CommonCheckpointTarget.from(admittedPayload))) {
      throw new IOException("Append-file Archive requires its exact coordination payload");
    }
    if (inspect(admittedTarget) == Status.NEEDS_MATERIALIZATION) {
      throw new IOException("Append-file Archive must be prepared before Common WAL publication");
    }
  }

  @Override
  public synchronized void publish(CommonCheckpointTarget target) throws IOException {
    CommonCheckpointTarget admitted = requireTarget(target);
    Status status = inspect(admitted);
    if (status == Status.PUBLISHED) {
      return;
    }
    if (status != Status.MATERIALIZED) {
      throw new IOException("Append-file Archive target is not materialized");
    }
    StateArchiveCheckpointMaterializer.publishReadableTarget(directory, admitted);
  }

  @Override
  public void afterCommit(CommonCheckpointTarget target) {
    try {
      servingWorker.offer(target);
    } catch (IOException | RuntimeException failure) {
      org.slf4j.LoggerFactory.getLogger("DB").error(
          "Archive serving degraded after Common commit at {}",
          target.getLastBlock().getBlockNumber(), failure);
    }
  }

  /** Explicit sync-lifecycle handoff; it never infers completion from peer/head timing. */
  public synchronized void completeServingInitialSync(CommonCheckpointTarget boundary)
      throws IOException {
    requireOpen();
    servingWorker.completeInitialSync(boundary);
  }

  public synchronized StateArchiveServingIndexBuildCoordinatorV3.BuildProgress
      servingIndexStatus() {
    return servingWorker.status();
  }

  public IOException servingIndexFailure() {
    return servingWorker.failure();
  }

  @Override
  public synchronized void close() throws IOException {
    if (!closed) {
      closed = true;
      IOException failure = null;
      try {
        servingWorker.close();
      } catch (IOException closeFailure) {
        failure = closeFailure;
      }
      try {
        writer.close();
      } catch (IOException closeFailure) {
        if (failure == null) {
          failure = closeFailure;
        } else {
          failure.addSuppressed(closeFailure);
        }
      }
      if (failure != null) {
        throw failure;
      }
    }
  }

  private void requireExactProof(CommonCheckpointTarget target) throws IOException {
    ArchiveDurabilityProof proof = StateArchiveFiveLaneDurabilityProofV3.loadAndVerify(
        directory, writer);
    if (!matches(proof, target)) {
      throw new IOException("Append-file Archive proof differs from readable target");
    }
  }

  private static boolean matches(ArchiveDurabilityProof proof,
      CommonCheckpointTarget target) {
    BlockSnapshotMeta meta = target.getLastBlock();
    RecoveryPoint point = proof.getTarget();
    return point.getEpoch() == meta.getEpoch()
        && point.getBlockNumber() == meta.getBlockNumber()
        && point.getTimestamp() == meta.getTimestamp()
        && Arrays.equals(point.getBlockHash(), meta.getBlockHash())
        && Arrays.equals(point.getParentHash(), meta.getParentHash())
        && Arrays.equals(proof.getCommonTargetDigest(), target.getPayloadDigest());
  }

  private CommonCheckpointTarget requireTarget(CommonCheckpointTarget target)
      throws IOException {
    CommonCheckpointTarget admitted = Objects.requireNonNull(target, "target");
    if (!Arrays.equals(commonFormatIdentity, admitted.getFormatIdentity())) {
      throw new IOException("Append-file Archive Common format identity differs");
    }
    return admitted;
  }

  private static void requireParent(CommonCheckpointTarget parent,
      CommonCheckpointTarget target) throws IOException {
    if (!Arrays.equals(parent.getFormatIdentity(), target.getFormatIdentity())
        || parent.getLastBlock().getBlockNumber() + 1 != target.getFirstBlock().getBlockNumber()
        || !Arrays.equals(parent.getLastBlock().getBlockHash(),
            target.getFirstBlock().getParentHash())
        || !Arrays.equals(parent.getStateRoot(), target.getParentStateRoot())) {
      throw new IOException("Append-file Archive readable target is not the parent");
    }
  }

  private static List<BlockReverseDiff> admittedDiffs(List<BlockReverseDiff> diffs) {
    List<BlockReverseDiff> admitted = new ArrayList<>(Objects.requireNonNull(diffs, "diffs"));
    if (admitted.isEmpty() || admitted.contains(null)) {
      throw new IllegalArgumentException("Append-file Archive checkpoint requires blocks");
    }
    BlockSnapshotMeta previous = null;
    for (BlockReverseDiff diff : admitted) {
      BlockSnapshotMeta current = diff.getMeta();
      if (current.getEpoch() != current.getBlockNumber()
          || previous != null && (current.getBlockNumber() != previous.getBlockNumber() + 1
          || !Arrays.equals(current.getParentHash(), previous.getBlockHash()))) {
        throw new IllegalArgumentException("Append-file Archive block chain is not contiguous");
      }
      previous = current;
    }
    return admitted;
  }

  private static int firstMissing(List<BlockReverseDiff> diffs, BlockSnapshotMeta head)
      throws IOException {
    if (head == null || head.getBlockNumber() < diffs.get(0).getMeta().getBlockNumber()) {
      return 0;
    }
    for (int index = 0; index < diffs.size(); index++) {
      BlockSnapshotMeta meta = diffs.get(index).getMeta();
      if (meta.getBlockNumber() == head.getBlockNumber()) {
        if (!meta.equals(head)) {
          throw new IOException("Append-file Archive retry prefix identity differs");
        }
        return index + 1;
      }
    }
    throw new IOException("Append-file Archive retry head is outside the checkpoint range");
  }

  private short writerCompressionId() {
    return compressionId;
  }

  private void requireOpen() throws IOException {
    if (closed) {
      throw new IOException("Append-file Archive materializer is closed");
    }
  }

  private static byte[] requireDigest(byte[] value, String name) {
    byte[] admitted = Arrays.copyOf(Objects.requireNonNull(value, name), value.length);
    if (admitted.length != StateArchiveFileFormatV3.HASH_LENGTH) {
      throw new IllegalArgumentException(name + " must contain exactly 32 bytes");
    }
    return admitted;
  }
}
