package org.tron.core.db;

import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.utils.FileUtil;
import org.tron.core.Constant;
import org.tron.core.config.Configuration;
import org.tron.core.config.args.Args;

@Slf4j
public class BlockStoreTest {
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
