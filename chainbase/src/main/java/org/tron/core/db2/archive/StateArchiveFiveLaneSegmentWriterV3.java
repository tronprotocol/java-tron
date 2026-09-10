package org.tron.core.db2.archive;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.tron.core.db2.archive.StateArchiveFiveLaneBlockCodecV3.DecodedBundle;
import org.tron.core.db2.archive.StateArchiveFiveLaneBlockCodecV3.EncodedBundle;
import org.tron.core.db2.archive.StateArchiveFiveLaneBlockCodecV3.EncodedLane;
import org.tron.core.db2.archive.StateArchiveFiveLaneRecoveryIntentV3.Intent;
import org.tron.core.db2.archive.StateArchiveFiveLaneRecoveryIntentV3.LaneTarget;
import org.tron.core.db2.archive.StateArchiveFiveLaneRecoveryIntentV3.RecoveryPoint;
import org.tron.core.db2.archive.StateArchiveSegmentFormatV3.BlockIndexEntry;
import org.tron.core.db2.archive.StateArchiveSegmentFormatV3.BlockIndexHeader;
import org.tron.core.db2.archive.StateArchiveSegmentFormatV3.CurrentSegment;
import org.tron.core.db2.archive.StateArchiveSegmentFormatV3.DurableMarker;
import org.tron.core.db2.archive.StateArchiveSegmentFormatV3.SealedSegment;
import org.tron.core.db2.archive.StateArchiveSegmentFormatV3.SegmentHeader;
import org.tron.core.db2.archive.StateArchiveSegmentFormatV3.SegmentManifest;
import org.tron.core.db2.archive.StateArchiveSegmentFormatV3.SegmentSeal;

/** Default-off five-lane append writer for State Archive v3 segment data and block indexes. */
public final class StateArchiveFiveLaneSegmentWriterV3 implements AutoCloseable {

  private static final int APPEND_BUFFER_BYTES = 2 * 1024 * 1024;
  private static final int BLOCK_NUMBER_OFFSET = 40;
  private static final int PREVIOUS_HISTORY_DIGEST_OFFSET = 152;
  private static final int RESULT_HISTORY_DIGEST_OFFSET = 248;
  private static final int ENTRY_COUNT_OFFSET = 304;
  private static final int COVERAGE_BITMAP_OFFSET = 280;
  private static final int RAW_PAYLOAD_LENGTH_OFFSET = 312;
  private static final int COMPRESSION_ID_OFFSET = 322;
  private static final int ENCODED_DIGEST_FROM_END = 48;

  private final Path segmentRoot;
  private final Path archiveRoot;
  private final byte[] baselineHistoryDigest;
  private final short compressionId;
  private final long rotationTargetBytes;
  private final StateArchiveFiveLaneBlockCodecV3 codec =
      new StateArchiveFiveLaneBlockCodecV3();
  private final Map<Integer, LaneState> lanes = new HashMap<>();
  private final List<SealedSegment> sealedSegments = new ArrayList<>();
  private final StateArchiveHistoryCatalogV3 catalog;
  private boolean structuralChanged;
  private BlockSnapshotMeta appendHead;
  private byte[] resultHistoryDigest;
  private boolean failed;
  private RecoveryRequest activeRecoveryRequest;
  private Intent activeRecoveryIntent;
  private RecoveryFaultHook recoveryFaultHook = RecoveryFaultHook.NONE;
  private ArchiveDurabilityProof lastDurabilityProof;
  private boolean lastDurabilityProofRecovered;
  private boolean rotationWithUnsyncedData;
  private final List<FileTailProof> pendingRotationTails = new ArrayList<>();
  private long activeCheckpointSequence = -1;
  private byte[] activeCommonTargetDigest;

  public StateArchiveFiveLaneSegmentWriterV3(Path archiveRoot,
      byte[] baselineHistoryDigest, short compressionId) throws IOException {
    this(archiveRoot, baselineHistoryDigest, compressionId,
        StateArchiveFileFormatV3.SEGMENT_TARGET_BYTES, null, RecoveryFaultHook.NONE);
  }

  StateArchiveFiveLaneSegmentWriterV3(Path archiveRoot, byte[] baselineHistoryDigest,
      short compressionId, long rotationTargetBytes) throws IOException {
    this(archiveRoot, baselineHistoryDigest, compressionId, rotationTargetBytes, null,
        RecoveryFaultHook.NONE);
  }

  /** Repairs open tails under complete caller and Common identities. */
  public static StateArchiveFiveLaneSegmentWriterV3 recover(Path archiveRoot,
      byte[] baselineHistoryDigest, short compressionId,
      RecoveryPoint authorizedCeiling, RecoveryPoint commonCommitted) throws IOException {
    return new StateArchiveFiveLaneSegmentWriterV3(archiveRoot, baselineHistoryDigest,
        compressionId, StateArchiveFileFormatV3.SEGMENT_TARGET_BYTES,
        new RecoveryRequest(authorizedCeiling, commonCommitted), RecoveryFaultHook.NONE);
  }

  /** Prototype-only numeric boundary retained for component tests, never a Common proof. */
  static StateArchiveFiveLaneSegmentWriterV3 recover(Path archiveRoot,
      byte[] baselineHistoryDigest, short compressionId, long authorizedHead)
      throws IOException {
    if (authorizedHead < 0) {
      throw new IllegalArgumentException("Invalid State Archive recovery boundary");
    }
    return new StateArchiveFiveLaneSegmentWriterV3(archiveRoot, baselineHistoryDigest,
        compressionId, StateArchiveFileFormatV3.SEGMENT_TARGET_BYTES,
        RecoveryRequest.prototype(authorizedHead), RecoveryFaultHook.NONE);
  }

  static StateArchiveFiveLaneSegmentWriterV3 recover(Path archiveRoot,
      byte[] baselineHistoryDigest, short compressionId, long rotationTargetBytes,
      long authorizedHead) throws IOException {
    if (authorizedHead < 0) {
      throw new IllegalArgumentException("Invalid State Archive recovery boundary");
    }
    return new StateArchiveFiveLaneSegmentWriterV3(archiveRoot, baselineHistoryDigest,
        compressionId, rotationTargetBytes, RecoveryRequest.prototype(authorizedHead),
        RecoveryFaultHook.NONE);
  }

  static StateArchiveFiveLaneSegmentWriterV3 recover(Path archiveRoot,
      byte[] baselineHistoryDigest, short compressionId, long rotationTargetBytes,
      RecoveryPoint authorizedCeiling, RecoveryPoint commonCommitted,
      RecoveryFaultHook faultHook) throws IOException {
    return new StateArchiveFiveLaneSegmentWriterV3(archiveRoot, baselineHistoryDigest,
        compressionId, rotationTargetBytes,
        new RecoveryRequest(authorizedCeiling, commonCommitted), faultHook);
  }

  private StateArchiveFiveLaneSegmentWriterV3(Path archiveRoot,
      byte[] baselineHistoryDigest, short compressionId, long rotationTargetBytes,
      RecoveryRequest recoveryRequest, RecoveryFaultHook faultHook) throws IOException {
    Objects.requireNonNull(archiveRoot, "archiveRoot");
    this.baselineHistoryDigest = requireHash(baselineHistoryDigest,
        "baseline history digest");
    requireCompression(compressionId);
    if (rotationTargetBytes <= StateArchiveFileFormatV3.PART_HEADER_LENGTH) {
      throw new IllegalArgumentException("Invalid State Archive rotation target");
    }
    this.compressionId = compressionId;
    this.rotationTargetBytes = rotationTargetBytes;
    this.archiveRoot = archiveRoot;
    this.segmentRoot = archiveRoot.resolve("segments");
    Files.createDirectories(segmentRoot);
    this.catalog = StateArchiveHistoryCatalogV3.openOrEmpty(archiveRoot);
    requireNoLegacyIntent();
    Intent existingIntent = loadIntent();
    if (existingIntent != null) {
      if (!Arrays.equals(existingIntent.getBaselineHistoryDigest(), this.baselineHistoryDigest)) {
        throw new IllegalArgumentException("State Archive recovery intent baseline mismatch");
      }
      recoverWithIntent(existingIntent, faultHook);
    } else if (recoveryRequest != null) {
      planAndRecover(recoveryRequest, faultHook);
    } else {
      discardUnpublishedTemporaryIntent();
      reopen(null);
    }
  }

  private void planAndRecover(RecoveryRequest request, RecoveryFaultHook faultHook)
      throws IOException {
    discardUnpublishedTemporaryIntent();
    activeRecoveryRequest = request;
    recoveryFaultHook = Objects.requireNonNull(faultHook, "faultHook");
    reopen(request.authorizedBlock());
  }

  private void recoverWithIntent(Intent intent, RecoveryFaultHook faultHook)
      throws IOException {
    activeRecoveryIntent = StateArchiveFiveLaneRecoveryIntentV3.decode(
        StateArchiveFiveLaneRecoveryIntentV3.encode(intent));
    recoveryFaultHook = Objects.requireNonNull(faultHook, "faultHook");
    Long target = intent.getTarget() == null ? 0L : intent.getTarget().getBlockNumber();
    reopen(target);
  }

  private void requireNoLegacyIntent() {
    if (Files.exists(archiveRoot.resolve("truncation.intent"))) {
      throw new IllegalArgumentException("Incompatible legacy State Archive truncation intent");
    }
  }

  private Intent loadIntent() throws IOException {
    Path path = archiveRoot.resolve(StateArchiveFiveLaneRecoveryIntentV3.FILE_NAME);
    if (!Files.exists(path)) {
      return null;
    }
    return StateArchiveFiveLaneRecoveryIntentV3.decode(Files.readAllBytes(path));
  }

  private void persistIntent(Intent intent) throws IOException {
    byte[] encoded = StateArchiveFiveLaneRecoveryIntentV3.encode(intent);
    Path temporary = archiveRoot.resolve(StateArchiveFiveLaneRecoveryIntentV3.TEMP_FILE_NAME);
    Path target = archiveRoot.resolve(StateArchiveFiveLaneRecoveryIntentV3.FILE_NAME);
    try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
      writeFully(channel, ByteBuffer.wrap(encoded));
      channel.force(true);
    }
    recoveryFaultHook.after(RecoveryStage.TEMPORARY_FORCED, -1);
    try {
      Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE,
          StandardCopyOption.REPLACE_EXISTING);
    } catch (AtomicMoveNotSupportedException unsupported) {
      throw new IOException("State Archive recovery intent requires atomic move", unsupported);
    }
    syncDirectory(archiveRoot);
    recoveryFaultHook.after(RecoveryStage.INTENT_PUBLISHED, -1);
  }

  private void clearIntent() throws IOException {
    Files.deleteIfExists(archiveRoot.resolve(StateArchiveFiveLaneRecoveryIntentV3.FILE_NAME));
    syncDirectory(archiveRoot);
    recoveryFaultHook.after(RecoveryStage.INTENT_DELETED, -1);
  }

  private void discardUnpublishedTemporaryIntent() throws IOException {
    if (Files.deleteIfExists(
        archiveRoot.resolve(StateArchiveFiveLaneRecoveryIntentV3.TEMP_FILE_NAME))) {
      syncDirectory(archiveRoot);
    }
  }

  /** Appends all five frames; the in-memory append head advances only after every lane succeeds. */
  public synchronized void append(EncodedBundle bundle) throws IOException {
    if (activeCheckpointSequence >= 0) {
      throw new IllegalStateException("State Archive checkpoint append is active");
    }
    append(bundle, -1, null, SyncFaultHook.NONE);
  }

  /** Appends one bundle under the Common identity needed to prove any intervening rotation. */
  public synchronized void appendForCheckpoint(EncodedBundle bundle, long checkpointSequence,
      byte[] commonTargetDigest) throws IOException {
    appendForCheckpoint(bundle, checkpointSequence, commonTargetDigest, SyncFaultHook.NONE);
  }

  synchronized void appendForCheckpoint(EncodedBundle bundle, long checkpointSequence,
      byte[] commonTargetDigest, SyncFaultHook faultHook) throws IOException {
    if (checkpointSequence < 0) {
      throw new IllegalArgumentException("Invalid State Archive checkpoint sequence");
    }
    byte[] admittedDigest = requireHash(commonTargetDigest, "Common target digest");
    if (activeCheckpointSequence >= 0
        && (activeCheckpointSequence != checkpointSequence
        || !Arrays.equals(activeCommonTargetDigest, admittedDigest))) {
      throw new IllegalArgumentException("State Archive checkpoint append identity mismatch");
    }
    append(bundle, checkpointSequence, admittedDigest,
        Objects.requireNonNull(faultHook, "faultHook"));
  }

  private void append(EncodedBundle bundle, long checkpointSequence,
      byte[] commonTargetDigest, SyncFaultHook faultHook) throws IOException {
    requireUsable();
    Objects.requireNonNull(bundle, "bundle");
    List<byte[]> frames = bundle.getLanes().stream().map(EncodedLane::getFrame)
        .collect(Collectors.toList());
    DecodedBundle decoded = codec.decode(frames);
    BlockSnapshotMeta meta = decoded.getDiff().getMeta();
    validateNext(meta, frames.get(0));
    if (checkpointSequence >= 0 && activeCheckpointSequence < 0) {
      activeCheckpointSequence = checkpointSequence;
      activeCommonTargetDigest = Arrays.copyOf(commonTargetDigest, commonTargetDigest.length);
    }
    try {
      for (EncodedLane lane : bundle.getLanes()) {
        if (ByteBuffer.wrap(lane.getFrame()).getShort(COMPRESSION_ID_OFFSET)
            != compressionId) {
          throw new IllegalArgumentException("State Archive writer compression mismatch");
        }
        LaneState state = lanes.get(lane.getLaneId());
        if (state != null && StateArchiveSegmentFormatV3.shouldRotate(
            state.blockFrameCount, state.dataEndOffset, rotationTargetBytes)) {
          // A fully marked segment belongs to the preceding checkpoint. Seal it without
          // adding its old marker to the new checkpoint's durability proof.
          if (checkpointSequence >= 0
              && state.markedBlockFrameCount < state.blockFrameCount) {
            addPendingTail(markRotation(state, checkpointSequence,
                commonTargetDigest, faultHook));
          }
          seal(state);
          state = null;
        }
        if (state == null) {
          state = openNewSegment(lane.getLaneId(), meta.getBlockNumber(),
              previousHistoryDigest(lane.getFrame()));
        }
        appendLaneFrame(state, meta, lane);
      }
      appendHead = meta;
      resultHistoryDigest = decoded.getResultHistoryDigest();
      if (structuralChanged || !catalog.isPublished()) {
        publishCatalog();
      }
    } catch (IOException | RuntimeException failure) {
      failed = true;
      throw failure;
    }
  }

  public synchronized BlockSnapshotMeta getAppendHead() {
    return appendHead;
  }

  public synchronized long getHistoryStartBlock() {
    long first = Long.MAX_VALUE;
    for (CurrentSegment segment : getCurrentSegments()) {
      first = Math.min(first, segment.getFirstBlock());
    }
    for (SealedSegment segment : sealedSegments) {
      first = Math.min(first, segment.getFirstBlock());
    }
    return first == Long.MAX_VALUE ? -1 : first;
  }

  public synchronized byte[] getResultHistoryDigest() {
    return resultHistoryDigest == null ? Arrays.copyOf(baselineHistoryDigest,
        baselineHistoryDigest.length) : Arrays.copyOf(resultHistoryDigest,
        resultHistoryDigest.length);
  }

  public synchronized List<CurrentSegment> getCurrentSegments() {
    return lanes.values().stream().sorted(Comparator.comparingInt(state -> state.laneId))
        .map(LaneState::currentMap).collect(Collectors.toList());
  }

  public synchronized List<SealedSegment> getSealedSegments() {
    return Collections.unmodifiableList(new ArrayList<>(sealedSegments));
  }

  /** Replays complete five-lane bundles from Catalog-selected authority for serving repair. */
  public synchronized List<BlockReverseDiff> readCommittedDiffs(long fromExclusive, long through)
      throws IOException {
    requireUsable();
    if (fromExclusive < 0 || through < fromExclusive || appendHead == null
        || through > appendHead.getBlockNumber()) {
      throw new IllegalArgumentException("Invalid State Archive committed read range");
    }
    Map<Long, Map<Integer, byte[]>> bundles = new java.util.TreeMap<>();
    for (Path path : listDataFiles(false)) {
      try (FileChannel data = FileChannel.open(path, StandardOpenOption.READ)) {
        long offset = StateArchiveFileFormatV3.PART_HEADER_LENGTH;
        while (offset < data.size()) {
          byte[] envelope = readExact(data, offset,
              StateArchiveFileFormatV3.FRAME_ENVELOPE_LENGTH);
          ByteBuffer fields = ByteBuffer.wrap(envelope);
          if (fields.getInt(0) != StateArchiveFileFormatV3.FRAME_MAGIC) {
            throw new IOException("State Archive serving source frame magic mismatch");
          }
          short frameType = fields.getShort(8);
          long length = fields.getLong(16);
          if (length <= 0 || length > Integer.MAX_VALUE || length > data.size() - offset) {
            throw new IOException("State Archive serving source frame length mismatch");
          }
          byte[] frame = readExact(data, offset, (int) length);
          if (frameType == StateArchiveFileFormatV3.BLOCK_FRAME_TYPE) {
            long block = blockNumber(frame);
            if (block > fromExclusive && block <= through) {
              int laneId = laneIdFromFrame(frame);
              byte[] previous = bundles.computeIfAbsent(block, ignored -> new HashMap<>())
                  .put(laneId, frame);
              if (previous != null) {
                throw new IOException("Duplicate State Archive serving source lane frame");
              }
            }
          }
          offset += length;
        }
      }
    }
    List<BlockReverseDiff> result = new ArrayList<>();
    for (long block = fromExclusive + 1; block <= through; block++) {
      Map<Integer, byte[]> laneFrames = bundles.get(block);
      if (laneFrames == null
          || laneFrames.size() != StateArchiveFileFormatV3.fiveLaneIds().length) {
        throw new IOException("Incomplete State Archive serving source bundle");
      }
      List<byte[]> ordered = new ArrayList<>();
      for (int laneId : StateArchiveFileFormatV3.fiveLaneIds()) {
        ordered.add(laneFrames.get(laneId));
      }
      result.add(codec.decode(ordered).getDiff());
    }
    return Collections.unmodifiableList(result);
  }

  private static int laneIdFromFrame(byte[] frame) throws IOException {
    long coverage = ByteBuffer.wrap(frame).getLong(COVERAGE_BITMAP_OFFSET);
    for (int laneId : StateArchiveFileFormatV3.fiveLaneIds()) {
      if (coverage == StateArchiveFileFormatV3.laneCoverage(laneId)) {
        return laneId;
      }
    }
    throw new IOException("State Archive serving source lane coverage mismatch");
  }

  public synchronized ArchiveDurabilityProof getLastDurabilityProof() {
    return lastDurabilityProof;
  }

  /** Forces one complete five-lane marker barrier and returns proof only after reread. */
  public synchronized ArchiveDurabilityProof sync(long checkpointSequence,
      RecoveryPoint target, byte[] commonTargetDigest) throws IOException {
    return sync(checkpointSequence, target, commonTargetDigest, SyncFaultHook.NONE);
  }

  synchronized ArchiveDurabilityProof sync(long checkpointSequence,
      RecoveryPoint target, byte[] commonTargetDigest, SyncFaultHook faultHook)
      throws IOException {
    requireUsable();
    RecoveryPoint admittedTarget = Objects.requireNonNull(target, "target");
    byte[] admittedCommonDigest = requireHash(commonTargetDigest, "Common target digest");
    SyncFaultHook admittedFaultHook = Objects.requireNonNull(faultHook, "faultHook");
    if (appendHead == null) {
      throw new IllegalStateException("State Archive has no bundle to sync");
    }
    requireSamePoint(recoveryPoint(appendHead, resultHistoryDigest), admittedTarget,
        "durability target");
    if (rotationWithUnsyncedData) {
      throw new IllegalStateException(
          "State Archive sync cannot prove a segment rotated before its marker");
    }
    List<LaneState> ordered = lanes.values().stream()
        .sorted(Comparator.comparingInt(state -> state.laneId))
        .collect(Collectors.toList());
    if (ordered.size() != StateArchiveFileFormatV3.fiveLaneIds().length) {
      throw new IllegalStateException("State Archive sync requires five current lanes");
    }
    boolean hasUnsynced = !pendingRotationTails.isEmpty() || ordered.stream()
        .anyMatch(state -> state.blockFrameCount > state.markedBlockFrameCount);
    if (!hasUnsynced) {
      if (lastDurabilityProof == null
          || !Arrays.equals(lastDurabilityProof.getCommonTargetDigest(),
              admittedCommonDigest)) {
        throw new IllegalStateException("State Archive sync has no new bundle range");
      }
      requireSamePoint(lastDurabilityProof.getTarget(), admittedTarget,
          "reused durability target");
      if (lastDurabilityProofRecovered) {
        reforceRecoveredProof(lastDurabilityProof, admittedFaultHook);
        lastDurabilityProofRecovered = false;
      }
      return lastDurabilityProof;
    }
    if (checkpointSequence < 0 || lastDurabilityProof != null
        && checkpointSequence <= lastDurabilityProof.getCheckpointSequence()
        || ordered.stream().anyMatch(state -> checkpointSequence < state.lastCheckpointSequence)) {
      throw new IllegalArgumentException("Invalid State Archive checkpoint sequence");
    }
    if (activeCheckpointSequence >= 0
        && (activeCheckpointSequence != checkpointSequence
        || !Arrays.equals(activeCommonTargetDigest, admittedCommonDigest))) {
      throw new IllegalArgumentException("State Archive checkpoint barrier identity mismatch");
    }
    List<MarkerWrite> markers = new ArrayList<>();
    try {
      for (LaneState state : ordered) {
        long blockCount = state.blockFrameCount - state.markedBlockFrameCount;
        if (blockCount == 0 && state.lastCheckpointSequence == checkpointSequence
            && state.lastBlock == admittedTarget.getBlockNumber()
            && Arrays.equals(state.lastCommonTargetDigest, admittedCommonDigest)) {
          addPendingTail(new FileTailProof(state.laneId, state.segmentSeq,
              state.lastMarkerOffset, StateArchiveFileFormatV3.MARKER_HEADER_LENGTH
                  + StateArchiveFileFormatV3.FRAME_TRAILER_LENGTH,
              state.lastMarkerEndOffset, state.previousMarkerDigest));
          continue;
        }
        if (blockCount <= 0 || state.lastBlock != admittedTarget.getBlockNumber()) {
          throw new IllegalStateException("State Archive sync lane range mismatch");
        }
        long firstBlock = state.lastBlock - blockCount + 1;
        long encodedBytes = state.encodedBlockFrameBytes - state.markedEncodedBytes;
        long markerOffset = state.dataEndOffset;
        long markerEnd = markerOffset + StateArchiveFileFormatV3.MARKER_HEADER_LENGTH
            + StateArchiveFileFormatV3.FRAME_TRAILER_LENGTH;
        DurableMarker marker = new DurableMarker(checkpointSequence, firstBlock,
            admittedTarget.getBlockNumber(), firstBlock, admittedTarget.getBlockNumber(),
            admittedTarget.getBlockHash(), admittedTarget.getResultHistoryDigest(),
            state.lastMarkerEndOffset, markerEnd, blockCount,
            state.logicalPayloadBytes - state.markedLogicalBytes, encodedBytes,
            state.previousMarkerDigest, admittedCommonDigest, state.laneId,
            state.segmentSeq);
        byte[] encoded = StateArchiveSegmentFormatV3.encodeDurableMarker(marker);
        state.data.position(markerOffset);
        writeFully(state.data, ByteBuffer.wrap(encoded));
        state.contentDigest.update(encoded);
        state.dataEndOffset = markerEnd;
        markers.add(new MarkerWrite(state, markerOffset, encoded,
            StateArchiveSegmentFormatV3.decodeDurableMarker(encoded)));
        admittedFaultHook.after(SyncStage.MARKER_WRITTEN, state.laneId);
      }
      for (LaneState state : ordered) {
        state.data.force(false);
        admittedFaultHook.after(SyncStage.DATA_FORCED, state.laneId);
      }
      List<FileTailProof> tails = new ArrayList<>(pendingRotationTails);
      for (MarkerWrite write : markers) {
        byte[] reread = readExact(write.state.data, write.offset, write.encoded.length);
        DurableMarker decoded = StateArchiveSegmentFormatV3.decodeDurableMarker(reread);
        if (!Arrays.equals(reread, write.encoded)
            || write.state.data.size() != decoded.getMarkerEndOffset()) {
          throw new IllegalArgumentException("State Archive durable marker reread mismatch");
        }
        tails.add(new FileTailProof(decoded.getLaneId(), decoded.getSegmentSeq(),
            write.offset, reread.length, decoded.getMarkerEndOffset(),
            decoded.getEncodedFrameDigest()));
        admittedFaultHook.after(SyncStage.MARKER_VERIFIED, write.state.laneId);
      }
      tails.sort(Comparator.comparingInt(FileTailProof::getLaneId)
          .thenComparingLong(FileTailProof::getSegmentSeq));
      ArchiveDurabilityProof proof = new ArchiveDurabilityProof(checkpointSequence,
          admittedTarget, admittedCommonDigest, tails);
      verifyDurabilityProof(proof);
      for (MarkerWrite write : markers) {
        LaneState state = write.state;
        state.markedBlockFrameCount = state.blockFrameCount;
        state.markedLogicalBytes = state.logicalPayloadBytes;
        state.markedEncodedBytes = state.encodedBlockFrameBytes;
        state.lastMarkerEndOffset = write.marker.getMarkerEndOffset();
        state.previousMarkerDigest = write.marker.getEncodedFrameDigest();
        state.lastCheckpointSequence = checkpointSequence;
        state.lastCommonTargetDigest = admittedCommonDigest;
      }
      lastDurabilityProof = proof;
      pendingRotationTails.clear();
      activeCheckpointSequence = -1;
      activeCommonTargetDigest = null;
      admittedFaultHook.after(SyncStage.PROOF_READY, -1);
      return proof;
    } catch (IOException | RuntimeException failure) {
      failed = true;
      throw failure;
    }
  }

  /** Reopens proof bytes by exact lane/segment/offset; ordinary readable bytes are insufficient. */
  public synchronized void verifyDurabilityProof(ArchiveDurabilityProof proof)
      throws IOException {
    ArchiveDurabilityProof admitted = Objects.requireNonNull(proof, "proof");
    int activeLane = -1;
    long previousBlock = -1;
    DurableMarker lastMarker = null;
    for (FileTailProof tail : admitted.getFileTails()) {
      if (tail.getLaneId() != activeLane) {
        requireFinalProofMarker(lastMarker, admitted);
        activeLane = tail.getLaneId();
        previousBlock = -1;
      }
      Path path = dataPath(tail.getLaneId(), tail.getSegmentSeq());
      try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
        byte[] encoded = readExact(channel, tail.getMarkerOffset(), tail.getMarkerLength());
        DurableMarker marker = StateArchiveSegmentFormatV3.decodeDurableMarker(encoded);
        if (channel.size() < tail.getMarkerEndOffset()
            || marker.getCheckpointSequence() != admitted.getCheckpointSequence()
            || marker.getLaneId() != tail.getLaneId()
            || marker.getSegmentSeq() != tail.getSegmentSeq()
            || marker.getMarkerEndOffset() != tail.getMarkerEndOffset()
            || marker.getLastBlock() <= previousBlock
            || marker.getLastBlock() > admitted.getTarget().getBlockNumber()
            || !Arrays.equals(marker.getCommonTargetDigest(),
                admitted.getCommonTargetDigest())
            || !Arrays.equals(marker.getEncodedFrameDigest(), tail.getMarkerDigest())) {
          throw new IllegalArgumentException("State Archive durability proof mismatch");
        }
        previousBlock = marker.getLastBlock();
        lastMarker = marker;
      }
    }
    requireFinalProofMarker(lastMarker, admitted);
  }

  private static void requireFinalProofMarker(DurableMarker marker,
      ArchiveDurabilityProof proof) {
    if (marker != null && (marker.getLastBlock() != proof.getTarget().getBlockNumber()
        || !Arrays.equals(marker.getLastBlockHash(), proof.getTarget().getBlockHash())
        || !Arrays.equals(marker.getResultHistoryDigest(),
            proof.getTarget().getResultHistoryDigest()))) {
      throw new IllegalArgumentException("State Archive durability proof final tail mismatch");
    }
  }

  private void validateNext(BlockSnapshotMeta meta, byte[] firstFrame) {
    byte[] previousDigest = previousHistoryDigest(firstFrame);
    if (appendHead == null) {
      if (!Arrays.equals(previousDigest, baselineHistoryDigest)) {
        throw new IllegalArgumentException("State Archive first bundle history mismatch");
      }
      return;
    }
    if (meta.getBlockNumber() != appendHead.getBlockNumber() + 1
        || meta.getEpoch() != appendHead.getEpoch() + 1
        || !Arrays.equals(meta.getParentHash(), appendHead.getBlockHash())
        || !Arrays.equals(previousDigest, resultHistoryDigest)) {
      throw new IllegalArgumentException("State Archive bundle is not expected-next");
    }
  }

  private LaneState openNewSegment(int laneId, long firstBlock,
      byte[] previousHistory) throws IOException {
    LaneState prior = lanes.get(laneId);
    long sequence = prior == null ? nextSequence(laneId) : prior.segmentSeq + 1;
    byte[] previousSegment = sequence == 0
        ? StateArchiveSegmentFormatV3.laneBaselineDigest(laneId)
        : previousChainDigest(laneId, sequence - 1);
    SegmentHeader header = new SegmentHeader(laneId, sequence, firstBlock,
        previousSegment, previousHistory, compressionId);
    byte[] headerBytes = StateArchiveSegmentFormatV3.encodeHeader(header);
    SegmentHeader decodedHeader = StateArchiveSegmentFormatV3.decodeHeader(headerBytes);
    Path dataPath = dataPath(laneId, sequence);
    Path indexPath = indexPath(laneId, sequence);
    Files.createDirectories(dataPath.getParent());
    FileChannel data = FileChannel.open(dataPath, StandardOpenOption.CREATE_NEW,
        StandardOpenOption.READ, StandardOpenOption.WRITE);
    FileChannel index = null;
    try {
      writeFully(data, ByteBuffer.wrap(headerBytes));
      data.force(false);
      BlockIndexHeader indexHeader = new BlockIndexHeader(laneId, sequence,
          decodedHeader.getHeaderDigest());
      index = FileChannel.open(indexPath, StandardOpenOption.CREATE_NEW,
          StandardOpenOption.READ, StandardOpenOption.WRITE);
      writeFully(index, ByteBuffer.wrap(
          StateArchiveSegmentFormatV3.encodeBlockIndexHeader(indexHeader)));
      index.force(false);
      syncDirectory(dataPath.getParent());
      LaneState state = new LaneState(laneId, sequence, firstBlock,
          decodedHeader.getHeaderDigest(), previousHistory, data, index,
          newContentDigest(headerBytes));
      lanes.put(laneId, state);
      structuralChanged = true;
      return state;
    } catch (IOException | RuntimeException failure) {
      data.close();
      if (index != null) {
        index.close();
      }
      throw failure;
    }
  }

  private void appendLaneFrame(LaneState state, BlockSnapshotMeta meta, EncodedLane lane)
      throws IOException {
    byte[] frame = lane.getFrame();
    if (frame.length > StateArchiveFileFormatV3.MAX_BLOCK_FRAME_BYTES) {
      throw new IllegalArgumentException("State Archive block frame exceeds 64 MiB");
    }
    long offset = state.dataEndOffset;
    state.data.position(offset);
    writeBuffered(state.data, frame, state.appendBuffer);
    state.contentDigest.update(frame);
    byte[] indexEntry = StateArchiveSegmentFormatV3.encodeBlockIndexEntry(
        new BlockIndexEntry(meta.getBlockNumber(), offset, frame.length,
            ByteBuffer.wrap(lane.getEncodedFrameDigest()).getLong()));
    state.index.position(state.index.size());
    writeFully(state.index, ByteBuffer.wrap(indexEntry));
    state.record(meta, frame, lane.getEncodedFrameDigest());
  }

  private FileTailProof markRotation(LaneState state, long checkpointSequence,
      byte[] commonTargetDigest, SyncFaultHook faultHook) throws IOException {
    if (state.lastMeta == null) {
      throw new IllegalStateException("State Archive rotation has no unmarked bundle range");
    }
    if (state.markedBlockFrameCount == state.blockFrameCount) {
      if (state.lastCheckpointSequence != checkpointSequence
          || !Arrays.equals(state.lastCommonTargetDigest, commonTargetDigest)) {
        throw new IllegalStateException("State Archive rotation marker identity differs");
      }
      return new FileTailProof(state.laneId, state.segmentSeq, state.lastMarkerOffset,
          StateArchiveFileFormatV3.MARKER_HEADER_LENGTH
              + StateArchiveFileFormatV3.FRAME_TRAILER_LENGTH,
          state.lastMarkerEndOffset, state.previousMarkerDigest);
    }
    long blockCount = state.blockFrameCount - state.markedBlockFrameCount;
    long firstBlock = state.lastBlock - blockCount + 1;
    long markerOffset = state.dataEndOffset;
    long markerEnd = markerOffset + StateArchiveFileFormatV3.MARKER_HEADER_LENGTH
        + StateArchiveFileFormatV3.FRAME_TRAILER_LENGTH;
    DurableMarker marker = new DurableMarker(checkpointSequence, firstBlock,
        state.lastBlock, firstBlock, state.lastBlock, state.lastMeta.getBlockHash(),
        state.endHistoryDigest, state.lastMarkerEndOffset, markerEnd, blockCount,
        state.logicalPayloadBytes - state.markedLogicalBytes,
        state.encodedBlockFrameBytes - state.markedEncodedBytes,
        state.previousMarkerDigest, commonTargetDigest, state.laneId, state.segmentSeq);
    byte[] encoded = StateArchiveSegmentFormatV3.encodeDurableMarker(marker);
    state.data.position(markerOffset);
    writeFully(state.data, ByteBuffer.wrap(encoded));
    state.contentDigest.update(encoded);
    state.dataEndOffset = markerEnd;
    faultHook.after(SyncStage.MARKER_WRITTEN, state.laneId);
    state.data.force(false);
    faultHook.after(SyncStage.DATA_FORCED, state.laneId);
    byte[] reread = readExact(state.data, markerOffset, encoded.length);
    DurableMarker decoded = StateArchiveSegmentFormatV3.decodeDurableMarker(reread);
    if (!Arrays.equals(reread, encoded) || state.data.size() != decoded.getMarkerEndOffset()) {
      throw new IllegalArgumentException("State Archive rotation marker reread mismatch");
    }
    faultHook.after(SyncStage.MARKER_VERIFIED, state.laneId);
    state.markedBlockFrameCount = state.blockFrameCount;
    state.markedLogicalBytes = state.logicalPayloadBytes;
    state.markedEncodedBytes = state.encodedBlockFrameBytes;
    state.lastMarkerEndOffset = decoded.getMarkerEndOffset();
    state.lastMarkerOffset = markerOffset;
    state.previousMarkerDigest = decoded.getEncodedFrameDigest();
    state.lastCheckpointSequence = checkpointSequence;
    state.lastCommonTargetDigest = commonTargetDigest;
    return new FileTailProof(state.laneId, state.segmentSeq, markerOffset,
        reread.length, decoded.getMarkerEndOffset(), decoded.getEncodedFrameDigest());
  }

  private void addPendingTail(FileTailProof tail) {
    boolean exists = pendingRotationTails.stream().anyMatch(candidate ->
        candidate.getLaneId() == tail.getLaneId()
            && candidate.getSegmentSeq() == tail.getSegmentSeq());
    if (!exists) {
      pendingRotationTails.add(tail);
    }
  }

  private void seal(LaneState state) throws IOException {
    if (state.markedBlockFrameCount != state.blockFrameCount) {
      rotationWithUnsyncedData = true;
    }
    byte[] contentDigest = state.contentDigest.digest();
    int sealLength = StateArchiveFileFormatV3.SEAL_HEADER_LENGTH
        + StateArchiveFileFormatV3.FRAME_TRAILER_LENGTH;
    SegmentSeal seal = new SegmentSeal(state.laneId, state.segmentSeq,
        state.firstBlock, state.lastBlock, state.blockFrameCount, state.entryCount,
        state.logicalPayloadBytes, state.encodedBlockFrameBytes, state.dataEndOffset,
        state.dataEndOffset + sealLength, state.firstFrameDigest,
        state.lastFrameDigest, state.startHistoryDigest, state.endHistoryDigest,
        contentDigest);
    byte[] encodedSeal = StateArchiveSegmentFormatV3.encodeSeal(seal);
    SegmentSeal decodedSeal = StateArchiveSegmentFormatV3.decodeSeal(encodedSeal);
    state.data.position(state.dataEndOffset);
    writeFully(state.data, ByteBuffer.wrap(encodedSeal));
    state.data.force(false);
    state.index.force(false);
    if (state.index.size() != StateArchiveFileFormatV3.BLOCK_INDEX_HEADER_LENGTH
        + state.blockFrameCount * StateArchiveFileFormatV3.BLOCK_INDEX_ENTRY_LENGTH) {
      throw new IllegalStateException("State Archive sealed block index length mismatch");
    }
    byte[] previousSegmentDigest = state.segmentSeq == 0
        ? StateArchiveSegmentFormatV3.laneBaselineDigest(state.laneId)
        : previousChainDigest(state.laneId, state.segmentSeq - 1);
    SegmentManifest manifest = new SegmentManifest(state.laneId, state.segmentSeq,
        state.firstBlock, state.lastBlock, state.blockFrameCount, state.entryCount,
        state.logicalPayloadBytes, state.encodedBlockFrameBytes,
        state.dataEndOffset + sealLength, state.index.size(), previousSegmentDigest,
        state.endHistoryDigest);
    byte[] encodedManifest = StateArchiveSegmentFormatV3.encodeManifest(manifest);
    SegmentManifest decodedManifest = StateArchiveSegmentFormatV3.decodeManifest(encodedManifest);
    publishManifest(state.laneId, state.segmentSeq, encodedManifest);
    sealedSegments.add(new SealedSegment(state.laneId, state.segmentSeq,
        state.firstBlock, state.lastBlock, state.blockFrameCount,
        state.dataEndOffset + sealLength, state.index.size(), state.headerDigest,
        contentDigest, decodedSeal.getEncodedFrameDigest(), decodedManifest.getManifestDigest()));
    state.close();
    lanes.remove(state.laneId);
    structuralChanged = true;
  }

  private void reopen(Long recoveryBoundary) throws IOException {
    List<Path> dataFiles = listDataFiles(recoveryBoundary != null || activeRecoveryIntent != null);
    if (dataFiles.isEmpty()) {
      resultHistoryDigest = Arrays.copyOf(baselineHistoryDigest,
          baselineHistoryDigest.length);
      if (activeRecoveryIntent != null) {
        if (activeRecoveryIntent.getTarget() != null) {
          throw new IllegalArgumentException("State Archive recovery target is unavailable");
        }
        try {
          verifyRecoveredIntent(activeRecoveryIntent);
          recoveryFaultHook.after(RecoveryStage.TARGET_VERIFIED, -1);
          clearIntent();
          activeRecoveryIntent = null;
        } catch (IOException | RuntimeException failure) {
          closeAfterFailure(failure);
          throw failure;
        }
      } else if (activeRecoveryRequest != null && !activeRecoveryRequest.prototype
          && activeRecoveryRequest.commonCommitted != null) {
        throw new IllegalArgumentException("State Archive cannot verify Common committed");
      }
      return;
    }
    Map<Long, Map<Integer, byte[]>> bundles = new java.util.TreeMap<>();
    Map<Integer, Long> expectedSequence = new HashMap<>();
    Map<Integer, byte[]> expectedPreviousChain = new HashMap<>();
    List<ScannedSegment> scannedSegments = new ArrayList<>();
    boolean recovering = recoveryBoundary != null;
    for (Path path : dataFiles) {
      ParsedName name = parseName(path);
      long expected = expectedSequence.getOrDefault(name.laneId, 0L);
      if (name.segmentSeq != expected) {
        throw new IllegalArgumentException("Non-contiguous State Archive segment sequence");
      }
      expectedSequence.put(name.laneId, expected + 1);
      ScannedSegment scanned = scanSegment(path, name, bundles, recovering);
      scannedSegments.add(scanned);
      byte[] expectedPrevious = name.segmentSeq == 0
          ? StateArchiveSegmentFormatV3.laneBaselineDigest(name.laneId)
          : expectedPreviousChain.get(name.laneId);
      if (!Arrays.equals(scanned.header.getPreviousSegmentDigest(), expectedPrevious)) {
        throw new IllegalArgumentException("State Archive previous segment chain mismatch");
      }
      if (!recovering && scanned.seal == null) {
        if (lanes.containsKey(name.laneId)) {
          throw new IllegalArgumentException("Multiple open State Archive lane segments");
        }
        lanes.put(name.laneId, scanned.openState());
      } else if (scanned.seal != null) {
        expectedPreviousChain.put(name.laneId, scanned.chainDigest());
        sealedSegments.add(scanned.sealedMap());
      }
    }
    long commonHead = rebuildBundleHead(bundles, recoveryBoundary);
    if (appendHead != null) {
      for (LaneState state : lanes.values()) {
        if (state.lastBlock == appendHead.getBlockNumber()) {
          state.lastMeta = appendHead;
        }
      }
    }
    if (!recovering) {
      rebuildLastDurabilityProof(scannedSegments, bundles);
      validateCatalogSelection();
    }
    if (recovering) {
      Intent intent = activeRecoveryIntent;
      if (intent == null && activeRecoveryRequest != null
          && !activeRecoveryRequest.prototype) {
        RecoveryPoint targetPoint = appendHead == null ? null
            : recoveryPoint(appendHead, resultHistoryDigest);
        verifyRecoveryPoint(activeRecoveryRequest.authorizedCeiling, bundles,
            "authorized ceiling", false);
        if (activeRecoveryRequest.commonCommitted != null) {
          verifyRecoveryPoint(activeRecoveryRequest.commonCommitted, bundles,
              "Common committed", true);
        }
        List<LaneTarget> laneTargets = buildLaneTargets(scannedSegments, commonHead);
        if (laneTargets.stream().noneMatch(StateArchiveFiveLaneSegmentWriterV3::mutates)) {
          intent = null;
        } else {
          intent = new Intent(baselineHistoryDigest,
            activeRecoveryRequest.authorizedCeiling,
            activeRecoveryRequest.commonCommitted, targetPoint,
              laneTargets);
          try {
            persistIntent(intent);
          } catch (IOException | RuntimeException failure) {
            closeScannedAfterFailure(scannedSegments, failure);
            throw failure;
          }
          intent = loadIntent();
          activeRecoveryIntent = intent;
        }
      }
      if (intent != null) {
        long intendedHead = intent.getTarget() == null ? -1
            : intent.getTarget().getBlockNumber();
        if (commonHead != intendedHead) {
          closeScanned(scannedSegments);
          throw new IllegalArgumentException("State Archive recovery target is unavailable");
        }
        verifyIntentSources(intent, scannedSegments);
      }
      for (ScannedSegment scanned : scannedSegments) {
        if (scanned.seal != null && scanned.lastBlock > commonHead) {
          closeScanned(scannedSegments);
          throw new IllegalArgumentException(
              "Authorized State Archive recovery boundary crosses a sealed segment");
        }
      }
      try {
        for (ScannedSegment scanned : scannedSegments) {
          if (scanned.seal == null) {
            scanned.repairTo(commonHead, recoveryFaultHook);
          }
        }
      } catch (IOException | RuntimeException failure) {
        closeScannedAfterFailure(scannedSegments, failure);
        throw failure;
      }
      closeScanned(scannedSegments);
      lanes.clear();
      sealedSegments.clear();
      appendHead = null;
      resultHistoryDigest = Arrays.copyOf(baselineHistoryDigest,
          baselineHistoryDigest.length);
      reopen(null);
      if (intent != null) {
        try {
          verifyRecoveredIntent(intent);
          recoveryFaultHook.after(RecoveryStage.TARGET_VERIFIED, -1);
          structuralChanged = true;
          publishCatalog();
          clearIntent();
          activeRecoveryIntent = null;
        } catch (IOException | RuntimeException failure) {
          closeAfterFailure(failure);
          throw failure;
        }
      }
    }
  }

  private void rebuildLastDurabilityProof(List<ScannedSegment> scannedSegments,
      Map<Long, Map<Integer, byte[]>> bundles) {
    if (appendHead == null) {
      return;
    }
    long latestSequence = scannedSegments.stream()
        .filter(scanned -> scanned.lastMarker != null)
        .mapToLong(scanned -> scanned.lastMarker.marker.getCheckpointSequence())
        .max().orElse(-1);
    if (latestSequence < 0) {
      return;
    }
    List<ScannedMarker> latest = scannedSegments.stream()
        .filter(scanned -> scanned.lastMarker != null
            && scanned.lastMarker.marker.getCheckpointSequence() == latestSequence)
        .map(scanned -> scanned.lastMarker)
        .sorted(Comparator.comparingInt((ScannedMarker marker) -> marker.marker.getLaneId())
            .thenComparingLong(marker -> marker.marker.getSegmentSeq()))
        .collect(Collectors.toList());
    DurableMarker identity = latest.get(0).marker;
    List<FileTailProof> tails = new ArrayList<>();
    for (ScannedMarker candidate : latest) {
      DurableMarker marker = candidate.marker;
      if (marker.getCheckpointSequence() != identity.getCheckpointSequence()
          || !Arrays.equals(marker.getCommonTargetDigest(),
              identity.getCommonTargetDigest())) {
        return;
      }
      tails.add(new FileTailProof(marker.getLaneId(), marker.getSegmentSeq(),
          candidate.offset, StateArchiveFileFormatV3.MARKER_HEADER_LENGTH
              + StateArchiveFileFormatV3.FRAME_TRAILER_LENGTH,
          marker.getMarkerEndOffset(), marker.getEncodedFrameDigest()));
    }
    ArchiveDurabilityProof rebuilt;
    try {
      DurableMarker finalIdentity = null;
      int laneCount = 0;
      for (int laneId : StateArchiveFileFormatV3.fiveLaneIds()) {
        DurableMarker laneFinal = null;
        for (ScannedMarker candidate : latest) {
          if (candidate.marker.getLaneId() == laneId) {
            laneFinal = candidate.marker;
          }
        }
        if (laneFinal == null) {
          throw new IllegalArgumentException("Incomplete State Archive marker group");
        }
        laneCount++;
        if (finalIdentity == null) {
          finalIdentity = laneFinal;
        } else if (laneFinal.getLastBlock() != finalIdentity.getLastBlock()
            || !Arrays.equals(laneFinal.getLastBlockHash(), finalIdentity.getLastBlockHash())
            || !Arrays.equals(laneFinal.getResultHistoryDigest(),
                finalIdentity.getResultHistoryDigest())) {
          throw new IllegalArgumentException("Incomplete State Archive marker identity");
        }
      }
      if (laneCount != StateArchiveFileFormatV3.fiveLaneIds().length) {
        throw new IllegalArgumentException("Incomplete State Archive marker lanes");
      }
      RecoveryPoint target = recoveryPointAt(bundles, finalIdentity.getLastBlock());
      if (target == null
          || !Arrays.equals(finalIdentity.getLastBlockHash(), target.getBlockHash())
          || !Arrays.equals(finalIdentity.getResultHistoryDigest(),
              target.getResultHistoryDigest())) {
        throw new IllegalArgumentException("Incomplete State Archive marker target");
      }
      rebuilt = new ArchiveDurabilityProof(identity.getCheckpointSequence(),
          target, identity.getCommonTargetDigest(), tails);
    } catch (IllegalArgumentException incomplete) {
      pendingRotationTails.addAll(tails);
      activeCheckpointSequence = identity.getCheckpointSequence();
      activeCommonTargetDigest = identity.getCommonTargetDigest();
      return;
    }
    lastDurabilityProof = rebuilt;
    lastDurabilityProofRecovered = true;
  }

  private void reforceRecoveredProof(ArchiveDurabilityProof proof,
      SyncFaultHook faultHook) throws IOException {
    int previousLane = -1;
    long previousSegment = -1;
    for (FileTailProof tail : proof.getFileTails()) {
      if (tail.getLaneId() == previousLane && tail.getSegmentSeq() == previousSegment) {
        continue;
      }
      LaneState current = lanes.get(tail.getLaneId());
      if (current != null && current.segmentSeq == tail.getSegmentSeq()) {
        current.data.force(false);
      } else {
        try (FileChannel channel = FileChannel.open(
            dataPath(tail.getLaneId(), tail.getSegmentSeq()), StandardOpenOption.WRITE)) {
          channel.force(false);
        }
      }
      faultHook.after(SyncStage.DATA_FORCED, tail.getLaneId());
      previousLane = tail.getLaneId();
      previousSegment = tail.getSegmentSeq();
    }
    verifyDurabilityProof(proof);
  }

  private RecoveryPoint recoveryPointAt(Map<Long, Map<Integer, byte[]>> bundles,
      long blockNumber) {
    Map<Integer, byte[]> laneFrames = bundles.get(blockNumber);
    if (laneFrames == null || laneFrames.size() != StateArchiveFileFormatV3.fiveLaneIds().length) {
      return null;
    }
    List<byte[]> frames = new ArrayList<>();
    for (int laneId : StateArchiveFileFormatV3.fiveLaneIds()) {
      frames.add(laneFrames.get(laneId));
    }
    DecodedBundle decoded = codec.decode(frames);
    return recoveryPoint(decoded.getDiff().getMeta(), decoded.getResultHistoryDigest());
  }

  private List<LaneTarget> buildLaneTargets(List<ScannedSegment> scannedSegments,
      long commonHead) throws IOException {
    List<LaneTarget> targets = new ArrayList<>();
    for (int laneId : StateArchiveFileFormatV3.fiveLaneIds()) {
      ScannedSegment open = null;
      for (ScannedSegment scanned : scannedSegments) {
        if (scanned.header.getLaneId() == laneId && scanned.seal == null) {
          open = scanned;
        }
      }
      targets.add(open == null ? missingLaneTarget(laneId) : open.laneTarget(commonHead));
    }
    return targets;
  }

  private static LaneTarget missingLaneTarget(int laneId) {
    byte[] zero = new byte[StateArchiveFileFormatV3.HASH_LENGTH];
    return new LaneTarget(laneId, StateArchiveFiveLaneRecoveryIntentV3.SOURCE_PAIR_MISSING,
        StateArchiveFiveLaneRecoveryIntentV3.NO_TARGET_SEGMENT, 0, 0,
        StateArchiveFiveLaneRecoveryIntentV3.NO_TARGET_SEGMENT, 0, 0,
        zero, zero, zero);
  }

  private static boolean mutates(LaneTarget target) {
    return (target.getActionFlags() & (StateArchiveFiveLaneRecoveryIntentV3.DATA_TRUNCATE
        | StateArchiveFiveLaneRecoveryIntentV3.DELETE_PAIR
        | StateArchiveFiveLaneRecoveryIntentV3.INDEX_REPLACE)) != 0;
  }

  private void verifyIntentSources(Intent intent, List<ScannedSegment> scannedSegments)
      throws IOException {
    for (LaneTarget target : intent.getLanes()) {
      ScannedSegment source = null;
      for (ScannedSegment scanned : scannedSegments) {
        if (scanned.seal == null && scanned.header.getLaneId() == target.getLaneId()
            && scanned.header.getSegmentSeq() == target.getSourceSegmentSeq()) {
          source = scanned;
        }
      }
      if ((target.getActionFlags()
          & StateArchiveFiveLaneRecoveryIntentV3.SOURCE_PAIR_MISSING) != 0) {
        if (source != null || hasLaneIndexFile(target.getLaneId())) {
          throw new IllegalArgumentException("State Archive missing-source lane appeared");
        }
        continue;
      }
      if (source == null) {
        if ((target.getActionFlags() & StateArchiveFiveLaneRecoveryIntentV3.DELETE_PAIR) != 0
            && !Files.exists(dataPath(target.getLaneId(), target.getSourceSegmentSeq()))
            && !Files.exists(indexPath(target.getLaneId(), target.getSourceSegmentSeq()))) {
          continue;
        }
        throw new IllegalArgumentException("State Archive recovery source is missing");
      }
      long dataSize = source.data.size();
      long indexSize = source.index == null ? 0 : source.index.size();
      boolean deleting = (target.getActionFlags()
          & StateArchiveFiveLaneRecoveryIntentV3.DELETE_PAIR) != 0;
      boolean indexSizeAllowed = indexSize == target.getOriginalIndexEnd()
          || (target.getActionFlags() & StateArchiveFiveLaneRecoveryIntentV3.INDEX_REPLACE) != 0
              && indexSize == target.getTargetIndexEnd()
          || deleting && indexSize == 0;
      boolean targetDataMatches = deleting
          || Arrays.equals(digestFilePrefix(StateArchiveFileFormatV3.RECOVERY_DATA_PREFIX_DOMAIN,
              source.data, target.getTargetDataEnd()), target.getTargetDataPrefixDigest());
      boolean targetIndexMatches = deleting || indexSize != target.getTargetIndexEnd()
          || source.index != null && Arrays.equals(digestFilePrefix(
              StateArchiveFileFormatV3.RECOVERY_INDEX_FILE_DOMAIN, source.index,
              target.getTargetIndexEnd()), target.getTargetIndexFileDigest());
      if (!Arrays.equals(source.header.getHeaderDigest(),
          target.getSourceSegmentHeaderDigest())
          || dataSize < target.getTargetDataEnd()
          || dataSize > target.getOriginalDataEnd()
          || !indexSizeAllowed || !targetDataMatches || !targetIndexMatches) {
        throw new IllegalArgumentException("State Archive recovery source drifted");
      }
    }
  }

  private void verifyRecoveryPoint(RecoveryPoint expected,
      Map<Long, Map<Integer, byte[]>> bundles, String name, boolean required) {
    Map<Integer, byte[]> laneFrames = bundles.get(expected.getBlockNumber());
    if (laneFrames == null || laneFrames.size() != StateArchiveFileFormatV3.fiveLaneIds().length) {
      if (required) {
        throw new IllegalArgumentException("State Archive cannot verify " + name);
      }
      return;
    }
    List<byte[]> frames = new ArrayList<>();
    for (int laneId : StateArchiveFileFormatV3.fiveLaneIds()) {
      frames.add(laneFrames.get(laneId));
    }
    DecodedBundle decoded = codec.decode(frames);
    RecoveryPoint actual = recoveryPoint(decoded.getDiff().getMeta(),
        decoded.getResultHistoryDigest());
    requireSamePoint(actual, expected, name);
  }

  private void verifyRecoveredIntent(Intent intent) {
    RecoveryPoint target = intent.getTarget();
    if (target == null) {
      if (appendHead != null) {
        throw new IllegalArgumentException("State Archive baseline recovery retained blocks");
      }
    } else {
      if (appendHead == null) {
        throw new IllegalArgumentException("State Archive recovery target is missing");
      }
      requireSamePoint(recoveryPoint(appendHead, resultHistoryDigest), target,
          "recovered target");
    }
    RecoveryPoint common = intent.getCommonCommitted();
    if (common != null && (appendHead == null
        || appendHead.getBlockNumber() < common.getBlockNumber())) {
      throw new IllegalArgumentException("State Archive recovery fell below Common");
    }
    for (LaneTarget lane : intent.getLanes()) {
      verifyLaneTarget(lane);
    }
  }

  private void verifyLaneTarget(LaneTarget target) {
    if ((target.getActionFlags() & StateArchiveFiveLaneRecoveryIntentV3.SOURCE_PAIR_MISSING)
        != 0) {
      try {
        if (hasLaneIndexFile(target.getLaneId())) {
          throw new IllegalArgumentException(
              "State Archive missing-source recovery index appeared");
        }
      } catch (IOException failure) {
        throw new IllegalArgumentException(
            "Cannot verify State Archive missing-source recovery lane", failure);
      }
      return;
    }
    Path data = dataPath(target.getLaneId(), target.getSourceSegmentSeq());
    Path index = indexPath(target.getLaneId(), target.getSourceSegmentSeq());
    if ((target.getActionFlags() & StateArchiveFiveLaneRecoveryIntentV3.DELETE_PAIR) != 0) {
      if (Files.exists(data) || Files.exists(index)) {
        throw new IllegalArgumentException("State Archive deleted recovery pair remains");
      }
      return;
    }
    try {
      try (FileChannel dataChannel = FileChannel.open(data, StandardOpenOption.READ);
          FileChannel indexChannel = FileChannel.open(index, StandardOpenOption.READ)) {
        if (dataChannel.size() != target.getTargetDataEnd()
            || indexChannel.size() != target.getTargetIndexEnd()
            || !Arrays.equals(digestFilePrefix(
                StateArchiveFileFormatV3.RECOVERY_DATA_PREFIX_DOMAIN, dataChannel,
                target.getTargetDataEnd()), target.getTargetDataPrefixDigest())
            || !Arrays.equals(digestFilePrefix(
                StateArchiveFileFormatV3.RECOVERY_INDEX_FILE_DOMAIN, indexChannel,
                target.getTargetIndexEnd()), target.getTargetIndexFileDigest())) {
          throw new IllegalArgumentException("State Archive recovered lane target mismatch");
        }
      }
    } catch (IOException failure) {
      throw new IllegalArgumentException("Cannot verify State Archive recovered lane", failure);
    }
  }

  private static RecoveryPoint recoveryPoint(BlockSnapshotMeta meta, byte[] historyDigest) {
    return new RecoveryPoint(meta.getEpoch(), meta.getBlockNumber(), meta.getTimestamp(),
        meta.getBlockHash(), meta.getParentHash(), historyDigest);
  }

  private static void requireSamePoint(RecoveryPoint actual, RecoveryPoint expected,
      String name) {
    if (actual.getEpoch() != expected.getEpoch()
        || actual.getBlockNumber() != expected.getBlockNumber()
        || actual.getTimestamp() != expected.getTimestamp()
        || !Arrays.equals(actual.getBlockHash(), expected.getBlockHash())
        || !Arrays.equals(actual.getParentHash(), expected.getParentHash())
        || !Arrays.equals(actual.getResultHistoryDigest(), expected.getResultHistoryDigest())) {
      throw new IllegalArgumentException("State Archive " + name + " mismatch");
    }
  }

  private ScannedSegment scanSegment(Path path, ParsedName name,
      Map<Long, Map<Integer, byte[]>> bundles, boolean recovering) throws IOException {
    FileChannel data = FileChannel.open(path, StandardOpenOption.READ,
        StandardOpenOption.WRITE);
    Path indexPath = indexPath(name.laneId, name.segmentSeq);
    FileChannel index = Files.exists(indexPath) ? FileChannel.open(indexPath,
        StandardOpenOption.READ, StandardOpenOption.WRITE) : null;
    try {
      byte[] headerBytes = readExact(data, 0, StateArchiveFileFormatV3.PART_HEADER_LENGTH);
      SegmentHeader header = StateArchiveSegmentFormatV3.decodeHeader(headerBytes);
      if (header.getLaneId() != name.laneId || header.getSegmentSeq() != name.segmentSeq
          || header.getCompressionId() != compressionId) {
        throw new IllegalArgumentException("State Archive segment filename/header mismatch");
      }
      if (index != null && index.size() >= StateArchiveFileFormatV3.BLOCK_INDEX_HEADER_LENGTH) {
        try {
          BlockIndexHeader indexHeader = StateArchiveSegmentFormatV3.decodeBlockIndexHeader(
              readExact(index, 0, StateArchiveFileFormatV3.BLOCK_INDEX_HEADER_LENGTH));
          if (indexHeader.getLaneId() != name.laneId
              || indexHeader.getSegmentSeq() != name.segmentSeq
              || !Arrays.equals(indexHeader.getDataSegmentHeaderDigest(),
                  header.getHeaderDigest())) {
            throw new IllegalArgumentException("State Archive block index/header mismatch");
          }
        } catch (IllegalArgumentException invalidIndexHeader) {
          if (!recovering) {
            throw invalidIndexHeader;
          }
        }
      } else if (!recovering) {
        throw new IllegalArgumentException("Truncated State Archive block index header");
      }
      ScannedSegment scanned = new ScannedSegment(path, data, index, indexPath, header,
          headerBytes, isCatalogSelectedCurrent(name));
      long offset = StateArchiveFileFormatV3.PART_HEADER_LENGTH;
      while (offset < data.size()) {
        if (data.size() - offset < StateArchiveFileFormatV3.FRAME_ENVELOPE_LENGTH) {
          if (recovering) {
            scanned.tailDamaged = true;
            break;
          }
          throw new IllegalArgumentException("Truncated State Archive frame envelope");
        }
        byte[] envelope = readExact(data, offset,
            StateArchiveFileFormatV3.FRAME_ENVELOPE_LENGTH);
        ByteBuffer fields = ByteBuffer.wrap(envelope);
        if (fields.getInt(0) != StateArchiveFileFormatV3.FRAME_MAGIC) {
          if (recovering) {
            scanned.tailDamaged = true;
            break;
          }
          throw new IllegalArgumentException("State Archive frame magic mismatch");
        }
        short frameType = fields.getShort(8);
        long totalLength = fields.getLong(16);
        if (totalLength <= 0 || totalLength > Integer.MAX_VALUE
            || totalLength > data.size() - offset) {
          if (recovering) {
            scanned.tailDamaged = true;
            break;
          }
          throw new IllegalArgumentException("Truncated State Archive frame");
        }
        byte[] frame = readExact(data, offset, (int) totalLength);
        if (frameType == StateArchiveFileFormatV3.BLOCK_FRAME_TYPE) {
          scanned.addBlock(offset, frame);
          byte[] duplicate = bundles.computeIfAbsent(blockNumber(frame),
              ignored -> new HashMap<>()).put(name.laneId, frame);
          if (duplicate != null) {
            throw new IllegalArgumentException("Duplicate State Archive lane block frame");
          }
        } else if (frameType == StateArchiveFileFormatV3.DURABLE_MARKER_FRAME_TYPE) {
          try {
            scanned.addMarker(offset,
                StateArchiveSegmentFormatV3.decodeDurableMarker(frame), frame);
          } catch (IllegalArgumentException invalidMarker) {
            if (!recovering) {
              throw invalidMarker;
            }
            scanned.tailDamaged = true;
            break;
          }
        } else if (frameType == StateArchiveFileFormatV3.PART_SEAL_FRAME_TYPE) {
          if (offset + totalLength != data.size()) {
            throw new IllegalArgumentException("State Archive seal has trailing bytes");
          }
          try {
            scanned.setSeal(StateArchiveSegmentFormatV3.decodeSeal(frame));
          } catch (IllegalArgumentException invalidSeal) {
            if (!recovering) {
              throw invalidSeal;
            }
            scanned.tailDamaged = true;
            break;
          }
        } else {
          if (recovering) {
            scanned.tailDamaged = true;
            break;
          }
          throw new IllegalArgumentException("Unknown State Archive frame type");
        }
        offset += totalLength;
      }
      scanned.finish(recovering);
      return scanned;
    } catch (IOException | RuntimeException failure) {
      data.close();
      if (index != null) {
        index.close();
      }
      throw failure;
    }
  }

  private long rebuildBundleHead(Map<Long, Map<Integer, byte[]>> bundles,
      Long recoveryBoundary) {
    long commonHead = -1;
    for (Map.Entry<Long, Map<Integer, byte[]>> entry : bundles.entrySet()) {
      if (recoveryBoundary != null && entry.getKey() > recoveryBoundary) {
        break;
      }
      List<byte[]> frames = new ArrayList<>();
      for (int laneId : StateArchiveFileFormatV3.fiveLaneIds()) {
        byte[] frame = entry.getValue().get(laneId);
        if (frame == null) {
          if (recoveryBoundary != null) {
            return commonHead;
          }
          throw new IllegalArgumentException("Incomplete State Archive five-lane bundle");
        }
        frames.add(frame);
      }
      DecodedBundle decoded;
      try {
        decoded = codec.decode(frames);
        validateNext(decoded.getDiff().getMeta(), frames.get(0));
      } catch (IllegalArgumentException invalidBundle) {
        if (recoveryBoundary != null) {
          return commonHead;
        }
        throw invalidBundle;
      }
      appendHead = decoded.getDiff().getMeta();
      resultHistoryDigest = decoded.getResultHistoryDigest();
      commonHead = appendHead.getBlockNumber();
    }
    return commonHead;
  }

  private static void closeScanned(List<ScannedSegment> scannedSegments)
      throws IOException {
    IOException failure = null;
    for (ScannedSegment scanned : scannedSegments) {
      try {
        scanned.close();
      } catch (IOException closeFailure) {
        if (failure == null) {
          failure = closeFailure;
        } else {
          failure.addSuppressed(closeFailure);
        }
      }
    }
    if (failure != null) {
      throw failure;
    }
  }

  private static void closeScannedAfterFailure(List<ScannedSegment> scannedSegments,
      Throwable failure) {
    try {
      closeScanned(scannedSegments);
    } catch (IOException closeFailure) {
      failure.addSuppressed(closeFailure);
    }
  }

  private void closeAfterFailure(Throwable failure) {
    try {
      close();
    } catch (IOException closeFailure) {
      failure.addSuppressed(closeFailure);
    }
  }

  private long nextSequence(int laneId) {
    long next = 0;
    for (SealedSegment segment : sealedSegments) {
      if (segment.getLaneId() == laneId) {
        next = Math.max(next, segment.getSegmentSeq() + 1);
      }
    }
    return next;
  }

  private byte[] previousChainDigest(int laneId, long sequence) {
    for (SealedSegment segment : sealedSegments) {
      if (segment.getLaneId() == laneId && segment.getSegmentSeq() == sequence) {
        return StateArchiveSegmentFormatV3.segmentChainDigest(laneId, sequence,
            segment.getSegmentHeaderDigest(), segment.getSegmentContentDigest(),
            segment.getSealFrameDigest());
      }
    }
    throw new IllegalStateException("Missing previous State Archive sealed segment");
  }

  private List<Path> listDataFiles(boolean recovering) throws IOException {
    if (catalog.isPublished()) {
      List<Path> selected = new ArrayList<>();
      for (SealedSegment segment : catalog.selected().getSealed()) {
        selected.add(dataPath(segment.getLaneId(), segment.getSegmentSeq()));
      }
      for (CurrentSegment segment : catalog.selected().getCurrent()) {
        selected.add(dataPath(segment.getLaneId(), segment.getSegmentSeq()));
        Path successor = dataPath(segment.getLaneId(), segment.getSegmentSeq() + 1);
        if (Files.isRegularFile(successor)) {
          selected.add(successor);
        }
      }
      for (Path path : selected) {
        if (!Files.isRegularFile(path)) {
          if (!recovering) {
            throw new IOException("State Archive Catalog selected segment is missing");
          }
        }
      }
      selected.removeIf(path -> !Files.isRegularFile(path));
      selected.sort(Comparator.comparing((Path path) -> parseName(path).laneId)
          .thenComparingLong(path -> parseName(path).segmentSeq));
      return selected;
    }
    try (Stream<Path> paths = Files.walk(segmentRoot)) {
      List<Path> discovered = paths.filter(Files::isRegularFile)
          .filter(path -> path.getFileName().toString().endsWith(".dat"))
          .sorted(Comparator.comparing((Path path) -> parseName(path).laneId)
              .thenComparingLong(path -> parseName(path).segmentSeq))
          .collect(Collectors.toList());
      if (!discovered.isEmpty()) {
        throw new IOException("State Archive Catalog CURRENT is missing");
      }
      return discovered;
    }
  }

  private void publishCatalog() throws IOException {
    catalog.publish(rotationTargetBytes, getCurrentSegments(), getSealedSegments());
    structuralChanged = false;
  }

  private boolean isCatalogSelectedCurrent(ParsedName name) {
    return catalog.isPublished() && catalog.selected().getCurrent().stream().anyMatch(segment ->
        segment.getLaneId() == name.laneId && segment.getSegmentSeq() == name.segmentSeq);
  }

  private void validateCatalogSelection() throws IOException {
    if (!catalog.isPublished()) {
      if (!lanes.isEmpty() || !sealedSegments.isEmpty()) {
        throw new IOException("State Archive Catalog selection is missing");
      }
      return;
    }
    List<CurrentSegment> expectedCurrent = catalog.selected().getCurrent();
    List<CurrentSegment> actualCurrent = getCurrentSegments();
    List<SealedSegment> expectedSealed = catalog.selected().getSealed();
    for (SealedSegment expected : expectedSealed) {
      SealedSegment actual = sealedSegments.stream().filter(candidate ->
          candidate.getLaneId() == expected.getLaneId()
              && candidate.getSegmentSeq() == expected.getSegmentSeq())
          .findFirst().orElse(null);
      if (actual == null || !Arrays.equals(StateArchiveSegmentFormatV3.encodeSealedMapRecord(
          expected), StateArchiveSegmentFormatV3.encodeSealedMapRecord(actual))) {
        throw new IOException("State Archive Catalog sealed segment identity mismatch");
      }
    }
    for (CurrentSegment expected : expectedCurrent) {
      CurrentSegment same = actualCurrent.stream().filter(actual ->
          actual.getLaneId() == expected.getLaneId()
              && actual.getSegmentSeq() == expected.getSegmentSeq()).findFirst().orElse(null);
      if (same != null) {
        if (same.getFirstBlock() != expected.getFirstBlock()
            || !Arrays.equals(same.getHeaderDigest(), expected.getHeaderDigest())) {
          throw new IOException("State Archive Catalog current segment identity mismatch");
        }
        continue;
      }
      SealedSegment promoted = sealedSegments.stream().filter(actual ->
          actual.getLaneId() == expected.getLaneId()
              && actual.getSegmentSeq() == expected.getSegmentSeq()).findFirst().orElse(null);
      if (promoted == null
          || !Arrays.equals(promoted.getSegmentHeaderDigest(), expected.getHeaderDigest())) {
        throw new IOException("State Archive Catalog selected current segment disappeared");
      }
      CurrentSegment successor = actualCurrent.stream().filter(actual ->
          actual.getLaneId() == expected.getLaneId()
              && actual.getSegmentSeq() == expected.getSegmentSeq() + 1)
          .findFirst().orElse(null);
      if (successor != null && successor.getFirstBlock() != promoted.getLastBlock() + 1) {
        throw new IOException("State Archive Catalog rotation successor is discontinuous");
      }
    }
    if (actualCurrent.size() == StateArchiveFileFormatV3.fiveLaneIds().length
        && sealedSegments.size() > expectedSealed.size()) {
      structuralChanged = true;
      publishCatalog();
    }
  }

  private boolean hasLaneIndexFile(int laneId) throws IOException {
    String prefix = String.format("lane-%04d-seg-", laneId);
    try (Stream<Path> paths = Files.walk(segmentRoot)) {
      return paths.filter(Files::isRegularFile)
          .map(path -> path.getFileName().toString())
          .anyMatch(name -> name.startsWith(prefix) && name.endsWith(".bidx"));
    }
  }

  private Path dataPath(int laneId, long sequence) {
    return segmentRoot.resolve(String.format("shard-%06d",
        sequence / StateArchiveFileFormatV3.SHARD_MAX_SEGMENTS))
        .resolve(String.format("lane-%04d-seg-%020d.dat", laneId, sequence));
  }

  private Path indexPath(int laneId, long sequence) {
    String name = dataPath(laneId, sequence).getFileName().toString();
    return dataPath(laneId, sequence).resolveSibling(
        name.substring(0, name.length() - 4) + ".bidx");
  }

  private Path manifestPath(int laneId, long sequence) {
    String name = dataPath(laneId, sequence).getFileName().toString();
    return dataPath(laneId, sequence).resolveSibling(
        name.substring(0, name.length() - 4) + ".manifest");
  }

  private void publishManifest(int laneId, long sequence, byte[] encoded) throws IOException {
    Path target = manifestPath(laneId, sequence);
    Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
    try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
      writeFully(channel, ByteBuffer.wrap(encoded));
      channel.force(true);
    }
    try {
      Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
    } catch (AtomicMoveNotSupportedException unsupported) {
      throw new IOException("State Archive manifest requires atomic publication", unsupported);
    }
    syncDirectory(target.getParent());
  }

  private static ParsedName parseName(Path path) {
    String name = path.getFileName().toString();
    if (!name.matches("lane-[0-9]{4}-seg-[0-9]{20}\\.dat")) {
      throw new IllegalArgumentException("Invalid State Archive segment filename: " + name);
    }
    try {
      return new ParsedName(Integer.parseInt(name.substring(5, 9)),
          Long.parseLong(name.substring(14, 34)));
    } catch (NumberFormatException failure) {
      throw new IllegalArgumentException("Invalid State Archive segment filename: " + name,
          failure);
    }
  }

  private static byte[] previousHistoryDigest(byte[] frame) {
    return Arrays.copyOfRange(frame, PREVIOUS_HISTORY_DIGEST_OFFSET,
        PREVIOUS_HISTORY_DIGEST_OFFSET + StateArchiveFileFormatV3.HASH_LENGTH);
  }

  private static long blockNumber(byte[] frame) {
    return ByteBuffer.wrap(frame).getLong(BLOCK_NUMBER_OFFSET);
  }

  private static byte[] encodedFrameDigest(byte[] frame) {
    return Arrays.copyOfRange(frame, frame.length - ENCODED_DIGEST_FROM_END,
        frame.length - ENCODED_DIGEST_FROM_END + StateArchiveFileFormatV3.HASH_LENGTH);
  }

  private static MessageDigest newContentDigest(byte[] header) {
    MessageDigest digest = sha256();
    digest.update(StateArchiveFileFormatV3.SEGMENT_CONTENT_DOMAIN);
    digest.update(header);
    return digest;
  }

  private static MessageDigest sha256() {
    try {
      return MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException impossible) {
      throw new IllegalStateException("SHA-256 is unavailable", impossible);
    }
  }

  private static byte[] readExact(FileChannel channel, long offset, int length)
      throws IOException {
    ByteBuffer bytes = ByteBuffer.allocate(length);
    channel.position(offset);
    while (bytes.hasRemaining()) {
      if (channel.read(bytes) < 0) {
        throw new IOException("Unexpected end of State Archive file");
      }
    }
    return bytes.array();
  }

  private static byte[] digestFilePrefix(byte[] domain, FileChannel channel, long length)
      throws IOException {
    if (length < 0 || length > channel.size()) {
      throw new IllegalArgumentException("Invalid State Archive digest prefix length");
    }
    MessageDigest digest = sha256();
    digest.update(domain);
    ByteBuffer bytes = ByteBuffer.allocateDirect(APPEND_BUFFER_BYTES);
    channel.position(0);
    long remaining = length;
    while (remaining > 0) {
      bytes.clear();
      bytes.limit((int) Math.min(bytes.capacity(), remaining));
      int read = channel.read(bytes);
      if (read < 0) {
        throw new IOException("Unexpected end of State Archive digest prefix");
      }
      bytes.flip();
      digest.update(bytes);
      remaining -= read;
    }
    return digest.digest();
  }

  private static void writeFully(FileChannel channel, ByteBuffer bytes) throws IOException {
    while (bytes.hasRemaining()) {
      channel.write(bytes);
    }
  }

  private static void writeBuffered(FileChannel channel, byte[] bytes, ByteBuffer staging)
      throws IOException {
    int offset = 0;
    while (offset < bytes.length) {
      staging.clear();
      int length = Math.min(staging.remaining(), bytes.length - offset);
      staging.put(bytes, offset, length);
      staging.flip();
      writeFully(channel, staging);
      offset += length;
    }
  }

  private static void syncDirectory(Path directory) throws IOException {
    try (FileChannel channel = FileChannel.open(directory, StandardOpenOption.READ)) {
      channel.force(true);
    }
  }

  private static void publishRecoveredManifest(Path target, byte[] encoded) throws IOException {
    Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
    try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
      writeFully(channel, ByteBuffer.wrap(encoded));
      channel.force(true);
    }
    try {
      Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
    } catch (AtomicMoveNotSupportedException unsupported) {
      throw new IOException("State Archive recovered manifest requires atomic move", unsupported);
    }
    syncDirectory(target.getParent());
  }

  private static byte[] requireHash(byte[] value, String name) {
    if (value == null || value.length != StateArchiveFileFormatV3.HASH_LENGTH) {
      throw new IllegalArgumentException("Invalid State Archive " + name + " length");
    }
    return Arrays.copyOf(value, value.length);
  }

  private static void requireCompression(short value) {
    if (value != StateArchiveFileFormatV3.COMPRESSION_NONE
        && value != StateArchiveFileFormatV3.COMPRESSION_RAW_DEFLATE_LEVEL_1) {
      throw new IllegalArgumentException("Unknown State Archive compression ID");
    }
  }

  private void requireUsable() {
    if (failed) {
      throw new IllegalStateException("State Archive writer requires reopen after append failure");
    }
  }

  @Override
  public synchronized void close() throws IOException {
    IOException failure = null;
    for (LaneState state : lanes.values()) {
      try {
        state.close();
      } catch (IOException closeFailure) {
        if (failure == null) {
          failure = closeFailure;
        } else {
          failure.addSuppressed(closeFailure);
        }
      }
    }
    lanes.clear();
    if (failure != null) {
      throw failure;
    }
  }

  private static final class ParsedName {
    private final int laneId;
    private final long segmentSeq;

    private ParsedName(int laneId, long segmentSeq) {
      StateArchiveFileFormatV3.laneKind(laneId);
      this.laneId = laneId;
      this.segmentSeq = segmentSeq;
    }
  }

  private static final class RecoveryRequest {
    private final RecoveryPoint authorizedCeiling;
    private final RecoveryPoint commonCommitted;
    private final long prototypeBoundary;
    private final boolean prototype;

    private RecoveryRequest(RecoveryPoint authorizedCeiling,
        RecoveryPoint commonCommitted) {
      this.authorizedCeiling = Objects.requireNonNull(authorizedCeiling,
          "authorizedCeiling");
      this.commonCommitted = commonCommitted;
      this.prototypeBoundary = authorizedCeiling.getBlockNumber();
      this.prototype = false;
    }

    private RecoveryRequest(long prototypeBoundary) {
      this.authorizedCeiling = null;
      this.commonCommitted = null;
      this.prototypeBoundary = prototypeBoundary;
      this.prototype = true;
    }

    private static RecoveryRequest prototype(long boundary) {
      return new RecoveryRequest(boundary);
    }

    private long authorizedBlock() {
      return prototypeBoundary;
    }
  }

  enum RecoveryStage {
    TEMPORARY_FORCED,
    INTENT_PUBLISHED,
    LANE_DATA_APPLIED,
    LANE_INDEX_APPLIED,
    TARGET_VERIFIED,
    INTENT_DELETED
  }

  @FunctionalInterface
  interface RecoveryFaultHook {
    RecoveryFaultHook NONE = (stage, laneId) -> { };

    void after(RecoveryStage stage, int laneId) throws IOException;
  }

  enum SyncStage {
    MARKER_WRITTEN,
    DATA_FORCED,
    MARKER_VERIFIED,
    PROOF_READY
  }

  @FunctionalInterface
  interface SyncFaultHook {
    SyncFaultHook NONE = (stage, laneId) -> { };

    void after(SyncStage stage, int laneId) throws IOException;
  }

  private static final class MarkerWrite {
    private final LaneState state;
    private final long offset;
    private final byte[] encoded;
    private final DurableMarker marker;

    private MarkerWrite(LaneState state, long offset, byte[] encoded,
        DurableMarker marker) {
      this.state = state;
      this.offset = offset;
      this.encoded = encoded;
      this.marker = marker;
    }
  }

  private static final class ScannedMarker {
    private final long offset;
    private final DurableMarker marker;

    private ScannedMarker(long offset, DurableMarker marker) {
      this.offset = offset;
      this.marker = marker;
    }
  }

  public static final class FileTailProof {
    private final int laneId;
    private final long segmentSeq;
    private final long markerOffset;
    private final int markerLength;
    private final long markerEndOffset;
    private final byte[] markerDigest;

    FileTailProof(int laneId, long segmentSeq, long markerOffset,
        int markerLength, long markerEndOffset, byte[] markerDigest) {
      StateArchiveFileFormatV3.laneKind(laneId);
      if (segmentSeq < 0 || markerOffset < StateArchiveFileFormatV3.PART_HEADER_LENGTH
          || markerLength != StateArchiveFileFormatV3.MARKER_HEADER_LENGTH
              + StateArchiveFileFormatV3.FRAME_TRAILER_LENGTH
          || markerEndOffset != markerOffset + markerLength) {
        throw new IllegalArgumentException("Invalid State Archive file tail proof");
      }
      this.laneId = laneId;
      this.segmentSeq = segmentSeq;
      this.markerOffset = markerOffset;
      this.markerLength = markerLength;
      this.markerEndOffset = markerEndOffset;
      this.markerDigest = requireHash(markerDigest, "marker digest");
    }

    public int getLaneId() {
      return laneId;
    }

    public long getSegmentSeq() {
      return segmentSeq;
    }

    public long getMarkerOffset() {
      return markerOffset;
    }

    public int getMarkerLength() {
      return markerLength;
    }

    public long getMarkerEndOffset() {
      return markerEndOffset;
    }

    public byte[] getMarkerDigest() {
      return Arrays.copyOf(markerDigest, markerDigest.length);
    }
  }

  public static final class ArchiveDurabilityProof {
    private final long checkpointSequence;
    private final RecoveryPoint target;
    private final byte[] commonTargetDigest;
    private final List<FileTailProof> fileTails;
    private final byte[] formatIdentity;
    private final byte[] descriptorDigest;

    ArchiveDurabilityProof(long checkpointSequence, RecoveryPoint target,
        byte[] commonTargetDigest, List<FileTailProof> fileTails) {
      if (checkpointSequence < 0) {
        throw new IllegalArgumentException("Invalid State Archive proof sequence");
      }
      this.checkpointSequence = checkpointSequence;
      this.target = Objects.requireNonNull(target, "target");
      this.commonTargetDigest = requireHash(commonTargetDigest, "proof Common target digest");
      this.fileTails = Collections.unmodifiableList(new ArrayList<>(fileTails));
      this.formatIdentity = StateArchiveFileFormatV3.compositeFormatDigest();
      this.descriptorDigest = StateArchiveFileFormatV3.fiveLaneDescriptorDigest();
      int[] laneIds = StateArchiveFileFormatV3.fiveLaneIds();
      if (this.fileTails.size() < laneIds.length) {
        throw new IllegalArgumentException("Incomplete State Archive durability proof");
      }
      int laneIndex = 0;
      long previousSegment = -1;
      for (FileTailProof tail : this.fileTails) {
        if (tail.getLaneId() != laneIds[laneIndex]) {
          if (laneIndex + 1 >= laneIds.length
              || tail.getLaneId() != laneIds[++laneIndex]) {
            throw new IllegalArgumentException("Non-canonical State Archive durability proof");
          }
          previousSegment = -1;
        }
        if (tail.getSegmentSeq() <= previousSegment) {
          throw new IllegalArgumentException("Non-canonical State Archive durability proof");
        }
        previousSegment = tail.getSegmentSeq();
      }
      if (laneIndex != laneIds.length - 1) {
        throw new IllegalArgumentException("Incomplete State Archive durability proof");
      }
    }

    public long getCheckpointSequence() {
      return checkpointSequence;
    }

    public RecoveryPoint getTarget() {
      return target;
    }

    public byte[] getCommonTargetDigest() {
      return Arrays.copyOf(commonTargetDigest, commonTargetDigest.length);
    }

    public List<FileTailProof> getFileTails() {
      return fileTails;
    }

    public byte[] getFormatIdentity() {
      return Arrays.copyOf(formatIdentity, formatIdentity.length);
    }

    public byte[] getDescriptorDigest() {
      return Arrays.copyOf(descriptorDigest, descriptorDigest.length);
    }
  }

  private static final class LaneState {
    private final int laneId;
    private final long segmentSeq;
    private final long firstBlock;
    private final byte[] headerDigest;
    private final byte[] startHistoryDigest;
    private final FileChannel data;
    private final FileChannel index;
    private final MessageDigest contentDigest;
    private final ByteBuffer appendBuffer = ByteBuffer.allocateDirect(APPEND_BUFFER_BYTES);
    private long lastBlock;
    private long blockFrameCount;
    private long entryCount;
    private long logicalPayloadBytes;
    private long encodedBlockFrameBytes;
    private long dataEndOffset = StateArchiveFileFormatV3.PART_HEADER_LENGTH;
    private byte[] firstFrameDigest;
    private byte[] lastFrameDigest;
    private byte[] endHistoryDigest;
    private BlockSnapshotMeta lastMeta;
    private long markedBlockFrameCount;
    private long markedLogicalBytes;
    private long markedEncodedBytes;
    private long lastMarkerEndOffset = StateArchiveFileFormatV3.PART_HEADER_LENGTH;
    private long lastMarkerOffset = -1;
    private byte[] previousMarkerDigest = new byte[StateArchiveFileFormatV3.HASH_LENGTH];
    private byte[] lastCommonTargetDigest;
    private long lastCheckpointSequence = -1;

    private LaneState(int laneId, long segmentSeq, long firstBlock, byte[] headerDigest,
        byte[] startHistoryDigest, FileChannel data, FileChannel index,
        MessageDigest contentDigest) {
      this.laneId = laneId;
      this.segmentSeq = segmentSeq;
      this.firstBlock = firstBlock;
      this.lastBlock = firstBlock - 1;
      this.headerDigest = headerDigest;
      this.startHistoryDigest = startHistoryDigest;
      this.data = data;
      this.index = index;
      this.contentDigest = contentDigest;
    }

    private void record(BlockSnapshotMeta meta, byte[] frame, byte[] digest) {
      long blockNumber = meta.getBlockNumber();
      if (blockFrameCount > 0 && blockNumber != lastBlock + 1) {
        throw new IllegalArgumentException("Non-contiguous State Archive lane block");
      }
      if (firstFrameDigest == null) {
        firstFrameDigest = Arrays.copyOf(digest, digest.length);
      }
      lastFrameDigest = Arrays.copyOf(digest, digest.length);
      lastBlock = blockNumber;
      blockFrameCount++;
      entryCount += ByteBuffer.wrap(frame).getLong(ENTRY_COUNT_OFFSET);
      logicalPayloadBytes += ByteBuffer.wrap(frame).getLong(RAW_PAYLOAD_LENGTH_OFFSET);
      encodedBlockFrameBytes += frame.length;
      dataEndOffset += frame.length;
      endHistoryDigest = Arrays.copyOfRange(frame, RESULT_HISTORY_DIGEST_OFFSET,
          RESULT_HISTORY_DIGEST_OFFSET + StateArchiveFileFormatV3.HASH_LENGTH);
      lastMeta = meta;
    }

    private CurrentSegment currentMap() {
      return new CurrentSegment(laneId, segmentSeq, firstBlock, lastBlock,
          dataEndOffset, blockFrameCount, headerDigest);
    }

    private void close() throws IOException {
      data.close();
      index.close();
    }
  }

  private static final class ScannedSegment {
    private final Path dataPath;
    private final FileChannel data;
    private final FileChannel index;
    private final Path indexPath;
    private final SegmentHeader header;
    private final boolean catalogSelectedCurrent;
    private final MessageDigest contentDigest;
    private final List<BlockIndexEntry> expectedIndex = new ArrayList<>();
    private long firstBlock = -1;
    private long lastBlock = -1;
    private long count;
    private long entryCount;
    private long logicalBytes;
    private long encodedBytes;
    private long dataEnd = StateArchiveFileFormatV3.PART_HEADER_LENGTH;
    private byte[] firstDigest;
    private byte[] lastDigest;
    private byte[] startHistory;
    private byte[] endHistory;
    private byte[] content;
    private SegmentSeal seal;
    private byte[] manifestDigest;
    private long markedCount;
    private long markedLogicalBytes;
    private long markedEncodedBytes;
    private long lastMarkerEndOffset = StateArchiveFileFormatV3.PART_HEADER_LENGTH;
    private long lastCheckpointSequence = -1;
    private byte[] previousMarkerDigest = new byte[StateArchiveFileFormatV3.HASH_LENGTH];
    private ScannedMarker lastMarker;
    private boolean tailDamaged;

    private ScannedSegment(Path dataPath, FileChannel data, FileChannel index,
        Path indexPath, SegmentHeader header, byte[] headerBytes,
        boolean catalogSelectedCurrent) {
      this.dataPath = dataPath;
      this.data = data;
      this.index = index;
      this.indexPath = indexPath;
      this.header = header;
      this.catalogSelectedCurrent = catalogSelectedCurrent;
      this.contentDigest = newContentDigest(headerBytes);
    }

    private void addBlock(long offset, byte[] frame) {
      if (ByteBuffer.wrap(frame).getShort(COMPRESSION_ID_OFFSET)
          != header.getCompressionId()) {
        throw new IllegalArgumentException("State Archive segment frame compression mismatch");
      }
      long number = blockNumber(frame);
      byte[] digest = encodedFrameDigest(frame);
      if (count == 0) {
        firstBlock = number;
        startHistory = previousHistoryDigest(frame);
        firstDigest = digest;
      } else if (number != lastBlock + 1) {
        throw new IllegalArgumentException("Non-contiguous State Archive segment block");
      }
      lastBlock = number;
      lastDigest = digest;
      endHistory = Arrays.copyOfRange(frame, RESULT_HISTORY_DIGEST_OFFSET,
          RESULT_HISTORY_DIGEST_OFFSET + StateArchiveFileFormatV3.HASH_LENGTH);
      count++;
      entryCount += ByteBuffer.wrap(frame).getLong(ENTRY_COUNT_OFFSET);
      logicalBytes += ByteBuffer.wrap(frame).getLong(RAW_PAYLOAD_LENGTH_OFFSET);
      encodedBytes += frame.length;
      dataEnd = offset + frame.length;
      contentDigest.update(frame);
      expectedIndex.add(new BlockIndexEntry(number, offset, frame.length,
          ByteBuffer.wrap(digest).getLong()));
    }

    private void addMarker(long offset, DurableMarker marker, byte[] frame) {
      long markerBlocks = count - markedCount;
      long firstMarkedBlock = markerBlocks == 0 ? -1 : lastBlock - markerBlocks + 1;
      if (marker.getLaneId() != header.getLaneId()
          || marker.getSegmentSeq() != header.getSegmentSeq()
          || marker.getCheckpointSequence() <= lastCheckpointSequence
          || markerBlocks <= 0 || marker.getBlockCount() != markerBlocks
          || marker.getFirstBlock() != firstMarkedBlock
          || marker.getLastBlock() != lastBlock
          || marker.getCoveredStartOffset() != lastMarkerEndOffset
          || marker.getMarkerEndOffset() != offset + frame.length
          || marker.getLogicalBytes() != logicalBytes - markedLogicalBytes
          || marker.getEncodedBytes() != encodedBytes - markedEncodedBytes
          || !Arrays.equals(marker.getResultHistoryDigest(), endHistory)
          || !Arrays.equals(marker.getPreviousMarkerDigest(), previousMarkerDigest)) {
        throw new IllegalArgumentException("State Archive durable marker range mismatch");
      }
      contentDigest.update(frame);
      dataEnd = offset + frame.length;
      markedCount = count;
      markedLogicalBytes = logicalBytes;
      markedEncodedBytes = encodedBytes;
      lastMarkerEndOffset = marker.getMarkerEndOffset();
      lastCheckpointSequence = marker.getCheckpointSequence();
      previousMarkerDigest = marker.getEncodedFrameDigest();
      lastMarker = new ScannedMarker(offset, marker);
    }

    private void setSeal(SegmentSeal value) {
      if (seal != null) {
        throw new IllegalArgumentException("Duplicate State Archive segment seal");
      }
      seal = value;
    }

    private void finish(boolean recovering) throws IOException {
      if (count == 0) {
        if (recovering && seal == null) {
          return;
        }
        throw new IllegalArgumentException("Empty State Archive segment");
      }
      if (header.getActualFirstBlock() != firstBlock
          || !Arrays.equals(header.getPreviousHistoryDigest(), startHistory)) {
        throw new IllegalArgumentException("State Archive segment first identity mismatch");
      }
      if (seal != null || !recovering) {
        validateIndex();
      }
      if (seal != null) {
        content = contentDigest.digest();
        if (seal.getLaneId() != header.getLaneId()
            || seal.getSegmentSeq() != header.getSegmentSeq()
            || seal.getActualFirstBlock() != firstBlock
            || seal.getActualLastBlock() != lastBlock
            || seal.getBlockFrameCount() != count || seal.getEntryCount() != entryCount
            || seal.getLogicalPayloadBytes() != logicalBytes
            || seal.getEncodedBlockFrameBytes() != encodedBytes
            || seal.getDataEndOffset() != dataEnd
            || seal.getPhysicalFileBytes() != data.size()
            || !Arrays.equals(seal.getFirstBlockFrameDigest(), firstDigest)
            || !Arrays.equals(seal.getLastBlockFrameDigest(), lastDigest)
            || !Arrays.equals(seal.getStartHistoryDigest(), startHistory)
            || !Arrays.equals(seal.getEndHistoryDigest(), endHistory)
            || !Arrays.equals(seal.getSegmentContentDigest(), content)) {
          throw new IllegalArgumentException("State Archive segment seal mismatch");
        }
        Path manifestPath = dataPath.resolveSibling(
            dataPath.getFileName().toString().replace(".dat", ".manifest"));
        if (!Files.isRegularFile(manifestPath)) {
          if (!catalogSelectedCurrent) {
            throw new IllegalArgumentException("State Archive sealed manifest is missing");
          }
          SegmentManifest recovered = new SegmentManifest(header.getLaneId(),
              header.getSegmentSeq(), firstBlock, lastBlock, count, entryCount, logicalBytes,
              encodedBytes, data.size(), StateArchiveFileFormatV3.BLOCK_INDEX_HEADER_LENGTH
                  + count * StateArchiveFileFormatV3.BLOCK_INDEX_ENTRY_LENGTH,
              header.getPreviousSegmentDigest(), endHistory);
          publishRecoveredManifest(manifestPath,
              StateArchiveSegmentFormatV3.encodeManifest(recovered));
        }
        SegmentManifest manifest = StateArchiveSegmentFormatV3.decodeManifest(
            Files.readAllBytes(manifestPath));
        long indexBytes = StateArchiveFileFormatV3.BLOCK_INDEX_HEADER_LENGTH
            + count * StateArchiveFileFormatV3.BLOCK_INDEX_ENTRY_LENGTH;
        if (manifest.getLaneId() != header.getLaneId()
            || manifest.getSegmentSeq() != header.getSegmentSeq()
            || manifest.getFirstBlock() != firstBlock || manifest.getLastBlock() != lastBlock
            || manifest.getBlockFrameCount() != count || manifest.getEntryCount() != entryCount
            || manifest.getLogicalPayloadBytes() != logicalBytes
            || manifest.getEncodedBlockFrameBytes() != encodedBytes
            || manifest.getDataFileBytes() != data.size()
            || manifest.getBlockIndexBytes() != indexBytes
            || !Arrays.equals(manifest.getPreviousSegmentDigest(),
                header.getPreviousSegmentDigest())
            || !Arrays.equals(manifest.getFinalHistoryDigest(), endHistory)) {
          throw new IllegalArgumentException("State Archive sealed manifest identity mismatch");
        }
        manifestDigest = manifest.getManifestDigest();
        data.close();
        index.close();
      }
    }

    private void validateIndex() throws IOException {
      long expectedLength = StateArchiveFileFormatV3.BLOCK_INDEX_HEADER_LENGTH
          + count * StateArchiveFileFormatV3.BLOCK_INDEX_ENTRY_LENGTH;
      if (index == null || index.size() != expectedLength) {
        throw new IllegalArgumentException("State Archive block index length mismatch");
      }
      for (int position = 0; position < expectedIndex.size(); position++) {
        BlockIndexEntry actual = StateArchiveSegmentFormatV3.decodeBlockIndexEntry(
            readExact(index, StateArchiveFileFormatV3.BLOCK_INDEX_HEADER_LENGTH
                + (long) position * StateArchiveFileFormatV3.BLOCK_INDEX_ENTRY_LENGTH,
                StateArchiveFileFormatV3.BLOCK_INDEX_ENTRY_LENGTH));
        BlockIndexEntry expected = expectedIndex.get(position);
        if (actual.getBlockNumber() != expected.getBlockNumber()
            || actual.getFrameOffset() != expected.getFrameOffset()
            || actual.getFrameLength() != expected.getFrameLength()
            || actual.getEncodedFrameDigestPrefix()
                != expected.getEncodedFrameDigestPrefix()) {
          throw new IllegalArgumentException("State Archive block index entry mismatch");
        }
      }
    }

    private LaneTarget laneTarget(long commonHead) throws IOException {
      int keepCount = retainedCount(commonHead);
      long originalDataEnd = data.size();
      long originalIndexEnd = index == null ? 0 : index.size();
      if (keepCount == 0) {
        return new LaneTarget(header.getLaneId(),
            StateArchiveFiveLaneRecoveryIntentV3.DELETE_PAIR
                | (index == null
                    ? StateArchiveFiveLaneRecoveryIntentV3.ORIGINAL_INDEX_MISSING : 0),
            header.getSegmentSeq(), originalDataEnd, originalIndexEnd,
            StateArchiveFiveLaneRecoveryIntentV3.NO_TARGET_SEGMENT, 0, 0,
            header.getHeaderDigest(), new byte[32], new byte[32]);
      }
      long targetDataEnd = expectedIndex.get(keepCount - 1).getFrameOffset()
          + expectedIndex.get(keepCount - 1).getFrameLength();
      byte[] targetIndex = targetIndexBytes(keepCount);
      int flags = targetDataEnd < originalDataEnd
          ? StateArchiveFiveLaneRecoveryIntentV3.DATA_TRUNCATE : 0;
      if (index == null) {
        flags |= StateArchiveFiveLaneRecoveryIntentV3.INDEX_REPLACE
            | StateArchiveFiveLaneRecoveryIntentV3.ORIGINAL_INDEX_MISSING;
      } else if (index.size() != targetIndex.length
          || !Arrays.equals(digestFilePrefix(
              StateArchiveFileFormatV3.RECOVERY_INDEX_FILE_DOMAIN, index, index.size()),
              StateArchiveFiveLaneRecoveryIntentV3.indexFileDigest(targetIndex))) {
        flags |= StateArchiveFiveLaneRecoveryIntentV3.INDEX_REPLACE;
      }
      return new LaneTarget(header.getLaneId(), flags, header.getSegmentSeq(),
          originalDataEnd, originalIndexEnd, header.getSegmentSeq(), targetDataEnd,
          targetIndex.length, header.getHeaderDigest(),
          digestFilePrefix(StateArchiveFileFormatV3.RECOVERY_DATA_PREFIX_DOMAIN,
              data, targetDataEnd),
          StateArchiveFiveLaneRecoveryIntentV3.indexFileDigest(targetIndex));
    }

    private int retainedCount(long commonHead) {
      int keepCount = 0;
      while (keepCount < expectedIndex.size()
          && expectedIndex.get(keepCount).getBlockNumber() <= commonHead) {
        keepCount++;
      }
      return keepCount;
    }

    private byte[] targetIndexBytes(int keepCount) {
      ByteBuffer bytes = ByteBuffer.allocate(StateArchiveFileFormatV3.BLOCK_INDEX_HEADER_LENGTH
          + keepCount * StateArchiveFileFormatV3.BLOCK_INDEX_ENTRY_LENGTH);
      bytes.put(StateArchiveSegmentFormatV3.encodeBlockIndexHeader(
          new BlockIndexHeader(header.getLaneId(), header.getSegmentSeq(),
              header.getHeaderDigest())));
      for (int position = 0; position < keepCount; position++) {
        bytes.put(StateArchiveSegmentFormatV3.encodeBlockIndexEntry(
            expectedIndex.get(position)));
      }
      return bytes.array();
    }

    private void repairTo(long commonHead, RecoveryFaultHook faultHook) throws IOException {
      int keepCount = retainedCount(commonHead);
      if (keepCount == 0) {
        close();
        Files.deleteIfExists(indexPath);
        faultHook.after(RecoveryStage.LANE_INDEX_APPLIED, header.getLaneId());
        Files.deleteIfExists(dataPath);
        syncDirectory(dataPath.getParent());
        faultHook.after(RecoveryStage.LANE_DATA_APPLIED, header.getLaneId());
        return;
      }
      long keepDataEnd = keepCount == 0 ? StateArchiveFileFormatV3.PART_HEADER_LENGTH
          : expectedIndex.get(keepCount - 1).getFrameOffset()
              + expectedIndex.get(keepCount - 1).getFrameLength();
      if (data.size() != keepDataEnd || tailDamaged) {
        data.truncate(keepDataEnd);
        data.force(false);
        faultHook.after(RecoveryStage.LANE_DATA_APPLIED, header.getLaneId());
      }
      boolean rewrite = index == null
          || index.size() != StateArchiveFileFormatV3.BLOCK_INDEX_HEADER_LENGTH
          + (long) keepCount * StateArchiveFileFormatV3.BLOCK_INDEX_ENTRY_LENGTH;
      if (!rewrite && index.size() >= StateArchiveFileFormatV3.BLOCK_INDEX_HEADER_LENGTH) {
        try {
          StateArchiveSegmentFormatV3.decodeBlockIndexHeader(readExact(index, 0,
              StateArchiveFileFormatV3.BLOCK_INDEX_HEADER_LENGTH));
          for (int position = 0; position < keepCount; position++) {
            BlockIndexEntry actual = StateArchiveSegmentFormatV3.decodeBlockIndexEntry(
                readExact(index, StateArchiveFileFormatV3.BLOCK_INDEX_HEADER_LENGTH
                    + (long) position * StateArchiveFileFormatV3.BLOCK_INDEX_ENTRY_LENGTH,
                    StateArchiveFileFormatV3.BLOCK_INDEX_ENTRY_LENGTH));
            BlockIndexEntry expected = expectedIndex.get(position);
            if (!sameEntry(actual, expected)) {
              rewrite = true;
              break;
            }
          }
        } catch (IllegalArgumentException invalidIndex) {
          rewrite = true;
        }
      }
      if (rewrite) {
        byte[] targetBytes = targetIndexBytes(keepCount);
        Path temporary = indexPath.resolveSibling(indexPath.getFileName() + ".recovery.tmp");
        try (FileChannel temporaryIndex = FileChannel.open(temporary,
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE)) {
          writeFully(temporaryIndex, ByteBuffer.wrap(targetBytes));
          temporaryIndex.force(true);
        }
        if (index != null && index.isOpen()) {
          index.close();
        }
        try {
          Files.move(temporary, indexPath, StandardCopyOption.ATOMIC_MOVE,
              StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException unsupported) {
          throw new IOException("State Archive index repair requires atomic move", unsupported);
        }
        syncDirectory(indexPath.getParent());
        faultHook.after(RecoveryStage.LANE_INDEX_APPLIED, header.getLaneId());
      }
    }

    private static boolean sameEntry(BlockIndexEntry left, BlockIndexEntry right) {
      return left.getBlockNumber() == right.getBlockNumber()
          && left.getFrameOffset() == right.getFrameOffset()
          && left.getFrameLength() == right.getFrameLength()
          && left.getEncodedFrameDigestPrefix() == right.getEncodedFrameDigestPrefix();
    }

    private void close() throws IOException {
      if (data.isOpen()) {
        data.close();
      }
      if (index != null && index.isOpen()) {
        index.close();
      }
    }

    private LaneState openState() {
      LaneState state = new LaneState(header.getLaneId(), header.getSegmentSeq(), firstBlock,
          header.getHeaderDigest(), startHistory, data, index, contentDigest);
      state.lastBlock = lastBlock;
      state.blockFrameCount = count;
      state.entryCount = entryCount;
      state.logicalPayloadBytes = logicalBytes;
      state.encodedBlockFrameBytes = encodedBytes;
      state.dataEndOffset = dataEnd;
      state.firstFrameDigest = firstDigest;
      state.lastFrameDigest = lastDigest;
      state.endHistoryDigest = endHistory;
      state.markedBlockFrameCount = markedCount;
      state.markedLogicalBytes = markedLogicalBytes;
      state.markedEncodedBytes = markedEncodedBytes;
      state.lastMarkerEndOffset = lastMarkerEndOffset;
      state.lastMarkerOffset = lastMarker == null ? -1 : lastMarker.offset;
      state.previousMarkerDigest = previousMarkerDigest;
      state.lastCheckpointSequence = lastCheckpointSequence;
      state.lastCommonTargetDigest = lastMarker == null ? null
          : lastMarker.marker.getCommonTargetDigest();
      return state;
    }

    private byte[] chainDigest() {
      return StateArchiveSegmentFormatV3.segmentChainDigest(header.getLaneId(),
          header.getSegmentSeq(), header.getHeaderDigest(), content,
          seal.getEncodedFrameDigest());
    }

    private SealedSegment sealedMap() {
      return new SealedSegment(header.getLaneId(), header.getSegmentSeq(), firstBlock,
          lastBlock, count, seal.getPhysicalFileBytes(),
          StateArchiveFileFormatV3.BLOCK_INDEX_HEADER_LENGTH
              + count * StateArchiveFileFormatV3.BLOCK_INDEX_ENTRY_LENGTH,
          header.getHeaderDigest(), content,
          seal.getEncodedFrameDigest(), manifestDigest);
    }
  }
}
