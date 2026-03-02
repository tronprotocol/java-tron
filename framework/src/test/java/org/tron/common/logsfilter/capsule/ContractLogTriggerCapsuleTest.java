package org.tron.common.logsfilter.capsule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.tron.common.logsfilter.trigger.Trigger.CONTRACTLOG_TRIGGER_NAME;

import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.logsfilter.trigger.ContractLogTrigger;

@Slf4j
public class ContractLogTriggerCapsuleTest {

  private ContractLogTriggerCapsule capsule;

  @Before
  public void setUp() {
    ContractLogTrigger contractLogTrigger = new ContractLogTrigger();
    contractLogTrigger.setBlockNumber(0L);
    capsule = new ContractLogTriggerCapsule(contractLogTrigger);
    capsule.setLatestSolidifiedBlockNumber(0);
  }

  @Test
  public void testSetAndGetContractLogTrigger() {
    capsule.setContractLogTrigger(capsule.getContractLogTrigger());
    assertEquals(CONTRACTLOG_TRIGGER_NAME, capsule.getContractLogTrigger().getTriggerName());
    try {
      capsule.processTrigger();
    } catch (Exception e) {
      assertTrue(e instanceof NullPointerException);
    }
  }

}
