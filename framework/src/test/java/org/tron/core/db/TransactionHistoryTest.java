package org.tron.core.db;

import javax.annotation.Resource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.utils.ByteArray;
import org.tron.core.Constant;
import org.tron.core.capsule.TransactionInfoCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.exception.BadItemException;
import org.tron.core.store.TransactionHistoryStore;

public class TransactionHistoryTest extends BaseTest {

  private static final byte[] transactionId = TransactionStoreTest.randomBytes(32);
  private static String dbDirectory = "db_TransactionHistoryStore_test";
  private static String indexDirectory = "index_TransactionHistoryStore_test";
  @Resource
  private TransactionHistoryStore transactionHistoryStore;

  private static TransactionInfoCapsule transactionInfoCapsule;

  static {
    dbPath = "output_TransactionHistoryStore_test";
    Args.setParam(
        new String[]{
            "--output-directory", dbPath,
            "--storage-db-directory", dbDirectory,
            "--storage-index-directory", indexDirectory
        },
        Constant.TEST_CONF
    );
  }

  @BeforeClass
  public static void init() {
    transactionInfoCapsule = new TransactionInfoCapsule();
    transactionInfoCapsule.setId(transactionId);
    transactionInfoCapsule.setFee(1000L);
    transactionInfoCapsule.setBlockNumber(100L);
    transactionInfoCapsule.setBlockTimeStamp(200L);

  }

  @Before
  public void before() {
    transactionHistoryStore.put(transactionId, transactionInfoCapsule);
  }

  @Test
  public void get() throws BadItemException {
    //test get and has Method
    TransactionInfoCapsule resultCapsule = transactionHistoryStore.get(transactionId);
    Assert.assertEquals(1000L, resultCapsule.getFee());
    Assert.assertEquals(100L, resultCapsule.getBlockNumber());
    Assert.assertEquals(200L, resultCapsule.getBlockTimeStamp());
    Assert.assertEquals(ByteArray.toHexString(transactionId),
        ByteArray.toHexString(resultCapsule.getId()));
  }
}