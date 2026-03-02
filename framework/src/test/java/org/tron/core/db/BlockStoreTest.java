package org.tron.core.db;

import com.google.protobuf.ByteString;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.Constant;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.ItemNotFoundException;


@Slf4j
public class BlockStoreTest extends BaseTest {

  @Resource
  private BlockStore blockStore;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath()},
        Constant.TEST_CONF);
  }

  private BlockCapsule getBlockCapsule(long number) {
    return new BlockCapsule(number, Sha256Hash.ZERO_HASH,
            System.currentTimeMillis(), ByteString.EMPTY);
  }

  @Test
  public void testCreateBlockStore() {
  }

  @Test
  public void testPut() {
    long number = 1;
    BlockCapsule blockCapsule = getBlockCapsule(number);

    byte[] blockId = blockCapsule.getBlockId().getBytes();
    blockStore.put(blockId, blockCapsule);
    try {
      BlockCapsule blockCapsule1 = blockStore.get(blockId);
      Assert.assertNotNull(blockCapsule1);
      Assert.assertEquals(number, blockCapsule1.getNum());
    } catch (ItemNotFoundException | BadItemException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testGet() {
    long number = 2;
    BlockCapsule blockCapsule = getBlockCapsule(number);
    byte[] blockId = blockCapsule.getBlockId().getBytes();
    blockStore.put(blockId, blockCapsule);
    try {
      boolean has = blockStore.has(blockId);
      Assert.assertTrue(has);
      BlockCapsule blockCapsule1 = blockStore.get(blockId);

      Assert.assertEquals(number, blockCapsule1.getNum());
    } catch (ItemNotFoundException | BadItemException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testDelete() {
    long number = 1;
    BlockCapsule blockCapsule = getBlockCapsule(number);

    byte[] blockId = blockCapsule.getBlockId().getBytes();
    blockStore.put(blockId, blockCapsule);
    try {
      BlockCapsule blockCapsule1 = blockStore.get(blockId);
      Assert.assertNotNull(blockCapsule1);
      Assert.assertEquals(number, blockCapsule1.getNum());

      blockStore.delete(blockId);
      BlockCapsule blockCapsule2 = blockStore.getUnchecked(blockId);
      Assert.assertNull(blockCapsule2);
    } catch (ItemNotFoundException | BadItemException e) {
      e.printStackTrace();
    }
  }

}
