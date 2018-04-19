package org.tron.core.net.node;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.net.message.BlockMessage;
import org.tron.core.net.message.TransactionMessage;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Inventory.InventoryType;
import org.tron.protos.Protocol.Transaction;

@Slf4j
public class BroadTest {

  private NodeImpl node = new NodeImpl();

  @Test
  public void testBlockBroad() throws NoSuchFieldException, IllegalAccessException {
    Block block = Block.getDefaultInstance();
    BlockMessage blockMessage = new BlockMessage(block);
    node.broadcast(blockMessage);
    Field advObjToSpreadField = node.getClass().getDeclaredField("advObjToSpread");
    advObjToSpreadField.setAccessible(true);
    ConcurrentHashMap<Sha256Hash, InventoryType> advObjToSpread = (ConcurrentHashMap<Sha256Hash, InventoryType>) advObjToSpreadField
        .get(node);
    Assert.assertEquals(advObjToSpread.get(blockMessage.getMessageId()), InventoryType.BLOCK);
  }

  @Test
  public void testTransactionBroad() throws NoSuchFieldException, IllegalAccessException {
    Transaction transaction = Transaction.getDefaultInstance();
    TransactionMessage transactionMessage = new TransactionMessage(transaction);
    node.broadcast(transactionMessage);
    Field advObjToSpreadField = node.getClass().getDeclaredField("advObjToSpread");
    advObjToSpreadField.setAccessible(true);
    ConcurrentHashMap<Sha256Hash, InventoryType> advObjToSpread = (ConcurrentHashMap<Sha256Hash, InventoryType>) advObjToSpreadField
        .get(node);
    Assert.assertEquals(advObjToSpread.get(transactionMessage.getMessageId()), InventoryType.TRX);
  }
}
