package org.tron.core.db2.archive;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;
import org.tron.core.db2.archive.StateArchiveFiveLaneRecoveryIntentV3.Intent;
import org.tron.core.db2.archive.StateArchiveFiveLaneRecoveryIntentV3.LaneTarget;
import org.tron.core.db2.archive.StateArchiveFiveLaneRecoveryIntentV3.RecoveryPoint;

public class StateArchiveFiveLaneRecoveryIntentV3Test {

  @Test
  public void freezesLayoutAndRoundTripsCompleteFiveLanePlan() {
    Intent intent = intent();
    byte[] encoded = StateArchiveFiveLaneRecoveryIntentV3.encode(intent);
    assertEquals(1_616, encoded.length);
    ByteBuffer bytes = ByteBuffer.wrap(encoded);
    assertEquals(StateArchiveFileFormatV3.RECOVERY_INTENT_MAGIC, bytes.getInt(0));
    assertEquals(768, bytes.getInt(8));
    assertEquals(160, Short.toUnsignedInt(bytes.getShort(12)));
    assertEquals(5, Short.toUnsignedInt(bytes.getShort(14)));
    assertEquals(1_616, bytes.getLong(16));
    assertEquals(3, Short.toUnsignedInt(bytes.getShort(26)));
    assertArrayEquals(StateArchiveFileFormatV3.compositeFormatDigest(),
        slice(encoded, 32, 32));
    assertArrayEquals(StateArchiveFileFormatV3.fiveLaneDescriptorDigest(),
        slice(encoded, 64, 32));
    assertEquals(12, bytes.getLong(136));
    assertEquals(10, bytes.getLong(256));
    assertEquals(11, bytes.getLong(376));
    assertEquals(0, Short.toUnsignedInt(bytes.getShort(768)));
    assertEquals(22, Short.toUnsignedInt(bytes.getShort(768 + 4 * 160)));

    Intent decoded = StateArchiveFiveLaneRecoveryIntentV3.decode(encoded);
    assertEquals(12, decoded.getAuthorizedCeiling().getBlockNumber());
    assertEquals(10, decoded.getCommonCommitted().getBlockNumber());
    assertEquals(11, decoded.getTarget().getBlockNumber());
    assertEquals(5, decoded.getLanes().size());
    assertEquals(StateArchiveFiveLaneRecoveryIntentV3.NO_TARGET_SEGMENT,
        decoded.getLanes().get(4).getTargetSegmentSeq());
    assertArrayEquals(slice(encoded, 732, 32), decoded.getHeaderDigest());
    assertArrayEquals(slice(encoded, 488, 32), decoded.getRecordsDigest());
    assertArrayEquals(slice(encoded, 1_568, 32), decoded.getIntentDigest());

    assertEquals(
        "524944330003000000000020030000a000050030000006500001000100010001"
            + ":e19713bb16c631eafb76b4fdd82101440de0e842e6c70e369f289934402e364b"
            + ":c5464d3c1c3b2ee2d5622dabd83169db2140ac05d7fe995866bd294524b8f6a4"
            + ":6399bb2ce2bdf435c2fd5b080c2eaade83e4fdd1ba00970495da96b41e186463",
        Hex.toHexString(StateArchiveFileFormatV3.recoveryIntentLayoutDescriptor()) + ":"
            + Hex.toHexString(StateArchiveFileFormatV3.recoveryIntentLayoutDigest()) + ":"
            + Hex.toHexString(StateArchiveFileFormatV3.compositeFormatDigest()) + ":"
            + Hex.toHexString(StateArchiveFileFormatV3.sha256(encoded)));
  }

  @Test
  public void rejectsHeaderRecordTrailerAndPointDrift() {
    byte[] encoded = StateArchiveFiveLaneRecoveryIntentV3.encode(intent());
    for (int offset : new int[]{600, 800, 1_615}) {
      byte[] corrupt = encoded.clone();
      corrupt[offset] ^= 1;
      assertThrows(IllegalArgumentException.class,
          () -> StateArchiveFiveLaneRecoveryIntentV3.decode(corrupt));
    }

    List<LaneTarget> wrongOrder = lanes();
    java.util.Collections.swap(wrongOrder, 0, 1);
    assertThrows(IllegalArgumentException.class, () -> new Intent(hash(90), point(12),
        point(10), point(11), wrongOrder));
    assertThrows(IllegalArgumentException.class, () -> new Intent(hash(90), point(12),
        point(11), point(10), lanes()));
    assertThrows(IllegalArgumentException.class, () -> new Intent(hash(90), point(11),
        point(11), new RecoveryPoint(11, 11, 33_000, hash(99), hash(10), hash(111)),
        lanes()));
    assertThrows(IllegalArgumentException.class, () -> new Intent(hash(90), point(12),
        point(10), null, lanes()));
  }

  @Test
  public void representsBaselineTargetAndLanesNotYetCreated() {
    byte[] zero = new byte[32];
    List<LaneTarget> baselineLanes = Arrays.asList(
        new LaneTarget(0, StateArchiveFiveLaneRecoveryIntentV3.DELETE_PAIR,
            0, 700, 128, StateArchiveFiveLaneRecoveryIntentV3.NO_TARGET_SEGMENT,
            0, 0, hash(1), zero, zero),
        missingLane(4), missingLane(5), missingLane(13), missingLane(22));
    byte[] encoded = StateArchiveFiveLaneRecoveryIntentV3.encode(
        new Intent(hash(90), point(12), null, null, baselineLanes));

    assertEquals(0, Short.toUnsignedInt(ByteBuffer.wrap(encoded).getShort(26)));
    assertArrayEquals(new byte[240], slice(encoded, 248, 240));
    Intent decoded = StateArchiveFiveLaneRecoveryIntentV3.decode(encoded);
    assertNull(decoded.getCommonCommitted());
    assertNull(decoded.getTarget());
    assertEquals(StateArchiveFiveLaneRecoveryIntentV3.SOURCE_PAIR_MISSING,
        decoded.getLanes().get(1).getActionFlags());
  }

  private static Intent intent() {
    return new Intent(hash(90), point(12), point(10), point(11), lanes());
  }

  private static RecoveryPoint point(int block) {
    return new RecoveryPoint(block, block, block * 3_000L,
        hash(block), hash(block - 1), hash(100 + block));
  }

  private static List<LaneTarget> lanes() {
    byte[] zero = new byte[32];
    return new java.util.ArrayList<>(Arrays.asList(
        new LaneTarget(0,
            StateArchiveFiveLaneRecoveryIntentV3.DATA_TRUNCATE
                | StateArchiveFiveLaneRecoveryIntentV3.INDEX_REPLACE,
            2, 1_000, 192, 2, 900, 160, hash(1), hash(2), hash(3)),
        new LaneTarget(4, StateArchiveFiveLaneRecoveryIntentV3.INDEX_REPLACE,
            1, 800, 160, 1, 800, 160, hash(4), hash(5), hash(6)),
        new LaneTarget(5, 0,
            1, 800, 160, 1, 800, 160, hash(7), hash(8), hash(9)),
        new LaneTarget(13,
            StateArchiveFiveLaneRecoveryIntentV3.INDEX_REPLACE
                | StateArchiveFiveLaneRecoveryIntentV3.ORIGINAL_INDEX_MISSING,
            1, 800, 0, 1, 800, 160, hash(10), hash(11), hash(12)),
        new LaneTarget(22, StateArchiveFiveLaneRecoveryIntentV3.DELETE_PAIR,
            3, 700, 128, StateArchiveFiveLaneRecoveryIntentV3.NO_TARGET_SEGMENT,
            0, 0, hash(13), zero, zero)));
  }

  private static LaneTarget missingLane(int laneId) {
    byte[] zero = new byte[32];
    return new LaneTarget(laneId, StateArchiveFiveLaneRecoveryIntentV3.SOURCE_PAIR_MISSING,
        StateArchiveFiveLaneRecoveryIntentV3.NO_TARGET_SEGMENT, 0, 0,
        StateArchiveFiveLaneRecoveryIntentV3.NO_TARGET_SEGMENT, 0, 0, zero, zero, zero);
  }

  private static byte[] hash(int suffix) {
    byte[] result = new byte[32];
    result[31] = (byte) suffix;
    return result;
  }

  private static byte[] slice(byte[] bytes, int offset, int length) {
    return Arrays.copyOfRange(bytes, offset, offset + length);
  }
}
