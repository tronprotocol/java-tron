package org.tron.core.actuator;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.io.File;
import java.util.Date;
import org.joda.time.DateTime;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.config.Configuration;
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
  private static Any contract;
  private static final String dbPath = "output_participateAsset_test";


  private static final String OWNER_ADDRESS = "548794500882809695a8a687866e76d4271a1abc";
  private static final String TO_ADDRESS = "abd4b9367799eaa3197fecb144eb71de1e049abc";
  private static final String ASSET_NAME = "myCoin";

  private static final long TOTAL_SUPPLY = 10L;
  private static final int TRX_NUM = 2;
  private static final int NUM = 11;
  private static final long START_TIME = 1;
  private static final long END_TIME = 2;
  private static final int DECAY_RATIO = 2;
  private static final int VOTE_SCORE = 2;
  private static final String DESCRIPTION = "TRX";
  private static final String URL = "https://tron.network";


  /**
   * Init data.
   */
  @BeforeClass
  public static void init() {
    Args.setParam(new String[]{"--output-directory", dbPath},
        Configuration.getByPath("config-junit.conf"));
    dbManager = new Manager();
    dbManager.init();
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
  }

  /**
   * create temp Capsule test need.
   */
  @Before
  public void createCapsule() {
    AccountCapsule ownerCapsule = new AccountCapsule(
        ByteString.copyFromUtf8("owner"),
        ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)), AccountType.Normal, 10000L);
    AccountCapsule toAccountCapsule = new AccountCapsule(
        ByteString.copyFromUtf8("toAccount"),
        ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)), AccountType.Normal, 10000L);
    toAccountCapsule.addAsset(ASSET_NAME, 10000000L);
    dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);
    dbManager.getAccountStore().put(toAccountCapsule.getAddress().toByteArray(), toAccountCapsule);
  }

  private Any getContract(long count) {
    long nowTime = new Date().getTime();
    return Any.pack(
        Contract.ParticipateAssetIssueContract
            .newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)))
            .setAssetName(ByteString.copyFromUtf8(ASSET_NAME))
            .setAmount((int) count)
            .build());
  }

  private void initAssetIssue(long startTimestmp, long endTimestmp) {
    AssetIssueContract assetIssueContract = AssetIssueContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)))
        .setName(ByteString.copyFrom(ByteArray.fromString(ASSET_NAME)))
        .setTotalSupply(TOTAL_SUPPLY)
        .setTrxNum(TRX_NUM)
        .setNum(NUM)
        .setStartTime(startTimestmp)
        .setEndTime(endTimestmp)
        .setDecayRatio(DECAY_RATIO)
        .setVoteScore(VOTE_SCORE)
        .setDescription(ByteString.copyFrom(ByteArray.fromString(DESCRIPTION)))
        .setUrl(ByteString.copyFrom(ByteArray.fromString(URL)))
        .build();
    AssetIssueCapsule assetIssueCapsule = new AssetIssueCapsule(assetIssueContract);
    dbManager.getAssetIssueStore()
        .put(assetIssueCapsule.getName().toByteArray(), assetIssueCapsule);
  }

  @Test
  public void rightAssetIssue() {
    DateTime now = DateTime.now();
    initAssetIssue(now.minusDays(1).getMillis(), now.plusDays(1).getMillis());
    ParticipateAssetIssueActuator actuator = new ParticipateAssetIssueActuator(getContract(1000L),
        dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule owner = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(TO_ADDRESS));

      Assert.assertEquals(owner.getBalance(), 9000L);
      Assert.assertEquals(toAccount.getBalance(), 11000L);
      Assert.assertEquals(owner.getAssetMap().get(ASSET_NAME).longValue(), (1000L) / 2 * 11);
      Assert.assertEquals(toAccount.getAssetMap().get(ASSET_NAME).longValue(),
          10000000L - (1000L) / 2 * 11);
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
    ParticipateAssetIssueActuator actuator = new ParticipateAssetIssueActuator(getContract(1000L),
        dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      AccountCapsule owner = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(TO_ADDRESS));

      Assert.assertEquals(owner.getBalance(), 10000L);
      Assert.assertEquals(toAccount.getBalance(), 10000L);
      Assert.assertTrue(isNullOrZero(owner.getAssetMap().get(ASSET_NAME)));
      Assert.assertEquals(toAccount.getAssetMap().get(ASSET_NAME).longValue(),
          10000000L);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void AssetIssueTimeLeft() {
    DateTime now = DateTime.now();
    initAssetIssue(now.minusDays(1).getMillis(), now.getMillis());
    ParticipateAssetIssueActuator actuator = new ParticipateAssetIssueActuator(getContract(1000L),
        dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      AccountCapsule owner = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(TO_ADDRESS));

      Assert.assertEquals(owner.getBalance(), 10000L);
      Assert.assertEquals(toAccount.getBalance(), 10000L);
      Assert.assertTrue(isNullOrZero(owner.getAssetMap().get(ASSET_NAME)));
      Assert.assertEquals(toAccount.getAssetMap().get(ASSET_NAME).longValue(),
          10000000L);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void ExchangeDevisibleTest(){
    DateTime now = DateTime.now();
    initAssetIssue(now.minusDays(1).getMillis(), now.plusDays(1).getMillis());
    ParticipateAssetIssueActuator actuator =
            new ParticipateAssetIssueActuator(getContract(999L), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try{
      actuator.validate();
      actuator.execute(ret);
    }
    catch(ContractValidateException e){
      Assert.assertTrue(e instanceof ContractValidateException);

      AccountCapsule owner = dbManager.getAccountStore()
              .get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount = dbManager.getAccountStore()
              .get(ByteArray.fromHexString(TO_ADDRESS));

      Assert.assertEquals(owner.getBalance(), 10000L);
      Assert.assertEquals(toAccount.getBalance(), 10000L);
      Assert.assertTrue(isNullOrZero(owner.getAssetMap().get(ASSET_NAME)));
      Assert.assertEquals(toAccount.getAssetMap().get(ASSET_NAME).longValue(),
              10000000L);
    }
    catch(ContractExeException e){
      Assert.assertFalse(e instanceof  ContractExeException);
    }

  }


  @Test
  public void NegativeAmountTest(){
    DateTime now = DateTime.now();
    initAssetIssue(now.minusDays(1).getMillis(), now.plusDays(1).getMillis());
    ParticipateAssetIssueActuator actuator =
            new ParticipateAssetIssueActuator(getContract(-999L), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try{
      actuator.validate();
      actuator.execute(ret);
    }
    catch(ContractValidateException e){
      Assert.assertTrue(e instanceof ContractValidateException);

      AccountCapsule owner = dbManager.getAccountStore()
              .get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount = dbManager.getAccountStore()
              .get(ByteArray.fromHexString(TO_ADDRESS));

      Assert.assertEquals(owner.getBalance(), 10000L);
      Assert.assertEquals(toAccount.getBalance(), 10000L);
      Assert.assertTrue(isNullOrZero(owner.getAssetMap().get(ASSET_NAME)));
      Assert.assertEquals(toAccount.getAssetMap().get(ASSET_NAME).longValue(),
              10000000L);
    }
    catch(ContractExeException e){
      Assert.assertFalse(e instanceof  ContractExeException);
    }

  }

  private boolean isNullOrZero(Long value) {
    if (null == value || value == 0) {
      return true;
    }
    return false;
  }
}