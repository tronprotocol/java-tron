package org.tron.core.net.messagehandler;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
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

}
