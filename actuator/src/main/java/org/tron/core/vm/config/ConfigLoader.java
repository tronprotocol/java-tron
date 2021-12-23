package org.tron.core.vm.config;

import static org.tron.core.capsule.ReceiptCapsule.checkForEnergyLimit;

import lombok.extern.slf4j.Slf4j;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.store.StoreFactory;
import org.tron.core.vm.OperationRegistry;

@Slf4j(topic = "VMConfigLoader")
public class ConfigLoader {

  //only for unit test
  public static boolean disable = false;

  public static void load(StoreFactory storeFactory) {
    if (disable) {
      OperationRegistry.clearOperations();
    } else {
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
      }
    }
    OperationRegistry.newBaseOperation();

    if (VMConfig.allowTvmTransferTrc10()) {
      OperationRegistry.newAllowTvmTransferTrc10Operation();
    }

    if (VMConfig.allowTvmConstantinople()) {
      OperationRegistry.newAllowTvmConstantinopleOperation();
    }

    if (VMConfig.allowTvmSolidity059()) {
      OperationRegistry.newAllowTvmSolidity059Operation();
    }

    if (VMConfig.allowTvmIstanbul()) {
      OperationRegistry.newAllowTvmIstanbulOperation();
    }

    if (VMConfig.allowTvmFreeze()) {
      OperationRegistry.newAllowTvmFreezeOperation();
    }

    if (VMConfig.allowTvmVote()) {
      OperationRegistry.newAllowTvmVoteOperation();
    }

    if (VMConfig.allowTvmLondon()) {
      OperationRegistry.newAllowTvmLondonOperation();
    }
  }
}
