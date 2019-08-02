package org.tron.core.actuator;

import static junit.framework.TestCase.fail;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.StringUtil;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.ContractCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;


@Slf4j
public class UpdateSettingContractActuatorTest {

  private static TronApplicationContext context;
  private static Manager dbManager;
  private static final String dbPath = "output_updatesettingcontract_test";
  private static final String OWNER_ADDRESS;
  private static final String OWNER_ADDRESS_ACCOUNT_NAME = "test_account";
  private static final String SECOND_ACCOUNT_ADDRESS;
  private static final String OWNER_ADDRESS_NOTEXIST;
  private static final String OWNER_ADDRESS_INVALID = "aaaa";
  private static final String SMART_CONTRACT_NAME = "smart_contarct";
  private static final String CONTRACT_ADDRESS = "111111";
  private static final String NO_EXIST_CONTRACT_ADDRESS = "2222222";
  private static final long SOURCE_PERCENT = 10L;
  private static final long TARGET_PERCENT = 30L;
  private static final long INVALID_PERCENT = 200L;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
    OWNER_ADDRESS_NOTEXIST =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
    SECOND_ACCOUNT_ADDRESS =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d427122222";
  }

  /**
   * Init data.
   */
  @BeforeClass
  public static void init() {
    dbManager = context.getBean(Manager.class);
  }

  /**
   * create temp Capsule test need.
   */
  @Before
  public void createCapsule() {
    // address in accountStore and the owner of contract
    AccountCapsule accountCapsule =
        new AccountCapsule(
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
            ByteString.copyFromUtf8(OWNER_ADDRESS_ACCOUNT_NAME),
            Protocol.AccountType.Normal);
    dbManager.getAccountStore().put(ByteArray.fromHexString(OWNER_ADDRESS), accountCapsule);

    // smartContarct in contractStore
    Protocol.SmartContract.Builder builder = Protocol.SmartContract.newBuilder();
    builder.setName(SMART_CONTRACT_NAME);
    builder.setOriginAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)));
    builder.setContractAddress(ByteString.copyFrom(ByteArray.fromHexString(CONTRACT_ADDRESS)));
    builder.setConsumeUserResourcePercent(SOURCE_PERCENT);
    dbManager.getContractStore().put(
        ByteArray.fromHexString(CONTRACT_ADDRESS),
        new ContractCapsule(builder.build()));

    // address in accountStore not the owner of contract
    AccountCapsule secondAccount =
        new AccountCapsule(
            ByteString.copyFrom(ByteArray.fromHexString(SECOND_ACCOUNT_ADDRESS)),
            ByteString.copyFromUtf8(OWNER_ADDRESS_ACCOUNT_NAME),
            Protocol.AccountType.Normal);
    dbManager.getAccountStore().put(ByteArray.fromHexString(SECOND_ACCOUNT_ADDRESS), secondAccount);

    // address does not exist in accountStore
    dbManager.getAccountStore().delete(ByteArray.fromHexString(OWNER_ADDRESS_NOTEXIST));
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

  private Any getContract(String accountAddress, String contractAddress, long percent) {
    return Any.pack(
        Contract.UpdateSettingContract.newBuilder()
            .setOwnerAddress(StringUtil.hexString2ByteString(accountAddress))
            .setContractAddress(StringUtil.hexString2ByteString(contractAddress))
            .setConsumeUserResourcePercent(percent).build());
  }

  @Test
  public void successUpdateSettingContract() {
    UpdateSettingContractActuator actuator =
        new UpdateSettingContractActuator(
            getContract(OWNER_ADDRESS, CONTRACT_ADDRESS, TARGET_PERCENT), dbManager);

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);

      // assert result state and consume_user_resource_percent
      Assert.assertEquals(ret.getInstance().getRet(), Protocol.Transaction.Result.code.SUCESS);
      Assert.assertEquals(
          dbManager.getContractStore().get(ByteArray.fromHexString(CONTRACT_ADDRESS)).
              getConsumeUserResourcePercent(),
          TARGET_PERCENT);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void invalidAddress() {
    UpdateSettingContractActuator actuator =
        new UpdateSettingContractActuator(
            getContract(OWNER_ADDRESS_INVALID, CONTRACT_ADDRESS, TARGET_PERCENT), dbManager);

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);

      fail("Invalid address");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Invalid address", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void noExistAccount() {
    UpdateSettingContractActuator actuator =
        new UpdateSettingContractActuator(
            getContract(OWNER_ADDRESS_NOTEXIST, CONTRACT_ADDRESS, TARGET_PERCENT), dbManager);

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);

      fail("Account[" + OWNER_ADDRESS_NOTEXIST + "] not exists");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Account[" + OWNER_ADDRESS_NOTEXIST + "] not exists", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void invalidResourcePercent() {
    UpdateSettingContractActuator actuator =
        new UpdateSettingContractActuator(
            getContract(OWNER_ADDRESS, CONTRACT_ADDRESS, INVALID_PERCENT), dbManager);

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);

      fail("percent not in [0, 100]");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("percent not in [0, 100]", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void noExistContract() {
    UpdateSettingContractActuator actuator =
        new UpdateSettingContractActuator(
            getContract(OWNER_ADDRESS, NO_EXIST_CONTRACT_ADDRESS, TARGET_PERCENT), dbManager);

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);

      fail("Contract not exists");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Contract not exists", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void callerNotContractOwner() {
    UpdateSettingContractActuator actuator =
        new UpdateSettingContractActuator(
            getContract(SECOND_ACCOUNT_ADDRESS, CONTRACT_ADDRESS, TARGET_PERCENT), dbManager);

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);

      fail("Account[" + SECOND_ACCOUNT_ADDRESS + "] is not the owner of the contract");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals(
          "Account[" + SECOND_ACCOUNT_ADDRESS + "] is not the owner of the contract",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void twiceUpdateSettingContract() {
    UpdateSettingContractActuator actuator =
        new UpdateSettingContractActuator(
            getContract(OWNER_ADDRESS, CONTRACT_ADDRESS, TARGET_PERCENT), dbManager);

    UpdateSettingContractActuator secondActuator =
        new UpdateSettingContractActuator(
            getContract(OWNER_ADDRESS, CONTRACT_ADDRESS, 90L), dbManager);

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      // first
      actuator.validate();
      actuator.execute(ret);

      Assert.assertEquals(ret.getInstance().getRet(), Protocol.Transaction.Result.code.SUCESS);
      Assert.assertEquals(
          dbManager.getContractStore().get(ByteArray.fromHexString(CONTRACT_ADDRESS)).
              getConsumeUserResourcePercent(),
          TARGET_PERCENT);

      // second
      secondActuator.validate();
      secondActuator.execute(ret);

      Assert.assertEquals(ret.getInstance().getRet(), Protocol.Transaction.Result.code.SUCESS);
      Assert.assertEquals(
          dbManager.getContractStore().get(ByteArray.fromHexString(CONTRACT_ADDRESS)).
              getConsumeUserResourcePercent(),
          90L);

    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

}
