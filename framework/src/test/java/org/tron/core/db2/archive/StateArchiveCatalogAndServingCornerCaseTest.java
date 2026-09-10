package org.tron.core.db2.archive;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.OptionalLong;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tron.core.db2.archive.BlockReverseDiff.DbGroup;
import org.tron.core.db2.archive.BlockReverseDiff.Entry;
import org.tron.core.db2.archive.StateArchiveServingIndexBuildCoordinatorV3.LiveServingIndexer;
import org.tron.core.db2.core.CommonCheckpointTarget;
import org.tron.core.db2.stateroot.PathStateStoreManifest.Engine;

public class StateArchiveCatalogAndServingCornerCaseTest {

  @Rule
  public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void bulkBatchCutsProduceTheSameExactIndexIdentity() throws Exception {
    List<BlockReverseDiff> all = diffs(1, 6, 0);
    Path oneBatch = temporaryFolder.newFolder("one-batch").toPath();
    Path splitBatch = temporaryFolder.newFolder("split-batch").toPath();
    try (StateArchiveServingIndexBuildCoordinatorV3 one =
        new StateArchiveServingIndexBuildCoordinatorV3(oneBatch, Engine.LEVELDB, 6);
        StateArchiveServingIndexBuildCoordinatorV3 split =
            new StateArchiveServingIndexBuildCoordinatorV3(splitBatch, Engine.LEVELDB, 3)) {
      one.offerCommittedRange(all, target(all, 1));
      split.offerCommittedRange(all.subList(0, 2), target(all.subList(0, 2), 2));
      split.offerCommittedRange(all.subList(2, 3), target(all.subList(2, 3), 3));
      split.offerCommittedRange(all.subList(3, 6), target(all.subList(3, 6), 4));
      assertEquals(6, one.status().getIndexedThrough());
      assertEquals(6, split.status().getIndexedThrough());
    }
    try (PersistentServingKeyIndexCatalog one = PersistentServingKeyIndexCatalog.open(
        oneBatch.resolve(StateArchiveServingIndexBuildCoordinatorV3.DIRECTORY),
        Engine.LEVELDB, stage -> { });
        PersistentServingKeyIndexCatalog split = PersistentServingKeyIndexCatalog.open(
            splitBatch.resolve(StateArchiveServingIndexBuildCoordinatorV3.DIRECTORY),
            Engine.LEVELDB, stage -> { });
        PersistentServingKeyIndexGeneration oneGeneration = one.pin();
        PersistentServingKeyIndexGeneration splitGeneration = split.pin()) {
      assertArrayEquals(oneGeneration.getAuthoritativePrefixDigest(),
          splitGeneration.getAuthoritativePrefixDigest());
      assertEquals(oneGeneration.getKeyChangeCount(), splitGeneration.getKeyChangeCount());
      OptionalLong oneChange = oneGeneration.firstChangeAfter("code", new byte[]{3}, 0, 6);
      OptionalLong splitChange = splitGeneration.firstChangeAfter("code", new byte[]{3}, 0, 6);
      assertEquals(oneChange, splitChange);
    }
  }

  @Test
  public void gapInvalidatesLiveHandleWithoutAdvancingI() throws Exception {
    Path root = temporaryFolder.newFolder("live-gap").toPath();
    List<BlockReverseDiff> first = diffs(1, 1, 0);
    try (StateArchiveServingIndexBuildCoordinatorV3 coordinator =
        new StateArchiveServingIndexBuildCoordinatorV3(root, Engine.LEVELDB, 1)) {
      CommonCheckpointTarget firstTarget = target(first, 1);
      coordinator.offerCommittedRange(first, firstTarget);
      LiveServingIndexer live = coordinator.completeInitialSync(firstTarget);
      List<BlockReverseDiff> gap = diffs(3, 1, 2);
      assertThrows(IllegalArgumentException.class,
          () -> live.indexNow(gap, target(gap, 3)));
      assertEquals(1, coordinator.status().getIndexedThrough());
      assertEquals(StateArchiveServingIndexBuildCoordinatorV3.Mode.CATCH_UP_REQUIRED,
          coordinator.status().getMode());
      List<BlockReverseDiff> successor = diffs(2, 1, 1);
      assertThrows(IllegalStateException.class,
          () -> live.indexNow(successor, target(successor, 2)));
    }
  }

  @Test
  public void catalogIgnoresOrphanButRejectsCorruptCurrentAndMissingManifest() throws Exception {
    Path root = temporaryFolder.newFolder("catalog-corners").toPath();
    byte[] baseline = hash(0);
    StateArchiveFiveLaneBlockCodecV3 codec = new StateArchiveFiveLaneBlockCodecV3();
    BlockReverseDiff first = diff(1, 0, 1_400);
    StateArchiveFiveLaneBlockCodecV3.EncodedBundle firstBundle = codec.encode(first, baseline,
        StateArchiveFileFormatV3.COMPRESSION_NONE);
    StateArchiveFiveLaneBlockCodecV3.EncodedBundle secondBundle = codec.encode(
        diff(2, 1, 0), firstBundle.getResultHistoryDigest(),
        StateArchiveFileFormatV3.COMPRESSION_NONE);
    Path current = root.resolve(StateArchiveHistoryCatalogV3.DIRECTORY)
        .resolve(StateArchiveHistoryCatalogV3.CURRENT);
    byte[] preRotationCurrent;
    try (StateArchiveFiveLaneSegmentWriterV3 writer =
        new StateArchiveFiveLaneSegmentWriterV3(root, baseline,
            StateArchiveFileFormatV3.COMPRESSION_NONE, 1_500)) {
      writer.append(firstBundle);
      preRotationCurrent = Files.readAllBytes(current);
      writer.append(secondBundle);
    }
    Path generations = root.resolve(StateArchiveHistoryCatalogV3.DIRECTORY)
        .resolve("generations");
    Files.write(generations.resolve("catalog-99999999999999999999.bin"), new byte[]{1});
    try (StateArchiveFiveLaneSegmentWriterV3 reopened =
        new StateArchiveFiveLaneSegmentWriterV3(root, baseline,
            StateArchiveFileFormatV3.COMPRESSION_NONE, 1_500)) {
      assertEquals(2, reopened.getAppendHead().getBlockNumber());
    }
    Path manifest;
    try (java.util.stream.Stream<Path> paths = Files.walk(root.resolve("segments"))) {
      manifest = paths.filter(path -> path.getFileName().toString().endsWith(".manifest"))
          .findFirst().orElseThrow(AssertionError::new);
    }
    Files.delete(manifest);
    Files.write(current, preRotationCurrent);
    try (StateArchiveFiveLaneSegmentWriterV3 recoveredPublication =
        new StateArchiveFiveLaneSegmentWriterV3(root, baseline,
            StateArchiveFileFormatV3.COMPRESSION_NONE, 1_500)) {
      assertEquals(2, recoveredPublication.getAppendHead().getBlockNumber());
    }

    byte[] validCurrent = Files.readAllBytes(current);
    byte[] corruptCurrent = validCurrent.clone();
    corruptCurrent[20] ^= 1;
    Files.write(current, corruptCurrent);
    assertThrows(IOException.class, () -> new StateArchiveFiveLaneSegmentWriterV3(root,
        baseline, StateArchiveFileFormatV3.COMPRESSION_NONE, 1_500));
    Files.write(current, validCurrent);

    Files.delete(manifest);
    assertThrows(IllegalArgumentException.class,
        () -> new StateArchiveFiveLaneSegmentWriterV3(root, baseline,
            StateArchiveFileFormatV3.COMPRESSION_NONE, 1_500));
  }

  private static List<BlockReverseDiff> diffs(int first, int count, int parent) {
    List<BlockReverseDiff> result = new ArrayList<>();
    for (int block = first; block < first + count; block++) {
      result.add(diff(block, block == first ? parent : block - 1, 8));
    }
    return result;
  }

  private static BlockReverseDiff diff(int block, int parent, int valueLength) {
    byte[] value = new byte[valueLength];
    Arrays.fill(value, (byte) block);
    List<DbGroup> groups = valueLength == 0 ? Collections.emptyList()
        : Collections.singletonList(new DbGroup("code", Collections.singletonList(
            new Entry(new byte[]{(byte) (block & 7)}, OldValue.present(value)))));
    return new BlockReverseDiff(new BlockSnapshotMeta(block, block, hash(block),
        hash(parent), block * 3_000L), groups);
  }

  private static CommonCheckpointTarget target(List<BlockReverseDiff> diffs, int salt) {
    return CommonCheckpointTarget.restore(hash(70), hash(80 + salt),
        diffs.get(0).getMeta(), diffs.get(diffs.size() - 1).getMeta(), hash(90), hash(91));
  }

  private static byte[] hash(int value) {
    byte[] result = new byte[32];
    result[27] = (byte) value;
    result[31] = (byte) value;
    return result;
  }
}
