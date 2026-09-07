package org.tron.core.db2.archive;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import org.tron.core.db2.core.CommonCheckpointCapture;
import org.tron.core.db2.core.CommonCheckpointMaterializer;
import org.tron.core.db2.core.CommonCheckpointPayload;
import org.tron.core.db2.core.CommonCheckpointTarget;

/** Default-off State Archive participant backed only by the independent Hot DB. */
public final class StateArchiveHotCheckpointMaterializer
    implements CommonCheckpointMaterializer {

  private final StateArchiveHotStore hotStore;

  public StateArchiveHotCheckpointMaterializer(StateArchiveHotStore hotStore) {
    this.hotStore = Objects.requireNonNull(hotStore, "hotStore");
  }

  /** Computes the exact Hot batch identity without writing bodies or checkpoint metadata. */
  public synchronized StateArchiveHotBatchDescriptor planCheckpoint(
      List<BlockReverseDiff> diffs) throws IOException {
    return hotStore.planCheckpoint(Objects.requireNonNull(diffs, "diffs"));
  }

  /** Prepares one capture whose payload, descriptor and transient bodies share one identity. */
  public synchronized CommonCheckpointTarget prepare(CommonCheckpointCapture capture)
      throws IOException {
    CommonCheckpointCapture admitted = Objects.requireNonNull(capture, "capture");
    CommonCheckpointTarget target = CommonCheckpointTarget.from(admitted.getPayload());
    prepare(target, admitted.getArchiveBinding(), admitted.getArchiveDiffs());
    return target;
  }

  synchronized void prepare(CommonCheckpointTarget target, List<BlockReverseDiff> diffs)
      throws IOException {
    if (inspect(Objects.requireNonNull(target, "target")) != Status.NEEDS_MATERIALIZATION) {
      hotStore.prepareCheckpoint(target.getPayloadDigest(), diffs);
      return;
    }
    prepare(target, hotStore.planCheckpoint(diffs), diffs);
  }

  /** Prepares transient bodies only when their computed identity equals the v2 binding. */
  synchronized void prepare(CommonCheckpointTarget target,
      StateArchiveHotBatchDescriptor descriptor, List<BlockReverseDiff> diffs)
      throws IOException {
    CommonCheckpointTarget admitted = Objects.requireNonNull(target, "target");
    List<BlockReverseDiff> admittedDiffs = Objects.requireNonNull(diffs, "diffs");
    if (admittedDiffs.isEmpty()
        || !admitted.getFirstBlock().equals(admittedDiffs.get(0).getMeta())
        || !admitted.getLastBlock().equals(
        admittedDiffs.get(admittedDiffs.size() - 1).getMeta())) {
      throw new ArchivePersistenceException(
          "Hot Archive checkpoint block range differs from target");
    }
    hotStore.requireFormatIdentity(admitted.getFormatIdentity());
    hotStore.prepareCheckpoint(admitted.getPayloadDigest(), descriptor, admittedDiffs);
  }

  /** Reconciles only an unpublished Hot DB tail to a caller-validated recovery authority. */
  public synchronized long reconcilePreparedTail(BlockSnapshotMeta authority)
      throws IOException {
    return hotStore.reconcilePreparedTail(authority);
  }

  @Override
  public Authority authority() {
    return Authority.STATE_ARCHIVE;
  }

  @Override
  public synchronized Status inspect(CommonCheckpointTarget target) throws IOException {
    CommonCheckpointTarget admitted = Objects.requireNonNull(target, "target");
    hotStore.requireFormatIdentity(admitted.getFormatIdentity());
    StateArchiveHotStore.HotCheckpointStatus status = hotStore.inspectCheckpoint(
        admitted.getPayloadDigest(), admitted.getArchiveBinding());
    switch (status) {
      case MATERIALIZED:
        return Status.MATERIALIZED;
      case PUBLISHED:
        return Status.PUBLISHED;
      default:
        return Status.NEEDS_MATERIALIZATION;
    }
  }

  /**
   * The common coordinator may verify an already prepared target, but must never ask its WAL
   * payload to materialize Archive bodies.
   */
  @Override
  public synchronized void materialize(CommonCheckpointPayload payload,
      CommonCheckpointTarget target) throws IOException {
    CommonCheckpointPayload admittedPayload = Objects.requireNonNull(payload, "payload");
    CommonCheckpointTarget admittedTarget = Objects.requireNonNull(target, "target");
    if (!admittedTarget.equals(CommonCheckpointTarget.from(admittedPayload))) {
      throw new IOException("Hot Archive checkpoint payload and target differ");
    }
    if (admittedPayload.getVersion()
        != CommonCheckpointPayload.COORDINATION_FORMAT_VERSION) {
      throw new IOException("Hot Archive materializer requires coordination payload v2");
    }
    if (inspect(admittedTarget) == Status.NEEDS_MATERIALIZATION) {
      throw new IOException(
          "Hot Archive target must be prepared before common checkpoint WAL publication");
    }
  }

  @Override
  public synchronized void publish(CommonCheckpointTarget target) throws IOException {
    CommonCheckpointTarget admitted = Objects.requireNonNull(target, "target");
    hotStore.requireFormatIdentity(admitted.getFormatIdentity());
    hotStore.publishCheckpoint(admitted.getPayloadDigest(), admitted.getArchiveBinding());
  }

  @Override
  public synchronized void close() throws IOException {
    hotStore.close();
  }
}
