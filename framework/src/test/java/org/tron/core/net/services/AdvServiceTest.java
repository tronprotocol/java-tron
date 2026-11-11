package org.tron.core.net.services;

import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.tron.common.TestConstants;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.es.ExecutorServiceManager;
import org.tron.common.overlay.message.Message;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.ReflectUtils;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.net.P2pEventHandlerImpl;
import org.tron.core.net.TronNetDelegate;
import org.tron.core.net.message.adv.BlockMessage;
import org.tron.core.net.message.adv.FetchInvDataMessage;
import org.tron.core.net.message.adv.InventoryMessage;
import org.tron.core.net.message.adv.TransactionMessage;
import org.tron.core.net.peer.Item;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.peer.PeerManager;
import org.tron.core.net.service.adv.AdvService;
import org.tron.core.net.service.effective.EffectiveCheckService;
import org.tron.core.net.service.sync.SyncService;
import org.tron.p2p.connection.Channel;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Inventory.InventoryType;

public class AdvServiceTest {
  private static TronApplicationContext context;
  private static AdvService service;
  private static P2pEventHandlerImpl p2pEventHandler;
  private static ApplicationContext ctx;

  @ClassRule
  public static final TemporaryFolder temporaryFolder = new TemporaryFolder();

  @BeforeClass
  public static void init() throws IOException {
    Args.setParam(new String[]{"--output-directory",
        temporaryFolder.newFolder().toString(), "--debug"}, TestConstants.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    service = context.getBean(AdvService.class);
    p2pEventHandler = context.getBean(P2pEventHandlerImpl.class);
    ctx = (ApplicationContext) ReflectUtils.getFieldObject(p2pEventHandler, "ctx");
  }

  @AfterClass
  public static void after() {
    for (PeerConnection p : PeerManager.getPeers()) {
      PeerManager.remove(p.getChannel());
    }
    Args.clearParam();
    context.destroy();
  }

  @Test
  public void test() throws IOException {
    testAddInv();
    testBroadcast();
    testTrxBroadcast();
  }

  private void testAddInv() {
    boolean flag;
    Item itemTrx = new Item(Sha256Hash.ZERO_HASH, InventoryType.TRX);
    flag = service.addInv(itemTrx);
    Assert.assertTrue(flag);
    flag = service.addInv(itemTrx);
    Assert.assertFalse(flag);

    Item itemBlock = new Item(Sha256Hash.ZERO_HASH, InventoryType.BLOCK);
    flag = service.addInv(itemBlock);
    Assert.assertFalse(flag);

    BlockCapsule.BlockId blockId = new BlockCapsule.BlockId(Sha256Hash.ZERO_HASH, 1000L);
    itemBlock = new Item(blockId, InventoryType.BLOCK);
    flag = service.addInv(itemBlock);
    Assert.assertTrue(flag);
    flag = service.addInv(itemBlock);
    Assert.assertFalse(flag);

    blockId = new BlockCapsule.BlockId(Sha256Hash.ZERO_HASH, 10000L);
    itemBlock = new Item(blockId, InventoryType.BLOCK);
    service.addInvToCache(itemBlock);
    flag = service.addInv(itemBlock);
    Assert.assertFalse(flag);
  }

  private void testBroadcast() {
    InetSocketAddress inetSocketAddress =
            new InetSocketAddress("127.0.0.2", 10001);

    Channel c1 = mock(Channel.class);
    Mockito.when(c1.getInetSocketAddress()).thenReturn(inetSocketAddress);
    Mockito.when(c1.getInetAddress()).thenReturn(inetSocketAddress.getAddress());

    PeerConnection peer = PeerManager.add(ctx, c1);
    peer.setChannel(c1);
    peer.setNeedSyncFromUs(false);
    peer.setNeedSyncFromPeer(false);

    BlockCapsule blockCapsule = new BlockCapsule(1, Sha256Hash.ZERO_HASH,
            System.currentTimeMillis(), Sha256Hash.ZERO_HASH.getByteString());
    BlockMessage msg = new BlockMessage(blockCapsule);
    service.broadcast(msg);
    Item item = new Item(blockCapsule.getBlockId(), InventoryType.BLOCK);
    Assert.assertNotNull(service.getMessage(item));
    Assert.assertNotNull(peer.getAdvInvSpread().getIfPresent(item));
  }

  private void testTrxBroadcast() throws IOException {
    Protocol.Transaction trx = Protocol.Transaction.newBuilder()
        .setRawData(
        Protocol.Transaction.raw.newBuilder()
            .setRefBlockNum(1)
            .setExpiration(System.currentTimeMillis() + 3000).build())
        .addRet(Protocol.Transaction.Result.newBuilder().setFee(123L).build())
        .build();
    CommonParameter.getInstance().setValidContractProtoThreadNum(1);
    TransactionMessage msg = new TransactionMessage(trx);
    service.broadcast(msg);
    Item item = new Item(msg.getMessageId(), InventoryType.TRX);
    TransactionMessage cached = (TransactionMessage) service.getMessage(item);
    Assert.assertNotNull(cached);
    assertRelayPreservesOriginal(trx, msg, cached);

    TransactionMessage fastMsg = new TransactionMessage(trx);
    service.fastBroadcastTransaction(fastMsg);
    TransactionMessage fastCached = (TransactionMessage) service.getMessage(item);
    Assert.assertNotSame(cached, fastCached);
    assertRelayPreservesOriginal(trx, fastMsg, fastCached);

    Protocol.Transaction expiredTrx = Protocol.Transaction.newBuilder()
        .setRawData(
            Protocol.Transaction.raw.newBuilder()
            .setRefBlockNum(1)
            .setExpiration(System.currentTimeMillis() - 1).build())
        .build();
    CommonParameter.getInstance().setValidContractProtoThreadNum(1);
    TransactionMessage msg1 = new TransactionMessage(expiredTrx);
    service.broadcast(msg);
    Item item1 = new Item(msg1.getMessageId(), InventoryType.TRX);
    Assert.assertNull(service.getMessage(item1));
  }

  private void assertRelayPreservesOriginal(Protocol.Transaction original,
      TransactionMessage message, TransactionMessage cached) throws IOException {
    Assert.assertNotNull(cached);
    Assert.assertEquals(original, message.getTransactionCapsule().getInstance());
    Assert.assertArrayEquals(original.toByteArray(), message.getData());
    Assert.assertNotSame(message.getTransactionCapsule(), cached.getTransactionCapsule());
    Protocol.Transaction relay = Protocol.Transaction.parseFrom(cached.getData());
    Assert.assertEquals(0, relay.getRetCount());
    Assert.assertEquals(original.getRawData(), relay.getRawData());
    Assert.assertEquals(original.getSignatureList(), relay.getSignatureList());
    Assert.assertEquals(relay, cached.getTransactionCapsule().getInstance());
    Assert.assertEquals(message.getMessageId(), cached.getMessageId());
  }

  @Test
  public void sameAddressPeersReceiveBroadcastAndDisconnectRetryIndependently() throws Exception {
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
    first.setNeedSyncFromPeer(false);
    first.setNeedSyncFromUs(false);
    second.setNeedSyncFromPeer(false);
    second.setNeedSyncFromUs(false);

    TronNetDelegate delegate = mock(TronNetDelegate.class);
    Mockito.when(delegate.getActivePeer()).thenReturn(Arrays.asList(first, second));
    AdvService isolated = new AdvService();
    ReflectUtils.setFieldValue(isolated, "tronNetDelegate", delegate);
    try {
      BlockCapsule block = new BlockCapsule(1, Sha256Hash.ZERO_HASH,
          System.currentTimeMillis(), Sha256Hash.ZERO_HASH.getByteString());
      isolated.broadcast(new BlockMessage(block));
      Assert.assertEquals(1, firstMessages.size());
      Assert.assertEquals(1, secondMessages.size());
      Assert.assertTrue(firstMessages.get(0) instanceof InventoryMessage);
      Assert.assertTrue(secondMessages.get(0) instanceof InventoryMessage);
      Assert.assertEquals(Collections.singletonList(block.getBlockId()),
          ((InventoryMessage) firstMessages.get(0)).getHashList());
      Assert.assertEquals(Collections.singletonList(block.getBlockId()),
          ((InventoryMessage) secondMessages.get(0)).getHashList());

      Item item = new Item(Sha256Hash.ZERO_HASH, InventoryType.TRX);
      first.getAdvInvRequest().put(item, System.currentTimeMillis());
      second.getAdvInvReceive().put(item, System.currentTimeMillis());
      isolated.onDisconnect(first);
      Assert.assertTrue(second.getAdvInvRequest().isEmpty());
      Assert.assertEquals(1, secondMessages.size());
      Method fetch = AdvService.class.getDeclaredMethod("consumerInvToFetch");
      fetch.setAccessible(true);
      fetch.invoke(isolated);
      Assert.assertTrue(second.getAdvInvRequest().containsKey(item));
      Assert.assertEquals(1, firstMessages.size());
      Assert.assertEquals(2, secondMessages.size());
      Assert.assertTrue(secondMessages.get(1) instanceof FetchInvDataMessage);
      Assert.assertEquals(Collections.singletonList(item.getHash()),
          ((FetchInvDataMessage) secondMessages.get(1)).getHashList());
    } finally {
      isolated.close();
    }
  }

  @Test(timeout = 20_000)
  public void inventorySendAllowsConcurrentDisconnectCleanup() throws Exception {
    AdvService isolated = new AdvService();
    P2pEventHandlerImpl handler = new P2pEventHandlerImpl();
    ReflectUtils.setFieldValue(handler, "effectiveCheckService", mock(EffectiveCheckService.class));
    SyncService syncService = mock(SyncService.class);
    PeerConnection spreadingPeer = Mockito.spy(new PeerConnection());
    PeerConnection disconnectingPeer = new PeerConnection();
    for (PeerConnection peer : Arrays.asList(spreadingPeer, disconnectingPeer)) {
      ReflectUtils.setFieldValue(peer, "advService", isolated);
      ReflectUtils.setFieldValue(peer, "syncService", syncService);
    }
    spreadingPeer.setNeedSyncFromPeer(false);
    spreadingPeer.setNeedSyncFromUs(false);

    Channel spreadingChannel = mock(Channel.class);
    Channel disconnectingChannel = mock(Channel.class);
    InetSocketAddress address = new InetSocketAddress("127.0.0.3", 10001);
    Mockito.when(spreadingChannel.getInetSocketAddress()).thenReturn(address);
    Mockito.when(spreadingChannel.getInetAddress()).thenReturn(address.getAddress());
    Mockito.when(disconnectingChannel.getInetSocketAddress()).thenReturn(address);
    Mockito.when(disconnectingChannel.getInetAddress()).thenReturn(address.getAddress());
    ApplicationContext peerContext = mock(ApplicationContext.class);
    Mockito.when(peerContext.getBean(PeerConnection.class))
        .thenReturn(spreadingPeer, disconnectingPeer);
    TronNetDelegate delegate = mock(TronNetDelegate.class);
    Mockito.when(delegate.getActivePeer()).thenReturn(Collections.singletonList(spreadingPeer));
    ReflectUtils.setFieldValue(isolated, "tronNetDelegate", delegate);
    ScheduledExecutorService fetchExecutor =
        ReflectUtils.getFieldValue(isolated, "fetchExecutor");

    CountDownLatch sending = new CountDownLatch(1);
    CountDownLatch cleanupFinished = new CountDownLatch(1);
    List<Message> sent = Collections.synchronizedList(new ArrayList<>());
    ExecutorService executor = ExecutorServiceManager.newFixedThreadPool("adv-send-test", 2);
    try {
      Assert.assertSame(spreadingPeer, PeerManager.add(peerContext, spreadingChannel));
      Assert.assertSame(disconnectingPeer, PeerManager.add(peerContext, disconnectingChannel));
      Item fetchItem = new Item(Sha256Hash.ZERO_HASH, InventoryType.TRX);
      spreadingPeer.getAdvInvReceive().put(fetchItem, System.currentTimeMillis());
      Assert.assertTrue(isolated.addInv(fetchItem));
      BlockCapsule block = new BlockCapsule(1, Sha256Hash.ZERO_HASH,
          System.currentTimeMillis(), Sha256Hash.ZERO_HASH.getByteString());
      Item spreadItem = new Item(block.getBlockId(), InventoryType.BLOCK);
      Mockito.doAnswer(invocation -> {
        handler.onDisconnect(spreadingChannel);
        return null;
      }).when(spreadingChannel).close();
      Mockito.doAnswer(invocation -> {
        Message message = invocation.getArgument(0);
        sent.add(message);
        if (!(message instanceof FetchInvDataMessage)) {
          Assert.assertNotNull(spreadingPeer.getAdvInvSpread().getIfPresent(spreadItem));
          sending.countDown();
          // Bound the wait before entering a synchronous close callback, so a regression
          // fails without leaving threads blocked on each other's monitors.
          Assert.assertTrue("Disconnect cleanup must complete while inventory sending is paused",
              cleanupFinished.await(5, TimeUnit.SECONDS));
          Assert.assertEquals("Disconnect cleanup must defer fetching to the executor",
              1, sent.size());
          // Run a fetch pass on its executor while inventory sending is still paused.
          // This must complete without waiting for the advertisement monitor.
          fetchExecutor.submit(() -> {
            ReflectUtils.invokeMethod(isolated, "consumerInvToFetch");
          }).get(5, TimeUnit.SECONDS);
          spreadingChannel.close();
        }
        return null;
      }).when(spreadingPeer).sendMessage(Mockito.any(Message.class));

      Future<?> cleanup = executor.submit(() -> {
        Assert.assertTrue(sending.await(5, TimeUnit.SECONDS));
        handler.onDisconnect(disconnectingChannel);
        cleanupFinished.countDown();
        return null;
      });
      Future<?> spread = executor.submit(() -> isolated.broadcast(new BlockMessage(block)));
      spread.get(10, TimeUnit.SECONDS);
      cleanup.get(10, TimeUnit.SECONDS);

      Assert.assertEquals(2, sent.size());
      Assert.assertEquals(Collections.singletonList(block.getBlockId()),
          ((InventoryMessage) sent.get(0)).getHashList());
      Assert.assertTrue(sent.get(1) instanceof FetchInvDataMessage);
      Assert.assertEquals(Collections.singletonList(fetchItem.getHash()),
          ((FetchInvDataMessage) sent.get(1)).getHashList());
      Mockito.verify(spreadingChannel).close();
      Mockito.verify(syncService).onDisconnect(spreadingPeer);
      Mockito.verify(syncService).onDisconnect(disconnectingPeer);
      Assert.assertNull(PeerManager.getPeerConnection(spreadingChannel));
      Assert.assertNull(PeerManager.getPeerConnection(disconnectingChannel));
    } finally {
      sending.countDown();
      cleanupFinished.countDown();
      executor.shutdown();
      try {
        if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
          executor.shutdownNow();
          Assert.assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
      } finally {
        PeerManager.remove(spreadingChannel);
        PeerManager.remove(disconnectingChannel);
        isolated.close();
      }
    }
  }

}
