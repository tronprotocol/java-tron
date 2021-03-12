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
import org.tron.core.capsule.AccountAssetIssueCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.store.AccountAssetIssueStore;

public class AccountAssetIssueStoreTest {

  private static final byte[] data = TransactionStoreTest.randomBytes(32);
  private static String dbPath = "output_AccountAssetIssueStore_test";
  private static String dbDirectory = "db_AccountAssetIssueStore_test";
  private static String indexDirectory = "index_AccountAssetIssueStore_test";
  private static TronApplicationContext context;
  private static AccountAssetIssueStore accountAssetIssueStore;
  private static byte[] address = TransactionStoreTest.randomBytes(32);
  private static byte[] accountAssetIssuetName = TransactionStoreTest.randomBytes(32);

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

  @BeforeClass
  public static void init() {
    accountAssetIssueStore = context.getBean(AccountAssetIssueStore.class);
    AccountAssetIssueCapsule accountCapsule = new AccountAssetIssueCapsule(
                    ByteString.copyFrom(accountAssetIssuetName),
                    ByteString.copyFrom(address)
            );
    accountAssetIssueStore.put(data, accountCapsule);
  }

  @AfterClass
  public static void destroy() {
    Args.clearParam();
    context.destroy();
    FileUtil.deleteDir(new File(dbPath));
  }

  @Test
  public void get() {
    //test get and has Method
    Assert
            .assertEquals(ByteArray.toHexString(address), ByteArray
                    .toHexString(accountAssetIssueStore.get(data)
                            .getInstance().getAddress().toByteArray()))
    ;
    Assert
            .assertEquals(ByteArray.toHexString(accountAssetIssuetName), ByteArray
                    .toHexString(accountAssetIssueStore.get(data)
                            .getInstance().getAssetIssuedName().toByteArray()))
    ;
    Assert.assertTrue(accountAssetIssueStore.has(data));
  }
}
