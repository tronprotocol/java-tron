package org.tron.core.db2.core;

import java.util.Arrays;
import java.util.Objects;
import org.tron.core.db2.archive.BlockSnapshotMeta;
import org.tron.core.db2.archive.StateArchiveHotBatchDescriptor;

/** Immutable identity every authority must reach for one common-checkpoint payload. */
public final class CommonCheckpointTarget {

  private final byte[] formatIdentity;
  private final byte[] payloadDigest;
  private final BlockSnapshotMeta firstBlock;
  private final BlockSnapshotMeta lastBlock;
  private final byte[] parentStateRoot;
  private final byte[] stateRoot;
  private final StateArchiveHotBatchDescriptor archiveBinding;

  private CommonCheckpointTarget(byte[] formatIdentity, byte[] payloadDigest,
      BlockSnapshotMeta firstBlock, BlockSnapshotMeta lastBlock, byte[] parentStateRoot,
      byte[] stateRoot, StateArchiveHotBatchDescriptor archiveBinding) {
    this.formatIdentity = copy(formatIdentity);
    this.payloadDigest = copy(payloadDigest);
    this.firstBlock = Objects.requireNonNull(firstBlock, "firstBlock");
    this.lastBlock = Objects.requireNonNull(lastBlock, "lastBlock");
    this.parentStateRoot = copy(parentStateRoot);
    this.stateRoot = copy(stateRoot);
    this.archiveBinding = archiveBinding;
  }

  public static CommonCheckpointTarget from(CommonCheckpointPayload payload) {
    CommonCheckpointPayload admitted = Objects.requireNonNull(payload, "payload");
    return new CommonCheckpointTarget(admitted.getFormatIdentity(),
        new CommonCheckpointPayloadCodec().digest(admitted),
        admitted.getBlocks().get(0).getMeta(),
        admitted.getBlocks().get(admitted.getBlocks().size() - 1).getMeta(),
        admitted.getParentStateRoot(), admitted.getStateRoot(),
        admitted.getVersion() == CommonCheckpointPayload.COORDINATION_FORMAT_VERSION
            ? admitted.getArchiveBinding() : null);
  }

  /** Reconstructs a target identity from a checksummed authority publication record. */
  public static CommonCheckpointTarget restore(byte[] formatIdentity, byte[] payloadDigest,
      BlockSnapshotMeta firstBlock, BlockSnapshotMeta lastBlock, byte[] parentStateRoot,
      byte[] stateRoot) {
    BlockSnapshotMeta first = Objects.requireNonNull(firstBlock, "firstBlock");
    BlockSnapshotMeta last = Objects.requireNonNull(lastBlock, "lastBlock");
    if (first.getEpoch() > last.getEpoch()
        || first.getBlockNumber() > last.getBlockNumber()) {
      throw new IllegalArgumentException("checkpoint target block range is reversed");
    }
    return new CommonCheckpointTarget(requireDigest(formatIdentity, "formatIdentity"),
        requireDigest(payloadDigest, "payloadDigest"), first, last,
        requireDigest(parentStateRoot, "parentStateRoot"), requireDigest(stateRoot, "stateRoot"),
        null);
  }

  public byte[] getFormatIdentity() {
    return copy(formatIdentity);
  }

  public byte[] getPayloadDigest() {
    return copy(payloadDigest);
  }

  public BlockSnapshotMeta getFirstBlock() {
    return firstBlock;
  }

  public BlockSnapshotMeta getLastBlock() {
    return lastBlock;
  }

  public byte[] getParentStateRoot() {
    return copy(parentStateRoot);
  }

  public byte[] getStateRoot() {
    return copy(stateRoot);
  }

  public StateArchiveHotBatchDescriptor getArchiveBinding() {
    if (archiveBinding == null) {
      throw new IllegalStateException("checkpoint target has no Hot Archive binding");
    }
    return archiveBinding;
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (!(object instanceof CommonCheckpointTarget)) {
      return false;
    }
    CommonCheckpointTarget that = (CommonCheckpointTarget) object;
    return firstBlock.equals(that.firstBlock)
        && lastBlock.equals(that.lastBlock)
        && Arrays.equals(formatIdentity, that.formatIdentity)
        && Arrays.equals(payloadDigest, that.payloadDigest)
        && Arrays.equals(parentStateRoot, that.parentStateRoot)
        && Arrays.equals(stateRoot, that.stateRoot);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(firstBlock, lastBlock);
    result = 31 * result + Arrays.hashCode(formatIdentity);
    result = 31 * result + Arrays.hashCode(payloadDigest);
    result = 31 * result + Arrays.hashCode(parentStateRoot);
    result = 31 * result + Arrays.hashCode(stateRoot);
    return result;
  }

  private static byte[] copy(byte[] value) {
    return Arrays.copyOf(Objects.requireNonNull(value, "value"), value.length);
  }

  private static byte[] requireDigest(byte[] value, String name) {
    byte[] admitted = copy(value);
    if (admitted.length != 32) {
      throw new IllegalArgumentException(name + " must contain exactly 32 bytes");
    }
    return admitted;
  }
}
