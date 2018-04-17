package org.tron.core.db;

import java.io.File;
import java.util.Random;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.tron.common.utils.FileUtil;
import org.tron.core.Constant;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.args.Args;


public class TransactionStoreTest {

  private static String dbPath = "output_TransactionStore_test";
  private static TransactionStore transactionStore;
  private static final byte[] key = TransactionStoreTest.randomBytes(32);
  private static final long value = 111L;

  @AfterClass
  public static void destroy() {
    Args.clearParam();
    FileUtil.deleteDir(new File(dbPath));
    TransactionStore.destory();
  }

  @BeforeClass
  public static void init() throws Exception {
    Args.setParam(new String[]{"-d", dbPath, "-w"},
        Constant.TEST_CONF);
    transactionStore = TransactionStore.create(dbPath);
    TransactionCapsule transactionCapsule = new TransactionCapsule(key, value);
    transactionStore.put(key, transactionCapsule);
  }

  /*@Test
  public void testGet() {
    //test get and has method
    Assert.assertTrue(transactionStore.has(data));
    Assert.assertEquals(key, ByteArray.toHexString(
        transactionStore.get(data).getInstance().getRawData().getVoutList().get(0).getPubKeyHash()
            .toByteArray()));
    Assert.assertEquals(value,
        transactionStore.get(data).getInstance().getRawData().getVoutList().get(0).getValue());
  } */

  /*@Test
  public void findTransactionByHash() {
    //test findTransactionByHash method
    TransactionCapsule transactionCapsuleByHash = new TransactionCapsule(
        transactionStore.findTransactionByHash(data));

    Assert.assertEquals(key, ByteArray.toHexString(
        transactionCapsuleByHash.getInstance().getRawData().getVoutList().get(0).getPubKeyHash()
            .toByteArray()));
    Assert.assertEquals(value,
        transactionCapsuleByHash.getInstance().getRawData().getVoutList().get(0).getValue());

  } */

  public static byte[] randomBytes(int length) {
    //generate the random number
    byte[] result = new byte[length];
    new Random().nextBytes(result);
    return result;
  }
}