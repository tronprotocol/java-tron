package org.tron.core.db2.archive;

import com.google.common.hash.Hashing;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.tron.core.db2.archive.StateArchiveSegmentFormatV3.CurrentSegment;
import org.tron.core.db2.archive.StateArchiveSegmentFormatV3.SealedSegment;

/** Atomic structural catalog for the five-lane append-file history. */
final class StateArchiveHistoryCatalogV3 {

  static final String DIRECTORY = "catalog";
  static final String CURRENT = "CURRENT";
  private static final String GENERATIONS = "generations";
  private static final int GENERATION_MAGIC = 0x53434733; // SCG3
  private static final int CURRENT_MAGIC = 0x53435533; // SCU3
  private static final int GENERATION_TRAILER_MAGIC = 0x33474353; // 3GCS
  private static final int CURRENT_TRAILER_MAGIC = 0x33554353; // 3UCS
  private static final int HEADER_LENGTH = 256;
  private static final int CURRENT_RECORD_LENGTH = 112;
  private static final int TRAILER_LENGTH = 48;
  private static final int CURRENT_LENGTH = 96;

  private final Path root;
  private final Path generations;
  private Generation selected;

  private StateArchiveHistoryCatalogV3(Path archiveRoot, Generation selected) {
    root = archiveRoot.resolve(DIRECTORY);
    generations = root.resolve(GENERATIONS);
    this.selected = selected;
  }

  static StateArchiveHistoryCatalogV3 openOrEmpty(Path archiveRoot) throws IOException {
    Path root = archiveRoot.resolve(DIRECTORY);
    Path current = root.resolve(CURRENT);
    if (!Files.exists(current)) {
      return new StateArchiveHistoryCatalogV3(archiveRoot, null);
    }
    CurrentPointer pointer = decodeCurrent(Files.readAllBytes(current));
    Path generationPath = root.resolve(GENERATIONS).resolve(fileName(pointer.generation));
    if (!Files.isRegularFile(generationPath)) {
      throw new IOException("State Archive Catalog selected generation is missing");
    }
    byte[] encoded = Files.readAllBytes(generationPath);
    Generation generation = decodeGeneration(encoded);
    if (generation.generation != pointer.generation
        || !Arrays.equals(generation.digest, pointer.digest)) {
      throw new IOException("State Archive Catalog CURRENT identity mismatch");
    }
    return new StateArchiveHistoryCatalogV3(archiveRoot, generation);
  }

  boolean isPublished() {
    return selected != null;
  }

  Generation selected() {
    if (selected == null) {
      throw new IllegalStateException("State Archive Catalog has no selected generation");
    }
    return selected;
  }

  void publish(long segmentTargetBytes, List<CurrentSegment> current,
      List<SealedSegment> sealed) throws IOException {
    long generation = selected == null ? 0 : selected.generation + 1;
    byte[] previous = selected == null ? new byte[32] : selected.digest;
    Generation replacement = new Generation(generation, previous, segmentTargetBytes,
        current, sealed, null);
    byte[] encoded = encodeGeneration(replacement);
    Generation verified = decodeGeneration(encoded);
    Files.createDirectories(generations);
    Path temporary = generations.resolve(fileName(generation) + ".tmp");
    Path target = generations.resolve(fileName(generation));
    writeForced(temporary, encoded);
    if (Files.isRegularFile(target)) {
      Generation orphan = decodeGeneration(Files.readAllBytes(target));
      if (!Arrays.equals(orphan.digest, verified.digest)) {
        throw new IOException("State Archive Catalog orphan generation identity differs");
      }
      Files.delete(temporary);
    } else {
      atomicMove(temporary, target);
      syncDirectory(generations);
    }
    byte[] currentBytes = encodeCurrent(generation, verified.digest);
    Path currentTemporary = root.resolve(CURRENT + ".tmp");
    writeForced(currentTemporary, currentBytes);
    atomicMove(currentTemporary, root.resolve(CURRENT));
    syncDirectory(root);
    selected = verified;
  }

  private static byte[] encodeGeneration(Generation generation) {
    generation.validate();
    int totalLength = Math.addExact(HEADER_LENGTH + TRAILER_LENGTH,
        Math.addExact(generation.current.size() * CURRENT_RECORD_LENGTH,
            generation.sealed.size() * StateArchiveFileFormatV3.SEGMENT_MAP_ENTRY_LENGTH));
    ByteBuffer bytes = ByteBuffer.allocate(totalLength);
    bytes.putInt(GENERATION_MAGIC);
    bytes.putShort(StateArchiveFileFormatV3.MAJOR_VERSION);
    bytes.putShort(StateArchiveFileFormatV3.MINOR_VERSION);
    bytes.putInt(HEADER_LENGTH);
    bytes.putInt(0);
    bytes.putLong(totalLength);
    bytes.putLong(generation.generation);
    bytes.putLong(generation.segmentTargetBytes);
    bytes.putInt(generation.current.size());
    bytes.putInt(generation.sealed.size());
    bytes.putInt(CURRENT_RECORD_LENGTH);
    bytes.putInt(StateArchiveFileFormatV3.SEGMENT_MAP_ENTRY_LENGTH);
    bytes.put(generation.previousDigest);
    bytes.put(StateArchiveFileFormatV3.compositeFormatDigest());
    bytes.put(StateArchiveFileFormatV3.fiveLaneDescriptorDigest());
    bytes.put(laneSetDigest());
    bytes.put(new byte[72]);
    for (CurrentSegment segment : generation.current) {
      bytes.put(encodeCurrentRecord(segment));
    }
    for (SealedSegment segment : generation.sealed) {
      bytes.put(StateArchiveSegmentFormatV3.encodeSealedMapRecord(segment));
    }
    int digestOffset = totalLength - TRAILER_LENGTH;
    if (bytes.position() != digestOffset) {
      throw new IllegalStateException("Invalid State Archive Catalog generation layout");
    }
    bytes.put(StateArchiveFileFormatV3.sha256(Arrays.copyOf(bytes.array(), digestOffset)));
    bytes.putLong(totalLength);
    bytes.putInt(crc32c(bytes.array(), 0, totalLength - 8));
    bytes.putInt(GENERATION_TRAILER_MAGIC);
    return bytes.array();
  }

  private static Generation decodeGeneration(byte[] encoded) throws IOException {
    try {
      if (encoded == null || encoded.length < HEADER_LENGTH + TRAILER_LENGTH) {
        throw new IllegalArgumentException("Catalog generation is truncated");
      }
      ByteBuffer bytes = ByteBuffer.wrap(encoded);
      require(bytes.getInt() == GENERATION_MAGIC, "Catalog generation magic mismatch");
      require(bytes.getShort() == StateArchiveFileFormatV3.MAJOR_VERSION,
          "Catalog generation major version mismatch");
      require(bytes.getShort() == StateArchiveFileFormatV3.MINOR_VERSION,
          "Catalog generation minor version mismatch");
      require(bytes.getInt() == HEADER_LENGTH, "Catalog generation header length mismatch");
      require(bytes.getInt() == 0, "Catalog generation flags mismatch");
      require(bytes.getLong() == encoded.length, "Catalog generation length mismatch");
      long generation = bytes.getLong();
      long segmentTargetBytes = bytes.getLong();
      int currentCount = bytes.getInt();
      int sealedCount = bytes.getInt();
      require(bytes.getInt() == CURRENT_RECORD_LENGTH,
          "Catalog current record length mismatch");
      require(bytes.getInt() == StateArchiveFileFormatV3.SEGMENT_MAP_ENTRY_LENGTH,
          "Catalog sealed record length mismatch");
      byte[] previous = read(bytes, 32);
      require(Arrays.equals(read(bytes, 32), StateArchiveFileFormatV3.compositeFormatDigest()),
          "Catalog format identity mismatch");
      require(Arrays.equals(read(bytes, 32), StateArchiveFileFormatV3.fiveLaneDescriptorDigest()),
          "Catalog descriptor identity mismatch");
      require(Arrays.equals(read(bytes, 32), laneSetDigest()),
          "Catalog lane set identity mismatch");
      requireZero(bytes, 72);
      require(generation >= 0 && segmentTargetBytes > StateArchiveFileFormatV3.PART_HEADER_LENGTH
          && (currentCount == 0
              || currentCount == StateArchiveFileFormatV3.fiveLaneIds().length)
          && sealedCount >= 0, "Catalog generation header is invalid");
      long expectedLength = HEADER_LENGTH + TRAILER_LENGTH
          + (long) currentCount * CURRENT_RECORD_LENGTH
          + (long) sealedCount * StateArchiveFileFormatV3.SEGMENT_MAP_ENTRY_LENGTH;
      require(expectedLength == encoded.length, "Catalog generation record count mismatch");
      List<CurrentSegment> current = new ArrayList<>();
      for (int index = 0; index < currentCount; index++) {
        current.add(decodeCurrentRecord(read(bytes, CURRENT_RECORD_LENGTH)));
      }
      List<SealedSegment> sealed = new ArrayList<>();
      for (int index = 0; index < sealedCount; index++) {
        sealed.add(StateArchiveSegmentFormatV3.decodeSealedMapRecord(
            read(bytes, StateArchiveFileFormatV3.SEGMENT_MAP_ENTRY_LENGTH)));
      }
      int digestOffset = encoded.length - TRAILER_LENGTH;
      byte[] digest = read(bytes, 32);
      require(Arrays.equals(digest,
          StateArchiveFileFormatV3.sha256(Arrays.copyOf(encoded, digestOffset))),
          "Catalog generation digest mismatch");
      require(bytes.getLong() == encoded.length, "Catalog repeated length mismatch");
      require(bytes.getInt() == crc32c(encoded, 0, encoded.length - 8),
          "Catalog generation checksum mismatch");
      require(bytes.getInt() == GENERATION_TRAILER_MAGIC,
          "Catalog generation trailer mismatch");
      return new Generation(generation, previous, segmentTargetBytes, current, sealed, digest);
    } catch (IllegalArgumentException invalid) {
      throw new IOException("State Archive Catalog generation is corrupt", invalid);
    }
  }

  private static byte[] encodeCurrentRecord(CurrentSegment segment) {
    return ByteBuffer.allocate(CURRENT_RECORD_LENGTH)
        .putShort((short) segment.getLaneId())
        .putShort(StateArchiveFileFormatV3.laneKind(segment.getLaneId()))
        .putInt(0).putLong(segment.getSegmentSeq()).putLong(segment.getFirstBlock())
        .putLong(segment.getCurrentLastBlock()).putLong(segment.getDataEndOffset())
        .putLong(segment.getBlockFrameCount()).put(segment.getHeaderDigest())
        .put(parentDigest(segment.getLaneId(), segment.getSegmentSeq())).array();
  }

  private static CurrentSegment decodeCurrentRecord(byte[] encoded) {
    ByteBuffer bytes = ByteBuffer.wrap(encoded);
    int laneId = Short.toUnsignedInt(bytes.getShort());
    require(bytes.getShort() == StateArchiveFileFormatV3.laneKind(laneId),
        "Catalog current lane kind mismatch");
    require(bytes.getInt() == 0, "Catalog current flags mismatch");
    long sequence = bytes.getLong();
    CurrentSegment current = new CurrentSegment(laneId, sequence, bytes.getLong(),
        bytes.getLong(), bytes.getLong(), bytes.getLong(), read(bytes, 32));
    require(Arrays.equals(read(bytes, 32), parentDigest(laneId, sequence)),
        "Catalog current parent digest mismatch");
    return current;
  }

  private static byte[] parentDigest(int laneId, long sequence) {
    return StateArchiveFileFormatV3.sha256(
        ByteBuffer.allocate(Short.BYTES + Long.BYTES).putShort((short) laneId)
            .putLong(sequence).array());
  }

  private static byte[] laneSetDigest() {
    int[] lanes = StateArchiveFileFormatV3.fiveLaneIds();
    ByteBuffer bytes = ByteBuffer.allocate(lanes.length * Short.BYTES);
    for (int lane : lanes) {
      bytes.putShort((short) lane);
    }
    return StateArchiveFileFormatV3.sha256(bytes.array());
  }

  private static byte[] encodeCurrent(long generation, byte[] digest) {
    ByteBuffer bytes = ByteBuffer.allocate(CURRENT_LENGTH);
    bytes.putInt(CURRENT_MAGIC).putShort(StateArchiveFileFormatV3.MAJOR_VERSION)
        .putShort(StateArchiveFileFormatV3.MINOR_VERSION).putLong(generation).put(digest)
        .put(new byte[36]);
    bytes.putInt(crc32c(bytes.array(), 0, CURRENT_LENGTH - 12));
    bytes.putInt(CURRENT_TRAILER_MAGIC);
    return bytes.array();
  }

  private static CurrentPointer decodeCurrent(byte[] encoded) throws IOException {
    try {
      require(encoded != null && encoded.length == CURRENT_LENGTH,
          "Catalog CURRENT length mismatch");
      ByteBuffer bytes = ByteBuffer.wrap(encoded);
      require(bytes.getInt() == CURRENT_MAGIC, "Catalog CURRENT magic mismatch");
      require(bytes.getShort() == StateArchiveFileFormatV3.MAJOR_VERSION,
          "Catalog CURRENT major version mismatch");
      require(bytes.getShort() == StateArchiveFileFormatV3.MINOR_VERSION,
          "Catalog CURRENT minor version mismatch");
      long generation = bytes.getLong();
      byte[] digest = read(bytes, 32);
      requireZero(bytes, 36);
      require(bytes.getInt() == crc32c(encoded, 0, CURRENT_LENGTH - 12),
          "Catalog CURRENT checksum mismatch");
      require(bytes.getInt() == CURRENT_TRAILER_MAGIC, "Catalog CURRENT trailer mismatch");
      require(generation >= 0, "Catalog CURRENT generation is invalid");
      return new CurrentPointer(generation, digest);
    } catch (IllegalArgumentException invalid) {
      throw new IOException("State Archive Catalog CURRENT is corrupt", invalid);
    }
  }

  private static void writeForced(Path path, byte[] encoded) throws IOException {
    try (FileChannel channel = FileChannel.open(path, StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
      ByteBuffer bytes = ByteBuffer.wrap(encoded);
      while (bytes.hasRemaining()) {
        channel.write(bytes);
      }
      channel.force(true);
    }
  }

  private static void atomicMove(Path source, Path target) throws IOException {
    try {
      Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
    } catch (AtomicMoveNotSupportedException unsupported) {
      throw new IOException("State Archive Catalog requires atomic publication", unsupported);
    }
  }

  private static void syncDirectory(Path directory) throws IOException {
    try (FileChannel channel = FileChannel.open(directory, StandardOpenOption.READ)) {
      channel.force(true);
    }
  }

  private static byte[] read(ByteBuffer bytes, int length) {
    byte[] value = new byte[length];
    bytes.get(value);
    return value;
  }

  private static void requireZero(ByteBuffer bytes, int length) {
    for (int index = 0; index < length; index++) {
      require(bytes.get() == 0, "Catalog reserved bytes are non-zero");
    }
  }

  private static void require(boolean condition, String message) {
    if (!condition) {
      throw new IllegalArgumentException(message);
    }
  }

  private static int crc32c(byte[] bytes, int offset, int length) {
    return Hashing.crc32c().hashBytes(bytes, offset, length).asInt();
  }

  private static String fileName(long generation) {
    return String.format("catalog-%020d.bin", generation);
  }

  static final class Generation {
    private final long generation;
    private final byte[] previousDigest;
    private final long segmentTargetBytes;
    private final List<CurrentSegment> current;
    private final List<SealedSegment> sealed;
    private final byte[] digest;

    private Generation(long generation, byte[] previousDigest, long segmentTargetBytes,
        List<CurrentSegment> current, List<SealedSegment> sealed, byte[] digest) {
      this.generation = generation;
      this.previousDigest = Arrays.copyOf(Objects.requireNonNull(previousDigest), 32);
      this.segmentTargetBytes = segmentTargetBytes;
      this.current = sortedCurrent(current);
      this.sealed = sortedSealed(sealed);
      this.digest = digest == null ? null : Arrays.copyOf(digest, digest.length);
      validate();
    }

    private void validate() {
      require(previousDigest.length == 32 && (digest == null || digest.length == 32),
          "Catalog digest length mismatch");
      int[] expected = StateArchiveFileFormatV3.fiveLaneIds();
      require(current.isEmpty() || current.size() == expected.length,
          "Catalog must contain zero or five current lanes");
      require(!current.isEmpty() || sealed.isEmpty(),
          "Catalog cannot omit current lanes after sealed history");
      for (int index = 0; index < expected.length; index++) {
        require(current.isEmpty() || current.get(index).getLaneId() == expected[index],
            "Catalog current lane set mismatch");
      }
      int previousLane = -1;
      long previousSequence = -1;
      long previousLast = -1;
      for (SealedSegment segment : sealed) {
        if (segment.getLaneId() != previousLane) {
          previousLane = segment.getLaneId();
          previousSequence = -1;
          previousLast = -1;
        }
        require(segment.getSegmentSeq() == previousSequence + 1,
            "Catalog sealed sequence has a gap");
        if (previousLast >= 0) {
          require(segment.getFirstBlock() == previousLast + 1,
              "Catalog sealed block range has a gap");
        }
        previousSequence = segment.getSegmentSeq();
        previousLast = segment.getLastBlock();
      }
      for (CurrentSegment segment : current) {
        long lastSequence = -1;
        long lastBlock = -1;
        for (SealedSegment sealedSegment : sealed) {
          if (sealedSegment.getLaneId() == segment.getLaneId()) {
            lastSequence = sealedSegment.getSegmentSeq();
            lastBlock = sealedSegment.getLastBlock();
          }
        }
        require(segment.getSegmentSeq() == lastSequence + 1,
            "Catalog current sequence does not follow sealed segments");
        if (lastBlock >= 0) {
          require(segment.getFirstBlock() == lastBlock + 1,
              "Catalog current block range does not follow sealed segments");
        }
      }
    }

    long getGeneration() {
      return generation;
    }

    List<CurrentSegment> getCurrent() {
      return current;
    }

    List<SealedSegment> getSealed() {
      return sealed;
    }

    byte[] getDigest() {
      return Arrays.copyOf(digest, digest.length);
    }
  }

  private static List<CurrentSegment> sortedCurrent(List<CurrentSegment> input) {
    List<CurrentSegment> copy = new ArrayList<>(Objects.requireNonNull(input));
    copy.sort(Comparator.comparingInt(CurrentSegment::getLaneId));
    return Collections.unmodifiableList(copy);
  }

  private static List<SealedSegment> sortedSealed(List<SealedSegment> input) {
    List<SealedSegment> copy = new ArrayList<>(Objects.requireNonNull(input));
    copy.sort(Comparator.comparingInt(SealedSegment::getLaneId)
        .thenComparingLong(SealedSegment::getSegmentSeq));
    return Collections.unmodifiableList(copy);
  }

  private static final class CurrentPointer {
    private final long generation;
    private final byte[] digest;

    private CurrentPointer(long generation, byte[] digest) {
      this.generation = generation;
      this.digest = digest;
    }
  }
}
