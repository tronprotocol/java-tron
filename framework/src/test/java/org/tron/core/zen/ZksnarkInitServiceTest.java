package org.tron.core.zen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Test;
import org.tron.core.exception.TronError;

public class ZksnarkInitServiceTest {

  private static final String TEST_PARAM_FILE = "test-zksnark.params";
  private static final Set<PosixFilePermission> OWNER_ONLY =
      EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);

  private final List<Path> tempFiles = new ArrayList<>();

  @After
  public void tearDown() throws IOException {
    for (Path tempFile : tempFiles) {
      Files.deleteIfExists(tempFile);
    }
  }

  @Test
  public void testGetParamsFileCreatesSecureTempFile() throws IOException {
    Path file = track(ZksnarkInitService.getParamsFile(TEST_PARAM_FILE));

    assertTrue(Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS));
    assertFalse(Files.isSymbolicLink(file));
    if (Files.getFileStore(file).supportsFileAttributeView(PosixFileAttributeView.class)) {
      assertEquals(OWNER_ONLY, Files.getPosixFilePermissions(file));
    }
    assertTrue(file.getFileName().toString().startsWith("test-zksnark-"));
    assertTrue(file.getFileName().toString().endsWith(".params"));

    try (InputStream expected = ZksnarkInitService.class
        .getResourceAsStream("/params/" + TEST_PARAM_FILE);
        InputStream actual = Files.newInputStream(file)) {
      assertTrue(IOUtils.contentEquals(expected, actual));
    }
  }

  @Test
  public void testGetParamsFileUsesUniqueNames() {
    Path first = track(ZksnarkInitService.getParamsFile(TEST_PARAM_FILE));
    Path second = track(ZksnarkInitService.getParamsFile(TEST_PARAM_FILE));

    assertNotEquals(first, second);
  }

  @Test
  public void testGetParamsFileMissingResourceThrows() {
    TronError exception = assertThrows(TronError.class,
        () -> ZksnarkInitService.getParamsFile("does-not-exist.params"));

    assertEquals(TronError.ErrCode.ZCASH_INIT, exception.getErrCode());
  }

  @Test
  public void testCopyFailureDeletesPartialFile() throws IOException {
    String fileName = "failing-zksnark-" + System.nanoTime() + ".params";
    Set<Path> filesBefore = findTempFiles(fileName);

    InputStream failingInput = new InputStream() {
      private boolean firstRead = true;

      @Override
      public int read() throws IOException {
        if (firstRead) {
          firstRead = false;
          return 1;
        }
        throw new IOException("expected copy failure");
      }

      @Override
      public int read(byte[] buffer, int offset, int length) throws IOException {
        if (firstRead) {
          firstRead = false;
          buffer[offset] = 1;
          return 1;
        }
        throw new IOException("expected copy failure");
      }
    };

    assertThrows(IOException.class,
        () -> ZksnarkInitService.copyToSecureTempFile(failingInput, fileName));
    assertEquals(filesBefore, findTempFiles(fileName));
  }

  private Path track(String fileName) {
    Path file = Paths.get(fileName);
    tempFiles.add(file);
    return file;
  }

  private static Set<Path> findTempFiles(String fileName) throws IOException {
    int dotIndex = fileName.lastIndexOf('.');
    String prefix = (dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName) + "-";
    Path tempDirectory = Paths.get(System.getProperty("java.io.tmpdir"));
    try (Stream<Path> files = Files.list(tempDirectory)) {
      return files
          .filter(path -> path.getFileName().toString().startsWith(prefix))
          .collect(Collectors.toSet());
    }
  }
}
