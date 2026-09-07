package org.tron.core.db2.stateroot;

import com.google.common.hash.Hashing;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.tron.core.db2.archive.BlockSnapshotMeta;
import org.tron.core.db2.core.CommonCheckpointBaseline;
import org.tron.core.db2.core.CommonCheckpointMaterializedStore;
import org.tron.core.db2.core.CommonCheckpointMaterializer;
import org.tron.core.db2.core.CommonCheckpointPayload;
import org.tron.core.db2.core.CommonCheckpointTarget;

/** Next-format PathState participant for the common-checkpoint two-barrier protocol. */
public final class PathStateCheckpointMaterializer implements CommonCheckpointMaterializer {

  public static final String CURRENT_FILE = "CURRENT";
  static final String LEGACY_BASELINE_FILE = "LEGACY_BASELINE";
  public static final String COMMON_MODE_FILE = "COMMON_CHECKPOINT_MODE";
  public static final String COMMON_BASELINE_HEAD_FILE = "COMMON_BASELINE_HEAD";
  static final String MATERIALIZED_DIRECTORY = "checkpoint-materialized";
  private static final int MAGIC = 0x50534354; // PSCT
  private static final short VERSION = 1;
  private static final int DIGEST_LENGTH = 32;
  private static final int RECORD_LENGTH = Integer.BYTES + Short.BYTES + Short.BYTES
      + 4 * DIGEST_LENGTH + 2 * Long.BYTES + DIGEST_LENGTH;

  private final PathStatePhysicalStoreSet stores;
  private final PathStateParticipantScope scope;
  private final Path directory;
  private final byte[] formatIdentity;
  private final FaultHook faultHook;
  private final CommonCheckpointBaseline baseline;
  private final CommonCheckpointMaterializedStore materializedStore;

  public PathStateCheckpointMaterializer(PathStatePhysicalStoreSet stores,
      PathStateParticipantScope scope, byte[] formatIdentity) {
    this(stores, scope, formatIdentity, null, null, (stage, storeId) -> { });
  }

  public PathStateCheckpointMaterializer(PathStatePhysicalStoreSet stores,
      PathStateParticipantScope scope, byte[] formatIdentity, CommonCheckpointBaseline baseline) {
    this(stores, scope, formatIdentity, baseline, null, (stage, storeId) -> { });
  }

  public PathStateCheckpointMaterializer(PathStatePhysicalStoreSet stores,
      PathStateParticipantScope scope, byte[] formatIdentity, CommonCheckpointBaseline baseline,
      CommonCheckpointMaterializedStore materializedStore) {
    this(stores, scope, formatIdentity, baseline, materializedStore, (stage, storeId) -> { });
  }

  PathStateCheckpointMaterializer(PathStatePhysicalStoreSet stores,
      PathStateParticipantScope scope, byte[] formatIdentity, FaultHook faultHook) {
    this(stores, scope, formatIdentity, null, null, faultHook);
  }

  private PathStateCheckpointMaterializer(PathStatePhysicalStoreSet stores,
      PathStateParticipantScope scope, byte[] formatIdentity, CommonCheckpointBaseline baseline,
      CommonCheckpointMaterializedStore materializedStore, FaultHook faultHook) {
    this.stores = Objects.requireNonNull(stores, "stores");
    this.scope = Objects.requireNonNull(scope, "scope");
    this.directory = stores.getDirectory();
    this.formatIdentity = digest(formatIdentity, "formatIdentity");
    this.faultHook = Objects.requireNonNull(faultHook, "faultHook");
    this.baseline = baseline;
    this.materializedStore = materializedStore;
  }

  @Override
  public Authority authority() {
    return Authority.PATH_STATE;
  }

  public static boolean isCommonModeAdmitted(Path directory, byte[] formatIdentity)
      throws IOException {
    Path mode = Objects.requireNonNull(directory, "directory").resolve(COMMON_MODE_FILE);
    if (!Files.exists(mode, LinkOption.NOFOLLOW_LINKS)) {
      return false;
    }
    if (!Files.isRegularFile(mode, LinkOption.NOFOLLOW_LINKS)
        || !Arrays.equals(Files.readAllBytes(mode),
        digest(formatIdentity, "formatIdentity"))) {
      throw new IOException("PathState common-mode marker differs");
    }
    return true;
  }

  /** Converts a newly rebuilt legacy pointer into a read-only baseline admission record. */
  public static void admitFreshBaseline(PathStatePhysicalStoreSet stores,
      PathStateRootMetadata metadata, CommonCheckpointBaseline baseline) throws IOException {
    Path directory = Objects.requireNonNull(stores, "stores").getDirectory();
    Path current = directory.resolve(CURRENT_FILE);
    Path legacy = directory.resolve(LEGACY_BASELINE_FILE);
    Path mode = directory.resolve(COMMON_MODE_FILE);
    if (Files.exists(mode, LinkOption.NOFOLLOW_LINKS)) {
      if (!Files.isRegularFile(mode, LinkOption.NOFOLLOW_LINKS)) {
        throw new IOException("PathState common-mode marker is not a regular file");
      }
      return;
    }
    if (!Files.isRegularFile(current, LinkOption.NOFOLLOW_LINKS)) {
      if (Files.isRegularFile(legacy, LinkOption.NOFOLLOW_LINKS)
          && Files.isRegularFile(directory.resolve(COMMON_BASELINE_HEAD_FILE),
          LinkOption.NOFOLLOW_LINKS)) {
        PathStateMetadataFile.publishImmutableBytes(mode, baseline.getFormatIdentity());
        return;
      }
      throw new IOException("PathState fresh common baseline requires one legacy CURRENT");
    }
    if (Files.exists(legacy, LinkOption.NOFOLLOW_LINKS)) {
      throw new IOException("PathState fresh common baseline has conflicting pointers");
    }
    if (metadata.getBlockNumber() != baseline.getHead().getBlockNumber()
        || !Arrays.equals(metadata.getBlockHash(), baseline.getHead().getBlockHash())
        || !Arrays.equals(metadata.getStateRoot(), baseline.getStateRoot())) {
      throw new IOException("PathState fresh baseline identity differs");
    }
    PathStateMetadataFile.publishImmutableBytes(directory.resolve(COMMON_BASELINE_HEAD_FILE),
        metadata.encode());
    try {
      Files.move(current, legacy, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
    } catch (java.nio.file.AtomicMoveNotSupportedException unsupported) {
      throw new IOException("PathState common baseline requires atomic rename", unsupported);
    }
    PathStateMetadataFile.publishImmutableBytes(mode, baseline.getFormatIdentity());
  }

  /** Loads the exact next-format head currently published by PathState CURRENT. */
  public static PublishedHead loadPublishedHead(Path directory,
      byte[] expectedFormatIdentity) throws IOException {
    Marker marker = load(Objects.requireNonNull(directory, "directory").resolve(CURRENT_FILE));
    if (!Arrays.equals(marker.formatIdentity,
        digest(expectedFormatIdentity, "expectedFormatIdentity"))) {
      throw new IOException("PathState published target format identity differs");
    }
    return new PublishedHead(marker.lastEpoch, marker.lastBlockNumber, marker.lastBlockHash,
        marker.stateRoot, marker.payloadDigest);
  }

  @Override
  public synchronized Status inspect(CommonCheckpointTarget target) throws IOException {
    CommonCheckpointTarget admitted = requireTarget(target);
    byte[] expected = encode(admitted);
    Path currentPath = directory.resolve(CURRENT_FILE);
    if (Files.exists(currentPath, LinkOption.NOFOLLOW_LINKS)) {
      Marker current = load(currentPath);
      if (Arrays.equals(current.encoded, expected)) {
        requireMaterialized(admitted, expected);
        return Status.PUBLISHED;
      }
      requireParent(current, admitted);
    } else if (baseline != null) {
      baseline.requireParent(admitted, "PathState");
    }
    if (!isMaterialized(admitted, expected)) {
      return Status.NEEDS_MATERIALIZATION;
    }
    return Status.MATERIALIZED;
  }

  @Override
  public synchronized void materialize(CommonCheckpointPayload payload,
      CommonCheckpointTarget target) throws IOException {
    CommonCheckpointPayload admittedPayload = Objects.requireNonNull(payload, "payload");
    CommonCheckpointTarget admittedTarget = requireTarget(target);
    if (!admittedTarget.equals(CommonCheckpointTarget.from(admittedPayload))) {
      throw new IOException("PathState checkpoint payload and target differ");
    }
    Status status = inspect(admittedTarget);
    if (status == Status.PUBLISHED || status == Status.MATERIALIZED) {
      return;
    }
    byte[] marker = marker(admittedTarget);
    Set<Integer> seen = new HashSet<>();
    for (CommonCheckpointPayload.PathStoreTarget pathStore
        : admittedPayload.getPathStores()) {
      PathStateParticipant participant = scope.require(pathStore.getDbName());
      if (participant.getStoreId() != pathStore.getStoreId()
          || !seen.add(pathStore.getStoreId())) {
        throw new IOException("PathState checkpoint participant identity differs");
      }
      PathStatePhysicalStoreSet.PhysicalStore store = stores.participant(pathStore.getDbName());
      if (!Arrays.equals(marker, store.checkpointTargetMarker())) {
        store.applyCheckpointParticipant(pathStore, marker);
        faultHook.after(Stage.AFTER_PARTICIPANT_BATCH, pathStore.getStoreId());
      }
    }
    PathStatePhysicalStoreSet.PhysicalStore superStore = stores.superStore();
    if (!Arrays.equals(marker, superStore.checkpointTargetMarker())) {
      superStore.applyCheckpointSuper(admittedPayload.getSuperNodeMutations(), marker);
      faultHook.after(Stage.AFTER_SUPER_BATCH, 0);
    }
    byte[] encoded = encode(admittedTarget);
    recordMaterialized(admittedTarget, encoded);
    faultHook.after(Stage.AFTER_MATERIALIZED_TARGET, 0);
  }

  @Override
  public synchronized void publish(CommonCheckpointTarget target) throws IOException {
    CommonCheckpointTarget admitted = requireTarget(target);
    Status status = inspect(admitted);
    if (status == Status.PUBLISHED) {
      return;
    }
    if (status != Status.MATERIALIZED) {
      throw new IOException("PathState checkpoint target is not fully materialized");
    }
    PathStateMetadataFile.replaceCurrentBytes(directory.resolve(CURRENT_FILE), encode(admitted));
    faultHook.after(Stage.AFTER_CURRENT, 0);
  }

  private CommonCheckpointTarget requireTarget(CommonCheckpointTarget target) throws IOException {
    CommonCheckpointTarget admitted = Objects.requireNonNull(target, "target");
    if (!Arrays.equals(formatIdentity, admitted.getFormatIdentity())) {
      throw new IOException("PathState checkpoint format identity differs");
    }
    return admitted;
  }

  private void requireMaterialized(CommonCheckpointTarget target, byte[] expected)
      throws IOException {
    if (!isMaterialized(target, expected)) {
      throw new IOException("PathState materialized checkpoint target is missing");
    }
  }

  private boolean isMaterialized(CommonCheckpointTarget target, byte[] expected)
      throws IOException {
    if (materializedStore != null && materializedStore.exists(Authority.PATH_STATE)) {
      return materializedStore.matches(Authority.PATH_STATE, expected);
    }
    Path legacy = materializedPath(target);
    if (!Files.exists(legacy, LinkOption.NOFOLLOW_LINKS)) {
      return false;
    }
    requireExact(legacy, expected);
    return true;
  }

  private void recordMaterialized(CommonCheckpointTarget target, byte[] encoded)
      throws IOException {
    if (materializedStore == null) {
      PathStateMetadataFile.publishImmutableBytes(materializedPath(target), encoded);
    } else {
      materializedStore.replace(Authority.PATH_STATE, encoded);
    }
  }

  private void requireParent(Marker current, CommonCheckpointTarget target) throws IOException {
    BlockSnapshotMeta first = target.getFirstBlock();
    if (!Arrays.equals(current.formatIdentity, target.getFormatIdentity())
        || current.lastEpoch + 1 != first.getEpoch()
        || current.lastBlockNumber + 1 != first.getBlockNumber()
        || !Arrays.equals(current.lastBlockHash, first.getParentHash())
        || !Arrays.equals(current.stateRoot, target.getParentStateRoot())) {
      throw new IOException("PathState CURRENT is not the checkpoint parent target");
    }
  }

  private Path materializedPath(CommonCheckpointTarget target) {
    return directory.resolve(MATERIALIZED_DIRECTORY).resolve(hex(target.getPayloadDigest()));
  }

  private static byte[] marker(CommonCheckpointTarget target) {
    byte[] marker = new byte[2 * DIGEST_LENGTH];
    System.arraycopy(target.getPayloadDigest(), 0, marker, 0, DIGEST_LENGTH);
    System.arraycopy(target.getStateRoot(), 0, marker, DIGEST_LENGTH, DIGEST_LENGTH);
    return marker;
  }

  private static byte[] encode(CommonCheckpointTarget target) {
    try {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream(RECORD_LENGTH);
      DataOutputStream output = new DataOutputStream(bytes);
      output.writeInt(MAGIC);
      output.writeShort(VERSION);
      output.writeShort(0);
      output.write(target.getFormatIdentity());
      output.write(target.getPayloadDigest());
      output.writeLong(target.getLastBlock().getEpoch());
      output.writeLong(target.getLastBlock().getBlockNumber());
      output.write(target.getLastBlock().getBlockHash());
      output.write(target.getStateRoot());
      output.flush();
      byte[] body = bytes.toByteArray();
      output.write(Hashing.sha256().hashBytes(body).asBytes());
      output.flush();
      return bytes.toByteArray();
    } catch (IOException impossible) {
      throw new IllegalStateException("in-memory PathState target encoding failed", impossible);
    }
  }

  private static Marker load(Path path) throws IOException {
    byte[] encoded = PathStateMetadataFile.loadImmutableBytes(path, RECORD_LENGTH);
    if (encoded.length != RECORD_LENGTH) {
      throw new IOException("PathState checkpoint target length is invalid");
    }
    int bodyLength = encoded.length - DIGEST_LENGTH;
    byte[] body = Arrays.copyOf(encoded, bodyLength);
    byte[] checksum = Arrays.copyOfRange(encoded, bodyLength, encoded.length);
    if (!Arrays.equals(checksum, Hashing.sha256().hashBytes(body).asBytes())) {
      throw new IOException("PathState checkpoint target checksum differs");
    }
    try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(body))) {
      if (input.readInt() != MAGIC || input.readShort() != VERSION || input.readShort() != 0) {
        throw new IOException("PathState checkpoint target format is unsupported");
      }
      return new Marker(encoded, readDigest(input), readDigest(input), input.readLong(),
          input.readLong(), readDigest(input), readDigest(input));
    } catch (EOFException truncated) {
      throw new IOException("PathState checkpoint target is truncated", truncated);
    }
  }

  private static void requireExact(Path path, byte[] expected) throws IOException {
    Marker actual = load(path);
    if (!Arrays.equals(actual.encoded, expected)) {
      throw new IOException("PathState checkpoint target identity differs");
    }
  }

  private static byte[] readDigest(DataInputStream input) throws IOException {
    byte[] value = new byte[DIGEST_LENGTH];
    input.readFully(value);
    return value;
  }

  private static byte[] digest(byte[] value, String name) {
    byte[] copy = Arrays.copyOf(Objects.requireNonNull(value, name), value.length);
    if (copy.length != DIGEST_LENGTH) {
      throw new IllegalArgumentException(name + " must contain exactly 32 bytes");
    }
    return copy;
  }

  private static String hex(byte[] value) {
    StringBuilder encoded = new StringBuilder(value.length * 2);
    for (byte current : value) {
      encoded.append(Character.forDigit(current >>> 4 & 0xf, 16));
      encoded.append(Character.forDigit(current & 0xf, 16));
    }
    return encoded.toString();
  }

  enum Stage {
    AFTER_PARTICIPANT_BATCH,
    AFTER_SUPER_BATCH,
    AFTER_MATERIALIZED_TARGET,
    AFTER_CURRENT
  }

  @FunctionalInterface
  interface FaultHook {
    void after(Stage stage, int storeId) throws IOException;
  }

  private static final class Marker {

    private final byte[] encoded;
    private final byte[] formatIdentity;
    private final byte[] payloadDigest;
    private final long lastEpoch;
    private final long lastBlockNumber;
    private final byte[] lastBlockHash;
    private final byte[] stateRoot;

    private Marker(byte[] encoded, byte[] formatIdentity, byte[] payloadDigest, long lastEpoch,
        long lastBlockNumber, byte[] lastBlockHash, byte[] stateRoot) {
      this.encoded = encoded;
      this.formatIdentity = formatIdentity;
      this.payloadDigest = payloadDigest;
      this.lastEpoch = lastEpoch;
      this.lastBlockNumber = lastBlockNumber;
      this.lastBlockHash = lastBlockHash;
      this.stateRoot = stateRoot;
    }
  }

  /** Minimal restart identity retained by the compact next-format CURRENT record. */
  public static final class PublishedHead {

    private final long epoch;
    private final long blockNumber;
    private final byte[] blockHash;
    private final byte[] stateRoot;
    private final byte[] payloadDigest;

    private PublishedHead(long epoch, long blockNumber, byte[] blockHash, byte[] stateRoot,
        byte[] payloadDigest) {
      this.epoch = epoch;
      this.blockNumber = blockNumber;
      this.blockHash = Arrays.copyOf(blockHash, blockHash.length);
      this.stateRoot = Arrays.copyOf(stateRoot, stateRoot.length);
      this.payloadDigest = Arrays.copyOf(payloadDigest, payloadDigest.length);
    }

    public long getEpoch() {
      return epoch;
    }

    public long getBlockNumber() {
      return blockNumber;
    }

    public byte[] getBlockHash() {
      return Arrays.copyOf(blockHash, blockHash.length);
    }

    public byte[] getStateRoot() {
      return Arrays.copyOf(stateRoot, stateRoot.length);
    }

    public byte[] getPayloadDigest() {
      return Arrays.copyOf(payloadDigest, payloadDigest.length);
    }
  }
}
