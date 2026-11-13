package org.tron.core.net.peer;

import java.util.HashSet;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.protos.Protocol.Inventory.InventoryType;

public class ItemTest {

  private static Sha256Hash idEndingInZeros(int i) {
    byte[] bytes = new byte[Sha256Hash.LENGTH];
    bytes[0] = (byte) (i >>> 24);
    bytes[1] = (byte) (i >>> 16);
    bytes[2] = (byte) (i >>> 8);
    bytes[3] = (byte) i;
    return Sha256Hash.wrap(bytes);
  }

  @Test
  public void testEqualItemsShareHashCode() {
    Sha256Hash id = idEndingInZeros(7);
    Item item = new Item(id, InventoryType.TRX);
    Item copy = new Item(Sha256Hash.wrap(id.getBytes().clone()), InventoryType.TRX);
    Assert.assertEquals(item, copy);
    Assert.assertEquals(item.hashCode(), copy.hashCode());
    Assert.assertEquals(item.hashCode(), item.hashCode());
  }

  @Test
  public void testBlockIdAndPlainHashWithSameBytesShareHashCode() {
    BlockId blockId = new BlockId(Sha256Hash.of(new byte[] {1}), 100L);
    Item fromBlockId = new Item(blockId, InventoryType.BLOCK);
    Item fromPlainHash = new Item(Sha256Hash.wrap(blockId.getBytes()), InventoryType.BLOCK);
    Assert.assertEquals(fromBlockId, fromPlainHash);
    Assert.assertEquals(fromPlainHash, fromBlockId);
    Assert.assertEquals(fromBlockId.hashCode(), fromPlainHash.hashCode());
  }

  @Test
  public void testIdsWithEqualLastFourBytesGetDistinctHashCodes() {
    int count = 10_000;
    Set<Integer> hashCodes = new HashSet<>();
    for (int i = 0; i < count; i++) {
      Sha256Hash id = idEndingInZeros(i);
      Assert.assertEquals(0, id.hashCode());
      hashCodes.add(new Item(id, InventoryType.TRX).hashCode());
    }
    // A keyed 32-bit hash gives about 0.01 expected collisions among 10,000 ids.
    Assert.assertTrue("distinct hash codes: " + hashCodes.size(), hashCodes.size() >= count - 10);
  }
}
