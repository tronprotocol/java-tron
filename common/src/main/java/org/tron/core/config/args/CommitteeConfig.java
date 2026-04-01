package org.tron.core.config.args;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import com.typesafe.config.ConfigFactory;
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
  // NON-STANDARD NAMING: "allowPBFT" and "pBFTExpireNum" in config.conf contain
  // consecutive uppercase letters ("PBFT"), which violates JavaBean naming convention.
  // ConfigBeanFactory derives config keys from setter names using JavaBean rules:
  //   setPBFTExpireNum -> property "PBFTExpireNum" (capital P, per JavaBean spec)
  //   but config.conf uses "pBFTExpireNum" (lowercase p) -> mismatch -> binding fails.
  //
  // These two fields are excluded from auto-binding and handled manually in fromConfig().
  // TODO: Rename config keys to standard camelCase (allowPbft, pbftExpireNum) when
  //       PBFT feature is enabled and a breaking config change is acceptable.
  @Getter(lombok.AccessLevel.NONE)
  @Setter(lombok.AccessLevel.NONE)
  private long allowPBFT = 0;
  @Getter(lombok.AccessLevel.NONE)
  @Setter(lombok.AccessLevel.NONE)
  private long pBFTExpireNum = 20;

  public long getAllowPBFT() { return allowPBFT; }
  public void setAllowPBFT(long v) { this.allowPBFT = v; }
  public long getPBFTExpireNum() { return pBFTExpireNum; }
  public void setPBFTExpireNum(long v) { this.pBFTExpireNum = v; }
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

  private static final Config DEFAULTS;

  static {
    StringBuilder sb = new StringBuilder();
    sb.append("allowCreationOfContracts = 0\n");
    sb.append("allowMultiSign = 0\n");
    sb.append("allowAdaptiveEnergy = 0\n");
    sb.append("allowDelegateResource = 0\n");
    sb.append("allowSameTokenName = 0\n");
    sb.append("allowTvmTransferTrc10 = 0\n");
    sb.append("allowTvmConstantinople = 0\n");
    sb.append("allowTvmSolidity059 = 0\n");
    sb.append("forbidTransferToContract = 0\n");
    sb.append("allowShieldedTRC20Transaction = 0\n");
    sb.append("allowMarketTransaction = 0\n");
    sb.append("allowTransactionFeePool = 0\n");
    sb.append("allowBlackHoleOptimization = 0\n");
    sb.append("allowNewResourceModel = 0\n");
    sb.append("allowTvmIstanbul = 0\n");
    sb.append("allowProtoFilterNum = 0\n");
    sb.append("allowAccountStateRoot = 0\n");
    sb.append("changedDelegation = 0\n");
    sb.append("allowPBFT = 0\n");
    sb.append("pBFTExpireNum = 20\n");
    sb.append("allowTvmFreeze = 0\n");
    sb.append("allowTvmVote = 0\n");
    sb.append("allowTvmLondon = 0\n");
    sb.append("allowTvmCompatibleEvm = 0\n");
    sb.append("allowHigherLimitForMaxCpuTimeOfOneTx = 0\n");
    sb.append("allowNewRewardAlgorithm = 0\n");
    sb.append("allowOptimizedReturnValueOfChainId = 0\n");
    sb.append("allowTvmShangHai = 0\n");
    sb.append("allowOldRewardOpt = 0\n");
    sb.append("allowEnergyAdjustment = 0\n");
    sb.append("allowStrictMath = 0\n");
    sb.append("consensusLogicOptimization = 0\n");
    sb.append("allowTvmCancun = 0\n");
    sb.append("allowTvmBlob = 0\n");
    sb.append("unfreezeDelayDays = 0\n");
    sb.append("allowReceiptsMerkleRoot = 0\n");
    sb.append("allowAccountAssetOptimization = 0\n");
    sb.append("allowAssetOptimization = 0\n");
    sb.append("allowNewReward = 0\n");
    sb.append("memoFee = 0\n");
    sb.append("allowDelegateOptimization = 0\n");
    sb.append("allowDynamicEnergy = 0\n");
    sb.append("dynamicEnergyThreshold = 0\n");
    sb.append("dynamicEnergyIncreaseFactor = 0\n");
    sb.append("dynamicEnergyMaxFactor = 0\n");
    DEFAULTS = ConfigFactory.parseString(sb.toString());
  }

  /**
   * Create CommitteeConfig from the "committee" section of the application config.
   *
   * Note: allowPBFT and pBFTExpireNum have non-standard JavaBean naming (consecutive
   * uppercase letters) which causes ConfigBeanFactory key mismatch. These two fields
   * are excluded from automatic binding and handled manually after.
   */
  public static CommitteeConfig fromConfig(Config config) {
    Config section = config.hasPath("committee")
        ? config.getConfig("committee").withFallback(DEFAULTS)
        : DEFAULTS;

    // ConfigBeanFactory derives key names from setter methods. For setPBFTExpireNum()
    // it expects "PBFTExpireNum" (capital P), but config.conf uses "pBFTExpireNum".
    // Similarly, getAllowPBFT() maps to "allowPBFT" which may be missing in test configs.
    // Add uppercase aliases so ConfigBeanFactory can find them.
    Config aliased = section;
    if (section.hasPath("pBFTExpireNum") && !section.hasPath("PBFTExpireNum")) {
      aliased = aliased.withValue("PBFTExpireNum", section.getValue("pBFTExpireNum"));
    }

    CommitteeConfig cc = ConfigBeanFactory.create(aliased, CommitteeConfig.class);
    // Ensure the manually-named fields get the right values from the original keys
    cc.allowPBFT = section.hasPath("allowPBFT") ? section.getLong("allowPBFT") : 0;
    cc.pBFTExpireNum = section.hasPath("pBFTExpireNum") ? section.getLong("pBFTExpireNum") : 20;

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
