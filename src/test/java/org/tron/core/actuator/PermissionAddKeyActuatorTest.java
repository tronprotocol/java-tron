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
import org.tron.protos.Contract;
import org.tron.protos.Protocol.Key;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Permission;
import org.tron.protos.Protocol.Transaction.Result.code;

@Slf4j
public class PermissionAddKeyActuatorTest {

  private static Manager dbManager;
  private static final String dbPath = "output_transfer_test";
  private static TronApplicationContext context;

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
    Args.setParam(new String[] {"--output-directory", dbPath}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
    KEY_ADDRESS = Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";

    // OWNER_ADDRESS = "415A523B449890854C8FC460AB602DF9F31FE4293F";
    // KEY_ADDRESS = "414948C2E8A756D9437037DCD8C7E0C73D560CA38D";
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

  /** Init data. */
  @BeforeClass
  public static void init() {
    dbManager = context.getBean(Manager.class);
  }

  /** Release resources. */
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

  /** create temp Capsule test need. */
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

  private void checkResult(
      String ownerAddress, String permissionName, String keyAddress, long keyWeight) {
    boolean checked = false;
    AccountCapsule owner = dbManager.getAccountStore().get(ByteArray.fromHexString(ownerAddress));
    Permission permission = owner.getPermissionByName(permissionName);
    if (permission != null) {
      for (Key key : permission.getKeysList()) {
        if (key.getAddress().equals(ByteString.copyFrom(ByteArray.fromHexString(keyAddress)))) {
          if (!checked) {
            checked = true;
          } else {
            Assert.assertFalse(checked);
          }
          Assert.assertEquals(key.getWeight(), keyWeight);
        }
      }
    }

    Assert.assertTrue(checked);
  }

  @Test
  public void successFistAddPermissionKey() {
    PermissionAddKeyActuator actuator = new PermissionAddKeyActuator(getContract(), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);

      checkResult(OWNER_ADDRESS, PERMISSION_NAME, KEY_ADDRESS, KEY_WEIGHT);

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
    String permissionName = "owner";
    long keyWeight = 2;

    byte[] owner_name_array = ByteArray.fromHexString(OWNER_ADDRESS);
    AccountCapsule account = dbManager.getAccountStore().get(owner_name_array);
    account.permissionAddKey(VALID_KEY, PERMISSION_NAME);
    dbManager.getAccountStore().put(owner_name_array, account);

    PermissionAddKeyActuator actuator =
        new PermissionAddKeyActuator(
            getContract(OWNER_ADDRESS, SECOND_KEY_ADDRESS, keyWeight, permissionName), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);

      checkResult(OWNER_ADDRESS, permissionName, SECOND_KEY_ADDRESS, keyWeight);

      Assert.assertTrue(true);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void addOwnerSelfKey() {
    PermissionAddKeyActuator actuator =
        new PermissionAddKeyActuator(
            getContract(OWNER_ADDRESS, OWNER_ADDRESS, 3, "owner"), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);

      checkResult(OWNER_ADDRESS, "owner", OWNER_ADDRESS, 3);

      Assert.assertTrue(true);

    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void invalidOwnerAddress() {
    PermissionAddKeyActuator actuator =
        new PermissionAddKeyActuator(
            getContract(OWNER_ADDRESS_INVALID, VALID_KEY, "owner"), dbManager);

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);

      fail("invalidate ownerAddress");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("invalidate ownerAddress", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void nullAccount() {
    PermissionAddKeyActuator actuator =
        new PermissionAddKeyActuator(
            getContract(OWNER_ADDRESS_NOACCOUNT, VALID_KEY, "owner"), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);

      fail("ownerAddress account does not exist");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("ownerAddress account does not exist", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void invalidPermissionName() {
    PermissionAddKeyActuator actuator =
        new PermissionAddKeyActuator(
            getContract(OWNER_ADDRESS, KEY_ADDRESS, KEY_WEIGHT, PERMISSION_NAME_INVALID),
            dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);

      fail("permission name should be owner or active");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("permission name should be owner or active", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void invalidKeyAddress() {
    PermissionAddKeyActuator actuator =
        new PermissionAddKeyActuator(
            getContract(OWNER_ADDRESS, KEY_ADDRESS_INVALID, KEY_WEIGHT, PERMISSION_NAME),
            dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);

      fail("address in key is invalidate");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("address in key is invalidate", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
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
    try {
      actuator.validate();
      actuator.execute(ret);

      fail("address already in permission");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals(
          "address "
              + Wallet.encode58Check(ByteArray.fromHexString(KEY_ADDRESS))
              + " is already in permission "
              + PERMISSION_NAME,
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void invalidWeightValue() {
    PermissionAddKeyActuator actuator =
        new PermissionAddKeyActuator(
            getContract(OWNER_ADDRESS, KEY_ADDRESS, KEY_WEIGHT_INVALID, PERMISSION_NAME),
            dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);

      fail("key weight should be greater than 0");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("key weight should be greater than 0", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }
}
