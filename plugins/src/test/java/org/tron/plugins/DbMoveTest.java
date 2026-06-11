package org.tron.plugins;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.rocksdb.RocksDBException;
import org.tron.plugins.utils.DBUtils;
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
    File victim = Objects.requireNonNull(accountDir.listFiles(File::isFile))[0];
    // Make one source file unreadable so its copy fails part-way through the move.
    Assert.assertTrue(victim.setReadable(false, false));

    String[] args = mvArgs(database, getConfig("config.conf"));
    CommandLine cli = new CommandLine(new Toolkit());
    StringWriter output = new StringWriter();
    cli.setOut(new PrintWriter(output));
    try {
      // Skip when the platform ignores the read bit (e.g. running as root).
      Assume.assumeFalse("file still readable (root?), cannot simulate copy failure",
          victim.canRead());
      Assert.assertEquals(1, cli.execute(args));

      // A failed copy must keep every source intact and roll back all destinations.
      Assert.assertTrue("source dir must be kept on copy failure", accountDir.exists());
      Assert.assertFalse("source must not be replaced by a symlink",
          Files.isSymbolicLink(accountDir.toPath()));
      Assert.assertTrue("source file must still exist", victim.exists());
      Assert.assertTrue("other source dirs must not be moved after a copy failure",
          marketDir.exists());
      Assert.assertFalse("other source dirs must not be replaced by symlinks",
          Files.isSymbolicLink(marketDir.toPath()));
      Assert.assertFalse("partial destination must be removed",
          Paths.get(OUTPUT_DIRECTORY, "dest", "database", ACCOUNT).toFile().exists());
      Assert.assertFalse("failure must not be reported as success",
          output.toString().contains("move db done."));
    } finally {
      victim.setReadable(true, false);
    }

    // Once the I/O problem is fixed, the unchanged command must be directly retryable.
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
        Paths.get(OUTPUT_DIRECTORY, "dest", "database", ACCOUNT).toFile().exists());
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
          Paths.get(OUTPUT_DIRECTORY, "dest", "database", ACCOUNT).toFile().exists());
    } finally {
      subDir.setReadable(true, false);
    }
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
      File dest = Paths.get(OUTPUT_DIRECTORY, "dest", "database", ACCOUNT).toFile();
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
