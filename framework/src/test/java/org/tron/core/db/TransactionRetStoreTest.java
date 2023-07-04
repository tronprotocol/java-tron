package org.tron.core.db;

import javax.annotation.Resource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.utils.ByteArray;
import org.tron.core.Constant;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.capsule.TransactionInfoCapsule;
import org.tron.core.capsule.TransactionRetCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.exception.BadItemException;
import org.tron.core.store.TransactionRetStore;
import org.tron.protos.Protocol.Transaction;

public class TransactionRetStoreTest extends BaseTest {

  private static final byte[] transactionId = TransactionStoreTest.randomBytes(32);
  private static final byte[] blockNum = ByteArray.fromLong(1);
  private static String dbDirectory = "db_TransactionRetStore_test";
  private static String indexDirectory = "index_TransactionRetStore_test";
  @Resource
  private TransactionRetStore transactionRetStore;
  private static Transaction transaction;
  @Resource
  private TransactionStore transactionStore;

  private static TransactionCapsule transactionCapsule;
  private static TransactionRetCapsule transactionRetCapsule;

  static {
    dbPath = "output_TransactionRetStore_test";
    Args.setParam(new String[]{"--output-directory", dbPath, "--storage-db-directory", dbDirectory,
        "--storage-index-directory", indexDirectory}, Constant.TEST_CONF);
  }

  @BeforeClass
  public static void init() {
    TransactionInfoCapsule transactionInfoCapsule = new TransactionInfoCapsule();

    transactionInfoCapsule.setId(transactionId);
    transactionInfoCapsule.setFee(1000L);
    transactionInfoCapsule.setBlockNumber(100L);
    transactionInfoCapsule.setBlockTimeStamp(200L);

    transactionRetCapsule = new TransactionRetCapsule();
    transactionRetCapsule.addTransactionInfo(transactionInfoCapsule.getInstance());

    transaction = Transaction.newBuilder().build();
    transactionCapsule = new TransactionCapsule(transaction);
    transactionCapsule.setBlockNum(1);

  }

  @Before
  public void before() {
    transactionRetStore.put(blockNum, transactionRetCapsule);
    transactionStore.put(transactionId, transactionCapsule);
  }

  @Test
  public void get() throws BadItemException {
    TransactionInfoCapsule resultCapsule = transactionRetStore.getTransactionInfo(transactionId);
    Assert.assertNotNull("get transaction ret store", resultCapsule);
  }

  @Test
  public void put() {
    TransactionInfoCapsule transactionInfoCapsule = new TransactionInfoCapsule();
    transactionInfoCapsule.setId(transactionId);
    transactionInfoCapsule.setFee(1000L);
    transactionInfoCapsule.setBlockNumber(100L);
    transactionInfoCapsule.setBlockTimeStamp(200L);

    TransactionRetCapsule transactionRetCapsule = new TransactionRetCapsule();
    transactionRetCapsule.addTransactionInfo(transactionInfoCapsule.getInstance());
    Assert.assertNull("put transaction info error",
        transactionRetStore.getUnchecked(transactionInfoCapsule.getId()));
    transactionRetStore.put(transactionInfoCapsule.getId(), transactionRetCapsule);
    Assert.assertNotNull("get transaction info error",
        transactionRetStore.getUnchecked(transactionInfoCapsule.getId()));
  }
}