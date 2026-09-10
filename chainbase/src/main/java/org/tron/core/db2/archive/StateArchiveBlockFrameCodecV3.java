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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import org.tron.core.db2.archive.BlockReverseDiff.DbGroup;
import org.tron.core.db2.archive.BlockReverseDiff.Entry;

/** Canonical codec for one default-off State Archive v3 mixed-stream block frame. */
public final class StateArchiveBlockFrameCodecV3 {

  private static final long UNSIGNED_INT_MAX = 0xffff_ffffL;
  private static final int TRAILER_CRC_AND_MAGIC_LENGTH = 8;

  /**
   * Validates the exact capture boundary before collecting old values and encoding the frame.
   */
  public EncodedBlock encode(BlockChangeView view, OldValueCollector collector,
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

  /** Encodes an already collected reverse diff with exact-27 descriptor coverage. */
  EncodedBlock encode(BlockReverseDiff diff, byte[] previousHistoryDigest,
      short compressionId) {
    Objects.requireNonNull(diff, "diff");
    byte[] previousDigest = requireHash(previousHistoryDigest, "previousHistoryDigest");
    requireCompression(compressionId);
    BlockSnapshotMeta meta = diff.getMeta();
    if (meta.getEpoch() != meta.getBlockNumber()) {
      throw new IllegalArgumentException("v3 requires epoch to equal blockNumber");
    }
    if (meta.getTimestamp() < 0) {
      throw new IllegalArgumentException("v3 timestamp must not be negative");
    }

    CanonicalPayload canonical = encodePayload(diff.getGroups());
    byte[] payloadDigest = StateArchiveFileFormatV3.sha256(
        StateArchiveFileFormatV3.PAYLOAD_DOMAIN, canonical.bytes);
    byte[] descriptorDigest = StateArchiveFileFormatV3.storeDescriptorDigest();
    byte[] blockHistoryDigest = blockHistoryDigest(meta, descriptorDigest, canonical,
        payloadDigest);
    byte[] resultHistoryDigest = StateArchiveFileFormatV3.sha256(
        StateArchiveFileFormatV3.ROLLING_DOMAIN, previousDigest, blockHistoryDigest);
    byte[] storedPayload = compressionId == StateArchiveFileFormatV3.COMPRESSION_NONE
        ? canonical.bytes : deflate(canonical.bytes);
    long totalLength = checkedAdd(StateArchiveFileFormatV3.BLOCK_HEADER_LENGTH,
        storedPayload.length, StateArchiveFileFormatV3.FRAME_TRAILER_LENGTH);
    if (totalLength > StateArchiveFileFormatV3.MAX_BLOCK_FRAME_BYTES) {
      throw new IllegalArgumentException("State Archive block frame exceeds 64 MiB");
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
    frame.put(payloadDigest);
    frame.put(blockHistoryDigest);
    frame.put(resultHistoryDigest);
    frame.putLong(StateArchiveFileFormatV3.EXACT_COVERAGE_BITMAP);
    frame.putLong(canonical.changedBitmap);
    frame.putInt(canonical.groupCount);
    frame.putInt(0);
    frame.putLong(canonical.entryCount);
    frame.putLong(canonical.bytes.length);
    frame.putShort(StateArchiveFileFormatV3.BODY_CODEC_ID);
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
    if (frame.hasRemaining()) {
      throw new IllegalStateException("Invalid State Archive frame length");
    }
    return new EncodedBlock(frame.array(), canonical.bytes, payloadDigest,
        blockHistoryDigest, resultHistoryDigest, encodedDigest, diff);
  }

  /** Decodes and fully verifies one complete v3 block frame. */
  public DecodedBlock decode(byte[] frameBytes) {
    Objects.requireNonNull(frameBytes, "frameBytes");
    if (frameBytes.length < StateArchiveFileFormatV3.BLOCK_HEADER_LENGTH
        + StateArchiveFileFormatV3.FRAME_TRAILER_LENGTH
        || frameBytes.length > StateArchiveFileFormatV3.MAX_BLOCK_FRAME_BYTES) {
      throw new IllegalArgumentException("State Archive block frame length is invalid");
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
      throw new IllegalArgumentException("State Archive frame envelope length mismatch");
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
    requireArray(descriptorDigest, StateArchiveFileFormatV3.storeDescriptorDigest(),
        "Store descriptor digest");
    byte[] previousHistoryDigest = getBytes(frame, StateArchiveFileFormatV3.HASH_LENGTH);
    byte[] payloadDigest = getBytes(frame, StateArchiveFileFormatV3.HASH_LENGTH);
    byte[] blockHistoryDigest = getBytes(frame, StateArchiveFileFormatV3.HASH_LENGTH);
    byte[] resultHistoryDigest = getBytes(frame, StateArchiveFileFormatV3.HASH_LENGTH);
    long coverageBitmap = frame.getLong();
    long changedBitmap = frame.getLong();
    int groupCount = requireNonNegative(frame.getInt(), "groupCount");
    requireInt(frame, 0, "block reserved field");
    long entryCount = requireNonNegative(frame.getLong(), "entryCount");
    long rawPayloadLength = requireNonNegative(frame.getLong(), "rawPayloadLength");
    requireShort(frame, StateArchiveFileFormatV3.BODY_CODEC_ID, "body codec");
    short compressionId = frame.getShort();
    requireCompression(compressionId);
    requireShort(frame, StateArchiveFileFormatV3.KEY_ORDER_ID, "key order");
    requireShort(frame, StateArchiveFileFormatV3.DIGEST_ID, "digest algorithm");
    requireShort(frame, StateArchiveFileFormatV3.CHECKSUM_ID, "checksum algorithm");
    requireShort(frame, StateArchiveFileFormatV3.IDENTITY_KIND, "identity kind");
    requireInt(frame, 0, "block reserved tail");
    if (coverageBitmap != StateArchiveFileFormatV3.EXACT_COVERAGE_BITMAP
        || (changedBitmap & ~coverageBitmap) != 0
        || groupCount != Long.bitCount(changedBitmap)
        || rawPayloadLength > StateArchiveFileFormatV3.MAX_BLOCK_FRAME_BYTES) {
      throw new IllegalArgumentException("State Archive block coverage is invalid");
    }

    byte[] storedPayload = getBytes(frame, checkedInt(payloadLength, "payloadLength"));
    int trailerStart = frame.position();
    byte[] encodedFrameDigest = getBytes(frame, StateArchiveFileFormatV3.HASH_LENGTH);
    if (frame.getLong() != totalLength) {
      throw new IllegalArgumentException("State Archive repeated frame length mismatch");
    }
    int expectedCrc = frame.getInt();
    requireInt(frame, StateArchiveFileFormatV3.FRAME_TRAILER_MAGIC, "trailer magic");
    int actualCrc = crc32c(frameBytes, 0,
        frameBytes.length - TRAILER_CRC_AND_MAGIC_LENGTH);
    if (expectedCrc != actualCrc) {
      throw new IllegalArgumentException("State Archive frame checksum mismatch");
    }
    byte[] actualEncodedDigest = StateArchiveFileFormatV3.sha256(
        StateArchiveFileFormatV3.ENCODED_BLOCK_DOMAIN,
        Arrays.copyOf(frameBytes, trailerStart));
    requireArray(encodedFrameDigest, actualEncodedDigest, "encoded frame digest");

    byte[] rawPayload = compressionId == StateArchiveFileFormatV3.COMPRESSION_NONE
        ? storedPayload : inflate(storedPayload, checkedInt(rawPayloadLength,
        "rawPayloadLength"));
    if (rawPayload.length != rawPayloadLength) {
      throw new IllegalArgumentException("State Archive raw payload length mismatch");
    }
    requireArray(payloadDigest, StateArchiveFileFormatV3.sha256(
        StateArchiveFileFormatV3.PAYLOAD_DOMAIN, rawPayload), "payload digest");
    DecodedPayload decodedPayload = decodePayload(rawPayload, groupCount, entryCount,
        changedBitmap);
    BlockSnapshotMeta meta = new BlockSnapshotMeta(epoch, blockNumber, blockHash, parentHash,
        timestamp);
    CanonicalPayload identityPayload = new CanonicalPayload(rawPayload, groupCount, entryCount,
        changedBitmap);
    byte[] actualBlockDigest = blockHistoryDigest(meta, descriptorDigest, identityPayload,
        payloadDigest);
    requireArray(blockHistoryDigest, actualBlockDigest, "block history digest");
    requireArray(resultHistoryDigest, StateArchiveFileFormatV3.sha256(
        StateArchiveFileFormatV3.ROLLING_DOMAIN, previousHistoryDigest, blockHistoryDigest),
        "result history digest");
    BlockReverseDiff diff = new BlockReverseDiff(meta, decodedPayload.groups);
    return new DecodedBlock(diff, rawPayload, previousHistoryDigest, payloadDigest,
        blockHistoryDigest, resultHistoryDigest, encodedFrameDigest, compressionId);
  }

  /** Returns the complete length described by a v3 common envelope. */
  public int frameLength(byte[] header) {
    if (header == null || header.length < StateArchiveFileFormatV3.FRAME_ENVELOPE_LENGTH) {
      throw new IllegalArgumentException("State Archive frame envelope is truncated");
    }
    ByteBuffer buffer = ByteBuffer.wrap(header);
    requireInt(buffer, StateArchiveFileFormatV3.FRAME_MAGIC, "frame magic");
    requireShort(buffer, StateArchiveFileFormatV3.MAJOR_VERSION, "major version");
    requireShort(buffer, StateArchiveFileFormatV3.MINOR_VERSION, "minor version");
    requireShort(buffer, StateArchiveFileFormatV3.BLOCK_FRAME_TYPE, "frame type");
    requireShort(buffer, (short) 0, "frame flags");
    requireInt(buffer, StateArchiveFileFormatV3.BLOCK_HEADER_LENGTH, "header length");
    long totalLength = buffer.getLong();
    long payloadLength = buffer.getLong();
    if (totalLength > StateArchiveFileFormatV3.MAX_BLOCK_FRAME_BYTES
        || totalLength != checkedAdd(StateArchiveFileFormatV3.BLOCK_HEADER_LENGTH,
        payloadLength, StateArchiveFileFormatV3.FRAME_TRAILER_LENGTH)) {
      throw new IllegalArgumentException("State Archive frame envelope length mismatch");
    }
    return checkedInt(totalLength, "totalLength");
  }

  private CanonicalPayload encodePayload(Collection<DbGroup> inputGroups) {
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
      groups.add(new StoreGroup(storeId, group.getEntries()));
    }
    groups.sort(Comparator.comparingInt(group -> group.storeId));
    List<byte[]> sections = new ArrayList<>();
    long entryCount = 0;
    long sectionBytes = 0;
    long changedBitmap = 0;
    for (StoreGroup group : groups) {
      byte[] section = encodeSection(group);
      sections.add(section);
      entryCount = checkedAdd(entryCount, group.entries.size());
      sectionBytes = checkedAdd(sectionBytes, section.length);
      changedBitmap |= 1L << (group.storeId - 1);
    }
    long payloadLength = checkedAdd(StateArchiveFileFormatV3.PAYLOAD_HEADER_LENGTH,
        sectionBytes);
    if (payloadLength > StateArchiveFileFormatV3.MAX_BLOCK_FRAME_BYTES) {
      throw new IllegalArgumentException("State Archive canonical payload is too large");
    }
    ByteBuffer payload = ByteBuffer.allocate((int) payloadLength);
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
    return new CanonicalPayload(payload.array(), groups.size(), entryCount, changedBitmap);
  }

  private byte[] encodeSection(StoreGroup group) {
    int entryCount = group.entries.size();
    long offsetVectorLength = checkedMultiply(entryCount + 1L, Integer.BYTES);
    ByteArrayOutputStream records = new ByteArrayOutputStream();
    ByteArrayOutputStream values = new ByteArrayOutputStream();
    ByteBuffer offsets = ByteBuffer.allocate(checkedInt(offsetVectorLength,
        "offsetVectorLength"));
    byte[] previousKey = null;
    try (DataOutputStream recordOutput = new DataOutputStream(records)) {
      for (Entry entry : group.entries) {
        byte[] key = entry.getKey();
        if (previousKey != null && BlockReverseDiff.compareUnsigned(previousKey, key) >= 0) {
          throw new IllegalArgumentException("Archive keys are not strictly sorted");
        }
        previousKey = key;
        offsets.putInt(records.size());
        recordOutput.writeInt(key.length);
        OldValue oldValue = entry.getOldValue();
        recordOutput.writeByte(oldValue.isPresent() ? 1 : 0);
        recordOutput.write(new byte[3]);
        if (oldValue.isPresent()) {
          byte[] value = oldValue.getValue();
          recordOutput.writeInt(values.size());
          recordOutput.writeInt(value.length);
          values.write(value);
        } else {
          recordOutput.writeInt(-1);
          recordOutput.writeInt(-1);
        }
        recordOutput.write(key);
      }
      recordOutput.flush();
    } catch (IOException impossible) {
      throw new IllegalStateException("Unexpected in-memory section encoding failure", impossible);
    }
    offsets.putInt(records.size());
    byte[] offsetBytes = offsets.array();
    byte[] recordBytes = records.toByteArray();
    byte[] valueBytes = values.toByteArray();
    long sectionLength = checkedAdd(StateArchiveFileFormatV3.SECTION_HEADER_LENGTH,
        offsetBytes.length, recordBytes.length, valueBytes.length);
    byte[] sectionDigest = StateArchiveFileFormatV3.sha256(
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
    section.put(sectionDigest);
    section.put(offsetBytes);
    section.put(recordBytes);
    section.put(valueBytes);
    return section.array();
  }

  private DecodedPayload decodePayload(byte[] payloadBytes, int expectedGroupCount,
      long expectedEntryCount, long expectedChangedBitmap) {
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
    List<DbGroup> groups = new ArrayList<>(groupCount);
    int previousStoreId = 0;
    long actualEntryCount = 0;
    long actualChangedBitmap = 0;
    for (int index = 0; index < groupCount; index++) {
      requireRemaining(payload, StateArchiveFileFormatV3.SECTION_HEADER_LENGTH,
          "Store section header");
      int sectionStart = payload.position();
      int storeId = Short.toUnsignedInt(payload.getShort());
      if (storeId <= previousStoreId || storeId > StateArchiveFileFormatV3.STORE_COUNT) {
        throw new IllegalArgumentException("State Archive Store sections are not ordered");
      }
      previousStoreId = storeId;
      requireShort(payload, (short) 1, "section version");
      requireInt(payload, StateArchiveFileFormatV3.SECTION_HEADER_LENGTH,
          "section header length");
      int count = payload.getInt();
      if (count <= 0) {
        throw new IllegalArgumentException("State Archive Store section is empty");
      }
      requireInt(payload, 0, "section flags");
      long offsetVectorLength = payload.getLong();
      long indexRecordLength = payload.getLong();
      long valueDataLength = payload.getLong();
      long sectionLength = payload.getLong();
      byte[] sectionDigest = getBytes(payload, StateArchiveFileFormatV3.HASH_LENGTH);
      long expectedOffsetLength = checkedMultiply(count + 1L, Integer.BYTES);
      long expectedSectionLength = checkedAdd(StateArchiveFileFormatV3.SECTION_HEADER_LENGTH,
          offsetVectorLength, indexRecordLength, valueDataLength);
      if (offsetVectorLength != expectedOffsetLength || sectionLength != expectedSectionLength
          || sectionLength > payloadBytes.length
          || sectionLength > payloadBytes.length - sectionStart) {
        throw new IllegalArgumentException("State Archive Store section length is invalid");
      }
      byte[] offsetBytes = getBytes(payload, checkedInt(offsetVectorLength,
          "offsetVectorLength"));
      byte[] recordBytes = getBytes(payload, checkedInt(indexRecordLength,
          "indexRecordLength"));
      byte[] valueBytes = getBytes(payload, checkedInt(valueDataLength,
          "valueDataLength"));
      byte[] actualSectionDigest = StateArchiveFileFormatV3.sha256(
          StateArchiveFileFormatV3.SECTION_DOMAIN,
          ByteBuffer.allocate(Short.BYTES + Integer.BYTES)
              .putShort((short) storeId).putInt(count).array(),
          offsetBytes, recordBytes, valueBytes);
      requireArray(sectionDigest, actualSectionDigest, "section digest");
      List<Entry> entries = decodeEntries(count, offsetBytes, recordBytes, valueBytes);
      groups.add(new DbGroup(StateArchiveFileFormatV3.dbName(storeId), entries));
      actualEntryCount = checkedAdd(actualEntryCount, count);
      actualChangedBitmap |= 1L << (storeId - 1);
    }
    if (payload.hasRemaining() || actualEntryCount != entryCount
        || actualChangedBitmap != expectedChangedBitmap) {
      throw new IllegalArgumentException("State Archive payload coverage mismatch");
    }
    return new DecodedPayload(groups);
  }

  private List<Entry> decodeEntries(int count, byte[] offsetBytes, byte[] recordBytes,
      byte[] valueBytes) {
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
      if (previousKey != null && BlockReverseDiff.compareUnsigned(previousKey, key) >= 0) {
        throw new IllegalArgumentException("State Archive decoded keys are not ordered");
      }
      previousKey = key;
      OldValue oldValue;
      if (tag == 0) {
        if (valueOffset != UNSIGNED_INT_MAX || valueLength != UNSIGNED_INT_MAX) {
          throw new IllegalArgumentException("State Archive ABSENT value locator is invalid");
        }
        oldValue = OldValue.absent();
      } else if (tag == 1) {
        if (valueOffset != valueCursor || valueLength > valueBytes.length - valueCursor) {
          throw new IllegalArgumentException("State Archive PRESENT value locator is invalid");
        }
        byte[] value = Arrays.copyOfRange(valueBytes, valueCursor,
            valueCursor + (int) valueLength);
        valueCursor += (int) valueLength;
        oldValue = OldValue.present(value);
      } else {
        throw new IllegalArgumentException("Unknown State Archive old-value tag: " + tag);
      }
      entries.add(new Entry(key, oldValue));
    }
    if (valueCursor != valueBytes.length) {
      throw new IllegalArgumentException("State Archive value data has an unreferenced tail");
    }
    return entries;
  }

  private byte[] blockHistoryDigest(BlockSnapshotMeta meta, byte[] descriptorDigest,
      CanonicalPayload payload, byte[] payloadDigest) {
    int identityLength = 3 * Long.BYTES + 3 * StateArchiveFileFormatV3.HASH_LENGTH
        + 2 * Long.BYTES + Integer.BYTES + 2 * Long.BYTES
        + StateArchiveFileFormatV3.HASH_LENGTH;
    ByteBuffer identity = ByteBuffer.allocate(identityLength);
    identity.putLong(meta.getEpoch());
    identity.putLong(meta.getBlockNumber());
    identity.putLong(meta.getTimestamp());
    identity.put(meta.getBlockHash());
    identity.put(meta.getParentHash());
    identity.put(descriptorDigest);
    identity.putLong(StateArchiveFileFormatV3.EXACT_COVERAGE_BITMAP);
    identity.putLong(payload.changedBitmap);
    identity.putInt(payload.groupCount);
    identity.putLong(payload.entryCount);
    identity.putLong(payload.bytes.length);
    identity.put(payloadDigest);
    return StateArchiveFileFormatV3.sha256(StateArchiveFileFormatV3.BLOCK_DOMAIN,
        identity.array());
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

  private static void requireInt(ByteBuffer buffer, int expected, String name) {
    requireRemaining(buffer, Integer.BYTES, name);
    if (buffer.getInt() != expected) {
      throw new IllegalArgumentException("Invalid State Archive " + name);
    }
  }

  private static void requireShort(ByteBuffer buffer, short expected, String name) {
    requireRemaining(buffer, Short.BYTES, name);
    if (buffer.getShort() != expected) {
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
    private final List<Entry> entries;

    private StoreGroup(int storeId, List<Entry> entries) {
      this.storeId = storeId;
      this.entries = entries;
    }
  }

  private static final class CanonicalPayload {
    private final byte[] bytes;
    private final int groupCount;
    private final long entryCount;
    private final long changedBitmap;

    private CanonicalPayload(byte[] bytes, int groupCount, long entryCount,
        long changedBitmap) {
      this.bytes = bytes;
      this.groupCount = groupCount;
      this.entryCount = entryCount;
      this.changedBitmap = changedBitmap;
    }
  }

  private static final class DecodedPayload {
    private final List<DbGroup> groups;

    private DecodedPayload(List<DbGroup> groups) {
      this.groups = groups;
    }
  }

  public static final class EncodedBlock {
    private final byte[] frame;
    private final byte[] canonicalPayload;
    private final byte[] payloadDigest;
    private final byte[] blockHistoryDigest;
    private final byte[] resultHistoryDigest;
    private final byte[] encodedFrameDigest;
    private final BlockReverseDiff diff;

    private EncodedBlock(byte[] frame, byte[] canonicalPayload, byte[] payloadDigest,
        byte[] blockHistoryDigest, byte[] resultHistoryDigest, byte[] encodedFrameDigest,
        BlockReverseDiff diff) {
      this.frame = frame;
      this.canonicalPayload = canonicalPayload;
      this.payloadDigest = payloadDigest;
      this.blockHistoryDigest = blockHistoryDigest;
      this.resultHistoryDigest = resultHistoryDigest;
      this.encodedFrameDigest = encodedFrameDigest;
      this.diff = diff;
    }

    public byte[] getFrame() {
      return Arrays.copyOf(frame, frame.length);
    }

    public byte[] getCanonicalPayload() {
      return Arrays.copyOf(canonicalPayload, canonicalPayload.length);
    }

    public byte[] getPayloadDigest() {
      return Arrays.copyOf(payloadDigest, payloadDigest.length);
    }

    public byte[] getBlockHistoryDigest() {
      return Arrays.copyOf(blockHistoryDigest, blockHistoryDigest.length);
    }

    public byte[] getResultHistoryDigest() {
      return Arrays.copyOf(resultHistoryDigest, resultHistoryDigest.length);
    }

    public byte[] getEncodedFrameDigest() {
      return Arrays.copyOf(encodedFrameDigest, encodedFrameDigest.length);
    }

    public BlockReverseDiff getDiff() {
      return diff;
    }
  }

  public static final class DecodedBlock {
    private final BlockReverseDiff diff;
    private final byte[] canonicalPayload;
    private final byte[] previousHistoryDigest;
    private final byte[] payloadDigest;
    private final byte[] blockHistoryDigest;
    private final byte[] resultHistoryDigest;
    private final byte[] encodedFrameDigest;
    private final short compressionId;

    private DecodedBlock(BlockReverseDiff diff, byte[] canonicalPayload,
        byte[] previousHistoryDigest, byte[] payloadDigest, byte[] blockHistoryDigest,
        byte[] resultHistoryDigest, byte[] encodedFrameDigest, short compressionId) {
      this.diff = diff;
      this.canonicalPayload = canonicalPayload;
      this.previousHistoryDigest = previousHistoryDigest;
      this.payloadDigest = payloadDigest;
      this.blockHistoryDigest = blockHistoryDigest;
      this.resultHistoryDigest = resultHistoryDigest;
      this.encodedFrameDigest = encodedFrameDigest;
      this.compressionId = compressionId;
    }

    public BlockReverseDiff getDiff() {
      return diff;
    }

    public byte[] getCanonicalPayload() {
      return Arrays.copyOf(canonicalPayload, canonicalPayload.length);
    }

    public byte[] getPreviousHistoryDigest() {
      return Arrays.copyOf(previousHistoryDigest, previousHistoryDigest.length);
    }

    public byte[] getPayloadDigest() {
      return Arrays.copyOf(payloadDigest, payloadDigest.length);
    }

    public byte[] getBlockHistoryDigest() {
      return Arrays.copyOf(blockHistoryDigest, blockHistoryDigest.length);
    }

    public byte[] getResultHistoryDigest() {
      return Arrays.copyOf(resultHistoryDigest, resultHistoryDigest.length);
    }

    public byte[] getEncodedFrameDigest() {
      return Arrays.copyOf(encodedFrameDigest, encodedFrameDigest.length);
    }

    public short getCompressionId() {
      return compressionId;
    }
  }
}
