package org.tron.p2p.connection.message.detect;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.connection.ChannelManager;
import org.tron.p2p.connection.message.MessageType;

/**
 * STATUS is the node-detect probe reply. Its remaining-connections figure is
 * what ConnPoolService uses to rank candidates, so the arithmetic matters.
 */
public class StatusMessageTest {

  private P2pConfig saved;

  @Before
  public void setUp() {
    saved = Parameter.p2pConfig;
    P2pConfig config = new P2pConfig();
    config.setPort(18888);
    config.setIp("127.0.0.1");
    config.setNetworkId(11111);
    config.setMaxConnections(30);
    Parameter.p2pConfig = config;
    ChannelManager.getChannels().clear();
  }

  @After
  public void tearDown() {
    ChannelManager.getChannels().clear();
    Parameter.p2pConfig = saved;
  }

  @Test
  public void roundTripsThroughItsWireBytes() throws Exception {
    StatusMessage sent = new StatusMessage();
    Assert.assertEquals(MessageType.STATUS, sent.getType());
    Assert.assertTrue(sent.valid());

    StatusMessage parsed = new StatusMessage(sent.getData());
    Assert.assertEquals(11111, parsed.getNetworkId());
    Assert.assertEquals(sent.getTimestamp(), parsed.getTimestamp());
    Assert.assertEquals("127.0.0.1", parsed.getFrom().getHostV4());
    Assert.assertEquals(18888, parsed.getFrom().getPort());
    Assert.assertTrue(parsed.valid());
    Assert.assertTrue(parsed.toString().startsWith("[StatusMessage"));
  }

  @Test
  public void remainingConnectionsIsMaxMinusCurrent() throws Exception {
    // No channels are registered, so the whole budget is free.
    StatusMessage empty = new StatusMessage(new StatusMessage().getData());
    Assert.assertEquals(30, empty.getRemainConnections());
  }

  @Test
  public void versionDefaultsToZeroWhenNotSet() throws Exception {
    StatusMessage parsed = new StatusMessage(new StatusMessage().getData());
    Assert.assertEquals(0, parsed.getVersion());
  }

  @Test(expected = Exception.class)
  public void malformedBytesAreRejected() throws Exception {
    new StatusMessage(new byte[] {(byte) 0xFF, (byte) 0xFF, 0x7F});
  }
}
