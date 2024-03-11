package org.tron.plugins;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.rocksdb.RocksDBException;
import org.tron.plugins.utils.DBUtils;
import org.tron.plugins.utils.db.DBInterface;
import org.tron.plugins.utils.db.DbTool;
import org.tron.plugins.utils.db.LevelDBImpl;
import picocli.CommandLine;

@Slf4j
public class DbRootTest {

  @Rule
  public final TemporaryFolder folder = new TemporaryFolder();

  CommandLine cli = new CommandLine(new Toolkit());

  private static final String NORMAL_DB  = "normal";
  private static final String EMPTY_DB  = "empty";
  private static final String ERROR_DB  = "error";

  @Test
  public void testRoot() throws IOException, RocksDBException {

    File file = folder.newFolder();

    File database = Paths.get(file.getPath(),"database").toFile();
    Assert.assertTrue(database.mkdirs());


    try (DBInterface normal = DbTool.getDB(database.toString(), NORMAL_DB, DbTool.DbType.LevelDB);
         DBInterface empty = DbTool.getDB(database.toString(), EMPTY_DB, DbTool.DbType.RocksDB)) {
      for (int i = 0; i < 10; i++) {
        normal.put(("" + i).getBytes(), (NORMAL_DB + "-" + i).getBytes());
      }
    }

    String[] args = new String[] {"db", "root", database.toString(),
        "--db", NORMAL_DB,  "--db", EMPTY_DB};
    Assert.assertEquals(0, cli.execute(args));
    args = new String[] {"db", "root", database.toString(),
        "--db", NORMAL_DB};
    Assert.assertEquals(0, cli.execute(args));
    args = new String[] {"db", "root", database.toString(),
        "--db", EMPTY_DB};
    Assert.assertEquals(0, cli.execute(args));

    try (DBInterface errorDb = new LevelDBImpl(
        DBUtils.newLevelDb(Paths.get(database.toString(), ERROR_DB)), ERROR_DB)) {
      for (int i = 0; i < 10; i++) {
        errorDb.put(("" + i).getBytes(), (ERROR_DB + "-" + i).getBytes());
      }
      args = new String[] {"db", "root", database.toString(), "--db", ERROR_DB};
      Assert.assertEquals(1, cli.execute(args));
    }

  }

  @Test
  public void testHelp() {
    String[] args = new String[] {"db", "root", "-h"};
    Assert.assertEquals(0, cli.execute(args));
  }

  @Test
  public void testEmpty() throws IOException {
    File file = folder.newFolder();
    File database = Paths.get(file.getPath(),"database").toFile();
    String[] args = new String[] {"db", "root", database.toString()};
    Assert.assertEquals(404, cli.execute(args));
    Assert.assertTrue(database.mkdirs());
    Assert.assertEquals(404, cli.execute(args));
  }
}
