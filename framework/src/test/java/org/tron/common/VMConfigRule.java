package org.tron.common;

import java.lang.reflect.Field;
import org.junit.rules.ExternalResource;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.vm.config.ConfigLoader;
import org.tron.core.vm.config.VMConfig;

/**
 * Restores VM flags after each test, including failed setup and assertion paths.
 *
 * <p>Snapshotting enumerates {@link VMConfig.Snapshot} fields reflectively, so any static flag
 * not mirrored there is outside this rule's protection: when adding a static flag to
 * {@link VMConfig} or {@link ConfigLoader}, it must also be mirrored into
 * {@code VMConfig.Snapshot} (before/after save and restore) or it will leak across tests.
 *
 * <p>This is a method-level rule: the baseline is captured before every test method, so global
 * flags written from class-level {@code @BeforeClass} code are not covered — such classes must
 * add their own {@code @AfterClass} to reset them manually (a leaked London hard-fork flag from
 * class-level setup is an instance of exactly this gap).
 */
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
