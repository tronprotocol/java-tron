package org.tron.core.net.messagehandler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.protobuf.ByteString;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ReflectUtils;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.db.Manager;
import org.tron.core.exception.P2pException;
import org.tron.core.exception.P2pException.TypeEnum;
import org.tron.core.net.P2pEventHandlerImpl;
import org.tron.core.net.PeerBlockTestSupport;
import org.tron.core.net.TronNetDelegate;
import org.tron.core.net.message.TronMessage;
import org.tron.core.net.message.adv.BlockMessage;
import org.tron.core.net.peer.Item;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.service.adv.AdvService;
import org.tron.core.net.service.fetchblock.FetchBlockService;
import org.tron.core.net.service.sync.SyncService;
import org.tron.core.services.WitnessProductBlockService;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.store.WitnessScheduleStore;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Inventory.InventoryType;

public class BlockContributionTest {

  private BlockMsgHandler handler;
  private TronNetDelegate delegate;
  private FetchBlockService fetch;
  private AdvService adv;
  private SyncService sync;
  private PeerConnection provider;
  private BlockCapsule block;
  private Item item;
  private long requestTime;

  @Before
  public void setUp() throws Exception {
    handler = new BlockMsgHandler();
    delegate = mock(TronNetDelegate.class);
    fetch = mock(FetchBlockService.class);
    adv = mock(AdvService.class);
    sync = mock(SyncService.class);
    ReflectUtils.setFieldValue(handler, "tronNetDelegate", delegate);
    ReflectUtils.setFieldValue(handler, "fetchBlockService", fetch);
    ReflectUtils.setFieldValue(handler, "advService", adv);
    ReflectUtils.setFieldValue(handler, "syncService", sync);
    ReflectUtils.setFieldValue(handler, "witnessProductBlockService",
        mock(WitnessProductBlockService.class));
    ReflectUtils.setFieldValue(handler, "fastForward", false);
    provider = PeerBlockTestSupport.peer(18888);
    block = PeerBlockTestSupport.block(85636071);
    item = new Item(block.getBlockId(), InventoryType.BLOCK);
    requestTime = System.currentTimeMillis() - 1_000;
    provider.getAdvInvRequest().put(item, requestTime);
    when(delegate.getHeadBlockId()).thenReturn(block.getParentBlockId());
    when(delegate.getActivePeer()).thenReturn(Collections.singletonList(provider));
    when(delegate.containBlock(block.getParentBlockId())).thenReturn(true);
    when(delegate.validBlock(block)).thenReturn(true);
  }

  @Test
  public void testValidationFetchBroadcastAndExecutionOrder() throws Exception {
    when(delegate.validBlock(block)).thenAnswer(call -> {
      Assert.assertEquals(Long.valueOf(requestTime), provider.getAdvInvRequest().get(item));
      verify(fetch, never()).blockFetchSuccess(any());
      return true;
    });
    doAnswer(call -> {
      verify(fetch).blockFetchSuccess(block.getBlockId());
      Assert.assertFalse(provider.getAdvInvRequest().containsKey(item));
      Assert.assertTrue(provider.getLastInteractiveTime() > 1);
      Assert.assertEquals(0, provider.getBlockRcvTime());
      verify(delegate, never()).processBlock(any(), eq(false));
      return null;
    }).when(adv).broadcast(any(BlockMessage.class));
    doAnswer(call -> {
      verify(adv).broadcast(any(BlockMessage.class));
      Assert.assertEquals(0, provider.getBlockRcvTime());
      return null;
    }).when(delegate).processBlock(block, false);

    handler.processMessage(provider, new BlockMessage(block));

    Assert.assertTrue(provider.getBlockRcvTime() > 0);
  }

  @Test
  public void testOnlyProviderGetsContribution() throws Exception {
    PeerConnection advertiser = PeerBlockTestSupport.peer(18889);
    advertiser.getAdvInvReceive().put(item, System.currentTimeMillis());
    when(delegate.getActivePeer()).thenReturn(Arrays.asList(provider, advertiser));

    handler.processMessage(provider, new BlockMessage(block));

    Assert.assertTrue(provider.getBlockRcvTime() > 0);
    Assert.assertEquals(0, advertiser.getBlockRcvTime());
  }

  @Test
  public void testBadMerkleRetainsRequestAndDoesNotCompleteFetch() throws Exception {
    assertInvalid(TypeEnum.BLOCK_MERKLE_INVALID);
  }

  @Test
  public void testBadSignatureRetainsRequestAndDoesNotCompleteFetch() throws Exception {
    assertInvalid(TypeEnum.BLOCK_SIGN_INVALID);
  }

  @Test
  public void testInactiveWitnessDoesNotImproveEitherTimestamp() throws Exception {
    when(delegate.validBlock(block)).thenReturn(false);

    handler.processMessage(provider, new BlockMessage(block));

    Assert.assertEquals(1, provider.getLastInteractiveTime());
    Assert.assertEquals(0, provider.getBlockRcvTime());
    Assert.assertFalse(provider.getAdvInvRequest().containsKey(item));
    verify(sync).startSync(provider);
    verify(adv, never()).broadcast(any());
    verify(provider, never()).disconnect(any());
  }

  @Test
  public void testMissingParentCreditsValidatedProviderAndStartsSync() throws Exception {
    when(delegate.containBlock(block.getParentBlockId())).thenReturn(false);

    handler.processMessage(provider, new BlockMessage(block));

    Assert.assertTrue(provider.getBlockRcvTime() > 0);
    verify(sync).startSync(provider);
    verify(adv, never()).broadcast(any());
    verify(delegate, never()).processBlock(any(), eq(false));
  }

  @Test
  public void testOldOrphanDoesNotGetContribution() throws Exception {
    when(delegate.getHeadBlockId()).thenReturn(new BlockId(block.getBlockId(), block.getNum() + 1));
    when(delegate.containBlock(block.getParentBlockId())).thenReturn(false);

    handler.processMessage(provider, new BlockMessage(block));

    Assert.assertEquals(0, provider.getBlockRcvTime());
    verify(sync, never()).startSync(any());
    verify(adv, never()).broadcast(any());
  }

  @Test
  public void testAlreadyKnownBlockDoesNotGetContribution() throws Exception {
    when(delegate.containBlock(block.getBlockId())).thenReturn(true);

    handler.processMessage(provider, new BlockMessage(block));

    Assert.assertEquals(0, provider.getBlockRcvTime());
    verify(adv, never()).broadcast(any());
  }

  @Test
  public void testExecutionFailureStartsSyncWithoutBlamingProvider() throws Exception {
    doThrow(new P2pException(TypeEnum.BAD_BLOCK, "local execution state"))
        .when(delegate).processBlock(block, false);

    handler.processMessage(provider, new BlockMessage(block));

    verify(adv).broadcast(any(BlockMessage.class));
    verify(sync).startSync(provider);
    verify(provider, never()).disconnect(any());
    Assert.assertTrue(provider.getLastInteractiveTime() > 1);
    Assert.assertEquals(0, provider.getBlockRcvTime());
  }

  @Test
  public void testShutdownDoesNotGetContribution() throws Exception {
    when(delegate.isHitDown()).thenReturn(true);

    handler.processMessage(provider, new BlockMessage(block));

    Assert.assertEquals(0, provider.getBlockRcvTime());
  }

  @Test
  public void testUnrequestedBlockCannotImproveTimestamps() throws Exception {
    provider.getAdvInvRequest().clear();
    try {
      handler.processMessage(provider, new BlockMessage(block));
      Assert.fail("Expected an unrequested block to be rejected");
    } catch (P2pException e) {
      Assert.assertEquals(TypeEnum.BAD_MESSAGE, e.getType());
    }
    verify(delegate, never()).validBlock(any());
    Assert.assertEquals(1, provider.getLastInteractiveTime());
    Assert.assertEquals(0, provider.getBlockRcvTime());
  }

  @Test
  public void testInvalidParentHeightCannotCompleteFetch() throws Exception {
    BlockCapsule invalid = new BlockCapsule(block.getInstance().toBuilder()
        .setBlockHeader(block.getInstance().getBlockHeader().toBuilder()
            .setRawData(block.getInstance().getBlockHeader().getRawData().toBuilder()
                .setNumber(block.getNum() + 1))).build());
    Item invalidItem = new Item(invalid.getBlockId(), InventoryType.BLOCK);
    provider.getAdvInvRequest().put(invalidItem, requestTime);
    try {
      handler.processMessage(provider, new BlockMessage(invalid));
      Assert.fail("Expected invalid parent height to be rejected");
    } catch (P2pException e) {
      Assert.assertEquals(TypeEnum.BAD_BLOCK, e.getType());
    }
    Assert.assertEquals(Long.valueOf(requestTime), provider.getAdvInvRequest().get(invalidItem));
    verify(fetch, never()).blockFetchSuccess(any());
    verify(adv, never()).broadcast(any());
    Assert.assertEquals(1, provider.getLastInteractiveTime());
  }

  @Test
  public void testDispatcherDoesNotUnconditionallyCreditBlocks() throws Exception {
    Method update = P2pEventHandlerImpl.class.getDeclaredMethod("updateLastInteractiveTime",
        PeerConnection.class, TronMessage.class);
    update.setAccessible(true);

    update.invoke(new P2pEventHandlerImpl(), provider, new BlockMessage(block));

    Assert.assertEquals(1, provider.getLastInteractiveTime());
  }

  private void assertInvalid(TypeEnum type) throws Exception {
    when(delegate.validBlock(block)).thenThrow(new P2pException(type, "invalid data"));
    try {
      handler.processMessage(provider, new BlockMessage(block));
      Assert.fail("Expected invalid block to be rejected");
    } catch (P2pException e) {
      Assert.assertEquals(type, e.getType());
    }
    Assert.assertEquals(Long.valueOf(requestTime), provider.getAdvInvRequest().get(item));
    Assert.assertEquals(1, provider.getLastInteractiveTime());
    Assert.assertEquals(0, provider.getBlockRcvTime());
    verify(fetch, never()).blockFetchSuccess(any());
    verify(adv, never()).broadcast(any());
  }

  @Test(timeout = 20_000)
  public void testRealSignedBlockPassesValidationBeforeContribution() throws Exception {
    useRealValidation();
    handler.processMessage(provider, new BlockMessage(block));
    Assert.assertTrue(provider.getBlockRcvTime() > 0);
    Assert.assertFalse(provider.getAdvInvRequest().containsKey(item));
    verify(fetch).blockFetchSuccess(block.getBlockId());
  }

  @Test(timeout = 20_000)
  public void testRealBadSignatureRetainsRequest() throws Exception {
    useRealValidation();
    block = new BlockCapsule(block.getInstance().toBuilder()
        .setBlockHeader(block.getInstance().getBlockHeader().toBuilder()
            .setWitnessSignature(ByteString.copyFrom(new byte[65]))).build());
    assertRealValidationFailure(TypeEnum.BLOCK_SIGN_INVALID);
  }

  @Test(timeout = 20_000)
  public void testRealTamperedBodyRetainsRequest() throws Exception {
    useRealValidation();
    block = new BlockCapsule(block.getInstance().toBuilder()
        .addTransactions(Protocol.Transaction.newBuilder()
            .setRawData(Protocol.Transaction.raw.newBuilder()
                .setData(ByteString.copyFromUtf8("tampered body")))).build());
    assertRealValidationFailure(TypeEnum.BLOCK_MERKLE_INVALID);
  }

  private void useRealValidation() throws Exception {
    ECKey key = new ECKey();
    block = new BlockCapsule(block.getNum(), block.getParentBlockId(), block.getTimeStamp(),
        ByteString.copyFrom(key.getAddress()));
    block.setMerkleRoot();
    block.sign(key.getPrivKeyBytes());
    item = new Item(block.getBlockId(), InventoryType.BLOCK);
    provider.getAdvInvRequest().clear();
    provider.getAdvInvRequest().put(item, requestTime);

    TronNetDelegate validator = new TronNetDelegate();
    Manager manager = mock(Manager.class);
    when(manager.getDynamicPropertiesStore()).thenReturn(mock(DynamicPropertiesStore.class));
    WitnessScheduleStore witnesses = mock(WitnessScheduleStore.class);
    when(witnesses.getActiveWitnesses())
        .thenReturn(Collections.singletonList(block.getWitnessAddress()));
    ReflectUtils.setFieldValue(validator, "dbManager", manager);
    ReflectUtils.setFieldValue(validator, "witnessScheduleStore", witnesses);
    when(delegate.validBlock(any())).thenAnswer(call -> validator.validBlock(call.getArgument(0)));
  }

  private void assertRealValidationFailure(TypeEnum type) throws Exception {
    Assert.assertEquals(item.getHash(), block.getBlockId());
    try {
      handler.processMessage(provider, new BlockMessage(block));
      Assert.fail("Expected cryptographic validation to reject the block");
    } catch (P2pException e) {
      Assert.assertEquals(type, e.getType());
    }
    Assert.assertEquals(Long.valueOf(requestTime), provider.getAdvInvRequest().get(item));
    Assert.assertEquals(1, provider.getLastInteractiveTime());
    Assert.assertEquals(0, provider.getBlockRcvTime());
    verify(fetch, never()).blockFetchSuccess(any());
  }
}
