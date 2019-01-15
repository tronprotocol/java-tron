package org.tron.core.actuator;

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
import org.tron.protos.Contract.AccountPermissionUpdateContract;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Key;
import org.tron.protos.Protocol.Permission;
import org.tron.protos.Protocol.Permission.PermissionType;
import org.tron.protos.Protocol.Transaction.Result.code;

@Slf4j
public class AccountPermissionUpdateActuatorTest {

  private static Manager dbManager;
  private static final String dbPath = "output_transfer_test";
  private static TronApplicationContext context;
  public static Application AppT;

  private static final String OWNER_ADDRESS;
  private static final String KEY_ADDRESS;
  private static final Key VALID_KEY;
  private static final long KEY_WEIGHT = 2;
  private static final String PERMISSION_NAME = "active";

  private static final String OWNER_ADDRESS_INVALID = "aaaa";
  private static final String OWNER_ADDRESS_NOACCOUNT;
  private static final String KEY_ADDRESS_INVALID = "bbbb";

  static {
    Args.setParam(new String[]{"--output-directory", dbPath}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    AppT = ApplicationFactory.create(context);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
    KEY_ADDRESS = Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";

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
  }

//  /**
//   * contract with default permissions
//   */
//  private Any getContract(String ownerAddress) {
//    String[] permissionNames = {"active", "owner"};
//
//    return getContract(ownerAddress, permissionNames);
//  }
//
//  private Any getContract(String ownerAddress, String[] permissionNames) {
//    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(ownerAddress));
//    List<Permission> initPermissions = new ArrayList<>();
//
//    for (String permissionName : permissionNames) {
//      Permission permission = TransactionCapsule.getDefaultPermission(address, permissionName);
//      initPermissions.add(permission);
//    }
//
//    Contract.AccountPermissionUpdateContract contract =
//        Contract.AccountPermissionUpdateContract.newBuilder()
//            .setOwnerAddress(address)
//            .clearPermissions()
//            .addAllPermissions(initPermissions)
//            .build();
//
//    return Any.pack(contract);
//  }

  private Any getContract(ByteString address, Permission owner, Permission witness,
      List<Permission> activeList) {
    AccountPermissionUpdateContract.Builder builder = AccountPermissionUpdateContract.newBuilder();
    builder.setOwnerAddress(address);
    if (owner != null) {
      builder.setOwner(owner);
    }
    if (witness != null) {
      builder.setWitness(witness);
    }
    if (activeList != null) {
      builder.addAllActives(activeList);
    }
    return Any.pack(builder.build());
  }

  //
//  /**
//   * return a PermissionAddKeyContract as an invalid contract
//   */
//  private Any getInvalidContract() {
//    return Any.pack(
//        Contract.PermissionAddKeyContract.newBuilder()
//            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
//            .setKey(VALID_KEY)
//            .setPermissionName(PERMISSION_NAME)
//            .build());
//  }
//
  private void addDefaultPermission() {
    byte[] owner_name_array = ByteArray.fromHexString(OWNER_ADDRESS);
    AccountCapsule account = dbManager.getAccountStore().get(owner_name_array);

    Permission owner = AccountCapsule.createDefaultOwnerPermission(account.getAddress());
    Permission active = AccountCapsule.createDefaultActivePermission(account.getAddress());
    List<Permission> activeList = new ArrayList<>();
    activeList.add(active);
    account.updatePermissions(owner, null, activeList);

    dbManager.getAccountStore().put(owner_name_array, account);
  }

  //
//  private void processAndCheckInvalid(
//      AccountPermissionUpdateActuator actuator,
//      TransactionResultCapsule ret,
//      String failMsg,
//      String expectedMsg) {
//    try {
//      actuator.validate();
//      actuator.execute(ret);
//
//      fail(failMsg);
//    } catch (ContractValidateException e) {
//      Assert.assertTrue(e instanceof ContractValidateException);
//      Assert.assertEquals(expectedMsg, e.getMessage());
//    } catch (ContractExeException e) {
//      Assert.assertFalse(e instanceof ContractExeException);
//    }
//  }
//
  @Test
  public void successUpdatePermissionKey() {
    String ownerAddress = OWNER_ADDRESS;
    String keyAddress = KEY_ADDRESS;

    // step 1, init
    addDefaultPermission();

    // step2, check init data
    byte[] owner_name_array = ByteArray.fromHexString(ownerAddress);
    ByteString address = ByteString.copyFrom(owner_name_array);
    AccountCapsule owner = dbManager.getAccountStore().get(owner_name_array);

    Permission ownerPermission = AccountCapsule.createDefaultOwnerPermission(address);
    Permission activePermission = AccountCapsule.createDefaultActivePermission(address);

    Assert.assertEquals(owner.getInstance().getActivePermissionCount(), 1);
    Permission ownerPermission1 = owner.getInstance().getOwnerPermission();
    Permission activePermission1 = owner.getInstance().getActivePermission(0);

    Assert.assertEquals(ownerPermission, ownerPermission1);
    Assert.assertEquals(activePermission, activePermission1);

    // step 3, execute update
    // add account
    AccountCapsule ownerCapsule =
        new AccountCapsule(
            ByteString.copyFrom(ByteArray.fromHexString(keyAddress)),
            ByteString.copyFromUtf8("active"),
            AccountType.Normal);
    dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);

    ownerPermission =
        Permission.newBuilder()
            .setType(PermissionType.Owner)
            .setPermissionName("owner")
            .setThreshold(2)
            .addKeys(Key.newBuilder().setAddress(address).setWeight(4).build())
            .addKeys(
                Key.newBuilder()
                    .setAddress(ByteString.copyFrom(ByteArray.fromHexString(keyAddress)))
                    .setWeight(5)
                    .build())
            .build();
    activePermission =
        Permission.newBuilder()
            .setType(PermissionType.Active)
            .setId(2)
            .setPermissionName("active")
            .setThreshold(2)
            .setOperations(ByteString.copyFrom(ByteArray
                .fromHexString("0000000000000000000000000000000000000000000000000000000000000000")))
            .addKeys(Key.newBuilder().setAddress(address).setWeight(2).build())
            .addKeys(
                Key.newBuilder()
                    .setAddress(ByteString.copyFrom(ByteArray.fromHexString(keyAddress)))
                    .setWeight(3)
                    .build())
            .build();

    List<Permission> activeList = new ArrayList<>();
    activeList.add(activePermission);

    AccountPermissionUpdateActuator actuator =
        new AccountPermissionUpdateActuator(getContract(address, ownerPermission, null, activeList),
            dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);

      // step 4, check result after update operation
      owner = dbManager.getAccountStore().get(owner_name_array);
      Assert.assertEquals(owner.getInstance().getActivePermissionCount(), 1);
      ownerPermission1 = owner.getInstance().getOwnerPermission();
      activePermission1 = owner.getInstance().getActivePermission(0);

      Assert.assertEquals(ownerPermission1, ownerPermission);
      Assert.assertEquals(activePermission1, activePermission);

    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }
//
//  @Test
//  public void nullContract() {
//    AccountPermissionUpdateActuator actuator =
//        new AccountPermissionUpdateActuator(null, dbManager);
//    TransactionResultCapsule ret = new TransactionResultCapsule();
//
//    processAndCheckInvalid(
//        actuator,
//        ret,
//        "No contract!",
//        "No contract!");
//  }
//
//  @Test
//  public void nullDbManager() {
//    AccountPermissionUpdateActuator actuator =
//        new AccountPermissionUpdateActuator(getContract(OWNER_ADDRESS), null);
//    TransactionResultCapsule ret = new TransactionResultCapsule();
//
//    processAndCheckInvalid(
//        actuator,
//        ret,
//        "No dbManager!",
//        "No dbManager!");
//  }
//
//  @Test
//  public void invalidContract() {
//    Any invalidContract = getInvalidContract();
//    AccountPermissionUpdateActuator actuator =
//        new AccountPermissionUpdateActuator(invalidContract, dbManager);
//    TransactionResultCapsule ret = new TransactionResultCapsule();
//
//    processAndCheckInvalid(
//        actuator,
//        ret,
//        "contract type error",
//        "contract type error,expected type [AccountPermissionUpdateContract],real type["
//            + invalidContract.getClass()
//            + "]");
//  }
//
//  @Test
//  public void invalidOwnerAddress() {
//    AccountPermissionUpdateActuator actuator =
//        new AccountPermissionUpdateActuator(getContract(OWNER_ADDRESS_INVALID), dbManager);
//    TransactionResultCapsule ret = new TransactionResultCapsule();
//
//    processAndCheckInvalid(actuator, ret, "invalidate ownerAddress", "invalidate ownerAddress");
//  }
//
//  @Test
//  public void nullAccount() {
//    AccountPermissionUpdateActuator actuator =
//        new AccountPermissionUpdateActuator(getContract(OWNER_ADDRESS_NOACCOUNT), dbManager);
//    TransactionResultCapsule ret = new TransactionResultCapsule();
//
//    processAndCheckInvalid(
//        actuator,
//        ret,
//        "ownerAddress account does not exist",
//        "ownerAddress account does not exist");
//  }
//
//  @Test
//  public void invalidPermissionsCount() {
//    AccountPermissionUpdateActuator actuator =
//        new AccountPermissionUpdateActuator(
//            getContract(OWNER_ADDRESS, new String[]{"active"}), dbManager);
//    TransactionResultCapsule ret = new TransactionResultCapsule();
//
//    processAndCheckInvalid(
//        actuator, ret, "permission's count should be 2.", "permission's count should be 2.");
//  }
//
//  @Test
//  public void invalidPermissionKeyCount() {
//    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS));
//
//    Permission ownerPermission = TransactionCapsule.getDefaultPermission(address, "owner");
//    Permission activePermission =
//        Permission.newBuilder().setName("active").setThreshold(1).setParent("owner").build();
//
//    List<Permission> initPermissions = new ArrayList<>();
//    initPermissions.add(ownerPermission);
//    initPermissions.add(activePermission);
//
//    AccountPermissionUpdateActuator actuator =
//        new AccountPermissionUpdateActuator(getContract(address, initPermissions), dbManager);
//    TransactionResultCapsule ret = new TransactionResultCapsule();
//
//    processAndCheckInvalid(
//        actuator,
//        ret,
//        "key's count should be greater than 0",
//        "key's count should be greater than 0");
//  }
//
//  @Test
//  public void invalidPermissionKeyCount2() {
//    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS));
//
//    Permission ownerPermission = Permission.newBuilder().setName("owner").setThreshold(1).build();
//    Permission activePermission = TransactionCapsule.getDefaultPermission(address, "active");
//
//    List<Permission> initPermissions = new ArrayList<>();
//    initPermissions.add(ownerPermission);
//    initPermissions.add(activePermission);
//
//    AccountPermissionUpdateActuator actuator =
//        new AccountPermissionUpdateActuator(getContract(address, initPermissions), dbManager);
//    TransactionResultCapsule ret = new TransactionResultCapsule();
//
//    processAndCheckInvalid(
//        actuator,
//        ret,
//        "key's count should be greater than 0",
//        "key's count should be greater than 0");
//  }
//
//  @Test
//  public void invalidThreshold() {
//    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS));
//
//    Permission ownerPermission =
//        Permission.newBuilder()
//            .setName("owner")
//            .setThreshold(0)
//            .addKeys(Key.newBuilder().setAddress(address).setWeight(1).build())
//            .build();
//    Permission activePermission = TransactionCapsule.getDefaultPermission(address, "active");
//
//    List<Permission> initPermissions = new ArrayList<>();
//    initPermissions.add(ownerPermission);
//    initPermissions.add(activePermission);
//
//    AccountPermissionUpdateActuator actuator =
//        new AccountPermissionUpdateActuator(getContract(address, initPermissions), dbManager);
//    TransactionResultCapsule ret = new TransactionResultCapsule();
//
//    processAndCheckInvalid(
//        actuator,
//        ret,
//        "permission's threshold should be greater than 0",
//        "permission's threshold should be greater than 0");
//  }
//
//  @Test
//  public void emptyPermissionName() {
//    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS));
//
//    Permission ownerPermission =
//        Permission.newBuilder()
//            .setThreshold(1)
//            .addKeys(Key.newBuilder().setAddress(address).setWeight(1).build())
//            .build();
//    Permission activePermission = TransactionCapsule.getDefaultPermission(address, "active");
//
//    List<Permission> initPermissions = new ArrayList<>();
//    initPermissions.add(ownerPermission);
//    initPermissions.add(activePermission);
//
//    AccountPermissionUpdateActuator actuator =
//        new AccountPermissionUpdateActuator(getContract(address, initPermissions), dbManager);
//    TransactionResultCapsule ret = new TransactionResultCapsule();
//
//    processAndCheckInvalid(
//        actuator,
//        ret,
//        "permission's name should not be empty",
//        "permission's name should not be empty");
//  }
//
//  @Test
//  public void invalidPermissionName() {
//    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS));
//
//    Permission ownerPermission =
//        Permission.newBuilder()
//            .setName("other")
//            .setThreshold(1)
//            .addKeys(Key.newBuilder().setAddress(address).setWeight(1).build())
//            .build();
//    Permission activePermission = TransactionCapsule.getDefaultPermission(address, "active");
//
//    List<Permission> initPermissions = new ArrayList<>();
//    initPermissions.add(ownerPermission);
//    initPermissions.add(activePermission);
//
//    AccountPermissionUpdateActuator actuator =
//        new AccountPermissionUpdateActuator(getContract(address, initPermissions), dbManager);
//    TransactionResultCapsule ret = new TransactionResultCapsule();
//
//    processAndCheckInvalid(
//        actuator,
//        ret,
//        "permission's name should be owner or active",
//        "permission's name should be owner or active");
//  }
//
//  @Test
//  public void invalidPermissionParent() {
//    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS));
//
//    Permission ownerPermission = TransactionCapsule.getDefaultPermission(address, "owner");
//    Permission activePermission =
//        Permission.newBuilder()
//            .setName("active")
//            .setParent("other")
//            .setThreshold(1)
//            .addKeys(Key.newBuilder().setAddress(address).setWeight(1).build())
//            .build();
//
//    List<Permission> initPermissions = new ArrayList<>();
//    initPermissions.add(ownerPermission);
//    initPermissions.add(activePermission);
//
//    AccountPermissionUpdateActuator actuator =
//        new AccountPermissionUpdateActuator(getContract(address, initPermissions), dbManager);
//    TransactionResultCapsule ret = new TransactionResultCapsule();
//
//    processAndCheckInvalid(
//        actuator,
//        ret,
//        "permission's parent should be owner",
//        "permission's parent should be owner");
//  }
//
//  @Test
//  public void emptyActivePermissionParent() {
//    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS));
//
//    Permission ownerPermission = TransactionCapsule.getDefaultPermission(address, "owner");
//    Permission activePermission =
//        Permission.newBuilder()
//            .setName("active")
//            .setThreshold(1)
//            .addKeys(Key.newBuilder().setAddress(address).setWeight(1).build())
//            .build();
//
//    List<Permission> initPermissions = new ArrayList<>();
//    initPermissions.add(ownerPermission);
//    initPermissions.add(activePermission);
//
//    AccountPermissionUpdateActuator actuator =
//        new AccountPermissionUpdateActuator(getContract(address, initPermissions), dbManager);
//    TransactionResultCapsule ret = new TransactionResultCapsule();
//
//    processAndCheckInvalid(
//        actuator,
//        ret,
//        "active permission's parent should not be empty",
//        "active permission's parent should not be empty");
//  }
//
//  @Test
//  public void addressNotDistinctInPermission() {
//    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS));
//
//    Permission ownerPermission = TransactionCapsule.getDefaultPermission(address, "owner");
//    Permission activePermission =
//        Permission.newBuilder()
//            .setName("active")
//            .setParent("owner")
//            .setThreshold(1)
//            .addKeys(Key.newBuilder().setAddress(address).setWeight(1).build())
//            .addKeys(Key.newBuilder().setAddress(address).setWeight(1).build())
//            .build();
//
//    List<Permission> initPermissions = new ArrayList<>();
//    initPermissions.add(ownerPermission);
//    initPermissions.add(activePermission);
//
//    AccountPermissionUpdateActuator actuator =
//        new AccountPermissionUpdateActuator(getContract(address, initPermissions), dbManager);
//    TransactionResultCapsule ret = new TransactionResultCapsule();
//
//    processAndCheckInvalid(
//        actuator,
//        ret,
//        "address should be distinct in permission",
//        "address should be distinct in permission active");
//  }
//
//  @Test
//  public void invalidKeyAddress() {
//    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS));
//
//    Permission ownerPermission = TransactionCapsule.getDefaultPermission(address, "owner");
//    Permission activePermission =
//        Permission.newBuilder()
//            .setName("active")
//            .setParent("owner")
//            .setThreshold(1)
//            .addKeys(Key.newBuilder().setAddress(address).setWeight(1).build())
//            .addKeys(
//                Key.newBuilder()
//                    .setAddress(ByteString.copyFrom(ByteArray.fromHexString(KEY_ADDRESS_INVALID)))
//                    .setWeight(1)
//                    .build())
//            .build();
//
//    List<Permission> initPermissions = new ArrayList<>();
//    initPermissions.add(ownerPermission);
//    initPermissions.add(activePermission);
//
//    AccountPermissionUpdateActuator actuator =
//        new AccountPermissionUpdateActuator(getContract(address, initPermissions), dbManager);
//    TransactionResultCapsule ret = new TransactionResultCapsule();
//
//    processAndCheckInvalid(
//        actuator, ret, "key is not a validate address", "key is not a validate address");
//  }
//
////  @Test
////  public void notExistKeyAddress() {
////    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS));
////
////    Permission ownerPermission = TransactionCapsule.getDefaultPermission(address, "owner");
////    Permission activePermission =
////        Permission.newBuilder()
////            .setName("active")
////            .setParent("owner")
////            .setThreshold(1)
////            .addKeys(Key.newBuilder().setAddress(address).setWeight(1).build())
////            .addKeys(
////                Key.newBuilder()
////                    .setAddress(ByteString.copyFrom(ByteArray.fromHexString(KEY_ADDRESS)))
////                    .setWeight(1)
////                    .build())
////            .build();
////
////    List<Permission> initPermissions = new ArrayList<>();
////    initPermissions.add(ownerPermission);
////    initPermissions.add(activePermission);
////
////    AccountPermissionUpdateActuator actuator =
////        new AccountPermissionUpdateActuator(getContract(address, initPermissions), dbManager);
////    TransactionResultCapsule ret = new TransactionResultCapsule();
////
////    processAndCheckInvalid(
////        actuator, ret, "key address does not exist", "key address does not exist");
////  }
//
//  @Test
//  public void weighValueInvalid() {
//    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS));
//
//    Permission ownerPermission = TransactionCapsule.getDefaultPermission(address, "owner");
//    Permission activePermission =
//        Permission.newBuilder()
//            .setName("active")
//            .setParent("owner")
//            .setThreshold(1)
//            .addKeys(Key.newBuilder().setAddress(address).setWeight(0).build())
//            .build();
//
//    List<Permission> initPermissions = new ArrayList<>();
//    initPermissions.add(ownerPermission);
//    initPermissions.add(activePermission);
//
//    AccountPermissionUpdateActuator actuator =
//        new AccountPermissionUpdateActuator(getContract(address, initPermissions), dbManager);
//    TransactionResultCapsule ret = new TransactionResultCapsule();
//
//    processAndCheckInvalid(
//        actuator,
//        ret,
//        "key's weight should be greater than 0",
//        "key's weight should be greater than 0");
//  }
//
//  @Test
//  public void sumWeightLessThanThreshold() {
//    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS));
//
//    Permission ownerPermission = TransactionCapsule.getDefaultPermission(address, "owner");
//    Permission activePermission =
//        Permission.newBuilder()
//            .setName("active")
//            .setParent("owner")
//            .setThreshold(2)
//            .addKeys(Key.newBuilder().setAddress(address).setWeight(1).build())
//            .build();
//
//    List<Permission> initPermissions = new ArrayList<>();
//    initPermissions.add(ownerPermission);
//    initPermissions.add(activePermission);
//
//    AccountPermissionUpdateActuator actuator =
//        new AccountPermissionUpdateActuator(getContract(address, initPermissions), dbManager);
//    TransactionResultCapsule ret = new TransactionResultCapsule();
//
//    processAndCheckInvalid(
//        actuator,
//        ret,
//        "sum of all keys weight should not be less that threshold",
//        "sum of all key's weight should not be less than threshold in permission active");
//  }
//
//  @Test
//  public void noActivePermission() {
//    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS));
//
//    Permission ownerPermission = TransactionCapsule.getDefaultPermission(address, "owner");
//    Permission activePermission = TransactionCapsule.getDefaultPermission(address, "owner");
//
//    List<Permission> initPermissions = new ArrayList<>();
//    initPermissions.add(ownerPermission);
//    initPermissions.add(activePermission);
//
//    AccountPermissionUpdateActuator actuator =
//        new AccountPermissionUpdateActuator(getContract(address, initPermissions), dbManager);
//    TransactionResultCapsule ret = new TransactionResultCapsule();
//
//    processAndCheckInvalid(
//        actuator, ret, "active permission is missed", "active permission is missed");
//  }
//
//  @Test
//  public void noOwnerPermission() {
//    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS));
//
//    Permission ownerPermission = TransactionCapsule.getDefaultPermission(address, "active");
//    Permission activePermission = TransactionCapsule.getDefaultPermission(address, "active");
//
//    List<Permission> initPermissions = new ArrayList<>();
//    initPermissions.add(ownerPermission);
//    initPermissions.add(activePermission);
//
//    AccountPermissionUpdateActuator actuator =
//        new AccountPermissionUpdateActuator(getContract(address, initPermissions), dbManager);
//    TransactionResultCapsule ret = new TransactionResultCapsule();
//
//    processAndCheckInvalid(
//        actuator, ret, "owner permission is missed", "owner permission is missed");
//  }
}