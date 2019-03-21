package org.tron.core.net;

import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.overlay.message.DisconnectMessage;
import org.tron.core.net.message.MessageTypes;
import org.tron.protos.Protocol.ReasonCode;

public class MessageTest {

  private DisconnectMessage disconnectMessage;

  @Test
  public void test1() throws Exception {
    byte[] bytes = new DisconnectMessage(ReasonCode.TOO_MANY_PEERS).getData();
    DisconnectMessageTest disconnectMessageTest = new DisconnectMessageTest();
    disconnectMessage = new DisconnectMessage(MessageTypes.P2P_DISCONNECT.asByte(),
        disconnectMessageTest.toByteArray());
    Assert.assertTrue(Arrays.equals(bytes, disconnectMessage.getData()));
  }

}
