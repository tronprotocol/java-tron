package org.tron.core.db2.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.tron.core.db2.archive.BlockReverseDiff;
import org.tron.core.db2.archive.BlockSnapshotMeta;
import org.tron.core.db2.archive.StateArchiveHotBatchDescriptor;
import org.tron.core.db2.stateroot.PathStateFlushTarget;
import org.tron.core.db2.stateroot.PathStateSnapshotDelta;

/** Versioned immutable redo or coordination input for one cross-authority checkpoint. */
public final class CommonCheckpointPayload {

  public static final int FORMAT_VERSION = 1;
  public static final int COORDINATION_FORMAT_VERSION = 2;
  private static final int DIGEST_LENGTH = 32;
  private static final Comparator<Mutation> MUTATION_ORDER =
      (left, right) -> compareUnsigned(left.key, right.key);

  private final int version;
  private final byte[] formatIdentity;
  private final List<BlockPayload> blocks;
  private final byte[] parentStateRoot;
  private final byte[] stateRoot;
  private final List<StoreMutations> chainbaseStores;
  private final List<PathStoreTarget> pathStores;
  private final List<Mutation> superNodeMutations;
  private final StateArchiveHotBatchDescriptor archiveBinding;

  private CommonCheckpointPayload(int version, byte[] formatIdentity, List<BlockPayload> blocks,
      byte[] parentStateRoot, byte[] stateRoot, List<StoreMutations> chainbaseStores,
      List<PathStoreTarget> pathStores, List<Mutation> superNodeMutations,
      StateArchiveHotBatchDescriptor archiveBinding) {
    if (version != FORMAT_VERSION && version != COORDINATION_FORMAT_VERSION) {
      throw new IllegalArgumentException("unsupported common checkpoint payload version");
    }
    this.version = version;
    this.formatIdentity = digest(formatIdentity, "formatIdentity");
    if (blocks.isEmpty()) {
      throw new IllegalArgumentException("common checkpoint must contain at least one block");
    }
    this.parentStateRoot = digest(parentStateRoot, "parentStateRoot");
    this.stateRoot = digest(stateRoot, "stateRoot");
    List<BlockPayload> admittedBlocks = new ArrayList<>(blocks);
    validateBlocks(version, admittedBlocks, this.parentStateRoot, this.stateRoot,
        archiveBinding);
    this.blocks = Collections.unmodifiableList(admittedBlocks);
    this.chainbaseStores = immutableStores(chainbaseStores);
    this.pathStores = immutablePathStores(pathStores);
    this.superNodeMutations = immutableMutations(superNodeMutations);
    this.archiveBinding = archiveBinding;
  }

  public static CommonCheckpointPayload create(byte[] formatIdentity,
      PathStateFlushTarget pathState, List<BlockReverseDiff> archiveBlocks,
      List<StoreMutations> chainbaseStores) {
    PathStateFlushTarget path = Objects.requireNonNull(pathState, "pathState");
    List<BlockReverseDiff> archives = new ArrayList<>(Objects.requireNonNull(archiveBlocks,
        "archiveBlocks"));
    if (path.getBlocks().size() != archives.size()) {
      throw new IllegalArgumentException("common checkpoint block payload count differs");
    }
    List<BlockPayload> blocks = new ArrayList<>();
    for (int index = 0; index < archives.size(); index++) {
      PathStateFlushTarget.BlockBinding binding = path.getBlocks().get(index);
      BlockReverseDiff archive = Objects.requireNonNull(archives.get(index), "archiveBlock");
      if (!binding.getMeta().equals(archive.getMeta())
          || archive.getMutationViewDigest() == null
          || !Arrays.equals(binding.getMutationViewDigest(),
              archive.getMutationViewDigest())) {
        throw new IllegalArgumentException(
            "common checkpoint Archive and PathState block identity differs");
      }
      blocks.add(new BlockPayload(binding.getMeta(), binding.getParentStateRoot(),
          binding.getStateRoot(), binding.getTransitionPayloadDigest(),
          binding.getMutationViewDigest(), archive));
    }
    List<PathStoreTarget> pathStores = new ArrayList<>();
    for (PathStateFlushTarget.StoreTarget store : path.getStores()) {
      pathStores.add(new PathStoreTarget(store.getStoreId(), store.getDbName(),
          store.getStoreRoot(), mutations(store.getFlatMutations()),
          mutations(store.getNodeMutations())));
    }
    return new CommonCheckpointPayload(FORMAT_VERSION, formatIdentity, blocks,
        path.getParentStateRoot(),
        path.getStateRoot(), chainbaseStores, pathStores,
        mutations(path.getSuperNodeMutations()), null);
  }

  /** Builds a v2 coordination payload whose Archive section contains only Hot DB digests. */
  public static CommonCheckpointPayload createV2(byte[] formatIdentity,
      PathStateFlushTarget pathState, StateArchiveHotBatchDescriptor archiveBinding,
      List<StoreMutations> chainbaseStores) {
    PathStateFlushTarget path = Objects.requireNonNull(pathState, "pathState");
    StateArchiveHotBatchDescriptor archive = Objects.requireNonNull(archiveBinding,
        "archiveBinding");
    if (path.getBlocks().size() != archive.getBlocks().size()) {
      throw new IllegalArgumentException("common checkpoint block payload count differs");
    }
    List<BlockPayload> blocks = new ArrayList<>();
    for (int index = 0; index < path.getBlocks().size(); index++) {
      PathStateFlushTarget.BlockBinding binding = path.getBlocks().get(index);
      StateArchiveHotBatchDescriptor.BlockDigest digest = archive.getBlocks().get(index);
      if (!binding.getMeta().equals(digest.getMeta())
          || !Arrays.equals(binding.getMutationViewDigest(), digest.getMutationViewDigest())) {
        throw new IllegalArgumentException(
            "common checkpoint Hot Archive and PathState block identity differs");
      }
      blocks.add(BlockPayload.coordination(binding.getMeta(), binding.getParentStateRoot(),
          binding.getStateRoot(), binding.getTransitionPayloadDigest(),
          binding.getMutationViewDigest(), digest.getArchiveRecordDigest()));
    }
    List<PathStoreTarget> pathStores = new ArrayList<>();
    for (PathStateFlushTarget.StoreTarget store : path.getStores()) {
      pathStores.add(new PathStoreTarget(store.getStoreId(), store.getDbName(),
          store.getStoreRoot(), mutations(store.getFlatMutations()),
          mutations(store.getNodeMutations())));
    }
    return new CommonCheckpointPayload(COORDINATION_FORMAT_VERSION, formatIdentity, blocks,
        path.getParentStateRoot(), path.getStateRoot(), chainbaseStores, pathStores,
        mutations(path.getSuperNodeMutations()), archive);
  }

  static CommonCheckpointPayload coordinateV2(CommonCheckpointPayload capturedV1,
      StateArchiveHotBatchDescriptor archiveBinding) {
    CommonCheckpointPayload source = Objects.requireNonNull(capturedV1, "capturedV1");
    if (source.version != FORMAT_VERSION) {
      throw new IllegalArgumentException("coordination conversion requires a v1 capture");
    }
    StateArchiveHotBatchDescriptor archive = Objects.requireNonNull(archiveBinding,
        "archiveBinding");
    if (source.blocks.size() != archive.getBlocks().size()) {
      throw new IllegalArgumentException("common checkpoint block payload count differs");
    }
    List<BlockPayload> blocks = new ArrayList<>();
    for (int index = 0; index < source.blocks.size(); index++) {
      BlockPayload block = source.blocks.get(index);
      StateArchiveHotBatchDescriptor.BlockDigest digest = archive.getBlocks().get(index);
      if (!block.meta.equals(digest.getMeta())
          || !Arrays.equals(block.mutationViewDigest, digest.getMutationViewDigest())) {
        throw new IllegalArgumentException(
            "common checkpoint Hot Archive and captured block identity differs");
      }
      blocks.add(BlockPayload.coordination(block.meta, block.parentStateRoot, block.stateRoot,
          block.transitionPayloadDigest, block.mutationViewDigest,
          digest.getArchiveRecordDigest()));
    }
    return new CommonCheckpointPayload(COORDINATION_FORMAT_VERSION, source.formatIdentity,
        blocks, source.parentStateRoot, source.stateRoot, source.chainbaseStores,
        source.pathStores, source.superNodeMutations, archive);
  }

  static CommonCheckpointPayload restore(byte[] formatIdentity, List<BlockPayload> blocks,
      byte[] parentStateRoot, byte[] stateRoot, List<StoreMutations> chainbaseStores,
      List<PathStoreTarget> pathStores, List<Mutation> superNodeMutations) {
    return new CommonCheckpointPayload(FORMAT_VERSION, formatIdentity, blocks, parentStateRoot,
        stateRoot, chainbaseStores, pathStores, superNodeMutations, null);
  }

  static CommonCheckpointPayload restoreV2(byte[] formatIdentity, List<BlockPayload> blocks,
      byte[] parentStateRoot, byte[] stateRoot, List<StoreMutations> chainbaseStores,
      List<PathStoreTarget> pathStores, List<Mutation> superNodeMutations,
      StateArchiveHotBatchDescriptor archiveBinding) {
    return new CommonCheckpointPayload(COORDINATION_FORMAT_VERSION, formatIdentity, blocks,
        parentStateRoot, stateRoot, chainbaseStores, pathStores, superNodeMutations,
        archiveBinding);
  }

  public int getVersion() {
    return version;
  }

  public byte[] getFormatIdentity() {
    return copy(formatIdentity);
  }

  public List<BlockPayload> getBlocks() {
    return blocks;
  }

  public byte[] getParentStateRoot() {
    return copy(parentStateRoot);
  }

  public byte[] getStateRoot() {
    return copy(stateRoot);
  }

  public List<StoreMutations> getChainbaseStores() {
    return chainbaseStores;
  }

  public List<PathStoreTarget> getPathStores() {
    return pathStores;
  }

  public List<Mutation> getSuperNodeMutations() {
    return superNodeMutations;
  }

  public StateArchiveHotBatchDescriptor getArchiveBinding() {
    if (archiveBinding == null) {
      throw new IllegalStateException("v1 common checkpoint has no Hot Archive binding");
    }
    return archiveBinding;
  }

  private static List<StoreMutations> immutableStores(List<StoreMutations> supplied) {
    List<StoreMutations> stores = new ArrayList<>(Objects.requireNonNull(supplied,
        "chainbaseStores"));
    stores.sort(Comparator.comparing(StoreMutations::getDbName));
    requireUniqueStoreNames(stores);
    return Collections.unmodifiableList(stores);
  }

  private static void validateBlocks(int version, List<BlockPayload> blocks,
      byte[] parentStateRoot, byte[] stateRoot,
      StateArchiveHotBatchDescriptor archiveBinding) {
    BlockPayload previous = null;
    for (BlockPayload block : blocks) {
      BlockPayload current = Objects.requireNonNull(block, "block");
      if (version == FORMAT_VERSION) {
        byte[] archiveView = current.requireArchiveDiff().getMutationViewDigest();
        if (archiveView == null || !Arrays.equals(archiveView, current.mutationViewDigest)) {
          throw new IllegalArgumentException("checkpoint block mutation-view identity differs");
        }
      } else if (current.archiveDiff != null || current.archiveRecordDigest == null) {
        throw new IllegalArgumentException("coordination checkpoint contains Archive body");
      }
      if (previous != null
          && (current.meta.getEpoch() != previous.meta.getEpoch() + 1
          || current.meta.getBlockNumber() != previous.meta.getBlockNumber() + 1
          || !Arrays.equals(current.meta.getParentHash(), previous.meta.getBlockHash())
          || !Arrays.equals(current.parentStateRoot, previous.stateRoot))) {
        throw new IllegalArgumentException("common checkpoint block chain is not consecutive");
      }
      previous = current;
    }
    if (!Arrays.equals(blocks.get(0).parentStateRoot, parentStateRoot)
        || !Arrays.equals(blocks.get(blocks.size() - 1).stateRoot, stateRoot)) {
      throw new IllegalArgumentException("common checkpoint target root range differs");
    }
    if (version == COORDINATION_FORMAT_VERSION
        && (archiveBinding == null
        || !blocks.get(0).meta.equals(archiveBinding.getFirstBlock())
        || !blocks.get(blocks.size() - 1).meta.equals(archiveBinding.getLastBlock()))) {
      throw new IllegalArgumentException("common checkpoint Archive binding range differs");
    }
    if (version == COORDINATION_FORMAT_VERSION) {
      List<StateArchiveHotBatchDescriptor.BlockDigest> archiveBlocks =
          archiveBinding.getBlocks();
      if (blocks.size() != archiveBlocks.size()) {
        throw new IllegalArgumentException("common checkpoint Archive binding count differs");
      }
      for (int index = 0; index < blocks.size(); index++) {
        BlockPayload block = blocks.get(index);
        StateArchiveHotBatchDescriptor.BlockDigest archive = archiveBlocks.get(index);
        if (!block.meta.equals(archive.getMeta())
            || !Arrays.equals(block.mutationViewDigest, archive.getMutationViewDigest())
            || !Arrays.equals(block.archiveRecordDigest, archive.getArchiveRecordDigest())) {
          throw new IllegalArgumentException("common checkpoint Archive block binding differs");
        }
      }
    }
  }

  private static List<PathStoreTarget> immutablePathStores(List<PathStoreTarget> supplied) {
    List<PathStoreTarget> stores = new ArrayList<>(Objects.requireNonNull(supplied,
        "pathStores"));
    stores.sort(Comparator.comparingInt(PathStoreTarget::getStoreId));
    for (int index = 1; index < stores.size(); index++) {
      if (stores.get(index - 1).storeId == stores.get(index).storeId) {
        throw new IllegalArgumentException("duplicate checkpoint path-state Store ID");
      }
    }
    return Collections.unmodifiableList(stores);
  }

  private static void requireUniqueStoreNames(List<StoreMutations> stores) {
    for (int index = 1; index < stores.size(); index++) {
      if (stores.get(index - 1).dbName.equals(stores.get(index).dbName)) {
        throw new IllegalArgumentException("duplicate checkpoint Chainbase Store");
      }
    }
  }

  private static List<Mutation> mutations(List<PathStateSnapshotDelta.Mutation> supplied) {
    List<Mutation> result = new ArrayList<>();
    for (PathStateSnapshotDelta.Mutation mutation : supplied) {
      result.add(new Mutation(mutation.getKey(), mutation.getValue()));
    }
    return result;
  }

  private static List<Mutation> immutableMutations(List<Mutation> supplied) {
    List<Mutation> mutations = new ArrayList<>(Objects.requireNonNull(supplied, "mutations"));
    mutations.sort(MUTATION_ORDER);
    for (int index = 1; index < mutations.size(); index++) {
      if (Arrays.equals(mutations.get(index - 1).key, mutations.get(index).key)) {
        throw new IllegalArgumentException("duplicate checkpoint mutation key");
      }
    }
    return Collections.unmodifiableList(mutations);
  }

  private static byte[] digest(byte[] value, String name) {
    byte[] copy = copy(Objects.requireNonNull(value, name));
    if (copy.length != DIGEST_LENGTH) {
      throw new IllegalArgumentException(name + " must contain exactly 32 bytes");
    }
    return copy;
  }

  private static byte[] copy(byte[] value) {
    return Arrays.copyOf(value, value.length);
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

  public static final class BlockPayload {

    private final BlockSnapshotMeta meta;
    private final byte[] parentStateRoot;
    private final byte[] stateRoot;
    private final byte[] transitionPayloadDigest;
    private final byte[] mutationViewDigest;
    private final BlockReverseDiff archiveDiff;
    private final byte[] archiveRecordDigest;

    BlockPayload(BlockSnapshotMeta meta, byte[] parentStateRoot, byte[] stateRoot,
        byte[] transitionPayloadDigest, byte[] mutationViewDigest,
        BlockReverseDiff archiveDiff) {
      this.meta = Objects.requireNonNull(meta, "meta");
      this.parentStateRoot = digest(parentStateRoot, "parentStateRoot");
      this.stateRoot = digest(stateRoot, "stateRoot");
      this.transitionPayloadDigest = digest(transitionPayloadDigest,
          "transitionPayloadDigest");
      this.mutationViewDigest = digest(mutationViewDigest, "mutationViewDigest");
      this.archiveDiff = Objects.requireNonNull(archiveDiff, "archiveDiff");
      this.archiveRecordDigest = null;
      if (!meta.equals(archiveDiff.getMeta())) {
        throw new IllegalArgumentException("checkpoint Archive block metadata differs");
      }
    }

    private BlockPayload(BlockSnapshotMeta meta, byte[] parentStateRoot, byte[] stateRoot,
        byte[] transitionPayloadDigest, byte[] mutationViewDigest,
        byte[] archiveRecordDigest) {
      this.meta = Objects.requireNonNull(meta, "meta");
      this.parentStateRoot = digest(parentStateRoot, "parentStateRoot");
      this.stateRoot = digest(stateRoot, "stateRoot");
      this.transitionPayloadDigest = digest(transitionPayloadDigest,
          "transitionPayloadDigest");
      this.mutationViewDigest = digest(mutationViewDigest, "mutationViewDigest");
      this.archiveDiff = null;
      this.archiveRecordDigest = digest(archiveRecordDigest, "archiveRecordDigest");
    }

    static BlockPayload coordination(BlockSnapshotMeta meta, byte[] parentStateRoot,
        byte[] stateRoot, byte[] transitionPayloadDigest, byte[] mutationViewDigest,
        byte[] archiveRecordDigest) {
      return new BlockPayload(meta, parentStateRoot, stateRoot, transitionPayloadDigest,
          mutationViewDigest, archiveRecordDigest);
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

    public byte[] getMutationViewDigest() {
      return copy(mutationViewDigest);
    }

    public BlockReverseDiff getArchiveDiff() {
      return requireArchiveDiff();
    }

    public byte[] getArchiveRecordDigest() {
      if (archiveRecordDigest == null) {
        throw new IllegalStateException("v1 common checkpoint has no Archive record digest");
      }
      return copy(archiveRecordDigest);
    }

    private BlockReverseDiff requireArchiveDiff() {
      if (archiveDiff == null) {
        throw new IllegalStateException("v2 coordination checkpoint has no Archive body");
      }
      return archiveDiff;
    }
  }

  public static class StoreMutations {

    private final String dbName;
    private final List<Mutation> mutations;

    public StoreMutations(String dbName, List<Mutation> mutations) {
      this.dbName = Objects.requireNonNull(dbName, "dbName");
      if (dbName.isEmpty()) {
        throw new IllegalArgumentException("checkpoint dbName must not be empty");
      }
      this.mutations = immutableMutations(mutations);
    }

    public String getDbName() {
      return dbName;
    }

    public List<Mutation> getMutations() {
      return mutations;
    }
  }

  public static final class PathStoreTarget extends StoreMutations {

    private final int storeId;
    private final byte[] storeRoot;
    private final List<Mutation> nodeMutations;

    PathStoreTarget(int storeId, String dbName, byte[] storeRoot,
        List<Mutation> flatMutations, List<Mutation> nodeMutations) {
      super(dbName, flatMutations);
      if (storeId <= 0) {
        throw new IllegalArgumentException("checkpoint path-state Store ID must be positive");
      }
      this.storeId = storeId;
      this.storeRoot = digest(storeRoot, "storeRoot");
      this.nodeMutations = immutableMutations(nodeMutations);
    }

    public int getStoreId() {
      return storeId;
    }

    public byte[] getStoreRoot() {
      return copy(storeRoot);
    }

    public List<Mutation> getFlatMutations() {
      return getMutations();
    }

    public List<Mutation> getNodeMutations() {
      return nodeMutations;
    }
  }

  public static final class Mutation {

    private final byte[] key;
    private final byte[] value;

    public Mutation(byte[] key, byte[] value) {
      this.key = copy(Objects.requireNonNull(key, "key"));
      this.value = value == null ? null : copy(value);
    }

    public byte[] getKey() {
      return copy(key);
    }

    public byte[] getValue() {
      return value == null ? null : copy(value);
    }

    public boolean isDelete() {
      return value == null;
    }
  }
}
