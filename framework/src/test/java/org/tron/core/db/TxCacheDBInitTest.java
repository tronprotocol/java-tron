package org.tron.core.db;

import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.ByteArray;
import org.tron.core.Constant;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.keystore.Wallet;

import java.io.IOException;

@Slf4j
public class TxCacheDBInitTest {

  private static TronApplicationContext context;

  @ClassRule
  public static final TemporaryFolder temporaryFolder = new TemporaryFolder();

  private final byte[][] hash = new byte[140000][64];

  @AfterClass
  public static void destroy() {
    context.destroy();
    Args.clearParam();
  }

  /**
   * Init data.
   */
  @BeforeClass
  public static void init() throws IOException {
    Args.setParam(new String[]{"--output-directory", temporaryFolder.newFolder().toString(),
            "--p2p-disable", "true"},
        Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
  }

  @Test
  public void reload(){
    putTransaction();
    DefaultListableBeanFactory defaultListableBeanFactory =
        (DefaultListableBeanFactory) context.getAutowireCapableBeanFactory();
    queryTransaction();
    defaultListableBeanFactory.destroySingleton("transactionCache");
    TransactionCache transactionCache = new TransactionCache("transactionCache",
        context.getBean(RecentTransactionStore.class));
    transactionCache.initCache();
    defaultListableBeanFactory.registerSingleton("transactionCache",transactionCache);
    queryTransaction();
  }

  private void putTransaction() {
    TransactionCache db = context.getBean(TransactionCache.class);
    for (int i = 1; i < 140000; i++) {
      hash[i] = Wallet.generateRandomBytes(64);
      db.put(hash[i], new BytesCapsule(ByteArray.fromLong(i)));
    }
  }

  private void queryTransaction() {
    TransactionCache db = context.getBean(TransactionCache.class);
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