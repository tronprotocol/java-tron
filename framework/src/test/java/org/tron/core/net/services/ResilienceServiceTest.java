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
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.parameter.ResilienceConfig;
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

  private ResilienceConfig resilienceConfig;
  @Rule
  public final TemporaryFolder temporaryFolder = new TemporaryFolder();


  @Before
  public void init() throws IOException {
    Args.setParam(new String[] {"--output-directory",
        temporaryFolder.newFolder().toString(), "--debug"}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    service = context.getBean(ResilienceService.class);
    chainBaseManager = context.getBean(ChainBaseManager.class);
    resilienceConfig = Args.getInstance().getResilienceConfig();
  }

  @After
  public void destroy() {
    Args.clearParam();
    context.destroy();
  }

  @Test
  public void testLanNode() {

    int minConnection = 8;
    Assert.assertEquals(minConnection, Args.getInstance().getMinConnections());
    clearPeers();
    Assert.assertEquals(0, PeerManager.getPeers().size());

    List<Channel> channelList = new ArrayList<>();
    for (int i = 0; i < minConnection; i++) {
      InetSocketAddress inetSocketAddress = new InetSocketAddress("201.0.0." + i, 10001);
      Channel c1 = spy(Channel.class);
      ReflectUtils.setFieldValue(c1, "inetSocketAddress", inetSocketAddress);
      ReflectUtils.setFieldValue(c1, "inetAddress", inetSocketAddress.getAddress());
      ReflectUtils.setFieldValue(c1, "isActive", true);
      ReflectUtils.setFieldValue(c1, "ctx", spy(ChannelHandlerContext.class));
      Mockito.doNothing().when(c1).send((byte[]) any());

      channelList.add(c1);
    }

    for (int i = 0; i < minConnection - 1; i++) {
      PeerManager.add(context, channelList.get(i));
    }
    Assert.assertEquals(minConnection - 1, PeerManager.getPeers().size());

    PeerManager.getPeers().get(0).getFeature().updateBadSyncBlockChainTime();
    //not enough peers
    service.resilienceNode();
    Assert.assertEquals(minConnection - 1, PeerManager.getPeers().size());

    PeerManager.add(context, channelList.get(minConnection - 1));
    Assert.assertEquals(minConnection, PeerManager.getPeers().size());

    //enough peers
    service.resilienceNode();
    Assert.assertEquals(minConnection - 1, PeerManager.getPeers().size());
  }

  @Test
  public void testLanNodeStopInv() {

    int minConnection = 8;
    Assert.assertEquals(minConnection, Args.getInstance().getMinConnections());
    Assert.assertFalse(resilienceConfig.isTestStopInv());
    clearPeers();
    Assert.assertEquals(0, PeerManager.getPeers().size());

    // test stop inventory
    resilienceConfig.setTestStopInv(true);

    long t1 =
        System.currentTimeMillis() - resilienceConfig.getPeerNotActiveThreshold() * 1000L - 1000L;
    for (int i = 0; i < minConnection; i++) {
      InetSocketAddress inetSocketAddress = new InetSocketAddress("201.0.0." + i, 10001);
      Channel c1 = spy(Channel.class);
      ReflectUtils.setFieldValue(c1, "inetSocketAddress", inetSocketAddress);
      ReflectUtils.setFieldValue(c1, "inetAddress", inetSocketAddress.getAddress());
      ReflectUtils.setFieldValue(c1, "isActive", true);
      ReflectUtils.setFieldValue(c1, "ctx", spy(ChannelHandlerContext.class));
      ReflectUtils.setFieldValue(c1, "lastActiveTime", t1);
      Mockito.doNothing().when(c1).send((byte[]) any());
      PeerManager.add(context, c1);
    }

    service.resilienceNode();
    Assert.assertEquals(minConnection, PeerManager.getPeers().size());

    PeerConnection p = PeerManager.getPeers().get(0);
    p.getFeature().setAdvStartTime(t1);
    p.getFeature().setLastRecBlockInvTime(t1);
    p.getFeature().setStopBlockInvStartTime(t1 + 1);
    p.getFeature().updateNoInvBackTime();
    service.resilienceNode();
    Assert.assertEquals(minConnection - 1, PeerManager.getPeers().size());

    //resume config
    resilienceConfig.setTestStopInv(false);
  }

  @Test
  public void testIsolated() {
    Assert.assertEquals(8, Args.getInstance().getMinConnections());
    Assert.assertEquals(30, Args.getInstance().getMaxConnections());
    clearPeers();
    Assert.assertEquals(0, PeerManager.getPeers().size());
    Assert.assertTrue(resilienceConfig.isEnabled());

    // peer 1 ~ 10 are not active,  needSyncFromPeer = true, needSyncFromUs = true
    // peer 11 ~ 20 are active, needSyncFromPeer = false, needSyncFromUs = false
    int totalNumber = 20;
    List<Channel> channelList = new ArrayList<>();
    long t1 =
        System.currentTimeMillis() - resilienceConfig.getPeerNotActiveThreshold() * 1000L - 1000L;
    for (int i = 0; i < totalNumber; i++) {
      InetSocketAddress inetSocketAddress = new InetSocketAddress("201.0.0." + i, 10001);
      Channel c1 = spy(Channel.class);
      ReflectUtils.setFieldValue(c1, "inetSocketAddress", inetSocketAddress);
      ReflectUtils.setFieldValue(c1, "inetAddress", inetSocketAddress.getAddress());
      ReflectUtils.setFieldValue(c1, "isActive", i >= 10);
      ReflectUtils.setFieldValue(c1, "ctx", spy(ChannelHandlerContext.class));
      ReflectUtils.setFieldValue(c1, "lastActiveTime", t1);
      Mockito.doNothing().when(c1).send((byte[]) any());

      channelList.add(c1);
    }

    for (Channel channel : channelList) {
      PeerManager.add(context, channel);
    }
    for (PeerConnection p : PeerManager.getPeers()) { //peer's order is not same as channelList
      if (p.getChannel().isActive()) {
        p.setNeedSyncFromPeer(false);
        p.setNeedSyncFromUs(false);
        p.getFeature().setAdvStartTime(t1);
        p.getFeature().updateNoInteractionTime();
      }
    }
    Assert.assertEquals(totalNumber, PeerManager.getPeers().size());

    service.resilienceNode();
    Assert.assertEquals(totalNumber, PeerManager.getPeers().size());
    Assert.assertEquals(10,
        PeerManager.getPeers().stream().filter(p -> p.getChannel().isActive()).count());

    int blockNotChangeThreshold = resilienceConfig.getBlockNotChangeThreshold();
    int disconnectNumber = resilienceConfig.getDisconnectNumber();
    Assert.assertEquals(2, disconnectNumber);
    // trigger that node is isolated
    chainBaseManager.setLatestSaveBlockTime(
        System.currentTimeMillis() - blockNotChangeThreshold * 1000L - 1000L);

    //disconnect some peer if node is isolated, prefer to disconnect active nodes
    service.resilienceNode();
    Assert.assertEquals(totalNumber - disconnectNumber, PeerManager.getPeers().size());
    Assert.assertEquals(10 - disconnectNumber,
        PeerManager.getPeers().stream().filter(p -> p.getChannel().isActive()).count());
    Assert.assertEquals(10,
        PeerManager.getPeers().stream().filter(p -> !p.getChannel().isActive()).count());
  }

  @Test
  public void testFullConnection() {
    int maxConnection = 30;
    Assert.assertEquals(maxConnection, Args.getInstance().getMaxConnections());
    clearPeers();
    Assert.assertEquals(0, PeerManager.getPeers().size());

    int activeNumber = 10;
    List<Channel> channelList = new ArrayList<>();
    for (int i = 0; i < maxConnection; i++) {
      InetSocketAddress inetSocketAddress = new InetSocketAddress("201.0.0." + i, 10001);
      Channel c1 = spy(Channel.class);
      ReflectUtils.setFieldValue(c1, "inetSocketAddress", inetSocketAddress);
      ReflectUtils.setFieldValue(c1, "inetAddress", inetSocketAddress.getAddress());
      ReflectUtils.setFieldValue(c1, "isActive", i < activeNumber);
      ReflectUtils.setFieldValue(c1, "ctx", spy(ChannelHandlerContext.class));
      Mockito.doNothing().when(c1).send((byte[]) any());

      channelList.add(c1);
    }

    for (Channel channel : channelList) {
      PeerManager.add(context, channel);
    }
    Assert.assertEquals(maxConnection, PeerManager.getPeers().size());

    //set one active peer to malicious (any feature is ok)
    PeerManager.getPeers().get(0).getFeature().updateBadSyncBlockChainTime();
    service.resilienceNode();
    Assert.assertEquals(maxConnection, PeerManager.getPeers().size());

    //set two passive peers to malicious (any feature is ok)
    String firstIp = PeerManager.getPeers().get(activeNumber).getChannel().getInetAddress()
        .getHostName();
    PeerManager.getPeers().get(activeNumber).getFeature().updateBadChainInventoryTime();
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      Assert.fail();
    }
    String secondIp = PeerManager.getPeers().get(activeNumber + 5).getChannel().getInetAddress()
        .getHostName();
    PeerManager.getPeers().get(activeNumber + 5).getFeature().updateBadChainInventoryTime();
    //verify that disconnect one malicious peer
    service.resilienceNode();
    Assert.assertEquals(maxConnection - 1, PeerManager.getPeers().size());

    //verify that disconnect oldest malicious peer
    Set<String> ipSet = new HashSet<>();
    for (PeerConnection p : PeerManager.getPeers()) {
      ipSet.add(p.getChannel().getInetAddress().getHostName());
    }
    Assert.assertFalse(ipSet.contains(firstIp));
    Assert.assertTrue(ipSet.contains(secondIp));
  }

  private void clearPeers() {
    for (PeerConnection p : PeerManager.getPeers()) {
      PeerManager.remove(p.getChannel());
    }
  }
}
