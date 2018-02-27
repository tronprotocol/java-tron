package org.tron.db;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.core.db.WitnessStore;

public class WitnessStoreTest {

  private static final Logger logger = LoggerFactory.getLogger("Test");
  protected WitnessStore witnessStore;

  @Before
  public void step() {
    witnessStore = WitnessStore.create("WitnessStore");
  }

  @Test
  public void testCreate() {
    logger.info("test create = {}:", WitnessStore.create("WitnessStore"));
    logger.info("test create = {}:", WitnessStore.create(1 + ""));
    logger.info("test create = {}:", WitnessStore.create(""));
  }

}
