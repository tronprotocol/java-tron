package org.tron.core.net.services;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;

import io.netty.channel.ChannelHandlerContext;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Resource;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.tron.common.BaseTest;
import org.tron.common.TestConstants;
import org.tron.common.utils.ReflectUtils;
import org.tron.core.ChainBaseManager;
import org.tron.core.config.args.Args;
import org.tron.core.net.P2pEventHandlerImpl;
import org.tron.core.net.TronNetDelegate;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.peer.PeerManager;
import org.tron.core.net.service.effective.ResilienceService;
import org.tron.p2p.connection.Channel;
import org.tron.protos.Protocol.ReasonCode;

public class ResilienceServiceTest extends BaseTest {

  @Resource
  private ResilienceService service;

  @Resource
  private P2pEventHandlerImpl p2pEventHandler;


  @BeforeClass
  public static void init() throws IOException {
    Args.setParam(new String[] {"--output-directory", dbPath(), "--debug"},
        TestConstants.TEST_CONF);
  }

  @After
  public void clearPeers() {
    closePeer();
  }

  @Test
  public void testDisconnectRandom() {
    int maxConnection = 30;
    Assert.assertEquals(maxConnection, Args.getInstance().getMaxConnections());
    Assert.assertEquals(0, PeerManager.getPeers().size());

    ApplicationContext ctx = (ApplicationContext) ReflectUtils.getFieldObject(p2pEventHandler,
        "ctx");
    for (int i = 0; i < maxConnection + 1; i++) {
      InetSocketAddress inetSocketAddress = new InetSocketAddress("201.0.0." + i, 10001);
      Channel c1 = spy(Channel.class);
      ReflectUtils.setFieldValue(c1, "inetSocketAddress", inetSocketAddress);
      ReflectUtils.setFieldValue(c1, "inetAddress", inetSocketAddress.getAddress());
      ReflectUtils.setFieldValue(c1, "ctx", spy(ChannelHandlerContext.class));
      Mockito.doNothing().when(c1).send((byte[]) any());

      PeerManager.add(ctx, c1);
    }
    for (PeerConnection peer : PeerManager.getPeers()
        .subList(0, ResilienceService.minBroadcastPeerSize)) {
      peer.setNeedSyncFromPeer(false);
      peer.setNeedSyncFromUs(false);
      peer.setLastInteractiveTime(System.currentTimeMillis() - 1000);
    }
    for (PeerConnection peer : PeerManager.getPeers()
        .subList(ResilienceService.minBroadcastPeerSize, maxConnection + 1)) {
      peer.setNeedSyncFromPeer(false);
      peer.setNeedSyncFromUs(true);
    }
    int size1 = (int) PeerManager.getPeers().stream()
        .filter(peer -> !peer.isNeedSyncFromUs() && !peer.isNeedSyncFromPeer())
        .count();
    Assert.assertEquals(ResilienceService.minBroadcastPeerSize, size1);
    Assert.assertEquals(maxConnection + 1, PeerManager.getPeers().size());

    //disconnect from broadcasting peer
    ReflectUtils.invokeMethod(service, "disconnectRandom");
    size1 = (int) PeerManager.getPeers().stream()
        .filter(peer -> !peer.isNeedSyncFromUs() && !peer.isNeedSyncFromPeer())
        .count();
    Assert.assertEquals(ResilienceService.minBroadcastPeerSize - 1, size1);
    Assert.assertEquals(maxConnection, PeerManager.getPeers().size());

    //disconnect from syncing peer
    ReflectUtils.invokeMethod(service, "disconnectRandom");
    size1 = (int) PeerManager.getPeers().stream()
        .filter(peer -> !peer.isNeedSyncFromUs() && !peer.isNeedSyncFromPeer())
        .count();
    Assert.assertEquals(ResilienceService.minBroadcastPeerSize - 1, size1);
    Assert.assertEquals(maxConnection - 1, PeerManager.getPeers().size());
  }

  @Test
  public void testDisconnectRandomPreservesRecentBlockRcvTimePeer() {
    int maxConnection = 30;
    Assert.assertEquals(0, PeerManager.getPeers().size());

    ApplicationContext ctx = (ApplicationContext) ReflectUtils.getFieldObject(p2pEventHandler,
        "ctx");

    // Create maxConnection + 1 peers (triggers disconnectRandom)
    for (int i = 0; i < maxConnection + 1; i++) {
      InetSocketAddress inetSocketAddress = new InetSocketAddress("202.0.0." + i, 10001);
      Channel c1 = spy(Channel.class);
      ReflectUtils.setFieldValue(c1, "inetSocketAddress", inetSocketAddress);
      ReflectUtils.setFieldValue(c1, "inetAddress", inetSocketAddress.getAddress());
      ReflectUtils.setFieldValue(c1, "ctx", spy(ChannelHandlerContext.class));
      Mockito.doNothing().when(c1).send((byte[]) any());
      PeerManager.add(ctx, c1);
    }

    // Set first minBroadcastPeerSize peers as broadcast-state
    List<PeerConnection> peers = PeerManager.getPeers();
    for (PeerConnection peer : peers.subList(0, ResilienceService.minBroadcastPeerSize)) {
      peer.setNeedSyncFromPeer(false);
      peer.setNeedSyncFromUs(false);
      peer.setLastInteractiveTime(System.currentTimeMillis() - 1000);
    }
    for (PeerConnection peer : peers.subList(ResilienceService.minBroadcastPeerSize,
        maxConnection + 1)) {
      peer.setNeedSyncFromPeer(false);
      peer.setNeedSyncFromUs(true);
    }

    // Give the LAST broadcast peer a very recent blockRcvTime — it must NOT be disconnected
    PeerConnection bestPeer = peers.stream()
        .filter(p -> !p.isNeedSyncFromUs() && !p.isNeedSyncFromPeer())
        .reduce((a, b) -> b)  // last broadcast peer
        .orElseThrow(() -> new AssertionError("no broadcast peer"));
    bestPeer.setBlockRcvTime(System.currentTimeMillis());

    InetSocketAddress bestPeerAddress = bestPeer.getChannel().getInetSocketAddress();

    // With minBroadcastPeerSize=3 broadcast peers, getRandomDisconnectionPeers returns
    // the 1 peer with oldest blockRcvTime (0). bestPeer has most recent time → exempt.
    ReflectUtils.invokeMethod(service, "disconnectRandom");

    boolean bestPeerStillConnected = PeerManager.getPeers().stream()
        .anyMatch(p -> p.getChannel().getInetSocketAddress().equals(bestPeerAddress));
    Assert.assertTrue("Peer with most recent blockRcvTime should not be disconnected",
        bestPeerStillConnected);
  }

  @Test
  public void testDisconnectLan() {
    int minConnection = 8;
    Assert.assertEquals(minConnection, Args.getInstance().getMinConnections());
    Assert.assertEquals(0, PeerManager.getPeers().size());

    ApplicationContext ctx = (ApplicationContext) ReflectUtils.getFieldObject(p2pEventHandler,
        "ctx");
    for (int i = 0; i < 9; i++) {
      InetSocketAddress inetSocketAddress = new InetSocketAddress("201.0.0." + i, 10001);
      Channel c1 = spy(Channel.class);
      ReflectUtils.setFieldValue(c1, "inetSocketAddress", inetSocketAddress);
      ReflectUtils.setFieldValue(c1, "inetAddress", inetSocketAddress.getAddress());
      ReflectUtils.setFieldValue(c1, "outbound", true);
      ReflectUtils.setFieldValue(c1, "ctx", spy(ChannelHandlerContext.class));
      Mockito.doNothing().when(c1).send((byte[]) any());

      PeerManager.add(ctx, c1);
    }
    for (PeerConnection peer : PeerManager.getPeers()) {
      peer.setNeedSyncFromPeer(false);
      peer.setNeedSyncFromUs(false);
    }
    Assert.assertEquals(9, PeerManager.getPeers().size());

    boolean isLan = ReflectUtils.invokeMethod(service, "isLanNode");
    Assert.assertTrue(isLan);

    PeerConnection p1 = PeerManager.getPeers().get(1);
    InetSocketAddress address1 = p1.getChannel().getInetSocketAddress();
    p1.setLastInteractiveTime(
        System.currentTimeMillis() - Args.getInstance().inactiveThreshold * 1000L - 1000);
    PeerConnection p2 = PeerManager.getPeers().get(2);
    InetSocketAddress address2 = p2.getChannel().getInetSocketAddress();
    p2.setLastInteractiveTime(
        System.currentTimeMillis() - Args.getInstance().inactiveThreshold * 1000L - 2000);

    ReflectUtils.invokeMethod(service, "disconnectLan");
    Assert.assertEquals(8, PeerManager.getPeers().size());
    Set<InetSocketAddress> addressSet = new HashSet<>();
    PeerManager.getPeers()
        .forEach(p -> addressSet.add(p.getChannel().getInetSocketAddress()));
    Assert.assertTrue(addressSet.contains(address1));
    Assert.assertFalse(addressSet.contains(address2));

    ReflectUtils.invokeMethod(service, "disconnectLan");
    Assert.assertEquals(7, PeerManager.getPeers().size());
    addressSet.clear();
    PeerManager.getPeers()
        .forEach(p -> addressSet.add(p.getChannel().getInetSocketAddress()));
    Assert.assertFalse(addressSet.contains(address1));

    ReflectUtils.invokeMethod(service, "disconnectLan");
    Assert.assertEquals(7, PeerManager.getPeers().size());
  }

  @Test
  public void testDisconnectIsolated2() {
    int maxConnection = 30;
    Assert.assertEquals(maxConnection, Args.getInstance().getMaxConnections());
    Assert.assertEquals(0, PeerManager.getPeers().size());

    int addSize = (int) (maxConnection * ResilienceService.retentionPercent) + 2; //26
    ApplicationContext ctx = (ApplicationContext) ReflectUtils.getFieldObject(p2pEventHandler,
        "ctx");
    for (int i = 0; i < addSize; i++) {
      InetSocketAddress inetSocketAddress = new InetSocketAddress("201.0.0." + i, 10001);
      Channel c1 = spy(Channel.class);
      ReflectUtils.setFieldValue(c1, "inetSocketAddress", inetSocketAddress);
      ReflectUtils.setFieldValue(c1, "inetAddress", inetSocketAddress.getAddress());
      // 1 ~ 3 is active, 4 ~ 26 is not active
      ReflectUtils.setFieldValue(c1, "outbound", i <= 2);
      ReflectUtils.setFieldValue(c1, "ctx", spy(ChannelHandlerContext.class));
      Mockito.doNothing().when(c1).send((byte[]) any());

      PeerManager.add(ctx, c1);
    }
    PeerManager.getPeers().get(10).setNeedSyncFromUs(false);
    PeerManager.getPeers().get(10).setNeedSyncFromPeer(false);
    chainBaseManager.setLatestSaveBlockTime(
        System.currentTimeMillis() - ResilienceService.blockNotChangeThreshold - 100L);
    boolean isIsolated = ReflectUtils.invokeMethod(service, "isIsolateLand2");
    Assert.assertTrue(isIsolated);

    ReflectUtils.invokeMethod(service, "disconnectIsolated2");
    int activeNodeSize = (int) PeerManager.getPeers().stream()
        .filter(p -> p.getChannel().isOutbound())
        .count();
    int passiveSize = (int) PeerManager.getPeers().stream()
        .filter(p -> !p.getChannel().isOutbound())
        .count();
    Assert.assertEquals(2, activeNodeSize);
    Assert.assertEquals((int) (maxConnection * ResilienceService.retentionPercent),
        activeNodeSize + passiveSize);
    Assert.assertEquals((int) (maxConnection * ResilienceService.retentionPercent),
        PeerManager.getPeers().size());
  }

  @Test
  public void testDisconnectIsolated2UsesCooldownBeforeBanThreshold() {
    List<PeerConnection> peers = isolatedPeers(3, 23);
    PeerConnection outbound = peers.get(0);
    PeerConnection passive = peers.get(3);
    ResilienceService isolatedService = isolatedService(peers, 35_000L);
    try {
      ReflectUtils.invokeMethod(isolatedService, "disconnectIsolated2");

      Mockito.verify(outbound).disconnect(ReasonCode.SYNC_FAIL);
      Mockito.verify(passive).disconnect(ReasonCode.SYNC_FAIL);
      Assert.assertEquals(24, peers.size());
      Assert.assertEquals(0L,
          (long) ReflectUtils.getFieldObject(isolatedService, "lastBanRoundTime"));
    } finally {
      isolatedService.close();
    }
  }

  @Test
  public void testDisconnectIsolated2LimitsBanRounds() {
    List<PeerConnection> peers = isolatedPeers(3, 23);
    List<PeerConnection> original = new ArrayList<>(peers);
    ResilienceService isolatedService = isolatedService(peers, 65_000L);
    try {
      ReflectUtils.invokeMethod(isolatedService, "disconnectIsolated2");

      Mockito.verify(original.get(0)).disconnect(ReasonCode.BAD_PROTOCOL);
      Mockito.verify(original.get(3)).disconnect(ReasonCode.BAD_PROTOCOL);
      long lastBanTime = (long) ReflectUtils.getFieldObject(isolatedService, "lastBanRoundTime");
      Assert.assertTrue(lastBanTime > 0);

      // Replace the two disconnected peers so the next round can free slots again.
      peers.add(isolatedPeer(peers, true, 100));
      peers.add(isolatedPeer(peers, false, 100));
      ReflectUtils.invokeMethod(isolatedService, "disconnectIsolated2");

      Mockito.verify(original.get(1)).disconnect(ReasonCode.SYNC_FAIL);
      Mockito.verify(original.get(4)).disconnect(ReasonCode.SYNC_FAIL);
      Assert.assertEquals(lastBanTime,
          (long) ReflectUtils.getFieldObject(isolatedService, "lastBanRoundTime"));

      peers.add(isolatedPeer(peers, true, 101));
      peers.add(isolatedPeer(peers, false, 101));
      ReflectUtils.setFieldValue(isolatedService, "lastBanRoundTime",
          System.currentTimeMillis() - 31_000L);
      ReflectUtils.invokeMethod(isolatedService, "disconnectIsolated2");

      Mockito.verify(original.get(2)).disconnect(ReasonCode.BAD_PROTOCOL);
      Mockito.verify(original.get(5)).disconnect(ReasonCode.BAD_PROTOCOL);
      Assert.assertTrue(
          (long) ReflectUtils.getFieldObject(isolatedService, "lastBanRoundTime") >= lastBanTime);
    } finally {
      isolatedService.close();
    }
  }

  @Test
  public void testDisconnectIsolated2RecordsPassiveBanRound() {
    List<PeerConnection> peers = isolatedPeers(0, 26);
    PeerConnection first = peers.get(0);
    PeerConnection second = peers.get(1);
    ResilienceService isolatedService = isolatedService(peers, 65_000L);
    try {
      ReflectUtils.invokeMethod(isolatedService, "disconnectIsolated2");

      Mockito.verify(first).disconnect(ReasonCode.BAD_PROTOCOL);
      Mockito.verify(second).disconnect(ReasonCode.BAD_PROTOCOL);
      Assert.assertEquals(24, peers.size());
      Assert.assertTrue(
          (long) ReflectUtils.getFieldObject(isolatedService, "lastBanRoundTime") > 0);
    } finally {
      isolatedService.close();
    }
  }

  @Test
  public void testDisconnectIsolated2WaitsForStallThreshold() {
    List<PeerConnection> peers = isolatedPeers(3, 23);
    ResilienceService isolatedService = isolatedService(peers, 25_000L);
    try {
      ReflectUtils.invokeMethod(isolatedService, "disconnectIsolated2");

      peers.forEach(peer -> Mockito.verify(peer, Mockito.never()).disconnect(any()));
      Assert.assertEquals(0L,
          (long) ReflectUtils.getFieldObject(isolatedService, "lastBanRoundTime"));
    } finally {
      isolatedService.close();
    }
  }

  @Test
  public void testDisconnectIsolated2StopsAfterBlockProgress() {
    List<PeerConnection> peers = isolatedPeers(3, 23);
    ResilienceService isolatedService = isolatedService(peers, 65_000L);
    try {
      ReflectUtils.invokeMethod(isolatedService, "disconnectIsolated2");
      long lastBanTime = (long) ReflectUtils.getFieldObject(isolatedService, "lastBanRoundTime");
      peers.add(isolatedPeer(peers, true, 100));
      peers.add(isolatedPeer(peers, false, 100));
      ChainBaseManager chain = (ChainBaseManager)
          ReflectUtils.getFieldObject(isolatedService, "chainBaseManager");
      Mockito.when(chain.getLatestSaveBlockTime()).thenReturn(System.currentTimeMillis());

      ReflectUtils.invokeMethod(isolatedService, "disconnectIsolated2");

      peers.forEach(peer -> Mockito.verify(peer, Mockito.never()).disconnect(any()));
      Assert.assertEquals(26, peers.size());
      Assert.assertEquals(lastBanTime,
          (long) ReflectUtils.getFieldObject(isolatedService, "lastBanRoundTime"));
    } finally {
      isolatedService.close();
    }
  }

  @Test
  public void testDisconnectIsolated2EmptyRoundDoesNotDelayBan() {
    List<PeerConnection> peers = isolatedPeers(2, 0);
    PeerConnection first = peers.get(0);
    ResilienceService isolatedService = isolatedService(peers, 65_000L);
    try {
      ReflectUtils.invokeMethod(isolatedService, "disconnectIsolated2");

      peers.forEach(peer -> Mockito.verify(peer, Mockito.never()).disconnect(any()));
      Assert.assertEquals(0L,
          (long) ReflectUtils.getFieldObject(isolatedService, "lastBanRoundTime"));

      peers.add(isolatedPeer(peers, true, 100));
      ReflectUtils.invokeMethod(isolatedService, "disconnectIsolated2");

      Mockito.verify(first).disconnect(ReasonCode.BAD_PROTOCOL);
      Assert.assertTrue(
          (long) ReflectUtils.getFieldObject(isolatedService, "lastBanRoundTime") > 0);
    } finally {
      isolatedService.close();
    }
  }

  private ResilienceService isolatedService(List<PeerConnection> peers, long stalledMillis) {
    ResilienceService isolatedService = new ResilienceService();
    TronNetDelegate delegate = Mockito.mock(TronNetDelegate.class);
    ChainBaseManager chain = Mockito.mock(ChainBaseManager.class);
    Mockito.when(delegate.getActivePeer()).thenReturn(peers);
    Mockito.when(chain.getLatestSaveBlockTime())
        .thenReturn(System.currentTimeMillis() - stalledMillis);
    ReflectUtils.setFieldValue(isolatedService, "tronNetDelegate", delegate);
    ReflectUtils.setFieldValue(isolatedService, "chainBaseManager", chain);
    return isolatedService;
  }

  private List<PeerConnection> isolatedPeers(int outboundCount, int passiveCount) {
    List<PeerConnection> peers = new ArrayList<>();
    for (int i = 0; i < outboundCount + passiveCount; i++) {
      peers.add(isolatedPeer(peers, i < outboundCount, i));
    }
    return peers;
  }

  private PeerConnection isolatedPeer(List<PeerConnection> peers, boolean outbound, long order) {
    PeerConnection peer = Mockito.mock(PeerConnection.class);
    Channel channel = Mockito.mock(Channel.class);
    Mockito.when(peer.getChannel()).thenReturn(channel);
    Mockito.when(channel.isOutbound()).thenReturn(outbound);
    Mockito.when(peer.getLastInteractiveTime()).thenReturn(order);
    Mockito.doAnswer(invocation -> {
      peers.remove(peer);
      return null;
    }).when(peer).disconnect(any());
    return peer;
  }
}
