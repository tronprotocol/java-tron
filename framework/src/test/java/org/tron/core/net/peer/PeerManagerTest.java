package org.tron.core.net.peer;

import static org.mockito.Mockito.mock;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.tron.common.utils.ReflectUtils;
import org.tron.p2p.connection.Channel;

public class PeerManagerTest {
  List<InetSocketAddress> relayNodes = new ArrayList<>();

  @Test
  public void testAdd() throws Exception {
    Field field = PeerManager.class.getDeclaredField("peers");
    field.setAccessible(true);
    field.set(PeerManager.class, Collections.synchronizedList(new ArrayList<>()));

    PeerConnection p1 = new PeerConnection();
    InetSocketAddress inetSocketAddress1 =
        new InetSocketAddress("127.0.0.2", 10001);
    Channel c1 = new Channel();
    ReflectUtils.setFieldValue(c1, "inetSocketAddress", inetSocketAddress1);
    ReflectUtils.setFieldValue(c1, "inetAddress", inetSocketAddress1.getAddress());
    ReflectUtils.setFieldValue(p1, "relayNodes", relayNodes);
    p1.setChannel(c1);

    ApplicationContext ctx = mock(ApplicationContext.class);
    Mockito.when(ctx.getBean(PeerConnection.class)).thenReturn(p1);

    PeerConnection p = PeerManager.add(ctx, c1);
    Assert.assertTrue(p != null);

    p = PeerManager.add(ctx, c1);
    Assert.assertTrue(p == null);
  }

  @Test
  public void testRemove() throws Exception {
    Field field = PeerManager.class.getDeclaredField("peers");
    field.setAccessible(true);
    field.set(PeerManager.class, Collections.synchronizedList(new ArrayList<>()));

    PeerConnection p1 = new PeerConnection();
    InetSocketAddress inetSocketAddress1 =
        new InetSocketAddress("127.0.0.2", 10001);
    Channel c1 = new Channel();
    ReflectUtils.setFieldValue(c1, "inetSocketAddress", inetSocketAddress1);
    ReflectUtils.setFieldValue(c1, "inetAddress", inetSocketAddress1.getAddress());
    ReflectUtils.setFieldValue(p1, "relayNodes", relayNodes);
    p1.setChannel(c1);

    ApplicationContext ctx = mock(ApplicationContext.class);
    Mockito.when(ctx.getBean(PeerConnection.class)).thenReturn(p1);

    PeerConnection p = PeerManager.remove(c1);
    Assert.assertTrue(p == null);

    PeerManager.add(ctx, c1);
    p = PeerManager.remove(c1);
    Assert.assertTrue(p != null);
  }

  @Test
  public void testGetPeerConnection() throws Exception {
    Field field = PeerManager.class.getDeclaredField("peers");
    field.setAccessible(true);
    field.set(PeerManager.class, Collections.synchronizedList(new ArrayList<>()));

    PeerConnection p1 = new PeerConnection();
    InetSocketAddress inetSocketAddress1 =
        new InetSocketAddress("127.0.0.2", 10001);
    Channel c1 = new Channel();
    ReflectUtils.setFieldValue(c1, "inetSocketAddress", inetSocketAddress1);
    ReflectUtils.setFieldValue(c1, "inetAddress", inetSocketAddress1.getAddress());
    ReflectUtils.setFieldValue(p1, "relayNodes", relayNodes);
    p1.setChannel(c1);

    ApplicationContext ctx = mock(ApplicationContext.class);
    Mockito.when(ctx.getBean(PeerConnection.class)).thenReturn(p1);

    PeerManager.add(ctx, c1);
    PeerConnection p = PeerManager.getPeerConnection(c1);
    Assert.assertTrue(p != null);
  }

  @Test
  public void testGetPeers() throws Exception {
    Field field = PeerManager.class.getDeclaredField("peers");
    field.setAccessible(true);
    field.set(PeerManager.class, Collections.synchronizedList(new ArrayList<>()));

    PeerConnection p1 = new PeerConnection();
    InetSocketAddress inetSocketAddress1 =
        new InetSocketAddress("127.0.0.1", 10001);
    Channel c1 = new Channel();
    ReflectUtils.setFieldValue(c1, "inetSocketAddress", inetSocketAddress1);
    ReflectUtils.setFieldValue(c1, "inetAddress", inetSocketAddress1.getAddress());
    ReflectUtils.setFieldValue(p1, "relayNodes", relayNodes);
    p1.setChannel(c1);

    ApplicationContext ctx = mock(ApplicationContext.class);
    Mockito.when(ctx.getBean(PeerConnection.class)).thenReturn(p1);

    PeerConnection p = PeerManager.add(ctx, c1);
    Assert.assertTrue(p != null);

    List<PeerConnection> peers = PeerManager.getPeers();
    Assert.assertEquals(1, peers.size());

    PeerConnection p2 = new PeerConnection();
    InetSocketAddress inetSocketAddress2 =
        new InetSocketAddress("127.0.0.2", 10001);
    Channel c2 = new Channel();
    ReflectUtils.setFieldValue(c2, "inetSocketAddress", inetSocketAddress2);
    ReflectUtils.setFieldValue(c2, "inetAddress", inetSocketAddress2.getAddress());
    ReflectUtils.setFieldValue(p2, "relayNodes", relayNodes);
    p2.setChannel(c2);

    ApplicationContext ctx2 = mock(ApplicationContext.class);
    Mockito.when(ctx2.getBean(PeerConnection.class)).thenReturn(p2);

    p = PeerManager.add(ctx2, c2);
    Assert.assertTrue(p != null);

    peers = PeerManager.getPeers();
    Assert.assertEquals(2, peers.size());
  }

  @Test
  public void testSortPeers() throws Exception {
    PeerConnection p1 = new PeerConnection();
    PeerConnection p2 = new PeerConnection();

    List<PeerConnection> peers = new ArrayList<>();
    peers.add(p1);
    peers.add(p2);

    Field field = PeerManager.class.getDeclaredField("peers");
    field.setAccessible(true);
    field.set(PeerManager.class, Collections.synchronizedList(peers));

    PeerManager.sortPeers();

    Channel c1 = new Channel();
    c1.updateAvgLatency(100000L);
    ReflectUtils.setFieldValue(p1, "channel", c1);

    Channel c2 = new Channel();
    c2.updateAvgLatency(1000L);
    ReflectUtils.setFieldValue(p2, "channel", c2);

    PeerManager.sortPeers();

    Assert.assertEquals(PeerManager.getPeers().get(0), p2);
  }

}
