package org.tron.plugins;

import java.io.IOException;
import org.junit.Test;

public class DbLiteLevelDbV2Test extends DbLiteTest {

  @Test
  public void testToolsWithLevelDBV2() throws InterruptedException, IOException {
    testTools("LEVELDB", 2);
  }
}
