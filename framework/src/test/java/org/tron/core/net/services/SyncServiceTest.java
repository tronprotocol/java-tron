package org.tron.core.net.services;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.tron.core.net.PeerSyncTestSupport.blockId;
import static org.tron.core.net.PeerSyncTestSupport.helloMessage;

import com.google.common.cache.Cache;
import com.google.protobuf.ByteString;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.tron.common.BaseMethodTest;
import org.tron.common.overlay.message.Message;
import org.tron.common.utils.Pair;
import org.tron.common.utils.ReflectUtils;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.net.P2pEventHandlerImpl;
import org.tron.core.net.PeerSyncTestSupport;
import org.tron.core.net.TronNetDelegate;
import org.tron.core.net.message.adv.BlockMessage;
import org.tron.core.net.message.adv.FetchInvDataMessage;
import org.tron.core.net.message.sync.ChainInventoryMessage;
import org.tron.core.net.message.sync.SyncBlockChainMessage;
import org.tron.core.net.messagehandler.BlockMsgHandler;
import org.tron.core.net.messagehandler.ChainInventoryMsgHandler;
import org.tron.core.net.messagehandler.PbftDataSyncHandler;
import org.tron.core.net.peer.Item;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.peer.PeerManager;
import org.tron.core.net.peer.TronState;
import org.tron.core.net.service.adv.AdvService;
import org.tron.core.net.service.fetchblock.FetchBlockService;
import org.tron.core.net.service.sync.SyncService;
import org.tron.core.net.service.sync.UnparsedBlock;
import org.tron.p2p.connection.Channel;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Inventory.InventoryType;
import org.tron.protos.Protocol.ReasonCode;

public class SyncServiceTest extends BaseMethodTest {
  private SyncService service;
  private SyncService recoveryService;
  private TronNetDelegate delegate;
  private PeerConnection failed;
  private PeerConnection replacement;
  private List<PeerConnection> peers;
  private final AtomicReference<BlockId> head = new AtomicReference<>();
  private final Set<BlockId> stored = new HashSet<>();
  private final Map<Long, BlockId> chainIds = new HashMap<>();

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
    if (recoveryService != null) {
      recoveryService.close();
    }
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

  @Test
  public void testRejectedSummaryRecoversPreviouslyDiscardedBlocks() throws Exception {
    initRecovery();
    BlockCapsule parent = block(101, head.get());
    BlockCapsule child = block(102, parent.getBlockId());
    BlockMsgHandler blocks = new BlockMsgHandler();
    ReflectUtils.setFieldValue(blocks, "tronNetDelegate", delegate);
    ReflectUtils.setFieldValue(blocks, "syncService", recoveryService);
    ReflectUtils.setFieldValue(blocks, "fetchBlockService", mock(FetchBlockService.class));
    ReflectUtils.setFieldValue(blocks, "fastForward", false);
    Item childItem = new Item(child.getBlockId(), InventoryType.BLOCK);
    failed.getAdvInvReceive().put(childItem, System.currentTimeMillis() - 1);
    failed.getAdvInvRequest().put(childItem, System.currentTimeMillis());

    blocks.processMessage(failed, new BlockMessage(child));
    Assert.assertFalse(failed.getAdvInvRequest().containsKey(childItem));
    Assert.assertFalse(stored.contains(child.getBlockId()));
    ChainInventoryMsgHandler inventory = new ChainInventoryMsgHandler();
    ReflectUtils.setFieldValue(inventory, "tronNetDelegate", delegate);
    ReflectUtils.setFieldValue(inventory, "syncService", recoveryService);
    P2pEventHandlerImpl events = new P2pEventHandlerImpl();
    ReflectUtils.setFieldValue(events, "chainInventoryMsgHandler", inventory);
    ChainInventoryMessage echo = new ChainInventoryMessage(
        Collections.singletonList(head.get()), 0L);
    ReflectUtils.invokeMethod(events, "processMessage",
        new Class<?>[]{PeerConnection.class, byte[].class}, failed, echo.getSendBytes());
    verify(failed).disconnect(ReasonCode.SYNC_FAIL);

    peers.remove(failed);
    when(failed.getChannel().isDisconnect()).thenReturn(true);
    ReflectUtils.setFieldValue(failed, "syncService", recoveryService);
    ReflectUtils.setFieldValue(failed, "advService", mock(AdvService.class));
    failed.onDisconnect();
    runFetchTask();
    verify(replacement).sendMessage(any(SyncBlockChainMessage.class));

    inventory.processMessage(replacement, new ChainInventoryMessage(
        Arrays.asList(blockId(100), parent.getBlockId(), child.getBlockId()), 0L));
    runFetchTask();
    ArgumentCaptor<Message> requests = ArgumentCaptor.forClass(Message.class);
    verify(replacement, times(2)).sendMessage(requests.capture());
    Assert.assertEquals(Arrays.asList(parent.getBlockId(), child.getBlockId()),
        ((FetchInvDataMessage) requests.getValue()).getHashList());

    blocks.processMessage(replacement, new BlockMessage(parent));
    blocks.processMessage(replacement, new BlockMessage(child));
    ReflectUtils.invokeMethod(recoveryService, "handleSyncBlock");
    inventory.processMessage(replacement, new ChainInventoryMessage(
        Collections.singletonList(child.getBlockId()), 0L));

    Assert.assertEquals(child.getBlockId(), head.get());
    Assert.assertTrue(stored.contains(parent.getBlockId()));
    Assert.assertTrue(stored.contains(child.getBlockId()));
    Assert.assertEquals(TronState.SYNC_COMPLETED, replacement.getTronState());
    Assert.assertFalse(replacement.isNeedSyncFromPeer());
    verify(replacement, never()).disconnect(any());
  }

  @Test
  public void testRecoveryFiltersCandidatesAndAllowsPendingTransactions() throws Exception {
    initRecovery();
    PeerConnection unhandshaken = readyPeer(18890);
    unhandshaken.setHelloMessageReceive(null);
    PeerConnection disconnected = readyPeer(18891);
    when(disconnected.getChannel().isDisconnect()).thenReturn(true);
    PeerConnection syncing = readyPeer(18892);
    syncing.setTronState(TronState.SYNCING);
    PeerConnection pendingSummary = readyPeer(18893);
    pendingSummary.setSyncChainRequested(new Pair<>(new LinkedList<>(
        Collections.singletonList(blockId(100))), System.currentTimeMillis()));
    PeerConnection pendingBlock = readyPeer(18894);
    pendingBlock.getSyncBlockRequested().put(blockId(101), System.currentTimeMillis());
    peers.addAll(Arrays.asList(unhandshaken, disconnected, syncing, pendingSummary, pendingBlock));
    replacement.getAdvInvRequest().put(new Item(Sha256Hash.ZERO_HASH, InventoryType.TRX),
        System.currentTimeMillis());
    Assert.assertFalse(replacement.isIdle());
    queueRecovery(failed);
    // The failed connection must remain excluded even if its state changes during cleanup.
    failed.setTronState(TronState.SYNC_COMPLETED);

    runFetchTask();

    verify(replacement).sendMessage(any(SyncBlockChainMessage.class));
    for (PeerConnection peer : Arrays.asList(failed, unhandshaken, disconnected, syncing,
        pendingSummary, pendingBlock)) {
      verify(peer, never()).sendMessage(any());
    }
  }

  @Test
  public void testRecoveryWaitsForAnEligibleConnection() throws Exception {
    initRecovery();
    peers.clear();
    queueRecovery(failed);
    runFetchTask();
    Assert.assertTrue((boolean) ReflectUtils.getFieldObject(recoveryService, "fetchFlag"));
    PeerConnection unhandshaken = readyPeer(18890);
    unhandshaken.setHelloMessageReceive(null);
    peers.add(unhandshaken);
    runFetchTask();
    Assert.assertTrue((boolean) ReflectUtils.getFieldObject(recoveryService, "fetchFlag"));
    peers.add(replacement);

    runFetchTask();
    runFetchTask();

    Assert.assertFalse((boolean) ReflectUtils.getFieldObject(recoveryService, "fetchFlag"));
    verify(replacement, times(1)).sendMessage(any(SyncBlockChainMessage.class));
    verify(unhandshaken, never()).sendMessage(any());
  }

  @Test
  public void testCompletedPeerDisconnectDoesNotStartRecovery() throws Exception {
    initRecovery();
    recoveryService.onDisconnect(failed);

    runFetchTask();

    verify(replacement, never()).sendMessage(any());
  }

  @Test
  public void testRecoveryStartsOnlyOneRandomCandidate() throws Exception {
    initRecovery();
    PeerConnection other = readyPeer(18890);
    peers.add(other);
    AtomicInteger requests = new AtomicInteger();
    for (PeerConnection peer : Arrays.asList(replacement, other)) {
      doAnswer(call -> {
        requests.incrementAndGet();
        return null;
      }).when(peer).sendMessage(any(SyncBlockChainMessage.class));
    }
    queueRecovery(failed);

    runFetchTask();
    runFetchTask();

    Assert.assertEquals(1, requests.get());
  }

  @Test
  public void testAnotherFailedSourceCanTriggerRecovery() throws Exception {
    initRecovery();
    queueRecovery(failed);
    runFetchTask();
    PeerConnection next = readyPeer(18890);
    peers.remove(replacement);
    peers.add(next);

    recoveryService.onDisconnect(replacement);
    runFetchTask();

    verify(next).sendMessage(any(SyncBlockChainMessage.class));
  }

  private void initRecovery() throws Exception {
    recoveryService = new SyncService();
    delegate = mock(TronNetDelegate.class);
    failed = readyPeer(18888);
    replacement = readyPeer(18889);
    peers = new ArrayList<>(Arrays.asList(failed, replacement));
    head.set(blockId(100));
    stored.add(blockId(0));
    stored.add(head.get());
    chainIds.put(0L, blockId(0));
    chainIds.put(100L, head.get());
    when(delegate.getActivePeer()).thenReturn(peers);
    when(delegate.getHeadBlockId()).thenAnswer(call -> head.get());
    when(delegate.getKhaosDbHeadBlockId()).thenAnswer(call -> head.get());
    when(delegate.getGenesisBlockId()).thenReturn(blockId(0));
    when(delegate.getSolidBlockId()).thenReturn(blockId(0));
    when(delegate.getBlockTime(any())).thenReturn(System.currentTimeMillis() - 30_000_000L);
    when(delegate.getBlockIdByNum(anyLong())).thenAnswer(call -> {
      long number = call.getArgument(0);
      return chainIds.getOrDefault(number, blockId(number));
    });
    when(delegate.containBlock(any())).thenAnswer(call -> stored.contains(call.getArgument(0)));
    when(delegate.containBlockInMainChain(any()))
        .thenAnswer(call -> stored.contains(call.getArgument(0)));
    when(delegate.getBlockLock()).thenReturn(new Object());
    when(delegate.getForkLock()).thenReturn(new Object());
    when(delegate.validAdvBlock(any(), any())).thenReturn(true);
    doAnswer(call -> {
      BlockCapsule block = call.getArgument(0);
      stored.add(block.getBlockId());
      chainIds.put(block.getNum(), block.getBlockId());
      head.set(block.getBlockId());
      return null;
    }).when(delegate).processBlock(any(), anyBoolean());
    ReflectUtils.setFieldValue(recoveryService, "tronNetDelegate", delegate);
    ReflectUtils.setFieldValue(recoveryService, "pbftDataSyncHandler",
        mock(PbftDataSyncHandler.class));
    ReflectUtils.setFieldValue(recoveryService, "maxPendingBlockSize", 10);
  }

  private void queueRecovery(PeerConnection peer) {
    peer.setTronState(TronState.SYNCING);
    peer.setNeedSyncFromPeer(true);
    recoveryService.onDisconnect(peer);
    Assert.assertTrue((boolean) ReflectUtils.getFieldObject(recoveryService, "fetchFlag"));
  }

  private void runFetchTask() {
    if ((boolean) ReflectUtils.getFieldObject(recoveryService, "fetchFlag")) {
      recoveryService.setFetchFlag(false);
      ReflectUtils.invokeMethod(recoveryService, "startFetchSyncBlock");
    }
  }

  private static PeerConnection readyPeer(int port) throws Exception {
    PeerConnection peer = PeerSyncTestSupport.peer(port);
    peer.setHelloMessageReceive(helloMessage(100));
    peer.setTronState(TronState.SYNC_COMPLETED);
    peer.setNeedSyncFromPeer(false);
    peer.setNeedSyncFromUs(false);
    return peer;
  }

  private static BlockCapsule block(long number, BlockId parent) {
    BlockCapsule block = new BlockCapsule(number, parent, System.currentTimeMillis() - 1_000,
        ByteString.copyFromUtf8("witness"));
    block.setMerkleRoot();
    return block;
  }
}
