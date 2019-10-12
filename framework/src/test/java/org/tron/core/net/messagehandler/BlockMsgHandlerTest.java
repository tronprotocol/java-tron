package org.tron.core.net.messagehandler;

import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.testng.collections.Lists;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.exception.P2pException;
import org.tron.core.net.message.BlockMessage;
import org.tron.core.net.peer.Item;
import org.tron.core.net.peer.PeerConnection;
import org.tron.protos.Protocol.Inventory.InventoryType;
import org.tron.protos.Protocol.Transaction;

public class BlockMsgHandlerTest {

  private BlockMsgHandler handler = new BlockMsgHandler();
  private PeerConnection peer = new PeerConnection();
  private BlockCapsule blockCapsule;
  private BlockMessage msg;

  @Test
  public void testProcessMessage() {
    try {
      blockCapsule = new BlockCapsule(1, Sha256Hash.ZERO_HASH,
          System.currentTimeMillis(), Sha256Hash.ZERO_HASH.getByteString());
      msg = new BlockMessage(blockCapsule);
      handler.processMessage(peer, new BlockMessage(blockCapsule));
    } catch (P2pException e) {
      Assert.assertTrue(e.getMessage().equals("no request"));
    }

    try {
      List<Transaction> transactionList = Lists.newArrayList();
      for (int i = 0; i < 1100000; i++) {
        transactionList.add(Transaction.newBuilder().build());
      }
      blockCapsule = new BlockCapsule(1, Sha256Hash.ZERO_HASH.getByteString(),
          System.currentTimeMillis() + 10000, transactionList);
      msg = new BlockMessage(blockCapsule);
      System.out.println("len = " + blockCapsule.getInstance().getSerializedSize());
      peer.getAdvInvRequest()
          .put(new Item(msg.getBlockId(), InventoryType.BLOCK), System.currentTimeMillis());
      handler.processMessage(peer, msg);
    } catch (P2pException e) {
      System.out.println(e);
      Assert.assertTrue(e.getMessage().equals("block size over limit"));
    }

    try {
      blockCapsule = new BlockCapsule(1, Sha256Hash.ZERO_HASH,
          System.currentTimeMillis() + 10000, Sha256Hash.ZERO_HASH.getByteString());
      msg = new BlockMessage(blockCapsule);
      peer.getAdvInvRequest()
          .put(new Item(msg.getBlockId(), InventoryType.BLOCK), System.currentTimeMillis());
      handler.processMessage(peer, msg);
    } catch (P2pException e) {
      System.out.println(e);
      Assert.assertTrue(e.getMessage().equals("block time error"));
    }
  }

}
