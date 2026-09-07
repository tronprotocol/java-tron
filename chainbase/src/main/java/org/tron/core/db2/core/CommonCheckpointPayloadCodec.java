package org.tron.core.db2.core;

import com.google.common.hash.Hashing;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.tron.core.db2.archive.BlockHistoryCodec;
import org.tron.core.db2.archive.BlockReverseDiff;
import org.tron.core.db2.archive.BlockSnapshotMeta;
import org.tron.core.db2.archive.StateArchiveHotBatchDescriptor;
import org.tron.core.db2.archive.StateArchiveHotBatchDescriptor.BlockDigest;
import org.tron.core.db2.core.CommonCheckpointPayload.BlockPayload;
import org.tron.core.db2.core.CommonCheckpointPayload.Mutation;
import org.tron.core.db2.core.CommonCheckpointPayload.PathStoreTarget;
import org.tron.core.db2.core.CommonCheckpointPayload.StoreMutations;
import org.tron.core.db2.stateroot.PathStateStoreManifest.Engine;

/** Deterministic, bounded codec for v1 redo bodies and v2 digest-only Archive coordination. */
public final class CommonCheckpointPayloadCodec {

  public static final int MAGIC = 0x54434350; // TCCP
  public static final short VERSION = 1;
  public static final short COORDINATION_VERSION = 2;
  public static final int HEADER_LENGTH = 44;
  public static final int DEFAULT_MAX_ENCODED_LENGTH = 256 * 1024 * 1024;
  private static final int DIGEST_LENGTH = 32;
  private static final int MAX_BLOCKS = 100_000;
  private static final int MAX_STORES = 1024;
  private static final int MAX_MUTATIONS = 1_000_000;
  private static final int MAX_NAME_LENGTH = 256;
  private static final int MAX_FIELD_LENGTH = 64 * 1024 * 1024;

  private final int maxEncodedLength;
  private final BlockHistoryCodec historyCodec = new BlockHistoryCodec();

  public CommonCheckpointPayloadCodec() {
    this(DEFAULT_MAX_ENCODED_LENGTH);
  }

  public CommonCheckpointPayloadCodec(int maxEncodedLength) {
    if (maxEncodedLength <= HEADER_LENGTH) {
      throw new IllegalArgumentException("common checkpoint maximum length is too small");
    }
    this.maxEncodedLength = maxEncodedLength;
  }

  public byte[] encode(CommonCheckpointPayload payload) {
    try {
      byte[] body = encodeBody(payload);
      checkEncodedLength(HEADER_LENGTH + (long) body.length);
      ByteArrayOutputStream bytes = new ByteArrayOutputStream(HEADER_LENGTH + body.length);
      DataOutputStream output = new DataOutputStream(bytes);
      output.writeInt(MAGIC);
      output.writeShort(payload.getVersion());
      output.writeShort(0);
      output.writeInt(body.length);
      output.write(Hashing.sha256().hashBytes(body).asBytes());
      output.write(body);
      output.flush();
      return bytes.toByteArray();
    } catch (IOException impossible) {
      throw new IllegalStateException("in-memory common checkpoint encoding failed", impossible);
    }
  }

  public CommonCheckpointPayload decode(byte[] encoded) {
    if (encoded == null || encoded.length < HEADER_LENGTH) {
      throw new IllegalArgumentException("common checkpoint payload is truncated");
    }
    checkEncodedLength(encoded.length);
    try {
      DataInputStream input = new DataInputStream(new ByteArrayInputStream(encoded));
      if (input.readInt() != MAGIC) {
        throw new IllegalArgumentException("invalid common checkpoint magic");
      }
      short version = input.readShort();
      if (version != VERSION && version != COORDINATION_VERSION) {
        throw new IllegalArgumentException("unsupported common checkpoint version");
      }
      if (input.readShort() != 0) {
        throw new IllegalArgumentException("unsupported common checkpoint flags");
      }
      int bodyLength = input.readInt();
      byte[] expectedDigest = readExact(input, DIGEST_LENGTH);
      if (bodyLength < 0 || HEADER_LENGTH + (long) bodyLength != encoded.length) {
        throw new IllegalArgumentException("invalid common checkpoint body length");
      }
      byte[] body = readExact(input, bodyLength);
      if (!Arrays.equals(expectedDigest, Hashing.sha256().hashBytes(body).asBytes())) {
        throw new IllegalArgumentException("common checkpoint payload checksum mismatch");
      }
      return version == VERSION ? decodeBodyV1(body) : decodeBodyV2(body);
    } catch (EOFException truncated) {
      throw new IllegalArgumentException("common checkpoint payload is truncated", truncated);
    } catch (IOException invalid) {
      throw new IllegalArgumentException("invalid common checkpoint payload", invalid);
    }
  }

  public byte[] digest(CommonCheckpointPayload payload) {
    return Hashing.sha256().hashBytes(encode(payload)).asBytes();
  }

  private byte[] encodeBody(CommonCheckpointPayload payload) throws IOException {
    CommonCheckpointPayload admitted = java.util.Objects.requireNonNull(payload, "payload");
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    DataOutputStream output = new DataOutputStream(bytes);
    output.write(admitted.getFormatIdentity());
    output.write(admitted.getParentStateRoot());
    output.write(admitted.getStateRoot());
    output.writeInt(admitted.getBlocks().size());
    for (BlockPayload block : admitted.getBlocks()) {
      writeMeta(output, block.getMeta());
      output.write(block.getParentStateRoot());
      output.write(block.getStateRoot());
      output.write(block.getTransitionPayloadDigest());
      output.write(block.getMutationViewDigest());
      if (admitted.getVersion() == CommonCheckpointPayload.FORMAT_VERSION) {
        writeBytes(output, historyCodec.encode(block.getArchiveDiff()));
      } else {
        output.write(block.getArchiveRecordDigest());
      }
    }
    if (admitted.getVersion() == CommonCheckpointPayload.COORDINATION_FORMAT_VERSION) {
      writeArchiveBinding(output, admitted.getArchiveBinding());
    }
    writeStores(output, admitted.getChainbaseStores());
    output.writeInt(admitted.getPathStores().size());
    for (PathStoreTarget store : admitted.getPathStores()) {
      output.writeInt(store.getStoreId());
      writeName(output, store.getDbName());
      output.write(store.getStoreRoot());
      writeMutations(output, store.getFlatMutations());
      writeMutations(output, store.getNodeMutations());
    }
    writeMutations(output, admitted.getSuperNodeMutations());
    output.flush();
    return bytes.toByteArray();
  }

  private CommonCheckpointPayload decodeBodyV1(byte[] body) throws IOException {
    DataInputStream input = new DataInputStream(new ByteArrayInputStream(body));
    byte[] formatIdentity = readExact(input, DIGEST_LENGTH);
    byte[] parentStateRoot = readExact(input, DIGEST_LENGTH);
    byte[] stateRoot = readExact(input, DIGEST_LENGTH);
    int blockCount = readCount(input, MAX_BLOCKS, "block");
    List<BlockPayload> blocks = new ArrayList<>(blockCount);
    for (int index = 0; index < blockCount; index++) {
      BlockSnapshotMeta meta = readMeta(input);
      byte[] parentRoot = readExact(input, DIGEST_LENGTH);
      byte[] blockRoot = readExact(input, DIGEST_LENGTH);
      byte[] transitionDigest = readExact(input, DIGEST_LENGTH);
      byte[] viewDigest = readExact(input, DIGEST_LENGTH);
      BlockReverseDiff decoded = historyCodec.decode(readBytes(input));
      BlockReverseDiff archive = new BlockReverseDiff(decoded.getMeta(), decoded.getGroups(),
          viewDigest);
      blocks.add(new BlockPayload(meta, parentRoot, blockRoot, transitionDigest, viewDigest,
          archive));
    }
    List<StoreMutations> chainbase = readStores(input);
    int pathStoreCount = readCount(input, MAX_STORES, "path-state Store");
    List<PathStoreTarget> pathStores = new ArrayList<>(pathStoreCount);
    for (int index = 0; index < pathStoreCount; index++) {
      int storeId = input.readInt();
      String dbName = readName(input);
      byte[] storeRoot = readExact(input, DIGEST_LENGTH);
      pathStores.add(new PathStoreTarget(storeId, dbName, storeRoot,
          readMutations(input), readMutations(input)));
    }
    List<Mutation> superNodes = readMutations(input);
    if (input.available() != 0) {
      throw new IllegalArgumentException("common checkpoint payload has trailing bytes");
    }
    return CommonCheckpointPayload.restore(formatIdentity, blocks, parentStateRoot, stateRoot,
        chainbase, pathStores, superNodes);
  }

  private CommonCheckpointPayload decodeBodyV2(byte[] body) throws IOException {
    DataInputStream input = new DataInputStream(new ByteArrayInputStream(body));
    byte[] formatIdentity = readExact(input, DIGEST_LENGTH);
    byte[] parentStateRoot = readExact(input, DIGEST_LENGTH);
    byte[] stateRoot = readExact(input, DIGEST_LENGTH);
    int blockCount = readCount(input, MAX_BLOCKS, "block");
    List<BlockPayload> blocks = new ArrayList<>(blockCount);
    List<BlockDigest> archiveBlocks = new ArrayList<>(blockCount);
    for (int index = 0; index < blockCount; index++) {
      BlockSnapshotMeta meta = readMeta(input);
      byte[] parentRoot = readExact(input, DIGEST_LENGTH);
      byte[] blockRoot = readExact(input, DIGEST_LENGTH);
      byte[] transitionDigest = readExact(input, DIGEST_LENGTH);
      byte[] viewDigest = readExact(input, DIGEST_LENGTH);
      byte[] recordDigest = readExact(input, DIGEST_LENGTH);
      blocks.add(BlockPayload.coordination(meta, parentRoot, blockRoot, transitionDigest,
          viewDigest, recordDigest));
      archiveBlocks.add(BlockDigest.restore(meta, viewDigest, recordDigest));
    }
    StateArchiveHotBatchDescriptor archiveBinding = readArchiveBinding(input, archiveBlocks);
    List<StoreMutations> chainbase = readStores(input);
    int pathStoreCount = readCount(input, MAX_STORES, "path-state Store");
    List<PathStoreTarget> pathStores = new ArrayList<>(pathStoreCount);
    for (int index = 0; index < pathStoreCount; index++) {
      int storeId = input.readInt();
      String dbName = readName(input);
      byte[] storeRoot = readExact(input, DIGEST_LENGTH);
      pathStores.add(new PathStoreTarget(storeId, dbName, storeRoot,
          readMutations(input), readMutations(input)));
    }
    List<Mutation> superNodes = readMutations(input);
    if (input.available() != 0) {
      throw new IllegalArgumentException("common checkpoint payload has trailing bytes");
    }
    return CommonCheckpointPayload.restoreV2(formatIdentity, blocks, parentStateRoot, stateRoot,
        chainbase, pathStores, superNodes, archiveBinding);
  }

  private static void writeArchiveBinding(DataOutputStream output,
      StateArchiveHotBatchDescriptor binding) throws IOException {
    output.writeShort(StateArchiveHotBatchDescriptor.HOT_FORMAT_VERSION);
    output.writeShort(engineTag(binding.getEngine()));
    output.writeLong(binding.getParentPublishedBlock());
    output.write(binding.getParentPublishedHash());
    writeMeta(output, binding.getFirstBlock());
    writeMeta(output, binding.getLastBlock());
    output.writeLong(binding.getBlockCount());
    output.writeLong(binding.getEncodedBytes());
    output.write(binding.getParentContentDigest());
    output.write(binding.getResultContentDigest());
    output.write(binding.getOrderedRecordDigest());
    output.write(binding.getMutationViewRangeDigest());
  }

  private static StateArchiveHotBatchDescriptor readArchiveBinding(DataInputStream input,
      List<BlockDigest> blocks) throws IOException {
    if (input.readUnsignedShort() != StateArchiveHotBatchDescriptor.HOT_FORMAT_VERSION) {
      throw new IllegalArgumentException("unsupported Hot Archive binding version");
    }
    Engine engine = engine(input.readUnsignedShort());
    long parentBlock = input.readLong();
    byte[] parentHash = readExact(input, DIGEST_LENGTH);
    BlockSnapshotMeta first = readMeta(input);
    BlockSnapshotMeta last = readMeta(input);
    long blockCount = input.readLong();
    long encodedBytes = input.readLong();
    byte[] parentContent = readExact(input, DIGEST_LENGTH);
    byte[] resultContent = readExact(input, DIGEST_LENGTH);
    byte[] orderedRecords = readExact(input, DIGEST_LENGTH);
    byte[] mutationViews = readExact(input, DIGEST_LENGTH);
    if (blockCount != blocks.size()) {
      throw new IllegalArgumentException("Hot Archive binding block count differs");
    }
    return StateArchiveHotBatchDescriptor.restore(engine, parentBlock, parentHash, first, last,
        encodedBytes, parentContent, resultContent, orderedRecords, mutationViews, blocks);
  }

  private static int engineTag(Engine engine) {
    return engine == Engine.LEVELDB ? 1 : 2;
  }

  private static Engine engine(int tag) {
    if (tag == 1) {
      return Engine.LEVELDB;
    }
    if (tag == 2) {
      return Engine.ROCKSDB;
    }
    throw new IllegalArgumentException("unsupported Hot Archive engine tag");
  }

  private void writeStores(DataOutputStream output, List<StoreMutations> stores)
      throws IOException {
    output.writeInt(stores.size());
    for (StoreMutations store : stores) {
      writeName(output, store.getDbName());
      writeMutations(output, store.getMutations());
    }
  }

  private List<StoreMutations> readStores(DataInputStream input) throws IOException {
    int count = readCount(input, MAX_STORES, "Chainbase Store");
    List<StoreMutations> stores = new ArrayList<>(count);
    for (int index = 0; index < count; index++) {
      stores.add(new StoreMutations(readName(input), readMutations(input)));
    }
    return stores;
  }

  private void writeMutations(DataOutputStream output, List<Mutation> mutations)
      throws IOException {
    output.writeInt(mutations.size());
    for (Mutation mutation : mutations) {
      writeBytes(output, mutation.getKey());
      byte[] value = mutation.getValue();
      output.writeInt(value == null ? -1 : value.length);
      if (value != null) {
        output.write(value);
      }
    }
  }

  private List<Mutation> readMutations(DataInputStream input) throws IOException {
    int count = readCount(input, MAX_MUTATIONS, "mutation");
    List<Mutation> mutations = new ArrayList<>(count);
    for (int index = 0; index < count; index++) {
      byte[] key = readBytes(input);
      int valueLength = input.readInt();
      if (valueLength < -1 || valueLength > MAX_FIELD_LENGTH) {
        throw new IllegalArgumentException("invalid checkpoint mutation value length");
      }
      mutations.add(new Mutation(key,
          valueLength < 0 ? null : readExact(input, valueLength)));
    }
    return mutations;
  }

  private static void writeMeta(DataOutputStream output, BlockSnapshotMeta meta)
      throws IOException {
    output.writeLong(meta.getEpoch());
    output.writeLong(meta.getBlockNumber());
    output.write(meta.getBlockHash());
    output.write(meta.getParentHash());
    output.writeLong(meta.getTimestamp());
  }

  private static BlockSnapshotMeta readMeta(DataInputStream input) throws IOException {
    return new BlockSnapshotMeta(input.readLong(), input.readLong(),
        readExact(input, DIGEST_LENGTH), readExact(input, DIGEST_LENGTH), input.readLong());
  }

  private static void writeName(DataOutputStream output, String value) throws IOException {
    byte[] encoded = value.getBytes(StandardCharsets.UTF_8);
    if (encoded.length == 0 || encoded.length > MAX_NAME_LENGTH) {
      throw new IllegalArgumentException("invalid common checkpoint Store name length");
    }
    output.writeInt(encoded.length);
    output.write(encoded);
  }

  private static String readName(DataInputStream input) throws IOException {
    int length = input.readInt();
    if (length <= 0 || length > MAX_NAME_LENGTH) {
      throw new IllegalArgumentException("invalid common checkpoint Store name length");
    }
    return new String(readExact(input, length), StandardCharsets.UTF_8);
  }

  private static void writeBytes(DataOutputStream output, byte[] value) throws IOException {
    if (value.length > MAX_FIELD_LENGTH) {
      throw new IllegalArgumentException("common checkpoint field exceeds maximum length");
    }
    output.writeInt(value.length);
    output.write(value);
  }

  private static byte[] readBytes(DataInputStream input) throws IOException {
    int length = input.readInt();
    if (length < 0 || length > MAX_FIELD_LENGTH) {
      throw new IllegalArgumentException("invalid common checkpoint field length");
    }
    return readExact(input, length);
  }

  private static int readCount(DataInputStream input, int maximum, String name)
      throws IOException {
    int count = input.readInt();
    if (count < 0 || count > maximum) {
      throw new IllegalArgumentException("invalid common checkpoint " + name + " count");
    }
    return count;
  }

  private static byte[] readExact(DataInputStream input, int length) throws IOException {
    byte[] value = new byte[length];
    input.readFully(value);
    return value;
  }

  private void checkEncodedLength(long length) {
    if (length > maxEncodedLength) {
      throw new IllegalArgumentException("common checkpoint exceeds maximum encoded length");
    }
  }
}
