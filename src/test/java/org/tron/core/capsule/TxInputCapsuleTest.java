package org.tron.core.capsule;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TxInputCapsuleTest {

  private static final Logger logger = LoggerFactory.getLogger("Test");

  protected TxInputCapsule txInputCapsule;

  @Before
  public void step() {
    txInputCapsule = new TxInputCapsule(new byte[]{1}, 1, new byte[]{2}, new byte[]{3});
  }
  
  @Test
  public void testGetTxInput() {
    logger.info("test get txInput = {}", txInputCapsule.getTxInput());
  }

}
