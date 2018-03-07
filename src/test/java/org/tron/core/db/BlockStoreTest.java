package org.tron.core.db;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.core.Constant;
import org.tron.core.config.Configuration;
import org.tron.core.config.args.Args;


public class BlockStoreTest {

  private static final Logger logger = LoggerFactory.getLogger("Test");

  @Before
  public void init() {
    Args.setParam(new String[]{}, Configuration.getByPath(Constant.TEST_CONF));
  }

  @Test
  public void testCreateBlockStore() {
    BlockStore blockStore = BlockStore.create("test-createBlock");
    Assert.assertEquals("0000000000000000000000000000000000000000000000000000000000000000",
        blockStore.getHeadBlockId().toString());
    Assert.assertEquals(0, blockStore.getHeadBlockNum());
    Assert.assertEquals(blockStore.getGenesisTime(), blockStore.getHeadBlockTime());
    Assert.assertEquals(0, blockStore.currentASlot());
    blockStore.close();

  }
}
