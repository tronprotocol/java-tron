package org.tron.core.net.services;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.net.message.BlockMessage;
import org.tron.core.net.peer.Item;
import org.tron.core.net.service.AdvService;
import org.tron.protos.Protocol.Inventory.InventoryType;

@Ignore
public class AdvServiceTest {

  AdvService service = new AdvService();

  @Test
  public void testAddInv() {
    boolean flag;
    Item item = new Item(Sha256Hash.ZERO_HASH, InventoryType.BLOCK);
    flag = service.addInv(item);
    Assert.assertTrue(flag);
    flag = service.addInv(item);
    Assert.assertTrue(!flag);
  }

  @Test
  public void testBroadcast() {
    BlockCapsule blockCapsule = new BlockCapsule(1, Sha256Hash.ZERO_HASH,
        System.currentTimeMillis(), Sha256Hash.ZERO_HASH.getByteString());
    BlockMessage msg = new BlockMessage(blockCapsule);
    service.broadcast(msg);
    Item item = new Item(blockCapsule.getBlockId(), InventoryType.BLOCK);
    Assert.assertTrue(service.getMessage(item) != null);
  }
}
