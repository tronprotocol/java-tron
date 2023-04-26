package org.tron.core.net.services;

import static org.mockito.Mockito.mock;

import com.google.common.cache.Cache;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.Map;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.ReflectUtils;
import org.tron.core.Constant;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.net.P2pEventHandlerImpl;
import org.tron.core.net.message.adv.BlockMessage;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.peer.PeerManager;
import org.tron.core.net.peer.TronState;
import org.tron.core.net.service.sync.SyncService;
import org.tron.p2p.connection.Channel;
import org.tron.protos.Protocol;

public class SyncServiceTest {
  protected TronApplicationContext context;
  private SyncService service;
  private PeerConnection peer;
  private P2pEventHandlerImpl p2pEventHandler;
  private ApplicationContext ctx;
  private String dbPath = "output-sync-service-test";

  public SyncServiceTest() {
  }

  /**
   * init context.
   */
  @Before
  public void init() throws Exception {
    Args.setParam(new String[]{"--output-directory", dbPath, "--debug"},
            Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    service = context.getBean(SyncService.class);
    p2pEventHandler = context.getBean(P2pEventHandlerImpl.class);
    ctx = (ApplicationContext) ReflectUtils.getFieldObject(p2pEventHandler, "ctx");
  }

  /**
   * destroy.
   */
  @After
  public void destroy() {
    Args.clearParam();
    context.destroy();
    FileUtil.deleteDir(new File(dbPath));
  }

  @Test
  public void testStartSync() {
    try {
      ReflectUtils.setFieldValue(service, "fetchFlag", true);
      ReflectUtils.setFieldValue(service, "handleFlag", true);
      service.init();
      Assert.assertTrue((boolean) ReflectUtils.getFieldObject(service, "fetchFlag"));
      Assert.assertTrue((boolean) ReflectUtils.getFieldObject(service, "handleFlag"));
      peer = context.getBean(PeerConnection.class);
      Assert.assertNull(peer.getSyncChainRequested());
      Channel c1 = new Channel();
      InetSocketAddress a1 = new InetSocketAddress("127.0.0.1", 10001);
      Field field = c1.getClass().getDeclaredField("inetSocketAddress");
      field.setAccessible(true);
      field.set(c1, a1.getAddress());
      peer.setChannel(c1);
      service.startSync(peer);
      ReflectUtils.setFieldValue(peer, "tronState", TronState.SYNCING);
      service.startSync(peer);
    } catch (Exception e) {
      // no need to deal with
    }
    service.close();
  }

  @Test
  public void testProcessBlock() throws Exception {
    peer = context.getBean(PeerConnection.class);
    Assert.assertNull(peer.getSyncChainRequested());
    Channel c1 = new Channel();
    InetSocketAddress a1 = new InetSocketAddress("127.0.0.1", 10001);
    Field field = c1.getClass().getDeclaredField("inetSocketAddress");
    field.setAccessible(true);
    field.set(c1, a1.getAddress());
    peer.setChannel(c1);
    service.processBlock(peer,
            new BlockMessage(new BlockCapsule(Protocol.Block.newBuilder().build())));
    boolean fetchFlag = (boolean) ReflectUtils.getFieldObject(service, "fetchFlag");
    boolean handleFlag = (boolean) ReflectUtils.getFieldObject(service, "handleFlag");
    Assert.assertTrue(fetchFlag);
    Assert.assertTrue(handleFlag);
  }

  @Test
  public void testOnDisconnect() throws Exception {
    Cache<BlockCapsule.BlockId, PeerConnection> requestBlockIds =
            (Cache) ReflectUtils.getFieldObject(service, "requestBlockIds");
    peer = context.getBean(PeerConnection.class);
    Assert.assertNull(peer.getSyncChainRequested());
    Channel c1 = new Channel();
    InetSocketAddress a1 = new InetSocketAddress("127.0.0.1", 10001);
    Field field = c1.getClass().getDeclaredField("inetSocketAddress");
    field.setAccessible(true);
    field.set(c1, a1.getAddress());
    peer.setChannel(c1);
    BlockCapsule.BlockId blockId = new BlockCapsule.BlockId();
    requestBlockIds.put(blockId, peer);
    peer.getSyncBlockToFetch().push(blockId);
    service.onDisconnect(peer);
    Assert.assertTrue(requestBlockIds.getIfPresent(blockId) == null);
  }

  @Test
  public void testStartFetchSyncBlock() throws Exception {
    BlockCapsule.BlockId blockId = new BlockCapsule.BlockId();
    InetSocketAddress a1 = new InetSocketAddress("127.0.0.1", 10001);

    Method method = service.getClass().getDeclaredMethod("startFetchSyncBlock");
    method.setAccessible(true);

    Cache<BlockCapsule.BlockId, PeerConnection> requestBlockIds =
            (Cache<BlockCapsule.BlockId, PeerConnection>)
                    ReflectUtils.getFieldObject(service, "requestBlockIds");

    Channel c1 = mock(Channel.class);
    Mockito.when(c1.getInetSocketAddress()).thenReturn(a1);
    Mockito.when(c1.getInetAddress()).thenReturn(a1.getAddress());

    PeerManager.add(ctx, c1);
    peer = PeerManager.getPeers().get(0);

    method.invoke(service);
    Assert.assertTrue(peer.getSyncBlockRequested().get(blockId) == null);

    peer.getSyncBlockToFetch().add(blockId);
    method.invoke(service);
    Assert.assertTrue(peer.getSyncBlockToFetch().size() == 1);
    Assert.assertTrue(peer.getSyncBlockRequested().get(blockId) == null);

    peer.setFetchAble(true);
    method.invoke(service);
    Assert.assertTrue(peer.getSyncBlockToFetch().size() == 1);
    Assert.assertTrue(peer.getSyncBlockRequested().get(blockId) != null);
    Assert.assertTrue(requestBlockIds.getIfPresent(blockId) != null);

    peer.getSyncBlockRequested().remove(blockId);
    method.invoke(service);
    Assert.assertTrue(peer.getSyncBlockRequested().get(blockId) == null);
  }

  @Test
  public void testHandleSyncBlock() throws Exception {
    Method method = service.getClass().getDeclaredMethod("handleSyncBlock");
    method.setAccessible(true);

    Map<BlockMessage, PeerConnection> blockJustReceived =
            (Map<BlockMessage, PeerConnection>)
            ReflectUtils.getFieldObject(service, "blockJustReceived");

    BlockCapsule blockCapsule = new BlockCapsule(Protocol.Block.newBuilder().build());

    BlockCapsule.BlockId blockId = blockCapsule.getBlockId();

    InetSocketAddress a1 = new InetSocketAddress("127.0.0.1", 10001);
    Channel c1 = mock(Channel.class);
    Mockito.when(c1.getInetSocketAddress()).thenReturn(a1);
    Mockito.when(c1.getInetAddress()).thenReturn(a1.getAddress());
    PeerManager.add(ctx, c1);
    peer = PeerManager.getPeers().get(0);

    blockJustReceived.put(new BlockMessage(blockCapsule), peer);

    peer.getSyncBlockToFetch().add(blockId);

    Cache<BlockCapsule.BlockId, PeerConnection> requestBlockIds =
            (Cache<BlockCapsule.BlockId, PeerConnection>)
                    ReflectUtils.getFieldObject(service, "requestBlockIds");

    requestBlockIds.put(blockId, peer);

    method.invoke(service);

    Assert.assertTrue(requestBlockIds.getIfPresent(blockId) == null);
  }
}
