package org.tron.core.services;

import static org.tron.core.config.Parameter.ChainConstant.PRECISION;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.capsule.ProposalCapsule;
import org.tron.core.config.Parameter.ForkBlockVersionConsts;
import org.tron.core.config.Parameter.ForkBlockVersionEnum;
import org.tron.core.db.Manager;
import org.tron.core.exception.ContractValidateException;

/**
 * Notice:
 *
 * if you want to add a proposal,you just should add a enum ProposalType and add the valid in the
 * validator method, add the process in the process method
 */
@Slf4j
public class ProposalService {

  private static final long LONG_VALUE = 100_000_000_000_000_000L;
  private static final String LONG_VALUE_ERROR =
      "Bad chain parameter value,valid range is [0," + LONG_VALUE + "]";
  private static final String BAD_PARAM_ID = "Bad chain parameter id";
  private static final int payBlockLength = 5;

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
    ALLOW_TVM_SOLIDITY_059(27), // 1, 27
    ADAPTIVE_RESOURCE_LIMIT_TARGET_RATIO(28), // 10, 28
    ADAPTIVE_RESOURCE_LIMIT_MULTIPLIER(29), // 1000, 29
    ALLOW_CHANGE_DELEGATION(30), //1, 30
    WITNESS_PAY_PER_BLOCK_STANDBY_ALLOWANCE(31); //16160, 31

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
      throw new ContractValidateException("not support code : " + code);
    }

    public static ProposalType getEnumOrNull(long code) {
      for (ProposalType parameters : values()) {
        if (parameters.code == code) {
          return parameters;
        }
      }
      return null;
    }

    private long code;

    public long getCode() {
      return code;
    }
  }

  public static void validator(Manager manager, long code, long value)
      throws ContractValidateException {
    ProposalType proposalType = ProposalType.getEnum(code);
    switch (proposalType) {
      case MAINTENANCE_TIME_INTERVAL: {
        if (value < 3 * 27 * 1000 || value > 24 * 3600 * 1000) {
          throw new ContractValidateException(
              "Bad chain parameter value,valid range is [3 * 27 * 1000,24 * 3600 * 1000]");
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
        if (manager.getDynamicPropertiesStore().getRemoveThePowerOfTheGr() == -1) {
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
              "Bad chain parameter value,valid range is [10,100]");
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
        if (!manager.getForkController().pass(ForkBlockVersionConsts.ENERGY_LIMIT)) {
          throw new ContractValidateException(BAD_PARAM_ID);
        }
        if (manager.getForkController().pass(ForkBlockVersionEnum.VERSION_3_2_2)) {
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
        if (manager.getDynamicPropertiesStore().getAllowSameTokenName() == 0) {
          throw new ContractValidateException("[ALLOW_SAME_TOKEN_NAME] proposal must be approved "
              + "before [ALLOW_TVM_TRANSFER_TRC10] can be proposed");
        }
        break;
      }
      case TOTAL_CURRENT_ENERGY_LIMIT: {
        if (!manager.getForkController().pass(ForkBlockVersionEnum.VERSION_3_2_2)) {
          throw new ContractValidateException(BAD_PARAM_ID);
        }
        if (value < 0 || value > LONG_VALUE) {
          throw new ContractValidateException(LONG_VALUE_ERROR);
        }
        break;
      }
      case ALLOW_MULTI_SIGN: {
        if (!manager.getForkController().pass(ForkBlockVersionEnum.VERSION_3_5)) {
          throw new ContractValidateException("Bad chain parameter id: ALLOW_MULTI_SIGN");
        }
        if (value != 1) {
          throw new ContractValidateException(
              "This value[ALLOW_MULTI_SIGN] is only allowed to be 1");
        }
        break;
      }
      case ALLOW_ADAPTIVE_ENERGY: {
        if (!manager.getForkController().pass(ForkBlockVersionEnum.VERSION_3_5)) {
          throw new ContractValidateException("Bad chain parameter id: ALLOW_ADAPTIVE_ENERGY");
        }
        if (value != 1) {
          throw new ContractValidateException(
              "This value[ALLOW_ADAPTIVE_ENERGY] is only allowed to be 1");
        }
        break;
      }
      case UPDATE_ACCOUNT_PERMISSION_FEE: {
        if (!manager.getForkController().pass(ForkBlockVersionEnum.VERSION_3_5)) {
          throw new ContractValidateException(
              "Bad chain parameter id: UPDATE_ACCOUNT_PERMISSION_FEE");
        }
        if (value < 0 || value > 100_000_000_000L) {
          throw new ContractValidateException(
              "Bad chain parameter value,valid range is [0,100_000_000_000L]");
        }
        break;
      }
      case MULTI_SIGN_FEE: {
        if (!manager.getForkController().pass(ForkBlockVersionEnum.VERSION_3_5)) {
          throw new ContractValidateException("Bad chain parameter id: MULTI_SIGN_FEE");
        }
        if (value < 0 || value > 100_000_000_000L) {
          throw new ContractValidateException(
              "Bad chain parameter value,valid range is [0,100_000_000_000L]");
        }
        break;
      }
      case ALLOW_PROTO_FILTER_NUM: {
        if (!manager.getForkController().pass(ForkBlockVersionEnum.VERSION_3_6)) {
          throw new ContractValidateException(BAD_PARAM_ID);
        }
        if (value != 1 && value != 0) {
          throw new ContractValidateException(
              "This value[ALLOW_PROTO_FILTER_NUM] is only allowed to be 1 or 0");
        }
        break;
      }
      case ALLOW_ACCOUNT_STATE_ROOT: {
        if (!manager.getForkController().pass(ForkBlockVersionEnum.VERSION_3_6)) {
          throw new ContractValidateException(BAD_PARAM_ID);
        }
        if (value != 1 && value != 0) {
          throw new ContractValidateException(
              "This value[ALLOW_ACCOUNT_STATE_ROOT] is only allowed to be 1 or 0");
        }
        break;
      }
      case ALLOW_TVM_CONSTANTINOPLE: {
        if (!manager.getForkController().pass(ForkBlockVersionEnum.VERSION_3_6)) {
          throw new ContractValidateException(BAD_PARAM_ID);
        }
        if (value != 1) {
          throw new ContractValidateException(
              "This value[ALLOW_TVM_CONSTANTINOPLE] is only allowed to be 1");
        }
        if (manager.getDynamicPropertiesStore().getAllowTvmTransferTrc10() == 0) {
          throw new ContractValidateException(
              "[ALLOW_TVM_TRANSFER_TRC10] proposal must be approved "
                  + "before [ALLOW_TVM_CONSTANTINOPLE] can be proposed");
        }
        break;
      }
      case ALLOW_TVM_SOLIDITY_059: {
        if (!manager.getForkController().pass(ForkBlockVersionEnum.VERSION_3_6_5)) {

          throw new ContractValidateException(BAD_PARAM_ID);
        }
        if (value != 1) {
          throw new ContractValidateException(
              "This value[ALLOW_TVM_SOLIDITY_059] is only allowed to be 1");
        }
        if (manager.getDynamicPropertiesStore().getAllowCreationOfContracts() == 0) {
          throw new ContractValidateException(
              "[ALLOW_CREATION_OF_CONTRACTS] proposal must be approved "
                  + "before [ALLOW_TVM_SOLIDITY_059] can be proposed");
        }
        break;
      }
      case ADAPTIVE_RESOURCE_LIMIT_TARGET_RATIO: {
        if (!manager.getForkController().pass(ForkBlockVersionEnum.VERSION_3_6_5)) {
          throw new ContractValidateException(BAD_PARAM_ID);
        }
        if (value < 1 || value > 1_000) {
          throw new ContractValidateException(
              "Bad chain parameter value,valid range is [1,1_000]");
        }
        break;
      }
      case ADAPTIVE_RESOURCE_LIMIT_MULTIPLIER: {
        if (!manager.getForkController().pass(ForkBlockVersionEnum.VERSION_3_6_5)) {
          throw new ContractValidateException(BAD_PARAM_ID);
        }
        if (value < 1 || value > 10_000L) {
          throw new ContractValidateException(
              "Bad chain parameter value,valid range is [1,10_000]");
        }
        break;
      }
      case ALLOW_CHANGE_DELEGATION: {
        if (!manager.getForkController().pass(ForkBlockVersionEnum.VERSION_3_6_5)) {
          throw new ContractValidateException(BAD_PARAM_ID);
        }
        if (value != 1 && value != 0) {
          throw new ContractValidateException(
              "This value[ALLOW_CHANGE_DELEGATION] is only allowed to be 1 or 0");
        }
        break;
      }
      case WITNESS_PAY_PER_BLOCK_STANDBY_ALLOWANCE: {
        if (!manager.getForkController().pass(ForkBlockVersionEnum.VERSION_3_6_5)) {
          throw new ContractValidateException(BAD_PARAM_ID);
        }
        if (String.valueOf(value).length() != payBlockLength) {
          throw new ContractValidateException(
              "This value[WITNESS_PAY_PER_BLOCK_STANDBY_ALLOWANCE] length must be "
                  + payBlockLength);
        }
        break;
      }
      default:
        break;
    }
  }

  public static boolean process(Manager manager, ProposalCapsule proposalCapsule) {
    Map<Long, Long> map = proposalCapsule.getInstance().getParametersMap();
    boolean find = true;
    for (Map.Entry<Long, Long> entry : map.entrySet()) {
      ProposalType proposalType = ProposalType.getEnumOrNull(entry.getKey());
      if (proposalType == null) {
        find = false;
        continue;
      }
      switch (proposalType) {
        case MAINTENANCE_TIME_INTERVAL: {
          manager.getDynamicPropertiesStore().saveMaintenanceTimeInterval(entry.getValue());
          break;
        }
        case ACCOUNT_UPGRADE_COST: {
          manager.getDynamicPropertiesStore().saveAccountUpgradeCost(entry.getValue());
          break;
        }
        case CREATE_ACCOUNT_FEE: {
          manager.getDynamicPropertiesStore().saveCreateAccountFee(entry.getValue());
          break;
        }
        case TRANSACTION_FEE: {
          manager.getDynamicPropertiesStore().saveTransactionFee(entry.getValue());
          break;
        }
        case ASSET_ISSUE_FEE: {
          manager.getDynamicPropertiesStore().saveAssetIssueFee(entry.getValue());
          break;
        }
        case WITNESS_PAY_PER_BLOCK: {
          manager.getDynamicPropertiesStore().saveWitnessPayPerBlock(entry.getValue());
          break;
        }
        case WITNESS_STANDBY_ALLOWANCE: {
          manager.getDynamicPropertiesStore().saveWitnessStandbyAllowance(entry.getValue());
          break;
        }
        case CREATE_NEW_ACCOUNT_FEE_IN_SYSTEM_CONTRACT: {
          manager.getDynamicPropertiesStore()
              .saveCreateNewAccountFeeInSystemContract(entry.getValue());
          break;
        }
        case CREATE_NEW_ACCOUNT_BANDWIDTH_RATE: {
          manager.getDynamicPropertiesStore().saveCreateNewAccountBandwidthRate(entry.getValue());
          break;
        }
        case ALLOW_CREATION_OF_CONTRACTS: {
          manager.getDynamicPropertiesStore().saveAllowCreationOfContracts(entry.getValue());
          break;
        }
        case REMOVE_THE_POWER_OF_THE_GR: {
          if (manager.getDynamicPropertiesStore().getRemoveThePowerOfTheGr() == 0) {
            manager.getDynamicPropertiesStore().saveRemoveThePowerOfTheGr(entry.getValue());
          }
          break;
        }
        case ENERGY_FEE: {
          manager.getDynamicPropertiesStore().saveEnergyFee(entry.getValue());
          break;
        }
        case EXCHANGE_CREATE_FEE: {
          manager.getDynamicPropertiesStore().saveExchangeCreateFee(entry.getValue());
          break;
        }
        case MAX_CPU_TIME_OF_ONE_TX: {
          manager.getDynamicPropertiesStore().saveMaxCpuTimeOfOneTx(entry.getValue());
          break;
        }
        case ALLOW_UPDATE_ACCOUNT_NAME: {
          manager.getDynamicPropertiesStore().saveAllowUpdateAccountName(entry.getValue());
          break;
        }
        case ALLOW_SAME_TOKEN_NAME: {
          manager.getDynamicPropertiesStore().saveAllowSameTokenName(entry.getValue());
          break;
        }
        case ALLOW_DELEGATE_RESOURCE: {
          manager.getDynamicPropertiesStore().saveAllowDelegateResource(entry.getValue());
          break;
        }
        case TOTAL_ENERGY_LIMIT: {
          manager.getDynamicPropertiesStore().saveTotalEnergyLimit(entry.getValue());
          break;
        }
        case ALLOW_TVM_TRANSFER_TRC10: {
          manager.getDynamicPropertiesStore().saveAllowTvmTransferTrc10(entry.getValue());
          break;
        }
        case TOTAL_CURRENT_ENERGY_LIMIT: {
          manager.getDynamicPropertiesStore().saveTotalEnergyLimit2(entry.getValue());
          break;
        }
        case ALLOW_MULTI_SIGN: {
          if (manager.getDynamicPropertiesStore().getAllowMultiSign() == 0) {
            manager.getDynamicPropertiesStore().saveAllowMultiSign(entry.getValue());
          }
          break;
        }
        case ALLOW_ADAPTIVE_ENERGY: {
          if (manager.getDynamicPropertiesStore().getAllowAdaptiveEnergy() == 0) {
            manager.getDynamicPropertiesStore().saveAllowAdaptiveEnergy(entry.getValue());
            if (manager.getForkController().pass(ForkBlockVersionEnum.VERSION_3_6_5)) {
              //24 * 60 * 2 . one minute,1/2 total limit.
              manager.getDynamicPropertiesStore().saveAdaptiveResourceLimitTargetRatio(2880);
              manager.getDynamicPropertiesStore().saveTotalEnergyTargetLimit(
                  manager.getDynamicPropertiesStore().getTotalEnergyLimit() / 2880);
              manager.getDynamicPropertiesStore().saveAdaptiveResourceLimitMultiplier(50);
            }
          }
          break;
        }
        case UPDATE_ACCOUNT_PERMISSION_FEE: {
          manager.getDynamicPropertiesStore().saveUpdateAccountPermissionFee(entry.getValue());
          break;
        }
        case MULTI_SIGN_FEE: {
          manager.getDynamicPropertiesStore().saveMultiSignFee(entry.getValue());
          break;
        }
        case ALLOW_PROTO_FILTER_NUM: {
          manager.getDynamicPropertiesStore().saveAllowProtoFilterNum(entry.getValue());
          break;
        }
        case ALLOW_ACCOUNT_STATE_ROOT: {
          manager.getDynamicPropertiesStore().saveAllowAccountStateRoot(entry.getValue());
          break;
        }
        case ALLOW_TVM_CONSTANTINOPLE: {
          manager.getDynamicPropertiesStore().saveAllowTvmConstantinople(entry.getValue());
          manager.getDynamicPropertiesStore().addSystemContractAndSetPermission(48);
          break;
        }
        case ALLOW_TVM_SOLIDITY_059: {
          manager.getDynamicPropertiesStore().saveAllowTvmSolidity059(entry.getValue());
          break;
        }
        case ADAPTIVE_RESOURCE_LIMIT_TARGET_RATIO: {
          long ratio = 24 * 60 * entry.getValue();
          manager.getDynamicPropertiesStore().saveAdaptiveResourceLimitTargetRatio(ratio);
          manager.getDynamicPropertiesStore().saveTotalEnergyTargetLimit(
              manager.getDynamicPropertiesStore().getTotalEnergyLimit() / ratio);
          break;
        }
        case ADAPTIVE_RESOURCE_LIMIT_MULTIPLIER: {
          manager.getDynamicPropertiesStore().saveAdaptiveResourceLimitMultiplier(entry.getValue());
          break;
        }
        case ALLOW_CHANGE_DELEGATION: {
          manager.getDynamicPropertiesStore().saveChangeDelegation(entry.getValue());
          manager.getDynamicPropertiesStore().addSystemContractAndSetPermission(49);
          break;
        }
        case WITNESS_PAY_PER_BLOCK_STANDBY_ALLOWANCE: {
          //before 2 is use to pay block and next 3 is use to 127 sr pay
          long value = entry.getValue();
          long payBlockValue = Long.valueOf(String.valueOf(value).substring(0, 2));
          long pay127SrValue = Long.valueOf(String.valueOf(value).substring(2, payBlockLength));
          manager.getDynamicPropertiesStore().saveWitnessPayPerBlock(payBlockValue * PRECISION);
          manager.getDynamicPropertiesStore().saveWitness127PayPerBlock(pay127SrValue * PRECISION);
          break;
        }
        default:
          find = false;
          break;
      }
    }
    return find;
  }

}
