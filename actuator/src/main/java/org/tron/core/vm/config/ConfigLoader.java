package org.tron.core.vm.config;

import static org.tron.core.capsule.ReceiptCapsule.checkForEnergyLimit;

import lombok.extern.slf4j.Slf4j;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.store.StoreFactory;

@Slf4j(topic = "VMConfigLoader")
public class ConfigLoader {

  //only for unit test
  public static boolean disable = false;

  // isolate=true: a constant call bound to a non-HEAD (solidity/PBFT) snapshot installs its
  // snapshot into a thread-local view instead of the process-wide global, so it cannot pollute
  // the flags the block-processing path reads concurrently.
  public static void load(StoreFactory storeFactory, boolean isolate) {
    if (!disable) {
      DynamicPropertiesStore ds = storeFactory.getChainBaseManager().getDynamicPropertiesStore();
      VMConfig.setVmTrace(CommonParameter.getInstance().isVmTrace());
      if (ds != null) {
        VMConfig.initVmHardFork(checkForEnergyLimit(ds));
        VMConfig.Snapshot snapshot = new VMConfig.Snapshot();
        snapshot.allowMultiSign = ds.getAllowMultiSign() == 1;
        snapshot.allowTvmTransferTrc10 = ds.getAllowTvmTransferTrc10() == 1;
        snapshot.allowTvmConstantinople = ds.getAllowTvmConstantinople() == 1;
        snapshot.allowTvmSolidity059 = ds.getAllowTvmSolidity059() == 1;
        snapshot.allowShieldedTRC20Transaction = ds.getAllowShieldedTRC20Transaction() == 1;
        snapshot.allowTvmIstanbul = ds.getAllowTvmIstanbul() == 1;
        snapshot.allowTvmFreeze = ds.getAllowTvmFreeze() == 1;
        snapshot.allowTvmVote = ds.getAllowTvmVote() == 1;
        snapshot.allowTvmLondon = ds.getAllowTvmLondon() == 1;
        snapshot.allowTvmCompatibleEvm = ds.getAllowTvmCompatibleEvm() == 1;
        snapshot.allowHigherLimitForMaxCpuTimeOfOneTx =
            ds.getAllowHigherLimitForMaxCpuTimeOfOneTx() == 1;
        snapshot.allowTvmFreezeV2 = ds.supportUnfreezeDelay();
        snapshot.allowOptimizedReturnValueOfChainId = ds.getAllowOptimizedReturnValueOfChainId() == 1;
        snapshot.allowDynamicEnergy = ds.getAllowDynamicEnergy() == 1;
        snapshot.dynamicEnergyThreshold = ds.getDynamicEnergyThreshold();
        snapshot.dynamicEnergyIncreaseFactor = ds.getDynamicEnergyIncreaseFactor();
        snapshot.dynamicEnergyMaxFactor = ds.getDynamicEnergyMaxFactor();
        snapshot.allowTvmShanghai = ds.getAllowTvmShangHai() == 1;
        snapshot.allowEnergyAdjustment = ds.getAllowEnergyAdjustment() == 1;
        snapshot.allowStrictMath = ds.getAllowStrictMath() == 1;
        snapshot.allowTvmCancun = ds.getAllowTvmCancun() == 1;
        snapshot.disableJavaLangMath = ds.getConsensusLogicOptimization() == 1;
        snapshot.allowTvmBlob = ds.getAllowTvmBlob() == 1;
        snapshot.allowTvmSelfdestructRestriction = ds.getAllowTvmSelfdestructRestriction() == 1;
        snapshot.allowTvmOsaka = ds.getAllowTvmOsaka() == 1;
        snapshot.allowHardenResourceCalculation = ds.getAllowHardenResourceCalculation() == 1;
        if (isolate) {
          VMConfig.setLocalSnapshot(snapshot);
        } else {
          VMConfig.setGlobalSnapshot(snapshot);
        }
      }
    }
  }
}
