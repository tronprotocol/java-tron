package org.tron.core.actuator;

import static org.testng.Assert.fail;

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
public class PermissionUpdateKeyActuatorTest {

  private static Manager dbManager;
  private static final String dbPath = "output_transfer_test";
  private static TronApplicationContext context;
  private static Application AppT;

  private static final String OWNER_ADDRESS;
  private static final String WITNESS_ADDRESS;
  private static final String KEY_ADDRESS;
  private static final Key VALID_KEY;
  private static final Key UPDATE_KEY;
  private static final long KEY_WEIGHT = 2;
  private static final long UPDATE_WEIGHT = 5;
  private static final int OWNER_PERMISSION_ID = 0;
  private static final int ACTIVE_PERMISSION_START_ID = 2;
  private static final String SECOND_KEY_ADDRESS;

  private static final String OWNER_ADDRESS_INVALID = "aaaa";
  private static final String OWNER_ADDRESS_NOACCOUNT;
  private static final int PERMISSION_ID_INVALID = 10;
  private static final String KEY_ADDRESS_INVALID = "bbbb";

  static {
    Args.setParam(new String[]{"--output-directory", dbPath}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    AppT = ApplicationFactory.create(context);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
    WITNESS_ADDRESS = Wallet.getAddressPreFixString() + "8CFC572CC20CA18B636BDD93B4FB15EA84CC2B4E";
    KEY_ADDRESS = Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";

    SECOND_KEY_ADDRESS =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a3456";
    OWNER_ADDRESS_NOACCOUNT =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1aed";

    VALID_KEY =
        Key.newBuilder()
            .setAddress(ByteString.copyFrom(ByteArray.fromHexString(KEY_ADDRESS)))
            .setWeight(KEY_WEIGHT)
            .build();

    UPDATE_KEY =
        Key.newBuilder()
            .setAddress(ByteString.copyFrom(ByteArray.fromHexString(KEY_ADDRESS)))
            .setWeight(UPDATE_WEIGHT)
            .build();
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

  private Any getContract(String ownerAddress, Key Key, int permissionId) {
    return Any.pack(
        Contract.PermissionUpdateKeyContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
            .setKey(Key)
            .setPermissionId(permissionId)
            .build());
  }

  private Any getContract(
      String ownerAddress, String keyAddress, long KeyWeight, int permissionId) {
    return Any.pack(
        Contract.PermissionUpdateKeyContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
            .setKey(
                Key.newBuilder()
                    .setAddress(ByteString.copyFrom(ByteArray.fromHexString(keyAddress)))
                    .setWeight(KeyWeight)
                    .build())
            .setPermissionId(permissionId)
            .build());
  }

  /**
   * return a PermissionAddKeyContract as an invalid contract
   */
  private Any getInvalidContract() {
    return Any.pack(
        Contract.PermissionAddKeyContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setKey(VALID_KEY)
            .setPermissionId(OWNER_PERMISSION_ID)
            .build());
  }

  private Any getUnpackContract() {
    return Any.pack(
        Contract.PermissionUpdateKeyContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setKey(
                Key.newBuilder()
                    .setAddress(ByteString.copyFrom(ByteArray.fromHexString(KEY_ADDRESS)))
                    .build())
            .setPermissionId(OWNER_PERMISSION_ID)
            .build());
  }

  private void addValidPermissionKey() {
    byte[] owner_name_array = ByteArray.fromHexString(OWNER_ADDRESS);
    AccountCapsule account = dbManager.getAccountStore().get(owner_name_array);
    account.permissionAddKey(VALID_KEY, OWNER_PERMISSION_ID);
    dbManager.getAccountStore().put(owner_name_array, account);
  }

  private void addDefaultPermissoin() {
    byte[] owner_name_array = ByteArray.fromHexString(OWNER_ADDRESS);
    AccountCapsule account = dbManager.getAccountStore().get(owner_name_array);

    Permission ownerPermission = AccountCapsule.createDefaultOwnerPermission(account.getAddress());
    Permission activePermission = AccountCapsule
        .createDefaultActivePermission(account.getAddress());
    List<Permission> arrayList = new ArrayList<>();
    arrayList.add(activePermission);

    account.updatePermissions(ownerPermission, null, arrayList);

    dbManager.getAccountStore().put(owner_name_array, account);
  }

  private void processAndCheckInvalid(
      PermissionUpdateKeyActuator actuator,
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
  public void successUpdatePermissionKey() {
    String ownerAddress = OWNER_ADDRESS;
    String keyAddress = KEY_ADDRESS;

    // step 1, init
    addValidPermissionKey();

    // step2, check init data
    byte[] owner_name_array = ByteArray.fromHexString(ownerAddress);
    AccountCapsule owner = dbManager.getAccountStore().get(owner_name_array);
    Permission ownerPermission = Permission.newBuilder()
        .setType(PermissionType.Owner)
        .setId(OWNER_PERMISSION_ID)
        .setPermissionName("owner")
        .setThreshold(1)
        .setParentId(OWNER_PERMISSION_ID)
        .addKeys(
            Key.newBuilder()
                .setAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
                .setWeight(1)
                .build())
        .addKeys(
            Key.newBuilder()
                .setAddress(ByteString.copyFrom(ByteArray.fromHexString(keyAddress)))
                .setWeight(KEY_WEIGHT)
                .build())
        .build();
    Permission activePermission = AccountCapsule.createDefaultActivePermission(owner.getAddress());

    Assert.assertEquals(owner.getInstance().getActivePermissionCount(), 1);
    Permission ownerPermission1 = owner.getInstance().getOwnerPermission();
    Permission activePermission1 = owner.getInstance().getActivePermission(0);

    Assert.assertEquals(ownerPermission1, ownerPermission);
    Assert.assertEquals(activePermission1, activePermission);

    // step 3, execute update owner permission
    PermissionUpdateKeyActuator actuator =
        new PermissionUpdateKeyActuator(
            getContract(ownerAddress, UPDATE_KEY, OWNER_PERMISSION_ID), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);

      // step 4, check result after update operation
      owner = dbManager.getAccountStore().get(owner_name_array);
      Permission ownerPermission2 = owner.getInstance().getOwnerPermission();
      Permission activePermission2 = owner.getInstance().getActivePermission(0);

      ownerPermission = Permission.newBuilder()
          .setType(PermissionType.Owner)
          .setId(OWNER_PERMISSION_ID)
          .setPermissionName("owner")
          .setThreshold(1)
          .setParentId(OWNER_PERMISSION_ID)
          .addKeys(
              Key.newBuilder()
                  .setAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
                  .setWeight(1)
                  .build())
          .addKeys(
              Key.newBuilder()
                  .setAddress(ByteString.copyFrom(ByteArray.fromHexString(keyAddress)))
                  .setWeight(UPDATE_WEIGHT)
                  .build())
          .build();

      Assert.assertEquals(ownerPermission2, ownerPermission);
      Assert.assertEquals(activePermission2, activePermission);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

    // step 4, execute update active permission
    Key updateWitness = Key.newBuilder().setAddress(owner.getAddress()).setWeight(UPDATE_WEIGHT)
        .build();
    actuator =
        new PermissionUpdateKeyActuator(
            getContract(ownerAddress, updateWitness, ACTIVE_PERMISSION_START_ID), dbManager);
    ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);

      // step 4, check result after update operation
      owner = dbManager.getAccountStore().get(owner_name_array);
      Permission ownerPermission2 = owner.getInstance().getOwnerPermission();
      Permission activePermission2 = owner.getInstance().getActivePermission(0);

      ownerPermission = Permission.newBuilder()
          .setType(PermissionType.Owner)
          .setId(OWNER_PERMISSION_ID)
          .setPermissionName("owner")
          .setThreshold(1)
          .setParentId(OWNER_PERMISSION_ID)
          .addKeys(
              Key.newBuilder()
                  .setAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
                  .setWeight(1)
                  .build())
          .addKeys(
              Key.newBuilder()
                  .setAddress(ByteString.copyFrom(ByteArray.fromHexString(keyAddress)))
                  .setWeight(UPDATE_WEIGHT)
                  .build())
          .build();

      activePermission = Permission.newBuilder()
          .setType(PermissionType.Active)
          .setId(ACTIVE_PERMISSION_START_ID)
          .setPermissionName("active")
          .setThreshold(1)
          .setParentId(OWNER_PERMISSION_ID)
          .setOperations(
              AccountCapsule.createDefaultActivePermission(owner.getAddress()).getOperations())
          .addKeys(
              Key.newBuilder()
                  .setAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
                  .setWeight(UPDATE_WEIGHT)
                  .build())
          .build();
      Assert.assertEquals(ownerPermission2, ownerPermission);
      Assert.assertEquals(activePermission2, activePermission);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void nullContract() {
    PermissionUpdateKeyActuator actuator = new PermissionUpdateKeyActuator(null, dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(actuator, ret, "No contract!", "No contract!");
  }

  @Test
  public void nullDbManager() {
    PermissionUpdateKeyActuator actuator =
        new PermissionUpdateKeyActuator(getContract(OWNER_ADDRESS, VALID_KEY, OWNER_PERMISSION_ID),
            null);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(actuator, ret, "No dbManager!", "No dbManager!");
  }

  @Test
  public void invalidContract() {
    Any invalidContract = getInvalidContract();
    PermissionUpdateKeyActuator actuator =
        new PermissionUpdateKeyActuator(invalidContract, dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(
        actuator,
        ret,
        "contract type error",
        "contract type error,expected type [PermissionUpdateKeyContract],real type["
            + invalidContract.getClass()
            + "]");
  }

  @Test
  public void invalidOwnerAddress() {
    PermissionUpdateKeyActuator actuator =
        new PermissionUpdateKeyActuator(
            getContract(OWNER_ADDRESS_INVALID, VALID_KEY, OWNER_PERMISSION_ID), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(actuator, ret, "invalidate ownerAddress", "invalidate ownerAddress");
  }

  @Test
  public void nullAccount() {
    PermissionUpdateKeyActuator actuator =
        new PermissionUpdateKeyActuator(
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
    PermissionUpdateKeyActuator actuator =
        new PermissionUpdateKeyActuator(
            getContract(OWNER_ADDRESS, VALID_KEY, PERMISSION_ID_INVALID), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(
        actuator,
        ret,
        "you have not set permission with the id " + PERMISSION_ID_INVALID,
        "you have not set permission with the id " + PERMISSION_ID_INVALID);
  }

  @Test
  public void cannotUpdateWitnessPermission() {
    byte[] witness_address_array = ByteArray.fromHexString(WITNESS_ADDRESS);
    AccountCapsule witness = dbManager.getAccountStore().get(witness_address_array);

    // step 1, init
    Permission ownerPermission = AccountCapsule.createDefaultOwnerPermission(witness.getAddress());
    Permission activePermission = AccountCapsule
        .createDefaultActivePermission(witness.getAddress());
    Permission witnessPermission = AccountCapsule
        .createDefaultWitnessPermission(witness.getAddress());
    List<Permission> activeList = new ArrayList<>();
    activeList.add(activePermission);

    witness.updatePermissions(ownerPermission, witnessPermission, activeList);
    dbManager.getAccountStore().put(witness_address_array, witness);

    PermissionUpdateKeyActuator actuator =
        new PermissionUpdateKeyActuator(
            getContract(WITNESS_ADDRESS, VALID_KEY, 1), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(
        actuator,
        ret,
        "Witness permission can't update key",
        "Witness permission can't update key");
  }

  @Test
  public void nullPermissionWithName() {
    String ownerAddress = OWNER_ADDRESS;
    String permissionName = "active";

    byte[] owner_name_array = ByteArray.fromHexString(ownerAddress);
    AccountCapsule owner = dbManager.getAccountStore().get(owner_name_array);
    Permission ownerPermission = AccountCapsule
        .createDefaultOwnerPermission(ByteString.copyFrom(owner_name_array));
    Permission activePermission = AccountCapsule
        .createDefaultActivePermission(ByteString.copyFrom(owner_name_array));
    List<Permission> activeList = new ArrayList<>();
    activeList.add(activePermission);
    owner.updatePermissions(ownerPermission, null, activeList);
    dbManager.getAccountStore().put(owner_name_array, owner);

    // check
    PermissionUpdateKeyActuator actuator =
        new PermissionUpdateKeyActuator(
            getContract(ownerAddress, VALID_KEY, PERMISSION_ID_INVALID), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(
        actuator,
        ret,
        "no permission with given id",
        "you have not set permission with the id " + PERMISSION_ID_INVALID);
  }

  @Test
  public void invalidKeyAddress() {
    // init account with permission
    addValidPermissionKey();

    PermissionUpdateKeyActuator actuator =
        new PermissionUpdateKeyActuator(
            getContract(OWNER_ADDRESS, KEY_ADDRESS_INVALID, KEY_WEIGHT, OWNER_PERMISSION_ID),
            dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(
        actuator, ret, "address in key is invalidate", "address in key is invalidate");
  }

  @Test
  public void keyAddressNotInPermission() {
    addValidPermissionKey();

    PermissionUpdateKeyActuator actuator =
        new PermissionUpdateKeyActuator(
            getContract(OWNER_ADDRESS, SECOND_KEY_ADDRESS, KEY_WEIGHT, OWNER_PERMISSION_ID),
            dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(
        actuator,
        ret,
        "address is not in permission",
        "address is not in permission " + OWNER_PERMISSION_ID);
  }

  @Test
  public void weighValueInvalid() {
    addValidPermissionKey();

    PermissionUpdateKeyActuator actuator =
        new PermissionUpdateKeyActuator(
            getContract(OWNER_ADDRESS, KEY_ADDRESS, 0, OWNER_PERMISSION_ID), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(
        actuator,
        ret,
        "key weight should be greater than 0",
        "key weight should be greater than 0");
  }

  @Test
  public void sumWeightLessThanThreshold() {
    String ownerAddress = OWNER_ADDRESS;
    String keyAddress = KEY_ADDRESS;

    byte[] owner_name_array = ByteArray.fromHexString(ownerAddress);
    AccountCapsule owner = dbManager.getAccountStore().get(owner_name_array);
    Permission ownerPermission = AccountCapsule
        .createDefaultOwnerPermission(ByteString.copyFrom(owner_name_array));
    Permission activePermission =
        Permission.newBuilder()
            .setType(PermissionType.Active)
            .setId(ACTIVE_PERMISSION_START_ID)
            .setPermissionName("active")
            .setThreshold(3)
            .setParentId(OWNER_PERMISSION_ID)
            .addKeys(
                Key.newBuilder()
                    .setAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
                    .setWeight(1)
                    .build())
            .addKeys(
                Key.newBuilder()
                    .setAddress(ByteString.copyFrom(ByteArray.fromHexString(keyAddress)))
                    .setWeight(3)
                    .build())
            .build();

    List<Permission> activeList = new ArrayList<>();
    activeList.add(activePermission);
    owner.updatePermissions(ownerPermission, null, activeList);
    dbManager.getAccountStore().put(owner_name_array, owner);

    PermissionUpdateKeyActuator actuator =
        new PermissionUpdateKeyActuator(
            getContract(ownerAddress, keyAddress, 1, ACTIVE_PERMISSION_START_ID), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(
        actuator,
        ret,
        "sum of all keys weight should not be less that threshold",
        "sum of all keys weight should not be less that threshold");
  }
}
