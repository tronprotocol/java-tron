package org.tron.core.vm.config;

import static org.tron.core.capsule.ReceiptCapsule.checkForEnergyLimit;

import lombok.extern.slf4j.Slf4j;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.ByteArray;
import org.tron.core.state.WorldStateQueryInstance;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.store.StoreFactory;

@Slf4j(topic = "VMConfigLoader")
public class ConfigLoader {

  //only for unit test
  public static boolean disable = false;

  public static void load(StoreFactory storeFactory) {
    if (!disable) {
      DynamicPropertiesStore ds = storeFactory.getChainBaseManager().getDynamicPropertiesStore();
      VMConfig.setVmTrace(CommonParameter.getInstance().isVmTrace());
      if (ds != null) {
        VMConfig.initVmHardFork(checkForEnergyLimit(ds));
        VMConfig.initAllowMultiSign(ds.getAllowMultiSign());
        VMConfig.initAllowTvmTransferTrc10(ds.getAllowTvmTransferTrc10());
        VMConfig.initAllowTvmConstantinople(ds.getAllowTvmConstantinople());
        VMConfig.initAllowTvmSolidity059(ds.getAllowTvmSolidity059());
        VMConfig.initAllowShieldedTRC20Transaction(ds.getAllowShieldedTRC20Transaction());
        VMConfig.initAllowTvmIstanbul(ds.getAllowTvmIstanbul());
        VMConfig.initAllowTvmFreeze(ds.getAllowTvmFreeze());
        VMConfig.initAllowTvmVote(ds.getAllowTvmVote());
        VMConfig.initAllowTvmLondon(ds.getAllowTvmLondon());
        VMConfig.initAllowTvmCompatibleEvm(ds.getAllowTvmCompatibleEvm());
        VMConfig.initAllowHigherLimitForMaxCpuTimeOfOneTx(
            ds.getAllowHigherLimitForMaxCpuTimeOfOneTx());
      }
    }
  }

  public static void load(StoreFactory storeFactory, byte[] rootHash) {
    if (!disable) {
      WorldStateQueryInstance wq = new WorldStateQueryInstance(rootHash, storeFactory.getChainBaseManager());
      VMConfig.setVmTrace(CommonParameter.getInstance().isVmTrace());

      long blockNum = wq.getLatestBlockHeaderNumber();
      boolean energyLimit = blockNum >=
          CommonParameter.getInstance().getBlockNumForEnergyLimit();

      VMConfig.initVmHardFork(energyLimit);
      VMConfig.initAllowMultiSign(wq.getAllowMultiSign());
      VMConfig.initAllowTvmTransferTrc10(wq.getAllowTvmTransferTrc10());
      VMConfig.initAllowTvmConstantinople(wq.getAllowTvmConstantinople());
      VMConfig.initAllowTvmSolidity059(wq.getAllowTvmSolidity059());
      VMConfig.initAllowShieldedTRC20Transaction(wq.getAllowShieldedTRC20Transaction());
      VMConfig.initAllowTvmIstanbul(wq.getAllowTvmIstanbul());
      VMConfig.initAllowTvmFreeze(wq.getAllowTvmFreeze());
      VMConfig.initAllowTvmVote(wq.getAllowTvmVote());
      VMConfig.initAllowTvmLondon(wq.getAllowTvmLondon());
      VMConfig.initAllowTvmCompatibleEvm(wq.getAllowTvmCompatibleEvm());
      VMConfig.initAllowHigherLimitForMaxCpuTimeOfOneTx(
          wq.getAllowHigherLimitForMaxCpuTimeOfOneTx());
    }
  }
}
