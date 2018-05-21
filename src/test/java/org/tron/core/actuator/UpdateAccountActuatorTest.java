package org.tron.core.actuator;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Contract;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Transaction.Result.code;

@Slf4j
public class UpdateAccountActuatorTest {

  private static AnnotationConfigApplicationContext context;
  private static Manager dbManager;
  private static final String dbPath = "output_updateaccount_test";
  private static final String ACCOUNT_NAME = "ownerTest";
  private static final String ACCOUNT_NAME_1 = "ownerTest1";
  private static final String OWNER_ADDRESS;
  private static final String OWNER_ADDRESS_1;
  private static final String OWNER_ADDRESS_INVALIATE = "aaaa";

  static {
    Args.setParam(new String[]{"--output-directory", dbPath}, Constant.TEST_CONF);
    context = new AnnotationConfigApplicationContext(DefaultConfig.class);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
    OWNER_ADDRESS_1 = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
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
    AccountCapsule ownerCapsule =
        new AccountCapsule(
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
            ByteString.EMPTY,
            AccountType.Normal);
    dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);
    dbManager.getAccountStore().delete(ByteArray.fromHexString(OWNER_ADDRESS_1));
    dbManager.getAccountIndexStore().delete(ACCOUNT_NAME.getBytes());
    dbManager.getAccountIndexStore().delete(ACCOUNT_NAME_1.getBytes());
  }

  private Any getContract(String name, String address) {
    return Any.pack(
        Contract.AccountUpdateContract.newBuilder()
            .setAccountName(ByteString.copyFromUtf8(name))
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(address)))
            .build());
  }

  private Any getContract(ByteString name, String address) {
    return Any.pack(
        Contract.AccountUpdateContract.newBuilder()
            .setAccountName(name)
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(address)))
            .build());
  }

  /**
   * Unit test.
   */
  /**
   * Update account when all right.
   */
  @Test
  public void rightUpdateAccount() {
    TransactionResultCapsule ret = new TransactionResultCapsule();
    UpdateAccountActuator actuator = new UpdateAccountActuator(
        getContract(ACCOUNT_NAME, OWNER_ADDRESS), dbManager);
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule accountCapsule = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));
      Assert.assertEquals(ACCOUNT_NAME, accountCapsule.getAccountName().toStringUtf8());
      Assert.assertTrue(true);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    }
  }

  @Test
  public void invalidAddress() {
    TransactionResultCapsule ret = new TransactionResultCapsule();
    UpdateAccountActuator actuator = new UpdateAccountActuator(
        getContract(ACCOUNT_NAME, OWNER_ADDRESS_INVALIATE), dbManager);
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertFalse(true);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Invalidate ownerAddress", e.getMessage());
    }
  }

  @Test
  public void noExitAccount() {
    TransactionResultCapsule ret = new TransactionResultCapsule();
    UpdateAccountActuator actuator = new UpdateAccountActuator(
        getContract(ACCOUNT_NAME, OWNER_ADDRESS_1), dbManager);
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertFalse(true);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Account has not existed", e.getMessage());
    }
  }

  @Test
  /*
   * Can update name only one time.
   */
  public void twiceUpdateAccount() {
    TransactionResultCapsule ret = new TransactionResultCapsule();
    UpdateAccountActuator actuator = new UpdateAccountActuator(
        getContract(ACCOUNT_NAME, OWNER_ADDRESS), dbManager);
    UpdateAccountActuator actuator1 = new UpdateAccountActuator(
        getContract(ACCOUNT_NAME_1, OWNER_ADDRESS), dbManager);
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule accountCapsule = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));
      Assert.assertEquals(ACCOUNT_NAME, accountCapsule.getAccountName().toStringUtf8());
      Assert.assertTrue(true);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    }

    try {
      actuator1.validate();
      actuator1.execute(ret);
      Assert.assertFalse(true);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("This account name already exist", e.getMessage());
      AccountCapsule accountCapsule = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));
      Assert.assertEquals(ACCOUNT_NAME, accountCapsule.getAccountName().toStringUtf8());
    }
  }

  @Test
  public void nameAlreadyUsed() {
    TransactionResultCapsule ret = new TransactionResultCapsule();
    UpdateAccountActuator actuator = new UpdateAccountActuator(
        getContract(ACCOUNT_NAME, OWNER_ADDRESS), dbManager);
    UpdateAccountActuator actuator1 = new UpdateAccountActuator(
        getContract(ACCOUNT_NAME, OWNER_ADDRESS_1), dbManager);
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule accountCapsule = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));
      Assert.assertEquals(ACCOUNT_NAME, accountCapsule.getAccountName().toStringUtf8());
      Assert.assertTrue(true);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    }

    AccountCapsule ownerCapsule =
        new AccountCapsule(
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_1)),
            ByteString.EMPTY,
            AccountType.Normal);
    dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);

    try {
      actuator1.validate();
      actuator1.execute(ret);
      Assert.assertFalse(true);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("This name has existed", e.getMessage());
      AccountCapsule accountCapsule = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));
      Assert.assertEquals(ACCOUNT_NAME, accountCapsule.getAccountName().toStringUtf8());
    }
  }

  @Test
  /*
   * Account name need 8 - 32 bytes.
   */
  public void invalidName() {
    TransactionResultCapsule ret = new TransactionResultCapsule();
    //Just OK 32 bytes is OK
    try {
      UpdateAccountActuator actuator = new UpdateAccountActuator(
          getContract("testname0123456789abcdefghijgklm", OWNER_ADDRESS), dbManager);
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule accountCapsule = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));
      Assert.assertEquals("testname0123456789abcdefghijgklm",
          accountCapsule.getAccountName().toStringUtf8());
      Assert.assertTrue(true);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    }
    //8 bytes is OK
    AccountCapsule accountCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    accountCapsule.setAccountName(ByteString.EMPTY.toByteArray());
    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
    try {
      UpdateAccountActuator actuator = new UpdateAccountActuator(
          getContract("testname", OWNER_ADDRESS), dbManager);
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      accountCapsule = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));
      Assert.assertEquals("testname",
          accountCapsule.getAccountName().toStringUtf8());
      Assert.assertTrue(true);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    }
    //Empty name
    try {
      UpdateAccountActuator actuator = new UpdateAccountActuator(
          getContract(ByteString.EMPTY, OWNER_ADDRESS), dbManager);
      actuator.validate();
      actuator.execute(ret);
      Assert.assertFalse(true);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Invalidate accountName", e.getMessage());
    }
    //Too long name 33 bytes
    try {
      UpdateAccountActuator actuator = new UpdateAccountActuator(
          getContract("testname0123456789abcdefghijgklmo", OWNER_ADDRESS), dbManager);
      actuator.validate();
      actuator.execute(ret);
      Assert.assertFalse(true);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Invalidate accountName", e.getMessage());
    }
    //Too short name 7 bytes
    try {
      UpdateAccountActuator actuator = new UpdateAccountActuator(
          getContract("testnam", OWNER_ADDRESS), dbManager);
      actuator.validate();
      actuator.execute(ret);
      Assert.assertFalse(true);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Invalidate accountName", e.getMessage());
    }

    //Can't contain space
    try {
      UpdateAccountActuator actuator = new UpdateAccountActuator(
          getContract("t e", OWNER_ADDRESS), dbManager);
      actuator.validate();
      actuator.execute(ret);
      Assert.assertFalse(true);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Invalidate accountName", e.getMessage());
    }
    //Can't contain chinese characters
    try {
      UpdateAccountActuator actuator = new UpdateAccountActuator(
          getContract("测试", OWNER_ADDRESS), dbManager);
      actuator.validate();
      actuator.execute(ret);
      Assert.assertFalse(true);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Invalidate accountName", e.getMessage());
    }
  }

  /**
   * Release resources.
   */
  @AfterClass
  public static void destroy() {
    Args.clearParam();
    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
    }
    context.destroy();
  }
}
