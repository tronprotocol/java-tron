package org.tron.core.db;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.core.Constant;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.Configuration;
import org.tron.core.config.args.Args;
import org.tron.core.exception.ValidateException;


public class BlockStoreTest {

  private static final Logger logger = LoggerFactory.getLogger("Test");

  @Before
  public void init() {
    Args.setParam(new String[]{}, Configuration.getByPath(Constant.TEST_CONF));

  }

  @Test
  public void testPushTransactions() {
    Args.setParam(new String[]{}, Configuration.getByPath(Constant.TEST_CONF));
    BlockStore blockStore = BlockStore.create("test_OperateTx");
    TransactionCapsule transactionCapsule = new TransactionCapsule(
        "2c0937534dd1b3832d05d865e8e6f2bf23218300b33a992740d45ccab7d4f519", 123);
    try {
      blockStore.pushTransactions(transactionCapsule);
    } catch (ValidateException e) {
      e.printStackTrace();
    }
    Assert
        .assertEquals(123, transactionCapsule.getTransaction().getRawData().getVout(0).getValue());
  }
}
