package org.tron.plugins.rocksdb;

import java.io.IOException;
import org.junit.Test;
import org.tron.plugins.DbLiteTest;

public class DbLiteExcludeHistoricalBalanceRocksDbTest extends DbLiteTest {

  @Test
  public void testToolsWithExcludeHistoricalBalance() throws InterruptedException, IOException {
    testTools("ROCKSDB", 1, true);
  }
}
