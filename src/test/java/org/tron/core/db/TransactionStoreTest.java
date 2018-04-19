package org.tron.core.db;

import com.google.protobuf.InvalidProtocolBufferException;
import java.io.File;
import java.util.Random;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.tron.common.utils.FileUtil;
import org.tron.core.Constant;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.protos.Contract.TransferContract;

public class TransactionStoreTest {

  private static String dbPath = "output_TransactionStore_test";
  private static TransactionStore transactionStore;
  private static AnnotationConfigApplicationContext context;
  private static final byte[] data = TransactionStoreTest.randomBytes(21);
  private static final byte[] key = TransactionStoreTest.randomBytes(21);
  private static final long value = 111L;

  static {
    Args.setParam(new String[] {"-d", dbPath, "-w"}, Constant.TEST_CONF);
    context = new AnnotationConfigApplicationContext(DefaultConfig.class);
  }

  @AfterClass
  public static void destroy() {
    Args.clearParam();
    FileUtil.deleteDir(new File(dbPath));
    context.destroy();
  }

  @BeforeClass
  public static void init() {
    transactionStore = context.getBean(TransactionStore.class);
    TransactionCapsule transactionCapsule = new TransactionCapsule(key, value);
    transactionStore.put(data, transactionCapsule);
  }

  @Test
  public void testGet() {
    // test get and has method
    try {
      Assert.assertTrue(transactionStore.has(data));
      Assert.assertArrayEquals(
          key,
          transactionStore
              .get(data)
              .getInstance()
              .getRawData()
              .getContractList()
              .get(0)
              .getParameter()
              .unpack(TransferContract.class)
              .getToAddress()
              .toByteArray());
      Assert.assertEquals(
          value,
          transactionStore
              .get(data)
              .getInstance()
              .getRawData()
              .getContractList()
              .get(0)
              .getParameter()
              .unpack(TransferContract.class)
              .getAmount());
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
    }
  }

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
    // generate the random number
    byte[] result = new byte[length];
    new Random().nextBytes(result);
    result[0] = Constant.ADD_PRE_FIX_BYTE_TESTNET;
    return result;
  }
}
