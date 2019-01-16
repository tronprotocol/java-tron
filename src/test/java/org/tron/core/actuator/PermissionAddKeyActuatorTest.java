package org.tron.core.actuator;

import static org.testng.Assert.fail;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.io.File;
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
import org.tron.protos.Protocol.Permission.PermissionType;
import org.tron.protos.Protocol.Transaction.Result.code;

@Slf4j
public class PermissionAddKeyActuatorTest {

  private static Manager dbManager;
  private static final String dbPath = "output_transfer_test";
  private static TronApplicationContext context;
  private static Application AppT;
  private static final String OWNER_ADDRESS;
  private static final String WITNESS_ADDRESS;
  private static final String KEY_ADDRESS;
  private static final String KEY_ADDRESS1;
  private static final String KEY_ADDRESS2;
  private static final String KEY_ADDRESS3;
  private static final String KEY_ADDRESS4;
  private static final String KEY_ADDRESS5;
  private static final Key VALID_KEY;
  private static final Key VALID_KEY1;
  private static final Key VALID_KEY2;
  private static final Key VALID_KEY3;
  private static final Key VALID_KEY4;
  private static final Key VALID_KEY5;
  private static final long KEY_WEIGHT = 1;
  private static final int OWNER_PERMISSION_ID = 0;
  private static final int ACTIVE_PERMISSION_START_ID = 2;
  private static final String SECOND_KEY_ADDRESS;

  private static final String OWNER_ADDRESS_INVALID = "aaaa";
  private static final String OWNER_ADDRESS_NOACCOUNT;
  private static final int PERMISSION_ID_INVALID = 10;
  private static final String KEY_ADDRESS_INVALID = "bbbb";
  private static final long KEY_WEIGHT_INVALID = -1;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    AppT = ApplicationFactory.create(context);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
    WITNESS_ADDRESS = Wallet.getAddressPreFixString() + "8CFC572CC20CA18B636BDD93B4FB15EA84CC2B4E";
    KEY_ADDRESS = Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
    KEY_ADDRESS1 = Wallet.getAddressPreFixString() + "BCE23C7D683B889326F762DDA2223A861EDA2E5C";
    KEY_ADDRESS2 = Wallet.getAddressPreFixString() + "B207296C464175C5124AD6DEBCE3E9EB3720D9EA";
    KEY_ADDRESS3 = Wallet.getAddressPreFixString() + "5FFAA69423DC87903948E788E0D5A7BE9BE58989";
    KEY_ADDRESS4 = Wallet.getAddressPreFixString() + "A727FD9B876A1040B14A7963AFDA8490ED2A2F00";
    KEY_ADDRESS5 = Wallet.getAddressPreFixString() + "474921F5AD0ACE57D8AFD7E878F38DB7C3977361";

    VALID_KEY =
        Key.newBuilder()
            .setAddress(ByteString.copyFrom(ByteArray.fromHexString(KEY_ADDRESS)))
            .setWeight(KEY_WEIGHT)
            .build();
    VALID_KEY1 =
        Key.newBuilder()
            .setAddress(ByteString.copyFrom(ByteArray.fromHexString(KEY_ADDRESS1)))
            .setWeight(KEY_WEIGHT)
            .build();
    VALID_KEY2 =
        Key.newBuilder()
            .setAddress(ByteString.copyFrom(ByteArray.fromHexString(KEY_ADDRESS2)))
            .setWeight(KEY_WEIGHT)
            .build();
    VALID_KEY3 =
        Key.newBuilder()
            .setAddress(ByteString.copyFrom(ByteArray.fromHexString(KEY_ADDRESS3)))
            .setWeight(KEY_WEIGHT)
            .build();
    VALID_KEY4 =
        Key.newBuilder()
            .setAddress(ByteString.copyFrom(ByteArray.fromHexString(KEY_ADDRESS4)))
            .setWeight(KEY_WEIGHT)
            .build();
    VALID_KEY5 =
        Key.newBuilder()
            .setAddress(ByteString.copyFrom(ByteArray.fromHexString(KEY_ADDRESS5)))
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

    AccountCapsule witnessCapsule =
        new AccountCapsule(
            ByteString.copyFrom(ByteArray.fromHexString(WITNESS_ADDRESS)),
            ByteString.copyFromUtf8("witness"),
            AccountType.Normal);
    witnessCapsule.setIsWitness(true);
    dbManager.getAccountStore().put(witnessCapsule.getAddress().toByteArray(), witnessCapsule);
  }

  private Any getContract() {
    return Any.pack(
        Contract.PermissionAddKeyContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setKey(VALID_KEY)
            .setPermissionId(0)
            .build());
  }

  private Any getContract(String address, int permissionId) {
    return Any.pack(
        Contract.PermissionAddKeyContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(address)))
            .setKey(VALID_KEY)
            .setPermissionId(permissionId)
            .build());
  }

  private Any getInvalidContract() {
    return Any.pack(
        Contract.PermissionDeleteKeyContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setKeyAddress(ByteString.copyFrom(ByteArray.fromHexString(KEY_ADDRESS)))
            .setPermissionId(OWNER_PERMISSION_ID)
            .build());
  }

  private Any getContract(String ownerAddress, Key key, int permissionId) {
    return Any.pack(
        Contract.PermissionAddKeyContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
            .setKey(key)
            .setPermissionId(permissionId)
            .build());
  }

  private Any getContract(
      String ownerAddress, String keyAddress, long keyWeight, int permissionId) {
    Key key =
        Key.newBuilder()
            .setAddress(ByteString.copyFrom(ByteArray.fromHexString(keyAddress)))
            .setWeight(keyWeight)
            .build();

    return Any.pack(
        Contract.PermissionAddKeyContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
            .setKey(key)
            .setPermissionId(permissionId)
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
  public void successFistAddOwnerAddressPermissionKey() {
    PermissionAddKeyActuator actuator = new PermissionAddKeyActuator(getContract(), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    byte[] owner_address_array = ByteArray.fromHexString(OWNER_ADDRESS);
    ByteString address = ByteString.copyFrom(owner_address_array);

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);

      AccountCapsule owner = dbManager.getAccountStore().get(owner_address_array);

      Permission ownerPermission =
          Permission.newBuilder()
              .setId(OWNER_PERMISSION_ID)
              .setThreshold(1)
              .setParentId(0)
              .setPermissionName("owner")
              .addKeys(
                  Key.newBuilder()
                      .setAddress(address)
                      .setWeight(1)
                      .build())
              .addKeys(
                  Key.newBuilder()
                      .setAddress(ByteString.copyFrom(ByteArray.fromHexString(KEY_ADDRESS)))
                      .setWeight(KEY_WEIGHT)
                      .build())
              .build();

      Permission activePermission =
          Permission.newBuilder()
              .setType(PermissionType.Active)
              .setId(ACTIVE_PERMISSION_START_ID)
              .setPermissionName("active")
              .setThreshold(1)
              .setParentId(OWNER_PERMISSION_ID)
              .setOperations(AccountCapsule.createDefaultActivePermission(address).getOperations())
              .addKeys(
                  Key.newBuilder()
                      .setAddress(address)
                      .setWeight(1)
                      .build())
              .build();

      Assert.assertEquals(owner.getInstance().getActivePermissionCount(), 1);
      Permission ownerPermission1 = owner.getInstance().getOwnerPermission();
      Permission witnessPermission1 = owner.getInstance().getWitnessPermission();
      Permission activePermission1 = owner.getInstance().getActivePermission(0);

      Assert.assertEquals(ownerPermission, ownerPermission1);
      Assert.assertEquals(activePermission, activePermission1);
      Assert.assertEquals(witnessPermission1, Permission.getDefaultInstance());
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

    byte[] owner_address_array = ByteArray.fromHexString(OWNER_ADDRESS);
    PermissionAddKeyActuator actuator =
        new PermissionAddKeyActuator(
            getContract(OWNER_ADDRESS, SECOND_KEY_ADDRESS, keyWeight, OWNER_PERMISSION_ID),
            dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);

      AccountCapsule owner = dbManager.getAccountStore().get(owner_address_array);

      Permission ownerPermission =
          Permission.newBuilder()
              .setType(PermissionType.Owner)
              .setId(OWNER_PERMISSION_ID)
              .setPermissionName("owner")
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
              .setType(PermissionType.Active)
              .setId(ACTIVE_PERMISSION_START_ID)
              .setPermissionName("active")
              .setThreshold(1)
              .setParentId(OWNER_PERMISSION_ID)
              .setOperations(AccountCapsule.createDefaultActivePermission(owner.getAddress())
                  .getOperations())
              .addKeys(
                  Key.newBuilder()
                      .setAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
                      .setWeight(1)
                      .build())
              .build();

      Assert.assertEquals(owner.getInstance().getActivePermissionCount(), 1);
      Permission ownerPermission1 = owner.getInstance().getOwnerPermission();
      Permission witnessPermission1 = owner.getInstance().getWitnessPermission();
      Permission activePermission1 = owner.getInstance().getActivePermission(0);

      Assert.assertEquals(ownerPermission, ownerPermission1);
      Assert.assertEquals(activePermission, activePermission1);
      Assert.assertEquals(witnessPermission1, Permission.getDefaultInstance());
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void successAddOwnerSelfKey() {
    long keyWeight = 3;
    PermissionAddKeyActuator actuator =
        new PermissionAddKeyActuator(
            getContract(OWNER_ADDRESS, OWNER_ADDRESS, keyWeight, OWNER_PERMISSION_ID), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);

      AccountCapsule owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));

      Permission ownerPermission =
          Permission.newBuilder()
              .setType(PermissionType.Owner)
              .setId(OWNER_PERMISSION_ID)
              .setPermissionName("owner")
              .setThreshold(1)
              .addKeys(
                  Key.newBuilder()
                      .setAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
                      .setWeight(keyWeight)
                      .build())
              .build();
      Permission activePermission =
          Permission.newBuilder()
              .setType(PermissionType.Active)
              .setId(ACTIVE_PERMISSION_START_ID)
              .setPermissionName("active")
              .setThreshold(1)
              .setParentId(OWNER_PERMISSION_ID)
              .setOperations(AccountCapsule.createDefaultActivePermission(owner.getAddress())
                  .getOperations())
              .addKeys(
                  Key.newBuilder()
                      .setAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
                      .setWeight(1)
                      .build())
              .build();

      Assert.assertEquals(owner.getInstance().getActivePermissionCount(), 1);
      Permission ownerPermission1 = owner.getInstance().getOwnerPermission();
      Permission activePermission1 = owner.getInstance().getActivePermission(0);

      Assert.assertEquals(ownerPermission, ownerPermission1);
      Assert.assertEquals(activePermission, activePermission1);
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
            getContract(OWNER_ADDRESS_INVALID, VALID_KEY, OWNER_PERMISSION_ID), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(actuator, ret, "invalidate ownerAddress", "invalidate ownerAddress");
  }

  @Test
  public void nullAccount() {
    PermissionAddKeyActuator actuator =
        new PermissionAddKeyActuator(
            getContract(OWNER_ADDRESS_NOACCOUNT, VALID_KEY, OWNER_PERMISSION_ID), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(
        actuator,
        ret,
        "ownerAddress account does not exist",
        "ownerAddress account does not exist");
  }

  @Test
  public void invalidPermissionId() {
    PermissionAddKeyActuator actuator =
        new PermissionAddKeyActuator(
            getContract(OWNER_ADDRESS, KEY_ADDRESS, KEY_WEIGHT, PERMISSION_ID_INVALID),
            dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(
        actuator,
        ret,
        "Permission isn't exist",
        "Permission isn't exist");
  }

  @Test
  public void invalidKeyAddress() {
    PermissionAddKeyActuator actuator =
        new PermissionAddKeyActuator(
            getContract(OWNER_ADDRESS, KEY_ADDRESS_INVALID, KEY_WEIGHT, OWNER_PERMISSION_ID),
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
    account.permissionAddKey(VALID_KEY, OWNER_PERMISSION_ID);
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
            + OWNER_PERMISSION_ID);
  }

  @Test
  public void invalidWeightValue() {
    PermissionAddKeyActuator actuator =
        new PermissionAddKeyActuator(
            getContract(OWNER_ADDRESS, KEY_ADDRESS, KEY_WEIGHT_INVALID, OWNER_PERMISSION_ID),
            dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(
        actuator,
        ret,
        "key weight should be greater than 0",
        "key weight should be greater than 0");
  }

  @Test
  public void tooManykey() {
    // init
    byte[] owner_name_array = ByteArray.fromHexString(OWNER_ADDRESS);
    AccountCapsule account = dbManager.getAccountStore().get(owner_name_array);
    account.permissionAddKey(VALID_KEY1, OWNER_PERMISSION_ID);
    account.permissionAddKey(VALID_KEY2, OWNER_PERMISSION_ID);
    account.permissionAddKey(VALID_KEY3, OWNER_PERMISSION_ID);
    account.permissionAddKey(VALID_KEY4, OWNER_PERMISSION_ID);
    dbManager.getAccountStore().put(owner_name_array, account);

    // check
    PermissionAddKeyActuator actuator = new PermissionAddKeyActuator(getContract(), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(
        actuator,
        ret,
        "number of keys in permission should not be greater than 5",
        "number of keys in permission should not be greater than 5");
  }

  @Test
  public void cannotAddKeyToWitnessPermission() {
    // init
    PermissionAddKeyActuator actuator = new PermissionAddKeyActuator(
        getContract(WITNESS_ADDRESS, OWNER_PERMISSION_ID), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    byte[] witness_address = ByteArray.fromHexString(WITNESS_ADDRESS);
    ByteString address = ByteString.copyFrom(witness_address);

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);

      AccountCapsule owner = dbManager.getAccountStore().get(witness_address);

      Permission ownerPermission =
          Permission.newBuilder()
              .setId(OWNER_PERMISSION_ID)
              .setThreshold(1)
              .setParentId(0)
              .setPermissionName("owner")
              .addKeys(
                  Key.newBuilder()
                      .setAddress(address)
                      .setWeight(1)
                      .build())
              .addKeys(
                  Key.newBuilder()
                      .setAddress(ByteString.copyFrom(ByteArray.fromHexString(KEY_ADDRESS)))
                      .setWeight(KEY_WEIGHT)
                      .build())
              .build();

      Permission witnessPermission =
          Permission.newBuilder()
              .setType(PermissionType.Witness)
              .setId(1)
              .setPermissionName("witness")
              .setThreshold(1)
              .setParentId(OWNER_PERMISSION_ID)
              .addKeys(
                  Key.newBuilder()
                      .setAddress(address)
                      .setWeight(1)
                      .build())
              .build();

      Permission activePermission =
          Permission.newBuilder()
              .setType(PermissionType.Active)
              .setId(ACTIVE_PERMISSION_START_ID)
              .setPermissionName("active")
              .setThreshold(1)
              .setParentId(OWNER_PERMISSION_ID)
              .setOperations(AccountCapsule.createDefaultActivePermission(address).getOperations())
              .addKeys(
                  Key.newBuilder()
                      .setAddress(address)
                      .setWeight(1)
                      .build())
              .build();

      Assert.assertEquals(owner.getInstance().getActivePermissionCount(), 1);
      Permission ownerPermission1 = owner.getInstance().getOwnerPermission();
      Permission witnessPermission1 = owner.getInstance().getWitnessPermission();
      Permission activePermission1 = owner.getInstance().getActivePermission(0);

      Assert.assertEquals(ownerPermission, ownerPermission1);
      Assert.assertEquals(witnessPermission, witnessPermission1);
      Assert.assertEquals(activePermission, activePermission1);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

    // check
    actuator = new PermissionAddKeyActuator(getContract(WITNESS_ADDRESS, 1), dbManager);
    ret = new TransactionResultCapsule();

    processAndCheckInvalid(
        actuator,
        ret,
        "Witness permission can't add key",
        "Witness permission can't add key");
  }


  @Test
  public void witnessAddOwnerAddressPermissionKey() {
    PermissionAddKeyActuator actuator = new PermissionAddKeyActuator(
        getContract(WITNESS_ADDRESS, OWNER_PERMISSION_ID), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    byte[] witness_address = ByteArray.fromHexString(WITNESS_ADDRESS);
    ByteString address = ByteString.copyFrom(witness_address);

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);

      AccountCapsule owner = dbManager.getAccountStore().get(witness_address);

      Permission ownerPermission =
          Permission.newBuilder()
              .setId(OWNER_PERMISSION_ID)
              .setThreshold(1)
              .setParentId(0)
              .setPermissionName("owner")
              .addKeys(
                  Key.newBuilder()
                      .setAddress(address)
                      .setWeight(1)
                      .build())
              .addKeys(
                  Key.newBuilder()
                      .setAddress(ByteString.copyFrom(ByteArray.fromHexString(KEY_ADDRESS)))
                      .setWeight(KEY_WEIGHT)
                      .build())
              .build();

      Permission witnessPermission =
          Permission.newBuilder()
              .setType(PermissionType.Witness)
              .setId(1)
              .setPermissionName("witness")
              .setThreshold(1)
              .setParentId(OWNER_PERMISSION_ID)
              .addKeys(
                  Key.newBuilder()
                      .setAddress(address)
                      .setWeight(1)
                      .build())
              .build();

      Permission activePermission =
          Permission.newBuilder()
              .setType(PermissionType.Active)
              .setId(ACTIVE_PERMISSION_START_ID)
              .setPermissionName("active")
              .setThreshold(1)
              .setParentId(OWNER_PERMISSION_ID)
              .setOperations(AccountCapsule.createDefaultActivePermission(address).getOperations())
              .addKeys(
                  Key.newBuilder()
                      .setAddress(address)
                      .setWeight(1)
                      .build())
              .build();

      Assert.assertEquals(owner.getInstance().getActivePermissionCount(), 1);
      Permission ownerPermission1 = owner.getInstance().getOwnerPermission();
      Permission witnessPermission1 = owner.getInstance().getWitnessPermission();
      Permission activePermission1 = owner.getInstance().getActivePermission(0);

      Assert.assertEquals(ownerPermission, ownerPermission1);
      Assert.assertEquals(witnessPermission, witnessPermission1);
      Assert.assertEquals(activePermission, activePermission1);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }
}
