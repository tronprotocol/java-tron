package org.tron.core.config.args;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import com.typesafe.config.ConfigFactory;
import org.tron.core.config.BeanDefaults;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Committee (governance) configuration bean.
 * Field names match config.conf keys under the "committee" section.
 * All fields are governance proposal toggles, default 0 (disabled).
 */
@Slf4j
@Getter
@Setter
@SuppressWarnings("unused") // setters used by ConfigBeanFactory via reflection
public class CommitteeConfig {

  private long allowCreationOfContracts = 0;
  private long allowMultiSign = 0;
  private long allowAdaptiveEnergy = 0;
  private long allowDelegateResource = 0;
  private long allowSameTokenName = 0;
  private long allowTvmTransferTrc10 = 0;
  private long allowTvmConstantinople = 0;
  private long allowTvmSolidity059 = 0;
  private long forbidTransferToContract = 0;
  private long allowShieldedTRC20Transaction = 0;
  private long allowMarketTransaction = 0;
  private long allowTransactionFeePool = 0;
  private long allowBlackHoleOptimization = 0;
  private long allowNewResourceModel = 0;
  private long allowTvmIstanbul = 0;
  private long allowProtoFilterNum = 0;
  private long allowAccountStateRoot = 0;
  private long changedDelegation = 0;
  private long allowPBFT = 0;
  private long pBFTExpireNum = 20;
  private long allowTvmFreeze = 0;
  private long allowTvmVote = 0;
  private long allowTvmLondon = 0;
  private long allowTvmCompatibleEvm = 0;
  private long allowHigherLimitForMaxCpuTimeOfOneTx = 0;
  private long allowNewRewardAlgorithm = 0;
  private long allowOptimizedReturnValueOfChainId = 0;
  private long allowTvmShangHai = 0;
  private long allowOldRewardOpt = 0;
  private long allowEnergyAdjustment = 0;
  private long allowStrictMath = 0;
  private long consensusLogicOptimization = 0;
  private long allowTvmCancun = 0;
  private long allowTvmBlob = 0;
  private long allowTvmOsaka = 0;
  private long unfreezeDelayDays = 0;
  private long allowReceiptsMerkleRoot = 0;
  private long allowAccountAssetOptimization = 0;
  private long allowAssetOptimization = 0;
  private long allowNewReward = 0;
  private long memoFee = 0;
  private long allowDelegateOptimization = 0;
  private long allowDynamicEnergy = 0;
  private long dynamicEnergyThreshold = 0;
  private long dynamicEnergyIncreaseFactor = 0;
  private long dynamicEnergyMaxFactor = 0;

  // proposalExpireTime is NOT a committee field — it's in block.* and handled by BlockConfig

  public static CommitteeConfig fromConfig(Config config) {
    Config defaults = BeanDefaults.toConfig(new CommitteeConfig());
    Config userSection = config.hasPath("committee")
        ? BeanDefaults.stripNullLeaves(config.getConfig("committee"))
        : ConfigFactory.empty();
    // pBFTExpireNum: config key uses lowercase-p prefix, but setPBFTExpireNum causes
    // Introspector to derive "PBFTExpireNum" (consecutive uppercase prevents decapitalization).
    // Remap so ConfigBeanFactory finds it under the expected key.
    userSection = BeanDefaults.remapKey(userSection, "pBFTExpireNum", "PBFTExpireNum");
    Config section = userSection.withFallback(defaults);
    CommitteeConfig cc = ConfigBeanFactory.create(section, CommitteeConfig.class);
    cc.postProcess();
    return cc;
  }

  private void postProcess() {
    // clamp unfreezeDelayDays to 0-365
    if (unfreezeDelayDays < 0) {
      unfreezeDelayDays = 0;
    }
    if (unfreezeDelayDays > 365) {
      unfreezeDelayDays = 365;
    }

    // clamp allowDelegateOptimization to 0-1
    if (allowDelegateOptimization < 0) { allowDelegateOptimization = 0; }
    if (allowDelegateOptimization > 1) { allowDelegateOptimization = 1; }

    // clamp allowDynamicEnergy to 0-1
    if (allowDynamicEnergy < 0) { allowDynamicEnergy = 0; }
    if (allowDynamicEnergy > 1) { allowDynamicEnergy = 1; }

    // clamp dynamicEnergyThreshold to 0-100_000_000_000_000_000
    if (dynamicEnergyThreshold < 0) { dynamicEnergyThreshold = 0; }
    if (dynamicEnergyThreshold > 100_000_000_000_000_000L) {
      dynamicEnergyThreshold = 100_000_000_000_000_000L;
    }

    // clamp dynamicEnergyIncreaseFactor to 0-10_000
    if (dynamicEnergyIncreaseFactor < 0) { dynamicEnergyIncreaseFactor = 0; }
    if (dynamicEnergyIncreaseFactor > 10_000L) { dynamicEnergyIncreaseFactor = 10_000L; }

    // clamp dynamicEnergyMaxFactor to 0-100_000
    if (dynamicEnergyMaxFactor < 0) { dynamicEnergyMaxFactor = 0; }
    if (dynamicEnergyMaxFactor > 100_000L) { dynamicEnergyMaxFactor = 100_000L; }

    // clamp allowNewReward to 0-1 (must run BEFORE the cross-field check below,
    // which depends on allowNewReward != 1)
    if (allowNewReward < 0) { allowNewReward = 0; }
    if (allowNewReward > 1) { allowNewReward = 1; }

    // clamp memoFee to 0-1_000_000_000
    if (memoFee < 0) { memoFee = 0; }
    if (memoFee > 1_000_000_000L) { memoFee = 1_000_000_000L; }

    // cross-field: allowOldRewardOpt requires at least one reward/vote flag
    if (allowOldRewardOpt == 1 && allowNewRewardAlgorithm != 1
        && allowNewReward != 1 && allowTvmVote != 1) {
      throw new IllegalArgumentException(
          "At least one of the following proposals is required to be opened first: "
              + "committee.allowNewRewardAlgorithm = 1"
              + " or committee.allowNewReward = 1"
              + " or committee.allowTvmVote = 1.");
    }
  }
}
