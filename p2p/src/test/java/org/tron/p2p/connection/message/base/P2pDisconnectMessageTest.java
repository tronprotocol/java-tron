package org.tron.p2p.connection.message.base;

import org.junit.Assert;
import org.junit.Test;
import org.tron.p2p.connection.message.MessageType;
import org.tron.p2p.protos.Connect.DisconnectReason;

public class P2pDisconnectMessageTest {

  @Test
  public void roundTripsEveryReason() throws Exception {
    for (DisconnectReason reason : DisconnectReason.values()) {
      if (reason == DisconnectReason.UNRECOGNIZED) {
        continue;
      }
      P2pDisconnectMessage sent = new P2pDisconnectMessage(reason);
      Assert.assertEquals(MessageType.DISCONNECT, sent.getType());
      Assert.assertTrue(sent.valid());

      P2pDisconnectMessage parsed = new P2pDisconnectMessage(sent.getData());
      Assert.assertTrue("reason should appear in toString for " + reason,
          parsed.toString().contains(reason.toString()));
      Assert.assertTrue(parsed.valid());
    }
  }

  @Test(expected = Exception.class)
  public void malformedBytesAreRejected() throws Exception {
    new P2pDisconnectMessage(new byte[] {(byte) 0xFF, (byte) 0xFF, 0x7F});
  }
}
