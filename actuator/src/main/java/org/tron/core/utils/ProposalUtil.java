package org.tron.core.utils;

import org.tron.common.utils.ForkUtils;
import org.tron.core.config.args.Parameter.ForkBlockVersionConsts;
import org.tron.core.config.args.Parameter.ForkBlockVersionEnum;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.store.DynamicPropertiesStore;

public class ProposalUtil {

  protected static final long LONG_VALUE = 100_000_000_000_000_000L;
  protected static final String BAD_PARAM_ID = "Bad chain parameter id";
  private static final String LONG_VALUE_ERROR =
      "Bad chain parameter value, valid range is [0," + LONG_VALUE + "]";

  public static void validator(DynamicPropertiesStore dynamicPropertiesStore, ForkUtils forkUtils,
      long code, long value)
      throws ContractValidateException {
    ProposalType proposalType = ProposalType.getEnum(code);
    switch (proposalType) {
      case MAINTENANCE_TIME_INTERVAL: {
        if (value < 3 * 27 * 1000 || value > 24 * 3600 * 1000) {
          throw new ContractValidateException(
              "Bad chain parameter value, valid range is [3 * 27 * 1000,24 * 3600 * 1000]");
        }
        return;
      }
      case ACCOUNT_UPGRADE_COST:
      case CREATE_ACCOUNT_FEE:
      case TRANSACTION_FEE:
      case ASSET_ISSUE_FEE:
      case WITNESS_PAY_PER_BLOCK:
      case WITNESS_STANDBY_ALLOWANCE:
      case CREATE_NEW_ACCOUNT_FEE_IN_SYSTEM_CONTRACT:
      case CREATE_NEW_ACCOUNT_BANDWIDTH_RATE: {
        if (value < 0 || value > LONG_VALUE) {
          throw new ContractValidateException(LONG_VALUE_ERROR);
        }
        break;
      }
      case ALLOW_CREATION_OF_CONTRACTS: {
        if (value != 1) {
          throw new ContractValidateException(
              "This value[ALLOW_CREATION_OF_CONTRACTS] is only allowed to be 1");
        }
        break;
      }
      case REMOVE_THE_POWER_OF_THE_GR: {
        if (dynamicPropertiesStore.getRemoveThePowerOfTheGr() == -1) {
          throw new ContractValidateException(
              "This proposal has been executed before and is only allowed to be executed once");
        }

        if (value != 1) {
          throw new ContractValidateException(
              "This value[REMOVE_THE_POWER_OF_THE_GR] is only allowed to be 1");
        }
        break;
      }
      case ENERGY_FEE:
      case EXCHANGE_CREATE_FEE:
        break;
      case MAX_CPU_TIME_OF_ONE_TX:
        if (value < 10 || value > 100) {
          throw new ContractValidateException(
              "Bad chain parameter value, valid range is [10,100]");
        }
        break;
      case ALLOW_UPDATE_ACCOUNT_NAME: {
        if (value != 1) {
          throw new ContractValidateException(
              "This value[ALLOW_UPDATE_ACCOUNT_NAME] is only allowed to be 1");
        }
        break;
      }
      case ALLOW_SAME_TOKEN_NAME: {
        if (value != 1) {
          throw new ContractValidateException(
              "This value[ALLOW_SAME_TOKEN_NAME] is only allowed to be 1");
        }
        break;
      }
      case ALLOW_DELEGATE_RESOURCE: {
        if (value != 1) {
          throw new ContractValidateException(
              "This value[ALLOW_DELEGATE_RESOURCE] is only allowed to be 1");
        }
        break;
      }
      case TOTAL_ENERGY_LIMIT: { // deprecated
        if (!forkUtils.pass(ForkBlockVersionConsts.ENERGY_LIMIT)) {
          throw new ContractValidateException(BAD_PARAM_ID);
        }
        if (forkUtils.pass(ForkBlockVersionEnum.VERSION_3_2_2)) {
          throw new ContractValidateException(BAD_PARAM_ID);
        }
        if (value < 0 || value > LONG_VALUE) {
          throw new ContractValidateException(LONG_VALUE_ERROR);
        }
        break;
      }
      case ALLOW_TVM_TRANSFER_TRC10: {
        if (value != 1) {
          throw new ContractValidateException(
              "This value[ALLOW_TVM_TRANSFER_TRC10] is only allowed to be 1");
        }
        if (dynamicPropertiesStore.getAllowSameTokenName() == 0) {
          throw new ContractValidateException("[ALLOW_SAME_TOKEN_NAME] proposal must be approved "
              + "before [ALLOW_TVM_TRANSFER_TRC10] can be proposed");
        }
        break;
      }
      case TOTAL_CURRENT_ENERGY_LIMIT: {
        if (!forkUtils.pass(ForkBlockVersionEnum.VERSION_3_2_2)) {
          throw new ContractValidateException(BAD_PARAM_ID);
        }
        if (value < 0 || value > LONG_VALUE) {
          throw new ContractValidateException(LONG_VALUE_ERROR);
        }
        break;
      }
      case ALLOW_MULTI_SIGN: {
        if (!forkUtils.pass(ForkBlockVersionEnum.VERSION_3_5)) {
          throw new ContractValidateException("Bad chain parameter id: ALLOW_MULTI_SIGN");
        }
        if (value != 1) {
          throw new ContractValidateException(
              "This value[ALLOW_MULTI_SIGN] is only allowed to be 1");
        }
        break;
      }
      case ALLOW_ADAPTIVE_ENERGY: {
        if (!forkUtils.pass(ForkBlockVersionEnum.VERSION_3_5)) {
          throw new ContractValidateException("Bad chain parameter id: ALLOW_ADAPTIVE_ENERGY");
        }
        if (value != 1) {
          throw new ContractValidateException(
              "This value[ALLOW_ADAPTIVE_ENERGY] is only allowed to be 1");
        }
        break;
      }
      case UPDATE_ACCOUNT_PERMISSION_FEE: {
        if (!forkUtils.pass(ForkBlockVersionEnum.VERSION_3_5)) {
          throw new ContractValidateException(
              "Bad chain parameter id: UPDATE_ACCOUNT_PERMISSION_FEE");
        }
        if (value < 0 || value > 100_000_000_000L) {
          throw new ContractValidateException(
              "Bad chain parameter value, valid range is [0,100_000_000_000L]");
        }
        break;
      }
      case MULTI_SIGN_FEE: {
        if (!forkUtils.pass(ForkBlockVersionEnum.VERSION_3_5)) {
          throw new ContractValidateException("Bad chain parameter id: MULTI_SIGN_FEE");
        }
        if (value < 0 || value > 100_000_000_000L) {
          throw new ContractValidateException(
              "Bad chain parameter value, valid range is [0,100_000_000_000L]");
        }
        break;
      }
      case ALLOW_PROTO_FILTER_NUM: {
        if (!forkUtils.pass(ForkBlockVersionEnum.VERSION_3_6)) {
          throw new ContractValidateException(BAD_PARAM_ID);
        }
        if (value != 1 && value != 0) {
          throw new ContractValidateException(
              "This value[ALLOW_PROTO_FILTER_NUM] is only allowed to be 1 or 0");
        }
        break;
      }
      case ALLOW_ACCOUNT_STATE_ROOT: {
        if (!forkUtils.pass(ForkBlockVersionEnum.VERSION_3_6)) {
          throw new ContractValidateException(BAD_PARAM_ID);
        }
        if (value != 1 && value != 0) {
          throw new ContractValidateException(
              "This value[ALLOW_ACCOUNT_STATE_ROOT] is only allowed to be 1 or 0");
        }
        break;
      }
      case ALLOW_TVM_CONSTANTINOPLE: {
        if (!forkUtils.pass(ForkBlockVersionEnum.VERSION_3_6)) {
          throw new ContractValidateException(BAD_PARAM_ID);
        }
        if (value != 1) {
          throw new ContractValidateException(
              "This value[ALLOW_TVM_CONSTANTINOPLE] is only allowed to be 1");
        }
        if (dynamicPropertiesStore.getAllowTvmTransferTrc10() == 0) {
          throw new ContractValidateException(
              "[ALLOW_TVM_TRANSFER_TRC10] proposal must be approved "
                  + "before [ALLOW_TVM_CONSTANTINOPLE] can be proposed");
        }
        break;
      }
      case ALLOW_TVM_SOLIDITY_059: {
        if (!forkUtils.pass(ForkBlockVersionEnum.VERSION_3_6_5)) {

          throw new ContractValidateException(BAD_PARAM_ID);
        }
        if (value != 1) {
          throw new ContractValidateException(
              "This value[ALLOW_TVM_SOLIDITY_059] is only allowed to be 1");
        }
        if (dynamicPropertiesStore.getAllowCreationOfContracts() == 0) {
          throw new ContractValidateException(
              "[ALLOW_CREATION_OF_CONTRACTS] proposal must be approved "
                  + "before [ALLOW_TVM_SOLIDITY_059] can be proposed");
        }
        break;
      }
      case ADAPTIVE_RESOURCE_LIMIT_TARGET_RATIO: {
        if (!forkUtils.pass(ForkBlockVersionEnum.VERSION_3_6_5)) {
          throw new ContractValidateException(BAD_PARAM_ID);
        }
        if (value < 1 || value > 1_000) {
          throw new ContractValidateException(
              "Bad chain parameter value, valid range is [1,1_000]");
        }
        break;
      }
      case ADAPTIVE_RESOURCE_LIMIT_MULTIPLIER: {
        if (!forkUtils.pass(ForkBlockVersionEnum.VERSION_3_6_5)) {
          throw new ContractValidateException(BAD_PARAM_ID);
        }
        if (value < 1 || value > 10_000L) {
          throw new ContractValidateException(
              "Bad chain parameter value, valid range is [1,10_000]");
        }
        break;
      }
      case ALLOW_CHANGE_DELEGATION: {
        if (!forkUtils.pass(ForkBlockVersionEnum.VERSION_3_6_5)) {
          throw new ContractValidateException(BAD_PARAM_ID);
        }
        if (value != 1 && value != 0) {
          throw new ContractValidateException(
              "This value[ALLOW_CHANGE_DELEGATION] is only allowed to be 1 or 0");
        }
        break;
      }
      case WITNESS_127_PAY_PER_BLOCK: {
        if (!forkUtils.pass(ForkBlockVersionEnum.VERSION_3_6_5)) {
          throw new ContractValidateException(BAD_PARAM_ID);
        }
        if (value < 0 || value > LONG_VALUE) {
          throw new ContractValidateException(LONG_VALUE_ERROR);
        }
        break;
      }
//      case ALLOW_SHIELDED_TRANSACTION: {
//        if (!forkUtils.pass(ForkBlockVersionEnum.VERSION_4_0)) {
//          throw new ContractValidateException(
//              "Bad chain parameter id [ALLOW_SHIELDED_TRANSACTION]");
//        }
//        if (value != 1) {
//          throw new ContractValidateException(
//              "This value[ALLOW_SHIELDED_TRANSACTION] is only allowed to be 1");
//        }
//        break;
//      }
//      case SHIELDED_TRANSACTION_FEE: {
//        if (!forkUtils.pass(ForkBlockVersionEnum.VERSION_4_0)) {
//          throw new ContractValidateException("Bad chain parameter id [SHIELD_TRANSACTION_FEE]");
//        }
//        if (!dynamicPropertiesStore.supportShieldedTransaction()) {
//          throw new ContractValidateException(
//              "Shielded Transaction is not activated, can not set Shielded Transaction fee");
//        }
//        if (value < 0 || value > 10_000_000_000L) {
//          throw new ContractValidateException(
//              "Bad SHIELD_TRANSACTION_FEE parameter value, valid range is [0,10_000_000_000L]");
//        }
//        break;
//      }
//      case SHIELDED_TRANSACTION_CREATE_ACCOUNT_FEE: {
//        if (!forkUtils.pass(ForkBlockVersionEnum.VERSION_4_0)) {
//          throw new ContractValidateException("Bad chain parameter id [SHIELDED_TRANSACTION_CREATE_ACCOUNT_FEE]");
//        }
//        if (value < 0 || value > 10_000_000_000L) {
//          throw new ContractValidateException(
//              "Bad SHIELDED_TRANSACTION_CREATE_ACCOUNT_FEE parameter value,valid range is [0,10_000_000_000L]");
//        }
//        break;
//      }
        case FORBID_TRANSFER_TO_CONTRACT: {
        if (!forkUtils.pass(ForkBlockVersionEnum.VERSION_3_6_6)) {

          throw new ContractValidateException(BAD_PARAM_ID);
        }
        if (value != 1) {
          throw new ContractValidateException(
              "This value[FORBID_TRANSFER_TO_CONTRACT] is only allowed to be 1");
        }
        if (dynamicPropertiesStore.getAllowCreationOfContracts() == 0) {
          throw new ContractValidateException(
              "[ALLOW_CREATION_OF_CONTRACTS] proposal must be approved "
                  + "before [FORBID_TRANSFER_TO_CONTRACT] can be proposed");
        }
        break;
      }
      case ALLOW_SHIELDED_TRC20_TRANSACTION: {
        if (!forkUtils.pass(ForkBlockVersionEnum.VERSION_4_0_1)) {
          throw new ContractValidateException(
              "Bad chain parameter id [ALLOW_SHIELDED_TRC20_TRANSACTION]");
        }
        if (value != 1 && value != 0) {
          throw new ContractValidateException(
              "This value[ALLOW_SHIELDED_TRC20_TRANSACTION] is only allowed to be 1 or 0");
        }
        break;
      }
      default:
        break;
    }
  }

  public enum ProposalType {
    MAINTENANCE_TIME_INTERVAL(0), //ms  ,0
    ACCOUNT_UPGRADE_COST(1), //drop ,1
    CREATE_ACCOUNT_FEE(2), //drop ,2
    TRANSACTION_FEE(3), //drop ,3
    ASSET_ISSUE_FEE(4), //drop ,4
    WITNESS_PAY_PER_BLOCK(5), //drop ,5
    WITNESS_STANDBY_ALLOWANCE(6), //drop ,6
    CREATE_NEW_ACCOUNT_FEE_IN_SYSTEM_CONTRACT(7), //drop ,7
    CREATE_NEW_ACCOUNT_BANDWIDTH_RATE(8), // 1 ~ ,8
    ALLOW_CREATION_OF_CONTRACTS(9), // 0 / >0 ,9
    REMOVE_THE_POWER_OF_THE_GR(10),  // 1 ,10
    ENERGY_FEE(11), // drop, 11
    EXCHANGE_CREATE_FEE(12), // drop, 12
    MAX_CPU_TIME_OF_ONE_TX(13), // ms, 13
    ALLOW_UPDATE_ACCOUNT_NAME(14), // 1, 14
    ALLOW_SAME_TOKEN_NAME(15), // 1, 15
    ALLOW_DELEGATE_RESOURCE(16), // 0, 16
    TOTAL_ENERGY_LIMIT(17), // 50,000,000,000, 17
    ALLOW_TVM_TRANSFER_TRC10(18), // 1, 18
    TOTAL_CURRENT_ENERGY_LIMIT(19), // 50,000,000,000, 19
    ALLOW_MULTI_SIGN(20), // 1, 20
    ALLOW_ADAPTIVE_ENERGY(21), // 1, 21
    UPDATE_ACCOUNT_PERMISSION_FEE(22), // 100, 22
    MULTI_SIGN_FEE(23), // 1, 23
    ALLOW_PROTO_FILTER_NUM(24), // 1, 24
    ALLOW_ACCOUNT_STATE_ROOT(25), // 1, 25
    ALLOW_TVM_CONSTANTINOPLE(26), // 1, 26
//    ALLOW_SHIELDED_TRANSACTION(27), // 27
//    SHIELDED_TRANSACTION_FEE(28), // 28
    ADAPTIVE_RESOURCE_LIMIT_MULTIPLIER(29), // 1000, 29
    ALLOW_CHANGE_DELEGATION(30), //1, 30
    WITNESS_127_PAY_PER_BLOCK(31), //drop, 31
    ALLOW_TVM_SOLIDITY_059(32), // 1, 32
    ADAPTIVE_RESOURCE_LIMIT_TARGET_RATIO(33), // 10, 33
    //    SHIELDED_TRANSACTION_CREATE_ACCOUNT_FEE(34); // 34
    FORBID_TRANSFER_TO_CONTRACT(35), // 1, 35
    ALLOW_SHIELDED_TRC20_TRANSACTION(39); // 1, 39

    private long code;

    ProposalType(long code) {
      this.code = code;
    }

    public static boolean contain(long code) {
      for (ProposalType parameters : values()) {
        if (parameters.code == code) {
          return true;
        }
      }
      return false;
    }

    public static ProposalType getEnum(long code) throws ContractValidateException {
      for (ProposalType parameters : values()) {
        if (parameters.code == code) {
          return parameters;
        }
      }
      throw new ContractValidateException("Does not support code : " + code);
    }

    public static ProposalType getEnumOrNull(long code) {
      for (ProposalType parameters : values()) {
        if (parameters.code == code) {
          return parameters;
        }
      }
      return null;
    }

    public long getCode() {
      return code;
    }
  }
}
