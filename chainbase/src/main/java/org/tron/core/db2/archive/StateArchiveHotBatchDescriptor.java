package org.tron.core.db2.archive;

import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.tron.core.db2.stateroot.PathStateStoreManifest.Engine;

/** Immutable logical binding between transient Archive diffs and one Hot DB prepare batch. */
public final class StateArchiveHotBatchDescriptor {

  public static final int HOT_FORMAT_VERSION = 2;
  private static final int DIGEST_LENGTH = 32;

  private final Engine engine;
  private final long parentPublishedBlock;
  private final byte[] parentPublishedHash;
  private final BlockSnapshotMeta firstBlock;
  private final BlockSnapshotMeta lastBlock;
  private final long encodedBytes;
  private final byte[] parentContentDigest;
  private final byte[] resultContentDigest;
  private final byte[] orderedRecordDigest;
  private final List<BlockDigest> blocks;

  StateArchiveHotBatchDescriptor(Engine engine, long parentPublishedBlock,
      byte[] parentPublishedHash, BlockSnapshotMeta firstBlock, BlockSnapshotMeta lastBlock,
      long encodedBytes, byte[] parentContentDigest, byte[] resultContentDigest,
      byte[] orderedRecordDigest, List<BlockDigest> blocks) {
    this.engine = Objects.requireNonNull(engine, "engine");
    if (parentPublishedBlock < 0 || encodedBytes <= 0) {
      throw new IllegalArgumentException("Hot Archive batch counters are invalid");
    }
    this.parentPublishedBlock = parentPublishedBlock;
    this.parentPublishedHash = digest(parentPublishedHash, "parentPublishedHash");
    this.firstBlock = Objects.requireNonNull(firstBlock, "firstBlock");
    this.lastBlock = Objects.requireNonNull(lastBlock, "lastBlock");
    this.encodedBytes = encodedBytes;
    this.parentContentDigest = digest(parentContentDigest, "parentContentDigest");
    this.resultContentDigest = digest(resultContentDigest, "resultContentDigest");
    this.orderedRecordDigest = digest(orderedRecordDigest, "orderedRecordDigest");
    List<BlockDigest> admitted = new ArrayList<>(Objects.requireNonNull(blocks, "blocks"));
    if (admitted.isEmpty() || admitted.size() != lastBlock.getBlockNumber()
        - firstBlock.getBlockNumber() + 1L
        || firstBlock.getBlockNumber() != parentPublishedBlock + 1
        || !firstBlock.equals(admitted.get(0).meta)
        || !lastBlock.equals(admitted.get(admitted.size() - 1).meta)) {
      throw new IllegalArgumentException("Hot Archive batch block range is invalid");
    }
    BlockDigest previous = null;
    Hasher orderedRecords = Hashing.sha256().newHasher();
    for (BlockDigest block : admitted) {
      BlockDigest current = Objects.requireNonNull(block, "block");
      if (current.meta.getEpoch() != current.meta.getBlockNumber()) {
        throw new IllegalArgumentException("Hot Archive batch requires block epochs");
      } else if (previous == null) {
        if (!Arrays.equals(current.meta.getParentHash(), this.parentPublishedHash)) {
          throw new IllegalArgumentException("Hot Archive batch parent hash differs");
        }
      } else if (current.meta.getBlockNumber() != previous.meta.getBlockNumber() + 1
          || !Arrays.equals(current.meta.getParentHash(), previous.meta.getBlockHash())) {
        throw new IllegalArgumentException("Hot Archive batch block chain is not consecutive");
      }
      orderedRecords.putLong(current.meta.getBlockNumber())
          .putBytes(current.archiveRecordDigest);
      previous = current;
    }
    if (!Arrays.equals(this.orderedRecordDigest, orderedRecords.hash().asBytes())) {
      throw new IllegalArgumentException("Hot Archive batch aggregate digest differs");
    }
    this.blocks = Collections.unmodifiableList(admitted);
  }

  /** Reconstructs and validates a descriptor decoded from a coordination payload. */
  public static StateArchiveHotBatchDescriptor restore(Engine engine,
      long parentPublishedBlock, byte[] parentPublishedHash, BlockSnapshotMeta firstBlock,
      BlockSnapshotMeta lastBlock, long encodedBytes, byte[] parentContentDigest,
      byte[] resultContentDigest, byte[] orderedRecordDigest, List<BlockDigest> blocks) {
    return new StateArchiveHotBatchDescriptor(engine, parentPublishedBlock,
        parentPublishedHash, firstBlock, lastBlock, encodedBytes, parentContentDigest,
        resultContentDigest, orderedRecordDigest, blocks);
  }

  public Engine getEngine() {
    return engine;
  }

  public long getParentPublishedBlock() {
    return parentPublishedBlock;
  }

  public byte[] getParentPublishedHash() {
    return copy(parentPublishedHash);
  }

  public BlockSnapshotMeta getFirstBlock() {
    return firstBlock;
  }

  public BlockSnapshotMeta getLastBlock() {
    return lastBlock;
  }

  public long getBlockCount() {
    return blocks.size();
  }

  public long getEncodedBytes() {
    return encodedBytes;
  }

  public byte[] getParentContentDigest() {
    return copy(parentContentDigest);
  }

  public byte[] getResultContentDigest() {
    return copy(resultContentDigest);
  }

  public byte[] getOrderedRecordDigest() {
    return copy(orderedRecordDigest);
  }

  public List<BlockDigest> getBlocks() {
    return blocks;
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (!(object instanceof StateArchiveHotBatchDescriptor)) {
      return false;
    }
    StateArchiveHotBatchDescriptor that = (StateArchiveHotBatchDescriptor) object;
    return engine == that.engine
        && parentPublishedBlock == that.parentPublishedBlock
        && encodedBytes == that.encodedBytes
        && firstBlock.equals(that.firstBlock)
        && lastBlock.equals(that.lastBlock)
        && blocks.equals(that.blocks)
        && Arrays.equals(parentPublishedHash, that.parentPublishedHash)
        && Arrays.equals(parentContentDigest, that.parentContentDigest)
        && Arrays.equals(resultContentDigest, that.resultContentDigest)
        && Arrays.equals(orderedRecordDigest, that.orderedRecordDigest);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(engine, parentPublishedBlock, firstBlock, lastBlock, encodedBytes,
        blocks);
    result = 31 * result + Arrays.hashCode(parentPublishedHash);
    result = 31 * result + Arrays.hashCode(parentContentDigest);
    result = 31 * result + Arrays.hashCode(resultContentDigest);
    result = 31 * result + Arrays.hashCode(orderedRecordDigest);
    return result;
  }

  private static byte[] digest(byte[] value, String name) {
    byte[] admitted = copy(Objects.requireNonNull(value, name));
    if (admitted.length != DIGEST_LENGTH) {
      throw new IllegalArgumentException(name + " must contain exactly 32 bytes");
    }
    return admitted;
  }

  private static byte[] copy(byte[] value) {
    return Arrays.copyOf(value, value.length);
  }

  /** Digest-only per-block Archive identity retained by the coordination payload. */
  public static final class BlockDigest {
    private final BlockSnapshotMeta meta;
    private final byte[] archiveRecordDigest;

    BlockDigest(BlockSnapshotMeta meta, byte[] archiveRecordDigest) {
      this.meta = Objects.requireNonNull(meta, "meta");
      this.archiveRecordDigest = digest(archiveRecordDigest, "archiveRecordDigest");
    }

    public static BlockDigest restore(BlockSnapshotMeta meta, byte[] archiveRecordDigest) {
      return new BlockDigest(meta, archiveRecordDigest);
    }

    public BlockSnapshotMeta getMeta() {
      return meta;
    }

    public byte[] getArchiveRecordDigest() {
      return copy(archiveRecordDigest);
    }

    @Override
    public boolean equals(Object object) {
      if (!(object instanceof BlockDigest)) {
        return false;
      }
      BlockDigest that = (BlockDigest) object;
      return meta.equals(that.meta)
          && Arrays.equals(archiveRecordDigest, that.archiveRecordDigest);
    }

    @Override
    public int hashCode() {
      int result = meta.hashCode();
      result = 31 * result + Arrays.hashCode(archiveRecordDigest);
      return result;
    }
  }
}
