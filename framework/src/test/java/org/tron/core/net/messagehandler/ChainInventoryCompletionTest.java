package org.tron.core.net.messagehandler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.tron.core.net.PeerSyncTestSupport.blockId;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.tron.common.overlay.message.Message;
import org.tron.common.utils.Pair;
import org.tron.common.utils.ReflectUtils;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.config.Parameter.NetConstants;
import org.tron.core.exception.P2pException;
import org.tron.core.exception.P2pException.TypeEnum;
import org.tron.core.net.PeerSyncTestSupport;
import org.tron.core.net.TronNetDelegate;
import org.tron.core.net.message.adv.InventoryMessage;
import org.tron.core.net.message.sync.ChainInventoryMessage;
import org.tron.core.net.message.sync.SyncBlockChainMessage;
import org.tron.core.net.peer.Item;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.peer.TronState;
import org.tron.core.net.service.adv.AdvService;
import org.tron.core.net.service.sync.SyncService;
import org.tron.protos.Protocol.Inventory.InventoryType;

public class ChainInventoryCompletionTest {

  private ChainInventoryMsgHandler handler;
  private TronNetDelegate delegate;
  private SyncService sync;
  private PeerConnection peer;

  @Before
  public void setUp() throws Exception {
    handler = new ChainInventoryMsgHandler();
    delegate = mock(TronNetDelegate.class);
    sync = mock(SyncService.class);
    ReflectUtils.setFieldValue(handler, "tronNetDelegate", delegate);
    ReflectUtils.setFieldValue(handler, "syncService", sync);
    ReflectUtils.setFieldValue(handler, "syncFetchBatchNum", 100L);
    when(delegate.getBlockLock()).thenReturn(new Object());
    when(delegate.getForkLock()).thenReturn(new Object());
    when(delegate.getHeadBlockId()).thenReturn(blockId(100));
    when(delegate.getKhaosDbHeadBlockId()).thenReturn(blockId(100));
    when(delegate.getSolidBlockId()).thenReturn(blockId(0));
    when(delegate.getGenesisBlockId()).thenReturn(blockId(0));
    when(delegate.getBlockTime(blockId(0))).thenReturn(System.currentTimeMillis() - 30_000_000L);
    when(delegate.containBlock(any())).thenAnswer(invocation ->
        ((BlockId) invocation.getArgument(0)).getNum() <= 100);
    when(delegate.getBlockIdByNum(org.mockito.ArgumentMatchers.anyLong()))
        .thenAnswer(invocation -> blockId(invocation.getArgument(0)));
    peer = PeerSyncTestSupport.peer(10001);
    peer.setTronState(TronState.SYNCING);
    peer.setNeedSyncFromPeer(true);
    peer.setNeedSyncFromUs(false);
    peer.setFetchAble(true);
    peer.setRemainNum(99);
    peer.setBlockRcvTime(7);
    request(blockId(0), blockId(50), blockId(100));
  }

  @Test
  public void testSummaryTailCompletesDownload() throws Exception {
    respond(0, blockId(100));

    assertDownloadCompleted();
    Assert.assertTrue(peer.isSyncFinish());
  }

  @Test
  public void testEarlierSummaryBlockPreservesNoUploadRequirement() throws Exception {
    respond(0, blockId(50));

    assertDownloadCompleted();
    Assert.assertFalse(peer.isNeedSyncFromUs());
    Assert.assertTrue(peer.isSyncFinish());
  }

  @Test
  public void testEarliestSummaryBlockPreservesNoUploadRequirement() throws Exception {
    respond(0, blockId(0));

    assertDownloadCompleted();
    Assert.assertFalse(peer.isNeedSyncFromUs());
    Assert.assertTrue(peer.isSyncFinish());
  }

  @Test
  public void testKnownResponseCompletesDownloadAfterHeadAdvances() throws Exception {
    when(delegate.getHeadBlockId()).thenReturn(blockId(101));

    respond(0, blockId(100));

    assertDownloadCompleted();
    Assert.assertFalse(peer.isNeedSyncFromUs());
  }

  @Test
  public void testTailDoesNotEraseExistingUploadDirection() throws Exception {
    peer.setNeedSyncFromUs(true);

    respond(0, blockId(100));

    assertDownloadCompleted();
    Assert.assertTrue(peer.isNeedSyncFromUs());
    Assert.assertFalse(peer.isSyncFinish());
  }

  @Test
  public void testEarlierSummaryBlockPreservesExistingUploadRequirement() throws Exception {
    peer.setNeedSyncFromUs(true);

    respond(0, blockId(50));

    assertDownloadCompleted();
    Assert.assertTrue(peer.isNeedSyncFromUs());
    Assert.assertFalse(peer.isSyncFinish());
  }

  @Test
  public void testSingleBlockReplyFromExistingPeerKeepsInventoryFlow() throws Exception {
    // The remote peer still believes it is ahead based on Hello. Our summary now includes
    // newer blocks learned from another connection; replying does not start a remote download.
    PeerConnection remotePeer = PeerSyncTestSupport.peer(10002);
    remotePeer.setNeedSyncFromPeer(false);
    remotePeer.setNeedSyncFromUs(true);
    TronNetDelegate remoteDelegate = mock(TronNetDelegate.class);
    when(remoteDelegate.getHeadBlockId()).thenReturn(blockId(50));
    when(remoteDelegate.containBlockInMainChain(any())).thenAnswer(invocation ->
        ((BlockId) invocation.getArgument(0)).getNum() <= 50);
    SyncBlockChainMsgHandler remoteHandler = new SyncBlockChainMsgHandler();
    ReflectUtils.setFieldValue(remoteHandler, "tronNetDelegate", remoteDelegate);

    remoteHandler.processMessage(remotePeer,
        new SyncBlockChainMessage(new ArrayList<>(peer.getSyncChainRequested().getKey())));

    ArgumentCaptor<Message> reply = ArgumentCaptor.forClass(Message.class);
    verify(remotePeer).sendMessage(reply.capture());
    Assert.assertTrue(reply.getValue() instanceof ChainInventoryMessage);
    ChainInventoryMessage response = new ChainInventoryMessage(reply.getValue().getData());
    Assert.assertEquals(Collections.singletonList(blockId(50)), response.getBlockIds());
    Assert.assertEquals(Long.valueOf(0), response.getRemainNum());
    handler.processMessage(peer, response);

    assertDownloadCompleted();
    Assert.assertTrue(peer.isSyncFinish());
    Assert.assertTrue(remotePeer.isSyncFinish());
    verify(remotePeer, never()).disconnect(any());

    InventoryMsgHandler inventoryHandler = new InventoryMsgHandler();
    AdvService adv = mock(AdvService.class);
    ReflectUtils.setFieldValue(inventoryHandler, "tronNetDelegate", delegate);
    ReflectUtils.setFieldValue(inventoryHandler, "advService", adv);
    ReflectUtils.setFieldValue(inventoryHandler, "transactionsMsgHandler",
        mock(TransactionsMsgHandler.class));
    for (InventoryType type : Arrays.asList(InventoryType.BLOCK, InventoryType.TRX)) {
      Item item = new Item(blockId(101), type);
      inventoryHandler.processMessage(peer,
          new InventoryMessage(Collections.singletonList(item.getHash()), type));
      Assert.assertNotNull(peer.getAdvInvReceive().getIfPresent(item));
      verify(adv).addInv(item);
    }
    verify(peer, never()).disconnect(any());
  }

  @Test
  public void testPeerBehindCanStartAnotherDownloadLater() throws Exception {
    respond(0, blockId(50));
    SyncService service = new SyncService();
    ReflectUtils.setFieldValue(service, "tronNetDelegate", delegate);
    try {
      service.startSync(peer);

      Assert.assertEquals(TronState.SYNCING, peer.getTronState());
      Assert.assertTrue(peer.isNeedSyncFromPeer());
      Assert.assertNotNull(peer.getSyncChainRequested());
      Assert.assertEquals(blockId(100), peer.getSyncChainRequested().getKey().peekLast());
      verify(peer).sendMessage(any(SyncBlockChainMessage.class));
      verify(peer, never()).disconnect(any());
    } finally {
      service.close();
    }
  }

  @Test
  public void testUnknownQueuedTailStillRequiresBlockDownload() throws Exception {
    request(blockId(0), blockId(100), blockId(102));
    peer.getSyncBlockToFetch().addAll(Arrays.asList(blockId(101), blockId(102)));

    respond(0, blockId(102));

    Assert.assertTrue(peer.isNeedSyncFromPeer());
    Assert.assertEquals(TronState.SYNCING, peer.getTronState());
    Assert.assertEquals(2, peer.getSyncBlockToFetch().size());
    Assert.assertTrue(peer.isFetchAble());
    Assert.assertNull(peer.getSyncChainRequested());
    Assert.assertEquals(7, peer.getBlockRcvTime());
    verify(sync).setFetchFlag(true);
    verify(sync, never()).syncNext(any());
  }

  @Test
  public void testFinalMultiBlockResponseSchedulesFetch() throws Exception {
    respond(0, blockId(100), blockId(101), blockId(102));

    Assert.assertEquals(Arrays.asList(blockId(101), blockId(102)),
        new ArrayList<>(peer.getSyncBlockToFetch()));
    Assert.assertTrue(peer.isNeedSyncFromPeer());
    Assert.assertEquals(TronState.SYNCING, peer.getTronState());
    Assert.assertEquals(0, peer.getRemainNum());
    Assert.assertEquals(7, peer.getBlockRcvTime());
    verify(sync).setFetchFlag(true);
    verify(sync, never()).syncNext(any());
  }

  @Test
  public void testKnownMultiBlockResponseRequestsNextSummary() throws Exception {
    respond(0, blockId(50), blockId(51), blockId(52));

    Assert.assertTrue(peer.getSyncBlockToFetch().isEmpty());
    Assert.assertEquals(blockId(52), peer.getBlockBothHave());
    Assert.assertTrue(peer.isNeedSyncFromPeer());
    Assert.assertEquals(7, peer.getBlockRcvTime());
    verify(sync).syncNext(peer);
  }

  @Test
  public void testPagedResponsePreservesPendingBlocks() throws Exception {
    List<BlockId> firstPage = range(100, (int) NetConstants.SYNC_FETCH_BATCH_NUM + 1);
    handler.processMessage(peer, new ChainInventoryMessage(firstPage, 2L));
    Assert.assertEquals(NetConstants.SYNC_FETCH_BATCH_NUM, peer.getSyncBlockToFetch().size());
    Assert.assertEquals(2, peer.getRemainNum());
    verify(sync).setFetchFlag(true);

    BlockId tail = firstPage.get(firstPage.size() - 1);
    request(blockId(0), blockId(100), tail);
    respond(0, tail, blockId(tail.getNum() + 1), blockId(tail.getNum() + 2));

    Assert.assertEquals(NetConstants.SYNC_FETCH_BATCH_NUM + 2,
        peer.getSyncBlockToFetch().size());
    Assert.assertEquals(blockId(101), peer.getSyncBlockToFetch().peekFirst());
    Assert.assertEquals(blockId(tail.getNum() + 2), peer.getSyncBlockToFetch().peekLast());
    Assert.assertEquals(0, peer.getRemainNum());
    Assert.assertTrue(peer.isNeedSyncFromPeer());
    Assert.assertEquals(7, peer.getBlockRcvTime());
  }

  @Test
  public void testSingleBlockWithRemainingBlocksIsRejected() throws Exception {
    assertRejected(new ChainInventoryMessage(Arrays.asList(blockId(50)), 1L));
  }

  @Test
  public void testNegativeRemainingCountIsRejectedForFullBatch() throws Exception {
    assertRejected(new ChainInventoryMessage(range(100,
        (int) NetConstants.SYNC_FETCH_BATCH_NUM), -1L));
  }

  @Test
  public void testOverflowingRemainingCountCannotBypassFutureLimit() throws Exception {
    assertRejected(new ChainInventoryMessage(range(100,
        (int) NetConstants.SYNC_FETCH_BATCH_NUM), Long.MAX_VALUE));
  }

  @Test
  public void testFutureRangeIsRejected() throws Exception {
    assertRejected(new ChainInventoryMessage(range(100,
        (int) NetConstants.SYNC_FETCH_BATCH_NUM), 100_000L));
  }

  @Test
  public void testKnownBlockOutsideSummaryIsRejected() throws Exception {
    assertRejected(new ChainInventoryMessage(Arrays.asList(blockId(75)), 0L));
  }

  @Test
  public void testSameHeightDifferentHashIsRejected() throws Exception {
    byte[] hash = new byte[Sha256Hash.LENGTH];
    hash[31] = 1;
    assertRejected(new ChainInventoryMessage(
        Arrays.asList(new BlockId(Sha256Hash.wrap(hash), 100)), 0L));
  }

  @Test
  public void testDiscontinuousResponseIsRejected() throws Exception {
    assertRejected(new ChainInventoryMessage(Arrays.asList(blockId(100), blockId(102)), 0L));
  }

  @Test
  public void testEmptyAndOversizedResponsesAreRejected() throws Exception {
    assertRejected(new ChainInventoryMessage(new ArrayList<>(), 0L));
    assertRejected(new ChainInventoryMessage(range(100,
        (int) NetConstants.SYNC_FETCH_BATCH_NUM + 2), 0L));
  }

  @Test
  public void testUnsolicitedResponseIsRejected() throws Exception {
    peer.setSyncChainRequested(null);
    assertRejected(new ChainInventoryMessage(Arrays.asList(blockId(100)), 0L));
  }

  private void request(BlockId... ids) {
    peer.setSyncChainRequested(new Pair<>(new LinkedList<>(Arrays.asList(ids)),
        System.currentTimeMillis()));
  }

  private void respond(long remain, BlockId... ids) throws Exception {
    handler.processMessage(peer, new ChainInventoryMessage(Arrays.asList(ids), remain));
  }

  private void assertDownloadCompleted() {
    Assert.assertFalse(peer.isNeedSyncFromPeer());
    Assert.assertEquals(TronState.SYNC_COMPLETED, peer.getTronState());
    Assert.assertNull(peer.getSyncChainRequested());
    Assert.assertFalse(peer.isFetchAble());
    Assert.assertEquals(0, peer.getRemainNum());
    Assert.assertEquals(7, peer.getBlockRcvTime());
    verify(sync, never()).syncNext(any());
    verify(sync, never()).setFetchFlag(true);
  }

  private void assertRejected(ChainInventoryMessage message) throws Exception {
    Pair<Deque<BlockId>, Long> requested = peer.getSyncChainRequested();
    try {
      handler.processMessage(peer, message);
      Assert.fail("Invalid chain inventory must be rejected before changing peer state");
    } catch (P2pException e) {
      Assert.assertEquals(TypeEnum.BAD_MESSAGE, e.getType());
    }
    Assert.assertSame(requested, peer.getSyncChainRequested());
    Assert.assertTrue(peer.isNeedSyncFromPeer());
    Assert.assertFalse(peer.isNeedSyncFromUs());
    Assert.assertTrue(peer.isFetchAble());
    Assert.assertEquals(TronState.SYNCING, peer.getTronState());
    Assert.assertEquals(99, peer.getRemainNum());
    Assert.assertEquals(7, peer.getBlockRcvTime());
    verify(sync, never()).syncNext(any());
    verify(sync, never()).setFetchFlag(true);
  }

  private List<BlockId> range(long first, int size) {
    List<BlockId> ids = new ArrayList<>();
    for (int i = 0; i < size; i++) {
      ids.add(blockId(first + i));
    }
    return ids;
  }
}
