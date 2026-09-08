package org.tron.core.db2.archive;

import com.google.common.hash.Hashing;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.OptionalLong;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.db2.archive.BlockReverseDiff.DbGroup;
import org.tron.core.db2.archive.BlockReverseDiff.Entry;
import org.tron.core.db2.core.CommonCheckpointPayload;
import org.tron.core.db2.core.CommonCheckpointTarget;
import org.tron.core.db2.stateroot.PathStateStoreManifest.Engine;

/** Persistent exact-key locator for next-format per-block checkpoint history files. */
final class StateArchiveCheckpointServingIndex {

  static final String DIRECTORY = "checkpoint-serving-index";
  private static final String DATABASE = "keys";
  private static final byte[] MARKER_KEY = new byte[]{0};
  private static final byte CHANGE_PREFIX = 1;
  private static final byte BLOCK_PREFIX = 2;
  private static final int MARKER_MAGIC = 0x53414349; // SACI
  private static final short MARKER_VERSION = 1;
  private static final int DIGEST_LENGTH = 32;
  private static final int MARKER_LENGTH = Integer.BYTES + 2 * Short.BYTES
      + 5 * DIGEST_LENGTH + 3 * Long.BYTES + DIGEST_LENGTH;
  private static final int LOCATION_LENGTH = DIGEST_LENGTH + Integer.BYTES + Long.BYTES
      + DIGEST_LENGTH;

  private StateArchiveCheckpointServingIndex() {
  }

  static Status inspect(Path archiveDirectory, CommonCheckpointTarget target)
      throws IOException {
    return inspect(archiveDirectory, target, configuredEngine());
  }

  static Status inspect(Path archiveDirectory, CommonCheckpointTarget target, Engine engine)
      throws IOException {
    Path databasePath = databasePath(archiveDirectory);
    if (!Files.exists(databasePath, LinkOption.NOFOLLOW_LINKS)) {
      return Status.ABSENT;
    }
    StateArchiveIndexEngineManifest.require(archiveDirectory.resolve(DIRECTORY), engine);
    try (StateArchiveIndexDatabase.Reader database =
        StateArchiveIndexDatabase.openReader(databasePath, engine)) {
      return inspect(database.get(MARKER_KEY), target);
    }
  }

  static void apply(Path archiveDirectory, CommonCheckpointPayload payload,
      CommonCheckpointTarget target) throws IOException {
    apply(archiveDirectory, payload, target, configuredEngine());
  }

  static void apply(Path archiveDirectory, CommonCheckpointPayload payload,
      CommonCheckpointTarget target, Engine engine) throws IOException {
    try (Session session = session(archiveDirectory, engine)) {
      session.apply(payload, target);
    }
  }

  static Session session(Path archiveDirectory, Engine engine) {
    return new Session(archiveDirectory, engine);
  }

  static int openReferenceCount(Path archiveDirectory, Engine engine) {
    return StateArchiveIndexDatabase.openReferenceCount(databasePath(archiveDirectory), engine);
  }

  private static Status inspect(byte[] encoded, CommonCheckpointTarget target)
      throws IOException {
    if (encoded == null) {
      throw new IOException("State Archive checkpoint serving marker is missing");
    }
    Marker marker = decodeMarker(encoded);
    if (Arrays.equals(encoded, encodeMarker(target, marker.baseBlockNumber,
        marker.baseBlockHash))) {
      return Status.EXACT;
    }
    requireParent(marker, target);
    return Status.PARENT;
  }

  static Reader openReader(Path archiveDirectory, CommonCheckpointTarget target)
      throws IOException {
    return openReader(archiveDirectory, target, configuredEngine());
  }

  static Reader openReader(Path archiveDirectory, CommonCheckpointTarget target, Engine engine)
      throws IOException {
    return new Reader(archiveDirectory, target, engine, true);
  }

  static Reader openTrustedReader(Path archiveDirectory, CommonCheckpointTarget target,
      Engine engine) throws IOException {
    return new Reader(archiveDirectory, target, engine, false);
  }

  static Engine configuredEngine() {
    org.tron.core.config.args.Storage storage = CommonParameter.getInstance().getStorage();
    if (storage == null) {
      return Engine.ROCKSDB;
    }
    String configured = storage.getStateArchiveServingIndexEngine();
    if (configured == null) {
      configured = storage.getDbEngine();
    }
    if (configured == null) {
      return Engine.ROCKSDB;
    }
    return Engine.valueOf(configured.trim().toUpperCase(Locale.ROOT));
  }

  private static void requireParent(Marker marker, CommonCheckpointTarget target)
      throws IOException {
    BlockSnapshotMeta first = target.getFirstBlock();
    if (!Arrays.equals(marker.formatIdentity, target.getFormatIdentity())
        || marker.lastEpoch + 1 != first.getEpoch()
        || marker.lastBlockNumber + 1 != first.getBlockNumber()
        || !Arrays.equals(marker.lastBlockHash, first.getParentHash())
        || !Arrays.equals(marker.stateRoot, target.getParentStateRoot())) {
      throw new IOException("State Archive checkpoint serving index is not the target parent");
    }
  }

  private static byte[] changeKey(String dbName, byte[] rawKey, long blockNumber) {
    int storeId = ArchiveParticipantDescriptor.current().getStoreId(dbName);
    byte[] key = Objects.requireNonNull(rawKey, "rawKey");
    return ByteBuffer.allocate(1 + Short.BYTES + Integer.BYTES + key.length + Long.BYTES)
        .put(CHANGE_PREFIX).putShort((short) storeId).putInt(key.length).put(key)
        .putLong(blockNumber).array();
  }

  private static byte[] changePrefix(String dbName, byte[] rawKey) {
    int storeId = ArchiveParticipantDescriptor.current().getStoreId(dbName);
    byte[] key = Objects.requireNonNull(rawKey, "rawKey");
    return ByteBuffer.allocate(1 + Short.BYTES + Integer.BYTES + key.length)
        .put(CHANGE_PREFIX).putShort((short) storeId).putInt(key.length).put(key).array();
  }

  private static byte[] blockKey(long blockNumber) {
    return ByteBuffer.allocate(1 + Long.BYTES).put(BLOCK_PREFIX).putLong(blockNumber).array();
  }

  private static byte[] encodeLocation(byte[] targetDigest, int index, BlockSnapshotMeta meta) {
    return ByteBuffer.allocate(LOCATION_LENGTH).put(targetDigest).putInt(index)
        .putLong(meta.getEpoch()).put(meta.getBlockHash()).array();
  }

  private static Location decodeLocation(byte[] encoded) throws IOException {
    if (encoded == null || encoded.length != LOCATION_LENGTH) {
      throw new IOException("State Archive checkpoint block location is missing or corrupt");
    }
    ByteBuffer input = ByteBuffer.wrap(encoded);
    byte[] targetDigest = new byte[DIGEST_LENGTH];
    input.get(targetDigest);
    int index = input.getInt();
    long epoch = input.getLong();
    byte[] blockHash = new byte[DIGEST_LENGTH];
    input.get(blockHash);
    if (index < 0 || epoch < 0) {
      throw new IOException("State Archive checkpoint block location is invalid");
    }
    return new Location(targetDigest, index, epoch, blockHash);
  }

  private static byte[] encodeMarker(CommonCheckpointTarget target, long baseBlockNumber,
      byte[] baseBlockHash) {
    if (baseBlockNumber < 0) {
      throw new IllegalArgumentException("checkpoint serving base block must not be negative");
    }
    try {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream(MARKER_LENGTH);
      DataOutputStream output = new DataOutputStream(bytes);
      output.writeInt(MARKER_MAGIC);
      output.writeShort(MARKER_VERSION);
      output.writeShort(0);
      output.write(target.getFormatIdentity());
      output.write(target.getPayloadDigest());
      output.writeLong(baseBlockNumber);
      output.write(baseBlockHash);
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
      throw new IllegalStateException("in-memory checkpoint serving marker encoding failed",
          impossible);
    }
  }

  private static Marker decodeMarker(byte[] encoded) throws IOException {
    if (encoded == null || encoded.length != MARKER_LENGTH) {
      throw new IOException("State Archive checkpoint serving marker length is invalid");
    }
    int bodyLength = encoded.length - DIGEST_LENGTH;
    byte[] body = Arrays.copyOf(encoded, bodyLength);
    if (!Arrays.equals(Arrays.copyOfRange(encoded, bodyLength, encoded.length),
        Hashing.sha256().hashBytes(body).asBytes())) {
      throw new IOException("State Archive checkpoint serving marker checksum differs");
    }
    try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(body))) {
      if (input.readInt() != MARKER_MAGIC || input.readShort() != MARKER_VERSION
          || input.readShort() != 0) {
        throw new IOException("State Archive checkpoint serving marker format is unsupported");
      }
      byte[] formatIdentity = readDigest(input);
      byte[] payloadDigest = readDigest(input);
      long baseBlockNumber = input.readLong();
      byte[] baseBlockHash = readDigest(input);
      long lastEpoch = input.readLong();
      long lastBlockNumber = input.readLong();
      byte[] lastBlockHash = readDigest(input);
      byte[] stateRoot = readDigest(input);
      if (baseBlockNumber < 0 || lastEpoch < 0 || lastBlockNumber <= baseBlockNumber) {
        throw new IOException("State Archive checkpoint serving marker range is invalid");
      }
      return new Marker(formatIdentity, payloadDigest, baseBlockNumber, baseBlockHash, lastEpoch,
          lastBlockNumber, lastBlockHash, stateRoot);
    } catch (EOFException truncated) {
      throw new IOException("State Archive checkpoint serving marker is truncated", truncated);
    }
  }

  private static Path databasePath(Path archiveDirectory) {
    return archiveDirectory.resolve(DIRECTORY).resolve(DATABASE);
  }

  private static void requireStateDatabase(String dbName) throws IOException {
    if (!ArchiveStoreScope.isStateDatabase(dbName)) {
      throw new IOException("State Archive checkpoint contains a non-state Store: " + dbName);
    }
  }

  private static byte[] readDigest(DataInputStream input) throws IOException {
    byte[] value = new byte[DIGEST_LENGTH];
    input.readFully(value);
    return value;
  }

  private static boolean startsWith(byte[] value, byte[] prefix) {
    if (value.length < prefix.length) {
      return false;
    }
    for (int index = 0; index < prefix.length; index++) {
      if (value[index] != prefix[index]) {
        return false;
      }
    }
    return true;
  }

  enum Status {
    ABSENT,
    PARENT,
    EXACT
  }

  static final class Session implements AutoCloseable {

    private final Path archiveDirectory;
    private final Engine engine;
    private StateArchiveIndexDatabase.Writer database;

    private Session(Path archiveDirectory, Engine engine) {
      this.archiveDirectory = Objects.requireNonNull(archiveDirectory, "archiveDirectory");
      this.engine = Objects.requireNonNull(engine, "engine");
    }

    Status inspect(CommonCheckpointTarget target) throws IOException {
      Path databasePath = databasePath(archiveDirectory);
      if (database == null && !Files.exists(databasePath, LinkOption.NOFOLLOW_LINKS)) {
        return Status.ABSENT;
      }
      return StateArchiveCheckpointServingIndex.inspect(
          openExisting().get(MARKER_KEY), target);
    }

    void apply(CommonCheckpointPayload payload, CommonCheckpointTarget target)
        throws IOException {
      Path indexDirectory = archiveDirectory.resolve(DIRECTORY);
      Path databasePath = databasePath(archiveDirectory);
      boolean databaseExisted = Files.exists(databasePath, LinkOption.NOFOLLOW_LINKS);
      Files.createDirectories(indexDirectory);
      if (!Files.isDirectory(indexDirectory, LinkOption.NOFOLLOW_LINKS)) {
        throw new IOException("State Archive checkpoint serving path is not a directory");
      }
      StateArchiveIndexEngineManifest.openOrCreate(indexDirectory, engine);
      StateArchiveIndexDatabase.Writer writer = open();
      byte[] existing = writer.get(MARKER_KEY);
      long baseBlockNumber = target.getFirstBlock().getBlockNumber() - 1;
      byte[] baseBlockHash = target.getFirstBlock().getParentHash();
      if (existing != null) {
        Marker parent = decodeMarker(existing);
        if (Arrays.equals(existing,
            encodeMarker(target, parent.baseBlockNumber, parent.baseBlockHash))) {
          return;
        }
        requireParent(parent, target);
        baseBlockNumber = parent.baseBlockNumber;
        baseBlockHash = parent.baseBlockHash;
      } else if (databaseExisted) {
        throw new IOException("State Archive checkpoint serving marker is missing");
      }
      List<StateArchiveIndexDatabase.Mutation> mutations = new ArrayList<>();
      for (int index = 0; index < payload.getBlocks().size(); index++) {
        CommonCheckpointPayload.BlockPayload block = payload.getBlocks().get(index);
        long blockNumber = block.getMeta().getBlockNumber();
        for (DbGroup group : block.getArchiveDiff().getGroups()) {
          requireStateDatabase(group.getDbName());
          for (Entry entry : group.getEntries()) {
            mutations.add(StateArchiveIndexDatabase.put(
                changeKey(group.getDbName(), entry.getKey(), blockNumber), new byte[]{1}));
          }
        }
        mutations.add(StateArchiveIndexDatabase.put(blockKey(blockNumber),
            encodeLocation(target.getPayloadDigest(), index, block.getMeta())));
      }
      mutations.add(StateArchiveIndexDatabase.put(MARKER_KEY,
          encodeMarker(target, baseBlockNumber, baseBlockHash)));
      writer.write(mutations);
      HistorySegmentStore.syncDirectory(indexDirectory);
    }

    private StateArchiveIndexDatabase.Writer openExisting() throws IOException {
      StateArchiveIndexEngineManifest.require(archiveDirectory.resolve(DIRECTORY), engine);
      return open();
    }

    private StateArchiveIndexDatabase.Writer open() throws IOException {
      if (database == null) {
        database = StateArchiveIndexDatabase.openWriter(databasePath(archiveDirectory), engine);
      }
      return database;
    }

    @Override
    public void close() throws IOException {
      if (database != null) {
        try {
          database.close();
        } finally {
          database = null;
        }
      }
    }
  }

  static final class Reader implements AutoCloseable {

    private final Path archiveDirectory;
    private final StateArchiveIndexDatabase.Reader database;
    private final Marker marker;
    private boolean closed;

    private Reader(Path archiveDirectory, CommonCheckpointTarget target, Engine engine,
        boolean validateEngine) throws IOException {
      this.archiveDirectory = Objects.requireNonNull(archiveDirectory, "archiveDirectory");
      Objects.requireNonNull(target, "target");
      if (validateEngine) {
        StateArchiveIndexEngineManifest.require(archiveDirectory.resolve(DIRECTORY), engine);
      }
      StateArchiveIndexDatabase.Reader opened;
      try {
        opened = StateArchiveIndexDatabase.openReader(databasePath(archiveDirectory), engine);
      } catch (IOException | RuntimeException failure) {
        throw new IOException("Failed to open State Archive checkpoint serving reader", failure);
      }
      this.database = opened;
      Marker loaded;
      try {
        byte[] encoded = opened.get(MARKER_KEY);
        loaded = decodeMarker(encoded);
        if (!Arrays.equals(encoded, encodeMarker(target, loaded.baseBlockNumber,
            loaded.baseBlockHash))) {
          throw new IOException("State Archive checkpoint reader target differs");
        }
      } catch (IOException | RuntimeException failure) {
        opened.close();
        if (failure instanceof IOException) {
          throw (IOException) failure;
        }
        throw new IOException("Failed to validate State Archive checkpoint serving reader",
            failure);
      }
      this.marker = loaded;
    }

    OptionalLong firstChangeAfter(String dbName, byte[] rawKey, long targetBlock,
        long upperBound) throws IOException {
      ensureOpen();
      requireStateDatabase(dbName);
      if (targetBlock < marker.baseBlockNumber || upperBound > marker.lastBlockNumber
          || targetBlock > upperBound) {
        throw new IllegalArgumentException("checkpoint serving query is outside coverage");
      }
      if (targetBlock == Long.MAX_VALUE) {
        return OptionalLong.empty();
      }
      byte[] prefix = changePrefix(dbName, rawKey);
      byte[] seek = ByteBuffer.allocate(prefix.length + Long.BYTES).put(prefix)
          .putLong(targetBlock + 1).array();
      StateArchiveIndexDatabase.KeyValue foundEntry = database.seek(seek);
      if (foundEntry == null) {
        return OptionalLong.empty();
      }
      byte[] found = foundEntry.getKey();
      if (found.length != prefix.length + Long.BYTES || !startsWith(found, prefix)) {
        return OptionalLong.empty();
      }
      long blockNumber = ByteBuffer.wrap(found, prefix.length, Long.BYTES).getLong();
      return blockNumber <= upperBound ? OptionalLong.of(blockNumber) : OptionalLong.empty();
    }

    OldValue readOldValue(String dbName, byte[] rawKey, long blockNumber) throws IOException {
      ensureOpen();
      requireStateDatabase(dbName);
      if (blockNumber <= marker.baseBlockNumber || blockNumber > marker.lastBlockNumber) {
        throw new IllegalArgumentException("checkpoint history block is outside coverage");
      }
      Location location = decodeLocation(database.get(blockKey(blockNumber)));
      String fileName = StateArchiveCheckpointMaterializer.blockFileName(location.index,
          new BlockSnapshotMeta(location.epoch, blockNumber, location.blockHash,
              new byte[DIGEST_LENGTH], 0));
      Path path = archiveDirectory.resolve(StateArchiveCheckpointMaterializer.TARGET_DIRECTORY)
          .resolve(StateArchiveCheckpointMaterializer.hex(location.targetDigest))
          .resolve(StateArchiveCheckpointMaterializer.BLOCK_DIRECTORY).resolve(fileName);
      BlockReverseDiff diff = StateArchiveCheckpointMaterializer.loadCheckpointBlock(path);
      if (diff.getMeta().getBlockNumber() != blockNumber
          || diff.getMeta().getEpoch() != location.epoch
          || !Arrays.equals(diff.getMeta().getBlockHash(), location.blockHash)) {
        throw new IOException("State Archive checkpoint block location identity differs");
      }
      for (DbGroup group : diff.getGroups()) {
        if (group.getDbName().equals(dbName)) {
          for (Entry entry : group.getEntries()) {
            if (Arrays.equals(entry.getKey(), rawKey)) {
              return entry.getOldValue();
            }
          }
        }
      }
      throw new IOException("State Archive checkpoint index references a missing key");
    }

    long getIndexedFrom() {
      return marker.baseBlockNumber;
    }

    long getIndexedThrough() {
      return marker.lastBlockNumber;
    }

    byte[] getHeadHash() {
      return Arrays.copyOf(marker.lastBlockHash, marker.lastBlockHash.length);
    }

    @Override
    public void close() {
      if (!closed) {
        closed = true;
        try {
          database.close();
        } catch (IOException failure) {
          throw new IllegalStateException("Failed to close Archive serving reader", failure);
        }
      }
    }

    private void ensureOpen() {
      if (closed) {
        throw new IllegalStateException("State Archive checkpoint serving reader is closed");
      }
    }
  }

  private static final class Marker {

    private final byte[] formatIdentity;
    private final byte[] payloadDigest;
    private final long baseBlockNumber;
    private final byte[] baseBlockHash;
    private final long lastEpoch;
    private final long lastBlockNumber;
    private final byte[] lastBlockHash;
    private final byte[] stateRoot;

    private Marker(byte[] formatIdentity, byte[] payloadDigest, long baseBlockNumber,
        byte[] baseBlockHash, long lastEpoch, long lastBlockNumber, byte[] lastBlockHash,
        byte[] stateRoot) {
      this.formatIdentity = formatIdentity;
      this.payloadDigest = payloadDigest;
      this.baseBlockNumber = baseBlockNumber;
      this.baseBlockHash = baseBlockHash;
      this.lastEpoch = lastEpoch;
      this.lastBlockNumber = lastBlockNumber;
      this.lastBlockHash = lastBlockHash;
      this.stateRoot = stateRoot;
    }
  }

  private static final class Location {

    private final byte[] targetDigest;
    private final int index;
    private final long epoch;
    private final byte[] blockHash;

    private Location(byte[] targetDigest, int index, long epoch, byte[] blockHash) {
      this.targetDigest = targetDigest;
      this.index = index;
      this.epoch = epoch;
      this.blockHash = blockHash;
    }
  }
}
