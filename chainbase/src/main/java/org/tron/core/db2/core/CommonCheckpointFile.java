package org.tron.core.db2.core;

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
import java.util.Objects;

/** Standalone durable file lifecycle for one immutable common-checkpoint redo payload. */
public final class CommonCheckpointFile {

  static final String FILE_NAME = "COMMON_CHECKPOINT";
  static final String TEMPORARY_FILE_NAME = ".COMMON_CHECKPOINT.tmp";

  private final Path directory;
  private final Path checkpoint;
  private final Path temporary;
  private final int maxEncodedLength;
  private final CommonCheckpointPayloadCodec codec;
  private final FaultHook faultHook;

  public CommonCheckpointFile(Path directory) {
    this(directory, CommonCheckpointPayloadCodec.DEFAULT_MAX_ENCODED_LENGTH,
        (stage, path) -> { });
  }

  CommonCheckpointFile(Path directory, int maxEncodedLength, FaultHook faultHook) {
    this.directory = Objects.requireNonNull(directory, "directory").toAbsolutePath().normalize();
    this.maxEncodedLength = maxEncodedLength;
    this.codec = new CommonCheckpointPayloadCodec(maxEncodedLength);
    this.faultHook = Objects.requireNonNull(faultHook, "faultHook");
    this.checkpoint = this.directory.resolve(FILE_NAME);
    this.temporary = this.directory.resolve(TEMPORARY_FILE_NAME);
  }

  /** Publishes once; an exact existing payload is an idempotent retry and seals its directory. */
  public synchronized void publish(CommonCheckpointPayload payload) throws IOException {
    byte[] encoded = codec.encode(Objects.requireNonNull(payload, "payload"));
    requireDirectory();
    if (Files.exists(checkpoint, LinkOption.NOFOLLOW_LINKS)) {
      requireExact(encoded);
      syncDirectory();
      return;
    }
    if (Files.deleteIfExists(temporary)) {
      syncDirectory();
    }
    try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.CREATE_NEW,
        StandardOpenOption.WRITE)) {
      writeFully(channel, ByteBuffer.wrap(encoded));
      channel.force(true);
    }
    faultHook.after(Stage.AFTER_TEMPORARY_FORCE, temporary);
    try {
      Files.move(temporary, checkpoint, StandardCopyOption.ATOMIC_MOVE);
    } catch (AtomicMoveNotSupportedException unsupported) {
      throw new IOException("common checkpoint requires atomic publication", unsupported);
    }
    faultHook.after(Stage.AFTER_ATOMIC_PUBLISH, checkpoint);
    syncDirectory();
    faultHook.after(Stage.AFTER_DIRECTORY_FORCE, checkpoint);
  }

  public synchronized CommonCheckpointPayload loadRequired() throws IOException {
    if (!Files.isRegularFile(checkpoint, LinkOption.NOFOLLOW_LINKS)) {
      throw new IOException("common checkpoint is missing or not a regular file");
    }
    long length = Files.size(checkpoint);
    if (length < CommonCheckpointPayloadCodec.HEADER_LENGTH || length > maxEncodedLength) {
      throw new IOException("common checkpoint file length is invalid");
    }
    try {
      return codec.decode(Files.readAllBytes(checkpoint));
    } catch (IllegalArgumentException invalid) {
      throw new IOException("common checkpoint file is corrupt", invalid);
    }
  }

  public synchronized CommonCheckpointPayload loadIfPresent() throws IOException {
    if (!Files.exists(checkpoint, LinkOption.NOFOLLOW_LINKS)) {
      return null;
    }
    return loadRequired();
  }

  /** Returns whether startup must run redo before opening a published physical state. */
  public synchronized boolean isPresent() {
    return Files.exists(checkpoint, LinkOption.NOFOLLOW_LINKS);
  }

  /** Retires only this checkpoint and its non-authoritative temporary file. */
  public synchronized void retire() throws IOException {
    requireDirectory();
    boolean changed = Files.deleteIfExists(checkpoint);
    changed |= Files.deleteIfExists(temporary);
    if (changed) {
      faultHook.after(Stage.AFTER_RETIRE_DELETE, checkpoint);
    }
    syncDirectory();
    faultHook.after(Stage.AFTER_RETIRE_DIRECTORY_FORCE, directory);
  }

  Path getCheckpointPath() {
    return checkpoint;
  }

  Path getTemporaryPath() {
    return temporary;
  }

  private void requireExact(byte[] expected) throws IOException {
    if (!Files.isRegularFile(checkpoint, LinkOption.NOFOLLOW_LINKS)) {
      throw new IOException("common checkpoint is not a regular file");
    }
    long length = Files.size(checkpoint);
    if (length != expected.length || length > maxEncodedLength
        || !Arrays.equals(expected, Files.readAllBytes(checkpoint))) {
      throw new IOException("immutable common checkpoint identity mismatch");
    }
    try {
      codec.decode(expected);
    } catch (IllegalArgumentException invalid) {
      throw new IOException("common checkpoint file is corrupt", invalid);
    }
  }

  private void requireDirectory() throws IOException {
    if (Files.isSymbolicLink(directory)) {
      throw new IOException("common checkpoint directory must not be a symbolic link");
    }
    Files.createDirectories(directory);
    if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) {
      throw new IOException("common checkpoint parent is not a direct directory");
    }
  }

  private void syncDirectory() throws IOException {
    try (FileChannel channel = FileChannel.open(directory, StandardOpenOption.READ)) {
      channel.force(true);
    }
  }

  private static void writeFully(FileChannel channel, ByteBuffer buffer) throws IOException {
    while (buffer.hasRemaining()) {
      channel.write(buffer);
    }
  }

  enum Stage {
    AFTER_TEMPORARY_FORCE,
    AFTER_ATOMIC_PUBLISH,
    AFTER_DIRECTORY_FORCE,
    AFTER_RETIRE_DELETE,
    AFTER_RETIRE_DIRECTORY_FORCE
  }

  @FunctionalInterface
  interface FaultHook {
    void after(Stage stage, Path path) throws IOException;
  }
}
