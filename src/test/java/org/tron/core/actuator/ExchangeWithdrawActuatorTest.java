package org.tron.core.actuator;

import static org.testng.Assert.fail;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.io.File;
import java.util.Arrays;
import java.util.Map;
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
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.capsule.ExchangeCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.protos.Contract;
import org.tron.protos.Contract.AssetIssueContract;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Transaction.Result.code;

@Slf4j

public class ExchangeWithdrawActuatorTest {

  private static TronApplicationContext context;
  private static Manager dbManager;
  private static final String dbPath = "output_ExchangeWithdraw_test";
  private static final String ACCOUNT_NAME_FIRST = "ownerF";
  private static final String OWNER_ADDRESS_FIRST;
  private static final String ACCOUNT_NAME_SECOND = "ownerS";
  private static final String OWNER_ADDRESS_SECOND;
  private static final String URL = "https://tron.network";
  private static final String OWNER_ADDRESS_INVALID = "aaaa";
  private static final String OWNER_ADDRESS_NOACCOUNT;
  private static final String OWNER_ADDRESS_BALANCENOTSUFFIENT;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    OWNER_ADDRESS_FIRST =
        Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
    OWNER_ADDRESS_SECOND =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
    OWNER_ADDRESS_NOACCOUNT =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1aed";
    OWNER_ADDRESS_BALANCENOTSUFFIENT =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e06d4271a1ced";
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
  public void initTest() {
    AccountCapsule ownerAccountFirstCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8(ACCOUNT_NAME_FIRST),
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_FIRST)),
            AccountType.Normal,
            10000_000_000L);
    AccountCapsule ownerAccountSecondCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8(ACCOUNT_NAME_SECOND),
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_SECOND)),
            AccountType.Normal,
            20000_000_000L);

    dbManager.getAccountStore()
        .put(ownerAccountFirstCapsule.getAddress().toByteArray(), ownerAccountFirstCapsule);
    dbManager.getAccountStore()
        .put(ownerAccountSecondCapsule.getAddress().toByteArray(), ownerAccountSecondCapsule);

    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1000000);
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderNumber(10);
    dbManager.getDynamicPropertiesStore().saveNextMaintenanceTime(2000000);
  }

  private Any getContract(String address, long exchangeId, String tokenId, long quant) {
    return Any.pack(
        Contract.ExchangeWithdrawContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(address)))
            .setExchangeId(exchangeId)
            .setTokenId(ByteString.copyFrom(tokenId.getBytes()))
            .setQuant(quant)
            .build());
  }

  private void InitExchangeBeforeSameTokenNameActive() {
    AssetIssueCapsule assetIssueCapsule1 = new AssetIssueCapsule(
            AssetIssueContract.newBuilder()
                    .setName(ByteString.copyFrom("abc".getBytes()))
                    .setId(String.valueOf(1L))
                    .build());

    AssetIssueCapsule assetIssueCapsule2 = new AssetIssueCapsule(
            AssetIssueContract.newBuilder()
                    .setName(ByteString.copyFrom("def".getBytes()))
                    .setId(String.valueOf(2L))
                    .build());
    dbManager.getAssetIssueStore().put(assetIssueCapsule1.createDbKey(), assetIssueCapsule1);
    dbManager.getAssetIssueStore().put(assetIssueCapsule2.createDbKey(), assetIssueCapsule2);
    dbManager.getAssetIssueV2Store().put(assetIssueCapsule1.createDbV2Key(), assetIssueCapsule1);
    dbManager.getAssetIssueV2Store().put(assetIssueCapsule2.createDbV2Key(), assetIssueCapsule2);

    //V1
    ExchangeCapsule exchangeCapsule =
            new ExchangeCapsule(
                    ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_FIRST)),
                    1,
                    1000000,
                    "abc".getBytes(),
                    "def".getBytes());
    exchangeCapsule.setBalance(100000000L, 200000000L);
    ExchangeCapsule exchangeCapsule2 =
            new ExchangeCapsule(
                    ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_FIRST)),
                    2,
                    1000000,
                    "_".getBytes(),
                    "def".getBytes());
    exchangeCapsule2.setBalance(1_000_000_000000L, 10_000_000L);
    ExchangeCapsule exchangeCapsule3 =
            new ExchangeCapsule(
                    ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_FIRST)),
                    3,
                    1000000,
                    "abc".getBytes(),
                    "def".getBytes());
    exchangeCapsule3.setBalance(903L, 737L);
    dbManager.getExchangeStore()
            .put(exchangeCapsule.createDbKey(), exchangeCapsule);
    dbManager.getExchangeStore()
            .put(exchangeCapsule2.createDbKey(), exchangeCapsule2);
    dbManager.getExchangeStore()
            .put(exchangeCapsule3.createDbKey(), exchangeCapsule3);

    //V2
    ExchangeCapsule exchangeCapsule4 =
            new ExchangeCapsule(
                    ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_FIRST)),
                    1,
                    1000000,
                    "1".getBytes(),
                    "2".getBytes());
    exchangeCapsule4.setBalance(100000000L, 200000000L);
    ExchangeCapsule exchangeCapsule5 =
            new ExchangeCapsule(
                    ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_FIRST)),
                    2,
                    1000000,
                    "_".getBytes(),
                    "2".getBytes());
    exchangeCapsule5.setBalance(1_000_000_000000L, 10_000_000L);
    ExchangeCapsule exchangeCapsule6 =
            new ExchangeCapsule(
                    ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_FIRST)),
                    3,
                    1000000,
                    "1".getBytes(),
                    "2".getBytes());
    exchangeCapsule6.setBalance(903L, 737L);
    dbManager.getExchangeV2Store()
            .put(exchangeCapsule4.createDbKey(), exchangeCapsule4);
    dbManager.getExchangeV2Store()
            .put(exchangeCapsule5.createDbKey(), exchangeCapsule5);
    dbManager.getExchangeV2Store()
            .put(exchangeCapsule6.createDbKey(), exchangeCapsule6);
  }

  private void InitExchangeSameTokenNameActive() {
    AssetIssueCapsule assetIssueCapsule1 = new AssetIssueCapsule(
            AssetIssueContract.newBuilder()
                    .setName(ByteString.copyFrom("123".getBytes()))
                    .setId(String.valueOf(1L))
                    .build());
    AssetIssueCapsule assetIssueCapsule2 = new AssetIssueCapsule(
            AssetIssueContract.newBuilder()
                    .setName(ByteString.copyFrom("456".getBytes()))
                    .setId(String.valueOf(2L))
                    .build());

    dbManager.getAssetIssueV2Store()
            .put(assetIssueCapsule1.createDbV2Key(), assetIssueCapsule1);
    dbManager.getAssetIssueV2Store()
            .put(assetIssueCapsule2.createDbV2Key(), assetIssueCapsule2);

    ExchangeCapsule exchangeCapsule =
            new ExchangeCapsule(
                    ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_FIRST)),
                    1,
                    1000000,
                    "123".getBytes(),
                    "456".getBytes());
    exchangeCapsule.setBalance(100000000L, 200000000L);
    ExchangeCapsule exchangeCapsule2 =
            new ExchangeCapsule(
                    ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_FIRST)),
                    2,
                    1000000,
                    "_".getBytes(),
                    "456".getBytes());
    exchangeCapsule2.setBalance(1_000_000_000000L, 10_000_000L);
    ExchangeCapsule exchangeCapsule3 =
            new ExchangeCapsule(
                    ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_FIRST)),
                    3,
                    1000000,
                    "123".getBytes(),
                    "456".getBytes());
    exchangeCapsule3.setBalance(903L, 737L);

    dbManager.getExchangeV2Store()
            .put(exchangeCapsule.createDbKey(), exchangeCapsule);
    dbManager.getExchangeV2Store()
            .put(exchangeCapsule2.createDbKey(), exchangeCapsule2);
    dbManager.getExchangeV2Store()
            .put(exchangeCapsule3.createDbKey(), exchangeCapsule3);
  }

  /**
   * SameTokenName close, first withdraw Exchange,result is success.
   */
  @Test
  public void SameTokenNameCloseSuccessExchangeWithdraw() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    InitExchangeBeforeSameTokenNameActive();
    long exchangeId = 1;
    String firstTokenId = "abc";
    long firstTokenQuant = 100000000L;
    String secondTokenId = "def";
    long secondTokenQuant = 200000000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    Map<String, Long> assetMap = accountCapsule.getAssetMap();
    Assert.assertEquals(10000_000000L, accountCapsule.getBalance());
    Assert.assertEquals(null, assetMap.get(firstTokenId));
    Assert.assertEquals(null, assetMap.get(secondTokenId));

    ExchangeWithdrawActuator actuator = new ExchangeWithdrawActuator(getContract(
        OWNER_ADDRESS_FIRST, exchangeId, firstTokenId, firstTokenQuant),
        dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      long id = 1;
      //V1
      ExchangeCapsule exchangeCapsule = dbManager.getExchangeStore().get(ByteArray.fromLong(id));
      Assert.assertNotNull(exchangeCapsule);
      Assert.assertEquals(ByteString.copyFrom(ownerAddress), exchangeCapsule.getCreatorAddress());
      Assert.assertEquals(id, exchangeCapsule.getID());
      Assert.assertEquals(1000000, exchangeCapsule.getCreateTime());
      Assert.assertTrue(Arrays.equals(firstTokenId.getBytes(), exchangeCapsule.getFirstTokenId()));
      Assert.assertEquals(firstTokenId, ByteArray.toStr(exchangeCapsule.getFirstTokenId()));
      Assert.assertEquals(0L, exchangeCapsule.getFirstTokenBalance());
      Assert.assertEquals(secondTokenId, ByteArray.toStr(exchangeCapsule.getSecondTokenId()));
      Assert.assertEquals(0L, exchangeCapsule.getSecondTokenBalance());
      //V2
      ExchangeCapsule exchangeCapsule2 = dbManager.getExchangeV2Store().get(ByteArray.fromLong(id));
      Assert.assertNotNull(exchangeCapsule2);
      Assert.assertEquals(ByteString.copyFrom(ownerAddress), exchangeCapsule2.getCreatorAddress());
      Assert.assertEquals(id, exchangeCapsule2.getID());
      Assert.assertEquals(1000000, exchangeCapsule2.getCreateTime());
      Assert.assertEquals(0L, exchangeCapsule2.getFirstTokenBalance());
      Assert.assertEquals(0L, exchangeCapsule2.getSecondTokenBalance());

      accountCapsule = dbManager.getAccountStore().get(ownerAddress);
      assetMap = accountCapsule.getAssetMap();
      Assert.assertEquals(10000_000000L, accountCapsule.getBalance());
      Assert.assertEquals(firstTokenQuant, assetMap.get(firstTokenId).longValue());
      Assert.assertEquals(secondTokenQuant, assetMap.get(secondTokenId).longValue());

      Assert.assertEquals(secondTokenQuant, ret.getExchangeWithdrawAnotherAmount());

    } catch (ContractValidateException e) {
      logger.info(e.getMessage());
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } catch (ItemNotFoundException e) {
      Assert.assertFalse(e instanceof ItemNotFoundException);
    } finally {
      dbManager.getExchangeStore().delete(ByteArray.fromLong(1L));
      dbManager.getExchangeStore().delete(ByteArray.fromLong(2L));
      dbManager.getExchangeV2Store().delete(ByteArray.fromLong(1L));
      dbManager.getExchangeV2Store().delete(ByteArray.fromLong(2L));
    }
  }


  /**
   * Init close SameTokenName,after init data,open SameTokenName
   */
  @Test
  public void oldNotUpdateSuccessExchangeWithdraw() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    InitExchangeBeforeSameTokenNameActive();
    long exchangeId = 1;
    String firstTokenId = "abc";
    long firstTokenQuant = 100000000L;
    String secondTokenId = "def";
    long secondTokenQuant = 200000000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    Map<String, Long> assetMap = accountCapsule.getAssetMap();
    Assert.assertEquals(10000_000000L, accountCapsule.getBalance());
    Assert.assertEquals(null, assetMap.get(firstTokenId));
    Assert.assertEquals(null, assetMap.get(secondTokenId));

    ExchangeWithdrawActuator actuator = new ExchangeWithdrawActuator(getContract(
            OWNER_ADDRESS_FIRST, exchangeId, String.valueOf(1), firstTokenQuant),
            dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      long id = 1;
      //V1 not update
      ExchangeCapsule exchangeCapsule = dbManager.getExchangeStore().get(ByteArray.fromLong(id));
      Assert.assertNotNull(exchangeCapsule);
      Assert.assertEquals(ByteString.copyFrom(ownerAddress), exchangeCapsule.getCreatorAddress());
      Assert.assertEquals(id, exchangeCapsule.getID());
      Assert.assertEquals(1000000, exchangeCapsule.getCreateTime());
      Assert.assertTrue(Arrays.equals(firstTokenId.getBytes(), exchangeCapsule.getFirstTokenId()));
      Assert.assertEquals(firstTokenId, ByteArray.toStr(exchangeCapsule.getFirstTokenId()));
      Assert.assertEquals(secondTokenId, ByteArray.toStr(exchangeCapsule.getSecondTokenId()));
      Assert.assertNotEquals(0L, exchangeCapsule.getFirstTokenBalance());
      Assert.assertNotEquals(0L, exchangeCapsule.getSecondTokenBalance());
      //V2
      ExchangeCapsule exchangeCapsule2 = dbManager.getExchangeV2Store().get(ByteArray.fromLong(id));
      Assert.assertNotNull(exchangeCapsule2);
      Assert.assertEquals(ByteString.copyFrom(ownerAddress), exchangeCapsule2.getCreatorAddress());
      Assert.assertEquals(id, exchangeCapsule2.getID());
      Assert.assertEquals(1000000, exchangeCapsule2.getCreateTime());
      Assert.assertEquals(0L, exchangeCapsule2.getFirstTokenBalance());
      Assert.assertEquals(0L, exchangeCapsule2.getSecondTokenBalance());

      accountCapsule = dbManager.getAccountStore().get(ownerAddress);
      assetMap = accountCapsule.getAssetMapV2();
      Assert.assertEquals(10000_000000L, accountCapsule.getBalance());
      Assert.assertEquals(firstTokenQuant, assetMap.get(String.valueOf(1)).longValue());
      Assert.assertEquals(secondTokenQuant, assetMap.get(String.valueOf(2)).longValue());

      Assert.assertEquals(secondTokenQuant, ret.getExchangeWithdrawAnotherAmount());

    } catch (ContractValidateException e) {
      logger.info(e.getMessage());
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } catch (ItemNotFoundException e) {
      Assert.assertFalse(e instanceof ItemNotFoundException);
    } finally {
      dbManager.getExchangeStore().delete(ByteArray.fromLong(1L));
      dbManager.getExchangeStore().delete(ByteArray.fromLong(2L));
      dbManager.getExchangeV2Store().delete(ByteArray.fromLong(1L));
      dbManager.getExchangeV2Store().delete(ByteArray.fromLong(2L));
    }
  }

  /**
   * SameTokenName open, first withdraw Exchange,result is success.
   */
  @Test
  public void SameTokenNameOpenSuccessExchangeWithdraw() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    InitExchangeSameTokenNameActive();
    long exchangeId = 1;
    String firstTokenId = "123";
    long firstTokenQuant = 100000000L;
    String secondTokenId = "456";
    long secondTokenQuant = 200000000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    Map<String, Long> assetV2Map = accountCapsule.getAssetMapV2();
    Assert.assertEquals(10000_000000L, accountCapsule.getBalance());
    Assert.assertEquals(null, assetV2Map.get(firstTokenId));
    Assert.assertEquals(null, assetV2Map.get(secondTokenId));

    ExchangeWithdrawActuator actuator = new ExchangeWithdrawActuator(getContract(
            OWNER_ADDRESS_FIRST, exchangeId, firstTokenId, firstTokenQuant),
            dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      // V1,Data is no longer update
      Assert.assertFalse(dbManager.getExchangeStore().has(ByteArray.fromLong(exchangeId)));
      //V2
      ExchangeCapsule exchangeCapsuleV2 =
              dbManager.getExchangeV2Store().get(ByteArray.fromLong(exchangeId));
      Assert.assertNotNull(exchangeCapsuleV2);
      Assert.assertEquals(ByteString.copyFrom(ownerAddress), exchangeCapsuleV2.getCreatorAddress());
      Assert.assertEquals(exchangeId, exchangeCapsuleV2.getID());
      Assert.assertEquals(1000000, exchangeCapsuleV2.getCreateTime());
      Assert.assertTrue(Arrays.equals(firstTokenId.getBytes(), exchangeCapsuleV2.getFirstTokenId()));
      Assert.assertEquals(firstTokenId, ByteArray.toStr(exchangeCapsuleV2.getFirstTokenId()));
      Assert.assertEquals(0L, exchangeCapsuleV2.getFirstTokenBalance());
      Assert.assertEquals(secondTokenId, ByteArray.toStr(exchangeCapsuleV2.getSecondTokenId()));
      Assert.assertEquals(0L, exchangeCapsuleV2.getSecondTokenBalance());

      accountCapsule = dbManager.getAccountStore().get(ownerAddress);
      assetV2Map = accountCapsule.getAssetMapV2();
      Assert.assertEquals(10000_000000L, accountCapsule.getBalance());
      Assert.assertEquals(firstTokenQuant, assetV2Map.get(firstTokenId).longValue());
      Assert.assertEquals(secondTokenQuant, assetV2Map.get(secondTokenId).longValue());

    } catch (ContractValidateException e) {
      logger.info(e.getMessage());
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } catch (ItemNotFoundException e) {
      Assert.assertFalse(e instanceof ItemNotFoundException);
    } finally {
      dbManager.getExchangeStore().delete(ByteArray.fromLong(1L));
      dbManager.getExchangeStore().delete(ByteArray.fromLong(2L));
      dbManager.getExchangeV2Store().delete(ByteArray.fromLong(1L));
      dbManager.getExchangeV2Store().delete(ByteArray.fromLong(2L));
    }
  }

  /**
   * SameTokenName close, second withdraw Exchange,result is success.
   */
  @Test
  public void SameTokenNameCloseSuccessExchangeWithdraw2() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    InitExchangeBeforeSameTokenNameActive();
    long exchangeId = 2;
    String firstTokenId = "_";
    long firstTokenQuant = 1_000_000_000000L;
    String secondTokenId = "def";
    long secondTokenQuant = 4_000_000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    Map<String, Long> assetMap = accountCapsule.getAssetMap();
    Assert.assertEquals(10000_000000L, accountCapsule.getBalance());
    Assert.assertEquals(null, assetMap.get(secondTokenId));

    ExchangeWithdrawActuator actuator = new ExchangeWithdrawActuator(getContract(
        OWNER_ADDRESS_FIRST, exchangeId, firstTokenId, firstTokenQuant),
        dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      //V1
      ExchangeCapsule exchangeCapsule = dbManager.getExchangeStore()
          .get(ByteArray.fromLong(exchangeId));
      Assert.assertNotNull(exchangeCapsule);
      Assert.assertEquals(ByteString.copyFrom(ownerAddress), exchangeCapsule.getCreatorAddress());
      Assert.assertEquals(exchangeId, exchangeCapsule.getID());
      Assert.assertEquals(1000000, exchangeCapsule.getCreateTime());
      Assert.assertTrue(Arrays.equals(firstTokenId.getBytes(), exchangeCapsule.getFirstTokenId()));
      Assert.assertEquals(firstTokenId, ByteArray.toStr(exchangeCapsule.getFirstTokenId()));
      Assert.assertEquals(0L, exchangeCapsule.getFirstTokenBalance());
      Assert.assertEquals(secondTokenId, ByteArray.toStr(exchangeCapsule.getSecondTokenId()));
      Assert.assertEquals(0L, exchangeCapsule.getSecondTokenBalance());
      //V2
      ExchangeCapsule exchangeCapsule2 = dbManager.getExchangeStore()
              .get(ByteArray.fromLong(exchangeId));
      Assert.assertNotNull(exchangeCapsule2);
      Assert.assertEquals(ByteString.copyFrom(ownerAddress), exchangeCapsule2.getCreatorAddress());
      Assert.assertEquals(exchangeId, exchangeCapsule2.getID());
      Assert.assertEquals(1000000, exchangeCapsule2.getCreateTime());
//      Assert.assertTrue(Arrays.equals(firstTokenId.getBytes(), exchangeCapsule2.getFirstTokenId()));
//      Assert.assertEquals(firstTokenId, ByteArray.toStr(exchangeCapsule2.getFirstTokenId()));
      Assert.assertEquals(0L, exchangeCapsule2.getFirstTokenBalance());
//      Assert.assertEquals(secondTokenId, ByteArray.toStr(exchangeCapsule2.getSecondTokenId()));
      Assert.assertEquals(0L, exchangeCapsule2.getSecondTokenBalance());

      accountCapsule = dbManager.getAccountStore().get(ownerAddress);
      assetMap = accountCapsule.getAssetMap();
      Assert.assertEquals(firstTokenQuant + 10000_000000L, accountCapsule.getBalance());
      Assert.assertEquals(10_000_000L, assetMap.get(secondTokenId).longValue());

    } catch (ContractValidateException e) {
      logger.info(e.getMessage());
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } catch (ItemNotFoundException e) {
      Assert.assertFalse(e instanceof ItemNotFoundException);
    } finally {
      dbManager.getExchangeStore().delete(ByteArray.fromLong(1L));
      dbManager.getExchangeStore().delete(ByteArray.fromLong(2L));
      dbManager.getExchangeV2Store().delete(ByteArray.fromLong(1L));
      dbManager.getExchangeV2Store().delete(ByteArray.fromLong(2L));
    }
  }

  /**
   * SameTokenName open, second withdraw Exchange,result is success.
   */
  @Test
  public void SameTokenNameOpenSuccessExchangeWithdraw2() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    InitExchangeSameTokenNameActive();
    long exchangeId = 2;
    String firstTokenId = "_";
    long firstTokenQuant = 1_000_000_000000L;
    String secondTokenId = "456";
    long secondTokenQuant = 4_000_000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    Map<String, Long> assetV2Map = accountCapsule.getAssetMapV2();
    Assert.assertEquals(10000_000000L, accountCapsule.getBalance());
    Assert.assertEquals(null, assetV2Map.get(secondTokenId));

    ExchangeWithdrawActuator actuator = new ExchangeWithdrawActuator(getContract(
            OWNER_ADDRESS_FIRST, exchangeId, firstTokenId, firstTokenQuant),
            dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      // V1,Data is no longer update
      Assert.assertFalse(dbManager.getExchangeStore().has(ByteArray.fromLong(exchangeId)));
      //V2
      ExchangeCapsule exchangeCapsuleV2 = dbManager.getExchangeV2Store()
              .get(ByteArray.fromLong(exchangeId));
      Assert.assertNotNull(exchangeCapsuleV2);
      Assert.assertEquals(ByteString.copyFrom(ownerAddress), exchangeCapsuleV2.getCreatorAddress());
      Assert.assertEquals(exchangeId, exchangeCapsuleV2.getID());
      Assert.assertEquals(1000000, exchangeCapsuleV2.getCreateTime());
      Assert.assertTrue(Arrays.equals(firstTokenId.getBytes(), exchangeCapsuleV2.getFirstTokenId()));
      Assert.assertEquals(firstTokenId, ByteArray.toStr(exchangeCapsuleV2.getFirstTokenId()));
      Assert.assertEquals(0L, exchangeCapsuleV2.getFirstTokenBalance());
      Assert.assertEquals(secondTokenId, ByteArray.toStr(exchangeCapsuleV2.getSecondTokenId()));
      Assert.assertEquals(0L, exchangeCapsuleV2.getSecondTokenBalance());

      accountCapsule = dbManager.getAccountStore().get(ownerAddress);
      assetV2Map = accountCapsule.getAssetMapV2();
      Assert.assertEquals(firstTokenQuant + 10000_000000L, accountCapsule.getBalance());
      Assert.assertEquals(10_000_000L, assetV2Map.get(secondTokenId).longValue());

    } catch (ContractValidateException e) {
      logger.info(e.getMessage());
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } catch (ItemNotFoundException e) {
      Assert.assertFalse(e instanceof ItemNotFoundException);
    } finally {
      dbManager.getExchangeStore().delete(ByteArray.fromLong(1L));
      dbManager.getExchangeStore().delete(ByteArray.fromLong(2L));
      dbManager.getExchangeV2Store().delete(ByteArray.fromLong(1L));
      dbManager.getExchangeV2Store().delete(ByteArray.fromLong(2L));
    }
  }

  /**
   * SameTokenName close, use Invalid Address, result is failed, exception is "Invalid address".
   */
  @Test
  public void SameTokenNameCloseInvalidAddress() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    InitExchangeBeforeSameTokenNameActive();
    long exchangeId = 1;
    String firstTokenId = "abc";
    long firstTokenQuant = 100000000L;
    String secondTokenId = "def";
    long secondTokenQuant = 200000000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    Map<String, Long> assetMap = accountCapsule.getAssetMap();
    Assert.assertEquals(10000_000000L, accountCapsule.getBalance());
    Assert.assertEquals(null, assetMap.get(firstTokenId));
    Assert.assertEquals(null, assetMap.get(secondTokenId));

    ExchangeWithdrawActuator actuator = new ExchangeWithdrawActuator(getContract(
        OWNER_ADDRESS_INVALID, exchangeId, firstTokenId, firstTokenQuant),
        dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      fail("Invalid address");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Invalid address", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getExchangeStore().delete(ByteArray.fromLong(1L));
      dbManager.getExchangeStore().delete(ByteArray.fromLong(2L));
      dbManager.getExchangeV2Store().delete(ByteArray.fromLong(1L));
      dbManager.getExchangeV2Store().delete(ByteArray.fromLong(2L));
    }
  }

  /**
   * SameTokenName open, use Invalid Address, result is failed, exception is "Invalid address".
   */
  @Test
  public void SameTokenNameOpenInvalidAddress() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    InitExchangeSameTokenNameActive();
    long exchangeId = 1;
    String firstTokenId = "123";
    long firstTokenQuant = 100000000L;
    String secondTokenId = "456";
    long secondTokenQuant = 200000000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    Map<String, Long> assetV2Map = accountCapsule.getAssetMapV2();
    Assert.assertEquals(10000_000000L, accountCapsule.getBalance());
    Assert.assertEquals(null, assetV2Map.get(firstTokenId));
    Assert.assertEquals(null, assetV2Map.get(secondTokenId));

    ExchangeWithdrawActuator actuator = new ExchangeWithdrawActuator(getContract(
            OWNER_ADDRESS_INVALID, exchangeId, firstTokenId, firstTokenQuant),
            dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      fail("Invalid address");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Invalid address", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getExchangeStore().delete(ByteArray.fromLong(1L));
      dbManager.getExchangeStore().delete(ByteArray.fromLong(2L));
      dbManager.getExchangeV2Store().delete(ByteArray.fromLong(1L));
      dbManager.getExchangeV2Store().delete(ByteArray.fromLong(2L));
    }
  }

  /**
   * SameTokenName close, use AccountStore not exists, result is failed, exception is "account not exists".
   */
  @Test
  public void SameTokenNameCloseNoAccount() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    InitExchangeBeforeSameTokenNameActive();
    long exchangeId = 1;
    String firstTokenId = "abc";
    long firstTokenQuant = 100000000L;
    String secondTokenId = "def";
    long secondTokenQuant = 200000000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    Map<String, Long> assetMap = accountCapsule.getAssetMap();
    Assert.assertEquals(10000_000000L, accountCapsule.getBalance());
    Assert.assertEquals(null, assetMap.get(firstTokenId));
    Assert.assertEquals(null, assetMap.get(secondTokenId));

    ExchangeWithdrawActuator actuator = new ExchangeWithdrawActuator(getContract(
        OWNER_ADDRESS_NOACCOUNT, exchangeId, firstTokenId, firstTokenQuant),
        dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      fail("account[+OWNER_ADDRESS_NOACCOUNT+] not exists");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("account[" + OWNER_ADDRESS_NOACCOUNT + "] not exists",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getExchangeStore().delete(ByteArray.fromLong(1L));
      dbManager.getExchangeStore().delete(ByteArray.fromLong(2L));
      dbManager.getExchangeV2Store().delete(ByteArray.fromLong(1L));
      dbManager.getExchangeV2Store().delete(ByteArray.fromLong(2L));
    }
  }

  /**
   * SameTokenName open, use AccountStore not exists, result is failed, exception is "account not exists".
   */
  @Test
  public void SameTokenNameOpenNoAccount() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    InitExchangeSameTokenNameActive();
    long exchangeId = 1;
    String firstTokenId = "123";
    long firstTokenQuant = 100000000L;
    String secondTokenId = "456";
    long secondTokenQuant = 200000000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    Map<String, Long> assetV2Map = accountCapsule.getAssetMapV2();
    Assert.assertEquals(10000_000000L, accountCapsule.getBalance());
    Assert.assertEquals(null, assetV2Map.get(firstTokenId));
    Assert.assertEquals(null, assetV2Map.get(secondTokenId));

    ExchangeWithdrawActuator actuator = new ExchangeWithdrawActuator(getContract(
            OWNER_ADDRESS_NOACCOUNT, exchangeId, firstTokenId, firstTokenQuant),
            dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      fail("account[+OWNER_ADDRESS_NOACCOUNT+] not exists");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("account[" + OWNER_ADDRESS_NOACCOUNT + "] not exists",
              e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getExchangeStore().delete(ByteArray.fromLong(1L));
      dbManager.getExchangeStore().delete(ByteArray.fromLong(2L));
      dbManager.getExchangeV2Store().delete(ByteArray.fromLong(1L));
      dbManager.getExchangeV2Store().delete(ByteArray.fromLong(2L));
    }
  }

  /**
   * SameTokenName close, Exchange not exists
   */
  @Test
  public void SameTokenNameCloseExchangeNotExist() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    InitExchangeBeforeSameTokenNameActive();
    long exchangeId = 4;
    String firstTokenId = "abc";
    long firstTokenQuant = 100000000L;
    String secondTokenId = "def";
    long secondTokenQuant = 200000000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    Map<String, Long> assetMap = accountCapsule.getAssetMap();
    Assert.assertEquals(10000_000000L, accountCapsule.getBalance());
    Assert.assertEquals(null, assetMap.get(firstTokenId));
    Assert.assertEquals(null, assetMap.get(secondTokenId));

    ExchangeWithdrawActuator actuator = new ExchangeWithdrawActuator(getContract(
        OWNER_ADDRESS_FIRST, exchangeId, firstTokenId, firstTokenQuant),
        dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      fail("Exchange not exists");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Exchange[4] not exists",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getExchangeStore().delete(ByteArray.fromLong(1L));
      dbManager.getExchangeStore().delete(ByteArray.fromLong(2L));
      dbManager.getExchangeV2Store().delete(ByteArray.fromLong(1L));
      dbManager.getExchangeV2Store().delete(ByteArray.fromLong(2L));
    }
  }

  /**
   * SameTokenName open, Exchange not exists
   */
  @Test
  public void SameTokenNameOpenExchangeNotExist() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    InitExchangeSameTokenNameActive();
    long exchangeId = 4;
    String firstTokenId = "123";
    long firstTokenQuant = 100000000L;
    String secondTokenId = "456";
    long secondTokenQuant = 200000000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    Map<String, Long> assetV2Map = accountCapsule.getAssetMapV2();
    Assert.assertEquals(10000_000000L, accountCapsule.getBalance());
    Assert.assertEquals(null, assetV2Map.get(firstTokenId));
    Assert.assertEquals(null, assetV2Map.get(secondTokenId));

    ExchangeWithdrawActuator actuator = new ExchangeWithdrawActuator(getContract(
            OWNER_ADDRESS_FIRST, exchangeId, firstTokenId, firstTokenQuant),
            dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      fail("Exchange not exists");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Exchange[4] not exists",
              e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getExchangeStore().delete(ByteArray.fromLong(1L));
      dbManager.getExchangeStore().delete(ByteArray.fromLong(2L));
      dbManager.getExchangeV2Store().delete(ByteArray.fromLong(1L));
      dbManager.getExchangeV2Store().delete(ByteArray.fromLong(2L));
    }
  }

  /**
   * SameTokenName close, account is not creator
   */
  @Test
  public void SameTokenNameCloseAccountIsNotCreator() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    InitExchangeBeforeSameTokenNameActive();
    long exchangeId = 1;
    String firstTokenId = "abc";
    long firstTokenQuant = 200000000L;
    String secondTokenId = "def";
    long secondTokenQuant = 400000000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_SECOND);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.addAssetAmount(firstTokenId.getBytes(), firstTokenQuant);
    accountCapsule.addAssetAmount(secondTokenId.getBytes(), secondTokenQuant);
    accountCapsule.setBalance(10000_000000L);
    dbManager.getAccountStore().put(ownerAddress, accountCapsule);

    ExchangeWithdrawActuator actuator = new ExchangeWithdrawActuator(getContract(
        OWNER_ADDRESS_SECOND, exchangeId, firstTokenId, firstTokenQuant),
        dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("account[a0548794500882809695a8a687866e76d4271a1abc]"
              + " is not creator",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getExchangeStore().delete(ByteArray.fromLong(1L));
      dbManager.getExchangeStore().delete(ByteArray.fromLong(2L));
      dbManager.getExchangeV2Store().delete(ByteArray.fromLong(1L));
      dbManager.getExchangeV2Store().delete(ByteArray.fromLong(2L));
    }
  }

  /**
   * SameTokenName open, account is not creator
   */
  @Test
  public void SameTokenNameOpenAccountIsNotCreator() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    InitExchangeSameTokenNameActive();
    long exchangeId = 1;
    String firstTokenId = "123";
    long firstTokenQuant = 200000000L;
    String secondTokenId = "456";
    long secondTokenQuant = 400000000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_SECOND);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.addAssetAmountV2(firstTokenId.getBytes(), firstTokenQuant, dbManager);
    accountCapsule.addAssetAmountV2(secondTokenId.getBytes(), secondTokenQuant, dbManager);
    accountCapsule.setBalance(10000_000000L);
    dbManager.getAccountStore().put(ownerAddress, accountCapsule);

    ExchangeWithdrawActuator actuator = new ExchangeWithdrawActuator(getContract(
            OWNER_ADDRESS_SECOND, exchangeId, firstTokenId, firstTokenQuant),
            dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("account[a0548794500882809695a8a687866e76d4271a1abc]"
                      + " is not creator",
              e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getExchangeStore().delete(ByteArray.fromLong(1L));
      dbManager.getExchangeStore().delete(ByteArray.fromLong(2L));
      dbManager.getExchangeV2Store().delete(ByteArray.fromLong(1L));
      dbManager.getExchangeV2Store().delete(ByteArray.fromLong(2L));
    }
  }

  /**
   * SameTokenName close, token is not in exchange
   */
  @Test
  public void SameTokenNameCloseTokenIsNotInExchange() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    InitExchangeBeforeSameTokenNameActive();
    long exchangeId = 1;
    String firstTokenId = "_";
    long firstTokenQuant = 200000000L;
    String secondTokenId = "def";
    long secondTokenQuant = 400000000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.addAssetAmount(secondTokenId.getBytes(), secondTokenQuant);
    accountCapsule.setBalance(firstTokenQuant);
    dbManager.getAccountStore().put(ownerAddress, accountCapsule);

    ExchangeWithdrawActuator actuator = new ExchangeWithdrawActuator(getContract(
        OWNER_ADDRESS_FIRST, exchangeId, firstTokenId, firstTokenQuant),
        dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("token is not in exchange",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getExchangeStore().delete(ByteArray.fromLong(1L));
      dbManager.getExchangeStore().delete(ByteArray.fromLong(2L));
      dbManager.getExchangeV2Store().delete(ByteArray.fromLong(1L));
      dbManager.getExchangeV2Store().delete(ByteArray.fromLong(2L));
    }
  }

  /**
   * SameTokenName open, token is not in exchange
   */
  @Test
  public void SameTokenNameOpenTokenIsNotInExchange() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    InitExchangeSameTokenNameActive();
    long exchangeId = 1;
    String firstTokenId = "_";
    long firstTokenQuant = 200000000L;
    String secondTokenId = "456";
    long secondTokenQuant = 400000000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.addAssetAmountV2(secondTokenId.getBytes(), secondTokenQuant, dbManager );
    accountCapsule.setBalance(firstTokenQuant);
    dbManager.getAccountStore().put(ownerAddress, accountCapsule);

    ExchangeWithdrawActuator actuator = new ExchangeWithdrawActuator(getContract(
            OWNER_ADDRESS_FIRST, exchangeId, firstTokenId, firstTokenQuant),
            dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("token is not in exchange",
              e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getExchangeStore().delete(ByteArray.fromLong(1L));
      dbManager.getExchangeStore().delete(ByteArray.fromLong(2L));
      dbManager.getExchangeV2Store().delete(ByteArray.fromLong(1L));
      dbManager.getExchangeV2Store().delete(ByteArray.fromLong(2L));
    }
  }

  /**
   * SameTokenName close, Token balance in exchange is equal with 0, the exchange has been closed"
   */
  @Test
  public void SameTokenNameCloseTokenBalanceZero() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    InitExchangeBeforeSameTokenNameActive();
    long exchangeId = 1;
    String firstTokenId = "abc";
    long firstTokenQuant = 200000000L;
    String secondTokenId = "def";
    long secondTokenQuant = 400000000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.addAssetAmount(firstTokenId.getBytes(), firstTokenQuant);
    accountCapsule.addAssetAmount(secondTokenId.getBytes(), secondTokenQuant);
    accountCapsule.setBalance(10000_000000L);
    dbManager.getAccountStore().put(ownerAddress, accountCapsule);

    ExchangeWithdrawActuator actuator = new ExchangeWithdrawActuator(getContract(
        OWNER_ADDRESS_FIRST, exchangeId, firstTokenId, firstTokenQuant),
        dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      ExchangeCapsule exchangeCapsule = dbManager.getExchangeStore()
          .get(ByteArray.fromLong(exchangeId));
      exchangeCapsule.setBalance(0, 0);
      dbManager.getExchangeStore().put(exchangeCapsule.createDbKey(), exchangeCapsule);

      actuator.validate();
      actuator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Token balance in exchange is equal with 0,"
              + "the exchange has been closed",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } catch (ItemNotFoundException e) {
      Assert.assertFalse(e instanceof ItemNotFoundException);
    } finally {
      dbManager.getExchangeStore().delete(ByteArray.fromLong(1L));
      dbManager.getExchangeStore().delete(ByteArray.fromLong(2L));
      dbManager.getExchangeV2Store().delete(ByteArray.fromLong(1L));
      dbManager.getExchangeV2Store().delete(ByteArray.fromLong(2L));
    }
  }

  /**
   * SameTokenName open, Token balance in exchange is equal with 0, the exchange has been closed"
   */
  @Test
  public void SameTokenNameOpenTokenBalanceZero() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    InitExchangeSameTokenNameActive();
    long exchangeId = 1;
    String firstTokenId = "123";
    long firstTokenQuant = 200000000L;
    String secondTokenId = "456";
    long secondTokenQuant = 400000000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.addAssetAmountV2(firstTokenId.getBytes(), firstTokenQuant, dbManager );
    accountCapsule.addAssetAmountV2(secondTokenId.getBytes(), secondTokenQuant, dbManager );
    accountCapsule.setBalance(10000_000000L);
    dbManager.getAccountStore().put(ownerAddress, accountCapsule);

    ExchangeWithdrawActuator actuator = new ExchangeWithdrawActuator(getContract(
            OWNER_ADDRESS_FIRST, exchangeId, firstTokenId, firstTokenQuant),
            dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      ExchangeCapsule exchangeCapsuleV2 = dbManager.getExchangeV2Store()
              .get(ByteArray.fromLong(exchangeId));
      exchangeCapsuleV2.setBalance(0, 0);
      dbManager.getExchangeV2Store().put(exchangeCapsuleV2.createDbKey(), exchangeCapsuleV2);

      actuator.validate();
      actuator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Token balance in exchange is equal with 0,"
                      + "the exchange has been closed",
              e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } catch (ItemNotFoundException e) {
      Assert.assertFalse(e instanceof ItemNotFoundException);
    } finally {
      dbManager.getExchangeStore().delete(ByteArray.fromLong(1L));
      dbManager.getExchangeStore().delete(ByteArray.fromLong(2L));
      dbManager.getExchangeV2Store().delete(ByteArray.fromLong(1L));
      dbManager.getExchangeV2Store().delete(ByteArray.fromLong(2L));
    }
  }

  /**
   * SameTokenName close, withdraw token quant must greater than zero
   */
  @Test
  public void SameTokenNameCloseTokenQuantLessThanZero() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    InitExchangeBeforeSameTokenNameActive();
    long exchangeId = 1;
    String firstTokenId = "abc";
    long firstTokenQuant = -1L;
    String secondTokenId = "def";
    long secondTokenQuant = 400000000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.addAssetAmount(firstTokenId.getBytes(), 1000L);
    accountCapsule.addAssetAmount(secondTokenId.getBytes(), secondTokenQuant);
    accountCapsule.setBalance(10000_000000L);
    dbManager.getAccountStore().put(ownerAddress, accountCapsule);

    ExchangeWithdrawActuator actuator = new ExchangeWithdrawActuator(getContract(
        OWNER_ADDRESS_FIRST, exchangeId, firstTokenId, firstTokenQuant),
        dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("withdraw token quant must greater than zero",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getExchangeStore().delete(ByteArray.fromLong(1L));
      dbManager.getExchangeStore().delete(ByteArray.fromLong(2L));
      dbManager.getExchangeV2Store().delete(ByteArray.fromLong(1L));
      dbManager.getExchangeV2Store().delete(ByteArray.fromLong(2L));
    }
  }

  /**
   * SameTokenName open, withdraw token quant must greater than zero
   */
  @Test
  public void SameTokenNameOpenTokenQuantLessThanZero() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    InitExchangeSameTokenNameActive();
    long exchangeId = 1;
    String firstTokenId = "123";
    long firstTokenQuant = -1L;
    String secondTokenId = "456";
    long secondTokenQuant = 400000000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.addAssetAmountV2(firstTokenId.getBytes(), 1000L, dbManager );
    accountCapsule.addAssetAmountV2(secondTokenId.getBytes(), secondTokenQuant, dbManager );
    accountCapsule.setBalance(10000_000000L);
    dbManager.getAccountStore().put(ownerAddress, accountCapsule);

    ExchangeWithdrawActuator actuator = new ExchangeWithdrawActuator(getContract(
            OWNER_ADDRESS_FIRST, exchangeId, firstTokenId, firstTokenQuant),
            dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("withdraw token quant must greater than zero",
              e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getExchangeStore().delete(ByteArray.fromLong(1L));
      dbManager.getExchangeStore().delete(ByteArray.fromLong(2L));
      dbManager.getExchangeV2Store().delete(ByteArray.fromLong(1L));
      dbManager.getExchangeV2Store().delete(ByteArray.fromLong(2L));
    }
  }

  /**
   * SameTokenName close, withdraw another token quant must greater than zero
   */
  @Test
  public void SameTokenNameCloseTnotherTokenQuantLessThanZero() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    InitExchangeBeforeSameTokenNameActive();
    long exchangeId = 1;
    String firstTokenId = "abc";
    long quant = 1L;
    String secondTokenId = "def";
    long secondTokenQuant = 400000000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.addAssetAmount(firstTokenId.getBytes(), 1000L);
    accountCapsule.addAssetAmount(secondTokenId.getBytes(), secondTokenQuant);
    accountCapsule.setBalance(10000_000000L);
    dbManager.getAccountStore().put(ownerAddress, accountCapsule);

    ExchangeWithdrawActuator actuator = new ExchangeWithdrawActuator(getContract(
        OWNER_ADDRESS_FIRST, exchangeId, secondTokenId, quant),
        dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("withdraw another token quant must greater than zero",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getExchangeStore().delete(ByteArray.fromLong(1L));
      dbManager.getExchangeStore().delete(ByteArray.fromLong(2L));
      dbManager.getExchangeV2Store().delete(ByteArray.fromLong(1L));
      dbManager.getExchangeV2Store().delete(ByteArray.fromLong(2L));
    }
  }

  /**
   * SameTokenName open, withdraw another token quant must greater than zero
   */
  @Test
  public void SameTokenNameOpenTnotherTokenQuantLessThanZero() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    InitExchangeSameTokenNameActive();
    long exchangeId = 1;
    String firstTokenId = "123";
    long quant = 1L;
    String secondTokenId = "456";
    long secondTokenQuant = 400000000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.addAssetAmountV2(firstTokenId.getBytes(), 1000L, dbManager );
    accountCapsule.addAssetAmountV2(secondTokenId.getBytes(), secondTokenQuant, dbManager );
    accountCapsule.setBalance(10000_000000L);
    dbManager.getAccountStore().put(ownerAddress, accountCapsule);

    ExchangeWithdrawActuator actuator = new ExchangeWithdrawActuator(getContract(
            OWNER_ADDRESS_FIRST, exchangeId, secondTokenId, quant),
            dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("withdraw another token quant must greater than zero",
              e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getExchangeStore().delete(ByteArray.fromLong(1L));
      dbManager.getExchangeStore().delete(ByteArray.fromLong(2L));
      dbManager.getExchangeV2Store().delete(ByteArray.fromLong(1L));
      dbManager.getExchangeV2Store().delete(ByteArray.fromLong(2L));
    }
  }

  /**
   * SameTokenName close, Not precise enough
   */
  @Test
  public void SameTokenNameCloseNotPreciseEnough() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    InitExchangeBeforeSameTokenNameActive();
    long exchangeId = 1;
    String firstTokenId = "abc";
    long quant = 9991L;
    String secondTokenId = "def";
    ExchangeWithdrawActuator actuator = new ExchangeWithdrawActuator(getContract(
        OWNER_ADDRESS_FIRST, exchangeId, secondTokenId, quant),
        dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Not precise enough",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

    quant = 10001;
    actuator = new ExchangeWithdrawActuator(getContract(
        OWNER_ADDRESS_FIRST, exchangeId, secondTokenId, quant),
        dbManager);
    ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
      Assert.assertEquals("Not precise enough",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getExchangeStore().delete(ByteArray.fromLong(1L));
      dbManager.getExchangeStore().delete(ByteArray.fromLong(2L));
      dbManager.getExchangeV2Store().delete(ByteArray.fromLong(1L));
      dbManager.getExchangeV2Store().delete(ByteArray.fromLong(2L));
    }

  }

  /**
   * SameTokenName open, Not precise enough
   */
  @Test
  public void SameTokenNameOpenNotPreciseEnough() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    InitExchangeSameTokenNameActive();
    long exchangeId = 1;
    String firstTokenId = "123";
    long quant = 9991L;
    String secondTokenId = "456";
    ExchangeWithdrawActuator actuator = new ExchangeWithdrawActuator(getContract(
            OWNER_ADDRESS_FIRST, exchangeId, secondTokenId, quant),
            dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Not precise enough",
              e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

    quant = 10001;
    actuator = new ExchangeWithdrawActuator(getContract(
            OWNER_ADDRESS_FIRST, exchangeId, secondTokenId, quant),
            dbManager);
    ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
      Assert.assertEquals("Not precise enough",
              e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getExchangeStore().delete(ByteArray.fromLong(1L));
      dbManager.getExchangeStore().delete(ByteArray.fromLong(2L));
      dbManager.getExchangeV2Store().delete(ByteArray.fromLong(1L));
      dbManager.getExchangeV2Store().delete(ByteArray.fromLong(2L));
    }

  }

  /**
   * SameTokenName close, Not precise enough
   */
  @Test
  public void SameTokenNameCloseNotPreciseEnough2() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    InitExchangeBeforeSameTokenNameActive();
    long exchangeId = 3;
    String firstTokenId = "abc";
    long quant = 1L;
    String secondTokenId = "def";
    ExchangeWithdrawActuator actuator = new ExchangeWithdrawActuator(getContract(
        OWNER_ADDRESS_FIRST, exchangeId, firstTokenId, quant),
        dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("withdraw another token quant must greater than zero",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

    quant = 11;
    actuator = new ExchangeWithdrawActuator(getContract(
        OWNER_ADDRESS_FIRST, exchangeId, secondTokenId, quant),
        dbManager);
    ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Not precise enough",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getExchangeStore().delete(ByteArray.fromLong(1L));
      dbManager.getExchangeStore().delete(ByteArray.fromLong(2L));
      dbManager.getExchangeV2Store().delete(ByteArray.fromLong(1L));
      dbManager.getExchangeV2Store().delete(ByteArray.fromLong(2L));
    }

  }

  /**
   * SameTokenName open, Not precise enough
   */
  @Test
  public void SameTokenNameOpenNotPreciseEnough2() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    InitExchangeSameTokenNameActive();
    long exchangeId = 3;
    String firstTokenId = "123";
    long quant = 1L;
    String secondTokenId = "456";
    ExchangeWithdrawActuator actuator = new ExchangeWithdrawActuator(getContract(
            OWNER_ADDRESS_FIRST, exchangeId, firstTokenId, quant),
            dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("withdraw another token quant must greater than zero",
              e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

    quant = 11;
    actuator = new ExchangeWithdrawActuator(getContract(
            OWNER_ADDRESS_FIRST, exchangeId, secondTokenId, quant),
            dbManager);
    ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Not precise enough",
              e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getExchangeStore().delete(ByteArray.fromLong(1L));
      dbManager.getExchangeStore().delete(ByteArray.fromLong(2L));
      dbManager.getExchangeV2Store().delete(ByteArray.fromLong(1L));
      dbManager.getExchangeV2Store().delete(ByteArray.fromLong(2L));
    }

  }

  /**
   * SameTokenName close, exchange balance is not enough
   */
  @Test
  public void SameTokenNameCloseExchangeBalanceIsNotEnough() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    InitExchangeBeforeSameTokenNameActive();
    long exchangeId = 1;
    String firstTokenId = "abc";
    long firstTokenQuant = 100_000_001L;
    String secondTokenId = "def";
    long secondTokenQuant = 400000000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    Map<String, Long> assetMap = accountCapsule.getAssetMap();
    Assert.assertEquals(10000_000000L, accountCapsule.getBalance());
    Assert.assertEquals(null, assetMap.get(firstTokenId));
    Assert.assertEquals(null, assetMap.get(secondTokenId));

    ExchangeWithdrawActuator actuator = new ExchangeWithdrawActuator(getContract(
        OWNER_ADDRESS_FIRST, exchangeId, firstTokenId, firstTokenQuant),
        dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("exchange balance is not enough",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getExchangeStore().delete(ByteArray.fromLong(1L));
      dbManager.getExchangeStore().delete(ByteArray.fromLong(2L));
      dbManager.getExchangeV2Store().delete(ByteArray.fromLong(1L));
      dbManager.getExchangeV2Store().delete(ByteArray.fromLong(2L));
    }
  }

  /**
   * SameTokenName open, exchange balance is not enough
   */
  @Test
  public void SameTokenNameOpenExchangeBalanceIsNotEnough() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    InitExchangeSameTokenNameActive();
    long exchangeId = 1;
    String firstTokenId = "123";
    long firstTokenQuant = 100_000_001L;
    String secondTokenId = "456";
    long secondTokenQuant = 400000000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    Map<String, Long> assetV2Map = accountCapsule.getAssetMapV2();
    Assert.assertEquals(10000_000000L, accountCapsule.getBalance());
    Assert.assertEquals(null, assetV2Map.get(firstTokenId));
    Assert.assertEquals(null, assetV2Map.get(secondTokenId));

    ExchangeWithdrawActuator actuator = new ExchangeWithdrawActuator(getContract(
            OWNER_ADDRESS_FIRST, exchangeId, firstTokenId, firstTokenQuant),
            dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("exchange balance is not enough",
              e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getExchangeStore().delete(ByteArray.fromLong(1L));
      dbManager.getExchangeStore().delete(ByteArray.fromLong(2L));
      dbManager.getExchangeV2Store().delete(ByteArray.fromLong(1L));
      dbManager.getExchangeV2Store().delete(ByteArray.fromLong(2L));
    }
  }

  /**
   * SameTokenName close, exchange balance is not enough
   */
  @Test
  public void SameTokenNameCloseExchangeBalanceIsNotEnough2() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    InitExchangeBeforeSameTokenNameActive();
    long exchangeId = 2;
    String firstTokenId = "_";
    long firstTokenQuant = 1000_000_000001L;
    String secondTokenId = "def";
    long secondTokenQuant = 400000000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    Map<String, Long> assetMap = accountCapsule.getAssetMap();
    Assert.assertEquals(10000_000000L, accountCapsule.getBalance());
    Assert.assertEquals(null, assetMap.get(secondTokenId));

    ExchangeWithdrawActuator actuator = new ExchangeWithdrawActuator(getContract(
        OWNER_ADDRESS_FIRST, exchangeId, firstTokenId, firstTokenQuant),
        dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("exchange balance is not enough",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getExchangeStore().delete(ByteArray.fromLong(1L));
      dbManager.getExchangeStore().delete(ByteArray.fromLong(2L));
      dbManager.getExchangeV2Store().delete(ByteArray.fromLong(1L));
      dbManager.getExchangeV2Store().delete(ByteArray.fromLong(2L));
    }
  }

  /**
   * SameTokenName open, exchange balance is not enough
   */
  @Test
  public void SameTokenNameOpenExchangeBalanceIsNotEnough2() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    InitExchangeSameTokenNameActive();
    long exchangeId = 2;
    String firstTokenId = "_";
    long firstTokenQuant = 1000_000_000001L;
    String secondTokenId = "456";
    long secondTokenQuant = 400000000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    Map<String, Long> assetV2Map = accountCapsule.getAssetMapV2();
    Assert.assertEquals(10000_000000L, accountCapsule.getBalance());
    Assert.assertEquals(null, assetV2Map.get(secondTokenId));

    ExchangeWithdrawActuator actuator = new ExchangeWithdrawActuator(getContract(
            OWNER_ADDRESS_FIRST, exchangeId, firstTokenId, firstTokenQuant),
            dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("exchange balance is not enough",
              e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getExchangeStore().delete(ByteArray.fromLong(1L));
      dbManager.getExchangeStore().delete(ByteArray.fromLong(2L));
      dbManager.getExchangeV2Store().delete(ByteArray.fromLong(1L));
      dbManager.getExchangeV2Store().delete(ByteArray.fromLong(2L));
    }
  }

  /**
   * SameTokenName open, Invalid param
   * "token id is not a valid number"
   */
  @Test
  public void SameTokenNameOpenInvalidParam() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    InitExchangeSameTokenNameActive();
    long exchangeId = 1;
    TransactionResultCapsule ret = new TransactionResultCapsule();

    //token id is not a valid number
    ExchangeWithdrawActuator actuator = new ExchangeWithdrawActuator(getContract(
            OWNER_ADDRESS_FIRST, exchangeId, "abc", 1000),
            dbManager);
    try {
      actuator.validate();
      actuator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("token id is not a valid number",
              e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getExchangeStore().delete(ByteArray.fromLong(1L));
      dbManager.getExchangeStore().delete(ByteArray.fromLong(2L));
      dbManager.getExchangeV2Store().delete(ByteArray.fromLong(1L));
      dbManager.getExchangeV2Store().delete(ByteArray.fromLong(2L));
    }
  }
}