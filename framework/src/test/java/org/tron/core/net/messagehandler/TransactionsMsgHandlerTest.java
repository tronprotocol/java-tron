package org.tron.core.net.messagehandler;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.tron.common.BaseTest;
import org.tron.common.TestConstants;
import org.tron.common.runtime.TvmTestUtils;
import org.tron.common.utils.ByteArray;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.exception.P2pException;
import org.tron.core.exception.P2pException.TypeEnum;
import org.tron.core.net.TronNetDelegate;
import org.tron.core.net.message.adv.TransactionMessage;
import org.tron.core.net.message.adv.TransactionsMessage;
import org.tron.core.net.peer.Item;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.service.adv.AdvService;
import org.tron.protos.Protocol;
import org.tron.protos.contract.BalanceContract;

public class TransactionsMsgHandlerTest extends BaseTest {
  @BeforeClass
  public static void init() {
    Args.setParam(new String[]{"--output-directory", dbPath(), "--debug"},
        TestConstants.TEST_CONF);

  }

  @Test
  public void testProcessMessage() {
    TransactionsMsgHandler transactionsMsgHandler = new TransactionsMsgHandler();
    ExecutorService originalPool = null;
    try {
      transactionsMsgHandler.init();

      PeerConnection peer = Mockito.mock(PeerConnection.class);
      TronNetDelegate tronNetDelegate = Mockito.mock(TronNetDelegate.class);
      AdvService advService = Mockito.mock(AdvService.class);

      Field field = TransactionsMsgHandler.class.getDeclaredField("tronNetDelegate");
      field.setAccessible(true);
      field.set(transactionsMsgHandler, tronNetDelegate);

      Assert.assertFalse(transactionsMsgHandler.isBusy());

      BalanceContract.TransferContract transferContract = BalanceContract.TransferContract
          .newBuilder()
          .setAmount(10)
          .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString("121212a9cf")))
          .setToAddress(ByteString.copyFrom(ByteArray.fromHexString("232323a9cf"))).build();

      long transactionTimestamp = ZonedDateTime.now().minusDays(4).toInstant().toEpochMilli();
      Protocol.Transaction trx = Protocol.Transaction.newBuilder().setRawData(
          Protocol.Transaction.raw.newBuilder().setTimestamp(transactionTimestamp)
          .setRefBlockNum(1)
          .addContract(
              Protocol.Transaction.Contract.newBuilder()
                  .setType(Protocol.Transaction.Contract.ContractType.TransferContract)
                  .setParameter(Any.pack(transferContract)).build()).build())
          .build();
      Map<Item, Long> advInvRequest = new ConcurrentHashMap<>();
      Item item = new Item(new TransactionMessage(trx).getMessageId(),
          Protocol.Inventory.InventoryType.TRX);
      advInvRequest.put(item, 0L);
      // The non-executing pool must be installed before the first submission so no
      // real-pool worker can touch the peer mock while the test re-stubs it (Mockito
      // stubbing is not thread-safe). The latch counts down only for off-thread callers,
      // which after the replacement is exactly the smart-contract scheduler.
      CountDownLatch smartContractSubmitted = new CountDownLatch(1);
      Thread testThread = Thread.currentThread();
      ExecutorService mockPool = Mockito.mock(ExecutorService.class);
      Future<?> submittedTask = Mockito.mock(Future.class);
      Mockito.when(mockPool.submit(Mockito.any(Runnable.class))).thenAnswer(invocation -> {
        if (Thread.currentThread() != testThread) {
          smartContractSubmitted.countDown();
        }
        return submittedTask;
      });
      originalPool = replaceTrxHandlePool(transactionsMsgHandler, mockPool);

      Mockito.when(peer.getAdvInvRequest()).thenReturn(advInvRequest);

      List<Protocol.Transaction> transactionList = new ArrayList<>();
      transactionList.add(trx);
      transactionsMsgHandler.processMessage(peer, new TransactionsMessage(transactionList));
      Assert.assertNull(advInvRequest.get(item));
      BlockingQueue<?> smartContractQueue = new LinkedBlockingQueue<>(1);
      Field field1 = TransactionsMsgHandler.class.getDeclaredField("smartContractQueue");
      field1.setAccessible(true);
      field1.set(transactionsMsgHandler, smartContractQueue);
      Protocol.Transaction trx1 = TvmTestUtils.generateTriggerSmartContractAndGetTransaction(
          ByteArray.fromHexString("121212a9cf"),
          ByteArray.fromHexString("121212a9cf"),
          ByteArray.fromHexString("123456"),
          100, 100000000, 0, 0);
      Protocol.Transaction trx3 = TvmTestUtils.generateTriggerSmartContractAndGetTransaction(
          ByteArray.fromHexString("121212a9cf"),
          ByteArray.fromHexString("121212a9cf"),
          ByteArray.fromHexString("123457"),
          100, 100000000, 0, 0);
      Map<Item, Long> advInvRequest1 = new ConcurrentHashMap<>();
      Item item1 = new Item(new TransactionMessage(trx1).getMessageId(),
          Protocol.Inventory.InventoryType.TRX);
      advInvRequest1.put(item1, 0L);
      Item item3 = new Item(new TransactionMessage(trx3).getMessageId(),
          Protocol.Inventory.InventoryType.TRX);
      advInvRequest1.put(item3, 0L);
      Mockito.when(peer.getAdvInvRequest()).thenReturn(advInvRequest1);
      List<Protocol.Transaction> transactionList1 = new ArrayList<>();
      transactionList1.add(trx1);
      transactionList1.add(trx3);
      transactionsMsgHandler.processMessage(peer, new TransactionsMessage(transactionList1));
      Assert.assertNull(advInvRequest1.get(item1));
      Assert.assertNull(advInvRequest1.get(item3));
      Assert.assertTrue("smart-contract scheduler did not submit work",
          smartContractSubmitted.await(3, TimeUnit.SECONDS));

      // test 0 contract
      Protocol.Transaction trx2 = Protocol.Transaction.newBuilder().setRawData(
          Protocol.Transaction.raw.newBuilder().setTimestamp(transactionTimestamp)
              .setRefBlockNum(1).build())
          .build();
      List<Protocol.Transaction> transactionList2 = new ArrayList<>();
      transactionList2.add(trx2);
      try {
        transactionsMsgHandler.processMessage(peer, new TransactionsMessage(transactionList2));
      } catch (Exception ep) {
        Assert.assertTrue(true);
      }
      Map<Item, Long> advInvRequest2 = new ConcurrentHashMap<>();
      Item item2 = new Item(new TransactionMessage(trx2).getMessageId(),
          Protocol.Inventory.InventoryType.TRX);
      advInvRequest2.put(item2, 0L);
      Mockito.when(peer.getAdvInvRequest()).thenReturn(advInvRequest2);
      try {
        transactionsMsgHandler.processMessage(peer, new TransactionsMessage(transactionList2));
      } catch (Exception ep) {
        Assert.assertTrue(true);
      }
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    } finally {
      closeHandlerAndOriginalPool(transactionsMsgHandler, originalPool);
    }
  }

  @Test
  public void testProcessMessageAfterClose() throws Exception {
    TransactionsMsgHandler handler = new TransactionsMsgHandler();
    try {
      handler.init();
      handler.close();

      PeerConnection peer = Mockito.mock(PeerConnection.class);
      TransactionsMessage msg = Mockito.mock(TransactionsMessage.class);

      handler.processMessage(peer, msg);

      Mockito.verify(msg, Mockito.never()).getTransactions();
      Mockito.verifyNoInteractions(peer);
    } finally {
      handler.close();
    }
  }

  @Test
  public void testRejectedExecution() throws Exception {
    TransactionsMsgHandler handler = new TransactionsMsgHandler();
    ExecutorService originalPool = null;
    try {
      ExecutorService mockPool = Mockito.mock(ExecutorService.class);
      Mockito.when(mockPool.submit(Mockito.any(Runnable.class)))
          .thenThrow(new RejectedExecutionException("pool closed"));
      originalPool = replaceTrxHandlePool(handler, mockPool);

      PeerConnection peer = Mockito.mock(PeerConnection.class);
      TransactionsMessage msg = buildTransferMessage(2);
      stubAdvInvRequest(peer, msg);
      // 2 transfer transactions, submit throws on the first → catch + break, only called once
      handler.processMessage(peer, msg);

      Mockito.verify(mockPool, Mockito.times(1)).submit(Mockito.any(Runnable.class));
    } finally {
      closeHandlerAndOriginalPool(handler, originalPool);
    }
  }

  @Test
  public void testCloseDuringProcessing() throws Exception {
    TransactionsMsgHandler handler = new TransactionsMsgHandler();
    ExecutorService originalPool = null;
    try {
      Field closedField = TransactionsMsgHandler.class.getDeclaredField("isClosed");
      closedField.setAccessible(true);

      ExecutorService mockPool = Mockito.mock(ExecutorService.class);
      Future<?> submittedTask = Mockito.mock(Future.class);
      // on the first submit, flip isClosed to true so the second iteration breaks
      Mockito.when(mockPool.submit(Mockito.any(Runnable.class))).thenAnswer(inv -> {
        closedField.set(handler, true);
        return submittedTask;
      });
      originalPool = replaceTrxHandlePool(handler, mockPool);

      PeerConnection peer = Mockito.mock(PeerConnection.class);
      TransactionsMessage msg = buildTransferMessage(2);
      stubAdvInvRequest(peer, msg);
      handler.processMessage(peer, msg);

      Mockito.verify(mockPool, Mockito.times(1)).submit(Mockito.any(Runnable.class));
    } finally {
      closeHandlerAndOriginalPool(handler, originalPool);
    }
  }

  private TransactionsMessage buildTransferMessage(int count) {
    List<Protocol.Transaction> txs = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      BalanceContract.TransferContract tc = BalanceContract.TransferContract.newBuilder()
          .setAmount(10 + i)
          .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString("121212a9cf")))
          .setToAddress(ByteString.copyFrom(ByteArray.fromHexString("232323a9cf")))
          .build();
      txs.add(Protocol.Transaction.newBuilder().setRawData(
          Protocol.Transaction.raw.newBuilder()
              .setTimestamp(1_700_000_000_000L + i)
              .setRefBlockNum(1)
              .addContract(Protocol.Transaction.Contract.newBuilder()
                  .setType(Protocol.Transaction.Contract.ContractType.TransferContract)
                  .setParameter(Any.pack(tc)).build()).build())
          .build());
    }
    return new TransactionsMessage(txs);
  }

  private void stubAdvInvRequest(PeerConnection peer, TransactionsMessage msg) {
    Map<Item, Long> advInvRequest = new ConcurrentHashMap<>();
    for (Protocol.Transaction trx : msg.getTransactions().getTransactionsList()) {
      Item item = new Item(new TransactionMessage(trx).getMessageId(),
          Protocol.Inventory.InventoryType.TRX);
      advInvRequest.put(item, 0L);
    }
    Mockito.when(peer.getAdvInvRequest()).thenReturn(advInvRequest);
  }

  private ExecutorService replaceTrxHandlePool(TransactionsMsgHandler handler, ExecutorService pool)
      throws Exception {
    Field poolField = TransactionsMsgHandler.class.getDeclaredField("trxHandlePool");
    poolField.setAccessible(true);
    ExecutorService originalPool = (ExecutorService) poolField.get(handler);
    poolField.set(handler, pool);
    return originalPool;
  }

  private void closeHandlerAndOriginalPool(TransactionsMsgHandler handler,
      ExecutorService originalPool) {
    try {
      handler.close();
    } finally {
      if (originalPool != null) {
        originalPool.shutdown();
        try {
          if (!originalPool.awaitTermination(5, TimeUnit.SECONDS)) {
            originalPool.shutdownNow();
          }
        } catch (InterruptedException e) {
          originalPool.shutdownNow();
          Thread.currentThread().interrupt();
        }
      }
    }
  }

  @Test
  public void testHandleTransaction() throws Exception {
    TransactionsMsgHandler handler = new TransactionsMsgHandler();
    try {
      TronNetDelegate tronNetDelegate = Mockito.mock(TronNetDelegate.class);
      AdvService advService = Mockito.mock(AdvService.class);
      ChainBaseManager chainBaseManager = Mockito.mock(ChainBaseManager.class);

      Field f1 = TransactionsMsgHandler.class.getDeclaredField("tronNetDelegate");
      f1.setAccessible(true);
      f1.set(handler, tronNetDelegate);
      Field f2 = TransactionsMsgHandler.class.getDeclaredField("advService");
      f2.setAccessible(true);
      f2.set(handler, advService);
      Field f3 = TransactionsMsgHandler.class.getDeclaredField("chainBaseManager");
      f3.setAccessible(true);
      f3.set(handler, chainBaseManager);

      PeerConnection peer = Mockito.mock(PeerConnection.class);

      BalanceContract.TransferContract tc = BalanceContract.TransferContract.newBuilder()
          .setAmount(10)
          .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString("121212a9cf")))
          .setToAddress(ByteString.copyFrom(ByteArray.fromHexString("232323a9cf")))
          .build();
      long now = System.currentTimeMillis();
      Protocol.Transaction trx = Protocol.Transaction.newBuilder().setRawData(
          Protocol.Transaction.raw.newBuilder()
              .setTimestamp(now)
              .setExpiration(now + 60_000)
              .setRefBlockNum(1)
              .addContract(Protocol.Transaction.Contract.newBuilder()
                  .setType(Protocol.Transaction.Contract.ContractType.TransferContract)
                  .setParameter(Any.pack(tc)).build()).build())
          .build();
      TransactionMessage trxMsg = new TransactionMessage(trx);

      Method handleTx = TransactionsMsgHandler.class.getDeclaredMethod(
          "handleTransaction", PeerConnection.class, TransactionMessage.class);
      handleTx.setAccessible(true);

      // happy path → push and broadcast
      Mockito.when(chainBaseManager.getNextBlockSlotTime()).thenReturn(now);
      handleTx.invoke(handler, peer, trxMsg);
      Mockito.verify(advService).broadcast(trxMsg);

      // P2pException BAD_TRX → disconnect
      Mockito.doThrow(new P2pException(TypeEnum.BAD_TRX, "bad"))
          .when(tronNetDelegate).pushTransaction(Mockito.any());
      handleTx.invoke(handler, peer, trxMsg);
      Mockito.verify(peer).setBadPeer(true);
      Mockito.verify(peer).disconnect(Protocol.ReasonCode.BAD_TX);
    } finally {
      handler.close();
    }
  }

  @Test
  public void testDuplicateTransactionRejected() throws Exception {
    TransactionsMsgHandler handler = new TransactionsMsgHandler();
    handler.init();
    try {
      PeerConnection peer = Mockito.mock(PeerConnection.class);

      // Build a transaction
      BalanceContract.TransferContract transferContract = BalanceContract.TransferContract
          .newBuilder()
          .setAmount(10)
          .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString("121212a9cf")))
          .setToAddress(ByteString.copyFrom(ByteArray.fromHexString("232323a9cf")))
          .build();
      Protocol.Transaction trx = Protocol.Transaction.newBuilder()
          .setRawData(Protocol.Transaction.raw.newBuilder()
              .addContract(Protocol.Transaction.Contract.newBuilder()
                  .setType(Protocol.Transaction.Contract.ContractType.TransferContract)
                  .setParameter(Any.pack(transferContract)).build())
              .build())
          .build();

      // Same trx twice → duplicate
      Protocol.Transactions transactions = Protocol.Transactions.newBuilder()
          .addTransactions(trx)
          .addTransactions(trx)
          .build();
      TransactionsMessage msg = new TransactionsMessage(transactions.getTransactionsList());

      TransactionMessage trxMsg = new TransactionMessage(trx);
      Item item = new Item(trxMsg.getMessageId(), Protocol.Inventory.InventoryType.TRX);
      Map<Item, Long> advInvRequest = new ConcurrentHashMap<>();
      advInvRequest.put(item, System.currentTimeMillis());
      Mockito.when(peer.getAdvInvRequest()).thenReturn(advInvRequest);

      try {
        handler.processMessage(peer, msg);
        Assert.fail("Expected P2pException for duplicate transaction");
      } catch (P2pException e) {
        Assert.assertEquals(P2pException.TypeEnum.BAD_MESSAGE, e.getType());
      }
    } finally {
      handler.close();
    }
  }

  @Test
  public void testInvalidSigLength() throws Exception {
    Protocol.Transaction base = buildTransferMessage(1).getTransactions().getTransactions(0);
    for (int length : new int[]{0, 64, 65, 66, 67, 68, 96}) {
      Protocol.Transaction transaction = withSignatures(base, length);
      List<Protocol.Transaction> expected = length == 65
          ? Collections.singletonList(transaction) : Collections.emptyList();
      assertSignatureBatch(Collections.singletonList(transaction), expected,
          Collections.emptyList());
    }
    assertSignatureBatch(Collections.singletonList(base), Collections.singletonList(base),
        Collections.emptyList());
  }

  @Test
  public void testMixedSignatureLengths() throws Exception {
    List<Protocol.Transaction> base = buildTransferMessage(3).getTransactions()
        .getTransactionsList();
    for (int invalidIndex = 0; invalidIndex < base.size(); invalidIndex++) {
      List<Protocol.Transaction> transactions = new ArrayList<>();
      List<Protocol.Transaction> expected = new ArrayList<>();
      for (int i = 0; i < base.size(); i++) {
        Protocol.Transaction transaction = i == invalidIndex
            ? withSignatures(base.get(i), 65, 66 + invalidIndex)
            : withSignatures(base.get(i), 65);
        transactions.add(transaction);
        if (i != invalidIndex) {
          expected.add(transaction);
        }
      }
      assertSignatureBatch(transactions, expected, Collections.emptyList());
    }
  }

  @Test
  public void testSmartContractSignatureLengths() throws Exception {
    byte[] address = ByteArray.fromHexString("121212a9cf");
    Protocol.Transaction valid = withSignatures(
        TvmTestUtils.generateTriggerSmartContractAndGetTransaction(address, address,
            ByteArray.fromHexString("123456"), 100, 100000000, 0, 0), 65);
    Protocol.Transaction invalid = withSignatures(
        TvmTestUtils.generateTriggerSmartContractAndGetTransaction(address, address,
            ByteArray.fromHexString("123457"), 100, 100000000, 0, 0), 65, 68);
    Protocol.Transaction transfer = withSignatures(
        buildTransferMessage(1).getTransactions().getTransactions(0), 65);
    assertSignatureBatch(Arrays.asList(invalid, valid, transfer),
        Collections.singletonList(transfer), Collections.singletonList(valid));
  }

  @Test
  public void testInvalidSignaturePreservesProtocolChecks() throws Exception {
    List<Protocol.Transaction> base = buildTransferMessage(2).getTransactions()
        .getTransactionsList();
    Protocol.Transaction padded = withSignatures(base.get(0), 68);
    Protocol.Transaction valid = withSignatures(base.get(1), 65);
    assertProtocolRejection(Arrays.asList(padded, valid), 1, TypeEnum.BAD_MESSAGE);
    assertProtocolRejection(Arrays.asList(padded, withSignatures(base.get(0), 65)), 2,
        TypeEnum.BAD_MESSAGE);
    Protocol.Transaction noContract = valid.toBuilder()
        .setRawData(valid.getRawData().toBuilder().clearContract()).build();
    assertProtocolRejection(Arrays.asList(padded, noContract), 2, TypeEnum.BAD_TRX);
  }

  private Protocol.Transaction withSignatures(Protocol.Transaction transaction, int... lengths) {
    Protocol.Transaction.Builder builder = transaction.toBuilder().clearSignature();
    for (int length : lengths) {
      builder.addSignature(ByteString.copyFrom(new byte[length]));
    }
    return builder.build();
  }

  private void assertSignatureBatch(List<Protocol.Transaction> transactions,
      List<Protocol.Transaction> expectedTransfers, List<Protocol.Transaction> expectedContracts)
      throws Exception {
    TransactionsMsgHandler handler = new TransactionsMsgHandler();
    ExecutorService originalPool = null;
    try {
      TronNetDelegate delegate = Mockito.mock(TronNetDelegate.class);
      AdvService advService = Mockito.mock(AdvService.class);
      ChainBaseManager chainBaseManager = Mockito.mock(ChainBaseManager.class);
      setField(handler, "tronNetDelegate", delegate);
      setField(handler, "advService", advService);
      setField(handler, "chainBaseManager", chainBaseManager);

      ExecutorService pool = Mockito.mock(ExecutorService.class);
      Future<?> submittedTask = Mockito.mock(Future.class);
      Mockito.when(pool.submit(Mockito.any(Runnable.class))).thenAnswer(invocation -> {
        ((Runnable) invocation.getArgument(0)).run();
        return submittedTask;
      });
      originalPool = replaceTrxHandlePool(handler, pool);

      PeerConnection peer = Mockito.mock(PeerConnection.class);
      TransactionsMessage message = new TransactionsMessage(transactions);
      stubAdvInvRequest(peer, message);
      Map<Item, Long> requests = peer.getAdvInvRequest();
      Protocol.Transaction unrelated = buildTransferMessage(4).getTransactions().getTransactions(3);
      Item unrelatedItem = new Item(new TransactionMessage(unrelated).getMessageId(),
          Protocol.Inventory.InventoryType.TRX);
      requests.put(unrelatedItem, 0L);

      handler.processMessage(peer, message);

      Assert.assertEquals(Collections.singletonMap(unrelatedItem, 0L), requests);
      Mockito.verify(peer, Mockito.never()).disconnect(Mockito.any());
      Mockito.verify(peer, Mockito.never()).setBadPeer(Mockito.anyBoolean());
      ArgumentCaptor<TransactionCapsule> pushed = ArgumentCaptor.forClass(TransactionCapsule.class);
      Mockito.verify(delegate, Mockito.times(expectedTransfers.size()))
          .pushTransaction(pushed.capture());
      List<Protocol.Transaction> actualTransfers = new ArrayList<>();
      pushed.getAllValues().forEach(transaction -> actualTransfers.add(transaction.getInstance()));
      Assert.assertEquals(expectedTransfers, actualTransfers);
      ArgumentCaptor<TransactionMessage> broadcast =
          ArgumentCaptor.forClass(TransactionMessage.class);
      Mockito.verify(advService, Mockito.times(expectedTransfers.size()))
          .broadcast(broadcast.capture());
      List<Protocol.Transaction> actualBroadcasts = new ArrayList<>();
      broadcast.getAllValues().forEach(transaction ->
          actualBroadcasts.add(transaction.getTransactionCapsule().getInstance()));
      Assert.assertEquals(expectedTransfers, actualBroadcasts);

      Field queueField = TransactionsMsgHandler.class.getDeclaredField("smartContractQueue");
      queueField.setAccessible(true);
      BlockingQueue<?> contracts = (BlockingQueue<?>) queueField.get(handler);
      List<Protocol.Transaction> actualContracts = new ArrayList<>();
      for (Object entry : contracts) {
        TransactionsMsgHandler.TrxEvent event = (TransactionsMsgHandler.TrxEvent) entry;
        actualContracts.add(event.getMsg().getTransactionCapsule().getInstance());
      }
      Assert.assertEquals(expectedContracts, actualContracts);
    } finally {
      closeHandlerAndOriginalPool(handler, originalPool);
    }
  }

  private void assertProtocolRejection(List<Protocol.Transaction> transactions, int requestedCount,
      TypeEnum expected) throws Exception {
    TransactionsMsgHandler handler = new TransactionsMsgHandler();
    ExecutorService originalPool = null;
    try {
      ExecutorService pool = Mockito.mock(ExecutorService.class);
      originalPool = replaceTrxHandlePool(handler, pool);
      PeerConnection peer = Mockito.mock(PeerConnection.class);
      stubAdvInvRequest(peer, new TransactionsMessage(transactions.subList(0, requestedCount)));
      int requestCount = peer.getAdvInvRequest().size();

      P2pException error = Assert.assertThrows(P2pException.class,
          () -> handler.processMessage(peer, new TransactionsMessage(transactions)));

      Assert.assertEquals(expected, error.getType());
      Assert.assertEquals(requestCount, peer.getAdvInvRequest().size());
      Mockito.verify(pool, Mockito.never()).submit(Mockito.any(Runnable.class));
    } finally {
      closeHandlerAndOriginalPool(handler, originalPool);
    }
  }

  private void setField(TransactionsMsgHandler handler, String name, Object value)
      throws Exception {
    Field field = TransactionsMsgHandler.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(handler, value);
  }

  @Test
  public void testIsBusyWithCachedTransactions() throws Exception {
    TransactionsMsgHandler handler = new TransactionsMsgHandler();
    try {
      int threshold = Args.getInstance().getMaxTrxCacheSize();
      TronNetDelegate tronNetDelegateMock = Mockito.mock(TronNetDelegate.class);
      Field field = TransactionsMsgHandler.class.getDeclaredField("tronNetDelegate");
      field.setAccessible(true);
      field.set(handler, tronNetDelegateMock);

      // queue and smartContractQueue are empty, but cached size > threshold
      Mockito.when(tronNetDelegateMock.getCachedTransactionSize()).thenReturn(threshold + 1);
      Assert.assertTrue(handler.isBusy());

      // boundary: cached size == threshold, isBusy() uses strict >, so not busy
      Mockito.when(tronNetDelegateMock.getCachedTransactionSize()).thenReturn(threshold);
      Assert.assertFalse(handler.isBusy());

      Mockito.when(tronNetDelegateMock.getCachedTransactionSize()).thenReturn(0);
      Assert.assertFalse(handler.isBusy());
    } finally {
      handler.close();
    }
  }
}
