package org.tron.core.net.services;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.cache.Cache;
import com.google.protobuf.ByteString;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tron.common.TestConstants;
import org.tron.common.utils.ReflectUtils;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.config.args.Args;
import org.tron.core.exception.BadBlockException;
import org.tron.core.exception.P2pException;
import org.tron.core.exception.P2pException.TypeEnum;
import org.tron.core.exception.ValidateScheduleException;
import org.tron.core.net.P2pEventHandlerImpl;
import org.tron.core.net.TronNetDelegate;
import org.tron.core.net.message.TronMessage;
import org.tron.core.net.message.adv.BlockMessage;
import org.tron.core.net.message.adv.FetchInvDataMessage;
import org.tron.core.net.messagehandler.BlockMsgHandler;
import org.tron.core.net.peer.Item;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.service.adv.AdvService;
import org.tron.core.net.service.fetchblock.FetchBlockService;
import org.tron.core.net.service.sync.SyncService;
import org.tron.p2p.connection.Channel;
import org.tron.protos.Protocol.Inventory.InventoryType;
import org.tron.protos.Protocol.ReasonCode;

public class BlockFetchFailureTest {

  @ClassRule
  public static final TemporaryFolder temporaryFolder = new TemporaryFolder();

  @BeforeClass
  public static void init() throws Exception {
    Args.setParam(new String[]{"-d", temporaryFolder.newFolder().toString()},
        TestConstants.TEST_CONF);
  }

  @AfterClass
  public static void destroy() {
    Args.clearParam();
  }

  @Test
  public void invalidAdvertisedSignatureDoesNotRetryAnotherPeer() throws Exception {
    assertAdvertisedBlockDoesNotRetry(TypeEnum.BLOCK_SIGN_INVALID);
  }

  @Test
  public void invalidAdvertisedMerkleRootDoesNotRetryAnotherPeer() throws Exception {
    assertAdvertisedBlockDoesNotRetry(TypeEnum.BLOCK_MERKLE_INVALID);
  }

  @Test
  public void inactiveAdvertisedWitnessIsBannedWithoutRetryingAnotherPeer() throws Exception {
    assertAdvertisedBlockDoesNotRetry(TypeEnum.BAD_BLOCK);
  }

  @Test
  public void advertisedScheduleFailureDoesNotDisconnectRelayingPeers() throws Exception {
    TronNetDelegate delegate = mock(TronNetDelegate.class);
    FetchBlockService fetch = mock(FetchBlockService.class);
    AdvService adv = new AdvService();
    ReflectUtils.setFieldValue(adv, "tronNetDelegate", delegate);
    ReflectUtils.setFieldValue(adv, "fetchBlockService", fetch);
    BlockMsgHandler handler = new BlockMsgHandler();
    ReflectUtils.setFieldValue(handler, "tronNetDelegate", delegate);
    ReflectUtils.setFieldValue(handler, "fetchBlockService", fetch);
    ReflectUtils.setFieldValue(handler, "advService", adv);
    PeerConnection sender = newPeer(10001);
    PeerConnection other = newPeer(10002);
    BlockCapsule block = newBlock();
    Item item = new Item(block.getBlockId(), InventoryType.BLOCK);
    when(delegate.getHeadBlockId()).thenReturn(new BlockId(Sha256Hash.ZERO_HASH, 0));
    when(delegate.containBlock(block.getParentBlockId())).thenReturn(true);
    when(delegate.getActivePeer()).thenReturn(Collections.singletonList(sender));
    sender.getAdvInvReceive().put(item, System.currentTimeMillis());
    other.getAdvInvReceive().put(item, System.currentTimeMillis());
    P2pException failure = new P2pException(TypeEnum.BAD_BLOCK,
        new ValidateScheduleException("wrong slot"));
    doThrow(failure).when(delegate).processBlock(any(BlockCapsule.class), anyBoolean());
    try {
      Assert.assertTrue(adv.addInv(item));
      when(delegate.getActivePeer()).thenReturn(Arrays.asList(sender, other));
      handler.processMessage(sender, new BlockMessage(block));
      verify(delegate).processBlock(block, false);
      verify(sender, never()).disconnect(any());
      Assert.assertFalse(sender.getAdvInvRequest().containsKey(item));
      verify(other, never()).disconnect(any());
      Assert.assertNotEquals(block.getBlockId(), other.getBlockBothHave());
      Assert.assertFalse(other.getAdvInvRequest().containsKey(item));
      verify(other, never()).sendMessage(any(FetchInvDataMessage.class));
    } finally {
      adv.close();
    }
  }

  private void assertAdvertisedBlockDoesNotRetry(TypeEnum type) throws Exception {
    TronNetDelegate delegate = mock(TronNetDelegate.class);
    FetchBlockService fetch = mock(FetchBlockService.class);
    AdvService adv = new AdvService();
    ReflectUtils.setFieldValue(adv, "tronNetDelegate", delegate);
    ReflectUtils.setFieldValue(adv, "fetchBlockService", fetch);
    BlockMsgHandler handler = new BlockMsgHandler();
    ReflectUtils.setFieldValue(handler, "tronNetDelegate", delegate);
    ReflectUtils.setFieldValue(handler, "fetchBlockService", fetch);
    PeerConnection sender = newPeer(10001);
    PeerConnection other = newPeer(10002);
    BlockCapsule block = newBlock();
    Item item = new Item(block.getBlockId(), InventoryType.BLOCK);
    when(delegate.getHeadBlockId()).thenReturn(new BlockId(Sha256Hash.ZERO_HASH, 0));
    when(delegate.getActivePeer()).thenReturn(Collections.singletonList(sender));
    sender.getAdvInvReceive().put(item, System.currentTimeMillis());
    other.getAdvInvReceive().put(item, System.currentTimeMillis());
    P2pException failure = new P2pException(type, "invalid block");
    doThrow(failure).when(delegate).validBlock(any(BlockCapsule.class));
    try {
      Assert.assertTrue(adv.addInv(item));
      Assert.assertTrue(sender.getAdvInvRequest().containsKey(item));
      try {
        handler.processMessage(sender, new BlockMessage(block));
        Assert.fail("Invalid block must be rejected");
      } catch (P2pException e) {
        Assert.assertSame(failure, e);
        Method exceptionHandler = P2pEventHandlerImpl.class.getDeclaredMethod(
            "processException", PeerConnection.class, TronMessage.class, Exception.class);
        exceptionHandler.setAccessible(true);
        exceptionHandler.invoke(new P2pEventHandlerImpl(), sender, new BlockMessage(block), e);
        verify(sender).disconnect(ReasonCode.BAD_BLOCK);
      }
      Assert.assertFalse("The failed advertisement must not be retried on disconnect",
          sender.getAdvInvRequest().containsKey(item));
      when(delegate.getActivePeer()).thenReturn(Collections.singletonList(other));
      adv.onDisconnect(sender);
      Assert.assertFalse(other.getAdvInvRequest().containsKey(item));
      verify(other, never()).sendMessage(any(FetchInvDataMessage.class));
      verify(other, never()).disconnect(any());
      Assert.assertFalse("Leave recovery to sync instead of accepting the same INV again",
          adv.addInv(item));
    } finally {
      adv.close();
    }
  }

  @Test
  public void scheduleFailureReleasesSyncRequestWithoutDisconnectingOtherPeers() throws Exception {
    assertSyncFailure(new P2pException(TypeEnum.BAD_BLOCK,
        new ValidateScheduleException("validateWitnessSchedule error")), ReasonCode.BAD_BLOCK);
  }

  @Test
  public void invalidSignatureReleasesSyncRequestWithoutDisconnectingOtherPeers() throws Exception {
    assertSyncFailure(new P2pException(TypeEnum.BLOCK_SIGN_INVALID, "signature"),
        ReasonCode.BAD_BLOCK);
  }

  @Test
  public void invalidMerkleRootReleasesSyncRequestAndPreservesOtherPeers() throws Exception {
    assertSyncFailure(new P2pException(TypeEnum.BAD_BLOCK, new BadBlockException(
            BadBlockException.TypeEnum.CALC_MERKLE_ROOT_FAILED, "merkle")),
        ReasonCode.BAD_BLOCK);
  }

  @Test
  public void invalidBlockReleasesSyncRequestWithoutDisconnectingOtherPeers() throws Exception {
    assertSyncFailure(new P2pException(TypeEnum.BAD_BLOCK, "invalid transaction"),
        ReasonCode.BAD_BLOCK);
  }

  @Test
  public void localFailureReleasesSyncRequestAndDisconnectsOnlySender() throws Exception {
    assertSyncFailure(new IllegalStateException("local processing failure"), ReasonCode.BAD_BLOCK);
  }

  @Test
  public void unlinkedBlockReleasesSyncRequestAndDisconnectsOnlySender() throws Exception {
    assertSyncFailure(new P2pException(TypeEnum.UNLINK_BLOCK, "missing parent"),
        ReasonCode.BAD_BLOCK);
  }

  @Test
  public void syncResponseDoesNotCheckCurrentActiveWitnesses() throws Exception {
    BlockMsgHandler handler = new BlockMsgHandler();
    TronNetDelegate delegate = mock(TronNetDelegate.class);
    SyncService sync = mock(SyncService.class);
    ReflectUtils.setFieldValue(handler, "tronNetDelegate", delegate);
    ReflectUtils.setFieldValue(handler, "syncService", sync);
    PeerConnection sender = newPeer(10001);
    BlockMessage message = new BlockMessage(newBlock());
    sender.getSyncBlockRequested().put(message.getBlockId(), System.currentTimeMillis());

    handler.processMessage(sender, message);

    verify(delegate, never()).validBlock(any());
    verify(sync).processBlock(sender, message);
    Assert.assertFalse(sender.getSyncBlockRequested().containsKey(message.getBlockId()));
    Assert.assertTrue(sender.getSyncBlockInProcess().contains(message.getBlockId()));
  }

  @SuppressWarnings("unchecked")
  private void assertSyncFailure(Exception failure, ReasonCode reason) throws Exception {
    SyncService sync = new SyncService();
    TronNetDelegate delegate = mock(TronNetDelegate.class);
    ReflectUtils.setFieldValue(sync, "tronNetDelegate", delegate);
    PeerConnection sender = newPeer(10001);
    PeerConnection other = newPeer(10002);
    BlockCapsule block = newBlock();
    BlockId id = block.getBlockId();
    BlockId previous = new BlockId(Sha256Hash.ZERO_HASH, 0);
    sender.getSyncBlockToFetch().add(id);
    other.getSyncBlockToFetch().add(id);
    sender.setBlockBothHave(previous);
    other.setBlockBothHave(previous);
    sender.getSyncBlockInProcess().add(id);
    other.setFetchAble(true);
    when(delegate.getActivePeer()).thenReturn(Arrays.asList(sender, other));
    when(delegate.getBlockLock()).thenReturn(new Object());
    when(delegate.getSolidBlockId()).thenReturn(previous);
    if (failure instanceof P2pException
        && ((P2pException) failure).getType() == TypeEnum.BLOCK_SIGN_INVALID) {
      doThrow(failure).when(delegate).validSignature(any(BlockCapsule.class));
    } else {
      doThrow(failure).when(delegate).processBlock(any(BlockCapsule.class), anyBoolean());
    }
    Cache<BlockId, PeerConnection> requests =
        (Cache<BlockId, PeerConnection>) ReflectUtils.getFieldObject(sync, "requestBlockIds");
    requests.put(id, sender);
    try {
      sync.processBlock(sender, new BlockMessage(block));
      invoke(sync, "handleSyncBlock");
      sync.onDisconnect(sender);
      Assert.assertNull("A failed response must release the global request",
          requests.getIfPresent(id));
      Assert.assertEquals(previous, other.getBlockBothHave());
      Assert.assertEquals(id, other.getSyncBlockToFetch().peek());
      Assert.assertFalse(sender.getSyncBlockInProcess().contains(id));
      verify(sender).disconnect(reason);
      verify(other, never()).disconnect(any());
      when(delegate.getActivePeer()).thenReturn(Collections.singletonList(other));
      invoke(sync, "startFetchSyncBlock");
      Assert.assertSame(other, requests.getIfPresent(id));
      Assert.assertTrue(other.getSyncBlockRequested().containsKey(id));
      verify(other).sendMessage(any(FetchInvDataMessage.class));
    } finally {
      sync.close();
    }
  }

  private static BlockCapsule newBlock() {
    return new BlockCapsule(1, Sha256Hash.ZERO_HASH, 0, ByteString.EMPTY);
  }

  private static PeerConnection newPeer(int port) {
    PeerConnection peer = spy(new PeerConnection());
    Channel channel = mock(Channel.class);
    InetSocketAddress address = new InetSocketAddress("127.0.0.1", port);
    when(channel.getInetSocketAddress()).thenReturn(address);
    when(channel.getInetAddress()).thenReturn(address.getAddress());
    peer.setChannel(channel);
    doNothing().when(peer).sendMessage(any());
    doNothing().when(peer).disconnect(any());
    return peer;
  }

  private static void invoke(SyncService service, String name) throws Exception {
    Method method = SyncService.class.getDeclaredMethod(name);
    method.setAccessible(true);
    method.invoke(service);
  }
}
