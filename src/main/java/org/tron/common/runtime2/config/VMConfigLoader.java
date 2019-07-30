package org.tron.common.runtime2.config;

import org.tron.core.config.args.Args;
import org.tron.core.db.DynamicPropertiesStore;

public class VMConfigLoader {

  public static final int MAX_FEE_LIMIT = 1_000_000_000; //1000 trx

  private static final VMConfigLoader INSTACE = new VMConfigLoader();

  private VMConfig cachedVmConfig;

  private DynamicPropertiesStore source;

  private VMConfigLoader() {
  }

  VMConfigLoader setSource(DynamicPropertiesStore source) {
    this.source = source;
    return this;
  }

  public static VMConfigLoader getInstance() {
    return INSTACE;
  }

  public VMConfig load() {
    if (cachedVmConfig == null) {
      cachedVmConfig = new VMConfig();
    }
    cachedVmConfig.setMaxFeeLimit(MAX_FEE_LIMIT);
    cachedVmConfig.setSwitchVm2(true);
    cachedVmConfig.setVmTrace(Args.getInstance().isVmTrace());
    cachedVmConfig.setVmTraceCompressed(false);
    cachedVmConfig.setMaxTimeRatio(Args.getInstance().getMaxTimeRatio());
    cachedVmConfig.setMinTimeRatio(Args.getInstance().getMinTimeRatio());
    if (source != null) {
      cachedVmConfig.setMaxCpuTimeOfOneTx(source.getMaxCpuTimeOfOneTx());
    }

    return cachedVmConfig;
  }

  public VMConfig loadCached() {
    return cachedVmConfig == null ? load() : cachedVmConfig;
  }

}
