package org.tron.p2p.connection;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Map;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.connection.business.handshake.DisconnectCode;
import org.tron.p2p.protos.Connect.DisconnectReason;
import org.tron.p2p.utils.ByteArray;
import org.tron.p2p.utils.NetUtil;

/**
 * processPeer is the admission decision for every inbound connection: ban list,
 * global cap, per-IP cap, then duplicate-nodeId resolution. Each branch here is
 * what stops a single host from taking every slot.
 */
public class ChannelManagerAdmissionTest {

  private P2pConfig saved;
  private Map<InetSocketAddress, Channel> channels;

  private static Channel channelAt(String ip, int port) throws Exception {
    Channel channel = new Channel();
    InetSocketAddress address = new InetSocketAddress(ip, port);
    Field socket = Channel.class.getDeclaredField("inetSocketAddress");
    socket.setAccessible(true);
    socket.set(channel, address);
    Field inet = Channel.class.getDeclaredField("inetAddress");
    inet.setAccessible(true);
    inet.set(channel, address.getAddress());
    return channel;
  }

  private static void setNodeId(Channel channel, String nodeId) throws Exception {
    Field field = Channel.class.getDeclaredField("nodeId");
    field.setAccessible(true);
    field.set(channel, nodeId);
  }

  private static void setStartTime(Channel channel, long startTime) throws Exception {
    Field field = Channel.class.getDeclaredField("startTime");
    field.setAccessible(true);
    field.set(channel, startTime);
  }

  @Before
  public void setUp() {
    saved = Parameter.p2pConfig;
    P2pConfig config = new P2pConfig();
    config.setMaxConnections(2);
    config.setMaxConnectionsWithSameIp(1);
    Parameter.p2pConfig = config;

    channels = ChannelManager.getChannels();
    channels.clear();
    ChannelManager.getBannedNodes().invalidateAll();
  }

  @After
  public void tearDown() {
    channels.clear();
    ChannelManager.getBannedNodes().invalidateAll();
    Parameter.p2pConfig = saved;
  }

  @Test
  public void aFreshPeerIsAdmitted() throws Exception {
    Channel channel = channelAt("127.0.0.1", 10001);
    Assert.assertEquals(DisconnectCode.NORMAL, ChannelManager.processPeer(channel));
    Assert.assertSame(channel, channels.get(channel.getInetSocketAddress()));
  }

  @Test
  public void aRecentlyBannedPeerIsRefused() throws Exception {
    Channel channel = channelAt("127.0.0.1", 10001);
    ChannelManager.banNode(channel.getInetAddress(), 60_000L);

    Assert.assertEquals(DisconnectCode.TIME_BANNED, ChannelManager.processPeer(channel));
    Assert.assertTrue(channels.isEmpty());
  }

  @Test
  public void anExpiredBanNoLongerBlocks() throws Exception {
    Channel channel = channelAt("127.0.0.1", 10001);
    // banNode stores an absolute expiry; a zero ban is already in the past.
    ChannelManager.banNode(channel.getInetAddress(), 0L);

    Assert.assertEquals(DisconnectCode.NORMAL, ChannelManager.processPeer(channel));
  }

  @Test
  public void theGlobalCapIsEnforced() throws Exception {
    Assert.assertEquals(DisconnectCode.NORMAL,
        ChannelManager.processPeer(channelAt("127.0.0.1", 10001)));
    Assert.assertEquals(DisconnectCode.NORMAL,
        ChannelManager.processPeer(channelAt("127.0.0.2", 10002)));

    // maxConnections is 2, so the third is refused.
    Assert.assertEquals(DisconnectCode.TOO_MANY_PEERS,
        ChannelManager.processPeer(channelAt("127.0.0.3", 10003)));
  }

  @Test
  public void thePerIpCapIsEnforced() throws Exception {
    Parameter.p2pConfig.setMaxConnections(10);
    Assert.assertEquals(DisconnectCode.NORMAL,
        ChannelManager.processPeer(channelAt("127.0.0.1", 10001)));

    // maxConnectionsWithSameIp is 1, so a second socket from the same host is
    // refused even though the global cap has room.
    Assert.assertEquals(DisconnectCode.MAX_CONNECTION_WITH_SAME_IP,
        ChannelManager.processPeer(channelAt("127.0.0.1", 10002)));
  }

  @Test
  public void connectionNumCountsOnlyTheSameAddress() throws Exception {
    ChannelManager.processPeer(channelAt("127.0.0.1", 10001));
    Assert.assertEquals(1,
        ChannelManager.getConnectionNum(InetAddress.getByName("127.0.0.1")));
    Assert.assertEquals(0,
        ChannelManager.getConnectionNum(InetAddress.getByName("127.0.0.2")));
  }

  @Test
  public void theOlderConnectionWinsADuplicateNodeId() throws Exception {
    Parameter.p2pConfig.setMaxConnections(10);
    Parameter.p2pConfig.setMaxConnectionsWithSameIp(10);
    String nodeId = ByteArray.toHexString(NetUtil.getNodeId());

    Channel first = channelAt("127.0.0.1", 10001);
    setNodeId(first, nodeId);
    setStartTime(first, 1000L);
    Assert.assertEquals(DisconnectCode.NORMAL, ChannelManager.processPeer(first));

    // The newcomer started later, so it is the duplicate and is refused.
    Channel later = channelAt("127.0.0.2", 10002);
    setNodeId(later, nodeId);
    setStartTime(later, 2000L);
    Assert.assertEquals(DisconnectCode.DUPLICATE_PEER, ChannelManager.processPeer(later));
  }

  @Test
  public void everyDisconnectCodeMapsToAReason() {
    Assert.assertEquals(DisconnectReason.DIFFERENT_VERSION,
        ChannelManager.getDisconnectReason(DisconnectCode.DIFFERENT_VERSION));
    Assert.assertEquals(DisconnectReason.RECENT_DISCONNECT,
        ChannelManager.getDisconnectReason(DisconnectCode.TIME_BANNED));
    Assert.assertEquals(DisconnectReason.DUPLICATE_PEER,
        ChannelManager.getDisconnectReason(DisconnectCode.DUPLICATE_PEER));
    Assert.assertEquals(DisconnectReason.TOO_MANY_PEERS,
        ChannelManager.getDisconnectReason(DisconnectCode.TOO_MANY_PEERS));
    Assert.assertEquals(DisconnectReason.TOO_MANY_PEERS_WITH_SAME_IP,
        ChannelManager.getDisconnectReason(DisconnectCode.MAX_CONNECTION_WITH_SAME_IP));
    Assert.assertEquals(DisconnectReason.UNKNOWN,
        ChannelManager.getDisconnectReason(DisconnectCode.NORMAL));
  }

  @Test
  public void banNodeKeepsTheLongerOfTwoBans() throws Exception {
    InetAddress address = InetAddress.getByName("127.0.0.9");
    ChannelManager.banNode(address, 60_000L);
    Long first = ChannelManager.getBannedNodes().getIfPresent(address);

    // A shorter ban must not shorten an existing longer one.
    ChannelManager.banNode(address, 1L);
    Assert.assertEquals(first, ChannelManager.getBannedNodes().getIfPresent(address));
  }
}
