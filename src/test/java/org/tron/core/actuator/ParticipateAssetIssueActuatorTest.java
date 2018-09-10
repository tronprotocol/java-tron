package org.tron.core.actuator;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.io.File;
import org.joda.time.DateTime;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
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
import org.tron.protos.Contract;
import org.tron.protos.Contract.AssetIssueContract;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Transaction.Result.code;

public class ParticipateAssetIssueActuatorTest {

  private static final Logger logger = LoggerFactory.getLogger("Test");
  private static Manager dbManager;
  private static final String dbPath = "output_participateAsset_test";
  private static TronApplicationContext context;
  private static final String OWNER_ADDRESS;
  private static final String TO_ADDRESS;
  private static final String THIRD_ADDRESS;
  private static final String NOT_EXIT_ADDRESS;
  private static final String ASSET_NAME = "myCoin";
  private static final long OWNER_BALANCE = 99999;
  private static final long TO_BALANCE = 100001;
  private static final long TOTAL_SUPPLY = 10000000000000L;
  private static final int TRX_NUM = 2;
  private static final int NUM = 2147483647;
  private static final int VOTE_SCORE = 2;
  private static final String DESCRIPTION = "TRX";
  private static final String URL = "https://tron.network";

  static {
    Args.setParam(new String[]{"--output-directory", dbPath}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
    TO_ADDRESS = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
    THIRD_ADDRESS = Wallet.getAddressPreFixString() + "4948c2e8a756d9437037dcd8c7e0c73d560ca38d";
    NOT_EXIT_ADDRESS = Wallet.getAddressPreFixString() + "B56446E617E924805E4D6CA021D341FEF6E2013B";
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
    AccountCapsule ownerCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8("owner"),
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
            AccountType.Normal,
            OWNER_BALANCE);
    AccountCapsule toAccountCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8("toAccount"),
            ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)),
            AccountType.Normal,
            TO_BALANCE);
    dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);
    dbManager.getAccountStore().put(toAccountCapsule.getAddress().toByteArray(), toAccountCapsule);
  }

  private Any getContract(long count) {
    return Any.pack(
        Contract.ParticipateAssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)))
            .setAssetName(ByteString.copyFromUtf8(ASSET_NAME))
            .setAmount(count)
            .build());
  }

  private Any getContractWithOwner(long count, String ownerAddress) {
    return Any.pack(
        Contract.ParticipateAssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
            .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)))
            .setAssetName(ByteString.copyFromUtf8(ASSET_NAME))
            .setAmount(count)
            .build());
  }

  private Any getContractWithTo(long count, String toAddress) {
    return Any.pack(
        Contract.ParticipateAssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(toAddress)))
            .setAssetName(ByteString.copyFromUtf8(ASSET_NAME))
            .setAmount(count)
            .build());
  }

  private Any getContract(long count, String assetName) {
    return Any.pack(
        Contract.ParticipateAssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)))
            .setAssetName(ByteString.copyFromUtf8(assetName))
            .setAmount(count)
            .build());
  }

  private Any getContract(long count, ByteString assetName) {
    return Any.pack(
        Contract.ParticipateAssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)))
            .setAssetName(assetName)
            .setAmount(count)
            .build());
  }

  private void initAssetIssue(long startTimestmp, long endTimestmp) {
    AssetIssueContract assetIssueContract =
        AssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)))
            .setName(ByteString.copyFrom(ByteArray.fromString(ASSET_NAME)))
            .setTotalSupply(TOTAL_SUPPLY)
            .setTrxNum(TRX_NUM)
            .setNum(NUM)
            .setStartTime(startTimestmp)
            .setEndTime(endTimestmp)
            .setVoteScore(VOTE_SCORE)
            .setDescription(ByteString.copyFrom(ByteArray.fromString(DESCRIPTION)))
            .setUrl(ByteString.copyFrom(ByteArray.fromString(URL)))
            .build();
    AssetIssueCapsule assetIssueCapsule = new AssetIssueCapsule(assetIssueContract);
    dbManager.getAssetIssueStore()
        .put(assetIssueCapsule.createDbKey(), assetIssueCapsule);
    AccountCapsule toAccountCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(TO_ADDRESS));
    toAccountCapsule.addAsset(ASSET_NAME.getBytes(), TOTAL_SUPPLY);
    dbManager.getAccountStore().put(toAccountCapsule.getAddress().toByteArray(), toAccountCapsule);
  }

  private void initAssetIssue(long startTimestmp, long endTimestmp, String assetName) {
    AssetIssueContract assetIssueContract =
        AssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)))
            .setName(ByteString.copyFrom(ByteArray.fromString(assetName)))
            .setTotalSupply(TOTAL_SUPPLY)
            .setTrxNum(TRX_NUM)
            .setNum(NUM)
            .setStartTime(startTimestmp)
            .setEndTime(endTimestmp)
            .setVoteScore(VOTE_SCORE)
            .setDescription(ByteString.copyFrom(ByteArray.fromString(DESCRIPTION)))
            .setUrl(ByteString.copyFrom(ByteArray.fromString(URL)))
            .build();
    AssetIssueCapsule assetIssueCapsule = new AssetIssueCapsule(assetIssueContract);
    dbManager.getAssetIssueStore()
        .put(assetIssueCapsule.createDbKey(), assetIssueCapsule);
    AccountCapsule toAccountCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(TO_ADDRESS));
    toAccountCapsule.addAsset(assetName.getBytes(), TOTAL_SUPPLY);
    dbManager.getAccountStore().put(toAccountCapsule.getAddress().toByteArray(), toAccountCapsule);
  }

  private void initAssetIssueWithOwner(long startTimestmp, long endTimestmp, String owner) {
    AssetIssueContract assetIssueContract =
        AssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(owner)))
            .setName(ByteString.copyFrom(ByteArray.fromString(ASSET_NAME)))
            .setTotalSupply(TOTAL_SUPPLY)
            .setTrxNum(TRX_NUM)
            .setNum(NUM)
            .setStartTime(startTimestmp)
            .setEndTime(endTimestmp)
            .setVoteScore(VOTE_SCORE)
            .setDescription(ByteString.copyFrom(ByteArray.fromString(DESCRIPTION)))
            .setUrl(ByteString.copyFrom(ByteArray.fromString(URL)))
            .build();
    AssetIssueCapsule assetIssueCapsule = new AssetIssueCapsule(assetIssueContract);
    dbManager.getAssetIssueStore()
        .put(assetIssueCapsule.createDbKey(), assetIssueCapsule);
    AccountCapsule toAccountCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(TO_ADDRESS));
    toAccountCapsule.addAsset(ASSET_NAME.getBytes(), TOTAL_SUPPLY);
    dbManager.getAccountStore().put(toAccountCapsule.getAddress().toByteArray(), toAccountCapsule);
  }

  @Test
  public void rightAssetIssue() {
    initAssetIssue(dbManager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp() - 1000,
        dbManager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp() + 1000);
    ParticipateAssetIssueActuator actuator =
        new ParticipateAssetIssueActuator(getContract(1000L), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount =
          dbManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));

      Assert.assertEquals(owner.getBalance(), OWNER_BALANCE - 1000);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE + 1000);
      Assert.assertEquals(owner.getAssetMap().get(ASSET_NAME).longValue(), (1000L) / TRX_NUM * NUM);
      Assert.assertEquals(
          toAccount.getAssetMap().get(ASSET_NAME).longValue(),
          TOTAL_SUPPLY - (1000L) / TRX_NUM * NUM);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void AssetIssueTimeRight() {
    DateTime now = DateTime.now();
    initAssetIssue(now.minusDays(1).getMillis(), now.getMillis());
    ParticipateAssetIssueActuator actuator =
        new ParticipateAssetIssueActuator(getContract(1000L), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertTrue("No longer valid period!".equals(e.getMessage()));
      AccountCapsule owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount =
          dbManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));

      Assert.assertEquals(owner.getBalance(), OWNER_BALANCE);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE);
      Assert.assertTrue(isNullOrZero(owner.getAssetMap().get(ASSET_NAME)));
      Assert.assertEquals(toAccount.getAssetMap().get(ASSET_NAME).longValue(), TOTAL_SUPPLY);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void AssetIssueTimeLeft() {
    DateTime now = DateTime.now();
    initAssetIssue(now.minusDays(1).getMillis(), now.getMillis());
    ParticipateAssetIssueActuator actuator =
        new ParticipateAssetIssueActuator(getContract(1000L), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertTrue("No longer valid period!".equals(e.getMessage()));
      AccountCapsule owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount =
          dbManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));

      Assert.assertEquals(owner.getBalance(), OWNER_BALANCE);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE);
      Assert.assertTrue(isNullOrZero(owner.getAssetMap().get(ASSET_NAME)));
      Assert.assertEquals(toAccount.getAssetMap().get(ASSET_NAME).longValue(), TOTAL_SUPPLY);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void ExchangeDevisibleTest() {
    initAssetIssue(dbManager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp() - 1000,
        dbManager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp() + 1000);
    ParticipateAssetIssueActuator actuator =
        new ParticipateAssetIssueActuator(getContract(999L), dbManager); //no problem
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);

      AccountCapsule owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount =
          dbManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));

      Assert.assertEquals(owner.getAssetMap().get(ASSET_NAME).longValue(), (999L * NUM) / TRX_NUM);
      Assert.assertEquals(
          toAccount.getAssetMap().get(ASSET_NAME).longValue(),
          TOTAL_SUPPLY - (999L * NUM) / TRX_NUM);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void negativeAmountTest() {
    DateTime now = DateTime.now();
    initAssetIssue(now.minusDays(1).getMillis(), now.plusDays(1).getMillis());
    ParticipateAssetIssueActuator actuator =
        new ParticipateAssetIssueActuator(getContract(-999L), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertTrue("Amount must greater than 0!".equals(e.getMessage()));

      AccountCapsule owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount =
          dbManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));

      Assert.assertEquals(owner.getBalance(), OWNER_BALANCE);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE);
      Assert.assertTrue(isNullOrZero(owner.getAssetMap().get(ASSET_NAME)));
      Assert.assertEquals(toAccount.getAssetMap().get(ASSET_NAME).longValue(), TOTAL_SUPPLY);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void zeroAmountTest() {
    DateTime now = DateTime.now();
    initAssetIssue(now.minusDays(1).getMillis(), now.plusDays(1).getMillis());
    ParticipateAssetIssueActuator actuator =
        new ParticipateAssetIssueActuator(getContract(0), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertTrue("Amount must greater than 0!".equals(e.getMessage()));

      AccountCapsule owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount =
          dbManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));

      Assert.assertEquals(owner.getBalance(), OWNER_BALANCE);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE);
      Assert.assertTrue(isNullOrZero(owner.getAssetMap().get(ASSET_NAME)));
      Assert.assertEquals(toAccount.getAssetMap().get(ASSET_NAME).longValue(), TOTAL_SUPPLY);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  /**
   * Owner account is not exit
   */
  public void noExitOwnerTest() {
    DateTime now = DateTime.now();
    initAssetIssue(now.minusDays(1).getMillis(), now.plusDays(1).getMillis());
    ParticipateAssetIssueActuator actuator = new ParticipateAssetIssueActuator(
        getContractWithOwner(101, NOT_EXIT_ADDRESS),
        dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Account does not exist!", e.getMessage());

      AccountCapsule owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount =
          dbManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));

      Assert.assertEquals(owner.getBalance(), OWNER_BALANCE);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE);
      Assert.assertTrue(isNullOrZero(owner.getAssetMap().get(ASSET_NAME)));
      Assert.assertEquals(toAccount.getAssetMap().get(ASSET_NAME).longValue(), TOTAL_SUPPLY);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  /**
   * To account is not exit.
   */
  public void noExitToTest() {
    initAssetIssueWithOwner(
        dbManager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp() - 1000,
        dbManager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp() + 1000,
        NOT_EXIT_ADDRESS);
    ParticipateAssetIssueActuator actuator = new ParticipateAssetIssueActuator(
        getContractWithTo(101, NOT_EXIT_ADDRESS),
        dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("To account does not exist!", e.getMessage());

      AccountCapsule owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount =
          dbManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));

      Assert.assertEquals(owner.getBalance(), OWNER_BALANCE);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE);
      Assert.assertTrue(isNullOrZero(owner.getAssetMap().get(ASSET_NAME)));
      Assert.assertEquals(toAccount.getAssetMap().get(ASSET_NAME).longValue(), TOTAL_SUPPLY);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  /**
   * Participate to self, will throw exception.
   */
  public void participateAssetSelf() {
    initAssetIssueWithOwner(
        dbManager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp() - 1000,
        dbManager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp() + 1000,
        OWNER_ADDRESS);
    ParticipateAssetIssueActuator actuator = new ParticipateAssetIssueActuator(
        getContractWithTo(101, OWNER_ADDRESS),
        dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Cannot participate asset Issue yourself !", e.getMessage());

      AccountCapsule owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount =
          dbManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));

      Assert.assertEquals(owner.getBalance(), OWNER_BALANCE);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE);
      Assert.assertTrue(isNullOrZero(owner.getAssetMap().get(ASSET_NAME)));
      Assert.assertEquals(toAccount.getAssetMap().get(ASSET_NAME).longValue(), TOTAL_SUPPLY);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  /**
   * Participate to the third party that not the issuer, will throw exception.
   */
  public void participateAssetToThird() {
    initAssetIssue(dbManager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp() - 1000,
        dbManager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp() + 1000);
    ParticipateAssetIssueActuator actuator = new ParticipateAssetIssueActuator(
        getContractWithTo(101, THIRD_ADDRESS), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("The asset is not issued by " + THIRD_ADDRESS, e.getMessage());

      AccountCapsule owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount =
          dbManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));

      Assert.assertEquals(owner.getBalance(), OWNER_BALANCE);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE);
      Assert.assertTrue(isNullOrZero(owner.getAssetMap().get(ASSET_NAME)));
      Assert.assertEquals(toAccount.getAssetMap().get(ASSET_NAME).longValue(), TOTAL_SUPPLY);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  /*
   * Asset name length must between 1 to 32 and can not contain space and other unreadable character, and can not contain chinese characters.
   */

  //asset name validation which is unnecessary has been removed!
  public void assetNameTest() {
    //Empty name, throw exception
    ByteString emptyName = ByteString.EMPTY;
    ParticipateAssetIssueActuator actuator = new ParticipateAssetIssueActuator(
        getContract(1000L, emptyName), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("No asset named null", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

    //Too long name, throw exception. Max long is 32.
    String assetName = "testname0123456789abcdefghijgklmo";
//    actuator = new ParticipateAssetIssueActuator(getContract(1000L, assetName), dbManager);
//    ret = new TransactionResultCapsule();
//    try {
//      actuator.validate();
//      actuator.execute(ret);
//      Assert.assertTrue(false);
//    } catch (ContractValidateException e) {
//      Assert.assertTrue(e instanceof ContractValidateException);
//      Assert.assertEquals("Invalid assetName", e.getMessage());
//      AccountCapsule ownerAccount = dbManager.getAccountStore()
//          .get(ByteArray.fromHexString(OWNER_ADDRESS));
//      Assert.assertTrue(isNullOrZero(ownerAccount.getAssetMap().get(assetName)));
//    } catch (ContractExeException e) {
//      Assert.assertFalse(e instanceof ContractExeException);
//    }

    //Contain space, throw exception. Every character need readable .
    assetName = "t e";
//    actuator = new ParticipateAssetIssueActuator(getContract(1000L, assetName), dbManager);
//    ret = new TransactionResultCapsule();
//    try {
//      actuator.validate();
//      actuator.execute(ret);
//      Assert.assertTrue(false);
//    } catch (ContractValidateException e) {
//      Assert.assertTrue(e instanceof ContractValidateException);
//      Assert.assertEquals("Invalid assetName", e.getMessage());
//      AccountCapsule ownerAccount = dbManager.getAccountStore()
//          .get(ByteArray.fromHexString(OWNER_ADDRESS));
//      Assert.assertTrue(isNullOrZero(ownerAccount.getAssetMap().get(assetName)));
//    } catch (ContractExeException e) {
//      Assert.assertFalse(e instanceof ContractExeException);
//    }

    //Contain chinese character, throw exception.
//    actuator = new ParticipateAssetIssueActuator(
//        getContract(1000L, ByteString.copyFrom(ByteArray.fromHexString("E6B58BE8AF95"))),
//        dbManager);
//    ret = new TransactionResultCapsule();
//    try {
//      actuator.validate();
//      actuator.execute(ret);
//      Assert.assertTrue(false);
//    } catch (ContractValidateException e) {
//      Assert.assertTrue(e instanceof ContractValidateException);
//      Assert.assertEquals("Invalid assetName", e.getMessage());
//      AccountCapsule ownerAccount = dbManager.getAccountStore()
//          .get(ByteArray.fromHexString(OWNER_ADDRESS));
//      Assert.assertTrue(isNullOrZero(ownerAccount.getAssetMap().get(assetName)));
//    } catch (ContractExeException e) {
//      Assert.assertFalse(e instanceof ContractExeException);
//    }

    // 32 byte readable character just ok.
    assetName = "testname0123456789abcdefghijgklm";
    initAssetIssue(dbManager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp() - 1000,
        dbManager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp() + 1000, assetName);
    actuator = new ParticipateAssetIssueActuator(getContract(1000L, assetName), dbManager);
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount =
          dbManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));

      Assert.assertEquals(owner.getBalance(), OWNER_BALANCE - 1000);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE + 1000);
      Assert.assertEquals(owner.getAssetMap().get(assetName).longValue(), (1000L) / TRX_NUM * NUM);
      Assert.assertEquals(toAccount.getAssetMap().get(assetName).longValue(),
          TOTAL_SUPPLY - (1000L) / TRX_NUM * NUM);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

    // 1 byte readable character ok.
    assetName = "t";
    initAssetIssue(dbManager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp() - 1000,
        dbManager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp() + 1000, assetName);
    actuator = new ParticipateAssetIssueActuator(getContract(1000L, assetName), dbManager);
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount =
          dbManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));

      Assert.assertEquals(owner.getBalance(), OWNER_BALANCE - 2000);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE + 2000);
      Assert.assertEquals(owner.getAssetMap().get(assetName).longValue(), (1000L) / TRX_NUM * NUM);
      Assert.assertEquals(toAccount.getAssetMap().get(assetName).longValue(),
          TOTAL_SUPPLY - (1000L) / TRX_NUM * NUM);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void notEnoughTrxTest() {
    initAssetIssue(dbManager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp() - 1000,
        dbManager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp() + 1000);
    // First, reduce the owner trx balance. Else can't complete this test case.
    AccountCapsule owner = dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
    owner.setBalance(100);
    dbManager.getAccountStore().put(owner.getAddress().toByteArray(), owner);
    ParticipateAssetIssueActuator actuator = new ParticipateAssetIssueActuator(getContract(101),
        dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertTrue("No enough balance !".equals(e.getMessage()));

      owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount =
          dbManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));

      Assert.assertEquals(owner.getBalance(), 100);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE);
      Assert.assertTrue(isNullOrZero(owner.getAssetMap().get(ASSET_NAME)));
      Assert.assertEquals(toAccount.getAssetMap().get(ASSET_NAME).longValue(), TOTAL_SUPPLY);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void notEnoughAssetTest() {
    initAssetIssue(dbManager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp() - 1000,
        dbManager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp() + 1000);
    // First, reduce to account asset balance. Else can't complete this test case.
    AccountCapsule toAccount = dbManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));
    toAccount.reduceAssetAmount(ByteString.copyFromUtf8(ASSET_NAME).toByteArray(),
        TOTAL_SUPPLY - 10000);
    dbManager.getAccountStore().put(toAccount.getAddress().toByteArray(), toAccount);
    ParticipateAssetIssueActuator actuator = new ParticipateAssetIssueActuator(getContract(1),
        dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertTrue("Asset balance is not enough !".equals(e.getMessage()));

      AccountCapsule owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      toAccount =
          dbManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));

      Assert.assertEquals(owner.getBalance(), OWNER_BALANCE);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE);
      Assert.assertTrue(isNullOrZero(owner.getAssetMap().get(ASSET_NAME)));
      Assert.assertEquals(toAccount.getAssetMap().get(ASSET_NAME).longValue(), 10000);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void noneExistAssetTest() {
    DateTime now = DateTime.now();
    initAssetIssue(now.minusDays(1).getMillis(), now.plusDays(1).getMillis());
    ParticipateAssetIssueActuator actuator = new ParticipateAssetIssueActuator(
        getContract(1, "TTTTTTTTTTTT"),
        dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertTrue(("No asset named " + "TTTTTTTTTTTT").equals(e.getMessage()));

      AccountCapsule owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount =
          dbManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));

      Assert.assertEquals(owner.getBalance(), OWNER_BALANCE);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE);
      Assert.assertTrue(isNullOrZero(owner.getAssetMap().get(ASSET_NAME)));
      Assert.assertEquals(toAccount.getAssetMap().get(ASSET_NAME).longValue(), TOTAL_SUPPLY);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void addOverflowTest() {
    initAssetIssue(dbManager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp() - 1000,
        dbManager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp() + 1000);
    // First, increase the owner asset balance. Else can't complete this test case.
    AccountCapsule owner = dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
    owner.addAsset(ASSET_NAME.getBytes(), Long.MAX_VALUE);
    dbManager.getAccountStore().put(owner.getAddress().toByteArray(), owner);
    ParticipateAssetIssueActuator actuator = new ParticipateAssetIssueActuator(
        getContract(1L),
        dbManager);
    //NUM = 2147483647;
    //ASSET_BLANCE = Long.MAX_VALUE + 2147483647/2
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertTrue(e instanceof ContractExeException);
      Assert.assertTrue(("long overflow").equals(e.getMessage()));

      owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount =
          dbManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));

      Assert.assertEquals(owner.getBalance(), OWNER_BALANCE);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE);
      Assert.assertEquals(owner.getAssetMap().get(ASSET_NAME).longValue(), Long.MAX_VALUE);
      Assert.assertEquals(toAccount.getAssetMap().get(ASSET_NAME).longValue(), TOTAL_SUPPLY);
    }
  }

  @Test
  public void multiplyOverflowTest() {
    initAssetIssue(dbManager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp() - 1000,
        dbManager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp() + 1000);
    // First, increase the owner trx balance. Else can't complete this test case.
    AccountCapsule owner = dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
    owner.setBalance(100000000000000L);
    dbManager.getAccountStore().put(owner.getAddress().toByteArray(), owner);
    ParticipateAssetIssueActuator actuator = new ParticipateAssetIssueActuator(
        getContract(8589934597L),
        dbManager);
    //NUM = 2147483647;
    //LONG_MAX = 9223372036854775807L = 0x7fffffffffffffff
    //4294967298 * 2147483647 = 9223372036854775806 = 0x7ffffffffffffffe
    //8589934596 * 2147483647 = 4294967298 * 2147483647 *2 = 0xfffffffffffffffc = -4
    //8589934597 * 2147483647 = 8589934596 * 2147483647 + 2147483647 = -4 + 2147483647 = 2147483643  vs 9223372036854775806*2 + 2147483647

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertTrue(("long overflow").equals(e.getMessage()));

      owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount =
          dbManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));

      Assert.assertEquals(owner.getBalance(), 100000000000000L);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE);
      Assert.assertTrue(isNullOrZero(owner.getAssetMap().get(ASSET_NAME)));
      Assert.assertEquals(toAccount.getAssetMap().get(ASSET_NAME).longValue(), TOTAL_SUPPLY);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * exchangeAmount <= 0 trx, throw exception
   */
  @Test
  public void exchangeAmountTest() {

    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1000000);
    AssetIssueContract assetIssueContract =
        AssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)))
            .setName(ByteString.copyFrom(ByteArray.fromString(ASSET_NAME)))
            .setTotalSupply(TOTAL_SUPPLY)
            .setTrxNum(100)
            .setNum(1)
            .setStartTime(dbManager.getHeadBlockTimeStamp() - 10000)
            .setEndTime(dbManager.getHeadBlockTimeStamp() + 11000000)
            .setVoteScore(VOTE_SCORE)
            .setDescription(ByteString.copyFrom(ByteArray.fromString(DESCRIPTION)))
            .setUrl(ByteString.copyFrom(ByteArray.fromString(URL)))
            .build();
    AssetIssueCapsule assetIssueCapsule = new AssetIssueCapsule(assetIssueContract);
    dbManager.getAssetIssueStore()
        .put(assetIssueCapsule.createDbKey(), assetIssueCapsule);

    AccountCapsule toAccountCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(TO_ADDRESS));
    toAccountCapsule.addAsset(ASSET_NAME.getBytes(), TOTAL_SUPPLY);
    dbManager.getAccountStore().put(toAccountCapsule.getAddress().toByteArray(), toAccountCapsule);
    AccountCapsule owner = dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
    owner.setBalance(100000000000000L);
    dbManager.getAccountStore().put(owner.getAddress().toByteArray(), owner);

    ParticipateAssetIssueActuator actuator = new ParticipateAssetIssueActuator(getContract(1),
        dbManager);

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertTrue(("Can not process the exchange!").equals(e.getMessage()));
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  private boolean isNullOrZero(Long value) {
    if (null == value || value == 0) {
      return true;
    }
    return false;
  }
}
