package org.tron.core.db2.archive;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;

/** Single source of truth for the default-off State Archive append-file v3 format. */
public final class StateArchiveFileFormatV3 {

  public static final String FORMAT_ID = "archive-state/mixed-frame-segment/v3";
  public static final String FIVE_LANE_FORMAT_ID =
      "archive-state/five-lane-frame-segment/v3";

  public static final int FRAME_MAGIC = 0x53414633;
  public static final int FRAME_TRAILER_MAGIC = 0x33464153;
  public static final int PAYLOAD_MAGIC = 0x53425031;
  public static final int PART_MAGIC = 0x53415033;
  public static final int SEGMENT_MAGIC = 0x53415333;
  public static final int BLOCK_INDEX_MAGIC = 0x53424933;
  public static final int RECOVERY_INTENT_MAGIC = 0x53524933;
  public static final int RECOVERY_INTENT_TRAILER_MAGIC = 0x33495253;
  public static final int SEGMENT_MANIFEST_MAGIC = 0x53414d33;
  public static final int SEGMENT_MANIFEST_TRAILER_MAGIC = 0x334d4153;

  public static final short MAJOR_VERSION = 3;
  public static final short MINOR_VERSION = 0;
  public static final short BLOCK_FRAME_TYPE = 1;
  public static final short DURABLE_MARKER_FRAME_TYPE = 2;
  public static final short PART_SEAL_FRAME_TYPE = 3;

  public static final short BODY_CODEC_ID = 1;
  public static final short LANE_VARIABLE_BODY_CODEC_ID = 2;
  public static final short DEDICATED_FIXED_WIDTH_BODY_CODEC_ID = 3;
  public static final short COMPRESSION_NONE = 0;
  public static final short COMPRESSION_RAW_DEFLATE_LEVEL_1 = 1;
  public static final short KEY_ORDER_ID = 1;
  public static final short DIGEST_ID = 1;
  public static final short CHECKSUM_ID = 1;
  public static final short IDENTITY_KIND = 1;
  public static final short LANE_KIND = 1;
  public static final short LANE_ID = 0;
  public static final short SIZE_ROLLED_AFTER_TARGET_ROTATION_ID = 1;
  public static final short SEGMENT_LAYOUT_ID = 2;
  public static final short SEGMENT_OVERSHOOT_POLICY_ID = 1;
  public static final short RECOVERY_INTENT_ACTION_SCHEMA_ID = 1;
  public static final short RECOVERY_POINT_SCHEMA_ID = 1;

  public static final int FORMAT_DESCRIPTOR_LENGTH = 96;
  public static final int FRAME_ENVELOPE_LENGTH = 32;
  public static final int BLOCK_HEADER_LENGTH = 336;
  public static final int MARKER_HEADER_LENGTH = 288;
  public static final int SEAL_HEADER_LENGTH = 320;
  public static final int FRAME_TRAILER_LENGTH = 48;
  public static final int PART_HEADER_LENGTH = 512;
  public static final int BLOCK_INDEX_HEADER_LENGTH = 128;
  public static final int BLOCK_INDEX_ENTRY_LENGTH = 32;
  public static final int MANIFEST_HEADER_LENGTH = 256;
  public static final int MANIFEST_PART_RECORD_LENGTH = 160;
  public static final int MANIFEST_TRAILER_LENGTH = 48;
  public static final int MANIFEST_TOTAL_LENGTH = MANIFEST_HEADER_LENGTH
      + MANIFEST_TRAILER_LENGTH;
  public static final int SEGMENT_LAYOUT_DESCRIPTOR_LENGTH = 64;
  public static final int SEGMENT_MAP_ENTRY_LENGTH = 192;
  public static final int RECOVERY_INTENT_LAYOUT_DESCRIPTOR_LENGTH = 32;
  public static final int RECOVERY_INTENT_HEADER_LENGTH = 768;
  public static final int RECOVERY_INTENT_LANE_RECORD_LENGTH = 160;
  public static final int RECOVERY_INTENT_TRAILER_LENGTH = 48;
  public static final int RECOVERY_INTENT_TOTAL_LENGTH = 1_616;
  public static final int PAYLOAD_HEADER_LENGTH = 32;
  public static final int SECTION_HEADER_LENGTH = 80;
  public static final int VARIABLE_INDEX_RECORD_BASE_LENGTH = 16;

  // Legacy U0 descriptor fields. They are not five-lane rotation rules.
  static final int U0_SEGMENT_BLOCK_SPAN = 16_384;
  static final long U0_PART_MAX_BYTES = 2_000_000_000L;
  public static final int MAX_BLOCK_FRAME_BYTES = 67_108_864;
  public static final long SEGMENT_TARGET_BYTES = 2_000_000_000L;
  public static final int SHARD_MAX_SEGMENTS = 1_024;
  public static final long EXACT_COVERAGE_BITMAP = 0x0000000007ffffffL;
  public static final long MIXED_LANE_COVERAGE_BITMAP = 0x0000000007dfefe7L;
  private static final int[] FIVE_LANE_ID_VALUES = {0, 4, 5, 13, 22};
  public static final int STORE_COUNT = 27;
  public static final int HASH_LENGTH = 32;

  static final byte[] FORMAT_DOMAIN = ascii("TRON-STATE-ARCHIVE-FORMAT-V3\0");
  static final byte[] STORE_DESCRIPTOR_DOMAIN =
      ascii("TRON-STATE-ARCHIVE-STORE-DESCRIPTOR-V3\0");
  static final byte[] FIVE_LANE_DESCRIPTOR_DOMAIN =
      ascii("TRON-STATE-ARCHIVE-FIVE-LANE-DESCRIPTOR-V3\0");
  static final byte[] SECTION_DOMAIN = ascii("TRON-STATE-ARCHIVE-SECTION-V3\0");
  static final byte[] PAYLOAD_DOMAIN = ascii("TRON-STATE-ARCHIVE-PAYLOAD-V3\0");
  static final byte[] BLOCK_DOMAIN = ascii("TRON-STATE-ARCHIVE-BLOCK-V3\0");
  static final byte[] ROLLING_DOMAIN = ascii("TRON-STATE-ARCHIVE-ROLLING-V3\0");
  static final byte[] ENCODED_BLOCK_DOMAIN =
      ascii("TRON-STATE-ARCHIVE-ENCODED-BLOCK-V3\0");
  static final byte[] FIXED_SECTION_DOMAIN =
      ascii("TRON-STATE-ARCHIVE-FIXED-SECTION-V3\0");
  static final byte[] LANE_ITEM_DOMAIN =
      ascii("TRON-STATE-ARCHIVE-LANE-ITEM-V3\0");
  static final byte[] BUNDLE_BLOCK_DOMAIN =
      ascii("TRON-STATE-ARCHIVE-BUNDLE-BLOCK-V3\0");
  static final byte[] PART_HEADER_DOMAIN =
      ascii("TRON-STATE-ARCHIVE-PART-HEADER-V3\0");
  static final byte[] SEGMENT_LAYOUT_DOMAIN =
      ascii("TRON-STATE-ARCHIVE-SEGMENT-LAYOUT-V3\0");
  static final byte[] COMPOSITE_FORMAT_DOMAIN =
      ascii("TRON-STATE-ARCHIVE-COMPOSITE-FORMAT-V3\0");
  static final byte[] SEGMENT_HEADER_DOMAIN =
      ascii("TRON-STATE-ARCHIVE-SEGMENT-HEADER-V3\0");
  static final byte[] SEGMENT_CONTENT_DOMAIN =
      ascii("TRON-STATE-ARCHIVE-SEGMENT-CONTENT-V3\0");
  static final byte[] ENCODED_SEGMENT_SEAL_DOMAIN =
      ascii("TRON-STATE-ARCHIVE-ENCODED-SEGMENT-SEAL-V3\0");
  static final byte[] ENCODED_MARKER_DOMAIN =
      ascii("TRON-STATE-ARCHIVE-ENCODED-MARKER-V3\0");
  static final byte[] SEGMENT_CHAIN_DOMAIN =
      ascii("TRON-STATE-ARCHIVE-SEGMENT-CHAIN-V3\0");
  static final byte[] LANE_SEGMENT_BASELINE_DOMAIN =
      ascii("TRON-STATE-ARCHIVE-LANE-SEGMENT-BASELINE-V3\0");
  static final byte[] BLOCK_INDEX_HEADER_DOMAIN =
      ascii("TRON-STATE-ARCHIVE-BLOCK-INDEX-HEADER-V3\0");
  static final byte[] RECOVERY_INTENT_LAYOUT_DOMAIN =
      ascii("TRON-STATE-ARCHIVE-RECOVERY-INTENT-LAYOUT-V3\0");
  static final byte[] RECOVERY_INTENT_RECORDS_DOMAIN =
      ascii("TRON-STATE-ARCHIVE-RECOVERY-INTENT-RECORDS-V3\0");
  static final byte[] RECOVERY_INTENT_HEADER_DOMAIN =
      ascii("TRON-STATE-ARCHIVE-RECOVERY-INTENT-HEADER-V3\0");
  static final byte[] RECOVERY_INTENT_DOMAIN =
      ascii("TRON-STATE-ARCHIVE-RECOVERY-INTENT-V3\0");
  static final byte[] RECOVERY_DATA_PREFIX_DOMAIN =
      ascii("TRON-STATE-ARCHIVE-RECOVERY-DATA-PREFIX-V3\0");
  static final byte[] RECOVERY_INDEX_FILE_DOMAIN =
      ascii("TRON-STATE-ARCHIVE-RECOVERY-INDEX-FILE-V3\0");

  private static final byte[] FORMAT_DESCRIPTOR = buildFormatDescriptor();
  private static final byte[] FORMAT_DIGEST = sha256(FORMAT_DOMAIN, FORMAT_DESCRIPTOR);
  private static final byte[] STORE_DESCRIPTOR_DIGEST = buildStoreDescriptorDigest();
  private static final byte[] FIVE_LANE_DESCRIPTOR_DIGEST =
      buildFiveLaneDescriptorDigest();
  private static final byte[] SEGMENT_LAYOUT_DESCRIPTOR = buildSegmentLayoutDescriptor();
  private static final byte[] SEGMENT_LAYOUT_DIGEST = sha256(SEGMENT_LAYOUT_DOMAIN,
      SEGMENT_LAYOUT_DESCRIPTOR);
  private static final byte[] RECOVERY_INTENT_LAYOUT_DESCRIPTOR =
      buildRecoveryIntentLayoutDescriptor();
  private static final byte[] RECOVERY_INTENT_LAYOUT_DIGEST = sha256(
      RECOVERY_INTENT_LAYOUT_DOMAIN, RECOVERY_INTENT_LAYOUT_DESCRIPTOR);
  private static final byte[] COMPOSITE_FORMAT_DIGEST = sha256(COMPOSITE_FORMAT_DOMAIN,
      FORMAT_DIGEST, SEGMENT_LAYOUT_DIGEST, RECOVERY_INTENT_LAYOUT_DIGEST);

  private StateArchiveFileFormatV3() {
  }

  public static byte[] formatDescriptor() {
    return Arrays.copyOf(FORMAT_DESCRIPTOR, FORMAT_DESCRIPTOR.length);
  }

  public static byte[] formatDigest() {
    return Arrays.copyOf(FORMAT_DIGEST, FORMAT_DIGEST.length);
  }

  public static byte[] storeDescriptorDigest() {
    return Arrays.copyOf(STORE_DESCRIPTOR_DIGEST, STORE_DESCRIPTOR_DIGEST.length);
  }

  public static byte[] fiveLaneDescriptorDigest() {
    return Arrays.copyOf(FIVE_LANE_DESCRIPTOR_DIGEST,
        FIVE_LANE_DESCRIPTOR_DIGEST.length);
  }

  public static byte[] segmentLayoutDescriptor() {
    return Arrays.copyOf(SEGMENT_LAYOUT_DESCRIPTOR, SEGMENT_LAYOUT_DESCRIPTOR.length);
  }

  public static byte[] segmentLayoutDigest() {
    return Arrays.copyOf(SEGMENT_LAYOUT_DIGEST, SEGMENT_LAYOUT_DIGEST.length);
  }

  public static byte[] recoveryIntentLayoutDescriptor() {
    return Arrays.copyOf(RECOVERY_INTENT_LAYOUT_DESCRIPTOR,
        RECOVERY_INTENT_LAYOUT_DESCRIPTOR.length);
  }

  public static byte[] recoveryIntentLayoutDigest() {
    return Arrays.copyOf(RECOVERY_INTENT_LAYOUT_DIGEST,
        RECOVERY_INTENT_LAYOUT_DIGEST.length);
  }

  public static byte[] compositeFormatDigest() {
    return Arrays.copyOf(COMPOSITE_FORMAT_DIGEST, COMPOSITE_FORMAT_DIGEST.length);
  }

  static int[] fiveLaneIds() {
    return Arrays.copyOf(FIVE_LANE_ID_VALUES, FIVE_LANE_ID_VALUES.length);
  }

  static int laneId(int storeId) {
    requireStoreId(storeId);
    switch (storeId) {
      case 4:
      case 5:
      case 13:
      case 22:
        return storeId;
      default:
        return 0;
    }
  }

  static long laneCoverage(int laneId) {
    if (laneId == 0) {
      return MIXED_LANE_COVERAGE_BITMAP;
    }
    requireDedicatedLane(laneId);
    return 1L << (laneId - 1);
  }

  static short laneKind(int laneId) {
    if (laneId == 0) {
      return 1;
    }
    requireDedicatedLane(laneId);
    return 2;
  }

  static short laneBodyCodec(int laneId) {
    if (laneId == 4 || laneId == 22) {
      return DEDICATED_FIXED_WIDTH_BODY_CODEC_ID;
    }
    if (laneId == 0 || laneId == 5 || laneId == 13) {
      return LANE_VARIABLE_BODY_CODEC_ID;
    }
    throw new IllegalArgumentException("Unknown State Archive lane ID: " + laneId);
  }

  static int fixedKeyWidth(int laneId) {
    if (laneId == 4) {
      return 21;
    }
    if (laneId == 22) {
      return 32;
    }
    return 0;
  }

  static int storeId(String dbName) {
    int storeId = ArchiveParticipantDescriptor.current().getStoreId(dbName);
    if (storeId < 1 || storeId > STORE_COUNT) {
      throw new IllegalArgumentException("Inactive archive Store: " + dbName);
    }
    return storeId;
  }

  static String dbName(int storeId) {
    String dbName = ArchiveParticipantDescriptor.current().getActiveDatabases().stream()
        .filter(candidate -> ArchiveParticipantDescriptor.current().getStoreId(candidate)
            == storeId)
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unknown archive Store ID: " + storeId));
    return dbName;
  }

  static void requireExactCapture(Collection<String> dbNames) {
    ArchiveParticipantDescriptor.current().requireExactParticipants(dbNames);
  }

  static byte[] sha256(byte[]... values) {
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException impossible) {
      throw new IllegalStateException("SHA-256 is unavailable", impossible);
    }
    for (byte[] value : values) {
      digest.update(value);
    }
    return digest.digest();
  }

  private static byte[] buildFormatDescriptor() {
    ByteBuffer buffer = ByteBuffer.allocate(FORMAT_DESCRIPTOR_LENGTH);
    buffer.putInt(0x464d5433);
    buffer.putShort(MAJOR_VERSION);
    buffer.putShort(MINOR_VERSION);
    buffer.putInt(FORMAT_DESCRIPTOR_LENGTH);
    buffer.putShort((short) BLOCK_HEADER_LENGTH);
    buffer.putShort((short) MARKER_HEADER_LENGTH);
    buffer.putShort((short) SEAL_HEADER_LENGTH);
    buffer.putShort((short) FRAME_TRAILER_LENGTH);
    buffer.putShort((short) PART_HEADER_LENGTH);
    buffer.putShort((short) BLOCK_INDEX_HEADER_LENGTH);
    buffer.putShort((short) BLOCK_INDEX_ENTRY_LENGTH);
    buffer.putShort((short) MANIFEST_HEADER_LENGTH);
    buffer.putShort((short) MANIFEST_PART_RECORD_LENGTH);
    buffer.putShort((short) MANIFEST_TRAILER_LENGTH);
    buffer.putShort((short) PAYLOAD_HEADER_LENGTH);
    buffer.putShort((short) SECTION_HEADER_LENGTH);
    buffer.putShort((short) VARIABLE_INDEX_RECORD_BASE_LENGTH);
    buffer.putShort(LANE_KIND);
    buffer.putShort(LANE_ID);
    buffer.putShort(BODY_CODEC_ID);
    buffer.putShort((short) 0x0003);
    buffer.putShort(KEY_ORDER_ID);
    buffer.putShort(DIGEST_ID);
    buffer.putShort(CHECKSUM_ID);
    buffer.putShort(IDENTITY_KIND);
    buffer.putInt(U0_SEGMENT_BLOCK_SPAN);
    buffer.putLong(U0_PART_MAX_BYTES);
    buffer.putLong(MAX_BLOCK_FRAME_BYTES);
    buffer.putLong(EXACT_COVERAGE_BITMAP);
    buffer.put(new byte[14]);
    if (buffer.hasRemaining()) {
      throw new IllegalStateException("Invalid State Archive format descriptor length");
    }
    return buffer.array();
  }

  private static byte[] buildStoreDescriptorDigest() {
    ArchiveParticipantDescriptor descriptor = ArchiveParticipantDescriptor.current();
    int encodedLength = STORE_DESCRIPTOR_DOMAIN.length + HASH_LENGTH + Short.BYTES;
    for (int storeId = 1; storeId <= STORE_COUNT; storeId++) {
      encodedLength += 2 + 1 + 1 + 2
          + descriptorName(descriptor, storeId).getBytes(StandardCharsets.UTF_8).length;
    }
    ByteBuffer buffer = ByteBuffer.allocate(encodedLength);
    buffer.put(STORE_DESCRIPTOR_DOMAIN);
    buffer.put(FORMAT_DIGEST);
    buffer.putShort((short) STORE_COUNT);
    for (int storeId = 1; storeId <= STORE_COUNT; storeId++) {
      byte[] name = descriptorName(descriptor, storeId).getBytes(StandardCharsets.UTF_8);
      if (name.length > 0xffff) {
        throw new IllegalStateException("Archive Store name is too long");
      }
      buffer.putShort((short) storeId);
      buffer.put((byte) 1);
      buffer.put((byte) 0);
      buffer.putShort((short) name.length);
      buffer.put(name);
    }
    return sha256(buffer.array());
  }

  private static byte[] buildSegmentLayoutDescriptor() {
    ByteBuffer buffer = ByteBuffer.allocate(SEGMENT_LAYOUT_DESCRIPTOR_LENGTH);
    buffer.putInt(0x534c4433);
    buffer.putShort(MAJOR_VERSION);
    buffer.putShort(MINOR_VERSION);
    buffer.putInt(SEGMENT_LAYOUT_DESCRIPTOR_LENGTH);
    buffer.putShort(SEGMENT_LAYOUT_ID);
    buffer.putShort((short) PART_HEADER_LENGTH);
    buffer.putShort((short) SEAL_HEADER_LENGTH);
    buffer.putShort((short) BLOCK_INDEX_HEADER_LENGTH);
    buffer.putShort((short) BLOCK_INDEX_ENTRY_LENGTH);
    buffer.putShort((short) MANIFEST_HEADER_LENGTH);
    buffer.putShort((short) MANIFEST_TRAILER_LENGTH);
    buffer.putShort((short) SEGMENT_MAP_ENTRY_LENGTH);
    buffer.putLong(SEGMENT_TARGET_BYTES);
    buffer.putLong(MAX_BLOCK_FRAME_BYTES);
    buffer.putInt(SHARD_MAX_SEGMENTS);
    buffer.putShort(SIZE_ROLLED_AFTER_TARGET_ROTATION_ID);
    buffer.putShort(SEGMENT_OVERSHOOT_POLICY_ID);
    buffer.put(new byte[12]);
    if (buffer.hasRemaining()) {
      throw new IllegalStateException("Invalid State Archive segment layout length");
    }
    return buffer.array();
  }

  private static byte[] buildRecoveryIntentLayoutDescriptor() {
    ByteBuffer buffer = ByteBuffer.allocate(RECOVERY_INTENT_LAYOUT_DESCRIPTOR_LENGTH);
    buffer.putInt(0x52494433);
    buffer.putShort(MAJOR_VERSION);
    buffer.putShort(MINOR_VERSION);
    buffer.putInt(RECOVERY_INTENT_LAYOUT_DESCRIPTOR_LENGTH);
    buffer.putShort((short) RECOVERY_INTENT_HEADER_LENGTH);
    buffer.putShort((short) RECOVERY_INTENT_LANE_RECORD_LENGTH);
    buffer.putShort((short) FIVE_LANE_ID_VALUES.length);
    buffer.putShort((short) RECOVERY_INTENT_TRAILER_LENGTH);
    buffer.putInt(RECOVERY_INTENT_TOTAL_LENGTH);
    buffer.putShort(RECOVERY_INTENT_ACTION_SCHEMA_ID);
    buffer.putShort(DIGEST_ID);
    buffer.putShort(CHECKSUM_ID);
    buffer.putShort(RECOVERY_POINT_SCHEMA_ID);
    if (buffer.hasRemaining()) {
      throw new IllegalStateException("Invalid State Archive recovery intent layout length");
    }
    return buffer.array();
  }

  private static byte[] buildFiveLaneDescriptorDigest() {
    ArchiveParticipantDescriptor descriptor = ArchiveParticipantDescriptor.current();
    int encodedLength = FIVE_LANE_DESCRIPTOR_DOMAIN.length + HASH_LENGTH
        + 3 * Short.BYTES + FIVE_LANE_ID_VALUES.length * 24 + Short.BYTES;
    for (int storeId = 1; storeId <= STORE_COUNT; storeId++) {
      encodedLength += 14
          + descriptorName(descriptor, storeId).getBytes(StandardCharsets.UTF_8).length;
    }
    ByteBuffer buffer = ByteBuffer.allocate(encodedLength);
    buffer.put(FIVE_LANE_DESCRIPTOR_DOMAIN);
    buffer.put(FORMAT_DIGEST);
    buffer.putShort((short) 1);
    buffer.putShort(SIZE_ROLLED_AFTER_TARGET_ROTATION_ID);
    buffer.putShort((short) FIVE_LANE_ID_VALUES.length);
    long union = 0;
    for (int laneId : FIVE_LANE_ID_VALUES) {
      long coverage = laneCoverage(laneId);
      if ((union & coverage) != 0) {
        throw new IllegalStateException("Overlapping State Archive lane coverage");
      }
      union |= coverage;
      buffer.putShort((short) laneId);
      buffer.putShort(laneKind(laneId));
      buffer.putShort(laneBodyCodec(laneId));
      buffer.putShort((short) 0);
      buffer.putLong(coverage);
      buffer.putInt(fixedKeyWidth(laneId));
      buffer.putInt(0);
    }
    if (union != EXACT_COVERAGE_BITMAP) {
      throw new IllegalStateException("Incomplete State Archive lane coverage");
    }
    buffer.putShort((short) STORE_COUNT);
    for (int storeId = 1; storeId <= STORE_COUNT; storeId++) {
      int laneId = laneId(storeId);
      int keyWidth = fixedKeyWidth(laneId);
      byte[] name = descriptorName(descriptor, storeId).getBytes(StandardCharsets.UTF_8);
      buffer.putShort((short) storeId);
      buffer.put((byte) 1);
      buffer.put((byte) 0);
      buffer.putShort((short) laneId);
      buffer.putShort((short) (keyWidth == 0 ? 1 : 2));
      buffer.putInt(keyWidth);
      buffer.putShort((short) name.length);
      buffer.put(name);
    }
    if (buffer.hasRemaining()) {
      throw new IllegalStateException("Invalid five-lane descriptor length");
    }
    return sha256(buffer.array());
  }

  private static void requireStoreId(int storeId) {
    if (storeId < 1 || storeId > STORE_COUNT) {
      throw new IllegalArgumentException("Unknown archive Store ID: " + storeId);
    }
  }

  private static void requireDedicatedLane(int laneId) {
    if (laneId != 4 && laneId != 5 && laneId != 13 && laneId != 22) {
      throw new IllegalArgumentException("Unknown State Archive lane ID: " + laneId);
    }
  }

  private static String descriptorName(ArchiveParticipantDescriptor descriptor, int storeId) {
    for (String dbName : descriptor.getActiveDatabases()) {
      if (descriptor.getStoreId(dbName) == storeId) {
        return dbName;
      }
    }
    throw new IllegalStateException("Missing active archive Store ID: " + storeId);
  }

  static byte[] ascii(String value) {
    return value.getBytes(StandardCharsets.US_ASCII);
  }
}
