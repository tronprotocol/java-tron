package org.tron.common.logsfilter.capsule;

import static org.junit.Assert.assertTrue;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.logsfilter.trigger.ContractEventTrigger;

@Slf4j
public class SolidityEventCapsuleTest {

  private SolidityEventCapsule capsule;

  @Before
  public void setUp() {
    ContractEventTrigger contractEventTrigger = new ContractEventTrigger();
    capsule = new SolidityEventCapsule(contractEventTrigger);
  }

  @Test
  public void testSetAndGetSolidityEventCapsule() {
    capsule.setSolidityEventTrigger(capsule.getSolidityEventTrigger());
    try {
      capsule.processTrigger();
    } catch (Exception e) {
      assertTrue(e instanceof NullPointerException);
    }
  }

}
