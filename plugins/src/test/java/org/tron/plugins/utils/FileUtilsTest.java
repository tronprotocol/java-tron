package org.tron.plugins.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class FileUtilsTest {

  @Rule
  public final TemporaryFolder folder = new TemporaryFolder();

  @Test
  public void testCopyDatabasesCopiesTreeAfterMissingDirectory() throws IOException {
    Path src = createSourceTree();
    Path dest = folder.newFolder().toPath();

    FileUtils.copyDatabases(src, dest, Arrays.asList("missing", "database"));

    Assert.assertFalse(Files.exists(dest.resolve("missing")));
    Assert.assertArrayEquals(new byte[]{1, 2, 3},
        Files.readAllBytes(dest.resolve("database/nested/data")));
  }

  @Test
  public void testCopyMethodsSkipMissingSource() throws IOException {
    Path src = folder.newFolder().toPath();
    Path dest = folder.getRoot().toPath().resolve("missing-dest");

    FileUtils.copyDatabases(src, dest, Collections.singletonList("missing"));
    FileUtils.copyDir(src, dest, "missing");

    Assert.assertFalse(Files.exists(dest));
  }

  @Test
  public void testCopyDatabasesDoesNotCreateDestinationParents() throws IOException {
    Path src = createSourceTree();
    Path dest = folder.getRoot().toPath().resolve("missing-parent/dest");

    RuntimeException error = Assert.assertThrows(RuntimeException.class,
        () -> FileUtils.copyDatabases(src, dest, Collections.singletonList("database")));

    Assert.assertTrue(error.getCause() instanceof IOException);
    Assert.assertFalse(Files.exists(dest.getParent()));
  }

  @Test
  public void testCopyDirCreatesDestinationParents() throws IOException {
    Path src = createSourceTree();
    Path dest = folder.getRoot().toPath().resolve("missing-parent/dest");

    FileUtils.copyDir(src, dest, "database");

    Assert.assertArrayEquals(new byte[]{1, 2, 3},
        Files.readAllBytes(dest.resolve("database/nested/data")));
  }

  @Test
  public void testCopyMethodsPreserveLeadingSeparatorInDirectory() throws IOException {
    Path src = createSourceTree();
    Path databasesDest = folder.newFolder().toPath();
    Path directoryDest = folder.newFolder().toPath();
    String dir = File.separator + "database";

    FileUtils.copyDatabases(src, databasesDest, Collections.singletonList(dir));
    FileUtils.copyDir(src, directoryDest, dir);

    Assert.assertArrayEquals(new byte[]{1, 2, 3},
        Files.readAllBytes(databasesDest.resolve("database/nested/data")));
    Assert.assertArrayEquals(new byte[]{1, 2, 3},
        Files.readAllBytes(directoryDest.resolve("database/nested/data")));
  }

  @Test
  public void testCopyDirWrapsDestinationCreationFailure() throws IOException {
    Path src = createSourceTree();
    Path dest = folder.newFile().toPath();

    RuntimeException error = Assert.assertThrows(RuntimeException.class,
        () -> FileUtils.copyDir(src, dest, "database"));

    Assert.assertTrue(error.getCause() instanceof IOException);
    Assert.assertTrue(error.getCause().getMessage().contains("create fail"));
  }

  private Path createSourceTree() throws IOException {
    Path src = folder.newFolder().toPath();
    Path nested = Files.createDirectories(src.resolve("database/nested"));
    Files.write(nested.resolve("data"), new byte[]{1, 2, 3});
    return src;
  }
}
