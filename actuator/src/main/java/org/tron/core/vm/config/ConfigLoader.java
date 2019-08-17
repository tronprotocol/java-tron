package org.tron.core.vm.config;


import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.DBConfig;
import org.tron.core.exception.TypeMismatchNamingException;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.store.StoreFactory;
@Slf4j(topic = "VMConfigLoader")
public class ConfigLoader {
    public static void load(StoreFactory storeFactory){
        DynamicPropertiesStore ds = null;
        try {
            storeFactory.getStore(DynamicPropertiesStore.class);
        } catch (TypeMismatchNamingException e) {
            logger.error("can not get DynamicPropertiesStore",e);
        }
        VMConfig.setVmTrace(DBConfig.isVmTrace());
        if(ds != null){
            VMConfig.initVmHardFork(checkForEnergyLimit(ds));
            VMConfig.initAllowMultiSign(ds.getAllowMultiSign());
            VMConfig.initAllowTvmTransferTrc10(ds.getAllowTvmTransferTrc10());
            VMConfig.initAllowTvmConstantinople(ds.getAllowTvmConstantinople());
            VMConfig.initAllowTvmSolidity059(ds.getAllowTvmSolidity059());

        }
    }

    private static boolean checkForEnergyLimit(DynamicPropertiesStore ds) {
        long blockNum = ds.getLatestBlockHeaderNumber();
        return blockNum >= DBConfig.getBlockNumForEneryLimit();
    }

}
