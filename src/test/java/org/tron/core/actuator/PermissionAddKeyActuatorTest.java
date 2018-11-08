package org.tron.core.actuator;

import static junit.framework.TestCase.fail;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Contract;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Key;
import org.tron.protos.Protocol.Permission;
import org.tron.protos.Protocol.Transaction.Result.code;

@Slf4j
public class PermissionAddKeyActuatorTest {

  private static Manager dbManager;
  private static final String dbPath = "output_transfer_test";
  private static TronApplicationContext context;
  private static Application AppT;
  private static final String OWNER_ADDRESS;
  private static final String KEY_ADDRESS;
  private static final Key VALID_KEY;
  private static final long KEY_WEIGHT = 1;
  private static final String PERMISSION_NAME = "active";
  private static final String SECOND_KEY_ADDRESS;

  private static final String OWNER_ADDRESS_INVALID = "aaaa";
  private static final String OWNER_ADDRESS_NOACCOUNT;
  private static final String PERMISSION_NAME_INVALID = "test";
  private static final String KEY_ADDRESS_INVALID = "bbbb";
  private static final long KEY_WEIGHT_INVALID = -1;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    AppT = ApplicationFactory.create(context);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
    KEY_ADDRESS = Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";

    VALID_KEY =
        Key.newBuilder()
            .setAddress(ByteString.copyFrom(ByteArray.fromHexString(KEY_ADDRESS)))
            .setWeight(KEY_WEIGHT)
            .build();

    SECOND_KEY_ADDRESS =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a3456";
    OWNER_ADDRESS_NOACCOUNT =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1aed";
  }

  /**
   * Init data.
   */
  @BeforeClass
  public static void init() {
    dbManager = context.getBean(Manager.class);
    dbManager.getDynamicPropertiesStore().saveAllowMultiSign(1);
    dbManager.getDynamicPropertiesStore().saveTotalSignNum(5);
  }

  /**
   * Release resources.
   */
  @AfterClass
  public static void destroy() {
    Args.clearParam();
    AppT.shutdownServices();
    AppT.shutdown();
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
    AccountCapsule ownerCapsule =
        new AccountCapsule(
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
            ByteString.copyFromUtf8("owner"),
            AccountType.Normal);
    dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);
  }

  private Any getContract() {
    return Any.pack(
        Contract.PermissionAddKeyContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setKey(VALID_KEY)
            .setPermissionName(PERMISSION_NAME)
            .build());
  }

  private Any getInvalidContract() {
    return Any.pack(
        Contract.PermissionDeleteKeyContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setKeyAddress(ByteString.copyFrom(ByteArray.fromHexString(KEY_ADDRESS)))
            .setPermissionName(PERMISSION_NAME)
            .build());
  }

  private Any getContract(String ownerAddress, Key key, String permissionName) {
    return Any.pack(
        Contract.PermissionAddKeyContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
            .setKey(key)
            .setPermissionName(permissionName)
            .build());
  }

  private Any getContract(
      String ownerAddress, String keyAddress, long keyWeight, String permissionName) {
    Key key =
        Key.newBuilder()
            .setAddress(ByteString.copyFrom(ByteArray.fromHexString(keyAddress)))
            .setWeight(keyWeight)
            .build();

    return Any.pack(
        Contract.PermissionAddKeyContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
            .setKey(key)
            .setPermissionName(permissionName)
            .build());
  }

  private void processAndCheckInvalid(
      PermissionAddKeyActuator actuator,
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
    }
  }

  @Test
  public void successFistAddPermissionKey() {
    PermissionAddKeyActuator actuator = new PermissionAddKeyActuator(getContract(), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    String ownerAddress = OWNER_ADDRESS;
    byte[] owner_name_array = ByteArray.fromHexString(ownerAddress);

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);

      AccountCapsule owner = dbManager.getAccountStore().get(owner_name_array);

      List<Permission> expectedPermissions = new ArrayList<>();
      Permission ownerPermission =
          TransactionCapsule.getDefaultPermission(ByteString.copyFrom(owner_name_array), "owner");
      Permission activePermission =
          Permission.newBuilder()
              .setName("active")
              .setThreshold(1)
              .setParent("owner")
              .addKeys(
                  Key.newBuilder()
                      .setAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
                      .setWeight(1)
                      .build())
              .addKeys(
                  Key.newBuilder()
                      .setAddress(ByteString.copyFrom(ByteArray.fromHexString(KEY_ADDRESS)))
                      .setWeight(KEY_WEIGHT)
                      .build())
              .build();
      expectedPermissions.add(ownerPermission);
      expectedPermissions.add(activePermission);

      List<Permission> ownerPermissions = owner.getPermissions();

      Assert.assertEquals(expectedPermissions, ownerPermissions);
      Assert.assertTrue(true);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void successSecondAddPermissionKey() {
    // init
    long keyWeight = 2;

    byte[] owner_name_array = ByteArray.fromHexString(OWNER_ADDRESS);
    AccountCapsule account = dbManager.getAccountStore().get(owner_name_array);
    account.permissionAddKey(VALID_KEY, PERMISSION_NAME);
    dbManager.getAccountStore().put(owner_name_array, account);

    PermissionAddKeyActuator actuator =
        new PermissionAddKeyActuator(
            getContract(OWNER_ADDRESS, SECOND_KEY_ADDRESS, keyWeight, "owner"), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);

      AccountCapsule owner = dbManager.getAccountStore().get(owner_name_array);

      List<Permission> expectedPermissions = new ArrayList<>();
      Permission ownerPermission =
          Permission.newBuilder()
              .setName("owner")
              .setThreshold(1)
              .addKeys(
                  Key.newBuilder()
                      .setAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
                      .setWeight(1)
                      .build())
              .addKeys(
                  Key.newBuilder()
                      .setAddress(ByteString.copyFrom(ByteArray.fromHexString(SECOND_KEY_ADDRESS)))
                      .setWeight(keyWeight)
                      .build())
              .build();
      Permission activePermission =
          Permission.newBuilder()
              .setName("active")
              .setThreshold(1)
              .setParent("owner")
              .addKeys(
                  Key.newBuilder()
                      .setAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
                      .setWeight(1)
                      .build())
              .addKeys(
                  Key.newBuilder()
                      .setAddress(ByteString.copyFrom(ByteArray.fromHexString(KEY_ADDRESS)))
                      .setWeight(KEY_WEIGHT)
                      .build())
              .build();
      expectedPermissions.add(ownerPermission);
      expectedPermissions.add(activePermission);

      List<Permission> ownerPermissions = owner.getPermissions();

      Assert.assertEquals(expectedPermissions, ownerPermissions);
      Assert.assertTrue(true);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void successAddOwnerSelfKey() {
    long keyWeight = 3;
    String permissionName = "owner";
    PermissionAddKeyActuator actuator =
        new PermissionAddKeyActuator(
            getContract(OWNER_ADDRESS, OWNER_ADDRESS, keyWeight, permissionName), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);

      AccountCapsule owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));

      List<Permission> expectedPermissions = new ArrayList<>();
      Permission ownerPermission =
          Permission.newBuilder()
              .setName("owner")
              .setThreshold(1)
              .addKeys(
                  Key.newBuilder()
                      .setAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
                      .setWeight(keyWeight)
                      .build())
              .build();
      Permission activePermission =
          Permission.newBuilder()
              .setName("active")
              .setThreshold(1)
              .setParent("owner")
              .addKeys(
                  Key.newBuilder()
                      .setAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
                      .setWeight(1)
                      .build())
              .build();
      expectedPermissions.add(ownerPermission);
      expectedPermissions.add(activePermission);

      List<Permission> ownerPermissions = owner.getPermissions();

      Assert.assertEquals(expectedPermissions, ownerPermissions);
      Assert.assertTrue(true);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void nullContract() {
    PermissionAddKeyActuator actuator = new PermissionAddKeyActuator(null, dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(actuator, ret, "No contract!", "No contract!");
  }

  @Test
  public void nullDbManager() {
    PermissionAddKeyActuator actuator = new PermissionAddKeyActuator(getContract(), null);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(actuator, ret, "No dbManager!", "No dbManager!");
  }

  @Test
  public void invalidContract() {
    Any invalidContract = getInvalidContract();
    PermissionAddKeyActuator actuator = new PermissionAddKeyActuator(invalidContract, dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(
        actuator,
        ret,
        "contract type error",
        "contract type error,expected type [PermissionAddKeyContract],real type["
            + invalidContract.getClass()
            + "]");
  }

  @Test
  public void invalidOwnerAddress() {
    PermissionAddKeyActuator actuator =
        new PermissionAddKeyActuator(
            getContract(OWNER_ADDRESS_INVALID, VALID_KEY, "owner"), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(actuator, ret, "invalidate ownerAddress", "invalidate ownerAddress");
  }

  @Test
  public void nullAccount() {
    PermissionAddKeyActuator actuator =
        new PermissionAddKeyActuator(
            getContract(OWNER_ADDRESS_NOACCOUNT, VALID_KEY, "owner"), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(
        actuator,
        ret,
        "ownerAddress account does not exist",
        "ownerAddress account does not exist");
  }

  @Test
  public void invalidPermissionName() {
    PermissionAddKeyActuator actuator =
        new PermissionAddKeyActuator(
            getContract(OWNER_ADDRESS, KEY_ADDRESS, KEY_WEIGHT, PERMISSION_NAME_INVALID),
            dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(
        actuator,
        ret,
        "permission name should be owner or active",
        "permission name should be owner or active");
  }

  @Test
  public void invalidKeyAddress() {
    PermissionAddKeyActuator actuator =
        new PermissionAddKeyActuator(
            getContract(OWNER_ADDRESS, KEY_ADDRESS_INVALID, KEY_WEIGHT, PERMISSION_NAME),
            dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(
        actuator, ret, "address in key is invalidate", "address in key is invalidate");
  }

  @Test
  public void keyAddressExists() {
    // init
    byte[] owner_name_array = ByteArray.fromHexString(OWNER_ADDRESS);
    AccountCapsule account = dbManager.getAccountStore().get(owner_name_array);
    account.permissionAddKey(VALID_KEY, PERMISSION_NAME);
    dbManager.getAccountStore().put(owner_name_array, account);

    // check
    PermissionAddKeyActuator actuator = new PermissionAddKeyActuator(getContract(), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(
        actuator,
        ret,
        "address already in permission",
        "address "
            + Wallet.encode58Check(ByteArray.fromHexString(KEY_ADDRESS))
            + " is already in permission "
            + PERMISSION_NAME);
  }

  @Test
  public void invalidWeightValue() {
    PermissionAddKeyActuator actuator =
        new PermissionAddKeyActuator(
            getContract(OWNER_ADDRESS, KEY_ADDRESS, KEY_WEIGHT_INVALID, PERMISSION_NAME),
            dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(
        actuator,
        ret,
        "key weight should be greater than 0",
        "key weight should be greater than 0");
  }
}
