package org.tron.plugins;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
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
    File database = temporaryFolder.newFolder("database");
    init(DbTool.DbType.RocksDB, Paths.get(database.getPath()).toString());
    String[] args = new String[] {"db", "mv", "-d",
        database.getParent(), "-c",
        getConfig("config.conf")};
    CommandLine cli = new CommandLine(new Toolkit());
    Assert.assertEquals(0, cli.execute(args));
    Assert.assertEquals(2, cli.execute(args));
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
