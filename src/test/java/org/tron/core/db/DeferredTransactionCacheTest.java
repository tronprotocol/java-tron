package org.tron.core.db;

import com.google.protobuf.ByteString;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.tron.core.Constant;
import org.tron.core.capsule.DeferredTransactionCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.protos.Contract.TransferContract;
import org.tron.protos.Protocol.DeferredStage;
import org.tron.protos.Protocol.DeferredTransaction;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;

public class DeferredTransactionCacheTest {
  private static DeferredTransactionCache deferredTransactionCache;
  private static DeferredTransactionIdIndexCache deferredTransactionIdIndexCache;
  private static final String dbPath = "output-deferred-transaction-cache-test";

  /**
   * Init data.
   */
  @BeforeClass
  public static void init() {
    Args.setParam(new String[]{"--output-directory", dbPath},
        Constant.TEST_CONF);
    deferredTransactionCache = new DeferredTransactionCache("deferred-transaction-cache");
    deferredTransactionIdIndexCache = new DeferredTransactionIdIndexCache("deferred-transactionid-index-cache");
    deferredTransactionCache.setDeferredTransactionIdIndexCache(deferredTransactionIdIndexCache);
  }

  @Test
  public void RemoveDeferredTransactionTest() {
    deferredTransactionCache.reset();
    // save in database with block number
    TransferContract tc =
        TransferContract.newBuilder()
            .setAmount(10)
            .setOwnerAddress(ByteString.copyFromUtf8("aaa"))
            .setToAddress(ByteString.copyFromUtf8("bbb"))
            .build();
    TransactionCapsule trx = new TransactionCapsule(tc, ContractType.TransferContract);
    DeferredTransactionCapsule deferredTransactionCapsule = new DeferredTransactionCapsule(
        buildDeferredTransaction(trx.getInstance()));
    deferredTransactionCache.put(deferredTransactionCapsule);
    deferredTransactionIdIndexCache.put(deferredTransactionCapsule);

    DeferredTransactionCapsule capsule =
        deferredTransactionCache.getByTransactionId(deferredTransactionCapsule.getTransactionId());
    Assert.assertNotNull(capsule);
    deferredTransactionCache.removeDeferredTransaction(deferredTransactionCapsule);
    capsule = deferredTransactionCache.getByTransactionId(deferredTransactionCapsule.getTransactionId());
    Assert.assertNull(capsule);
  }

  @Test
  public void GetScheduledTransactionsTest (){
    deferredTransactionCache.reset();

    // save in database with block number
    TransferContract tc =
        TransferContract.newBuilder()
            .setAmount(10)
            .setOwnerAddress(ByteString.copyFromUtf8("aaa"))
            .setToAddress(ByteString.copyFromUtf8("bbb"))
            .build();
    TransactionCapsule trx = new TransactionCapsule(tc, ContractType.TransferContract);
    DeferredTransactionCapsule deferredTransactionCapsule = new DeferredTransactionCapsule(
        buildDeferredTransaction(trx.getInstance()));
    deferredTransactionCache.put(
        new DeferredTransactionCapsule(deferredTransactionCapsule.getInstance()
            .toBuilder().setDelayUntil(System.currentTimeMillis() + 1000).build()));
    deferredTransactionIdIndexCache.put(deferredTransactionCapsule);
    Assert.assertNotNull(deferredTransactionCache.getScheduledTransactions(System.currentTimeMillis()));
  }

  @Test
  public void GetScheduledTransactionsTest2 (){
    deferredTransactionCache.reset();
    for (int i = 999; i >= 0; i --) {
      TransferContract tc =
          TransferContract.newBuilder()
              .setAmount(i)
              .setOwnerAddress(ByteString.copyFromUtf8("aaa"))
              .setToAddress(ByteString.copyFromUtf8("bbb"))
              .build();
      TransactionCapsule trx = new TransactionCapsule(tc, ContractType.TransferContract);
      DeferredTransactionCapsule deferredTransactionCapsule = new DeferredTransactionCapsule(
          buildDeferredTransaction(trx.getInstance()));

      deferredTransactionCache.put(new DeferredTransactionCapsule(deferredTransactionCapsule.getInstance().toBuilder().setDelayUntil(i).build()));
      deferredTransactionIdIndexCache.put(deferredTransactionCapsule);
    }
    // save in database with block number
    Assert.assertEquals(100, deferredTransactionCache.getScheduledTransactions(99).size());
    Assert.assertEquals(500, deferredTransactionCache.getScheduledTransactions(499).size());
    Assert.assertEquals(334, deferredTransactionCache.getScheduledTransactions(333).size());
    Assert.assertEquals(178, deferredTransactionCache.getScheduledTransactions(177).size());

  }

  private static DeferredTransaction buildDeferredTransaction(Transaction transaction) {
    DeferredStage deferredStage = transaction.getRawData().toBuilder().getDeferredStage().toBuilder()
        .setDelaySeconds(86400).build();
    Transaction.raw rawData = transaction.toBuilder().getRawData().toBuilder().setDeferredStage(deferredStage).build();
    transaction = transaction.toBuilder().setRawData(rawData).build();
    DeferredTransaction.Builder deferredTransaction = DeferredTransaction.newBuilder();
    TransactionCapsule transactionCapsule = new TransactionCapsule(transaction);
    deferredTransaction.setTransactionId(transactionCapsule.getTransactionId().getByteString());
    deferredTransaction.setDelaySeconds(transactionCapsule.getDeferredSeconds());
    deferredTransaction.setDelayUntil(System.currentTimeMillis() + 100);
    deferredTransaction.setTransaction(transactionCapsule.getInstance());
    return deferredTransaction.build();
  }

  @AfterClass
  public static void destroy() {
    Args.clearParam();
  }
}
