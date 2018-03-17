package org.tron.core.db;

import java.io.File;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.utils.FileUtil;
import org.tron.core.Constant;
import org.tron.core.config.Configuration;
import org.tron.core.config.args.Args;


public class BlockStoreTest {

  private static final Logger logger = LoggerFactory.getLogger("Test");
  private static final String dbPath = "output-blockStore-test";

  @Before
  public void init() {
    Args.setParam(new String[]{"-d", dbPath}, Configuration.getByPath(Constant.TEST_CONF));
  }

  @After
  public void destroy() {
    Args.clearParam();
    FileUtil.deleteDir(new File(dbPath));
  }

  @Test
  public void testCreateBlockStore() {
    BlockStore blockStore = BlockStore.create("test-createBlock");
    Assert.assertEquals("0000000000000000000000000000000000000000000000000000000000000000",
        blockStore.getHeadBlockId().toString());
    Assert.assertEquals(0, blockStore.getHeadBlockNum());
    Assert.assertEquals(blockStore.getGenesisTime(), blockStore.getHeadBlockTime());
    Assert.assertEquals(0, blockStore.currentASlot());
  }
}
