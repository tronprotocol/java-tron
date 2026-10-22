package org.tron.core.net.messagehandler;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.UnknownFieldSet;
import io.prometheus.client.CollectorRegistry;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
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
import org.tron.common.prometheus.MetricKeys;
import org.tron.common.prometheus.MetricLabels;
import org.tron.common.runtime.TvmTestUtils;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.ReflectUtils;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.ChainBaseManager;
import org.tron.core.Constant;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.exception.P2pException;
import org.tron.core.exception.P2pException.TypeEnum;
import org.tron.core.net.P2pEventHandlerImpl;
import org.tron.core.net.TronNetDelegate;
import org.tron.core.net.message.adv.FetchInvDataMessage;
import org.tron.core.net.message.adv.TransactionMessage;
import org.tron.core.net.message.adv.TransactionsMessage;
import org.tron.core.net.peer.Item;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.service.adv.AdvService;
import org.tron.core.net.service.sync.SyncService;
import org.tron.p2p.connection.Channel;
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

      // Unrequested transactions are rejected before their contract count is checked.
      Protocol.Transaction trx2 = Protocol.Transaction.newBuilder().setRawData(
          Protocol.Transaction.raw.newBuilder().setTimestamp(transactionTimestamp)
              .setRefBlockNum(1).build())
          .build();
      List<Protocol.Transaction> transactionList2 = new ArrayList<>();
      transactionList2.add(trx2);
      P2pException unrequested = Assert.assertThrows(P2pException.class,
          () -> transactionsMsgHandler.processMessage(peer,
              new TransactionsMessage(transactionList2)));
      Assert.assertEquals(P2pException.TypeEnum.BAD_MESSAGE, unrequested.getType());
      Assert.assertTrue(unrequested.getMessage().contains("without request"));
      Map<Item, Long> advInvRequest2 = new ConcurrentHashMap<>();
      Item item2 = new Item(new TransactionMessage(trx2).getMessageId(),
          Protocol.Inventory.InventoryType.TRX);
      advInvRequest2.put(item2, 0L);
      Mockito.when(peer.getAdvInvRequest()).thenReturn(advInvRequest2);
      P2pException emptyContract = Assert.assertThrows(P2pException.class,
          () -> transactionsMsgHandler.processMessage(peer,
              new TransactionsMessage(transactionList2)));
      Assert.assertEquals(P2pException.TypeEnum.BAD_TRX, emptyContract.getType());
      Assert.assertTrue(emptyContract.getMessage()
          .contains("contract size should be greater than 0"));
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

      Assert.assertTrue(peer.getAdvInvRequest().isEmpty());
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
      Mockito.when(tronNetDelegate.pushTransaction(Mockito.any())).thenReturn(true);
      handleTx.invoke(handler, peer, trxMsg);
      Mockito.verify(advService).broadcast(trxMsg);

      // local capacity rejection → do not broadcast or penalize the peer
      Mockito.when(tronNetDelegate.pushTransaction(Mockito.any())).thenReturn(false);
      handleTx.invoke(handler, peer, trxMsg);
      Mockito.verify(advService, Mockito.times(1)).broadcast(trxMsg);
      Mockito.verify(peer, Mockito.never()).setBadPeer(true);
      Mockito.verify(peer, Mockito.never()).disconnect(Mockito.any());

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
      Assert.assertEquals(1, advInvRequest.size());
      Assert.assertTrue(advInvRequest.containsKey(item));
    } finally {
      handler.close();
    }
  }

  @Test
  public void testUnrequestedTransactionReportsTransactionId() throws Exception {
    TransactionsMsgHandler handler = new TransactionsMsgHandler();
    try {
      PeerConnection peer = Mockito.mock(PeerConnection.class);
      Mockito.when(peer.getAdvInvRequest()).thenReturn(new ConcurrentHashMap<>());
      TransactionsMessage msg = buildTransferMessage(1);
      Protocol.Transaction trx = msg.getTransactions().getTransactions(0);
      String transactionId = new TransactionMessage(trx).getMessageId().toString();

      try {
        handler.processMessage(peer, msg);
        Assert.fail("Expected an unrequested transaction to be rejected");
      } catch (P2pException e) {
        Assert.assertEquals(P2pException.TypeEnum.BAD_MESSAGE, e.getType());
        Assert.assertTrue(e.getMessage().contains(transactionId));
      }
    } finally {
      handler.close();
    }
  }

  @Test
  public void testInvalidBatchPreservesRequests() throws Exception {
    TransactionsMsgHandler handler = new TransactionsMsgHandler();
    ExecutorService originalPool = null;
    try {
      ExecutorService pool = Mockito.mock(ExecutorService.class);
      originalPool = replaceTrxHandlePool(handler, pool);
      List<Protocol.Transaction> transactions = new ArrayList<>(
          buildTransferMessage(2).getTransactions().getTransactionsList());
      // The first transaction passes; the second has no contract, invalidating the batch.
      transactions.set(1, transactions.get(1).toBuilder()
          .setRawData(transactions.get(1).getRawData().toBuilder().clearContract()).build());
      TransactionsMessage msg = new TransactionsMessage(transactions);
      PeerConnection peer = Mockito.mock(PeerConnection.class);
      stubAdvInvRequest(peer, msg);
      Map<Item, Long> requests = new ConcurrentHashMap<>(peer.getAdvInvRequest());
      Assert.assertEquals(2, requests.size());

      P2pException exception = Assert.assertThrows(P2pException.class,
          () -> handler.processMessage(peer, msg));

      Assert.assertEquals(TypeEnum.BAD_TRX, exception.getType());
      Assert.assertEquals(requests, peer.getAdvInvRequest());
      Mockito.verify(pool, Mockito.never()).submit(Mockito.any(Runnable.class));
    } finally {
      closeHandlerAndOriginalPool(handler, originalPool);
    }
  }

  @Test
  public void testInvalidSigLength() throws Exception {
    Protocol.Transaction base = buildTransferMessage(1).getTransactions().getTransactions(0);
    for (int length : new int[]{0, 64, 65, 66, 67, 68, 69, 96}) {
      Protocol.Transaction transaction = withSignatures(base, length);
      if (length >= 65 && length <= 68) {
        assertSignatureBatch(Collections.singletonList(transaction),
            Collections.singletonList(transaction), Collections.emptyList());
      } else {
        assertRejectedSignatureBatch(Collections.singletonList(transaction));
      }
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
      for (int i = 0; i < base.size(); i++) {
        Protocol.Transaction transaction = i == invalidIndex
            ? withSignatures(base.get(i), 65, 69 + invalidIndex)
            : withSignatures(base.get(i), 65);
        transactions.add(transaction);
      }
      assertRejectedSignatureBatch(transactions);
    }
  }

  @Test
  public void testDropMetricCountsFirstInvalidTransaction() throws Exception {
    boolean metricsEnabled = Args.getInstance().isMetricsPrometheusEnable();
    Args.getInstance().setMetricsPrometheusEnable(true);
    try {
      List<Protocol.Transaction> base = buildTransferMessage(3).getTransactions()
          .getTransactionsList();
      Protocol.Transaction valid = withSignatures(base.get(0), 65);
      Protocol.Transaction shortSignature = withSignatures(base.get(1), 64);
      Protocol.Transaction invalidMultiSign = withSignatures(base.get(2), 65, 69, 70);
      double before = droppedTransactions();

      for (int batch = 1; batch <= 2; batch++) {
        assertRejectedSignatureBatch(Arrays.asList(shortSignature, valid, invalidMultiSign));
        Assert.assertEquals(before + batch, droppedTransactions(), 0);
      }
    } finally {
      Args.getInstance().setMetricsPrometheusEnable(metricsEnabled);
    }
  }

  @Test
  public void testAcceptedTransactionsDoNotIncrementDropMetric() throws Exception {
    boolean metricsEnabled = Args.getInstance().isMetricsPrometheusEnable();
    Args.getInstance().setMetricsPrometheusEnable(true);
    try {
      List<Protocol.Transaction> base = buildTransferMessage(2).getTransactions()
          .getTransactionsList();
      List<Protocol.Transaction> transactions = Arrays.asList(
          withSignatures(base.get(0), 65, 66, 67, 68), base.get(1));
      double before = droppedTransactions();

      assertSignatureBatch(transactions, transactions, Collections.emptyList());

      Assert.assertEquals(before, droppedTransactions(), 0);
    } finally {
      Args.getInstance().setMetricsPrometheusEnable(metricsEnabled);
    }
  }

  @Test
  public void testDisabledDropMetricPreservesTransactionHandling() throws Exception {
    boolean metricsEnabled = Args.getInstance().isMetricsPrometheusEnable();
    Args.getInstance().setMetricsPrometheusEnable(false);
    try {
      List<Protocol.Transaction> base = buildTransferMessage(2).getTransactions()
          .getTransactionsList();
      Protocol.Transaction valid = withSignatures(base.get(0), 65);
      Protocol.Transaction invalid = withSignatures(base.get(1), 69);
      double before = droppedTransactions();

      assertRejectedSignatureBatch(Arrays.asList(invalid, valid));

      Assert.assertEquals(before, droppedTransactions(), 0);
    } finally {
      Args.getInstance().setMetricsPrometheusEnable(metricsEnabled);
    }
  }

  @Test
  public void testInvalidSignatureDisconnectRetriesAnotherAnnouncer() throws Exception {
    assertDisconnectRecovery(true, false);
  }

  @Test
  public void testInvalidSignatureDisconnectWaitsForBusyAnnouncer() throws Exception {
    assertDisconnectRecovery(true, true);
  }

  @Test
  public void testInvalidSignatureDisconnectReleasesFetchCache() throws Exception {
    assertDisconnectRecovery(false, false);
  }

  private void assertDisconnectRecovery(boolean announced, boolean busy) throws Exception {
    TransactionsMsgHandler handler = new TransactionsMsgHandler();
    AdvService adv = new AdvService();
    ExecutorService originalPool = null;
    try {
      TronNetDelegate delegate = Mockito.mock(TronNetDelegate.class);
      Mockito.when(delegate.pushTransaction(Mockito.any())).thenReturn(true);
      PeerConnection source = recoveryPeer("127.0.0.11", adv);
      PeerConnection other = recoveryPeer("127.0.0.12", adv);
      List<PeerConnection> activePeers = new ArrayList<>(Collections.singletonList(source));
      Mockito.when(delegate.getActivePeer()).thenReturn(activePeers);
      ReflectUtils.setFieldValue(adv, "tronNetDelegate", delegate);
      setField(handler, "tronNetDelegate", delegate);
      setField(handler, "advService", adv);
      setField(handler, "chainBaseManager", Mockito.mock(ChainBaseManager.class));

      List<Runnable> tasks = new ArrayList<>();
      ExecutorService pool = Mockito.mock(ExecutorService.class);
      Mockito.when(pool.submit(Mockito.any(Runnable.class))).thenAnswer(call -> {
        tasks.add(call.getArgument(0));
        return Mockito.mock(Future.class);
      });
      originalPool = replaceTrxHandlePool(handler, pool);

      long now = System.currentTimeMillis();
      List<Protocol.Transaction> transfers = buildTransferMessage(2).getTransactions()
          .getTransactionsList();
      Protocol.Transaction retry = withSignatures(transfers.get(0).toBuilder().setRawData(
          transfers.get(0).getRawData().toBuilder().setExpiration(now + 60_000)).build(), 65);
      Protocol.Transaction transfer = withSignatures(transfers.get(1), 65);
      Protocol.Transaction smart = withSignatures(admissionTransaction(true), 65);
      List<Protocol.Transaction> valid = Arrays.asList(retry, transfer, smart);
      List<Item> items = new ArrayList<>();
      for (Protocol.Transaction transaction : valid) {
        Item item = new Item(new TransactionMessage(transaction).getMessageId(),
            Protocol.Inventory.InventoryType.TRX);
        items.add(item);
        source.getAdvInvReceive().put(item, now);
        Assert.assertTrue(adv.addInv(item));
      }
      Method fetch = AdvService.class.getDeclaredMethod("consumerInvToFetch");
      fetch.setAccessible(true);
      fetch.invoke(adv);
      Map<Item, Long> requests = new ConcurrentHashMap<>(source.getAdvInvRequest());
      Assert.assertEquals(3, requests.size());
      if (announced) {
        activePeers.add(other);
        items.forEach(item -> other.getAdvInvReceive().put(item, now));
      }
      Item busyItem = new Item(Sha256Hash.ZERO_HASH, Protocol.Inventory.InventoryType.TRX);
      if (busy) {
        other.getAdvInvRequest().put(busyItem, now);
      }

      P2pEventHandlerImpl events = new P2pEventHandlerImpl();
      ReflectUtils.setFieldValue(events, "transactionsMsgHandler", handler);
      Method dispatch = P2pEventHandlerImpl.class.getDeclaredMethod("processMessage",
          PeerConnection.class, byte[].class);
      dispatch.setAccessible(true);
      TransactionsMessage response = new TransactionsMessage(
          Arrays.asList(transfer, withSignatures(retry, 69), smart));
      dispatch.invoke(events, source, response.getSendBytes());

      Mockito.verify(source).disconnect(Protocol.ReasonCode.BAD_TX);
      Mockito.verify(source.getChannel()).close(Mockito.anyLong());
      Assert.assertEquals(requests, source.getAdvInvRequest());
      Assert.assertTrue(tasks.isEmpty());
      Mockito.verify(delegate, Mockito.never()).pushTransaction(Mockito.any());
      BlockingQueue<?> contracts = (BlockingQueue<?>)
          ReflectUtils.getFieldObject(handler, "smartContractQueue");
      Assert.assertTrue(contracts.isEmpty());

      // The transport removes the source before invoking its disconnect cleanup.
      activePeers.remove(source);
      source.onDisconnect();
      Assert.assertTrue(source.getAdvInvRequest().isEmpty());
      if (!announced) {
        for (Item item : items) {
          Assert.assertTrue("rejected batch must be fetchable again", adv.addInv(item));
          other.getAdvInvReceive().put(item, now);
        }
        activePeers.add(other);
        fetch.invoke(adv);
      } else if (busy) {
        Mockito.verify(other, Mockito.never()).sendMessage(Mockito.any());
        other.getAdvInvRequest().remove(busyItem);
        fetch.invoke(adv);
      }
      Assert.assertEquals(requests.keySet(), other.getAdvInvRequest().keySet());
      ArgumentCaptor<FetchInvDataMessage> requested =
          ArgumentCaptor.forClass(FetchInvDataMessage.class);
      Mockito.verify(other).sendMessage(requested.capture());
      Assert.assertEquals(3, requested.getValue().getHashList().size());
      for (Item item : items) {
        Assert.assertTrue(requested.getValue().getHashList().contains(item.getHash()));
      }

      dispatch.invoke(events, other, new TransactionsMessage(valid).getSendBytes());
      Assert.assertTrue(other.getAdvInvRequest().isEmpty());
      Assert.assertEquals(2, tasks.size());
      tasks.forEach(Runnable::run);
      Assert.assertEquals(1, contracts.size());
      TransactionsMsgHandler.TrxEvent contract =
          (TransactionsMsgHandler.TrxEvent) contracts.remove();
      Method handle = TransactionsMsgHandler.class.getDeclaredMethod("handleTransaction",
          PeerConnection.class, TransactionMessage.class);
      handle.setAccessible(true);
      handle.invoke(handler, contract.getPeer(), contract.getMsg());
      ArgumentCaptor<TransactionCapsule> pushed = ArgumentCaptor.forClass(TransactionCapsule.class);
      Mockito.verify(delegate, Mockito.times(3)).pushTransaction(pushed.capture());
      List<Protocol.Transaction> accepted = new ArrayList<>();
      pushed.getAllValues().forEach(transaction -> accepted.add(transaction.getInstance()));
      Assert.assertEquals(valid, accepted);
      for (Item item : items) {
        Assert.assertNotNull(adv.getMessage(item));
      }
      Mockito.verify(other, Mockito.never()).disconnect(Mockito.any());
    } finally {
      closeHandlerAndOriginalPool(handler, originalPool);
      adv.close();
    }
  }

  private PeerConnection recoveryPeer(String host, AdvService adv) {
    InetSocketAddress address = new InetSocketAddress(host, 18888);
    Channel channel = Mockito.mock(Channel.class);
    Mockito.when(channel.getInetSocketAddress()).thenReturn(address);
    Mockito.when(channel.getInetAddress()).thenReturn(address.getAddress());
    PeerConnection peer = Mockito.spy(new PeerConnection());
    peer.setChannel(channel);
    Mockito.doNothing().when(peer).sendMessage(Mockito.any());
    ReflectUtils.setFieldValue(peer, "advService", adv);
    ReflectUtils.setFieldValue(peer, "syncService", Mockito.mock(SyncService.class));
    return peer;
  }

  private double droppedTransactions() {
    Double value = CollectorRegistry.defaultRegistry.getSampleValue(
        MetricKeys.Counter.TX_REJECT + "_total", new String[]{"source", "reason"},
        new String[]{MetricLabels.Counter.TX_REJECT_P2P,
            MetricLabels.Counter.TX_REJECT_INVALID_SIGNATURE_LENGTH});
    return value == null ? 0 : value;
  }

  @Test
  public void testSmartContractSignatureLengths() throws Exception {
    byte[] address = ByteArray.fromHexString("121212a9cf");
    Protocol.Transaction valid = withSignatures(
        TvmTestUtils.generateTriggerSmartContractAndGetTransaction(address, address,
            ByteArray.fromHexString("123456"), 100, 100000000, 0, 0), 65, 66, 67, 68);
    Protocol.Transaction invalid = withSignatures(
        TvmTestUtils.generateTriggerSmartContractAndGetTransaction(address, address,
            ByteArray.fromHexString("123457"), 100, 100000000, 0, 0), 65, 69);
    Protocol.Transaction transfer = withSignatures(
        buildTransferMessage(1).getTransactions().getTransactions(0), 65);
    assertRejectedSignatureBatch(Arrays.asList(invalid, valid, transfer));
    assertSignatureBatch(Arrays.asList(valid, transfer), Collections.singletonList(transfer),
        Collections.singletonList(valid));
  }

  @Test
  public void testInvalidSignaturePreservesProtocolChecks() throws Exception {
    boolean metricsEnabled = Args.getInstance().isMetricsPrometheusEnable();
    Args.getInstance().setMetricsPrometheusEnable(true);
    try {
      double before = droppedTransactions();
      List<Protocol.Transaction> base = buildTransferMessage(2).getTransactions()
          .getTransactionsList();
      Protocol.Transaction padded = withSignatures(base.get(0), 69);
      Protocol.Transaction valid = withSignatures(base.get(1), 65);
      assertProtocolRejection(Arrays.asList(padded, valid), 1, TypeEnum.BAD_MESSAGE);
      assertProtocolRejection(Arrays.asList(padded, withSignatures(base.get(0), 65)), 2,
          TypeEnum.BAD_MESSAGE);
      Protocol.Transaction noContract = valid.toBuilder()
          .setRawData(valid.getRawData().toBuilder().clearContract()).build();
      assertProtocolRejection(Arrays.asList(padded, noContract), 2, TypeEnum.BAD_TRX);
      Assert.assertEquals(before, droppedTransactions(), 0);
    } finally {
      Args.getInstance().setMetricsPrometheusEnable(metricsEnabled);
    }
  }

  private Protocol.Transaction withSignatures(Protocol.Transaction transaction, int... lengths) {
    Protocol.Transaction.Builder builder = transaction.toBuilder().clearSignature();
    for (int length : lengths) {
      builder.addSignature(ByteString.copyFrom(new byte[length]));
    }
    return builder.build();
  }

  private void assertRejectedSignatureBatch(List<Protocol.Transaction> transactions)
      throws Exception {
    assertSignatureBatch(transactions, Collections.emptyList(), Collections.emptyList(), true);
  }

  private void assertSignatureBatch(List<Protocol.Transaction> transactions,
      List<Protocol.Transaction> expectedTransfers, List<Protocol.Transaction> expectedContracts)
      throws Exception {
    assertSignatureBatch(transactions, expectedTransfers, expectedContracts, false);
  }

  private void assertSignatureBatch(List<Protocol.Transaction> transactions,
      List<Protocol.Transaction> expectedTransfers, List<Protocol.Transaction> expectedContracts,
      boolean rejected) throws Exception {
    TransactionsMsgHandler handler = new TransactionsMsgHandler();
    ExecutorService originalPool = null;
    try {
      TronNetDelegate delegate = Mockito.mock(TronNetDelegate.class);
      Mockito.when(delegate.pushTransaction(Mockito.any())).thenReturn(true);
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

      if (rejected) {
        Map<Item, Long> originalRequests = new ConcurrentHashMap<>(requests);
        P2pException error = Assert.assertThrows(P2pException.class,
            () -> handler.processMessage(peer, message));
        Assert.assertEquals(TypeEnum.BAD_TRX, error.getType());
        Assert.assertTrue(error.getMessage().contains("invalid signature lengths"));
        Assert.assertEquals(originalRequests, requests);
        Mockito.verify(pool, Mockito.never()).submit(Mockito.any(Runnable.class));
      } else {
        handler.processMessage(peer, message);
        Assert.assertEquals(Collections.singletonMap(unrelatedItem, 0L), requests);
      }
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
  public void testMixedAdmissionPreservesSignaturePolicyAndCleanup() throws Exception {
    boolean metricsEnabled = Args.getInstance().isMetricsPrometheusEnable();
    Args.getInstance().setMetricsPrometheusEnable(true);
    try {
      for (boolean smart : new boolean[]{false, true}) {
        Protocol.Transaction base = admissionTransaction(smart);
        Protocol.Transaction expected = withSignatures(base.toBuilder().setRawData(
            base.getRawData().toBuilder().setTimestamp(1)).build(), 65, 66, 67, 68);
        Protocol.Transaction padded = expected.toBuilder()
            .setUnknownFields(padding(600 * 1024))
            .addRet(Protocol.Transaction.Result.newBuilder()
                .setContractRet(Protocol.Transaction.Result.contractResult.REVERT)).build();
        Protocol.Transaction invalid = withSignatures(base.toBuilder().setRawData(
            base.getRawData().toBuilder().setTimestamp(2)).build(), 65, 69, 70);
        Protocol.Transaction oversized = withSignatures(base.toBuilder().setRawData(
            base.getRawData().toBuilder().setTimestamp(3)
                .setData(ByteString.copyFrom(new byte[600 * 1024]))).build(), 68);
        double before = droppedTransactions();

        assertRejectedSignatureBatch(Arrays.asList(invalid, padded, oversized));
        Assert.assertEquals(before + 1, droppedTransactions(), 0);
        assertSignatureBatch(Arrays.asList(padded, oversized),
            smart ? Collections.emptyList() : Collections.singletonList(expected),
            smart ? Collections.singletonList(expected) : Collections.emptyList());
        Assert.assertEquals(before + 1, droppedTransactions(), 0);
      }
    } finally {
      Args.getInstance().setMetricsPrometheusEnable(metricsEnabled);
    }
  }

  @Test
  public void testSanitizeBeforeQueueAdmission() throws Exception {
    for (boolean smart : new boolean[] {false, true}) {
      Protocol.Transaction base = admissionTransaction(smart);
      Protocol.Transaction clean = base.toBuilder().setRawData(base.getRawData().toBuilder()
          .setUnknownFields(padding(32))).build();
      Protocol.Transaction padded = clean.toBuilder().setUnknownFields(padding(600 * 1024))
          .build();
      assertAdmission(padded, clean);
    }
  }

  @Test
  public void testOversizedTransactionDroppedBeforeQueueAdmission() throws Exception {
    for (boolean smart : new boolean[] {false, true}) {
      Protocol.Transaction base = admissionTransaction(smart);
      Protocol.Transaction oversized = base.toBuilder().setRawData(base.getRawData().toBuilder()
          .setData(ByteString.copyFrom(new byte[600 * 1024]))).build();
      assertAdmission(oversized, null);
    }
  }

  @Test
  public void testQueueAdmissionSizeBoundary() throws Exception {
    int limit = (int) Constant.TRANSACTION_MAX_BYTE_SIZE;
    for (boolean smart : new boolean[] {false, true}) {
      Protocol.Transaction.raw.Builder raw = admissionTransaction(smart).getRawData().toBuilder()
          .setData(ByteString.copyFrom(new byte[limit]));
      int overhead = Protocol.Transaction.newBuilder().setRawData(raw).build()
          .getSerializedSize() - limit;
      Protocol.Transaction exact = Protocol.Transaction.newBuilder().setRawData(
          raw.setData(ByteString.copyFrom(new byte[limit - overhead]))).build();
      Assert.assertEquals(limit, exact.getSerializedSize());
      assertAdmission(exact, exact);
      Protocol.Transaction over = exact.toBuilder().setRawData(
          raw.setData(ByteString.copyFrom(new byte[limit - overhead + 1]))).build();
      Assert.assertEquals(limit + 1, over.getSerializedSize());
      assertAdmission(over, null);
    }
  }

  @Test
  public void testResultsClearedBeforeQueueAdmission() throws Exception {
    for (boolean smart : new boolean[] {false, true}) {
      Protocol.Transaction expected = admissionTransaction(smart).toBuilder()
          .addSignature(ByteString.copyFrom(new byte[65])).build();
      // Even a single result must be discarded, including its known and unknown fields.
      Protocol.Transaction.Result result = Protocol.Transaction.Result.newBuilder()
          .setFee(123456).setWithdrawAmount(77)
          .setContractRet(Protocol.Transaction.Result.contractResult.REVERT)
          .setUnknownFields(padding(32)).build();
      assertAdmission(expected.toBuilder().addRet(result).build(), expected);
      assertAdmission(expected.toBuilder().addRet(result).addRet(result).build(), expected);
    }
  }

  @Test
  public void testOversizedResultsClearedBeforeQueueAdmission() throws Exception {
    for (boolean smart : new boolean[] {false, true}) {
      Protocol.Transaction expected = admissionTransaction(smart).toBuilder()
          .addSignature(ByteString.copyFrom(new byte[65])).build();
      // Padding in the first result must be removed before the transaction size check.
      Protocol.Transaction padded = expected.toBuilder().addRet(
          Protocol.Transaction.Result.newBuilder().setUnknownFields(padding(600 * 1024))).build();
      Assert.assertTrue(padded.getSerializedSize() > Constant.TRANSACTION_MAX_BYTE_SIZE);
      assertAdmission(padded, expected);
    }
  }

  private void assertAdmission(Protocol.Transaction input, Protocol.Transaction expected)
      throws Exception {
    TransactionsMsgHandler handler = new TransactionsMsgHandler();
    ExecutorService originalPool = null;
    try {
      ExecutorService pool = Mockito.mock(ExecutorService.class);
      List<Runnable> tasks = new ArrayList<>();
      Mockito.when(pool.submit(Mockito.any(Runnable.class))).thenAnswer(call -> {
        tasks.add(call.getArgument(0));
        return Mockito.mock(Future.class);
      });
      originalPool = replaceTrxHandlePool(handler, pool);
      TronNetDelegate delegate = Mockito.mock(TronNetDelegate.class);
      Mockito.when(delegate.pushTransaction(Mockito.any())).thenReturn(true);
      AdvService adv = Mockito.mock(AdvService.class);
      ReflectUtils.setFieldValue(handler, "tronNetDelegate", delegate);
      ReflectUtils.setFieldValue(handler, "advService", adv);
      ReflectUtils.setFieldValue(handler, "chainBaseManager", Mockito.mock(ChainBaseManager.class));
      PeerConnection peer = Mockito.mock(PeerConnection.class);
      TransactionsMessage message = new TransactionsMessage(Collections.singletonList(input));
      stubAdvInvRequest(peer, message);
      handler.processMessage(peer, message);
      BlockingQueue<?> smartQueue = (BlockingQueue<?>)
          ReflectUtils.getFieldObject(handler, "smartContractQueue");
      if (expected == null) {
        Assert.assertTrue(smartQueue.isEmpty());
        Mockito.verify(pool, Mockito.never()).submit(Mockito.any(Runnable.class));
      } else {
        TransactionMessage admitted;
        if (input.getRawData().getContract(0).getType()
            == Protocol.Transaction.Contract.ContractType.TriggerSmartContract) {
          Assert.assertEquals(1, smartQueue.size());
          admitted = (TransactionMessage) ReflectUtils.getFieldObject(smartQueue.peek(), "msg");
        } else {
          Assert.assertEquals(1, tasks.size());
          // Run only after admission completes, exercising the transaction retained by the task.
          tasks.get(0).run();
          org.mockito.ArgumentCaptor<TransactionMessage> captured =
              org.mockito.ArgumentCaptor.forClass(TransactionMessage.class);
          Mockito.verify(adv).broadcast(captured.capture());
          admitted = captured.getValue();
        }
        Protocol.Transaction retained = admitted.getTransactionCapsule().getInstance();
        Protocol.Transaction serialized = Protocol.Transaction.parseFrom(admitted.getData());
        Assert.assertEquals(expected, retained);
        Assert.assertEquals(0, retained.getRetCount());
        Assert.assertEquals(0, serialized.getRetCount());
        Assert.assertArrayEquals(input.getRawData().toByteArray(),
            retained.getRawData().toByteArray());
        Assert.assertEquals(input.getSignatureList(), retained.getSignatureList());
        Assert.assertArrayEquals(expected.toByteArray(), admitted.getData());
        Assert.assertEquals(new TransactionMessage(input).getMessageId(), admitted.getMessageId());
      }
      Assert.assertTrue(peer.getAdvInvRequest().isEmpty());
      Mockito.verify(peer, Mockito.never()).disconnect(Mockito.any());
    } finally {
      closeHandlerAndOriginalPool(handler, originalPool);
    }
  }

  private Protocol.Transaction admissionTransaction(boolean smart) {
    if (smart) {
      return TvmTestUtils.generateTriggerSmartContractAndGetTransaction(
          ByteArray.fromHexString("121212a9cf"), ByteArray.fromHexString("121212a9cf"),
          ByteArray.fromHexString("123456"), 100, 100000000, 0, 0);
    }
    return buildTransferMessage(1).getTransactions().getTransactions(0);
  }

  private UnknownFieldSet padding(int size) {
    return UnknownFieldSet.newBuilder().addField(99999,
        UnknownFieldSet.Field.newBuilder().addLengthDelimited(ByteString.copyFrom(new byte[size]))
            .build()).build();
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
