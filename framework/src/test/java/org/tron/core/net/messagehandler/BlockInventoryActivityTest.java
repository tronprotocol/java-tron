package org.tron.core.net.messagehandler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.google.common.base.Ticker;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.tron.common.utils.ReflectUtils;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.exception.P2pException;
import org.tron.core.exception.P2pException.TypeEnum;
import org.tron.core.net.PeerBlockTestSupport;
import org.tron.core.net.TronNetDelegate;
import org.tron.core.net.message.adv.BlockMessage;
import org.tron.core.net.message.adv.InventoryMessage;
import org.tron.core.net.peer.Item;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.service.adv.AdvService;
import org.tron.core.net.service.fetchblock.FetchBlockService;
import org.tron.core.net.service.sync.SyncService;
import org.tron.core.services.WitnessProductBlockService;
import org.tron.protos.Protocol.Inventory.InventoryType;

public class BlockInventoryActivityTest {

  private AdvService adv;
  private TronNetDelegate delegate;
  private InventoryMsgHandler inventory;
  private BlockMsgHandler blocks;
  private PeerConnection provider;
  private PeerConnection advertiser;
  private BlockCapsule block;
  private Item item;
  private AtomicReference<BlockId> head;
  private AtomicBoolean accepted;

  @Before
  public void setUp() throws Exception {
    adv = spy(new AdvService());
    delegate = mock(TronNetDelegate.class);
    inventory = new InventoryMsgHandler();
    blocks = new BlockMsgHandler();
    provider = PeerBlockTestSupport.peer(18888);
    advertiser = PeerBlockTestSupport.peer(18889);
    block = PeerBlockTestSupport.block(101);
    item = new Item(block.getBlockId(), InventoryType.BLOCK);
    head = new AtomicReference<>(block.getParentBlockId());
    accepted = new AtomicBoolean();
    provider.getAdvInvRequest().put(item, System.currentTimeMillis());
    when(delegate.getActivePeer()).thenReturn(Arrays.asList(provider, advertiser));
    when(delegate.getHeadBlockId()).thenAnswer(call -> head.get());
    when(delegate.containBlock(block.getParentBlockId())).thenReturn(true);
    when(delegate.containBlock(block.getBlockId())).thenAnswer(call -> accepted.get());
    when(delegate.validBlock(block)).thenReturn(true);
    doAnswer(call -> {
      acceptBlock();
      return null;
    }).when(delegate).processBlock(eq(block), anyBoolean());
    ReflectUtils.setFieldValue(adv, "tronNetDelegate", delegate);
    ReflectUtils.setFieldValue(adv, "fastForward", false);
    // Exercise inventory admission independently of provider selection and background workers.
    doReturn(false).when(adv).addInv(any());
    ReflectUtils.setFieldValue(inventory, "tronNetDelegate", delegate);
    ReflectUtils.setFieldValue(inventory, "advService", adv);
    ReflectUtils.setFieldValue(inventory, "transactionsMsgHandler",
        mock(TransactionsMsgHandler.class));
    ReflectUtils.setFieldValue(blocks, "tronNetDelegate", delegate);
    ReflectUtils.setFieldValue(blocks, "advService", adv);
    ReflectUtils.setFieldValue(blocks, "fetchBlockService", mock(FetchBlockService.class));
    ReflectUtils.setFieldValue(blocks, "syncService", mock(SyncService.class));
    ReflectUtils.setFieldValue(blocks, "witnessProductBlockService",
        mock(WitnessProductBlockService.class));
    ReflectUtils.setFieldValue(blocks, "fastForward", false);
  }

  @After
  public void tearDown() {
    adv.close();
  }

  @Test
  public void testAnnouncementWaitsForSuccessfulBlockProcessing() throws Exception {
    record(100);
    doAnswer(call -> {
      Assert.assertEquals(1, advertiser.getLastInteractiveTime());
      Assert.assertNotNull(adv.getMessage(item));
      acceptBlock();
      return null;
    }).when(delegate).processBlock(block, false);

    processBlock();

    Assert.assertEquals(100, advertiser.getLastInteractiveTime());
    Assert.assertEquals(0, advertiser.getBlockRcvTime());
    Assert.assertTrue(provider.getBlockRcvTime() > 0);
    Assert.assertNull(advertiser.getAdvBlockInvReceive().getIfPresent(item));
  }

  @Test
  public void testInventoryIsRegisteredBeforeFetchCanComplete() throws Exception {
    AtomicLong receivedAt = new AtomicLong();
    doAnswer(call -> {
      Long pending = advertiser.getAdvBlockInvReceive().getIfPresent(item);
      Assert.assertNotNull(pending);
      receivedAt.set(pending);
      Assert.assertEquals(1, advertiser.getLastInteractiveTime());
      processBlock();
      return false;
    }).when(adv).addInv(item);

    inventory.processMessage(advertiser, inventory(item));

    Assert.assertEquals(block.getBlockId(), head.get());
    Assert.assertEquals(receivedAt.get(), advertiser.getLastInteractiveTime());
    Assert.assertNull(advertiser.getAdvBlockInvReceive().getIfPresent(item));
  }

  @Test
  public void testEqualAndLowerInventoryCannotRefreshActivity() throws Exception {
    head.set(block.getBlockId());
    inventory.processMessage(advertiser, inventory(item));
    Item lower = new Item(block.getParentBlockId(), InventoryType.BLOCK);
    inventory.processMessage(advertiser, inventory(lower));

    adv.confirmBlockInventory(block.getBlockId());
    adv.confirmBlockInventory(block.getParentBlockId());

    Assert.assertEquals(0, advertiser.getAdvBlockInvReceive().size());
    Assert.assertEquals(1, advertiser.getLastInteractiveTime());
  }

  @Test
  public void testPreviouslySpreadInventoryCannotRefreshActivity() throws Exception {
    advertiser.getAdvInvSpread().put(item, System.currentTimeMillis());
    inventory.processMessage(advertiser, inventory(item));

    processBlock();

    Assert.assertEquals(0, advertiser.getAdvBlockInvReceive().size());
    Assert.assertEquals(1, advertiser.getLastInteractiveTime());
  }

  @Test
  public void testTransactionInventoryCannotRefreshBlockActivity() throws Exception {
    inventory.processMessage(advertiser, inventory(new Item(item.getHash(), InventoryType.TRX)));

    processBlock();

    Assert.assertEquals(0, advertiser.getAdvBlockInvReceive().size());
    Assert.assertEquals(1, advertiser.getLastInteractiveTime());
  }

  @Test
  public void testSpreadingAfterReceiptDoesNotChangeEligibility() throws Exception {
    record(100);
    advertiser.getAdvInvSpread().put(item, 200L);

    processBlock();

    Assert.assertEquals(100, advertiser.getLastInteractiveTime());
  }

  @Test
  public void testLateDuplicateCannotReplaceEligibleReceiptTime() throws Exception {
    record(100);
    doAnswer(call -> {
      acceptBlock();
      adv.recordInventory(advertiser, item, 200);
      Assert.assertEquals(Long.valueOf(200), advertiser.getAdvInvReceive().getIfPresent(item));
      Assert.assertEquals(Long.valueOf(100), advertiser.getAdvBlockInvReceive().getIfPresent(item));
      return null;
    }).when(delegate).processBlock(block, false);

    processBlock();
    adv.recordInventory(advertiser, item, 300);
    adv.confirmBlockInventory(block.getBlockId());

    Assert.assertEquals(100, advertiser.getLastInteractiveTime());
    Assert.assertNull(advertiser.getAdvBlockInvReceive().getIfPresent(item));
  }

  @Test
  public void testDifferentHashAtSameHeightCannotConfirmInventory() throws Exception {
    record(100);
    BlockId different = new BlockId(Sha256Hash.ZERO_HASH, block.getNum());
    Assert.assertNotEquals(block.getBlockId(), different);

    adv.confirmBlockInventory(different);

    Assert.assertEquals(1, advertiser.getLastInteractiveTime());
    Assert.assertEquals(Long.valueOf(100), advertiser.getAdvBlockInvReceive().getIfPresent(item));
    processBlock();
    Assert.assertEquals(100, advertiser.getLastInteractiveTime());
  }

  @Test
  public void testBadSignatureDoesNotConfirmInventory() throws Exception {
    assertInvalidBlockDoesNotConfirm(TypeEnum.BLOCK_SIGN_INVALID);
  }

  @Test
  public void testBadMerkleDoesNotConfirmInventory() throws Exception {
    assertInvalidBlockDoesNotConfirm(TypeEnum.BLOCK_MERKLE_INVALID);
  }

  @Test
  public void testInactiveWitnessDoesNotConfirmInventory() throws Exception {
    record(100);
    when(delegate.validBlock(block)).thenReturn(false);

    processBlock();

    assertUnconfirmed();
  }

  @Test
  public void testExecutionFailureDoesNotConfirmCachedInventory() throws Exception {
    record(100);
    doThrow(new P2pException(TypeEnum.BAD_BLOCK, "execution failed"))
        .when(delegate).processBlock(block, false);

    processBlock();

    Assert.assertNotNull(adv.getMessage(item));
    assertUnconfirmed();
  }

  @Test
  public void testShutdownDoesNotConfirmInventory() throws Exception {
    record(100);
    when(delegate.isHitDown()).thenReturn(true);

    processBlock();

    assertUnconfirmed();
  }

  @Test
  public void testConfirmationDoesNotOverwriteNewerInteraction() throws Exception {
    record(100);
    advertiser.updateLastInteractiveTime(200);

    processBlock();

    Assert.assertEquals(200, advertiser.getLastInteractiveTime());
    Assert.assertNull(advertiser.getAdvBlockInvReceive().getIfPresent(item));
  }

  @Test
  public void testPendingInventoryEvictionDoesNotAffectBlockFetch() throws Exception {
    record(100);
    Long requestTime = provider.getAdvInvRequest().get(item);
    for (int i = 1; i <= 200; i++) {
      Item another = new Item(new BlockId(block.getBlockId(), block.getNum() + i),
          InventoryType.BLOCK);
      adv.recordInventory(advertiser, another, 200);
    }
    Assert.assertTrue(advertiser.getAdvBlockInvReceive().size() > 0);
    Assert.assertTrue(advertiser.getAdvBlockInvReceive().size() <= 100);
    Assert.assertNull(advertiser.getAdvBlockInvReceive().getIfPresent(item));
    Assert.assertEquals(Long.valueOf(100), advertiser.getAdvInvReceive().getIfPresent(item));
    Assert.assertEquals(requestTime, provider.getAdvInvRequest().get(item));

    processBlock();

    Assert.assertEquals(1, advertiser.getLastInteractiveTime());
    Assert.assertEquals(0, advertiser.getBlockRcvTime());
    Assert.assertTrue(provider.getBlockRcvTime() > 0);
    Assert.assertFalse(provider.getAdvInvRequest().containsKey(item));
  }

  @Test
  public void testPendingInventoryExpiresAfterOneMinuteWithoutExtendingOnRead() throws Exception {
    AtomicLong nanos = new AtomicLong();
    Ticker ticker = new Ticker() {
      @Override
      public long read() {
        return nanos.get();
      }
    };
    try (MockedStatic<Ticker> tickers = mockStatic(Ticker.class)) {
      tickers.when(Ticker::systemTicker).thenReturn(ticker);
      // Construct the production cache with a controlled clock, retaining its actual policy.
      advertiser = PeerBlockTestSupport.peer(18890);
      when(delegate.getActivePeer()).thenReturn(Arrays.asList(provider, advertiser));
      record(100);
      Long requestTime = provider.getAdvInvRequest().get(item);
      nanos.set(TimeUnit.MINUTES.toNanos(1) - 1);
      Assert.assertEquals(Long.valueOf(100), advertiser.getAdvBlockInvReceive().getIfPresent(item));
      nanos.incrementAndGet();
      Assert.assertEquals(Long.valueOf(100), advertiser.getAdvInvReceive().getIfPresent(item));
      Assert.assertEquals(requestTime, provider.getAdvInvRequest().get(item));

      processBlock();

      Assert.assertEquals(1, advertiser.getLastInteractiveTime());
      Assert.assertEquals(0, advertiser.getBlockRcvTime());
      Assert.assertNull(advertiser.getAdvBlockInvReceive().getIfPresent(item));
      Assert.assertTrue(provider.getBlockRcvTime() > 0);
      Assert.assertFalse(provider.getAdvInvRequest().containsKey(item));
    }
  }

  @Test(timeout = 10_000)
  public void testConcurrentConfirmationCannotMissEligibleInventory() throws Exception {
    CountDownLatch headRead = new CountDownLatch(1);
    CountDownLatch finishRegistration = new CountDownLatch(1);
    CountDownLatch confirmationStarted = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    when(delegate.getHeadBlockId()).thenAnswer(call -> {
      BlockId snapshot = head.get();
      headRead.countDown();
      Assert.assertTrue(finishRegistration.await(5, TimeUnit.SECONDS));
      return snapshot;
    });
    try {
      Future<?> registration = executor.submit(() -> adv.recordInventory(advertiser, item, 100));
      Assert.assertTrue(headRead.await(5, TimeUnit.SECONDS));
      Assert.assertNull(advertiser.getAdvInvReceive().getIfPresent(item));
      acceptBlock();
      Future<?> confirmation = executor.submit(() -> {
        confirmationStarted.countDown();
        adv.confirmBlockInventory(block.getBlockId());
      });
      Assert.assertTrue(confirmationStarted.await(5, TimeUnit.SECONDS));
      try {
        confirmation.get(100, TimeUnit.MILLISECONDS);
        Assert.fail("Confirmation must wait for the eligible inventory to be registered");
      } catch (TimeoutException expected) {
        // The receipt observed the old head; it must be registered before confirmation finishes.
      }
      finishRegistration.countDown();
      registration.get(5, TimeUnit.SECONDS);
      confirmation.get(5, TimeUnit.SECONDS);

      Assert.assertEquals(100, advertiser.getLastInteractiveTime());
      Assert.assertEquals(Long.valueOf(100), advertiser.getAdvInvReceive().getIfPresent(item));
      Assert.assertNull(advertiser.getAdvBlockInvReceive().getIfPresent(item));
    } finally {
      finishRegistration.countDown();
      executor.shutdownNow();
      Assert.assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }
  }

  private void record(long receivedAt) {
    adv.recordInventory(advertiser, item, receivedAt);
    Assert.assertEquals(1, advertiser.getLastInteractiveTime());
    Assert.assertEquals(Long.valueOf(receivedAt),
        advertiser.getAdvBlockInvReceive().getIfPresent(item));
  }

  private void acceptBlock() {
    head.set(block.getBlockId());
    accepted.set(true);
  }

  private void processBlock() throws Exception {
    blocks.processMessage(provider, new BlockMessage(block));
  }

  private InventoryMessage inventory(Item value) {
    return new InventoryMessage(Collections.singletonList(value.getHash()), value.getType());
  }

  private void assertInvalidBlockDoesNotConfirm(TypeEnum type) throws Exception {
    record(100);
    when(delegate.validBlock(block)).thenThrow(new P2pException(type, "invalid block"));
    try {
      processBlock();
      Assert.fail("Expected block validation to fail");
    } catch (P2pException e) {
      Assert.assertEquals(type, e.getType());
    }
    assertUnconfirmed();
  }

  private void assertUnconfirmed() {
    Assert.assertEquals(1, advertiser.getLastInteractiveTime());
    Assert.assertEquals(0, advertiser.getBlockRcvTime());
    Assert.assertEquals(Long.valueOf(100), advertiser.getAdvBlockInvReceive().getIfPresent(item));
  }
}
