package org.tron.core.net.services;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;

import io.netty.channel.ChannelHandlerContext;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.ReflectUtils;
import org.tron.core.ChainBaseManager;
import org.tron.core.Constant;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.peer.PeerManager;
import org.tron.core.net.service.effective.ResilienceService;
import org.tron.p2p.connection.Channel;

public class ResilienceServiceTest {

  protected TronApplicationContext context;
  private ResilienceService service;
  private ChainBaseManager chainBaseManager;

  @Rule
  public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Before
  public void init() throws IOException {
    Args.setParam(new String[] {"--output-directory",
        temporaryFolder.newFolder().toString(), "--debug"}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    chainBaseManager = context.getBean(ChainBaseManager.class);
    service = context.getBean(ResilienceService.class);
  }

  @Test
  public void testDisconnectRandom() {
    int maxConnection = 30;
    Assert.assertEquals(maxConnection, Args.getInstance().getMaxConnections());
    clearPeers();
    Assert.assertEquals(0, PeerManager.getPeers().size());

    for (int i = 0; i < maxConnection + 1; i++) {
      InetSocketAddress inetSocketAddress = new InetSocketAddress("201.0.0." + i, 10001);
      Channel c1 = spy(Channel.class);
      ReflectUtils.setFieldValue(c1, "inetSocketAddress", inetSocketAddress);
      ReflectUtils.setFieldValue(c1, "inetAddress", inetSocketAddress.getAddress());
      ReflectUtils.setFieldValue(c1, "ctx", spy(ChannelHandlerContext.class));
      Mockito.doNothing().when(c1).send((byte[]) any());

      PeerManager.add(context, c1);
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
  public void testDisconnectLan() {
    int minConnection = 8;
    Assert.assertEquals(minConnection, Args.getInstance().getMinConnections());
    clearPeers();
    Assert.assertEquals(0, PeerManager.getPeers().size());

    for (int i = 0; i < 9; i++) {
      InetSocketAddress inetSocketAddress = new InetSocketAddress("201.0.0." + i, 10001);
      Channel c1 = spy(Channel.class);
      ReflectUtils.setFieldValue(c1, "inetSocketAddress", inetSocketAddress);
      ReflectUtils.setFieldValue(c1, "inetAddress", inetSocketAddress.getAddress());
      ReflectUtils.setFieldValue(c1, "isActive", true);
      ReflectUtils.setFieldValue(c1, "ctx", spy(ChannelHandlerContext.class));
      Mockito.doNothing().when(c1).send((byte[]) any());

      PeerManager.add(context, c1);
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
    clearPeers();
    Assert.assertEquals(0, PeerManager.getPeers().size());

    int addSize = (int) (maxConnection * ResilienceService.retentionPercent) + 2; //26
    for (int i = 0; i < addSize; i++) {
      InetSocketAddress inetSocketAddress = new InetSocketAddress("201.0.0." + i, 10001);
      Channel c1 = spy(Channel.class);
      ReflectUtils.setFieldValue(c1, "inetSocketAddress", inetSocketAddress);
      ReflectUtils.setFieldValue(c1, "inetAddress", inetSocketAddress.getAddress());
      // 1 ~ 3 is active, 4 ~ 26 is not active
      ReflectUtils.setFieldValue(c1, "isActive", i <= 2);
      ReflectUtils.setFieldValue(c1, "ctx", spy(ChannelHandlerContext.class));
      Mockito.doNothing().when(c1).send((byte[]) any());

      PeerManager.add(context, c1);
    }
    PeerManager.getPeers().get(10).setNeedSyncFromUs(false);
    PeerManager.getPeers().get(10).setNeedSyncFromPeer(false);
    chainBaseManager.setLatestSaveBlockTime(
        System.currentTimeMillis() - ResilienceService.blockNotChangeThreshold - 100L);
    boolean isIsolated = ReflectUtils.invokeMethod(service, "isIsolateLand2");
    Assert.assertTrue(isIsolated);

    ReflectUtils.invokeMethod(service, "disconnectIsolated2");
    int activeNodeSize = (int) PeerManager.getPeers().stream()
        .filter(p -> p.getChannel().isActive())
        .count();
    int passiveSize = (int) PeerManager.getPeers().stream()
        .filter(p -> !p.getChannel().isActive())
        .count();
    Assert.assertEquals(2, activeNodeSize);
    Assert.assertEquals((int) (maxConnection * ResilienceService.retentionPercent),
        activeNodeSize + passiveSize);
    Assert.assertEquals((int) (maxConnection * ResilienceService.retentionPercent),
        PeerManager.getPeers().size());
  }

  private void clearPeers() {
    for (PeerConnection p : PeerManager.getPeers()) {
      PeerManager.remove(p.getChannel());
    }
  }

  @After
  public void destroy() {
    Args.clearParam();
    context.destroy();
  }
}