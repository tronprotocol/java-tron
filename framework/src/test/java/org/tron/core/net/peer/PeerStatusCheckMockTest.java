package org.tron.core.net.peer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.tron.core.net.PeerSyncTestSupport.blockId;

import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.tron.common.utils.Pair;
import org.tron.common.utils.ReflectUtils;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.config.Parameter.NetConstants;
import org.tron.core.net.P2pEventHandlerImpl;
import org.tron.core.net.PeerSyncTestSupport;
import org.tron.core.net.TronNetDelegate;
import org.tron.core.net.message.TronMessage;
import org.tron.core.net.message.keepalive.PingMessage;
import org.tron.core.net.message.keepalive.PongMessage;
import org.tron.core.net.message.sync.ChainInventoryMessage;
import org.tron.core.net.message.sync.SyncBlockChainMessage;
import org.tron.core.net.messagehandler.ChainInventoryMsgHandler;
import org.tron.core.net.messagehandler.SyncBlockChainMsgHandler;
import org.tron.core.net.service.keepalive.KeepAliveService;
import org.tron.core.net.service.sync.SyncService;
import org.tron.protos.Protocol.Inventory.InventoryType;
import org.tron.protos.Protocol.ReasonCode;

public class PeerStatusCheckMockTest {

  private PeerStatusCheck check;
  private TronNetDelegate delegate;
  private PeerConnection peer;

  @Before
  public void setUp() throws Exception {
    check = new PeerStatusCheck();
    delegate = mock(TronNetDelegate.class);
    ReflectUtils.setFieldValue(check, "tronNetDelegate", delegate);
    peer = PeerSyncTestSupport.peer(11001);
    peer.setNeedSyncFromPeer(false);
    peer.setNeedSyncFromUs(false);
    peer.setLastInteractiveTime(1);
    when(delegate.getActivePeer()).thenReturn(Collections.singletonList(peer));
    when(delegate.getHeadBlockId()).thenReturn(blockId(20));
    when(delegate.getKhaosDbHeadBlockId()).thenReturn(blockId(20));
    when(delegate.getSolidBlockId()).thenReturn(blockId(0));
    when(delegate.getBlockTime(blockId(0))).thenReturn(System.currentTimeMillis() - 60_000);
    when(delegate.getForkLock()).thenReturn(new Object());
    when(delegate.getBlockIdByNum(anyLong()))
        .thenAnswer(invocation -> blockId(invocation.getArgument(0)));
    request(peer, System.currentTimeMillis() - 6_000);
  }

  @After
  public void tearDown() {
    try {
      check.close();
    } finally {
      Mockito.framework().clearInlineMocks();
    }
  }

  @Test
  public void testInitException() {
    PeerStatusCheck peerStatusCheck = spy(check);
    ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
    ReflectUtils.setFieldValue(peerStatusCheck, "peerStatusCheckExecutor", executor);
    doThrow(new RuntimeException("test exception")).when(peerStatusCheck).statusCheck();

    peerStatusCheck.init();

    Mockito.verify(executor).scheduleWithFixedDelay(any(Runnable.class), eq(5L), eq(2L),
        eq(TimeUnit.SECONDS));
    Runnable scheduledTask = Mockito.mockingDetails(executor).getInvocations().stream()
        .filter(invocation -> invocation.getMethod().getName().equals("scheduleWithFixedDelay"))
        .map(invocation -> (Runnable) invocation.getArgument(0))
        .findFirst()
        .orElseThrow(() -> new AssertionError("scheduled task was not registered"));

    scheduledTask.run();

    Mockito.verify(peerStatusCheck).statusCheck();
  }

  @Test
  public void testExpiredSummaryDisconnectsOnlyResponsiblePeer() {
    PeerConnection other = PeerSyncTestSupport.peer(11002);
    other.setNeedSyncFromPeer(false);
    other.setNeedSyncFromUs(false);
    request(other, System.currentTimeMillis());
    when(delegate.getActivePeer()).thenReturn(Arrays.asList(peer, other));

    check.statusCheck();

    verify(peer).disconnect(ReasonCode.TIME_OUT);
    verify(other, never()).disconnect(any());
  }

  @Test
  public void testRecentSyncProgressAndInteractionDoNotExtendDeadline() {
    Pair<Deque<BlockId>, Long> requested = peer.getSyncChainRequested();
    peer.setNeedSyncFromPeer(true);
    peer.setBlockBothHave(blockId(20));
    peer.setLastInteractiveTime(System.currentTimeMillis());
    peer.setBlockRcvTime(System.currentTimeMillis());

    check.statusCheck();

    Assert.assertSame(requested, peer.getSyncChainRequested());
    verify(peer).disconnect(ReasonCode.TIME_OUT);
  }

  @Test
  public void testPingAndPongDoNotExtendDeadline() {
    Pair<Deque<BlockId>, Long> requested = peer.getSyncChainRequested();
    P2pEventHandlerImpl events = new P2pEventHandlerImpl();
    ReflectUtils.setFieldValue(events, "keepAliveService", new KeepAliveService());

    dispatch(events, new PingMessage());
    dispatch(events, new PongMessage());

    verify(peer).sendMessage(any(PongMessage.class));
    verify(peer, never()).disconnect(any());
    Assert.assertSame(requested, peer.getSyncChainRequested());
    Assert.assertEquals(1, peer.getLastInteractiveTime());
    check.statusCheck();
    verify(peer).disconnect(ReasonCode.TIME_OUT);
  }

  @Test
  public void testValidIncomingSyncRequestDoesNotExtendOutgoingDeadline() throws Exception {
    Pair<Deque<BlockId>, Long> requested = peer.getSyncChainRequested();
    SyncBlockChainMsgHandler handler = new SyncBlockChainMsgHandler();
    ReflectUtils.setFieldValue(handler, "tronNetDelegate", delegate);
    when(delegate.containBlockInMainChain(any())).thenReturn(true);
    P2pEventHandlerImpl events = new P2pEventHandlerImpl();
    ReflectUtils.setFieldValue(events, "syncBlockChainMsgHandler", handler);

    dispatch(events, new SyncBlockChainMessage(
        new LinkedList<>(Arrays.asList(blockId(0), blockId(20)))));

    verify(peer).sendMessage(any(ChainInventoryMessage.class));
    verify(peer, never()).disconnect(any());
    Assert.assertTrue(peer.getLastInteractiveTime() > 1);
    Assert.assertSame(requested, peer.getSyncChainRequested());
    check.statusCheck();
    verify(peer).disconnect(ReasonCode.TIME_OUT);
  }

  @Test
  public void testRequestWithinFiveSecondsDoesNotTimeOut() {
    request(peer, System.currentTimeMillis() - 1_000);

    check.statusCheck();

    verify(peer, never()).disconnect(any());
  }

  @Test
  public void testCompletedResponseClearsDeadlineWithoutContribution() throws Exception {
    ChainInventoryMsgHandler handler = new ChainInventoryMsgHandler();
    ReflectUtils.setFieldValue(handler, "tronNetDelegate", delegate);
    ReflectUtils.setFieldValue(handler, "syncService", mock(SyncService.class));
    when(delegate.containBlock(blockId(20))).thenReturn(true);
    peer.setNeedSyncFromPeer(true);
    peer.setBlockRcvTime(7);

    handler.processMessage(peer, new ChainInventoryMessage(Collections.singletonList(blockId(20)),
        0L));
    check.statusCheck();

    Assert.assertNull(peer.getSyncChainRequested());
    Assert.assertTrue(peer.isSyncFinish());
    Assert.assertEquals(7, peer.getBlockRcvTime());
    verify(peer, never()).disconnect(any());
  }

  @Test
  public void testNewRequestUsesItsOwnDeadline() {
    peer.setSyncChainRequested(null);
    check.statusCheck();
    request(peer, System.currentTimeMillis());
    check.statusCheck();

    verify(peer, never()).disconnect(any());
  }

  @Test
  public void testRepeatedSyncNextDoesNotReplacePendingRequest() {
    SyncService service = new SyncService();
    ReflectUtils.setFieldValue(service, "tronNetDelegate", delegate);
    peer.setSyncChainRequested(null);
    try {
      service.syncNext(peer);
      Pair<Deque<BlockId>, Long> requested = peer.getSyncChainRequested();
      Assert.assertNotNull(requested);
      Assert.assertEquals(blockId(20), requested.getKey().peekLast());

      service.syncNext(peer);

      Assert.assertSame(requested, peer.getSyncChainRequested());
      verify(peer, times(1)).sendMessage(any(SyncBlockChainMessage.class));
      verify(peer, never()).disconnect(any());
    } finally {
      service.close();
    }
  }

  @Test
  public void testExistingInventoryAndSyncBlockTimeoutsRemainEffective() {
    peer.setSyncChainRequested(null);
    peer.getAdvInvRequest().put(new Item(blockId(21), InventoryType.BLOCK),
        System.currentTimeMillis() - NetConstants.ADV_TIME_OUT - 1_000);
    PeerConnection other = PeerSyncTestSupport.peer(11002);
    other.setNeedSyncFromPeer(false);
    other.getSyncBlockRequested().put(blockId(21), System.currentTimeMillis() - 6_000);
    when(delegate.getActivePeer()).thenReturn(Arrays.asList(peer, other));

    check.statusCheck();

    verify(peer).disconnect(ReasonCode.TIME_OUT);
    verify(other).disconnect(ReasonCode.TIME_OUT);
  }

  private void request(PeerConnection target, long time) {
    target.setSyncChainRequested(new Pair<>(
        new LinkedList<>(Arrays.asList(blockId(0), blockId(20))), time));
  }

  private void dispatch(P2pEventHandlerImpl events, TronMessage message) {
    ReflectUtils.invokeMethod(events, "processMessage",
        new Class<?>[]{PeerConnection.class, byte[].class}, peer, message.getSendBytes());
  }
}
