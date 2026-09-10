package org.tron.core.db2.archive;

import com.google.common.hash.Hashing;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Byte-exact crash-recovery plan for five-lane open-tail repair. */
public final class StateArchiveFiveLaneRecoveryIntentV3 {

  public static final String FILE_NAME = "five-lane-recovery.intent";
  public static final String TEMP_FILE_NAME = "five-lane-recovery.intent.tmp";
  public static final int DATA_TRUNCATE = 1;
  public static final int DELETE_PAIR = 1 << 1;
  public static final int INDEX_REPLACE = 1 << 2;
  public static final int ORIGINAL_INDEX_MISSING = 1 << 3;
  public static final int SOURCE_PAIR_MISSING = 1 << 4;
  public static final long NO_TARGET_SEGMENT = -1L;

  private static final int KNOWN_FLAGS = DATA_TRUNCATE | DELETE_PAIR
      | INDEX_REPLACE | ORIGINAL_INDEX_MISSING | SOURCE_PAIR_MISSING;
  private static final int COMMON_POINT_PRESENT = 1;
  private static final int TARGET_POINT_PRESENT = 1 << 1;
  private static final int KNOWN_HEADER_FLAGS = COMMON_POINT_PRESENT | TARGET_POINT_PRESENT;
  private static final int HEADER_DIGEST_OFFSET = 732;
  private static final int HEADER_CRC_OFFSET = 764;
  private static final int INTENT_DIGEST_OFFSET = 1_568;
  private static final int INTENT_CRC_OFFSET = 1_608;

  private StateArchiveFiveLaneRecoveryIntentV3() {
  }

  public static byte[] dataPrefixDigest(byte[] exactTargetPrefix) {
    return StateArchiveFileFormatV3.sha256(
        StateArchiveFileFormatV3.RECOVERY_DATA_PREFIX_DOMAIN,
        Objects.requireNonNull(exactTargetPrefix, "exactTargetPrefix"));
  }

  public static byte[] indexFileDigest(byte[] exactTargetIndex) {
    return StateArchiveFileFormatV3.sha256(
        StateArchiveFileFormatV3.RECOVERY_INDEX_FILE_DOMAIN,
        Objects.requireNonNull(exactTargetIndex, "exactTargetIndex"));
  }

  public static byte[] encode(Intent intent) {
    Objects.requireNonNull(intent, "intent");
    intent.validate();
    byte[] records = encodeRecords(intent.lanes);
    byte[] recordsDigest = StateArchiveFileFormatV3.sha256(
        StateArchiveFileFormatV3.RECOVERY_INTENT_RECORDS_DOMAIN, records);
    ByteBuffer bytes = ByteBuffer.allocate(
        StateArchiveFileFormatV3.RECOVERY_INTENT_TOTAL_LENGTH);
    bytes.putInt(StateArchiveFileFormatV3.RECOVERY_INTENT_MAGIC);
    bytes.putShort(StateArchiveFileFormatV3.MAJOR_VERSION);
    bytes.putShort(StateArchiveFileFormatV3.MINOR_VERSION);
    bytes.putInt(StateArchiveFileFormatV3.RECOVERY_INTENT_HEADER_LENGTH);
    bytes.putShort((short) StateArchiveFileFormatV3.RECOVERY_INTENT_LANE_RECORD_LENGTH);
    bytes.putShort((short) StateArchiveFileFormatV3.fiveLaneIds().length);
    bytes.putLong(StateArchiveFileFormatV3.RECOVERY_INTENT_TOTAL_LENGTH);
    bytes.putShort(StateArchiveFileFormatV3.RECOVERY_INTENT_ACTION_SCHEMA_ID);
    int flags = (intent.commonCommitted == null ? 0 : COMMON_POINT_PRESENT)
        | (intent.target == null ? 0 : TARGET_POINT_PRESENT);
    bytes.putShort((short) flags);
    bytes.putInt(0);
    bytes.put(StateArchiveFileFormatV3.compositeFormatDigest());
    bytes.put(StateArchiveFileFormatV3.fiveLaneDescriptorDigest());
    bytes.put(intent.baselineHistoryDigest);
    putPoint(bytes, intent.authorizedCeiling);
    putPointOrZero(bytes, intent.commonCommitted);
    putPointOrZero(bytes, intent.target);
    bytes.put(recordsDigest);
    bytes.put(new byte[212]);
    if (bytes.position() != HEADER_DIGEST_OFFSET) {
      throw new IllegalStateException("Invalid State Archive recovery intent header layout");
    }
    bytes.put(StateArchiveFileFormatV3.sha256(
        StateArchiveFileFormatV3.RECOVERY_INTENT_HEADER_DOMAIN,
        Arrays.copyOf(bytes.array(), HEADER_DIGEST_OFFSET)));
    bytes.putInt(crc32c(bytes.array(), 0, HEADER_CRC_OFFSET));
    bytes.put(records);
    if (bytes.position() != INTENT_DIGEST_OFFSET) {
      throw new IllegalStateException("Invalid State Archive recovery intent record layout");
    }
    bytes.put(StateArchiveFileFormatV3.sha256(
        StateArchiveFileFormatV3.RECOVERY_INTENT_DOMAIN,
        Arrays.copyOf(bytes.array(), INTENT_DIGEST_OFFSET)));
    bytes.putLong(StateArchiveFileFormatV3.RECOVERY_INTENT_TOTAL_LENGTH);
    bytes.putInt(crc32c(bytes.array(), 0, INTENT_CRC_OFFSET));
    bytes.putInt(StateArchiveFileFormatV3.RECOVERY_INTENT_TRAILER_MAGIC);
    if (bytes.hasRemaining()) {
      throw new IllegalStateException("Invalid State Archive recovery intent length");
    }
    return bytes.array();
  }

  public static Intent decode(byte[] encoded) {
    requireLength(encoded, StateArchiveFileFormatV3.RECOVERY_INTENT_TOTAL_LENGTH,
        "recovery intent");
    ByteBuffer bytes = ByteBuffer.wrap(encoded);
    requireInt(bytes, StateArchiveFileFormatV3.RECOVERY_INTENT_MAGIC, "intent magic");
    requireShort(bytes, StateArchiveFileFormatV3.MAJOR_VERSION, "intent major version");
    requireShort(bytes, StateArchiveFileFormatV3.MINOR_VERSION, "intent minor version");
    requireInt(bytes, StateArchiveFileFormatV3.RECOVERY_INTENT_HEADER_LENGTH,
        "intent header length");
    requireShort(bytes, (short) StateArchiveFileFormatV3.RECOVERY_INTENT_LANE_RECORD_LENGTH,
        "intent record length");
    requireShort(bytes, (short) StateArchiveFileFormatV3.fiveLaneIds().length,
        "intent lane count");
    requireLong(bytes, StateArchiveFileFormatV3.RECOVERY_INTENT_TOTAL_LENGTH,
        "intent total length");
    requireShort(bytes, StateArchiveFileFormatV3.RECOVERY_INTENT_ACTION_SCHEMA_ID,
        "intent action schema");
    int flags = Short.toUnsignedInt(bytes.getShort());
    if ((flags & ~KNOWN_HEADER_FLAGS) != 0
        || (flags & COMMON_POINT_PRESENT) != 0 && (flags & TARGET_POINT_PRESENT) == 0) {
      throw new IllegalArgumentException("State Archive recovery intent flags mismatch");
    }
    requireInt(bytes, 0, "intent reserved field");
    requireArray(getBytes(bytes, 32), StateArchiveFileFormatV3.compositeFormatDigest(),
        "intent composite format");
    requireArray(getBytes(bytes, 32), StateArchiveFileFormatV3.fiveLaneDescriptorDigest(),
        "intent placement descriptor");
    byte[] baselineHistoryDigest = getBytes(bytes, 32);
    RecoveryPoint authorized = readPoint(bytes);
    RecoveryPoint common = readPointOrZero(bytes, (flags & COMMON_POINT_PRESENT) != 0);
    RecoveryPoint target = readPointOrZero(bytes, (flags & TARGET_POINT_PRESENT) != 0);
    byte[] recordsDigest = getBytes(bytes, 32);
    requireZero(bytes, 212, "intent header reserved bytes");
    byte[] headerDigest = getBytes(bytes, 32);
    requireArray(headerDigest, StateArchiveFileFormatV3.sha256(
        StateArchiveFileFormatV3.RECOVERY_INTENT_HEADER_DOMAIN,
        Arrays.copyOf(encoded, HEADER_DIGEST_OFFSET)), "intent header digest");
    if (bytes.getInt() != crc32c(encoded, 0, HEADER_CRC_OFFSET)) {
      throw new IllegalArgumentException("State Archive recovery intent header checksum mismatch");
    }
    byte[] recordBytes = getBytes(bytes, StateArchiveFileFormatV3.fiveLaneIds().length
        * StateArchiveFileFormatV3.RECOVERY_INTENT_LANE_RECORD_LENGTH);
    requireArray(recordsDigest, StateArchiveFileFormatV3.sha256(
        StateArchiveFileFormatV3.RECOVERY_INTENT_RECORDS_DOMAIN, recordBytes),
        "intent records digest");
    List<LaneTarget> lanes = decodeRecords(recordBytes);
    byte[] intentDigest = getBytes(bytes, 32);
    requireArray(intentDigest, StateArchiveFileFormatV3.sha256(
        StateArchiveFileFormatV3.RECOVERY_INTENT_DOMAIN,
        Arrays.copyOf(encoded, INTENT_DIGEST_OFFSET)), "intent digest");
    requireLong(bytes, StateArchiveFileFormatV3.RECOVERY_INTENT_TOTAL_LENGTH,
        "repeated intent total length");
    if (bytes.getInt() != crc32c(encoded, 0, INTENT_CRC_OFFSET)) {
      throw new IllegalArgumentException("State Archive recovery intent checksum mismatch");
    }
    requireInt(bytes, StateArchiveFileFormatV3.RECOVERY_INTENT_TRAILER_MAGIC,
        "intent trailer magic");
    return new Intent(baselineHistoryDigest, authorized, common, target, lanes,
        headerDigest, recordsDigest, intentDigest);
  }

  private static byte[] encodeRecords(List<LaneTarget> lanes) {
    ByteBuffer bytes = ByteBuffer.allocate(lanes.size()
        * StateArchiveFileFormatV3.RECOVERY_INTENT_LANE_RECORD_LENGTH);
    for (LaneTarget lane : lanes) {
      bytes.putShort((short) lane.laneId);
      bytes.putShort((short) lane.actionFlags);
      bytes.putInt(0);
      bytes.putLong(lane.sourceSegmentSeq);
      bytes.putLong(lane.originalDataEnd);
      bytes.putLong(lane.originalIndexEnd);
      bytes.putLong(lane.targetSegmentSeq);
      bytes.putLong(lane.targetDataEnd);
      bytes.putLong(lane.targetIndexEnd);
      bytes.put(lane.sourceSegmentHeaderDigest);
      bytes.put(lane.targetDataPrefixDigest);
      bytes.put(lane.targetIndexFileDigest);
      bytes.putLong(0);
    }
    return bytes.array();
  }

  private static List<LaneTarget> decodeRecords(byte[] encoded) {
    ByteBuffer bytes = ByteBuffer.wrap(encoded);
    List<LaneTarget> lanes = new ArrayList<>();
    while (bytes.hasRemaining()) {
      int laneId = Short.toUnsignedInt(bytes.getShort());
      int actionFlags = Short.toUnsignedInt(bytes.getShort());
      requireInt(bytes, 0, "lane intent reserved field");
      LaneTarget lane = new LaneTarget(laneId, actionFlags,
          bytes.getLong(), bytes.getLong(), bytes.getLong(), bytes.getLong(),
          bytes.getLong(), bytes.getLong(), getBytes(bytes, 32), getBytes(bytes, 32),
          getBytes(bytes, 32));
      requireLong(bytes, 0, "lane intent reserved tail");
      lanes.add(lane);
    }
    return lanes;
  }

  private static void putPoint(ByteBuffer bytes, RecoveryPoint point) {
    bytes.putLong(point.epoch);
    bytes.putLong(point.blockNumber);
    bytes.putLong(point.timestamp);
    bytes.put(point.blockHash);
    bytes.put(point.parentHash);
    bytes.put(point.resultHistoryDigest);
  }

  private static void putPointOrZero(ByteBuffer bytes, RecoveryPoint point) {
    if (point == null) {
      bytes.put(new byte[120]);
    } else {
      putPoint(bytes, point);
    }
  }

  private static RecoveryPoint readPoint(ByteBuffer bytes) {
    return new RecoveryPoint(bytes.getLong(), bytes.getLong(), bytes.getLong(),
        getBytes(bytes, 32), getBytes(bytes, 32), getBytes(bytes, 32));
  }

  private static RecoveryPoint readPointOrZero(ByteBuffer bytes, boolean present) {
    if (present) {
      return readPoint(bytes);
    }
    requireZero(bytes, 120, "absent recovery point");
    return null;
  }

  private static void requireSameIfEqual(RecoveryPoint first, RecoveryPoint second) {
    if (first.blockNumber == second.blockNumber
        && (first.epoch != second.epoch || first.timestamp != second.timestamp
        || !Arrays.equals(first.blockHash, second.blockHash)
        || !Arrays.equals(first.parentHash, second.parentHash)
        || !Arrays.equals(first.resultHistoryDigest, second.resultHistoryDigest))) {
      throw new IllegalArgumentException("State Archive recovery point identity mismatch");
    }
  }

  private static byte[] requireHash(byte[] value, String name) {
    requireLength(value, StateArchiveFileFormatV3.HASH_LENGTH, name);
    return Arrays.copyOf(value, value.length);
  }

  private static void requireLength(byte[] value, int expected, String name) {
    if (value == null || value.length != expected) {
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

  private static boolean isZero(byte[] value) {
    for (byte element : value) {
      if (element != 0) {
        return false;
      }
    }
    return true;
  }

  private static int crc32c(byte[] bytes, int offset, int length) {
    return Hashing.crc32c().hashBytes(bytes, offset, length).asInt();
  }

  public static final class RecoveryPoint {
    private final long epoch;
    private final long blockNumber;
    private final long timestamp;
    private final byte[] blockHash;
    private final byte[] parentHash;
    private final byte[] resultHistoryDigest;

    public RecoveryPoint(long epoch, long blockNumber, long timestamp,
        byte[] blockHash, byte[] parentHash, byte[] resultHistoryDigest) {
      if (epoch < 0 || blockNumber < 0 || timestamp < 0 || epoch != blockNumber) {
        throw new IllegalArgumentException("Invalid State Archive recovery block identity");
      }
      this.epoch = epoch;
      this.blockNumber = blockNumber;
      this.timestamp = timestamp;
      this.blockHash = requireHash(blockHash, "recovery block hash");
      this.parentHash = requireHash(parentHash, "recovery parent hash");
      this.resultHistoryDigest = requireHash(resultHistoryDigest,
          "recovery history digest");
    }

    public long getEpoch() {
      return epoch;
    }

    public long getBlockNumber() {
      return blockNumber;
    }

    public long getTimestamp() {
      return timestamp;
    }

    public byte[] getBlockHash() {
      return Arrays.copyOf(blockHash, blockHash.length);
    }

    public byte[] getParentHash() {
      return Arrays.copyOf(parentHash, parentHash.length);
    }

    public byte[] getResultHistoryDigest() {
      return Arrays.copyOf(resultHistoryDigest, resultHistoryDigest.length);
    }
  }

  public static final class LaneTarget {
    private final int laneId;
    private final int actionFlags;
    private final long sourceSegmentSeq;
    private final long originalDataEnd;
    private final long originalIndexEnd;
    private final long targetSegmentSeq;
    private final long targetDataEnd;
    private final long targetIndexEnd;
    private final byte[] sourceSegmentHeaderDigest;
    private final byte[] targetDataPrefixDigest;
    private final byte[] targetIndexFileDigest;

    public LaneTarget(int laneId, int actionFlags, long sourceSegmentSeq,
        long originalDataEnd, long originalIndexEnd, long targetSegmentSeq,
        long targetDataEnd, long targetIndexEnd, byte[] sourceSegmentHeaderDigest,
        byte[] targetDataPrefixDigest, byte[] targetIndexFileDigest) {
      this.laneId = laneId;
      this.actionFlags = actionFlags;
      this.sourceSegmentSeq = sourceSegmentSeq;
      this.originalDataEnd = originalDataEnd;
      this.originalIndexEnd = originalIndexEnd;
      this.targetSegmentSeq = targetSegmentSeq;
      this.targetDataEnd = targetDataEnd;
      this.targetIndexEnd = targetIndexEnd;
      this.sourceSegmentHeaderDigest = requireHash(sourceSegmentHeaderDigest,
          "intent segment header digest");
      this.targetDataPrefixDigest = requireHash(targetDataPrefixDigest,
          "intent target data digest");
      this.targetIndexFileDigest = requireHash(targetIndexFileDigest,
          "intent target index digest");
      validate();
    }

    private void validate() {
      StateArchiveFileFormatV3.laneKind(laneId);
      if ((actionFlags & ~KNOWN_FLAGS) != 0) {
        throw new IllegalArgumentException("Invalid State Archive lane recovery flags");
      }
      if ((actionFlags & SOURCE_PAIR_MISSING) != 0) {
        if (actionFlags != SOURCE_PAIR_MISSING || sourceSegmentSeq != NO_TARGET_SEGMENT
            || originalDataEnd != 0 || originalIndexEnd != 0
            || targetSegmentSeq != NO_TARGET_SEGMENT || targetDataEnd != 0
            || targetIndexEnd != 0 || !isZero(sourceSegmentHeaderDigest)
            || !isZero(targetDataPrefixDigest) || !isZero(targetIndexFileDigest)) {
          throw new IllegalArgumentException("Invalid State Archive missing lane source");
        }
        return;
      }
      if (sourceSegmentSeq < 0
          || originalDataEnd < StateArchiveFileFormatV3.PART_HEADER_LENGTH
          || originalIndexEnd < 0
          || ((actionFlags & ORIGINAL_INDEX_MISSING) != 0) != (originalIndexEnd == 0)) {
        throw new IllegalArgumentException("Invalid State Archive lane recovery source");
      }
      if ((actionFlags & DELETE_PAIR) != 0) {
        if ((actionFlags & (DATA_TRUNCATE | INDEX_REPLACE)) != 0
            || targetSegmentSeq != NO_TARGET_SEGMENT || targetDataEnd != 0
            || targetIndexEnd != 0 || !isZero(targetDataPrefixDigest)
            || !isZero(targetIndexFileDigest)) {
          throw new IllegalArgumentException("Invalid State Archive lane delete target");
        }
        return;
      }
      if (targetSegmentSeq != sourceSegmentSeq
          || targetDataEnd < StateArchiveFileFormatV3.PART_HEADER_LENGTH
          || targetDataEnd > originalDataEnd
          || ((actionFlags & DATA_TRUNCATE) != 0) != (targetDataEnd < originalDataEnd)
          || targetIndexEnd < StateArchiveFileFormatV3.BLOCK_INDEX_HEADER_LENGTH
          || (targetIndexEnd - StateArchiveFileFormatV3.BLOCK_INDEX_HEADER_LENGTH)
              % StateArchiveFileFormatV3.BLOCK_INDEX_ENTRY_LENGTH != 0
          || ((originalIndexEnd == 0 || originalIndexEnd != targetIndexEnd)
              && (actionFlags & INDEX_REPLACE) == 0)) {
        throw new IllegalArgumentException("Invalid State Archive lane recovery target");
      }
    }

    public int getLaneId() {
      return laneId;
    }

    public int getActionFlags() {
      return actionFlags;
    }

    public long getSourceSegmentSeq() {
      return sourceSegmentSeq;
    }

    public long getOriginalDataEnd() {
      return originalDataEnd;
    }

    public long getOriginalIndexEnd() {
      return originalIndexEnd;
    }

    public long getTargetSegmentSeq() {
      return targetSegmentSeq;
    }

    public long getTargetDataEnd() {
      return targetDataEnd;
    }

    public long getTargetIndexEnd() {
      return targetIndexEnd;
    }

    public byte[] getSourceSegmentHeaderDigest() {
      return Arrays.copyOf(sourceSegmentHeaderDigest, sourceSegmentHeaderDigest.length);
    }

    public byte[] getTargetDataPrefixDigest() {
      return Arrays.copyOf(targetDataPrefixDigest, targetDataPrefixDigest.length);
    }

    public byte[] getTargetIndexFileDigest() {
      return Arrays.copyOf(targetIndexFileDigest, targetIndexFileDigest.length);
    }
  }

  public static final class Intent {
    private final byte[] baselineHistoryDigest;
    private final RecoveryPoint authorizedCeiling;
    private final RecoveryPoint commonCommitted;
    private final RecoveryPoint target;
    private final List<LaneTarget> lanes;
    private final byte[] headerDigest;
    private final byte[] recordsDigest;
    private final byte[] intentDigest;

    public Intent(byte[] baselineHistoryDigest, RecoveryPoint authorizedCeiling,
        RecoveryPoint commonCommitted, RecoveryPoint target, List<LaneTarget> lanes) {
      this(baselineHistoryDigest, authorizedCeiling, commonCommitted, target, lanes,
          null, null, null);
    }

    private Intent(byte[] baselineHistoryDigest, RecoveryPoint authorizedCeiling,
        RecoveryPoint commonCommitted, RecoveryPoint target, List<LaneTarget> lanes,
        byte[] headerDigest, byte[] recordsDigest, byte[] intentDigest) {
      this.baselineHistoryDigest = requireHash(baselineHistoryDigest,
          "intent baseline history digest");
      this.authorizedCeiling = Objects.requireNonNull(authorizedCeiling,
          "authorizedCeiling");
      this.commonCommitted = commonCommitted;
      this.target = target;
      this.lanes = Collections.unmodifiableList(new ArrayList<>(
          Objects.requireNonNull(lanes, "lanes")));
      this.headerDigest = headerDigest == null ? null
          : requireHash(headerDigest, "intent header digest");
      this.recordsDigest = recordsDigest == null ? null
          : requireHash(recordsDigest, "intent records digest");
      this.intentDigest = intentDigest == null ? null
          : requireHash(intentDigest, "intent digest");
      validate();
    }

    private void validate() {
      if (commonCommitted != null && target == null) {
        throw new IllegalArgumentException("Invalid State Archive recovery point order");
      }
      if (target != null && target.blockNumber > authorizedCeiling.blockNumber) {
        throw new IllegalArgumentException("Invalid State Archive recovery point order");
      }
      if (commonCommitted != null && commonCommitted.blockNumber > target.blockNumber) {
        throw new IllegalArgumentException("Invalid State Archive recovery point order");
      }
      if (commonCommitted != null) {
        requireSameIfEqual(commonCommitted, target);
        requireSameIfEqual(commonCommitted, authorizedCeiling);
      }
      if (target != null) {
        requireSameIfEqual(target, authorizedCeiling);
      }
      int[] laneIds = StateArchiveFileFormatV3.fiveLaneIds();
      if (lanes.size() != laneIds.length) {
        throw new IllegalArgumentException("Incomplete State Archive recovery lane plan");
      }
      boolean mutation = false;
      for (int index = 0; index < laneIds.length; index++) {
        LaneTarget lane = Objects.requireNonNull(lanes.get(index), "lane");
        if (lane.laneId != laneIds[index]) {
          throw new IllegalArgumentException("Non-canonical State Archive recovery lane plan");
        }
        mutation |= (lane.actionFlags & (DATA_TRUNCATE | DELETE_PAIR | INDEX_REPLACE)) != 0;
      }
      if (!mutation) {
        throw new IllegalArgumentException("State Archive recovery intent has no mutation");
      }
    }

    public byte[] getBaselineHistoryDigest() {
      return Arrays.copyOf(baselineHistoryDigest, baselineHistoryDigest.length);
    }

    public RecoveryPoint getAuthorizedCeiling() {
      return authorizedCeiling;
    }

    public RecoveryPoint getCommonCommitted() {
      return commonCommitted;
    }

    public RecoveryPoint getTarget() {
      return target;
    }

    public List<LaneTarget> getLanes() {
      return lanes;
    }

    public byte[] getHeaderDigest() {
      return headerDigest == null ? null : Arrays.copyOf(headerDigest, headerDigest.length);
    }

    public byte[] getRecordsDigest() {
      return recordsDigest == null ? null : Arrays.copyOf(recordsDigest, recordsDigest.length);
    }

    public byte[] getIntentDigest() {
      return intentDigest == null ? null : Arrays.copyOf(intentDigest, intentDigest.length);
    }
  }
}
