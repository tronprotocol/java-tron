package org.tron.core.utils;

import static org.tron.core.Constant.CREATE_ACCOUNT_TRANSACTION_MAX_BYTE_SIZE;
import static org.tron.core.Constant.CREATE_ACCOUNT_TRANSACTION_MIN_BYTE_SIZE;
import static org.tron.core.Constant.DYNAMIC_ENERGY_INCREASE_FACTOR_RANGE;
import static org.tron.core.Constant.DYNAMIC_ENERGY_MAX_FACTOR_RANGE;
import static org.tron.core.config.Parameter.ChainConstant.ONE_YEAR_BLOCK_NUMBERS;

import org.tron.common.utils.ForkController;
import org.tron.core.config.Parameter.ForkBlockVersionConsts;
import org.tron.core.config.Parameter.ForkBlockVersionEnum;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.store.DynamicPropertiesStore;

public class ProposalUtil {

  protected static final long LONG_VALUE = 100_000_000_000_000_000L;
  protected static final String BAD_PARAM_ID = "Bad chain parameter id";
  private static final String LONG_VALUE_ERROR =
      "Bad chain parameter value, valid range is [0," + LONG_VALUE + "]";
  private static final String PRE_VALUE_NOT_ONE_ERROR = "This value[";
  private static final String VALUE_NOT_ONE_ERROR = "] is only allowed to be 1";
  private static final long MAX_SUPPLY = 100_000_000_000L;
  private static final String MAX_SUPPLY_ERROR
      = "Bad chain parameter value, valid range is [0, 100_000_000_000L]";

  public static void validator(DynamicPropertiesStore dynamicPropertiesStore,
      ForkController forkController,
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
              PRE_VALUE_NOT_ONE_ERROR + "ALLOW_CREATION_OF_CONTRACTS" + VALUE_NOT_ONE_ERROR);
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
              PRE_VALUE_NOT_ONE_ERROR + "REMOVE_THE_POWER_OF_THE_GR" + VALUE_NOT_ONE_ERROR);
        }
        break;
      }
      case ENERGY_FEE:
      case EXCHANGE_CREATE_FEE:
        break;
      case MAX_CPU_TIME_OF_ONE_TX:
        if (dynamicPropertiesStore.getAllowHigherLimitForMaxCpuTimeOfOneTx() == 1) {
          if (value < 10 || value > 400) {
            throw new ContractValidateException(
                "Bad chain parameter value, valid range is [10,400]");
          }
        } else {
          if (value < 10 || value > 100) {
            throw new ContractValidateException(
                "Bad chain parameter value, valid range is [10,100]");
          }
        }
        break;
      case ALLOW_UPDATE_ACCOUNT_NAME: {
        if (value != 1) {
          throw new ContractValidateException(
              PRE_VALUE_NOT_ONE_ERROR + "ALLOW_UPDATE_ACCOUNT_NAME" + VALUE_NOT_ONE_ERROR);
        }
        break;
      }
      case ALLOW_SAME_TOKEN_NAME: {
        if (value != 1) {
          throw new ContractValidateException(
              PRE_VALUE_NOT_ONE_ERROR + "ALLOW_SAME_TOKEN_NAME" + VALUE_NOT_ONE_ERROR);
        }
        break;
      }
      case ALLOW_DELEGATE_RESOURCE: {
        if (value != 1) {
          throw new ContractValidateException(
              PRE_VALUE_NOT_ONE_ERROR + "ALLOW_DELEGATE_RESOURCE" + VALUE_NOT_ONE_ERROR);
        }
        break;
      }
      case TOTAL_ENERGY_LIMIT: { // deprecated
        if (!forkController.pass(ForkBlockVersionConsts.ENERGY_LIMIT)) {
          throw new ContractValidateException(BAD_PARAM_ID);
        }
        if (forkController.pass(ForkBlockVersionEnum.VERSION_3_2_2)) {
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
              PRE_VALUE_NOT_ONE_ERROR + "ALLOW_TVM_TRANSFER_TRC10" + VALUE_NOT_ONE_ERROR);
        }
        if (dynamicPropertiesStore.getAllowSameTokenName() == 0) {
          throw new ContractValidateException("[ALLOW_SAME_TOKEN_NAME] proposal must be approved "
              + "before [ALLOW_TVM_TRANSFER_TRC10] can be proposed");
        }
        break;
      }
      case TOTAL_CURRENT_ENERGY_LIMIT: {
        if (!forkController.pass(ForkBlockVersionEnum.VERSION_3_2_2)) {
          throw new ContractValidateException(BAD_PARAM_ID);
        }
        if (value < 0 || value > LONG_VALUE) {
          throw new ContractValidateException(LONG_VALUE_ERROR);
        }
        break;
      }
      case ALLOW_MULTI_SIGN: {
        if (!forkController.pass(ForkBlockVersionEnum.VERSION_3_5)) {
          throw new ContractValidateException("Bad chain parameter id: ALLOW_MULTI_SIGN");
        }
        if (value != 1) {
          throw new ContractValidateException(
              PRE_VALUE_NOT_ONE_ERROR + "ALLOW_MULTI_SIGN" + VALUE_NOT_ONE_ERROR);
        }
        break;
      }
      case ALLOW_ADAPTIVE_ENERGY: {
        if (!forkController.pass(ForkBlockVersionEnum.VERSION_3_5)) {
          throw new ContractValidateException("Bad chain parameter id: ALLOW_ADAPTIVE_ENERGY");
        }
        if (value != 1) {
          throw new ContractValidateException(
              PRE_VALUE_NOT_ONE_ERROR + "ALLOW_ADAPTIVE_ENERGY" + VALUE_NOT_ONE_ERROR);
        }
        break;
      }
      case UPDATE_ACCOUNT_PERMISSION_FEE: {
        if (!forkController.pass(ForkBlockVersionEnum.VERSION_3_5)) {
          throw new ContractValidateException(
              "Bad chain parameter id: UPDATE_ACCOUNT_PERMISSION_FEE");
        }
        if (value < 0 || value > MAX_SUPPLY) {
          throw new ContractValidateException(MAX_SUPPLY_ERROR);
        }
        break;
      }
      case MULTI_SIGN_FEE: {
        if (!forkController.pass(ForkBlockVersionEnum.VERSION_3_5)) {
          throw new ContractValidateException("Bad chain parameter id: MULTI_SIGN_FEE");
        }
        if (value < 0 || value > MAX_SUPPLY) {
          throw new ContractValidateException(MAX_SUPPLY_ERROR);
        }
        break;
      }
      case ALLOW_PROTO_FILTER_NUM: {
        if (!forkController.pass(ForkBlockVersionEnum.VERSION_3_6)) {
          throw new ContractValidateException(BAD_PARAM_ID);
        }
        if (value != 1 && value != 0) {
          throw new ContractValidateException(
              "This value[ALLOW_PROTO_FILTER_NUM] is only allowed to be 1 or 0");
        }
        break;
      }
      case ALLOW_ACCOUNT_STATE_ROOT: {
        if (!forkController.pass(ForkBlockVersionEnum.VERSION_3_6)) {
          throw new ContractValidateException(BAD_PARAM_ID);
        }
        if (value != 1 && value != 0) {
          throw new ContractValidateException(
              "This value[ALLOW_ACCOUNT_STATE_ROOT] is only allowed to be 1 or 0");
        }
        break;
      }
      case ALLOW_TVM_CONSTANTINOPLE: {
        if (!forkController.pass(ForkBlockVersionEnum.VERSION_3_6)) {
          throw new ContractValidateException(BAD_PARAM_ID);
        }
        if (value != 1) {
          throw new ContractValidateException(
              PRE_VALUE_NOT_ONE_ERROR + "ALLOW_TVM_CONSTANTINOPLE" + VALUE_NOT_ONE_ERROR);
        }
        if (dynamicPropertiesStore.getAllowTvmTransferTrc10() == 0) {
          throw new ContractValidateException(
              "[ALLOW_TVM_TRANSFER_TRC10] proposal must be approved "
                  + "before [ALLOW_TVM_CONSTANTINOPLE] can be proposed");
        }
        break;
      }
      case ALLOW_TVM_SOLIDITY_059: {
        if (!forkController.pass(ForkBlockVersionEnum.VERSION_3_6_5)) {

          throw new ContractValidateException(BAD_PARAM_ID);
        }
        if (value != 1) {
          throw new ContractValidateException(
              PRE_VALUE_NOT_ONE_ERROR + "ALLOW_TVM_SOLIDITY_059" + VALUE_NOT_ONE_ERROR);
        }
        if (dynamicPropertiesStore.getAllowCreationOfContracts() == 0) {
          throw new ContractValidateException(
              "[ALLOW_CREATION_OF_CONTRACTS] proposal must be approved "
                  + "before [ALLOW_TVM_SOLIDITY_059] can be proposed");
        }
        break;
      }
      case ADAPTIVE_RESOURCE_LIMIT_TARGET_RATIO: {
        if (!forkController.pass(ForkBlockVersionEnum.VERSION_3_6_5)) {
          throw new ContractValidateException(BAD_PARAM_ID);
        }
        if (value < 1 || value > 1_000) {
          throw new ContractValidateException(
              "Bad chain parameter value, valid range is [1,1_000]");
        }
        break;
      }
      case ADAPTIVE_RESOURCE_LIMIT_MULTIPLIER: {
        if (!forkController.pass(ForkBlockVersionEnum.VERSION_3_6_5)) {
          throw new ContractValidateException(BAD_PARAM_ID);
        }
        if (value < 1 || value > 10_000L) {
          throw new ContractValidateException(
              "Bad chain parameter value, valid range is [1,10_000]");
        }
        break;
      }
      case ALLOW_CHANGE_DELEGATION: {
        if (!forkController.pass(ForkBlockVersionEnum.VERSION_3_6_5)) {
          throw new ContractValidateException(BAD_PARAM_ID);
        }
        if (value != 1 && value != 0) {
          throw new ContractValidateException(
              "This value[ALLOW_CHANGE_DELEGATION] is only allowed to be 1 or 0");
        }
        break;
      }
      case WITNESS_127_PAY_PER_BLOCK: {
        if (!forkController.pass(ForkBlockVersionEnum.VERSION_3_6_5)) {
          throw new ContractValidateException(BAD_PARAM_ID);
        }
        if (value < 0 || value > LONG_VALUE) {
          throw new ContractValidateException(LONG_VALUE_ERROR);
        }
        break;
      }
//      case ALLOW_SHIELDED_TRANSACTION: {
//        if (!forkController.pass(ForkBlockVersionEnum.VERSION_4_0)) {
//          throw new ContractValidateException(
//              "Bad chain parameter id [ALLOW_SHIELDED_TRANSACTION]");
//        }
//        if (value != 1) {
//          throw new ContractValidateException(
//                  PRE_VALUE_NOT_ONE_ERROR + "ALLOW_SHIELDED_TRANSACTION" + VALUE_NOT_ONE_ERROR);
//        }
//        break;
//      }
//      case SHIELDED_TRANSACTION_FEE: {
//        if (!forkController.pass(ForkBlockVersionEnum.VERSION_4_0)) {
//          throw new ContractValidateException("Bad chain parameter id [SHIELD_TRANSACTION_FEE]");
//        }
//        if (!dynamicPropertiesStore.supportShieldedTransaction()) {
//          throw new ContractValidateException(
//              "Shielded Transaction is not activated, can not set Shielded Transaction fee");
//        }
//        if (dynamicPropertiesStore.getAllowCreationOfContracts() == 0) {
//          throw new ContractValidateException(
//              "[ALLOW_CREATION_OF_CONTRACTS] proposal must be approved "
//                  + "before [FORBID_TRANSFER_TO_CONTRACT] can be proposed");
//        }
//        break;
//      }
//      case SHIELDED_TRANSACTION_CREATE_ACCOUNT_FEE: {
//        if (!forkController.pass(ForkBlockVersionEnum.VERSION_4_0)) {
//          throw new ContractValidateException(
//              "Bad chain parameter id [SHIELDED_TRANSACTION_CREATE_ACCOUNT_FEE]");
//        }
//        if (value < 0 || value > 10_000_000_000L) {
//          throw new ContractValidateException(
//              "Bad SHIELDED_TRANSACTION_CREATE_ACCOUNT_FEE parameter value, valid range is [0,10_000_000_000L]");
//        }
//        break;
//      }
      case FORBID_TRANSFER_TO_CONTRACT: {
        if (!forkController.pass(ForkBlockVersionEnum.VERSION_3_6_6)) {

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
      case ALLOW_PBFT: {
        if (!forkController.pass(ForkBlockVersionEnum.VERSION_4_1)) {
          throw new ContractValidateException(
              "Bad chain parameter id [ALLOW_PBFT]");
        }
        if (value != 1) {
          throw new ContractValidateException(
              "This value[ALLOW_PBFT] is only allowed to be 1");
        }
        break;
      }
      case ALLOW_TVM_ISTANBUL: {
        if (!forkController.pass(ForkBlockVersionEnum.VERSION_4_1)) {
          throw new ContractValidateException(
              "Bad chain parameter id [ALLOW_TVM_ISTANBUL]");
        }
        if (value != 1) {
          throw new ContractValidateException(
              "This value[ALLOW_TVM_ISTANBUL] is only allowed to be 1");
        }
        break;
      }
      case ALLOW_SHIELDED_TRC20_TRANSACTION: {
        if (!forkController.pass(ForkBlockVersionEnum.VERSION_4_0_1)) {
          throw new ContractValidateException(
              "Bad chain parameter id [ALLOW_SHIELDED_TRC20_TRANSACTION]");
        }
        if (value != 1 && value != 0) {
          throw new ContractValidateException(
              "This value[ALLOW_SHIELDED_TRC20_TRANSACTION] is only allowed to be 1 or 0");
        }
        break;
      }
      case ALLOW_MARKET_TRANSACTION: {
        if (!forkController.pass(ForkBlockVersionEnum.VERSION_4_1)) {
          throw new ContractValidateException(
              "Bad chain parameter id [ALLOW_MARKET_TRANSACTION]");
        }
        if (value != 1) {
          throw new ContractValidateException(
              "This value[ALLOW_MARKET_TRANSACTION] is only allowed to be 1");
        }
        break;
      }
      case MARKET_SELL_FEE: {
        if (!forkController.pass(ForkBlockVersionEnum.VERSION_4_1)) {
          throw new ContractValidateException("Bad chain parameter id [MARKET_SELL_FEE]");
        }
        if (!dynamicPropertiesStore.supportAllowMarketTransaction()) {
          throw new ContractValidateException(
              "Market Transaction is not activated, can not set Market Sell Fee");
        }
        if (value < 0 || value > 10_000_000_000L) {
          throw new ContractValidateException(
              "Bad MARKET_SELL_FEE parameter value, valid range is [0,10_000_000_000L]");
        }
        break;
      }
      case MARKET_CANCEL_FEE: {
        if (!forkController.pass(ForkBlockVersionEnum.VERSION_4_1)) {
          throw new ContractValidateException("Bad chain parameter id [MARKET_CANCEL_FEE]");
        }
        if (!dynamicPropertiesStore.supportAllowMarketTransaction()) {
          throw new ContractValidateException(
              "Market Transaction is not activated, can not set Market Cancel Fee");
        }
        if (value < 0 || value > 10_000_000_000L) {
          throw new ContractValidateException(
              "Bad MARKET_CANCEL_FEE parameter value, valid range is [0,10_000_000_000L]");
        }
        break;
      }
      case MAX_FEE_LIMIT: {
        if (!forkController.pass(ForkBlockVersionEnum.VERSION_4_1_2)) {
          throw new ContractValidateException("Bad chain parameter id [MAX_FEE_LIMIT]");
        }
        if (value < 0) {
          throw new ContractValidateException(
              "Bad MAX_FEE_LIMIT parameter value, value must not be negative");
        } else if (value > 10_000_000_000L) {
          if (dynamicPropertiesStore.getAllowTvmLondon() == 0) {
            throw new ContractValidateException(
                "Bad MAX_FEE_LIMIT parameter value, valid range is [0,10_000_000_000L]");
          }
          if (value > LONG_VALUE) {
            throw new ContractValidateException(LONG_VALUE_ERROR);
          }
        }
        break;
      }
      case ALLOW_TRANSACTION_FEE_POOL: {
        if (!forkController.pass(ForkBlockVersionEnum.VERSION_4_1_2)) {
          throw new ContractValidateException(
              "Bad chain parameter id [ALLOW_TRANSACTION_FEE_POOL]");
        }
        if (value != 1 && value != 0) {
          throw new ContractValidateException(
              "This value[ALLOW_TRANSACTION_FEE_POOL] is only allowed to be 1 or 0");
        }
        break;
      }
      case ALLOW_BLACKHOLE_OPTIMIZATION: {
        if (!forkController.pass(ForkBlockVersionEnum.VERSION_4_1_2)) {
          throw new ContractValidateException(
              "Bad chain parameter id [ALLOW_REMOVE_BLACKHOLE]");
        }
        if (value != 1 && value != 0) {
          throw new ContractValidateException(
              "This value[ALLOW_REMOVE_BLACKHOLE] is only allowed to be 1 or 0");
        }
        break;
      }
      case ALLOW_NEW_RESOURCE_MODEL: {
        if (!forkController.pass(ForkBlockVersionEnum.VERSION_4_2)) {
          throw new ContractValidateException(
              "Bad chain parameter id [ALLOW_NEW_RESOURCE_MODEL]");
        }
        if (value != 1) {
          throw new ContractValidateException(
              "This value[ALLOW_NEW_RESOURCE_MODEL] is only allowed to be 1");
        }
        break;
      }
      case ALLOW_TVM_FREEZE: {
        if (!forkController.pass(ForkBlockVersionEnum.VERSION_4_2)) {
          throw new ContractValidateException(
              "Bad chain parameter id [ALLOW_TVM_FREEZE]");
        }
        if (value != 1) {
          throw new ContractValidateException(
              PRE_VALUE_NOT_ONE_ERROR + "ALLOW_TVM_FREEZE" + VALUE_NOT_ONE_ERROR);
        }
        if (dynamicPropertiesStore.getAllowDelegateResource() == 0) {
          throw new ContractValidateException(
              "[ALLOW_DELEGATE_RESOURCE] proposal must be approved "
                  + "before [ALLOW_TVM_FREEZE] can be proposed");
        }
        if (dynamicPropertiesStore.getAllowMultiSign() == 0) {
          throw new ContractValidateException(
              "[ALLOW_MULTI_SIGN] proposal must be approved "
                  + "before [ALLOW_TVM_FREEZE] can be proposed");
        }
        if (dynamicPropertiesStore.getAllowTvmConstantinople() == 0) {
          throw new ContractValidateException(
              "[ALLOW_TVM_CONSTANTINOPLE] proposal must be approved "
                  + "before [ALLOW_TVM_FREEZE] can be proposed");
        }
        if (dynamicPropertiesStore.getAllowTvmSolidity059() == 0) {
          throw new ContractValidateException(
              "[ALLOW_TVM_SOLIDITY_059] proposal must be approved "
                  + "before [ALLOW_TVM_FREEZE] can be proposed");
        }
        break;
      }
      case ALLOW_TVM_VOTE: {
        if (!forkController.pass(ForkBlockVersionEnum.VERSION_4_3)) {
          throw new ContractValidateException(
              "Bad chain parameter id [ALLOW_TVM_VOTE]");
        }
        if (value != 1) {
          throw new ContractValidateException(
              PRE_VALUE_NOT_ONE_ERROR + "ALLOW_TVM_VOTE" + VALUE_NOT_ONE_ERROR);
        }
        if (dynamicPropertiesStore.getChangeDelegation() == 0) {
          throw new ContractValidateException(
              "[ALLOW_CHANGE_DELEGATION] proposal must be approved "
                  + "before [ALLOW_TVM_VOTE] can be proposed");
        }
        break;
      }
      case FREE_NET_LIMIT: {
        if (!forkController.pass(ForkBlockVersionEnum.VERSION_4_3)) {
          throw new ContractValidateException("Bad chain parameter id [FREE_NET_LIMIT]");
        }
        if (value < 0 || value > 100_000L) {
          throw new ContractValidateException(
              "Bad chain parameter value, valid range is [0,100_000]");
        }
        break;
      }
      case TOTAL_NET_LIMIT: {
        if (!forkController.pass(ForkBlockVersionEnum.VERSION_4_3)) {
          throw new ContractValidateException("Bad chain parameter id [TOTAL_NET_LIMIT]");
        }
        if (value < 0 || value > 1_000_000_000_000L) {
          throw new ContractValidateException(
              "Bad chain parameter value, valid range is [0, 1_000_000_000_000L]");
        }
        break;
      }

      case ALLOW_ACCOUNT_ASSET_OPTIMIZATION: {
        if (!forkController.pass(ForkBlockVersionEnum.VERSION_4_3)) {
          throw new ContractValidateException(
              "Bad chain parameter id [ALLOW_ACCOUNT_ASSET_OPTIMIZATION]");
        }
        if (value != 1) {
          throw new ContractValidateException(
              "This value[ALLOW_ACCOUNT_ASSET_OPTIMIZATION] is only allowed to be 1");
        }
        break;
      }
      case ALLOW_TVM_LONDON: {
        if (!forkController.pass(ForkBlockVersionEnum.VERSION_4_4)) {
          throw new ContractValidateException(
              "Bad chain parameter id [ALLOW_TVM_LONDON]");
        }
        if (value != 1) {
          throw new ContractValidateException(
              "This value[ALLOW_TVM_LONDON] is only allowed to be 1");
        }
        break;
      }
      case ALLOW_TVM_COMPATIBLE_EVM: {
        if (!forkController.pass(ForkBlockVersionEnum.VERSION_4_4)) {
          throw new ContractValidateException(
              "Bad chain parameter id [ALLOW_TVM_COMPATIBLE_EVM]");
        }
        if (value != 1) {
          throw new ContractValidateException(
              "This value[ALLOW_TVM_COMPATIBLE_EVM] is only allowed to be 1");
        }
        break;
      }
      case ALLOW_HIGHER_LIMIT_FOR_MAX_CPU_TIME_OF_ONE_TX: {
        if (!forkController.pass(ForkBlockVersionEnum.VERSION_4_5)) {
          throw new ContractValidateException(
              "Bad chain parameter id [ALLOW_HIGHER_LIMIT_FOR_MAX_CPU_TIME_OF_ONE_TX]");
        }
        if (value != 1) {
          throw new ContractValidateException(
              "This value[ALLOW_HIGHER_LIMIT_FOR_MAX_CPU_TIME_OF_ONE_TX] is only allowed to be 1");
        }
        break;
      }
      case ALLOW_ASSET_OPTIMIZATION: {
        if (!forkController.pass(ForkBlockVersionEnum.VERSION_4_5)) {
          throw new ContractValidateException(
                  "Bad chain parameter id [ALLOW_ASSET_OPTIMIZATION]");
        }
        if (value != 1) {
          throw new ContractValidateException(
                  "This value[ALLOW_ASSET_OPTIMIZATION] is only allowed to be 1");
        }
        break;
      }
      case ALLOW_NEW_REWARD: {
        if (!forkController.pass(ForkBlockVersionEnum.VERSION_4_6)) {
          throw new ContractValidateException(
              "Bad chain parameter id [ALLOW_NEW_REWARD]");
        }
        if (dynamicPropertiesStore.allowNewReward()) {
          throw new ContractValidateException(
              "New reward has been valid.");
        }
        if (value != 1) {
          throw new ContractValidateException(
              "This value[ALLOW_NEW_REWARD] is only allowed to be 1");
        }
        break;
      }
      case MEMO_FEE: {
        if (!forkController.pass(ForkBlockVersionEnum.VERSION_4_6)) {
          throw new ContractValidateException(
              "Bad chain parameter id [MEMO_FEE]");
        }
        if (value < 0 || value > 1_000_000_000) {
          throw new ContractValidateException(
              "This value[MEMO_FEE] is only allowed to be in the range 0-1000_000_000");
        }
        break;
      }
      case ALLOW_DELEGATE_OPTIMIZATION: {
        if (!forkController.pass(ForkBlockVersionEnum.VERSION_4_6)) {
          throw new ContractValidateException(
              "Bad chain parameter id [ALLOW_DELEGATE_OPTIMIZATION]");
        }
        if (value != 1) {
          throw new ContractValidateException(
              "This value[ALLOW_DELEGATE_OPTIMIZATION] is only allowed to be 1");
        }
        break;
      }
      case UNFREEZE_DELAY_DAYS: {
        if (!forkController.pass(ForkBlockVersionEnum.VERSION_4_7)) {
          throw new ContractValidateException(
                  "Bad chain parameter id [UNFREEZE_DELAY_DAYS]");
        }
        if (value < 1 || value > 365) {
          throw new ContractValidateException(
                  "This value[UNFREEZE_DELAY_DAYS] is only allowed to be in the range 1-365");
        }
        break;
      }
      case ALLOW_OPTIMIZED_RETURN_VALUE_OF_CHAIN_ID: {
        if (!forkController.pass(ForkBlockVersionEnum.VERSION_4_7)) {
          throw new ContractValidateException(
              "Bad chain parameter id [ALLOW_OPTIMIZED_RETURN_VALUE_OF_CHAIN_ID]");
        }
        if (value != 1) {
          throw new ContractValidateException(
              "This value[ALLOW_OPTIMIZED_RETURN_VALUE_OF_CHAIN_ID] is only allowed to be 1");
        }
        break;
      }

      case ALLOW_DYNAMIC_ENERGY: {
        if (!forkController.pass(ForkBlockVersionEnum.VERSION_4_7)) {
          throw new ContractValidateException(
              "Bad chain parameter id [ALLOW_DYNAMIC_ENERGY]");
        }
        if (value < 0 || value > 1) {
          throw new ContractValidateException(
              "This value[ALLOW_DYNAMIC_ENERGY] is only allowed to be in the range 0-1"
          );
        }
        if (value == 1 && dynamicPropertiesStore.getChangeDelegation() == 0) {
          throw new ContractValidateException(
              "[ALLOW_CHANGE_DELEGATION] proposal must be approved "
                  + "before [ALLOW_DYNAMIC_ENERGY] can be opened");
        }
        break;
      }
      case DYNAMIC_ENERGY_THRESHOLD: {
        if (!forkController.pass(ForkBlockVersionEnum.VERSION_4_7)) {
          throw new ContractValidateException(
              "Bad chain parameter id [DYNAMIC_ENERGY_THRESHOLD]");
        }

        if (value < 0 || value > LONG_VALUE) {
          throw new ContractValidateException(LONG_VALUE_ERROR);
        }
        break;
      }
      case DYNAMIC_ENERGY_INCREASE_FACTOR: {
        if (!forkController.pass(ForkBlockVersionEnum.VERSION_4_7)) {
          throw new ContractValidateException(
              "Bad chain parameter id [DYNAMIC_ENERGY_INCREASE_FACTOR]");
        }

        if (value < 0 || value > DYNAMIC_ENERGY_INCREASE_FACTOR_RANGE) {
          throw new ContractValidateException(
              "This value[DYNAMIC_ENERGY_INCREASE_FACTOR] "
                  + "is only allowed to be in the range 0-"
                  + DYNAMIC_ENERGY_INCREASE_FACTOR_RANGE
          );
        }
        break;
      }
      case DYNAMIC_ENERGY_MAX_FACTOR: {
        if (!forkController.pass(ForkBlockVersionEnum.VERSION_4_7)) {
          throw new ContractValidateException(
              "Bad chain parameter id [DYNAMIC_ENERGY_MAX_FACTOR]");
        }

        if (value < 0 || value > DYNAMIC_ENERGY_MAX_FACTOR_RANGE) {
          throw new ContractValidateException(
              "This value[DYNAMIC_ENERGY_MAX_FACTOR] "
                  + "is only allowed to be in the range 0-"
                  + DYNAMIC_ENERGY_MAX_FACTOR_RANGE
          );
        }
        break;
      }
      case ALLOW_TVM_SHANGHAI: {
        if (!forkController.pass(ForkBlockVersionEnum.VERSION_4_7_2)) {
          throw new ContractValidateException(
              "Bad chain parameter id [ALLOW_TVM_SHANGHAI]");
        }
        if (value != 1) {
          throw new ContractValidateException(
              "This value[ALLOW_TVM_SHANGHAI] is only allowed to be 1");
        }
        break;
      }
      case ALLOW_CANCEL_ALL_UNFREEZE_V2: {
        if (!forkController.pass(ForkBlockVersionEnum.VERSION_4_7_2)) {
          throw new ContractValidateException(
              "Bad chain parameter id [ALLOW_CANCEL_ALL_UNFREEZE_V2]");
        }
        if (value != 1) {
          throw new ContractValidateException(
              "This value[ALLOW_CANCEL_ALL_UNFREEZE_V2] is only allowed to be 1");
        }
        if (dynamicPropertiesStore.getUnfreezeDelayDays() == 0) {
          throw new ContractValidateException(
              "[UNFREEZE_DELAY_DAYS] proposal must be approved "
                  + "before [ALLOW_CANCEL_ALL_UNFREEZE_V2] can be proposed");
        }
        break;
      }
      case MAX_DELEGATE_LOCK_PERIOD: {
        if (!forkController.pass(ForkBlockVersionEnum.VERSION_4_7_2)) {
          throw new ContractValidateException(
              "Bad chain parameter id [MAX_DELEGATE_LOCK_PERIOD]");
        }
        long maxDelegateLockPeriod = dynamicPropertiesStore.getMaxDelegateLockPeriod();
        if (value <= maxDelegateLockPeriod || value > ONE_YEAR_BLOCK_NUMBERS) {
          throw new ContractValidateException(
              "This value[MAX_DELEGATE_LOCK_PERIOD] is only allowed to be greater than "
                  + maxDelegateLockPeriod + " and less than or equal to " + ONE_YEAR_BLOCK_NUMBERS
                      + " !");
        }
        if (dynamicPropertiesStore.getUnfreezeDelayDays() == 0) {
          throw new ContractValidateException(
              "[UNFREEZE_DELAY_DAYS] proposal must be approved "
                  + "before [MAX_DELEGATE_LOCK_PERIOD] can be proposed");
        }
        break;
      }
      case ALLOW_OLD_REWARD_OPT: {
        if (!forkController.pass(ForkBlockVersionEnum.VERSION_4_7_4)) {
          throw new ContractValidateException(
              "Bad chain parameter id [ALLOW_OLD_REWARD_OPT]");
        }
        if (dynamicPropertiesStore.allowOldRewardOpt()) {
          throw new ContractValidateException(
              "[ALLOW_OLD_REWARD_OPT] has been valid, no need to propose again");
        }
        if (value != 1) {
          throw new ContractValidateException(
              "This value[ALLOW_OLD_REWARD_OPT] is only allowed to be 1");
        }
        if (!dynamicPropertiesStore.useNewRewardAlgorithm()) {
          throw new ContractValidateException(
              "[ALLOW_NEW_REWARD] or [ALLOW_TVM_VOTE] proposal must be approved "
                  + "before [ALLOW_OLD_REWARD_OPT] can be proposed");
        }
        break;
      }
      case ALLOW_ENERGY_ADJUSTMENT: {
        if (!forkController.pass(ForkBlockVersionEnum.VERSION_4_7_5)) {
          throw new ContractValidateException(
                  "Bad chain parameter id [ALLOW_ENERGY_ADJUSTMENT]");
        }
        if (dynamicPropertiesStore.getAllowEnergyAdjustment() == 1) {
          throw new ContractValidateException(
              "[ALLOW_ENERGY_ADJUSTMENT] has been valid, no need to propose again");
        }
        if (value != 1) {
          throw new ContractValidateException(
                  "This value[ALLOW_ENERGY_ADJUSTMENT] is only allowed to be 1");
        }
        break;
      }
      case MAX_CREATE_ACCOUNT_TX_SIZE: {
        if (!forkController.pass(ForkBlockVersionEnum.VERSION_4_7_5)) {
          throw new ContractValidateException(
              "Bad chain parameter id [MAX_CREATE_ACCOUNT_TX_SIZE]");
        }
        if (value < CREATE_ACCOUNT_TRANSACTION_MIN_BYTE_SIZE
            || value > CREATE_ACCOUNT_TRANSACTION_MAX_BYTE_SIZE) {
          throw new ContractValidateException(
              "This value[MAX_CREATE_ACCOUNT_TX_SIZE] is only allowed to be greater than or equal "
                  + "to " + CREATE_ACCOUNT_TRANSACTION_MIN_BYTE_SIZE + " and less than or equal to "
                  + CREATE_ACCOUNT_TRANSACTION_MAX_BYTE_SIZE + "!");
        }
        break;
      }
      case ALLOW_STRICT_MATH: {
        if (!forkController.pass(ForkBlockVersionEnum.VERSION_4_7_7)) {
          throw new ContractValidateException(
              "Bad chain parameter id [ALLOW_STRICT_MATH]");
        }
        if (dynamicPropertiesStore.allowStrictMath()) {
          throw new ContractValidateException(
              "[ALLOW_STRICT_MATH] has been valid, no need to propose again");
        }
        if (value != 1) {
          throw new ContractValidateException(
              "This value[ALLOW_STRICT_MATH] is only allowed to be 1");
        }
        break;
      }
      case CONSENSUS_LOGIC_OPTIMIZATION: {
        if (!forkController.pass(ForkBlockVersionEnum.VERSION_4_8_0)) {
          throw new ContractValidateException(
              "Bad chain parameter id [CONSENSUS_LOGIC_OPTIMIZATION]");
        }
        if (dynamicPropertiesStore.getConsensusLogicOptimization() == 1) {
          throw new ContractValidateException(
              "[CONSENSUS_LOGIC_OPTIMIZATION] has been valid, no need to propose again");
        }
        if (value != 1) {
          throw new ContractValidateException(
              "This value[CONSENSUS_LOGIC_OPTIMIZATION] is only allowed to be 1");
        }
        break;
      }
      default:
        break;
    }
  }

  public enum ProposalType {         // current value, value range
    MAINTENANCE_TIME_INTERVAL(0), // 6 Hours, [3 * 27, 24 * 3600] s
    ACCOUNT_UPGRADE_COST(1), // 9999 TRX, [0, 100000000000] TRX
    CREATE_ACCOUNT_FEE(2), // 0.1 TRX, [0, 100000000000] TRX
    TRANSACTION_FEE(3), // 10 Sun/Byte, [0, 100000000000] TRX
    ASSET_ISSUE_FEE(4), // 1024 TRX, [0, 100000000000] TRX
    WITNESS_PAY_PER_BLOCK(5), // 16 TRX, [0, 100000000000] TRX
    WITNESS_STANDBY_ALLOWANCE(6), // 115200 TRX, [0, 100000000000] TRX
    CREATE_NEW_ACCOUNT_FEE_IN_SYSTEM_CONTRACT(7), // 0 TRX, [0, 100000000000] TRX
    CREATE_NEW_ACCOUNT_BANDWIDTH_RATE(8), // 1 Bandwith/Byte, [0, 100000000000000000] Bandwith/Byte
    ALLOW_CREATION_OF_CONTRACTS(9), // 1, {0, 1}
    REMOVE_THE_POWER_OF_THE_GR(10),  // 1, {0, 1}
    ENERGY_FEE(11), // 10 Sun, [0, 100000000000] TRX
    EXCHANGE_CREATE_FEE(12), // 1024 TRX, [0, 100000000000] TRX
    MAX_CPU_TIME_OF_ONE_TX(13), // 50 ms, [0, 1000] ms
    ALLOW_UPDATE_ACCOUNT_NAME(14), // 0, {0, 1}
    ALLOW_SAME_TOKEN_NAME(15), // 1, {0, 1}
    ALLOW_DELEGATE_RESOURCE(16), // 1, {0, 1}
    TOTAL_ENERGY_LIMIT(17), // 50,000,000,000, [0, 100000000000000000]
    ALLOW_TVM_TRANSFER_TRC10(18), // 1, {0, 1}
    TOTAL_CURRENT_ENERGY_LIMIT(19), // 50,000,000,000, [0, 100000000000000000]
    ALLOW_MULTI_SIGN(20), // 1, {0, 1}
    ALLOW_ADAPTIVE_ENERGY(21), // 1, {0, 1}
    UPDATE_ACCOUNT_PERMISSION_FEE(22), // 100 TRX, [0, 100000] TRX
    MULTI_SIGN_FEE(23), // 1 TRX, [0, 100000] TRX
    ALLOW_PROTO_FILTER_NUM(24), // 0, {0, 1}
    ALLOW_ACCOUNT_STATE_ROOT(25), // 1, {0, 1}
    ALLOW_TVM_CONSTANTINOPLE(26), // 1, {0, 1}
    // ALLOW_SHIELDED_TRANSACTION(27), // 0, {0, 1}
    // SHIELDED_TRANSACTION_FEE(28), // 10 TRX, [0, 10000] TRX
    ADAPTIVE_RESOURCE_LIMIT_MULTIPLIER(29), // 1000, [1, 10000]
    ALLOW_CHANGE_DELEGATION(30), // 1, {0, 1}
    WITNESS_127_PAY_PER_BLOCK(31), // 160 TRX, [0, 100000000000] TRX
    ALLOW_TVM_SOLIDITY_059(32), // 1, {0, 1}
    ADAPTIVE_RESOURCE_LIMIT_TARGET_RATIO(33), // 10, [1, 1000]
    // SHIELDED_TRANSACTION_CREATE_ACCOUNT_FEE(34), // 1 TRX, [0, 10000] TRX
    FORBID_TRANSFER_TO_CONTRACT(35), // 1, {0, 1}
    ALLOW_SHIELDED_TRC20_TRANSACTION(39), // 1, 39
    ALLOW_PBFT(40),// 1,40
    ALLOW_TVM_ISTANBUL(41),//1, {0,1}
    // ALLOW_TVM_ASSET_ISSUE(42), // 0, 1
    // ALLOW_TVM_STAKE(43), // 0, 1
    ALLOW_MARKET_TRANSACTION(44), // {0, 1}
    MARKET_SELL_FEE(45), // 0 [0,10_000_000_000]
    MARKET_CANCEL_FEE(46), // 0 [0,10_000_000_000]
    MAX_FEE_LIMIT(47), // [0, 100_000_000_000] TRX
    ALLOW_TRANSACTION_FEE_POOL(48), // 0, 1
    ALLOW_BLACKHOLE_OPTIMIZATION(49),// 0,1
    ALLOW_NEW_RESOURCE_MODEL(51),// 0,1
    ALLOW_TVM_FREEZE(52), // 0, 1
    ALLOW_ACCOUNT_ASSET_OPTIMIZATION(53), // 1
    // ALLOW_NEW_REWARD_ALGORITHM(58), // 0, 1
    ALLOW_TVM_VOTE(59), // 0, 1
    ALLOW_TVM_COMPATIBLE_EVM(60), // 0, 1
    FREE_NET_LIMIT(61), // 5000, [0, 100_000]
    TOTAL_NET_LIMIT(62), // 43_200_000_000L, [0, 1000_000_000_000L]
    ALLOW_TVM_LONDON(63), // 0, 1
    ALLOW_HIGHER_LIMIT_FOR_MAX_CPU_TIME_OF_ONE_TX(65), // 0, 1
    ALLOW_ASSET_OPTIMIZATION(66), // 0, 1
    ALLOW_NEW_REWARD(67), // 0, 1
    MEMO_FEE(68), // 0, [0, 1000_000_000]
    ALLOW_DELEGATE_OPTIMIZATION(69),
    UNFREEZE_DELAY_DAYS(70), // 0, [1, 365]
    ALLOW_OPTIMIZED_RETURN_VALUE_OF_CHAIN_ID(71), // 0, 1
    ALLOW_DYNAMIC_ENERGY(72), // 0, 1
    DYNAMIC_ENERGY_THRESHOLD(73), // 0, [0, LONG]
    DYNAMIC_ENERGY_INCREASE_FACTOR(74), // 0, [0, 10_000]
    DYNAMIC_ENERGY_MAX_FACTOR(75), // 0, [0, 100_000]
    ALLOW_TVM_SHANGHAI(76), // 0, 1
    ALLOW_CANCEL_ALL_UNFREEZE_V2(77), // 0, 1
    MAX_DELEGATE_LOCK_PERIOD(78), // (86400, 10512000]
    ALLOW_OLD_REWARD_OPT(79), // 0, 1
    ALLOW_ENERGY_ADJUSTMENT(81), // 0, 1
    MAX_CREATE_ACCOUNT_TX_SIZE(82), // [500, 10000]
    ALLOW_STRICT_MATH(87), // 0, 1
    CONSENSUS_LOGIC_OPTIMIZATION(88); // 0, 1

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
