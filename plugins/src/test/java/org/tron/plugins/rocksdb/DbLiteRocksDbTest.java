package org.tron.plugins.rocksdb;

import java.io.IOException;
import org.junit.Test;
import org.tron.plugins.DbLiteTest;

public class DbLiteRocksDbTest extends DbLiteTest {

  @Test
  public void testToolsWithRocksDB() throws InterruptedException, IOException {
    testTools("ROCKSDB", 1);
  }
}
