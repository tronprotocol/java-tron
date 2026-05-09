package org.tron.core.net.services;

import static org.mockito.Mockito.mock;

import com.google.common.cache.Cache;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.tron.common.BaseMethodTest;
import org.tron.common.utils.ReflectUtils;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.net.P2pEventHandlerImpl;
import org.tron.core.net.message.adv.BlockMessage;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.peer.PeerManager;
import org.tron.core.net.peer.TronState;
import org.tron.core.net.service.sync.SyncService;
import org.tron.core.net.service.sync.UnparsedBlock;
import org.tron.p2p.connection.Channel;
import org.tron.protos.Protocol;

public class SyncServiceTest extends BaseMethodTest {
  private SyncService service;
  private PeerConnection peer;
  private P2pEventHandlerImpl p2pEventHandler;
  private ApplicationContext ctx;
  private InetSocketAddress inetSocketAddress =
          new InetSocketAddress("127.0.0.2", 10001);

  @Override
  protected String[] extraArgs() {
    return new String[]{"--debug"};
  }

  @Override
  protected void afterInit() {
    service = context.getBean(SyncService.class);
    p2pEventHandler = context.getBean(P2pEventHandlerImpl.class);
    ctx = (ApplicationContext) ReflectUtils.getFieldObject(p2pEventHandler, "ctx");
  }

  @Override
  protected void beforeDestroy() {
    for (PeerConnection p : PeerManager.getPeers()) {
      PeerManager.remove(p.getChannel());
    }
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
      ReflectUtils.setFieldValue(c1, "inetSocketAddress", inetSocketAddress);
      ReflectUtils.setFieldValue(c1, "inetAddress", inetSocketAddress.getAddress());

      peer.setChannel(c1);

      ReflectUtils.setFieldValue(peer, "tronState", TronState.SYNCING);

      service.startSync(peer);

      ReflectUtils.setFieldValue(peer, "tronState", TronState.INIT);

      try {
        peer.setBlockBothHave(new BlockCapsule.BlockId(Sha256Hash.ZERO_HASH, -1));
        service.syncNext(peer);
      } catch (Exception e) {
        // no need to deal with
      }

      service.startSync(peer);
    } catch (Exception e) {
      // no need to deal with
    }
    service.close();
  }

  @Test
  public void testProcessBlock() {
    peer = context.getBean(PeerConnection.class);
    Assert.assertNull(peer.getSyncChainRequested());
    Channel c1 = new Channel();
    ReflectUtils.setFieldValue(c1, "inetSocketAddress", inetSocketAddress);
    ReflectUtils.setFieldValue(c1, "inetAddress", inetSocketAddress.getAddress());
    peer.setChannel(c1);

    BlockCapsule blockCapsule = new BlockCapsule(Protocol.Block.newBuilder().build());
    BlockMessage blockMessage = new BlockMessage(blockCapsule);
    service.processBlock(peer, blockMessage);

    boolean fetchFlag = (boolean) ReflectUtils.getFieldObject(service, "fetchFlag");
    boolean handleFlag = (boolean) ReflectUtils.getFieldObject(service, "handleFlag");
    Assert.assertTrue(fetchFlag);
    Assert.assertTrue(handleFlag);

    Map<UnparsedBlock, PeerConnection> blockJustReceived =
        (Map<UnparsedBlock, PeerConnection>)
            ReflectUtils.getFieldObject(service, "blockJustReceived");
    Assert.assertEquals(1, blockJustReceived.size());
    UnparsedBlock stored = blockJustReceived.keySet().iterator().next();
    Assert.assertEquals(blockMessage.getBlockId(), stored.getBlockId());
  }

  @Test
  public void testOnDisconnect() {
    Cache<BlockCapsule.BlockId, PeerConnection> requestBlockIds =
            (Cache) ReflectUtils.getFieldObject(service, "requestBlockIds");
    peer = context.getBean(PeerConnection.class);
    Assert.assertNull(peer.getSyncChainRequested());
    Channel c1 = mock(Channel.class);
    Mockito.when(c1.getInetSocketAddress()).thenReturn(inetSocketAddress);
    Mockito.when(c1.getInetAddress()).thenReturn(inetSocketAddress.getAddress());
    peer.setChannel(c1);
    BlockCapsule.BlockId blockId = new BlockCapsule.BlockId();
    requestBlockIds.put(blockId, peer);
    peer.getSyncBlockRequested().put(blockId, System.currentTimeMillis());
    service.onDisconnect(peer);
    Assert.assertTrue(requestBlockIds.getIfPresent(blockId) == null);
  }

  @Test
  public void testStartFetchSyncBlock() throws Exception {
    Field field = PeerManager.class.getDeclaredField("peers");
    field.setAccessible(true);
    field.set(PeerManager.class, Collections.synchronizedList(new ArrayList<>()));

    BlockCapsule.BlockId blockId = new BlockCapsule.BlockId();

    Method method = service.getClass().getDeclaredMethod("startFetchSyncBlock");
    method.setAccessible(true);

    Cache<BlockCapsule.BlockId, PeerConnection> requestBlockIds =
            (Cache<BlockCapsule.BlockId, PeerConnection>)
                    ReflectUtils.getFieldObject(service, "requestBlockIds");

    Channel c1 = mock(Channel.class);
    Mockito.when(c1.getInetSocketAddress()).thenReturn(inetSocketAddress);
    Mockito.when(c1.getInetAddress()).thenReturn(inetSocketAddress.getAddress());

    PeerManager.add(ctx, c1);
    peer = PeerManager.getPeers().get(0);

    Method method1 = PeerManager.class.getDeclaredMethod("check");
    method1.setAccessible(true);
    method1.invoke(PeerManager.class);
    Method method2 = PeerManager.class.getDeclaredMethod("logPeerStats");
    method2.setAccessible(true);
    method2.invoke(PeerManager.class);

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

    // reset maxRequestedBlockNum to 0
    Field maxRequestedBlockNumField = service.getClass().getDeclaredField("maxRequestedBlockNum");
    maxRequestedBlockNumField.setAccessible(true);
    maxRequestedBlockNumField.set(service, 0L);

    Map<UnparsedBlock, PeerConnection> blockWaitToProcess =
        (Map<UnparsedBlock, PeerConnection>)
            ReflectUtils.getFieldObject(service, "blockWaitToProcess");

    // target block has num=1, above maxRequestedBlockNum=0 so it can be throttled
    BlockCapsule.BlockId highBlockId = new BlockCapsule.BlockId(Sha256Hash.ZERO_HASH, 1);
    peer.getSyncBlockToFetch().clear();
    peer.getSyncBlockToFetch().add(highBlockId);
    peer.getSyncBlockRequested().clear();
    requestBlockIds.invalidateAll();

    // fill blockWaitToProcess to reach maxPendingBlockSize (default 500)
    int maxPendingBlockSize = (int) ReflectUtils.getFieldObject(service, "maxPendingBlockSize");
    for (int i = 0; i < maxPendingBlockSize; i++) {
      BlockCapsule.BlockId fillId = new BlockCapsule.BlockId(Sha256Hash.ZERO_HASH, 10000 + i);
      blockWaitToProcess.put(new UnparsedBlock(fillId, new byte[0]), peer);
    }
    method.invoke(service);
    // highBlockId must NOT be requested: remainNum <= 0 and num > maxRequestedBlockNum
    Assert.assertNull(peer.getSyncBlockRequested().get(highBlockId));

    // Symmetric retry-exemption case: budget still saturated, but the target block's num
    // is below maxRequestedBlockNum, so it must still be requested (deadlock-avoidance
    // retry path — guards an explicit invariant of the throttling design).
    maxRequestedBlockNumField.set(service, 100L);
    BlockCapsule.BlockId retryBlockId = new BlockCapsule.BlockId(Sha256Hash.ZERO_HASH, 50);
    peer.getSyncBlockToFetch().clear();
    peer.getSyncBlockToFetch().add(retryBlockId);
    peer.getSyncBlockRequested().clear();
    requestBlockIds.invalidateAll();
    method.invoke(service);
    // retryBlockId MUST be requested: remainNum <= 0 but num=50 <= maxRequestedBlockNum=100
    Assert.assertNotNull(peer.getSyncBlockRequested().get(retryBlockId));
    blockWaitToProcess.clear();
  }

  @Test
  public void testHandleSyncBlock() throws Exception {

    Field field = PeerManager.class.getDeclaredField("peers");
    field.setAccessible(true);
    field.set(PeerManager.class, Collections.synchronizedList(new ArrayList<>()));

    Method method = service.getClass().getDeclaredMethod("handleSyncBlock");
    method.setAccessible(true);

    Map<UnparsedBlock, PeerConnection> blockJustReceived =
        (Map<UnparsedBlock, PeerConnection>)
            ReflectUtils.getFieldObject(service, "blockJustReceived");

    Protocol.BlockHeader.raw blockHeaderRaw = Protocol.BlockHeader.raw.newBuilder()
        .setNumber(100000)
        .build();
    Protocol.BlockHeader blockHeader = Protocol.BlockHeader.newBuilder()
        .setRawData(blockHeaderRaw).build();
    BlockCapsule blockCapsule = new BlockCapsule(
        Protocol.Block.newBuilder().setBlockHeader(blockHeader).build());
    BlockCapsule.BlockId blockId = blockCapsule.getBlockId();

    InetSocketAddress a1 = new InetSocketAddress("127.0.0.1", 10001);
    Channel c1 = mock(Channel.class);
    Mockito.when(c1.getInetSocketAddress()).thenReturn(a1);
    Mockito.when(c1.getInetAddress()).thenReturn(a1.getAddress());
    PeerManager.add(ctx, c1);
    peer = PeerManager.getPeers().get(0);

    UnparsedBlock unparsedBlock = new UnparsedBlock(blockId, blockCapsule.getData());
    blockJustReceived.put(unparsedBlock, peer);

    peer.getSyncBlockToFetch().add(blockId);

    Cache<BlockCapsule.BlockId, PeerConnection> requestBlockIds =
        (Cache<BlockCapsule.BlockId, PeerConnection>)
            ReflectUtils.getFieldObject(service, "requestBlockIds");
    requestBlockIds.put(blockId, peer);

    method.invoke(service);

    Assert.assertTrue(requestBlockIds.getIfPresent(blockId) == null);
  }
}
