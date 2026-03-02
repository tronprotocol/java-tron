package org.tron.common.utils;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.tron.common.utils.FileUtil.readData;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class FileUtilTest {
  private Path tempDir;

  @Before
  public void setUp() throws IOException {
    tempDir = Files.createTempDirectory("testDir");

    Files.createFile(tempDir.resolve("file1.txt"));
    Files.createFile(tempDir.resolve("file2.txt"));

    Path subDir = Files.createDirectory(tempDir.resolve("subdir"));
    Files.createFile(subDir.resolve("file3.txt"));
  }

  @After
  public void tearDown() throws IOException {
    Files.walk(tempDir)
        .sorted(Comparator.reverseOrder())
        .forEach(path -> {
          try {
            Files.delete(path);
          } catch (IOException e) {
            e.printStackTrace();
          }
        });
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
    Path tempFile = Files.createTempFile("testfile", ".txt");
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
  public void testCreateFileIfNotExists() {
    String existFile = "existsfile.txt";
    File file1 = new File(existFile);
    try {
      file1.createNewFile();
    } catch (IOException e) {
      System.out.println("ignore this exception.");
    }
    assertTrue(file1.exists());
    assertTrue(FileUtil.createDirIfNotExists(existFile));
    assertTrue(file1.exists());

    String notExistFile = "notexistsfile.txt";
    File file2 = new File(notExistFile);
    assertTrue(!file2.exists());
    assertTrue(FileUtil.createDirIfNotExists(notExistFile));
    assertTrue(file2.exists());
    file1.delete();
    file2.delete();
  }

  @Test
  public void testCreateDirIfNotExists() {
    String existDir = "existsdir";
    File fileDir1 = new File(existDir);
    fileDir1.mkdir();
    assertTrue(fileDir1.exists());
    assertTrue(FileUtil.createDirIfNotExists(existDir));
    assertTrue(fileDir1.exists());

    String notExistDir = "notexistsdir";
    File fileDir2 = new File(notExistDir);
    assertTrue(!fileDir2.exists());
    assertTrue(FileUtil.createDirIfNotExists(notExistDir));
    assertTrue(fileDir2.exists());
    fileDir1.delete();
    fileDir2.delete();
  }


}