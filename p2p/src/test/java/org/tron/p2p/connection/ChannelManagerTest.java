package org.tron.p2p.connection;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.connection.business.handshake.DisconnectCode;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;

@Slf4j(topic = "net")
public class ChannelManagerTest {

  @Test
  public synchronized void testGetConnectionNum() throws Exception{
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
}
