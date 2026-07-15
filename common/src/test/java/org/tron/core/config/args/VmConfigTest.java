package org.tron.core.config.args;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Assert;
import org.junit.Test;

public class VmConfigTest {

  private static Config withRef(String hocon) {
    return ConfigFactory.parseString(hocon).withFallback(ConfigFactory.defaultReference());
  }

  private static Config withRef() {
    return ConfigFactory.defaultReference();
  }

  @Test
  public void testDefaults() {
    Config empty = withRef();
    VmConfig vm = VmConfig.fromConfig(empty);
    assertFalse(vm.isSupportConstant());
    assertEquals(100_000_000L, vm.getMaxEnergyLimitForConstant());
    assertEquals(500, vm.getLruCacheSize());
    assertEquals(0.0, vm.getMinTimeRatio(), 0.001);
    assertEquals(5.0, vm.getMaxTimeRatio(), 0.001);
    assertEquals(10, vm.getLongRunningTime());
    assertFalse(vm.isEstimateEnergy());
    assertEquals(3, vm.getEstimateEnergyMaxRetry());
    assertFalse(vm.isVmTrace());
    assertFalse(vm.isSaveInternalTx());
    assertFalse(vm.isSaveFeaturedInternalTx());
    assertFalse(vm.isSaveCancelAllUnfreezeV2Details());
  }

  @Test
  public void testFromConfig() {
    Config config = withRef(
        "vm { supportConstant = true, lruCacheSize = 1000, minTimeRatio = 0.5 }");
    VmConfig vm = VmConfig.fromConfig(config);
    assertTrue(vm.isSupportConstant());
    assertEquals(1000, vm.getLruCacheSize());
    assertEquals(0.5, vm.getMinTimeRatio(), 0.001);
  }

  @Test
  public void testMaxEnergyLimitClamped() {
    Config config = withRef("vm { maxEnergyLimitForConstant = 100 }");
    VmConfig vm = VmConfig.fromConfig(config);
    assertEquals(3_000_000L, vm.getMaxEnergyLimitForConstant());
  }

  @Test
  public void testEstimateEnergyMaxRetryClamped() {
    Config tooHigh = withRef("vm { estimateEnergyMaxRetry = 50 }");
    assertEquals(10, VmConfig.fromConfig(tooHigh).getEstimateEnergyMaxRetry());

    Config tooLow = withRef("vm { estimateEnergyMaxRetry = -5 }");
    assertEquals(0, VmConfig.fromConfig(tooLow).getEstimateEnergyMaxRetry());
  }

  @Test
  public void testPartialConfig() {
    Config config = withRef("vm { saveInternalTx = true }");
    VmConfig vm = VmConfig.fromConfig(config);
    assertTrue(vm.isSaveInternalTx());
    assertFalse(vm.isSupportConstant()); // default
    assertEquals(500, vm.getLruCacheSize()); // default
  }

  // ===========================================================================
  // Boundary tests for postProcess() clamps
  // Pin every clamp in VmConfig.postProcess() so future refactors cannot
  // drop them undetected (regression seen in PR #6615 with CommitteeConfig).
  // ===========================================================================

  // ----- estimateEnergyMaxRetry: clamped to [0, 10] -----

  @Test
  public void testEstimateEnergyMaxRetryBoundaryValues() {
    assertEquals(0, VmConfig.fromConfig(
        withRef("vm { estimateEnergyMaxRetry = 0 }")).getEstimateEnergyMaxRetry());
    assertEquals(10, VmConfig.fromConfig(
        withRef("vm { estimateEnergyMaxRetry = 10 }")).getEstimateEnergyMaxRetry());
    assertEquals(3, VmConfig.fromConfig(
        withRef("vm { estimateEnergyMaxRetry = 3 }")).getEstimateEnergyMaxRetry());
  }

  // ===========================================================================
  // Constant-call timeout (issue #6681). The validation rule: any zero or positive
  // value that fits VM deadline conversion is accepted, but negative is
  // rejected ONLY when the operator explicitly set the property in their
  // config. Absence keeps the in-Java default (0L = "share the
  // block-processing deadline").
  // ===========================================================================

  @Test
  public void testConstantCallTimeoutDefaultWhenAbsent() {
    // reference.conf default is 0; absence of a user override keeps that default;
    // no validation triggered.
    VmConfig vm = VmConfig.fromConfig(withRef());
    assertEquals(0L, vm.getConstantCallTimeoutMs());
  }

  @Test
  public void testConstantCallTimeoutAcceptsAnyPositiveValue() {
    assertEquals(0L, VmConfig.fromConfig(
        withRef("vm { constantCallTimeoutMs = 0 }")).getConstantCallTimeoutMs());
    assertEquals(1L, VmConfig.fromConfig(
        withRef("vm { constantCallTimeoutMs = 1 }")).getConstantCallTimeoutMs());
    assertEquals(50L, VmConfig.fromConfig(
        withRef("vm { constantCallTimeoutMs = 50 }")).getConstantCallTimeoutMs());
    assertEquals(500L, VmConfig.fromConfig(
        withRef("vm { constantCallTimeoutMs = 500 }")).getConstantCallTimeoutMs());
    assertEquals(5_000L, VmConfig.fromConfig(
        withRef("vm { constantCallTimeoutMs = 5000 }")).getConstantCallTimeoutMs());
  }

  @Test
  public void testConstantCallTimeoutNegativeRejected() {
    IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
      VmConfig.fromConfig(withRef("vm { constantCallTimeoutMs = -1 }"));
    });
    Assert.assertTrue(thrown.getMessage().contains("constantCallTimeoutMs"));
  }

  @Test
  public void testConstantCallTimeoutOverflowRejected() {
    long value = Long.MAX_VALUE / 1000 + 1L;
    IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
      VmConfig.fromConfig(withRef("vm { constantCallTimeoutMs = " + value + " }"));
    });
    Assert.assertTrue(thrown.getMessage().contains("deadline conversion"));
  }
}
