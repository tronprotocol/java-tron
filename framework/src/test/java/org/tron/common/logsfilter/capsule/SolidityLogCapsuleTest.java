package org.tron.common.logsfilter.capsule;

import static org.junit.Assert.assertTrue;

import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.logsfilter.trigger.ContractLogTrigger;

@Slf4j
public class SolidityLogCapsuleTest {

  private SolidityLogCapsule capsule;

  @Before
  public void setUp() {
    ContractLogTrigger trigger = new ContractLogTrigger();
    capsule = new SolidityLogCapsule(trigger);
  }

  @Test
  public void testSetAndGetSolidityLogCapsule() {
    capsule.setSolidityLogTrigger(capsule.getSolidityLogTrigger());
    try {
      capsule.processTrigger();
    } catch (Exception e) {
      assertTrue(e instanceof NullPointerException);
    }
  }

}
