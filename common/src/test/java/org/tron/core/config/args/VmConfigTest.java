package org.tron.core.config.args;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
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
  // Constant-call timeout (issue #6681). The validation rule: any positive
  // value that fits VM deadline conversion is accepted, but zero/negative is
  // rejected ONLY when the operator explicitly set the property in their
  // config. Absence keeps the in-Java default (0L = "share the
  // block-processing deadline").
  // ===========================================================================

  @Test
  public void testConstantCallTimeoutDefaultWhenAbsent() {
    // No path in the config, no entry in reference.conf -> default 0L kept,
    // no validation triggered.
    VmConfig vm = VmConfig.fromConfig(withRef());
    assertEquals(0L, vm.getConstantCallTimeoutMs());
  }

  @Test
  public void testConstantCallTimeoutAcceptsAnyPositiveValue() {
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
  public void testConstantCallTimeoutZeroRejectedWhenExplicitlyConfigured() {
    // Operator wrote `= 0` in config -> treated as a misconfiguration even
    // though it equals the in-Java default. Forces an explicit positive value.
    try {
      VmConfig.fromConfig(withRef("vm { constantCallTimeoutMs = 0 }"));
      org.junit.Assert.fail("expected IllegalArgumentException for explicit 0");
    } catch (IllegalArgumentException ex) {
      org.junit.Assert.assertTrue(ex.getMessage(),
          ex.getMessage().contains("constantCallTimeoutMs"));
    }
  }

  @Test
  public void testConstantCallTimeoutNegativeRejected() {
    try {
      VmConfig.fromConfig(withRef("vm { constantCallTimeoutMs = -1 }"));
      org.junit.Assert.fail("expected IllegalArgumentException for negative ms");
    } catch (IllegalArgumentException ex) {
      org.junit.Assert.assertTrue(ex.getMessage(),
          ex.getMessage().contains("constantCallTimeoutMs"));
    }
  }

  @Test
  public void testConstantCallTimeoutOverflowRejected() {
    long value = VmConfig.MAX_CONSTANT_CALL_TIMEOUT_MS + 1L;
    try {
      VmConfig.fromConfig(withRef("vm { constantCallTimeoutMs = " + value + " }"));
      org.junit.Assert.fail("expected IllegalArgumentException for overflowing ms");
    } catch (IllegalArgumentException ex) {
      org.junit.Assert.assertTrue(ex.getMessage(),
          ex.getMessage().contains("deadline conversion"));
    }
  }
}
