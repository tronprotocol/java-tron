package org.tron.core.db;

import com.google.protobuf.ByteString;
import java.io.File;
import java.util.Random;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.core.Constant;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.config.Configuration;
import org.tron.core.config.args.Args;
import org.tron.protos.Protocol.AccountType;

public class AccountStoreTest {

  private static String dbPath = "output_AccountStore_test";
  private static AccountStore AccountStoreTest;
  private static final byte[] data = TransactionStoreTest.randomBytes(32);
  private static byte[] address = TransactionStoreTest.randomBytes(32);
  private static byte[] accountName = TransactionStoreTest.randomBytes(32);
  private static final String ACCOUNT_SUN_ADDRESS
      = "4948c2e8a756d9437037dcd8c7e0c73d560ca38d";
  private static final String ACCOUNT_BLACKHOLE_ADDRESS
      = "548794500882809695a8a687866e76d4271a146a";
  private static final String ACCOUNT_ZION_ADDRESS
      = "55ddae14564f82d5b94c7a131b5fcfd31ad6515a";

  @AfterClass
  public static void destroy() {
    Args.clearParam();
    FileUtil.deleteDir(new File(dbPath));
  }

  @BeforeClass
  public static void init() {
    Args.setParam(new String[]{"-d", dbPath, "-w"},
        Configuration.getByPath(Constant.TEST_CONF));
    AccountStoreTest = AccountStore.create(dbPath);
    AccountCapsule accountCapsule = new AccountCapsule(ByteString.copyFrom(address),
        ByteString.copyFrom(accountName),
        AccountType.forNumber(1));
    AccountStoreTest.put(data, accountCapsule);
  }


  @Test
  public void get() {
    //test get Method
    Assert
        .assertEquals(ByteArray.toHexString(address), ByteArray
            .toHexString(AccountStoreTest.get(data).getInstance().getAddress().toByteArray()))
    ;
    Assert
        .assertEquals(ByteArray.toHexString(accountName), ByteArray
            .toHexString(AccountStoreTest.get(data).getInstance().getAccountName().toByteArray()))
    ;
    Assert.assertTrue(AccountStoreTest.has(data));
  }

  @Test
  public void getSunBlackZion() {
    //test getSun,getBlackhole and getZion Method
    AccountCapsule accountCapsuleSun = new AccountCapsule(ByteString.copyFrom(address),
        ByteString.copyFrom(accountName),
        AccountType.forNumber(1));
    AccountStoreTest.put(ByteString.copyFrom(ByteArray.fromHexString(ACCOUNT_SUN_ADDRESS))
        .toByteArray(), accountCapsuleSun);
    AccountCapsule accountCapsuleBlack = accountCapsuleSun;

    Assert.assertEquals(accountCapsuleSun.getInstance(), AccountStoreTest.getSun().getInstance());

    AccountStoreTest.put(ByteString.copyFrom(ByteArray.fromHexString(ACCOUNT_BLACKHOLE_ADDRESS))
        .toByteArray(), accountCapsuleBlack);

    Assert.assertEquals(accountCapsuleBlack.getInstance(),
        AccountStoreTest.getBlackhole().getInstance());

    AccountCapsule accountCapsuleZion = accountCapsuleSun;

    AccountStoreTest.put(ByteString.copyFrom(ByteArray.fromHexString(ACCOUNT_ZION_ADDRESS))
        .toByteArray(), accountCapsuleZion);

    Assert.assertEquals(accountCapsuleZion.getInstance(),
        AccountStoreTest.getZion().getInstance());
  }


  public static byte[] randomBytes(int length) {
    //generate the random number
    byte[] result = new byte[length];
    new Random().nextBytes(result);
    return result;
  }
}