package org.tron.p2p.connection.message.handshake;

import static org.tron.p2p.base.Parameter.p2pConfig;

import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.connection.business.handshake.DisconnectCode;
import org.tron.p2p.connection.message.MessageType;

public class HelloMessageTest {

  @Test
  public void testHelloMessage() throws Exception {
    p2pConfig = new P2pConfig();
    HelloMessage m1 = new HelloMessage(DisconnectCode.NORMAL, 0);
    Assert.assertEquals(0, m1.getCode());

    Assert.assertTrue(Arrays.equals(p2pConfig.getNodeID(), m1.getFrom().getId()));
    Assert.assertEquals(p2pConfig.getPort(), m1.getFrom().getPort());
    Assert.assertEquals(p2pConfig.getIp(), m1.getFrom().getHostV4());
    Assert.assertEquals(p2pConfig.getNetworkId(), m1.getNetworkId());
    Assert.assertEquals(MessageType.HANDSHAKE_HELLO, m1.getType());

    HelloMessage m2 = new HelloMessage(m1.getData());
    Assert.assertTrue(Arrays.equals(p2pConfig.getNodeID(), m2.getFrom().getId()));
    Assert.assertEquals(p2pConfig.getPort(), m2.getFrom().getPort());
    Assert.assertEquals(p2pConfig.getIp(), m2.getFrom().getHostV4());
    Assert.assertEquals(p2pConfig.getNetworkId(), m2.getNetworkId());
    Assert.assertEquals(MessageType.HANDSHAKE_HELLO, m2.getType());
  }
}
