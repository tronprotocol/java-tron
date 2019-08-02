package org.tron.core.db;

import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.FileUtil;
import org.tron.core.Constant;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;

@Slf4j
public class BlockStoreTest {

  private static final String dbPath = "output-blockStore-test";
  BlockStore blockStore;
  private static TronApplicationContext context;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath},
        Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
  }

  @Before
  public void init() {
    blockStore = context.getBean(BlockStore.class);
  }

  @After
  public void destroy() {
    Args.clearParam();
    context.destroy();
    FileUtil.deleteDir(new File(dbPath));
  }

  @Test
  public void testCreateBlockStore() {
  }
}
