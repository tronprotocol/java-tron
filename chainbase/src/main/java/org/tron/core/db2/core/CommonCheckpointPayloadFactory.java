package org.tron.core.db2.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.tron.core.db2.archive.ArchiveStoreScope;
import org.tron.core.db2.archive.BlockReverseDiff;
import org.tron.core.db2.archive.BlockSnapshotMeta;
import org.tron.core.db2.archive.StateArchiveHotBatchDescriptor;
import org.tron.core.db2.archive.StateArchiveCheckpointPlanner;
import org.tron.core.db2.common.Key;
import org.tron.core.db2.common.Value;
import org.tron.core.db2.common.WrappedByteArray;
import org.tron.core.db2.stateroot.PathStateFlushTarget;
import org.tron.core.db2.stateroot.PathStateSnapshotDelta;

/** Builds one immutable common-checkpoint redo payload without querying durable databases. */
public final class CommonCheckpointPayloadFactory {

  /** Captures v2 coordination data while retaining Archive bodies only in transient memory. */
  public CommonCheckpointCapture captureV2(byte[] formatIdentity, List<Chainbase> databases,
      int flushCount, StateArchiveCheckpointPlanner archivePlanner) throws IOException {
    CommonCheckpointPayload captured = capture(formatIdentity, databases, flushCount);
    List<BlockReverseDiff> archiveDiffs = new ArrayList<>();
    for (CommonCheckpointPayload.BlockPayload block : captured.getBlocks()) {
      archiveDiffs.add(block.getArchiveDiff());
    }
    StateArchiveHotBatchDescriptor binding = Objects.requireNonNull(archivePlanner,
        "archivePlanner")
        .planCheckpoint(archiveDiffs);
    CommonCheckpointPayload coordination = CommonCheckpointPayload.coordinateV2(captured,
        binding);
    return new CommonCheckpointCapture(coordination, archiveDiffs, binding);
  }

  /** Captures the oldest {@code flushCount} Snapshot layers from every registered Store. */
  public CommonCheckpointPayload capture(byte[] formatIdentity, List<Chainbase> databases,
      int flushCount) {
    Objects.requireNonNull(formatIdentity, "formatIdentity");
    if (flushCount <= 0) {
      throw new IllegalArgumentException("common checkpoint flushCount must be positive");
    }
    List<Chainbase> admitted = new ArrayList<>(Objects.requireNonNull(databases, "databases"));
    if (admitted.isEmpty()) {
      throw new IllegalArgumentException("common checkpoint requires registered Stores");
    }

    Map<String, List<SnapshotImpl>> layersByStore = new LinkedHashMap<>();
    List<BlockSnapshotMeta> expectedMetas = null;
    for (Chainbase database : admitted) {
      Chainbase candidate = Objects.requireNonNull(database, "database");
      List<SnapshotImpl> layers = layers(candidate, flushCount);
      if (layersByStore.putIfAbsent(candidate.getDbName(), layers) != null) {
        throw new IllegalArgumentException("duplicate common checkpoint Store: "
            + candidate.getDbName());
      }
      List<BlockSnapshotMeta> metas = metas(layers, candidate.getDbName());
      if (expectedMetas == null) {
        expectedMetas = metas;
      } else if (!expectedMetas.equals(metas)) {
        throw new IllegalStateException("common checkpoint block identities differ across Stores");
      }
    }

    List<BlockReverseDiff> archiveBlocks = new ArrayList<>();
    List<PathStateSnapshotDelta> pathDeltas = new ArrayList<>();
    boolean foundStateStore = false;
    for (Map.Entry<String, List<SnapshotImpl>> entry : layersByStore.entrySet()) {
      if (!ArchiveStoreScope.isStateDatabase(entry.getKey())) {
        continue;
      }
      foundStateStore = true;
      for (int index = 0; index < entry.getValue().size(); index++) {
        SnapshotImpl layer = entry.getValue().get(index);
        BlockReverseDiff archive = layer.getPreparedArchiveBlock();
        PathStateSnapshotDelta path = layer.getPreparedPathStateDelta();
        requireArtifacts(expectedMetas.get(index), archive, path, entry.getKey());
        if (archiveBlocks.size() == index) {
          archiveBlocks.add(archive);
          pathDeltas.add(path);
        } else {
          requireSameArtifacts(archiveBlocks.get(index), pathDeltas.get(index), archive, path,
              entry.getKey());
        }
      }
    }
    if (!foundStateStore) {
      throw new IllegalStateException("common checkpoint has no state Store");
    }

    List<CommonCheckpointPayload.StoreMutations> stores = new ArrayList<>();
    for (Map.Entry<String, List<SnapshotImpl>> entry : layersByStore.entrySet()) {
      if ("trans-cache".equals(entry.getKey())) {
        continue;
      }
      Map<WrappedByteArray, CommonCheckpointPayload.Mutation> coalesced =
          new LinkedHashMap<>();
      for (SnapshotImpl layer : entry.getValue()) {
        for (Map.Entry<Key, Value> mutation : layer.getDb()) {
          byte[] key = mutation.getKey().getBytes();
          coalesced.put(WrappedByteArray.of(key), new CommonCheckpointPayload.Mutation(key,
              mutation.getValue().getBytes()));
        }
      }
      if (!coalesced.isEmpty()) {
        stores.add(new CommonCheckpointPayload.StoreMutations(entry.getKey(),
            new ArrayList<>(coalesced.values())));
      }
    }
    return CommonCheckpointPayload.create(formatIdentity,
        PathStateFlushTarget.coalesce(pathDeltas), archiveBlocks, stores);
  }

  private static List<SnapshotImpl> layers(Chainbase database, int count) {
    Snapshot next = database.getHead().getRoot();
    List<SnapshotImpl> layers = new ArrayList<>(count);
    for (int index = 0; index < count; index++) {
      next = next.getNext();
      if (!(next instanceof SnapshotImpl)) {
        throw new IllegalStateException("common checkpoint Store has too few Snapshot layers: "
            + database.getDbName());
      }
      layers.add((SnapshotImpl) next);
    }
    return layers;
  }

  private static List<BlockSnapshotMeta> metas(List<SnapshotImpl> layers, String dbName) {
    List<BlockSnapshotMeta> metas = new ArrayList<>(layers.size());
    for (SnapshotImpl layer : layers) {
      BlockSnapshotMeta meta = layer.getBlockSnapshotMeta();
      if (meta == null) {
        throw new IllegalStateException("common checkpoint Snapshot has no block identity: "
            + dbName);
      }
      metas.add(meta);
    }
    return metas;
  }

  private static void requireArtifacts(BlockSnapshotMeta meta, BlockReverseDiff archive,
      PathStateSnapshotDelta path, String dbName) {
    if (archive == null || path == null || !meta.equals(archive.getMeta())
        || !meta.equals(path.getMeta())) {
      throw new IllegalStateException("common checkpoint Snapshot artifacts differ: " + dbName);
    }
  }

  private static void requireSameArtifacts(BlockReverseDiff expectedArchive,
      PathStateSnapshotDelta expectedPath, BlockReverseDiff archive,
      PathStateSnapshotDelta path, String dbName) {
    if (expectedArchive != archive || expectedPath != path) {
      throw new IllegalStateException("common checkpoint artifacts differ across state Stores: "
          + dbName);
    }
  }
}
