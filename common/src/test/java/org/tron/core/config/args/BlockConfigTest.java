package org.tron.core.config.args;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Test;
import org.tron.core.exception.TronError;

public class BlockConfigTest {

  private static Config withRef(String hocon) {
    return ConfigFactory.parseString(hocon).withFallback(ConfigFactory.defaultReference());
  }

  private static Config withRef() {
    return ConfigFactory.defaultReference();
  }

  @Test
  public void testDefaults() {
    BlockConfig bc = BlockConfig.fromConfig(withRef());
    assertEquals(21600000L, bc.getMaintenanceTimeInterval());
    assertEquals(1, bc.getCheckFrozenTime());
  }

  @Test
  public void testFromConfig() {
    Config config = withRef(
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
    BlockConfig.fromConfig(withRef("block { proposalExpireTime = 0 }"));
  }

  @Test(expected = TronError.class)
  public void testProposalExpireTimeTooHigh() {
    BlockConfig.fromConfig(withRef("block { proposalExpireTime = 999999999999 }"));
  }

  @Test(expected = TronError.class)
  public void testRejectsCommitteeProposalExpireTime() {
    BlockConfig.fromConfig(withRef(
        "committee { proposalExpireTime = 300000 }\n"
            + "block { proposalExpireTime = 300000 }"));
  }
}
