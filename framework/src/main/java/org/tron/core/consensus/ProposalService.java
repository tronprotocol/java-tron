package org.tron.core.consensus;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.capsule.ProposalCapsule;
import org.tron.core.config.Parameter.ForkBlockVersionEnum;
import org.tron.core.db.Manager;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.utils.ProposalUtil;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;

/**
 * Notice:
 * <p>
 * if you want to add a proposal,you just should add a enum ProposalType and add the valid in the
 * validator method, add the process in the process method
 */
@Slf4j
public class ProposalService extends ProposalUtil {

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
          // update bandwidth price history
          manager.getDynamicPropertiesStore().saveBandwidthPriceHistory(
              manager.getDynamicPropertiesStore().getBandwidthPriceHistory()
                  + "," + proposalCapsule.getExpirationTime() + ":" + entry.getValue());
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
          // update energy price history
          manager.getDynamicPropertiesStore().saveEnergyPriceHistory(
              manager.getDynamicPropertiesStore().getEnergyPriceHistory()
                  + "," + proposalCapsule.getExpirationTime() + ":" + entry.getValue());
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
            if (manager.getChainBaseManager()
                .getForkController().pass(ForkBlockVersionEnum.VERSION_3_6_5)) {
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
        case WITNESS_127_PAY_PER_BLOCK: {
          manager.getDynamicPropertiesStore().saveWitness127PayPerBlock(entry.getValue());
          break;
        }
        //case ALLOW_SHIELDED_TRANSACTION: {
        //  if (manager.getDynamicPropertiesStore().getAllowShieldedTransaction() == 0) {
        //    manager.getDynamicPropertiesStore().saveAllowShieldedTransaction(entry.getValue());
        //    manager.getDynamicPropertiesStore().addSystemContractAndSetPermission(51);
        //  }
        //  break;
        //}
        //case SHIELDED_TRANSACTION_FEE: {
        //  manager.getDynamicPropertiesStore().saveShieldedTransactionFee(entry.getValue());
        //  break;
        //}
        //        case SHIELDED_TRANSACTION_CREATE_ACCOUNT_FEE: {
        //          manager.getDynamicPropertiesStore()
        //              .saveShieldedTransactionCreateAccountFee(entry.getValue());
        //          break;
        //        }
        case FORBID_TRANSFER_TO_CONTRACT: {
          manager.getDynamicPropertiesStore().saveForbidTransferToContract(entry.getValue());
          break;
        }
        case ALLOW_PBFT: {
          manager.getDynamicPropertiesStore().saveAllowPBFT(entry.getValue());
          break;
        }
        case ALLOW_TVM_ISTANBUL: {
          manager.getDynamicPropertiesStore().saveAllowTvmIstanbul(entry.getValue());
          break;
        }
        case ALLOW_SHIELDED_TRC20_TRANSACTION: {
          manager.getDynamicPropertiesStore().saveAllowShieldedTRC20Transaction(entry.getValue());
          break;
        }
        case ALLOW_MARKET_TRANSACTION: {
          if (manager.getDynamicPropertiesStore().getAllowMarketTransaction() == 0) {
            manager.getDynamicPropertiesStore().saveAllowMarketTransaction(entry.getValue());
            manager.getDynamicPropertiesStore().addSystemContractAndSetPermission(52);
            manager.getDynamicPropertiesStore().addSystemContractAndSetPermission(53);
          }
          break;
        }
        case MARKET_SELL_FEE: {
          manager.getDynamicPropertiesStore().saveMarketSellFee(entry.getValue());
          break;
        }
        case MARKET_CANCEL_FEE: {
          manager.getDynamicPropertiesStore().saveMarketCancelFee(entry.getValue());
          break;
        }
        case MAX_FEE_LIMIT: {
          manager.getDynamicPropertiesStore().saveMaxFeeLimit(entry.getValue());
          break;
        }
        case ALLOW_TRANSACTION_FEE_POOL: {
          manager.getDynamicPropertiesStore().saveAllowTransactionFeePool(entry.getValue());
          break;
        }
        case ALLOW_BLACKHOLE_OPTIMIZATION: {
          manager.getDynamicPropertiesStore().saveAllowBlackHoleOptimization(entry.getValue());
          break;
        }
        case ALLOW_NEW_RESOURCE_MODEL: {
          manager.getDynamicPropertiesStore().saveAllowNewResourceModel(entry.getValue());
          break;
        }
        case ALLOW_TVM_FREEZE: {
          manager.getDynamicPropertiesStore().saveAllowTvmFreeze(entry.getValue());
          break;
        }
        case ALLOW_TVM_VOTE: {
          manager.getDynamicPropertiesStore().saveNewRewardAlgorithmEffectiveCycle();
          manager.getDynamicPropertiesStore().saveAllowTvmVote(entry.getValue());
          break;
        }
        case ALLOW_TVM_LONDON: {
          manager.getDynamicPropertiesStore().saveAllowTvmLondon(entry.getValue());
          break;
        }
        case ALLOW_TVM_COMPATIBLE_EVM: {
          manager.getDynamicPropertiesStore().saveAllowTvmCompatibleEvm(entry.getValue());
          break;
        }
        case FREE_NET_LIMIT: {
          manager.getDynamicPropertiesStore().saveFreeNetLimit(entry.getValue());
          break;
        }
        case TOTAL_NET_LIMIT: {
          manager.getDynamicPropertiesStore().saveTotalNetLimit(entry.getValue());
          break;
        }
        case ALLOW_ACCOUNT_ASSET_OPTIMIZATION: {
          manager.getDynamicPropertiesStore().setAllowAccountAssetOptimization(entry.getValue());
          break;
        }
        case ALLOW_HIGHER_LIMIT_FOR_MAX_CPU_TIME_OF_ONE_TX: {
          manager.getDynamicPropertiesStore().saveAllowHigherLimitForMaxCpuTimeOfOneTx(
              entry.getValue());
          break;
        }
        case ALLOW_ASSET_OPTIMIZATION: {
          manager.getDynamicPropertiesStore().setAllowAssetOptimization(entry.getValue());
          break;
        }
        case ALLOW_NEW_REWARD: {
          manager.getDynamicPropertiesStore().saveNewRewardAlgorithmEffectiveCycle();
          manager.getDynamicPropertiesStore().saveAllowNewReward(entry.getValue());
          break;
        }
        case MEMO_FEE: {
          manager.getDynamicPropertiesStore().saveMemoFee(entry.getValue());
          // update memo fee history
          manager.getDynamicPropertiesStore().saveMemoFeeHistory(
              manager.getDynamicPropertiesStore().getMemoFeeHistory()
                  + "," + proposalCapsule.getExpirationTime() + ":" + entry.getValue());
          break;
        }
        case UNFREEZE_DELAY_DAYS: {
          DynamicPropertiesStore dynamicStore = manager.getDynamicPropertiesStore();
          dynamicStore.saveUnfreezeDelayDays(entry.getValue());
          dynamicStore.addSystemContractAndSetPermission(
              ContractType.FreezeBalanceV2Contract_VALUE);
          dynamicStore.addSystemContractAndSetPermission(
              ContractType.UnfreezeBalanceV2Contract_VALUE);
          dynamicStore.addSystemContractAndSetPermission(
              ContractType.WithdrawExpireUnfreezeContract_VALUE);
          dynamicStore.addSystemContractAndSetPermission(
              ContractType.DelegateResourceContract_VALUE);
          dynamicStore.addSystemContractAndSetPermission(
              ContractType.UnDelegateResourceContract_VALUE);
          break;
        }
        case ALLOW_DELEGATE_OPTIMIZATION: {
          manager.getDynamicPropertiesStore().saveAllowDelegateOptimization(entry.getValue());
          break;
        }
        case ALLOW_OPTIMIZED_RETURN_VALUE_OF_CHAIN_ID: {
          manager.getDynamicPropertiesStore()
              .saveAllowOptimizedReturnValueOfChainId(entry.getValue());
          break;
        }
        case ALLOW_DYNAMIC_ENERGY: {
          manager.getDynamicPropertiesStore().saveAllowDynamicEnergy(entry.getValue());
          break;
        }
        case DYNAMIC_ENERGY_THRESHOLD: {
          manager.getDynamicPropertiesStore().saveDynamicEnergyThreshold(entry.getValue());
          break;
        }
        case DYNAMIC_ENERGY_INCREASE_FACTOR: {
          manager.getDynamicPropertiesStore().saveDynamicEnergyIncreaseFactor(entry.getValue());
          break;
        }
        case DYNAMIC_ENERGY_MAX_FACTOR: {
          manager.getDynamicPropertiesStore().saveDynamicEnergyMaxFactor(entry.getValue());
          break;
        }
        case ALLOW_TVM_SHANGHAI: {
          manager.getDynamicPropertiesStore().saveAllowTvmShangHai(entry.getValue());
          break;
        }
        case ALLOW_CANCEL_ALL_UNFREEZE_V2: {
          if (manager.getDynamicPropertiesStore().getAllowCancelAllUnfreezeV2() == 0) {
            manager.getDynamicPropertiesStore().saveAllowCancelAllUnfreezeV2(entry.getValue());
            manager.getDynamicPropertiesStore().addSystemContractAndSetPermission(
                ContractType.CancelAllUnfreezeV2Contract_VALUE);
          }
          break;
        }
        case MAX_DELEGATE_LOCK_PERIOD: {
          manager.getDynamicPropertiesStore().saveMaxDelegateLockPeriod(entry.getValue());
          break;
        }
        case ALLOW_OLD_REWARD_OPT: {
          manager.getDynamicPropertiesStore().saveAllowOldRewardOpt(entry.getValue());
          break;
        }
        case ALLOW_ENERGY_ADJUSTMENT: {
          manager.getDynamicPropertiesStore().saveAllowEnergyAdjustment(entry.getValue());
          break;
        }
        case MAX_CREATE_ACCOUNT_TX_SIZE: {
          manager.getDynamicPropertiesStore().saveMaxCreateAccountTxSize(entry.getValue());
          break;
        }
        case ALLOW_STRICT_MATH: {
          manager.getDynamicPropertiesStore().saveAllowStrictMath(entry.getValue());
          break;
        }
        case CONSENSUS_LOGIC_OPTIMIZATION: {
          manager.getDynamicPropertiesStore()
            .saveConsensusLogicOptimization(entry.getValue());
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
