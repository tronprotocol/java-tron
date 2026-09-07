package org.tron.core.db2.archive;

import com.google.common.hash.Hashing;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.tron.core.db2.archive.StateArchiveHotBatchDescriptor.BlockDigest;
import org.tron.core.db2.stateroot.PathStateStoreManifest.Engine;

/** Checksummed persistent encoding for one exact Hot Archive batch descriptor. */
final class StateArchiveHotBatchDescriptorCodec {

  private static final int MAGIC = 0x53414844; // SAHD
  private static final short VERSION = 1;
  private static final int DIGEST_LENGTH = 32;
  private static final int HEADER_LENGTH = 44;
  private static final int MAX_BLOCKS = 100_000;
  private static final int MAX_ENCODED_LENGTH = 32 * 1024 * 1024;

  byte[] encode(StateArchiveHotBatchDescriptor descriptor) {
    try {
      ByteArrayOutputStream bodyBytes = new ByteArrayOutputStream();
      DataOutputStream body = new DataOutputStream(bodyBytes);
      body.writeShort(StateArchiveHotBatchDescriptor.HOT_FORMAT_VERSION);
      body.writeShort(engineTag(descriptor.getEngine()));
      body.writeLong(descriptor.getParentPublishedBlock());
      body.write(descriptor.getParentPublishedHash());
      writeMeta(body, descriptor.getFirstBlock());
      writeMeta(body, descriptor.getLastBlock());
      body.writeLong(descriptor.getBlockCount());
      body.writeLong(descriptor.getEncodedBytes());
      body.write(descriptor.getParentContentDigest());
      body.write(descriptor.getResultContentDigest());
      body.write(descriptor.getOrderedRecordDigest());
      body.write(descriptor.getMutationViewRangeDigest());
      for (BlockDigest block : descriptor.getBlocks()) {
        writeMeta(body, block.getMeta());
        body.write(block.getMutationViewDigest());
        body.write(block.getArchiveRecordDigest());
      }
      body.flush();
      byte[] payload = bodyBytes.toByteArray();
      ByteArrayOutputStream encodedBytes = new ByteArrayOutputStream(HEADER_LENGTH
          + payload.length);
      DataOutputStream encoded = new DataOutputStream(encodedBytes);
      encoded.writeInt(MAGIC);
      encoded.writeShort(VERSION);
      encoded.writeShort(0);
      encoded.writeInt(payload.length);
      encoded.write(Hashing.sha256().hashBytes(payload).asBytes());
      encoded.write(payload);
      encoded.flush();
      return encodedBytes.toByteArray();
    } catch (IOException impossible) {
      throw new IllegalStateException("in-memory Hot descriptor encoding failed", impossible);
    }
  }

  StateArchiveHotBatchDescriptor decode(byte[] supplied) throws IOException {
    if (supplied == null || supplied.length < HEADER_LENGTH
        || supplied.length > MAX_ENCODED_LENGTH) {
      throw new ArchivePersistenceException("Hot Archive descriptor length is invalid");
    }
    byte[] encoded = Arrays.copyOf(supplied, supplied.length);
    try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(encoded))) {
      if (input.readInt() != MAGIC || input.readShort() != VERSION || input.readShort() != 0) {
        throw new ArchivePersistenceException("Hot Archive descriptor format is unsupported");
      }
      int bodyLength = input.readInt();
      byte[] checksum = readExact(input, DIGEST_LENGTH);
      if (bodyLength < 0 || HEADER_LENGTH + (long) bodyLength != encoded.length) {
        throw new ArchivePersistenceException("Hot Archive descriptor length is invalid");
      }
      byte[] body = readExact(input, bodyLength);
      if (!Arrays.equals(checksum, Hashing.sha256().hashBytes(body).asBytes())) {
        throw new ArchivePersistenceException("Hot Archive descriptor checksum differs");
      }
      return decodeBody(body);
    } catch (EOFException truncated) {
      throw new ArchivePersistenceException("Hot Archive descriptor is truncated", truncated);
    }
  }

  private StateArchiveHotBatchDescriptor decodeBody(byte[] body) throws IOException {
    try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(body))) {
      if (input.readUnsignedShort() != StateArchiveHotBatchDescriptor.HOT_FORMAT_VERSION) {
        throw new ArchivePersistenceException("Hot Archive descriptor version differs");
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
      if (blockCount <= 0 || blockCount > MAX_BLOCKS) {
        throw new ArchivePersistenceException("Hot Archive descriptor block count is invalid");
      }
      List<BlockDigest> blocks = new ArrayList<>((int) blockCount);
      for (long index = 0; index < blockCount; index++) {
        BlockSnapshotMeta meta = readMeta(input);
        blocks.add(BlockDigest.restore(meta, readExact(input, DIGEST_LENGTH),
            readExact(input, DIGEST_LENGTH)));
      }
      if (input.available() != 0) {
        throw new ArchivePersistenceException("Hot Archive descriptor has trailing bytes");
      }
      try {
        return StateArchiveHotBatchDescriptor.restore(engine, parentBlock, parentHash, first,
            last, encodedBytes, parentContent, resultContent, orderedRecords, mutationViews,
            blocks);
      } catch (IllegalArgumentException invalid) {
        throw new ArchivePersistenceException("Hot Archive descriptor is inconsistent", invalid);
      }
    } catch (EOFException truncated) {
      throw new ArchivePersistenceException("Hot Archive descriptor is truncated", truncated);
    }
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

  private static byte[] readExact(DataInputStream input, int length) throws IOException {
    byte[] value = new byte[length];
    input.readFully(value);
    return value;
  }

  private static int engineTag(Engine engine) {
    return engine == Engine.LEVELDB ? 1 : 2;
  }

  private static Engine engine(int tag) throws IOException {
    if (tag == 1) {
      return Engine.LEVELDB;
    }
    if (tag == 2) {
      return Engine.ROCKSDB;
    }
    throw new ArchivePersistenceException("Hot Archive descriptor engine is unsupported");
  }
}
