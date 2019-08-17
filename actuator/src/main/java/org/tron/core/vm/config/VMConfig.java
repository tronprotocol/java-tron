package org.tron.core.vm.config;

import lombok.Setter;
import org.tron.common.utils.DBConfig;

/**
 * For developer only
 */
public class VMConfig {

    public static final int MAX_CODE_LENGTH = 1024 * 1024;

    public static final int MAX_FEE_LIMIT = 1_000_000_000; //1000 trx

    private static boolean vmTraceCompressed = false;

    @Setter
    private static boolean vmTrace =  false;

    @Setter
    private static boolean ENERGY_LIMIT_HARD_FORK = false;

    @Setter
    private static boolean ALLOW_TVM_TRANSFER_TRC10 = false;

    @Setter
    private static boolean ALLOW_TVM_CONSTANTINOPLE = false;

    @Setter
    private static boolean ALLOW_MULTI_SIGN = false;

    @Setter
    private static boolean ALLOW_TVM_SOLIDITY_059 = false;


    private VMConfig() {
    }

    private static class SystemPropertiesInstance {

        private static final VMConfig INSTANCE = new VMConfig();
    }

    public static VMConfig getInstance() {
        return SystemPropertiesInstance.INSTANCE;
    }

    public static boolean vmTrace() {
        return vmTrace;
    }

    public static boolean vmTraceCompressed() {
        return vmTraceCompressed;
    }

    public static void initVmHardFork(boolean pass) {
        ENERGY_LIMIT_HARD_FORK = pass;
    }

    public static void initAllowMultiSign(long allow) {
        ALLOW_MULTI_SIGN = allow == 1;
    }

    public static void initAllowTvmTransferTrc10(long allow) {
        ALLOW_TVM_TRANSFER_TRC10 = allow == 1;
    }

    public static void initAllowTvmConstantinople(long allow) {
        ALLOW_TVM_CONSTANTINOPLE = allow == 1;
    }

    public static void initAllowTvmSolidity059(long allow) {
        ALLOW_TVM_SOLIDITY_059 = allow == 1;
    }

    public static boolean getEnergyLimitHardFork() {
        return ENERGY_LIMIT_HARD_FORK;
    }

    public static boolean allowTvmTransferTrc10() {
        return ALLOW_TVM_TRANSFER_TRC10;
    }

    public static boolean allowTvmConstantinople() {
        return ALLOW_TVM_CONSTANTINOPLE;
    }

    public static boolean allowMultiSign() {
        return ALLOW_MULTI_SIGN;
    }

    public static boolean allowTvmSolidity059() {
        return ALLOW_TVM_SOLIDITY_059;
    }

}