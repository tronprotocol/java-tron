package org.tron.core.db2.archive;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.tron.core.db2.archive.BlockReverseDiff.DbGroup;
import org.tron.core.db2.archive.BlockReverseDiff.Entry;
import org.tron.core.db2.archive.StateArchiveFiveLaneBlockCodecV3.DecodedBundle;
import org.tron.core.db2.archive.StateArchiveFiveLaneBlockCodecV3.EncodedBundle;
import org.tron.core.db2.archive.StateArchiveFiveLaneBlockCodecV3.EncodedLane;

public class StateArchiveFiveLaneBlockCodecV3Test {

  private static final String EMPTY_BUNDLE_GOLDEN =
      "9a9e46c5b7fea88a262bc7e4e52582b0c0d42bd6e5e454001c1d571b77076c4a"
          + ":2c6ef2d937c331ce2d8dbdf9f0cfbb5656addc43bb1720259b3cac873d6166f5"
          + ":da6987a93e4fdcd3550e17d5abe60152bce13ca674d18e92e108b83f55550485"
          + ":ef3b67f170ae5154c8fbe8b7021695a46492146686e3557bedf0c1763b5fd2b1"
          + ":2f50e5993055074c70ee5d32b5f7208782ef9ee2fa5cf1f94b4319ae0fa401dd"
          + ":937abd4c413f25d5e9ed4147339b0a9b9b17a22e86171df82d347bf861aa9a7b"
          + ":d5c21252a7b74ee1ea037dcf891a81918673d14c953ada93a26a02b2f44bc9c5"
          + ":72c3cadb4dad71dc9f230afd2bd306d442325a2705e325e95083fc28c9470dcc";

  private final StateArchiveFiveLaneBlockCodecV3 codec =
      new StateArchiveFiveLaneBlockCodecV3();

  @Test
  public void emitsFiveEmptyFramesWithExactDisjointCoverage() {
    EncodedBundle bundle = codec.encode(diff(12, Collections.emptyList()), hash(11),
        StateArchiveFileFormatV3.COMPRESSION_NONE);

    assertEquals(5, bundle.getLanes().size());
    long coverage = 0;
    int[] laneIds = StateArchiveFileFormatV3.fiveLaneIds();
    for (int index = 0; index < laneIds.length; index++) {
      EncodedLane lane = bundle.getLanes().get(index);
      int laneId = laneIds[index];
      assertEquals(laneId, lane.getLaneId());
      assertEquals(StateArchiveFileFormatV3.laneBodyCodec(laneId), lane.getBodyCodec());
      assertEquals(32, lane.getCanonicalPayload().length);
      assertEquals(0, coverage & lane.getCoverageBitmap());
      coverage |= lane.getCoverageBitmap();
    }
    assertEquals(StateArchiveFileFormatV3.EXACT_COVERAGE_BITMAP, coverage);
    assertEquals(StateArchiveFileFormatV3.MIXED_LANE_COVERAGE_BITMAP,
        bundle.getLanes().get(0).getCoverageBitmap());
    assertFalse(Arrays.equals(StateArchiveFileFormatV3.storeDescriptorDigest(),
        StateArchiveFileFormatV3.fiveLaneDescriptorDigest()));
    StringBuilder golden = new StringBuilder()
        .append(hex(StateArchiveFileFormatV3.fiveLaneDescriptorDigest()));
    for (EncodedLane lane : bundle.getLanes()) {
      golden.append(':').append(hex(StateArchiveFileFormatV3.sha256(lane.getFrame())));
    }
    golden.append(':').append(hex(bundle.getBlockHistoryDigest()))
        .append(':').append(hex(bundle.getResultHistoryDigest()));
    assertEquals(EMPTY_BUNDLE_GOLDEN, golden.toString());

    DecodedBundle decoded = codec.decode(frames(bundle));
    assertEquivalent(diff(12, Collections.emptyList()), decoded.getDiff());
    assertArrayEquals(bundle.getBlockHistoryDigest(), decoded.getBlockHistoryDigest());
    assertArrayEquals(bundle.getResultHistoryDigest(), decoded.getResultHistoryDigest());
  }

  @Test
  public void roundTripsVariableAndFixedWidthLanes() {
    BlockReverseDiff input = diff(42, Arrays.asList(
        new DbGroup("abi", Arrays.asList(
            new Entry(bytes(0), OldValue.absent()),
            new Entry(bytes(0xff), OldValue.present(bytes(1))))),
        new DbGroup("account", Arrays.asList(
            new Entry(fixedKey(21, 1), OldValue.absent()),
            new Entry(fixedKey(21, 2), OldValue.present(new byte[0])),
            new Entry(fixedKey(21, 3), OldValue.present(bytes(7, 8))))),
        new DbGroup("account-asset", Arrays.asList(
            new Entry(fixedKey(22, 1), OldValue.present(bytes(9))),
            new Entry(fixedKey(29, 2), OldValue.absent()))),
        new DbGroup("delegation", Arrays.asList(
            new Entry(bytes(0, 0x80), OldValue.present(bytes(3))),
            new Entry(bytes(0xff), OldValue.absent()))),
        new DbGroup("storage-row", Arrays.asList(
            new Entry(fixedKey(32, 4), OldValue.present(bytes(5))),
            new Entry(fixedKey(32, 5), OldValue.absent())))));

    EncodedBundle bundle = codec.encode(input, hash(41),
        StateArchiveFileFormatV3.COMPRESSION_NONE);
    EncodedLane account = lane(bundle, 4);
    EncodedLane accountAsset = lane(bundle, 5);
    EncodedLane storage = lane(bundle, 22);
    assertEquals(StateArchiveFileFormatV3.DEDICATED_FIXED_WIDTH_BODY_CODEC_ID,
        account.getBodyCodec());
    assertEquals(StateArchiveFileFormatV3.LANE_VARIABLE_BODY_CODEC_ID,
        accountAsset.getBodyCodec());
    assertEquals(StateArchiveFileFormatV3.DEDICATED_FIXED_WIDTH_BODY_CODEC_ID,
        storage.getBodyCodec());
    assertFixedSection(account.getCanonicalPayload(), 4, 21, 3);
    assertVariableSection(accountAsset.getCanonicalPayload(), 5, 2);
    assertFixedSection(storage.getCanonicalPayload(), 22, 32, 2);

    byte[] u0AccountPayload = new StateArchiveBlockFrameCodecV3().encode(
        diff(42, Collections.singletonList(group(input, "account"))), hash(41),
        StateArchiveFileFormatV3.COMPRESSION_NONE).getCanonicalPayload();
    assertTrue(account.getCanonicalPayload().length < u0AccountPayload.length);
    assertEquivalent(input, codec.decode(frames(bundle)).getDiff());
  }

  @Test
  public void roundTripsEveryExact27StoreThroughItsAssignedLane() {
    ArchiveParticipantDescriptor descriptor = ArchiveParticipantDescriptor.current();
    List<DbGroup> groups = new ArrayList<>();
    for (String dbName : descriptor.getActiveDatabases()) {
      int storeId = descriptor.getStoreId(dbName);
      int laneId = StateArchiveFileFormatV3.laneId(storeId);
      int keyWidth = StateArchiveFileFormatV3.fixedKeyWidth(laneId);
      byte[] key = keyWidth == 0
          ? bytes(storeId, 0xff - storeId)
          : fixedKey(keyWidth, storeId);
      OldValue oldValue = (storeId & 1) == 0
          ? OldValue.absent()
          : OldValue.present(bytes(storeId));
      groups.add(new DbGroup(dbName,
          Collections.singletonList(new Entry(key, oldValue))));
    }
    BlockReverseDiff input = diff(64, groups);

    EncodedBundle bundle = codec.encode(input, hash(63),
        StateArchiveFileFormatV3.COMPRESSION_RAW_DEFLATE_LEVEL_1);

    assertEquals(5, bundle.getLanes().size());
    assertEquivalent(input, codec.decode(frames(bundle)).getDiff());
  }

  @Test
  public void compressionKeepsBundleIdentityAndCanonicalLanePayloads() {
    BlockReverseDiff input = diff(77, Arrays.asList(
        new DbGroup("account", Collections.singletonList(
            new Entry(fixedKey(21, 1), OldValue.present(new byte[4096])))),
        new DbGroup("votes", Collections.singletonList(
            new Entry(bytes(0x80), OldValue.present(new byte[2048]))))));
    EncodedBundle none = codec.encode(input, hash(76),
        StateArchiveFileFormatV3.COMPRESSION_NONE);
    EncodedBundle compressed = codec.encode(input, hash(76),
        StateArchiveFileFormatV3.COMPRESSION_RAW_DEFLATE_LEVEL_1);

    assertArrayEquals(none.getBlockHistoryDigest(), compressed.getBlockHistoryDigest());
    assertArrayEquals(none.getResultHistoryDigest(), compressed.getResultHistoryDigest());
    boolean physicalDifference = false;
    for (int index = 0; index < none.getLanes().size(); index++) {
      assertArrayEquals(none.getLanes().get(index).getCanonicalPayload(),
          compressed.getLanes().get(index).getCanonicalPayload());
      physicalDifference |= !Arrays.equals(none.getLanes().get(index).getFrame(),
          compressed.getLanes().get(index).getFrame());
    }
    assertTrue(physicalDifference);
    assertEquivalent(input, codec.decode(frames(compressed)).getDiff());
  }

  @Test
  public void rejectsInvalidFixedKeysIncompleteAndMixedBundles() {
    BlockReverseDiff invalidAccount = diff(12, Collections.singletonList(
        new DbGroup("account", Collections.singletonList(
            new Entry(fixedKey(20, 1), OldValue.absent())))));
    assertThrows(IllegalArgumentException.class, () -> codec.encode(
        invalidAccount, hash(11), StateArchiveFileFormatV3.COMPRESSION_NONE));
    BlockReverseDiff invalidStorage = diff(12, Collections.singletonList(
        new DbGroup("storage-row", Collections.singletonList(
            new Entry(fixedKey(33, 1), OldValue.absent())))));
    assertThrows(IllegalArgumentException.class, () -> codec.encode(
        invalidStorage, hash(11), StateArchiveFileFormatV3.COMPRESSION_NONE));

    EncodedBundle first = codec.encode(diff(12, Collections.emptyList()), hash(11),
        StateArchiveFileFormatV3.COMPRESSION_NONE);
    List<byte[]> missing = frames(first);
    missing.remove(0);
    assertThrows(IllegalArgumentException.class, () -> codec.decode(missing));

    List<byte[]> duplicate = frames(first);
    duplicate.set(4, duplicate.get(0));
    assertThrows(IllegalArgumentException.class, () -> codec.decode(duplicate));

    EncodedBundle second = codec.encode(diff(13, Collections.emptyList()), hash(12),
        StateArchiveFileFormatV3.COMPRESSION_NONE);
    List<byte[]> mixed = frames(first);
    mixed.set(2, second.getLanes().get(2).getFrame());
    assertThrows(IllegalArgumentException.class, () -> codec.decode(mixed));

    List<byte[]> corrupted = frames(first);
    byte[] bad = corrupted.get(1);
    bad[StateArchiveFileFormatV3.BLOCK_HEADER_LENGTH] ^= 1;
    assertThrows(IllegalArgumentException.class, () -> codec.decode(corrupted));
  }

  @Test
  public void bundleEncodingIsDeterministicAndMatchesLegacySemanticOracle() {
    List<Entry> entries = Arrays.asList(
        new Entry(bytes(0xff), OldValue.absent()),
        new Entry(bytes(0), OldValue.present(new byte[0])),
        new Entry(bytes(0x80), OldValue.present(bytes(3, 4))));
    BlockReverseDiff input = diff(88, Arrays.asList(
        new DbGroup("votes", entries),
        new DbGroup("abi", Collections.singletonList(
            new Entry(bytes(7), OldValue.present(bytes(1)))))));
    List<Entry> reversedEntries = new ArrayList<>(entries);
    Collections.reverse(reversedEntries);
    BlockReverseDiff reordered = diff(88, Arrays.asList(
        new DbGroup("abi", Collections.singletonList(
            new Entry(bytes(7), OldValue.present(bytes(1))))),
        new DbGroup("votes", reversedEntries)));

    EncodedBundle first = codec.encode(input, hash(87),
        StateArchiveFileFormatV3.COMPRESSION_NONE);
    EncodedBundle second = codec.encode(reordered, hash(87),
        StateArchiveFileFormatV3.COMPRESSION_NONE);
    for (int index = 0; index < first.getLanes().size(); index++) {
      assertArrayEquals(first.getLanes().get(index).getFrame(),
          second.getLanes().get(index).getFrame());
    }
    BlockReverseDiff legacy = new BlockHistoryCodec().decode(
        new BlockHistoryCodec().encode(input));
    assertEquivalent(legacy, codec.decode(frames(first)).getDiff());
    assertNotEquals(hex(StateArchiveFileFormatV3.formatDigest()),
        hex(StateArchiveFileFormatV3.fiveLaneDescriptorDigest()));
  }

  private static void assertFixedSection(byte[] payloadBytes, int storeId,
      int keyWidth, int entryCount) {
    ByteBuffer payload = ByteBuffer.wrap(payloadBytes);
    payload.position(StateArchiveFileFormatV3.PAYLOAD_HEADER_LENGTH);
    assertEquals(storeId, Short.toUnsignedInt(payload.getShort()));
    assertEquals(2, Short.toUnsignedInt(payload.getShort()));
    assertEquals(96, payload.getInt());
    assertEquals(entryCount, payload.getInt());
    assertEquals(0, payload.getInt());
    assertEquals(keyWidth, payload.getInt());
  }

  private static void assertVariableSection(byte[] payloadBytes, int storeId,
      int entryCount) {
    ByteBuffer payload = ByteBuffer.wrap(payloadBytes);
    payload.position(StateArchiveFileFormatV3.PAYLOAD_HEADER_LENGTH);
    assertEquals(storeId, Short.toUnsignedInt(payload.getShort()));
    assertEquals(1, Short.toUnsignedInt(payload.getShort()));
    assertEquals(StateArchiveFileFormatV3.SECTION_HEADER_LENGTH, payload.getInt());
    assertEquals(entryCount, payload.getInt());
  }

  private static EncodedLane lane(EncodedBundle bundle, int laneId) {
    for (EncodedLane lane : bundle.getLanes()) {
      if (lane.getLaneId() == laneId) {
        return lane;
      }
    }
    throw new AssertionError("Missing lane " + laneId);
  }

  private static List<byte[]> frames(EncodedBundle bundle) {
    List<byte[]> frames = new ArrayList<>();
    for (EncodedLane lane : bundle.getLanes()) {
      frames.add(lane.getFrame());
    }
    return frames;
  }

  private static BlockReverseDiff diff(long blockNumber, List<DbGroup> groups) {
    return new BlockReverseDiff(BlockSnapshotMeta.forBlock(
        blockNumber, hash((int) blockNumber), hash((int) blockNumber - 1),
        blockNumber * 3_000L), groups);
  }

  private static DbGroup group(BlockReverseDiff diff, String dbName) {
    for (DbGroup group : diff.getGroups()) {
      if (group.getDbName().equals(dbName)) {
        return group;
      }
    }
    throw new AssertionError("Missing group " + dbName);
  }

  private static void assertEquivalent(BlockReverseDiff expected, BlockReverseDiff actual) {
    assertEquals(expected.getMeta(), actual.getMeta());
    assertEquals(expected.getGroups().size(), actual.getGroups().size());
    for (int groupIndex = 0; groupIndex < expected.getGroups().size(); groupIndex++) {
      DbGroup expectedGroup = expected.getGroups().get(groupIndex);
      DbGroup actualGroup = actual.getGroups().get(groupIndex);
      assertEquals(expectedGroup.getDbName(), actualGroup.getDbName());
      assertEquals(expectedGroup.getEntries().size(), actualGroup.getEntries().size());
      for (int entryIndex = 0; entryIndex < expectedGroup.getEntries().size(); entryIndex++) {
        Entry expectedEntry = expectedGroup.getEntries().get(entryIndex);
        Entry actualEntry = actualGroup.getEntries().get(entryIndex);
        assertArrayEquals(expectedEntry.getKey(), actualEntry.getKey());
        assertEquals(expectedEntry.getOldValue(), actualEntry.getOldValue());
      }
    }
  }

  private static byte[] hash(int suffix) {
    byte[] hash = new byte[32];
    hash[31] = (byte) suffix;
    return hash;
  }

  private static byte[] fixedKey(int length, int suffix) {
    byte[] key = new byte[length];
    key[length - 1] = (byte) suffix;
    return key;
  }

  private static byte[] bytes(int... values) {
    byte[] bytes = new byte[values.length];
    for (int index = 0; index < values.length; index++) {
      bytes[index] = (byte) values[index];
    }
    return bytes;
  }

  private static String hex(byte[] bytes) {
    StringBuilder result = new StringBuilder(bytes.length * 2);
    for (byte value : bytes) {
      result.append(String.format("%02x", value & 0xff));
    }
    return result.toString();
  }
}
