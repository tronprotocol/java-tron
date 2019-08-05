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

  public VMConfigLoader setSource(DynamicPropertiesStore source) {
    this.source = source;
    return this;
  }

  public static VMConfigLoader getInstance() {
    return INSTACE;
  }

  public VMConfig loadNew() {
    cachedVmConfig = new VMConfig();
    cachedVmConfig.setMaxFeeLimit(MAX_FEE_LIMIT);
    cachedVmConfig.setSwitchVm2(true);
    cachedVmConfig.setVmTrace(Args.getInstance().isVmTrace());
    cachedVmConfig.setVmTraceCompressed(false);
    cachedVmConfig.setMaxTimeRatio(Args.getInstance().getMaxTimeRatio());
    cachedVmConfig.setMinTimeRatio(Args.getInstance().getMinTimeRatio());
    if (source != null) {
      cachedVmConfig.setMaxCpuTimeOfOneTx(source.getMaxCpuTimeOfOneTx());
      //for previous vm1 use
      org.tron.common.runtime.config.VMConfig.initVmHardFork();
      org.tron.common.runtime.config.VMConfig.initAllowMultiSign(source.getAllowMultiSign());
      org.tron.common.runtime.config.VMConfig.initAllowTvmTransferTrc10(source.getAllowTvmTransferTrc10());
      org.tron.common.runtime.config.VMConfig.initAllowTvmConstantinople(source.getAllowTvmConstantinople());
      org.tron.common.runtime.config.VMConfig.initAllowTvmSolidity059(source.getAllowTvmSolidity059());
    }
    return cachedVmConfig;
  }

  public VMConfig loadIntoCache() {
    if (cachedVmConfig == null) {
      cachedVmConfig = loadNew();
    }
    return cachedVmConfig;
  }

  public VMConfig loadCached() {
    return cachedVmConfig == null ? loadIntoCache() : cachedVmConfig;
  }

}
