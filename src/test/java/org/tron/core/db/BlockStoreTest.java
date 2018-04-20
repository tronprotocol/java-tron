package org.tron.core.db;

import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.utils.FileUtil;
import org.tron.core.Constant;
import org.tron.core.config.args.Args;

@Slf4j
public class BlockStoreTest {

  private static final String dbPath = "output-blockStore-test";

  @Before
  public void init() {
    Args.setParam(new String[]{"-d", dbPath}, Constant.TEST_CONF);
  }

  @After
  public void destroy() {
    Args.clearParam();
    FileUtil.deleteDir(new File(dbPath));
    BlockStore.destroy();

  }

  @Test
  public void testCreateBlockStore() {
  }
}
