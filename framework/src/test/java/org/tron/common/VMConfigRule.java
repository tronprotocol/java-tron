package org.tron.common;

import java.lang.reflect.Field;
import org.junit.rules.ExternalResource;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.vm.config.ConfigLoader;
import org.tron.core.vm.config.VMConfig;

/** Restores VM flags after each test, including failed setup and assertion paths. */
public class VMConfigRule extends ExternalResource {

  private VMConfig.Snapshot savedSnapshot;
  private boolean savedLoaderDisabled;
  private boolean savedHardFork;
  private boolean savedTrace;

  @Override
  protected void before() throws Exception {
    Field global = VMConfig.class.getDeclaredField("globalSnapshot");
    global.setAccessible(true);
    VMConfig.Snapshot current = (VMConfig.Snapshot) global.get(null);
    savedSnapshot = new VMConfig.Snapshot();
    // init* methods mutate the snapshot in place, so saving only its reference is insufficient.
    for (Field flag : VMConfig.Snapshot.class.getFields()) {
      flag.set(savedSnapshot, flag.get(current));
    }
    savedLoaderDisabled = ConfigLoader.disable;
    savedHardFork = CommonParameter.ENERGY_LIMIT_HARD_FORK;
    savedTrace = VMConfig.vmTrace();
    VMConfig.clearLocalSnapshot();
  }

  @Override
  protected void after() {
    VMConfig.setGlobalSnapshot(savedSnapshot);
    ConfigLoader.disable = savedLoaderDisabled;
    VMConfig.initVmHardFork(savedHardFork);
    VMConfig.setVmTrace(savedTrace);
  }
}
