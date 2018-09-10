package org.tron.core.db;

import com.google.protobuf.ByteString;

import java.io.File;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.core.Constant;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.protos.Protocol.AccountType;

public class AccountStoreTest {

  private static String dbPath = "output_AccountStore_test";
  private static String dbDirectory = "db_AccountStore_test";
  private static String indexDirectory = "index_AccountStore_test";
  private static TronApplicationContext context;
  private static AccountStore accountStore;
  private static final byte[] data = TransactionStoreTest.randomBytes(32);
  private static byte[] address = TransactionStoreTest.randomBytes(32);
  private static byte[] accountName = TransactionStoreTest.randomBytes(32);

  static {
    Args.setParam(
        new String[]{
            "--output-directory", dbPath,
            "--storage-db-directory", dbDirectory,
            "--storage-index-directory", indexDirectory
        },
        Constant.TEST_CONF
    );
    context = new TronApplicationContext(DefaultConfig.class);
  }

  @AfterClass
  public static void destroy() {
    Args.clearParam();
    context.destroy();
    FileUtil.deleteDir(new File(dbPath));
  }

  @BeforeClass
  public static void init() {
    accountStore = context.getBean(AccountStore.class);
    AccountCapsule accountCapsule = new AccountCapsule(ByteString.copyFrom(address),
        ByteString.copyFrom(accountName),
        AccountType.forNumber(1));
    accountStore.put(data, accountCapsule);
  }

  @Test
  public void get() {
    //test get and has Method
    Assert
        .assertEquals(ByteArray.toHexString(address), ByteArray
            .toHexString(accountStore.get(data).getInstance().getAddress().toByteArray()))
    ;
    Assert
        .assertEquals(ByteArray.toHexString(accountName), ByteArray
            .toHexString(accountStore.get(data).getInstance().getAccountName().toByteArray()))
    ;
    Assert.assertTrue(accountStore.has(data));
  }
}