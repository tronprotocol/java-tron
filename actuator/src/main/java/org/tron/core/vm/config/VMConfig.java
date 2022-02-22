package org.tron.core.vm.config;

import lombok.Setter;
import org.tron.common.parameter.CommonParameter;

/**
 * For developer only
 */
public class VMConfig {

  private static boolean vmTraceCompressed = false;

  @Setter
  private static boolean vmTrace = false;

  private static boolean ALLOW_TVM_TRANSFER_TRC10 = false;

  private static boolean ALLOW_TVM_CONSTANTINOPLE = false;

  private static boolean ALLOW_MULTI_SIGN = false;

  private static boolean ALLOW_TVM_SOLIDITY_059 = false;

  private static boolean ALLOW_SHIELDED_TRC20_TRANSACTION = false;

  private static boolean ALLOW_TVM_ISTANBUL = false;

  private static boolean ALLOW_TVM_FREEZE = false;

  private static boolean ALLOW_TVM_VOTE = false;

  private static boolean ALLOW_TVM_LONDON = false;

  private static boolean ALLOW_TVM_COMPATIBLE_EVM = false;

  private VMConfig() {
  }

  public static boolean vmTrace() {
    return vmTrace;
  }

  public static boolean vmTraceCompressed() {
    return vmTraceCompressed;
  }

  public static void initVmHardFork(boolean pass) {
    CommonParameter.ENERGY_LIMIT_HARD_FORK = pass;
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

  public static void initAllowShieldedTRC20Transaction(long allow) {
    ALLOW_SHIELDED_TRC20_TRANSACTION = allow == 1;
  }

  public static void initAllowTvmIstanbul(long allow) {
    ALLOW_TVM_ISTANBUL = allow == 1;
  }

  public static void initAllowTvmFreeze(long allow) {
    ALLOW_TVM_FREEZE = allow == 1;
  }

  public static void initAllowTvmVote(long allow) {
    ALLOW_TVM_VOTE = allow == 1;
  }

  public static void initAllowTvmLondon(long allow) {
    ALLOW_TVM_LONDON = allow == 1;
  }

  public static void initAllowTvmCompatibleEvm(long allow) {
    ALLOW_TVM_COMPATIBLE_EVM = allow == 1;
  }

  public static boolean getEnergyLimitHardFork() {
    return CommonParameter.ENERGY_LIMIT_HARD_FORK;
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

  public static boolean allowShieldedTRC20Transaction() {
    return ALLOW_SHIELDED_TRC20_TRANSACTION;
  }

  public static boolean allowTvmIstanbul() {
    return ALLOW_TVM_ISTANBUL;
  }

  public static boolean allowTvmFreeze() {
    return ALLOW_TVM_FREEZE;
  }

  public static boolean allowTvmVote() {
    return ALLOW_TVM_VOTE;
  }

  public static boolean allowTvmLondon() {
    return ALLOW_TVM_LONDON;
  }

  public static boolean allowTvmCompatibleEvm() {
    return ALLOW_TVM_COMPATIBLE_EVM;
  }
}
