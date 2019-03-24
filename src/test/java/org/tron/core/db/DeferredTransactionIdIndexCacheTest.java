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
import org.tron.core.config.args.Args;
import org.tron.protos.Contract.TransferContract;
import org.tron.protos.Protocol.DeferredStage;
import org.tron.protos.Protocol.DeferredTransaction;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;

public class DeferredTransactionIdIndexCacheTest {
  private static String dbPath = "output_deferred_transactionIdIndexCache_test";
  private static DeferredTransactionCache deferredTransactionCache;
  private static DeferredTransactionIdIndexCache deferredTransactionIdIndexCache;

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
  public void RemoveDeferredTransactionIdIndexTest() {
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
    deferredTransactionIdIndexCache.put(deferredTransactionCapsule);
    deferredTransactionIdIndexCache.removeDeferredTransactionIdIndex(deferredTransactionCapsule.getTransactionId());
    Assert.assertNull(deferredTransactionIdIndexCache.getDeferredTransactionKeyById(deferredTransactionCapsule.getTransactionId()));
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
