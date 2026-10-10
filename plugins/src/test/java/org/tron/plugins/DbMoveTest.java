package org.tron.plugins;

import com.typesafe.config.ConfigException;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBar;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.rocksdb.RocksDBException;
import org.tron.plugins.utils.DBUtils;
import org.tron.plugins.utils.FileUtils;
import org.tron.plugins.utils.db.DbTool;
import picocli.CommandLine;

@Slf4j
public class DbMoveTest {

  private static final String OUTPUT_DIRECTORY = "output-directory-toolkit";

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private static final String ACCOUNT = "account";
  private static final String TRANS = "trans";


  private void init(DbTool.DbType dbType, String path) throws IOException, RocksDBException {
    DbTool.getDB(path, ACCOUNT, dbType).close();
    DbTool.getDB(path, DBUtils.MARKET_PAIR_PRICE_TO_ORDER, dbType).close();
    DbTool.getDB(path, TRANS, dbType).close();
  }

  @After
  public void destroy() {
    deleteDir(new File(OUTPUT_DIRECTORY));
  }

  /**
   * delete directory.
   */
  private static boolean deleteDir(File dir) {
    if (dir.isDirectory()) {
      String[] children = dir.list();
      assert children != null;
      for (String child : children) {
        boolean success = deleteDir(new File(dir, child));
        if (!success) {
          logger.warn("can't delete dir:" + dir);
          return false;
        }
      }
    }
    return dir.delete();
  }

  private static String getConfig(String config) {
    URL path = DbMoveTest.class.getClassLoader().getResource(config);
    return path == null ? null : path.getPath();
  }

  /** Create and initialize a RocksDB database folder. */
  private File newDatabase() throws IOException, RocksDBException {
    File database = temporaryFolder.newFolder("database");
    init(DbTool.DbType.RocksDB, database.getPath());
    return database;
  }

  private static String[] mvArgs(File database, String configPath) {
    return new String[] {"db", "mv", "-d", database.getParent(), "-c", configPath};
  }

  /** Run {@code db mv} with a fresh CommandLine and return the exit code. */
  private static int mv(File database, String configPath) {
    return new CommandLine(new Toolkit()).execute(mvArgs(database, configPath));
  }

  private static int mv(File database, String configPath, StringWriter err) {
    CommandLine cli = new CommandLine(new Toolkit());
    cli.setErr(new PrintWriter(err));
    return cli.execute(mvArgs(database, configPath));
  }

  private File writeConfig(String fileName, String[]... entries) throws IOException {
    StringBuilder content = new StringBuilder("storage {\n  properties = [\n");
    for (String[] entry : entries) {
      content.append("    {\n      name = \"").append(entry[0])
          .append("\",\n      path = \"").append(entry[1]).append("\",\n    },\n");
    }
    content.append("  ]\n}\n");
    return writeConfig(fileName, content.toString());
  }

  private File writeConfig(String fileName, String content) throws IOException {
    File config = temporaryFolder.newFile(fileName);
    Files.write(config.toPath(), content.getBytes(StandardCharsets.UTF_8));
    return config;
  }

  private void assertConfigRejected(String content) throws RocksDBException, IOException {
    File database = newDatabase();
    File accountDir = Paths.get(database.getPath(), ACCOUNT).toFile();
    Map<String, String> accountBefore = snapshot(accountDir);
    File config = writeConfig("invalid.conf", content);

    StringWriter err = new StringWriter();
    Assert.assertEquals(2, mv(database, config.getPath(), err));
    Assert.assertTrue(err.toString().contains(ConfigException.WrongType.class.getName()));
    assertUntouched(accountDir);
    Assert.assertEquals(accountBefore, snapshot(accountDir));
    Assert.assertFalse(Paths.get(OUTPUT_DIRECTORY, "dest").toFile().exists());
  }

  private static void assertUntouched(File source) {
    Assert.assertTrue(source.isDirectory());
    Assert.assertFalse(Files.isSymbolicLink(source.toPath()));
  }

  private static File destination(String name) {
    return Paths.get(OUTPUT_DIRECTORY, "dest", "database", name).toFile();
  }

  private static Map<String, String> snapshot(File dir) throws IOException {
    Path root = dir.toPath();
    List<Path> files;
    try (Stream<Path> paths = Files.walk(root)) {
      files = paths.filter(Files::isRegularFile).collect(Collectors.toList());
    }
    Map<String, String> contents = new TreeMap<>();
    for (Path file : files) {
      contents.put(root.relativize(file).toString(),
          Base64.getEncoder().encodeToString(Files.readAllBytes(file)));
    }
    return contents;
  }

  @Test
  public void testMvForLevelDB() throws RocksDBException, IOException {
    File database = temporaryFolder.newFolder("database");
    init(DbTool.DbType.LevelDB, Paths.get(database.getPath()).toString());
    String[] args = new String[] {"db", "mv", "-d",
        database.getParent(), "-c",
        getConfig("config.conf")};
    CommandLine cli = new CommandLine(new Toolkit());
    Assert.assertEquals(0, cli.execute(args));
    Assert.assertEquals(2, cli.execute(args));
  }

  @Test
  public void testMvForRocksDB() throws RocksDBException, IOException {
    File database = newDatabase();
    Assert.assertEquals(0, mv(database, getConfig("config.conf")));
    Assert.assertEquals(2, mv(database, getConfig("config.conf")));
  }

  @Test
  public void testSourceKeptWhenCopyFails() throws RocksDBException, IOException {
    File database = newDatabase();
    File accountDir = Paths.get(database.getPath(), ACCOUNT).toFile();
    File marketDir = Paths.get(database.getPath(), DBUtils.MARKET_PAIR_PRICE_TO_ORDER).toFile();
    Map<String, String> accountBefore = snapshot(accountDir);
    Map<String, String> marketBefore = snapshot(marketDir);
    File victim = Objects.requireNonNull(marketDir.listFiles(File::isFile))[0];
    Assert.assertTrue(victim.setReadable(false, false));

    String[] args = mvArgs(database, getConfig("config.conf"));
    CommandLine cli = new CommandLine(new Toolkit());
    StringWriter output = new StringWriter();
    cli.setOut(new PrintWriter(output));
    try {
      Assume.assumeFalse("file still readable (root?), cannot simulate copy failure",
          victim.canRead());
      Assert.assertEquals(1, cli.execute(args));
    } finally {
      victim.setReadable(true, false);
    }
    Assert.assertFalse(output.toString().contains("move db done."));
    assertUntouched(accountDir);
    assertUntouched(marketDir);
    Assert.assertEquals(accountBefore, snapshot(accountDir));
    Assert.assertEquals(marketBefore, snapshot(marketDir));
    Assert.assertFalse(destination(ACCOUNT).exists());
    Assert.assertFalse(destination(DBUtils.MARKET_PAIR_PRICE_TO_ORDER).exists());

    Assert.assertEquals(0, cli.execute(args));
    Assert.assertTrue(Files.isSymbolicLink(accountDir.toPath()));
    Assert.assertTrue(Files.isSymbolicLink(marketDir.toPath()));
    Assert.assertEquals("move db done." + System.lineSeparator(), output.toString());
  }

  @Test
  public void testOptionOrderConfigFirst() throws RocksDBException, IOException {
    File database = newDatabase();
    // '-c' parsed before '-d': path validation must still use the final
    // database value, not the stale one visible at conversion time.
    String[] args = new String[] {"db", "mv", "-c",
        getConfig("config.conf"), "-d",
        database.getParent()};
    CommandLine cli = new CommandLine(new Toolkit());
    Assert.assertEquals(0, cli.execute(args));
    Assert.assertTrue(Files.isSymbolicLink(
        Paths.get(database.getPath(), ACCOUNT)));
    Assert.assertTrue(Files.isSymbolicLink(
        Paths.get(database.getPath(), DBUtils.MARKET_PAIR_PRICE_TO_ORDER)));
  }

  @Test
  public void testInTreeSymlinkRejected() throws RocksDBException, IOException {
    File database = newDatabase();
    File accountDir = Paths.get(database.getPath(), ACCOUNT).toFile();
    File outside = temporaryFolder.newFolder("outside");
    File sentinel = new File(outside, "sentinel");
    Assert.assertTrue(sentinel.createNewFile());
    Files.createSymbolicLink(
        Paths.get(accountDir.getPath(), "evil-link"), outside.toPath());

    Assert.assertEquals(1, mv(database, getConfig("config.conf")));
    // The move must fail without touching the source or the symlink target.
    Assert.assertTrue("source dir must be kept", accountDir.exists());
    Assert.assertFalse("source must not be replaced by a symlink",
        Files.isSymbolicLink(accountDir.toPath()));
    Assert.assertTrue("symlink target must never be touched", sentinel.exists());
    Assert.assertFalse("partial destination must be rolled back",
        destination(ACCOUNT).exists());
  }

  @Test
  public void testNestedDirsAndFilesPreserved() throws RocksDBException, IOException {
    File database = newDatabase();
    File emptySub = Paths.get(database.getPath(), ACCOUNT, "archive", "sub").toFile();
    Assert.assertTrue(emptySub.mkdirs());
    byte[] payload = {1, 2, 3};
    Files.write(Paths.get(database.getPath(), ACCOUNT, "archive", "keep.dat"), payload);

    Assert.assertEquals(0, mv(database, getConfig("config.conf")));
    Assert.assertTrue("empty nested dirs must be recreated at the destination",
        Paths.get(OUTPUT_DIRECTORY, "dest", "database", ACCOUNT, "archive", "sub")
            .toFile().isDirectory());
    Assert.assertArrayEquals("files inside sub-directories must be copied",
        payload, Files.readAllBytes(
            Paths.get(OUTPUT_DIRECTORY, "dest", "database", ACCOUNT, "archive", "keep.dat")));
  }

  @Test
  public void testDestinationCreateFails() throws RocksDBException, IOException {
    File database = newDatabase();
    File accountDir = Paths.get(database.getPath(), ACCOUNT).toFile();
    File destParent = Paths.get(OUTPUT_DIRECTORY, "dest", "database").toFile();
    Assert.assertTrue(destParent.mkdirs());
    Assert.assertTrue(destParent.setWritable(false, false));
    try {
      // Skip when the platform ignores the write bit (e.g. running as root).
      Assume.assumeFalse("dir still writable (root?), cannot simulate mkdirs failure",
          destParent.canWrite());
      Assert.assertEquals(1, mv(database, getConfig("config.conf")));
      Assert.assertTrue("source dir must be kept", accountDir.exists());
      Assert.assertFalse("source must not be replaced by a symlink",
          Files.isSymbolicLink(accountDir.toPath()));
    } finally {
      destParent.setWritable(true, false);
    }
  }

  @Test
  public void testUnreadableSubdirFailsCopy() throws RocksDBException, IOException {
    File database = newDatabase();
    File accountDir = Paths.get(database.getPath(), ACCOUNT).toFile();
    File subDir = new File(accountDir, "subdir");
    Assert.assertTrue(subDir.mkdir());
    Assert.assertTrue(new File(subDir, "data").createNewFile());
    Assert.assertTrue(subDir.setReadable(false, false));
    try {
      // Skip when the platform ignores the read bit (e.g. running as root).
      Assume.assumeFalse("subdir still readable (root?), cannot simulate traversal failure",
          subDir.canRead());
      Assert.assertEquals(1, mv(database, getConfig("config.conf")));
      Assert.assertTrue("source dir must be kept on traversal failure", accountDir.exists());
      Assert.assertFalse("source must not be replaced by a symlink",
          Files.isSymbolicLink(accountDir.toPath()));
      Assert.assertFalse("partial destination must be rolled back",
          destination(ACCOUNT).exists());
    } finally {
      subDir.setReadable(true, false);
    }

    // The rollback must leave a directly retryable state: no copy task still in
    // flight when the traversal failed may recreate the destination afterwards.
    Assert.assertEquals(0, mv(database, getConfig("config.conf")));
    Assert.assertTrue(Files.isSymbolicLink(accountDir.toPath()));
    Assert.assertTrue(Files.isSymbolicLink(
        Paths.get(database.getPath(), DBUtils.MARKET_PAIR_PRICE_TO_ORDER)));
  }

  @Test
  public void testDanglingDestinationLinkRejected() throws RocksDBException, IOException {
    File database = newDatabase();
    File destParent = Paths.get(OUTPUT_DIRECTORY, "dest", "database").toFile();
    Assert.assertTrue(destParent.mkdirs());
    // A dangling symlink occupies the destination: File.exists() reports it as
    // absent, but mkdirs would fail on it forever. Validation must fail closed.
    Path danglingLink = Paths.get(destParent.getPath(), ACCOUNT);
    Files.createSymbolicLink(danglingLink,
        Paths.get(destParent.getPath(), "no-such-target"));

    Assert.assertEquals(2, mv(database, getConfig("config.conf")));
    Assert.assertTrue("dangling link must be reported, not treated as absent",
        Files.isSymbolicLink(danglingLink));
    Assert.assertFalse("nothing may be moved",
        Files.isSymbolicLink(Paths.get(database.getPath(), ACCOUNT)));
  }

  @Test
  public void testRecoveryHintWhenSourceDeleteFails() throws RocksDBException, IOException {
    File database = newDatabase();
    File accountDir = Paths.get(database.getPath(), ACCOUNT).toFile();
    Assert.assertTrue(accountDir.setWritable(false, false));

    StringWriter err = new StringWriter();
    CommandLine cli = new CommandLine(new Toolkit());
    cli.setErr(new PrintWriter(err));
    try {
      // Skip when the platform ignores the write bit (e.g. running as root).
      Assume.assumeFalse("dir still writable (root?), cannot simulate delete failure",
          accountDir.canWrite());
      Assert.assertEquals(1, cli.execute(mvArgs(database, getConfig("config.conf"))));

      // Copy succeeded but finalization failed: source kept, complete copy kept.
      Assert.assertTrue("source dir must be kept", accountDir.exists());
      Assert.assertFalse("source must not be replaced by a symlink",
          Files.isSymbolicLink(accountDir.toPath()));
      File dest = destination(ACCOUNT);
      Assert.assertTrue("complete copy must be kept for manual recovery", dest.exists());
      String expectedHint = String.format(
          "To recover manually: remove %s if present, then create a symbolic link at %s"
              + " pointing to %s.",
          accountDir.getCanonicalFile().toPath(),
          accountDir.getCanonicalFile().toPath(),
          dest.getCanonicalFile().toPath());
      Assert.assertTrue("operator must get exact recovery instructions with real paths",
          err.toString().contains(expectedHint));
      // Finalization continues for the remaining dbs.
      Assert.assertTrue(Files.isSymbolicLink(
          Paths.get(database.getPath(), DBUtils.MARKET_PAIR_PRICE_TO_ORDER)));
    } finally {
      accountDir.setWritable(true, false);
    }
  }

  @Test
  public void testDestinationInsideOwnSourceRejected() throws RocksDBException, IOException {
    File database = newDatabase();
    File accountDir = Paths.get(database.getPath(), ACCOUNT).toFile();
    File config = writeConfig("self-nested.conf", new String[] {ACCOUNT, accountDir.getPath()});

    Assert.assertEquals(2, mv(database, config.getPath()));
    assertUntouched(accountDir);
    Assert.assertFalse(new File(accountDir, "database").exists());
  }

  @Test
  public void testDestinationInsideOtherSourceRejected() throws RocksDBException, IOException {
    File database = newDatabase();
    File accountDir = Paths.get(database.getPath(), ACCOUNT).toFile();
    File transDir = Paths.get(database.getPath(), TRANS).toFile();
    String[] trans = {TRANS, OUTPUT_DIRECTORY + "/dest"};
    String[] account = {ACCOUNT, new File(transDir, "nested").getPath()};
    File transFirst = writeConfig("trans-first.conf", trans, account);
    File accountFirst = writeConfig("account-first.conf", account, trans);

    Assert.assertEquals(2, mv(database, transFirst.getPath()));
    Assert.assertEquals(2, mv(database, accountFirst.getPath()));
    assertUntouched(accountDir);
    assertUntouched(transDir);
    Assert.assertFalse(new File(transDir, "nested").exists());
    Assert.assertFalse(Paths.get(OUTPUT_DIRECTORY, "dest").toFile().exists());
  }

  @Test
  public void testOverlappingDestinationsRejected() throws RocksDBException, IOException {
    File database = newDatabase();
    File accountDir = Paths.get(database.getPath(), ACCOUNT).toFile();
    File transDir = Paths.get(database.getPath(), TRANS).toFile();
    Files.write(Paths.get(accountDir.getPath(), "data"), new byte[] {1});
    Path nestedData = Paths.get(transDir.getPath(), "database", ACCOUNT, "data");
    Files.createDirectories(nestedData.getParent());
    Files.write(nestedData, new byte[] {2});
    Map<String, String> accountBefore = snapshot(accountDir);
    Map<String, String> transBefore = snapshot(transDir);
    String[] account = {ACCOUNT, destination(TRANS).getPath()};
    String[] trans = {TRANS, OUTPUT_DIRECTORY + "/dest"};
    File accountFirst = writeConfig("overlap-account-first.conf", account, trans);
    File transFirst = writeConfig("overlap-trans-first.conf", trans, account);
    String rejection = String.format("destination [%s] can not be inside destination [%s]",
        new File(destination(TRANS), "database/" + ACCOUNT).getCanonicalPath(),
        destination(TRANS).getCanonicalPath());

    StringWriter accountFirstErr = new StringWriter();
    Assert.assertEquals(2, mv(database, accountFirst.getPath(), accountFirstErr));
    Assert.assertTrue(accountFirstErr.toString().contains(rejection));
    StringWriter transFirstErr = new StringWriter();
    Assert.assertEquals(2, mv(database, transFirst.getPath(), transFirstErr));
    Assert.assertTrue(transFirstErr.toString().contains(rejection));
    assertUntouched(accountDir);
    assertUntouched(transDir);
    Assert.assertEquals(accountBefore, snapshot(accountDir));
    Assert.assertEquals(transBefore, snapshot(transDir));
    Assert.assertFalse(Paths.get(OUTPUT_DIRECTORY, "dest").toFile().exists());
  }

  @Test
  public void testDestinationAppearingAfterValidationKept() throws RocksDBException, IOException {
    File database = newDatabase();
    File accountDir = Paths.get(database.getPath(), ACCOUNT).toFile();
    File marketDir = Paths.get(database.getPath(), DBUtils.MARKET_PAIR_PRICE_TO_ORDER).toFile();
    Map<String, String> accountBefore = snapshot(accountDir);
    Path sentinel = destination(ACCOUNT).toPath().resolve("sentinel");
    byte[] payload = {7};

    CommandLine cli = new CommandLine(new Toolkit());
    StringWriter output = new StringWriter();
    StringWriter error = new StringWriter();
    cli.setOut(new PrintWriter(output));
    cli.setErr(new PrintWriter(error));
    try (MockedStatic<FileUtils> fileUtils =
        Mockito.mockStatic(FileUtils.class, Mockito.CALLS_REAL_METHODS)) {
      fileUtils.when(() -> FileUtils.isSymbolicLink(
          ArgumentMatchers.argThat(f -> f.getName().equals(marketDir.getName()))))
          .thenAnswer(invocation -> {
            Files.createDirectories(sentinel.getParent());
            Files.write(sentinel, payload);
            return invocation.callRealMethod();
          });
      Assert.assertEquals(1, cli.execute(mvArgs(database, getConfig("config.conf"))));
    }
    Assert.assertFalse(output.toString().contains("move db done."));
    assertUntouched(accountDir);
    assertUntouched(marketDir);
    Assert.assertEquals(accountBefore, snapshot(accountDir));
    Assert.assertEquals(Collections.singletonMap("sentinel",
        Base64.getEncoder().encodeToString(payload)), snapshot(destination(ACCOUNT)));
    Assert.assertFalse(destination(DBUtils.MARKET_PAIR_PRICE_TO_ORDER).exists());
    Assert.assertTrue(error.toString().contains(destination(ACCOUNT).getCanonicalPath()
        + " was not created by this run and was kept"));
    Assert.assertTrue(error.toString().contains("some destinations remain"));
  }

  @Test
  public void testFileCollisionInsideFreshDestinationFails()
      throws RocksDBException, IOException {
    File database = newDatabase();
    File accountDir = Paths.get(database.getPath(), ACCOUNT).toFile();
    Map<String, String> accountBefore = snapshot(accountDir);
    Path accountDest = destination(ACCOUNT).getCanonicalFile().toPath();
    String collision = Objects.requireNonNull(accountDir.listFiles(File::isFile))[0].getName();

    try (MockedStatic<Files> files = Mockito.mockStatic(Files.class, Mockito.CALLS_REAL_METHODS)) {
      files.when(() -> Files.createDirectory(ArgumentMatchers.eq(accountDest)))
          .thenAnswer(invocation -> {
            Path created = (Path) invocation.callRealMethod();
            Files.write(created.resolve(collision), new byte[] {7});
            return created;
          });
      Assert.assertEquals(1, mv(database, getConfig("config.conf")));
    }
    assertUntouched(accountDir);
    Assert.assertEquals(accountBefore, snapshot(accountDir));
    Assert.assertFalse(destination(ACCOUNT).exists());
  }

  @Test
  public void testAliasedSourceRejected() throws RocksDBException, IOException {
    File database = newDatabase();
    File accountDir = Paths.get(database.getPath(), ACCOUNT).toFile();
    Map<String, String> accountBefore = snapshot(accountDir);
    String rejection = String.format("original [%1$s] can not be inside original [%1$s]",
        accountDir.getCanonicalPath());

    String[] aliases = {ACCOUNT + "/", "./" + ACCOUNT};
    for (int i = 0; i < aliases.length; i++) {
      File config = writeConfig("alias-" + i + ".conf",
          new String[] {ACCOUNT, OUTPUT_DIRECTORY + "/dest"},
          new String[] {aliases[i], OUTPUT_DIRECTORY + "/dest2"});
      StringWriter err = new StringWriter();
      Assert.assertEquals(2, mv(database, config.getPath(), err));
      Assert.assertTrue(err.toString().contains(rejection));
    }
    assertUntouched(accountDir);
    Assert.assertEquals(accountBefore, snapshot(accountDir));
    Assert.assertFalse(Paths.get(OUTPUT_DIRECTORY, "dest").toFile().exists());
    Assert.assertFalse(Paths.get(OUTPUT_DIRECTORY, "dest2").toFile().exists());
  }

  @Test
  public void testLeftoverReportedWhenCleanupFails() throws RocksDBException, IOException {
    File database = newDatabase();
    File accountDir = Paths.get(database.getPath(), ACCOUNT).toFile();
    File marketDir = Paths.get(database.getPath(), DBUtils.MARKET_PAIR_PRICE_TO_ORDER).toFile();
    File accountDest = destination(ACCOUNT).getCanonicalFile();
    Map<String, String> accountBefore = snapshot(accountDir);
    Map<String, String> marketBefore = snapshot(marketDir);
    File victim = Objects.requireNonNull(marketDir.listFiles(File::isFile))[0];
    Assert.assertTrue(victim.setReadable(false, false));

    CommandLine cli = new CommandLine(new Toolkit());
    StringWriter output = new StringWriter();
    StringWriter error = new StringWriter();
    cli.setOut(new PrintWriter(output));
    cli.setErr(new PrintWriter(error));
    try (MockedStatic<FileUtils> fileUtils =
        Mockito.mockStatic(FileUtils.class, Mockito.CALLS_REAL_METHODS)) {
      Assume.assumeFalse("file still readable (root?), cannot simulate copy failure",
          victim.canRead());
      fileUtils.when(() -> FileUtils.deleteDir(ArgumentMatchers.argThat(accountDest::equals)))
          .thenReturn(false);
      Assert.assertEquals(1, cli.execute(mvArgs(database, getConfig("config.conf"))));
    } finally {
      victim.setReadable(true, false);
    }
    Assert.assertFalse(output.toString().contains("move db done."));
    assertUntouched(accountDir);
    assertUntouched(marketDir);
    Assert.assertEquals(accountBefore, snapshot(accountDir));
    Assert.assertEquals(marketBefore, snapshot(marketDir));
    Assert.assertTrue(accountDest.isDirectory());
    Assert.assertFalse(destination(DBUtils.MARKET_PAIR_PRICE_TO_ORDER).exists());
    Assert.assertTrue(error.toString().contains(accountDest + " cleanup failed"));
    Assert.assertTrue(error.toString().contains("some destinations remain"));
  }

  @Test
  public void testRecoveryHintWhenLinkCreationFails() throws RocksDBException, IOException {
    File database = newDatabase();
    File accountDir = Paths.get(database.getPath(), ACCOUNT).toFile().getCanonicalFile();
    File marketDir = Paths.get(database.getPath(), DBUtils.MARKET_PAIR_PRICE_TO_ORDER).toFile();
    File accountDest = destination(ACCOUNT).getCanonicalFile();
    Map<String, String> accountBefore = snapshot(accountDir);

    CommandLine cli = new CommandLine(new Toolkit());
    StringWriter output = new StringWriter();
    StringWriter error = new StringWriter();
    cli.setOut(new PrintWriter(output));
    cli.setErr(new PrintWriter(error));
    try (MockedStatic<FileUtils> fileUtils =
        Mockito.mockStatic(FileUtils.class, Mockito.CALLS_REAL_METHODS)) {
      fileUtils.when(() -> FileUtils.deleteDir(ArgumentMatchers.argThat(accountDir::equals)))
          .thenAnswer(invocation -> {
            boolean deleted = (boolean) invocation.callRealMethod();
            Files.createFile(accountDir.toPath());
            return deleted;
          });
      Assert.assertEquals(1, cli.execute(mvArgs(database, getConfig("config.conf"))));
    }
    Assert.assertFalse(output.toString().contains("move db done."));
    Assert.assertEquals(accountBefore, snapshot(accountDest));
    Assert.assertTrue(error.toString().contains(
        accountDir + " move failed; the complete copy is at " + accountDest + ", keep it."));
    Assert.assertTrue(error.toString().contains(String.format(
        "To recover manually: remove %s if present, then create a symbolic link at %s"
            + " pointing to %s.", accountDir, accountDir, accountDest)));
    Assert.assertTrue(Files.isSymbolicLink(marketDir.toPath()));
  }

  @Test
  public void testInvalidPathTypeRejected() throws RocksDBException, IOException {
    assertConfigRejected("storage.properties = [{name = \"" + ACCOUNT + "\", path = [1, 2]}]");
  }

  @Test
  public void testInvalidDbDirectoryTypeRejected() throws RocksDBException, IOException {
    assertConfigRejected("storage {\n  db.directory = [1]\n  properties = [{name = \""
        + ACCOUNT + "\", path = \"" + OUTPUT_DIRECTORY + "/dest\"}]\n}\n");
  }

  @Test
  public void testCopyProgressBarClosedOnFailure() throws RocksDBException, IOException {
    File database = newDatabase();
    File accountDir = Paths.get(database.getPath(), ACCOUNT).toFile();
    Files.createSymbolicLink(Paths.get(accountDir.getPath(), "evil-link"),
        temporaryFolder.newFolder("outside").toPath());

    List<ProgressBar> copyBars = new ArrayList<>();
    try (MockedConstruction<ProgressBar> bars = Mockito.mockConstruction(ProgressBar.class,
        (bar, context) -> {
          if ("copy task".equals(context.arguments().get(0))) {
            copyBars.add(bar);
          }
        })) {
      Assert.assertEquals(1, mv(database, getConfig("config.conf")));
    }
    Assert.assertEquals(1, copyBars.size());
    Mockito.verify(copyBars.get(0)).close();
    Mockito.verify(copyBars.get(0), Mockito.never()).step();
  }

  @Test
  public void testDuplicate() throws IOException {
    File output = temporaryFolder.newFolder();
    String[] args = new String[] {"db", "mv", "-d",
        output.getPath(), "-c",
        getConfig("config-duplicate.conf")};
    CommandLine cli = new CommandLine(new Toolkit());
    Assert.assertEquals(2, cli.execute(args));
  }

  @Test
  public void testHelp() {
    String[] args = new String[] {"db", "mv", "-h"};
    CommandLine cli = new CommandLine(new Toolkit());
    Assert.assertEquals(0, cli.execute(args));
    CommandLine db = cli.getSubcommands().get("db");
    Assert.assertFalse(db.getUsageMessage().contains("`db`"));
    Assert.assertTrue(db.getSubcommands().get("mv").getUsageMessage()
        .contains("you must stop the currently running FullNode service"));
  }

  @Test
  public void testDicNotExist() {
    String[] args = new String[] {"db", "mv", "-d", "dicNotExist"};
    CommandLine cli = new CommandLine(new Toolkit());
    Assert.assertEquals(2, cli.execute(args));
  }

  @Test
  public void testConfNotExist() throws IOException {
    File output = temporaryFolder.newFolder();
    String[] args = new String[] {"db", "mv", "-d",
        output.getPath(), "-c",
        "config.conf"};
    CommandLine cli = new CommandLine(new Toolkit());
    Assert.assertEquals(2, cli.execute(args));
  }

  @Test
  public void testEmpty() throws IOException {
    File output = temporaryFolder.newFolder();
    String[] args = new String[] {"db", "mv", "-d", output.getPath(), "-c",
        getConfig("config.conf")};
    CommandLine cli = new CommandLine(new Toolkit());

    Assert.assertEquals(2, cli.execute(args));
  }
}
