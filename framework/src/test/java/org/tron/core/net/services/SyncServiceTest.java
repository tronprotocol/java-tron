package org.tron.core.net.services;

import static org.mockito.Mockito.mock;

import com.google.common.cache.Cache;
import com.google.protobuf.ByteString;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.tron.common.BaseMethodTest;
import org.tron.common.crypto.ECKey;
import org.tron.common.overlay.message.Message;
import org.tron.common.utils.ReflectUtils;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.net.P2pEventHandlerImpl;
import org.tron.core.net.TronNetDelegate;
import org.tron.core.net.message.MessageTypes;
import org.tron.core.net.message.adv.BlockMessage;
import org.tron.core.net.message.adv.FetchInvDataMessage;
import org.tron.core.net.messagehandler.BlockMsgHandler;
import org.tron.core.net.messagehandler.PbftDataSyncHandler;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.peer.PeerManager;
import org.tron.core.net.peer.TronState;
import org.tron.core.net.service.adv.AdvService;
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
  public void testProcessingBlockResumesThrottledFetch() throws Exception {
    SyncService sync = new SyncService();
    TronNetDelegate delegate = mock(TronNetDelegate.class);
    ReflectUtils.setFieldValue(sync, "tronNetDelegate", delegate);
    ReflectUtils.setFieldValue(sync, "pbftDataSyncHandler", mock(PbftDataSyncHandler.class));
    ReflectUtils.setFieldValue(sync, "maxPendingBlockSize", 1);
    PeerConnection syncingPeer = context.getBean(PeerConnection.class);
    Channel channel = mock(Channel.class);
    Mockito.when(channel.getInetSocketAddress()).thenReturn(inetSocketAddress);
    Mockito.when(channel.getInetAddress()).thenReturn(inetSocketAddress.getAddress());
    syncingPeer.setChannel(channel);
    syncingPeer.setNeedSyncFromPeer(true);
    syncingPeer.setFetchAble(true);
    Mockito.when(delegate.getActivePeer()).thenReturn(Collections.singletonList(syncingPeer));
    Mockito.when(delegate.getSolidBlockId())
        .thenReturn(new BlockCapsule.BlockId(Sha256Hash.ZERO_HASH, 0));
    Mockito.when(delegate.getBlockLock()).thenReturn(new Object());
    BlockCapsule block = new BlockCapsule(Protocol.Block.newBuilder().setBlockHeader(
        Protocol.BlockHeader.newBuilder().setRawData(
            Protocol.BlockHeader.raw.newBuilder().setNumber(1))).build());
    BlockCapsule.BlockId next = new BlockCapsule.BlockId(Sha256Hash.ZERO_HASH, 2);
    syncingPeer.getSyncBlockToFetch().add(block.getBlockId());
    syncingPeer.getSyncBlockToFetch().add(next);
    syncingPeer.getSyncBlockInProcess().add(block.getBlockId());
    Map<UnparsedBlock, PeerConnection> waiting = (Map<UnparsedBlock, PeerConnection>)
        ReflectUtils.getFieldObject(sync, "blockWaitToProcess");
    waiting.put(new UnparsedBlock(block.getBlockId(), block.getData()), syncingPeer);
    Method fetch = SyncService.class.getDeclaredMethod("startFetchSyncBlock");
    fetch.setAccessible(true);
    fetch.invoke(sync);
    Assert.assertTrue(syncingPeer.getSyncBlockRequested().isEmpty());
    Method handle = SyncService.class.getDeclaredMethod("handleSyncBlock");
    handle.setAccessible(true);
    handle.invoke(sync);
    Assert.assertTrue("Freeing the window must wake the fetch scheduler",
        (boolean) ReflectUtils.getFieldObject(sync, "fetchFlag"));
    fetch.invoke(sync);
    Assert.assertEquals(Collections.singleton(next), syncingPeer.getSyncBlockRequested().keySet());
    sync.close();
  }

  @Test
  public void testStoredGenesisBlockRoundTripsThroughNetworkDecoder() throws Exception {
    BlockCapsule stored = chainBaseManager.getBlockById(chainBaseManager.getGenesisBlockId());
    for (int filter : new int[] {0, 1}) {
      chainBaseManager.getDynamicPropertiesStore().saveAllowProtoFilterNum(filter);
      BlockMessage message = new BlockMessage(new BlockMessage(stored).getData());
      Assert.assertEquals(stored.getInstance(), message.getBlockCapsule().getInstance());
      Assert.assertEquals(stored.getBlockId(), message.getBlockId());
      Assert.assertArrayEquals(stored.getData(), message.getData());
    }
  }

  @Test
  public void testIngressParseFailureAllowsRetryAfterDisconnect() throws Exception {
    chainBaseManager.getDynamicPropertiesStore().saveAllowProtoFilterNum(0);
    SyncService sync = new SyncService();
    try {
      TronNetDelegate delegate = mock(TronNetDelegate.class);
      ReflectUtils.setFieldValue(sync, "tronNetDelegate", delegate);
      PeerConnection badPeer = Mockito.spy(context.getBean(PeerConnection.class));
      PeerConnection goodPeer = Mockito.spy(context.getBean(PeerConnection.class));
      Channel channel = mock(Channel.class);
      Channel goodChannel = mock(Channel.class);
      Mockito.when(channel.getInetSocketAddress()).thenReturn(inetSocketAddress);
      Mockito.when(channel.getInetAddress()).thenReturn(inetSocketAddress.getAddress());
      Mockito.when(goodChannel.getInetSocketAddress()).thenReturn(inetSocketAddress);
      Mockito.when(goodChannel.getInetAddress()).thenReturn(inetSocketAddress.getAddress());
      badPeer.setChannel(channel);
      goodPeer.setChannel(goodChannel);
      badPeer.setFetchAble(true);
      goodPeer.setFetchAble(true);
      ReflectUtils.setFieldValue(badPeer, "syncService", sync);
      ReflectUtils.setFieldValue(badPeer, "advService", mock(AdvService.class));
      Mockito.when(delegate.getActivePeer()).thenReturn(Arrays.asList(badPeer, goodPeer));

      BlockCapsule block = new BlockCapsule(1, Sha256Hash.ZERO_HASH,
          System.currentTimeMillis(),
          ByteString.copyFrom(ECKey.fromPrivate(BigInteger.ONE).getAddress()));
      BlockCapsule.BlockId blockId = block.getBlockId();
      badPeer.getSyncBlockToFetch().add(blockId);
      goodPeer.getSyncBlockToFetch().add(blockId);
      Method fetch = SyncService.class.getDeclaredMethod("startFetchSyncBlock");
      fetch.setAccessible(true);
      fetch.invoke(sync);
      Cache<BlockCapsule.BlockId, PeerConnection> requests =
          (Cache<BlockCapsule.BlockId, PeerConnection>)
              ReflectUtils.getFieldObject(sync, "requestBlockIds");
      Assert.assertSame(badPeer, requests.getIfPresent(blockId));

      byte[] data = block.getData();
      byte[] wire = new byte[data.length + 2];
      wire[0] = MessageTypes.BLOCK.asByte();
      System.arraycopy(data, 0, wire, 1, data.length);
      wire[wire.length - 1] = 0x0c;
      P2pEventHandlerImpl handler = new P2pEventHandlerImpl();
      BlockMsgHandler blockHandler = mock(BlockMsgHandler.class);
      ReflectUtils.setFieldValue(handler, "blockMsgHandler", blockHandler);
      Method process = P2pEventHandlerImpl.class
          .getDeclaredMethod("processMessage", PeerConnection.class, byte[].class);
      process.setAccessible(true);
      process.invoke(handler, badPeer, wire);

      Mockito.verifyNoInteractions(blockHandler);
      Mockito.verify(badPeer).disconnect(Mockito.any(Protocol.ReasonCode.class));
      Assert.assertTrue(badPeer.getSyncBlockRequested().containsKey(blockId));
      Assert.assertTrue(badPeer.getSyncBlockInProcess().isEmpty());
      badPeer.onDisconnect();
      Assert.assertNull(requests.getIfPresent(blockId));
      Assert.assertTrue(badPeer.getSyncBlockRequested().isEmpty());
      Mockito.when(delegate.getActivePeer()).thenReturn(Collections.singletonList(goodPeer));
      fetch.invoke(sync);
      Assert.assertSame(goodPeer, requests.getIfPresent(blockId));
      Assert.assertTrue(goodPeer.getSyncBlockRequested().containsKey(blockId));
      Mockito.verify(goodPeer).sendMessage(Mockito.any(FetchInvDataMessage.class));
    } finally {
      sync.close();
    }
  }

  @Test
  public void testDeserializeFailureAllowsRetryFromAnotherPeer() throws Exception {
    chainBaseManager.getDynamicPropertiesStore().saveAllowProtoFilterNum(0);
    SyncService sync = new SyncService();
    try {
      TronNetDelegate delegate = mock(TronNetDelegate.class);
      ReflectUtils.setFieldValue(sync, "tronNetDelegate", delegate);
      ReflectUtils.setFieldValue(sync, "pbftDataSyncHandler", mock(PbftDataSyncHandler.class));
      Mockito.when(delegate.getSolidBlockId())
          .thenReturn(new BlockCapsule.BlockId(Sha256Hash.ZERO_HASH, 0));
      Mockito.when(delegate.getBlockLock()).thenReturn(new Object());

      PeerConnection badPeer = Mockito.spy(context.getBean(PeerConnection.class));
      PeerConnection goodPeer = Mockito.spy(context.getBean(PeerConnection.class));
      Channel badChannel = mock(Channel.class);
      Channel goodChannel = mock(Channel.class);
      Mockito.when(badChannel.getInetSocketAddress()).thenReturn(inetSocketAddress);
      Mockito.when(badChannel.getInetAddress()).thenReturn(inetSocketAddress.getAddress());
      Mockito.when(goodChannel.getInetSocketAddress()).thenReturn(inetSocketAddress);
      Mockito.when(goodChannel.getInetAddress()).thenReturn(inetSocketAddress.getAddress());
      badPeer.setChannel(badChannel);
      goodPeer.setChannel(goodChannel);
      badPeer.setFetchAble(true);
      goodPeer.setFetchAble(true);
      Mockito.when(delegate.getActivePeer()).thenReturn(Arrays.asList(badPeer, goodPeer));

      BlockCapsule block = new BlockCapsule(1, Sha256Hash.ZERO_HASH,
          System.currentTimeMillis(),
          ByteString.copyFrom(ECKey.fromPrivate(BigInteger.ONE).getAddress()));
      BlockCapsule.BlockId blockId = block.getBlockId();
      byte[] data = block.getData();
      // Repeating the singular header field preserves the block but changes its wire length.
      byte[] nonCanonical = Arrays.copyOf(data, data.length * 2);
      System.arraycopy(data, 0, nonCanonical, data.length, data.length);
      BlockMessage message = new BlockMessage(nonCanonical);
      Assert.assertEquals(block.getInstance(), message.getBlockCapsule().getInstance());
      badPeer.getSyncBlockToFetch().add(blockId);
      goodPeer.getSyncBlockToFetch().add(blockId);

      Method fetch = SyncService.class.getDeclaredMethod("startFetchSyncBlock");
      fetch.setAccessible(true);
      fetch.invoke(sync);
      Assert.assertTrue(badPeer.getSyncBlockRequested().containsKey(blockId));
      Assert.assertTrue(goodPeer.getSyncBlockRequested().isEmpty());
      Cache<BlockCapsule.BlockId, PeerConnection> requests =
          (Cache<BlockCapsule.BlockId, PeerConnection>)
              ReflectUtils.getFieldObject(sync, "requestBlockIds");
      Assert.assertSame(badPeer, requests.getIfPresent(blockId));

      BlockMsgHandler handler = new BlockMsgHandler();
      ReflectUtils.setFieldValue(handler, "syncService", sync);
      handler.processMessage(badPeer, message);
      Assert.assertTrue(badPeer.getSyncBlockRequested().isEmpty());
      Assert.assertTrue(badPeer.getSyncBlockInProcess().contains(blockId));
      // A filter change while the block is queued can make deferred validation fail.
      chainBaseManager.getDynamicPropertiesStore().saveAllowProtoFilterNum(1);
      Method handle = SyncService.class.getDeclaredMethod("handleSyncBlock");
      handle.setAccessible(true);
      ReflectUtils.setFieldValue(sync, "fetchFlag", false);
      handle.invoke(sync);

      Assert.assertNull("A failed parse must release the global request",
          requests.getIfPresent(blockId));
      Assert.assertFalse(badPeer.getSyncBlockInProcess().contains(blockId));
      Mockito.verify(badPeer, Mockito.never()).disconnect(Mockito.any(Protocol.ReasonCode.class));
      Mockito.verify(delegate, Mockito.never())
          .processBlock(Mockito.any(BlockCapsule.class), Mockito.anyBoolean());
      Assert.assertTrue((boolean) ReflectUtils.getFieldObject(sync, "fetchFlag"));

      Mockito.when(delegate.getActivePeer()).thenReturn(Collections.singletonList(goodPeer));
      fetch.invoke(sync);
      ArgumentCaptor<FetchInvDataMessage> request =
          ArgumentCaptor.forClass(FetchInvDataMessage.class);
      Mockito.verify(goodPeer).sendMessage(request.capture());
      Assert.assertEquals(Collections.singletonList(blockId), request.getValue().getHashList());
      Assert.assertSame(goodPeer, requests.getIfPresent(blockId));

      // Leave a next block queued so successful processing does not request a chain summary.
      BlockCapsule.BlockId next = new BlockCapsule.BlockId(Sha256Hash.ZERO_HASH, 2);
      goodPeer.getSyncBlockToFetch().add(next);
      handler.processMessage(goodPeer, new BlockMessage(block));
      handle.invoke(sync);
      Mockito.verify(delegate).processBlock(Mockito.any(BlockCapsule.class), Mockito.eq(true));
      Assert.assertEquals(blockId, goodPeer.getBlockBothHave());
      Assert.assertEquals(next, goodPeer.getSyncBlockToFetch().peek());
      Assert.assertFalse(goodPeer.getSyncBlockInProcess().contains(blockId));
      Mockito.verify(goodPeer, Mockito.never()).disconnect(Mockito.any(Protocol.ReasonCode.class));
    } finally {
      sync.close();
    }
  }

  @Test
  public void sameAddressPeersKeepSyncRequestsAndResponsesIndependent() throws Exception {
    InetSocketAddress address = new InetSocketAddress("127.0.0.2", 10001);
    Channel firstChannel = new Channel();
    Channel secondChannel = new Channel();
    ReflectUtils.setFieldValue(firstChannel, "inetSocketAddress", address);
    ReflectUtils.setFieldValue(firstChannel, "inetAddress", address.getAddress());
    ReflectUtils.setFieldValue(secondChannel, "inetSocketAddress", address);
    ReflectUtils.setFieldValue(secondChannel, "inetAddress", address.getAddress());

    List<Message> firstMessages = new ArrayList<>();
    List<Message> secondMessages = new ArrayList<>();
    PeerConnection first = new PeerConnection() {
      @Override
      public void sendMessage(Message message) {
        firstMessages.add(message);
      }
    };
    PeerConnection second = new PeerConnection() {
      @Override
      public void sendMessage(Message message) {
        secondMessages.add(message);
      }
    };
    first.setChannel(firstChannel);
    second.setChannel(secondChannel);
    Assert.assertEquals(firstChannel, secondChannel);
    Assert.assertNotEquals(first, second);
    first.setFetchAble(true);
    second.setFetchAble(true);

    BlockMessage firstBlock = new BlockMessage(new BlockCapsule(1, Sha256Hash.ZERO_HASH,
        System.currentTimeMillis(), Sha256Hash.ZERO_HASH.getByteString()));
    BlockMessage secondBlock = new BlockMessage(new BlockCapsule(2, Sha256Hash.ZERO_HASH,
        System.currentTimeMillis(), Sha256Hash.ZERO_HASH.getByteString()));
    BlockCapsule.BlockId firstId = firstBlock.getBlockId();
    BlockCapsule.BlockId secondId = secondBlock.getBlockId();
    first.getSyncBlockToFetch().add(firstId);
    second.getSyncBlockToFetch().add(secondId);

    TronNetDelegate delegate = mock(TronNetDelegate.class);
    Mockito.when(delegate.getActivePeer()).thenReturn(Arrays.asList(first, second));
    SyncService isolated = new SyncService();
    ReflectUtils.setFieldValue(isolated, "tronNetDelegate", delegate);
    try {
      Method fetch = SyncService.class.getDeclaredMethod("startFetchSyncBlock");
      fetch.setAccessible(true);
      fetch.invoke(isolated);

      Assert.assertEquals(1, firstMessages.size());
      Assert.assertEquals(1, secondMessages.size());
      Assert.assertTrue(firstMessages.get(0) instanceof FetchInvDataMessage);
      Assert.assertTrue(secondMessages.get(0) instanceof FetchInvDataMessage);
      Assert.assertEquals(Collections.singletonList(firstId),
          ((FetchInvDataMessage) firstMessages.get(0)).getHashList());
      Assert.assertEquals(Collections.singletonList(secondId),
          ((FetchInvDataMessage) secondMessages.get(0)).getHashList());

      Cache<BlockCapsule.BlockId, PeerConnection> requests =
          (Cache<BlockCapsule.BlockId, PeerConnection>)
              ReflectUtils.getFieldObject(isolated, "requestBlockIds");
      Assert.assertSame(first, requests.getIfPresent(firstId));
      Assert.assertSame(second, requests.getIfPresent(secondId));

      isolated.processBlock(first, firstBlock);
      isolated.processBlock(second, secondBlock);
      Map<UnparsedBlock, PeerConnection> received =
          (Map<UnparsedBlock, PeerConnection>)
              ReflectUtils.getFieldObject(isolated, "blockJustReceived");
      Assert.assertEquals(2, received.size());
      received.forEach((block, owner) -> {
        if (block.getBlockId().equals(firstId)) {
          Assert.assertSame(first, owner);
        } else {
          Assert.assertEquals(secondId, block.getBlockId());
          Assert.assertSame(second, owner);
        }
      });

      // A stale entry on the closing peer must not clear the survivor's request.
      first.getSyncBlockRequested().put(secondId, System.currentTimeMillis());
      isolated.onDisconnect(first);
      Assert.assertNull(requests.getIfPresent(firstId));
      Assert.assertSame(second, requests.getIfPresent(secondId));
      Assert.assertTrue(second.getSyncBlockRequested().containsKey(secondId));
    } finally {
      isolated.close();
    }
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
