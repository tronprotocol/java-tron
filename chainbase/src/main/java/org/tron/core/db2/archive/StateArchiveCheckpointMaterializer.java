package org.tron.core.db2.archive;

import com.google.common.hash.Hashing;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.tron.core.db2.core.CommonCheckpointBaseline;
import org.tron.core.db2.core.CommonCheckpointMaterializedStore;
import org.tron.core.db2.core.CommonCheckpointMaterializer;
import org.tron.core.db2.core.CommonCheckpointPayload;
import org.tron.core.db2.core.CommonCheckpointTarget;
import org.tron.core.db2.stateroot.PathStateStoreManifest.Engine;

/** Next-format State Archive participant for the common-checkpoint two-barrier protocol. */
public final class StateArchiveCheckpointMaterializer implements CommonCheckpointMaterializer {

  static final String READABLE_FILE = "READABLE";
  static final String TARGET_DIRECTORY = "checkpoint-targets";
  static final String BLOCK_DIRECTORY = "blocks";
  static final String MATERIALIZED_FILE = "MATERIALIZED";

  private static final int TARGET_MAGIC = 0x53414354; // SACT
  private static final short TARGET_VERSION = 2;
  private static final int BLOCK_MAGIC = 0x53414342; // SACB
  private static final short BLOCK_VERSION = 2;
  private static final int DIGEST_LENGTH = 32;
  private static final int META_LENGTH = 3 * Long.BYTES + 2 * DIGEST_LENGTH;
  private static final int TARGET_LENGTH = Integer.BYTES + 2 * Short.BYTES
      + 4 * DIGEST_LENGTH + 2 * META_LENGTH + DIGEST_LENGTH;
  private static final int BLOCK_FIXED_LENGTH = Integer.BYTES + 2 * Short.BYTES
      + Integer.BYTES + DIGEST_LENGTH;
  private static final long MAX_BLOCK_LENGTH = BlockHistoryCodec.DEFAULT_MAX_RECORD_LENGTH
      + (long) BLOCK_FIXED_LENGTH;

  private final Path directory;
  private final byte[] formatIdentity;
  private final BlockHistoryCodec historyCodec = new BlockHistoryCodec();
  private final FaultHook faultHook;
  private final CommonCheckpointBaseline baseline;
  private final Engine engine;
  private final CommonCheckpointMaterializedStore materializedStore;
  private StateArchiveCheckpointServingIndex.Session checkpointServingIndex;
  private CommonCheckpointTarget activeCheckpoint;
  private boolean closed;

  public StateArchiveCheckpointMaterializer(Path directory, byte[] formatIdentity) {
    this(directory, formatIdentity, null, StateArchiveCheckpointServingIndex.configuredEngine(),
        null, (stage, blockIndex) -> { });
  }

  public StateArchiveCheckpointMaterializer(Path directory, byte[] formatIdentity,
      CommonCheckpointBaseline baseline) {
    this(directory, formatIdentity, baseline,
        StateArchiveCheckpointServingIndex.configuredEngine(), null, (stage, blockIndex) -> { });
  }

  public StateArchiveCheckpointMaterializer(Path directory, byte[] formatIdentity,
      CommonCheckpointBaseline baseline, Engine engine) {
    this(directory, formatIdentity, baseline, engine, null, (stage, blockIndex) -> { });
  }

  public StateArchiveCheckpointMaterializer(Path directory, byte[] formatIdentity,
      CommonCheckpointBaseline baseline, Engine engine,
      CommonCheckpointMaterializedStore materializedStore) {
    this(directory, formatIdentity, baseline, engine, materializedStore,
        (stage, blockIndex) -> { });
  }

  StateArchiveCheckpointMaterializer(Path directory, byte[] formatIdentity,
      FaultHook faultHook) {
    this(directory, formatIdentity, null, StateArchiveCheckpointServingIndex.configuredEngine(),
        null, faultHook);
  }

  StateArchiveCheckpointMaterializer(Path directory, byte[] formatIdentity, Engine engine,
      FaultHook faultHook) {
    this(directory, formatIdentity, null, engine, null, faultHook);
  }

  private StateArchiveCheckpointMaterializer(Path directory, byte[] formatIdentity,
      CommonCheckpointBaseline baseline, Engine engine,
      CommonCheckpointMaterializedStore materializedStore, FaultHook faultHook) {
    this.directory = Objects.requireNonNull(directory, "directory");
    this.formatIdentity = digest(formatIdentity, "formatIdentity");
    this.faultHook = Objects.requireNonNull(faultHook, "faultHook");
    this.baseline = baseline;
    this.engine = Objects.requireNonNull(engine, "engine");
    this.materializedStore = materializedStore;
  }

  @Override
  public Authority authority() {
    return Authority.STATE_ARCHIVE;
  }

  @Override
  public synchronized void beginCheckpoint(CommonCheckpointTarget target) throws IOException {
    CommonCheckpointTarget admitted = requireTarget(target);
    requireOpen();
    if (activeCheckpoint != null) {
      throw new IOException("State Archive checkpoint serving session is already open");
    }
    if (checkpointServingIndex == null) {
      checkpointServingIndex = StateArchiveCheckpointServingIndex.session(directory, engine);
    }
    activeCheckpoint = admitted;
  }

  @Override
  public synchronized void endCheckpoint(CommonCheckpointTarget target) throws IOException {
    CommonCheckpointTarget admitted = requireTarget(target);
    if (activeCheckpoint == null) {
      return;
    }
    if (!activeCheckpoint.equals(admitted)) {
      throw new IOException("State Archive checkpoint serving session target differs");
    }
    activeCheckpoint = null;
  }

  /** Releases the serving-index writer retained across checkpoint targets. */
  @Override
  public synchronized void close() throws IOException {
    if (closed) {
      return;
    }
    closed = true;
    activeCheckpoint = null;
    if (checkpointServingIndex != null) {
      try {
        checkpointServingIndex.close();
      } finally {
        checkpointServingIndex = null;
      }
    }
  }

  @Override
  public synchronized Status inspect(CommonCheckpointTarget target) throws IOException {
    requireOpen();
    CommonCheckpointTarget admitted = requireTarget(target);
    byte[] expected = encodeTarget(admitted);
    Path readable = directory.resolve(READABLE_FILE);
    if (Files.exists(readable, LinkOption.NOFOLLOW_LINKS)) {
      TargetMarker current = loadTarget(readable);
      if (Arrays.equals(current.encoded, expected)) {
        requireMaterialized(admitted, expected);
        requireServingIndex(admitted);
        return Status.PUBLISHED;
      }
      requireParent(current, admitted);
    } else if (baseline != null) {
      baseline.requireParent(admitted, "State Archive");
    }
    if (!isMaterialized(admitted, expected)) {
      return Status.NEEDS_MATERIALIZATION;
    }
    requireServingIndex(admitted);
    return Status.MATERIALIZED;
  }

  /** Loads and fully validates the target currently published by Archive READABLE. */
  public static CommonCheckpointTarget loadPublishedTarget(Path directory,
      byte[] expectedFormatIdentity) throws IOException {
    return loadPublishedTarget(directory, expectedFormatIdentity,
        StateArchiveCheckpointServingIndex.configuredEngine());
  }

  public static CommonCheckpointTarget loadPublishedTarget(Path directory,
      byte[] expectedFormatIdentity, Engine engine) throws IOException {
    return loadPublishedTarget(directory, expectedFormatIdentity, engine, null);
  }

  public static CommonCheckpointTarget loadPublishedTarget(Path directory,
      byte[] expectedFormatIdentity, Engine engine,
      CommonCheckpointMaterializedStore materializedStore) throws IOException {
    StateArchiveCheckpointMaterializer materializer =
        new StateArchiveCheckpointMaterializer(directory, expectedFormatIdentity, null, engine,
            materializedStore);
    Path readable = directory.resolve(READABLE_FILE);
    if (!Files.exists(readable, LinkOption.NOFOLLOW_LINKS)) {
      throw new IOException("State Archive READABLE target is missing");
    }
    CommonCheckpointTarget target = loadTarget(readable).target;
    materializer.requireTarget(target);
    if (materializer.inspect(target) != Status.PUBLISHED) {
      throw new IOException("State Archive READABLE target is not fully published");
    }
    return target;
  }

  /** Returns the published target when present, validating its complete serving boundary once. */
  public static Optional<CommonCheckpointTarget> loadPublishedTargetIfPresent(Path directory,
      byte[] expectedFormatIdentity, Engine engine) throws IOException {
    return loadPublishedTargetIfPresent(directory, expectedFormatIdentity, engine, null);
  }

  public static Optional<CommonCheckpointTarget> loadPublishedTargetIfPresent(Path directory,
      byte[] expectedFormatIdentity, Engine engine,
      CommonCheckpointMaterializedStore materializedStore) throws IOException {
    if (!Files.exists(directory.resolve(READABLE_FILE), LinkOption.NOFOLLOW_LINKS)) {
      return Optional.empty();
    }
    return Optional.of(loadPublishedTarget(directory, expectedFormatIdentity, engine,
        materializedStore));
  }

  /** Resolves the configured Archive index engine at runtime construction boundaries. */
  public static Engine configuredEngine() {
    return StateArchiveCheckpointServingIndex.configuredEngine();
  }

  @Override
  public synchronized void materialize(CommonCheckpointPayload payload,
      CommonCheckpointTarget target) throws IOException {
    requireOpen();
    CommonCheckpointPayload admittedPayload = Objects.requireNonNull(payload, "payload");
    CommonCheckpointTarget admittedTarget = requireTarget(target);
    if (!admittedTarget.equals(CommonCheckpointTarget.from(admittedPayload))) {
      throw new IOException("State Archive checkpoint payload and target differ");
    }
    Status status = inspect(admittedTarget);
    if (status != Status.NEEDS_MATERIALIZATION) {
      return;
    }

    Path blocks = blocksPath(admittedTarget);
    createDirectory(blocks);
    Set<String> expectedNames = new HashSet<>();
    for (int index = 0; index < admittedPayload.getBlocks().size(); index++) {
      CommonCheckpointPayload.BlockPayload block = admittedPayload.getBlocks().get(index);
      String name = blockFileName(index, block.getMeta());
      expectedNames.add(name);
      byte[] encoded = encodeBlock(block);
      publishImmutable(blocks.resolve(name), encoded);
      faultHook.after(Stage.AFTER_BLOCK_FILE, index);
    }
    requireExactBlockSet(blocks, expectedNames);
    if (checkpointServingIndex == null) {
      StateArchiveCheckpointServingIndex.apply(directory, admittedPayload, admittedTarget, engine);
    } else {
      checkpointServingIndex.apply(admittedPayload, admittedTarget);
    }
    faultHook.after(Stage.AFTER_SERVING_INDEX_BATCH, -1);
    recordMaterialized(admittedTarget, encodeTarget(admittedTarget));
    faultHook.after(Stage.AFTER_MATERIALIZED_TARGET, -1);
  }

  @Override
  public synchronized void publish(CommonCheckpointTarget target) throws IOException {
    requireOpen();
    CommonCheckpointTarget admitted = requireTarget(target);
    Status status = inspect(admitted);
    if (status == Status.PUBLISHED) {
      return;
    }
    if (status != Status.MATERIALIZED) {
      throw new IOException("State Archive checkpoint target is not fully materialized");
    }
    replace(directory.resolve(READABLE_FILE), encodeTarget(admitted));
    faultHook.after(Stage.AFTER_READABLE, -1);
  }

  BlockReverseDiff loadBlock(CommonCheckpointTarget target, int index) throws IOException {
    CommonCheckpointTarget admitted = requireTarget(target);
    Path blocks = blocksPath(admitted);
    String prefix = String.format("%08d-", index);
    Path found = null;
    try (DirectoryStream<Path> paths = Files.newDirectoryStream(blocks, prefix + "*.diff")) {
      for (Path path : paths) {
        if (found != null) {
          throw new IOException("State Archive checkpoint block index is ambiguous");
        }
        found = path;
      }
    }
    if (found == null) {
      throw new IOException("State Archive checkpoint block is missing");
    }
    return loadCheckpointBlock(found);
  }

  private CommonCheckpointTarget requireTarget(CommonCheckpointTarget target) throws IOException {
    CommonCheckpointTarget admitted = Objects.requireNonNull(target, "target");
    if (!Arrays.equals(formatIdentity, admitted.getFormatIdentity())) {
      throw new IOException("State Archive checkpoint format identity differs");
    }
    return admitted;
  }

  private void requireOpen() throws IOException {
    if (closed) {
      throw new IOException("State Archive checkpoint materializer is closed");
    }
  }

  private void requireParent(TargetMarker current, CommonCheckpointTarget target)
      throws IOException {
    BlockSnapshotMeta first = target.getFirstBlock();
    CommonCheckpointTarget published = current.target;
    if (!Arrays.equals(published.getFormatIdentity(), target.getFormatIdentity())
        || published.getLastBlock().getEpoch() + 1 != first.getEpoch()
        || published.getLastBlock().getBlockNumber() + 1 != first.getBlockNumber()
        || !Arrays.equals(published.getLastBlock().getBlockHash(), first.getParentHash())
        || !Arrays.equals(published.getStateRoot(), target.getParentStateRoot())) {
      throw new IOException("State Archive READABLE is not the checkpoint parent target");
    }
  }

  private void requireServingIndex(CommonCheckpointTarget target) throws IOException {
    StateArchiveCheckpointServingIndex.Status status = checkpointServingIndex == null
        ? StateArchiveCheckpointServingIndex.inspect(directory, target, engine)
        : checkpointServingIndex.inspect(target);
    if (status != StateArchiveCheckpointServingIndex.Status.EXACT) {
      throw new IOException("State Archive checkpoint serving index target differs");
    }
  }

  private Path targetPath(CommonCheckpointTarget target) {
    return directory.resolve(TARGET_DIRECTORY).resolve(hex(target.getPayloadDigest()));
  }

  private Path blocksPath(CommonCheckpointTarget target) {
    return targetPath(target).resolve(BLOCK_DIRECTORY);
  }

  private Path materializedPath(CommonCheckpointTarget target) {
    return targetPath(target).resolve(MATERIALIZED_FILE);
  }

  private boolean isMaterialized(CommonCheckpointTarget target, byte[] expected)
      throws IOException {
    if (materializedStore != null && materializedStore.exists(Authority.STATE_ARCHIVE)) {
      return materializedStore.matches(Authority.STATE_ARCHIVE, expected);
    }
    Path legacy = materializedPath(target);
    if (!Files.exists(legacy, LinkOption.NOFOLLOW_LINKS)) {
      return false;
    }
    requireExact(legacy, expected);
    return true;
  }

  private void requireMaterialized(CommonCheckpointTarget target, byte[] expected)
      throws IOException {
    if (!isMaterialized(target, expected)) {
      throw new IOException("State Archive materialized checkpoint target is missing");
    }
  }

  private void recordMaterialized(CommonCheckpointTarget target, byte[] encoded)
      throws IOException {
    if (materializedStore == null) {
      publishImmutable(materializedPath(target), encoded);
    } else {
      materializedStore.replace(Authority.STATE_ARCHIVE, encoded);
    }
  }

  private byte[] encodeBlock(CommonCheckpointPayload.BlockPayload block) {
    try {
      byte[] history = historyCodec.encode(block.getArchiveDiff());
      ByteArrayOutputStream bytes = new ByteArrayOutputStream(BLOCK_FIXED_LENGTH + history.length);
      DataOutputStream output = new DataOutputStream(bytes);
      output.writeInt(BLOCK_MAGIC);
      output.writeShort(BLOCK_VERSION);
      output.writeShort(0);
      output.writeInt(history.length);
      output.write(history);
      output.flush();
      byte[] body = bytes.toByteArray();
      output.write(Hashing.sha256().hashBytes(body).asBytes());
      output.flush();
      return bytes.toByteArray();
    } catch (IOException impossible) {
      throw new IllegalStateException("in-memory State Archive block encoding failed", impossible);
    }
  }

  static BlockReverseDiff loadCheckpointBlock(Path path) throws IOException {
    return decodeBlock(readBounded(path, MAX_BLOCK_LENGTH));
  }

  private static BlockReverseDiff decodeBlock(byte[] encoded) throws IOException {
    if (encoded.length < BLOCK_FIXED_LENGTH) {
      throw new IOException("State Archive checkpoint block is truncated");
    }
    int bodyLength = encoded.length - DIGEST_LENGTH;
    byte[] body = Arrays.copyOf(encoded, bodyLength);
    byte[] checksum = Arrays.copyOfRange(encoded, bodyLength, encoded.length);
    if (!Arrays.equals(checksum, Hashing.sha256().hashBytes(body).asBytes())) {
      throw new IOException("State Archive checkpoint block checksum differs");
    }
    try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(body))) {
      if (input.readInt() != BLOCK_MAGIC || input.readShort() != BLOCK_VERSION
          || input.readShort() != 0) {
        throw new IOException("State Archive checkpoint block format is unsupported");
      }
      int historyLength = input.readInt();
      if (historyLength <= 0 || historyLength != input.available()) {
        throw new IOException("State Archive checkpoint history length is invalid");
      }
      byte[] history = new byte[historyLength];
      input.readFully(history);
      BlockReverseDiff decoded;
      try {
        decoded = new BlockHistoryCodec().decode(history);
      } catch (IllegalArgumentException invalid) {
        throw new IOException("State Archive checkpoint history is corrupt", invalid);
      }
      return decoded;
    } catch (EOFException truncated) {
      throw new IOException("State Archive checkpoint block is truncated", truncated);
    }
  }

  static String blockFileName(int index, BlockSnapshotMeta meta) {
    return String.format("%08d-%020d-%s.diff", index, meta.getEpoch(), hex(meta.getBlockHash()));
  }

  private static byte[] encodeTarget(CommonCheckpointTarget target) {
    try {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream(TARGET_LENGTH);
      DataOutputStream output = new DataOutputStream(bytes);
      output.writeInt(TARGET_MAGIC);
      output.writeShort(TARGET_VERSION);
      output.writeShort(0);
      output.write(target.getFormatIdentity());
      output.write(target.getPayloadDigest());
      writeMeta(output, target.getFirstBlock());
      writeMeta(output, target.getLastBlock());
      output.write(target.getParentStateRoot());
      output.write(target.getStateRoot());
      output.flush();
      byte[] body = bytes.toByteArray();
      output.write(Hashing.sha256().hashBytes(body).asBytes());
      output.flush();
      return bytes.toByteArray();
    } catch (IOException impossible) {
      throw new IllegalStateException("in-memory State Archive target encoding failed", impossible);
    }
  }

  private static TargetMarker loadTarget(Path path) throws IOException {
    byte[] encoded = readBounded(path, TARGET_LENGTH);
    if (encoded.length != TARGET_LENGTH) {
      throw new IOException("State Archive checkpoint target length is invalid");
    }
    int bodyLength = encoded.length - DIGEST_LENGTH;
    byte[] body = Arrays.copyOf(encoded, bodyLength);
    byte[] checksum = Arrays.copyOfRange(encoded, bodyLength, encoded.length);
    if (!Arrays.equals(checksum, Hashing.sha256().hashBytes(body).asBytes())) {
      throw new IOException("State Archive checkpoint target checksum differs");
    }
    try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(body))) {
      if (input.readInt() != TARGET_MAGIC || input.readShort() != TARGET_VERSION
          || input.readShort() != 0) {
        throw new IOException("State Archive checkpoint target format is unsupported");
      }
      byte[] formatIdentity = readDigest(input);
      byte[] payloadDigest = readDigest(input);
      BlockSnapshotMeta first = readMeta(input);
      BlockSnapshotMeta last = readMeta(input);
      byte[] parentStateRoot = readDigest(input);
      byte[] stateRoot = readDigest(input);
      CommonCheckpointTarget target;
      try {
        target = CommonCheckpointTarget.restore(formatIdentity, payloadDigest, first, last,
            parentStateRoot, stateRoot);
      } catch (IllegalArgumentException invalid) {
        throw new IOException("State Archive checkpoint target identity is invalid", invalid);
      }
      return new TargetMarker(encoded, target);
    } catch (EOFException truncated) {
      throw new IOException("State Archive checkpoint target is truncated", truncated);
    }
  }

  private static void requireExact(Path path, byte[] expected) throws IOException {
    if (!Arrays.equals(loadTarget(path).encoded, expected)) {
      throw new IOException("State Archive checkpoint target identity differs");
    }
  }

  private static void requireExactBlockSet(Path directory, Set<String> expected)
      throws IOException {
    Set<String> actual = new HashSet<>();
    try (DirectoryStream<Path> paths = Files.newDirectoryStream(directory)) {
      for (Path path : paths) {
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
            || !actual.add(path.getFileName().toString())) {
          throw new IOException("State Archive checkpoint block directory is ambiguous");
        }
      }
    }
    if (!actual.equals(expected)) {
      throw new IOException("State Archive checkpoint block set differs");
    }
  }

  private static void createDirectory(Path path) throws IOException {
    Files.createDirectories(path);
    if (!Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
      throw new IOException("State Archive checkpoint path is not a directory");
    }
  }

  private static void publishImmutable(Path path, byte[] bytes) throws IOException {
    createDirectory(path.getParent());
    if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
      if (!Arrays.equals(readBounded(path, bytes.length), bytes)) {
        throw new IOException("State Archive immutable checkpoint file differs");
      }
      return;
    }
    Path temporary = path.resolveSibling(path.getFileName() + ".tmp-" + UUID.randomUUID());
    try {
      writeForced(temporary, bytes);
      try {
        Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE);
      } catch (AtomicMoveNotSupportedException unsupported) {
        throw new IOException("State Archive filesystem lacks atomic immutable publication",
            unsupported);
      } catch (java.nio.file.FileAlreadyExistsException raced) {
        if (!Arrays.equals(readBounded(path, bytes.length), bytes)) {
          throw new IOException("State Archive immutable checkpoint publication raced", raced);
        }
      }
      HistorySegmentStore.syncDirectory(path.getParent());
    } finally {
      Files.deleteIfExists(temporary);
    }
  }

  private static void replace(Path path, byte[] bytes) throws IOException {
    createDirectory(path.getParent());
    Path temporary = path.resolveSibling(path.getFileName() + ".tmp-" + UUID.randomUUID());
    try {
      writeForced(temporary, bytes);
      try {
        Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE,
            StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException unsupported) {
        throw new IOException("State Archive filesystem lacks atomic READABLE replacement",
            unsupported);
      }
      HistorySegmentStore.syncDirectory(path.getParent());
    } finally {
      Files.deleteIfExists(temporary);
    }
  }

  private static void writeForced(Path path, byte[] bytes) throws IOException {
    try (FileChannel channel = FileChannel.open(path, StandardOpenOption.CREATE_NEW,
        StandardOpenOption.WRITE)) {
      ByteBuffer buffer = ByteBuffer.wrap(bytes);
      while (buffer.hasRemaining()) {
        channel.write(buffer);
      }
      channel.force(true);
    }
  }

  private static byte[] readBounded(Path path, long maximum) throws IOException {
    if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
      throw new IOException("State Archive checkpoint path is not a regular file");
    }
    long size = Files.size(path);
    if (size <= 0 || size > maximum) {
      throw new IOException("State Archive checkpoint file length is invalid");
    }
    return Files.readAllBytes(path);
  }

  private static byte[] readDigest(DataInputStream input) throws IOException {
    byte[] value = new byte[DIGEST_LENGTH];
    input.readFully(value);
    return value;
  }

  private static void writeMeta(DataOutputStream output, BlockSnapshotMeta meta)
      throws IOException {
    output.writeLong(meta.getEpoch());
    output.writeLong(meta.getBlockNumber());
    output.write(meta.getBlockHash());
    output.write(meta.getParentHash());
    output.writeLong(meta.getTimestamp());
  }

  private static BlockSnapshotMeta readMeta(DataInputStream input) throws IOException {
    return new BlockSnapshotMeta(input.readLong(), input.readLong(), readDigest(input),
        readDigest(input), input.readLong());
  }

  private static byte[] digest(byte[] value, String name) {
    byte[] copy = Arrays.copyOf(Objects.requireNonNull(value, name), value.length);
    if (copy.length != DIGEST_LENGTH) {
      throw new IllegalArgumentException(name + " must contain exactly 32 bytes");
    }
    return copy;
  }

  static String hex(byte[] value) {
    StringBuilder encoded = new StringBuilder(value.length * 2);
    for (byte current : value) {
      encoded.append(Character.forDigit(current >>> 4 & 0xf, 16));
      encoded.append(Character.forDigit(current & 0xf, 16));
    }
    return encoded.toString();
  }

  enum Stage {
    AFTER_BLOCK_FILE,
    AFTER_SERVING_INDEX_BATCH,
    AFTER_MATERIALIZED_TARGET,
    AFTER_READABLE
  }

  @FunctionalInterface
  interface FaultHook {
    void after(Stage stage, int blockIndex) throws IOException;
  }

  private static final class TargetMarker {

    private final byte[] encoded;
    private final CommonCheckpointTarget target;

    private TargetMarker(byte[] encoded, CommonCheckpointTarget target) {
      this.encoded = encoded;
      this.target = target;
    }
  }
}
