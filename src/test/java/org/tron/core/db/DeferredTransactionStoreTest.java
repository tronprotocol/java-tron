package org.tron.core.db;

import com.google.protobuf.ByteString;
import java.io.File;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.FileUtil;
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

public class DeferredTransactionStoreTest {
  private static String dbPath = "output_deferred_transaction_store_test";
  private static String dbDirectory = "db_deferred_transaction_store_test";
  private static String indexDirectory = "index_deferred_transaction_store_test";
  private static TronApplicationContext context;
  private static Manager dbManager;

  static {
    Args.setParam(
        new String[]{
            "--output-directory", dbPath,
            "--storage-db-directory", dbDirectory,
            "--storage-index-directory", indexDirectory,
            "-w"
        },
        Constant.TEST_CONF
    );
    context = new TronApplicationContext(DefaultConfig.class);
  }

  /**
   * Init data.
   */
  @BeforeClass
  public static void init() {
    dbManager = context.getBean(Manager.class);
  }

  @Test
  public void RemoveDeferredTransactionTest() {
    DeferredTransactionStore deferredTransactionStore = dbManager.getDeferredTransactionStore();
    DeferredTransactionIdIndexStore deferredTransactionIdIndexStore = dbManager.getDeferredTransactionIdIndexStore();
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
    deferredTransactionStore.put(deferredTransactionCapsule);
    deferredTransactionIdIndexStore.put(deferredTransactionCapsule);

    DeferredTransactionCapsule capsule =
        deferredTransactionStore.getByTransactionId(deferredTransactionCapsule.getTransactionId());
    Assert.assertNotNull(capsule);
    deferredTransactionStore.removeDeferredTransaction(deferredTransactionCapsule);
    capsule = deferredTransactionStore.getByTransactionId(deferredTransactionCapsule.getTransactionId());
    Assert.assertNull(capsule);
  }

  @Test
  public void GetScheduledTransactionsTest (){
    DeferredTransactionStore deferredTransactionStore = dbManager.getDeferredTransactionStore();
    DeferredTransactionIdIndexStore deferredTransactionIdIndexStore = dbManager.getDeferredTransactionIdIndexStore();
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
    deferredTransactionStore.put(
        new DeferredTransactionCapsule(deferredTransactionCapsule.getInstance()
            .toBuilder().setDelayUntil(System.currentTimeMillis() + 1000).build()));
    deferredTransactionIdIndexStore.put(deferredTransactionCapsule);
    Assert.assertNotNull(dbManager.getDeferredTransactionStore().getScheduledTransactions(System.currentTimeMillis()));
  }

  @Test
  public void GetScheduledTransactionsTest2 (){
    DeferredTransactionStore deferredTransactionStore = dbManager.getDeferredTransactionStore();
    DeferredTransactionIdIndexStore deferredTransactionIdIndexStore = dbManager.getDeferredTransactionIdIndexStore();

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

      deferredTransactionStore.put(new DeferredTransactionCapsule(deferredTransactionCapsule.getInstance().toBuilder().setDelayUntil(i).build()));
      deferredTransactionIdIndexStore.put(deferredTransactionCapsule);
    }
    // save in database with block number
    Assert.assertEquals(100, dbManager.getDeferredTransactionStore().getScheduledTransactions(99).size());
    Assert.assertEquals(500, dbManager.getDeferredTransactionStore().getScheduledTransactions(499).size());
    Assert.assertEquals(334, dbManager.getDeferredTransactionStore().getScheduledTransactions(333).size());
    Assert.assertEquals(178, dbManager.getDeferredTransactionStore().getScheduledTransactions(177).size());

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
    context.destroy();
    FileUtil.deleteDir(new File(dbPath));
  }
}
