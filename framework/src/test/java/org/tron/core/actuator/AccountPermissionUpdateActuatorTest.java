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
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Key;
import org.tron.protos.Protocol.Permission;
import org.tron.protos.Protocol.Permission.PermissionType;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.Transaction.Result.code;
import org.tron.protos.contract.AccountContract.AccountCreateContract;
import org.tron.protos.contract.AccountContract.AccountPermissionUpdateContract;

@Slf4j
public class AccountPermissionUpdateActuatorTest {

  private static final String dbPath = "output_transfer_test";
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
  private static final long KEY_WEIGHT = 2;
  private static final String OWNER_ADDRESS_INVALID = "aaaa";
  private static final String OWNER_ADDRESS_NOACCOUNT;
  private static final String KEY_ADDRESS_INVALID = "bbbb";
  public static Application AppT;
  private static Manager dbManager;
  private static TronApplicationContext context;

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

    OWNER_ADDRESS_NOACCOUNT = Wallet
        .getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1aed";

    VALID_KEY = Key.newBuilder()
        .setAddress(ByteString.copyFrom(ByteArray.fromHexString(KEY_ADDRESS)))
        .setWeight(KEY_WEIGHT).build();
    VALID_KEY1 = Key.newBuilder()
        .setAddress(ByteString.copyFrom(ByteArray.fromHexString(KEY_ADDRESS1)))
        .setWeight(KEY_WEIGHT).build();
    VALID_KEY2 = Key.newBuilder()
        .setAddress(ByteString.copyFrom(ByteArray.fromHexString(KEY_ADDRESS2)))
        .setWeight(KEY_WEIGHT).build();
    VALID_KEY3 = Key.newBuilder()
        .setAddress(ByteString.copyFrom(ByteArray.fromHexString(KEY_ADDRESS3)))
        .setWeight(KEY_WEIGHT).build();
    VALID_KEY4 = Key.newBuilder()
        .setAddress(ByteString.copyFrom(ByteArray.fromHexString(KEY_ADDRESS4)))
        .setWeight(KEY_WEIGHT).build();
    VALID_KEY5 = Key.newBuilder()
        .setAddress(ByteString.copyFrom(ByteArray.fromHexString(KEY_ADDRESS5)))
        .setWeight(KEY_WEIGHT).build();
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
    AccountCapsule ownerCapsule = new AccountCapsule(
        ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
        ByteString.copyFromUtf8("owner"), AccountType.Normal);
    dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);

    AccountCapsule witnessCapsule = new AccountCapsule(
        ByteString.copyFrom(ByteArray.fromHexString(WITNESS_ADDRESS)),
        ByteString.copyFromUtf8("witness"), AccountType.Normal);
    witnessCapsule.setIsWitness(true);
    dbManager.getAccountStore().put(witnessCapsule.getAddress().toByteArray(), witnessCapsule);
  }

  /**
   * contract with default permissions
   */
  private Any getContract(String ownerAddress) {
    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(ownerAddress));
    Permission owner = AccountCapsule.createDefaultOwnerPermission(address);
    Permission active = AccountCapsule
        .createDefaultActivePermission(address, dbManager.getDynamicPropertiesStore());

    AccountPermissionUpdateContract contract = AccountPermissionUpdateContract.newBuilder()
        .setOwnerAddress(address)
        .setOwner(owner).addActives(active).build();
    return Any.pack(contract);
  }

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

  /**
   * return a PermissionAddKeyContract as an invalid contract
   */
  private Any getInvalidContract() {
    return Any.pack(AccountCreateContract.newBuilder().build());
  }

  private void addDefaultPermission() {
    byte[] owner_name_array = ByteArray.fromHexString(OWNER_ADDRESS);
    AccountCapsule account = dbManager.getAccountStore().get(owner_name_array);

    Permission owner = AccountCapsule.createDefaultOwnerPermission(account.getAddress());
    Permission active = AccountCapsule.createDefaultActivePermission(account.getAddress(),
        dbManager.getDynamicPropertiesStore());
    List<Permission> activeList = new ArrayList<>();
    activeList.add(active);
    account.updatePermissions(owner, null, activeList);

    dbManager.getAccountStore().put(owner_name_array, account);
  }

  private void processAndCheckInvalid(AccountPermissionUpdateActuator actuator,
      TransactionResultCapsule ret,
      String failMsg, String expectedMsg) {
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
    Permission activePermission = AccountCapsule.createDefaultActivePermission(address,
        dbManager.getDynamicPropertiesStore());

    Assert.assertEquals(owner.getInstance().getActivePermissionCount(), 1);
    Permission ownerPermission1 = owner.getInstance().getOwnerPermission();
    Permission activePermission1 = owner.getInstance().getActivePermission(0);

    Assert.assertEquals(ownerPermission, ownerPermission1);
    Assert.assertEquals(activePermission, activePermission1);

    // step 3, execute update
    // add account
    AccountCapsule ownerCapsule = new AccountCapsule(
        ByteString.copyFrom(ByteArray.fromHexString(keyAddress)),
        ByteString.copyFromUtf8("active"), AccountType.Normal);
    dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);

    owner.setBalance(1000_000_000L);
    dbManager.getAccountStore().put(owner.getAddress().toByteArray(), owner);

    ownerPermission = Permission.newBuilder().setType(PermissionType.Owner)
        .setPermissionName("owner").setThreshold(2)
        .addKeys(Key.newBuilder().setAddress(address).setWeight(4).build())
        .addKeys(
            Key.newBuilder().setAddress(ByteString.copyFrom(ByteArray.fromHexString(keyAddress)))
                .setWeight(5).build())
        .build();
    activePermission = Permission.newBuilder().setType(PermissionType.Active).setId(2)
        .setPermissionName("active")
        .setThreshold(2)
        .setOperations(ByteString
            .copyFrom(ByteArray
                .fromHexString("0000000000000000000000000000000000000000000000000000000000000000")))
        .addKeys(Key.newBuilder().setAddress(address).setWeight(2).build())
        .addKeys(
            Key.newBuilder().setAddress(ByteString.copyFrom(ByteArray.fromHexString(keyAddress)))
                .setWeight(3).build())
        .build();

    List<Permission> activeList = new ArrayList<>();
    activeList.add(activePermission);

    AccountPermissionUpdateActuator actuator = new AccountPermissionUpdateActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(address, ownerPermission, null, activeList));
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

  @Test
  public void noContract() {
    AccountPermissionUpdateActuator actuator = new AccountPermissionUpdateActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(null);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(actuator, ret, "No contract!", "No contract!");
  }

  @Test
  public void nullDbManager() {
    AccountPermissionUpdateActuator actuator = new AccountPermissionUpdateActuator();
    actuator.setChainBaseManager(null).setAny(null);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(actuator, ret, "No account store or dynamic store!",
        "No account store or dynamic store!");
  }

  @Test
  public void invalidContract() {
    Any invalidContract = getInvalidContract();
    AccountPermissionUpdateActuator actuator = new AccountPermissionUpdateActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(invalidContract);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(actuator, ret, "contract type error",
        "contract type error,expected type [AccountPermissionUpdateContract],real type["
            + invalidContract.getClass() + "]");
  }

  @Test
  public void invalidTransactionResultCapsule() {

    AccountPermissionUpdateActuator actuator = new AccountPermissionUpdateActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(OWNER_ADDRESS));
    TransactionResultCapsule ret = null;
    processAndCheckInvalid(actuator, ret, "TransactionResultCapsule is null",
        "TransactionResultCapsule is null");
  }

  @Test
  public void invalidOwnerAddress() {
    AccountPermissionUpdateActuator actuator = new AccountPermissionUpdateActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(OWNER_ADDRESS_INVALID));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(actuator, ret, "invalidate ownerAddress",
        "invalidate ownerAddress");
  }

  @Test
  public void nullAccount() {
    AccountPermissionUpdateActuator actuator = new AccountPermissionUpdateActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(OWNER_ADDRESS_NOACCOUNT));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(actuator, ret, "ownerAddress account does not exist",
        "ownerAddress account does not exist");
  }

  @Test
  public void ownerMissed() {
    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS));
    Permission activePermission = AccountCapsule.createDefaultActivePermission(address,
        dbManager.getDynamicPropertiesStore());

    List<Permission> activeList = new ArrayList<>();
    activeList.add(activePermission);
    AccountPermissionUpdateActuator actuator = new AccountPermissionUpdateActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(address, null, null, activeList));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(actuator, ret, "owner permission is missed",
        "owner permission is missed");
  }

  @Test
  public void activeMissed() {
    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS));
    Permission ownerPermission = AccountCapsule.createDefaultOwnerPermission(address);

    AccountPermissionUpdateActuator actuator = new AccountPermissionUpdateActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(address, ownerPermission, null, null));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(actuator, ret, "active permission is missed",
        "active permission is missed");
  }

  @Test
  public void activeToMany() {
    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS));
    Permission ownerPermission = AccountCapsule.createDefaultOwnerPermission(address);
    Permission activePermission = AccountCapsule.createDefaultActivePermission(address,
        dbManager.getDynamicPropertiesStore());

    List<Permission> activeList = new ArrayList<>();
    for (int i = 0; i <= 8; i++) {
      activeList.add(activePermission);
    }

    AccountPermissionUpdateActuator actuator = new AccountPermissionUpdateActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(address, ownerPermission, null, null));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(actuator, ret, "active permission is missed",
        "active permission is missed");
  }

  @Test
  public void witnessNeedless() {
    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS));
    Permission ownerPermission = AccountCapsule.createDefaultOwnerPermission(address);
    Permission witnessPermission = AccountCapsule.createDefaultWitnessPermission(address);
    Permission activePermission = AccountCapsule.createDefaultActivePermission(address,
        dbManager.getDynamicPropertiesStore());

    List<Permission> activeList = new ArrayList<>();
    activeList.add(activePermission);

    AccountPermissionUpdateActuator actuator = new AccountPermissionUpdateActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(address, ownerPermission, witnessPermission, activeList));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(actuator, ret, "account isn't witness can't set witness permission",
        "account isn't witness can't set witness permission");
  }

  @Test
  public void witnessMissed() {
    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(WITNESS_ADDRESS));
    Permission ownerPermission = AccountCapsule.createDefaultOwnerPermission(address);
    Permission activePermission = AccountCapsule.createDefaultActivePermission(address,
        dbManager.getDynamicPropertiesStore());

    List<Permission> activeList = new ArrayList<>();
    activeList.add(activePermission);

    AccountPermissionUpdateActuator actuator = new AccountPermissionUpdateActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(address, ownerPermission, null, activeList));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(actuator, ret, "witness permission is missed",
        "witness permission is missed");
  }

  @Test
  public void invalidOwnerPermissionType() {
    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS));

    Permission ownerPermission = Permission.newBuilder().setType(PermissionType.Active)
        .setPermissionName("owner")
        .setThreshold(1).setParentId(0).build();
    Permission activePermission = AccountCapsule.createDefaultActivePermission(address,
        dbManager.getDynamicPropertiesStore());

    List<Permission> activeList = new ArrayList<>();
    activeList.add(activePermission);

    AccountPermissionUpdateActuator actuator = new AccountPermissionUpdateActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(address, ownerPermission, null, activeList));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(actuator, ret, "owner permission type is error",
        "owner permission type is error");
  }

  @Test
  public void invalidActivePermissionType() {
    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS));
    Permission ownerPermission = AccountCapsule.createDefaultOwnerPermission(address);
    Permission activePermission = Permission.newBuilder().setPermissionName("witness")
        .setThreshold(1).setParentId(0)
        .build();

    List<Permission> activeList = new ArrayList<>();
    activeList.add(activePermission);

    AccountPermissionUpdateActuator actuator = new AccountPermissionUpdateActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(address, ownerPermission, null, activeList));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(actuator, ret, "active permission type is error",
        "active permission type is error");
  }

  @Test
  public void invalidWitnessPermissionType() {
    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(WITNESS_ADDRESS));

    Permission ownerPermission = AccountCapsule.createDefaultOwnerPermission(address);
    Permission witnessPermission = Permission.newBuilder().setPermissionName("witness")
        .setThreshold(1).setParentId(0)
        .build();
    Permission activePermission = AccountCapsule.createDefaultWitnessPermission(address);

    List<Permission> activeList = new ArrayList<>();
    activeList.add(activePermission);

    AccountPermissionUpdateActuator actuator = new AccountPermissionUpdateActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(address, ownerPermission, witnessPermission, activeList));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(actuator, ret, "witness permission type is error",
        "witness permission type is error");
  }

  @Test
  public void ownerPermissionNoKey() {
    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS));

    Permission ownerPermission = Permission.newBuilder().setPermissionName("owner").setThreshold(1)
        .build();
    Permission activePermission = AccountCapsule.createDefaultActivePermission(address,
        dbManager.getDynamicPropertiesStore());

    List<Permission> activeList = new ArrayList<>();
    activeList.add(activePermission);

    AccountPermissionUpdateActuator actuator = new AccountPermissionUpdateActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(address, ownerPermission, null, activeList));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(actuator, ret, "key's count should be greater than 0",
        "key's count should be greater than 0");
  }

  @Test
  public void ownerPermissionToManyKey() {
    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS));

    Permission ownerPermission = Permission.newBuilder().setPermissionName("owner")
        .addKeys(VALID_KEY)
        .addKeys(VALID_KEY1).addKeys(VALID_KEY2).addKeys(VALID_KEY3).addKeys(VALID_KEY4)
        .addKeys(VALID_KEY5)
        .setThreshold(1).build();
    Permission activePermission = AccountCapsule.createDefaultActivePermission(address,
        dbManager.getDynamicPropertiesStore());

    List<Permission> activeList = new ArrayList<>();
    activeList.add(activePermission);

    AccountPermissionUpdateActuator actuator = new AccountPermissionUpdateActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(address, ownerPermission, null, activeList));

    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(actuator, ret,
        "number of keys in permission should not be greater than 5",
        "number of keys in permission should not be greater than 5");
  }

  @Test
  public void activePermissionNoKey() {
    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS));

    Permission ownerPermission = AccountCapsule.createDefaultOwnerPermission(address);
    Permission activePermission = Permission.newBuilder().setType(PermissionType.Active)
        .setPermissionName("active")
        .setThreshold(1).setParentId(0).build();

    List<Permission> activeList = new ArrayList<>();
    activeList.add(activePermission);

    AccountPermissionUpdateActuator actuator = new AccountPermissionUpdateActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(address, ownerPermission, null, activeList));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(actuator, ret, "key's count should be greater than 0",
        "key's count should be greater than 0");
  }

  @Test
  public void activePermissionToManyKey() {
    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS));

    Permission ownerPermission = AccountCapsule.createDefaultOwnerPermission(address);
    Permission activePermission = Permission.newBuilder().setType(PermissionType.Active)
        .setPermissionName("active")
        .addKeys(VALID_KEY).addKeys(VALID_KEY1).addKeys(VALID_KEY2).addKeys(VALID_KEY3)
        .addKeys(VALID_KEY4)
        .addKeys(VALID_KEY5).setThreshold(1).build();

    List<Permission> activeList = new ArrayList<>();
    activeList.add(activePermission);

    AccountPermissionUpdateActuator actuator = new AccountPermissionUpdateActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(address, ownerPermission, null, activeList));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(actuator, ret,
        "number of keys in permission should not be greater than 5",
        "number of keys in permission should not be greater than 5");
  }

  @Test
  public void witnessPermissionNoKey() {
    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(WITNESS_ADDRESS));

    Permission ownerPermission = AccountCapsule.createDefaultOwnerPermission(address);
    Permission activePermission = AccountCapsule.createDefaultActivePermission(address,
        dbManager.getDynamicPropertiesStore());
    Permission witnessPermission = Permission.newBuilder().setType(PermissionType.Witness)
        .setPermissionName("active")
        .setThreshold(1).setParentId(0).build();

    List<Permission> activeList = new ArrayList<>();
    activeList.add(activePermission);

    AccountPermissionUpdateActuator actuator = new AccountPermissionUpdateActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(address, ownerPermission, witnessPermission, activeList));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(actuator, ret, "key's count should be greater than 0",
        "key's count should be greater than 0");
  }

  @Test
  public void witnessPermissionToManyKey() {
    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(WITNESS_ADDRESS));

    Permission ownerPermission = AccountCapsule.createDefaultOwnerPermission(address);
    Permission activePermission = AccountCapsule.createDefaultActivePermission(address,
        dbManager.getDynamicPropertiesStore());
    Permission witnessPermission = Permission.newBuilder().setType(PermissionType.Witness)
        .setPermissionName("witness")
        .addKeys(VALID_KEY).addKeys(VALID_KEY1).addKeys(VALID_KEY2).addKeys(VALID_KEY3)
        .addKeys(VALID_KEY4)
        .addKeys(VALID_KEY5).setThreshold(1).build();

    List<Permission> activeList = new ArrayList<>();
    activeList.add(activePermission);

    AccountPermissionUpdateActuator actuator = new AccountPermissionUpdateActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(address, ownerPermission, witnessPermission, activeList));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(actuator, ret,
        "number of keys in permission should not be greater than 5",
        "number of keys in permission should not be greater than 5");
  }

  @Test
  public void witnessPermissionToManyKey1() {
    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(WITNESS_ADDRESS));

    Permission ownerPermission = AccountCapsule.createDefaultOwnerPermission(address);
    Permission activePermission = AccountCapsule.createDefaultActivePermission(address,
        dbManager.getDynamicPropertiesStore());
    Permission witnessPermission = Permission.newBuilder().setType(PermissionType.Witness)
        .setPermissionName("witness")
        .addKeys(VALID_KEY).addKeys(VALID_KEY1).addKeys(VALID_KEY2).addKeys(VALID_KEY3)
        .addKeys(VALID_KEY4)
        .setThreshold(1).build();

    List<Permission> activeList = new ArrayList<>();
    activeList.add(activePermission);

    AccountPermissionUpdateActuator actuator = new AccountPermissionUpdateActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(address, ownerPermission, witnessPermission, activeList));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(actuator, ret, "Witness permission's key count should be 1",
        "Witness permission's key count should be 1");
  }

  @Test
  public void invalidThreshold() {
    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS));

    Permission ownerPermission = Permission.newBuilder().setPermissionName("owner").setThreshold(0)
        .addKeys(Key.newBuilder().setAddress(address).setWeight(1).build()).build();
    Permission activePermission = AccountCapsule.createDefaultActivePermission(address,
        dbManager.getDynamicPropertiesStore());

    List<Permission> activeList = new ArrayList<>();
    activeList.add(activePermission);

    AccountPermissionUpdateActuator actuator = new AccountPermissionUpdateActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(address, ownerPermission, null, activeList));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(actuator, ret, "permission's threshold should be greater than 0",
        "permission's threshold should be greater than 0");
  }

  @Test
  public void permissionNameTooLong() {
    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS));

    Permission ownerPermission = Permission.newBuilder().setThreshold(1)
        .setPermissionName("0123456789ABCDEF0123456789ABCDEF0")
        .addKeys(Key.newBuilder().setAddress(address).setWeight(1).build()).build();
    Permission activePermission = AccountCapsule.createDefaultActivePermission(address,
        dbManager.getDynamicPropertiesStore());

    List<Permission> activeList = new ArrayList<>();
    activeList.add(activePermission);

    AccountPermissionUpdateActuator actuator = new AccountPermissionUpdateActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(address, ownerPermission, null, activeList));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(actuator, ret, "permission's name is too long",
        "permission's name is too long");
  }

  @Test
  public void invalidPermissionParent() {
    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS));

    Permission ownerPermission = AccountCapsule.createDefaultOwnerPermission(address);
    Permission activePermission = Permission.newBuilder().setType(PermissionType.Active)
        .setPermissionName("active")
        .setParentId(1).setThreshold(1)
        .addKeys(Key.newBuilder().setAddress(address).setWeight(1).build()).build();

    List<Permission> activeList = new ArrayList<>();
    activeList.add(activePermission);

    AccountPermissionUpdateActuator actuator = new AccountPermissionUpdateActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(address, ownerPermission, null, activeList));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(actuator, ret, "permission's parent should be owner",
        "permission's parent should be owner");
  }

  @Test
  public void addressNotDistinctInPermission() {
    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS));

    Permission ownerPermission = AccountCapsule.createDefaultOwnerPermission(address);
    Permission activePermission = Permission.newBuilder().setType(PermissionType.Active)
        .setPermissionName("active")
        .setParentId(0).setThreshold(1)
        .addKeys(Key.newBuilder().setAddress(address).setWeight(1).build())
        .addKeys(Key.newBuilder().setAddress(address).setWeight(1).build()).build();

    List<Permission> activeList = new ArrayList<>();
    activeList.add(activePermission);

    AccountPermissionUpdateActuator actuator = new AccountPermissionUpdateActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(address, ownerPermission, null, activeList));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(actuator, ret, "address should be distinct in permission",
        "address should be distinct in permission Active");
  }

  @Test
  public void invalidKeyAddress() {
    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS));

    Permission ownerPermission = AccountCapsule.createDefaultOwnerPermission(address);
    Permission activePermission = Permission
        .newBuilder().setType(PermissionType.Active).setPermissionName("active").setParentId(0)
        .setThreshold(1)
        .addKeys(Key.newBuilder().setAddress(address).setWeight(1).build()).addKeys(Key.newBuilder()
            .setAddress(ByteString.copyFrom(ByteArray.fromHexString(KEY_ADDRESS_INVALID)))
            .setWeight(1).build())
        .build();

    List<Permission> activeList = new ArrayList<>();
    activeList.add(activePermission);

    AccountPermissionUpdateActuator actuator = new AccountPermissionUpdateActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(address, ownerPermission, null, activeList));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(actuator, ret, "key is not a validate address",
        "key is not a validate address");
  }

  @Test
  public void weighValueInvalid() {
    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS));

    Permission ownerPermission = AccountCapsule.createDefaultOwnerPermission(address);
    Permission activePermission = Permission.newBuilder().setType(PermissionType.Active)
        .setPermissionName("active")
        .setParentId(0).setThreshold(1)
        .addKeys(Key.newBuilder().setAddress(address).setWeight(0).build()).build();

    List<Permission> activeList = new ArrayList<>();
    activeList.add(activePermission);

    AccountPermissionUpdateActuator actuator = new AccountPermissionUpdateActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(address, ownerPermission, null, activeList));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(actuator, ret, "key's weight should be greater than 0",
        "key's weight should be greater than 0");
  }

  @Test
  public void sumWeightLessThanThreshold() {
    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS));

    Permission ownerPermission = AccountCapsule.createDefaultOwnerPermission(address);
    Permission activePermission = Permission.newBuilder().setType(PermissionType.Active)
        .setPermissionName("active")
        .setParentId(0).setThreshold(2)
        .addKeys(Key.newBuilder().setAddress(address).setWeight(1).build()).build();

    List<Permission> activeList = new ArrayList<>();
    activeList.add(activePermission);

    AccountPermissionUpdateActuator actuator = new AccountPermissionUpdateActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(address, ownerPermission, null, activeList));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(actuator, ret,
        "sum of all keys weight should not be less that threshold",
        "sum of all key's weight should not be less than threshold in permission Active");
  }

  @Test
  public void onwerPermissionOperationNeedless() {
    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS));

    Permission ownerPermission = Permission.newBuilder().setType(PermissionType.Owner)
        .setPermissionName("owner")
        .setThreshold(1)
        .setOperations(ByteString
            .copyFrom(ByteArray
                .fromHexString("0000000000000000000000000000000000000000000000000000000000000000")))
        .setParentId(0).addKeys(VALID_KEY).build();
    Permission activePermission = AccountCapsule.createDefaultActivePermission(address,
        dbManager.getDynamicPropertiesStore());

    List<Permission> activeList = new ArrayList<>();
    activeList.add(activePermission);

    AccountPermissionUpdateActuator actuator = new AccountPermissionUpdateActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(address, ownerPermission, null, activeList));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(actuator, ret, "Owner permission needn't operations",
        "Owner permission needn't operations");
  }

  @Test
  public void activePermissionNoOperation() {
    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS));

    Permission ownerPermission = AccountCapsule.createDefaultOwnerPermission(address);
    Permission activePermission = Permission.newBuilder().setType(PermissionType.Active)
        .setPermissionName("active")
        .setThreshold(1).setParentId(0).addKeys(VALID_KEY).build();

    List<Permission> activeList = new ArrayList<>();
    activeList.add(activePermission);

    AccountPermissionUpdateActuator actuator = new AccountPermissionUpdateActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(address, ownerPermission, null, activeList));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(actuator, ret, "operations size must 32", "operations size must 32");
  }

  @Test
  public void activePermissionInvalidOperationSize() {
    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS));

    Permission ownerPermission = AccountCapsule.createDefaultOwnerPermission(address);
    Permission activePermission = Permission.newBuilder().setType(PermissionType.Active)
        .setPermissionName("active")
        .setThreshold(1)
        .setOperations(ByteString
            .copyFrom(ByteArray
                .fromHexString("00000000000000000000000000000000000000000000000000000000000000")))
        .setParentId(0).addKeys(VALID_KEY).build();

    List<Permission> activeList = new ArrayList<>();
    activeList.add(activePermission);

    AccountPermissionUpdateActuator actuator = new AccountPermissionUpdateActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(address, ownerPermission, null, activeList));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(actuator, ret, "operations size must 32", "operations size must 32");
  }

  @Test
  public void activePermissionInvalidOperationBit() {
    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS));

    Permission ownerPermission = AccountCapsule.createDefaultOwnerPermission(address);
    Permission activePermission = Permission.newBuilder().setType(PermissionType.Active)
        .setPermissionName("active")
        .setThreshold(1)
        .setOperations(ByteString
            .copyFrom(ByteArray
                .fromHexString("8000000000000000000000000000000000000000000000000000000000000000")))
        .setParentId(0).addKeys(VALID_KEY).build();

    List<Permission> activeList = new ArrayList<>();
    activeList.add(activePermission);

    AccountPermissionUpdateActuator actuator = new AccountPermissionUpdateActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(address, ownerPermission, null, activeList));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(actuator, ret, "7 isn't a validate ContractType",
        "7 isn't a validate ContractType");
  }

  @Test
  public void witnessPermissionOperationNeedless() {
    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(WITNESS_ADDRESS));

    Permission ownerPermission = AccountCapsule.createDefaultOwnerPermission(address);
    Permission activePermission = AccountCapsule.createDefaultActivePermission(address,
        dbManager.getDynamicPropertiesStore());
    Permission witnessPermission = Permission.newBuilder().setType(PermissionType.Witness)
        .setPermissionName("witness")
        .setThreshold(1)
        .setOperations(ByteString
            .copyFrom(ByteArray
                .fromHexString("0000000000000000000000000000000000000000000000000000000000000000")))
        .setParentId(0).addKeys(VALID_KEY).build();

    List<Permission> activeList = new ArrayList<>();
    activeList.add(activePermission);

    AccountPermissionUpdateActuator actuator = new AccountPermissionUpdateActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(address, ownerPermission, witnessPermission, activeList));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(actuator, ret, "Witness permission needn't operations",
        "Witness permission needn't operations");
  }

  @Test
  public void checkAvailableContractTypeCorrespondingToCode() {
    // note: The aim of this test case is to show how the current codes work.
    // The default value is
    // 7fff1fc0037e0000000000000000000000000000000000000000000000000000,
    // and it should call the addSystemContractAndSetPermission to add new contract
    // type
    // When you add a new contact, you can add its to contractType,
    // as '|| contractType = ContractType.XXX',
    // and you will get the value from the output,
    // then update the value to checkAvailableContractType
    // and checkActiveDefaultOperations
    String validContractType = "7fff1fc0037e3800000000000000000000000000000000000000000000000000";

    byte[] availableContractType = new byte[32];
    for (ContractType contractType : ContractType.values()) {
      if (contractType == org.tron.protos.Protocol.Transaction.Contract.ContractType.UNRECOGNIZED
          || contractType == ContractType.ClearABIContract
          || contractType == ContractType.UpdateBrokerageContract) {
        continue;
      }
      int id = contractType.getNumber();
      System.out.println("id is " + id);
      availableContractType[id / 8] |= (1 << id % 8);
    }

    System.out.println(ByteArray.toHexString(availableContractType));

    Assert.assertEquals(validContractType, ByteArray.toHexString(availableContractType));

  }

  @Test
  public void checkActiveDefaultOperationsCorrespondingToCode() {
    // note: The aim of this test case is to show how the current codes work.
    // The default value is
    // 7fff1fc0033e0000000000000000000000000000000000000000000000000000,
    // and it should call the addSystemContractAndSetPermission to add new contract
    // type
    String validContractType = "7fff1fc0033e3800000000000000000000000000000000000000000000000000";

    byte[] availableContractType = new byte[32];
    for (ContractType contractType : ContractType.values()) {
      if (contractType == org.tron.protos.Protocol.Transaction.Contract.ContractType.UNRECOGNIZED
          || contractType == ContractType.AccountPermissionUpdateContract
          || contractType == ContractType.ClearABIContract
          || contractType == ContractType.UpdateBrokerageContract) {
        continue;
      }
      int id = contractType.getNumber();
      System.out.println("id is " + id);
      availableContractType[id / 8] |= (1 << id % 8);
    }

    System.out.println(ByteArray.toHexString(availableContractType));

    Assert.assertEquals(validContractType, ByteArray.toHexString(availableContractType));

  }

  @Test
  public void checkAvailableContractType() {
    String validContractType = "7fff1fc0037e3900000000000000000000000000000000000000000000000000";

    byte[] availableContractType = new byte[32];
    for (ContractType contractType : ContractType.values()) {
      if (contractType == org.tron.protos.Protocol.Transaction.Contract.ContractType.UNRECOGNIZED
          || contractType == ContractType.UpdateBrokerageContract) {
        continue;
      }
      int id = contractType.getNumber();
      System.out.println("id is " + id);
      availableContractType[id / 8] |= (1 << id % 8);
    }

    System.out.println(ByteArray.toHexString(availableContractType));

    Assert.assertEquals(validContractType, ByteArray.toHexString(availableContractType));

  }

  @Test
  public void checkActiveDefaultOperations() {
    String validContractType = "7fff1fc0033e3900000000000000000000000000000000000000000000000000";

    byte[] availableContractType = new byte[32];
    for (ContractType contractType : ContractType.values()) {
      if (contractType == org.tron.protos.Protocol.Transaction.Contract.ContractType.UNRECOGNIZED
          || contractType == ContractType.AccountPermissionUpdateContract
          || contractType == ContractType.UpdateBrokerageContract) {
        continue;
      }
      int id = contractType.getNumber();
      System.out.println("id is " + id);
      availableContractType[id / 8] |= (1 << id % 8);
    }

    System.out.println(ByteArray.toHexString(availableContractType));

    Assert.assertEquals(validContractType, ByteArray.toHexString(availableContractType));

  }

}