package org.tron.plugins.leveldb;

import java.io.IOException;
import org.junit.Test;
import org.tron.plugins.DbLiteTest;


public class DbLiteLevelDbTest  extends DbLiteTest {

  @Test
  public void testToolsWithLevelDB() throws InterruptedException, IOException {
    testTools("LEVELDB", 1);
  }
}
