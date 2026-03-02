package org.tron.common.logsfilter.capsule;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.logsfilter.trigger.SolidityTrigger;

@Slf4j
public class SolidityTriggerCapsuleTest {

  private SolidityTriggerCapsule capsule;

  @Before
  public void setUp() {
    capsule = new SolidityTriggerCapsule(0);
    SolidityTrigger trigger = new SolidityTrigger();
    assertNotNull(trigger.toString());
    capsule.setSolidityTrigger(trigger);
    capsule.setTimeStamp(System.currentTimeMillis());
  }

  @Test
  public void testSetAndGetSolidityLogCapsule() {
    capsule.setSolidityTrigger(capsule.getSolidityTrigger());
    capsule.setTimeStamp(capsule.getSolidityTrigger().getTimeStamp());
    try {
      capsule.processTrigger();
    } catch (Exception e) {
      assertTrue(e instanceof NullPointerException);
    }
  }

}
