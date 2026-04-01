package org.tron.core.config.args;

import static org.junit.Assert.assertEquals;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Test;

public class CommitteeConfigTest {

  @Test
  public void testDefaults() {
    Config empty = ConfigFactory.empty();
    CommitteeConfig cc = CommitteeConfig.fromConfig(empty);
    assertEquals(0, cc.getAllowCreationOfContracts());
    assertEquals(0, cc.getAllowPBFT());
    assertEquals(20, cc.getPBFTExpireNum());
    assertEquals(0, cc.getUnfreezeDelayDays());
    assertEquals(0, cc.getAllowDynamicEnergy());
  }

  @Test
  public void testFromConfig() {
    Config config = ConfigFactory.parseString(
        "committee { allowCreationOfContracts = 1, allowPBFT = 1, pBFTExpireNum = 30 }");
    CommitteeConfig cc = CommitteeConfig.fromConfig(config);
    assertEquals(1, cc.getAllowCreationOfContracts());
    assertEquals(1, cc.getAllowPBFT());
    assertEquals(30, cc.getPBFTExpireNum());
  }

  @Test
  public void testUnfreezeDelayDaysClamped() {
    Config tooHigh = ConfigFactory.parseString("committee { unfreezeDelayDays = 500 }");
    assertEquals(365, CommitteeConfig.fromConfig(tooHigh).getUnfreezeDelayDays());

    Config tooLow = ConfigFactory.parseString("committee { unfreezeDelayDays = -10 }");
    assertEquals(0, CommitteeConfig.fromConfig(tooLow).getUnfreezeDelayDays());
  }

  @Test
  public void testDynamicEnergyClamped() {
    Config config = ConfigFactory.parseString("committee { allowDynamicEnergy = 5 }");
    assertEquals(1, CommitteeConfig.fromConfig(config).getAllowDynamicEnergy());
  }

  @Test
  public void testDynamicEnergyThresholdClamped() {
    Config config = ConfigFactory.parseString(
        "committee { dynamicEnergyThreshold = 999999999999999999 }");
    assertEquals(100_000_000_000_000_000L,
        CommitteeConfig.fromConfig(config).getDynamicEnergyThreshold());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAllowOldRewardOptWithoutPrerequisites() {
    Config config = ConfigFactory.parseString(
        "committee { allowOldRewardOpt = 1 }");
    CommitteeConfig.fromConfig(config);
  }

  @Test
  public void testAllowOldRewardOptWithPrerequisite() {
    Config config = ConfigFactory.parseString(
        "committee { allowOldRewardOpt = 1, allowTvmVote = 1 }");
    CommitteeConfig cc = CommitteeConfig.fromConfig(config);
    assertEquals(1, cc.getAllowOldRewardOpt());
  }
}
