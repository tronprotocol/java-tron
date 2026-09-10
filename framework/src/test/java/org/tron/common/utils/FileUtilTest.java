package org.tron.common.utils;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.tron.common.utils.FileUtil.readData;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class FileUtilTest {
  @Rule
  public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  private Path tempDir;

  @Before
  public void setUp() throws IOException {
    tempDir = temporaryFolder.newFolder("testDir").toPath();

    Files.createFile(tempDir.resolve("file1.txt"));
    Files.createFile(tempDir.resolve("file2.txt"));

    Path subDir = Files.createDirectory(tempDir.resolve("subdir"));
    Files.createFile(subDir.resolve("file3.txt"));
  }

  @Test
  public void testRecursiveList() throws IOException {
    List<String> files = FileUtil.recursiveList(tempDir.toString());

    assertTrue(files.contains(tempDir.resolve("file1.txt").toString()));
    assertTrue(files.contains(tempDir.resolve("file2.txt").toString()));
    assertTrue(files.contains(tempDir.resolve("subdir").resolve("file3.txt").toString()));

    assertEquals(3, files.size());
  }

  @Test
  public void testReadData_NormalFile() throws IOException {
    Path tempFile = Files.createFile(tempDir.resolve("testfile.txt"));
    try (FileWriter writer = new FileWriter(tempFile.toFile())) {
      writer.write("Hello, World!");
    }

    char[] buffer = new char[1024];
    int len = readData(tempFile.toString(), buffer);

    assertEquals(13, len);
    assertArrayEquals("Hello, World!".toCharArray(), Arrays.copyOf(buffer, 13));
  }

  @Test
  public void testReadData_IOException() {
    char[] buffer = new char[1024];
    File dir = new File(System.getProperty("java.io.tmpdir"));
    int len = readData(dir.getAbsolutePath(), buffer);
    assertEquals(0, len);
  }


  @Test
  public void testCreateFileIfNotExists() throws IOException {
    String existFile = tempDir.resolve("existsfile.txt").toString();
    File file1 = new File(existFile);
    assertTrue(file1.createNewFile());
    assertTrue(file1.exists());
    assertTrue(FileUtil.createDirIfNotExists(existFile));
    assertTrue(file1.exists());

    String notExistFile = tempDir.resolve("notexistsfile.txt").toString();
    File file2 = new File(notExistFile);
    assertTrue(!file2.exists());
    assertTrue(FileUtil.createDirIfNotExists(notExistFile));
    assertTrue(file2.exists());
  }

  @Test
  public void testCreateDirIfNotExists() {
    String existDir = tempDir.resolve("existsdir").toString();
    File fileDir1 = new File(existDir);
    fileDir1.mkdir();
    assertTrue(fileDir1.exists());
    assertTrue(FileUtil.createDirIfNotExists(existDir));
    assertTrue(fileDir1.exists());

    String notExistDir = tempDir.resolve("notexistsdir").toString();
    File fileDir2 = new File(notExistDir);
    assertTrue(!fileDir2.exists());
    assertTrue(FileUtil.createDirIfNotExists(notExistDir));
    assertTrue(fileDir2.exists());
  }


}