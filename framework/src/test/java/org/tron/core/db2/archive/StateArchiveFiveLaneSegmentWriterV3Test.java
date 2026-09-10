package org.tron.core.db2.archive;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tron.core.db2.archive.BlockReverseDiff.DbGroup;
import org.tron.core.db2.archive.BlockReverseDiff.Entry;
import org.tron.core.db2.archive.StateArchiveFiveLaneBlockCodecV3.EncodedBundle;
import org.tron.core.db2.archive.StateArchiveFiveLaneRecoveryIntentV3.RecoveryPoint;
import org.tron.core.db2.archive.StateArchiveFiveLaneSegmentWriterV3.ArchiveDurabilityProof;
import org.tron.core.db2.archive.StateArchiveFiveLaneSegmentWriterV3.RecoveryStage;
import org.tron.core.db2.archive.StateArchiveFiveLaneSegmentWriterV3.SyncStage;
import org.tron.core.db2.archive.StateArchiveSegmentFormatV3.CurrentSegment;
import org.tron.core.db2.archive.StateArchiveSegmentFormatV3.SealedSegment;

public class StateArchiveFiveLaneSegmentWriterV3Test {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void independentlyRotatesOvershotLaneAndReopensCompleteBundle() throws Exception {
    Path root = temporaryFolder.newFolder("five-lane-segments").toPath();
    byte[] baseline = hash(90);
    StateArchiveFiveLaneBlockCodecV3 codec = new StateArchiveFiveLaneBlockCodecV3();
    EncodedBundle first = codec.encode(diff(1, 1_400), baseline,
        StateArchiveFileFormatV3.COMPRESSION_NONE);
    EncodedBundle second = codec.encode(diff(2, 0), first.getResultHistoryDigest(),
        StateArchiveFileFormatV3.COMPRESSION_NONE);

    try (StateArchiveFiveLaneSegmentWriterV3 writer =
        new StateArchiveFiveLaneSegmentWriterV3(root, baseline,
            StateArchiveFileFormatV3.COMPRESSION_NONE, 1_500)) {
      writer.append(first);
      writer.append(second);
      assertEquals(2, writer.getAppendHead().getBlockNumber());
      assertEquals(1, writer.getSealedSegments().size());
      SealedSegment sealed = writer.getSealedSegments().get(0);
      assertEquals(0, sealed.getLaneId());
      assertEquals(0, sealed.getSegmentSeq());
      assertEquals(1, sealed.getFirstBlock());
      assertEquals(1, sealed.getLastBlock());

      List<CurrentSegment> current = writer.getCurrentSegments();
      assertEquals(5, current.size());
      assertEquals(1, current.stream().filter(segment -> segment.getSegmentSeq() == 1)
          .count());
      CurrentSegment mixed = current.stream().filter(segment -> segment.getLaneId() == 0)
          .findFirst().get();
      assertEquals(2, mixed.getFirstBlock());
      assertEquals(2, mixed.getCurrentLastBlock());
      assertTrue(current.stream().filter(segment -> segment.getLaneId() != 0)
          .allMatch(segment -> segment.getFirstBlock() == 1
              && segment.getCurrentLastBlock() == 2
              && segment.getSegmentSeq() == 0));
    }

    assertEquals(6, filesWithSuffix(root, ".dat").size());
    assertEquals(6, filesWithSuffix(root, ".bidx").size());

    try (StateArchiveFiveLaneSegmentWriterV3 reopened =
        new StateArchiveFiveLaneSegmentWriterV3(root, baseline,
            StateArchiveFileFormatV3.COMPRESSION_NONE, 1_500)) {
      assertEquals(2, reopened.getAppendHead().getBlockNumber());
      assertEquals(1, reopened.getSealedSegments().size());
      EncodedBundle third = codec.encode(diff(3, 0), reopened.getResultHistoryDigest(),
          StateArchiveFileFormatV3.COMPRESSION_NONE);
      reopened.append(third);
      assertEquals(3, reopened.getAppendHead().getBlockNumber());
    }
  }

  @Test
  public void rotatesPreviouslyPublishedTailAtNextCheckpointBoundary() throws Exception {
    Path root = temporaryFolder.newFolder("published-tail-rotation").toPath();
    byte[] baseline = hash(89);
    StateArchiveFiveLaneBlockCodecV3 codec = new StateArchiveFiveLaneBlockCodecV3();
    EncodedBundle first = codec.encode(diff(1, 1_400), baseline,
        StateArchiveFileFormatV3.COMPRESSION_NONE);
    EncodedBundle second = codec.encode(diff(2, 0), first.getResultHistoryDigest(),
        StateArchiveFileFormatV3.COMPRESSION_NONE);
    try (StateArchiveFiveLaneSegmentWriterV3 writer =
        new StateArchiveFiveLaneSegmentWriterV3(root, baseline,
            StateArchiveFileFormatV3.COMPRESSION_NONE, 1_500)) {
      writer.appendForCheckpoint(first, 11, hash(109));
      writer.sync(11, point(first), hash(109));
      writer.appendForCheckpoint(second, 12, hash(110));
      ArchiveDurabilityProof proof = writer.sync(12, point(second), hash(110));
      assertEquals(5, proof.getFileTails().size());
      assertFalse(writer.getSealedSegments().isEmpty());
    }
    try (StateArchiveFiveLaneSegmentWriterV3 reopened =
        new StateArchiveFiveLaneSegmentWriterV3(root, baseline,
            StateArchiveFileFormatV3.COMPRESSION_NONE, 1_500)) {
      assertEquals(2, reopened.getAppendHead().getBlockNumber());
      assertEquals(2, reopened.readCommittedDiffs(0, 2).size());
    }
  }

  @Test
  public void provesSealedAndCurrentTailsAcrossIndependentRotation() throws Exception {
    Path root = temporaryFolder.newFolder("five-lane-rotation-proof").toPath();
    byte[] baseline = hash(89);
    byte[] commonTarget = hash(109);
    StateArchiveFiveLaneBlockCodecV3 codec = new StateArchiveFiveLaneBlockCodecV3();
    EncodedBundle first = codec.encode(diff(1, 1_400), baseline,
        StateArchiveFileFormatV3.COMPRESSION_NONE);
    EncodedBundle second = codec.encode(diff(2, 0), first.getResultHistoryDigest(),
        StateArchiveFileFormatV3.COMPRESSION_NONE);
    ArchiveDurabilityProof proof;

    try (StateArchiveFiveLaneSegmentWriterV3 writer =
        new StateArchiveFiveLaneSegmentWriterV3(root, baseline,
            StateArchiveFileFormatV3.COMPRESSION_NONE, 1_500)) {
      writer.appendForCheckpoint(first, 11, commonTarget);
      writer.appendForCheckpoint(second, 11, commonTarget);
      proof = writer.sync(11, point(second), commonTarget);
      assertEquals(6, proof.getFileTails().size());
      assertEquals(2, proof.getFileTails().stream()
          .filter(tail -> tail.getLaneId() == 0).count());
      assertEquals(0, proof.getFileTails().get(0).getSegmentSeq());
      assertEquals(1, proof.getFileTails().get(1).getSegmentSeq());
      writer.verifyDurabilityProof(proof);
      StateArchiveFiveLaneDurabilityProofV3.publish(root, proof);
    }

    assertEquals(832, Files.size(
        root.resolve(StateArchiveFiveLaneDurabilityProofV3.FILE_NAME)));
    try (StateArchiveFiveLaneSegmentWriterV3 reopened =
        new StateArchiveFiveLaneSegmentWriterV3(root, baseline,
            StateArchiveFileFormatV3.COMPRESSION_NONE, 1_500)) {
      ArchiveDurabilityProof verified = StateArchiveFiveLaneDurabilityProofV3.loadAndVerify(
          root, reopened, point(second), commonTarget);
      assertEquals(6, verified.getFileTails().size());
    }
  }

  @Test
  public void failsClosedAtEveryRotationMarkerPhase() throws Exception {
    for (SyncStage stage : new SyncStage[]{SyncStage.MARKER_WRITTEN,
        SyncStage.DATA_FORCED, SyncStage.MARKER_VERIFIED}) {
      Path root = temporaryFolder.newFolder("five-lane-rotation-fault-" + stage).toPath();
      byte[] baseline = hash(88);
      byte[] commonTarget = hash(108);
      StateArchiveFiveLaneBlockCodecV3 codec = new StateArchiveFiveLaneBlockCodecV3();
      EncodedBundle first = codec.encode(diff(1, 1_400), baseline,
          StateArchiveFileFormatV3.COMPRESSION_NONE);
      EncodedBundle second = codec.encode(diff(2, 0), first.getResultHistoryDigest(),
          StateArchiveFileFormatV3.COMPRESSION_NONE);
      try (StateArchiveFiveLaneSegmentWriterV3 writer =
          new StateArchiveFiveLaneSegmentWriterV3(root, baseline,
              StateArchiveFileFormatV3.COMPRESSION_NONE, 1_500)) {
        writer.appendForCheckpoint(first, 12, commonTarget);
        assertThrows(java.io.IOException.class,
            () -> writer.appendForCheckpoint(second, 12, commonTarget,
                (actual, laneId) -> {
                  if (actual == stage && laneId == 0) {
                    throw new java.io.IOException("injected rotation fault at " + stage);
                  }
                }));
        assertThrows(IllegalStateException.class, () -> writer.sync(
            12, point(first), commonTarget));
      }
    }
  }

  @Test
  public void repairsPartialDataAndIndexAtFiveLaneCommonBoundary() throws Exception {
    Path root = temporaryFolder.newFolder("five-lane-recovery").toPath();
    byte[] baseline = hash(80);
    StateArchiveFiveLaneBlockCodecV3 codec = new StateArchiveFiveLaneBlockCodecV3();
    EncodedBundle first = codec.encode(diff(1, 10), baseline,
        StateArchiveFileFormatV3.COMPRESSION_NONE);
    EncodedBundle second = codec.encode(diff(2, 10), first.getResultHistoryDigest(),
        StateArchiveFileFormatV3.COMPRESSION_NONE);
    EncodedBundle third = codec.encode(diff(3, 10), second.getResultHistoryDigest(),
        StateArchiveFileFormatV3.COMPRESSION_NONE);
    try (StateArchiveFiveLaneSegmentWriterV3 writer =
        new StateArchiveFiveLaneSegmentWriterV3(root, baseline,
            StateArchiveFileFormatV3.COMPRESSION_NONE, 10_000)) {
      writer.append(first);
      writer.append(second);
    }

    Path lane22Data = segment(root, 22, ".dat");
    try (FileChannel channel = FileChannel.open(lane22Data, StandardOpenOption.WRITE)) {
      channel.truncate(channel.size() - 10);
      channel.force(false);
    }
    Path lane5Index = segment(root, 5, ".bidx");
    try (FileChannel channel = FileChannel.open(lane5Index, StandardOpenOption.WRITE)) {
      channel.truncate(channel.size() - 7);
      channel.force(false);
    }
    Files.delete(segment(root, 13, ".bidx"));
    byte[] extraMixedFrame = third.getLanes().stream()
        .filter(lane -> lane.getLaneId() == 0).findFirst().get().getFrame();
    try (FileChannel channel = FileChannel.open(segment(root, 0, ".dat"),
        StandardOpenOption.WRITE)) {
      channel.position(channel.size());
      ByteBuffer bytes = ByteBuffer.wrap(extraMixedFrame);
      while (bytes.hasRemaining()) {
        channel.write(bytes);
      }
      channel.force(false);
    }

    try (StateArchiveFiveLaneSegmentWriterV3 recovered =
        StateArchiveFiveLaneSegmentWriterV3.recover(root, baseline,
            StateArchiveFileFormatV3.COMPRESSION_NONE, 10_000, 2)) {
      assertEquals(1, recovered.getAppendHead().getBlockNumber());
      assertEquals(5, recovered.getCurrentSegments().size());
      assertTrue(recovered.getCurrentSegments().stream()
          .allMatch(segment -> segment.getCurrentLastBlock() == 1
              && segment.getBlockFrameCount() == 1));
      assertTrue(Files.isRegularFile(segment(root, 13, ".bidx")));
    }

    Map<Path, Long> repairedSizes = fileSizes(root);
    try (StateArchiveFiveLaneSegmentWriterV3 reopened =
        new StateArchiveFiveLaneSegmentWriterV3(root, baseline,
            StateArchiveFileFormatV3.COMPRESSION_NONE, 10_000)) {
      assertEquals(1, reopened.getAppendHead().getBlockNumber());
    }
    assertEquals(repairedSizes, fileSizes(root));
  }

  @Test
  public void resumesDurableRecoveryIntentAtEveryMutationBoundary() throws Exception {
    for (RecoveryStage stage : RecoveryStage.values()) {
      Path root = temporaryFolder.newFolder("five-lane-intent-" + stage).toPath();
      byte[] baseline = hash(70);
      StateArchiveFiveLaneBlockCodecV3 codec = new StateArchiveFiveLaneBlockCodecV3();
      EncodedBundle first = codec.encode(diff(1, 10), baseline,
          StateArchiveFileFormatV3.COMPRESSION_NONE);
      EncodedBundle second = codec.encode(diff(2, 10), first.getResultHistoryDigest(),
          StateArchiveFileFormatV3.COMPRESSION_NONE);
      prepareDamagedTail(root, baseline, first, second);

      RecoveryPoint authorized = point(second);
      RecoveryPoint common = point(first);
      StateArchiveFiveLaneSegmentWriterV3.RecoveryFaultHook fault = (actual, laneId) -> {
        if (actual == stage) {
          throw new java.io.IOException("injected recovery fault at " + stage);
        }
      };
      assertThrows(java.io.IOException.class,
          () -> StateArchiveFiveLaneSegmentWriterV3.recover(root, baseline,
              StateArchiveFileFormatV3.COMPRESSION_NONE, 10_000,
              authorized, common, fault));

      Path intent = root.resolve(StateArchiveFiveLaneRecoveryIntentV3.FILE_NAME);
      Path temporary = root.resolve(StateArchiveFiveLaneRecoveryIntentV3.TEMP_FILE_NAME);
      if (stage == RecoveryStage.TEMPORARY_FORCED) {
        assertFalse(Files.exists(intent));
        assertTrue(Files.isRegularFile(temporary));
        try (StateArchiveFiveLaneSegmentWriterV3 recovered =
            StateArchiveFiveLaneSegmentWriterV3.recover(root, baseline,
                StateArchiveFileFormatV3.COMPRESSION_NONE, 10_000,
                authorized, common, StateArchiveFiveLaneSegmentWriterV3.RecoveryFaultHook.NONE)) {
          assertEquals(1, recovered.getAppendHead().getBlockNumber());
        }
      } else {
        assertFalse(Files.exists(temporary));
        assertEquals(stage != RecoveryStage.INTENT_DELETED, Files.exists(intent));
        try (StateArchiveFiveLaneSegmentWriterV3 recovered =
            new StateArchiveFiveLaneSegmentWriterV3(root, baseline,
                StateArchiveFileFormatV3.COMPRESSION_NONE, 10_000)) {
          assertEquals(1, recovered.getAppendHead().getBlockNumber());
          assertTrue(recovered.getCurrentSegments().stream()
              .allMatch(segment -> segment.getCurrentLastBlock() == 1));
        }
      }
      assertFalse(Files.exists(intent));
      assertFalse(Files.exists(temporary));
      Map<Path, Long> recoveredSizes = fileSizes(root);
      try (StateArchiveFiveLaneSegmentWriterV3 reopened =
          new StateArchiveFiveLaneSegmentWriterV3(root, baseline,
              StateArchiveFileFormatV3.COMPRESSION_NONE, 10_000)) {
        assertEquals(1, reopened.getAppendHead().getBlockNumber());
      }
      assertEquals(recoveredSizes, fileSizes(root));
    }
  }

  @Test
  public void resumesDeletePairPlanWithExplicitMissingLane() throws Exception {
    Path root = temporaryFolder.newFolder("five-lane-delete-intent").toPath();
    byte[] baseline = hash(60);
    StateArchiveFiveLaneBlockCodecV3 codec = new StateArchiveFiveLaneBlockCodecV3();
    EncodedBundle first = codec.encode(diff(1, 10), baseline,
        StateArchiveFileFormatV3.COMPRESSION_NONE);
    try (StateArchiveFiveLaneSegmentWriterV3 writer =
        new StateArchiveFiveLaneSegmentWriterV3(root, baseline,
            StateArchiveFileFormatV3.COMPRESSION_NONE, 10_000)) {
      writer.append(first);
    }
    Files.delete(segment(root, 22, ".bidx"));
    Files.delete(segment(root, 22, ".dat"));

    assertThrows(java.io.IOException.class,
        () -> StateArchiveFiveLaneSegmentWriterV3.recover(root, baseline,
            StateArchiveFileFormatV3.COMPRESSION_NONE, 10_000, point(first), null,
            (stage, laneId) -> {
              if (stage == RecoveryStage.LANE_INDEX_APPLIED && laneId == 0) {
                throw new java.io.IOException("injected delete-pair fault");
              }
            }));
    assertTrue(Files.isRegularFile(
        root.resolve(StateArchiveFiveLaneRecoveryIntentV3.FILE_NAME)));
    assertFalse(Files.exists(segment(root, 0, ".bidx")));
    assertTrue(Files.isRegularFile(segment(root, 0, ".dat")));

    try (StateArchiveFiveLaneSegmentWriterV3 recovered =
        new StateArchiveFiveLaneSegmentWriterV3(root, baseline,
            StateArchiveFileFormatV3.COMPRESSION_NONE, 10_000)) {
      assertEquals(null, recovered.getAppendHead());
      assertTrue(recovered.getCurrentSegments().isEmpty());
    }
    assertTrue(filesWithSuffix(root, ".dat").isEmpty());
    assertTrue(filesWithSuffix(root, ".bidx").isEmpty());
    assertFalse(Files.exists(root.resolve(StateArchiveFiveLaneRecoveryIntentV3.FILE_NAME)));

    Path emptyRoot = temporaryFolder.newFolder("five-lane-delete-empty-intent").toPath();
    try (StateArchiveFiveLaneSegmentWriterV3 writer =
        new StateArchiveFiveLaneSegmentWriterV3(emptyRoot, baseline,
            StateArchiveFileFormatV3.COMPRESSION_NONE, 10_000)) {
      writer.append(first);
    }
    Files.delete(segment(emptyRoot, 22, ".bidx"));
    Files.delete(segment(emptyRoot, 22, ".dat"));
    assertThrows(java.io.IOException.class,
        () -> StateArchiveFiveLaneSegmentWriterV3.recover(emptyRoot, baseline,
            StateArchiveFileFormatV3.COMPRESSION_NONE, 10_000, point(first), null,
            (stage, laneId) -> {
              if (stage == RecoveryStage.LANE_DATA_APPLIED && laneId == 13) {
                throw new java.io.IOException("injected final delete-pair fault");
              }
            }));
    assertTrue(filesWithSuffix(emptyRoot, ".dat").isEmpty());
    assertTrue(Files.isRegularFile(
        emptyRoot.resolve(StateArchiveFiveLaneRecoveryIntentV3.FILE_NAME)));
    try (StateArchiveFiveLaneSegmentWriterV3 recovered =
        new StateArchiveFiveLaneSegmentWriterV3(emptyRoot, baseline,
            StateArchiveFileFormatV3.COMPRESSION_NONE, 10_000)) {
      assertEquals(null, recovered.getAppendHead());
    }
    assertFalse(Files.exists(
        emptyRoot.resolve(StateArchiveFiveLaneRecoveryIntentV3.FILE_NAME)));
  }

  @Test
  public void durableIntentRejectsTargetPrefixDrift() throws Exception {
    Path root = temporaryFolder.newFolder("five-lane-intent-drift").toPath();
    byte[] baseline = hash(50);
    StateArchiveFiveLaneBlockCodecV3 codec = new StateArchiveFiveLaneBlockCodecV3();
    EncodedBundle first = codec.encode(diff(1, 10), baseline,
        StateArchiveFileFormatV3.COMPRESSION_NONE);
    EncodedBundle second = codec.encode(diff(2, 10), first.getResultHistoryDigest(),
        StateArchiveFileFormatV3.COMPRESSION_NONE);
    prepareDamagedTail(root, baseline, first, second);
    assertThrows(java.io.IOException.class,
        () -> StateArchiveFiveLaneSegmentWriterV3.recover(root, baseline,
            StateArchiveFileFormatV3.COMPRESSION_NONE, 10_000, point(second), point(first),
            (stage, laneId) -> {
              if (stage == RecoveryStage.INTENT_PUBLISHED) {
                throw new java.io.IOException("injected published-intent fault");
              }
            }));
    Path lane0 = segment(root, 0, ".dat");
    try (FileChannel channel = FileChannel.open(lane0,
        StandardOpenOption.READ, StandardOpenOption.WRITE)) {
      long offset = StateArchiveFileFormatV3.PART_HEADER_LENGTH + 40;
      channel.position(offset);
      ByteBuffer value = ByteBuffer.allocate(1);
      channel.read(value);
      value.flip();
      value.put(0, (byte) (value.get(0) ^ 1));
      channel.position(offset);
      channel.write(value);
      channel.force(false);
    }
    assertThrows(IllegalArgumentException.class,
        () -> new StateArchiveFiveLaneSegmentWriterV3(root, baseline,
            StateArchiveFileFormatV3.COMPRESSION_NONE, 10_000));
    assertTrue(Files.isRegularFile(
        root.resolve(StateArchiveFiveLaneRecoveryIntentV3.FILE_NAME)));
  }

  @Test
  public void forcesAndReverifiesCompleteFiveLaneDurabilityProof() throws Exception {
    Path root = temporaryFolder.newFolder("five-lane-sync").toPath();
    byte[] baseline = hash(40);
    byte[] commonTarget = hash(100);
    StateArchiveFiveLaneBlockCodecV3 codec = new StateArchiveFiveLaneBlockCodecV3();
    EncodedBundle first = codec.encode(diff(1, 10), baseline,
        StateArchiveFileFormatV3.COMPRESSION_NONE);
    EncodedBundle second = codec.encode(diff(2, 10), first.getResultHistoryDigest(),
        StateArchiveFileFormatV3.COMPRESSION_NONE);
    EncodedBundle third = codec.encode(diff(3, 10), second.getResultHistoryDigest(),
        StateArchiveFileFormatV3.COMPRESSION_NONE);
    ArchiveDurabilityProof proof;
    try (StateArchiveFiveLaneSegmentWriterV3 writer =
        new StateArchiveFiveLaneSegmentWriterV3(root, baseline,
            StateArchiveFileFormatV3.COMPRESSION_NONE, 10_000)) {
      writer.append(first);
      writer.append(second);
      proof = writer.sync(7, point(second), commonTarget);
      assertEquals(7, proof.getCheckpointSequence());
      assertEquals(5, proof.getFileTails().size());
      assertArrayEquals(StateArchiveFileFormatV3.compositeFormatDigest(),
          proof.getFormatIdentity());
      assertArrayEquals(StateArchiveFileFormatV3.fiveLaneDescriptorDigest(),
          proof.getDescriptorDigest());
      writer.verifyDurabilityProof(proof);
      assertEquals(proof, writer.sync(8, point(second), commonTarget));
      writer.append(third);
    }

    try (StateArchiveFiveLaneSegmentWriterV3 reopened =
        new StateArchiveFiveLaneSegmentWriterV3(root, baseline,
            StateArchiveFileFormatV3.COMPRESSION_NONE, 10_000)) {
      reopened.verifyDurabilityProof(proof);
      assertEquals(7, reopened.getLastDurabilityProof().getCheckpointSequence());
      assertEquals(3, reopened.getAppendHead().getBlockNumber());
      ArchiveDurabilityProof next = reopened.sync(8, point(third), hash(101));
      assertEquals(8, next.getCheckpointSequence());
      assertTrue(next.getFileTails().stream()
          .allMatch(tail -> tail.getMarkerOffset() > proof.getFileTails().stream()
              .filter(previous -> previous.getLaneId() == tail.getLaneId())
              .findFirst().get().getMarkerOffset()));
      reopened.verifyDurabilityProof(next);
    }
  }

  @Test
  public void neverReturnsProofBeforeEveryMarkerForceAndRereadCompletes()
      throws Exception {
    for (SyncStage expected : SyncStage.values()) {
      Path root = temporaryFolder.newFolder("five-lane-sync-fault-" + expected).toPath();
      byte[] baseline = hash(30);
      StateArchiveFiveLaneBlockCodecV3 codec = new StateArchiveFiveLaneBlockCodecV3();
      EncodedBundle first = codec.encode(diff(1, 10), baseline,
          StateArchiveFileFormatV3.COMPRESSION_NONE);
      try (StateArchiveFiveLaneSegmentWriterV3 writer =
          new StateArchiveFiveLaneSegmentWriterV3(root, baseline,
              StateArchiveFileFormatV3.COMPRESSION_NONE, 10_000)) {
        writer.append(first);
        assertThrows(java.io.IOException.class,
            () -> writer.sync(1, point(first), hash(99), (actual, laneId) -> {
              if (actual == expected) {
                throw new java.io.IOException("injected sync fault at " + expected);
              }
            }));
        assertThrows(IllegalStateException.class, () -> writer.append(first));
      }
    }
  }

  private static void prepareDamagedTail(Path root, byte[] baseline,
      EncodedBundle first, EncodedBundle second) throws Exception {
    try (StateArchiveFiveLaneSegmentWriterV3 writer =
        new StateArchiveFiveLaneSegmentWriterV3(root, baseline,
            StateArchiveFileFormatV3.COMPRESSION_NONE, 10_000)) {
      writer.append(first);
      writer.append(second);
    }
    try (FileChannel channel = FileChannel.open(segment(root, 22, ".dat"),
        StandardOpenOption.WRITE)) {
      channel.truncate(channel.size() - 10);
      channel.force(false);
    }
  }

  private static RecoveryPoint point(EncodedBundle bundle) {
    BlockSnapshotMeta meta = bundle.getDiff().getMeta();
    return new RecoveryPoint(meta.getEpoch(), meta.getBlockNumber(), meta.getTimestamp(),
        meta.getBlockHash(), meta.getParentHash(), bundle.getResultHistoryDigest());
  }

  private static BlockReverseDiff diff(int blockNumber, int valueLength) {
    byte[] value = new byte[valueLength];
    java.util.Arrays.fill(value, (byte) blockNumber);
    List<DbGroup> groups = valueLength == 0 ? Collections.emptyList()
        : Collections.singletonList(new DbGroup(StateArchiveFileFormatV3.dbName(1),
            Collections.singletonList(new Entry(new byte[]{1}, OldValue.present(value)))));
    return new BlockReverseDiff(new BlockSnapshotMeta(blockNumber, blockNumber,
        hash(blockNumber), hash(blockNumber - 1), blockNumber * 3_000L), groups);
  }

  private static List<Path> filesWithSuffix(Path root, String suffix) throws Exception {
    try (Stream<Path> files = Files.walk(root)) {
      return files.filter(Files::isRegularFile)
          .filter(path -> path.getFileName().toString().endsWith(suffix))
          .collect(Collectors.toList());
    }
  }

  private static Path segment(Path root, int laneId, String suffix) {
    return root.resolve("segments/shard-000000")
        .resolve(String.format("lane-%04d-seg-%020d%s", laneId, 0, suffix));
  }

  private static Map<Path, Long> fileSizes(Path root) throws Exception {
    Map<Path, Long> sizes = new HashMap<>();
    try (Stream<Path> files = Files.walk(root)) {
      for (Path path : files.filter(Files::isRegularFile).collect(Collectors.toList())) {
        sizes.put(root.relativize(path), Files.size(path));
      }
    }
    return sizes;
  }

  private static byte[] hash(int suffix) {
    byte[] result = new byte[32];
    result[31] = (byte) suffix;
    return result;
  }
}
