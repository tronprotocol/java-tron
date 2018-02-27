package org.tron.db;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.core.db.AccountStore;

public class AccountStoreTest {

  private static final Logger logger = LoggerFactory.getLogger("Test");

  protected AccountStore accountStore;

  @Before
  public void step() {
    accountStore = AccountStore.create("AccountStore");
  }

  @Test
  public void testCreate() {
    logger.info("test create =\n{}\n", AccountStore.create("AccountStore"));
    logger.info("test create =\n{}\n", AccountStore.create(1 + ""));
    logger.info("test create =\n{}\n", AccountStore.create(""));
  }

  // error parameter: no name set to the dbStore
  //  @Test
  //  public void testCreateParamNull() {
  //    logger.info("test create: {}", AccountStore.create(null));
  //  }
}
