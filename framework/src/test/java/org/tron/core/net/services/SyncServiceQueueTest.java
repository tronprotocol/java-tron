package org.tron.core.net.services;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.tron.common.overlay.message.Message;
import org.tron.common.utils.ReflectUtils;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.net.TronNetDelegate;
import org.tron.core.net.message.adv.BlockMessage;
import org.tron.core.net.messagehandler.PbftDataSyncHandler;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.service.sync.SyncService;
import org.tron.core.net.service.sync.UnparsedBlock;
import org.tron.protos.Protocol;

public class SyncServiceQueueTest {

  @Test
  public void receivedBlockReplacementKeepsBytesAndPeerTogether() throws Exception {
    assertReplacementKeepsBytesAndPeerTogether(false);
  }

  @Test
  public void waitingBlockReplacementKeepsBytesAndPeerTogether() throws Exception {
    assertReplacementKeepsBytesAndPeerTogether(true);
  }

  private void assertReplacementKeepsBytesAndPeerTogether(boolean waiting) throws Exception {
    SyncService sync = new SyncService();
    try (MockedStatic<Message> messages = mockStatic(Message.class, CALLS_REAL_METHODS)) {
      messages.when(Message::isFilter).thenReturn(false);
      TronNetDelegate delegate = mock(TronNetDelegate.class);
      when(delegate.getActivePeer()).thenReturn(Collections.emptyList());
      when(delegate.getSolidBlockId()).thenReturn(new BlockId(Sha256Hash.ZERO_HASH, 0));
      when(delegate.getBlockLock()).thenReturn(new Object());
      doCallRealMethod().when(delegate).validBlock(any(BlockCapsule.class));
      ReflectUtils.setFieldValue(sync, "tronNetDelegate", delegate);
      ReflectUtils.setFieldValue(sync, "pbftDataSyncHandler", mock(PbftDataSyncHandler.class));
      PeerConnection first = mock(PeerConnection.class);
      PeerConnection second = mock(PeerConnection.class);
      BlockCapsule latest = new BlockCapsule(Protocol.Block.newBuilder()
          .setBlockHeader(Protocol.BlockHeader.newBuilder()
              .setRawData(Protocol.BlockHeader.raw.newBuilder().setNumber(1)
                  .setTxTrieRoot(Sha256Hash.ZERO_HASH.getByteString())))
          .build());
      BlockCapsule stale = new BlockCapsule(latest.getInstance().toBuilder()
          .addTransactions(Protocol.Transaction.getDefaultInstance()).build());
      assertEquals(stale.getBlockId(), latest.getBlockId());

      sync.processBlock(first, new BlockMessage(stale));
      if (waiting) {
        ReflectUtils.invokeMethod(sync, "handleSyncBlock");
      }
      sync.processBlock(second, new BlockMessage(latest));
      if (waiting) {
        ReflectUtils.invokeMethod(sync, "handleSyncBlock");
      }

      Map<UnparsedBlock, PeerConnection> queue =
          (Map<UnparsedBlock, PeerConnection>) ReflectUtils.getFieldObject(sync,
              waiting ? "blockWaitToProcess" : "blockJustReceived");
      assertEquals(1, queue.size());
      UnparsedBlock queued = queue.keySet().iterator().next();
      assertSame(second, queue.get(queued));
      assertArrayEquals(latest.getData(), queued.getData());

      BlockId next = new BlockId(Sha256Hash.ZERO_HASH, 2);
      when(second.getSyncBlockToFetch()).thenReturn(
          new ConcurrentLinkedDeque<>(Arrays.asList(latest.getBlockId(), next)));
      when(second.getSyncBlockInProcess()).thenReturn(ConcurrentHashMap.newKeySet());
      when(delegate.getActivePeer()).thenReturn(Collections.singletonList(second));
      ReflectUtils.invokeMethod(sync, "handleSyncBlock");

      ArgumentCaptor<BlockCapsule> processed = ArgumentCaptor.forClass(BlockCapsule.class);
      verify(delegate).processBlock(processed.capture(), eq(true));
      assertArrayEquals(latest.getData(), processed.getValue().getData());
      verify(second).setBlockBothHave(latest.getBlockId());
      verify(second, never()).disconnect(any(Protocol.ReasonCode.class));
    } finally {
      sync.close();
    }
  }
}
