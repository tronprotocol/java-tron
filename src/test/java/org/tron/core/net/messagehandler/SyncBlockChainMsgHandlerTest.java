package org.tron.core.net.messagehandler;

import org.junit.Assert;
import org.junit.Test;
import org.tron.core.exception.P2pException;
import org.tron.core.net.message.SyncBlockChainMessage;
import org.tron.core.net.peer.PeerConnection;

import java.util.ArrayList;

public class SyncBlockChainMsgHandlerTest {

  private SyncBlockChainMsgHandler handler = new SyncBlockChainMsgHandler();
  private PeerConnection peer = new PeerConnection();

  @Test
  public void testProcessMessage() {
    try {
      handler.processMessage(peer, new SyncBlockChainMessage(new ArrayList<>()));
    } catch (P2pException e) {
      Assert.assertTrue(e.getMessage().equals("SyncBlockChain blockIds is empty"));
    }
  }

}
