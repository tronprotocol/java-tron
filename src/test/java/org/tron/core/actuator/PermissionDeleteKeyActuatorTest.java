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
public class PermissionDeleteKeyActuatorTest {

  private static Manager dbManager;
  private static final String dbPath = "output_transfer_test";
  private static TronApplicationContext context;
  private static Application AppT;

  private static final String OWNER_ADDRESS;
  private static final String WITNESS_ADDRESS;
  private static final String KEY_ADDRESS;
  private static final Key VALID_KEY;
  private static final long KEY_WEIGHT = 1;
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

  private Any getContract(String ownerAddress, String keyAddress, int permissionId) {
    return Any.pack(
        Contract.PermissionDeleteKeyContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
            .setKeyAddress(ByteString.copyFrom(ByteArray.fromHexString(keyAddress)))
            .setPermissionId(permissionId)
            .build());
  }

  private Any getInvalidContract() {
    return Any.pack(
        Contract.PermissionAddKeyContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setKey(VALID_KEY)
            .setPermissionId(OWNER_PERMISSION_ID)
            .build());
  }

  private void addValidPermissionKey() {
    byte[] owner_name_array = ByteArray.fromHexString(OWNER_ADDRESS);
    AccountCapsule account = dbManager.getAccountStore().get(owner_name_array);
    account.permissionAddKey(VALID_KEY, OWNER_PERMISSION_ID);
    dbManager.getAccountStore().put(owner_name_array, account);
  }

  private void processAndCheckInvalid(
      PermissionDeleteKeyActuator actuator,
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
  public void successDeleteOwnerPermissionKey() {
    String ownerAddress = OWNER_ADDRESS;
    String keyAddress = KEY_ADDRESS;

    // step 1, init
    addValidPermissionKey();

    // step2, check init data
    byte[] owner_address_array = ByteArray.fromHexString(ownerAddress);
    AccountCapsule owner = dbManager.getAccountStore().get(owner_address_array);
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
                .setWeight(1)
                .build())
        .build();
    Permission activePermission = AccountCapsule
        .createDefaultActivePermission(ByteString.copyFrom(owner_address_array));

    Assert.assertEquals(owner.getInstance().getActivePermissionCount(), 1);
    Permission ownerPermission1 = owner.getInstance().getOwnerPermission();
    Permission activePermission1 = owner.getInstance().getActivePermission(0);

    Assert.assertEquals(ownerPermission1, ownerPermission);
    Assert.assertEquals(activePermission1, activePermission);

    // step 3, execute delete
    PermissionDeleteKeyActuator actuator =
        new PermissionDeleteKeyActuator(getContract(ownerAddress, keyAddress, OWNER_PERMISSION_ID),
            dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);

      // step 4, check result after delete operation
      owner = dbManager.getAccountStore().get(owner_address_array);

      Assert.assertEquals(owner.getInstance().getActivePermissionCount(), 1);
      Permission ownerPermission2 = owner.getInstance().getOwnerPermission();
      Permission activePermission2 = owner.getInstance().getActivePermission(0);

      ownerPermission = owner.getInstance().getOwnerPermission();

      Assert.assertEquals(ownerPermission2, ownerPermission);
      Assert.assertEquals(activePermission2, activePermission);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void successDeleteActivPermissionKey() {
    String ownerAddress = OWNER_ADDRESS;
    String keyAddress = KEY_ADDRESS;

    // step 1, init
    byte[] owner_address_array = ByteArray.fromHexString(ownerAddress);
    AccountCapsule owner = dbManager.getAccountStore().get(owner_address_array);
    Permission ownerPermission = AccountCapsule
        .createDefaultOwnerPermission(ByteString.copyFrom(owner_address_array));

    Permission activePermission = Permission.newBuilder()
        .setType(PermissionType.Active)
        .setId(ACTIVE_PERMISSION_START_ID)
        .setPermissionName("active")
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
                .setWeight(1)
                .build())
        .build();

    List<Permission> activeList = new ArrayList<>();
    activeList.add(activePermission);

    owner.updatePermissions(ownerPermission, null, activeList);
    dbManager.getAccountStore().put(owner_address_array, owner);
    owner = dbManager.getAccountStore().get(owner_address_array);
    // step2, check init data
    Assert.assertEquals(owner.getInstance().getActivePermissionCount(), 1);
    Permission ownerPermission1 = owner.getInstance().getOwnerPermission();
    Permission activePermission1 = owner.getInstance().getActivePermission(0);

    Assert.assertEquals(ownerPermission1, ownerPermission);
    Assert.assertEquals(activePermission1, activePermission);

    // step 3, execute delete
    PermissionDeleteKeyActuator actuator =
        new PermissionDeleteKeyActuator(getContract(ownerAddress, keyAddress, ACTIVE_PERMISSION_START_ID),
            dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);

      // step 4, check result after delete operation
      owner = dbManager.getAccountStore().get(owner_address_array);

      Assert.assertEquals(owner.getInstance().getActivePermissionCount(), 1);
      Permission ownerPermission2 = owner.getInstance().getOwnerPermission();
      Permission activePermission2 = owner.getInstance().getActivePermission(0);

      activePermission = owner.getInstance().getActivePermission(0);

      Assert.assertEquals(ownerPermission2, ownerPermission);
      Assert.assertEquals(activePermission2, activePermission);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void witnessPermissionCannotDeleteKey() {
    String witnessAddress = WITNESS_ADDRESS;
    String keyAddress = KEY_ADDRESS;

    // step 1, init
    byte[] witness_address_array = ByteArray.fromHexString(witnessAddress);
    AccountCapsule witness = dbManager.getAccountStore().get(witness_address_array);
    Permission ownerPermission = AccountCapsule
        .createDefaultOwnerPermission(ByteString.copyFrom(witness_address_array));
    Permission activePermission = AccountCapsule
        .createDefaultActivePermission(ByteString.copyFrom(witness_address_array));
    Permission witnessPermission = AccountCapsule
        .createDefaultWitnessPermission(ByteString.copyFrom(witness_address_array));
    List<Permission> activeList = new ArrayList<>();
    activeList.add(activePermission);

    witness.updatePermissions(ownerPermission, witnessPermission, activeList);
    dbManager.getAccountStore().put(witness_address_array, witness);
    witness = dbManager.getAccountStore().get(witness_address_array);
    // step2, check init data
    Assert.assertEquals(witness.getInstance().getActivePermissionCount(), 1);
    Permission ownerPermission1 = witness.getInstance().getOwnerPermission();
    Permission witnessPermission1 = witness.getInstance().getWitnessPermission();
    Permission activePermission1 = witness.getInstance().getActivePermission(0);

    Assert.assertEquals(ownerPermission1, ownerPermission);
    Assert.assertEquals(activePermission1, activePermission);
    Assert.assertEquals(witnessPermission1, witnessPermission);

    // step 3, execute delete
    PermissionDeleteKeyActuator actuator =
        new PermissionDeleteKeyActuator(getContract(witnessAddress, witnessAddress, 1),
            dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(actuator, ret,
        "Witness permission can't delete key",
        "Witness permission can't delete key");
  }

  @Test
  public void nullContract() {
    PermissionDeleteKeyActuator actuator = new PermissionDeleteKeyActuator(null, dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(actuator, ret, "No contract!", "No contract!");
  }

  @Test
  public void nullDbManager() {
    PermissionDeleteKeyActuator actuator =
        new PermissionDeleteKeyActuator(
            getContract(OWNER_ADDRESS_INVALID, KEY_ADDRESS, OWNER_PERMISSION_ID), null);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(actuator, ret, "No dbManager!", "No dbManager!");
  }

  @Test
  public void invalidContract() {
    Any invalidContract = getInvalidContract();
    PermissionDeleteKeyActuator actuator =
        new PermissionDeleteKeyActuator(invalidContract, dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(
        actuator,
        ret,
        "contract type error",
        "contract type error,expected type [PermissionDeleteKeyContract],real type["
            + invalidContract.getClass()
            + "]");
  }

  @Test
  public void invalidOwnerAddress() {
    PermissionDeleteKeyActuator actuator =
        new PermissionDeleteKeyActuator(
            getContract(OWNER_ADDRESS_INVALID, KEY_ADDRESS, OWNER_PERMISSION_ID), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(actuator, ret, "invalidate ownerAddress", "invalidate ownerAddress");
  }

  @Test
  public void nullAccount() {
    PermissionDeleteKeyActuator actuator =
        new PermissionDeleteKeyActuator(
            getContract(OWNER_ADDRESS_NOACCOUNT, KEY_ADDRESS, OWNER_PERMISSION_ID), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(
        actuator,
        ret,
        "ownerAddress account does not exist",
        "ownerAddress account does not exist");
  }

  @Test
  public void invalidPermissionName() {
    PermissionDeleteKeyActuator actuator =
        new PermissionDeleteKeyActuator(
            getContract(OWNER_ADDRESS, KEY_ADDRESS, PERMISSION_ID_INVALID), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(
        actuator,
        ret,
        "you have not set permission with the id " + PERMISSION_ID_INVALID,
        "you have not set permission with the id " + PERMISSION_ID_INVALID);
  }

  @Test
  public void nullPermissionWithId() {
    int permissionId = ACTIVE_PERMISSION_START_ID;
    PermissionDeleteKeyActuator actuator =
        new PermissionDeleteKeyActuator(
            getContract(OWNER_ADDRESS, KEY_ADDRESS, permissionId), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(
        actuator,
        ret,
        "no permission with given id",
        "you have not set permission with the id " + permissionId);
  }

  @Test
  public void invalidKeyAddress() {
    // init account with permission
    addValidPermissionKey();

    PermissionDeleteKeyActuator actuator =
        new PermissionDeleteKeyActuator(
            getContract(OWNER_ADDRESS, KEY_ADDRESS_INVALID, OWNER_PERMISSION_ID), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(
        actuator, ret, "address in key is invalidate", "address in key is invalidate");
  }

  @Test
  public void keyAddressNotInPermission() {
    addValidPermissionKey();

    PermissionDeleteKeyActuator actuator =
        new PermissionDeleteKeyActuator(
            getContract(OWNER_ADDRESS, SECOND_KEY_ADDRESS, OWNER_PERMISSION_ID), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(
        actuator,
        ret,
        "address is not in permission",
        "address is not in permission " + OWNER_PERMISSION_ID);
  }

  @Test
  public void sumWeightLessThanThreshold() {
    //init
    byte[] owner_address_array = ByteArray.fromHexString(OWNER_ADDRESS);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(owner_address_array);

    Permission ownerPermission =
        Permission.newBuilder()
            .setType(PermissionType.Owner)
            .setPermissionName("owner")
            .setThreshold(2)
            .addKeys(Key.newBuilder().setAddress(accountCapsule.getAddress()).setWeight(4).build())
            .addKeys(
                Key.newBuilder()
                    .setAddress((accountCapsule.getAddress()))
                    .setWeight(2)
                    .build())
            .build();
    Permission activePermission = AccountCapsule
        .createDefaultActivePermission(accountCapsule.getAddress());
    List<Permission> activeList = new ArrayList<>();
    activeList.add(activePermission);
    accountCapsule.updatePermissions(ownerPermission, null, activeList);
    dbManager.getAccountStore().put(owner_address_array, accountCapsule);

    addValidPermissionKey();

    PermissionDeleteKeyActuator actuator =
        new PermissionDeleteKeyActuator(
            getContract(OWNER_ADDRESS, OWNER_ADDRESS, OWNER_PERMISSION_ID), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(
        actuator,
        ret,
        "sum of weight is less than threshold after delete",
        "the sum of weight is less than threshold after delete this address, "
            + "please add a new key before delete this key");
  }
}
