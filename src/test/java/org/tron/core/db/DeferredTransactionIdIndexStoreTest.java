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
import org.tron.protos.Protocol.DeferredTransaction;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.Transaction.raw.Builder;

public class DeferredTransactionIdIndexStoreTest {
  private static String dbPath = "output_deferred_transactionIdIndexStore_test";
  private static String dbDirectory = "db_deferred_transactionIdIndexStore_test";
  private static String indexDirectory = "index_deferred_transactionIdIndexStore_test";
  private static DeferredTransactionIdIndexStore deferredTransactionIdIndexStore;
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
    deferredTransactionIdIndexStore = dbManager.getDeferredTransactionIdIndexStore();
  }

  @Test
  public void RemoveDeferredTransactionIdIndexTest() {
    final DeferredTransactionIdIndexStore deferredTransactionIdIndexStore = dbManager.getDeferredTransactionIdIndexStore();

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
    deferredTransactionIdIndexStore.put(deferredTransactionCapsule);

    Assert.assertNotNull("remove deferred transacion id index",
        deferredTransactionIdIndexStore.getDeferredTransactionKeyById(deferredTransactionCapsule.getTransactionId()));

    deferredTransactionIdIndexStore.removeDeferredTransactionIdIndex(deferredTransactionCapsule.getTransactionId());

    Assert.assertNull("remove deferred transacion id index",
        deferredTransactionIdIndexStore.getDeferredTransactionKeyById(deferredTransactionCapsule.getTransactionId()));
  }

  @Test
  public void GetDeferredTransactionIdIndexTest() {
    final DeferredTransactionIdIndexStore deferredTransactionIdIndexStore = dbManager.getDeferredTransactionIdIndexStore();

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
    deferredTransactionIdIndexStore.put(deferredTransactionCapsule);

    Assert.assertNotNull("Get deferred transacion id index",
        deferredTransactionIdIndexStore.getDeferredTransactionKeyById(deferredTransactionCapsule.getTransactionId()));
  }

  private static DeferredTransaction buildDeferredTransaction(Transaction transaction) {
    Builder rawData = transaction.getRawData().toBuilder().setDelaySeconds(86400);
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
