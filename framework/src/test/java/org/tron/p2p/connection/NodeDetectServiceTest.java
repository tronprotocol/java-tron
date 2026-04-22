package org.tron.p2p.connection;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.connection.business.detect.NodeDetectService;
import org.tron.p2p.connection.business.detect.NodeStat;
import org.tron.p2p.connection.message.detect.StatusMessage;
import org.tron.p2p.discover.Node;

public class NodeDetectServiceTest {

  private NodeDetectService service;

  @Before
  public void setUp() {
    Parameter.p2pConfig = new P2pConfig();
    service = new NodeDetectService();
  }

  @After
  public void tearDown() {
    service.close();
    NodeDetectService.getBadNodesCache().invalidateAll();
  }

  @Test
  public void testInitDisabled() {
    Parameter.p2pConfig.setNodeDetectEnable(false);
    service.init(null);
    // Should return without starting executor
  }

  @Test
  public void testClose() {
    service.close();
    // Should not throw
  }

  @Test
  public void testTrimNodeMapRemovesTimedOut() throws Exception {
    Map<InetSocketAddress, NodeStat> nodeStatMap = getNodeStatMap();

    InetSocketAddress addr = new InetSocketAddress("10.0.0.1", 100);
    Node node = new Node(addr);
    NodeStat stat = new NodeStat(node);
    // Set lastDetectTime far in the past and make it not finished
    stat.setLastDetectTime(System.currentTimeMillis() - 10000);
    stat.setLastSuccessDetectTime(0);
    nodeStatMap.put(addr, stat);

    service.trimNodeMap();

    Assert.assertFalse(nodeStatMap.containsKey(addr));
    Assert.assertNotNull(NodeDetectService.getBadNodesCache().getIfPresent(addr.getAddress()));
  }

  @Test
  public void testTrimNodeMapKeepsFinished() throws Exception {
    Map<InetSocketAddress, NodeStat> nodeStatMap = getNodeStatMap();

    InetSocketAddress addr = new InetSocketAddress("10.0.0.2", 100);
    Node node = new Node(addr);
    NodeStat stat = new NodeStat(node);
    long now = System.currentTimeMillis();
    stat.setLastDetectTime(now - 10000);
    stat.setLastSuccessDetectTime(now - 10000); // finishDetect() returns true
    nodeStatMap.put(addr, stat);

    service.trimNodeMap();

    Assert.assertTrue(nodeStatMap.containsKey(addr));
  }

  @Test
  public void testProcessMessagePassiveChannel() throws Exception {
    Channel channel = createChannelWithMockCtx("10.0.0.3", 100, false);

    StatusMessage statusMsg = new StatusMessage();
    service.processMessage(channel, statusMsg);

    Assert.assertTrue(channel.isDiscoveryMode());
  }

  @Test
  public void testProcessMessageActiveChannelNotInMap() throws Exception {
    Channel channel = createChannelWithMockCtx("10.0.0.4", 100, true);

    StatusMessage statusMsg = new StatusMessage();
    service.processMessage(channel, statusMsg);
    // nodeStat is null, should return early
  }

  @Test
  public void testProcessMessageActiveChannelTimedOut() throws Exception {
    Map<InetSocketAddress, NodeStat> nodeStatMap = getNodeStatMap();

    InetSocketAddress addr = new InetSocketAddress("10.0.0.5", 100);
    Node node = new Node(addr);
    NodeStat stat = new NodeStat(node);
    // Set detect time far in the past (> NODE_DETECT_TIMEOUT)
    stat.setLastDetectTime(System.currentTimeMillis() - 5000);
    nodeStatMap.put(addr, stat);

    Channel channel = createChannelWithMockCtx("10.0.0.5", 100, true);

    StatusMessage statusMsg = new StatusMessage();
    service.processMessage(channel, statusMsg);

    // Should be removed from nodeStatMap and added to bad cache
    Assert.assertFalse(nodeStatMap.containsKey(addr));
  }

  @Test
  public void testNotifyDisconnectPassiveChannel() throws Exception {
    Channel channel = createChannelWithMockCtx("10.0.0.6", 100, false);
    service.notifyDisconnect(channel);
    // Should return early because not active
  }

  @Test
  public void testNotifyDisconnectNullAddress() throws Exception {
    Channel channel = new Channel();
    Field field = channel.getClass().getDeclaredField("isActive");
    field.setAccessible(true);
    field.set(channel, true);

    service.notifyDisconnect(channel);
    // Should return early because inetSocketAddress is null
  }

  @Test
  public void testNotifyDisconnectNotInMap() throws Exception {
    Channel channel = createChannelWithMockCtx("10.0.0.7", 100, true);
    service.notifyDisconnect(channel);
    // nodeStat is null, should return early
  }

  @Test
  public void testNotifyDisconnectFailedDetect() throws Exception {
    Map<InetSocketAddress, NodeStat> nodeStatMap = getNodeStatMap();

    InetSocketAddress addr = new InetSocketAddress("10.0.0.8", 100);
    Node node = new Node(addr);
    NodeStat stat = new NodeStat(node);
    stat.setLastDetectTime(100);
    stat.setLastSuccessDetectTime(50); // different = failed detect
    nodeStatMap.put(addr, stat);

    Channel channel = createChannelWithMockCtx("10.0.0.8", 100, true);

    service.notifyDisconnect(channel);

    Assert.assertFalse(nodeStatMap.containsKey(addr));
    Assert.assertNotNull(NodeDetectService.getBadNodesCache().getIfPresent(addr.getAddress()));
  }

  @Test
  public void testNotifyDisconnectSuccessfulDetect() throws Exception {
    Map<InetSocketAddress, NodeStat> nodeStatMap = getNodeStatMap();

    InetSocketAddress addr = new InetSocketAddress("10.0.0.9", 100);
    Node node = new Node(addr);
    NodeStat stat = new NodeStat(node);
    stat.setLastDetectTime(100);
    stat.setLastSuccessDetectTime(100); // same = successful detect
    nodeStatMap.put(addr, stat);

    Channel channel = createChannelWithMockCtx("10.0.0.9", 100, true);

    service.notifyDisconnect(channel);

    // Should NOT remove from map since detect was successful
    Assert.assertTrue(nodeStatMap.containsKey(addr));
  }

  @Test
  public void testGetConnectableNodesEmpty() {
    List<Node> nodes = service.getConnectableNodes();
    Assert.assertTrue(nodes.isEmpty());
  }

  @Test
  public void testGetConnectableNodesWithStats() throws Exception {
    Map<InetSocketAddress, NodeStat> nodeStatMap = getNodeStatMap();

    // Add a node with statusMessage set
    InetSocketAddress addr1 = new InetSocketAddress("10.0.0.10", 100);
    Node node1 = new Node(addr1);
    NodeStat stat1 = new NodeStat(node1);
    StatusMessage statusMsg1 = new StatusMessage();
    stat1.setStatusMessage(statusMsg1);
    nodeStatMap.put(addr1, stat1);

    // Add a node without statusMessage
    InetSocketAddress addr2 = new InetSocketAddress("10.0.0.11", 100);
    Node node2 = new Node(addr2);
    NodeStat stat2 = new NodeStat(node2);
    nodeStatMap.put(addr2, stat2);

    List<Node> nodes = service.getConnectableNodes();
    Assert.assertEquals(1, nodes.size());
  }

  @Test
  public void testTrimNodeMapKeepsRecentNotFinished() throws Exception {
    Map<InetSocketAddress, NodeStat> nodeStatMap = getNodeStatMap();

    InetSocketAddress addr = new InetSocketAddress("10.0.0.20", 100);
    Node node = new Node(addr);
    NodeStat stat = new NodeStat(node);
    // Set detect time very recently (within timeout) and not finished
    stat.setLastDetectTime(System.currentTimeMillis());
    stat.setLastSuccessDetectTime(0);
    nodeStatMap.put(addr, stat);

    service.trimNodeMap();

    // Should NOT be removed because detect time is recent (within 2s timeout)
    Assert.assertTrue(nodeStatMap.containsKey(addr));
  }

  @SuppressWarnings("unchecked")
  private Map<InetSocketAddress, NodeStat> getNodeStatMap() throws Exception {
    Field field = service.getClass().getDeclaredField("nodeStatMap");
    field.setAccessible(true);
    return (Map<InetSocketAddress, NodeStat>) field.get(service);
  }

  private Channel createChannelWithMockCtx(
      String ip, int port, boolean active) throws Exception {
    Channel channel = new Channel();
    InetSocketAddress addr = new InetSocketAddress(ip, port);
    setFieldValue(channel, "inetSocketAddress", addr);
    setFieldValue(channel, "inetAddress", addr.getAddress());
    if (active) {
      setFieldValue(channel, "isActive", true);
    }

    ChannelHandlerContext mockCtx = mock(ChannelHandlerContext.class);
    io.netty.channel.Channel mockNettyChannel = mock(io.netty.channel.Channel.class);
    when(mockCtx.channel()).thenReturn(mockNettyChannel);
    when(mockNettyChannel.remoteAddress()).thenReturn(addr);
    ChannelFuture mockFuture = mock(ChannelFuture.class);
    when(mockCtx.writeAndFlush(org.mockito.Mockito.any())).thenReturn(mockFuture);
    when(mockFuture.addListener(org.mockito.Mockito.any())).thenReturn(mockFuture);
    when(mockCtx.close()).thenReturn(mockFuture);
    when(mockNettyChannel.close()).thenReturn(mockFuture);
    setFieldValue(channel, "ctx", mockCtx);

    return channel;
  }

  private void setFieldValue(Object obj, String fieldName, Object value) throws Exception {
    Field field = obj.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(obj, value);
  }
}
