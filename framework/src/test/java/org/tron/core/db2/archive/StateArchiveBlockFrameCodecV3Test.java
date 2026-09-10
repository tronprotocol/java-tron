package org.tron.core.db2.archive;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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

public class StateArchiveBlockFrameCodecV3Test {

  private static final String FORMAT_DESCRIPTOR_HEX =
      "464d543300030000000000600150012001400030020000800020010000a000300020"
          + "0050001000010000000100030001000100010001000040000000000077359400"
          + "00000000040000000000000007ffffff0000000000000000000000000000";

  private final StateArchiveBlockFrameCodecV3 codec =
      new StateArchiveBlockFrameCodecV3();

  @Test
  public void freezesFormatDescriptorAndEmptyBlockGoldenFrame() {
    assertEquals(96, StateArchiveFileFormatV3.formatDescriptor().length);
    assertEquals(FORMAT_DESCRIPTOR_HEX,
        hex(StateArchiveFileFormatV3.formatDescriptor()));
    assertEquals("380d370ba9adf724ae313c05ede5625749c82ab1240ef5f8b510e3aa5c4f649a",
        hex(StateArchiveFileFormatV3.formatDigest()));
    assertEquals("19c7827c66ad7811d60a964d3a24b85a0b2ed41119bc5eedf444835f738159c8",
        hex(StateArchiveFileFormatV3.storeDescriptorDigest()));

    StateArchiveBlockFrameCodecV3.EncodedBlock encoded = codec.encode(
        diff(12, Collections.emptyList()), hash(10),
        StateArchiveFileFormatV3.COMPRESSION_NONE);
    assertEquals(416, encoded.getFrame().length);
    assertEquals(32, encoded.getCanonicalPayload().length);
    assertEquals("3123bbd47cd083892c1c5cd9e38eb016081d47dc238ddc5757ccb7e4112f63fb",
        hex(StateArchiveFileFormatV3.sha256(encoded.getFrame())));

    StateArchiveBlockFrameCodecV3.DecodedBlock decoded = codec.decode(encoded.getFrame());
    assertEquals(diff(12, Collections.emptyList()).getMeta(), decoded.getDiff().getMeta());
    assertTrue(decoded.getDiff().getGroups().isEmpty());
  }

  @Test
  public void canonicalizesStoreIdsAndUnsignedKeysAndPreservesValueStates() {
    List<Entry> storeOneEntries = Arrays.asList(
        new Entry(bytes(0xff), OldValue.present(bytes(4, 5))),
        new Entry(bytes(0x00, 0x01), OldValue.absent()),
        new Entry(bytes(0x80), OldValue.present(new byte[0])),
        new Entry(bytes(0x00), OldValue.present(bytes(7))));
    BlockReverseDiff first = diff(12, Arrays.asList(
        new DbGroup("IncrementalMerkleTree", Collections.singletonList(
            new Entry(bytes(0x7f), OldValue.absent()))),
        new DbGroup("abi", storeOneEntries)));
    List<Entry> reversedEntries = new ArrayList<>(storeOneEntries);
    Collections.reverse(reversedEntries);
    BlockReverseDiff reordered = diff(12, Arrays.asList(
        new DbGroup("abi", reversedEntries),
        new DbGroup("IncrementalMerkleTree", Collections.singletonList(
            new Entry(bytes(0x7f), OldValue.absent())))));

    StateArchiveBlockFrameCodecV3.EncodedBlock firstFrame = codec.encode(first, hash(10),
        StateArchiveFileFormatV3.COMPRESSION_NONE);
    StateArchiveBlockFrameCodecV3.EncodedBlock reorderedFrame = codec.encode(reordered,
        hash(10), StateArchiveFileFormatV3.COMPRESSION_NONE);
    assertArrayEquals(firstFrame.getFrame(), reorderedFrame.getFrame());

    ByteBuffer payload = ByteBuffer.wrap(firstFrame.getCanonicalPayload());
    payload.position(StateArchiveFileFormatV3.PAYLOAD_HEADER_LENGTH);
    assertEquals(1, Short.toUnsignedInt(payload.getShort()));
    StateArchiveBlockFrameCodecV3.DecodedBlock decoded = codec.decode(firstFrame.getFrame());
    assertEquivalent(first, decoded.getDiff());
    List<Entry> decodedStoreOne = group(decoded.getDiff(), "abi").getEntries();
    assertArrayEquals(bytes(0x00), decodedStoreOne.get(0).getKey());
    assertArrayEquals(bytes(0x00, 0x01), decodedStoreOne.get(1).getKey());
    assertArrayEquals(bytes(0x80), decodedStoreOne.get(2).getKey());
    assertArrayEquals(bytes(0xff), decodedStoreOne.get(3).getKey());
    assertTrue(decodedStoreOne.get(0).getOldValue().isPresent());
    assertFalse(decodedStoreOne.get(1).getOldValue().isPresent());
    assertEquals(0, decodedStoreOne.get(2).getOldValue().getValue().length);
  }

  @Test
  public void compressionChangesPhysicalFrameButNotLogicalIdentity() {
    BlockReverseDiff diff = diff(42, Collections.singletonList(
        new DbGroup("account", Collections.singletonList(
            new Entry(bytes(1, 2, 3), OldValue.present(new byte[4096]))))));
    StateArchiveBlockFrameCodecV3.EncodedBlock none = codec.encode(diff, hash(41),
        StateArchiveFileFormatV3.COMPRESSION_NONE);
    StateArchiveBlockFrameCodecV3.EncodedBlock compressed = codec.encode(diff, hash(41),
        StateArchiveFileFormatV3.COMPRESSION_RAW_DEFLATE_LEVEL_1);

    assertFalse(Arrays.equals(none.getFrame(), compressed.getFrame()));
    assertTrue(compressed.getFrame().length < none.getFrame().length);
    assertArrayEquals(none.getCanonicalPayload(), compressed.getCanonicalPayload());
    assertArrayEquals(none.getPayloadDigest(), compressed.getPayloadDigest());
    assertArrayEquals(none.getBlockHistoryDigest(), compressed.getBlockHistoryDigest());
    assertArrayEquals(none.getResultHistoryDigest(), compressed.getResultHistoryDigest());
    assertFalse(Arrays.equals(none.getEncodedFrameDigest(),
        compressed.getEncodedFrameDigest()));
    assertEquivalent(diff, codec.decode(compressed.getFrame()).getDiff());
  }

  @Test
  public void matchesLegacyHotBodyAtStoreKeyAndOldValueBoundary() {
    BlockReverseDiff expected = diff(77, Arrays.asList(
        new DbGroup("votes", Arrays.asList(
            new Entry(bytes(0x80), OldValue.present(bytes(9))),
            new Entry(bytes(0xff), OldValue.absent()))),
        new DbGroup("account", Arrays.asList(
            new Entry(bytes(0), OldValue.present(new byte[0])),
            new Entry(bytes(0, 1), OldValue.present(bytes(3, 4)))))));
    BlockReverseDiff legacy = new BlockHistoryCodec().decode(
        new BlockHistoryCodec().encode(expected));
    BlockReverseDiff v3 = codec.decode(codec.encode(expected, hash(76),
        StateArchiveFileFormatV3.COMPRESSION_NONE).getFrame()).getDiff();

    assertEquivalent(legacy, v3);
  }

  @Test
  public void rejectsDuplicateStoreKeyCorruptionAndIdentityDrift() {
    assertThrows(ArchivePersistenceException.class,
        () -> StateArchiveFileFormatV3.requireExactCapture(
            Collections.singletonList("account")));

    BlockReverseDiff duplicateStore = diff(12, Arrays.asList(
        new DbGroup("account", Collections.singletonList(
            new Entry(bytes(1), OldValue.absent()))),
        new DbGroup("account", Collections.singletonList(
            new Entry(bytes(2), OldValue.absent())))));
    assertThrows(IllegalArgumentException.class, () -> codec.encode(duplicateStore, hash(11),
        StateArchiveFileFormatV3.COMPRESSION_NONE));

    BlockReverseDiff duplicateKey = diff(12, Collections.singletonList(
        new DbGroup("account", Arrays.asList(
            new Entry(bytes(1), OldValue.absent()),
            new Entry(bytes(1), OldValue.present(bytes(2)))))));
    assertThrows(IllegalArgumentException.class, () -> codec.encode(duplicateKey, hash(11),
        StateArchiveFileFormatV3.COMPRESSION_NONE));

    BlockReverseDiff epochDrift = new BlockReverseDiff(new BlockSnapshotMeta(
        11, 12, hash(12), hash(11), 36_000L), Collections.emptyList());
    assertThrows(IllegalArgumentException.class, () -> codec.encode(epochDrift, hash(10),
        StateArchiveFileFormatV3.COMPRESSION_NONE));

    byte[] frame = codec.encode(diff(12, Collections.emptyList()), hash(10),
        StateArchiveFileFormatV3.COMPRESSION_NONE).getFrame();
    byte[] corrupted = Arrays.copyOf(frame, frame.length);
    corrupted[StateArchiveFileFormatV3.BLOCK_HEADER_LENGTH] ^= 1;
    assertThrows(IllegalArgumentException.class, () -> codec.decode(corrupted));
    assertThrows(IllegalArgumentException.class,
        () -> codec.decode(Arrays.copyOf(frame, frame.length - 1)));
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
