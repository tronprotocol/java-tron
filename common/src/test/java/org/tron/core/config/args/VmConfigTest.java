package org.tron.core.config.args;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Test;

public class VmConfigTest {

  @Test
  public void testDefaults() {
    Config empty = ConfigFactory.empty();
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
    Config config = ConfigFactory.parseString(
        "vm { supportConstant = true, lruCacheSize = 1000, minTimeRatio = 0.5 }");
    VmConfig vm = VmConfig.fromConfig(config);
    assertTrue(vm.isSupportConstant());
    assertEquals(1000, vm.getLruCacheSize());
    assertEquals(0.5, vm.getMinTimeRatio(), 0.001);
  }

  @Test
  public void testMaxEnergyLimitClamped() {
    Config config = ConfigFactory.parseString("vm { maxEnergyLimitForConstant = 100 }");
    VmConfig vm = VmConfig.fromConfig(config);
    assertEquals(3_000_000L, vm.getMaxEnergyLimitForConstant());
  }

  @Test
  public void testEstimateEnergyMaxRetryClamped() {
    Config tooHigh = ConfigFactory.parseString("vm { estimateEnergyMaxRetry = 50 }");
    assertEquals(10, VmConfig.fromConfig(tooHigh).getEstimateEnergyMaxRetry());

    Config tooLow = ConfigFactory.parseString("vm { estimateEnergyMaxRetry = -5 }");
    assertEquals(0, VmConfig.fromConfig(tooLow).getEstimateEnergyMaxRetry());
  }

  @Test
  public void testPartialConfig() {
    Config config = ConfigFactory.parseString("vm { saveInternalTx = true }");
    VmConfig vm = VmConfig.fromConfig(config);
    assertTrue(vm.isSaveInternalTx());
    assertFalse(vm.isSupportConstant()); // default
    assertEquals(500, vm.getLruCacheSize()); // default
  }
}
