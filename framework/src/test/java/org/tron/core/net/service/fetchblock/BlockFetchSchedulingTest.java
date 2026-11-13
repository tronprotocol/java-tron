package org.tron.core.net.service.fetchblock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.utils.Pair;
import org.tron.common.utils.ReflectUtils;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.config.Parameter.NetConstants;
import org.tron.core.net.TronNetDelegate;
import org.tron.core.net.message.adv.BlockMessage;
import org.tron.core.net.message.adv.FetchInvDataMessage;
import org.tron.core.net.peer.Item;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.service.adv.AdvService;
import org.tron.core.net.service.sync.SyncService;
import org.tron.p2p.connection.Channel;
import org.tron.protos.Protocol.Inventory.InventoryType;

public class BlockFetchSchedulingTest {

  private FetchBlockService fetch;
  private AdvService adv;
  private TronNetDelegate delegate;
  private PeerConnection first;
  private PeerConnection second;
  private List<PeerConnection> peers;
  private Item item;

  @Before
  public void setUp() {
    fetch = new FetchBlockService();
    adv = new AdvService();
    delegate = mock(TronNetDelegate.class);
    ChainBaseManager chain = mock(ChainBaseManager.class);
    when(chain.getHeadBlockNum()).thenReturn(100L);
    first = peer(18888);
    second = peer(18889);
    peers = new ArrayList<>(Arrays.asList(first, second));
    item = blockItem(101);
    when(delegate.getActivePeer()).thenReturn(peers);
    ReflectUtils.setFieldValue(fetch, "tronNetDelegate", delegate);
    ReflectUtils.setFieldValue(fetch, "chainBaseManager", chain);
    ReflectUtils.setFieldValue(adv, "tronNetDelegate", delegate);
    ReflectUtils.setFieldValue(adv, "fetchBlockService", fetch);
  }

  @After
  public void tearDown() {
    adv.close();
    fetch.close();
  }

  @Test
  public void pendingTransactionsAllowInitialBlockFetch() {
    first.getAdvInvRequest().put(transaction(), System.currentTimeMillis());
    announce(first, item);

    Assert.assertTrue(adv.addInv(item));

    Assert.assertTrue(first.getAdvInvRequest().containsKey(transaction()));
    Assert.assertTrue(first.getAdvInvRequest().containsKey(item));
    verify(first).sendMessage(any(FetchInvDataMessage.class));
  }

  @Test
  public void initialFetchPrefersFullyIdleSourceOverFasterTransactionSource() {
    first.getAdvInvRequest().put(transaction(), System.currentTimeMillis());
    when(first.getChannel().getAvgLatency()).thenReturn(1L);
    when(second.getChannel().getAvgLatency()).thenReturn(500L);
    announce(first, item);
    announce(second, item);

    adv.addInv(item);

    verify(first, never()).sendMessage(any());
    verify(second).sendMessage(any(FetchInvDataMessage.class));
  }

  @Test
  public void backupPrefersLowerLatencyEvenWithPendingTransactions() throws Exception {
    PeerConnection slower = peer(18890);
    peers.add(slower);
    when(second.getChannel().getAvgLatency()).thenReturn(10L);
    when(slower.getChannel().getAvgLatency()).thenReturn(100L);
    second.getAdvInvRequest().put(transaction(), System.currentTimeMillis());
    announce(second, item);
    announce(slower, item);
    beginFetch(first, System.currentTimeMillis() - 2_000);

    tick(state());

    verify(second).sendMessage(any(FetchInvDataMessage.class));
    verify(slower, never()).sendMessage(any());
    Assert.assertNull("Keep the baseline single-backup policy", state());
  }

  @Test
  public void blockFetchExcludesPendingBlocksAndSyncWork() {
    announce(first, item);
    long now = System.currentTimeMillis();
    first.getAdvInvRequest().put(transaction(), now);
    Assert.assertFalse(first.isIdle());
    Assert.assertTrue(fetch.canFetchBlock(first, item, now));
    first.getAdvInvRequest().put(blockItem(102), now);
    Assert.assertFalse(first.isIdleForAdvBlockFetch());
    first.getAdvInvRequest().remove(blockItem(102));
    first.getSyncBlockRequested().put(new BlockId(), now);
    Assert.assertFalse(fetch.canFetchBlock(first, item, now));
    first.getSyncBlockRequested().clear();
    first.setSyncChainRequested(new Pair<>(new ArrayDeque<>(), now));
    Assert.assertFalse(fetch.canFetchBlock(first, item, now));
    first.setSyncChainRequested(null);
    first.getSyncBlockInProcess().add(new BlockId());
    Assert.assertFalse(fetch.canFetchBlock(first, item, now));
    first.getSyncBlockInProcess().clear();
    first.setNeedSyncFromPeer(true);
    Assert.assertFalse(fetch.canFetchBlock(first, item, now));
    first.setNeedSyncFromPeer(false);
    first.setNeedSyncFromUs(true);
    Assert.assertFalse(fetch.canFetchBlock(first, item, now));
    first.setNeedSyncFromUs(false);
    when(first.getChannel().isDisconnect()).thenReturn(true);
    Assert.assertFalse(fetch.canFetchBlock(first, item, now));
  }

  @Test
  public void blockFetchRequiresRecentAnnouncement() {
    long now = System.currentTimeMillis();
    Assert.assertFalse(fetch.canFetchBlock(first, item, now));
    first.getAdvInvReceive().put(item, now - NetConstants.ADV_TIME_OUT - 1);
    Assert.assertFalse(fetch.canFetchBlock(first, item, now));
  }

  @Test
  public void queuedBlocksReserveSourcesInHeightOrder() throws Exception {
    Item higher = blockItem(102);
    announce(first, higher);
    announce(first, item);
    pending().put(higher, System.currentTimeMillis());
    pending().put(item, System.currentTimeMillis());

    consume();

    Assert.assertTrue(first.getAdvInvRequest().containsKey(item));
    Assert.assertFalse(first.getAdvInvRequest().containsKey(higher));
    Assert.assertTrue(pending().containsKey(higher));
    Assert.assertFalse(pending().containsKey(item));
  }

  @Test
  public void unavailableEarlierBlockDoesNotPreventLaterFetch() throws Exception {
    Item higher = blockItem(102);
    announce(first, higher);
    pending().put(item, System.currentTimeMillis());
    pending().put(higher, System.currentTimeMillis());

    consume();

    Assert.assertTrue(first.getAdvInvRequest().containsKey(higher));
    Assert.assertTrue(pending().containsKey(item));
  }

  @Test
  public void transactionRequestsStillBatchTogether() throws Exception {
    Item other = new Item(new BlockId(Sha256Hash.ZERO_HASH, 1), InventoryType.TRX);
    announce(first, transaction());
    announce(first, other);
    adv.addInv(transaction());
    adv.addInv(other);

    consume();

    Assert.assertTrue(first.getAdvInvRequest().containsKey(transaction()));
    Assert.assertTrue(first.getAdvInvRequest().containsKey(other));
    verify(first).sendMessage(any(FetchInvDataMessage.class));
  }

  @Test
  public void disconnectReusesPendingBackupAndItsOriginalDeadline() throws Exception {
    beginFetch(first, System.currentTimeMillis() - 2_000);
    long backupTime = System.currentTimeMillis() - 1_000;
    second.getAdvInvRequest().put(item, backupTime);

    disconnect(first);
    consume();

    Assert.assertSame(second, ReflectUtils.getFieldObject(state(), "peer"));
    Assert.assertEquals(backupTime, ReflectUtils.getFieldObject(state(), "time"));
    Assert.assertEquals(Long.valueOf(backupTime), second.getAdvInvRequest().get(item));
    Assert.assertFalse(pending().containsKey(item));
    verify(second, never()).sendMessage(any());
  }

  @Test
  public void backupDisconnectRestoresOriginalPendingRequest() throws Exception {
    beginFetch(first, System.currentTimeMillis() - 2_000);
    long originalTime = first.getAdvInvRequest().get(item);
    announce(second, item);
    tick(state());
    Assert.assertNull(state());

    disconnect(second);

    Assert.assertSame(first, ReflectUtils.getFieldObject(state(), "peer"));
    Assert.assertEquals(originalTime, ReflectUtils.getFieldObject(state(), "time"));
    verify(first, never()).sendMessage(any());
  }

  @Test
  public void disconnectWaitsForBusyAnnouncerWithoutLosingRecovery() throws Exception {
    announce(first, item);
    adv.addInv(item);
    announce(second, item);
    second.getAdvInvRequest().put(blockItem(102), System.currentTimeMillis());

    disconnect(first);
    consume();
    Assert.assertTrue(pending().containsKey(item));
    verify(second, never()).sendMessage(any());
    second.getAdvInvRequest().clear();
    consume();

    verify(second).sendMessage(any(FetchInvDataMessage.class));
    Assert.assertTrue(second.getAdvInvRequest().containsKey(item));
  }

  @Test
  public void disconnectWithoutSourcesReleasesDeduplication() throws Exception {
    announce(first, item);
    adv.addInv(item);

    disconnect(first);
    announce(second, item);

    Assert.assertNull(state());
    Assert.assertTrue(adv.addInv(item));
    verify(second).sendMessage(any(FetchInvDataMessage.class));
  }

  @Test
  public void disconnectClearsTrackingAfterOutstandingRequestWasRemoved() throws Exception {
    beginFetch(first, System.currentTimeMillis());
    first.getAdvInvRequest().clear();

    disconnect(first);

    Assert.assertNull(state());
  }

  @Test
  public void disconnectDoesNotRefetchStoredBlock() throws Exception {
    beginFetch(first, System.currentTimeMillis());
    announce(second, item);
    when(delegate.containBlock(new BlockId(item.getHash()))).thenReturn(true);

    disconnect(first);
    consume();

    Assert.assertNull(state());
    Assert.assertFalse(pending().containsKey(item));
    verify(second, never()).sendMessage(any());
  }

  @Test
  public void disconnectDoesNotRefetchCachedBlock() throws Exception {
    BlockCapsule block = new BlockCapsule(101, new BlockId(Sha256Hash.ZERO_HASH, 100),
        System.currentTimeMillis(), Sha256Hash.ZERO_HASH.getByteString());
    item = new Item(block.getBlockId(), InventoryType.BLOCK);
    beginFetch(first, System.currentTimeMillis());
    announce(second, item);
    adv.broadcast(new BlockMessage(block));

    disconnect(first);
    consume();

    Assert.assertNull(state());
    Assert.assertFalse(pending().containsKey(item));
    Assert.assertFalse(second.getAdvInvRequest().containsKey(item));
    verify(second, never()).sendMessage(any(FetchInvDataMessage.class));
  }

  @Test(timeout = 10_000)
  public void disconnectSnapshotsRequestsWithoutWaitingForFetchExecutor() throws Exception {
    CountDownLatch blocked = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    ScheduledExecutorService executor = fetchExecutor();
    executor.submit(() -> {
      blocked.countDown();
      release.await(5, TimeUnit.SECONDS);
      return null;
    });
    ExecutorService callback = Executors.newSingleThreadExecutor();
    try {
      Assert.assertTrue(blocked.await(5, TimeUnit.SECONDS));
      beginFetch(first, System.currentTimeMillis());
      announce(second, item);
      peers.remove(first);
      synchronized (adv) {
        callback.submit(() -> adv.onDisconnect(first)).get(2, TimeUnit.SECONDS);
      }
      first.getAdvInvRequest().clear();
      Assert.assertTrue(pending().isEmpty());
      release.countDown();
      awaitRecovery();
      Assert.assertTrue(pending().containsKey(item));
      verify(second, never()).sendMessage(any());
      consume();
      verify(second).sendMessage(any(FetchInvDataMessage.class));
    } finally {
      release.countDown();
      callback.shutdownNow();
      Assert.assertTrue(callback.awaitTermination(5, TimeUnit.SECONDS));
    }
  }

  @Test
  public void disconnectAfterServiceShutdownStillClearsPeerRequests() {
    ReflectUtils.setFieldValue(first, "advService", adv);
    ReflectUtils.setFieldValue(first, "syncService", mock(SyncService.class));
    beginFetch(first, System.currentTimeMillis());
    adv.close();

    first.onDisconnect();

    Assert.assertTrue(first.getAdvInvRequest().isEmpty());
  }

  @Test
  public void completedRequestCannotBeRestoredByRecovery() {
    beginFetch(first, System.currentTimeMillis());
    first.getAdvInvRequest().remove(item);
    fetch.blockFetchSuccess(item.getHash());

    fetch.fetchBlock(Collections.singletonList(item.getHash()), first);

    Assert.assertNull(state());
  }

  @Test
  public void staleWorkerCannotClearRecoveredRequest() throws Exception {
    beginFetch(first, System.currentTimeMillis() - 2_000);
    Object oldState = state();
    second.getAdvInvRequest().put(item, System.currentTimeMillis());
    disconnect(first);
    Object recovered = state();

    tick(oldState);

    Assert.assertNotNull(recovered);
    Assert.assertSame(recovered, state());
    verify(second, never()).sendMessage(any());
  }

  @Test
  public void stalePeerSnapshotDoesNotSuppressRecoveredBlock() throws Exception {
    assertStaleSnapshotPreservesRecovery(false);
  }

  @Test
  public void stalePeerSnapshotKeepsRecoveryQueuedForBusySource() throws Exception {
    assertStaleSnapshotPreservesRecovery(true);
  }

  private void assertStaleSnapshotPreservesRecovery(boolean busy) throws Exception {
    announce(first, item);
    Assert.assertTrue(adv.addInv(item));
    announce(second, item);
    Item busyBlock = blockItem(102);
    if (busy) {
      second.getAdvInvRequest().put(busyBlock, System.currentTimeMillis());
    }
    AtomicBoolean disconnected = new AtomicBoolean();
    when(delegate.getActivePeer()).thenAnswer(invocation -> {
      List<PeerConnection> snapshot = new ArrayList<>(peers);
      if (disconnected.compareAndSet(false, true)) {
        // Recovery runs after the snapshot is captured but before the service monitor is held.
        peers.remove(first);
        when(first.getChannel().isDisconnect()).thenReturn(true);
        adv.onDisconnect(first);
        awaitRecovery();
        Assert.assertTrue(pending().containsKey(item));
        Assert.assertTrue(first.getAdvInvRequest().containsKey(item));
        Assert.assertNull(state());
      }
      return snapshot;
    });

    consume();
    // Transport cleanup can finish after recovery and the stale scheduling pass.
    first.getAdvInvRequest().clear();
    if (busy) {
      Assert.assertTrue(pending().containsKey(item));
      Assert.assertFalse(second.getAdvInvRequest().containsKey(item));
      verify(second, never()).sendMessage(any());
      second.getAdvInvRequest().remove(busyBlock);
      consume();
    }

    Assert.assertTrue(second.getAdvInvRequest().containsKey(item));
    Assert.assertFalse(pending().containsKey(item));
    Assert.assertSame(second, ReflectUtils.getFieldObject(state(), "peer"));
    verify(second).sendMessage(any(FetchInvDataMessage.class));
    Assert.assertFalse(adv.addInv(item));
  }

  @Test
  public void disconnectBeforeReservationRollsBackNewRequest() throws Exception {
    assertDisconnectDuringReservation(false, false);
  }

  @Test
  public void disconnectCleanupAfterReservationKeepsBlockQueued() throws Exception {
    assertDisconnectDuringReservation(true, false);
  }

  @Test
  public void disconnectedReservationRollbackPreservesNewerRequest() throws Exception {
    assertDisconnectDuringReservation(false, true);
  }

  private void assertDisconnectDuringReservation(boolean clearAfterReservation,
      boolean replaceReservation) throws Exception {
    announce(first, item);
    announce(second, item);
    AtomicLong replacementTime = new AtomicLong();
    doAnswer(invocation -> {
      // Eligibility passed, but disconnect recovery snapshots before the request is inserted.
      peers.remove(first);
      when(first.getChannel().isDisconnect()).thenReturn(true);
      Assert.assertTrue(first.getAdvInvRequest().isEmpty());
      adv.onDisconnect(first);
      awaitRecovery();
      boolean reserved = (boolean) invocation.callRealMethod();
      Assert.assertTrue(reserved);
      if (clearAfterReservation) {
        first.getAdvInvRequest().clear();
      } else if (replaceReservation) {
        replacementTime.set(invocation.<Long>getArgument(1) + 1);
        first.getAdvInvRequest().put(item, replacementTime.get());
      }
      return reserved;
    }).when(first).checkAndPutAdvInvRequest(eq(item), anyLong());

    Assert.assertTrue(adv.addInv(item));

    Assert.assertTrue(pending().containsKey(item));
    Assert.assertNull(state());
    verify(first, never()).sendMessage(any());
    verify(second, never()).sendMessage(any());
    if (replaceReservation) {
      Assert.assertEquals(Long.valueOf(replacementTime.get()), first.getAdvInvRequest().get(item));
    } else {
      Assert.assertTrue(first.getAdvInvRequest().isEmpty());
    }
    consume();

    Assert.assertTrue(second.getAdvInvRequest().containsKey(item));
    Assert.assertFalse(pending().containsKey(item));
    Assert.assertSame(second, ReflectUtils.getFieldObject(state(), "peer"));
    verify(second).sendMessage(any(FetchInvDataMessage.class));
  }

  @Test
  public void shortTimeoutStillClearsTrackingWithoutAlternative() throws Exception {
    beginFetch(first, System.currentTimeMillis() - 2_000);

    tick(state());

    Assert.assertNull(state());
    Assert.assertTrue(first.getAdvInvRequest().containsKey(item));
  }

  private PeerConnection peer(int port) {
    PeerConnection peer = spy(new PeerConnection());
    Channel channel = mock(Channel.class);
    InetSocketAddress address = new InetSocketAddress("127.0.0.1", port);
    when(channel.getInetSocketAddress()).thenReturn(address);
    when(channel.getInetAddress()).thenReturn(address.getAddress());
    ReflectUtils.setFieldValue(peer, "channel", channel);
    peer.setNeedSyncFromPeer(false);
    peer.setNeedSyncFromUs(false);
    doNothing().when(peer).sendMessage(any());
    return peer;
  }

  private Item blockItem(long height) {
    return new Item(new BlockId(Sha256Hash.ZERO_HASH, height), InventoryType.BLOCK);
  }

  private Item transaction() {
    return new Item(Sha256Hash.ZERO_HASH, InventoryType.TRX);
  }

  private void announce(PeerConnection peer, Item announced) {
    peer.getAdvInvReceive().put(announced, System.currentTimeMillis());
  }

  private void beginFetch(PeerConnection peer, long time) {
    peer.getAdvInvRequest().put(item, time);
    fetch.fetchBlock(Collections.singletonList(item.getHash()), peer);
    Assert.assertNotNull(state());
  }

  private void disconnect(PeerConnection peer) throws Exception {
    peers.remove(peer);
    when(peer.getChannel().isDisconnect()).thenReturn(true);
    adv.onDisconnect(peer);
    peer.getAdvInvRequest().clear();
    awaitRecovery();
  }

  private void awaitRecovery() throws Exception {
    fetchExecutor().submit(() -> { }).get(5, TimeUnit.SECONDS);
  }

  private ScheduledExecutorService fetchExecutor() {
    return ReflectUtils.getFieldValue(adv, "fetchExecutor");
  }

  private Map<Item, Long> pending() {
    return ReflectUtils.getFieldValue(adv, "invToFetch");
  }

  private Object state() {
    return ReflectUtils.getFieldObject(fetch, "fetchBlockInfo");
  }

  private void consume() {
    ReflectUtils.invokeMethod(adv, "consumerInvToFetch");
  }

  private void tick(Object snapshot) throws Exception {
    Method method = FetchBlockService.class.getDeclaredMethod("fetchBlockProcess",
        Class.forName(FetchBlockService.class.getName() + "$FetchBlockInfo"));
    method.setAccessible(true);
    method.invoke(fetch, snapshot);
  }
}
