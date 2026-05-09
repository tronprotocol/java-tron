package org.tron.core.net.messagehandler;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;

import lombok.Getter;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
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
  public void testProcessMessage() {
    TransactionsMsgHandler transactionsMsgHandler = new TransactionsMsgHandler();
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

      long transactionTimestamp = DateTime.now().minusDays(4).getMillis();
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
      Mockito.when(peer.getAdvInvRequest()).thenReturn(advInvRequest);

      List<Protocol.Transaction> transactionList = new ArrayList<>();
      transactionList.add(trx);
      transactionsMsgHandler.processMessage(peer, new TransactionsMessage(transactionList));
      Assert.assertNull(advInvRequest.get(item));
      //Thread.sleep(10);
      BlockingQueue<TrxEvent> smartContractQueue =
          new LinkedBlockingQueue(2);
      smartContractQueue.offer(new TrxEvent(null, null));
      smartContractQueue.offer(new TrxEvent(null, null));
      Field field1 = TransactionsMsgHandler.class.getDeclaredField("smartContractQueue");
      field1.setAccessible(true);
      field1.set(transactionsMsgHandler, smartContractQueue);
      Protocol.Transaction trx1 = TvmTestUtils.generateTriggerSmartContractAndGetTransaction(
          ByteArray.fromHexString("121212a9cf"),
          ByteArray.fromHexString("121212a9cf"),
          ByteArray.fromHexString("123456"),
          100, 100000000, 0, 0);
      Map<Item, Long> advInvRequest1 = new ConcurrentHashMap<>();
      Item item1 = new Item(new TransactionMessage(trx1).getMessageId(),
          Protocol.Inventory.InventoryType.TRX);
      advInvRequest1.put(item1, 0L);
      Mockito.when(peer.getAdvInvRequest()).thenReturn(advInvRequest1);
      List<Protocol.Transaction> transactionList1 = new ArrayList<>();
      transactionList1.add(trx1);
      transactionsMsgHandler.processMessage(peer, new TransactionsMessage(transactionList1));
      Assert.assertNull(advInvRequest.get(item1));

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
      Assert.fail();
    } finally {
      transactionsMsgHandler.close();
    }
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
      ExecutorService mockPool = Mockito.mock(ExecutorService.class);
      Mockito.when(mockPool.submit(Mockito.any(Runnable.class)))
          .thenThrow(new RejectedExecutionException("pool closed"));
      Field poolField = TransactionsMsgHandler.class.getDeclaredField("trxHandlePool");
      poolField.setAccessible(true);
      poolField.set(handler, mockPool);

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

      ExecutorService mockPool = Mockito.mock(ExecutorService.class);
      // on the first submit, flip isClosed to true so the second iteration breaks
      Mockito.when(mockPool.submit(Mockito.any(Runnable.class))).thenAnswer(inv -> {
        closedField.set(handler, true);
        return null;
      });
      Field poolField = TransactionsMsgHandler.class.getDeclaredField("trxHandlePool");
      poolField.setAccessible(true);
      poolField.set(handler, mockPool);

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
  public void testIsBusyWithCachedTransactions() throws Exception {
    TransactionsMsgHandler handler = new TransactionsMsgHandler();

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
  }

  class TrxEvent {

    @Getter
    private PeerConnection peer;
    @Getter
    private TransactionMessage msg;
    @Getter
    private long time;

    public TrxEvent(PeerConnection peer, TransactionMessage msg) {
      this.peer = peer;
      this.msg = msg;
      this.time = System.currentTimeMillis();
    }
  }
}
