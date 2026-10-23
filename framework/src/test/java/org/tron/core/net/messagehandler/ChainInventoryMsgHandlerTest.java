package org.tron.core.net.messagehandler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.tron.common.TestConstants;
import org.tron.common.utils.Pair;
import org.tron.common.utils.ReflectUtils;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.config.Parameter.NetConstants;
import org.tron.core.config.args.Args;
import org.tron.core.exception.P2pException;
import org.tron.core.net.TronNetDelegate;
import org.tron.core.net.message.sync.ChainInventoryMessage;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.service.sync.SyncService;

public class ChainInventoryMsgHandlerTest {

  @BeforeClass
  public static void init() {
    Args.setParam(new String[]{}, TestConstants.TEST_CONF);
  }

  @AfterClass
  public static void destroy() {
    Args.clearParam();
  }

  private ChainInventoryMsgHandler handler = new ChainInventoryMsgHandler();
  private PeerConnection peer = new PeerConnection();
  private ChainInventoryMessage msg = new ChainInventoryMessage(new ArrayList<>(), 0L);
  private List<BlockId> blockIds = new ArrayList<>();

  @Test
  public void testQueueChangedDuringProcessing() throws Exception {
    ChainInventoryMsgHandler handler = new ChainInventoryMsgHandler();
    TronNetDelegate delegate = Mockito.mock(TronNetDelegate.class);
    SyncService syncService = Mockito.mock(SyncService.class);
    ReflectUtils.setFieldValue(handler, "tronNetDelegate", delegate);
    ReflectUtils.setFieldValue(handler, "syncService", syncService);

    BlockId parent = new BlockId(Sha256Hash.ZERO_HASH, 0);
    BlockId next = new BlockId(Sha256Hash.ZERO_HASH, 1);
    PeerConnection peer = Mockito.mock(PeerConnection.class);
    Deque<BlockId> toFetch = Mockito.mock(Deque.class);
    LinkedList<BlockId> requested = new LinkedList<>();
    requested.add(parent);
    Mockito.when(peer.getSyncChainRequested())
        .thenReturn(new Pair<>(requested, System.currentTimeMillis()));
    Mockito.when(peer.getSyncBlockToFetch()).thenReturn(toFetch);
    Mockito.when(toFetch.peek()).thenReturn(next, (BlockId) null);
    Mockito.when(toFetch.isEmpty()).thenReturn(false, true);
    Mockito.when(toFetch.peekLast()).thenReturn(null);
    Mockito.when(delegate.getHeadBlockId()).thenReturn(parent);
    Mockito.when(delegate.getBlockLock()).thenReturn(new Object());
    Mockito.when(delegate.containBlock(next)).thenReturn(true);
    Mockito.when(toFetch.remove(next)).thenReturn(false);

    handler.processMessage(peer, new ChainInventoryMessage(
        java.util.Arrays.asList(parent, next), 0L));

    Mockito.verify(toFetch).pollLast();
    Mockito.verify(peer, Mockito.never()).setBlockBothHave(Mockito.any());
  }

  @Test
  public void testProcessMessage() throws Exception {
    try {
      handler.processMessage(peer, msg);
    } catch (P2pException e) {
      Assert.assertEquals("not send syncBlockChainMsg", e.getMessage());
    }

    peer.setSyncChainRequested(new Pair<>(new LinkedList<>(), System.currentTimeMillis()));

    try {
      handler.processMessage(peer, msg);
    } catch (P2pException e) {
      Assert.assertEquals("blockIds is empty", e.getMessage());
    }

    long size = NetConstants.SYNC_FETCH_BATCH_NUM + 2;
    for (int i = 0; i < size; i++) {
      blockIds.add(new BlockId());
    }
    msg = new ChainInventoryMessage(blockIds, 0L);

    try {
      handler.processMessage(peer, msg);
    } catch (P2pException e) {
      Assert.assertEquals(e.getMessage(), "big blockIds size: " + size);
    }

    blockIds.clear();
    size = NetConstants.SYNC_FETCH_BATCH_NUM / 100;
    for (int i = 0; i < size; i++) {
      blockIds.add(new BlockId());
    }
    msg = new ChainInventoryMessage(blockIds, 100L);

    try {
      handler.processMessage(peer, msg);
    } catch (P2pException e) {
      Assert.assertEquals(e.getMessage(), "remain: 100, blockIds size: " + size);
    }
    Assert.assertNotNull(msg.toString());
    Assert.assertNull(msg.getAnswerMessage());
  }

  @Test
  public void testFetchFlagDecisionIsMadeUnderBlockLock() throws Exception {
    Object blockLock = new Object();
    TronNetDelegate tronNetDelegate = Mockito.mock(TronNetDelegate.class);
    SyncService syncService = Mockito.mock(SyncService.class);
    PeerConnection testPeer = Mockito.spy(new PeerConnection());
    AtomicBoolean fetchAbleSetUnderLock = new AtomicBoolean();
    AtomicBoolean fetchFlagSetUnderLock = new AtomicBoolean();
    BlockId firstBlock = new BlockId(Sha256Hash.ZERO_HASH, 1);
    BlockId secondBlock = new BlockId(Sha256Hash.ZERO_HASH, 2);

    Mockito.when(tronNetDelegate.getBlockLock()).thenReturn(blockLock);
    Mockito.when(tronNetDelegate.getHeadBlockId()).thenReturn(new BlockId(Sha256Hash.ZERO_HASH, 0));
    Mockito.when(tronNetDelegate.containBlock(Mockito.any())).thenReturn(false);
    Mockito.doAnswer(invocation -> {
      if (invocation.getArgument(0)) {
        fetchAbleSetUnderLock.set(Thread.holdsLock(blockLock));
      }
      return invocation.callRealMethod();
    }).when(testPeer).setFetchAble(Mockito.anyBoolean());
    Mockito.doAnswer(invocation -> {
      fetchFlagSetUnderLock.set(Thread.holdsLock(blockLock));
      return null;
    }).when(syncService).setFetchFlag(true);
    ReflectUtils.setFieldValue(handler, "tronNetDelegate", tronNetDelegate);
    ReflectUtils.setFieldValue(handler, "syncService", syncService);
    testPeer.setSyncChainRequested(new Pair<>(new LinkedList<>(Arrays.asList(firstBlock)),
        System.currentTimeMillis()));

    handler.processMessage(testPeer,
        new ChainInventoryMessage(Arrays.asList(firstBlock, secondBlock), 0L));

    Assert.assertTrue(fetchAbleSetUnderLock.get());
    Assert.assertTrue(fetchFlagSetUnderLock.get());
  }

  @Test
  public void testSyncNextDecisionIsMadeUnderBlockLock() throws Exception {
    Object blockLock = new Object();
    TronNetDelegate tronNetDelegate = Mockito.mock(TronNetDelegate.class);
    SyncService syncService = Mockito.mock(SyncService.class);
    PeerConnection testPeer = Mockito.spy(new PeerConnection());
    AtomicBoolean fetchAbleSetUnderLock = new AtomicBoolean();
    AtomicBoolean syncNextCalledUnderLock = new AtomicBoolean();
    BlockId firstBlock = new BlockId(Sha256Hash.ZERO_HASH, 1);

    Mockito.when(tronNetDelegate.getBlockLock()).thenReturn(blockLock);
    Mockito.when(tronNetDelegate.getHeadBlockId()).thenReturn(new BlockId(Sha256Hash.ZERO_HASH, 0));
    Mockito.when(tronNetDelegate.containBlock(Mockito.any())).thenReturn(false);
    Mockito.doAnswer(invocation -> {
      if (invocation.getArgument(0)) {
        fetchAbleSetUnderLock.set(Thread.holdsLock(blockLock));
      }
      return invocation.callRealMethod();
    }).when(testPeer).setFetchAble(Mockito.anyBoolean());
    Mockito.doAnswer(invocation -> {
      syncNextCalledUnderLock.set(Thread.holdsLock(blockLock));
      return null;
    }).when(syncService).syncNext(testPeer);
    ReflectUtils.setFieldValue(handler, "tronNetDelegate", tronNetDelegate);
    ReflectUtils.setFieldValue(handler, "syncService", syncService);
    testPeer.setSyncChainRequested(new Pair<>(new LinkedList<>(Arrays.asList(firstBlock)),
        System.currentTimeMillis()));

    handler.processMessage(testPeer, new ChainInventoryMessage(Arrays.asList(firstBlock), 0L));

    Assert.assertTrue(fetchAbleSetUnderLock.get());
    Assert.assertTrue(syncNextCalledUnderLock.get());
  }

  @Test
  public void testNegativeRemainNumRejected() throws Exception {
    assertCheckRejects(createContinuousBlockIds(0L), -1L);
  }

  @Test
  public void testRemainNumOverflowRejected() throws Exception {
    assertCheckRejects(createContinuousBlockIds(
        Long.MAX_VALUE - NetConstants.SYNC_FETCH_BATCH_NUM + 1), 1L);
  }

  private void assertCheckRejects(List<BlockId> ids, long remainNum) throws Exception {
    ChainInventoryMsgHandler messageHandler = new ChainInventoryMsgHandler();
    TronNetDelegate tronNetDelegate = Mockito.mock(TronNetDelegate.class);
    ReflectUtils.setFieldValue(messageHandler, "tronNetDelegate", tronNetDelegate);
    Mockito.when(tronNetDelegate.getHeadBlockId()).thenReturn(
        new BlockId(Sha256Hash.ZERO_HASH, 1L));
    Mockito.when(tronNetDelegate.getSolidBlockId()).thenReturn(
        new BlockId(Sha256Hash.ZERO_HASH, 1L));
    Mockito.when(tronNetDelegate.getBlockTime(Mockito.any())).thenReturn(0L);

    PeerConnection connection = Mockito.mock(PeerConnection.class);
    LinkedList<BlockId> requestedIds = new LinkedList<>();
    requestedIds.add(ids.get(0));
    Mockito.when(connection.getSyncChainRequested()).thenReturn(
        new Pair<>(requestedIds, System.currentTimeMillis()));

    Method check = ChainInventoryMsgHandler.class.getDeclaredMethod(
        "check", PeerConnection.class, ChainInventoryMessage.class);
    check.setAccessible(true);
    try {
      check.invoke(messageHandler, connection, new ChainInventoryMessage(ids, remainNum));
      Assert.fail("Expected invalid remainNum to be rejected");
    } catch (InvocationTargetException e) {
      Assert.assertTrue(e.getCause() instanceof P2pException);
      Assert.assertEquals(P2pException.TypeEnum.BAD_MESSAGE,
          ((P2pException) e.getCause()).getType());
    }
  }

  private List<BlockId> createContinuousBlockIds(long firstNum) {
    List<BlockId> ids = new ArrayList<>();
    for (int i = 0; i < NetConstants.SYNC_FETCH_BATCH_NUM; i++) {
      ids.add(new BlockId(Sha256Hash.ZERO_HASH, firstNum + i));
    }
    return ids;
  }

}
