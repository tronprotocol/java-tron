package org.tron.core.db2.archive;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Backend-neutral logical plan for advancing the exact-27 serving index over {@code (I,H]}.
 *
 * <p>The plan deliberately defines no persistent key, page, checksum, compression, or partition
 * encoding. A durable backend may apply it only after the corresponding format decision is
 * approved. Construction validates the complete suffix before exposing a target boundary, so a
 * rejected suffix cannot partially advance serving coverage.
 */
public final class ServingIndexIncrementalPlan {

  private final long indexedFrom;
  private final byte[] indexedFromHash;
  private final long indexedThrough;
  private final byte[] headHash;
  private final byte[] deltaSourceDigest;
  private final byte[] sourceSeedDigest;
  private final List<byte[]> sourceStepDigests;
  private final List<String> participatingDatabases;
  private final Map<String, List<KeyChange>> changesByDatabase;

  private ServingIndexIncrementalPlan(long indexedFrom, byte[] indexedFromHash,
      long indexedThrough, byte[] headHash, byte[] deltaSourceDigest,
      byte[] sourceSeedDigest, List<byte[]> sourceStepDigests,
      List<String> participatingDatabases,
      Map<String, List<KeyChange>> changesByDatabase) {
    this.indexedFrom = indexedFrom;
    this.indexedFromHash = Arrays.copyOf(indexedFromHash, indexedFromHash.length);
    this.indexedThrough = indexedThrough;
    this.headHash = Arrays.copyOf(headHash, headHash.length);
    this.deltaSourceDigest = Arrays.copyOf(deltaSourceDigest, deltaSourceDigest.length);
    this.sourceSeedDigest = Arrays.copyOf(sourceSeedDigest, sourceSeedDigest.length);
    List<byte[]> immutableSteps = new ArrayList<>(sourceStepDigests.size());
    sourceStepDigests.forEach(step -> immutableSteps.add(Arrays.copyOf(step, step.length)));
    this.sourceStepDigests = Collections.unmodifiableList(immutableSteps);
    this.participatingDatabases = participatingDatabases;
    this.changesByDatabase = changesByDatabase;
  }

  /** Validates and plans only the committed suffix after {@code indexedThrough}. */
  public static ServingIndexIncrementalPlan plan(long indexedThrough, byte[] headHash,
      List<String> participatingDatabases, List<HistoryCommitMarker> committedSuffix,
      ServingKeyIndexGeneration.AuthoritativeIndexReader reader) throws IOException {
    if (indexedThrough < 0) {
      throw new IllegalArgumentException("indexedThrough must not be negative");
    }
    requireHash(headHash, "headHash");
    List<String> participants = exactParticipants(participatingDatabases);
    Objects.requireNonNull(committedSuffix, "committedSuffix");
    Objects.requireNonNull(reader, "reader");

    Map<String, List<KeyChange>> changes = new LinkedHashMap<>();
    participants.forEach(database -> changes.put(database, new ArrayList<>()));
    MessageDigest seedDigest = sha256();
    updateLong(seedDigest, indexedThrough);
    seedDigest.update(headHash);
    updateParticipants(seedDigest, participants);
    byte[] sourceSeedDigest = seedDigest.digest();
    MessageDigest deltaDigest = sha256();
    deltaDigest.update(sourceSeedDigest);
    List<byte[]> sourceSteps = new ArrayList<>();

    long previousEpoch = indexedThrough;
    long previousBlock = indexedThrough;
    byte[] previousHash = Arrays.copyOf(headHash, headHash.length);
    for (HistoryCommitMarker marker : committedSuffix) {
      Objects.requireNonNull(marker, "committed marker");
      validateNext(marker, previousEpoch, previousBlock, previousHash, participants);
      HistoryIndexRecord record = reader.read(marker.getIndexLocation());
      validateRecord(marker, record, participants);
      collectChanges(changes, record);
      MessageDigest stepDigest = sha256();
      updateSourceDigest(stepDigest, marker);
      byte[] sourceStep = stepDigest.digest();
      sourceSteps.add(sourceStep);
      deltaDigest.update(sourceStep);
      previousEpoch = marker.getMeta().getEpoch();
      previousBlock = marker.getMeta().getBlockNumber();
      previousHash = marker.getMeta().getBlockHash();
    }

    Map<String, List<KeyChange>> immutableChanges = new LinkedHashMap<>();
    changes.forEach((database, databaseChanges) -> immutableChanges.put(database,
        Collections.unmodifiableList(new ArrayList<>(databaseChanges))));
    return new ServingIndexIncrementalPlan(indexedThrough, headHash, previousEpoch, previousHash,
        deltaDigest.digest(), sourceSeedDigest, sourceSteps, participants,
        Collections.unmodifiableMap(immutableChanges));
  }

  /** Plans an exact-27 increment directly from Common-committed captured reverse diffs. */
  static ServingIndexIncrementalPlan planCommittedDiffs(long indexedThrough, byte[] headHash,
      List<BlockReverseDiff> committedDiffs) {
    if (indexedThrough < 0) {
      throw new IllegalArgumentException("indexedThrough must not be negative");
    }
    requireHash(headHash, "headHash");
    List<String> participants = exactParticipants(
        new ArrayList<>(ArchiveStoreScope.getStateDatabases()));
    List<BlockReverseDiff> diffs = new ArrayList<>(Objects.requireNonNull(committedDiffs,
        "committedDiffs"));
    Map<String, List<KeyChange>> changes = new LinkedHashMap<>();
    participants.forEach(database -> changes.put(database, new ArrayList<>()));
    MessageDigest seed = sha256();
    updateLong(seed, indexedThrough);
    seed.update(headHash);
    updateParticipants(seed, participants);
    byte[] sourceSeed = seed.digest();
    MessageDigest delta = sha256();
    delta.update(sourceSeed);
    List<byte[]> steps = new ArrayList<>();
    long previousBlock = indexedThrough;
    byte[] previousHash = Arrays.copyOf(headHash, headHash.length);
    BlockHistoryCodec sourceCodec = new BlockHistoryCodec();
    for (BlockReverseDiff diff : diffs) {
      BlockSnapshotMeta meta = Objects.requireNonNull(diff, "committedDiff").getMeta();
      if (meta.getEpoch() != previousBlock + 1 || meta.getBlockNumber() != previousBlock + 1
          || !Arrays.equals(meta.getParentHash(), previousHash)) {
        throw new IllegalArgumentException("Serving committed diff suffix is not contiguous");
      }
      String previousDatabase = null;
      for (BlockReverseDiff.DbGroup group : diff.getGroups()) {
        List<KeyChange> database = changes.get(group.getDbName());
        if (database == null || previousDatabase != null
            && previousDatabase.compareTo(group.getDbName()) >= 0) {
          throw new IllegalArgumentException("Serving committed diff Store coverage is invalid");
        }
        byte[] previousKey = null;
        for (BlockReverseDiff.Entry entry : group.getEntries()) {
          byte[] key = entry.getKey();
          if (previousKey != null && BlockReverseDiff.compareUnsigned(previousKey, key) >= 0) {
            throw new IllegalArgumentException("Serving committed diff keys are not unique");
          }
          database.add(new KeyChange(key, meta.getEpoch()));
          previousKey = key;
        }
        previousDatabase = group.getDbName();
      }
      byte[] step = sha256().digest(sourceCodec.encode(diff));
      steps.add(step);
      delta.update(step);
      previousBlock = meta.getBlockNumber();
      previousHash = meta.getBlockHash();
    }
    Map<String, List<KeyChange>> immutable = new LinkedHashMap<>();
    changes.forEach((database, databaseChanges) -> immutable.put(database,
        Collections.unmodifiableList(new ArrayList<>(databaseChanges))));
    return new ServingIndexIncrementalPlan(indexedThrough, headHash, previousBlock,
        previousHash, delta.digest(), sourceSeed, steps, participants,
        Collections.unmodifiableMap(immutable));
  }

  public long getIndexedFrom() {
    return indexedFrom;
  }

  public long getIndexedThrough() {
    return indexedThrough;
  }

  public byte[] getIndexedFromHash() {
    return Arrays.copyOf(indexedFromHash, indexedFromHash.length);
  }

  public byte[] getHeadHash() {
    return Arrays.copyOf(headHash, headHash.length);
  }

  /** Identity of this validated suffix only; it is not a frozen persistent rolling digest. */
  public byte[] getDeltaSourceDigest() {
    return Arrays.copyOf(deltaSourceDigest, deltaSourceDigest.length);
  }

  /** Stable seed for a rebuild beginning at this plan's I/hash/exact-27 scope. */
  public byte[] getSourceSeedDigest() {
    return Arrays.copyOf(sourceSeedDigest, sourceSeedDigest.length);
  }

  /** Per-commit source steps make the rolling identity independent of flush batch boundaries. */
  public List<byte[]> getSourceStepDigests() {
    List<byte[]> copies = new ArrayList<>(sourceStepDigests.size());
    sourceStepDigests.forEach(step -> copies.add(Arrays.copyOf(step, step.length)));
    return Collections.unmodifiableList(copies);
  }

  public List<String> getParticipatingDatabases() {
    return participatingDatabases;
  }

  /** Returns an entry for every exact-27 Store, including Stores with no changes in this suffix. */
  public Map<String, List<KeyChange>> getChangesByDatabase() {
    return changesByDatabase;
  }

  public List<KeyChange> getChanges(String dbName) {
    List<KeyChange> changes = changesByDatabase.get(Objects.requireNonNull(dbName, "dbName"));
    if (changes == null) {
      throw new IllegalArgumentException("Database is outside exact-27 serving scope: " + dbName);
    }
    return changes;
  }

  private static void validateNext(HistoryCommitMarker marker, long previousEpoch,
      long previousBlock, byte[] previousHash, List<String> participants) {
    BlockSnapshotMeta meta = marker.getMeta();
    if (marker.getPreviousEpoch() != previousEpoch || meta.getEpoch() != previousEpoch + 1
        || meta.getBlockNumber() != previousBlock + 1
        || !Arrays.equals(meta.getParentHash(), previousHash)
        || !participants.equals(marker.getDatabases())) {
      throw new IllegalArgumentException("Serving index suffix is not contiguous exact-27 history");
    }
  }

  private static void validateRecord(HistoryCommitMarker marker, HistoryIndexRecord record,
      List<String> participants) {
    if (record == null || !marker.getMeta().equals(record.getMeta())
        || !same(marker.getHistoryLocation(), record.getHistoryLocation())) {
      throw new IllegalArgumentException(
          "Serving index suffix marker does not match authoritative history");
    }
    String previousDatabase = null;
    for (HistoryIndexRecord.KeyGroup group : record.getGroups()) {
      String database = group.getDbName();
      if (Collections.binarySearch(participants, database) < 0) {
        throw new IllegalArgumentException("Serving index suffix contains an unknown Store");
      }
      if (previousDatabase != null && previousDatabase.compareTo(database) >= 0) {
        throw new IllegalArgumentException("Serving index suffix Store groups are not sorted");
      }
      previousDatabase = database;
      byte[] previousKey = null;
      for (byte[] key : group.getKeys()) {
        if (previousKey != null && BlockReverseDiff.compareUnsigned(previousKey, key) >= 0) {
          throw new IllegalArgumentException("Serving index suffix keys are not unique and sorted");
        }
        previousKey = key;
      }
    }
  }

  private static void collectChanges(Map<String, List<KeyChange>> changes,
      HistoryIndexRecord record) {
    long epoch = record.getMeta().getEpoch();
    for (HistoryIndexRecord.KeyGroup group : record.getGroups()) {
      List<KeyChange> databaseChanges = changes.get(group.getDbName());
      for (byte[] key : group.getKeys()) {
        databaseChanges.add(new KeyChange(key, epoch));
      }
    }
  }

  private static List<String> exactParticipants(List<String> participatingDatabases) {
    List<String> participants = new ArrayList<>(Objects.requireNonNull(participatingDatabases,
        "participatingDatabases"));
    Collections.sort(participants);
    List<String> expected = new ArrayList<>(ArchiveStoreScope.getStateDatabases());
    Collections.sort(expected);
    if (!participants.equals(expected)) {
      throw new IllegalArgumentException("Serving index participant set must be exact-27");
    }
    return Collections.unmodifiableList(participants);
  }

  private static boolean same(HistoryLocation left, HistoryLocation right) {
    return left.getSegmentId() == right.getSegmentId()
        && left.getOffset() == right.getOffset()
        && left.getRecordLength() == right.getRecordLength()
        && left.getBodyChecksum() == right.getBodyChecksum()
        && Arrays.equals(left.getBodyDigest(), right.getBodyDigest());
  }

  private static void updateSourceDigest(MessageDigest digest, HistoryCommitMarker marker) {
    updateLong(digest, marker.getMeta().getEpoch());
    updateLong(digest, marker.getMeta().getBlockNumber());
    digest.update(marker.getMeta().getBlockHash());
    digest.update(marker.getMeta().getParentHash());
    updateLong(digest, marker.getIndexLocation().getOffset());
    updateLong(digest, marker.getIndexLocation().getRecordLength());
    digest.update(marker.getIndexLocation().getDigest());
    digest.update(marker.getHistoryLocation().getBodyDigest());
  }

  private static void updateParticipants(MessageDigest digest, List<String> participants) {
    updateLong(digest, participants.size());
    for (String participant : participants) {
      byte[] encoded = participant.getBytes(StandardCharsets.UTF_8);
      updateLong(digest, encoded.length);
      digest.update(encoded);
    }
  }

  private static void updateLong(MessageDigest digest, long value) {
    digest.update(ByteBuffer.allocate(Long.BYTES).putLong(value).array());
  }

  private static MessageDigest sha256() {
    try {
      return MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException impossible) {
      throw new IllegalStateException("SHA-256 is unavailable", impossible);
    }
  }

  private static void requireHash(byte[] hash, String name) {
    if (hash == null || hash.length != 32) {
      throw new IllegalArgumentException(name + " must be exactly 32 bytes");
    }
  }

  /** One exact physical key changed once at one committed epoch. */
  public static final class KeyChange {
    private final byte[] rawKey;
    private final long epoch;

    private KeyChange(byte[] rawKey, long epoch) {
      this.rawKey = Arrays.copyOf(rawKey, rawKey.length);
      this.epoch = epoch;
    }

    public byte[] getRawKey() {
      return Arrays.copyOf(rawKey, rawKey.length);
    }

    public long getEpoch() {
      return epoch;
    }
  }
}
