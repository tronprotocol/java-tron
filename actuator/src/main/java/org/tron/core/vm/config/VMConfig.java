/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.tron.core.vm.config;


import static org.tron.common.parameter.CommonParameter.ENERGY_LIMIT_HARD_FORK;

import lombok.Setter;

/**
 * For developer only
 */
public class VMConfig {

  //1000 TRX
  //public static final int MAX_FEE_LIMIT = 1_000_000_000;

  private static boolean vmTraceCompressed = false;

  @Setter
  private static boolean vmTrace = false;

  private static boolean ALLOW_TVM_TRANSFER_TRC10 = false;

  private static boolean ALLOW_TVM_CONSTANTINOPLE = false;

  private static boolean ALLOW_MULTI_SIGN = false;

  private static boolean ALLOW_TVM_SOLIDITY_059 = false;

  private static boolean ALLOW_SHIELDED_TRC20_TRANSACTION = false;

  private static boolean ALLOW_TVM_ISTANBUL = false;

  private static boolean ALLOW_TVM_STAKE = false;

  private static boolean ALLOW_TVM_ASSET_ISSUE = false;

  private VMConfig() {
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

  public static void initAllowShieldedTRC20Transaction(long allow) {
    ALLOW_SHIELDED_TRC20_TRANSACTION = allow == 1;
  }

  public static void initAllowTvmIstanbul(long allow) {
    ALLOW_TVM_ISTANBUL = allow == 1;
  }

  public static void initAllowTvmStake(long allow) {
    ALLOW_TVM_STAKE = allow == 1;
  }

  public static void initAllowTvmAssetIssue(long allow) {
    ALLOW_TVM_ASSET_ISSUE = allow == 1;
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

  public static boolean allowShieldedTRC20Transaction() {
    return ALLOW_SHIELDED_TRC20_TRANSACTION;
  }

  public static boolean allowTvmIstanbul() {return ALLOW_TVM_ISTANBUL; }

  public static boolean allowTvmStake() {
    return ALLOW_TVM_STAKE;
  }

  public static boolean allowTvmAssetIssue() {
    return ALLOW_TVM_ASSET_ISSUE;
  }

  private static class SystemPropertiesInstance {

    private static final VMConfig INSTANCE = new VMConfig();
  }
}
