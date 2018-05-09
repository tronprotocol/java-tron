package org.tron.core.db;

import com.google.protobuf.ByteString;
import java.io.File;
import java.util.Random;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.tron.common.utils.FileUtil;
import org.tron.core.Constant;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.protos.Protocol.AccountType;

public class AccountIndexStoreTest {

  private static String dbPath = "output_AccountIndexStore_test";
  private static AnnotationConfigApplicationContext context;
  private static AccountIndexStore accountIndexStore;
  private static final byte[] ACCOUNT_ADDRESS_ONE = randomBytes(16);
  private static final byte[] ACCOUNT_ADDRESS_TWO = randomBytes(16);
  private static final byte[] ACCOUNT_ADDRESS_THREE = randomBytes(16);
  private static final byte[] ACCOUNT_ADDRESS_FOUR = randomBytes(16);
  private static final byte[] ACCOUNT_NAME_ONE = randomBytes(6);
  private static final byte[] ACCOUNT_NAME_TWO = randomBytes(6);
  private static final byte[] ACCOUNT_NAME_THREE = randomBytes(6);
  private static final byte[] ACCOUNT_NAME_FOUR = randomBytes(6);
  private static final byte[] ACCOUNT_NAME_FIVE = randomBytes(6);
  private static AccountCapsule accountCapsule1;
  private static AccountCapsule accountCapsule2;
  private static AccountCapsule accountCapsule3;
  private static AccountCapsule accountCapsule4;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath},
        Constant.TEST_CONF);
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
    accountIndexStore = context.getBean(AccountIndexStore.class);
    accountCapsule1 = new AccountCapsule(ByteString.copyFrom(ACCOUNT_ADDRESS_ONE),
        ByteString.copyFrom(ACCOUNT_NAME_ONE), AccountType.Normal);
    accountCapsule2 = new AccountCapsule(ByteString.copyFrom(ACCOUNT_ADDRESS_TWO),
        ByteString.copyFrom(ACCOUNT_NAME_TWO), AccountType.Normal);
    accountCapsule3 = new AccountCapsule(ByteString.copyFrom(ACCOUNT_ADDRESS_THREE),
        ByteString.copyFrom(ACCOUNT_NAME_THREE), AccountType.Normal);
    accountCapsule4 = new AccountCapsule(ByteString.copyFrom(ACCOUNT_ADDRESS_FOUR),
        ByteString.copyFrom(ACCOUNT_NAME_FOUR), AccountType.Normal);
    accountIndexStore.put(accountCapsule1);
    accountIndexStore.put(accountCapsule2);
    accountIndexStore.put(accountCapsule3);
    accountIndexStore.put(accountCapsule4);
  }

  @Test
  public void putAndGet() {
    byte[] address = accountIndexStore.get(ByteString.copyFrom(ACCOUNT_NAME_ONE));
    Assert.assertArrayEquals("putAndGet1", address, ACCOUNT_ADDRESS_ONE);
    address = accountIndexStore.get(ByteString.copyFrom(ACCOUNT_NAME_TWO));
    Assert.assertArrayEquals("putAndGet2", address, ACCOUNT_ADDRESS_TWO);
    address = accountIndexStore.get(ByteString.copyFrom(ACCOUNT_NAME_THREE));
    Assert.assertArrayEquals("putAndGet3", address, ACCOUNT_ADDRESS_THREE);
    address = accountIndexStore.get(ByteString.copyFrom(ACCOUNT_NAME_FOUR));
    Assert.assertArrayEquals("putAndGet4", address, ACCOUNT_ADDRESS_FOUR);
    address = accountIndexStore.get(ByteString.copyFrom(ACCOUNT_NAME_FIVE));
    Assert.assertNull("putAndGet4", address);

  }

  @Test
  public void putAndHas() {
    Boolean result = accountIndexStore.has(ACCOUNT_NAME_ONE);
    Assert.assertTrue("putAndGet1", result);
    result = accountIndexStore.has(ACCOUNT_NAME_TWO);
    Assert.assertTrue("putAndGet2", result);
    result = accountIndexStore.has(ACCOUNT_NAME_THREE);
    Assert.assertTrue("putAndGet3", result);
    result = accountIndexStore.has(ACCOUNT_NAME_FOUR);
    Assert.assertTrue("putAndGet4", result);
    result = accountIndexStore.has(ACCOUNT_NAME_FIVE);
    Assert.assertFalse("putAndGet4", result);
  }

  public static byte[] randomBytes(int length) {
    // generate the random number
    byte[] result = new byte[length];
    new Random().nextBytes(result);
    result[0] = Constant.ADD_PRE_FIX_BYTE_TESTNET;
    return result;
  }
}