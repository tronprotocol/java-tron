package org.tron.core.net.messagehandler;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.utils.Pair;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.config.Parameter.NetConstants;
import org.tron.core.exception.P2pException;
import org.tron.core.net.message.keepalive.PingMessage;
import org.tron.core.net.message.sync.ChainInventoryMessage;
import org.tron.core.net.peer.PeerConnection;

public class ChainInventoryMsgHandlerTest {

  private ChainInventoryMsgHandler handler = new ChainInventoryMsgHandler();
  private PeerConnection peer = new PeerConnection();
  private ChainInventoryMessage msg = new ChainInventoryMessage(new ArrayList<>(), 0L);
  private List<BlockId> blockIds = new ArrayList<>();

  @Test
  public void testProcessMessage() throws Exception {
    try {
      handler.processMessage(peer, msg);
    } catch (P2pException e) {
      Assert.assertEquals("not send syncBlockChainMsg", e.getMessage());
    }

    peer.setSyncChainRequested(new Pair<>(new LinkedList<>(), System.currentTimeMillis()));

    try {
      handler.processMessage(peer, msg);
    } catch (P2pException e) {
      Assert.assertEquals("blockIds is empty", e.getMessage());
    }

    long size = NetConstants.SYNC_FETCH_BATCH_NUM + 2;
    for (int i = 0; i < size; i++) {
      blockIds.add(new BlockId());
    }
    msg = new ChainInventoryMessage(blockIds, 0L);

    try {
      handler.processMessage(peer, msg);
    } catch (P2pException e) {
      Assert.assertEquals(e.getMessage(), "big blockIds size: " + size);
    }

    blockIds.clear();
    size = NetConstants.SYNC_FETCH_BATCH_NUM / 100;
    for (int i = 0; i < size; i++) {
      blockIds.add(new BlockId());
    }
    msg = new ChainInventoryMessage(blockIds, 100L);

    try {
      handler.processMessage(peer, msg);
    } catch (P2pException e) {
      Assert.assertEquals(e.getMessage(), "remain: 100, blockIds size: " + size);
    }
    Assert.assertNotNull(msg.toString());
    Assert.assertNull(msg.getAnswerMessage());
  }

}
