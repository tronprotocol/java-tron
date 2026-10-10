package org.tron.core.net.peer;

import java.util.ArrayDeque;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.utils.Pair;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.protos.Protocol.Inventory.InventoryType;

public class PeerBlockIdleTest {

  @Test
  public void testTransactionRequestDoesNotBlockBlockFetch() {
    PeerConnection peer = new PeerConnection();
    Item trx = new Item(Sha256Hash.ZERO_HASH, InventoryType.TRX);
    peer.getAdvInvRequest().put(trx, System.currentTimeMillis());
    Assert.assertFalse(peer.isIdle());
    Assert.assertTrue(peer.isBlockFetchIdle());

    Item block = new Item(new BlockId(), InventoryType.BLOCK);
    peer.getAdvInvRequest().put(block, System.currentTimeMillis());
    Assert.assertFalse(peer.isBlockFetchIdle());
    peer.getAdvInvRequest().remove(block);
    Assert.assertTrue(peer.isBlockFetchIdle());
  }

  @Test
  public void testSyncRequestAndProcessingExcludeBlockProvider() {
    PeerConnection peer = new PeerConnection();
    peer.getSyncBlockRequested().put(new BlockId(), System.currentTimeMillis());
    Assert.assertFalse(peer.isBlockFetchIdle());
    peer.getSyncBlockRequested().clear();
    peer.setSyncChainRequested(new Pair<>(new ArrayDeque<>(), System.currentTimeMillis()));
    Assert.assertFalse(peer.isBlockFetchIdle());
    peer.setSyncChainRequested(null);
    peer.getSyncBlockInProcess().add(new BlockId());
    Assert.assertFalse(peer.isBlockFetchIdle());
    peer.getSyncBlockInProcess().clear();
    Assert.assertTrue(peer.isBlockFetchIdle());
  }
}
