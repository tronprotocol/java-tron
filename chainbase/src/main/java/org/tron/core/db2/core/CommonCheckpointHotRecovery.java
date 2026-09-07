package org.tron.core.db2.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import org.tron.core.db2.archive.BlockSnapshotMeta;

/** Selects one externally verified Hot DB recovery ceiling before common-checkpoint redo. */
public final class CommonCheckpointHotRecovery {

  private static final byte[] DYNAMIC_BLOCK_NUMBER =
      "latest_block_header_number".getBytes(StandardCharsets.UTF_8);
  private static final byte[] DYNAMIC_BLOCK_HASH =
      "latest_block_header_hash".getBytes(StandardCharsets.UTF_8);
  private static final String DYNAMIC_STORE = "properties";
  private static final int HASH_LENGTH = 32;

  private final CommonCheckpointFile checkpointFile;
  private final PersistentDynamicHeadSource dynamicHeadSource;
  private final BlockMetaSource blockMetaSource;
  private final HotTailReconciler reconciler;

  public CommonCheckpointHotRecovery(CommonCheckpointFile checkpointFile,
      PersistentDynamicHeadSource dynamicHeadSource, BlockMetaSource blockMetaSource,
      HotTailReconciler reconciler) {
    this.checkpointFile = Objects.requireNonNull(checkpointFile, "checkpointFile");
    this.dynamicHeadSource = Objects.requireNonNull(dynamicHeadSource, "dynamicHeadSource");
    this.blockMetaSource = Objects.requireNonNull(blockMetaSource, "blockMetaSource");
    this.reconciler = Objects.requireNonNull(reconciler, "reconciler");
  }

  /**
   * Reconciles Hot PREPARED data before common redo. A WAL target is authoritative even when its
   * Block Store write has not happened yet; without a WAL, the persisted dynamic head must already
   * match an exact Block Store record.
   */
  public Result reconcileBeforeCommonRedo() throws IOException {
    CommonCheckpointPayload payload = checkpointFile.loadIfPresent();
    BlockSnapshotMeta authority;
    Source source;
    if (payload != null) {
      if (payload.getVersion() != CommonCheckpointPayload.COORDINATION_FORMAT_VERSION) {
        throw new IOException("Hot Archive recovery requires common checkpoint payload v2");
      }
      authority = CommonCheckpointTarget.from(payload).getLastBlock();
      requireWalDynamicIdentity(payload, authority);
      BlockSnapshotMeta stored = blockMetaSource.loadIfPresent(authority.getBlockNumber());
      if (stored != null && !authority.equals(stored)) {
        throw new IOException("common checkpoint WAL and Block Store identity differ");
      }
      source = Source.COMMON_WAL;
    } else {
      PersistentDynamicHead dynamic = Objects.requireNonNull(dynamicHeadSource.load(),
          "persistent dynamic head source returned null");
      BlockSnapshotMeta stored = blockMetaSource.loadIfPresent(dynamic.getBlockNumber());
      if (stored == null) {
        throw new IOException("persisted dynamic head is missing from Block Store");
      }
      requireNumberAndHash(dynamic.getBlockNumber(), dynamic.getBlockHash(), stored,
          "persisted dynamic head and Block Store identity differ");
      authority = stored;
      source = Source.PERSISTED_DYNAMIC;
    }
    return new Result(source, authority, reconciler.reconcile(authority));
  }

  static void requireWalDynamicIdentity(CommonCheckpointPayload payload,
      BlockSnapshotMeta target) throws IOException {
    CommonCheckpointPayload.StoreMutations dynamic = null;
    for (CommonCheckpointPayload.StoreMutations store : payload.getChainbaseStores()) {
      if (DYNAMIC_STORE.equals(store.getDbName())) {
        dynamic = store;
        break;
      }
    }
    if (dynamic == null) {
      throw new IOException("common checkpoint WAL has no dynamic Store mutations");
    }
    byte[] encodedNumber = null;
    byte[] encodedHash = null;
    for (CommonCheckpointPayload.Mutation mutation : dynamic.getMutations()) {
      if (Arrays.equals(DYNAMIC_BLOCK_NUMBER, mutation.getKey())) {
        encodedNumber = mutation.getValue();
      } else if (Arrays.equals(DYNAMIC_BLOCK_HASH, mutation.getKey())) {
        encodedHash = mutation.getValue();
      }
    }
    if (encodedNumber == null || encodedNumber.length != Long.BYTES
        || encodedHash == null || encodedHash.length != HASH_LENGTH) {
      throw new IOException("common checkpoint WAL dynamic head is incomplete");
    }
    long number = ByteBuffer.wrap(encodedNumber).getLong();
    requireNumberAndHash(number, encodedHash, target,
        "common checkpoint WAL dynamic head and target differ");
  }

  private static void requireNumberAndHash(long number, byte[] hash, BlockSnapshotMeta meta,
      String message) throws IOException {
    if (number < 0 || number != meta.getBlockNumber()
        || !Arrays.equals(hash, meta.getBlockHash())) {
      throw new IOException(message);
    }
  }

  public enum Source {
    COMMON_WAL,
    PERSISTED_DYNAMIC
  }

  public static final class PersistentDynamicHead {

    private final long blockNumber;
    private final byte[] blockHash;

    public PersistentDynamicHead(long blockNumber, byte[] blockHash) {
      if (blockNumber < 0) {
        throw new IllegalArgumentException("persistent dynamic block number must not be negative");
      }
      this.blockNumber = blockNumber;
      this.blockHash = copyHash(blockHash);
    }

    public long getBlockNumber() {
      return blockNumber;
    }

    public byte[] getBlockHash() {
      return Arrays.copyOf(blockHash, blockHash.length);
    }
  }

  public static final class Result {

    private final Source source;
    private final BlockSnapshotMeta authority;
    private final long removedBlocks;

    private Result(Source source, BlockSnapshotMeta authority, long removedBlocks) {
      this.source = source;
      this.authority = authority;
      this.removedBlocks = removedBlocks;
    }

    public Source getSource() {
      return source;
    }

    public BlockSnapshotMeta getAuthority() {
      return authority;
    }

    public long getRemovedBlocks() {
      return removedBlocks;
    }
  }

  @FunctionalInterface
  public interface PersistentDynamicHeadSource {
    PersistentDynamicHead load() throws IOException;
  }

  @FunctionalInterface
  public interface BlockMetaSource {
    BlockSnapshotMeta loadIfPresent(long blockNumber) throws IOException;
  }

  @FunctionalInterface
  public interface HotTailReconciler {
    long reconcile(BlockSnapshotMeta authority) throws IOException;
  }

  private static byte[] copyHash(byte[] value) {
    byte[] copy = Arrays.copyOf(Objects.requireNonNull(value, "blockHash"), value.length);
    if (copy.length != HASH_LENGTH) {
      throw new IllegalArgumentException("blockHash must contain exactly 32 bytes");
    }
    return copy;
  }
}
