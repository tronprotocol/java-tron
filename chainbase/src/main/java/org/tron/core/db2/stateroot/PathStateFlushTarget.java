package org.tron.core.db2.stateroot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.tron.core.db2.archive.BlockSnapshotMeta;

/** Immutable deterministic coalescing target for one consecutive Snapshot flush range. */
public final class PathStateFlushTarget {

  private static final Comparator<PathStateSnapshotDelta.Mutation> MUTATION_ORDER =
      (left, right) -> compareUnsigned(left.getKey(), right.getKey());

  private final List<BlockBinding> blocks;
  private final byte[] parentStateRoot;
  private final byte[] stateRoot;
  private final List<StoreTarget> stores;
  private final List<PathStateSnapshotDelta.Mutation> superNodeMutations;
  private final long mutationBytes;

  private PathStateFlushTarget(List<BlockBinding> blocks, byte[] parentStateRoot,
      byte[] stateRoot, List<StoreTarget> stores,
      List<PathStateSnapshotDelta.Mutation> superNodeMutations) {
    this.blocks = Collections.unmodifiableList(new ArrayList<>(blocks));
    this.parentStateRoot = copy(parentStateRoot);
    this.stateRoot = copy(stateRoot);
    this.stores = Collections.unmodifiableList(new ArrayList<>(stores));
    this.superNodeMutations = immutableMutations(superNodeMutations);
    this.mutationBytes = mutationBytes(this.stores, this.superNodeMutations);
  }

  /** Validates and coalesces a non-empty oldest-to-newest path-state Snapshot delta chain. */
  public static PathStateFlushTarget coalesce(List<PathStateSnapshotDelta> supplied) {
    List<PathStateSnapshotDelta> deltas = new ArrayList<>(Objects.requireNonNull(supplied,
        "deltas"));
    if (deltas.isEmpty()) {
      throw new IllegalArgumentException("path-state flush delta chain must not be empty");
    }

    List<BlockBinding> blocks = new ArrayList<>();
    Map<Integer, StoreAccumulator> stores = new LinkedHashMap<>();
    Map<String, Integer> storeNames = new LinkedHashMap<>();
    Map<BytesKey, PathStateSnapshotDelta.Mutation> superNodes = new LinkedHashMap<>();
    PathStateSnapshotDelta previous = null;
    for (PathStateSnapshotDelta candidate : deltas) {
      PathStateSnapshotDelta delta = Objects.requireNonNull(candidate, "delta");
      if (previous != null) {
        requireChild(previous, delta);
      }
      blocks.add(new BlockBinding(delta));
      for (PathStateSnapshotDelta.StoreDelta store : delta.getStores()) {
        Integer priorId = storeNames.putIfAbsent(store.getDbName(), store.getStoreId());
        if (priorId != null && priorId != store.getStoreId()) {
          throw new IllegalArgumentException("path-state flush Store name changes identity");
        }
        StoreAccumulator accumulator = stores.computeIfAbsent(store.getStoreId(), ignored ->
            new StoreAccumulator(store.getStoreId(), store.getDbName()));
        accumulator.add(store);
      }
      putAll(superNodes, delta.getSuperNodeMutations());
      previous = delta;
    }

    List<StoreTarget> storeTargets = new ArrayList<>();
    stores.values().stream().sorted(Comparator.comparingInt(store -> store.storeId))
        .forEach(store -> storeTargets.add(store.freeze()));
    PathStateSnapshotDelta first = deltas.get(0);
    PathStateSnapshotDelta last = deltas.get(deltas.size() - 1);
    return new PathStateFlushTarget(blocks, first.getParentStateRoot(), last.getStateRoot(),
        storeTargets, new ArrayList<>(superNodes.values()));
  }

  public List<BlockBinding> getBlocks() {
    return blocks;
  }

  public byte[] getParentStateRoot() {
    return copy(parentStateRoot);
  }

  public byte[] getStateRoot() {
    return copy(stateRoot);
  }

  public List<StoreTarget> getStores() {
    return stores;
  }

  public List<PathStateSnapshotDelta.Mutation> getSuperNodeMutations() {
    return superNodeMutations;
  }

  public long getMutationBytes() {
    return mutationBytes;
  }

  private static void requireChild(PathStateSnapshotDelta parent,
      PathStateSnapshotDelta child) {
    BlockSnapshotMeta parentMeta = parent.getMeta();
    BlockSnapshotMeta childMeta = child.getMeta();
    if (childMeta.getEpoch() != parentMeta.getEpoch() + 1
        || childMeta.getBlockNumber() != parentMeta.getBlockNumber() + 1
        || !Arrays.equals(childMeta.getParentHash(), parentMeta.getBlockHash())
        || !Arrays.equals(child.getParentStateRoot(), parent.getStateRoot())) {
      throw new IllegalArgumentException("path-state flush delta chain is not consecutive");
    }
  }

  private static void putAll(Map<BytesKey, PathStateSnapshotDelta.Mutation> target,
      List<PathStateSnapshotDelta.Mutation> mutations) {
    for (PathStateSnapshotDelta.Mutation mutation : mutations) {
      byte[] key = mutation.getKey();
      target.put(new BytesKey(key), new PathStateSnapshotDelta.Mutation(key,
          mutation.getValue()));
    }
  }

  private static List<PathStateSnapshotDelta.Mutation> immutableMutations(
      List<PathStateSnapshotDelta.Mutation> mutations) {
    List<PathStateSnapshotDelta.Mutation> copy = new ArrayList<>(mutations);
    copy.sort(MUTATION_ORDER);
    return Collections.unmodifiableList(copy);
  }

  private static long mutationBytes(List<StoreTarget> stores,
      List<PathStateSnapshotDelta.Mutation> superNodes) {
    long bytes = mutationsBytes(superNodes);
    for (StoreTarget store : stores) {
      bytes = Math.addExact(bytes, mutationsBytes(store.flatMutations));
      bytes = Math.addExact(bytes, mutationsBytes(store.nodeMutations));
    }
    return bytes;
  }

  private static long mutationsBytes(List<PathStateSnapshotDelta.Mutation> mutations) {
    long bytes = 0;
    for (PathStateSnapshotDelta.Mutation mutation : mutations) {
      bytes = Math.addExact(bytes, mutation.getKey().length);
      byte[] value = mutation.getValue();
      if (value != null) {
        bytes = Math.addExact(bytes, value.length);
      }
    }
    return bytes;
  }

  private static byte[] copy(byte[] value) {
    return Arrays.copyOf(Objects.requireNonNull(value, "value"), value.length);
  }

  private static int compareUnsigned(byte[] left, byte[] right) {
    for (int index = 0; index < Math.min(left.length, right.length); index++) {
      int compared = Integer.compare(left[index] & 0xff, right[index] & 0xff);
      if (compared != 0) {
        return compared;
      }
    }
    return Integer.compare(left.length, right.length);
  }

  /** Per-block chain and payload identity retained even though forward mutations are coalesced. */
  public static final class BlockBinding {

    private final BlockSnapshotMeta meta;
    private final byte[] parentStateRoot;
    private final byte[] stateRoot;
    private final byte[] transitionPayloadDigest;

    private BlockBinding(PathStateSnapshotDelta delta) {
      this.meta = delta.getMeta();
      this.parentStateRoot = delta.getParentStateRoot();
      this.stateRoot = delta.getStateRoot();
      this.transitionPayloadDigest = delta.getTransitionPayloadDigest();
    }

    public BlockSnapshotMeta getMeta() {
      return meta;
    }

    public byte[] getParentStateRoot() {
      return copy(parentStateRoot);
    }

    public byte[] getStateRoot() {
      return copy(stateRoot);
    }

    public byte[] getTransitionPayloadDigest() {
      return copy(transitionPayloadDigest);
    }

  }

  /** Final target for one participant changed anywhere in the coalesced range. */
  public static final class StoreTarget {

    private final int storeId;
    private final String dbName;
    private final byte[] storeRoot;
    private final List<PathStateSnapshotDelta.Mutation> flatMutations;
    private final List<PathStateSnapshotDelta.Mutation> nodeMutations;

    private StoreTarget(StoreAccumulator accumulator) {
      this.storeId = accumulator.storeId;
      this.dbName = accumulator.dbName;
      this.storeRoot = copy(accumulator.storeRoot);
      this.flatMutations = immutableMutations(new ArrayList<>(accumulator.flat.values()));
      this.nodeMutations = immutableMutations(new ArrayList<>(accumulator.nodes.values()));
    }

    public int getStoreId() {
      return storeId;
    }

    public String getDbName() {
      return dbName;
    }

    public byte[] getStoreRoot() {
      return copy(storeRoot);
    }

    public List<PathStateSnapshotDelta.Mutation> getFlatMutations() {
      return flatMutations;
    }

    public List<PathStateSnapshotDelta.Mutation> getNodeMutations() {
      return nodeMutations;
    }
  }

  private static final class StoreAccumulator {

    private final int storeId;
    private final String dbName;
    private final Map<BytesKey, PathStateSnapshotDelta.Mutation> flat = new LinkedHashMap<>();
    private final Map<BytesKey, PathStateSnapshotDelta.Mutation> nodes = new LinkedHashMap<>();
    private byte[] storeRoot;

    private StoreAccumulator(int storeId, String dbName) {
      this.storeId = storeId;
      this.dbName = dbName;
    }

    private void add(PathStateSnapshotDelta.StoreDelta store) {
      if (!dbName.equals(store.getDbName())) {
        throw new IllegalArgumentException("path-state flush Store ID changes identity");
      }
      putAll(flat, store.getFlatMutations());
      putAll(nodes, store.getNodeMutations());
      storeRoot = store.getStoreRoot();
    }

    private StoreTarget freeze() {
      return new StoreTarget(this);
    }
  }

  private static final class BytesKey {

    private final byte[] bytes;

    private BytesKey(byte[] bytes) {
      this.bytes = copy(bytes);
    }

    @Override
    public boolean equals(Object other) {
      return this == other || other instanceof BytesKey
          && Arrays.equals(bytes, ((BytesKey) other).bytes);
    }

    @Override
    public int hashCode() {
      return Arrays.hashCode(bytes);
    }
  }
}
