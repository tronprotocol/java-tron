package org.tron.db;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.core.db.Manager;

public class ManagerTest {

  public static final Logger logger = LoggerFactory.getLogger("Test");

  private Manager manager;

  @Before
  public void step() {
    manager = new Manager();
  }

  @Test
  public void testGetWitness() {

    logger.info("test get witness ={}:" + manager.getWitnesses());
  }

  // error: blockStore is null
  @Test
  public void testGetScheduledWitness() {
    logger.info("test getScheduledWitness {}:" + manager.getScheduledWitness(1));
  }
}
