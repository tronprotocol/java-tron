package org.tron.core.net.peer;

import static org.mockito.Mockito.mock;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.tron.common.TestConstants;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.PeerManagerStateResetter;
import org.tron.common.utils.ReflectUtils;
import org.tron.core.config.args.Args;
import org.tron.p2p.connection.Channel;

public class PeerManagerTest {

  @BeforeClass
  public static void initArgs() {
    PeerManagerStateResetter.reset();
    Args.setParam(new String[]{}, TestConstants.TEST_CONF);
    CommonParameter.getInstance().setRateLimiterSyncBlockChain(10);
    CommonParameter.getInstance().setRateLimiterFetchInvData(10);
    CommonParameter.getInstance().setRateLimiterDisconnect(10);
  }

  @AfterClass
  public static void destroy() {
    Args.clearParam();
  }

  @After
  public void clearPeers() {
    for (PeerConnection p : PeerManager.getPeers()) {
      PeerManager.remove(p.getChannel());
    }
  }

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

    p1.setChannel(c1);

    ApplicationContext ctx = mock(ApplicationContext.class);
    Mockito.when(ctx.getBean(PeerConnection.class)).thenReturn(p1);

    PeerManager.add(ctx, c1);
    PeerConnection p = PeerManager.getPeerConnection(c1);
    Assert.assertTrue(p != null);
  }

  @Test
  public void sameAddressChannelsRemainDistinctInEitherRemovalOrder() {
    for (boolean removeFirst : new boolean[]{false, true}) {
      PeerManagerStateResetter.reset();
      InetSocketAddress address = new InetSocketAddress("127.0.0.2", 10001);
      Channel first = new Channel();
      Channel second = new Channel();
      ReflectUtils.setFieldValue(first, "inetSocketAddress", address);
      ReflectUtils.setFieldValue(first, "inetAddress", address.getAddress());
      ReflectUtils.setFieldValue(first, "isActive", true);
      ReflectUtils.setFieldValue(second, "inetSocketAddress", address);
      ReflectUtils.setFieldValue(second, "inetAddress", address.getAddress());
      ReflectUtils.setFieldValue(second, "isActive", false);

      PeerConnection firstPeer = new PeerConnection();
      PeerConnection secondPeer = new PeerConnection();
      ApplicationContext ctx = mock(ApplicationContext.class);
      Mockito.when(ctx.getBean(PeerConnection.class)).thenReturn(firstPeer, secondPeer);
      Assert.assertSame(firstPeer, PeerManager.add(ctx, first));
      Assert.assertSame(secondPeer, PeerManager.add(ctx, second));
      Assert.assertNull(PeerManager.add(ctx, first));
      Assert.assertSame(firstPeer, PeerManager.getPeerConnection(first));
      Assert.assertSame(secondPeer, PeerManager.getPeerConnection(second));
      Assert.assertEquals(2, PeerManager.getPeers().size());
      Assert.assertEquals(1, PeerManager.getActivePeersCount().get());
      Assert.assertEquals(1, PeerManager.getPassivePeersCount().get());

      Channel removed = removeFirst ? first : second;
      Channel survivor = removeFirst ? second : first;
      PeerConnection survivorPeer = removeFirst ? secondPeer : firstPeer;
      Assert.assertNotNull(PeerManager.remove(removed));
      Assert.assertNull(PeerManager.remove(removed));
      Assert.assertSame(survivorPeer, PeerManager.getPeerConnection(survivor));
      Assert.assertEquals(1, PeerManager.getPeers().size());
      Assert.assertEquals(survivor.isActive() ? 1 : 0,
          PeerManager.getActivePeersCount().get());
      Assert.assertEquals(survivor.isActive() ? 0 : 1,
          PeerManager.getPassivePeersCount().get());
      Assert.assertSame(survivorPeer, PeerManager.remove(survivor));
      Assert.assertTrue(PeerManager.getPeers().isEmpty());
      Assert.assertEquals(0, PeerManager.getActivePeersCount().get());
      Assert.assertEquals(0, PeerManager.getPassivePeersCount().get());
    }
  }

  @Test
  public void timedOutSameAddressPeerDoesNotRemoveItsSurvivor() throws Exception {
    PeerManagerStateResetter.reset();
    InetSocketAddress address = new InetSocketAddress("127.0.0.2", 10001);
    Channel survivor = new Channel();
    Channel timedOut = new Channel();
    ReflectUtils.setFieldValue(survivor, "inetSocketAddress", address);
    ReflectUtils.setFieldValue(survivor, "inetAddress", address.getAddress());
    ReflectUtils.setFieldValue(survivor, "isActive", true);
    ReflectUtils.setFieldValue(timedOut, "inetSocketAddress", address);
    ReflectUtils.setFieldValue(timedOut, "inetAddress", address.getAddress());
    ReflectUtils.setFieldValue(timedOut, "disconnectTime", System.currentTimeMillis() - 61_000);

    PeerConnection survivorPeer = new PeerConnection();
    PeerConnection timedOutPeer = Mockito.spy(new PeerConnection());
    Mockito.doNothing().when(timedOutPeer).onDisconnect();
    ApplicationContext ctx = mock(ApplicationContext.class);
    Mockito.when(ctx.getBean(PeerConnection.class)).thenReturn(survivorPeer, timedOutPeer);
    PeerManager.add(ctx, survivor);
    PeerManager.add(ctx, timedOut);

    Method check = PeerManager.class.getDeclaredMethod("check");
    check.setAccessible(true);
    check.invoke(null);

    Assert.assertSame(survivorPeer, PeerManager.getPeerConnection(survivor));
    Assert.assertNull(PeerManager.getPeerConnection(timedOut));
    Assert.assertEquals(1, PeerManager.getPeers().size());
    Assert.assertEquals(1, PeerManager.getActivePeersCount().get());
    Assert.assertEquals(0, PeerManager.getPassivePeersCount().get());
    Mockito.verify(timedOutPeer).onDisconnect();
    PeerManager.remove(survivor);
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

  @Test
  public void checkDoesNotRemoveOrCleanUpPeerAfterDisconnectCallback() throws Exception {
    assertCheckDoesNotRemovePeerTwice(true);
    assertCheckDoesNotRemovePeerTwice(false);
  }

  private void assertCheckDoesNotRemovePeerTwice(boolean active) throws Exception {
    AtomicReference<PeerConnection> removedByCallback = new AtomicReference<>();
    Channel channel = new Channel() {
      @Override
      public boolean isActive() {
        return active;
      }

      @Override
      public long getDisconnectTime() {
        removedByCallback.set(PeerManager.remove(this));
        return System.currentTimeMillis() - 120_000;
      }
    };
    PeerConnection peer = mock(PeerConnection.class);
    Mockito.when(peer.getChannel()).thenReturn(channel);

    Field peersField = PeerManager.class.getDeclaredField("peers");
    peersField.setAccessible(true);
    peersField.set(null, Collections.synchronizedList(
        new ArrayList<>(Collections.singletonList(peer))));
    PeerManager.getActivePeersCount().set(active ? 1 : 0);
    PeerManager.getPassivePeersCount().set(active ? 0 : 1);

    Method check = PeerManager.class.getDeclaredMethod("check");
    check.setAccessible(true);
    try {
      check.invoke(null);

      Assert.assertSame(peer, removedByCallback.get());
      Assert.assertEquals(0, PeerManager.getActivePeersCount().get());
      Assert.assertEquals(0, PeerManager.getPassivePeersCount().get());
      Mockito.verify(peer, Mockito.never()).onDisconnect();
    } finally {
      peersField.set(null, Collections.synchronizedList(new ArrayList<>()));
      PeerManager.getActivePeersCount().set(0);
      PeerManager.getPassivePeersCount().set(0);
    }
  }

}
