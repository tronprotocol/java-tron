package org.tron.common.logsfilter.capsule;

import com.google.protobuf.ByteString;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.services.jsonrpc.TronJsonRpcImpl;

public class BlockFilterCapsuleTest {

  private BlockFilterCapsule blockFilterCapsule;
  private TronJsonRpcImpl jsonRpc;

  @Before
  public void setUp() {
    jsonRpc = new TronJsonRpcImpl(null, null, null);
    BlockCapsule blockCapsule = new BlockCapsule(1, Sha256Hash.ZERO_HASH,
        System.currentTimeMillis(), ByteString.EMPTY);
    blockFilterCapsule = new BlockFilterCapsule(blockCapsule, false, jsonRpc);
  }

  @Test
  public void testSetAndGetBlockHash() {
    blockFilterCapsule
        .setBlockHash("e58f33f9baf9305dc6f82b9f1934ea8f0ade2defb951258d50167028c780351f");
    Assert.assertEquals("e58f33f9baf9305dc6f82b9f1934ea8f0ade2defb951258d50167028c780351f",
        blockFilterCapsule.getBlockHash());
  }

  @Test
  public void testSetAndIsSolidified() {
    blockFilterCapsule = new BlockFilterCapsule(
        "e58f33f9baf9305dc6f82b9f1934ea8f0ade2defb951258d50167028c780351f", false, jsonRpc);
    blockFilterCapsule.setSolidified(true);
    blockFilterCapsule.processFilterTrigger();
    Assert.assertTrue(blockFilterCapsule.isSolidified());
  }
}
