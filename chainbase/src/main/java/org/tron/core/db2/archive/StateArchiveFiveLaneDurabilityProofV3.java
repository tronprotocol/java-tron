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
import java.util.List;
import java.util.Objects;
import org.tron.core.db2.archive.StateArchiveFiveLaneRecoveryIntentV3.RecoveryPoint;
import org.tron.core.db2.archive.StateArchiveFiveLaneSegmentWriterV3.ArchiveDurabilityProof;
import org.tron.core.db2.archive.StateArchiveFiveLaneSegmentWriterV3.FileTailProof;

/** Byte-exact Common authority record for one append-file durability proof. */
public final class StateArchiveFiveLaneDurabilityProofV3 {

  public static final String FILE_NAME = "append-file-proof.current";
  public static final String TEMP_FILE_NAME = "append-file-proof.current.tmp";
  public static final int MAGIC = 0x53415033; // SAP3
  public static final int TRAILER_MAGIC = 0x33504153; // 3PAS
  public static final int HEADER_LENGTH = 352;
  public static final int TAIL_LENGTH = 72;
  public static final int TRAILER_LENGTH = 48;

  private static final byte[] HEADER_DOMAIN = StateArchiveFileFormatV3.ascii(
      "TRON-STATE-ARCHIVE-PROOF-HEADER-V3\0");
  private static final byte[] TAILS_DOMAIN = StateArchiveFileFormatV3.ascii(
      "TRON-STATE-ARCHIVE-PROOF-TAILS-V3\0");
  private static final byte[] PROOF_DOMAIN = StateArchiveFileFormatV3.ascii(
      "TRON-STATE-ARCHIVE-PROOF-V3\0");
  private static final int HEADER_DIGEST_OFFSET = 316;
  private static final int HEADER_CRC_OFFSET = 348;

  private StateArchiveFiveLaneDurabilityProofV3() {
  }

  public static byte[] encode(ArchiveDurabilityProof proof) {
    ArchiveDurabilityProof admitted = Objects.requireNonNull(proof, "proof");
    List<FileTailProof> tails = admitted.getFileTails();
    byte[] tailBytes = encodeTails(tails);
    int totalLength = Math.addExact(HEADER_LENGTH + TRAILER_LENGTH, tailBytes.length);
    ByteBuffer bytes = ByteBuffer.allocate(totalLength);
    bytes.putInt(MAGIC);
    bytes.putShort(StateArchiveFileFormatV3.MAJOR_VERSION);
    bytes.putShort(StateArchiveFileFormatV3.MINOR_VERSION);
    bytes.putInt(HEADER_LENGTH);
    bytes.putShort((short) TAIL_LENGTH);
    bytes.putShort((short) tails.size());
    bytes.putLong(totalLength);
    bytes.putInt(0);
    bytes.putInt(0);
    bytes.put(admitted.getFormatIdentity());
    bytes.put(admitted.getDescriptorDigest());
    bytes.putLong(admitted.getCheckpointSequence());
    putPoint(bytes, admitted.getTarget());
    bytes.put(admitted.getCommonTargetDigest());
    bytes.put(StateArchiveFileFormatV3.sha256(TAILS_DOMAIN, tailBytes));
    bytes.put(new byte[28]);
    if (bytes.position() != HEADER_DIGEST_OFFSET) {
      throw new IllegalStateException("Invalid State Archive proof header layout");
    }
    bytes.put(StateArchiveFileFormatV3.sha256(HEADER_DOMAIN,
        Arrays.copyOf(bytes.array(), HEADER_DIGEST_OFFSET)));
    bytes.putInt(crc32c(bytes.array(), 0, HEADER_CRC_OFFSET));
    bytes.put(tailBytes);
    int proofDigestOffset = bytes.position();
    bytes.put(StateArchiveFileFormatV3.sha256(PROOF_DOMAIN,
        Arrays.copyOf(bytes.array(), proofDigestOffset)));
    bytes.putLong(totalLength);
    bytes.putInt(crc32c(bytes.array(), 0, totalLength - 8));
    bytes.putInt(TRAILER_MAGIC);
    return bytes.array();
  }

  public static ArchiveDurabilityProof decode(byte[] encoded) {
    Objects.requireNonNull(encoded, "encoded");
    if (encoded.length < HEADER_LENGTH + TRAILER_LENGTH) {
      throw new IllegalArgumentException("State Archive proof is truncated");
    }
    ByteBuffer bytes = ByteBuffer.wrap(encoded);
    require(bytes.getInt() == MAGIC, "proof magic");
    require(bytes.getShort() == StateArchiveFileFormatV3.MAJOR_VERSION, "proof major version");
    require(bytes.getShort() == StateArchiveFileFormatV3.MINOR_VERSION, "proof minor version");
    require(bytes.getInt() == HEADER_LENGTH, "proof header length");
    require(Short.toUnsignedInt(bytes.getShort()) == TAIL_LENGTH, "proof tail length");
    int tailCount = Short.toUnsignedInt(bytes.getShort());
    int expectedLength = Math.addExact(HEADER_LENGTH + TRAILER_LENGTH,
        Math.multiplyExact(tailCount, TAIL_LENGTH));
    require(bytes.getLong() == expectedLength && encoded.length == expectedLength,
        "proof total length");
    require(bytes.getInt() == 0 && bytes.getInt() == 0, "proof flags or reserved field");
    requireArray(read(bytes, 32), StateArchiveFileFormatV3.compositeFormatDigest(),
        "proof composite format");
    requireArray(read(bytes, 32), StateArchiveFileFormatV3.fiveLaneDescriptorDigest(),
        "proof placement descriptor");
    long checkpointSequence = bytes.getLong();
    RecoveryPoint target = readPoint(bytes);
    byte[] commonTargetDigest = read(bytes, 32);
    byte[] tailsDigest = read(bytes, 32);
    requireZero(bytes, 28, "proof reserved bytes");
    requireArray(read(bytes, 32), StateArchiveFileFormatV3.sha256(HEADER_DOMAIN,
        Arrays.copyOf(encoded, HEADER_DIGEST_OFFSET)), "proof header digest");
    require(bytes.getInt() == crc32c(encoded, 0, HEADER_CRC_OFFSET),
        "proof header checksum");
    byte[] tailBytes = read(bytes, tailCount * TAIL_LENGTH);
    requireArray(tailsDigest, StateArchiveFileFormatV3.sha256(TAILS_DOMAIN, tailBytes),
        "proof tails digest");
    List<FileTailProof> tails = decodeTails(tailBytes);
    int proofDigestOffset = HEADER_LENGTH + tailBytes.length;
    requireArray(read(bytes, 32), StateArchiveFileFormatV3.sha256(PROOF_DOMAIN,
        Arrays.copyOf(encoded, proofDigestOffset)), "proof digest");
    require(bytes.getLong() == expectedLength, "proof repeated length");
    require(bytes.getInt() == crc32c(encoded, 0, encoded.length - 8),
        "proof checksum");
    require(bytes.getInt() == TRAILER_MAGIC, "proof trailer magic");
    return new ArchiveDurabilityProof(checkpointSequence, target, commonTargetDigest, tails);
  }

  public static void publish(Path archiveRoot, ArchiveDurabilityProof proof) throws IOException {
    Path root = Objects.requireNonNull(archiveRoot, "archiveRoot");
    Files.createDirectories(root);
    byte[] encoded = encode(proof);
    Path temporary = root.resolve(TEMP_FILE_NAME);
    Path target = root.resolve(FILE_NAME);
    try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
      ByteBuffer buffer = ByteBuffer.wrap(encoded);
      while (buffer.hasRemaining()) {
        channel.write(buffer);
      }
      channel.force(true);
    }
    try {
      Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE,
          StandardCopyOption.REPLACE_EXISTING);
    } catch (AtomicMoveNotSupportedException unsupported) {
      throw new IOException("State Archive proof requires atomic publication", unsupported);
    }
    HistorySegmentStore.syncDirectory(root);
    decode(Files.readAllBytes(target));
  }

  public static ArchiveDurabilityProof loadAndVerify(Path archiveRoot,
      StateArchiveFiveLaneSegmentWriterV3 writer) throws IOException {
    ArchiveDurabilityProof proof = decode(Files.readAllBytes(
        Objects.requireNonNull(archiveRoot, "archiveRoot").resolve(FILE_NAME)));
    Objects.requireNonNull(writer, "writer").verifyDurabilityProof(proof);
    return proof;
  }

  /** Proves the persisted Archive tail is the exact History authority referenced by Common W. */
  public static ArchiveDurabilityProof loadAndVerify(Path archiveRoot,
      StateArchiveFiveLaneSegmentWriterV3 writer, RecoveryPoint commonCommitted,
      byte[] commonTargetDigest) throws IOException {
    ArchiveDurabilityProof proof = decode(Files.readAllBytes(
        Objects.requireNonNull(archiveRoot, "archiveRoot").resolve(FILE_NAME)));
    RecoveryPoint common = Objects.requireNonNull(commonCommitted, "commonCommitted");
    if (!samePoint(proof.getTarget(), common)
        || !Arrays.equals(proof.getCommonTargetDigest(), commonTargetDigest)) {
      throw new IllegalArgumentException("State Archive proof differs from Common target");
    }
    Objects.requireNonNull(writer, "writer").verifyDurabilityProof(proof);
    return proof;
  }

  private static byte[] encodeTails(List<FileTailProof> tails) {
    ByteBuffer bytes = ByteBuffer.allocate(Math.multiplyExact(tails.size(), TAIL_LENGTH));
    for (FileTailProof tail : tails) {
      bytes.putShort((short) tail.getLaneId());
      bytes.putShort((short) 0);
      bytes.putInt(0);
      bytes.putLong(tail.getSegmentSeq());
      bytes.putLong(tail.getMarkerOffset());
      bytes.putLong(tail.getMarkerLength());
      bytes.putLong(tail.getMarkerEndOffset());
      bytes.put(tail.getMarkerDigest());
    }
    return bytes.array();
  }

  private static List<FileTailProof> decodeTails(byte[] encoded) {
    ByteBuffer bytes = ByteBuffer.wrap(encoded);
    List<FileTailProof> tails = new ArrayList<>();
    while (bytes.hasRemaining()) {
      int laneId = Short.toUnsignedInt(bytes.getShort());
      require(bytes.getShort() == 0 && bytes.getInt() == 0, "proof tail reserved field");
      long segmentSeq = bytes.getLong();
      long markerOffset = bytes.getLong();
      long markerLength = bytes.getLong();
      long markerEndOffset = bytes.getLong();
      require(markerLength <= Integer.MAX_VALUE, "proof marker length");
      tails.add(new FileTailProof(laneId, segmentSeq, markerOffset, (int) markerLength,
          markerEndOffset, read(bytes, 32)));
    }
    return tails;
  }

  private static void putPoint(ByteBuffer bytes, RecoveryPoint point) {
    bytes.putLong(point.getEpoch());
    bytes.putLong(point.getBlockNumber());
    bytes.putLong(point.getTimestamp());
    bytes.put(point.getBlockHash());
    bytes.put(point.getParentHash());
    bytes.put(point.getResultHistoryDigest());
  }

  private static RecoveryPoint readPoint(ByteBuffer bytes) {
    return new RecoveryPoint(bytes.getLong(), bytes.getLong(), bytes.getLong(),
        read(bytes, 32), read(bytes, 32), read(bytes, 32));
  }

  private static boolean samePoint(RecoveryPoint left, RecoveryPoint right) {
    return left.getEpoch() == right.getEpoch()
        && left.getBlockNumber() == right.getBlockNumber()
        && left.getTimestamp() == right.getTimestamp()
        && Arrays.equals(left.getBlockHash(), right.getBlockHash())
        && Arrays.equals(left.getParentHash(), right.getParentHash())
        && Arrays.equals(left.getResultHistoryDigest(), right.getResultHistoryDigest());
  }

  private static byte[] read(ByteBuffer bytes, int length) {
    byte[] result = new byte[length];
    bytes.get(result);
    return result;
  }

  private static void requireZero(ByteBuffer bytes, int length, String field) {
    requireArray(read(bytes, length), new byte[length], field);
  }

  private static void requireArray(byte[] actual, byte[] expected, String field) {
    require(Arrays.equals(actual, expected), field);
  }

  private static void require(boolean condition, String field) {
    if (!condition) {
      throw new IllegalArgumentException("State Archive " + field + " mismatch");
    }
  }

  private static int crc32c(byte[] bytes, int offset, int length) {
    return Hashing.crc32c().hashBytes(bytes, offset, length).asInt();
  }
}
