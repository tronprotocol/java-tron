package org.tron.core.services.admin.ipc.server;

import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.tron.core.exception.TronError;

public class IpcSocketFilesTest {

  private final IpcSocketFiles socketFiles = new IpcSocketFiles();

  @Test
  public void testResolveSocketFilePathUsesOutputDirectory() throws Exception {
    String outputDirectory = "/tmp/node-output";

    for (String configuredDirectory : new String[] {null, "", " \t "}) {
      Path socketFilePath = socketFiles.resolveSocketFilePath(
          outputDirectory, configuredDirectory, "1234");
      Assert.assertEquals(Paths.get("/tmp/node-output", "ipc", "1234.sock"), socketFilePath);
    }
  }

  @Test
  public void testResolveSocketFilePathRejectsLongOutputPath() throws Exception {
    String outputDirectory = Paths.get("/tmp",
        "a-very-long-output-directory-name-that-makes-the-resulting-unix-domain-socket-path-"
            + "exceed-the-portable-limit").toString();

    try {
      socketFiles.resolveSocketFilePath(outputDirectory, null, "1234");
      Assert.fail("Expected an overlong IPC socket path to be rejected");
    } catch (TronError e) {
      Path expectedSocketFile = Paths.get(outputDirectory, "ipc", "1234.sock")
          .toAbsolutePath().normalize();
      Assert.assertTrue(e.getMessage().contains("exceeding the portable limit of 100 bytes"));
      Assert.assertTrue(e.getMessage().contains("node.admin.ipc.socketDirectory"));
      Assert.assertFalse(e.getMessage().contains(expectedSocketFile.toString()));
    }
  }

  @Test
  public void testSocketPathLengthCountsUtf8Bytes() {
    Path socketPath = Paths.get("/tmp/目录.sock");

    int encodedLength = IpcSocketFiles.getSocketPathLength(socketPath, StandardCharsets.UTF_8);

    Assert.assertEquals(socketPath.toString().getBytes(StandardCharsets.UTF_8).length,
        encodedLength);
    Assert.assertTrue(encodedLength > socketPath.toString().length());
  }

  @Test
  public void testResolveSocketFilePathUsesConfiguredDirectory() throws Exception {
    String outputDirectory = "node-output";
    String configuredDirectory = "/tmp/tron-ipc";

    Path socketFilePath = socketFiles.resolveSocketFilePath(
        outputDirectory, configuredDirectory, "1234");

    Assert.assertEquals(Paths.get("/tmp/tron-ipc/ipc/1234.sock"),
        socketFilePath);
  }

  @Test
  public void testResolveSocketFilePathRejectsRelativeConfiguredDirectory() throws Exception {
    String configuredDirectory = "relative-ipc";

    try {
      socketFiles.resolveSocketFilePath("unused", configuredDirectory, "1234");
      Assert.fail("Expected a relative IPC socket directory to be rejected");
    } catch (TronError e) {
      Assert.assertEquals("node.admin.ipc.socketDirectory must be an absolute path",
          e.getMessage());
    }
  }

  @Test
  public void testResolveSocketFilePathRejectsLongConfiguredDirectory() throws Exception {
    String outputDirectory = "/tmp";
    String configuredDirectory = Paths.get("/tmp",
        "a-very-long-explicit-ipc-directory-that-makes-the-resulting-unix-domain-socket-path-"
            + "exceed-the-portable-limit").toString();

    try {
      socketFiles.resolveSocketFilePath(outputDirectory, configuredDirectory, "1234");
      Assert.fail("Expected an overlong configured IPC socket path to be rejected");
    } catch (TronError e) {
      Path expectedSocketFile = Paths.get(configuredDirectory, "ipc", "1234.sock")
          .toAbsolutePath().normalize();
      Assert.assertTrue(e.getMessage().contains("exceeding the portable limit of 100 bytes"));
      Assert.assertTrue(e.getMessage().contains("node.admin.ipc.socketDirectory"));
      Assert.assertFalse(e.getMessage().contains(expectedSocketFile.toString()));
    }
  }

  @Test
  public void testValidateSocketRootDirectoryRejectsMissingDirectory() throws Exception {
    Path outputDirectory = Files.createTempDirectory("ipc-missing-output-test-");
    Files.delete(outputDirectory);

    try {
      socketFiles.validateSocketRootDirectory(outputDirectory);
      Assert.fail("Expected a missing output directory to be rejected");
    } catch (TronError e) {
      Assert.assertEquals("IPC socket root directory does not exist or is not a directory",
          e.getMessage());
    }
  }

  @Test
  public void testRecreateSocketDirectoryRejectsRegularFile() throws Exception {
    Path outputDirectory = Files.createTempDirectory("ipc-regular-file-test-");
    Path socketDirectory = outputDirectory.resolve("ipc");
    Files.createFile(socketDirectory);
    try {
      socketFiles.recreateSocketDirectory(socketDirectory);
      Assert.fail("Expected a regular file at the reserved directory path to be preserved");
    } catch (TronError e) {
      Assert.assertEquals("Refusing to replace a non-directory IPC path", e.getMessage());
      Assert.assertTrue(Files.isRegularFile(socketDirectory, LinkOption.NOFOLLOW_LINKS));
    } finally {
      Files.deleteIfExists(socketDirectory);
      Files.deleteIfExists(outputDirectory);
    }
  }

  @Test
  public void testRecreateSocketDirectoryRejectsSymbolicLink() throws Exception {
    assumePosixFileSystem();
    Path outputDirectory = Files.createTempDirectory("ipc-symbolic-link-test-");
    Path targetFile = outputDirectory.resolve("target");
    Path socketDirectory = outputDirectory.resolve("ipc");
    Files.createFile(targetFile);
    Files.createSymbolicLink(socketDirectory, targetFile.getFileName());
    try {
      socketFiles.recreateSocketDirectory(socketDirectory);
      Assert.fail("Expected a symbolic link to be preserved");
    } catch (TronError e) {
      Assert.assertEquals("Refusing to replace a non-directory IPC path", e.getMessage());
      Assert.assertTrue(Files.isSymbolicLink(socketDirectory));
      Assert.assertTrue(Files.exists(targetFile));
    } finally {
      Files.deleteIfExists(socketDirectory);
      Files.deleteIfExists(targetFile);
      Files.deleteIfExists(outputDirectory);
    }
  }

  @Test
  public void testRecreateSocketDirectoryRemovesStaleFilesAndUsesOwnerOnlyPermissions()
      throws Exception {
    assumePosixFileSystem();
    Path outputDirectory = Files.createTempDirectory("ipc-stale-directory-test-");
    Path socketDirectory = Files.createDirectory(outputDirectory.resolve("ipc"));
    Files.createFile(socketDirectory.resolve("1234.sock"));
    try {
      socketFiles.recreateSocketDirectory(socketDirectory);

      Assert.assertTrue(Files.isDirectory(socketDirectory));
      Assert.assertEquals(
          EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE,
              PosixFilePermission.OWNER_EXECUTE),
          Files.getPosixFilePermissions(socketDirectory));
      Assert.assertFalse(Files.exists(socketDirectory.resolve("1234.sock")));
    } finally {
      Files.deleteIfExists(socketDirectory);
      Files.deleteIfExists(outputDirectory);
    }
  }

  @Test
  public void testValidateSocketRootDirectorySupportsPosixPermissions() throws Exception {
    assumePosixFileSystem();
    Path outputDirectory = Files.createTempDirectory("ipc-posix-output-test-");
    try {
      socketFiles.validateSocketRootDirectory(outputDirectory);
    } finally {
      Files.deleteIfExists(outputDirectory);
    }
  }

  @Test
  public void testDeleteSocketFilesPreservesRootDirectory() throws Exception {
    Path root = Files.createTempDirectory("ipc-delete-test-");
    Path directory = Files.createDirectory(root.resolve("ipc"));
    Path socketFile = Files.createFile(directory.resolve("1234.sock"));
    try {
      socketFiles.deleteSocketFile(socketFile);
      Assert.assertFalse(Files.exists(socketFile));
      Assert.assertTrue(Files.isDirectory(directory));
      socketFiles.deleteSocketDirectory(socketFile);
      Assert.assertFalse(Files.exists(directory));
      Assert.assertTrue(Files.isDirectory(root));

      // Cleanup also runs before bind or after an earlier cleanup already removed the files.
      socketFiles.deleteSocketFile(socketFile);
      socketFiles.deleteSocketDirectory(socketFile);
      socketFiles.deleteSocketFile(null);
      socketFiles.deleteSocketDirectory(null);
      Assert.assertTrue(Files.isDirectory(root));
    } finally {
      Files.deleteIfExists(socketFile);
      Files.deleteIfExists(directory);
      Files.deleteIfExists(root);
    }
  }

  private void assumePosixFileSystem() {
    Assume.assumeTrue("IPC requires POSIX file permissions",
        FileSystems.getDefault().supportedFileAttributeViews().contains("posix"));
  }
}
