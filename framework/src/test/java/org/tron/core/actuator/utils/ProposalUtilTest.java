package org.tron.core.actuator.utils;

import com.google.protobuf.ByteString;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.ForkController;
import org.tron.core.Constant;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.Parameter;
import org.tron.core.config.Parameter.ForkBlockVersionEnum;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.utils.ProposalUtil;
import org.tron.core.utils.ProposalUtil.ProposalType;

@Slf4j(topic = "actuator")
public class ProposalUtilTest {

  private static final String dbPath = "output_ProposalUtil_test";
  private static final long LONG_VALUE = 100_000_000_000_000_000L;
  private static final String LONG_VALUE_ERROR =
      "Bad chain parameter value, valid range is [0," + LONG_VALUE + "]";
  public static Application AppT;
  private static TronApplicationContext context;
  private static Manager dbManager;

  /**
   * Init .
   */
  @BeforeClass
  public static void init() {
    Args.setParam(new String[]{"--output-directory", dbPath}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    dbManager = context.getBean(Manager.class);
    AppT = ApplicationFactory.create(context);
  }

  /**
   * Release resources.
   */
  @AfterClass
  public static void destroy() {
    Args.clearParam();
    context.destroy();
    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
    }
  }

  @Test
  public void validProposalTypeCheck() throws ContractValidateException {

    Assert.assertEquals(false, ProposalType.contain(4000));
    Assert.assertEquals(false, ProposalType.contain(-1));
    Assert.assertEquals(true, ProposalType.contain(2));

    Assert.assertEquals(null, ProposalType.getEnumOrNull(-2));
    Assert.assertEquals(ProposalType.ALLOW_TVM_SOLIDITY_059, ProposalType.getEnumOrNull(32));

    long code = -1;
    try {
      ProposalType.getEnum(code);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertEquals("Does not support code : " + code, e.getMessage());
    }

    code = 32;
    Assert.assertEquals(ProposalType.ALLOW_TVM_SOLIDITY_059, ProposalType.getEnum(code));

  }

  @Test
  public void validateCheck() {
    ProposalUtil actuatorUtil = new ProposalUtil();
    DynamicPropertiesStore dynamicPropertiesStore = null;
    ForkController forkUtils = ForkController.instance();
    long invalidValue = -1;

    try {
      actuatorUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.ACCOUNT_UPGRADE_COST.getCode(), invalidValue);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertEquals(LONG_VALUE_ERROR, e.getMessage());
    }

    try {
      actuatorUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.ACCOUNT_UPGRADE_COST.getCode(), LONG_VALUE + 1);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertEquals(LONG_VALUE_ERROR, e.getMessage());
    }

    try {
      actuatorUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.CREATE_ACCOUNT_FEE.getCode(), invalidValue);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertEquals(LONG_VALUE_ERROR, e.getMessage());
    }

    try {
      actuatorUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.CREATE_ACCOUNT_FEE.getCode(), LONG_VALUE + 1);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertEquals(LONG_VALUE_ERROR, e.getMessage());
    }

    try {
      actuatorUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.ASSET_ISSUE_FEE.getCode(), invalidValue);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertEquals(LONG_VALUE_ERROR, e.getMessage());
    }

    try {
      actuatorUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.ASSET_ISSUE_FEE.getCode(), LONG_VALUE + 1);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertEquals(LONG_VALUE_ERROR, e.getMessage());
    }

    try {
      actuatorUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.WITNESS_PAY_PER_BLOCK.getCode(), invalidValue);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertEquals(LONG_VALUE_ERROR, e.getMessage());
    }

    try {
      actuatorUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.WITNESS_PAY_PER_BLOCK.getCode(), LONG_VALUE + 1);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertEquals(LONG_VALUE_ERROR, e.getMessage());
    }

    try {
      actuatorUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.WITNESS_STANDBY_ALLOWANCE.getCode(), invalidValue);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertEquals(LONG_VALUE_ERROR, e.getMessage());
    }

    try {
      actuatorUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.WITNESS_STANDBY_ALLOWANCE.getCode(), LONG_VALUE + 1);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertEquals(LONG_VALUE_ERROR, e.getMessage());
    }

    try {
      actuatorUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.CREATE_NEW_ACCOUNT_FEE_IN_SYSTEM_CONTRACT.getCode(), invalidValue);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertEquals(LONG_VALUE_ERROR, e.getMessage());
    }

    try {
      actuatorUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.CREATE_NEW_ACCOUNT_FEE_IN_SYSTEM_CONTRACT.getCode(), LONG_VALUE + 1);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertEquals(LONG_VALUE_ERROR, e.getMessage());
    }

    try {
      actuatorUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.CREATE_NEW_ACCOUNT_BANDWIDTH_RATE.getCode(), invalidValue);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertEquals(LONG_VALUE_ERROR, e.getMessage());
    }

    try {
      actuatorUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.CREATE_NEW_ACCOUNT_BANDWIDTH_RATE.getCode(), LONG_VALUE + 1);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertEquals(LONG_VALUE_ERROR, e.getMessage());
    }

    long value = 32;
    try {
      actuatorUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.MAINTENANCE_TIME_INTERVAL.getCode(), 3 * 27 * 1000 - 1);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertEquals(
          "Bad chain parameter value, valid range is [3 * 27 * 1000,24 * 3600 * 1000]",
          e.getMessage());
    }

    try {
      actuatorUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.MAINTENANCE_TIME_INTERVAL.getCode(), 24 * 3600 * 1000 + 1);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertEquals(
          "Bad chain parameter value, valid range is [3 * 27 * 1000,24 * 3600 * 1000]",
          e.getMessage());
    }

    try {
      actuatorUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.ALLOW_CREATION_OF_CONTRACTS.getCode(), 2);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertEquals(
          "This value[ALLOW_CREATION_OF_CONTRACTS] is only allowed to be 1",
          e.getMessage());
    }

    dynamicPropertiesStore = dbManager.getDynamicPropertiesStore();
    dynamicPropertiesStore.saveRemoveThePowerOfTheGr(1);
    try {
      actuatorUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.REMOVE_THE_POWER_OF_THE_GR.getCode(), 2);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertEquals(
          "This value[REMOVE_THE_POWER_OF_THE_GR] is only allowed to be 1",
          e.getMessage());
    }

    dynamicPropertiesStore.saveRemoveThePowerOfTheGr(-1);
    try {
      actuatorUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.REMOVE_THE_POWER_OF_THE_GR.getCode(), 1);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertEquals(
          "This proposal has been executed before and is only allowed to be executed once",
          e.getMessage());
    }

    try {
      actuatorUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.MAX_CPU_TIME_OF_ONE_TX.getCode(), 9);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertEquals(
          "Bad chain parameter value, valid range is [10,100]", e.getMessage());
    }

    try {
      actuatorUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.MAX_CPU_TIME_OF_ONE_TX.getCode(), 101);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertEquals(
          "Bad chain parameter value, valid range is [10,100]", e.getMessage());
    }

    try {
      actuatorUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.ALLOW_DELEGATE_RESOURCE.getCode(), 2);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertEquals(
          "This value[ALLOW_DELEGATE_RESOURCE] is only allowed to be 1", e.getMessage());
    }

    dynamicPropertiesStore.saveAllowSameTokenName(1);
    try {
      actuatorUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.ALLOW_TVM_TRANSFER_TRC10.getCode(), 2);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertEquals(
          "This value[ALLOW_TVM_TRANSFER_TRC10] is only allowed to be 1", e.getMessage());
    }

    dynamicPropertiesStore.saveAllowSameTokenName(0);
    try {
      actuatorUtil.validator(dynamicPropertiesStore, forkUtils,
          ProposalType.ALLOW_TVM_TRANSFER_TRC10.getCode(), 1);
      Assert.assertTrue(false);
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
