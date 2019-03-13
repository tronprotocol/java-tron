package org.tron.core.db;

import static org.tron.core.config.Parameter.NodeConstant.MAX_TRANSACTION_PENDING;

import com.google.common.primitives.Longs;
import com.google.protobuf.ByteString;
import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
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
import org.tron.protos.Protocol.Transaction.raw.Builder;

public class DeferredTransactionCacheTest {
  private static String dbPath = "output_deferred_transactionCache_test";
  private static String dbDirectory = "db_deferred_transactionCache_test";
  private static String indexDirectory = "index_deferred_transactionCache_test";
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
    DeferredTransactionCache deferredTransactionCache = dbManager.getDeferredTransactionCache();
    DeferredTransactionIdIndexCache deferredTransactionIdIndexCache = dbManager.getDeferredTransactionIdIndexCache();
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
    DeferredTransactionCache deferredTransactionCache = dbManager.getDeferredTransactionCache();
    DeferredTransactionIdIndexCache deferredTransactionIdIndexCache = dbManager.getDeferredTransactionIdIndexCache();
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

    dbManager.getDeferredTransactionCache().getScheduledTransactions(System.currentTimeMillis());
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
