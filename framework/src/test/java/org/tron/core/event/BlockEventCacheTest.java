package org.tron.core.event;

import java.util.List;
import java.util.Random;
import org.junit.Assert;
import org.junit.Test;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.services.event.BlockEventCache;
import org.tron.core.services.event.bo.BlockEvent;
import org.tron.core.services.event.exception.EventException;

public class BlockEventCacheTest {

  @Test
  public void test() throws Exception {
    BlockEvent be1 = new BlockEvent();
    BlockCapsule.BlockId b1 = new BlockCapsule.BlockId(getBlockId(), 1);
    be1.setBlockId(b1);
    be1.setParentId(b1);
    be1.setSolidId(b1);
    try {
      BlockEventCache.add(be1);
      Assert.fail();
    } catch (Exception e) {
      Assert.assertTrue(e instanceof EventException);
    }

    BlockEventCache.init(new BlockCapsule.BlockId(getBlockId(), 100));

    try {
      BlockEventCache.add(be1);
      Assert.fail();
    } catch (Exception e) {
      Assert.assertTrue(e instanceof EventException);
    }

    BlockEventCache.init(b1);

    BlockEvent be2 = new BlockEvent();
    BlockCapsule.BlockId b2 = new BlockCapsule.BlockId(getBlockId(), 2);
    be2.setBlockId(b2);
    be2.setParentId(b1);
    be2.setSolidId(b1);
    BlockEventCache.add(be2);
    Assert.assertEquals(be2, BlockEventCache.getHead());
    Assert.assertEquals(be2, BlockEventCache.getBlockEvent(b2));

    BlockEvent be22 = new BlockEvent();
    BlockCapsule.BlockId b22 = new BlockCapsule.BlockId(getBlockId(), 2);
    be22.setBlockId(b22);
    be22.setParentId(b1);
    be22.setSolidId(b22);
    BlockEventCache.add(be22);
    Assert.assertEquals(be2, BlockEventCache.getHead());
    Assert.assertEquals(be22, BlockEventCache.getBlockEvent(b22));
    Assert.assertEquals(b22, BlockEventCache.getSolidId());

    BlockEvent be3 = new BlockEvent();
    BlockCapsule.BlockId b3 = new BlockCapsule.BlockId(getBlockId(), 3);
    be3.setBlockId(b3);
    be3.setParentId(b22);
    be3.setSolidId(b22);
    BlockEventCache.add(be3);
    Assert.assertEquals(be3, BlockEventCache.getHead());

    List<BlockEvent> list =  BlockEventCache.getSolidBlockEvents(b2);
    Assert.assertEquals(1, list.size());
    list =  BlockEventCache.getSolidBlockEvents(b22);
    Assert.assertEquals(1, list.size());

    list =  BlockEventCache.getSolidBlockEvents(b3);
    Assert.assertEquals(2, list.size());

    BlockEventCache.remove(b22);
    Assert.assertEquals(2, BlockEventCache.getSolidNum());

    list =  BlockEventCache.getSolidBlockEvents(b2);
    Assert.assertEquals(0, list.size());
    list =  BlockEventCache.getSolidBlockEvents(b22);
    Assert.assertEquals(0, list.size());

    list =  BlockEventCache.getSolidBlockEvents(b3);
    Assert.assertEquals(1, list.size());
  }

  public static byte[] getBlockId() {
    byte[] id = new byte[32];
    new Random().nextBytes(id);
    return id;
  }
}
