package org.tron.core.db2.archive;

import com.google.common.hash.Hashing;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import org.tron.core.db2.archive.BlockReverseDiff.DbGroup;
import org.tron.core.db2.archive.BlockReverseDiff.Entry;

/** Canonical codec for one complete State Archive v3 five-lane block bundle. */
public final class StateArchiveFiveLaneBlockCodecV3 {

  private static final long UNSIGNED_INT_MAX = 0xffff_ffffL;
  private static final int FIXED_SECTION_HEADER_LENGTH = 96;
  private static final int TRAILER_CRC_AND_MAGIC_LENGTH = 8;

  /** Validates one exact capture before emitting any lane frame. */
  public EncodedBundle encode(BlockChangeView view, OldValueCollector collector,
      byte[] previousHistoryDigest, short compressionId) {
    Objects.requireNonNull(view, "view");
    Objects.requireNonNull(collector, "collector");
    List<String> captured = new ArrayList<>();
    for (BlockChangeView.DatabaseChanges database : view.getDatabases()) {
      captured.add(database.getDbName());
    }
    StateArchiveFileFormatV3.requireExactCapture(captured);
    BlockReverseDiff diff = Objects.requireNonNull(collector.collect(view), "collected diff");
    if (!view.getMeta().equals(diff.getMeta())) {
      throw new IllegalArgumentException("Collected history block identity changed");
    }
    return encode(diff, previousHistoryDigest, compressionId);
  }

  /** Encodes an already collected reverse diff into exactly five lane frames. */
  EncodedBundle encode(BlockReverseDiff diff, byte[] previousHistoryDigest,
      short compressionId) {
    Objects.requireNonNull(diff, "diff");
    byte[] previousDigest = requireHash(previousHistoryDigest, "previousHistoryDigest");
    requireCompression(compressionId);
    BlockSnapshotMeta meta = requireMeta(diff.getMeta());
    List<StoreGroup> groups = admitAndCanonicalize(diff.getGroups());

    int[] laneIds = StateArchiveFileFormatV3.fiveLaneIds();
    List<LanePayload> payloads = new ArrayList<>(laneIds.length);
    for (int laneId : laneIds) {
      payloads.add(encodeLanePayload(laneId, groups));
    }
    byte[] descriptorDigest = StateArchiveFileFormatV3.fiveLaneDescriptorDigest();
    byte[] blockHistoryDigest = bundleBlockHistoryDigest(meta, descriptorDigest, payloads);
    byte[] resultHistoryDigest = StateArchiveFileFormatV3.sha256(
        StateArchiveFileFormatV3.ROLLING_DOMAIN, previousDigest, blockHistoryDigest);

    List<EncodedLane> lanes = new ArrayList<>(payloads.size());
    for (LanePayload payload : payloads) {
      lanes.add(encodeLaneFrame(meta, previousDigest, descriptorDigest,
          blockHistoryDigest, resultHistoryDigest, payload, compressionId));
    }
    return new EncodedBundle(lanes, blockHistoryDigest, resultHistoryDigest, diff);
  }

  /** Decodes five frames and only returns after verifying the complete bundle. */
  public DecodedBundle decode(List<byte[]> frameBytes) {
    Objects.requireNonNull(frameBytes, "frameBytes");
    int[] laneIds = StateArchiveFileFormatV3.fiveLaneIds();
    if (frameBytes.size() != laneIds.length) {
      throw new IllegalArgumentException("State Archive bundle must contain exactly five frames");
    }
    Map<Integer, DecodedLane> byLane = new HashMap<>();
    for (byte[] frame : frameBytes) {
      DecodedLane lane = decodeLane(frame);
      if (byLane.put(lane.laneId, lane) != null) {
        throw new IllegalArgumentException("Duplicate State Archive lane: " + lane.laneId);
      }
    }
    List<DecodedLane> ordered = new ArrayList<>(frameBytes.size());
    for (int laneId : laneIds) {
      DecodedLane lane = byLane.get(laneId);
      if (lane == null) {
        throw new IllegalArgumentException("Missing State Archive lane: " + laneId);
      }
      ordered.add(lane);
    }
    DecodedLane first = ordered.get(0);
    long coverage = 0;
    List<LanePayload> payloads = new ArrayList<>(ordered.size());
    List<DbGroup> groups = new ArrayList<>();
    for (DecodedLane lane : ordered) {
      requireMetaEquals(first, lane);
      if ((coverage & lane.coverageBitmap) != 0) {
        throw new IllegalArgumentException("Overlapping State Archive lane coverage");
      }
      coverage |= lane.coverageBitmap;
      payloads.add(lane.payload);
      groups.addAll(lane.groups);
    }
    if (coverage != StateArchiveFileFormatV3.EXACT_COVERAGE_BITMAP) {
      throw new IllegalArgumentException("Incomplete State Archive bundle coverage");
    }
    byte[] actualBlockDigest = bundleBlockHistoryDigest(first.meta,
        StateArchiveFileFormatV3.fiveLaneDescriptorDigest(), payloads);
    requireArray(first.blockHistoryDigest, actualBlockDigest, "bundle block digest");
    byte[] actualResultDigest = StateArchiveFileFormatV3.sha256(
        StateArchiveFileFormatV3.ROLLING_DOMAIN, first.previousHistoryDigest,
        actualBlockDigest);
    requireArray(first.resultHistoryDigest, actualResultDigest, "bundle result digest");
    groups.sort(Comparator.comparingInt(group ->
        StateArchiveFileFormatV3.storeId(group.getDbName())));
    BlockReverseDiff diff = new BlockReverseDiff(first.meta, groups);
    return new DecodedBundle(diff, ordered, actualBlockDigest, actualResultDigest);
  }

  private List<StoreGroup> admitAndCanonicalize(Collection<DbGroup> inputGroups) {
    List<StoreGroup> groups = new ArrayList<>();
    Set<Integer> storeIds = new HashSet<>();
    for (DbGroup group : inputGroups) {
      int storeId = StateArchiveFileFormatV3.storeId(group.getDbName());
      if (!storeIds.add(storeId)) {
        throw new IllegalArgumentException("Duplicate archive Store ID: " + storeId);
      }
      if (group.getEntries().isEmpty()) {
        throw new IllegalArgumentException("Changed archive Store section must not be empty");
      }
      int laneId = StateArchiveFileFormatV3.laneId(storeId);
      int keyWidth = StateArchiveFileFormatV3.fixedKeyWidth(laneId);
      byte[] previousKey = null;
      for (Entry entry : group.getEntries()) {
        byte[] key = entry.getKey();
        if (keyWidth != 0 && key.length != keyWidth) {
          throw new IllegalArgumentException("Invalid fixed-width key for Store " + storeId);
        }
        if (previousKey != null && BlockReverseDiff.compareUnsigned(previousKey, key) >= 0) {
          throw new IllegalArgumentException("Archive keys are not strictly sorted");
        }
        previousKey = key;
      }
      groups.add(new StoreGroup(storeId, laneId, group.getEntries()));
    }
    groups.sort(Comparator.comparingInt(group -> group.storeId));
    return groups;
  }

  private LanePayload encodeLanePayload(int laneId, List<StoreGroup> allGroups) {
    List<StoreGroup> groups = new ArrayList<>();
    for (StoreGroup group : allGroups) {
      if (group.laneId == laneId) {
        groups.add(group);
      }
    }
    short bodyCodec = StateArchiveFileFormatV3.laneBodyCodec(laneId);
    if (bodyCodec == StateArchiveFileFormatV3.DEDICATED_FIXED_WIDTH_BODY_CODEC_ID
        && groups.size() > 1) {
      throw new IllegalArgumentException("Fixed-width lane has multiple Store sections");
    }
    List<byte[]> sections = new ArrayList<>(groups.size());
    long entryCount = 0;
    long sectionBytes = 0;
    long changedBitmap = 0;
    for (StoreGroup group : groups) {
      byte[] section = bodyCodec == StateArchiveFileFormatV3.LANE_VARIABLE_BODY_CODEC_ID
          ? encodeVariableSection(group) : encodeFixedSection(group);
      sections.add(section);
      entryCount = checkedAdd(entryCount, group.entries.size());
      sectionBytes = checkedAdd(sectionBytes, section.length);
      changedBitmap |= 1L << (group.storeId - 1);
    }
    long payloadLength = checkedAdd(StateArchiveFileFormatV3.PAYLOAD_HEADER_LENGTH,
        sectionBytes);
    if (payloadLength > StateArchiveFileFormatV3.MAX_BLOCK_FRAME_BYTES) {
      throw new IllegalArgumentException("State Archive lane payload is too large");
    }
    ByteBuffer payload = ByteBuffer.allocate(checkedInt(payloadLength, "payloadLength"));
    payload.putInt(StateArchiveFileFormatV3.PAYLOAD_MAGIC);
    payload.putShort((short) 1);
    payload.putShort((short) 0);
    payload.putInt(StateArchiveFileFormatV3.PAYLOAD_HEADER_LENGTH);
    payload.putInt(groups.size());
    payload.putLong(entryCount);
    payload.putLong(sectionBytes);
    for (byte[] section : sections) {
      payload.put(section);
    }
    byte[] bytes = payload.array();
    byte[] payloadDigest = StateArchiveFileFormatV3.sha256(
        StateArchiveFileFormatV3.PAYLOAD_DOMAIN, bytes);
    return new LanePayload(laneId, StateArchiveFileFormatV3.laneKind(laneId),
        bodyCodec, StateArchiveFileFormatV3.laneCoverage(laneId), changedBitmap,
        groups.size(), entryCount, bytes, payloadDigest);
  }

  private byte[] encodeVariableSection(StoreGroup group) {
    int entryCount = group.entries.size();
    ByteBuffer offsets = ByteBuffer.allocate(checkedInt(
        checkedMultiply(entryCount + 1L, Integer.BYTES), "offsetVectorLength"));
    ByteArrayOutputStream records = new ByteArrayOutputStream();
    ByteArrayOutputStream values = new ByteArrayOutputStream();
    try (DataOutputStream output = new DataOutputStream(records)) {
      for (Entry entry : group.entries) {
        offsets.putInt(records.size());
        byte[] key = entry.getKey();
        output.writeInt(key.length);
        writeValueLocator(output, values, entry.getOldValue());
        output.write(key);
      }
      output.flush();
    } catch (IOException impossible) {
      throw new IllegalStateException("Unexpected variable section encoding failure", impossible);
    }
    offsets.putInt(records.size());
    byte[] offsetBytes = offsets.array();
    byte[] recordBytes = records.toByteArray();
    byte[] valueBytes = values.toByteArray();
    long sectionLength = checkedAdd(StateArchiveFileFormatV3.SECTION_HEADER_LENGTH,
        offsetBytes.length, recordBytes.length, valueBytes.length);
    byte[] digest = StateArchiveFileFormatV3.sha256(
        StateArchiveFileFormatV3.SECTION_DOMAIN,
        ByteBuffer.allocate(Short.BYTES + Integer.BYTES)
            .putShort((short) group.storeId).putInt(entryCount).array(),
        offsetBytes, recordBytes, valueBytes);
    ByteBuffer section = ByteBuffer.allocate(checkedInt(sectionLength, "sectionLength"));
    section.putShort((short) group.storeId);
    section.putShort((short) 1);
    section.putInt(StateArchiveFileFormatV3.SECTION_HEADER_LENGTH);
    section.putInt(entryCount);
    section.putInt(0);
    section.putLong(offsetBytes.length);
    section.putLong(recordBytes.length);
    section.putLong(valueBytes.length);
    section.putLong(sectionLength);
    section.put(digest);
    section.put(offsetBytes);
    section.put(recordBytes);
    section.put(valueBytes);
    return section.array();
  }

  private byte[] encodeFixedSection(StoreGroup group) {
    int keyWidth = StateArchiveFileFormatV3.fixedKeyWidth(group.laneId);
    int count = group.entries.size();
    byte[] keys = new byte[checkedInt(checkedMultiply(count, keyWidth), "keyDataLength")];
    byte[] presence = new byte[(count + 7) / 8];
    ByteBuffer ends = ByteBuffer.allocate(checkedInt(
        checkedMultiply(count + 1L, Integer.BYTES), "valueEndVectorLength"));
    ByteArrayOutputStream values = new ByteArrayOutputStream();
    ends.putInt(0);
    for (int index = 0; index < count; index++) {
      Entry entry = group.entries.get(index);
      byte[] key = entry.getKey();
      System.arraycopy(key, 0, keys, index * keyWidth, keyWidth);
      if (entry.getOldValue().isPresent()) {
        presence[index / 8] |= (byte) (1 << (index % 8));
        byte[] value = entry.getOldValue().getValue();
        values.write(value, 0, value.length);
      }
      ends.putInt(values.size());
    }
    byte[] endBytes = ends.array();
    byte[] valueBytes = values.toByteArray();
    long sectionLength = checkedAdd(FIXED_SECTION_HEADER_LENGTH, keys.length,
        presence.length, endBytes.length, valueBytes.length);
    byte[] digest = StateArchiveFileFormatV3.sha256(
        StateArchiveFileFormatV3.FIXED_SECTION_DOMAIN,
        ByteBuffer.allocate(Short.BYTES + 2 * Integer.BYTES)
            .putShort((short) group.storeId).putInt(count).putInt(keyWidth).array(),
        keys, presence, endBytes, valueBytes);
    ByteBuffer section = ByteBuffer.allocate(checkedInt(sectionLength, "sectionLength"));
    section.putShort((short) group.storeId);
    section.putShort((short) 2);
    section.putInt(FIXED_SECTION_HEADER_LENGTH);
    section.putInt(count);
    section.putInt(0);
    section.putInt(keyWidth);
    section.putInt(0);
    section.putLong(keys.length);
    section.putLong(presence.length);
    section.putLong(endBytes.length);
    section.putLong(valueBytes.length);
    section.putLong(sectionLength);
    section.put(digest);
    section.put(keys);
    section.put(presence);
    section.put(endBytes);
    section.put(valueBytes);
    return section.array();
  }

  private void writeValueLocator(DataOutputStream output, ByteArrayOutputStream values,
      OldValue oldValue) throws IOException {
    output.writeByte(oldValue.isPresent() ? 1 : 0);
    output.write(new byte[3]);
    if (oldValue.isPresent()) {
      byte[] value = oldValue.getValue();
      output.writeInt(values.size());
      output.writeInt(value.length);
      values.write(value);
    } else {
      output.writeInt(-1);
      output.writeInt(-1);
    }
  }

  private EncodedLane encodeLaneFrame(BlockSnapshotMeta meta, byte[] previousDigest,
      byte[] descriptorDigest, byte[] blockHistoryDigest, byte[] resultHistoryDigest,
      LanePayload payload, short compressionId) {
    byte[] storedPayload = compressionId == StateArchiveFileFormatV3.COMPRESSION_NONE
        ? payload.bytes : deflate(payload.bytes);
    long totalLength = checkedAdd(StateArchiveFileFormatV3.BLOCK_HEADER_LENGTH,
        storedPayload.length, StateArchiveFileFormatV3.FRAME_TRAILER_LENGTH);
    if (totalLength > StateArchiveFileFormatV3.MAX_BLOCK_FRAME_BYTES) {
      throw new IllegalArgumentException("State Archive lane frame exceeds 64 MiB");
    }
    ByteBuffer frame = ByteBuffer.allocate((int) totalLength);
    putEnvelope(frame, totalLength, storedPayload.length);
    frame.putLong(meta.getEpoch());
    frame.putLong(meta.getBlockNumber());
    frame.putLong(meta.getTimestamp());
    frame.put(meta.getBlockHash());
    frame.put(meta.getParentHash());
    frame.put(descriptorDigest);
    frame.put(previousDigest);
    frame.put(payload.payloadDigest);
    frame.put(blockHistoryDigest);
    frame.put(resultHistoryDigest);
    frame.putLong(payload.coverageBitmap);
    frame.putLong(payload.changedBitmap);
    frame.putInt(payload.groupCount);
    frame.putInt(0);
    frame.putLong(payload.entryCount);
    frame.putLong(payload.bytes.length);
    frame.putShort(payload.bodyCodec);
    frame.putShort(compressionId);
    frame.putShort(StateArchiveFileFormatV3.KEY_ORDER_ID);
    frame.putShort(StateArchiveFileFormatV3.DIGEST_ID);
    frame.putShort(StateArchiveFileFormatV3.CHECKSUM_ID);
    frame.putShort(StateArchiveFileFormatV3.IDENTITY_KIND);
    frame.putInt(0);
    if (frame.position() != StateArchiveFileFormatV3.BLOCK_HEADER_LENGTH) {
      throw new IllegalStateException("Invalid State Archive block header length");
    }
    frame.put(storedPayload);
    byte[] encodedDigest = StateArchiveFileFormatV3.sha256(
        StateArchiveFileFormatV3.ENCODED_BLOCK_DOMAIN,
        Arrays.copyOf(frame.array(), frame.position()));
    frame.put(encodedDigest);
    frame.putLong(totalLength);
    int crcLength = frame.position();
    frame.putInt(crc32c(frame.array(), 0, crcLength));
    frame.putInt(StateArchiveFileFormatV3.FRAME_TRAILER_MAGIC);
    return new EncodedLane(payload.laneId, payload.bodyCodec, payload.coverageBitmap,
        payload.bytes, payload.payloadDigest, encodedDigest, frame.array());
  }

  private DecodedLane decodeLane(byte[] frameBytes) {
    Objects.requireNonNull(frameBytes, "frameBytes");
    if (frameBytes.length < StateArchiveFileFormatV3.BLOCK_HEADER_LENGTH
        + StateArchiveFileFormatV3.FRAME_TRAILER_LENGTH
        || frameBytes.length > StateArchiveFileFormatV3.MAX_BLOCK_FRAME_BYTES) {
      throw new IllegalArgumentException("State Archive lane frame length is invalid");
    }
    ByteBuffer frame = ByteBuffer.wrap(frameBytes);
    requireInt(frame, StateArchiveFileFormatV3.FRAME_MAGIC, "frame magic");
    requireShort(frame, StateArchiveFileFormatV3.MAJOR_VERSION, "major version");
    requireShort(frame, StateArchiveFileFormatV3.MINOR_VERSION, "minor version");
    requireShort(frame, StateArchiveFileFormatV3.BLOCK_FRAME_TYPE, "frame type");
    requireShort(frame, (short) 0, "frame flags");
    requireInt(frame, StateArchiveFileFormatV3.BLOCK_HEADER_LENGTH, "header length");
    long totalLength = frame.getLong();
    long payloadLength = frame.getLong();
    if (totalLength != frameBytes.length || payloadLength < 0
        || checkedAdd(StateArchiveFileFormatV3.BLOCK_HEADER_LENGTH, payloadLength,
        StateArchiveFileFormatV3.FRAME_TRAILER_LENGTH) != totalLength) {
      throw new IllegalArgumentException("State Archive lane envelope length mismatch");
    }
    long epoch = requireNonNegative(frame.getLong(), "epoch");
    long blockNumber = requireNonNegative(frame.getLong(), "blockNumber");
    long timestamp = requireNonNegative(frame.getLong(), "timestamp");
    if (epoch != blockNumber) {
      throw new IllegalArgumentException("v3 requires epoch to equal blockNumber");
    }
    byte[] blockHash = getBytes(frame, StateArchiveFileFormatV3.HASH_LENGTH);
    byte[] parentHash = getBytes(frame, StateArchiveFileFormatV3.HASH_LENGTH);
    byte[] descriptorDigest = getBytes(frame, StateArchiveFileFormatV3.HASH_LENGTH);
    requireArray(descriptorDigest, StateArchiveFileFormatV3.fiveLaneDescriptorDigest(),
        "five-lane descriptor digest");
    byte[] previousHistoryDigest = getBytes(frame, StateArchiveFileFormatV3.HASH_LENGTH);
    byte[] payloadDigest = getBytes(frame, StateArchiveFileFormatV3.HASH_LENGTH);
    byte[] blockHistoryDigest = getBytes(frame, StateArchiveFileFormatV3.HASH_LENGTH);
    byte[] resultHistoryDigest = getBytes(frame, StateArchiveFileFormatV3.HASH_LENGTH);
    long coverageBitmap = frame.getLong();
    int laneId = laneIdForCoverage(coverageBitmap);
    long changedBitmap = frame.getLong();
    int groupCount = requireNonNegative(frame.getInt(), "groupCount");
    requireInt(frame, 0, "block reserved field");
    long entryCount = requireNonNegative(frame.getLong(), "entryCount");
    long rawPayloadLength = requireNonNegative(frame.getLong(), "rawPayloadLength");
    short bodyCodec = frame.getShort();
    requireShortValue(bodyCodec, StateArchiveFileFormatV3.laneBodyCodec(laneId),
        "body codec");
    short compressionId = frame.getShort();
    requireCompression(compressionId);
    requireShort(frame, StateArchiveFileFormatV3.KEY_ORDER_ID, "key order");
    requireShort(frame, StateArchiveFileFormatV3.DIGEST_ID, "digest algorithm");
    requireShort(frame, StateArchiveFileFormatV3.CHECKSUM_ID, "checksum algorithm");
    requireShort(frame, StateArchiveFileFormatV3.IDENTITY_KIND, "identity kind");
    requireInt(frame, 0, "block reserved tail");
    if ((changedBitmap & ~coverageBitmap) != 0
        || groupCount != Long.bitCount(changedBitmap)
        || rawPayloadLength > StateArchiveFileFormatV3.MAX_BLOCK_FRAME_BYTES) {
      throw new IllegalArgumentException("State Archive lane coverage is invalid");
    }
    byte[] storedPayload = getBytes(frame, checkedInt(payloadLength, "payloadLength"));
    int trailerStart = frame.position();
    byte[] encodedFrameDigest = getBytes(frame, StateArchiveFileFormatV3.HASH_LENGTH);
    if (frame.getLong() != totalLength) {
      throw new IllegalArgumentException("State Archive repeated frame length mismatch");
    }
    int expectedCrc = frame.getInt();
    requireInt(frame, StateArchiveFileFormatV3.FRAME_TRAILER_MAGIC, "trailer magic");
    if (expectedCrc != crc32c(frameBytes, 0,
        frameBytes.length - TRAILER_CRC_AND_MAGIC_LENGTH)) {
      throw new IllegalArgumentException("State Archive lane frame checksum mismatch");
    }
    byte[] actualEncodedDigest = StateArchiveFileFormatV3.sha256(
        StateArchiveFileFormatV3.ENCODED_BLOCK_DOMAIN,
        Arrays.copyOf(frameBytes, trailerStart));
    requireArray(encodedFrameDigest, actualEncodedDigest, "encoded frame digest");
    byte[] rawPayload = compressionId == StateArchiveFileFormatV3.COMPRESSION_NONE
        ? storedPayload : inflate(storedPayload,
        checkedInt(rawPayloadLength, "rawPayloadLength"));
    if (rawPayload.length != rawPayloadLength) {
      throw new IllegalArgumentException("State Archive raw payload length mismatch");
    }
    requireArray(payloadDigest, StateArchiveFileFormatV3.sha256(
        StateArchiveFileFormatV3.PAYLOAD_DOMAIN, rawPayload), "payload digest");
    DecodedPayload decoded = decodePayload(laneId, bodyCodec, rawPayload,
        groupCount, entryCount, changedBitmap);
    LanePayload payload = new LanePayload(laneId,
        StateArchiveFileFormatV3.laneKind(laneId), bodyCodec, coverageBitmap,
        changedBitmap, groupCount, entryCount, rawPayload, payloadDigest);
    return new DecodedLane(laneId, new BlockSnapshotMeta(epoch, blockNumber,
        blockHash, parentHash, timestamp), previousHistoryDigest, blockHistoryDigest,
        resultHistoryDigest, payload, decoded.groups, compressionId,
        encodedFrameDigest);
  }

  private DecodedPayload decodePayload(int laneId, short bodyCodec, byte[] payloadBytes,
      int expectedGroupCount, long expectedEntryCount, long expectedChangedBitmap) {
    ByteBuffer payload = ByteBuffer.wrap(payloadBytes);
    requireRemaining(payload, StateArchiveFileFormatV3.PAYLOAD_HEADER_LENGTH,
        "payload header");
    requireInt(payload, StateArchiveFileFormatV3.PAYLOAD_MAGIC, "payload magic");
    requireShort(payload, (short) 1, "payload version");
    requireShort(payload, (short) 0, "payload flags");
    requireInt(payload, StateArchiveFileFormatV3.PAYLOAD_HEADER_LENGTH,
        "payload header length");
    int groupCount = requireNonNegative(payload.getInt(), "payload section count");
    long entryCount = requireNonNegative(payload.getLong(), "payload entry count");
    long sectionBytes = requireNonNegative(payload.getLong(), "payload section bytes");
    if (groupCount != expectedGroupCount || entryCount != expectedEntryCount
        || sectionBytes != payload.remaining()) {
      throw new IllegalArgumentException("State Archive payload counts or length mismatch");
    }
    if (bodyCodec == StateArchiveFileFormatV3.DEDICATED_FIXED_WIDTH_BODY_CODEC_ID
        && groupCount > 1) {
      throw new IllegalArgumentException("Fixed-width lane has multiple sections");
    }
    List<DbGroup> groups = new ArrayList<>(groupCount);
    int previousStoreId = 0;
    long actualEntryCount = 0;
    long actualChangedBitmap = 0;
    for (int index = 0; index < groupCount; index++) {
      int sectionStart = payload.position();
      requireRemaining(payload, Short.BYTES, "Store ID");
      int storeId = Short.toUnsignedInt(payload.getShort());
      if (storeId <= previousStoreId || StateArchiveFileFormatV3.laneId(storeId) != laneId) {
        throw new IllegalArgumentException("State Archive Store section lane is invalid");
      }
      previousStoreId = storeId;
      List<Entry> entries = bodyCodec == StateArchiveFileFormatV3.LANE_VARIABLE_BODY_CODEC_ID
          ? decodeVariableSection(payloadBytes, payload, sectionStart, storeId)
          : decodeFixedSection(payloadBytes, payload, sectionStart, storeId, laneId);
      groups.add(new DbGroup(StateArchiveFileFormatV3.dbName(storeId), entries));
      actualEntryCount = checkedAdd(actualEntryCount, entries.size());
      actualChangedBitmap |= 1L << (storeId - 1);
    }
    if (payload.hasRemaining() || actualEntryCount != entryCount
        || actualChangedBitmap != expectedChangedBitmap) {
      throw new IllegalArgumentException("State Archive lane payload coverage mismatch");
    }
    return new DecodedPayload(groups);
  }

  private List<Entry> decodeVariableSection(byte[] payloadBytes, ByteBuffer payload,
      int sectionStart, int storeId) {
    requireRemaining(payload, StateArchiveFileFormatV3.SECTION_HEADER_LENGTH - Short.BYTES,
        "variable Store section header");
    requireShort(payload, (short) 1, "section version");
    requireInt(payload, StateArchiveFileFormatV3.SECTION_HEADER_LENGTH,
        "section header length");
    int count = requirePositive(payload.getInt(), "section entry count");
    requireInt(payload, 0, "section flags");
    long offsetLength = payload.getLong();
    long recordLength = payload.getLong();
    long valueLength = payload.getLong();
    long sectionLength = payload.getLong();
    byte[] sectionDigest = getBytes(payload, StateArchiveFileFormatV3.HASH_LENGTH);
    long expectedOffsetLength = checkedMultiply(count + 1L, Integer.BYTES);
    requireSectionLength(payloadBytes, sectionStart, sectionLength,
        StateArchiveFileFormatV3.SECTION_HEADER_LENGTH, offsetLength, recordLength,
        valueLength);
    if (offsetLength != expectedOffsetLength) {
      throw new IllegalArgumentException("State Archive record offset length is invalid");
    }
    byte[] offsets = getBytes(payload, checkedInt(offsetLength, "offsetLength"));
    byte[] records = getBytes(payload, checkedInt(recordLength, "recordLength"));
    byte[] values = getBytes(payload, checkedInt(valueLength, "valueLength"));
    byte[] actualDigest = StateArchiveFileFormatV3.sha256(
        StateArchiveFileFormatV3.SECTION_DOMAIN,
        ByteBuffer.allocate(Short.BYTES + Integer.BYTES)
            .putShort((short) storeId).putInt(count).array(), offsets, records, values);
    requireArray(sectionDigest, actualDigest, "section digest");
    return decodeVariableEntries(count, offsets, records, values);
  }

  private List<Entry> decodeFixedSection(byte[] payloadBytes, ByteBuffer payload,
      int sectionStart, int storeId, int laneId) {
    requireRemaining(payload, FIXED_SECTION_HEADER_LENGTH - Short.BYTES,
        "fixed Store section header");
    requireShort(payload, (short) 2, "fixed section version");
    requireInt(payload, FIXED_SECTION_HEADER_LENGTH, "fixed section header length");
    int count = requirePositive(payload.getInt(), "fixed section entry count");
    requireInt(payload, 0, "fixed section flags");
    int keyWidth = payload.getInt();
    requireInt(payload, 0, "fixed section reserved field");
    long keyLength = payload.getLong();
    long presenceLength = payload.getLong();
    long endsLength = payload.getLong();
    long valueLength = payload.getLong();
    long sectionLength = payload.getLong();
    byte[] sectionDigest = getBytes(payload, StateArchiveFileFormatV3.HASH_LENGTH);
    int expectedKeyWidth = StateArchiveFileFormatV3.fixedKeyWidth(laneId);
    if (storeId != laneId || keyWidth != expectedKeyWidth
        || keyLength != checkedMultiply(count, keyWidth)
        || presenceLength != (count + 7L) / 8
        || endsLength != checkedMultiply(count + 1L, Integer.BYTES)) {
      throw new IllegalArgumentException("State Archive fixed section shape is invalid");
    }
    requireSectionLength(payloadBytes, sectionStart, sectionLength,
        FIXED_SECTION_HEADER_LENGTH, keyLength, presenceLength, endsLength, valueLength);
    byte[] keys = getBytes(payload, checkedInt(keyLength, "keyLength"));
    byte[] presence = getBytes(payload, checkedInt(presenceLength, "presenceLength"));
    byte[] ends = getBytes(payload, checkedInt(endsLength, "endsLength"));
    byte[] values = getBytes(payload, checkedInt(valueLength, "valueLength"));
    byte[] actualDigest = StateArchiveFileFormatV3.sha256(
        StateArchiveFileFormatV3.FIXED_SECTION_DOMAIN,
        ByteBuffer.allocate(Short.BYTES + 2 * Integer.BYTES)
            .putShort((short) storeId).putInt(count).putInt(keyWidth).array(),
        keys, presence, ends, values);
    requireArray(sectionDigest, actualDigest, "fixed section digest");
    return decodeFixedEntries(count, keyWidth, keys, presence, ends, values);
  }

  private List<Entry> decodeVariableEntries(int count, byte[] offsetBytes,
      byte[] recordBytes, byte[] valueBytes) {
    ByteBuffer offsets = ByteBuffer.wrap(offsetBytes);
    int[] positions = new int[count + 1];
    for (int index = 0; index <= count; index++) {
      long offset = Integer.toUnsignedLong(offsets.getInt());
      if (offset > recordBytes.length || (index == 0 && offset != 0)
          || (index > 0 && offset <= positions[index - 1])) {
        throw new IllegalArgumentException("State Archive record offsets are invalid");
      }
      positions[index] = (int) offset;
    }
    if (positions[count] != recordBytes.length) {
      throw new IllegalArgumentException("State Archive final record offset is invalid");
    }
    List<Entry> entries = new ArrayList<>(count);
    byte[] previousKey = null;
    int valueCursor = 0;
    for (int index = 0; index < count; index++) {
      int recordLength = positions[index + 1] - positions[index];
      if (recordLength < StateArchiveFileFormatV3.VARIABLE_INDEX_RECORD_BASE_LENGTH) {
        throw new IllegalArgumentException("State Archive index record is truncated");
      }
      ByteBuffer record = ByteBuffer.wrap(recordBytes, positions[index], recordLength).slice();
      long keyLength = Integer.toUnsignedLong(record.getInt());
      int tag = Byte.toUnsignedInt(record.get());
      requireZero(record, 3, "index record reserved bytes");
      long valueOffset = Integer.toUnsignedLong(record.getInt());
      long valueLength = Integer.toUnsignedLong(record.getInt());
      if (keyLength != record.remaining()) {
        throw new IllegalArgumentException("State Archive key length is invalid");
      }
      byte[] key = getBytes(record, checkedInt(keyLength, "keyLength"));
      requireOrdered(previousKey, key);
      previousKey = key;
      ValueRead valueRead = readValue(tag, valueOffset, valueLength,
          valueCursor, valueBytes);
      valueCursor = valueRead.nextCursor;
      entries.add(new Entry(key, valueRead.oldValue));
    }
    if (valueCursor != valueBytes.length) {
      throw new IllegalArgumentException("State Archive value data has an unreferenced tail");
    }
    return entries;
  }

  private List<Entry> decodeFixedEntries(int count, int keyWidth, byte[] keys,
      byte[] presence, byte[] endBytes, byte[] values) {
    int usedBits = count % 8;
    if (usedBits != 0 && (Byte.toUnsignedInt(presence[presence.length - 1])
        & ~((1 << usedBits) - 1)) != 0) {
      throw new IllegalArgumentException("State Archive presence bitmap has non-zero tail bits");
    }
    ByteBuffer ends = ByteBuffer.wrap(endBytes);
    long first = Integer.toUnsignedLong(ends.getInt());
    if (first != 0) {
      throw new IllegalArgumentException("State Archive first value end is invalid");
    }
    List<Entry> entries = new ArrayList<>(count);
    byte[] previousKey = null;
    int valueCursor = 0;
    for (int index = 0; index < count; index++) {
      long next = Integer.toUnsignedLong(ends.getInt());
      if (next < valueCursor || next > values.length) {
        throw new IllegalArgumentException("State Archive value ends are invalid");
      }
      byte[] key = Arrays.copyOfRange(keys, index * keyWidth, (index + 1) * keyWidth);
      requireOrdered(previousKey, key);
      previousKey = key;
      boolean present = (presence[index / 8] & (1 << (index % 8))) != 0;
      if (!present && next != valueCursor) {
        throw new IllegalArgumentException("State Archive absent fixed value has bytes");
      }
      OldValue oldValue = present
          ? OldValue.present(Arrays.copyOfRange(values, valueCursor, (int) next))
          : OldValue.absent();
      valueCursor = (int) next;
      entries.add(new Entry(key, oldValue));
    }
    if (valueCursor != values.length) {
      throw new IllegalArgumentException("State Archive fixed value data has a tail");
    }
    return entries;
  }

  private ValueRead readValue(int tag, long valueOffset, long valueLength,
      int valueCursor, byte[] values) {
    if (tag == 0) {
      if (valueOffset != UNSIGNED_INT_MAX || valueLength != UNSIGNED_INT_MAX) {
        throw new IllegalArgumentException("State Archive ABSENT value locator is invalid");
      }
      return new ValueRead(OldValue.absent(), valueCursor);
    }
    if (tag != 1 || valueOffset != valueCursor
        || valueLength > values.length - valueCursor) {
      throw new IllegalArgumentException("State Archive PRESENT value locator is invalid");
    }
    int next = valueCursor + (int) valueLength;
    return new ValueRead(OldValue.present(Arrays.copyOfRange(values, valueCursor, next)), next);
  }

  private byte[] bundleBlockHistoryDigest(BlockSnapshotMeta meta,
      byte[] descriptorDigest, List<LanePayload> payloads) {
    ByteBuffer identity = ByteBuffer.allocate(3 * Long.BYTES
        + 3 * StateArchiveFileFormatV3.HASH_LENGTH + Short.BYTES
        + payloads.size() * StateArchiveFileFormatV3.HASH_LENGTH);
    identity.putLong(meta.getEpoch());
    identity.putLong(meta.getBlockNumber());
    identity.putLong(meta.getTimestamp());
    identity.put(meta.getBlockHash());
    identity.put(meta.getParentHash());
    identity.put(descriptorDigest);
    identity.putShort((short) payloads.size());
    int previousLane = -1;
    for (LanePayload payload : payloads) {
      if (payload.laneId <= previousLane) {
        throw new IllegalArgumentException("State Archive lane items are not ordered");
      }
      previousLane = payload.laneId;
      identity.put(laneItemDigest(payload));
    }
    return StateArchiveFileFormatV3.sha256(
        StateArchiveFileFormatV3.BUNDLE_BLOCK_DOMAIN, identity.array());
  }

  private byte[] laneItemDigest(LanePayload payload) {
    ByteBuffer identity = ByteBuffer.allocate(3 * Short.BYTES + 2 * Long.BYTES
        + Integer.BYTES + 2 * Long.BYTES + StateArchiveFileFormatV3.HASH_LENGTH);
    identity.putShort(payload.laneKind);
    identity.putShort((short) payload.laneId);
    identity.putShort(payload.bodyCodec);
    identity.putLong(payload.coverageBitmap);
    identity.putLong(payload.changedBitmap);
    identity.putInt(payload.groupCount);
    identity.putLong(payload.entryCount);
    identity.putLong(payload.bytes.length);
    identity.put(payload.payloadDigest);
    return StateArchiveFileFormatV3.sha256(
        StateArchiveFileFormatV3.LANE_ITEM_DOMAIN, identity.array());
  }

  private void requireMetaEquals(DecodedLane expected, DecodedLane actual) {
    if (!expected.meta.equals(actual.meta)
        || !Arrays.equals(expected.previousHistoryDigest, actual.previousHistoryDigest)
        || !Arrays.equals(expected.blockHistoryDigest, actual.blockHistoryDigest)
        || !Arrays.equals(expected.resultHistoryDigest, actual.resultHistoryDigest)) {
      throw new IllegalArgumentException("State Archive lane bundle identity mismatch");
    }
  }

  private int laneIdForCoverage(long coverage) {
    for (int laneId : StateArchiveFileFormatV3.fiveLaneIds()) {
      if (coverage == StateArchiveFileFormatV3.laneCoverage(laneId)) {
        return laneId;
      }
    }
    throw new IllegalArgumentException("Unknown State Archive lane coverage");
  }

  private BlockSnapshotMeta requireMeta(BlockSnapshotMeta meta) {
    if (meta.getEpoch() != meta.getBlockNumber()) {
      throw new IllegalArgumentException("v3 requires epoch to equal blockNumber");
    }
    if (meta.getTimestamp() < 0) {
      throw new IllegalArgumentException("v3 timestamp must not be negative");
    }
    return meta;
  }

  private void putEnvelope(ByteBuffer frame, long totalLength, long payloadLength) {
    frame.putInt(StateArchiveFileFormatV3.FRAME_MAGIC);
    frame.putShort(StateArchiveFileFormatV3.MAJOR_VERSION);
    frame.putShort(StateArchiveFileFormatV3.MINOR_VERSION);
    frame.putShort(StateArchiveFileFormatV3.BLOCK_FRAME_TYPE);
    frame.putShort((short) 0);
    frame.putInt(StateArchiveFileFormatV3.BLOCK_HEADER_LENGTH);
    frame.putLong(totalLength);
    frame.putLong(payloadLength);
  }

  private byte[] deflate(byte[] input) {
    Deflater deflater = new Deflater(1, true);
    deflater.setStrategy(Deflater.DEFAULT_STRATEGY);
    deflater.setInput(input);
    deflater.finish();
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    byte[] buffer = new byte[8192];
    while (!deflater.finished()) {
      int count = deflater.deflate(buffer);
      if (count == 0 && deflater.needsInput()) {
        throw new IllegalStateException("Unexpected raw DEFLATE termination");
      }
      output.write(buffer, 0, count);
    }
    deflater.end();
    return output.toByteArray();
  }

  private byte[] inflate(byte[] input, int expectedLength) {
    Inflater inflater = new Inflater(true);
    inflater.setInput(input);
    byte[] output = new byte[expectedLength];
    try {
      int count = inflater.inflate(output);
      if (count != expectedLength || !inflater.finished() || inflater.getRemaining() != 0) {
        throw new IllegalArgumentException("State Archive compressed payload length mismatch");
      }
      return output;
    } catch (DataFormatException e) {
      throw new IllegalArgumentException("Invalid State Archive compressed payload", e);
    } finally {
      inflater.end();
    }
  }

  private void requireSectionLength(byte[] payload, int start, long sectionLength,
      long... components) {
    if (sectionLength != checkedAdd(components) || sectionLength > payload.length - start) {
      throw new IllegalArgumentException("State Archive Store section length is invalid");
    }
  }

  private static void requireOrdered(byte[] previous, byte[] key) {
    if (previous != null && BlockReverseDiff.compareUnsigned(previous, key) >= 0) {
      throw new IllegalArgumentException("State Archive decoded keys are not ordered");
    }
  }

  private static byte[] requireHash(byte[] value, String name) {
    Objects.requireNonNull(value, name);
    if (value.length != StateArchiveFileFormatV3.HASH_LENGTH) {
      throw new IllegalArgumentException(name + " must be exactly 32 bytes");
    }
    return Arrays.copyOf(value, value.length);
  }

  private static void requireCompression(short compressionId) {
    if (compressionId != StateArchiveFileFormatV3.COMPRESSION_NONE
        && compressionId != StateArchiveFileFormatV3.COMPRESSION_RAW_DEFLATE_LEVEL_1) {
      throw new IllegalArgumentException("Unsupported State Archive compression: "
          + compressionId);
    }
  }

  private static long requireNonNegative(long value, String name) {
    if (value < 0) {
      throw new IllegalArgumentException("State Archive " + name + " is negative");
    }
    return value;
  }

  private static int requireNonNegative(int value, String name) {
    if (value < 0) {
      throw new IllegalArgumentException("State Archive " + name + " is negative");
    }
    return value;
  }

  private static int requirePositive(int value, String name) {
    if (value <= 0) {
      throw new IllegalArgumentException("State Archive " + name + " is not positive");
    }
    return value;
  }

  private static void requireInt(ByteBuffer buffer, int expected, String name) {
    requireRemaining(buffer, Integer.BYTES, name);
    if (buffer.getInt() != expected) {
      throw new IllegalArgumentException("Invalid State Archive " + name);
    }
  }

  private static void requireShort(ByteBuffer buffer, short expected, String name) {
    requireRemaining(buffer, Short.BYTES, name);
    requireShortValue(buffer.getShort(), expected, name);
  }

  private static void requireShortValue(short actual, short expected, String name) {
    if (actual != expected) {
      throw new IllegalArgumentException("Invalid State Archive " + name);
    }
  }

  private static void requireZero(ByteBuffer buffer, int length, String name) {
    requireRemaining(buffer, length, name);
    for (int index = 0; index < length; index++) {
      if (buffer.get() != 0) {
        throw new IllegalArgumentException("Invalid State Archive " + name);
      }
    }
  }

  private static void requireRemaining(ByteBuffer buffer, int length, String name) {
    if (length < 0 || buffer.remaining() < length) {
      throw new IllegalArgumentException("Truncated State Archive " + name);
    }
  }

  private static byte[] getBytes(ByteBuffer buffer, int length) {
    requireRemaining(buffer, length, "byte field");
    byte[] bytes = new byte[length];
    buffer.get(bytes);
    return bytes;
  }

  private static void requireArray(byte[] actual, byte[] expected, String name) {
    if (!Arrays.equals(actual, expected)) {
      throw new IllegalArgumentException("State Archive " + name + " mismatch");
    }
  }

  private static int crc32c(byte[] bytes, int offset, int length) {
    return Hashing.crc32c().hashBytes(bytes, offset, length).asInt();
  }

  private static long checkedAdd(long... values) {
    long result = 0;
    for (long value : values) {
      if (value < 0 || Long.MAX_VALUE - result < value) {
        throw new IllegalArgumentException("State Archive length overflow");
      }
      result += value;
    }
    return result;
  }

  private static long checkedMultiply(long left, long right) {
    if (left < 0 || right < 0 || (left != 0 && right > Long.MAX_VALUE / left)) {
      throw new IllegalArgumentException("State Archive length overflow");
    }
    return left * right;
  }

  private static int checkedInt(long value, String name) {
    if (value < 0 || value > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("State Archive " + name + " exceeds int range");
    }
    return (int) value;
  }

  private static final class StoreGroup {
    private final int storeId;
    private final int laneId;
    private final List<Entry> entries;

    private StoreGroup(int storeId, int laneId, List<Entry> entries) {
      this.storeId = storeId;
      this.laneId = laneId;
      this.entries = entries;
    }
  }

  private static final class LanePayload {
    private final int laneId;
    private final short laneKind;
    private final short bodyCodec;
    private final long coverageBitmap;
    private final long changedBitmap;
    private final int groupCount;
    private final long entryCount;
    private final byte[] bytes;
    private final byte[] payloadDigest;

    private LanePayload(int laneId, short laneKind, short bodyCodec,
        long coverageBitmap, long changedBitmap, int groupCount, long entryCount,
        byte[] bytes, byte[] payloadDigest) {
      this.laneId = laneId;
      this.laneKind = laneKind;
      this.bodyCodec = bodyCodec;
      this.coverageBitmap = coverageBitmap;
      this.changedBitmap = changedBitmap;
      this.groupCount = groupCount;
      this.entryCount = entryCount;
      this.bytes = bytes;
      this.payloadDigest = payloadDigest;
    }
  }

  private static final class DecodedPayload {
    private final List<DbGroup> groups;

    private DecodedPayload(List<DbGroup> groups) {
      this.groups = groups;
    }
  }

  private static final class ValueRead {
    private final OldValue oldValue;
    private final int nextCursor;

    private ValueRead(OldValue oldValue, int nextCursor) {
      this.oldValue = oldValue;
      this.nextCursor = nextCursor;
    }
  }

  public static final class EncodedBundle {
    private final List<EncodedLane> lanes;
    private final byte[] blockHistoryDigest;
    private final byte[] resultHistoryDigest;
    private final BlockReverseDiff diff;

    private EncodedBundle(List<EncodedLane> lanes, byte[] blockHistoryDigest,
        byte[] resultHistoryDigest, BlockReverseDiff diff) {
      this.lanes = java.util.Collections.unmodifiableList(new ArrayList<>(lanes));
      this.blockHistoryDigest = blockHistoryDigest;
      this.resultHistoryDigest = resultHistoryDigest;
      this.diff = diff;
    }

    public List<EncodedLane> getLanes() {
      return lanes;
    }

    public byte[] getBlockHistoryDigest() {
      return Arrays.copyOf(blockHistoryDigest, blockHistoryDigest.length);
    }

    public byte[] getResultHistoryDigest() {
      return Arrays.copyOf(resultHistoryDigest, resultHistoryDigest.length);
    }

    public BlockReverseDiff getDiff() {
      return diff;
    }
  }

  public static final class EncodedLane {
    private final int laneId;
    private final short bodyCodec;
    private final long coverageBitmap;
    private final byte[] canonicalPayload;
    private final byte[] payloadDigest;
    private final byte[] encodedFrameDigest;
    private final byte[] frame;

    private EncodedLane(int laneId, short bodyCodec, long coverageBitmap,
        byte[] canonicalPayload, byte[] payloadDigest, byte[] encodedFrameDigest,
        byte[] frame) {
      this.laneId = laneId;
      this.bodyCodec = bodyCodec;
      this.coverageBitmap = coverageBitmap;
      this.canonicalPayload = canonicalPayload;
      this.payloadDigest = payloadDigest;
      this.encodedFrameDigest = encodedFrameDigest;
      this.frame = frame;
    }

    public int getLaneId() {
      return laneId;
    }

    public short getBodyCodec() {
      return bodyCodec;
    }

    public long getCoverageBitmap() {
      return coverageBitmap;
    }

    public byte[] getCanonicalPayload() {
      return Arrays.copyOf(canonicalPayload, canonicalPayload.length);
    }

    public byte[] getPayloadDigest() {
      return Arrays.copyOf(payloadDigest, payloadDigest.length);
    }

    public byte[] getEncodedFrameDigest() {
      return Arrays.copyOf(encodedFrameDigest, encodedFrameDigest.length);
    }

    public byte[] getFrame() {
      return Arrays.copyOf(frame, frame.length);
    }
  }

  public static final class DecodedBundle {
    private final BlockReverseDiff diff;
    private final List<DecodedLane> lanes;
    private final byte[] blockHistoryDigest;
    private final byte[] resultHistoryDigest;

    private DecodedBundle(BlockReverseDiff diff, List<DecodedLane> lanes,
        byte[] blockHistoryDigest, byte[] resultHistoryDigest) {
      this.diff = diff;
      this.lanes = java.util.Collections.unmodifiableList(new ArrayList<>(lanes));
      this.blockHistoryDigest = blockHistoryDigest;
      this.resultHistoryDigest = resultHistoryDigest;
    }

    public BlockReverseDiff getDiff() {
      return diff;
    }

    public List<DecodedLane> getLanes() {
      return lanes;
    }

    public byte[] getBlockHistoryDigest() {
      return Arrays.copyOf(blockHistoryDigest, blockHistoryDigest.length);
    }

    public byte[] getResultHistoryDigest() {
      return Arrays.copyOf(resultHistoryDigest, resultHistoryDigest.length);
    }
  }

  public static final class DecodedLane {
    private final int laneId;
    private final BlockSnapshotMeta meta;
    private final byte[] previousHistoryDigest;
    private final byte[] blockHistoryDigest;
    private final byte[] resultHistoryDigest;
    private final LanePayload payload;
    private final List<DbGroup> groups;
    private final short compressionId;
    private final byte[] encodedFrameDigest;
    private final long coverageBitmap;

    private DecodedLane(int laneId, BlockSnapshotMeta meta,
        byte[] previousHistoryDigest, byte[] blockHistoryDigest,
        byte[] resultHistoryDigest, LanePayload payload, List<DbGroup> groups,
        short compressionId, byte[] encodedFrameDigest) {
      this.laneId = laneId;
      this.meta = meta;
      this.previousHistoryDigest = previousHistoryDigest;
      this.blockHistoryDigest = blockHistoryDigest;
      this.resultHistoryDigest = resultHistoryDigest;
      this.payload = payload;
      this.groups = java.util.Collections.unmodifiableList(new ArrayList<>(groups));
      this.compressionId = compressionId;
      this.encodedFrameDigest = encodedFrameDigest;
      this.coverageBitmap = payload.coverageBitmap;
    }

    public int getLaneId() {
      return laneId;
    }

    public List<DbGroup> getGroups() {
      return groups;
    }

    public short getCompressionId() {
      return compressionId;
    }

    public byte[] getEncodedFrameDigest() {
      return Arrays.copyOf(encodedFrameDigest, encodedFrameDigest.length);
    }
  }
}
