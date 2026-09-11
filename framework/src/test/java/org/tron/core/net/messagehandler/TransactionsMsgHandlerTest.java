package org.tron.core.net.messagehandler;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
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
import org.tron.common.utils.ReflectUtils;
import org.tron.core.ChainBaseManager;
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
  public void testProcessMessage() throws Exception {
    TransactionsMsgHandler handler = new TransactionsMsgHandler();
    try {
      ExecutorService pool = installMockPool(handler);
      TronNetDelegate delegate = Mockito.mock(TronNetDelegate.class);
      AdvService advService = Mockito.mock(AdvService.class);
      ChainBaseManager chainManager = Mockito.mock(ChainBaseManager.class);
      ReflectUtils.setFieldValue(handler, "tronNetDelegate", delegate);
      ReflectUtils.setFieldValue(handler, "advService", advService);
      ReflectUtils.setFieldValue(handler, "chainBaseManager", chainManager);
      Assert.assertFalse(handler.isBusy());

      PeerConnection peer = Mockito.mock(PeerConnection.class);
      Protocol.Transaction trx = buildTransferMessage(1).getTransactions().getTransactions(0);
      trx = trx.toBuilder().setRawData(trx.getRawData().toBuilder()
          .setExpiration(1_700_000_060_000L)).build();
      TransactionsMessage msg = new TransactionsMessage(Collections.singletonList(trx));
      stubAdvInvRequest(peer, msg);
      handler.processMessage(peer, msg);

      Assert.assertTrue(peer.getAdvInvRequest().isEmpty());
      ArgumentCaptor<Runnable> submitted = ArgumentCaptor.forClass(Runnable.class);
      Mockito.verify(pool).submit(submitted.capture());
      Mockito.verify(delegate).getCachedTransactionSize();
      Mockito.verifyNoMoreInteractions(delegate);
      Mockito.verifyNoInteractions(advService, chainManager);
      Mockito.when(chainManager.getNextBlockSlotTime()).thenReturn(1_700_000_000_000L);
      submitted.getValue().run();
      Mockito.verify(delegate).pushTransaction(Mockito.any());
      Mockito.verify(advService).broadcast(Mockito.any(TransactionMessage.class));
    } finally {
      handler.close();
    }
  }

  @Test
  public void testSmartContractQueueFull() throws Exception {
    TransactionsMsgHandler handler = new TransactionsMsgHandler();
    try {
      ExecutorService pool = installMockPool(handler);
      // Leave the scheduler stopped so queue capacity does not depend on thread timing.
      BlockingQueue<TransactionsMsgHandler.TrxEvent> smartQueue = new LinkedBlockingQueue<>(1);
      ReflectUtils.setFieldValue(handler, "smartContractQueue", smartQueue);
      PeerConnection peer = Mockito.mock(PeerConnection.class);
      Protocol.Transaction trx = TvmTestUtils.generateTriggerSmartContractAndGetTransaction(
          ByteArray.fromHexString("121212a9cf"), ByteArray.fromHexString("121212a9cf"),
          ByteArray.fromHexString("123456"), 100, 100000000, 0, 0);
      TransactionsMessage msg = new TransactionsMessage(Collections.singletonList(trx));
      stubAdvInvRequest(peer, msg);
      handler.processMessage(peer, msg);
      Assert.assertTrue(peer.getAdvInvRequest().isEmpty());
      Assert.assertEquals(1, smartQueue.size());
      TransactionsMsgHandler.TrxEvent queued = smartQueue.peek();
      Assert.assertSame(peer, queued.getPeer());
      Assert.assertEquals(new TransactionMessage(trx).getMessageId(),
          queued.getMsg().getMessageId());

      // A second requested transaction is dropped when the queue is already full.
      Protocol.Transaction second = trx.toBuilder().setRawData(trx.getRawData().toBuilder()
          .setTimestamp(1)).build();
      TransactionsMessage secondMsg = new TransactionsMessage(
          Collections.singletonList(second));
      stubAdvInvRequest(peer, secondMsg);
      handler.processMessage(peer, secondMsg);
      Assert.assertTrue(peer.getAdvInvRequest().isEmpty());
      Assert.assertEquals(1, smartQueue.size());
      Assert.assertSame(queued, smartQueue.peek());
      Mockito.verify(pool, Mockito.never()).submit(Mockito.any(Runnable.class));
    } finally {
      handler.close();
    }
  }

  @Test
  public void testTransactionWithoutContract() throws Exception {
    TransactionsMsgHandler handler = new TransactionsMsgHandler();
    try {
      ExecutorService pool = installMockPool(handler);
      PeerConnection peer = Mockito.mock(PeerConnection.class);
      Protocol.Transaction trx = Protocol.Transaction.newBuilder().setRawData(
          Protocol.Transaction.raw.newBuilder().setTimestamp(1).setRefBlockNum(1)).build();
      TransactionsMessage msg = new TransactionsMessage(Collections.singletonList(trx));
      Mockito.when(peer.getAdvInvRequest()).thenReturn(new ConcurrentHashMap<>());
      P2pException missingRequest = Assert.assertThrows(P2pException.class,
          () -> handler.processMessage(peer, msg));
      Assert.assertEquals(TypeEnum.BAD_MESSAGE, missingRequest.getType());

      stubAdvInvRequest(peer, msg);
      P2pException missingContract = Assert.assertThrows(P2pException.class,
          () -> handler.processMessage(peer, msg));
      Assert.assertEquals(TypeEnum.BAD_TRX, missingContract.getType());
      Assert.assertEquals(1, peer.getAdvInvRequest().size());
      Mockito.verify(pool, Mockito.never()).submit(Mockito.any(Runnable.class));
    } finally {
      handler.close();
    }
  }

  private ExecutorService installMockPool(TransactionsMsgHandler handler) throws Exception {
    ExecutorService original = (ExecutorService) ReflectUtils.getFieldObject(handler,
        "trxHandlePool");
    original.shutdown();
    Assert.assertTrue(original.awaitTermination(5, TimeUnit.SECONDS));
    ExecutorService pool = Mockito.mock(ExecutorService.class);
    Mockito.when(pool.awaitTermination(Mockito.anyLong(), Mockito.any())).thenReturn(true);
    ReflectUtils.setFieldValue(handler, "trxHandlePool", pool);
    return pool;
  }

  @Test
  public void testProcessMessageAfterClose() throws Exception {
    TransactionsMsgHandler handler = new TransactionsMsgHandler();
    handler.init();
    handler.close();

    PeerConnection peer = Mockito.mock(PeerConnection.class);
    TransactionsMessage msg = Mockito.mock(TransactionsMessage.class);

    handler.processMessage(peer, msg);

    Mockito.verify(msg, Mockito.never()).getTransactions();
    Mockito.verifyNoInteractions(peer);
  }

  @Test
  public void testRejectedExecution() throws Exception {
    TransactionsMsgHandler handler = new TransactionsMsgHandler();
    try {
      ExecutorService mockPool = installMockPool(handler);
      Mockito.when(mockPool.submit(Mockito.any(Runnable.class)))
          .thenThrow(new RejectedExecutionException("pool closed"));
      PeerConnection peer = Mockito.mock(PeerConnection.class);
      TransactionsMessage msg = buildTransferMessage(2);
      stubAdvInvRequest(peer, msg);
      // 2 transfer transactions, submit throws on the first → catch + break, only called once
      handler.processMessage(peer, msg);

      Mockito.verify(mockPool, Mockito.times(1)).submit(Mockito.any(Runnable.class));
    } finally {
      handler.close();
    }
  }

  @Test
  public void testCloseDuringProcessing() throws Exception {
    TransactionsMsgHandler handler = new TransactionsMsgHandler();
    try {
      Field closedField = TransactionsMsgHandler.class.getDeclaredField("isClosed");
      closedField.setAccessible(true);

      ExecutorService mockPool = installMockPool(handler);
      // on the first submit, flip isClosed to true so the second iteration breaks
      Mockito.when(mockPool.submit(Mockito.any(Runnable.class))).thenAnswer(inv -> {
        closedField.set(handler, true);
        return null;
      });
      PeerConnection peer = Mockito.mock(PeerConnection.class);
      TransactionsMessage msg = buildTransferMessage(2);
      stubAdvInvRequest(peer, msg);
      handler.processMessage(peer, msg);

      Mockito.verify(mockPool, Mockito.times(1)).submit(Mockito.any(Runnable.class));
    } finally {
      handler.close();
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
    TransactionsMsgHandler handler = new TransactionsMsgHandler();
    try {
      ExecutorService pool = installMockPool(handler);
      PeerConnection peer = Mockito.mock(PeerConnection.class);

      BalanceContract.TransferContract transferContract = BalanceContract.TransferContract
          .newBuilder()
          .setAmount(10)
          .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString("121212a9cf")))
          .setToAddress(ByteString.copyFrom(ByteArray.fromHexString("232323a9cf")))
          .build();

      // signature shorter than 65 bytes → BAD_TRX
      Protocol.Transaction shortSigTrx = Protocol.Transaction.newBuilder()
          .setRawData(Protocol.Transaction.raw.newBuilder()
              .addContract(Protocol.Transaction.Contract.newBuilder()
                  .setType(Protocol.Transaction.Contract.ContractType.TransferContract)
                  .setParameter(Any.pack(transferContract)).build())
              .build())
          .addSignature(ByteString.copyFrom(new byte[64]))
          .build();

      List<Protocol.Transaction> shortList = new ArrayList<>();
      shortList.add(shortSigTrx);
      stubAdvInvRequest(peer, new TransactionsMessage(shortList));
      P2pException shortEx = Assert.assertThrows(P2pException.class,
          () -> handler.processMessage(peer, new TransactionsMessage(shortList)));
      Assert.assertEquals(TypeEnum.BAD_TRX, shortEx.getType());

      // signature longer than 68 bytes → BAD_TRX
      Protocol.Transaction longSigTrx = Protocol.Transaction.newBuilder()
          .setRawData(Protocol.Transaction.raw.newBuilder()
              .setRefBlockNum(1)
              .addContract(Protocol.Transaction.Contract.newBuilder()
                  .setType(Protocol.Transaction.Contract.ContractType.TransferContract)
                  .setParameter(Any.pack(transferContract)).build())
              .build())
          .addSignature(ByteString.copyFrom(new byte[69]))
          .build();

      List<Protocol.Transaction> longList = new ArrayList<>();
      longList.add(longSigTrx);
      stubAdvInvRequest(peer, new TransactionsMessage(longList));
      P2pException longEx = Assert.assertThrows(P2pException.class,
          () -> handler.processMessage(peer, new TransactionsMessage(longList)));
      Assert.assertEquals(TypeEnum.BAD_TRX, longEx.getType());

      // exactly 65 bytes → passes the length check (no P2pException from check)
      Protocol.Transaction validSigTrx = Protocol.Transaction.newBuilder()
          .setRawData(Protocol.Transaction.raw.newBuilder()
              .setRefBlockNum(2)
              .addContract(Protocol.Transaction.Contract.newBuilder()
                  .setType(Protocol.Transaction.Contract.ContractType.TransferContract)
                  .setParameter(Any.pack(transferContract)).build())
              .build())
          .addSignature(ByteString.copyFrom(new byte[65]))
          .build();

      List<Protocol.Transaction> validList = new ArrayList<>();
      validList.add(validSigTrx);
      stubAdvInvRequest(peer, new TransactionsMessage(validList));
      handler.processMessage(peer, new TransactionsMessage(validList));

      // 68 bytes (upper bound) also passes the length check
      Protocol.Transaction paddedSigTrx = Protocol.Transaction.newBuilder()
          .setRawData(Protocol.Transaction.raw.newBuilder()
              .setRefBlockNum(3)
              .addContract(Protocol.Transaction.Contract.newBuilder()
                  .setType(Protocol.Transaction.Contract.ContractType.TransferContract)
                  .setParameter(Any.pack(transferContract)).build())
              .build())
          .addSignature(ByteString.copyFrom(new byte[68]))
          .build();

      List<Protocol.Transaction> paddedList = new ArrayList<>();
      paddedList.add(paddedSigTrx);
      stubAdvInvRequest(peer, new TransactionsMessage(paddedList));
      handler.processMessage(peer, new TransactionsMessage(paddedList));
      Mockito.verify(pool, Mockito.times(2)).submit(Mockito.any(Runnable.class));
    } finally {
      handler.close();
    }
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
