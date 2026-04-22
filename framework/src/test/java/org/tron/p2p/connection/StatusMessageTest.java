package org.tron.p2p.connection;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.connection.message.MessageType;
import org.tron.p2p.connection.message.detect.StatusMessage;
import org.tron.p2p.discover.Node;

public class StatusMessageTest {

  @Before
  public void setUp() {
    Parameter.p2pConfig = new P2pConfig();
    ChannelManager.getChannels().clear();
  }

  @Test
  public void testCreateDefault() {
    StatusMessage msg = new StatusMessage();
    Assert.assertNotNull(msg.getData());
    Assert.assertEquals(MessageType.STATUS, msg.getType());
    Assert.assertEquals(Parameter.p2pConfig.getNetworkId(), msg.getNetworkId());
    Assert.assertTrue(msg.getTimestamp() > 0);
  }

  @Test
  public void testGetRemainConnections() {
    Parameter.p2pConfig.setMaxConnections(50);
    StatusMessage msg = new StatusMessage();
    // No channels, so remain = max - 0 = 50
    Assert.assertEquals(50, msg.getRemainConnections());
  }

  @Test
  public void testGetRemainConnectionsWithExistingChannels() throws Exception {
    Parameter.p2pConfig.setMaxConnections(50);
    // Add a fake channel
    Channel ch = new Channel();
    java.lang.reflect.Field field = ch.getClass().getDeclaredField("inetSocketAddress");
    field.setAccessible(true);
    field.set(ch, new java.net.InetSocketAddress("10.0.0.1", 100));
    ChannelManager.getChannels().put(
        (java.net.InetSocketAddress) field.get(ch), ch);

    StatusMessage msg = new StatusMessage();
    Assert.assertEquals(49, msg.getRemainConnections());

    ChannelManager.getChannels().clear();
  }

  @Test
  public void testGetFrom() {
    StatusMessage msg = new StatusMessage();
    Node from = msg.getFrom();
    Assert.assertNotNull(from);
  }

  @Test
  public void testToString() {
    StatusMessage msg = new StatusMessage();
    String str = msg.toString();
    Assert.assertTrue(str.startsWith("[StatusMessage:"));
  }

  @Test
  public void testValid() {
    StatusMessage msg = new StatusMessage();
    Assert.assertTrue(msg.valid());
  }

  @Test
  public void testCreateFromBytes() throws Exception {
    StatusMessage original = new StatusMessage();
    byte[] data = original.getData();
    StatusMessage parsed = new StatusMessage(data);
    Assert.assertEquals(original.getNetworkId(), parsed.getNetworkId());
    Assert.assertEquals(original.getTimestamp(), parsed.getTimestamp());
  }

  @Test
  public void testGetVersion() {
    StatusMessage msg = new StatusMessage();
    // Version defaults to 0 since we don't set it
    Assert.assertEquals(0, msg.getVersion());
  }

  @Test
  public void testGetSendData() {
    StatusMessage msg = new StatusMessage();
    byte[] sendData = msg.getSendData();
    Assert.assertNotNull(sendData);
    Assert.assertEquals(MessageType.STATUS.getType(), sendData[0]);
  }
}
