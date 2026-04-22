package org.tron.p2p.connection;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.connection.socket.PeerClient;

public class PeerClientTest {

  @Before
  public void setUp() {
    Parameter.p2pConfig = new P2pConfig();
  }

  @Test
  public void testConnectAsyncWhenShutdown() throws Exception {
    PeerClient client = new PeerClient();
    client.init();

    // Set isShutdown to true
    boolean originalShutdown = ChannelManager.isShutdown;
    ChannelManager.isShutdown = true;

    try {
      java.net.InetSocketAddress addr = new java.net.InetSocketAddress("10.0.0.1", 100);
      org.tron.p2p.discover.Node node = new org.tron.p2p.discover.Node(addr);
      io.netty.channel.ChannelFuture result = client.connectAsync(node, false);

      // connectAsync internal method should return null when shutdown
      Assert.assertNull(result);
    } finally {
      ChannelManager.isShutdown = originalShutdown;
      client.close();
    }
  }

  @Test
  public void testConnectNodeWhenShutdown() throws Exception {
    PeerClient client = new PeerClient();
    client.init();

    boolean originalShutdown = ChannelManager.isShutdown;
    ChannelManager.isShutdown = true;

    try {
      java.net.InetSocketAddress addr = new java.net.InetSocketAddress("10.0.0.2", 100);
      org.tron.p2p.discover.Node node = new org.tron.p2p.discover.Node(addr);

      io.netty.channel.ChannelFuture result = client.connect(node, null);
      Assert.assertNull(result);
    } finally {
      ChannelManager.isShutdown = originalShutdown;
      client.close();
    }
  }

  @Test
  public void testInitAndClose() {
    PeerClient client = new PeerClient();
    client.init();
    client.close();
    // Should not throw
  }
}
