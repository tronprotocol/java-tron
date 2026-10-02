package org.tron.core.db;

import javax.annotation.Resource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.TestConstants;
import org.tron.common.utils.ByteArray;
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
  @Resource
  private TransactionRetStore transactionRetStore;
  private static Transaction transaction;
  @Resource
  private TransactionStore transactionStore;

  private static TransactionCapsule transactionCapsule;
  private static TransactionRetCapsule transactionRetCapsule;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath(),
        "--storage-db-directory", dbDirectory}, TestConstants.TEST_CONF);
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
  public void getLowestBlockNum() {
    Assert.assertEquals(1L, transactionRetStore.getLowestBlockNum().getAsLong());
  }

  @Test
  public void getLowestBlockNumPicksMinimumKey() {
    transactionRetStore.put(ByteArray.fromLong(7), transactionRetCapsule);
    transactionRetStore.put(ByteArray.fromLong(3), transactionRetCapsule);
    try {
      Assert.assertEquals(1L, transactionRetStore.getLowestBlockNum().getAsLong());
      transactionRetStore.delete(blockNum);
      Assert.assertEquals(3L, transactionRetStore.getLowestBlockNum().getAsLong());
    } finally {
      transactionRetStore.delete(ByteArray.fromLong(3));
      transactionRetStore.delete(ByteArray.fromLong(7));
    }
  }

  @Test
  public void getLowestBlockNumOnEmptyStore() {
    transactionRetStore.delete(blockNum);
    Assert.assertFalse(transactionRetStore.getLowestBlockNum().isPresent());
  }

  @Test
  public void initLowestBlockNumOfReceiptStoreReadsStore() {
    // the init runs after checkpoint recovery, so it must reflect whatever the store holds
    // at call time: the first key while present, the next block once the store is empty
    chainBaseManager.initLowestBlockNumOfReceiptStore();
    Assert.assertEquals(1L, chainBaseManager.getLowestBlockNumOfReceiptStore());

    // head must be non-zero, otherwise head + 1 collides with the first key asserted above
    transactionRetStore.delete(blockNum);
    chainBaseManager.getDynamicPropertiesStore().saveLatestBlockHeaderNumber(5);
    try {
      chainBaseManager.initLowestBlockNumOfReceiptStore();
      Assert.assertEquals(6L, chainBaseManager.getLowestBlockNumOfReceiptStore());
    } finally {
      chainBaseManager.getDynamicPropertiesStore().saveLatestBlockHeaderNumber(0);
    }
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
    try {
      Assert.assertNotNull("get transaction info error",
          transactionRetStore.getUnchecked(transactionInfoCapsule.getId()));
    } finally {
      // a 32-byte key left behind breaks getNext's fixed-length key comparison
      transactionRetStore.delete(transactionInfoCapsule.getId());
    }
  }
}