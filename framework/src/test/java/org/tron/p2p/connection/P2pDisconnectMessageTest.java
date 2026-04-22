package org.tron.p2p.connection;

import org.junit.Assert;
import org.junit.Test;
import org.tron.p2p.connection.message.base.P2pDisconnectMessage;
import org.tron.p2p.protos.Connect.DisconnectReason;

public class P2pDisconnectMessageTest {

  @Test
  public void testCreateFromReason() {
    P2pDisconnectMessage msg = new P2pDisconnectMessage(DisconnectReason.TOO_MANY_PEERS);
    Assert.assertNotNull(msg.getData());
    Assert.assertTrue(msg.getData().length > 0);
    Assert.assertTrue(msg.valid());
  }

  @Test
  public void testToString() {
    P2pDisconnectMessage msg = new P2pDisconnectMessage(DisconnectReason.DUPLICATE_PEER);
    String str = msg.toString();
    Assert.assertTrue(str.contains("reason:"));
    Assert.assertTrue(str.contains("DUPLICATE_PEER"));
  }

  @Test
  public void testCreateFromBytes() throws Exception {
    P2pDisconnectMessage original = new P2pDisconnectMessage(DisconnectReason.PING_TIMEOUT);
    byte[] data = original.getData();

    P2pDisconnectMessage parsed = new P2pDisconnectMessage(data);
    Assert.assertNotNull(parsed);
    Assert.assertTrue(parsed.valid());
  }

  @Test
  public void testDifferentReasons() {
    for (DisconnectReason reason : DisconnectReason.values()) {
      if (reason == DisconnectReason.UNRECOGNIZED) {
        continue;
      }
      P2pDisconnectMessage msg = new P2pDisconnectMessage(reason);
      Assert.assertTrue(msg.valid());
      Assert.assertNotNull(msg.getData());
    }
  }

  @Test
  public void testGetSendData() {
    P2pDisconnectMessage msg = new P2pDisconnectMessage(DisconnectReason.PEER_QUITING);
    byte[] sendData = msg.getSendData();
    Assert.assertNotNull(sendData);
    // First byte is the message type
    Assert.assertEquals(
        org.tron.p2p.connection.message.MessageType.DISCONNECT.getType(),
        sendData[0]);
  }

  @Test
  public void testNeedToLog() {
    P2pDisconnectMessage msg = new P2pDisconnectMessage(DisconnectReason.UNKNOWN);
    Assert.assertTrue(msg.needToLog());
  }
}
