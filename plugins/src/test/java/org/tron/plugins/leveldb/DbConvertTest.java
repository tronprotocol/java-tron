package org.tron.plugins.leveldb;

import java.io.IOException;
import java.util.UUID;
import org.junit.Assert;
import org.junit.Test;
import org.rocksdb.RocksDBException;
import org.tron.plugins.DbTest;
import org.tron.plugins.Toolkit;
import org.tron.plugins.utils.db.DbTool;
import picocli.CommandLine;

public class DbConvertTest extends DbTest {

  @Test
  public void testRun() throws IOException, RocksDBException {
    init(DbTool.DbType.LevelDB);
    String[] args = new String[] { "db", "convert",  INPUT_DIRECTORY,
        temporaryFolder.newFolder().toString() };
    Assert.assertEquals(0, cli.execute(args));
  }

  @Test
  public void testHelp() {
    String[] args = new String[] {"db", "convert", "-h"};
    CommandLine cli = new CommandLine(new Toolkit());
    Assert.assertEquals(0, cli.execute(args));
  }

  @Test
  public void testNotExist() {
    String[] args = new String[] {"db", "convert", UUID.randomUUID().toString(),
        UUID.randomUUID().toString()};
    Assert.assertEquals(404, cli.execute(args));
  }

  @Test
  public void testEmpty() throws IOException {
    String[] args = new String[] {"db", "convert", temporaryFolder.newFolder().toString(),
        temporaryFolder.newFolder().toString()};
    Assert.assertEquals(0, cli.execute(args));
  }

}
