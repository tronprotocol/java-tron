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
import org.tron.common.utils.StringUtil;
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
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Account.Frozen;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Transaction.Result.code;

@Slf4j
public class UnfreezeAssetActuatorTest {

  private static Manager dbManager;
  private static final String dbPath = "output_unfreeze_asset_test";
  private static AnnotationConfigApplicationContext context;
  private static final String OWNER_ADDRESS;
  private static final String OWNER_ADDRESS_INVALID = "aaaa";
  private static final String OWNER_ACCOUNT_INVALID;
  private static final long initBalance = 10_000_000_000L;
  private static final long frozenBalance = 1_000_000_000L;
  private static final String assetName = "testCoin";

  static {
    Args.setParam(new String[]{"--output-directory", dbPath}, Constant.TEST_CONF);
    context = new AnnotationConfigApplicationContext(DefaultConfig.class);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
    OWNER_ACCOUNT_INVALID =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a3456";
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
    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
    }
    context.destroy();
  }

  /**
   * create temp Capsule test need.
   */
  @Before
  public void createAccountCapsule() {
    AccountCapsule ownerCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8("owner"),
            StringUtil.hexString2ByteString(OWNER_ADDRESS),
            AccountType.Normal,
            initBalance);
    ownerCapsule.setAssetIssuedName(assetName.getBytes());
    dbManager.getAccountStore().put(ownerCapsule.createDbKey(), ownerCapsule);
  }

  private Any getContract(String ownerAddress) {
    return Any.pack(
        Contract.UnfreezeAssetContract.newBuilder()
            .setOwnerAddress(StringUtil.hexString2ByteString(ownerAddress))
            .build());
  }

  @Test
  public void testUnfreezeAsset() {
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    Account account = dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS))
        .getInstance();
    Frozen newFrozen0 = Frozen.newBuilder()
        .setFrozenBalance(frozenBalance)
        .setExpireTime(now)
        .build();
    Frozen newFrozen1 = Frozen.newBuilder()
        .setFrozenBalance(frozenBalance + 1)
        .setExpireTime(now + 600000)
        .build();
    account = account.toBuilder().addFrozenSupply(newFrozen0).addFrozenSupply(newFrozen1).build();
    AccountCapsule accountCapsule = new AccountCapsule(account);
    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
    UnfreezeAssetActuator actuator = new UnfreezeAssetActuator(getContract(OWNER_ADDRESS),
        dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule owner = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));
      Assert.assertEquals(owner.getAssetMap().get(assetName).longValue(), frozenBalance);
      Assert.assertEquals(owner.getFrozenSupplyCount(), 1);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void invalidOwnerAddress() {
    UnfreezeAssetActuator actuator = new UnfreezeAssetActuator(getContract(OWNER_ADDRESS_INVALID),
        dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Invalid address", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertTrue(e instanceof ContractExeException);
    }
  }

  @Test
  public void invalidOwnerAccount() {
    UnfreezeAssetActuator actuator = new UnfreezeAssetActuator(getContract(OWNER_ACCOUNT_INVALID),
        dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Account[" + OWNER_ACCOUNT_INVALID + "] not exists",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void notIssueAsset() {
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    Account account = dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS))
        .getInstance();
    Frozen newFrozen = Frozen.newBuilder()
        .setFrozenBalance(frozenBalance)
        .setExpireTime(now)
        .build();
    account = account.toBuilder().addFrozenSupply(newFrozen).setAssetIssuedName(ByteString.EMPTY)
        .build();
    AccountCapsule accountCapsule = new AccountCapsule(account);
    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
    UnfreezeAssetActuator actuator = new UnfreezeAssetActuator(getContract(OWNER_ADDRESS),
        dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("this account did not issue any asset", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void noFrozenSupply() {
    UnfreezeAssetActuator actuator = new UnfreezeAssetActuator(getContract(OWNER_ADDRESS),
        dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("no frozen supply balance", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void notTimeToUnfreeze() {
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    Account account = dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS))
        .getInstance();
    Frozen newFrozen = Frozen.newBuilder()
        .setFrozenBalance(frozenBalance)
        .setExpireTime(now + 60000)
        .build();
    account = account.toBuilder().addFrozenSupply(newFrozen).build();
    AccountCapsule accountCapsule = new AccountCapsule(account);
    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
    UnfreezeAssetActuator actuator = new UnfreezeAssetActuator(getContract(OWNER_ADDRESS),
        dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("It's not time to unfreeze asset supply", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }
}