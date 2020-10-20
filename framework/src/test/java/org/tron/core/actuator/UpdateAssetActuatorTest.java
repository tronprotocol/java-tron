package org.tron.core.actuator;

import static junit.framework.TestCase.fail;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.io.File;
import java.util.Date;
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
import org.tron.common.utils.StringUtil;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Protocol;
import org.tron.protos.contract.AssetIssueContractOuterClass;
import org.tron.protos.contract.AssetIssueContractOuterClass.AssetIssueContract;
import org.tron.protos.contract.AssetIssueContractOuterClass.UpdateAssetContract;

@Slf4j
public class UpdateAssetActuatorTest {

  private static final String dbPath = "output_updateAsset_test";
  private static final String OWNER_ADDRESS;
  private static final String OWNER_ADDRESS_ACCOUNT_NAME = "test_account";
  private static final String SECOND_ACCOUNT_ADDRESS;
  private static final String OWNER_ADDRESS_NOTEXIST;
  private static final String OWNER_ADDRESS_INVALID = "aaaa";
  private static final String NAME = "trx-my";
  private static final long TOTAL_SUPPLY = 10000L;
  private static final String DESCRIPTION = "myCoin";
  private static final String URL = "tron-my.com";
  private static TronApplicationContext context;
  private static Application AppT;
  private static Manager dbManager;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    AppT = ApplicationFactory.create(context);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
    OWNER_ADDRESS_NOTEXIST =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
    SECOND_ACCOUNT_ADDRESS =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d427122222";
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
    // address in accountStore not the owner of contract
    AccountCapsule secondAccount =
        new AccountCapsule(
            ByteString.copyFrom(ByteArray.fromHexString(SECOND_ACCOUNT_ADDRESS)),
            ByteString.copyFromUtf8(OWNER_ADDRESS_ACCOUNT_NAME),
            Protocol.AccountType.Normal);
    dbManager.getAccountStore().put(ByteArray.fromHexString(SECOND_ACCOUNT_ADDRESS), secondAccount);

    // address does not exist in accountStore
    dbManager.getAccountStore().delete(ByteArray.fromHexString(OWNER_ADDRESS_NOTEXIST));
  }

  private Any getContract(
      String accountAddress, String description, String url, long newLimit, long newPublicLimit) {
    return Any.pack(
        UpdateAssetContract.newBuilder()
            .setOwnerAddress(StringUtil.hexString2ByteString(accountAddress))
            .setDescription(ByteString.copyFromUtf8(description))
            .setUrl(ByteString.copyFromUtf8(url))
            .setNewLimit(newLimit)
            .setNewPublicLimit(newPublicLimit)
            .build());
  }

  private AssetIssueContract getAssetIssueContract() {
    long tokenId = dbManager.getDynamicPropertiesStore().getTokenIdNum() + 1;
    dbManager.getDynamicPropertiesStore().saveTokenIdNum(tokenId);

    long nowTime = new Date().getTime();
    return AssetIssueContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
        .setName(ByteString.copyFromUtf8(NAME))
        .setTotalSupply(TOTAL_SUPPLY)
        .setId(String.valueOf(tokenId))
        .setTrxNum(100)
        .setNum(10)
        .setStartTime(nowTime)
        .setEndTime(nowTime + 24 * 3600 * 1000)
        .setOrder(0)
        .setDescription(ByteString.copyFromUtf8("assetTest"))
        .setUrl(ByteString.copyFromUtf8("tron.test.com"))
        .build();
  }

  private void createAssertBeforSameTokenNameActive() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);

    // address in accountStore and the owner of contract
    AccountCapsule accountCapsule =
        new AccountCapsule(
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
            ByteString.copyFromUtf8(OWNER_ADDRESS_ACCOUNT_NAME),
            Protocol.AccountType.Normal);

    // add asset issue
    AssetIssueCapsule assetIssueCapsule = new AssetIssueCapsule(getAssetIssueContract());
    dbManager.getAssetIssueStore().put(assetIssueCapsule.createDbKey(), assetIssueCapsule);
    dbManager.getAssetIssueV2Store().put(assetIssueCapsule.createDbV2Key(), assetIssueCapsule);

    accountCapsule.setAssetIssuedName(assetIssueCapsule.createDbKey());
    accountCapsule.setAssetIssuedID(assetIssueCapsule.getId().getBytes());

    accountCapsule.addAsset(assetIssueCapsule.createDbKey(), TOTAL_SUPPLY);
    accountCapsule.addAssetV2(assetIssueCapsule.createDbV2Key(), TOTAL_SUPPLY);

    dbManager.getAccountStore().put(ByteArray.fromHexString(OWNER_ADDRESS), accountCapsule);
  }

  private void createAssertSameTokenNameActive() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);

    // address in accountStore and the owner of contract
    AccountCapsule accountCapsule =
        new AccountCapsule(
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
            ByteString.copyFromUtf8(OWNER_ADDRESS_ACCOUNT_NAME),
            Protocol.AccountType.Normal);

    // add asset issue
    AssetIssueCapsule assetIssueCapsule = new AssetIssueCapsule(getAssetIssueContract());
    dbManager.getAssetIssueV2Store().put(assetIssueCapsule.createDbV2Key(), assetIssueCapsule);

    accountCapsule.setAssetIssuedName(assetIssueCapsule.createDbKey());
    accountCapsule.setAssetIssuedID(assetIssueCapsule.getId().getBytes());
    accountCapsule.addAssetV2(assetIssueCapsule.createDbV2Key(), TOTAL_SUPPLY);

    dbManager.getAccountStore().put(ByteArray.fromHexString(OWNER_ADDRESS), accountCapsule);
  }

  @Test
  public void successUpdateAssetBeforeSameTokenNameActive() {
    createAssertBeforSameTokenNameActive();
    long tokenId = dbManager.getDynamicPropertiesStore().getTokenIdNum();
    TransactionResultCapsule ret = new TransactionResultCapsule();
    UpdateAssetActuator actuator;
    actuator = new UpdateAssetActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(OWNER_ADDRESS, DESCRIPTION, URL, 500L, 8000L));
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), Protocol.Transaction.Result.code.SUCESS);
      //V1
      AssetIssueCapsule assetIssueCapsule =
          dbManager.getAssetIssueStore().get(ByteString.copyFromUtf8(NAME).toByteArray());
      Assert.assertNotNull(assetIssueCapsule);
      Assert.assertEquals(
          DESCRIPTION, assetIssueCapsule.getInstance().getDescription().toStringUtf8());
      Assert.assertEquals(URL, assetIssueCapsule.getInstance().getUrl().toStringUtf8());
      Assert.assertEquals(assetIssueCapsule.getFreeAssetNetLimit(), 500L);
      Assert.assertEquals(assetIssueCapsule.getPublicFreeAssetNetLimit(), 8000L);
      //V2
      AssetIssueCapsule assetIssueCapsuleV2 =
          dbManager.getAssetIssueV2Store().get(ByteArray.fromString(String.valueOf(tokenId)));
      Assert.assertNotNull(assetIssueCapsuleV2);
      Assert.assertEquals(
          DESCRIPTION, assetIssueCapsuleV2.getInstance().getDescription().toStringUtf8());
      Assert.assertEquals(URL, assetIssueCapsuleV2.getInstance().getUrl().toStringUtf8());
      Assert.assertEquals(assetIssueCapsuleV2.getFreeAssetNetLimit(), 500L);
      Assert.assertEquals(assetIssueCapsuleV2.getPublicFreeAssetNetLimit(), 8000L);

    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueV2Store().delete(ByteArray.fromString(String.valueOf(tokenId)));
      dbManager.getAssetIssueStore().delete(ByteString.copyFromUtf8(NAME).toByteArray());
    }
  }

  /**
   * Init close SameTokenName,after init data,open SameTokenName
   */
  @Test
  public void oldNotUpdataSuccessUpdateAsset() {
    createAssertBeforSameTokenNameActive();
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    long tokenId = dbManager.getDynamicPropertiesStore().getTokenIdNum();
    TransactionResultCapsule ret = new TransactionResultCapsule();
    UpdateAssetActuator actuator;
    actuator = new UpdateAssetActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(OWNER_ADDRESS, DESCRIPTION, URL, 500L, 8000L));

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), Protocol.Transaction.Result.code.SUCESS);
      //V1 old version exist but  not updata
      AssetIssueCapsule assetIssueCapsule =
          dbManager.getAssetIssueStore().get(ByteString.copyFromUtf8(NAME).toByteArray());
      Assert.assertNotNull(assetIssueCapsule);
      Assert.assertNotEquals(
          DESCRIPTION, assetIssueCapsule.getInstance().getDescription().toStringUtf8());
      Assert.assertNotEquals(URL, assetIssueCapsule.getInstance().getUrl().toStringUtf8());
      Assert.assertNotEquals(assetIssueCapsule.getFreeAssetNetLimit(), 500L);
      Assert.assertNotEquals(assetIssueCapsule.getPublicFreeAssetNetLimit(), 8000L);
      //V2
      AssetIssueCapsule assetIssueCapsuleV2 =
          dbManager.getAssetIssueV2Store().get(ByteArray.fromString(String.valueOf(tokenId)));
      Assert.assertNotNull(assetIssueCapsuleV2);
      Assert.assertEquals(
          DESCRIPTION, assetIssueCapsuleV2.getInstance().getDescription().toStringUtf8());
      Assert.assertEquals(URL, assetIssueCapsuleV2.getInstance().getUrl().toStringUtf8());
      Assert.assertEquals(assetIssueCapsuleV2.getFreeAssetNetLimit(), 500L);
      Assert.assertEquals(assetIssueCapsuleV2.getPublicFreeAssetNetLimit(), 8000L);

    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueV2Store().delete(ByteArray.fromString(String.valueOf(tokenId)));
      dbManager.getAssetIssueStore().delete(ByteString.copyFromUtf8(NAME).toByteArray());
    }
  }

  @Test
  public void successUpdateAssetAfterSameTokenNameActive() {
    createAssertSameTokenNameActive();
    long tokenId = dbManager.getDynamicPropertiesStore().getTokenIdNum();
    TransactionResultCapsule ret = new TransactionResultCapsule();
    UpdateAssetActuator actuator;
    actuator = new UpdateAssetActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(OWNER_ADDRESS, DESCRIPTION, URL,
            500L, 8000L));
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), Protocol.Transaction.Result.code.SUCESS);
      //V1，Data is no longer update
      AssetIssueCapsule assetIssueCapsule =
          dbManager.getAssetIssueStore().get(ByteString.copyFromUtf8(NAME).toByteArray());
      Assert.assertNull(assetIssueCapsule);
      //V2
      AssetIssueCapsule assetIssueCapsuleV2 =
          dbManager.getAssetIssueV2Store().get(ByteArray.fromString(String.valueOf(tokenId)));
      Assert.assertNotNull(assetIssueCapsuleV2);
      Assert.assertEquals(
          DESCRIPTION, assetIssueCapsuleV2.getInstance().getDescription().toStringUtf8());
      Assert.assertEquals(URL, assetIssueCapsuleV2.getInstance().getUrl().toStringUtf8());
      Assert.assertEquals(assetIssueCapsuleV2.getFreeAssetNetLimit(), 500L);
      Assert.assertEquals(assetIssueCapsuleV2.getPublicFreeAssetNetLimit(), 8000L);

    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueV2Store().delete(ByteArray.fromString(String.valueOf(tokenId)));
      dbManager.getAssetIssueStore().delete(ByteString.copyFromUtf8(NAME).toByteArray());
    }
  }

  @Test
  public void invalidAddress() {
    createAssertBeforSameTokenNameActive();
    long tokenId = dbManager.getDynamicPropertiesStore().getTokenIdNum();
    UpdateAssetActuator actuator = new UpdateAssetActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(OWNER_ADDRESS_INVALID, DESCRIPTION, URL,
            500L, 8000L));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);

      fail("Invalid ownerAddress");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Invalid ownerAddress", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueV2Store().delete(ByteArray.fromString(String.valueOf(tokenId)));
      dbManager.getAssetIssueStore().delete(ByteString.copyFromUtf8(NAME).toByteArray());
    }
  }

  @Test
  public void noExistAccount() {
    createAssertBeforSameTokenNameActive();
    long tokenId = dbManager.getDynamicPropertiesStore().getTokenIdNum();
    UpdateAssetActuator actuator = new UpdateAssetActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(OWNER_ADDRESS_NOTEXIST, DESCRIPTION, URL,
            500L, 8000L));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);

      fail("Account does not exist");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Account does not exist", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueV2Store().delete(ByteArray.fromString(String.valueOf(tokenId)));
      dbManager.getAssetIssueStore().delete(ByteString.copyFromUtf8(NAME).toByteArray());
    }
  }

  @Test
  public void noAsset() {
    createAssertBeforSameTokenNameActive();
    long tokenId = dbManager.getDynamicPropertiesStore().getTokenIdNum();
    UpdateAssetActuator actuator = new UpdateAssetActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(SECOND_ACCOUNT_ADDRESS, DESCRIPTION, URL,
            500L, 8000L));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);

      fail("Account has not issued any asset");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Account has not issued any asset", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueV2Store().delete(ByteArray.fromString(String.valueOf(tokenId)));
      dbManager.getAssetIssueStore().delete(ByteString.copyFromUtf8(NAME).toByteArray());
    }
  }

  /*
   * empty url
   */
  @Test
  public void invalidAssetUrl() {
    createAssertBeforSameTokenNameActive();
    long tokenId = dbManager.getDynamicPropertiesStore().getTokenIdNum();
    String localUrl = "";
    UpdateAssetActuator actuator = new UpdateAssetActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(OWNER_ADDRESS, DESCRIPTION, localUrl,
            500L, 8000L));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);

      fail("Invalid url");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Invalid url", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueV2Store().delete(ByteArray.fromString(String.valueOf(tokenId)));
      dbManager.getAssetIssueStore().delete(ByteString.copyFromUtf8(NAME).toByteArray());
    }
  }

  /*
   * description is more than 200 character
   */
  @Test
  public void invalidAssetDescription() {
    createAssertBeforSameTokenNameActive();
    long tokenId = dbManager.getDynamicPropertiesStore().getTokenIdNum();
    String localDescription =
        "abchefghijklmnopqrstuvwxyzabchefghijklmnopqrstuvwxyzabchefghijklmnopqrstuv"
            + "wxyzabchefghijklmnopqrstuvwxyzabchefghijklmnopqrstuvwxyzabchefghijkl"
            + "mnopqrstuvwxyzabchefghijklmnopqrstuvwxyzabchefghijklmnopqrstuvwxyzab"
            + "chefghijklmnopqrstuvwxyz";

    UpdateAssetActuator actuator = new UpdateAssetActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(OWNER_ADDRESS, localDescription, URL,
            500L, 8000L));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);

      fail("Invalid description");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Invalid description", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueV2Store().delete(ByteArray.fromString(String.valueOf(tokenId)));
      dbManager.getAssetIssueStore().delete(ByteString.copyFromUtf8(NAME).toByteArray());
    }
  }

  /*
   * new limit is more than 57_600_000_000
   */
  @Test
  public void invalidNewLimit() {
    createAssertBeforSameTokenNameActive();
    long tokenId = dbManager.getDynamicPropertiesStore().getTokenIdNum();
    long localNewLimit = 57_600_000_001L;
    UpdateAssetActuator actuator = new UpdateAssetActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(OWNER_ADDRESS, DESCRIPTION, URL, localNewLimit, 8000L));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);

      fail("Invalid FreeAssetNetLimit");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Invalid FreeAssetNetLimit", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueV2Store().delete(ByteArray.fromString(String.valueOf(tokenId)));
      dbManager.getAssetIssueStore().delete(ByteString.copyFromUtf8(NAME).toByteArray());
    }
  }

  @Test
  public void invalidNewPublicLimit() {
    createAssertBeforSameTokenNameActive();
    long tokenId = dbManager.getDynamicPropertiesStore().getTokenIdNum();
    long localNewPublicLimit = -1L;
    UpdateAssetActuator actuator = new UpdateAssetActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(OWNER_ADDRESS, DESCRIPTION, URL, 500L, localNewPublicLimit));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);

      fail("Invalid PublicFreeAssetNetLimit");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Invalid PublicFreeAssetNetLimit", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueV2Store().delete(ByteArray.fromString(String.valueOf(tokenId)));
      dbManager.getAssetIssueStore().delete(ByteString.copyFromUtf8(NAME).toByteArray());
    }
  }

  @Test
  public void commonErrorCheck() {

    UpdateAssetActuator actuator = new UpdateAssetActuator();
    ActuatorTest actuatorTest = new ActuatorTest(actuator, dbManager);
    actuatorTest.noContract();

    Any invalidContractTypes = Any.pack(AssetIssueContractOuterClass.AssetIssueContract.newBuilder()
        .build());
    actuatorTest.setInvalidContract(invalidContractTypes);
    actuatorTest.setInvalidContractTypeMsg("contract type error",
        "contract type error, expected type [UpdateAssetContract],real type["
    );
    actuatorTest.invalidContractType();
    createAssertBeforSameTokenNameActive();
    actuatorTest.setContract(getContract(OWNER_ADDRESS, DESCRIPTION, URL, 500L, 8000L));
    actuatorTest.nullTransationResult();

    actuatorTest.setNullDBManagerMsg("No account store or dynamic store!");
    actuatorTest.nullDBManger();
  }

}
