package org.tron.core.db;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.core.Constant;
import org.tron.core.config.args.Args;

@Slf4j
public class BlockStoreTest extends BaseTest {


  static {
    dbPath = "output-blockStore-test";
    Args.setParam(new String[]{"--output-directory", dbPath},
        Constant.TEST_CONF);
  }

  @Test
  public void testCreateBlockStore() {
  }
}
