package org.tron.core.db2.archive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tron.core.db2.archive.BlockReverseDiff.DbGroup;
import org.tron.core.db2.archive.BlockReverseDiff.Entry;
import org.tron.core.db2.archive.StateArchiveServingIndexBuildCoordinatorV3.BuildProgress;
import org.tron.core.db2.archive.StateArchiveServingIndexBuildCoordinatorV3.LiveServingIndexer;
import org.tron.core.db2.core.CommonCheckpointTarget;
import org.tron.core.db2.stateroot.PathStateStoreManifest.Engine;

/** Bounded mechanism benchmark; it records throughput without imposing a host-sensitive gate. */
public class StateArchiveServingIndexSpeedTest {

  @Rule
  public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void measuresBulkThenLiveDurablePublication() throws Exception {
    Path root = temporaryFolder.newFolder("serving-speed").toPath();
    List<BlockReverseDiff> bulk = diffs(1, 1_000);
    long bulkStart = System.nanoTime();
    long liveNanos = 0;
    try (StateArchiveServingIndexBuildCoordinatorV3 coordinator =
        new StateArchiveServingIndexBuildCoordinatorV3(root, Engine.LEVELDB, 1_000)) {
      BuildProgress bulkProgress = coordinator.offerCommittedRange(bulk,
          target(bulk, 1));
      long bulkEnd = System.nanoTime();
      assertEquals(1_000, bulkProgress.getIndexedThrough());
      LiveServingIndexer live = coordinator.completeInitialSync(target(bulk, 1));
      for (int block = 1_001; block <= 1_020; block++) {
        List<BlockReverseDiff> one = diffs(block, 1);
        long started = System.nanoTime();
        live.indexNow(one, target(one, block));
        liveNanos += System.nanoTime() - started;
      }
      assertEquals(1_020, coordinator.status().getIndexedThrough());
      double bulkSeconds = (bulkEnd - bulkStart) / 1_000_000_000.0;
      double liveMillis = liveNanos / 20.0 / 1_000_000.0;
      System.out.printf("STATE_ARCHIVE_SERVING_SPEED bulk_blocks=1000 bulk_seconds=%.6f "
          + "bulk_blocks_per_second=%.2f live_blocks=20 live_mean_ms=%.3f%n",
          bulkSeconds, 1_000.0 / bulkSeconds, liveMillis);
      assertTrue(bulkSeconds > 0);
      assertTrue(liveMillis > 0);
    }
  }

  private static List<BlockReverseDiff> diffs(int first, int count) {
    List<BlockReverseDiff> result = new ArrayList<>();
    for (int block = first; block < first + count; block++) {
      BlockSnapshotMeta meta = new BlockSnapshotMeta(block, block, hash(block),
          hash(block - 1), block * 3_000L);
      DbGroup group = new DbGroup("code", Collections.singletonList(
          new Entry(new byte[]{(byte) (block & 31)}, OldValue.absent())));
      result.add(new BlockReverseDiff(meta, Collections.singletonList(group)));
    }
    return result;
  }

  private static CommonCheckpointTarget target(List<BlockReverseDiff> diffs, int salt) {
    return CommonCheckpointTarget.restore(hash(70), hash(80 + salt),
        diffs.get(0).getMeta(), diffs.get(diffs.size() - 1).getMeta(), hash(90), hash(91));
  }

  private static byte[] hash(int value) {
    byte[] result = new byte[32];
    result[24] = (byte) (value >>> 24);
    result[25] = (byte) (value >>> 16);
    result[26] = (byte) (value >>> 8);
    result[27] = (byte) value;
    result[31] = (byte) value;
    return result;
  }
}
