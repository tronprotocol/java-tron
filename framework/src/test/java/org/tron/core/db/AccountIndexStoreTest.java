package org.tron.core.db;

import com.google.protobuf.ByteString;
import javax.annotation.Resource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.utils.ByteArray;
import org.tron.core.Constant;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.store.AccountIndexStore;
import org.tron.protos.Protocol.AccountType;

public class AccountIndexStoreTest extends BaseTest {

  private static String dbDirectory = "db_AccountIndexStore_test";
  private static String indexDirectory = "index_AccountIndexStore_test";
  @Resource
  private AccountIndexStore accountIndexStore;
  private static byte[] address = TransactionStoreTest.randomBytes(32);
  private static byte[] accountName = TransactionStoreTest.randomBytes(32);

  static {
    dbPath = "output_AccountIndexStore_test";
    Args.setParam(
        new String[]{
            "--output-directory", dbPath,
            "--storage-db-directory", dbDirectory,
            "--storage-index-directory", indexDirectory
        },
        Constant.TEST_CONF
    );
  }

  @Before
  public void init() {
    AccountCapsule accountCapsule = new AccountCapsule(ByteString.copyFrom(address),
        ByteString.copyFrom(accountName),
        AccountType.forNumber(1));
    accountIndexStore.put(accountCapsule);
  }

  @Test
  public void get() {
    //test get(ByteString name)
    Assert
        .assertEquals(ByteArray.toHexString(address), ByteArray
            .toHexString(accountIndexStore.get(ByteString.copyFrom(accountName))))
    ;
    //test get(byte[] key)
    Assert
        .assertEquals(ByteArray.toHexString(address), ByteArray
            .toHexString(accountIndexStore.get(accountName).getData()))
    ;
    Assert.assertTrue(accountIndexStore.has(accountName));
  }
}