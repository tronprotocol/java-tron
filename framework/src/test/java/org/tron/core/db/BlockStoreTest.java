package org.tron.core.db;

import com.google.protobuf.ByteString;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.TestConstants;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.config.args.Args;


@Slf4j
public class BlockStoreTest extends BaseTest {

  @Resource
  private BlockStore blockStore;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath()},
        TestConstants.TEST_CONF);
  }

  private BlockCapsule getBlockCapsule(long number) {
    return new BlockCapsule(number, Sha256Hash.ZERO_HASH,
            System.currentTimeMillis(), ByteString.EMPTY);
  }

  @Test
  public void testCreateBlockStore() {
  }

  @Test
  public void testPut() throws Exception {
    long number = 1;
    BlockCapsule blockCapsule = getBlockCapsule(number);

    byte[] blockId = blockCapsule.getBlockId().getBytes();
    blockStore.put(blockId, blockCapsule);
    BlockCapsule blockCapsule1 = blockStore.get(blockId);
    Assert.assertNotNull(blockCapsule1);
    Assert.assertEquals(number, blockCapsule1.getNum());
  }

  @Test
  public void testGet() throws Exception {
    long number = 2;
    BlockCapsule blockCapsule = getBlockCapsule(number);
    byte[] blockId = blockCapsule.getBlockId().getBytes();
    blockStore.put(blockId, blockCapsule);
    boolean has = blockStore.has(blockId);
    Assert.assertTrue(has);
    BlockCapsule blockCapsule1 = blockStore.get(blockId);
    Assert.assertEquals(number, blockCapsule1.getNum());
  }

  @Test
  public void testDelete() throws Exception {
    long number = 1;
    BlockCapsule blockCapsule = getBlockCapsule(number);

    byte[] blockId = blockCapsule.getBlockId().getBytes();
    blockStore.put(blockId, blockCapsule);
    BlockCapsule blockCapsule1 = blockStore.get(blockId);
    Assert.assertNotNull(blockCapsule1);
    Assert.assertEquals(number, blockCapsule1.getNum());

    blockStore.delete(blockId);
    BlockCapsule blockCapsule2 = blockStore.getUnchecked(blockId);
    Assert.assertNull(blockCapsule2);
  }

}
