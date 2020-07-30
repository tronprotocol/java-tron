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
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Transaction.Result.code;
import org.tron.protos.contract.AccountContract.AccountUpdateContract;
import org.tron.protos.contract.AssetIssueContractOuterClass;

@Slf4j
public class UpdateAccountActuatorTest {

  private static final String dbPath = "output_updateaccount_test";
  private static final String ACCOUNT_NAME = "ownerTest";
  private static final String ACCOUNT_NAME_1 = "ownerTest1";
  private static final String OWNER_ADDRESS;
  private static final String OWNER_ADDRESS_1;
  private static final String OWNER_ADDRESS_INVALID = "aaaa";
  private static TronApplicationContext context;
  private static Manager dbManager;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
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

  /**
   * create temp Capsule test need.
   */
  @Before
  public void createCapsule() {

    dbManager.getDynamicPropertiesStore().saveAllowUpdateAccountName(0);   // reset allowUpdate

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
        AccountUpdateContract.newBuilder()
            .setAccountName(ByteString.copyFromUtf8(name))
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(address)))
            .build());
  }

  /**
   * Unit test.
   */

  private Any getContract(ByteString name, String address) {
    return Any.pack(
        AccountUpdateContract.newBuilder()
            .setAccountName(name)
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(address)))
            .build());
  }


  /**
   * update a account with a valid accountName and OwnerAddress
   */
  private void UpdateAccount(String accountName, String OwnerAddress) {

    TransactionResultCapsule ret = new TransactionResultCapsule();
    UpdateAccountActuator actuator = new UpdateAccountActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(accountName, OwnerAddress));
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule accountCapsule = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OwnerAddress));
      Assert.assertEquals(accountName, accountCapsule.getAccountName().toStringUtf8());
      Assert.assertTrue(true);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }


  /**
   * Update account when all right.
   */
  @Test
  public void rightUpdateAccount() {
    UpdateAccount(ACCOUNT_NAME, OWNER_ADDRESS);
  }

  @Test
  public void invalidAddress() {
    TransactionResultCapsule ret = new TransactionResultCapsule();
    UpdateAccountActuator actuator = new UpdateAccountActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(ACCOUNT_NAME, OWNER_ADDRESS_INVALID));

    processAndCheckInvalid(actuator, ret, "Invalid ownerAddress",
        "Invalid ownerAddress");

  }

  @Test
  public void noExitAccount() {
    TransactionResultCapsule ret = new TransactionResultCapsule();
    UpdateAccountActuator actuator = new UpdateAccountActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(ACCOUNT_NAME, OWNER_ADDRESS_1));

    processAndCheckInvalid(actuator, ret, "Account does not exist",
        "Account does not exist");

  }

  @Test
  /*
   * Can update name only one time.
   */
  public void twiceUpdateAccountFail() {

    UpdateAccount(ACCOUNT_NAME, OWNER_ADDRESS);  // firstly update account
    TransactionResultCapsule ret = new TransactionResultCapsule();
    UpdateAccountActuator actuator1 = new UpdateAccountActuator();
    actuator1.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(ACCOUNT_NAME_1, OWNER_ADDRESS));

    try {
      actuator1.validate();
      actuator1.execute(ret);
      Assert.assertFalse(true);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("This account name is already existed", e.getMessage());
      AccountCapsule accountCapsule = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));
      Assert.assertEquals(ACCOUNT_NAME, accountCapsule.getAccountName().toStringUtf8());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  /*
   * Can update name more than one time
   */
  public void twiceUpdateAccountSuccess() {

    UpdateAccount(ACCOUNT_NAME, OWNER_ADDRESS);  // firstly update account

    dbManager.getDynamicPropertiesStore().saveAllowUpdateAccountName(1);   // allowUpdate more
    // than 1 time
    UpdateAccount(ACCOUNT_NAME_1, OWNER_ADDRESS);  // second update

    String accountTest = "third Update";

    UpdateAccount(accountTest, OWNER_ADDRESS);  // Third update

    dbManager.getAccountIndexStore().delete(accountTest.getBytes());  // delete it after test


  }


  @Test
  public void updateSameNameSuccess() {

    UpdateAccount(ACCOUNT_NAME, OWNER_ADDRESS);   // first update account

    dbManager.getDynamicPropertiesStore().saveAllowUpdateAccountName(1);   // allow update more
    // than one time
    UpdateAccount(ACCOUNT_NAME, OWNER_ADDRESS);   // second update with same account Name

    UpdateAccount("sameName", OWNER_ADDRESS);   // Third Update

    UpdateAccount("sameName", OWNER_ADDRESS);   // fourth Update with same accountName

    dbManager.getAccountIndexStore().delete(ACCOUNT_NAME.getBytes());

  }

  @Test
  public void updateSameNameFail() {
    UpdateAccount(ACCOUNT_NAME, OWNER_ADDRESS);   // first update account

    TransactionResultCapsule ret = new TransactionResultCapsule();
    UpdateAccountActuator actuator1 = new UpdateAccountActuator();
    actuator1.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(ACCOUNT_NAME, OWNER_ADDRESS_1));

    AccountCapsule ownerCapsule =
        new AccountCapsule(
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_1)),
            ByteString.EMPTY,
            AccountType.Normal);
    dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);

    dbManager.getDynamicPropertiesStore().saveAllowUpdateAccountName(0);   // reset allowUpdate

    try {
      actuator1.validate();
      actuator1.execute(ret);
      Assert.assertFalse(true);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("This name is existed", e.getMessage());
      AccountCapsule accountCapsule = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));
      Assert.assertEquals(ACCOUNT_NAME, accountCapsule.getAccountName().toStringUtf8());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }


  @Test
  /*
   * Account name need 8 - 32 bytes.
   */
  public void invalidName() {
    dbManager.getDynamicPropertiesStore().saveAllowUpdateAccountName(1);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    //Just OK 32 bytes is OK
    try {
      UpdateAccountActuator actuator = new UpdateAccountActuator();
      actuator.setChainBaseManager(dbManager.getChainBaseManager())
          .setAny(getContract("testname0123456789abcdefghijgklm", OWNER_ADDRESS));

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
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
    //8 bytes is OK
    AccountCapsule accountCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    accountCapsule.setAccountName(ByteString.EMPTY.toByteArray());
    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
    try {
      UpdateAccountActuator actuator = new UpdateAccountActuator();
      actuator.setChainBaseManager(dbManager.getChainBaseManager())
          .setAny(getContract("testname", OWNER_ADDRESS));

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
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
    //Empty name
    try {
      UpdateAccountActuator actuator = new UpdateAccountActuator();
      actuator.setChainBaseManager(dbManager.getChainBaseManager())
          .setAny(getContract(ByteString.EMPTY, OWNER_ADDRESS));

      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Invalid accountName", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
    //Too long name 33 bytes
    try {
      UpdateAccountActuator actuator = new UpdateAccountActuator();
      actuator.setChainBaseManager(dbManager.getChainBaseManager())
          .setAny(getContract("testname0123456789abcdefghijgklmo0123456789abcdefghijgk"
              + "lmo0123456789abcdefghijgklmo0123456789abcdefghijgklmo0123456789abcdefghijgklmo"
              + "0123456789abcdefghijgklmo0123456789abcdefghijgklmo0123456789abcdefghijgklmo"
              + "0123456789abcdefghijgklmo0123456789abcdefghijgklmo", OWNER_ADDRESS));

      actuator.validate();
      actuator.execute(ret);
      Assert.assertFalse(true);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Invalid accountName", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }


  @Test
  public void noContract() {

    UpdateAccountActuator actuator = new UpdateAccountActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(null);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    processAndCheckInvalid(actuator, ret, "No contract!", "No contract!");
  }

  @Test
  public void invalidContractType() {
    UpdateAccountActuator actuator = new UpdateAccountActuator();
    // create AssetIssueContract, not a valid ClearABI contract , which will throw e expectipon
    Any invalidContractTypes = Any.pack(AssetIssueContractOuterClass.AssetIssueContract.newBuilder()
        .build());
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(invalidContractTypes);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    processAndCheckInvalid(actuator, ret, "contract type error",
        "contract type error, expected type [AccountUpdateContract], real type["
            + invalidContractTypes.getClass() + "]");
  }

  @Test
  public void nullTransationResult() {
    UpdateAccountActuator actuator = new UpdateAccountActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(ACCOUNT_NAME, OWNER_ADDRESS));
    TransactionResultCapsule ret = null;
    processAndCheckInvalid(actuator, ret, "TransactionResultCapsule is null",
        "TransactionResultCapsule is null");
  }

  private void processAndCheckInvalid(UpdateAccountActuator actuator,
      TransactionResultCapsule ret,
      String failMsg,
      String expectedMsg) {
    try {
      actuator.validate();
      actuator.execute(ret);
      fail(failMsg);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals(expectedMsg, e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } catch (RuntimeException e) {
      Assert.assertTrue(e instanceof RuntimeException);
      Assert.assertEquals(expectedMsg, e.getMessage());
    }
  }


}
