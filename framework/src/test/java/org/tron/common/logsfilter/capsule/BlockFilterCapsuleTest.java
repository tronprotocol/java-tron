package org.tron.common.logsfilter.capsule;

import com.google.protobuf.ByteString;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;

public class BlockFilterCapsuleTest {

  private BlockFilterCapsule blockFilterCapsule;

  @Before
  public void setUp() {
    BlockCapsule blockCapsule = new BlockCapsule(1, Sha256Hash.ZERO_HASH,
        System.currentTimeMillis(), ByteString.EMPTY);
    blockFilterCapsule = new BlockFilterCapsule(blockCapsule, false);
  }

  @Test
  public void testSetAndGetBlockHash() {
    blockFilterCapsule
        .setBlockHash("e58f33f9baf9305dc6f82b9f1934ea8f0ade2defb951258d50167028c780351f");
    System.out.println(blockFilterCapsule);
    Assert.assertEquals("e58f33f9baf9305dc6f82b9f1934ea8f0ade2defb951258d50167028c780351f",
        blockFilterCapsule.getBlockHash());
  }

  @Test
  public void testSetAndIsSolidified() {
    blockFilterCapsule = new BlockFilterCapsule(
        "e58f33f9baf9305dc6f82b9f1934ea8f0ade2defb951258d50167028c780351f", false);
    blockFilterCapsule.setSolidified(true);
    blockFilterCapsule.processFilterTrigger();
    Assert.assertTrue(blockFilterCapsule.isSolidified());
  }
}
