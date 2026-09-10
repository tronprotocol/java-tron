package org.tron.core.db2.archive;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tron.core.db2.archive.BlockReverseDiff.DbGroup;
import org.tron.core.db2.archive.BlockReverseDiff.Entry;
import org.tron.core.db2.archive.StateArchiveFiveLaneBlockCodecV3.EncodedBundle;
import org.tron.core.db2.archive.StateArchiveFiveLaneRecoveryIntentV3.RecoveryPoint;
import org.tron.core.db2.archive.StateArchiveFiveLaneSegmentWriterV3.ArchiveDurabilityProof;
import org.tron.core.db2.archive.StateArchiveFiveLaneSegmentWriterV3.FileTailProof;

public class StateArchiveFiveLaneDurabilityProofV3Test {

  @Rule
  public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void freezesRoundTripAndReloadsExactMarkerProof() throws Exception {
    Path root = temporaryFolder.newFolder("proof").toPath();
    byte[] baseline = hash(40);
    byte[] commonDigest = hash(99);
    ArchiveDurabilityProof proof;
    try (StateArchiveFiveLaneSegmentWriterV3 writer = writer(root, baseline)) {
      EncodedBundle bundle = bundle(baseline);
      writer.append(bundle);
      proof = writer.sync(7, point(bundle), commonDigest);
      StateArchiveFiveLaneDurabilityProofV3.publish(root, proof);
    }

    byte[] encoded = Files.readAllBytes(
        root.resolve(StateArchiveFiveLaneDurabilityProofV3.FILE_NAME));
    assertEquals(760, encoded.length);
    ByteBuffer bytes = ByteBuffer.wrap(encoded);
    assertEquals(StateArchiveFiveLaneDurabilityProofV3.MAGIC, bytes.getInt(0));
    assertEquals(352, bytes.getInt(8));
    assertEquals(72, Short.toUnsignedInt(bytes.getShort(12)));
    assertEquals(5, Short.toUnsignedInt(bytes.getShort(14)));
    assertEquals(760, bytes.getLong(16));
    assertArrayEquals(StateArchiveFileFormatV3.compositeFormatDigest(),
        slice(encoded, 32, 32));
    assertArrayEquals(StateArchiveFileFormatV3.fiveLaneDescriptorDigest(),
        slice(encoded, 64, 32));
    assertEquals(7, bytes.getLong(96));
    assertEquals(1, bytes.getLong(112));
    assertEquals(0, Short.toUnsignedInt(bytes.getShort(352)));
    assertEquals(22, Short.toUnsignedInt(bytes.getShort(352 + 4 * 72)));

    ArchiveDurabilityProof decoded = StateArchiveFiveLaneDurabilityProofV3.decode(encoded);
    assertEquals(7, decoded.getCheckpointSequence());
    assertEquals(1, decoded.getTarget().getBlockNumber());
    assertArrayEquals(commonDigest, decoded.getCommonTargetDigest());
    assertEquals(5, decoded.getFileTails().size());

    try (StateArchiveFiveLaneSegmentWriterV3 reopened = writer(root, baseline)) {
      ArchiveDurabilityProof verified = StateArchiveFiveLaneDurabilityProofV3.loadAndVerify(
          root, reopened, proof.getTarget(), commonDigest);
      assertEquals(1, verified.getTarget().getBlockNumber());
      assertArrayEquals(proof.getFileTails().get(4).getMarkerDigest(),
          verified.getFileTails().get(4).getMarkerDigest());
      assertThrows(IllegalArgumentException.class,
          () -> StateArchiveFiveLaneDurabilityProofV3.loadAndVerify(root, reopened,
              proof.getTarget(), hash(98)));
    }
  }

  @Test
  public void rejectsHeaderTailTrailerAndMarkerDrift() throws Exception {
    Path root = temporaryFolder.newFolder("proof-fault").toPath();
    byte[] baseline = hash(50);
    ArchiveDurabilityProof proof;
    try (StateArchiveFiveLaneSegmentWriterV3 writer = writer(root, baseline)) {
      EncodedBundle bundle = bundle(baseline);
      writer.append(bundle);
      proof = writer.sync(8, point(bundle), hash(100));
      StateArchiveFiveLaneDurabilityProofV3.publish(root, proof);
    }
    byte[] encoded = Files.readAllBytes(
        root.resolve(StateArchiveFiveLaneDurabilityProofV3.FILE_NAME));
    for (int offset : new int[]{50, 400, 759}) {
      byte[] corrupt = encoded.clone();
      corrupt[offset] ^= 1;
      assertThrows(IllegalArgumentException.class,
          () -> StateArchiveFiveLaneDurabilityProofV3.decode(corrupt));
    }

    FileTailProof last = proof.getFileTails().get(4);
    try (StateArchiveFiveLaneSegmentWriterV3 reopened = writer(root, baseline)) {
      Path marker = root.resolve("segments").resolve("shard-000000")
          .resolve(String.format("lane-%04d-seg-%020d.dat", last.getLaneId(),
              last.getSegmentSeq()));
      try (FileChannel channel = FileChannel.open(marker, StandardOpenOption.READ,
          StandardOpenOption.WRITE)) {
        ByteBuffer one = ByteBuffer.allocate(1);
        channel.position(last.getMarkerOffset() + 20);
        channel.read(one);
        one.flip();
        one.put(0, (byte) (one.get(0) ^ 1));
        channel.position(last.getMarkerOffset() + 20);
        channel.write(one);
        channel.force(false);
      }
      assertThrows(IllegalArgumentException.class,
          () -> StateArchiveFiveLaneDurabilityProofV3.loadAndVerify(root, reopened));
    }
  }

  @Test
  public void rejectsMissingDuplicateAndOutOfOrderLaneTails() throws Exception {
    Path root = temporaryFolder.newFolder("proof-order").toPath();
    byte[] baseline = hash(60);
    ArchiveDurabilityProof proof;
    try (StateArchiveFiveLaneSegmentWriterV3 writer = writer(root, baseline)) {
      EncodedBundle bundle = bundle(baseline);
      writer.append(bundle);
      proof = writer.sync(9, point(bundle), hash(101));
    }
    assertThrows(IllegalArgumentException.class, () -> new ArchiveDurabilityProof(9,
        proof.getTarget(), proof.getCommonTargetDigest(),
        proof.getFileTails().subList(0, 4)));
    java.util.List<FileTailProof> duplicate = new java.util.ArrayList<>(proof.getFileTails());
    duplicate.add(1, duplicate.get(0));
    assertThrows(IllegalArgumentException.class, () -> new ArchiveDurabilityProof(9,
        proof.getTarget(), proof.getCommonTargetDigest(), duplicate));
    java.util.List<FileTailProof> reversed = new java.util.ArrayList<>(proof.getFileTails());
    Collections.swap(reversed, 0, 1);
    assertThrows(IllegalArgumentException.class, () -> new ArchiveDurabilityProof(9,
        proof.getTarget(), proof.getCommonTargetDigest(), reversed));
    assertTrue(proof.getFileTails().stream().allMatch(tail -> tail.getMarkerLength() == 336));
  }

  private static StateArchiveFiveLaneSegmentWriterV3 writer(Path root, byte[] baseline)
      throws Exception {
    return new StateArchiveFiveLaneSegmentWriterV3(root, baseline,
        StateArchiveFileFormatV3.COMPRESSION_NONE, 10_000);
  }

  private static EncodedBundle bundle(byte[] baseline) {
    BlockSnapshotMeta meta = new BlockSnapshotMeta(1, 1, hash(1), hash(0), 3_000);
    DbGroup group = new DbGroup(StateArchiveFileFormatV3.dbName(1),
        Collections.singletonList(new Entry(new byte[]{1}, OldValue.present(new byte[]{7}))));
    return new StateArchiveFiveLaneBlockCodecV3().encode(
        new BlockReverseDiff(meta, Collections.singletonList(group)), baseline,
        StateArchiveFileFormatV3.COMPRESSION_NONE);
  }

  private static RecoveryPoint point(EncodedBundle bundle) {
    BlockSnapshotMeta meta = bundle.getDiff().getMeta();
    return new RecoveryPoint(meta.getEpoch(), meta.getBlockNumber(), meta.getTimestamp(),
        meta.getBlockHash(), meta.getParentHash(), bundle.getResultHistoryDigest());
  }

  private static byte[] hash(int suffix) {
    byte[] result = new byte[32];
    result[31] = (byte) suffix;
    return result;
  }

  private static byte[] slice(byte[] bytes, int offset, int length) {
    return java.util.Arrays.copyOfRange(bytes, offset, offset + length);
  }
}
