package org.tron.plugins;

import static org.iq80.leveldb.impl.Iq80DBFactory.factory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.plugins.utils.FileUtils;
import picocli.CommandLine;

@Slf4j
public class DbMoveTest {

  private static final String OUTPUT_DIRECTORY = "output-directory-toolkit";
  private static final String OUTPUT_DIRECTORY_DATABASE =
      Paths.get(OUTPUT_DIRECTORY,"ori","database").toString();
  private static final String ENGINE = "ENGINE";
  private static final String LEVELDB = "LEVELDB";
  private static final String ACCOUNT = "account";
  private static final String TRANS = "trans";
  private static final String MARKET = "market_pair_price_to_order";
  private static final String ENGINE_FILE = "engine.properties";


  @BeforeClass
  public static void init() throws IOException {
    File file = new File(OUTPUT_DIRECTORY_DATABASE, ACCOUNT);
    factory.open(file, ArchiveManifest.newDefaultLevelDbOptions()).close();
    FileUtils.writeProperty(file + File.separator + ENGINE_FILE, ENGINE, LEVELDB);

    file = new File(OUTPUT_DIRECTORY_DATABASE, MARKET);
    factory.open(file, ArchiveManifest.newDefaultLevelDbOptions()).close();
    FileUtils.writeProperty(file + File.separator + ENGINE_FILE, ENGINE, LEVELDB);

    file = new File(OUTPUT_DIRECTORY_DATABASE, TRANS);
    factory.open(file, ArchiveManifest.newDefaultLevelDbOptions()).close();
    FileUtils.writeProperty(file + File.separator + ENGINE_FILE, ENGINE, LEVELDB);

  }

  @AfterClass
  public static void destroy() {
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
  public void testMv() {
    String[] args = new String[] {"db", "mv", "-d",
        Paths.get(OUTPUT_DIRECTORY,"ori").toString(), "-c",
        getConfig("config.conf")};
    CommandLine cli = new CommandLine(new Toolkit());
    Assert.assertEquals(0, cli.execute(args));
    Assert.assertEquals(2, cli.execute(args));
  }

  @Test
  public void testDuplicate() {
    String[] args = new String[] {"db", "mv", "-d",
        Paths.get(OUTPUT_DIRECTORY,"ori").toString(), "-c",
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
  public void testConfNotExist() {
    String[] args = new String[] {"db", "mv", "-d",
        Paths.get(OUTPUT_DIRECTORY,"ori").toString(), "-c",
        "config.conf"};
    CommandLine cli = new CommandLine(new Toolkit());
    Assert.assertEquals(2, cli.execute(args));
  }

  @Test
  public void testEmpty() {
    File file = new File(OUTPUT_DIRECTORY_DATABASE + File.separator + UUID.randomUUID());
    file.mkdirs();
    file.deleteOnExit();
    String[] args = new String[] {"db", "mv", "-d", file.toString(), "-c",
        getConfig("config.conf")};
    CommandLine cli = new CommandLine(new Toolkit());

    Assert.assertEquals(2, cli.execute(args));
  }
}
