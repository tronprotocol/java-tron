package org.tron.core.db;

import javax.annotation.Resource;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.application.Application;
import org.tron.common.utils.ByteArray;
import org.tron.core.Constant;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.config.args.Args;
import org.tron.keystore.Wallet;

public class TxCacheDBTest extends BaseTest {
  @Resource
  private Application appT;

  /**
   * Init data.
   */
  @BeforeClass
  public static void init() {
    String dbDirectory = "db_TransactionCache_test";
    String indexDirectory = "index_TransactionCache_test";
    dbPath = "output_TransactionCache_test";
    Args.setParam(new String[]{"--output-directory", dbPath, "--storage-db-directory",
        dbDirectory, "--storage-index-directory", indexDirectory, "-w"}, Constant.TEST_CONF);
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