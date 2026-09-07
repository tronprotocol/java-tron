package org.tron.core.db2.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.tron.core.db2.archive.BlockReverseDiff;
import org.tron.core.db2.archive.StateArchiveHotBatchDescriptor;

/** V2 coordination payload plus transient Archive bodies that are never encoded into its WAL. */
public final class CommonCheckpointCapture {

  private final CommonCheckpointPayload payload;
  private final List<BlockReverseDiff> archiveDiffs;
  private final StateArchiveHotBatchDescriptor archiveBinding;

  CommonCheckpointCapture(CommonCheckpointPayload payload, List<BlockReverseDiff> archiveDiffs,
      StateArchiveHotBatchDescriptor archiveBinding) {
    this.payload = Objects.requireNonNull(payload, "payload");
    if (payload.getVersion() != CommonCheckpointPayload.COORDINATION_FORMAT_VERSION) {
      throw new IllegalArgumentException("common checkpoint capture requires payload v2");
    }
    this.archiveDiffs = Collections.unmodifiableList(new ArrayList<>(
        Objects.requireNonNull(archiveDiffs, "archiveDiffs")));
    this.archiveBinding = Objects.requireNonNull(archiveBinding, "archiveBinding");
    if (this.archiveDiffs.size() != archiveBinding.getBlockCount()
        || !archiveBinding.equals(payload.getArchiveBinding())) {
      throw new IllegalArgumentException("common checkpoint capture binding differs");
    }
    for (int index = 0; index < this.archiveDiffs.size(); index++) {
      BlockReverseDiff diff = Objects.requireNonNull(this.archiveDiffs.get(index),
          "archiveDiff");
      StateArchiveHotBatchDescriptor.BlockDigest block = archiveBinding.getBlocks().get(index);
      if (!diff.getMeta().equals(block.getMeta())
          || diff.getMutationViewDigest() == null
          || !Arrays.equals(diff.getMutationViewDigest(),
          block.getMutationViewDigest())) {
        throw new IllegalArgumentException("common checkpoint transient Archive diff differs");
      }
    }
  }

  public static CommonCheckpointCapture create(CommonCheckpointPayload payload,
      List<BlockReverseDiff> archiveDiffs, StateArchiveHotBatchDescriptor archiveBinding) {
    return new CommonCheckpointCapture(payload, archiveDiffs, archiveBinding);
  }

  public CommonCheckpointPayload getPayload() {
    return payload;
  }

  public List<BlockReverseDiff> getArchiveDiffs() {
    return archiveDiffs;
  }

  public StateArchiveHotBatchDescriptor getArchiveBinding() {
    return archiveBinding;
  }
}
