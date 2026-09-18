package org.tron.core.services.admin.ipc.server;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.EnumSet;
import org.newsclub.net.unix.AFUNIXSocketAddress;
import org.tron.core.exception.TronError;
import org.tron.core.exception.TronError.ErrCode;

/**
 * Resolves and manages IPC socket files and their private directory.
 * The service owns the socket and decides when to prepare or remove these files.
 */
final class IpcSocketFiles {

  private static final String IPC_DIRECTORY_NAME = "ipc";

  // macOS/Linux sun_path buffers are 104/108 bytes. Reserve one byte for the terminating null
  // and three bytes of portability margin below the smaller macOS limit.
  private static final int MAX_SOCKET_PATH_BYTES = 100;

  Path resolveSocketFilePath(String outputDirectory, String configuredDirectory, String pid) {
    Path socketRootDirectory;
    if (configuredDirectory == null || configuredDirectory.trim().isEmpty()) {
      socketRootDirectory = Paths.get(outputDirectory);
    } else {
      socketRootDirectory = Paths.get(configuredDirectory);
      if (!socketRootDirectory.isAbsolute()) {
        throw new TronError("node.admin.ipc.socketDirectory must be an absolute path",
            ErrCode.API_SERVER_INIT);
      }
    }

    Path socketFile = socketRootDirectory.resolve(IPC_DIRECTORY_NAME)
        .resolve(pid + ".sock").toAbsolutePath().normalize();
    int socketPathLength = getSocketPathLength(socketFile);
    if (socketPathLength > MAX_SOCKET_PATH_BYTES) {
      throw new TronError("IPC socket path is " + socketPathLength
          + " bytes, exceeding the portable limit of " + MAX_SOCKET_PATH_BYTES
          + " bytes. Configure node.admin.ipc.socketDirectory to a shorter absolute directory",
          ErrCode.API_SERVER_INIT);
    }
    return socketFile;
  }

  private int getSocketPathLength(Path socketFile) {
    return getSocketPathLength(socketFile, AFUNIXSocketAddress.addressCharset());
  }

  static int getSocketPathLength(Path socketFile, Charset charset) {
    return socketFile.toString().getBytes(charset).length;
  }

  void validateSocketRootDirectory(Path socketRootDirectory) throws IOException {
    if (socketRootDirectory == null || !Files.isDirectory(socketRootDirectory)) {
      throw new TronError("IPC socket root directory does not exist or is not a directory",
          ErrCode.API_SERVER_INIT);
    }
    if (!Files.getFileStore(socketRootDirectory)
        .supportsFileAttributeView(PosixFileAttributeView.class)) {
      throw new TronError("IPC requires a POSIX-compatible socket root directory",
          ErrCode.API_SERVER_INIT);
    }
  }

  void recreateSocketDirectory(Path socketDirectory) throws IOException {
    if (Files.exists(socketDirectory, LinkOption.NOFOLLOW_LINKS)) {
      BasicFileAttributes attributes = Files.readAttributes(socketDirectory,
          BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
      if (attributes.isSymbolicLink() || !attributes.isDirectory()) {
        throw new TronError("Refusing to replace a non-directory IPC path",
            ErrCode.API_SERVER_INIT);
      }
      deleteDirectoryWithDirectEntries(socketDirectory);
    }
    Files.createDirectory(socketDirectory, PosixFilePermissions.asFileAttribute(
        EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.OWNER_EXECUTE)));
  }

  private void deleteDirectoryWithDirectEntries(Path directory) throws IOException {
    try (DirectoryStream<Path> entries = Files.newDirectoryStream(directory)) {
      for (Path entry : entries) {
        Files.delete(entry);
      }
    }
    Files.delete(directory);
  }

  void setOwnerOnlyPermissions(Path socketFilePath) throws IOException {
    Files.setPosixFilePermissions(socketFilePath,
        EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
  }

  void deleteSocketFile(Path socketFilePath) throws IOException {
    if (socketFilePath != null) {
      Files.deleteIfExists(socketFilePath);
    }
  }

  void deleteSocketDirectory(Path socketFilePath) throws IOException {
    if (socketFilePath != null) {
      Files.deleteIfExists(socketFilePath.getParent());
    }
  }
}
