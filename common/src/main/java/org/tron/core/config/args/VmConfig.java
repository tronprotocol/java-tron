package org.tron.core.config.args;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * VM configuration bean. Field names match config.conf keys under the "vm" section.
 * Most fields are bound automatically via ConfigBeanFactory; opt-in fields that
 * must stay absent from reference.conf are bound manually after hasPath checks.
 */
@Slf4j
@Getter
@Setter
public class VmConfig {

  private static final String CONSTANT_CALL_TIMEOUT_MS_KEY = "constantCallTimeoutMs";
  static final long MAX_CONSTANT_CALL_TIMEOUT_MS = Long.MAX_VALUE / 1_000L;

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
  // Excluded from ConfigBeanFactory binding (no setter): the property is
  // intentionally absent from reference.conf so {@code Config#hasPath} alone
  // signals operator opt-in. Bound manually in {@link #fromConfig}.
  @Setter(AccessLevel.NONE)
  private long constantCallTimeoutMs = 0L;

  /**
   * Create VmConfig from the "vm" section of the application config.
   * Defaults come from reference.conf (loaded globally via Configuration.java),
   * so no per-bean DEFAULTS needed.
   */
  public static VmConfig fromConfig(Config config) {
    Config vmSection = config.getConfig("vm");
    VmConfig vmConfig = ConfigBeanFactory.create(vmSection, VmConfig.class);
    vmConfig.postProcess(vmSection);
    return vmConfig;
  }

  private void postProcess(Config vmSection) {
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

    // constantCallTimeoutMs is excluded from ConfigBeanFactory binding (no
    // setter) and intentionally absent from reference.conf, so hasPath alone
    // tells us whether the operator opted in. Only positive values that can be
    // safely converted to microseconds are valid.
    if (vmSection.hasPath(CONSTANT_CALL_TIMEOUT_MS_KEY)) {
      long value = vmSection.getLong(CONSTANT_CALL_TIMEOUT_MS_KEY);
      if (value <= 0L) {
        throw new IllegalArgumentException(
            "vm.constantCallTimeoutMs must be > 0 when configured, got " + value);
      }
      if (value > MAX_CONSTANT_CALL_TIMEOUT_MS) {
        throw new IllegalArgumentException(
            "vm.constantCallTimeoutMs must be <= " + MAX_CONSTANT_CALL_TIMEOUT_MS
                + " to fit VM deadline conversion, got " + value);
      }
      constantCallTimeoutMs = value;
    }
  }
}
