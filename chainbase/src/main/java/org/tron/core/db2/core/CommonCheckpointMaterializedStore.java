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
import java.util.EnumSet;
import java.util.Objects;
import org.tron.core.db2.core.CommonCheckpointMaterializer.Authority;

/** Central, bounded materialization records for all common-checkpoint authorities. */
public final class CommonCheckpointMaterializedStore {

  static final String DIRECTORY = "materialized";

  private final Path directory;
  private final FaultHook faultHook;
  private final EnumSet<Authority> sealed = EnumSet.noneOf(Authority.class);

  public CommonCheckpointMaterializedStore(Path checkpointDirectory) {
    this(checkpointDirectory, (stage, path) -> { });
  }

  CommonCheckpointMaterializedStore(Path checkpointDirectory, FaultHook faultHook) {
    this.directory = Objects.requireNonNull(checkpointDirectory, "checkpointDirectory")
        .toAbsolutePath().normalize().resolve(DIRECTORY);
    this.faultHook = Objects.requireNonNull(faultHook, "faultHook");
  }

  /** Returns whether this authority's single retained slot exactly matches {@code expected}. */
  public synchronized boolean matches(Authority authority, byte[] expected) throws IOException {
    Path path = path(authority);
    if (!exists(authority)) {
      return false;
    }
    requireRegular(path);
    byte[] actual = Files.readAllBytes(path);
    if (!Arrays.equals(actual, Objects.requireNonNull(expected, "expected"))) {
      return false;
    }
    seal(authority);
    return true;
  }

  public synchronized boolean exists(Authority authority) throws IOException {
    if (Files.exists(directory, LinkOption.NOFOLLOW_LINKS)
        && (Files.isSymbolicLink(directory)
        || !Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS))) {
      throw new IOException("common checkpoint materialized path is not a direct directory");
    }
    return Files.exists(path(authority), LinkOption.NOFOLLOW_LINKS);
  }

  /** Atomically replaces this authority's prior slot and durably retires its old target. */
  public synchronized void replace(Authority authority, byte[] encoded) throws IOException {
    byte[] admitted = Arrays.copyOf(Objects.requireNonNull(encoded, "encoded"), encoded.length);
    if (admitted.length == 0) {
      throw new IllegalArgumentException("materialized checkpoint record must not be empty");
    }
    requireDirectory();
    Path target = path(authority);
    if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
      requireRegular(target);
      if (Arrays.equals(Files.readAllBytes(target), admitted)) {
        seal(authority);
        return;
      }
    }
    sealed.remove(authority);
    Path temporary = directory.resolve("." + authority.name() + ".tmp");
    if (Files.deleteIfExists(temporary)) {
      syncDirectory();
    }
    try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.CREATE_NEW,
        StandardOpenOption.WRITE)) {
      ByteBuffer buffer = ByteBuffer.wrap(admitted);
      while (buffer.hasRemaining()) {
        channel.write(buffer);
      }
      channel.force(true);
    }
    faultHook.after(Stage.AFTER_TEMPORARY_FORCE, temporary);
    try {
      Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE,
          StandardCopyOption.REPLACE_EXISTING);
    } catch (AtomicMoveNotSupportedException unsupported) {
      throw new IOException("common checkpoint materialized store requires atomic replace",
          unsupported);
    }
    faultHook.after(Stage.AFTER_ATOMIC_REPLACE, target);
    syncDirectory();
    sealed.addAll(EnumSet.allOf(Authority.class));
    faultHook.after(Stage.AFTER_DIRECTORY_FORCE, target);
  }

  Path path(Authority authority) {
    return directory.resolve(Objects.requireNonNull(authority, "authority").name());
  }

  private void requireDirectory() throws IOException {
    if (Files.isSymbolicLink(directory)) {
      throw new IOException("common checkpoint materialized directory must not be a symlink");
    }
    Files.createDirectories(directory);
    if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) {
      throw new IOException("common checkpoint materialized path is not a directory");
    }
  }

  private void syncDirectory() throws IOException {
    try (FileChannel channel = FileChannel.open(directory, StandardOpenOption.READ)) {
      channel.force(true);
    }
  }

  private void seal(Authority authority) throws IOException {
    if (!sealed.contains(authority)) {
      syncDirectory();
      sealed.addAll(EnumSet.allOf(Authority.class));
    }
  }

  private static void requireRegular(Path path) throws IOException {
    if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
      throw new IOException("common checkpoint materialized slot is not a regular file");
    }
  }

  enum Stage {
    AFTER_TEMPORARY_FORCE,
    AFTER_ATOMIC_REPLACE,
    AFTER_DIRECTORY_FORCE
  }

  @FunctionalInterface
  interface FaultHook {
    void after(Stage stage, Path path) throws IOException;
  }
}
