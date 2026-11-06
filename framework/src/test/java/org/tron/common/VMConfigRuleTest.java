package org.tron.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.tron.core.vm.config.ConfigLoader;
import org.tron.core.vm.config.VMConfig;

public class VMConfigRuleTest {
  @Rule
  public final VMConfigRule vmConfigRule = new VMConfigRule();

  @Test
  public void restoresConfigurationAfterSuccessfulExecution() throws Throwable {
    verifyRestoration(null, false);
  }

  @Test
  public void restoresConfigurationAfterSetupFailure() throws Throwable {
    verifyRestoration(new IllegalStateException("setup failed"), false);
  }

  @Test
  public void restoresConfigurationAfterAssertionFailure() throws Throwable {
    verifyRestoration(new AssertionError("assertion failed"), true);
  }

  private void verifyRestoration(Throwable failure, boolean replaceGlobal) throws Throwable {
    VMConfig.Snapshot baseline = new VMConfig.Snapshot();
    baseline.allowTvmLondon = true;
    baseline.dynamicEnergyThreshold = 13;
    VMConfig.setGlobalSnapshot(new VMConfig.Snapshot());
    VMConfig.initAllowTvmLondon(1);
    VMConfig.initDynamicEnergyThreshold(13);
    ConfigLoader.disable = true;
    VMConfig.initVmHardFork(false);
    VMConfig.setVmTrace(false);
    Statement execution = new VMConfigRule().apply(new Statement() {
      @Override
      public void evaluate() throws Throwable {
        if (replaceGlobal) {
          VMConfig.setGlobalSnapshot(new VMConfig.Snapshot());
        }
        // Include every governance flag, including flags added by either stacked TIP.
        Field global = VMConfig.class.getDeclaredField("globalSnapshot");
        global.setAccessible(true);
        VMConfig.Snapshot current = (VMConfig.Snapshot) global.get(null);
        for (Field flag : VMConfig.Snapshot.class.getFields()) {
          flag.set(current, flag.getType() == boolean.class ? Boolean.TRUE : Long.valueOf(7));
        }
        ConfigLoader.disable = false;
        VMConfig.initVmHardFork(true);
        VMConfig.setVmTrace(true);
        VMConfig.setLocalSnapshot(current);
        if (failure != null) {
          throw failure;
        }
      }
    }, Description.EMPTY);

    if (failure == null) {
      execution.evaluate();
    } else {
      assertSame(failure, assertThrows(Throwable.class, execution::evaluate));
    }

    assertTrue(ConfigLoader.disable);
    assertFalse(VMConfig.getEnergyLimitHardFork());
    assertFalse(VMConfig.vmTrace());
    Field global = VMConfig.class.getDeclaredField("globalSnapshot");
    global.setAccessible(true);
    Object restored = global.get(null);
    for (Field flag : VMConfig.Snapshot.class.getFields()) {
      assertEquals(flag.getName(), flag.get(baseline), flag.get(restored));
    }
    assertFalse("the local view must be cleared", VMConfig.allowTvmVotePowerOptimization());
  }
}
