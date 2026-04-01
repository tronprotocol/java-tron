package org.tron.core.config.args;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Test;
import org.tron.core.exception.TronError;

public class BlockConfigTest {

  @Test
  public void testDefaults() {
    Config empty = ConfigFactory.empty();
    BlockConfig bc = BlockConfig.fromConfig(empty);
    assertFalse(bc.isNeedSyncCheck());
    assertEquals(21600000L, bc.getMaintenanceTimeInterval());
    assertEquals(1, bc.getCheckFrozenTime());
  }

  @Test
  public void testFromConfig() {
    Config config = ConfigFactory.parseString(
        "block { needSyncCheck = true, maintenanceTimeInterval = 10000,"
            + " checkFrozenTime = 5, proposalExpireTime = 300000 }");
    BlockConfig bc = BlockConfig.fromConfig(config);
    assertEquals(true, bc.isNeedSyncCheck());
    assertEquals(10000L, bc.getMaintenanceTimeInterval());
    assertEquals(5, bc.getCheckFrozenTime());
    assertEquals(300000L, bc.getProposalExpireTime());
  }

  @Test(expected = TronError.class)
  public void testProposalExpireTimeTooLow() {
    // MIN_PROPOSAL_EXPIRE_TIME = 0, so value must be > 0
    Config config = ConfigFactory.parseString("block { proposalExpireTime = 0 }");
    BlockConfig.fromConfig(config);
  }

  @Test(expected = TronError.class)
  public void testProposalExpireTimeTooHigh() {
    Config config = ConfigFactory.parseString("block { proposalExpireTime = 999999999999 }");
    BlockConfig.fromConfig(config);
  }

  @Test(expected = TronError.class)
  public void testRejectsCommitteeProposalExpireTime() {
    Config config = ConfigFactory.parseString(
        "committee { proposalExpireTime = 300000 }\n"
            + "block { proposalExpireTime = 300000 }");
    BlockConfig.fromConfig(config);
  }
}
