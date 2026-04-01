package org.tron.core.config.args;

import static org.junit.Assert.assertEquals;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Test;

public class CommitteeConfigTest {

  private static Config withRef(String hocon) {
    return ConfigFactory.parseString(hocon).withFallback(ConfigFactory.defaultReference());
  }

  private static Config withRef() {
    return ConfigFactory.defaultReference();
  }

  @Test
  public void testDefaults() {
    CommitteeConfig cc = CommitteeConfig.fromConfig(withRef());
    assertEquals(0, cc.getAllowCreationOfContracts());
    assertEquals(0, cc.getAllowPBFT());
    assertEquals(20, cc.getPBFTExpireNum());
    assertEquals(0, cc.getUnfreezeDelayDays());
    assertEquals(0, cc.getAllowDynamicEnergy());
  }

  @Test
  public void testFromConfig() {
    Config config = withRef(
        "committee { allowCreationOfContracts = 1, allowPBFT = 1, pBFTExpireNum = 30 }");
    CommitteeConfig cc = CommitteeConfig.fromConfig(config);
    assertEquals(1, cc.getAllowCreationOfContracts());
    assertEquals(1, cc.getAllowPBFT());
    assertEquals(30, cc.getPBFTExpireNum());
  }

  @Test
  public void testUnfreezeDelayDaysClamped() {
    assertEquals(365, CommitteeConfig.fromConfig(
        withRef("committee { unfreezeDelayDays = 500 }")).getUnfreezeDelayDays());
    assertEquals(0, CommitteeConfig.fromConfig(
        withRef("committee { unfreezeDelayDays = -10 }")).getUnfreezeDelayDays());
  }

  @Test
  public void testDynamicEnergyClamped() {
    assertEquals(1, CommitteeConfig.fromConfig(
        withRef("committee { allowDynamicEnergy = 5 }")).getAllowDynamicEnergy());
  }

  @Test
  public void testDynamicEnergyThresholdClamped() {
    assertEquals(100_000_000_000_000_000L, CommitteeConfig.fromConfig(
        withRef("committee { dynamicEnergyThreshold = 999999999999999999 }"))
        .getDynamicEnergyThreshold());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAllowOldRewardOptWithoutPrerequisites() {
    CommitteeConfig.fromConfig(withRef("committee { allowOldRewardOpt = 1 }"));
  }

  @Test
  public void testAllowOldRewardOptWithPrerequisite() {
    CommitteeConfig cc = CommitteeConfig.fromConfig(
        withRef("committee { allowOldRewardOpt = 1, allowTvmVote = 1 }"));
    assertEquals(1, cc.getAllowOldRewardOpt());
  }
}
