package org.tron.core;


import org.junit.Assert;
import org.junit.Test;
import org.tron.core.db.BlockFilledSlots;

public class BlockFilledSlotsTest {

  @Test
  public void test() {
    BlockFilledSlots blockFilledSlots = new BlockFilledSlots();
    Assert.assertEquals(1, blockFilledSlots.getBlockFilledSlots()[0]);

    blockFilledSlots.applyBlock(false);
    Assert.assertEquals(0, blockFilledSlots.getBlockFilledSlots()[0]);

    blockFilledSlots.applyBlock(false);
    Assert.assertEquals(0, blockFilledSlots.getBlockFilledSlots()[1]);
    Assert.assertEquals(BlockFilledSlots.SLOT_NUMBER - 2,
        blockFilledSlots.calculateFilledSlotsCount());

    for (int i = 2; i < BlockFilledSlots.SLOT_NUMBER; i++) {
      blockFilledSlots.applyBlock(true);
    }
    blockFilledSlots.applyBlock(true);
    Assert.assertEquals(1, blockFilledSlots.getBlockFilledSlots()[0]);

  }

}
