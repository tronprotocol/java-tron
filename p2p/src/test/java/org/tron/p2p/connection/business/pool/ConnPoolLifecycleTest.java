package org.tron.p2p.connection.business.pool;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.List;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.connection.Channel;
import org.tron.p2p.connection.ChannelManager;

/**
 * The pool's active/passive bookkeeping. Those two counters drive how many
 * outbound slots connect() tries to fill, so a miscount either starves the node
 * of peers or makes it dial forever.
 */
public class ConnPoolLifecycleTest {

  private P2pConfig saved;
  private ConnPoolService service;

  private static Channel channelAt(String ip, int port, boolean active) throws Exception {
    Channel channel = new Channel();
    InetSocketAddress address = new InetSocketAddress(ip, port);
    Field socket = Channel.class.getDeclaredField("inetSocketAddress");
    socket.setAccessible(true);
    socket.set(channel, address);
    Field inet = Channel.class.getDeclaredField("inetAddress");
    inet.setAccessible(true);
    inet.set(channel, address.getAddress());
    Field isActive = Channel.class.getDeclaredField("isActive");
    isActive.setAccessible(true);
    isActive.set(channel, active);
    return channel;
  }

  @SuppressWarnings("unchecked")
  private List<Channel> activePeers() throws Exception {
    Field field = ConnPoolService.class.getDeclaredField("activePeers");
    field.setAccessible(true);
    return (List<Channel>) field.get(service);
  }

  private int counter(String name) throws Exception {
    Field field = ConnPoolService.class.getDeclaredField(name);
    field.setAccessible(true);
    return ((java.util.concurrent.atomic.AtomicInteger) field.get(service)).get();
  }

  @Before
  public void setUp() {
    saved = Parameter.p2pConfig;
    P2pConfig config = new P2pConfig();
    config.setMaxConnections(10);
    config.setMaxConnectionsWithSameIp(5);
    Parameter.p2pConfig = config;
    ChannelManager.getChannels().clear();
    service = new ConnPoolService();
  }

  @After
  public void tearDown() {
    ChannelManager.getChannels().clear();
    Parameter.p2pConfig = saved;
  }

  @Test
  public void anOutboundPeerCountsAsActive() throws Exception {
    Channel peer = channelAt("127.0.0.1", 10001, true);
    service.onConnect(peer);

    Assert.assertEquals(1, activePeers().size());
    Assert.assertEquals(1, counter("activePeersCount"));
    Assert.assertEquals(0, counter("passivePeersCount"));
  }

  @Test
  public void anInboundPeerCountsAsPassive() throws Exception {
    Channel peer = channelAt("127.0.0.1", 10001, false);
    service.onConnect(peer);

    Assert.assertEquals(1, counter("passivePeersCount"));
    Assert.assertEquals(0, counter("activePeersCount"));
  }

  @Test
  public void connectingTheSamePeerTwiceCountsOnce() throws Exception {
    Channel peer = channelAt("127.0.0.1", 10001, true);
    service.onConnect(peer);
    service.onConnect(peer);

    Assert.assertEquals(1, activePeers().size());
    Assert.assertEquals(1, counter("activePeersCount"));
  }

  @Test
  public void disconnectReversesTheCount() throws Exception {
    Channel active = channelAt("127.0.0.1", 10001, true);
    Channel passive = channelAt("127.0.0.2", 10002, false);
    service.onConnect(active);
    service.onConnect(passive);

    service.onDisconnect(active);
    Assert.assertEquals(0, counter("activePeersCount"));
    Assert.assertEquals(1, counter("passivePeersCount"));

    service.onDisconnect(passive);
    Assert.assertEquals(0, counter("passivePeersCount"));
    Assert.assertTrue(activePeers().isEmpty());
  }

  @Test
  public void disconnectingAnUnknownPeerIsANoOp() throws Exception {
    service.onDisconnect(channelAt("127.0.0.9", 10009, true));

    Assert.assertEquals(0, counter("activePeersCount"));
    Assert.assertEquals(0, counter("passivePeersCount"));
    Assert.assertTrue(activePeers().isEmpty());
  }

  @Test
  public void onMessageIsInert() throws Exception {
    Channel peer = channelAt("127.0.0.1", 10001, true);
    service.onConnect(peer);

    service.onMessage(peer, new byte[] {1, 2, 3});

    Assert.assertEquals(1, activePeers().size());
  }
}
