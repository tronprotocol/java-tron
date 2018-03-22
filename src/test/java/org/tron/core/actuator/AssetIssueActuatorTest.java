package org.tron.core.actuator;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.io.File;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
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
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Transaction.Result.code;

@Slf4j
public class AssetIssueActuatorTest {
  private static Manager dbManager;
  private static Any contract;
  private static final String dbPath = "output_assetIssue_test";

  private static final String OWNER_ADDRESS = "abd4b9367799eaa3197fecb144eb71de1e049150";
  private static final String NAME = "trx-my";
  private static final long TOTAL_SUPPLY = 10000L;
  private static final int TRX_NUM = 10000;
  private static final int NUM = 100000;
  private static final int DECAY_RATIO = 50;
  private static final long VOTE_SCORE = 100;
  private static final String DESCRIPTION = "myCoin";
  private static final String URL = "tron-my.com";

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
   * create temp Capsule test need.
   */
  @Before
  public void createCapsule() {
    AccountCapsule ownerCapsule = new AccountCapsule(
        ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
        ByteString.copyFromUtf8("owner"), AccountType.AssetIssue);
    dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);
  }

  private Any getContract() {
    long nowTime = new Date().getTime();
    return Any.pack(
        Contract.AssetIssueContract
            .newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setName(ByteString.copyFromUtf8(NAME))
            .setTotalSupply(TOTAL_SUPPLY)
            .setTrxNum(TRX_NUM)
            .setNum(NUM)
            .setStartTime(nowTime)
            .setEndTime(nowTime + 24 * 3600 * 1000)
            .setDecayRatio(DECAY_RATIO)
            .setDescription(ByteString.copyFromUtf8(DESCRIPTION))
            .setUrl(ByteString.copyFromUtf8(URL))
            .build());
  }

  @Test
  public void rightAssetIssue() {
    AssetIssueActuator actuator = new AssetIssueActuator(getContract(), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule owner = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));
      AssetIssueCapsule assetIssueCapsule = dbManager.getAssetIssueStore()
          .get(ByteString.copyFromUtf8(NAME).toByteArray());
      Assert.assertNotNull(assetIssueCapsule);
      Assert.assertEquals(owner.getInstance().getAssetMap().get(NAME).longValue(), 10000L);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueStore().delete(ByteArray.fromString(NAME));
    }
  }

  @Test
  public void repeatAssetIssue() {
    AssetIssueActuator actuator = new AssetIssueActuator(getContract(), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      dbManager.getAssetIssueStore().put(ByteArray.fromString(NAME),
          new AssetIssueCapsule(getContract().unpack(Contract.AssetIssueContract.class)));
      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      AccountCapsule owner = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));
      AssetIssueCapsule assetIssueCapsule = dbManager.getAssetIssueStore()
          .get(ByteArray.fromString(NAME));
      Assert.assertNotNull(assetIssueCapsule);
      Assert.assertNull(owner.getInstance().getAssetMap().get(NAME));
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } catch (InvalidProtocolBufferException e) {
      Assert.assertFalse(e instanceof InvalidProtocolBufferException);
    } finally {
      dbManager.getAssetIssueStore().delete(ByteArray.fromString(NAME));
    }
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
}