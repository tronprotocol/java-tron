package org.tron.common.logsfilter.capsule;

import com.google.protobuf.ByteString;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;

public class BlockLogTriggerCapsuleTest {

  private BlockLogTriggerCapsule blockLogTriggerCapsule;

  @Before
  public void setUp() {
    BlockCapsule blockCapsule = new BlockCapsule(1, Sha256Hash.ZERO_HASH,
        System.currentTimeMillis(), ByteString.EMPTY);
    blockLogTriggerCapsule = new BlockLogTriggerCapsule(blockCapsule);
  }

  @Test
  public void testSetAndGetBlockLogTrigger() {
    blockLogTriggerCapsule
        .setBlockLogTrigger(blockLogTriggerCapsule.getBlockLogTrigger());
    Assert.assertEquals(1,
        blockLogTriggerCapsule.getBlockLogTrigger().getBlockNumber());
  }

  @Test
  public void testSetLatestSolidifiedBlockNumber() {
    blockLogTriggerCapsule.setLatestSolidifiedBlockNumber(100);
    Assert.assertEquals(100,
        blockLogTriggerCapsule.getBlockLogTrigger().getLatestSolidifiedBlockNumber());
  }
}
