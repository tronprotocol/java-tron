package org.tron.core.db2.core;

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
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.tron.core.db2.archive.BlockSnapshotMeta;
import org.tron.core.db2.common.WrappedByteArray;

/** Chainbase participant for common-checkpoint idempotent materialization and publication. */
public final class ChainbaseCheckpointMaterializer implements CommonCheckpointMaterializer {

  static final String CURRENT_FILE = "CHAINBASE_CURRENT";
  static final String MATERIALIZED_DIRECTORY = "chainbase-checkpoint-materialized";
  private static final int MAGIC = 0x43424354; // CBCT
  private static final short VERSION = 1;
  private static final int DIGEST_LENGTH = 32;
  private static final int RECORD_LENGTH = Integer.BYTES + 2 * Short.BYTES
      + 4 * DIGEST_LENGTH + 2 * Long.BYTES + DIGEST_LENGTH;

  private final Path directory;
  private final byte[] formatIdentity;
  private final Map<String, Chainbase> databases;
  private final FaultHook faultHook;
  private final CommonCheckpointBaseline baseline;
  private final CommonCheckpointMaterializedStore materializedStore;

  public ChainbaseCheckpointMaterializer(Path directory, byte[] formatIdentity,
      List<Chainbase> databases) {
    this(directory, formatIdentity, databases, null, null, (stage, dbName) -> { });
  }

  public ChainbaseCheckpointMaterializer(Path directory, byte[] formatIdentity,
      List<Chainbase> databases, CommonCheckpointBaseline baseline) {
    this(directory, formatIdentity, databases, baseline, null, (stage, dbName) -> { });
  }

  public ChainbaseCheckpointMaterializer(Path directory, byte[] formatIdentity,
      List<Chainbase> databases, CommonCheckpointBaseline baseline,
      CommonCheckpointMaterializedStore materializedStore) {
    this(directory, formatIdentity, databases, baseline, materializedStore,
        (stage, dbName) -> { });
  }

  ChainbaseCheckpointMaterializer(Path directory, byte[] formatIdentity,
      List<Chainbase> databases, FaultHook faultHook) {
    this(directory, formatIdentity, databases, null, null, faultHook);
  }

  private ChainbaseCheckpointMaterializer(Path directory, byte[] formatIdentity,
      List<Chainbase> databases, CommonCheckpointBaseline baseline,
      CommonCheckpointMaterializedStore materializedStore, FaultHook faultHook) {
    this.directory = Objects.requireNonNull(directory, "directory");
    this.formatIdentity = digest(formatIdentity, "formatIdentity");
    this.databases = index(databases);
    this.faultHook = Objects.requireNonNull(faultHook, "faultHook");
    this.baseline = baseline;
    this.materializedStore = materializedStore;
  }

  @Override
  public Authority authority() {
    return Authority.CHAINBASE;
  }

  /** Loads the compact next-format head published beside the Chainbase databases. */
  public static PublishedHead loadPublishedHead(Path directory, byte[] expectedFormatIdentity)
      throws IOException {
    Marker marker = load(Objects.requireNonNull(directory, "directory").resolve(CURRENT_FILE));
    if (!Arrays.equals(marker.formatIdentity,
        digest(expectedFormatIdentity, "expectedFormatIdentity"))) {
      throw new IOException("Chainbase published target format identity differs");
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
      baseline.requireParent(admitted, "Chainbase");
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
      throw new IOException("Chainbase checkpoint payload and target differ");
    }
    Status status = inspect(admittedTarget);
    if (status != Status.NEEDS_MATERIALIZATION) {
      return;
    }
    for (CommonCheckpointPayload.StoreMutations store
        : admittedPayload.getChainbaseStores()) {
      Chainbase database = databases.get(store.getDbName());
      if (database == null) {
        throw new IOException("Chainbase checkpoint Store is not registered: "
            + store.getDbName());
      }
      Snapshot root = database.getHead().getRoot();
      if (!(root instanceof SnapshotRoot)) {
        throw new IOException("Chainbase checkpoint Store has no SnapshotRoot: "
            + store.getDbName());
      }
      ((SnapshotRoot) root).applyCheckpointMutations(batch(store));
      faultHook.after(Stage.AFTER_STORE_BATCH, store.getDbName());
    }
    recordMaterialized(admittedTarget, encode(admittedTarget));
    faultHook.after(Stage.AFTER_MATERIALIZED_TARGET, null);
  }

  @Override
  public synchronized void publish(CommonCheckpointTarget target) throws IOException {
    CommonCheckpointTarget admitted = requireTarget(target);
    Status status = inspect(admitted);
    if (status == Status.PUBLISHED) {
      return;
    }
    if (status != Status.MATERIALIZED) {
      throw new IOException("Chainbase checkpoint target is not fully materialized");
    }
    replace(directory.resolve(CURRENT_FILE), encode(admitted));
    faultHook.after(Stage.AFTER_CURRENT, null);
  }

  private CommonCheckpointTarget requireTarget(CommonCheckpointTarget target) throws IOException {
    CommonCheckpointTarget admitted = Objects.requireNonNull(target, "target");
    if (!Arrays.equals(formatIdentity, admitted.getFormatIdentity())) {
      throw new IOException("Chainbase checkpoint format identity differs");
    }
    return admitted;
  }

  private void requireParent(Marker current, CommonCheckpointTarget target) throws IOException {
    BlockSnapshotMeta first = target.getFirstBlock();
    if (!Arrays.equals(current.formatIdentity, target.getFormatIdentity())
        || current.lastEpoch + 1 != first.getEpoch()
        || current.lastBlockNumber + 1 != first.getBlockNumber()
        || !Arrays.equals(current.lastBlockHash, first.getParentHash())
        || !Arrays.equals(current.stateRoot, target.getParentStateRoot())) {
      throw new IOException("Chainbase CURRENT is not the checkpoint parent target");
    }
  }

  private Path materializedPath(CommonCheckpointTarget target) {
    return directory.resolve(MATERIALIZED_DIRECTORY).resolve(hex(target.getPayloadDigest()));
  }

  private boolean isMaterialized(CommonCheckpointTarget target, byte[] expected)
      throws IOException {
    if (materializedStore != null && materializedStore.exists(Authority.CHAINBASE)) {
      return materializedStore.matches(Authority.CHAINBASE, expected);
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
      throw new IOException("Chainbase materialized checkpoint target is missing");
    }
  }

  private void recordMaterialized(CommonCheckpointTarget target, byte[] encoded)
      throws IOException {
    if (materializedStore == null) {
      publishImmutable(materializedPath(target), encoded);
    } else {
      materializedStore.replace(Authority.CHAINBASE, encoded);
    }
  }

  private static Map<WrappedByteArray, WrappedByteArray> batch(
      CommonCheckpointPayload.StoreMutations store) {
    Map<WrappedByteArray, WrappedByteArray> batch = new LinkedHashMap<>();
    for (CommonCheckpointPayload.Mutation mutation : store.getMutations()) {
      batch.put(WrappedByteArray.of(mutation.getKey()),
          WrappedByteArray.of(mutation.getValue()));
    }
    return batch;
  }

  private static Map<String, Chainbase> index(List<Chainbase> supplied) {
    Map<String, Chainbase> indexed = new LinkedHashMap<>();
    for (Chainbase database : Objects.requireNonNull(supplied, "databases")) {
      Chainbase admitted = Objects.requireNonNull(database, "database");
      if (indexed.putIfAbsent(admitted.getDbName(), admitted) != null) {
        throw new IllegalArgumentException("duplicate Chainbase checkpoint Store: "
            + admitted.getDbName());
      }
    }
    return indexed;
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
      throw new IllegalStateException("in-memory Chainbase target encoding failed", impossible);
    }
  }

  private static Marker load(Path path) throws IOException {
    byte[] encoded = readBounded(path, RECORD_LENGTH);
    if (encoded.length != RECORD_LENGTH) {
      throw new IOException("Chainbase checkpoint target length is invalid");
    }
    int bodyLength = encoded.length - DIGEST_LENGTH;
    byte[] body = Arrays.copyOf(encoded, bodyLength);
    byte[] checksum = Arrays.copyOfRange(encoded, bodyLength, encoded.length);
    if (!Arrays.equals(checksum, Hashing.sha256().hashBytes(body).asBytes())) {
      throw new IOException("Chainbase checkpoint target checksum differs");
    }
    try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(body))) {
      if (input.readInt() != MAGIC || input.readShort() != VERSION || input.readShort() != 0) {
        throw new IOException("Chainbase checkpoint target format is unsupported");
      }
      return new Marker(encoded, readDigest(input), readDigest(input), input.readLong(),
          input.readLong(), readDigest(input), readDigest(input));
    } catch (EOFException truncated) {
      throw new IOException("Chainbase checkpoint target is truncated", truncated);
    }
  }

  private static void requireExact(Path path, byte[] expected) throws IOException {
    if (!Arrays.equals(load(path).encoded, expected)) {
      throw new IOException("Chainbase checkpoint target identity differs");
    }
  }

  private static void publishImmutable(Path path, byte[] bytes) throws IOException {
    createDirectory(path.getParent());
    if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
      if (!Arrays.equals(readBounded(path, bytes.length), bytes)) {
        throw new IOException("Chainbase immutable checkpoint target differs");
      }
      return;
    }
    Path temporary = path.resolveSibling(path.getFileName() + ".tmp-" + UUID.randomUUID());
    try {
      writeForced(temporary, bytes);
      try {
        Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE);
      } catch (AtomicMoveNotSupportedException unsupported) {
        throw new IOException("Chainbase filesystem lacks atomic target publication",
            unsupported);
      } catch (java.nio.file.FileAlreadyExistsException raced) {
        if (!Arrays.equals(readBounded(path, bytes.length), bytes)) {
          throw new IOException("Chainbase immutable target publication raced", raced);
        }
      }
      syncDirectory(path.getParent());
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
        throw new IOException("Chainbase filesystem lacks atomic CURRENT replacement",
            unsupported);
      }
      syncDirectory(path.getParent());
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
      throw new IOException("Chainbase checkpoint path is not a regular file");
    }
    long size = Files.size(path);
    if (size <= 0 || size > maximum) {
      throw new IOException("Chainbase checkpoint target length is invalid");
    }
    return Files.readAllBytes(path);
  }

  private static void createDirectory(Path path) throws IOException {
    Files.createDirectories(path);
    if (!Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
      throw new IOException("Chainbase checkpoint path is not a directory");
    }
  }

  private static void syncDirectory(Path path) throws IOException {
    try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
      channel.force(true);
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
    AFTER_STORE_BATCH,
    AFTER_MATERIALIZED_TARGET,
    AFTER_CURRENT
  }

  @FunctionalInterface
  interface FaultHook {
    void after(Stage stage, String dbName) throws IOException;
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

  /** Minimal restart identity retained by CHAINBASE_CURRENT. */
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
