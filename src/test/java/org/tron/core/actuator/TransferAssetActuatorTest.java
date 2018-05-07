/*
 * java-tron is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * java-tron is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
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

@Slf4j
public class TransferAssetActuatorTest {

  private static AnnotationConfigApplicationContext context;
  private static Manager dbManager;
  private static Any contract;
  private static final String dbPath = "output_contract_test";
  private static final String ASSET_NAME = "trx";
  private static final String OWNER_ADDRESS =
      Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049150";
  private static final String TO_ADDRESS =
      Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a146a";
  private static final String NOT_EXIT_ADDRESS =
      Wallet.getAddressPreFixString() + "B56446E617E924805E4D6CA021D341FEF6E2013B";
  private static final long OWNER_ASSET_BALANCE = 99999;

  private static final long TOTAL_SUPPLY = 10L;
  private static final int TRX_NUM = 10;
  private static final int NUM = 1;
  private static final long START_TIME = 1;
  private static final long END_TIME = 2;
  private static final int DECAY_RATIO = 2;
  private static final int VOTE_SCORE = 2;
  private static final String DESCRIPTION = "TRX";
  private static final String URL = "https://tron.network";

  static {
    Args.setParam(new String[]{"-d", dbPath}, Constant.TEST_CONF);
    context = new AnnotationConfigApplicationContext(DefaultConfig.class);
  }

  /**
   * Init data.
   */
  @BeforeClass
  public static void init() {
    Args.setParam(new String[]{"--output-directory", dbPath}, "config-junit.conf");
    dbManager = context.getBean(Manager.class);
    //    dbManager = new Manager();
    //    dbManager.init();
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
            AccountType.AssetIssue);
    ownerCapsule.addAsset(ASSET_NAME, OWNER_ASSET_BALANCE);

    AccountCapsule toAccountCapsule =
        new AccountCapsule(
            ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)),
            ByteString.copyFromUtf8("toAccount"),
            AccountType.Normal);
    AssetIssueContract assetIssueContract =
        AssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setName(ByteString.copyFrom(ByteArray.fromString(ASSET_NAME)))
            .setTotalSupply(TOTAL_SUPPLY)
            .setTrxNum(TRX_NUM)
            .setNum(NUM)
            .setStartTime(START_TIME)
            .setEndTime(END_TIME)
            .setDecayRatio(DECAY_RATIO)
            .setVoteScore(VOTE_SCORE)
            .setDescription(ByteString.copyFrom(ByteArray.fromString(DESCRIPTION)))
            .setUrl(ByteString.copyFrom(ByteArray.fromString(URL)))
            .build();
    AssetIssueCapsule assetIssueCapsule = new AssetIssueCapsule(assetIssueContract);
    dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);
    dbManager.getAccountStore().put(toAccountCapsule.getAddress().toByteArray(), toAccountCapsule);
    dbManager
        .getAssetIssueStore()
        .put(assetIssueCapsule.getName().toByteArray(), assetIssueCapsule);
  }

  private Any getContract(long sendCoin) {
    return Any.pack(
        Contract.TransferAssetContract.newBuilder()
            .setAssetName(ByteString.copyFrom(ByteArray.fromString(ASSET_NAME)))
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)))
            .setAmount(sendCoin)
            .build());
  }

  /**
   * Unit test.
   */
  private Any getContract(long sendCoin, String assetName) {
    return Any.pack(
        Contract.TransferAssetContract.newBuilder()
            .setAssetName(ByteString.copyFrom(ByteArray.fromString(assetName)))
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)))
            .setAmount(sendCoin)
            .build());
  }

  private Any getContract(long sendCoin, String owner, String to) {
    return Any.pack(
        Contract.TransferAssetContract.newBuilder()
            .setAssetName(ByteString.copyFrom(ByteArray.fromString(ASSET_NAME)))
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(owner)))
            .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(to)))
            .setAmount(sendCoin)
            .build());
  }

  /**
   * Unit test.
   */
  @Test
  public void rightTransfer() {
    TransferAssetActuator actuator = new TransferAssetActuator(getContract(100L), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount =
          dbManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));
      Assert.assertEquals(owner.getInstance().getAssetMap().get(ASSET_NAME).longValue(),
          OWNER_ASSET_BALANCE - 100);
      Assert.assertEquals(toAccount.getInstance().getAssetMap().get(ASSET_NAME).longValue(), 100L);
      Assert.assertTrue(true);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * Unit test.
   */
  @Test
  public void perfectTransfer() {
    TransferAssetActuator actuator = new TransferAssetActuator(getContract(OWNER_ASSET_BALANCE),
        dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount =
          dbManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));
      Assert.assertEquals(owner.getInstance().getAssetMap().get(ASSET_NAME).longValue(), 0L);
      Assert.assertEquals(
          toAccount.getInstance().getAssetMap().get(ASSET_NAME).longValue(), OWNER_ASSET_BALANCE);
      Assert.assertTrue(true);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * Unit test.
   */
  @Test
  public void notEnoughAssetTest() {
    TransferAssetActuator actuator = new TransferAssetActuator(getContract(OWNER_ASSET_BALANCE + 1),
        dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertTrue("assetBalance is not sufficient.".equals(e.getMessage()));
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount =
          dbManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));
      Assert.assertEquals(owner.getAssetMap().get(ASSET_NAME).longValue(), OWNER_ASSET_BALANCE);
      Assert.assertTrue(isNullOrZero(toAccount.getAssetMap().get(ASSET_NAME)));
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void zeroAmountTest() {
    TransferAssetActuator actuator = new TransferAssetActuator(getContract(0), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertTrue("Amount must greater than 0.".equals(e.getMessage()));
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount =
          dbManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));
      Assert.assertEquals(owner.getAssetMap().get(ASSET_NAME).longValue(), OWNER_ASSET_BALANCE);
      Assert.assertTrue(isNullOrZero(toAccount.getAssetMap().get(ASSET_NAME)));
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void negativeAmountTest() {
    TransferAssetActuator actuator = new TransferAssetActuator(getContract(-999), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertTrue("Amount must greater than 0.".equals(e.getMessage()));
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount =
          dbManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));
      Assert.assertEquals(owner.getAssetMap().get(ASSET_NAME).longValue(), OWNER_ASSET_BALANCE);
      Assert.assertTrue(isNullOrZero(toAccount.getAssetMap().get(ASSET_NAME)));
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void noneExistAssetTest() {
    TransferAssetActuator actuator = new TransferAssetActuator(getContract(1, "TTTTTTTTTTTT"),
        dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertTrue("No asset !".equals(e.getMessage()));
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount =
          dbManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));
      Assert.assertEquals(owner.getAssetMap().get(ASSET_NAME).longValue(), OWNER_ASSET_BALANCE);
      Assert.assertTrue(isNullOrZero(toAccount.getAssetMap().get(ASSET_NAME)));
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void noExitOwnerAccount() {
    TransferAssetActuator actuator = new TransferAssetActuator(
        getContract(100L, NOT_EXIT_ADDRESS, TO_ADDRESS), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("Validate TransferContract error, no OwnerAccount.");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("No owner account!", e.getMessage());
      AccountCapsule owner = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(TO_ADDRESS));
      Assert.assertEquals(owner.getAssetMap().get(ASSET_NAME).longValue(), OWNER_ASSET_BALANCE);
      Assert.assertTrue(isNullOrZero(toAccount.getAssetMap().get(ASSET_NAME)));
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  /**
   * If to account not exit, creat it.
   */
  public void noExitToAccount() {
    TransferAssetActuator actuator = new TransferAssetActuator(
        getContract(100L, OWNER_ADDRESS, NOT_EXIT_ADDRESS), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      AccountCapsule noExitAccount = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(NOT_EXIT_ADDRESS));
      Assert.assertTrue(null == noExitAccount);
      actuator.validate();
      noExitAccount = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(NOT_EXIT_ADDRESS));
      Assert.assertFalse(null == noExitAccount);    //Had created.
      Assert.assertEquals(noExitAccount.getBalance(), 0);
      actuator.execute(ret);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("To account is not exit!", e.getMessage());
      AccountCapsule owner = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));
      Assert
          .assertEquals(owner.getAssetMap().get(ASSET_NAME).longValue(), OWNER_ASSET_BALANCE);
      AccountCapsule noExitAccount = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(NOT_EXIT_ADDRESS));
      Assert.assertTrue(noExitAccount == null);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

  }

  @Test
  public void addOverflowTest() {
    // First, increase the to balance. Else can't complete this test case.
    AccountCapsule toAccount = dbManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));
    toAccount.addAsset(ASSET_NAME, Long.MAX_VALUE);
    dbManager.getAccountStore().put(ByteArray.fromHexString(TO_ADDRESS), toAccount);
    TransferAssetActuator actuator = new TransferAssetActuator(getContract(1), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertTrue("long overflow".equals(e.getMessage()));
      AccountCapsule owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      toAccount =
          dbManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));
      Assert.assertEquals(owner.getAssetMap().get(ASSET_NAME).longValue(), OWNER_ASSET_BALANCE);
      Assert.assertEquals(toAccount.getAssetMap().get(ASSET_NAME).longValue(), Long.MAX_VALUE);
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
}
