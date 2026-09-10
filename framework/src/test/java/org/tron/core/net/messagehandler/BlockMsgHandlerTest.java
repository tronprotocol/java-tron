package org.tron.core.net.messagehandler;

import com.google.common.cache.CacheBuilder;
import com.google.protobuf.ByteString;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.tron.common.utils.ReflectUtils;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.Constant;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.config.Parameter;
import org.tron.core.exception.P2pException;
import org.tron.core.net.TronNetDelegate;
import org.tron.core.net.message.adv.BlockMessage;
import org.tron.core.net.peer.Item;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.service.adv.AdvService;
import org.tron.core.net.service.fetchblock.FetchBlockService;
import org.tron.core.net.service.sync.SyncService;
import org.tron.core.services.WitnessProductBlockService;
import org.tron.protos.Protocol.Inventory.InventoryType;
import org.tron.protos.Protocol.Transaction;

public class BlockMsgHandlerTest {
  private BlockMsgHandler handler;
  private PeerConnection peer;
  private TronNetDelegate delegate;
  private AdvService advService;
  private SyncService syncService;
  private FetchBlockService fetchService;
  private WitnessProductBlockService witnessService;

  @Before
  public void before() {
    // Each test owns its handler and collaborators; no Spring singleton is modified.
    handler = new BlockMsgHandler();
    peer = Mockito.mock(PeerConnection.class);
    delegate = Mockito.mock(TronNetDelegate.class);
    advService = Mockito.mock(AdvService.class);
    syncService = Mockito.mock(SyncService.class);
    fetchService = Mockito.mock(FetchBlockService.class);
    witnessService = Mockito.mock(WitnessProductBlockService.class);
    ReflectUtils.setFieldValue(handler, "tronNetDelegate", delegate);
    ReflectUtils.setFieldValue(handler, "advService", advService);
    ReflectUtils.setFieldValue(handler, "syncService", syncService);
    ReflectUtils.setFieldValue(handler, "fetchBlockService", fetchService);
    ReflectUtils.setFieldValue(handler, "witnessProductBlockService", witnessService);
    ReflectUtils.setFieldValue(handler, "fastForward", false);
    Mockito.when(peer.getAdvInvRequest()).thenReturn(new ConcurrentHashMap<>());
    Mockito.when(peer.getSyncBlockRequested()).thenReturn(new ConcurrentHashMap<>());
    Mockito.when(peer.getSyncBlockInProcess()).thenReturn(new HashSet<>());
    Mockito.when(peer.getAdvInvReceive()).thenReturn(CacheBuilder.newBuilder().build());
    Mockito.when(peer.getInetAddress()).thenReturn(InetAddress.getLoopbackAddress());
    Mockito.when(peer.getInetSocketAddress()).thenReturn(
        new InetSocketAddress(InetAddress.getLoopbackAddress(), 100));
  }

  @Test
  public void testUnrequestedBlock() {
    BlockMessage msg = new BlockMessage(block(1, 1));
    assertRejected(msg, "no request");
  }

  @Test
  public void testOversizedBlock() {
    Transaction trx = Transaction.newBuilder().setRawData(Transaction.raw.newBuilder()
        .setData(ByteString.copyFrom(new byte[Parameter.ChainConstant.BLOCK_SIZE
            + Constant.ONE_THOUSAND]))).build();
    BlockCapsule block = new BlockCapsule(1, Sha256Hash.ZERO_HASH.getByteString(), 1,
        Collections.singletonList(trx));
    assertRejected(new BlockMessage(block), "block size over limit");
  }

  @Test
  public void testFutureBlock() {
    assertRejected(new BlockMessage(block(1, Long.MAX_VALUE)), "block time error");
  }

  @Test
  public void testSyncBlock() throws Exception {
    BlockMessage msg = new BlockMessage(block(1, 1));
    peer.getSyncBlockRequested().put(msg.getBlockId(), 1L);
    handler.processMessage(peer, msg);
    Assert.assertTrue(peer.getSyncBlockRequested().isEmpty());
    Assert.assertTrue(peer.getSyncBlockInProcess().contains(msg.getBlockId()));
    Mockito.verify(syncService).processBlock(peer, msg);
    Mockito.verifyNoInteractions(delegate, advService, fetchService, witnessService);
  }

  @Test
  public void testAdvertisedBlock() throws Exception {
    BlockCapsule block = block(1, 1);
    BlockMessage msg = new BlockMessage(block);
    peer.getAdvInvRequest().put(new Item(msg.getBlockId(), InventoryType.BLOCK), 1L);
    stubValidBlock(block);
    handler.processMessage(peer, msg);
    Assert.assertTrue(peer.getAdvInvRequest().isEmpty());
    Mockito.verify(fetchService).blockFetchSuccess(msg.getBlockId());
    Mockito.verify(delegate).processBlock(block, false);
    Mockito.verify(advService).broadcast(Mockito.any(BlockMessage.class));
    Mockito.verify(witnessService).validWitnessProductTwoBlock(block);
  }

  @Test
  public void testProcessBlock() throws Exception {
    BlockCapsule block = block(1, 1);
    stubValidBlock(block);
    peer.getAdvInvReceive().put(new Item(block.getBlockId(), InventoryType.BLOCK), 1L);
    Method method = BlockMsgHandler.class.getDeclaredMethod("processBlock",
        PeerConnection.class, BlockCapsule.class);
    method.setAccessible(true);
    method.invoke(handler, peer, block);
    Mockito.verify(delegate).processBlock(block, false);
    Mockito.verify(peer).setBlockBothHave(block.getBlockId());
    Mockito.verify(witnessService).validWitnessProductTwoBlock(block);
  }

  private void assertRejected(BlockMessage msg, String reason) {
    P2pException failure = Assert.assertThrows(P2pException.class,
        () -> handler.processMessage(peer, msg));
    Assert.assertEquals(P2pException.TypeEnum.BAD_MESSAGE, failure.getType());
    Assert.assertEquals(reason, failure.getMessage());
    Mockito.verifyNoInteractions(delegate, advService, syncService, fetchService, witnessService);
  }

  private void stubValidBlock(BlockCapsule block) throws Exception {
    Mockito.when(delegate.validBlock(block)).thenReturn(true);
    Mockito.when(delegate.containBlock(Mockito.any(BlockId.class))).thenReturn(true);
    Mockito.when(delegate.getHeadBlockId()).thenReturn(block.getBlockId());
    Mockito.when(delegate.getActivePeer()).thenReturn(Collections.singletonList(peer));
  }

  private BlockCapsule block(long number, long timestamp) {
    BlockCapsule block = new BlockCapsule(number, Sha256Hash.ZERO_HASH, timestamp,
        Sha256Hash.ZERO_HASH.getByteString());
    block.setMerkleRoot();
    return block;
  }
}
