package org.tron.core.db;

import java.io.File;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.core.Constant;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.keystore.Wallet;

public class TxCacheDBTest {
  private static final String dbPath = "output_TransactionCache_test";

  private static TronApplicationContext context;
  private static Manager dbManager;

  /**
   * Init data.
   */
  @BeforeClass
  public static void init() {
    String dbDirectory = "db_TransactionCache_test";
    String indexDirectory = "index_TransactionCache_test";
    Args.setParam(new String[]{"--output-directory", dbPath, "--storage-db-directory",
        dbDirectory, "--storage-index-directory", indexDirectory, "-w"}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    Application appT = ApplicationFactory.create(context);
    dbManager = context.getBean(Manager.class);
  }

  /**
   * release resources.
   */
  @AfterClass
  public static void destroy() {
    Args.clearParam();
    context.destroy();
    FileUtil.deleteDir(new File(dbPath));
  }

  @Test
  public void putTransactionTest() {
    TransactionCache db = dbManager.getTransactionCache();
    byte[][] hash = new byte[140000][64];
    for (int i = 1; i < 140000; i++) {
      hash[i] = Wallet.generateRandomBytes(64);
      db.put(hash[i], new BytesCapsule(ByteArray.fromLong(i)));
    }
    // [1,65537] are expired
    for (int i = 1; i < 65538; i++) {
      try {
        Assert.assertFalse("index = " + i, db.has(hash[i]));
      } catch (Exception e) {
        Assert.fail("transaction should be expired index = " + i);
      }
    }
    // [65538,140000] are in cache
    for (int i = 65538; i < 140000; i++) {
      try {
        Assert.assertTrue("index = " + i, db.has(hash[i]));
      } catch (Exception e) {
        Assert.fail("transaction should not be expired index = " + i);
      }
    }
  }
}