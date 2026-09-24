package org.tron.p2p.connection;

import com.google.protobuf.ByteString;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.Assert;
import org.junit.Test;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.connection.business.handshake.DisconnectCode;
import org.tron.p2p.connection.message.MessageType;
import org.tron.p2p.protos.Connect;
import org.tron.p2p.protos.Discover;

@Slf4j(topic = "net")
public class ChannelManagerTest {

  @Test
  public synchronized void testGetConnectionNum() throws Exception {
    Channel c1 = new Channel();
    InetSocketAddress a1 = new InetSocketAddress("100.1.1.1", 100);
    Field field =  c1.getClass().getDeclaredField("inetAddress");
    field.setAccessible(true);
    field.set(c1, a1.getAddress());

    Channel c2 = new Channel();
    InetSocketAddress a2 = new InetSocketAddress("100.1.1.2", 100);
    field =  c2.getClass().getDeclaredField("inetAddress");
    field.setAccessible(true);
    field.set(c2, a2.getAddress());

    Channel c3 = new Channel();
    InetSocketAddress a3 = new InetSocketAddress("100.1.1.2", 99);
    field =  c3.getClass().getDeclaredField("inetAddress");
    field.setAccessible(true);
    field.set(c3, a3.getAddress());

    int cnt = ChannelManager.getConnectionNum(a1.getAddress());
    Assert.assertTrue(cnt == 0);

    ChannelManager.getChannels().put(a1, c1);
    cnt = ChannelManager.getConnectionNum(a1.getAddress());
    Assert.assertTrue(cnt == 1);

    ChannelManager.getChannels().put(a2, c2);
    cnt = ChannelManager.getConnectionNum(a2.getAddress());
    Assert.assertTrue(cnt == 1);

    ChannelManager.getChannels().put(a3, c3);
    cnt = ChannelManager.getConnectionNum(a3.getAddress());
    Assert.assertTrue(cnt == 2);
  }

  @Test
  public synchronized void testNotifyDisconnect() throws Exception {
    Channel c1 = new Channel();
    InetSocketAddress a1 = new InetSocketAddress("100.1.1.1", 100);

    Field field =  c1.getClass().getDeclaredField("inetSocketAddress");
    field.setAccessible(true);
    field.set(c1, a1);

    InetAddress inetAddress = a1.getAddress();
    field =  c1.getClass().getDeclaredField("inetAddress");
    field.setAccessible(true);
    field.set(c1, inetAddress);

    ChannelManager.getChannels().put(a1, c1);

    Long time = ChannelManager.getBannedNodes().getIfPresent(a1.getAddress());
    Assert.assertTrue(ChannelManager.getChannels().size() == 1);
    Assert.assertTrue(time == null);

    ChannelManager.notifyDisconnect(c1);
    time = ChannelManager.getBannedNodes().getIfPresent(a1.getAddress());
    Assert.assertTrue(time != null);
    Assert.assertTrue(ChannelManager.getChannels().size() == 0);
  }

  @Test
  public synchronized void testProcessPeer() throws Exception {
    clearChannels();
    Parameter.p2pConfig = new P2pConfig();

    Channel c1 = new Channel();
    InetSocketAddress a1 = new InetSocketAddress("100.1.1.2", 100);

    Field field =  c1.getClass().getDeclaredField("inetSocketAddress");
    field.setAccessible(true);
    field.set(c1, a1);
    field =  c1.getClass().getDeclaredField("inetAddress");
    field.setAccessible(true);
    field.set(c1, a1.getAddress());

    DisconnectCode code = ChannelManager.processPeer(c1);
    Assert.assertTrue(code.equals(DisconnectCode.NORMAL));

    Thread.sleep(5);

    Parameter.p2pConfig.setMaxConnections(1);

    Channel c2 = new Channel();
    InetSocketAddress a2 = new InetSocketAddress("100.1.1.2", 99);

    field =  c2.getClass().getDeclaredField("inetSocketAddress");
    field.setAccessible(true);
    field.set(c2, a2);
    field =  c2.getClass().getDeclaredField("inetAddress");
    field.setAccessible(true);
    field.set(c2, a2.getAddress());

    code = ChannelManager.processPeer(c2);
    Assert.assertTrue(code.equals(DisconnectCode.TOO_MANY_PEERS));

    Parameter.p2pConfig.setMaxConnections(2);
    Parameter.p2pConfig.setMaxConnectionsWithSameIp(1);
    code = ChannelManager.processPeer(c2);
    Assert.assertTrue(code.equals(DisconnectCode.MAX_CONNECTION_WITH_SAME_IP));

    Parameter.p2pConfig.setMaxConnectionsWithSameIp(2);
    c1.setNodeId("cc");
    c2.setNodeId("cc");
    code = ChannelManager.processPeer(c2);
    Assert.assertTrue(code.equals(DisconnectCode.DUPLICATE_PEER));
  }

  private void clearChannels() {
    ChannelManager.getChannels().clear();
    ChannelManager.getBannedNodes().invalidateAll();
  }

  @Test
  public synchronized void testDiscoveryModeRejectsHelloMessage() throws Exception {
    clearChannels();
    Parameter.p2pConfig = new P2pConfig();

    Channel channel = new Channel();
    channel.setDiscoveryMode(true);

    InetSocketAddress addr = new InetSocketAddress("100.1.1.5", 18888);
    Field f = channel.getClass().getDeclaredField("inetSocketAddress");
    f.setAccessible(true);
    f.set(channel, addr);
    f = channel.getClass().getDeclaredField("inetAddress");
    f.setAccessible(true);
    f.set(channel, addr.getAddress());

    EmbeddedChannel ec = new EmbeddedChannel(new ChannelInboundHandlerAdapter());
    ChannelHandlerContext ctx = ec.pipeline().firstContext();
    f = channel.getClass().getDeclaredField("ctx");
    f.setAccessible(true);
    f.set(channel, ctx);

    byte[] helloBytes = buildHelloMessageBytes();

    ChannelManager.processMessage(channel, helloBytes);

    Assert.assertTrue(channel.isDisconnect());
    Assert.assertNull(channel.getHelloMessage());
    Assert.assertFalse(channel.isFinishHandshake());
    Assert.assertFalse(ChannelManager.getChannels().containsKey(addr));
  }

  private byte[] buildHelloMessageBytes() {
    Discover.Endpoint endpoint = Discover.Endpoint.newBuilder()
        .setNodeId(ByteString.copyFrom(new byte[64]))
        .setAddress(ByteString.copyFromUtf8("127.0.0.1"))
        .setPort(18888)
        .build();
    Connect.HelloMessage hello = Connect.HelloMessage.newBuilder()
        .setFrom(endpoint)
        .setNetworkId(1)
        .setCode(DisconnectCode.NORMAL.getValue())
        .setVersion(1)
        .setTimestamp(System.currentTimeMillis())
        .build();
    return ArrayUtils.add(hello.toByteArray(), 0, MessageType.HANDSHAKE_HELLO.getType());
  }
}
