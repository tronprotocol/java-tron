package org.tron.core.net.messagehandler;

import java.util.ArrayList;
import java.util.Arrays;
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
import org.tron.core.net.message.keepalive.PingMessage;
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

}
