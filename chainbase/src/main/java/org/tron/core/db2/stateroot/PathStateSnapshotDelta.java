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

/**
 * Immutable Snapshot-owned path-state forward delta for one successfully applied block.
 *
 * <p>This object contains the actual F/N mutations needed by a future common-checkpoint redo
 * payload. It is memory-only in this slice and does not publish CURRENT or write a native Store.
 */
public final class PathStateSnapshotDelta {

  private static final Comparator<Mutation> MUTATION_ORDER =
      (left, right) -> compareUnsigned(left.key, right.key);

  private final BlockSnapshotMeta meta;
  private final byte[] parentStateRoot;
  private final byte[] stateRoot;
  private final byte[] transitionPayloadDigest;
  private final List<StoreDelta> stores;
  private final List<Mutation> superNodeMutations;

  private PathStateSnapshotDelta(BlockSnapshotMeta meta, byte[] parentStateRoot,
      byte[] stateRoot, byte[] transitionPayloadDigest,
      List<StoreDelta> stores, List<Mutation> superNodeMutations) {
    this.meta = Objects.requireNonNull(meta, "meta");
    this.parentStateRoot = root(parentStateRoot, "parentStateRoot");
    this.stateRoot = root(stateRoot, "stateRoot");
    this.transitionPayloadDigest = root(transitionPayloadDigest, "transitionPayloadDigest");
    this.stores = Collections.unmodifiableList(new ArrayList<>(stores));
    this.superNodeMutations = immutableMutations(superNodeMutations);
  }

  static PathStateSnapshotDelta from(BlockSnapshotMeta meta,
      PreparedPathStateTransition prepared, PathStateParticipantScope scope) {
    BlockSnapshotMeta admittedMeta = Objects.requireNonNull(meta, "meta");
    PreparedPathStateTransition candidate = Objects.requireNonNull(prepared, "prepared");
    PathStateBlockTransition transition = candidate.getTransition();
    requireSameBlock(admittedMeta, transition);
    PathStateParticipantScope admittedScope = Objects.requireNonNull(scope, "scope");

    Map<Integer, StoreBuilder> builders = new LinkedHashMap<>();
    for (PathStateMutation mutation : transition.getMutations()) {
      PathStateParticipant participant = admittedScope.require(mutation.getDbName());
      StoreBuilder builder = builders.computeIfAbsent(participant.getStoreId(),
          ignored -> new StoreBuilder(participant));
      byte[] secureKey = PathStateCommitmentCodec.storeLeafKey(participant.getStoreId(),
          mutation.getPhysicalKey());
      byte[] encodedValue = mutation.isDelete() ? null
          : PathStateCommitmentCodec.presentLeafValue(mutation.getPhysicalValue());
      builder.flatMutations.add(new Mutation(secureKey, encodedValue));
    }

    List<Mutation> superMutations = new ArrayList<>();
    for (PreparedPathStateTransition.NodeMutation mutation : candidate.getNodeMutations()) {
      Mutation forward = new Mutation(mutation.getPath(), mutation.getEncodedNode());
      if (mutation.getStoreId() == 0) {
        superMutations.add(forward);
      } else {
        PathStateParticipant participant = participant(admittedScope, mutation.getStoreId());
        builders.computeIfAbsent(participant.getStoreId(),
            ignored -> new StoreBuilder(participant)).nodeMutations.add(forward);
      }
    }

    List<StoreDelta> deltas = new ArrayList<>();
    for (PathStateParticipant participant : admittedScope.getParticipants()) {
      StoreBuilder builder = builders.get(participant.getStoreId());
      if (builder != null) {
        deltas.add(builder.freeze(candidate.getSnapshot().participantRoot(
            participant.getDbName())));
      }
    }
    return new PathStateSnapshotDelta(admittedMeta, candidate.getParent().getStateRoot(),
        candidate.getStateRoot(), transition.getPayloadDigest(), deltas, superMutations);
  }

  static PathStateSnapshotDelta fromPhysical(BlockSnapshotMeta meta,
      PathStateRootMetadata parent, PathStateBlockTransition transition,
      PathStateRoot.Snapshot snapshot, List<StoreDelta> stores,
      List<Mutation> superNodeMutations) {
    BlockSnapshotMeta admittedMeta = Objects.requireNonNull(meta, "meta");
    PathStateRootMetadata admittedParent = Objects.requireNonNull(parent, "parent");
    PathStateBlockTransition admittedTransition = Objects.requireNonNull(transition,
        "transition");
    PathStateRoot.Snapshot admittedSnapshot = Objects.requireNonNull(snapshot, "snapshot");
    requireSameBlock(admittedMeta, admittedTransition);
    return new PathStateSnapshotDelta(admittedMeta, admittedParent.getStateRoot(),
        admittedSnapshot.getStateRoot(), admittedTransition.getPayloadDigest(), stores,
        superNodeMutations);
  }

  public BlockSnapshotMeta getMeta() {
    return meta;
  }

  public byte[] getParentStateRoot() {
    return Arrays.copyOf(parentStateRoot, parentStateRoot.length);
  }

  public byte[] getStateRoot() {
    return Arrays.copyOf(stateRoot, stateRoot.length);
  }

  public byte[] getTransitionPayloadDigest() {
    return Arrays.copyOf(transitionPayloadDigest, transitionPayloadDigest.length);
  }

  public List<StoreDelta> getStores() {
    return stores;
  }

  public List<Mutation> getSuperNodeMutations() {
    return superNodeMutations;
  }

  private static void requireSameBlock(BlockSnapshotMeta meta,
      PathStateBlockTransition transition) {
    if (meta.getBlockNumber() != transition.getBlockNumber()
        || !Arrays.equals(meta.getBlockHash(), transition.getBlockHash())
        || !Arrays.equals(meta.getParentHash(), transition.getParentHash())
        || meta.getTimestamp() != transition.getTimestamp()) {
      throw new IllegalArgumentException(
          "path-state delta differs from Snapshot block identity");
    }
  }

  private static PathStateParticipant participant(PathStateParticipantScope scope, int storeId) {
    for (PathStateParticipant candidate : scope.getParticipants()) {
      if (candidate.getStoreId() == storeId) {
        return candidate;
      }
    }
    throw new IllegalArgumentException("unknown path-state Store ID: " + storeId);
  }

  private static byte[] root(byte[] value, String name) {
    byte[] copy = Arrays.copyOf(Objects.requireNonNull(value, name), value.length);
    if (copy.length != PathStateCommitmentCodec.ROOT_LENGTH) {
      throw new IllegalArgumentException(name + " must contain exactly 32 bytes");
    }
    return copy;
  }

  private static List<Mutation> immutableMutations(List<Mutation> supplied) {
    List<Mutation> copy = new ArrayList<>(Objects.requireNonNull(supplied, "mutations"));
    copy.sort(MUTATION_ORDER);
    for (int index = 1; index < copy.size(); index++) {
      if (Arrays.equals(copy.get(index - 1).key, copy.get(index).key)) {
        throw new IllegalArgumentException("duplicate path-state forward mutation key");
      }
    }
    return Collections.unmodifiableList(copy);
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

  public static final class StoreDelta {

    private final int storeId;
    private final String dbName;
    private final byte[] storeRoot;
    private final List<Mutation> flatMutations;
    private final List<Mutation> nodeMutations;

    StoreDelta(PathStateParticipant participant, byte[] storeRoot,
        List<Mutation> flatMutations, List<Mutation> nodeMutations) {
      this.storeId = participant.getStoreId();
      this.dbName = participant.getDbName();
      this.storeRoot = root(storeRoot, "storeRoot");
      this.flatMutations = immutableMutations(flatMutations);
      this.nodeMutations = immutableMutations(nodeMutations);
      if (this.flatMutations.isEmpty()) {
        throw new IllegalArgumentException("changed path-state Store has no flat mutations");
      }
    }

    public int getStoreId() {
      return storeId;
    }

    public String getDbName() {
      return dbName;
    }

    public byte[] getStoreRoot() {
      return Arrays.copyOf(storeRoot, storeRoot.length);
    }

    public List<Mutation> getFlatMutations() {
      return flatMutations;
    }

    public List<Mutation> getNodeMutations() {
      return nodeMutations;
    }
  }

  public static final class Mutation {

    private final byte[] key;
    private final byte[] value;

    Mutation(byte[] key, byte[] value) {
      this.key = Arrays.copyOf(Objects.requireNonNull(key, "key"), key.length);
      this.value = value == null ? null : Arrays.copyOf(value, value.length);
    }

    public byte[] getKey() {
      return Arrays.copyOf(key, key.length);
    }

    public byte[] getValue() {
      return value == null ? null : Arrays.copyOf(value, value.length);
    }

    public boolean isDelete() {
      return value == null;
    }
  }

  private static final class StoreBuilder {

    private final PathStateParticipant participant;
    private final List<Mutation> flatMutations = new ArrayList<>();
    private final List<Mutation> nodeMutations = new ArrayList<>();

    private StoreBuilder(PathStateParticipant participant) {
      this.participant = participant;
    }

    private StoreDelta freeze(byte[] storeRoot) {
      return new StoreDelta(participant, storeRoot, flatMutations, nodeMutations);
    }
  }
}
