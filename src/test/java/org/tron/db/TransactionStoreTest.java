package org.tron.db;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.core.db.TransactionStore;

public class TransactionStoreTest {

  private static final Logger logger = LoggerFactory.getLogger("Test");

  TransactionStore transactionStore;

  @Before
  public void step() {

    transactionStore = TransactionStore.create("TransactionStore");
  }

  @Test
  public void testCreate() {
    logger.info("test create = {}:", TransactionStore.create("TransactionStore"));
    logger.info("test create = {}:", TransactionStore.create(1 + ""));
    logger.info("test create = {}:", TransactionStore.create(""));
  }


}
