package org.tron.core.actuator.utils;

import static org.tron.core.Constant.DYNAMIC_ENERGY_INCREASE_FACTOR_RANGE;
import static org.tron.core.config.Parameter.ChainConstant.ONE_YEAR_BLOCK_NUMBERS;

import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.ForkController;
import org.tron.core.Constant;
import org.tron.core.config.Parameter;
import org.tron.core.config.Parameter.ForkBlockVersionEnum;
import org.tron.core.config.args.Args;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.utils.ProposalUtil;
import org.tron.core.utils.ProposalUtil.ProposalType;

@Slf4j(topic = "actuator")
public class ProposalUtilTest extends BaseTest {

  private static final long LONG_VALUE = 100_000_000_000_000_000L;
  private static final String LONG_VALUE_ERROR =
      "Bad chain parameter value, valid range is [0," + LONG_VALUE + "]";
  private static final String BAD_PARAM_ID = "Bad chain parameter id";

  /**
   * Init .
   */
  @BeforeClass
  public static void init() {
    dbPath = "output_ProposalUtil_test";
    Args.setParam(new String[]{"--output-directory", dbPath}, Constant.TEST_CONF);
  }
  
  @Test
  public void validProposalTypeCheck() throws ContractValidateException {

    Assert.assertFalse(ProposalType.contain(4000));
    Assert.assertFalse(ProposalType.contain(-1));
    Assert.assertTrue(ProposalType.contain(2));

    Assert.assertNull(ProposalType.getEnumOrNull(-2));
    Assert.assertEquals(ProposalType.ALLOW_TVM_SOLIDITY_059, ProposalType.getEnumOrNull(32));

    long code = -1;
    try {
      ProposalType.getEnum(code);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals("Does not support code : " + code, e.getMessage());
    }

    code = 32;
    Assert.assertEquals(ProposalType.ALLOW_TVM_SOLIDITY_059, ProposalType.getEnum(code));

  }

  @Test
  public void validateCheck() {
    DynamicPropertiesStore dynamicPropertiesStore = null;
    ForkController forkUtils = ForkController.instance();

    long invalidValue = -1;

    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.ACCOUNT_UPGRADE_COST.getCode(), invalidValue);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals(LONG_VALUE_ERROR, e.getMessage());
    }

    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.ACCOUNT_UPGRADE_COST.getCode(), LONG_VALUE + 1);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals(LONG_VALUE_ERROR, e.getMessage());
    }

    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.CREATE_ACCOUNT_FEE.getCode(), invalidValue);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals(LONG_VALUE_ERROR, e.getMessage());
    }

    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.CREATE_ACCOUNT_FEE.getCode(), LONG_VALUE + 1);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals(LONG_VALUE_ERROR, e.getMessage());
    }

    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.ASSET_ISSUE_FEE.getCode(), invalidValue);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals(LONG_VALUE_ERROR, e.getMessage());
    }

    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.ASSET_ISSUE_FEE.getCode(), LONG_VALUE + 1);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals(LONG_VALUE_ERROR, e.getMessage());
    }

    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.WITNESS_PAY_PER_BLOCK.getCode(), invalidValue);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals(LONG_VALUE_ERROR, e.getMessage());
    }

    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.WITNESS_PAY_PER_BLOCK.getCode(), LONG_VALUE + 1);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals(LONG_VALUE_ERROR, e.getMessage());
    }

    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.WITNESS_STANDBY_ALLOWANCE.getCode(), invalidValue);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals(LONG_VALUE_ERROR, e.getMessage());
    }

    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.WITNESS_STANDBY_ALLOWANCE.getCode(), LONG_VALUE + 1);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals(LONG_VALUE_ERROR, e.getMessage());
    }

    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.CREATE_NEW_ACCOUNT_FEE_IN_SYSTEM_CONTRACT.getCode(), invalidValue);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals(LONG_VALUE_ERROR, e.getMessage());
    }

    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.CREATE_NEW_ACCOUNT_FEE_IN_SYSTEM_CONTRACT.getCode(), LONG_VALUE + 1);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals(LONG_VALUE_ERROR, e.getMessage());
    }

    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.CREATE_NEW_ACCOUNT_BANDWIDTH_RATE.getCode(), invalidValue);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals(LONG_VALUE_ERROR, e.getMessage());
    }

    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.CREATE_NEW_ACCOUNT_BANDWIDTH_RATE.getCode(), LONG_VALUE + 1);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals(LONG_VALUE_ERROR, e.getMessage());
    }

    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.MAINTENANCE_TIME_INTERVAL.getCode(), 3 * 27 * 1000 - 1);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals(
          "Bad chain parameter value, valid range is [3 * 27 * 1000,24 * 3600 * 1000]",
          e.getMessage());
    }

    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.MAINTENANCE_TIME_INTERVAL.getCode(), 24 * 3600 * 1000 + 1);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals(
          "Bad chain parameter value, valid range is [3 * 27 * 1000,24 * 3600 * 1000]",
          e.getMessage());
    }

    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.ALLOW_CREATION_OF_CONTRACTS.getCode(), 2);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals(
          "This value[ALLOW_CREATION_OF_CONTRACTS] is only allowed to be 1",
          e.getMessage());
    }

    dynamicPropertiesStore = dbManager.getDynamicPropertiesStore();
    dynamicPropertiesStore.saveRemoveThePowerOfTheGr(1);
    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.REMOVE_THE_POWER_OF_THE_GR.getCode(), 2);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals(
          "This value[REMOVE_THE_POWER_OF_THE_GR] is only allowed to be 1",
          e.getMessage());
    }

    dynamicPropertiesStore.saveRemoveThePowerOfTheGr(-1);
    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.REMOVE_THE_POWER_OF_THE_GR.getCode(), 1);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals(
          "This proposal has been executed before and is only allowed to be executed once",
          e.getMessage());
    }

    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.MAX_CPU_TIME_OF_ONE_TX.getCode(), 9);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals(
          "Bad chain parameter value, valid range is [10,100]", e.getMessage());
    }

    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.MAX_CPU_TIME_OF_ONE_TX.getCode(), 101);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals(
          "Bad chain parameter value, valid range is [10,100]", e.getMessage());
    }

    dynamicPropertiesStore.saveAllowHigherLimitForMaxCpuTimeOfOneTx(1);
    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.MAX_CPU_TIME_OF_ONE_TX.getCode(), 401);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals(
          "Bad chain parameter value, valid range is [10,400]", e.getMessage());
    }

    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.MAX_CPU_TIME_OF_ONE_TX.getCode(), 9);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals(
          "Bad chain parameter value, valid range is [10,400]", e.getMessage());
    }

    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.ALLOW_UPDATE_ACCOUNT_NAME.getCode(), 0);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals(
          "This value[ALLOW_UPDATE_ACCOUNT_NAME] is only allowed to be 1", e.getMessage());
    }

    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.ALLOW_SAME_TOKEN_NAME.getCode(), 0);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals(
          "This value[ALLOW_SAME_TOKEN_NAME] is only allowed to be 1", e.getMessage());
    }

    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.ALLOW_DELEGATE_RESOURCE.getCode(), 0);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals(
          "This value[ALLOW_DELEGATE_RESOURCE] is only allowed to be 1", e.getMessage());
    }

    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.TOTAL_ENERGY_LIMIT.getCode(), -1);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals("Bad chain parameter id", e.getMessage());
    }

    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.TOTAL_CURRENT_ENERGY_LIMIT.getCode(), -1);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals("Bad chain parameter id", e.getMessage());
    }

    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.ALLOW_MULTI_SIGN.getCode(), -1);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals("Bad chain parameter id: ALLOW_MULTI_SIGN", e.getMessage());
    }

    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.ALLOW_ADAPTIVE_ENERGY.getCode(), -1);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals("Bad chain parameter id: ALLOW_ADAPTIVE_ENERGY", e.getMessage());
    }

    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.UPDATE_ACCOUNT_PERMISSION_FEE.getCode(), -1);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals("Bad chain parameter id: UPDATE_ACCOUNT_PERMISSION_FEE", e.getMessage());
    }

    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.MULTI_SIGN_FEE.getCode(), -1);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals("Bad chain parameter id: MULTI_SIGN_FEE", e.getMessage());
    }

    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.ALLOW_PROTO_FILTER_NUM.getCode(), -1);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals("Bad chain parameter id", e.getMessage());
    }

    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.ALLOW_ACCOUNT_STATE_ROOT.getCode(), -1);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals("Bad chain parameter id", e.getMessage());
    }

    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.ALLOW_TVM_CONSTANTINOPLE.getCode(), -1);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals("Bad chain parameter id", e.getMessage());
    }

    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.ALLOW_TVM_SOLIDITY_059.getCode(), -1);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals("Bad chain parameter id", e.getMessage());
    }

    dynamicPropertiesStore.saveAllowCreationOfContracts(0);
    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.ALLOW_TVM_SOLIDITY_059.getCode(), 1);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals(
          "Bad chain parameter id",
          e.getMessage());
    }

    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.ADAPTIVE_RESOURCE_LIMIT_TARGET_RATIO.getCode(), -1);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals("Bad chain parameter id", e.getMessage());
    }

    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.ADAPTIVE_RESOURCE_LIMIT_MULTIPLIER.getCode(), -1);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals("Bad chain parameter id", e.getMessage());
    }

    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.ALLOW_CHANGE_DELEGATION.getCode(), -1);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals("Bad chain parameter id", e.getMessage());
    }

    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.WITNESS_127_PAY_PER_BLOCK.getCode(), -1);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals("Bad chain parameter id", e.getMessage());
    }

    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.FORBID_TRANSFER_TO_CONTRACT.getCode(), -1);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals("Bad chain parameter id", e.getMessage());
    }

    dynamicPropertiesStore.saveAllowCreationOfContracts(0);
    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.FORBID_TRANSFER_TO_CONTRACT.getCode(), 1);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals(
          "Bad chain parameter id",
          e.getMessage());
    }

    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.ALLOW_PBFT.getCode(), -1);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals("Bad chain parameter id [ALLOW_PBFT]", e.getMessage());
    }

    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.ALLOW_TVM_ISTANBUL.getCode(), -1);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals("Bad chain parameter id [ALLOW_TVM_ISTANBUL]", e.getMessage());
    }

    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.ALLOW_SHIELDED_TRC20_TRANSACTION.getCode(), -1);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals("Bad chain parameter id [ALLOW_SHIELDED_TRC20_TRANSACTION]", e.getMessage());
    }

    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.ALLOW_MARKET_TRANSACTION.getCode(), -1);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals("Bad chain parameter id [ALLOW_MARKET_TRANSACTION]", e.getMessage());
    }

    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.MARKET_SELL_FEE.getCode(), -1);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals("Bad chain parameter id [MARKET_SELL_FEE]", e.getMessage());
    }

    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.MARKET_CANCEL_FEE.getCode(), -1);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals("Bad chain parameter id [MARKET_CANCEL_FEE]", e.getMessage());
    }

    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.MAX_FEE_LIMIT.getCode(), -1);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals("Bad chain parameter id [MAX_FEE_LIMIT]", e.getMessage());
    }


    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.ALLOW_TRANSACTION_FEE_POOL.getCode(), -1);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals("Bad chain parameter id [ALLOW_TRANSACTION_FEE_POOL]", e.getMessage());
    }

    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.ALLOW_BLACKHOLE_OPTIMIZATION.getCode(), -1);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals("Bad chain parameter id [ALLOW_REMOVE_BLACKHOLE]", e.getMessage());
    }

    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.ALLOW_NEW_RESOURCE_MODEL.getCode(), -1);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals("Bad chain parameter id [ALLOW_NEW_RESOURCE_MODEL]", e.getMessage());
    }

    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.ALLOW_TVM_FREEZE.getCode(), -1);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals("Bad chain parameter id [ALLOW_TVM_FREEZE]", e.getMessage());
    }

    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.ALLOW_TVM_VOTE.getCode(), -1);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals("Bad chain parameter id [ALLOW_TVM_VOTE]", e.getMessage());
    }

    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.ALLOW_ACCOUNT_ASSET_OPTIMIZATION.getCode(), -1);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals("Bad chain parameter id [ALLOW_ACCOUNT_ASSET_OPTIMIZATION]", e.getMessage());
    }

    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.ALLOW_TVM_LONDON.getCode(), -1);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals("Bad chain parameter id [ALLOW_TVM_LONDON]", e.getMessage());
    }

    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.ALLOW_TVM_COMPATIBLE_EVM.getCode(), -1);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals("Bad chain parameter id [ALLOW_TVM_COMPATIBLE_EVM]", e.getMessage());
    }

    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.ALLOW_HIGHER_LIMIT_FOR_MAX_CPU_TIME_OF_ONE_TX.getCode(), -1);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals("Bad chain parameter id [ALLOW_HIGHER_LIMIT_FOR_MAX_CPU_TIME_OF_ONE_TX]", e.getMessage());
    }

    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.ALLOW_ASSET_OPTIMIZATION.getCode(), -1);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals("Bad chain parameter id [ALLOW_ASSET_OPTIMIZATION]", e.getMessage());
    }

    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.ALLOW_NEW_REWARD.getCode(), -1);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals("Bad chain parameter id [ALLOW_NEW_REWARD]", e.getMessage());
    }

    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.MEMO_FEE.getCode(), -1);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals("Bad chain parameter id [MEMO_FEE]", e.getMessage());
    }

    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.ALLOW_DELEGATE_OPTIMIZATION.getCode(), -1);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals("Bad chain parameter id [ALLOW_DELEGATE_OPTIMIZATION]", e.getMessage());
    }

    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.UNFREEZE_DELAY_DAYS.getCode(), -1);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals("Bad chain parameter id [UNFREEZE_DELAY_DAYS]", e.getMessage());
    }

    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.ALLOW_OPTIMIZED_RETURN_VALUE_OF_CHAIN_ID.getCode(), -1);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals("Bad chain parameter id [ALLOW_OPTIMIZED_RETURN_VALUE_OF_CHAIN_ID]", e.getMessage());
    }

    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.ALLOW_DYNAMIC_ENERGY.getCode(), -1);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals("Bad chain parameter id [ALLOW_DYNAMIC_ENERGY]", e.getMessage());
    }

    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.DYNAMIC_ENERGY_THRESHOLD.getCode(), -1);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals("Bad chain parameter id [DYNAMIC_ENERGY_THRESHOLD]", e.getMessage());
    }

    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.DYNAMIC_ENERGY_INCREASE_FACTOR.getCode(), -1);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals("Bad chain parameter id [DYNAMIC_ENERGY_INCREASE_FACTOR]", e.getMessage());
    }

    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.ALLOW_TVM_SHANGHAI.getCode(), -1);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals("Bad chain parameter id [ALLOW_TVM_SHANGHAI]", e.getMessage());
    }

    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.ALLOW_CANCEL_ALL_UNFREEZE_V2.getCode(), -1);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals("Bad chain parameter id [ALLOW_CANCEL_ALL_UNFREEZE_V2]", e.getMessage());
    }

    long maxDelegateLockPeriod = dynamicPropertiesStore.getMaxDelegateLockPeriod();
    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.MAX_DELEGATE_LOCK_PERIOD.getCode(), -1);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals("Bad chain parameter id [MAX_DELEGATE_LOCK_PERIOD]", e.getMessage());
    }

    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.ALLOW_DELEGATE_RESOURCE.getCode(), 2);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals(
          "This value[ALLOW_DELEGATE_RESOURCE] is only allowed to be 1", e.getMessage());
    }

    dynamicPropertiesStore.saveAllowSameTokenName(1);
    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.ALLOW_TVM_TRANSFER_TRC10.getCode(), 2);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals(
          "This value[ALLOW_TVM_TRANSFER_TRC10] is only allowed to be 1", e.getMessage());
    }

    dynamicPropertiesStore.saveAllowSameTokenName(0);
    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.ALLOW_TVM_TRANSFER_TRC10.getCode(), 1);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals("[ALLOW_SAME_TOKEN_NAME] proposal must be approved "
          + "before [ALLOW_TVM_TRANSFER_TRC10] can be proposed", e.getMessage());
    }

    forkUtils.init(dbManager.getChainBaseManager());
    long maintenanceTimeInterval = forkUtils.getManager().getDynamicPropertiesStore()
        .getMaintenanceTimeInterval();
    long hardForkTime =
        ((ForkBlockVersionEnum.VERSION_4_0_1.getHardForkTime() - 1) / maintenanceTimeInterval + 1)
            * maintenanceTimeInterval;
    forkUtils.getManager().getDynamicPropertiesStore()
        .saveLatestBlockHeaderTimestamp(hardForkTime + 1);
    byte[] stats = new byte[27];
    Arrays.fill(stats, (byte) 1);
    forkUtils.getManager().getDynamicPropertiesStore()
        .statsByVersion(ForkBlockVersionEnum.VERSION_4_0_1.getValue(), stats);
    ByteString address = ByteString
        .copyFrom(ByteArray.fromHexString("a0ec6525979a351a54fa09fea64beb4cce33ffbb7a"));
    List<ByteString> w = new ArrayList<>();
    w.add(address);
    forkUtils.getManager().getWitnessScheduleStore().saveActiveWitnesses(w);
    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.ALLOW_SHIELDED_TRC20_TRANSACTION
              .getCode(), 2);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals("This value[ALLOW_SHIELDED_TRC20_TRANSACTION] is only allowed"
          + " to be 1 or 0", e.getMessage());
    }

    hardForkTime =
        ((ForkBlockVersionEnum.VERSION_4_3.getHardForkTime() - 1) / maintenanceTimeInterval + 1)
            * maintenanceTimeInterval;
    forkUtils.getManager().getDynamicPropertiesStore()
        .saveLatestBlockHeaderTimestamp(hardForkTime + 1);
    forkUtils.getManager().getDynamicPropertiesStore()
        .statsByVersion(ForkBlockVersionEnum.VERSION_4_3.getValue(), stats);
    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils, ProposalType.FREE_NET_LIMIT
          .getCode(), -1);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals("Bad chain parameter value, valid range is [0,100_000]",
          e.getMessage());
    }

    try {
      ProposalUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.TOTAL_NET_LIMIT.getCode(), -1);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals("Bad chain parameter value, valid range is [0, 1_000_000_000_000L]",
          e.getMessage());
    }

    forkUtils.getManager().getDynamicPropertiesStore()
        .statsByVersion(ForkBlockVersionEnum.ENERGY_LIMIT.getValue(), stats);
    forkUtils.reset();
  }

  @Test
  public void blockVersionCheck() {
    for (ForkBlockVersionEnum forkVersion : ForkBlockVersionEnum.values()) {
      if (forkVersion.getValue() > Parameter.ChainConstant.BLOCK_VERSION) {
        Assert.fail("ForkBlockVersion must be less than BLOCK_VERSION");
      }
    }
  }
}
