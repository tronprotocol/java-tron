package org.tron.core.actuator;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.utils.ByteArray;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Transaction.Result.code;
import org.tron.protos.contract.AssetIssueContractOuterClass;
import org.tron.protos.contract.AssetIssueContractOuterClass.AssetIssueContract;
import org.tron.protos.contract.AssetIssueContractOuterClass.ParticipateAssetIssueContract;

public class ParticipateAssetIssueActuatorTest extends BaseTest {

  private static final String OWNER_ADDRESS;
  private static final String TO_ADDRESS;
  private static final String TO_ADDRESS_2;
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
  private static long AMOUNT = TOTAL_SUPPLY - (1000L) / TRX_NUM * NUM;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath()}, Constant.TEST_CONF);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1234";
    TO_ADDRESS = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
    TO_ADDRESS_2 = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e048892";
    THIRD_ADDRESS = Wallet.getAddressPreFixString() + "4948c2e8a756d9437037dcd8c7e0c73d560ca38d";
    NOT_EXIT_ADDRESS = Wallet.getAddressPreFixString() + "B56446E617E924805E4D6CA021D341FEF6E2013B";
  }

  /**
   * create temp Capsule test need.
   */
  @Before
  public void createCapsule() {
    chainBaseManager.getDynamicPropertiesStore().saveTokenIdNum(1000000);

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
    AccountCapsule toAccountCapsule2 =
        new AccountCapsule(
            ByteString.copyFromUtf8("toAccount2"),
            ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS_2)),
            AccountType.Normal,
            TO_BALANCE);

    chainBaseManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);
    chainBaseManager.getAccountStore().put(toAccountCapsule.getAddress().toByteArray(),
        toAccountCapsule);
    chainBaseManager.getAccountStore()
        .put(toAccountCapsule2.getAddress().toByteArray(), toAccountCapsule2);
    chainBaseManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
  }

  private boolean isNullOrZero(Long value) {
    if (null == value || value == 0) {
      return true;
    }
    return false;
  }

  private Any getContract(long count) {
    String assertName = ASSET_NAME;
    if (chainBaseManager.getDynamicPropertiesStore().getAllowSameTokenName() == 1) {
      long tokenIdNum = chainBaseManager.getDynamicPropertiesStore().getTokenIdNum();
      assertName = String.valueOf(tokenIdNum);
    }

    return Any.pack(
        ParticipateAssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)))
            .setAssetName(ByteString.copyFrom(ByteArray.fromString(assertName)))
            .setAmount(count)
            .build());
  }

  private Any getContract(long count, String assetName) {
    return Any.pack(
        ParticipateAssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)))
            .setAssetName(ByteString.copyFrom(ByteArray.fromString(assetName)))
            .setAmount(count)
            .build());
  }

  private Any getContract(long count, ByteString assetName) {
    return Any.pack(
        ParticipateAssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)))
            .setAssetName(assetName)
            .setAmount(count)
            .build());
  }

  private Any getContractWithOwner(long count, String ownerAddress) {
    String assertName = ASSET_NAME;
    if (chainBaseManager.getDynamicPropertiesStore().getAllowSameTokenName() == 1) {
      long tokenIdNum = chainBaseManager.getDynamicPropertiesStore().getTokenIdNum();
      assertName = String.valueOf(tokenIdNum);
    }

    return Any.pack(
        ParticipateAssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
            .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)))
            .setAssetName(ByteString.copyFrom(ByteArray.fromString(assertName)))
            .setAmount(count)
            .build());
  }

  private Any getContractWithTo(long count, String toAddress) {
    String assertName = ASSET_NAME;
    if (chainBaseManager.getDynamicPropertiesStore().getAllowSameTokenName() == 1) {
      long tokenIdNum = chainBaseManager.getDynamicPropertiesStore().getTokenIdNum();
      assertName = String.valueOf(tokenIdNum);
    }
    return Any.pack(
        ParticipateAssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(toAddress)))
            .setAssetName(ByteString.copyFrom(ByteArray.fromString(assertName)))
            .setAmount(count)
            .build());
  }

  private void initAssetIssue(long startTimestmp, long endTimestmp) {
    long id = chainBaseManager.getDynamicPropertiesStore().getTokenIdNum() + 1;
    chainBaseManager.getDynamicPropertiesStore().saveTokenIdNum(id);
    System.out.println("id:" + id);
    AssetIssueContract assetIssueContract =
        AssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)))
            .setName(ByteString.copyFrom(ByteArray.fromString(ASSET_NAME)))
            .setId(Long.toString(id))
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
    AccountCapsule toAccountCapsule = chainBaseManager.getAccountStore()
        .get(ByteArray.fromHexString(TO_ADDRESS));
    if (chainBaseManager.getDynamicPropertiesStore().getAllowSameTokenName() == 0) {
      chainBaseManager.getAssetIssueStore().put(assetIssueCapsule.createDbKey(),
          assetIssueCapsule);
      assetIssueCapsule.setPrecision(0);
      chainBaseManager.getAssetIssueV2Store().put(assetIssueCapsule.createDbV2Key(),
          assetIssueCapsule);
      toAccountCapsule.addAsset(ASSET_NAME.getBytes(), TOTAL_SUPPLY);
      toAccountCapsule.addAssetV2(ByteArray.fromString(String.valueOf(id)), TOTAL_SUPPLY);
    } else {
      chainBaseManager.getAssetIssueV2Store().put(assetIssueCapsule.createDbV2Key(),
          assetIssueCapsule);
      toAccountCapsule.addAssetV2(ByteArray.fromString(String.valueOf(id)), TOTAL_SUPPLY);
    }

    chainBaseManager.getAccountStore().put(toAccountCapsule.getAddress().toByteArray(),
        toAccountCapsule);
  }

  private void initAssetIssue(long startTimestmp, long endTimestmp, String assetName) {
    long id = chainBaseManager.getDynamicPropertiesStore().getTokenIdNum() + 1;
    chainBaseManager.getDynamicPropertiesStore().saveTokenIdNum(id);

    AssetIssueContract assetIssueContract =
        AssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)))
            .setName(ByteString.copyFrom(ByteArray.fromString(assetName)))
            .setId(Long.toString(id))
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
    AccountCapsule toAccountCapsule = chainBaseManager.getAccountStore()
        .get(ByteArray.fromHexString(TO_ADDRESS));
    if (chainBaseManager.getDynamicPropertiesStore().getAllowSameTokenName() == 0) {
      chainBaseManager.getAssetIssueStore().put(assetIssueCapsule.createDbKey(), assetIssueCapsule);
      assetIssueCapsule.setPrecision(0);
      chainBaseManager.getAssetIssueV2Store().put(assetIssueCapsule.createDbV2Key(),
          assetIssueCapsule);

      toAccountCapsule.addAsset(assetName.getBytes(), TOTAL_SUPPLY);
      toAccountCapsule.addAssetV2(ByteArray.fromString(String.valueOf(id)), TOTAL_SUPPLY);
    } else {
      chainBaseManager.getAssetIssueV2Store().put(assetIssueCapsule.createDbV2Key(),
          assetIssueCapsule);
      toAccountCapsule.addAssetV2(ByteArray.fromString(String.valueOf(id)), TOTAL_SUPPLY);
    }

    chainBaseManager.getAccountStore().put(toAccountCapsule.getAddress().toByteArray(),
        toAccountCapsule);
  }

  private void initAssetIssueWithOwner(long startTimestmp, long endTimestmp, String owner) {
    long id = chainBaseManager.getDynamicPropertiesStore().getTokenIdNum() + 1;
    chainBaseManager.getDynamicPropertiesStore().saveTokenIdNum(id);

    AssetIssueContract assetIssueContract =
        AssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(owner)))
            .setName(ByteString.copyFrom(ByteArray.fromString(ASSET_NAME)))
            .setTotalSupply(TOTAL_SUPPLY)
            .setTrxNum(TRX_NUM)
            .setId(Long.toString(id))
            .setNum(NUM)
            .setStartTime(startTimestmp)
            .setEndTime(endTimestmp)
            .setVoteScore(VOTE_SCORE)
            .setDescription(ByteString.copyFrom(ByteArray.fromString(DESCRIPTION)))
            .setUrl(ByteString.copyFrom(ByteArray.fromString(URL)))
            .build();
    AssetIssueCapsule assetIssueCapsule = new AssetIssueCapsule(assetIssueContract);
    AccountCapsule toAccountCapsule = chainBaseManager.getAccountStore()
        .get(ByteArray.fromHexString(TO_ADDRESS));
    if (chainBaseManager.getDynamicPropertiesStore().getAllowSameTokenName() == 0) {
      chainBaseManager.getAssetIssueStore().put(assetIssueCapsule.createDbKey(),
          assetIssueCapsule);
      assetIssueCapsule.setPrecision(0);
      chainBaseManager.getAssetIssueV2Store().put(assetIssueCapsule.createDbV2Key(),
          assetIssueCapsule);
      toAccountCapsule.addAsset(ASSET_NAME.getBytes(), TOTAL_SUPPLY);
      toAccountCapsule.addAssetV2(ByteArray.fromString(String.valueOf(id)), TOTAL_SUPPLY);
    } else {
      chainBaseManager.getAssetIssueV2Store().put(assetIssueCapsule.createDbV2Key(),
          assetIssueCapsule);
      toAccountCapsule.addAssetV2(ByteArray.fromString(String.valueOf(id)), TOTAL_SUPPLY);
    }

    chainBaseManager.getAccountStore().put(toAccountCapsule.getAddress().toByteArray(),
        toAccountCapsule);
  }

  /**
   * SameTokenName close, success participate assert
   */
  @Test
  public void sameTokenNameCloseRightAssetIssue() {
    chainBaseManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    initAssetIssue(chainBaseManager.getDynamicPropertiesStore()
            .getLatestBlockHeaderTimestamp() - 1000,
        chainBaseManager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp() + 1000);
    ParticipateAssetIssueActuator actuator = new ParticipateAssetIssueActuator();
    actuator.setChainBaseManager(chainBaseManager).setAny(getContract(1000L));
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule owner = chainBaseManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount = chainBaseManager.getAccountStore()
          .get(ByteArray.fromHexString(TO_ADDRESS));
      Assert.assertEquals(owner.getBalance(), OWNER_BALANCE - 1000);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE + 1000);
      //V1
      Assert.assertEquals(owner.getAssetMapForTest().get(ASSET_NAME).longValue(),
          (1000L) / TRX_NUM * NUM);

      Assert.assertEquals(toAccount.getAssetMapForTest().get(ASSET_NAME).longValue(), AMOUNT);
      //V2
      long tokenId = chainBaseManager.getDynamicPropertiesStore().getTokenIdNum();
      Assert.assertEquals(owner.getAssetV2MapForTest().get(String.valueOf(tokenId)).longValue(),
          (1000L) / TRX_NUM * NUM);
      Assert.assertEquals(toAccount.getAssetV2MapForTest()
                      .get(String.valueOf(tokenId)).longValue(), AMOUNT);

    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }


  /**
   * Init close SameTokenName,after init data,open SameTokenName
   */
  @Test
  public void OldNotUpdateSuccessAssetIssue() {
    chainBaseManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    initAssetIssue(chainBaseManager
            .getDynamicPropertiesStore().getLatestBlockHeaderTimestamp() - 1000,
        chainBaseManager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp() + 1000);
    chainBaseManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    ParticipateAssetIssueActuator actuator = new ParticipateAssetIssueActuator();
    actuator.setChainBaseManager(chainBaseManager).setAny(getContract(1000L));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule owner = chainBaseManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount = chainBaseManager.getAccountStore()
          .get(ByteArray.fromHexString(TO_ADDRESS));
      Assert.assertEquals(owner.getBalance(), OWNER_BALANCE - 1000);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE + 1000);
      //V1 data not update
      Assert.assertNull(owner.getAssetMapForTest().get(ASSET_NAME));
      Assert.assertEquals(toAccount.getAssetMapForTest().get(ASSET_NAME).longValue(), TOTAL_SUPPLY);
      //V2
      long tokenId = chainBaseManager.getDynamicPropertiesStore().getTokenIdNum();
      Assert.assertEquals(owner.getAssetV2MapForTest().get(String.valueOf(tokenId)).longValue(),
          (1000L) / TRX_NUM * NUM);
      Assert.assertEquals(toAccount.getAssetV2MapForTest()
                      .get(String.valueOf(tokenId)).longValue(), AMOUNT);

    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * SameTokenName open, success participate assert
   */
  @Test
  public void sameTokenNameOpenRightAssetIssue() {
    chainBaseManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    initAssetIssue(
        chainBaseManager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp() - 1000,
        chainBaseManager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp() + 1000);
    ParticipateAssetIssueActuator actuator = new ParticipateAssetIssueActuator();
    actuator.setChainBaseManager(chainBaseManager).setAny(getContract(1000L));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule owner =
          chainBaseManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount =
          chainBaseManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));
      Assert.assertEquals(owner.getBalance(), OWNER_BALANCE - 1000);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE + 1000);
      // V1, data is not exist
      Assert.assertNull(owner.getAssetMapForTest().get(ASSET_NAME));
      Assert.assertNull(toAccount.getAssetMapForTest().get(ASSET_NAME));
      //V2
      long id = chainBaseManager.getDynamicPropertiesStore().getTokenIdNum();
      Assert.assertEquals(owner.getAssetV2MapForTest()
                      .get(String.valueOf(id)).longValue(), (1000L) / TRX_NUM * NUM);
      Assert.assertEquals(
          toAccount.getAssetV2MapForTest().get(String.valueOf(id)).longValue(),
          TOTAL_SUPPLY - (1000L) / TRX_NUM * NUM);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * SameTokenName close, check asset start time and end time
   */
  @Test
  public void sameTokenNameCloseAssetIssueTimeRight() {
    DateTime now = DateTime.now();
    initAssetIssue(now.minusDays(1).getMillis(), now.getMillis());
    ParticipateAssetIssueActuator actuator = new ParticipateAssetIssueActuator();
    actuator.setChainBaseManager(chainBaseManager).setAny(getContract(1000L));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertTrue("No longer valid period!".equals(e.getMessage()));
      AccountCapsule owner =
          chainBaseManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount =
          chainBaseManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));

      Assert.assertEquals(owner.getBalance(), OWNER_BALANCE);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE);
      Assert.assertTrue(isNullOrZero(owner.getAssetMapForTest().get(ASSET_NAME)));
      Assert.assertEquals(toAccount.getAssetMapForTest().get(ASSET_NAME).longValue(), TOTAL_SUPPLY);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * SameTokenName open, check asset start time and end time
   */
  @Test
  public void sameTokenNameOpenAssetIssueTimeRight() {
    chainBaseManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    DateTime now = DateTime.now();
    initAssetIssue(now.minusDays(1).getMillis(), now.getMillis());
    ParticipateAssetIssueActuator actuator = new ParticipateAssetIssueActuator();
    actuator.setChainBaseManager(chainBaseManager).setAny(getContract(1000L));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertTrue("No longer valid period!".equals(e.getMessage()));
      AccountCapsule owner =
          chainBaseManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount =
          chainBaseManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));

      Assert.assertEquals(owner.getBalance(), OWNER_BALANCE);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE);
      long id = chainBaseManager.getDynamicPropertiesStore().getTokenIdNum();
      Assert.assertTrue(isNullOrZero(owner.getAssetV2MapForTest().get(String.valueOf(id))));
      Assert.assertEquals(toAccount.getAssetV2MapForTest().get(String.valueOf(id)).longValue(),
          TOTAL_SUPPLY);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * SameTokenName close, check asset left time
   */
  @Test
  public void sameTokenNameCloseAssetIssueTimeLeft() {
    DateTime now = DateTime.now();
    initAssetIssue(now.minusDays(1).getMillis(), now.getMillis());
    ParticipateAssetIssueActuator actuator = new ParticipateAssetIssueActuator();
    actuator.setChainBaseManager(chainBaseManager).setAny(getContract(1000L));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertTrue("No longer valid period!".equals(e.getMessage()));
      AccountCapsule owner =
          chainBaseManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount =
          chainBaseManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));

      Assert.assertEquals(owner.getBalance(), OWNER_BALANCE);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE);
      Assert.assertTrue(isNullOrZero(owner.getAssetMapForTest().get(ASSET_NAME)));
      Assert.assertEquals(toAccount.getAssetMapForTest()
              .get(ASSET_NAME).longValue(), TOTAL_SUPPLY);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * SameTokenName open, check asset left time
   */
  @Test
  public void sameTokenNameOpenAssetIssueTimeLeft() {
    chainBaseManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    DateTime now = DateTime.now();
    initAssetIssue(now.minusDays(1).getMillis(), now.getMillis());
    ParticipateAssetIssueActuator actuator = new ParticipateAssetIssueActuator();
    actuator.setChainBaseManager(chainBaseManager).setAny(getContract(1000L));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertTrue("No longer valid period!".equals(e.getMessage()));
      AccountCapsule owner =
          chainBaseManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount =
          chainBaseManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));

      Assert.assertEquals(owner.getBalance(), OWNER_BALANCE);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE);
      long id = chainBaseManager.getDynamicPropertiesStore().getTokenIdNum();
      Assert.assertTrue(isNullOrZero(owner.getAssetV2MapForTest().get(String.valueOf(id))));
      Assert.assertEquals(toAccount.getAssetV2MapForTest()
                      .get(String.valueOf(id)).longValue(), TOTAL_SUPPLY);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * SameTokenName close, exchange devisible
   */
  @Test
  public void sameTokenNameCloseExchangeDevisibleTest() {
    initAssetIssue(chainBaseManager.getDynamicPropertiesStore()
            .getLatestBlockHeaderTimestamp() - 1000,
        chainBaseManager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp() + 1000);
    ParticipateAssetIssueActuator actuator = new ParticipateAssetIssueActuator(); //no problem
    actuator.setChainBaseManager(chainBaseManager).setAny(getContract(999L));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);

      AccountCapsule owner =
          chainBaseManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount =
          chainBaseManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));

      Assert.assertEquals(owner.getAssetMapForTest().get(ASSET_NAME).longValue(),
          (999L * NUM) / TRX_NUM);
      Assert.assertEquals(
          toAccount.getAssetMapForTest().get(ASSET_NAME).longValue(),
          TOTAL_SUPPLY - (999L * NUM) / TRX_NUM);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * SameTokenName open, exchange devisible
   */
  @Test
  public void sameTokenNameOpenExchangeDevisibleTest() {
    chainBaseManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    initAssetIssue(
        chainBaseManager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp() - 1000,
        chainBaseManager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp() + 1000);
    ParticipateAssetIssueActuator actuator = new ParticipateAssetIssueActuator(); //no problem
    actuator.setChainBaseManager(chainBaseManager).setAny(getContract(999L));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);

      AccountCapsule owner =
          chainBaseManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount =
          chainBaseManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));
      long id = chainBaseManager.getDynamicPropertiesStore().getTokenIdNum();
      Assert.assertEquals(owner.getAssetV2MapForTest().get(String.valueOf(id)).longValue(),
          (999L * NUM) / TRX_NUM);
      Assert.assertEquals(
          toAccount.getAssetV2MapForTest().get(String.valueOf(id)).longValue(),
          TOTAL_SUPPLY - (999L * NUM) / TRX_NUM);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * SameTokenName close, nagative amount
   */
  @Test
  public void sameTokenNameCloseNegativeAmountTest() {
    DateTime now = DateTime.now();
    initAssetIssue(now.minusDays(1).getMillis(), now.plusDays(1).getMillis());
    ParticipateAssetIssueActuator actuator = new ParticipateAssetIssueActuator();
    actuator.setChainBaseManager(chainBaseManager).setAny(getContract(-999L));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertTrue("Amount must greater than 0!".equals(e.getMessage()));

      AccountCapsule owner =
          chainBaseManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount =
          chainBaseManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));

      Assert.assertEquals(owner.getBalance(), OWNER_BALANCE);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE);
      Assert.assertTrue(isNullOrZero(owner.getAssetMapForTest().get(ASSET_NAME)));
      Assert.assertEquals(toAccount.getAssetMapForTest().get(ASSET_NAME).longValue(), TOTAL_SUPPLY);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * SameTokenName open, nagative amount
   */
  @Test
  public void sameTokenNameOpenNegativeAmountTest() {
    chainBaseManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    DateTime now = DateTime.now();
    initAssetIssue(now.minusDays(1).getMillis(), now.plusDays(1).getMillis());
    ParticipateAssetIssueActuator actuator = new ParticipateAssetIssueActuator();
    actuator.setChainBaseManager(chainBaseManager).setAny(getContract(-999L));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertTrue("Amount must greater than 0!".equals(e.getMessage()));

      AccountCapsule owner =
          chainBaseManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount =
          chainBaseManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));

      Assert.assertEquals(owner.getBalance(), OWNER_BALANCE);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE);
      long id = chainBaseManager.getDynamicPropertiesStore().getTokenIdNum();

      Assert.assertTrue(isNullOrZero(owner.getAssetV2MapForTest().get(String.valueOf(id))));
      Assert.assertEquals(toAccount.getAssetV2MapForTest()
                      .get(String.valueOf(id)).longValue(), TOTAL_SUPPLY);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * SameTokenName close, zere amount
   */
  @Test
  public void sameTokenNameCloseZeroAmountTest() {
    DateTime now = DateTime.now();
    initAssetIssue(now.minusDays(1).getMillis(), now.plusDays(1).getMillis());
    ParticipateAssetIssueActuator actuator = new ParticipateAssetIssueActuator();
    actuator.setChainBaseManager(chainBaseManager).setAny(getContract(0));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertTrue("Amount must greater than 0!".equals(e.getMessage()));

      AccountCapsule owner =
          chainBaseManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount =
          chainBaseManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));

      Assert.assertEquals(owner.getBalance(), OWNER_BALANCE);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE);
      Assert.assertTrue(isNullOrZero(owner.getAssetMapForTest().get(ASSET_NAME)));
      Assert.assertEquals(toAccount.getAssetMapForTest().get(ASSET_NAME).longValue(), TOTAL_SUPPLY);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * SameTokenName open, zere amount
   */
  @Test
  public void sameTokenNameOpenZeroAmountTest() {
    chainBaseManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    DateTime now = DateTime.now();
    initAssetIssue(now.minusDays(1).getMillis(), now.plusDays(1).getMillis());
    ParticipateAssetIssueActuator actuator = new ParticipateAssetIssueActuator();
    actuator.setChainBaseManager(chainBaseManager).setAny(getContract(0));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertTrue("Amount must greater than 0!".equals(e.getMessage()));

      AccountCapsule owner =
          chainBaseManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount =
          chainBaseManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));

      Assert.assertEquals(owner.getBalance(), OWNER_BALANCE);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE);
      long id = chainBaseManager.getDynamicPropertiesStore().getTokenIdNum();

      Assert.assertTrue(isNullOrZero(owner.getAssetV2MapForTest().get(String.valueOf(id))));
      Assert.assertEquals(toAccount.getAssetV2MapForTest()
                      .get(String.valueOf(id)).longValue(), TOTAL_SUPPLY);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }


  /**
   * SameTokenName close, Owner account is not exit
   */
  @Test
  public void sameTokenNameCloseNoExitOwnerTest() {
    DateTime now = DateTime.now();
    initAssetIssue(now.minusDays(1).getMillis(), now.plusDays(1).getMillis());
    ParticipateAssetIssueActuator actuator = new ParticipateAssetIssueActuator();
    actuator.setChainBaseManager(chainBaseManager)
        .setAny(getContractWithOwner(101, NOT_EXIT_ADDRESS));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Account does not exist!", e.getMessage());

      AccountCapsule owner =
          chainBaseManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount =
          chainBaseManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));

      Assert.assertEquals(owner.getBalance(), OWNER_BALANCE);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE);
      Assert.assertTrue(isNullOrZero(owner.getAssetMapForTest().get(ASSET_NAME)));
      Assert.assertEquals(toAccount.getAssetMapForTest().get(ASSET_NAME).longValue(), TOTAL_SUPPLY);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }


  /**
   * SameTokenName open, Owner account is not exit
   */
  @Test
  public void sameTokenNameOpenNoExitOwnerTest() {
    chainBaseManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    DateTime now = DateTime.now();
    initAssetIssue(now.minusDays(1).getMillis(), now.plusDays(1).getMillis());
    ParticipateAssetIssueActuator actuator = new ParticipateAssetIssueActuator();
    actuator.setChainBaseManager(chainBaseManager)
        .setAny(getContractWithOwner(101, NOT_EXIT_ADDRESS));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Account does not exist!", e.getMessage());

      AccountCapsule owner =
          chainBaseManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount =
          chainBaseManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));

      Assert.assertEquals(owner.getBalance(), OWNER_BALANCE);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE);
      long id = chainBaseManager.getDynamicPropertiesStore().getTokenIdNum();
      Assert.assertTrue(isNullOrZero(owner.getAssetV2MapForTest().get(String.valueOf(id))));
      Assert.assertEquals(toAccount.getAssetV2MapForTest().get(String.valueOf(id)).longValue(),
          TOTAL_SUPPLY);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * SameTokenName close, To account is not exit.
   */
  @Test
  public void sameTokenNameCloseNoExitToTest() {
    initAssetIssueWithOwner(
        chainBaseManager.getDynamicPropertiesStore()
            .getLatestBlockHeaderTimestamp() - 1000,
        chainBaseManager.getDynamicPropertiesStore()
            .getLatestBlockHeaderTimestamp() + 1000,
        NOT_EXIT_ADDRESS);
    ParticipateAssetIssueActuator actuator = new ParticipateAssetIssueActuator();
    actuator.setChainBaseManager(chainBaseManager)
        .setAny(getContractWithTo(101, NOT_EXIT_ADDRESS));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("To account does not exist!", e.getMessage());

      AccountCapsule owner =
          chainBaseManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount =
          chainBaseManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));

      Assert.assertEquals(owner.getBalance(), OWNER_BALANCE);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE);
      Assert.assertTrue(isNullOrZero(owner.getAssetMapForTest().get(ASSET_NAME)));
      Assert.assertEquals(toAccount.getAssetMapForTest().get(ASSET_NAME).longValue(), TOTAL_SUPPLY);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }


  /**
   * SameTokenName open, To account is not exit.
   */
  @Test
  public void sameTokenNameOpenNoExitToTest() {
    chainBaseManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    initAssetIssueWithOwner(
        chainBaseManager.getDynamicPropertiesStore()
            .getLatestBlockHeaderTimestamp() - 1000,
        chainBaseManager.getDynamicPropertiesStore()
            .getLatestBlockHeaderTimestamp() + 1000,
        NOT_EXIT_ADDRESS);
    ParticipateAssetIssueActuator actuator = new ParticipateAssetIssueActuator();
    actuator.setChainBaseManager(chainBaseManager)
        .setAny(getContractWithTo(101, NOT_EXIT_ADDRESS));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("To account does not exist!", e.getMessage());

      AccountCapsule owner =
          chainBaseManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount =
          chainBaseManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));

      Assert.assertEquals(owner.getBalance(), OWNER_BALANCE);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE);
      long id = chainBaseManager.getDynamicPropertiesStore().getTokenIdNum();
      Assert.assertTrue(isNullOrZero(owner.getAssetV2MapForTest().get(String.valueOf(id))));
      Assert.assertEquals(toAccount.getAssetV2MapForTest().get(String.valueOf(id)).longValue(),
          TOTAL_SUPPLY);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  /**
   * SameTokenName close, Participate to self, will throw exception.
   */
  public void sameTokenNameCloseParticipateAssetSelf() {
    initAssetIssueWithOwner(
        chainBaseManager.getDynamicPropertiesStore()
            .getLatestBlockHeaderTimestamp() - 1000,
        chainBaseManager.getDynamicPropertiesStore()
            .getLatestBlockHeaderTimestamp() + 1000,
        OWNER_ADDRESS);
    ParticipateAssetIssueActuator actuator = new ParticipateAssetIssueActuator();
    actuator.setChainBaseManager(chainBaseManager)
        .setAny(getContractWithTo(101, OWNER_ADDRESS));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Cannot participate asset Issue yourself !", e.getMessage());

      AccountCapsule owner =
          chainBaseManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount =
          chainBaseManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));

      Assert.assertEquals(owner.getBalance(), OWNER_BALANCE);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE);
      Assert.assertTrue(isNullOrZero(owner.getAssetMapForTest().get(ASSET_NAME)));
      Assert.assertEquals(toAccount.getAssetMapForTest().get(ASSET_NAME).longValue(), TOTAL_SUPPLY);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  /**
   * SameTokenName open, Participate to self, will throw exception.
   */
  public void sameTokenNameOpenParticipateAssetSelf() {
    chainBaseManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    initAssetIssueWithOwner(
        chainBaseManager.getDynamicPropertiesStore()
            .getLatestBlockHeaderTimestamp() - 1000,
        chainBaseManager.getDynamicPropertiesStore()
            .getLatestBlockHeaderTimestamp() + 1000,
        OWNER_ADDRESS);
    ParticipateAssetIssueActuator actuator = new ParticipateAssetIssueActuator();
    actuator.setChainBaseManager(chainBaseManager)
        .setAny(getContractWithTo(101, OWNER_ADDRESS));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Cannot participate asset Issue yourself !", e.getMessage());

      AccountCapsule owner =
          chainBaseManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount =
          chainBaseManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));

      Assert.assertEquals(owner.getBalance(), OWNER_BALANCE);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE);
      long id = chainBaseManager.getDynamicPropertiesStore().getTokenIdNum();
      Assert.assertTrue(isNullOrZero(owner.getAssetV2MapForTest().get(String.valueOf(id))));
      Assert.assertEquals(toAccount.getAssetV2MapForTest().get(String.valueOf(id)).longValue(),
          TOTAL_SUPPLY);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  /**
   * SameTokenName close, Participate to the third party that not the issuer, will throw exception.
   */
  public void sameTokenNameCloseParticipateAssetToThird() {
    initAssetIssue(chainBaseManager.getDynamicPropertiesStore()
            .getLatestBlockHeaderTimestamp() - 1000,
        chainBaseManager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp() + 1000);
    ParticipateAssetIssueActuator actuator = new ParticipateAssetIssueActuator();
    actuator.setChainBaseManager(chainBaseManager)
        .setAny(getContractWithTo(101, THIRD_ADDRESS));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("The asset is not issued by " + THIRD_ADDRESS, e.getMessage());

      AccountCapsule owner =
          chainBaseManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount =
          chainBaseManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));

      Assert.assertEquals(owner.getBalance(), OWNER_BALANCE);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE);
      Assert.assertTrue(isNullOrZero(owner.getAssetMapForTest().get(ASSET_NAME)));
      Assert.assertEquals(toAccount.getAssetMapForTest().get(ASSET_NAME).longValue(), TOTAL_SUPPLY);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  /**
   * SameTokenName open, Participate to the third party that not the issuer, will throw exception.
   */
  public void sameTokenNameOpenParticipateAssetToThird() {
    chainBaseManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    initAssetIssue(chainBaseManager.getDynamicPropertiesStore()
            .getLatestBlockHeaderTimestamp() - 1000,
        chainBaseManager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp() + 1000);
    ParticipateAssetIssueActuator actuator = new ParticipateAssetIssueActuator();
    actuator.setChainBaseManager(chainBaseManager)
        .setAny(getContractWithTo(101, THIRD_ADDRESS));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("The asset is not issued by " + THIRD_ADDRESS, e.getMessage());

      AccountCapsule owner =
          chainBaseManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount =
          chainBaseManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));

      Assert.assertEquals(owner.getBalance(), OWNER_BALANCE);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE);
      long id = chainBaseManager.getDynamicPropertiesStore().getTokenIdNum();
      Assert.assertTrue(isNullOrZero(owner.getAssetV2MapForTest().get(String.valueOf(id))));
      Assert.assertEquals(toAccount.getAssetV2MapForTest().get(String.valueOf(id)).longValue(),
          TOTAL_SUPPLY);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  /*
   * Asset name length must between 1 to 32 and can not contain space and other unreadable
   * character, and can not contain chinese characters.
   */

  //asset name validation which is unnecessary has been removed!
  public void assetNameTest() {
    //Empty name, throw exception
    ByteString emptyName = ByteString.EMPTY;
    ParticipateAssetIssueActuator actuator = new ParticipateAssetIssueActuator();
    actuator.setChainBaseManager(chainBaseManager)
        .setAny(getContract(1000L, emptyName));

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

    String assetName = "testname0123456789abcdefghijgklmo";

    // 32 byte readable character just ok.
    assetName = "testname0123456789abcdefghijgklm";
    initAssetIssue(chainBaseManager.getDynamicPropertiesStore()
            .getLatestBlockHeaderTimestamp() - 1000,
        chainBaseManager.getDynamicPropertiesStore()
            .getLatestBlockHeaderTimestamp() + 1000, assetName);
    actuator = new ParticipateAssetIssueActuator();
    actuator.setChainBaseManager(chainBaseManager)
        .setAny(getContract(1000L, assetName));

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule owner =
          chainBaseManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount =
          chainBaseManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));

      Assert.assertEquals(owner.getBalance(), OWNER_BALANCE - 1000);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE + 1000);
      Assert.assertEquals(owner.getAssetMapForTest().get(assetName).longValue(),
          (1000L) / TRX_NUM * NUM);
      Assert.assertEquals(toAccount.getAssetMapForTest().get(assetName).longValue(),
          TOTAL_SUPPLY - (1000L) / TRX_NUM * NUM);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

    // 1 byte readable character ok.
    assetName = "t";
    initAssetIssue(chainBaseManager.getDynamicPropertiesStore()
            .getLatestBlockHeaderTimestamp() - 1000,
        chainBaseManager.getDynamicPropertiesStore()
            .getLatestBlockHeaderTimestamp() + 1000,
        assetName);
    actuator = new ParticipateAssetIssueActuator();
    actuator.setChainBaseManager(chainBaseManager)
        .setAny(getContract(1000L, assetName));

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule owner =
          chainBaseManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount =
          chainBaseManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));

      Assert.assertEquals(owner.getBalance(), OWNER_BALANCE - 2000);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE + 2000);
      Assert.assertEquals(owner.getAssetMapForTest().get(assetName).longValue(),
          (1000L) / TRX_NUM * NUM);
      Assert.assertEquals(toAccount.getAssetMapForTest().get(assetName).longValue(),
          TOTAL_SUPPLY - (1000L) / TRX_NUM * NUM);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * SameTokenName close, not enough trx
   */
  @Test
  public void sameTokenNameCloseNotEnoughTrxTest() {
    initAssetIssue(chainBaseManager.getDynamicPropertiesStore()
            .getLatestBlockHeaderTimestamp() - 1000,
        chainBaseManager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp() + 1000);
    // First, reduce the owner trx balance. Else can't complete this test case.
    AccountCapsule owner = chainBaseManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    owner.setBalance(100);
    chainBaseManager.getAccountStore().put(owner.getAddress().toByteArray(), owner);
    ParticipateAssetIssueActuator actuator = new ParticipateAssetIssueActuator();
    actuator.setChainBaseManager(chainBaseManager).setAny(getContract(101));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertTrue("No enough balance !".equals(e.getMessage()));

      owner =
          chainBaseManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount =
          chainBaseManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));

      Assert.assertEquals(owner.getBalance(), 100);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE);
      Assert.assertTrue(isNullOrZero(owner.getAssetMapForTest().get(ASSET_NAME)));
      Assert.assertEquals(toAccount.getAssetMapForTest().get(ASSET_NAME).longValue(), TOTAL_SUPPLY);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }


  /**
   * SameTokenName open, not enough trx
   */
  @Test
  public void sameTokenNameOpenNotEnoughTrxTest() {
    chainBaseManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    initAssetIssue(chainBaseManager.getDynamicPropertiesStore()
            .getLatestBlockHeaderTimestamp() - 1000,
        chainBaseManager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp() + 1000);
    // First, reduce the owner trx balance. Else can't complete this test case.
    AccountCapsule owner = chainBaseManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    owner.setBalance(100);
    chainBaseManager.getAccountStore().put(owner.getAddress().toByteArray(), owner);
    ParticipateAssetIssueActuator actuator = new ParticipateAssetIssueActuator();
    actuator.setChainBaseManager(chainBaseManager).setAny(getContract(101));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertTrue("No enough balance !".equals(e.getMessage()));

      owner = chainBaseManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount = chainBaseManager.getAccountStore()
          .get(ByteArray.fromHexString(TO_ADDRESS));

      Assert.assertEquals(owner.getBalance(), 100);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE);
      long id = chainBaseManager.getDynamicPropertiesStore().getTokenIdNum();
      Assert.assertTrue(isNullOrZero(owner.getAssetV2MapForTest().get(String.valueOf(id))));
      Assert.assertEquals(toAccount.getAssetV2MapForTest().get(String.valueOf(id)).longValue(),
          TOTAL_SUPPLY);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * SameTokenName close, not enough asset
   */
  @Test
  public void sameTokenNameCloseNotEnoughAssetTest() {
    initAssetIssue(chainBaseManager.getDynamicPropertiesStore()
            .getLatestBlockHeaderTimestamp() - 1000,
        chainBaseManager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp() + 1000);
    // First, reduce to account asset balance. Else can't complete this test case.
    AccountCapsule toAccount =
        chainBaseManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));
    toAccount.reduceAssetAmount(ByteString.copyFromUtf8(ASSET_NAME).toByteArray(),
        TOTAL_SUPPLY - 10000, true);
    chainBaseManager.getAccountStore().put(toAccount.getAddress().toByteArray(), toAccount);
    ParticipateAssetIssueActuator actuator = new ParticipateAssetIssueActuator();
    actuator.setChainBaseManager(chainBaseManager).setAny(getContract(1));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertTrue("Asset balance is not enough !".equals(e.getMessage()));

      AccountCapsule owner =
          chainBaseManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      toAccount =
          chainBaseManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));

      Assert.assertEquals(owner.getBalance(), OWNER_BALANCE);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE);
      Assert.assertTrue(isNullOrZero(owner.getAssetMapForTest().get(ASSET_NAME)));
      Assert.assertEquals(toAccount.getAssetMapForTest().get(ASSET_NAME).longValue(), 10000);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }


  /**
   * SameTokenName open, not enough asset
   */
  @Test
  public void sameTokenNameOpenNotEnoughAssetTest() {
    chainBaseManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    initAssetIssue(chainBaseManager.getDynamicPropertiesStore()
            .getLatestBlockHeaderTimestamp() - 1000,
        chainBaseManager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp() + 1000);
    // First, reduce to account asset balance. Else can't complete this test case.
    AccountCapsule toAccount = chainBaseManager.getAccountStore()
        .get(ByteArray.fromHexString(TO_ADDRESS));
    long id = chainBaseManager.getDynamicPropertiesStore().getTokenIdNum();

    toAccount.reduceAssetAmountV2(ByteString.copyFromUtf8(String.valueOf(id)).toByteArray(),
        TOTAL_SUPPLY - 10000, chainBaseManager.getDynamicPropertiesStore(),
        chainBaseManager.getAssetIssueStore());
    chainBaseManager.getAccountStore().put(toAccount.getAddress().toByteArray(), toAccount);
    ParticipateAssetIssueActuator actuator = new ParticipateAssetIssueActuator();
    actuator.setChainBaseManager(chainBaseManager).setAny(getContract(1));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertTrue("Asset balance is not enough !".equals(e.getMessage()));

      AccountCapsule owner =
          chainBaseManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      toAccount =
          chainBaseManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));

      Assert.assertEquals(owner.getBalance(), OWNER_BALANCE);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE);

      Assert.assertTrue(isNullOrZero(owner.getAssetV2MapForTest().get(String.valueOf(id))));
      Assert.assertEquals(
          toAccount.getAssetV2MapForTest().get(String.valueOf(id)).longValue(), 10000);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * SameTokenName close, nont exist asset
   */
  @Test
  public void sameTokenNameCloseNoneExistAssetTest() {
    DateTime now = DateTime.now();
    initAssetIssue(now.minusDays(1).getMillis(), now.plusDays(1).getMillis());
    ParticipateAssetIssueActuator actuator = new ParticipateAssetIssueActuator();
    actuator.setChainBaseManager(chainBaseManager)
        .setAny(getContract(1, "TTTTTTTTTTTT"));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertTrue(("No asset named " + "TTTTTTTTTTTT").equals(e.getMessage()));

      AccountCapsule owner =
          chainBaseManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount =
          chainBaseManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));

      Assert.assertEquals(owner.getBalance(), OWNER_BALANCE);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE);
      Assert.assertTrue(isNullOrZero(owner.getAssetMapForTest().get(ASSET_NAME)));
      Assert.assertEquals(toAccount.getAssetMapForTest().get(ASSET_NAME).longValue(), TOTAL_SUPPLY);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }


  /**
   * SameTokenName open, nont exist asset
   */
  @Test
  public void sameTokenNameOpenNoneExistAssetTest() {
    chainBaseManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    DateTime now = DateTime.now();
    initAssetIssue(now.minusDays(1).getMillis(), now.plusDays(1).getMillis());
    ParticipateAssetIssueActuator actuator = new ParticipateAssetIssueActuator();
    actuator.setChainBaseManager(chainBaseManager)
        .setAny(getContract(1, "TTTTTTTTTTTT"));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertTrue(("No asset named " + "TTTTTTTTTTTT").equals(e.getMessage()));

      AccountCapsule owner =
          chainBaseManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount =
          chainBaseManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));

      Assert.assertEquals(owner.getBalance(), OWNER_BALANCE);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE);
      long id = chainBaseManager.getDynamicPropertiesStore().getTokenIdNum();
      Assert.assertTrue(isNullOrZero(owner.getAssetV2MapForTest().get(String.valueOf(id))));
      Assert.assertEquals(toAccount.getAssetV2MapForTest().get(String.valueOf(id)).longValue(),
          TOTAL_SUPPLY);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * SameTokenName close, add over flow
   */
  @Test
  public void sameTokenNameCloseAddOverflowTest() {
    initAssetIssue(chainBaseManager.getDynamicPropertiesStore()
            .getLatestBlockHeaderTimestamp() - 1000,
        chainBaseManager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp() + 1000);
    // First, increase the owner asset balance. Else can't complete this test case.
    AccountCapsule owner = chainBaseManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    owner.addAsset(ASSET_NAME.getBytes(), Long.MAX_VALUE);
    chainBaseManager.getAccountStore().put(owner.getAddress().toByteArray(), owner);
    ParticipateAssetIssueActuator actuator = new ParticipateAssetIssueActuator();
    actuator.setChainBaseManager(chainBaseManager).setAny(getContract(1L));

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
          chainBaseManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount =
          chainBaseManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));

      Assert.assertEquals(owner.getBalance(), OWNER_BALANCE);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE);
      Assert.assertEquals(owner.getAssetMapForTest().get(ASSET_NAME).longValue(), Long.MAX_VALUE);
      Assert.assertEquals(toAccount.getAssetMapForTest().get(ASSET_NAME).longValue(), TOTAL_SUPPLY);
    }
  }


  /**
   * SameTokenName open, add over flow
   */
  @Test
  public void sameTokenNameOpenAddOverflowTest() {
    chainBaseManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    initAssetIssue(chainBaseManager.getDynamicPropertiesStore()
            .getLatestBlockHeaderTimestamp() - 1000,
        chainBaseManager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp() + 1000);
    // First, increase the owner asset balance. Else can't complete this test case.
    AccountCapsule owner = chainBaseManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    long id = chainBaseManager.getDynamicPropertiesStore().getTokenIdNum();
    owner.addAssetV2(ByteString.copyFromUtf8(String.valueOf(id)).toByteArray(), Long.MAX_VALUE);
    chainBaseManager.getAccountStore().put(owner.getAddress().toByteArray(), owner);
    ParticipateAssetIssueActuator actuator = new ParticipateAssetIssueActuator();
    actuator.setChainBaseManager(chainBaseManager).setAny(getContract(1L));

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

      owner = chainBaseManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount =
          chainBaseManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));

      Assert.assertEquals(owner.getBalance(), OWNER_BALANCE);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE);

      Assert.assertEquals(owner.getAssetV2MapForTest().get(String.valueOf(id)).longValue(),
          Long.MAX_VALUE);
      Assert.assertEquals(toAccount.getAssetV2MapForTest().get(String.valueOf(id)).longValue(),
          TOTAL_SUPPLY);
    }
  }

  /**
   * SameTokenName close, multiply over flow
   */
  @Test
  public void sameTokenNameCloseMultiplyOverflowTest() {
    initAssetIssue(chainBaseManager.getDynamicPropertiesStore()
            .getLatestBlockHeaderTimestamp() - 1000,
        chainBaseManager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp() + 1000);
    // First, increase the owner trx balance. Else can't complete this test case.
    AccountCapsule owner = chainBaseManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    owner.setBalance(100000000000000L);
    chainBaseManager.getAccountStore().put(owner.getAddress().toByteArray(), owner);
    ParticipateAssetIssueActuator actuator = new ParticipateAssetIssueActuator();
    actuator.setChainBaseManager(
        chainBaseManager).setAny(getContract(8589934597L));

    //NUM = 2147483647;
    //LONG_MAX = 9223372036854775807L = 0x7fffffffffffffff
    //4294967298 * 2147483647 = 9223372036854775806 = 0x7ffffffffffffffe
    //8589934596 * 2147483647 = 4294967298 * 2147483647 *2 = 0xfffffffffffffffc = -4
    //8589934597 * 2147483647 = 8589934596 * 2147483647 + 2147483647 = -4 + 2147483647 =
    // 2147483643  vs 9223372036854775806*2 + 2147483647

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertTrue(("long overflow").equals(e.getMessage()));

      owner =
          chainBaseManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount =
          chainBaseManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));

      Assert.assertEquals(owner.getBalance(), 100000000000000L);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE);
      Assert.assertTrue(isNullOrZero(owner.getAssetMapForTest().get(ASSET_NAME)));
      Assert.assertEquals(toAccount.getAssetMapForTest().get(ASSET_NAME).longValue(), TOTAL_SUPPLY);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }


  /**
   * SameTokenName open, multiply over flow
   */
  @Test
  public void sameTokenNameOpenMultiplyOverflowTest() {
    chainBaseManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    initAssetIssue(chainBaseManager.getDynamicPropertiesStore()
            .getLatestBlockHeaderTimestamp() - 1000,
        chainBaseManager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp() + 1000);
    // First, increase the owner trx balance. Else can't complete this test case.
    AccountCapsule owner = chainBaseManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    owner.setBalance(100000000000000L);
    chainBaseManager.getAccountStore().put(owner.getAddress().toByteArray(), owner);
    ParticipateAssetIssueActuator actuator = new ParticipateAssetIssueActuator();
    actuator.setChainBaseManager(
        chainBaseManager).setAny(getContract(8589934597L));

    //NUM = 2147483647;
    //LONG_MAX = 9223372036854775807L = 0x7fffffffffffffff
    //4294967298 * 2147483647 = 9223372036854775806 = 0x7ffffffffffffffe
    //8589934596 * 2147483647 = 4294967298 * 2147483647 *2 = 0xfffffffffffffffc = -4
    //8589934597 * 2147483647 = 8589934596 * 2147483647 + 2147483647 = -4 + 2147483647 = 2147483643
    // vs 9223372036854775806*2 + 2147483647

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertTrue(("long overflow").equals(e.getMessage()));

      owner =
          chainBaseManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount =
          chainBaseManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));

      Assert.assertEquals(owner.getBalance(), 100000000000000L);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE);
      long id = chainBaseManager.getDynamicPropertiesStore().getTokenIdNum();
      Assert.assertTrue(isNullOrZero(owner.getAssetV2MapForTest().get(String.valueOf(id))));
      Assert.assertEquals(toAccount.getAssetV2MapForTest().get(String.valueOf(id)).longValue(),
          TOTAL_SUPPLY);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * SameTokenName close, exchangeAmount <= 0 trx, throw exception
   */
  @Test
  public void sameTokenNameCloseExchangeAmountTest() {

    chainBaseManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1000000);
    AssetIssueContract assetIssueContract =
        AssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)))
            .setName(ByteString.copyFrom(ByteArray.fromString(ASSET_NAME)))
            .setTotalSupply(TOTAL_SUPPLY)
            .setTrxNum(100)
            .setNum(1)
            .setStartTime(chainBaseManager.getHeadBlockTimeStamp() - 10000)
            .setEndTime(chainBaseManager.getHeadBlockTimeStamp() + 11000000)
            .setVoteScore(VOTE_SCORE)
            .setDescription(ByteString.copyFrom(ByteArray.fromString(DESCRIPTION)))
            .setUrl(ByteString.copyFrom(ByteArray.fromString(URL)))
            .build();
    AssetIssueCapsule assetIssueCapsule = new AssetIssueCapsule(assetIssueContract);
    chainBaseManager.getAssetIssueStore()
        .put(assetIssueCapsule.createDbKey(), assetIssueCapsule);

    AccountCapsule toAccountCapsule = chainBaseManager.getAccountStore()
        .get(ByteArray.fromHexString(TO_ADDRESS));
    toAccountCapsule.addAsset(ASSET_NAME.getBytes(), TOTAL_SUPPLY);
    chainBaseManager.getAccountStore()
        .put(toAccountCapsule.getAddress().toByteArray(), toAccountCapsule);
    AccountCapsule owner =
        chainBaseManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
    owner.setBalance(100000000000000L);
    chainBaseManager.getAccountStore().put(owner.getAddress().toByteArray(), owner);

    ParticipateAssetIssueActuator actuator = new ParticipateAssetIssueActuator();
    actuator.setChainBaseManager(chainBaseManager).setAny(getContract(1));

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


  /**
   * SameTokenName open, exchangeAmount <= 0 trx, throw exception
   */
  @Test
  public void sameTokenNameOpenExchangeAmountTest() {
    chainBaseManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    chainBaseManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1000000);
    long tokenId = chainBaseManager.getDynamicPropertiesStore().getTokenIdNum() + 1;
    chainBaseManager.getDynamicPropertiesStore().saveTokenIdNum(tokenId);
    AssetIssueContract assetIssueContract =
        AssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS_2)))
            .setName(ByteString.copyFrom(ByteArray.fromString(ASSET_NAME)))
            .setTotalSupply(TOTAL_SUPPLY)
            .setTrxNum(100)
            .setId(String.valueOf(tokenId))
            .setNum(1)
            .setStartTime(chainBaseManager.getHeadBlockTimeStamp() - 10000)
            .setEndTime(chainBaseManager.getHeadBlockTimeStamp() + 11000000)
            .setVoteScore(VOTE_SCORE)
            .setDescription(ByteString.copyFrom(ByteArray.fromString(DESCRIPTION)))
            .setUrl(ByteString.copyFrom(ByteArray.fromString(URL)))
            .build();
    AssetIssueCapsule assetIssueCapsule = new AssetIssueCapsule(assetIssueContract);
    chainBaseManager.getAssetIssueV2Store()
        .put(assetIssueCapsule.createDbV2Key(), assetIssueCapsule);

    AccountCapsule toAccountCapsule = chainBaseManager.getAccountStore()
        .get(ByteArray.fromHexString(TO_ADDRESS_2));
    toAccountCapsule.addAssetV2(ByteArray.fromString(String.valueOf(tokenId)), TOTAL_SUPPLY);

    chainBaseManager.getAccountStore().put(toAccountCapsule.getAddress().toByteArray(),
        toAccountCapsule);
    AccountCapsule owner =
        chainBaseManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
    owner.setBalance(100000000000000L);
    chainBaseManager.getAccountStore().put(owner.getAddress().toByteArray(), owner);

    ParticipateAssetIssueActuator actuator = new ParticipateAssetIssueActuator();
    actuator.setChainBaseManager(chainBaseManager)
        .setAny(getContractWithTo(1, TO_ADDRESS_2));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      System.out.println("e:" + e.getMessage());
      Assert.assertTrue(("Can not process the exchange!").equals(e.getMessage()));
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }


  /**
   * SameTokenName close, invalid oweraddress
   */
  @Test
  public void sameTokenNameCloseInvalidOwerAddressTest() {
    chainBaseManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    Any any = Any.pack(
        ParticipateAssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString("12131312")))
            .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)))
            .setAssetName(ByteString.copyFrom(ByteArray.fromString(ASSET_NAME)))
            .setAmount(1000)
            .build());

    ParticipateAssetIssueActuator actuator = new ParticipateAssetIssueActuator();
    actuator.setChainBaseManager(chainBaseManager).setAny(any);

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      System.out.println("e:" + e.getMessage());
      Assert.assertTrue(("Invalid ownerAddress").equals(e.getMessage()));
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * SameTokenName close, invalid Invalid toAddress
   */
  @Test
  public void sameTokenNameCloseInvalidToAddressTest() {
    chainBaseManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    Any any = Any.pack(
        ParticipateAssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setToAddress(ByteString.copyFrom(ByteArray.fromHexString("12313123")))
            .setAssetName(ByteString.copyFrom(ByteArray.fromString(ASSET_NAME)))
            .setAmount(1000)
            .build());

    ParticipateAssetIssueActuator actuator = new ParticipateAssetIssueActuator();
    actuator.setChainBaseManager(chainBaseManager).setAny(any);

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      System.out.println("e:" + e.getMessage());
      Assert.assertTrue(("Invalid toAddress").equals(e.getMessage()));
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void commonErrorCheck() {
    chainBaseManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);

    ParticipateAssetIssueActuator actuator = new ParticipateAssetIssueActuator();
    ActuatorTest actuatorTest = new ActuatorTest(actuator, dbManager);
    actuatorTest.noContract();

    Any invalidContractTypes = Any.pack(AssetIssueContractOuterClass.AssetIssueContract.newBuilder()
        .build());
    actuatorTest.setInvalidContract(invalidContractTypes);
    actuatorTest.setInvalidContractTypeMsg(
        "contract type error",
        "contract type error,expected type "
            + "[ParticipateAssetIssueContract],real type[");
    actuatorTest.invalidContractType();

    initAssetIssue(chainBaseManager.getDynamicPropertiesStore()
            .getLatestBlockHeaderTimestamp() - 1000,
        chainBaseManager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp() + 1000);
    actuatorTest.setContract(getContract(1000L));
    actuatorTest.nullTransationResult();

    actuatorTest.setNullDBManagerMsg("No account store or dynamic store!");
    actuatorTest.nullDBManger();
  }

}
