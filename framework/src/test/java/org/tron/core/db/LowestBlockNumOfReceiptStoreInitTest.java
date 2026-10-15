package org.tron.core.db;

import javax.annotation.Resource;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.TestConstants;
import org.tron.core.config.args.Args;
import org.tron.core.store.TransactionRetStore;

public class LowestBlockNumOfReceiptStoreInitTest extends BaseTest {

  @Resource
  private TransactionRetStore transactionRetStore;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath()}, TestConstants.TEST_CONF);
  }

  @Test
  public void contextStartupInitializesLowestBlockNumOfReceiptStore() {
    Assert.assertFalse(transactionRetStore.getLowestReceiptBlockNum().isPresent());
    Assert.assertEquals(chainBaseManager.getHeadBlockNum() + 1,
        chainBaseManager.getLowestBlockNumOfReceiptStore());
  }
}
