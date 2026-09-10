package org.tron.core.db2.archive;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;
import org.tron.core.db2.archive.StateArchiveSegmentFormatV3.BlockIndexHeader;
import org.tron.core.db2.archive.StateArchiveSegmentFormatV3.DurableMarker;
import org.tron.core.db2.archive.StateArchiveSegmentFormatV3.SealedSegment;
import org.tron.core.db2.archive.StateArchiveSegmentFormatV3.SegmentHeader;
import org.tron.core.db2.archive.StateArchiveSegmentFormatV3.SegmentManifest;
import org.tron.core.db2.archive.StateArchiveSegmentFormatV3.SegmentSeal;

public class StateArchiveSegmentFormatV3Test {

  @Test
  public void freezesSegmentLayoutAndCompositeFormatIdentity() {
    assertEquals(
        "534c44330003000000000040000202000140008000200100003000c00000000077359400"
            + "00000000040000000000040000010001000000000000000000000000",
        Hex.toHexString(StateArchiveFileFormatV3.segmentLayoutDescriptor()));
    assertEquals("d0d9c2111ddb5a30b2a97632e9c776da7eadbf81c1d08ec56ddaacab7a3ccddf",
        Hex.toHexString(StateArchiveFileFormatV3.segmentLayoutDigest()));
    assertEquals("c5464d3c1c3b2ee2d5622dabd83169db2140ac05d7fe995866bd294524b8f6a4",
        Hex.toHexString(StateArchiveFileFormatV3.compositeFormatDigest()));
  }

  @Test
  public void roundTripsByteExactSegmentHeaderAndRejectsCorruption() {
    SegmentHeader input = new SegmentHeader(22, 7, 101, hash(1), hash(2),
        StateArchiveFileFormatV3.COMPRESSION_RAW_DEFLATE_LEVEL_1);
    byte[] encoded = StateArchiveSegmentFormatV3.encodeHeader(input);
    assertEquals(StateArchiveFileFormatV3.PART_HEADER_LENGTH, encoded.length);

    ByteBuffer bytes = ByteBuffer.wrap(encoded);
    assertEquals(StateArchiveFileFormatV3.SEGMENT_MAGIC, bytes.getInt(0));
    assertEquals(22, Short.toUnsignedInt(bytes.getShort(14)));
    assertEquals(7, bytes.getLong(20));
    assertEquals(101, bytes.getLong(28));
    assertEquals(StateArchiveFileFormatV3.SEGMENT_TARGET_BYTES, bytes.getLong(44));
    assertArrayEquals(StateArchiveFileFormatV3.fiveLaneDescriptorDigest(),
        slice(encoded, 60, 32));
    assertArrayEquals(StateArchiveFileFormatV3.compositeFormatDigest(),
        slice(encoded, 92, 32));

    SegmentHeader decoded = StateArchiveSegmentFormatV3.decodeHeader(encoded);
    assertEquals(22, decoded.getLaneId());
    assertEquals(7, decoded.getSegmentSeq());
    assertEquals(101, decoded.getActualFirstBlock());
    assertArrayEquals(hash(1), decoded.getPreviousSegmentDigest());
    assertArrayEquals(hash(2), decoded.getPreviousHistoryDigest());
    assertArrayEquals(slice(encoded, 476, 32), decoded.getHeaderDigest());

    byte[] corruptReserved = encoded.clone();
    corruptReserved[200] = 1;
    assertThrows(IllegalArgumentException.class,
        () -> StateArchiveSegmentFormatV3.decodeHeader(corruptReserved));
    byte[] corruptDigest = encoded.clone();
    corruptDigest[507] ^= 1;
    assertThrows(IllegalArgumentException.class,
        () -> StateArchiveSegmentFormatV3.decodeHeader(corruptDigest));
  }

  @Test
  public void rotatesOnlyBeforeTheBlockAfterTargetIsReached() {
    long header = StateArchiveFileFormatV3.PART_HEADER_LENGTH;
    long target = 1_000;
    assertFalse(StateArchiveSegmentFormatV3.shouldRotate(0, target, target));
    assertFalse(StateArchiveSegmentFormatV3.shouldRotate(1, target - 1, target));
    assertTrue(StateArchiveSegmentFormatV3.shouldRotate(1, target, target));
    assertTrue(StateArchiveSegmentFormatV3.shouldRotate(1, target + 500, target));
    assertThrows(IllegalArgumentException.class,
        () -> StateArchiveSegmentFormatV3.shouldRotate(1, header - 1, target));
  }

  @Test
  public void roundTripsExactSealedSegmentMapRecordAndRejectsInvalidRanges() {
    SealedSegment input = new SealedSegment(4, 9, 100, 102, 3, 8_000, 224,
        hash(1), hash(2), hash(3), hash(4));
    byte[] encoded = StateArchiveSegmentFormatV3.encodeSealedMapRecord(input);
    assertEquals(192, encoded.length);
    ByteBuffer bytes = ByteBuffer.wrap(encoded);
    assertEquals(4, Short.toUnsignedInt(bytes.getShort(0)));
    assertEquals(2, Short.toUnsignedInt(bytes.getShort(2)));
    assertEquals(9, bytes.getLong(8));
    assertEquals(100, bytes.getLong(16));
    assertEquals(102, bytes.getLong(24));
    assertEquals(3, bytes.getLong(32));
    assertArrayEquals(hash(1), slice(encoded, 56, 32));
    assertArrayEquals(hash(4), slice(encoded, 152, 32));

    SealedSegment decoded = StateArchiveSegmentFormatV3.decodeSealedMapRecord(encoded);
    assertEquals(input.getLaneId(), decoded.getLaneId());
    assertEquals(input.getSegmentSeq(), decoded.getSegmentSeq());
    assertEquals(input.getFirstBlock(), decoded.getFirstBlock());
    assertEquals(input.getLastBlock(), decoded.getLastBlock());
    assertEquals(input.getBlockFrameCount(), decoded.getBlockFrameCount());
    assertArrayEquals(input.getSegmentContentDigest(), decoded.getSegmentContentDigest());

    byte[] invalid = encoded.clone();
    ByteBuffer.wrap(invalid).putLong(32, 4);
    assertThrows(IllegalArgumentException.class,
        () -> StateArchiveSegmentFormatV3.decodeSealedMapRecord(invalid));
  }

  @Test
  public void roundTripsFrozenSealedManifestAndRejectsEveryTrailerAuthority() {
    SegmentManifest input = new SegmentManifest(22, 7, 100, 102, 3, 9,
        300, 2_000, 2_880, 224, hash(1), hash(2));
    byte[] encoded = StateArchiveSegmentFormatV3.encodeManifest(input);
    assertEquals(304, encoded.length);
    ByteBuffer bytes = ByteBuffer.wrap(encoded);
    assertEquals(StateArchiveFileFormatV3.SEGMENT_MANIFEST_MAGIC, bytes.getInt(0));
    assertEquals(256, bytes.getInt(8));
    assertEquals(304, bytes.getLong(16));
    assertEquals(22, Short.toUnsignedInt(bytes.getShort(26)));
    assertEquals(7, bytes.getLong(32));
    assertEquals(100, bytes.getLong(40));
    assertEquals(102, bytes.getLong(48));
    assertArrayEquals(StateArchiveFileFormatV3.fiveLaneDescriptorDigest(),
        slice(encoded, 104, 32));
    assertArrayEquals(StateArchiveFileFormatV3.compositeFormatDigest(),
        slice(encoded, 136, 32));

    SegmentManifest decoded = StateArchiveSegmentFormatV3.decodeManifest(encoded);
    assertEquals(7, decoded.getSegmentSeq());
    assertEquals(9, decoded.getEntryCount());
    assertArrayEquals(hash(1), decoded.getPreviousSegmentDigest());
    assertArrayEquals(hash(2), decoded.getFinalHistoryDigest());
    assertArrayEquals(slice(encoded, 256, 32), decoded.getManifestDigest());

    for (int offset : new int[]{232, 260, 291, 299, 303}) {
      byte[] corrupt = encoded.clone();
      corrupt[offset] ^= 1;
      assertThrows(IllegalArgumentException.class,
          () -> StateArchiveSegmentFormatV3.decodeManifest(corrupt));
    }
  }

  @Test
  public void freezesSealDomainSegmentChainAndLaneBaselines() {
    SegmentSeal input = new SegmentSeal(0, 0, 10, 11, 2, 3,
        64, 900, 1_412, 1_780, hash(1), hash(2), hash(3), hash(4), hash(5));
    byte[] encoded = StateArchiveSegmentFormatV3.encodeSeal(input);
    assertEquals(368, encoded.length);
    ByteBuffer bytes = ByteBuffer.wrap(encoded);
    assertEquals(StateArchiveFileFormatV3.PART_SEAL_FRAME_TYPE, bytes.getShort(8));
    assertEquals(0, Short.toUnsignedInt(bytes.getShort(32)));
    assertEquals(0, bytes.getLong(36));
    assertEquals(10, bytes.getLong(44));
    assertEquals(11, bytes.getLong(52));
    assertEquals(1_412, bytes.getLong(92));
    assertEquals(1_780, bytes.getLong(100));

    SegmentSeal decoded = StateArchiveSegmentFormatV3.decodeSeal(encoded);
    assertEquals(11, decoded.getActualLastBlock());
    assertArrayEquals(slice(encoded, 320, 32), decoded.getEncodedFrameDigest());
    byte[] chain = StateArchiveSegmentFormatV3.segmentChainDigest(0, 0,
        hash(6), decoded.getSegmentContentDigest(), decoded.getEncodedFrameDigest());
    assertEquals(
        "3147cb009a99874675be38717a31e10655fe0be89fe5278bcb2503caa9385d2a"
            + ":8f19c3370ce7750bcfad451d7d14de70cef356f1dd84a1ffda8c826cf2f70ae8"
            + ":8a34caee97c24ec70adf60393672986423070d506935d9593e000594310a6798"
            + ":a33fecda61e5cf29483fe4df26defa471ce3e5e6e0905c9ebfebeffe8932e0e8",
        Hex.toHexString(StateArchiveSegmentFormatV3.laneBaselineDigest(0)) + ":"
            + Hex.toHexString(StateArchiveSegmentFormatV3.laneBaselineDigest(4)) + ":"
            + Hex.toHexString(decoded.getEncodedFrameDigest()) + ":"
            + Hex.toHexString(chain));

    byte[] corrupt = encoded.clone();
    corrupt[319] ^= 1;
    assertThrows(IllegalArgumentException.class,
        () -> StateArchiveSegmentFormatV3.decodeSeal(corrupt));
  }

  @Test
  public void freezesCompleteBlockIndexHeader() {
    BlockIndexHeader input = new BlockIndexHeader(22, 7, hash(9));
    byte[] encoded = StateArchiveSegmentFormatV3.encodeBlockIndexHeader(input);
    assertEquals(128, encoded.length);
    ByteBuffer bytes = ByteBuffer.wrap(encoded);
    assertEquals(StateArchiveFileFormatV3.BLOCK_INDEX_MAGIC, bytes.getInt(0));
    assertEquals(128, Short.toUnsignedInt(bytes.getShort(8)));
    assertEquals(32, Short.toUnsignedInt(bytes.getShort(10)));
    assertEquals(22, Short.toUnsignedInt(bytes.getShort(12)));
    assertEquals(7, bytes.getLong(16));
    assertArrayEquals(hash(9), slice(encoded, 24, 32));
    assertArrayEquals(StateArchiveFileFormatV3.fiveLaneDescriptorDigest(),
        slice(encoded, 56, 32));

    BlockIndexHeader decoded = StateArchiveSegmentFormatV3.decodeBlockIndexHeader(encoded);
    assertEquals(22, decoded.getLaneId());
    assertEquals(7, decoded.getSegmentSeq());
    assertArrayEquals(hash(9), decoded.getDataSegmentHeaderDigest());
    assertArrayEquals(slice(encoded, 92, 32), decoded.getHeaderDigest());
    assertEquals(
        "5342493300030000008000200016000200000000000000070000000000000000"
            + "0000000000000000000000000000000000000000000000099a9e46c5b7fea88a"
            + "262bc7e4e52582b0c0d42bd6e5e454001c1d571b77076c4a00000000c055e205"
            + "9140d1afec27340529f6685ebcda60627e2a594616f08aa53bb6d37708bc208f",
        Hex.toHexString(encoded));

    byte[] corrupt = encoded.clone();
    corrupt[88] = 1;
    assertThrows(IllegalArgumentException.class,
        () -> StateArchiveSegmentFormatV3.decodeBlockIndexHeader(corrupt));
  }

  @Test
  public void roundTripsFiveLaneDurableMarkerAndRejectsDrift() {
    DurableMarker input = new DurableMarker(7, 10, 11, 10, 11, hash(1), hash(2),
        512, 1_748, 2, 64, 900, hash(3), hash(4), 22, 5);
    byte[] encoded = StateArchiveSegmentFormatV3.encodeDurableMarker(input);
    assertEquals(336, encoded.length);
    ByteBuffer bytes = ByteBuffer.wrap(encoded);
    assertEquals(StateArchiveFileFormatV3.DURABLE_MARKER_FRAME_TYPE, bytes.getShort(8));
    assertEquals(288, bytes.getInt(12));
    assertEquals(7, bytes.getLong(32));
    assertEquals(10, bytes.getLong(56));
    assertEquals(11, bytes.getLong(64));
    assertArrayEquals(StateArchiveFileFormatV3.fiveLaneDescriptorDigest(),
        slice(encoded, 104, 32));
    assertEquals(22, Short.toUnsignedInt(bytes.getShort(272)));
    assertEquals(5, bytes.getLong(276));

    DurableMarker decoded = StateArchiveSegmentFormatV3.decodeDurableMarker(encoded);
    assertEquals(7, decoded.getCheckpointSequence());
    assertEquals(10, decoded.getFirstBlock());
    assertEquals(11, decoded.getLastBlock());
    assertEquals(512, decoded.getCoveredStartOffset());
    assertEquals(1_748, decoded.getMarkerEndOffset());
    assertEquals(22, decoded.getLaneId());
    assertEquals(5, decoded.getSegmentSeq());
    assertArrayEquals(slice(encoded, 288, 32), decoded.getEncodedFrameDigest());

    byte[] corrupt = encoded.clone();
    corrupt[240] ^= 1;
    assertThrows(IllegalArgumentException.class,
        () -> StateArchiveSegmentFormatV3.decodeDurableMarker(corrupt));
  }

  private static byte[] hash(int suffix) {
    byte[] result = new byte[32];
    result[31] = (byte) suffix;
    return result;
  }

  private static byte[] slice(byte[] bytes, int offset, int length) {
    byte[] result = new byte[length];
    System.arraycopy(bytes, offset, result, 0, length);
    return result;
  }
}
