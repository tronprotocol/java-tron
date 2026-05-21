package org.tron.plugins.leveldb;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.rocksdb.RocksDBException;
import org.tron.plugins.Toolkit;
import org.tron.plugins.utils.DBUtils;
import org.tron.plugins.utils.db.DbTool;
import picocli.CommandLine;

@Slf4j
public class DbArchiveTest {

  @ClassRule
  public static final TemporaryFolder temporaryFolder = new TemporaryFolder();

  private static  String OUTPUT_DIRECTORY;

  private static final String ACCOUNT = "account";
  private static final String ACCOUNT_ROCKSDB = "account-rocksdb";

  @BeforeClass
  public static void init() throws IOException, RocksDBException {
    OUTPUT_DIRECTORY = temporaryFolder.newFolder("database").toString();
    File file = new File(OUTPUT_DIRECTORY, ACCOUNT);
    DbTool.openLevelDb(file.toPath(),ACCOUNT).close();

    file = new File(OUTPUT_DIRECTORY, DBUtils.MARKET_PAIR_PRICE_TO_ORDER);
    DbTool.openLevelDb(file.toPath(), DBUtils.MARKET_PAIR_PRICE_TO_ORDER).close();

    file = new File(OUTPUT_DIRECTORY, ACCOUNT_ROCKSDB);
    DbTool.openRocksDb(file.toPath(), ACCOUNT_ROCKSDB).close();

  }

  @Test
  public void testRun() {
    String[] args = new String[] {"db", "archive", "-d", OUTPUT_DIRECTORY };
    CommandLine cli = new CommandLine(new Toolkit());
    Assert.assertEquals(0, cli.execute(args));
  }

  @Test
  public void testHelp() {
    String[] args = new String[] {"db", "archive", "-h"};
    CommandLine cli = new CommandLine(new Toolkit());
    Assert.assertEquals(0, cli.execute(args));
  }

  @Test
  public void testMaxManifest() {
    String[] args = new String[] {"db", "archive", "-d", OUTPUT_DIRECTORY, "-m", "128"};
    CommandLine cli = new CommandLine(new Toolkit());
    Assert.assertEquals(0, cli.execute(args));
  }

  @Test
  public void testNotExist() {
    String[] args = new String[] {"db", "archive", "-d",
        OUTPUT_DIRECTORY + File.separator + UUID.randomUUID()};
    CommandLine cli = new CommandLine(new Toolkit());
    Assert.assertEquals(404, cli.execute(args));
  }

  @Test
  public void testEmpty() {
    File file = new File(OUTPUT_DIRECTORY + File.separator + UUID.randomUUID());
    file.mkdirs();
    file.deleteOnExit();
    String[] args = new String[] {"db", "archive", "-d", file.toString()};
    CommandLine cli = new CommandLine(new Toolkit());
    Assert.assertEquals(0, cli.execute(args));
  }

}
