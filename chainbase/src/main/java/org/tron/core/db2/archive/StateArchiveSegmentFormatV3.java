package org.tron.core.db2.archive;

import com.google.common.hash.Hashing;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;

/** Byte-exact codecs for the default-off State Archive v3 size-rolled segment metadata. */
public final class StateArchiveSegmentFormatV3 {

  private static final int HEADER_DIGEST_OFFSET = 476;
  private static final int HEADER_CRC_OFFSET = 508;
  private static final int SEAL_DIGEST_OFFSET = 320;
  private static final int SEAL_CRC_OFFSET = 360;
  private static final int MARKER_DIGEST_OFFSET = 288;
  private static final int MARKER_CRC_OFFSET = 328;
  private static final int BLOCK_INDEX_HEADER_DIGEST_OFFSET = 92;
  private static final int BLOCK_INDEX_HEADER_CRC_OFFSET = 124;

  private StateArchiveSegmentFormatV3() {
  }

  /** Rotation is decided before the next frame and never considers that frame's length. */
  public static boolean shouldRotate(long blockFrameCount, long dataEndOffset,
      long segmentTargetBytes) {
    if (blockFrameCount < 0 || dataEndOffset < StateArchiveFileFormatV3.PART_HEADER_LENGTH
        || segmentTargetBytes <= StateArchiveFileFormatV3.PART_HEADER_LENGTH) {
      throw new IllegalArgumentException("Invalid State Archive segment rotation state");
    }
    return blockFrameCount > 0 && dataEndOffset >= segmentTargetBytes;
  }

  public static byte[] encodeHeader(SegmentHeader header) {
    Objects.requireNonNull(header, "header");
    header.validate();
    ByteBuffer bytes = ByteBuffer.allocate(StateArchiveFileFormatV3.PART_HEADER_LENGTH);
    bytes.putInt(StateArchiveFileFormatV3.SEGMENT_MAGIC);
    bytes.putShort(StateArchiveFileFormatV3.MAJOR_VERSION);
    bytes.putShort(StateArchiveFileFormatV3.MINOR_VERSION);
    bytes.putInt(StateArchiveFileFormatV3.PART_HEADER_LENGTH);
    bytes.putShort(StateArchiveFileFormatV3.laneKind(header.laneId));
    bytes.putShort((short) header.laneId);
    bytes.putShort(StateArchiveFileFormatV3.SEGMENT_LAYOUT_ID);
    bytes.putShort((short) 0);
    bytes.putLong(header.segmentSeq);
    bytes.putLong(header.actualFirstBlock);
    bytes.putLong(header.actualFirstBlock);
    bytes.putLong(StateArchiveFileFormatV3.SEGMENT_TARGET_BYTES);
    bytes.putLong(StateArchiveFileFormatV3.MAX_BLOCK_FRAME_BYTES);
    bytes.put(StateArchiveFileFormatV3.fiveLaneDescriptorDigest());
    bytes.put(StateArchiveFileFormatV3.compositeFormatDigest());
    bytes.put(header.previousSegmentDigest);
    bytes.put(header.previousHistoryDigest);
    bytes.putShort(StateArchiveFileFormatV3.laneBodyCodec(header.laneId));
    bytes.putShort(header.compressionId);
    bytes.putShort(StateArchiveFileFormatV3.KEY_ORDER_ID);
    bytes.putShort(StateArchiveFileFormatV3.DIGEST_ID);
    bytes.putShort(StateArchiveFileFormatV3.CHECKSUM_ID);
    bytes.putShort(StateArchiveFileFormatV3.IDENTITY_KIND);
    bytes.put(new byte[276]);
    if (bytes.position() != HEADER_DIGEST_OFFSET) {
      throw new IllegalStateException("Invalid State Archive segment header layout");
    }
    bytes.put(StateArchiveFileFormatV3.sha256(StateArchiveFileFormatV3.SEGMENT_HEADER_DOMAIN,
        Arrays.copyOf(bytes.array(), HEADER_DIGEST_OFFSET)));
    bytes.putInt(crc32c(bytes.array(), 0, HEADER_CRC_OFFSET));
    if (bytes.hasRemaining()) {
      throw new IllegalStateException("Invalid State Archive segment header length");
    }
    return bytes.array();
  }

  public static SegmentHeader decodeHeader(byte[] encoded) {
    requireLength(encoded, StateArchiveFileFormatV3.PART_HEADER_LENGTH, "segment header");
    ByteBuffer bytes = ByteBuffer.wrap(encoded);
    requireInt(bytes, StateArchiveFileFormatV3.SEGMENT_MAGIC, "segment magic");
    requireShort(bytes, StateArchiveFileFormatV3.MAJOR_VERSION, "major version");
    requireShort(bytes, StateArchiveFileFormatV3.MINOR_VERSION, "minor version");
    requireInt(bytes, StateArchiveFileFormatV3.PART_HEADER_LENGTH, "header length");
    short laneKind = bytes.getShort();
    int laneId = Short.toUnsignedInt(bytes.getShort());
    if (laneKind != StateArchiveFileFormatV3.laneKind(laneId)) {
      throw new IllegalArgumentException("State Archive segment lane kind mismatch");
    }
    requireShort(bytes, StateArchiveFileFormatV3.SEGMENT_LAYOUT_ID, "segment layout");
    requireShort(bytes, (short) 0, "segment flags");
    long segmentSeq = requireNonNegative(bytes.getLong(), "segment sequence");
    long firstBlock = requireNonNegative(bytes.getLong(), "first block");
    if (bytes.getLong() != firstBlock) {
      throw new IllegalArgumentException("State Archive segment first epoch mismatch");
    }
    if (bytes.getLong() != StateArchiveFileFormatV3.SEGMENT_TARGET_BYTES
        || bytes.getLong() != StateArchiveFileFormatV3.MAX_BLOCK_FRAME_BYTES) {
      throw new IllegalArgumentException("State Archive segment size policy mismatch");
    }
    requireArray(getBytes(bytes, 32), StateArchiveFileFormatV3.fiveLaneDescriptorDigest(),
        "placement descriptor");
    requireArray(getBytes(bytes, 32), StateArchiveFileFormatV3.compositeFormatDigest(),
        "composite format");
    byte[] previousSegmentDigest = getBytes(bytes, 32);
    byte[] previousHistoryDigest = getBytes(bytes, 32);
    requireShort(bytes, StateArchiveFileFormatV3.laneBodyCodec(laneId), "body codec");
    short compressionId = bytes.getShort();
    requireCompression(compressionId);
    requireShort(bytes, StateArchiveFileFormatV3.KEY_ORDER_ID, "key order");
    requireShort(bytes, StateArchiveFileFormatV3.DIGEST_ID, "digest algorithm");
    requireShort(bytes, StateArchiveFileFormatV3.CHECKSUM_ID, "checksum algorithm");
    requireShort(bytes, StateArchiveFileFormatV3.IDENTITY_KIND, "identity kind");
    requireZero(bytes, 276, "segment reserved bytes");
    byte[] headerDigest = getBytes(bytes, 32);
    requireArray(headerDigest, StateArchiveFileFormatV3.sha256(
        StateArchiveFileFormatV3.SEGMENT_HEADER_DOMAIN,
        Arrays.copyOf(encoded, HEADER_DIGEST_OFFSET)), "segment header digest");
    int expectedCrc = bytes.getInt();
    if (expectedCrc != crc32c(encoded, 0, HEADER_CRC_OFFSET)) {
      throw new IllegalArgumentException("State Archive segment header checksum mismatch");
    }
    return new SegmentHeader(laneId, segmentSeq, firstBlock, previousSegmentDigest,
        previousHistoryDigest, compressionId, headerDigest);
  }

  /** Returns the deterministic predecessor anchor for segment sequence zero of one lane. */
  public static byte[] laneBaselineDigest(int laneId) {
    StateArchiveFileFormatV3.laneKind(laneId);
    return StateArchiveFileFormatV3.sha256(
        StateArchiveFileFormatV3.LANE_SEGMENT_BASELINE_DOMAIN,
        StateArchiveFileFormatV3.compositeFormatDigest(),
        StateArchiveFileFormatV3.fiveLaneDescriptorDigest(),
        ByteBuffer.allocate(Short.BYTES).putShort((short) laneId).array());
  }

  /** Computes the link stored as previousSegmentDigest in the next segment header. */
  public static byte[] segmentChainDigest(int laneId, long segmentSeq,
      byte[] segmentHeaderDigest, byte[] segmentContentDigest, byte[] sealFrameDigest) {
    StateArchiveFileFormatV3.laneKind(laneId);
    requireNonNegative(segmentSeq, "segment sequence");
    byte[] identity = ByteBuffer.allocate(Short.BYTES + Long.BYTES)
        .putShort((short) laneId).putLong(segmentSeq).array();
    return StateArchiveFileFormatV3.sha256(StateArchiveFileFormatV3.SEGMENT_CHAIN_DOMAIN,
        identity,
        requireHash(segmentHeaderDigest, "segment header digest"),
        requireHash(segmentContentDigest, "segment content digest"),
        requireHash(sealFrameDigest, "seal frame digest"));
  }

  public static byte[] encodeSeal(SegmentSeal seal) {
    Objects.requireNonNull(seal, "seal");
    seal.validate();
    int totalLength = StateArchiveFileFormatV3.SEAL_HEADER_LENGTH
        + StateArchiveFileFormatV3.FRAME_TRAILER_LENGTH;
    ByteBuffer bytes = ByteBuffer.allocate(totalLength);
    bytes.putInt(StateArchiveFileFormatV3.FRAME_MAGIC);
    bytes.putShort(StateArchiveFileFormatV3.MAJOR_VERSION);
    bytes.putShort(StateArchiveFileFormatV3.MINOR_VERSION);
    bytes.putShort(StateArchiveFileFormatV3.PART_SEAL_FRAME_TYPE);
    bytes.putShort((short) 0);
    bytes.putInt(StateArchiveFileFormatV3.SEAL_HEADER_LENGTH);
    bytes.putLong(totalLength);
    bytes.putLong(0);
    bytes.putShort((short) seal.laneId);
    bytes.putShort((short) 0);
    bytes.putLong(seal.segmentSeq);
    bytes.putLong(seal.actualFirstBlock);
    bytes.putLong(seal.actualLastBlock);
    bytes.putLong(seal.blockFrameCount);
    bytes.putLong(seal.entryCount);
    bytes.putLong(seal.logicalPayloadBytes);
    bytes.putLong(seal.encodedBlockFrameBytes);
    bytes.putLong(seal.dataEndOffset);
    bytes.putLong(seal.physicalFileBytes);
    bytes.put(StateArchiveFileFormatV3.fiveLaneDescriptorDigest());
    bytes.put(seal.firstBlockFrameDigest);
    bytes.put(seal.lastBlockFrameDigest);
    bytes.put(seal.startHistoryDigest);
    bytes.put(seal.endHistoryDigest);
    bytes.put(seal.segmentContentDigest);
    bytes.put(new byte[20]);
    if (bytes.position() != SEAL_DIGEST_OFFSET) {
      throw new IllegalStateException("Invalid State Archive seal header layout");
    }
    byte[] encodedFrameDigest = StateArchiveFileFormatV3.sha256(
        StateArchiveFileFormatV3.ENCODED_SEGMENT_SEAL_DOMAIN,
        Arrays.copyOf(bytes.array(), SEAL_DIGEST_OFFSET));
    bytes.put(encodedFrameDigest);
    bytes.putLong(totalLength);
    bytes.putInt(crc32c(bytes.array(), 0, SEAL_CRC_OFFSET));
    bytes.putInt(StateArchiveFileFormatV3.FRAME_TRAILER_MAGIC);
    if (bytes.hasRemaining()) {
      throw new IllegalStateException("Invalid State Archive seal frame length");
    }
    return bytes.array();
  }

  public static SegmentSeal decodeSeal(byte[] encoded) {
    int totalLength = StateArchiveFileFormatV3.SEAL_HEADER_LENGTH
        + StateArchiveFileFormatV3.FRAME_TRAILER_LENGTH;
    requireLength(encoded, totalLength, "seal frame");
    ByteBuffer bytes = ByteBuffer.wrap(encoded);
    requireInt(bytes, StateArchiveFileFormatV3.FRAME_MAGIC, "seal magic");
    requireShort(bytes, StateArchiveFileFormatV3.MAJOR_VERSION, "seal major version");
    requireShort(bytes, StateArchiveFileFormatV3.MINOR_VERSION, "seal minor version");
    requireShort(bytes, StateArchiveFileFormatV3.PART_SEAL_FRAME_TYPE, "seal frame type");
    requireShort(bytes, (short) 0, "seal flags");
    requireInt(bytes, StateArchiveFileFormatV3.SEAL_HEADER_LENGTH, "seal header length");
    requireLong(bytes, totalLength, "seal total length");
    requireLong(bytes, 0, "seal payload length");
    int laneId = Short.toUnsignedInt(bytes.getShort());
    StateArchiveFileFormatV3.laneKind(laneId);
    requireShort(bytes, (short) 0, "seal reserved field");
    long segmentSeq = requireNonNegative(bytes.getLong(), "segment sequence");
    long firstBlock = requireNonNegative(bytes.getLong(), "first block");
    long lastBlock = requireNonNegative(bytes.getLong(), "last block");
    long blockFrameCount = requireNonNegative(bytes.getLong(), "block frame count");
    long entryCount = requireNonNegative(bytes.getLong(), "entry count");
    long logicalPayloadBytes = requireNonNegative(bytes.getLong(),
        "logical payload bytes");
    long encodedBlockFrameBytes = requireNonNegative(bytes.getLong(),
        "encoded block frame bytes");
    long dataEndOffset = requireNonNegative(bytes.getLong(), "data end offset");
    long physicalFileBytes = requireNonNegative(bytes.getLong(), "physical file bytes");
    requireArray(getBytes(bytes, 32), StateArchiveFileFormatV3.fiveLaneDescriptorDigest(),
        "seal placement descriptor");
    SegmentSeal seal = new SegmentSeal(laneId,
        segmentSeq, firstBlock, lastBlock, blockFrameCount, entryCount,
        logicalPayloadBytes, encodedBlockFrameBytes, dataEndOffset, physicalFileBytes,
        getBytes(bytes, 32), getBytes(bytes, 32), getBytes(bytes, 32),
        getBytes(bytes, 32), getBytes(bytes, 32), null);
    requireZero(bytes, 20, "seal reserved bytes");
    byte[] encodedFrameDigest = getBytes(bytes, 32);
    requireArray(encodedFrameDigest, StateArchiveFileFormatV3.sha256(
        StateArchiveFileFormatV3.ENCODED_SEGMENT_SEAL_DOMAIN,
        Arrays.copyOf(encoded, SEAL_DIGEST_OFFSET)), "seal encoded frame digest");
    requireLong(bytes, totalLength, "repeated seal total length");
    if (bytes.getInt() != crc32c(encoded, 0, SEAL_CRC_OFFSET)) {
      throw new IllegalArgumentException("State Archive seal checksum mismatch");
    }
    requireInt(bytes, StateArchiveFileFormatV3.FRAME_TRAILER_MAGIC,
        "seal trailer magic");
    return seal.withEncodedFrameDigest(encodedFrameDigest);
  }

  public static byte[] encodeDurableMarker(DurableMarker marker) {
    Objects.requireNonNull(marker, "marker");
    marker.validate();
    int totalLength = StateArchiveFileFormatV3.MARKER_HEADER_LENGTH
        + StateArchiveFileFormatV3.FRAME_TRAILER_LENGTH;
    ByteBuffer bytes = ByteBuffer.allocate(totalLength);
    bytes.putInt(StateArchiveFileFormatV3.FRAME_MAGIC);
    bytes.putShort(StateArchiveFileFormatV3.MAJOR_VERSION);
    bytes.putShort(StateArchiveFileFormatV3.MINOR_VERSION);
    bytes.putShort(StateArchiveFileFormatV3.DURABLE_MARKER_FRAME_TYPE);
    bytes.putShort((short) 0);
    bytes.putInt(StateArchiveFileFormatV3.MARKER_HEADER_LENGTH);
    bytes.putLong(totalLength);
    bytes.putLong(0);
    bytes.putLong(marker.checkpointSequence);
    bytes.putLong(marker.firstEpoch);
    bytes.putLong(marker.lastEpoch);
    bytes.putLong(marker.firstBlock);
    bytes.putLong(marker.lastBlock);
    bytes.put(marker.lastBlockHash);
    bytes.put(StateArchiveFileFormatV3.fiveLaneDescriptorDigest());
    bytes.put(marker.resultHistoryDigest);
    bytes.putLong(marker.coveredStartOffset);
    bytes.putLong(marker.markerEndOffset);
    bytes.putLong(marker.blockCount);
    bytes.putLong(marker.logicalBytes);
    bytes.putLong(marker.encodedBytes);
    bytes.put(marker.previousMarkerDigest);
    bytes.put(marker.commonTargetDigest);
    bytes.putShort((short) marker.laneId);
    bytes.putShort((short) 0);
    bytes.putLong(marker.segmentSeq);
    bytes.putInt(0);
    if (bytes.position() != MARKER_DIGEST_OFFSET) {
      throw new IllegalStateException("Invalid State Archive durable marker layout");
    }
    byte[] digest = StateArchiveFileFormatV3.sha256(
        StateArchiveFileFormatV3.ENCODED_MARKER_DOMAIN,
        Arrays.copyOf(bytes.array(), MARKER_DIGEST_OFFSET));
    bytes.put(digest);
    bytes.putLong(totalLength);
    bytes.putInt(crc32c(bytes.array(), 0, MARKER_CRC_OFFSET));
    bytes.putInt(StateArchiveFileFormatV3.FRAME_TRAILER_MAGIC);
    if (bytes.hasRemaining()) {
      throw new IllegalStateException("Invalid State Archive durable marker length");
    }
    return bytes.array();
  }

  public static DurableMarker decodeDurableMarker(byte[] encoded) {
    int totalLength = StateArchiveFileFormatV3.MARKER_HEADER_LENGTH
        + StateArchiveFileFormatV3.FRAME_TRAILER_LENGTH;
    requireLength(encoded, totalLength, "durable marker");
    ByteBuffer bytes = ByteBuffer.wrap(encoded);
    requireInt(bytes, StateArchiveFileFormatV3.FRAME_MAGIC, "marker magic");
    requireShort(bytes, StateArchiveFileFormatV3.MAJOR_VERSION, "marker major version");
    requireShort(bytes, StateArchiveFileFormatV3.MINOR_VERSION, "marker minor version");
    requireShort(bytes, StateArchiveFileFormatV3.DURABLE_MARKER_FRAME_TYPE,
        "marker frame type");
    requireShort(bytes, (short) 0, "marker flags");
    requireInt(bytes, StateArchiveFileFormatV3.MARKER_HEADER_LENGTH,
        "marker header length");
    requireLong(bytes, totalLength, "marker total length");
    requireLong(bytes, 0, "marker payload length");
    long checkpointSequence = requireNonNegative(bytes.getLong(), "checkpoint sequence");
    long firstEpoch = requireNonNegative(bytes.getLong(), "first epoch");
    long lastEpoch = requireNonNegative(bytes.getLong(), "last epoch");
    long firstBlock = requireNonNegative(bytes.getLong(), "first block");
    long lastBlock = requireNonNegative(bytes.getLong(), "last block");
    byte[] lastBlockHash = getBytes(bytes, 32);
    requireArray(getBytes(bytes, 32), StateArchiveFileFormatV3.fiveLaneDescriptorDigest(),
        "marker placement descriptor");
    byte[] resultHistoryDigest = getBytes(bytes, 32);
    long coveredStartOffset = requireNonNegative(bytes.getLong(), "covered start offset");
    long markerEndOffset = requireNonNegative(bytes.getLong(), "marker end offset");
    long blockCount = requireNonNegative(bytes.getLong(), "marker block count");
    long logicalBytes = requireNonNegative(bytes.getLong(), "marker logical bytes");
    long encodedBytes = requireNonNegative(bytes.getLong(), "marker encoded bytes");
    byte[] previousMarkerDigest = getBytes(bytes, 32);
    byte[] commonTargetDigest = getBytes(bytes, 32);
    int laneId = Short.toUnsignedInt(bytes.getShort());
    requireShort(bytes, (short) 0, "marker lane reserved field");
    long segmentSeq = requireNonNegative(bytes.getLong(), "marker segment sequence");
    requireInt(bytes, 0, "marker reserved tail");
    DurableMarker marker = new DurableMarker(checkpointSequence, firstEpoch, lastEpoch,
        firstBlock, lastBlock, lastBlockHash, resultHistoryDigest, coveredStartOffset,
        markerEndOffset, blockCount, logicalBytes, encodedBytes, previousMarkerDigest,
        commonTargetDigest, laneId, segmentSeq);
    byte[] digest = getBytes(bytes, 32);
    requireArray(digest, StateArchiveFileFormatV3.sha256(
        StateArchiveFileFormatV3.ENCODED_MARKER_DOMAIN,
        Arrays.copyOf(encoded, MARKER_DIGEST_OFFSET)), "marker encoded frame digest");
    requireLong(bytes, totalLength, "repeated marker total length");
    if (bytes.getInt() != crc32c(encoded, 0, MARKER_CRC_OFFSET)) {
      throw new IllegalArgumentException("State Archive durable marker checksum mismatch");
    }
    requireInt(bytes, StateArchiveFileFormatV3.FRAME_TRAILER_MAGIC,
        "marker trailer magic");
    return marker.withEncodedFrameDigest(digest);
  }

  public static byte[] encodeBlockIndexHeader(BlockIndexHeader header) {
    Objects.requireNonNull(header, "header");
    header.validate();
    ByteBuffer bytes = ByteBuffer.allocate(StateArchiveFileFormatV3.BLOCK_INDEX_HEADER_LENGTH);
    bytes.putInt(StateArchiveFileFormatV3.BLOCK_INDEX_MAGIC);
    bytes.putShort(StateArchiveFileFormatV3.MAJOR_VERSION);
    bytes.putShort(StateArchiveFileFormatV3.MINOR_VERSION);
    bytes.putShort((short) StateArchiveFileFormatV3.BLOCK_INDEX_HEADER_LENGTH);
    bytes.putShort((short) StateArchiveFileFormatV3.BLOCK_INDEX_ENTRY_LENGTH);
    bytes.putShort((short) header.laneId);
    bytes.putShort(StateArchiveFileFormatV3.SEGMENT_LAYOUT_ID);
    bytes.putLong(header.segmentSeq);
    bytes.put(header.dataSegmentHeaderDigest);
    bytes.put(StateArchiveFileFormatV3.fiveLaneDescriptorDigest());
    bytes.putInt(0);
    if (bytes.position() != BLOCK_INDEX_HEADER_DIGEST_OFFSET) {
      throw new IllegalStateException("Invalid State Archive block index header layout");
    }
    bytes.put(StateArchiveFileFormatV3.sha256(
        StateArchiveFileFormatV3.BLOCK_INDEX_HEADER_DOMAIN,
        Arrays.copyOf(bytes.array(), BLOCK_INDEX_HEADER_DIGEST_OFFSET)));
    bytes.putInt(crc32c(bytes.array(), 0, BLOCK_INDEX_HEADER_CRC_OFFSET));
    if (bytes.hasRemaining()) {
      throw new IllegalStateException("Invalid State Archive block index header length");
    }
    return bytes.array();
  }

  public static BlockIndexHeader decodeBlockIndexHeader(byte[] encoded) {
    requireLength(encoded, StateArchiveFileFormatV3.BLOCK_INDEX_HEADER_LENGTH,
        "block index header");
    ByteBuffer bytes = ByteBuffer.wrap(encoded);
    requireInt(bytes, StateArchiveFileFormatV3.BLOCK_INDEX_MAGIC, "block index magic");
    requireShort(bytes, StateArchiveFileFormatV3.MAJOR_VERSION, "index major version");
    requireShort(bytes, StateArchiveFileFormatV3.MINOR_VERSION, "index minor version");
    requireShort(bytes, (short) StateArchiveFileFormatV3.BLOCK_INDEX_HEADER_LENGTH,
        "index header length");
    requireShort(bytes, (short) StateArchiveFileFormatV3.BLOCK_INDEX_ENTRY_LENGTH,
        "index entry length");
    int laneId = Short.toUnsignedInt(bytes.getShort());
    StateArchiveFileFormatV3.laneKind(laneId);
    requireShort(bytes, StateArchiveFileFormatV3.SEGMENT_LAYOUT_ID,
        "index segment layout");
    long segmentSeq = requireNonNegative(bytes.getLong(), "index segment sequence");
    byte[] dataHeaderDigest = getBytes(bytes, 32);
    requireArray(getBytes(bytes, 32), StateArchiveFileFormatV3.fiveLaneDescriptorDigest(),
        "index placement descriptor");
    requireInt(bytes, 0, "index reserved field");
    byte[] headerDigest = getBytes(bytes, 32);
    requireArray(headerDigest, StateArchiveFileFormatV3.sha256(
        StateArchiveFileFormatV3.BLOCK_INDEX_HEADER_DOMAIN,
        Arrays.copyOf(encoded, BLOCK_INDEX_HEADER_DIGEST_OFFSET)),
        "block index header digest");
    if (bytes.getInt() != crc32c(encoded, 0, BLOCK_INDEX_HEADER_CRC_OFFSET)) {
      throw new IllegalArgumentException("State Archive block index checksum mismatch");
    }
    return new BlockIndexHeader(laneId, segmentSeq, dataHeaderDigest, headerDigest);
  }

  public static byte[] encodeBlockIndexEntry(BlockIndexEntry entry) {
    Objects.requireNonNull(entry, "entry");
    entry.validate();
    return ByteBuffer.allocate(StateArchiveFileFormatV3.BLOCK_INDEX_ENTRY_LENGTH)
        .putLong(entry.blockNumber)
        .putLong(entry.frameOffset)
        .putInt(entry.frameLength)
        .putInt(0)
        .putLong(entry.encodedFrameDigestPrefix)
        .array();
  }

  public static BlockIndexEntry decodeBlockIndexEntry(byte[] encoded) {
    requireLength(encoded, StateArchiveFileFormatV3.BLOCK_INDEX_ENTRY_LENGTH,
        "block index entry");
    ByteBuffer bytes = ByteBuffer.wrap(encoded);
    BlockIndexEntry entry = new BlockIndexEntry(
        requireNonNegative(bytes.getLong(), "index block number"),
        requireNonNegative(bytes.getLong(), "index frame offset"),
        bytes.getInt(), bytes.getInt(), bytes.getLong());
    entry.validate();
    return entry;
  }

  public static byte[] encodeSealedMapRecord(SealedSegment segment) {
    Objects.requireNonNull(segment, "segment");
    segment.validate();
    ByteBuffer bytes = ByteBuffer.allocate(StateArchiveFileFormatV3.SEGMENT_MAP_ENTRY_LENGTH);
    bytes.putShort((short) segment.laneId);
    bytes.putShort(StateArchiveFileFormatV3.laneKind(segment.laneId));
    bytes.putInt(0);
    bytes.putLong(segment.segmentSeq);
    bytes.putLong(segment.firstBlock);
    bytes.putLong(segment.lastBlock);
    bytes.putLong(segment.blockFrameCount);
    bytes.putLong(segment.dataFileBytes);
    bytes.putLong(segment.blockIndexBytes);
    bytes.put(segment.segmentHeaderDigest);
    bytes.put(segment.segmentContentDigest);
    bytes.put(segment.sealFrameDigest);
    bytes.put(segment.manifestDigest);
    bytes.putLong(0);
    if (bytes.hasRemaining()) {
      throw new IllegalStateException("Invalid State Archive SegmentMap record length");
    }
    return bytes.array();
  }

  public static SealedSegment decodeSealedMapRecord(byte[] encoded) {
    requireLength(encoded, StateArchiveFileFormatV3.SEGMENT_MAP_ENTRY_LENGTH,
        "SegmentMap record");
    ByteBuffer bytes = ByteBuffer.wrap(encoded);
    int laneId = Short.toUnsignedInt(bytes.getShort());
    if (bytes.getShort() != StateArchiveFileFormatV3.laneKind(laneId)) {
      throw new IllegalArgumentException("State Archive SegmentMap lane kind mismatch");
    }
    requireInt(bytes, 0, "SegmentMap flags");
    SealedSegment result = new SealedSegment(laneId,
        requireNonNegative(bytes.getLong(), "segment sequence"),
        requireNonNegative(bytes.getLong(), "first block"),
        requireNonNegative(bytes.getLong(), "last block"),
        requireNonNegative(bytes.getLong(), "block count"),
        requireNonNegative(bytes.getLong(), "data file bytes"),
        requireNonNegative(bytes.getLong(), "block index bytes"),
        getBytes(bytes, 32), getBytes(bytes, 32), getBytes(bytes, 32), getBytes(bytes, 32));
    requireLong(bytes, 0, "SegmentMap reserved field");
    result.validate();
    return result;
  }

  private static void requireCompression(short compressionId) {
    if (compressionId != StateArchiveFileFormatV3.COMPRESSION_NONE
        && compressionId != StateArchiveFileFormatV3.COMPRESSION_RAW_DEFLATE_LEVEL_1) {
      throw new IllegalArgumentException("Unknown State Archive compression ID");
    }
  }

  private static byte[] requireHash(byte[] value, String name) {
    requireLength(value, StateArchiveFileFormatV3.HASH_LENGTH, name);
    return Arrays.copyOf(value, value.length);
  }

  private static void requireLength(byte[] value, int length, String name) {
    if (value == null || value.length != length) {
      throw new IllegalArgumentException("Invalid State Archive " + name + " length");
    }
  }

  private static byte[] getBytes(ByteBuffer bytes, int length) {
    byte[] result = new byte[length];
    bytes.get(result);
    return result;
  }

  private static void requireZero(ByteBuffer bytes, int length, String name) {
    for (int index = 0; index < length; index++) {
      if (bytes.get() != 0) {
        throw new IllegalArgumentException("Non-zero State Archive " + name);
      }
    }
  }

  private static long requireNonNegative(long value, String name) {
    if (value < 0) {
      throw new IllegalArgumentException("Negative State Archive " + name);
    }
    return value;
  }

  private static void requireInt(ByteBuffer bytes, int expected, String name) {
    if (bytes.getInt() != expected) {
      throw new IllegalArgumentException("State Archive " + name + " mismatch");
    }
  }

  private static void requireShort(ByteBuffer bytes, short expected, String name) {
    if (bytes.getShort() != expected) {
      throw new IllegalArgumentException("State Archive " + name + " mismatch");
    }
  }

  private static void requireLong(ByteBuffer bytes, long expected, String name) {
    if (bytes.getLong() != expected) {
      throw new IllegalArgumentException("State Archive " + name + " mismatch");
    }
  }

  private static void requireArray(byte[] actual, byte[] expected, String name) {
    if (!Arrays.equals(actual, expected)) {
      throw new IllegalArgumentException("State Archive " + name + " mismatch");
    }
  }

  private static int crc32c(byte[] bytes, int offset, int length) {
    return Hashing.crc32c().hashBytes(bytes, offset, length).asInt();
  }

  public static final class DurableMarker {
    private final long checkpointSequence;
    private final long firstEpoch;
    private final long lastEpoch;
    private final long firstBlock;
    private final long lastBlock;
    private final byte[] lastBlockHash;
    private final byte[] resultHistoryDigest;
    private final long coveredStartOffset;
    private final long markerEndOffset;
    private final long blockCount;
    private final long logicalBytes;
    private final long encodedBytes;
    private final byte[] previousMarkerDigest;
    private final byte[] commonTargetDigest;
    private final int laneId;
    private final long segmentSeq;
    private final byte[] encodedFrameDigest;

    public DurableMarker(long checkpointSequence, long firstEpoch, long lastEpoch,
        long firstBlock, long lastBlock, byte[] lastBlockHash,
        byte[] resultHistoryDigest, long coveredStartOffset, long markerEndOffset,
        long blockCount, long logicalBytes, long encodedBytes,
        byte[] previousMarkerDigest, byte[] commonTargetDigest, int laneId,
        long segmentSeq) {
      this(checkpointSequence, firstEpoch, lastEpoch, firstBlock, lastBlock,
          lastBlockHash, resultHistoryDigest, coveredStartOffset, markerEndOffset,
          blockCount, logicalBytes, encodedBytes, previousMarkerDigest,
          commonTargetDigest, laneId, segmentSeq, null);
    }

    private DurableMarker(long checkpointSequence, long firstEpoch, long lastEpoch,
        long firstBlock, long lastBlock, byte[] lastBlockHash,
        byte[] resultHistoryDigest, long coveredStartOffset, long markerEndOffset,
        long blockCount, long logicalBytes, long encodedBytes,
        byte[] previousMarkerDigest, byte[] commonTargetDigest, int laneId,
        long segmentSeq, byte[] encodedFrameDigest) {
      this.checkpointSequence = checkpointSequence;
      this.firstEpoch = firstEpoch;
      this.lastEpoch = lastEpoch;
      this.firstBlock = firstBlock;
      this.lastBlock = lastBlock;
      this.lastBlockHash = requireHash(lastBlockHash, "marker last block hash");
      this.resultHistoryDigest = requireHash(resultHistoryDigest,
          "marker result history digest");
      this.coveredStartOffset = coveredStartOffset;
      this.markerEndOffset = markerEndOffset;
      this.blockCount = blockCount;
      this.logicalBytes = logicalBytes;
      this.encodedBytes = encodedBytes;
      this.previousMarkerDigest = requireHash(previousMarkerDigest,
          "previous marker digest");
      this.commonTargetDigest = requireHash(commonTargetDigest,
          "marker Common target digest");
      this.laneId = laneId;
      this.segmentSeq = segmentSeq;
      this.encodedFrameDigest = encodedFrameDigest == null ? null
          : requireHash(encodedFrameDigest, "marker encoded frame digest");
      validate();
    }

    private DurableMarker withEncodedFrameDigest(byte[] digest) {
      return new DurableMarker(checkpointSequence, firstEpoch, lastEpoch, firstBlock,
          lastBlock, lastBlockHash, resultHistoryDigest, coveredStartOffset,
          markerEndOffset, blockCount, logicalBytes, encodedBytes,
          previousMarkerDigest, commonTargetDigest, laneId, segmentSeq, digest);
    }

    private void validate() {
      StateArchiveFileFormatV3.laneKind(laneId);
      int totalLength = StateArchiveFileFormatV3.MARKER_HEADER_LENGTH
          + StateArchiveFileFormatV3.FRAME_TRAILER_LENGTH;
      if (checkpointSequence < 0 || firstEpoch < 0 || lastEpoch < firstEpoch
          || firstBlock < 0 || lastBlock < firstBlock
          || firstEpoch != firstBlock || lastEpoch != lastBlock
          || blockCount <= 0 || lastBlock - firstBlock != blockCount - 1
          || logicalBytes < 0 || encodedBytes <= 0
          || coveredStartOffset < StateArchiveFileFormatV3.PART_HEADER_LENGTH
          || coveredStartOffset > Long.MAX_VALUE - encodedBytes - totalLength
          || markerEndOffset != coveredStartOffset + encodedBytes + totalLength
          || segmentSeq < 0) {
        throw new IllegalArgumentException("Invalid State Archive durable marker state");
      }
    }

    public long getCheckpointSequence() {
      return checkpointSequence;
    }

    public long getFirstBlock() {
      return firstBlock;
    }

    public long getLastBlock() {
      return lastBlock;
    }

    public byte[] getLastBlockHash() {
      return Arrays.copyOf(lastBlockHash, lastBlockHash.length);
    }

    public byte[] getResultHistoryDigest() {
      return Arrays.copyOf(resultHistoryDigest, resultHistoryDigest.length);
    }

    public long getCoveredStartOffset() {
      return coveredStartOffset;
    }

    public long getMarkerEndOffset() {
      return markerEndOffset;
    }

    public long getBlockCount() {
      return blockCount;
    }

    public long getLogicalBytes() {
      return logicalBytes;
    }

    public long getEncodedBytes() {
      return encodedBytes;
    }

    public byte[] getPreviousMarkerDigest() {
      return Arrays.copyOf(previousMarkerDigest, previousMarkerDigest.length);
    }

    public byte[] getCommonTargetDigest() {
      return Arrays.copyOf(commonTargetDigest, commonTargetDigest.length);
    }

    public int getLaneId() {
      return laneId;
    }

    public long getSegmentSeq() {
      return segmentSeq;
    }

    public byte[] getEncodedFrameDigest() {
      return encodedFrameDigest == null ? null
          : Arrays.copyOf(encodedFrameDigest, encodedFrameDigest.length);
    }
  }

  public static final class SegmentSeal {
    private final int laneId;
    private final long segmentSeq;
    private final long actualFirstBlock;
    private final long actualLastBlock;
    private final long blockFrameCount;
    private final long entryCount;
    private final long logicalPayloadBytes;
    private final long encodedBlockFrameBytes;
    private final long dataEndOffset;
    private final long physicalFileBytes;
    private final byte[] firstBlockFrameDigest;
    private final byte[] lastBlockFrameDigest;
    private final byte[] startHistoryDigest;
    private final byte[] endHistoryDigest;
    private final byte[] segmentContentDigest;
    private final byte[] encodedFrameDigest;

    public SegmentSeal(int laneId, long segmentSeq, long actualFirstBlock,
        long actualLastBlock, long blockFrameCount, long entryCount,
        long logicalPayloadBytes, long encodedBlockFrameBytes, long dataEndOffset,
        long physicalFileBytes, byte[] firstBlockFrameDigest,
        byte[] lastBlockFrameDigest, byte[] startHistoryDigest,
        byte[] endHistoryDigest, byte[] segmentContentDigest) {
      this(laneId, segmentSeq, actualFirstBlock, actualLastBlock, blockFrameCount,
          entryCount, logicalPayloadBytes, encodedBlockFrameBytes, dataEndOffset,
          physicalFileBytes, firstBlockFrameDigest, lastBlockFrameDigest,
          startHistoryDigest, endHistoryDigest, segmentContentDigest, null);
    }

    private SegmentSeal(int laneId, long segmentSeq, long actualFirstBlock,
        long actualLastBlock, long blockFrameCount, long entryCount,
        long logicalPayloadBytes, long encodedBlockFrameBytes, long dataEndOffset,
        long physicalFileBytes, byte[] firstBlockFrameDigest,
        byte[] lastBlockFrameDigest, byte[] startHistoryDigest,
        byte[] endHistoryDigest, byte[] segmentContentDigest,
        byte[] encodedFrameDigest) {
      this.laneId = laneId;
      this.segmentSeq = segmentSeq;
      this.actualFirstBlock = actualFirstBlock;
      this.actualLastBlock = actualLastBlock;
      this.blockFrameCount = blockFrameCount;
      this.entryCount = entryCount;
      this.logicalPayloadBytes = logicalPayloadBytes;
      this.encodedBlockFrameBytes = encodedBlockFrameBytes;
      this.dataEndOffset = dataEndOffset;
      this.physicalFileBytes = physicalFileBytes;
      this.firstBlockFrameDigest = requireHash(firstBlockFrameDigest,
          "first block frame digest");
      this.lastBlockFrameDigest = requireHash(lastBlockFrameDigest,
          "last block frame digest");
      this.startHistoryDigest = requireHash(startHistoryDigest,
          "start history digest");
      this.endHistoryDigest = requireHash(endHistoryDigest, "end history digest");
      this.segmentContentDigest = requireHash(segmentContentDigest,
          "segment content digest");
      this.encodedFrameDigest = encodedFrameDigest == null ? null
          : requireHash(encodedFrameDigest, "seal encoded frame digest");
      validate();
    }

    private SegmentSeal withEncodedFrameDigest(byte[] digest) {
      return new SegmentSeal(laneId, segmentSeq, actualFirstBlock, actualLastBlock,
          blockFrameCount, entryCount, logicalPayloadBytes, encodedBlockFrameBytes,
          dataEndOffset, physicalFileBytes, firstBlockFrameDigest,
          lastBlockFrameDigest, startHistoryDigest, endHistoryDigest,
          segmentContentDigest, digest);
    }

    private void validate() {
      StateArchiveFileFormatV3.laneKind(laneId);
      int sealLength = StateArchiveFileFormatV3.SEAL_HEADER_LENGTH
          + StateArchiveFileFormatV3.FRAME_TRAILER_LENGTH;
      if (segmentSeq < 0 || actualFirstBlock < 0 || actualLastBlock < actualFirstBlock
          || blockFrameCount <= 0
          || actualLastBlock - actualFirstBlock != blockFrameCount - 1
          || entryCount < 0 || logicalPayloadBytes < 0 || encodedBlockFrameBytes < 0
          || dataEndOffset < StateArchiveFileFormatV3.PART_HEADER_LENGTH
          || dataEndOffset > Long.MAX_VALUE - sealLength
          || physicalFileBytes != dataEndOffset + sealLength) {
        throw new IllegalArgumentException("Invalid State Archive segment seal state");
      }
    }

    public int getLaneId() {
      return laneId;
    }

    public long getSegmentSeq() {
      return segmentSeq;
    }

    public long getActualFirstBlock() {
      return actualFirstBlock;
    }

    public long getActualLastBlock() {
      return actualLastBlock;
    }

    public long getBlockFrameCount() {
      return blockFrameCount;
    }

    public long getEntryCount() {
      return entryCount;
    }

    public long getLogicalPayloadBytes() {
      return logicalPayloadBytes;
    }

    public long getEncodedBlockFrameBytes() {
      return encodedBlockFrameBytes;
    }

    public long getDataEndOffset() {
      return dataEndOffset;
    }

    public long getPhysicalFileBytes() {
      return physicalFileBytes;
    }

    public byte[] getFirstBlockFrameDigest() {
      return Arrays.copyOf(firstBlockFrameDigest, firstBlockFrameDigest.length);
    }

    public byte[] getLastBlockFrameDigest() {
      return Arrays.copyOf(lastBlockFrameDigest, lastBlockFrameDigest.length);
    }

    public byte[] getStartHistoryDigest() {
      return Arrays.copyOf(startHistoryDigest, startHistoryDigest.length);
    }

    public byte[] getEndHistoryDigest() {
      return Arrays.copyOf(endHistoryDigest, endHistoryDigest.length);
    }

    public byte[] getSegmentContentDigest() {
      return Arrays.copyOf(segmentContentDigest, segmentContentDigest.length);
    }

    public byte[] getEncodedFrameDigest() {
      return encodedFrameDigest == null ? null
          : Arrays.copyOf(encodedFrameDigest, encodedFrameDigest.length);
    }
  }

  public static final class BlockIndexHeader {
    private final int laneId;
    private final long segmentSeq;
    private final byte[] dataSegmentHeaderDigest;
    private final byte[] headerDigest;

    public BlockIndexHeader(int laneId, long segmentSeq,
        byte[] dataSegmentHeaderDigest) {
      this(laneId, segmentSeq, dataSegmentHeaderDigest, null);
    }

    private BlockIndexHeader(int laneId, long segmentSeq,
        byte[] dataSegmentHeaderDigest, byte[] headerDigest) {
      this.laneId = laneId;
      this.segmentSeq = segmentSeq;
      this.dataSegmentHeaderDigest = requireHash(dataSegmentHeaderDigest,
          "data segment header digest");
      this.headerDigest = headerDigest == null ? null
          : requireHash(headerDigest, "block index header digest");
      validate();
    }

    private void validate() {
      StateArchiveFileFormatV3.laneKind(laneId);
      requireNonNegative(segmentSeq, "index segment sequence");
    }

    public int getLaneId() {
      return laneId;
    }

    public long getSegmentSeq() {
      return segmentSeq;
    }

    public byte[] getDataSegmentHeaderDigest() {
      return Arrays.copyOf(dataSegmentHeaderDigest, dataSegmentHeaderDigest.length);
    }

    public byte[] getHeaderDigest() {
      return headerDigest == null ? null : Arrays.copyOf(headerDigest, headerDigest.length);
    }
  }

  public static final class BlockIndexEntry {
    private final long blockNumber;
    private final long frameOffset;
    private final int frameLength;
    private final int flags;
    private final long encodedFrameDigestPrefix;

    public BlockIndexEntry(long blockNumber, long frameOffset, int frameLength,
        long encodedFrameDigestPrefix) {
      this(blockNumber, frameOffset, frameLength, 0, encodedFrameDigestPrefix);
    }

    private BlockIndexEntry(long blockNumber, long frameOffset, int frameLength,
        int flags, long encodedFrameDigestPrefix) {
      this.blockNumber = blockNumber;
      this.frameOffset = frameOffset;
      this.frameLength = frameLength;
      this.flags = flags;
      this.encodedFrameDigestPrefix = encodedFrameDigestPrefix;
      validate();
    }

    private void validate() {
      if (blockNumber < 0 || frameOffset < StateArchiveFileFormatV3.PART_HEADER_LENGTH
          || frameLength < StateArchiveFileFormatV3.BLOCK_HEADER_LENGTH
              + StateArchiveFileFormatV3.FRAME_TRAILER_LENGTH
          || frameLength > StateArchiveFileFormatV3.MAX_BLOCK_FRAME_BYTES || flags != 0) {
        throw new IllegalArgumentException("Invalid State Archive block index entry");
      }
    }

    public long getBlockNumber() {
      return blockNumber;
    }

    public long getFrameOffset() {
      return frameOffset;
    }

    public int getFrameLength() {
      return frameLength;
    }

    public long getEncodedFrameDigestPrefix() {
      return encodedFrameDigestPrefix;
    }
  }

  public static final class SegmentHeader {
    private final int laneId;
    private final long segmentSeq;
    private final long actualFirstBlock;
    private final byte[] previousSegmentDigest;
    private final byte[] previousHistoryDigest;
    private final short compressionId;
    private final byte[] headerDigest;

    public SegmentHeader(int laneId, long segmentSeq, long actualFirstBlock,
        byte[] previousSegmentDigest, byte[] previousHistoryDigest, short compressionId) {
      this(laneId, segmentSeq, actualFirstBlock, previousSegmentDigest,
          previousHistoryDigest, compressionId, null);
    }

    private SegmentHeader(int laneId, long segmentSeq, long actualFirstBlock,
        byte[] previousSegmentDigest, byte[] previousHistoryDigest, short compressionId,
        byte[] headerDigest) {
      this.laneId = laneId;
      this.segmentSeq = segmentSeq;
      this.actualFirstBlock = actualFirstBlock;
      this.previousSegmentDigest = requireHash(previousSegmentDigest,
          "previous segment digest");
      this.previousHistoryDigest = requireHash(previousHistoryDigest,
          "previous history digest");
      this.compressionId = compressionId;
      this.headerDigest = headerDigest == null ? null : requireHash(headerDigest,
          "segment header digest");
    }

    private void validate() {
      StateArchiveFileFormatV3.laneKind(laneId);
      requireNonNegative(segmentSeq, "segment sequence");
      requireNonNegative(actualFirstBlock, "first block");
      requireCompression(compressionId);
    }

    public int getLaneId() {
      return laneId;
    }

    public long getSegmentSeq() {
      return segmentSeq;
    }

    public long getActualFirstBlock() {
      return actualFirstBlock;
    }

    public byte[] getPreviousSegmentDigest() {
      return Arrays.copyOf(previousSegmentDigest, previousSegmentDigest.length);
    }

    public byte[] getPreviousHistoryDigest() {
      return Arrays.copyOf(previousHistoryDigest, previousHistoryDigest.length);
    }

    public short getCompressionId() {
      return compressionId;
    }

    public byte[] getHeaderDigest() {
      return headerDigest == null ? null : Arrays.copyOf(headerDigest, headerDigest.length);
    }
  }

  public static final class CurrentSegment {
    private final int laneId;
    private final long segmentSeq;
    private final long firstBlock;
    private final long currentLastBlock;
    private final long dataEndOffset;
    private final long blockFrameCount;
    private final byte[] headerDigest;

    public CurrentSegment(int laneId, long segmentSeq, long firstBlock, long currentLastBlock,
        long dataEndOffset, long blockFrameCount, byte[] headerDigest) {
      StateArchiveFileFormatV3.laneKind(laneId);
      if (segmentSeq < 0 || firstBlock < 0 || currentLastBlock < firstBlock
          || blockFrameCount <= 0 || currentLastBlock - firstBlock + 1 != blockFrameCount
          || dataEndOffset < StateArchiveFileFormatV3.PART_HEADER_LENGTH) {
        throw new IllegalArgumentException("Invalid State Archive current SegmentMap state");
      }
      this.laneId = laneId;
      this.segmentSeq = segmentSeq;
      this.firstBlock = firstBlock;
      this.currentLastBlock = currentLastBlock;
      this.dataEndOffset = dataEndOffset;
      this.blockFrameCount = blockFrameCount;
      this.headerDigest = requireHash(headerDigest, "current segment header digest");
    }

    public int getLaneId() {
      return laneId;
    }

    public long getSegmentSeq() {
      return segmentSeq;
    }

    public long getFirstBlock() {
      return firstBlock;
    }

    public long getCurrentLastBlock() {
      return currentLastBlock;
    }

    public long getDataEndOffset() {
      return dataEndOffset;
    }

    public long getBlockFrameCount() {
      return blockFrameCount;
    }

    public byte[] getHeaderDigest() {
      return Arrays.copyOf(headerDigest, headerDigest.length);
    }
  }

  public static final class SealedSegment {
    private final int laneId;
    private final long segmentSeq;
    private final long firstBlock;
    private final long lastBlock;
    private final long blockFrameCount;
    private final long dataFileBytes;
    private final long blockIndexBytes;
    private final byte[] segmentHeaderDigest;
    private final byte[] segmentContentDigest;
    private final byte[] sealFrameDigest;
    private final byte[] manifestDigest;

    public SealedSegment(int laneId, long segmentSeq, long firstBlock, long lastBlock,
        long blockFrameCount, long dataFileBytes, long blockIndexBytes,
        byte[] segmentHeaderDigest, byte[] segmentContentDigest, byte[] sealFrameDigest,
        byte[] manifestDigest) {
      this.laneId = laneId;
      this.segmentSeq = segmentSeq;
      this.firstBlock = firstBlock;
      this.lastBlock = lastBlock;
      this.blockFrameCount = blockFrameCount;
      this.dataFileBytes = dataFileBytes;
      this.blockIndexBytes = blockIndexBytes;
      this.segmentHeaderDigest = requireHash(segmentHeaderDigest, "segment header digest");
      this.segmentContentDigest = requireHash(segmentContentDigest, "segment content digest");
      this.sealFrameDigest = requireHash(sealFrameDigest, "seal frame digest");
      this.manifestDigest = requireHash(manifestDigest, "manifest digest");
      validate();
    }

    private void validate() {
      StateArchiveFileFormatV3.laneKind(laneId);
      if (segmentSeq < 0 || firstBlock < 0 || lastBlock < firstBlock
          || blockFrameCount <= 0 || lastBlock - firstBlock + 1 != blockFrameCount
          || dataFileBytes < StateArchiveFileFormatV3.PART_HEADER_LENGTH
              + StateArchiveFileFormatV3.SEAL_HEADER_LENGTH
              + StateArchiveFileFormatV3.FRAME_TRAILER_LENGTH
          || blockIndexBytes < StateArchiveFileFormatV3.BLOCK_INDEX_HEADER_LENGTH) {
        throw new IllegalArgumentException("Invalid State Archive sealed SegmentMap state");
      }
    }

    public int getLaneId() {
      return laneId;
    }

    public long getSegmentSeq() {
      return segmentSeq;
    }

    public long getFirstBlock() {
      return firstBlock;
    }

    public long getLastBlock() {
      return lastBlock;
    }

    public long getBlockFrameCount() {
      return blockFrameCount;
    }

    public long getDataFileBytes() {
      return dataFileBytes;
    }

    public long getBlockIndexBytes() {
      return blockIndexBytes;
    }

    public byte[] getSegmentHeaderDigest() {
      return Arrays.copyOf(segmentHeaderDigest, segmentHeaderDigest.length);
    }

    public byte[] getSegmentContentDigest() {
      return Arrays.copyOf(segmentContentDigest, segmentContentDigest.length);
    }

    public byte[] getSealFrameDigest() {
      return Arrays.copyOf(sealFrameDigest, sealFrameDigest.length);
    }

    public byte[] getManifestDigest() {
      return Arrays.copyOf(manifestDigest, manifestDigest.length);
    }
  }
}
