package org.tron.core.db2.archive;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.bouncycastle.util.encoders.Hex;
import org.tron.core.db2.core.CommonCheckpointTarget;
import org.tron.core.db2.stateroot.PathStateStoreManifest.Engine;

/** Single-owner bulk-to-live coordinator for the append-file v3 exact-27 serving index. */
public final class StateArchiveServingIndexBuildCoordinatorV3 implements AutoCloseable {

  static final String DIRECTORY = "serving-index-v3";
  private final Path archiveRoot;
  private final Path catalogRoot;
  private final Engine engine;
  private final int bulkStartBlocks;
  private final List<BlockReverseDiff> pending = new ArrayList<>();
  private PersistentServingKeyIndexCatalog catalog;
  private CommonCheckpointTarget committedHead;
  private byte[] latestSourceIdentity;
  private long indexedThrough = -1;
  private byte[] indexedHash;
  private long buildSequence;
  private long syncSession;
  private Mode mode = Mode.RECOVERING;
  private boolean closed;

  public StateArchiveServingIndexBuildCoordinatorV3(Path archiveRoot, Engine engine,
      int bulkStartBlocks) throws IOException {
    this.archiveRoot = Objects.requireNonNull(archiveRoot, "archiveRoot");
    this.catalogRoot = archiveRoot.resolve(DIRECTORY);
    this.engine = Objects.requireNonNull(engine, "engine");
    if (bulkStartBlocks <= 0) {
      throw new IllegalArgumentException("bulkStartBlocks must be positive");
    }
    this.bulkStartBlocks = bulkStartBlocks;
    if (Files.isRegularFile(catalogRoot.resolve("current"), LinkOption.NOFOLLOW_LINKS)) {
      catalog = PersistentServingKeyIndexCatalog.open(catalogRoot, engine, stage -> { });
      try (PersistentServingKeyIndexGeneration current = catalog.pin()) {
        indexedThrough = current.getIndexedThrough();
        indexedHash = current.getHeadHash();
      }
    }
    mode = Mode.BULK_CATCH_UP;
  }

  /** Accepts only a Common-published contiguous range; bulk flushes at the configured threshold. */
  public synchronized BuildProgress offerCommittedRange(List<BlockReverseDiff> diffs,
      CommonCheckpointTarget target) throws IOException {
    requireOpen();
    if (mode != Mode.BULK_CATCH_UP) {
      throw new IllegalStateException("Serving bulk input is closed after handoff");
    }
    admit(diffs, target);
    if (pending.size() >= bulkStartBlocks) {
      flushPending();
    }
    return progress();
  }

  synchronized void recoverCommittedRange(List<BlockReverseDiff> supplied,
      CommonCheckpointTarget publishedHead, boolean finalRange) throws IOException {
    requireOpen();
    if (mode != Mode.BULK_CATCH_UP) {
      throw new IllegalStateException("Serving recovery requires bulk mode");
    }
    List<BlockReverseDiff> diffs = new ArrayList<>(Objects.requireNonNull(supplied, "diffs"));
    if (diffs.isEmpty()) {
      if (finalRange) {
        CommonCheckpointTarget target = Objects.requireNonNull(publishedHead, "publishedHead");
        if (indexedThrough != target.getLastBlock().getBlockNumber()
            || !Arrays.equals(indexedHash, target.getLastBlock().getBlockHash())) {
          throw new IOException("Serving recovery zero-action boundary mismatch");
        }
        committedHead = target;
      }
      return;
    }
    BlockSnapshotMeta previous = pending.isEmpty() ? null : pending.get(pending.size() - 1).getMeta();
    if (previous == null && indexedThrough >= 0) {
      BlockSnapshotMeta first = diffs.get(0).getMeta();
      if (first.getBlockNumber() != indexedThrough + 1
          || !Arrays.equals(first.getParentHash(), indexedHash)) {
        throw new IOException("Serving recovery suffix does not extend durable I");
      }
    }
    for (BlockReverseDiff diff : diffs) {
      if (previous != null && (diff.getMeta().getBlockNumber()
          != previous.getBlockNumber() + 1
          || !Arrays.equals(diff.getMeta().getParentHash(), previous.getBlockHash()))) {
        throw new IOException("Serving recovery suffix has a gap");
      }
      previous = diff.getMeta();
    }
    pending.addAll(diffs);
    latestSourceIdentity = StateArchiveFileFormatV3.sha256(
        new BlockHistoryCodec().encode(diffs.get(diffs.size() - 1)));
    if (finalRange) {
      CommonCheckpointTarget target = Objects.requireNonNull(publishedHead, "publishedHead");
      if (!previous.equals(target.getLastBlock())) {
        throw new IOException("Serving recovery suffix differs from published W");
      }
      committedHead = target;
      latestSourceIdentity = target.getPayloadDigest();
    }
    if (pending.size() >= bulkStartBlocks || finalRange) {
      flushPending();
    }
  }

  /** Drains the FIFO through the exact Common boundary and returns a generation-bound live handle. */
  public synchronized LiveServingIndexer completeInitialSync(CommonCheckpointTarget boundary)
      throws IOException {
    requireOpen();
    if (mode != Mode.BULK_CATCH_UP || committedHead == null
        || !committedHead.equals(Objects.requireNonNull(boundary, "boundary"))) {
      throw new IllegalArgumentException("Serving handoff boundary is not the committed head");
    }
    mode = Mode.HANDOFF_DRAINING;
    try {
      flushPending();
      if (indexedThrough != boundary.getLastBlock().getBlockNumber()
          || !Arrays.equals(indexedHash, boundary.getLastBlock().getBlockHash())) {
        throw new IOException("Serving handoff did not reach the exact Common boundary");
      }
      mode = Mode.LIVE_IMMEDIATE;
      syncSession++;
      return new LiveServingIndexer(syncSession, buildSequence,
          catalog.getCurrentGenerationId());
    } catch (IOException | RuntimeException failure) {
      mode = Mode.CATCH_UP_REQUIRED;
      throw failure;
    }
  }

  public synchronized BuildProgress status() {
    requireOpen();
    return progress();
  }

  synchronized void flushRecoveryBatch() throws IOException {
    requireOpen();
    flushPending();
  }

  @Override
  public synchronized void close() throws IOException {
    if (!closed) {
      closed = true;
      mode = Mode.CLOSED;
      if (catalog != null) {
        catalog.close();
      }
    }
  }

  private void admit(List<BlockReverseDiff> supplied, CommonCheckpointTarget target) {
    List<BlockReverseDiff> diffs = new ArrayList<>(Objects.requireNonNull(supplied, "diffs"));
    CommonCheckpointTarget admittedTarget = Objects.requireNonNull(target, "target");
    if (diffs.isEmpty() || diffs.contains(null)
        || !diffs.get(0).getMeta().equals(admittedTarget.getFirstBlock())
        || !diffs.get(diffs.size() - 1).getMeta().equals(admittedTarget.getLastBlock())) {
      throw new IllegalArgumentException("Serving committed range differs from Common target");
    }
    BlockSnapshotMeta expectedParent;
    if (!pending.isEmpty()) {
      expectedParent = pending.get(pending.size() - 1).getMeta();
    } else if (committedHead != null) {
      expectedParent = committedHead.getLastBlock();
    } else if (indexedThrough >= 0) {
      expectedParent = new BlockSnapshotMeta(indexedThrough, indexedThrough, indexedHash,
          new byte[32], 0);
    } else {
      expectedParent = null;
    }
    BlockSnapshotMeta previous = expectedParent;
    for (BlockReverseDiff diff : diffs) {
      BlockSnapshotMeta meta = diff.getMeta();
      if (previous != null && (meta.getBlockNumber() != previous.getBlockNumber() + 1
          || !Arrays.equals(meta.getParentHash(), previous.getBlockHash()))) {
        throw new IllegalArgumentException("Serving committed ranges are not contiguous");
      }
      previous = meta;
    }
    pending.addAll(diffs);
    committedHead = admittedTarget;
    latestSourceIdentity = admittedTarget.getPayloadDigest();
  }

  private void flushPending() throws IOException {
    if (pending.isEmpty()) {
      return;
    }
    long base = indexedThrough >= 0 ? indexedThrough
        : pending.get(0).getMeta().getBlockNumber() - 1;
    byte[] baseHash = indexedHash == null ? pending.get(0).getMeta().getParentHash() : indexedHash;
    ServingIndexIncrementalPlan plan = ServingIndexIncrementalPlan.planCommittedDiffs(
        base, baseHash, pending);
    byte[] sourceIdentity = Objects.requireNonNull(latestSourceIdentity,
        "latestSourceIdentity");
    String generationId = generationId(plan.getIndexedThrough(), plan.getHeadHash());
    Path shadow = archiveRoot.resolve(".serving-index-v3-" + UUID.randomUUID());
    try {
      if (catalog == null) {
        try (PersistentServingKeyIndexGeneration ignored =
            PersistentServingKeyIndexGeneration.buildExact(shadow, generationId, plan,
                sourceIdentity, engine, () -> { })) {
          // Full descriptor and exact-27 coverage are verified again by catalog creation.
        }
        catalog = PersistentServingKeyIndexCatalog.create(catalogRoot, shadow);
      } else {
        String expected = catalog.getCurrentGenerationId();
        try (PersistentServingKeyIndexGeneration current = catalog.pin();
            PersistentServingKeyIndexGeneration ignored = current.extendExact(shadow,
                generationId, plan, sourceIdentity)) {
          // Verify immutable generation before CAS publication.
        }
        if (!catalog.publish(expected, shadow)) {
          throw new IOException("Serving generation changed during FIFO publication");
        }
      }
      indexedThrough = plan.getIndexedThrough();
      indexedHash = plan.getHeadHash();
      buildSequence++;
      pending.clear();
    } catch (IOException | RuntimeException failure) {
      mode = Mode.CATCH_UP_REQUIRED;
      throw failure;
    }
  }

  private String generationId(long blockNumber, byte[] hash) {
    return String.format("append-v3-%020d-%s-%08d", blockNumber,
        Hex.toHexString(Arrays.copyOf(hash, 6)), buildSequence + 1);
  }

  private BuildProgress progress() {
    long committed = committedHead == null ? indexedThrough
        : committedHead.getLastBlock().getBlockNumber();
    return new BuildProgress(mode, indexedThrough, committed, pending.size(), buildSequence);
  }

  private void requireOpen() {
    if (closed) {
      throw new IllegalStateException("Serving coordinator is closed");
    }
  }

  public enum Mode {
    RECOVERING,
    BULK_CATCH_UP,
    HANDOFF_DRAINING,
    LIVE_IMMEDIATE,
    CATCH_UP_REQUIRED,
    DEGRADED,
    CLOSED
  }

  public static final class BuildProgress {
    private final Mode mode;
    private final long indexedThrough;
    private final long committedThrough;
    private final int pendingBlocks;
    private final long buildSequence;

    BuildProgress(Mode mode, long indexedThrough, long committedThrough,
        int pendingBlocks, long buildSequence) {
      this.mode = mode;
      this.indexedThrough = indexedThrough;
      this.committedThrough = committedThrough;
      this.pendingBlocks = pendingBlocks;
      this.buildSequence = buildSequence;
    }

    public Mode getMode() { return mode; }
    public long getIndexedThrough() { return indexedThrough; }
    public long getCommittedThrough() { return committedThrough; }
    public int getPendingBlocks() { return pendingBlocks; }
    public long getBuildSequence() { return buildSequence; }
  }

  public final class LiveServingIndexer {
    private final long session;
    private long sequence;
    private String generation;
    private boolean valid = true;

    private LiveServingIndexer(long session, long sequence, String generation) {
      this.session = session;
      this.sequence = sequence;
      this.generation = generation;
    }

    /** Publishes every block in the committed range as an individual durable generation. */
    public BuildProgress indexNow(List<BlockReverseDiff> diffs, CommonCheckpointTarget target)
        throws IOException {
      synchronized (StateArchiveServingIndexBuildCoordinatorV3.this) {
        requireOpen();
        if (!valid || mode != Mode.LIVE_IMMEDIATE || session != syncSession
            || sequence != buildSequence || !generation.equals(catalog.getCurrentGenerationId())) {
          throw new IllegalStateException("Serving live handle is stale");
        }
        List<BlockReverseDiff> admitted = new ArrayList<>(Objects.requireNonNull(diffs, "diffs"));
        int published = 0;
        boolean rangeAccepted = false;
        try {
          admit(admitted, target);
          rangeAccepted = true;
          pending.clear();
          for (BlockReverseDiff diff : admitted) {
            pending.add(diff);
            flushPending();
            published++;
          }
          sequence = buildSequence;
          generation = catalog.getCurrentGenerationId();
          return progress();
        } catch (IOException | RuntimeException failure) {
          if (rangeAccepted) {
            pending.clear();
            pending.addAll(admitted.subList(published, admitted.size()));
          }
          valid = false;
          mode = Mode.CATCH_UP_REQUIRED;
          throw failure;
        }
      }
    }
  }
}
