package org.tron.core.config.args;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import com.typesafe.config.ConfigFactory;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * VM configuration bean. Field names match config.conf keys under the "vm" section.
 * Bound automatically via ConfigBeanFactory — no manual key constants needed.
 */
@Slf4j
@Getter
@Setter
public class VmConfig {

  private boolean supportConstant = false;
  private long maxEnergyLimitForConstant = 100_000_000L;
  private int lruCacheSize = 500;
  private double minTimeRatio = 0.0;
  private double maxTimeRatio = 5.0;
  private int longRunningTime = 10;
  private boolean estimateEnergy = false;
  private int estimateEnergyMaxRetry = 3;
  private boolean vmTrace = false;
  private boolean saveInternalTx = false;
  private boolean saveFeaturedInternalTx = false;
  private boolean saveCancelAllUnfreezeV2Details = false;

  private static final Config DEFAULTS = ConfigFactory.parseString(
      "supportConstant = false\n"
          + "maxEnergyLimitForConstant = 100000000\n"
          + "lruCacheSize = 500\n"
          + "minTimeRatio = 0.0\n"
          + "maxTimeRatio = 5.0\n"
          + "longRunningTime = 10\n"
          + "estimateEnergy = false\n"
          + "estimateEnergyMaxRetry = 3\n"
          + "vmTrace = false\n"
          + "saveInternalTx = false\n"
          + "saveFeaturedInternalTx = false\n"
          + "saveCancelAllUnfreezeV2Details = false\n"
  );

  /**
   * Create VmConfig from the "vm" section of the application config.
   */
  public static VmConfig fromConfig(Config config) {
    Config vmSection = config.hasPath("vm")
        ? config.getConfig("vm").withFallback(DEFAULTS)
        : DEFAULTS;
    VmConfig vmConfig = ConfigBeanFactory.create(vmSection, VmConfig.class);
    vmConfig.postProcess();
    return vmConfig;
  }

  private void postProcess() {
    // clamp maxEnergyLimitForConstant
    if (maxEnergyLimitForConstant < 3_000_000L) {
      maxEnergyLimitForConstant = 3_000_000L;
    }

    // clamp estimateEnergyMaxRetry to 0-10
    if (estimateEnergyMaxRetry < 0) {
      estimateEnergyMaxRetry = 0;
    }
    if (estimateEnergyMaxRetry > 10) {
      estimateEnergyMaxRetry = 10;
    }

    // cross-field dependency warning
    if (saveCancelAllUnfreezeV2Details
        && (!saveInternalTx || !saveFeaturedInternalTx)) {
      logger.warn("Configuring [vm.saveCancelAllUnfreezeV2Details] won't work as "
          + "vm.saveInternalTx or vm.saveFeaturedInternalTx is off.");
    }
  }
}
