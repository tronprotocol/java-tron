package org.tron.core.db;

import com.google.protobuf.ByteString;
import java.io.File;
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
    //test get and has Method
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
}