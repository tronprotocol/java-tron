package org.tron.core.db2.archive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tron.core.db2.archive.StateArchiveServingIndexBuildCoordinatorV3.Mode;
import org.tron.core.db2.core.CommonCheckpointTarget;
import org.tron.core.db2.stateroot.PathStateStoreManifest.Engine;

public class StateArchiveServingWorkerV3Test {
  @Rule
  public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test(timeout = 15000)
  public void blockedBuilderAllowsCoalescedOffersAndOrderedHandoffThenLive() throws Exception {
    Path root = temporaryFolder.newFolder().toPath();
    CountDownLatch entered = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    try (StateArchiveFiveLaneSegmentWriterV3 source = source(root)) {
      append(source, 1);
      StateArchiveServingWorkerV3 worker = worker(root, source, () -> {
        entered.countDown();
        try {
          if (!release.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("test release timed out");
          }
        } catch (InterruptedException failure) {
          Thread.currentThread().interrupt();
          throw new IllegalStateException(failure);
        }
      });
      try {
        worker.offer(target(1, 1));
        assertTrue(entered.await(5, TimeUnit.SECONDS));
        append(source, 2);
        worker.offer(target(2, 2));
        assertEquals(2, worker.status().getCommittedThrough());
        assertEquals(-1, worker.status().getIndexedThrough());
        release.countDown();
        worker.completeInitialSync(target(2, 2));
        assertEquals(2, worker.status().getIndexedThrough());
        assertEquals(Mode.LIVE_IMMEDIATE, worker.status().getMode());
        append(source, 3);
        worker.offer(target(3, 3));
        assertEquals(3, worker.status().getIndexedThrough());
        long sequence = worker.status().getBuildSequence();
        worker.offer(target(3, 3));
        assertEquals(sequence, worker.status().getBuildSequence());
        assertThrows(IOException.class, () -> worker.offer(target(5, 5)));
      } finally {
        release.countDown();
        worker.close();
      }
    }
  }

  @Test(timeout = 15000)
  public void failedOwnerIsObservableAndRestartReplaysLostNotification() throws Exception {
    Path root = temporaryFolder.newFolder().toPath();
    try (StateArchiveFiveLaneSegmentWriterV3 source = source(root)) {
      append(source, 1);
      try (StateArchiveServingWorkerV3 failed = worker(root, source, () -> {
        throw new IllegalStateException("injected builder failure");
      })) {
        failed.offer(target(1, 1));
        assertThrows(IOException.class, () -> failed.completeInitialSync(target(1, 1)));
        assertEquals(Mode.CATCH_UP_REQUIRED, failed.status().getMode());
        assertNotNull(failed.failure());
        assertEquals(-1, failed.status().getIndexedThrough());
      }
      // No volatile mailbox survives. The recovered Common target is sufficient.
      try (StateArchiveServingWorkerV3 recovered = worker(root, source, () -> { })) {
        recovered.offer(target(1, 1));
        recovered.completeInitialSync(target(1, 1));
        assertEquals(1, recovered.status().getIndexedThrough());
      }
      try (StateArchiveServingWorkerV3 again = worker(root, source, () -> { })) {
        again.offer(target(1, 1));
        again.completeInitialSync(target(1, 1));
        assertEquals(0, again.status().getBuildSequence());
      }
    }
  }

  @Test(timeout = 15000)
  public void indexOpenFailureDoesNotThrowOnConstruction() throws Exception {
    Path root = temporaryFolder.newFolder().toPath();
    try (StateArchiveFiveLaneSegmentWriterV3 source = source(root);
        StateArchiveServingWorkerV3 worker = new StateArchiveServingWorkerV3(() -> {
          throw new IOException("injected index open failure");
        }, source, () -> { })) {
      assertThrows(IOException.class, () -> {
        worker.offer(target(1, 1));
        worker.completeInitialSync(target(1, 1));
      });
      assertEquals(Mode.CATCH_UP_REQUIRED, worker.status().getMode());
    }
  }

  @Test
  public void readBudgetRejectsBeforeAllocatingAnOversizedFrame() throws Exception {
    Path root = temporaryFolder.newFolder().toPath();
    try (StateArchiveFiveLaneSegmentWriterV3 source = source(root)) {
      append(source, 1);
      assertThrows(StateArchiveFiveLaneSegmentWriterV3.ServingReadBudgetException.class,
          () -> source.readCommittedDiffs(0, 1, 1));
      assertEquals(0, source.getServingReadBytes());
      assertEquals(1, source.readCommittedDiffs(0, 1).size());
    }
  }

  private StateArchiveServingWorkerV3 worker(Path root,
      StateArchiveFiveLaneSegmentWriterV3 source, Runnable hook) {
    return new StateArchiveServingWorkerV3(
        () -> new StateArchiveServingIndexBuildCoordinatorV3(root, Engine.LEVELDB, 1000),
        source, hook);
  }

  private StateArchiveFiveLaneSegmentWriterV3 source(Path root) throws IOException {
    return new StateArchiveFiveLaneSegmentWriterV3(root, hash(90),
        StateArchiveFileFormatV3.COMPRESSION_NONE, 1500);
  }

  private void append(StateArchiveFiveLaneSegmentWriterV3 source, int block) throws IOException {
    source.append(new StateArchiveFiveLaneBlockCodecV3().encode(diff(block),
        source.getResultHistoryDigest(), StateArchiveFileFormatV3.COMPRESSION_NONE));
  }

  private static BlockReverseDiff diff(int block) {
    return new BlockReverseDiff(BlockSnapshotMeta.forBlock(block, hash(block), hash(block - 1),
        block * 3000L), Collections.emptyList());
  }

  private static CommonCheckpointTarget target(int first, int last) {
    return CommonCheckpointTarget.restore(hash(70), hash(last + 80), diff(first).getMeta(),
        diff(last).getMeta(), hash(first + 30), hash(last + 31));
  }

  private static byte[] hash(int value) {
    byte[] hash = new byte[32];
    hash[31] = (byte) value;
    return hash;
  }
}
