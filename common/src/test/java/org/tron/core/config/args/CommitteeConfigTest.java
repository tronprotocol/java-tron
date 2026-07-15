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
    assertEquals(0, cc.getAllowPbft());
    assertEquals(20, cc.getPbftExpireNum());
    assertEquals(0, cc.getUnfreezeDelayDays());
    assertEquals(0, cc.getAllowDynamicEnergy());
  }

  @Test
  public void testFromConfig() {
    Config config = withRef(
        "committee { allowCreationOfContracts = 1, allowPBFT = 1, pBFTExpireNum = 30 }");
    CommitteeConfig cc = CommitteeConfig.fromConfig(config);
    assertEquals(1, cc.getAllowCreationOfContracts());
    assertEquals(1, cc.getAllowPbft());
    assertEquals(30, cc.getPbftExpireNum());
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

  // ===========================================================================
  // Boundary tests for postProcess() clamps
  //
  // Background: PR #6615 (config bean refactor) silently dropped clamps for
  // memoFee and allowNewReward because no test covered the boundary cases.
  // These tests pin every clamp in CommitteeConfig.postProcess() so future
  // refactors cannot drop them undetected.
  // ===========================================================================

  // ----- memoFee: clamped to [0, 1_000_000_000] -----

  @Test
  public void testMemoFeeClampedBelowZero() {
    assertEquals(0, CommitteeConfig.fromConfig(
        withRef("committee { memoFee = -100 }")).getMemoFee());
  }

  @Test
  public void testMemoFeeClampedAboveMax() {
    assertEquals(1_000_000_000L, CommitteeConfig.fromConfig(
        withRef("committee { memoFee = 5000000000 }")).getMemoFee());
  }

  @Test
  public void testMemoFeeInRangeUnchanged() {
    assertEquals(500_000_000L, CommitteeConfig.fromConfig(
        withRef("committee { memoFee = 500000000 }")).getMemoFee());
  }

  @Test
  public void testMemoFeeBoundaryValues() {
    assertEquals(0, CommitteeConfig.fromConfig(
        withRef("committee { memoFee = 0 }")).getMemoFee());
    assertEquals(1_000_000_000L, CommitteeConfig.fromConfig(
        withRef("committee { memoFee = 1000000000 }")).getMemoFee());
  }

  // ----- allowNewReward: clamped to [0, 1] -----

  @Test
  public void testAllowNewRewardClampedBelowZero() {
    assertEquals(0, CommitteeConfig.fromConfig(
        withRef("committee { allowNewReward = -5 }")).getAllowNewReward());
  }

  @Test
  public void testAllowNewRewardClampedAboveOne() {
    assertEquals(1, CommitteeConfig.fromConfig(
        withRef("committee { allowNewReward = 99 }")).getAllowNewReward());
  }

  @Test
  public void testAllowNewRewardBoundaryValues() {
    assertEquals(0, CommitteeConfig.fromConfig(
        withRef("committee { allowNewReward = 0 }")).getAllowNewReward());
    assertEquals(1, CommitteeConfig.fromConfig(
        withRef("committee { allowNewReward = 1 }")).getAllowNewReward());
  }

  // Critical: clamp must run BEFORE the cross-field check, otherwise
  // `allowNewReward = 2` (intended as "enabled") would still satisfy
  // `allowNewReward != 1` and the cross-field check would throw.
  // This test pins the clamp ordering.
  @Test
  public void testAllowNewRewardClampRunsBeforeCrossFieldCheck() {
    CommitteeConfig cc = CommitteeConfig.fromConfig(withRef(
        "committee { allowOldRewardOpt = 1, allowNewReward = 2 }"));
    assertEquals(1, cc.getAllowNewReward());
    assertEquals(1, cc.getAllowOldRewardOpt());
  }

  // ----- allowDelegateOptimization: clamped to [0, 1] -----

  @Test
  public void testAllowDelegateOptimizationClampedBelowZero() {
    assertEquals(0, CommitteeConfig.fromConfig(
        withRef("committee { allowDelegateOptimization = -3 }"))
        .getAllowDelegateOptimization());
  }

  @Test
  public void testAllowDelegateOptimizationClampedAboveOne() {
    assertEquals(1, CommitteeConfig.fromConfig(
        withRef("committee { allowDelegateOptimization = 7 }"))
        .getAllowDelegateOptimization());
  }

  // ----- allowDynamicEnergy: clamped to [0, 1] -----

  @Test
  public void testAllowDynamicEnergyClampedBelowZero() {
    assertEquals(0, CommitteeConfig.fromConfig(
        withRef("committee { allowDynamicEnergy = -1 }")).getAllowDynamicEnergy());
  }

  // ----- unfreezeDelayDays: clamped to [0, 365] (boundary values) -----

  @Test
  public void testUnfreezeDelayDaysBoundaryValues() {
    assertEquals(0, CommitteeConfig.fromConfig(
        withRef("committee { unfreezeDelayDays = 0 }")).getUnfreezeDelayDays());
    assertEquals(365, CommitteeConfig.fromConfig(
        withRef("committee { unfreezeDelayDays = 365 }")).getUnfreezeDelayDays());
    assertEquals(100, CommitteeConfig.fromConfig(
        withRef("committee { unfreezeDelayDays = 100 }")).getUnfreezeDelayDays());
  }

  // ----- dynamicEnergyThreshold: clamped to [0, 100_000_000_000_000_000] -----

  @Test
  public void testDynamicEnergyThresholdClampedBelowZero() {
    assertEquals(0, CommitteeConfig.fromConfig(
        withRef("committee { dynamicEnergyThreshold = -1 }"))
        .getDynamicEnergyThreshold());
  }

  // ----- dynamicEnergyIncreaseFactor: clamped to [0, 10_000] -----

  @Test
  public void testDynamicEnergyIncreaseFactorClamped() {
    assertEquals(0, CommitteeConfig.fromConfig(
        withRef("committee { dynamicEnergyIncreaseFactor = -1 }"))
        .getDynamicEnergyIncreaseFactor());
    assertEquals(10_000L, CommitteeConfig.fromConfig(
        withRef("committee { dynamicEnergyIncreaseFactor = 10001 }"))
        .getDynamicEnergyIncreaseFactor());
    assertEquals(5_000L, CommitteeConfig.fromConfig(
        withRef("committee { dynamicEnergyIncreaseFactor = 5000 }"))
        .getDynamicEnergyIncreaseFactor());
  }

  // ----- dynamicEnergyMaxFactor: clamped to [0, 100_000] -----

  @Test
  public void testDynamicEnergyMaxFactorClamped() {
    assertEquals(0, CommitteeConfig.fromConfig(
        withRef("committee { dynamicEnergyMaxFactor = -1 }"))
        .getDynamicEnergyMaxFactor());
    assertEquals(100_000L, CommitteeConfig.fromConfig(
        withRef("committee { dynamicEnergyMaxFactor = 100001 }"))
        .getDynamicEnergyMaxFactor());
    assertEquals(50_000L, CommitteeConfig.fromConfig(
        withRef("committee { dynamicEnergyMaxFactor = 50000 }"))
        .getDynamicEnergyMaxFactor());
  }

  // ----- Cross-field validation for allowOldRewardOpt -----

  @Test
  public void testAllowOldRewardOptWithAllowNewReward() {
    CommitteeConfig cc = CommitteeConfig.fromConfig(
        withRef("committee { allowOldRewardOpt = 1, allowNewReward = 1 }"));
    assertEquals(1, cc.getAllowOldRewardOpt());
  }

  @Test
  public void testAllowOldRewardOptWithAllowNewRewardAlgorithm() {
    CommitteeConfig cc = CommitteeConfig.fromConfig(
        withRef("committee { allowOldRewardOpt = 1, allowNewRewardAlgorithm = 1 }"));
    assertEquals(1, cc.getAllowOldRewardOpt());
  }
}
