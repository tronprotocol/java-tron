package org.tron.common.logsfilter.capsule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.tron.common.logsfilter.trigger.Trigger.CONTRACTLOG_TRIGGER_NAME;

import org.junit.Before;
import org.junit.Test;
import org.tron.common.logsfilter.trigger.ContractLogTrigger;

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
    assertThrows(NullPointerException.class, capsule::processTrigger);
  }

}
