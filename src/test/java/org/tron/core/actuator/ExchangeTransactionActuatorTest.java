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

public class ExchangeTransactionActuatorTest {

  private static TronApplicationContext context;
  private static Manager dbManager;
  private static final String dbPath = "output_ExchangeTransaction_test";
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

  private Any getContract(String address, long exchangeId, String tokenId,
      long quant, long expected) {
    return Any.pack(
        Contract.ExchangeTransactionContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(address)))
            .setExchangeId(exchangeId)
            .setTokenId(ByteString.copyFrom(tokenId.getBytes()))
            .setQuant(quant)
            .setExpected(expected)
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
                    "_".getBytes(),
                    "abc".getBytes());
    exchangeCapsule.setBalance(1_000_000_000_000L, 10_000_000L); // 1M TRX == 10M abc
    ExchangeCapsule exchangeCapsule2 =
            new ExchangeCapsule(
                    ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_FIRST)),
                    2,
                    1000000,
                    "abc".getBytes(),
                    "def".getBytes());
    exchangeCapsule2.setBalance(100000000L, 200000000L);
    dbManager.getExchangeStore().put(exchangeCapsule.createDbKey(), exchangeCapsule);
    dbManager.getExchangeStore().put(exchangeCapsule2.createDbKey(), exchangeCapsule2);
    //V2
    ExchangeCapsule exchangeCapsule3 =
            new ExchangeCapsule(
                    ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_FIRST)),
                    1,
                    1000000,
                    "_".getBytes(),
                    "1".getBytes());
    exchangeCapsule3.setBalance(1_000_000_000_000L, 10_000_000L); // 1M TRX == 10M abc
    ExchangeCapsule exchangeCapsule4 =
            new ExchangeCapsule(
                    ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_FIRST)),
                    2,
                    1000000,
                    "1".getBytes(),   //abc's Id
                    "2".getBytes()); //def's Id
    exchangeCapsule4.setBalance(100000000L, 200000000L);
    dbManager.getExchangeV2Store().put(exchangeCapsule3.createDbKey(), exchangeCapsule3);
    dbManager.getExchangeV2Store().put(exchangeCapsule4.createDbKey(), exchangeCapsule4);
  }

  private void InitExchangeSameTokenNameActive() {
    AssetIssueCapsule assetIssueCapsule1 = new AssetIssueCapsule(
            AssetIssueContract.newBuilder()
                    .setName(ByteString.copyFrom("123".getBytes()))
                    .setId(String.valueOf(1L))
                    .build());
    AssetIssueCapsule assetIssueCapsule2 =
            new AssetIssueCapsule(
                    AssetIssueContract.newBuilder()
                            .setName(ByteString.copyFrom("456".getBytes()))
                            .setId(String.valueOf(2))
                            .build());
    dbManager.getAssetIssueV2Store().put(assetIssueCapsule1.createDbV2Key(),
            assetIssueCapsule1);
    dbManager.getAssetIssueV2Store().put(assetIssueCapsule2.createDbV2Key(),
            assetIssueCapsule2);

    ExchangeCapsule exchangeCapsule =
            new ExchangeCapsule(
                    ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_FIRST)),
                    1,
                    1000000,
                    "_".getBytes(),
                    "123".getBytes());
    exchangeCapsule.setBalance(1_000_000_000_000L, 10_000_000L); // 1M TRX == 10M abc
    ExchangeCapsule exchangeCapsule2 =
            new ExchangeCapsule(
                    ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_FIRST)),
                    2,
                    1000000,
                    "123".getBytes(),
                    "456".getBytes());
    exchangeCapsule2.setBalance(100000000L, 200000000L);
    dbManager.getExchangeV2Store().put(exchangeCapsule.createDbKey(), exchangeCapsule);
    dbManager.getExchangeV2Store().put(exchangeCapsule2.createDbKey(), exchangeCapsule2);
  }

  /**
   * SameTokenName close,first transaction Exchange,result is success.
   */
  @Test
  public void SameTokenNameCloseSuccessExchangeTransaction() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    InitExchangeBeforeSameTokenNameActive();
    long exchangeId = 1;
    String tokenId = "_";
    long quant = 100_000_000L; // use 100 TRX to buy abc

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_SECOND);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    Map<String, Long> assetMap = accountCapsule.getAssetMap();
    Assert.assertEquals(20000_000000L, accountCapsule.getBalance());
    Assert.assertEquals(null, assetMap.get("def"));

    ExchangeTransactionActuator actuator = new ExchangeTransactionActuator(getContract(
        OWNER_ADDRESS_SECOND, exchangeId, tokenId, quant, 1),
        dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      ExchangeCapsule exchangeCapsule = dbManager.getExchangeStore()
          .get(ByteArray.fromLong(exchangeId));
      Assert.assertNotNull(exchangeCapsule);
      long firstTokenBalance = exchangeCapsule.getFirstTokenBalance();
      long secondTokenBalance = exchangeCapsule.getSecondTokenBalance();

      Assert.assertEquals(exchangeId, exchangeCapsule.getID());
      Assert.assertEquals(tokenId, ByteArray.toStr(exchangeCapsule.getFirstTokenId()));
      Assert.assertEquals(1_000_000_000_000L, firstTokenBalance);
      Assert.assertEquals("abc", ByteArray.toStr(exchangeCapsule.getSecondTokenId()));
      Assert.assertEquals(10_000_000L, secondTokenBalance);

      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      //V1
      exchangeCapsule = dbManager.getExchangeStore().get(ByteArray.fromLong(exchangeId));
      Assert.assertNotNull(exchangeCapsule);
      Assert.assertEquals(exchangeId, exchangeCapsule.getID());
      Assert.assertEquals(1000000, exchangeCapsule.getCreateTime());
      Assert.assertTrue(Arrays.equals(tokenId.getBytes(), exchangeCapsule.getFirstTokenId()));
      Assert.assertEquals(tokenId, ByteArray.toStr(exchangeCapsule.getFirstTokenId()));
      Assert.assertEquals(firstTokenBalance + quant, exchangeCapsule.getFirstTokenBalance());
      Assert.assertEquals("abc", ByteArray.toStr(exchangeCapsule.getSecondTokenId()));
      Assert.assertEquals(9999001L, exchangeCapsule.getSecondTokenBalance());
      //V2
      ExchangeCapsule exchangeCapsule2 =
              dbManager.getExchangeV2Store().get(ByteArray.fromLong(exchangeId));
      Assert.assertNotNull(exchangeCapsule2);
      Assert.assertEquals(exchangeId, exchangeCapsule2.getID());
      Assert.assertEquals(1000000, exchangeCapsule2.getCreateTime());
      Assert.assertEquals(firstTokenBalance + quant, exchangeCapsule2.getFirstTokenBalance());
      Assert.assertEquals(9999001L, exchangeCapsule2.getSecondTokenBalance());

      accountCapsule = dbManager.getAccountStore().get(ownerAddress);
      assetMap = accountCapsule.getAssetMap();
      Assert.assertEquals(20000_000000L - quant, accountCapsule.getBalance());
      Assert.assertEquals(999L, assetMap.get("abc").longValue());

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
  public void oldNotUpdateSuccessExchangeTransaction() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    InitExchangeBeforeSameTokenNameActive();
    long exchangeId = 1;
    String tokenId = "_";
    long quant = 100_000_000L; // use 100 TRX to buy abc

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_SECOND);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    Map<String, Long> assetMap = accountCapsule.getAssetMap();
    Assert.assertEquals(20000_000000L, accountCapsule.getBalance());
    Assert.assertEquals(null, assetMap.get("def"));

    ExchangeTransactionActuator actuator = new ExchangeTransactionActuator(getContract(
            OWNER_ADDRESS_SECOND, exchangeId, tokenId, quant, 1),
            dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);

    try {
      ExchangeCapsule exchangeCapsule = dbManager.getExchangeStore()
              .get(ByteArray.fromLong(exchangeId));
      Assert.assertNotNull(exchangeCapsule);
      long firstTokenBalance = exchangeCapsule.getFirstTokenBalance();
      long secondTokenBalance = exchangeCapsule.getSecondTokenBalance();

      Assert.assertEquals(exchangeId, exchangeCapsule.getID());
      Assert.assertEquals(tokenId, ByteArray.toStr(exchangeCapsule.getFirstTokenId()));
      Assert.assertEquals(1_000_000_000_000L, firstTokenBalance);
      Assert.assertEquals("abc", ByteArray.toStr(exchangeCapsule.getSecondTokenId()));
      Assert.assertEquals(10_000_000L, secondTokenBalance);

      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      //V1 not update
      exchangeCapsule = dbManager.getExchangeStore().get(ByteArray.fromLong(exchangeId));
      Assert.assertNotNull(exchangeCapsule);
      Assert.assertEquals(exchangeId, exchangeCapsule.getID());
      Assert.assertEquals(1000000, exchangeCapsule.getCreateTime());
      Assert.assertTrue(Arrays.equals(tokenId.getBytes(), exchangeCapsule.getFirstTokenId()));
      Assert.assertEquals(tokenId, ByteArray.toStr(exchangeCapsule.getFirstTokenId()));
      Assert.assertEquals("abc", ByteArray.toStr(exchangeCapsule.getSecondTokenId()));
      Assert.assertNotEquals(firstTokenBalance + quant, exchangeCapsule.getFirstTokenBalance());
      Assert.assertNotEquals(9999001L, exchangeCapsule.getSecondTokenBalance());
      //V2
      ExchangeCapsule exchangeCapsule2 =
              dbManager.getExchangeV2Store().get(ByteArray.fromLong(exchangeId));
      Assert.assertNotNull(exchangeCapsule2);
      Assert.assertEquals(exchangeId, exchangeCapsule2.getID());
      Assert.assertEquals(1000000, exchangeCapsule2.getCreateTime());
      Assert.assertEquals(firstTokenBalance + quant, exchangeCapsule2.getFirstTokenBalance());
      Assert.assertEquals(9999001L, exchangeCapsule2.getSecondTokenBalance());

      accountCapsule = dbManager.getAccountStore().get(ownerAddress);
      assetMap = accountCapsule.getAssetMapV2();
      Assert.assertEquals(20000_000000L - quant, accountCapsule.getBalance());
      Assert.assertEquals(999L, assetMap.get("1").longValue());

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
   * SameTokenName open,first transaction Exchange,result is success.
   */
  @Test
  public void SameTokenNameOpenSuccessExchangeTransaction() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    InitExchangeSameTokenNameActive();
    long exchangeId = 1;
    String tokenId = "_";
    long quant = 100_000_000L; // use 100 TRX to buy abc

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_SECOND);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    Map<String, Long> assetV2Map = accountCapsule.getAssetMapV2();
    Assert.assertEquals(20000_000000L, accountCapsule.getBalance());
    Assert.assertEquals(null, assetV2Map.get("456"));

    ExchangeTransactionActuator actuator = new ExchangeTransactionActuator(getContract(
            OWNER_ADDRESS_SECOND, exchangeId, tokenId, quant, 1),
            dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      ExchangeCapsule exchangeV2Capsule = dbManager.getExchangeV2Store()
              .get(ByteArray.fromLong(exchangeId));
      Assert.assertNotNull(exchangeV2Capsule);
      long firstTokenBalance = exchangeV2Capsule.getFirstTokenBalance();
      long secondTokenBalance = exchangeV2Capsule.getSecondTokenBalance();

      Assert.assertEquals(exchangeId, exchangeV2Capsule.getID());
      Assert.assertEquals(tokenId, ByteArray.toStr(exchangeV2Capsule.getFirstTokenId()));
      Assert.assertEquals(1_000_000_000_000L, firstTokenBalance);
      Assert.assertEquals("123", ByteArray.toStr(exchangeV2Capsule.getSecondTokenId()));
      Assert.assertEquals(10_000_000L, secondTokenBalance);

      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      // V1,Data is no longer update
      Assert.assertFalse(dbManager.getExchangeStore().has(ByteArray.fromLong(exchangeId)));

      //V2
      exchangeV2Capsule = dbManager.getExchangeV2Store().get(ByteArray.fromLong(exchangeId));
      Assert.assertNotNull(exchangeV2Capsule);
      Assert.assertEquals(exchangeId, exchangeV2Capsule.getID());
      Assert.assertEquals(1000000, exchangeV2Capsule.getCreateTime());
      Assert.assertTrue(Arrays.equals(tokenId.getBytes(), exchangeV2Capsule.getFirstTokenId()));
      Assert.assertEquals(tokenId, ByteArray.toStr(exchangeV2Capsule.getFirstTokenId()));
      Assert.assertEquals(firstTokenBalance + quant, exchangeV2Capsule.getFirstTokenBalance());
      Assert.assertEquals("123", ByteArray.toStr(exchangeV2Capsule.getSecondTokenId()));
      Assert.assertEquals(9999001L, exchangeV2Capsule.getSecondTokenBalance());

      accountCapsule = dbManager.getAccountStore().get(ownerAddress);
      assetV2Map = accountCapsule.getAssetMapV2();
      Assert.assertEquals(20000_000000L - quant, accountCapsule.getBalance());
      Assert.assertEquals(999L, assetV2Map.get("123").longValue());

      Assert.assertEquals(999L, ret.getExchangeReceivedAmount());

    } catch (ContractValidateException e) {
      logger.info(e.getMessage());
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } catch (ItemNotFoundException e) {
      Assert.assertFalse(e instanceof ItemNotFoundException);
    }finally {
      dbManager.getExchangeStore().delete(ByteArray.fromLong(1L));
      dbManager.getExchangeStore().delete(ByteArray.fromLong(2L));
      dbManager.getExchangeV2Store().delete(ByteArray.fromLong(1L));
      dbManager.getExchangeV2Store().delete(ByteArray.fromLong(2L));
    }
  }

  /**
   * SameTokenName close,second transaction Exchange,result is success.
   */
  @Test
  public void SameTokenNameCloseSuccessExchangeTransaction2() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    InitExchangeBeforeSameTokenNameActive();
    long exchangeId = 2;
    String tokenId = "abc";
    long quant = 1_000L;
    String buyTokenId = "def";

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_SECOND);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.addAssetAmount(tokenId.getBytes(), 10000);
    Map<String, Long> assetMap = accountCapsule.getAssetMap();
    Assert.assertEquals(20000_000000L, accountCapsule.getBalance());
    Assert.assertEquals(null, assetMap.get(buyTokenId));
    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);

    ExchangeTransactionActuator actuator = new ExchangeTransactionActuator(getContract(
        OWNER_ADDRESS_SECOND, exchangeId, tokenId, quant, 1),
        dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      ExchangeCapsule exchangeCapsule = dbManager.getExchangeStore()
          .get(ByteArray.fromLong(exchangeId));
      Assert.assertNotNull(exchangeCapsule);
      long firstTokenBalance = exchangeCapsule.getFirstTokenBalance();
      long secondTokenBalance = exchangeCapsule.getSecondTokenBalance();

      Assert.assertEquals(exchangeId, exchangeCapsule.getID());
      Assert.assertEquals(tokenId, ByteArray.toStr(exchangeCapsule.getFirstTokenId()));
      Assert.assertEquals(100000000L, firstTokenBalance);
      Assert.assertEquals("def", ByteArray.toStr(exchangeCapsule.getSecondTokenId()));
      Assert.assertEquals(200000000L, secondTokenBalance);

      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      //V1
      exchangeCapsule = dbManager.getExchangeStore().get(ByteArray.fromLong(exchangeId));
      Assert.assertNotNull(exchangeCapsule);
      Assert.assertEquals(exchangeId, exchangeCapsule.getID());
      Assert.assertEquals(1000000, exchangeCapsule.getCreateTime());
      Assert.assertTrue(Arrays.equals(tokenId.getBytes(), exchangeCapsule.getFirstTokenId()));
      Assert.assertEquals(tokenId, ByteArray.toStr(exchangeCapsule.getFirstTokenId()));
      Assert.assertEquals(firstTokenBalance + quant, exchangeCapsule.getFirstTokenBalance());
      Assert.assertEquals("def", ByteArray.toStr(exchangeCapsule.getSecondTokenId()));
      Assert.assertEquals(199998001L, exchangeCapsule.getSecondTokenBalance());
      //V2
      ExchangeCapsule exchangeCapsule2 =
              dbManager.getExchangeStore().get(ByteArray.fromLong(exchangeId));
      Assert.assertNotNull(exchangeCapsule2);
      Assert.assertEquals(exchangeId, exchangeCapsule2.getID());
      Assert.assertEquals(1000000, exchangeCapsule2.getCreateTime());
//      Assert.assertTrue(Arrays.equals(tokenId.getBytes(), exchangeCapsule2.getFirstTokenId()));
//      Assert.assertEquals(tokenId, ByteArray.toStr(exchangeCapsule2.getFirstTokenId()));
      Assert.assertEquals(firstTokenBalance + quant, exchangeCapsule2.getFirstTokenBalance());
//      Assert.assertEquals("def", ByteArray.toStr(exchangeCapsule2.getSecondTokenId()));
      Assert.assertEquals(199998001L, exchangeCapsule2.getSecondTokenBalance());

      accountCapsule = dbManager.getAccountStore().get(ownerAddress);
      assetMap = accountCapsule.getAssetMap();
      Assert.assertEquals(9000L, assetMap.get("abc").longValue());
      Assert.assertEquals(1999L, assetMap.get("def").longValue());

    } catch (ContractValidateException e) {
      logger.info(e.getMessage());
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } catch (ItemNotFoundException e) {
      Assert.assertFalse(e instanceof ItemNotFoundException);
    }finally {
      dbManager.getExchangeStore().delete(ByteArray.fromLong(1L));
      dbManager.getExchangeStore().delete(ByteArray.fromLong(2L));
      dbManager.getExchangeV2Store().delete(ByteArray.fromLong(1L));
      dbManager.getExchangeV2Store().delete(ByteArray.fromLong(2L));
    }
  }

  /**
   * SameTokenName open,second transaction Exchange,result is success.
   */
  @Test
  public void SameTokenNameOpenSuccessExchangeTransaction2() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    InitExchangeSameTokenNameActive();
    long exchangeId = 2;
    String tokenId = "123";
    long quant = 1_000L;
    String buyTokenId = "456";

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_SECOND);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.addAssetAmountV2(tokenId.getBytes(), 10000, dbManager );
    Map<String, Long> assetV2Map = accountCapsule.getAssetMapV2();
    Assert.assertEquals(20000_000000L, accountCapsule.getBalance());
    Assert.assertEquals(null, assetV2Map.get(buyTokenId));
    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);

    ExchangeTransactionActuator actuator = new ExchangeTransactionActuator(getContract(
            OWNER_ADDRESS_SECOND, exchangeId, tokenId, quant, 1),
            dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      ExchangeCapsule exchangeCapsuleV2 = dbManager.getExchangeV2Store()
              .get(ByteArray.fromLong(exchangeId));
      Assert.assertNotNull(exchangeCapsuleV2);
      long firstTokenBalance = exchangeCapsuleV2.getFirstTokenBalance();
      long secondTokenBalance = exchangeCapsuleV2.getSecondTokenBalance();

      Assert.assertEquals(exchangeId, exchangeCapsuleV2.getID());
      Assert.assertEquals(tokenId, ByteArray.toStr(exchangeCapsuleV2.getFirstTokenId()));
      Assert.assertEquals(100000000L, firstTokenBalance);
      Assert.assertEquals("456", ByteArray.toStr(exchangeCapsuleV2.getSecondTokenId()));
      Assert.assertEquals(200000000L, secondTokenBalance);

      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      // V1,Data is no longer update
      Assert.assertFalse(dbManager.getExchangeStore().has(ByteArray.fromLong(exchangeId)));

      //V2
      exchangeCapsuleV2 = dbManager.getExchangeV2Store().get(ByteArray.fromLong(exchangeId));
      Assert.assertNotNull(exchangeCapsuleV2);
      Assert.assertEquals(exchangeId, exchangeCapsuleV2.getID());
      Assert.assertEquals(1000000, exchangeCapsuleV2.getCreateTime());
      Assert.assertTrue(Arrays.equals(tokenId.getBytes(), exchangeCapsuleV2.getFirstTokenId()));
      Assert.assertEquals(tokenId, ByteArray.toStr(exchangeCapsuleV2.getFirstTokenId()));
      Assert.assertEquals(firstTokenBalance + quant, exchangeCapsuleV2.getFirstTokenBalance());
      Assert.assertEquals("456", ByteArray.toStr(exchangeCapsuleV2.getSecondTokenId()));
      Assert.assertEquals(199998001L, exchangeCapsuleV2.getSecondTokenBalance());

      accountCapsule = dbManager.getAccountStore().get(ownerAddress);
      assetV2Map = accountCapsule.getAssetMapV2();
      Assert.assertEquals(9000L, assetV2Map.get("123").longValue());
      Assert.assertEquals(1999L, assetV2Map.get("456").longValue());

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
   * SameTokenName close,use Invalid Address, result is failed, exception is "Invalid address".
   */
  @Test
  public void SameTokenNameCloseInvalidAddress() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    InitExchangeBeforeSameTokenNameActive();
    long exchangeId = 2;
    String tokenId = "abc";
    long quant = 1_000L;
    String buyTokenId = "def";

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_SECOND);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.addAssetAmount(tokenId.getBytes(), 10000);
    Map<String, Long> assetMap = accountCapsule.getAssetMap();
    Assert.assertEquals(20000_000000L, accountCapsule.getBalance());
    Assert.assertEquals(null, assetMap.get(buyTokenId));
    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);

    ExchangeTransactionActuator actuator = new ExchangeTransactionActuator(getContract(
        OWNER_ADDRESS_INVALID, exchangeId, tokenId, quant, 1),
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
   * SameTokenName open,use Invalid Address, result is failed, exception is "Invalid address".
   */
  @Test
  public void SameTokenNameOpenInvalidAddress() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    InitExchangeSameTokenNameActive();
    long exchangeId = 2;
    String tokenId = "123";
    long quant = 1_000L;
    String buyTokenId = "456";

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_SECOND);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.addAssetAmountV2(tokenId.getBytes(), 10000, dbManager);
    Map<String, Long> assetV2Map = accountCapsule.getAssetMapV2();
    Assert.assertEquals(20000_000000L, accountCapsule.getBalance());
    Assert.assertEquals(null, assetV2Map.get(buyTokenId));
    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);

    ExchangeTransactionActuator actuator = new ExchangeTransactionActuator(getContract(
            OWNER_ADDRESS_INVALID, exchangeId, tokenId, quant, 1),
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
   * SameTokenName close,use AccountStore not exists, result is failed, exception is "account not exists".
   */
  @Test
  public void SameTokenNameCloseNoAccount() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    InitExchangeBeforeSameTokenNameActive();
    long exchangeId = 2;
    String tokenId = "abc";
    long quant = 1_000L;
    String buyTokenId = "def";

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_SECOND);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.addAssetAmount(tokenId.getBytes(), 10000);
    Map<String, Long> assetMap = accountCapsule.getAssetMap();
    Assert.assertEquals(20000_000000L, accountCapsule.getBalance());
    Assert.assertEquals(null, assetMap.get(buyTokenId));
    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);

    ExchangeTransactionActuator actuator = new ExchangeTransactionActuator(getContract(
        OWNER_ADDRESS_NOACCOUNT, exchangeId, tokenId, quant, 1),
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
   * SameTokenName open,use AccountStore not exists, result is failed, exception is "account not
   * exists".
   */
  @Test
  public void SameTokenNameOpenNoAccount() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    InitExchangeSameTokenNameActive();
    long exchangeId = 2;
    String tokenId = "123";
    long quant = 1_000L;
    String buyTokenId = "456";

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_SECOND);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.addAssetAmountV2(tokenId.getBytes(), 10000, dbManager );
    Map<String, Long> assetV2Map = accountCapsule.getAssetMapV2();
    Assert.assertEquals(20000_000000L, accountCapsule.getBalance());
    Assert.assertEquals(null, assetV2Map.get(buyTokenId));
    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);

    ExchangeTransactionActuator actuator = new ExchangeTransactionActuator(getContract(
            OWNER_ADDRESS_NOACCOUNT, exchangeId, tokenId, quant, 1),
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
   * SameTokenName close,Exchange not exists
   */
  @Test
  public void SameTokenNameCloseExchangeNotExist() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    InitExchangeBeforeSameTokenNameActive();
    long exchangeId = 3;
    String tokenId = "abc";
    long quant = 1_000L;
    String buyTokenId = "def";

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_SECOND);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.addAssetAmount(tokenId.getBytes(), 10000);
    Map<String, Long> assetMap = accountCapsule.getAssetMap();
    Assert.assertEquals(20000_000000L, accountCapsule.getBalance());
    Assert.assertEquals(null, assetMap.get(buyTokenId));
    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);

    ExchangeTransactionActuator actuator = new ExchangeTransactionActuator(getContract(
        OWNER_ADDRESS_SECOND, exchangeId, tokenId, quant, 1),
        dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      fail("Exchange not exists");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Exchange[3] not exists",
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
   * SameTokenName open,Exchange not exists
   */
  @Test
  public void SameTokenNameOpenExchangeNotExist() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    InitExchangeSameTokenNameActive();
    long exchangeId = 3;
    String tokenId = "123";
    long quant = 1_000L;
    String buyTokenId = "456";

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_SECOND);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.addAssetAmountV2(tokenId.getBytes(), 10000, dbManager );
    Map<String, Long> assetV2Map = accountCapsule.getAssetMapV2();
    Assert.assertEquals(20000_000000L, accountCapsule.getBalance());
    Assert.assertEquals(null, assetV2Map.get(buyTokenId));
    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);

    ExchangeTransactionActuator actuator = new ExchangeTransactionActuator(getContract(
            OWNER_ADDRESS_SECOND, exchangeId, tokenId, quant, 1),
            dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      fail("Exchange not exists");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Exchange[3] not exists",
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
   * SameTokenName close,token is not in exchange
   */
  @Test
  public void SameTokenNameCloseTokenIsNotInExchange() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    InitExchangeBeforeSameTokenNameActive();
    long exchangeId = 1;
    String tokenId = "ddd";
    long quant = 1_000L;
    String buyTokenId = "def";

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_SECOND);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.addAssetAmount(tokenId.getBytes(), 10000);
    Map<String, Long> assetMap = accountCapsule.getAssetMap();
    Assert.assertEquals(20000_000000L, accountCapsule.getBalance());
    Assert.assertEquals(null, assetMap.get(buyTokenId));
    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);

    ExchangeTransactionActuator actuator = new ExchangeTransactionActuator(getContract(
        OWNER_ADDRESS_SECOND, exchangeId, tokenId, quant, 1),
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
   * SameTokenName open,token is not in exchange
   */
  @Test
  public void SameTokenNameOpenTokenIsNotInExchange() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    InitExchangeSameTokenNameActive();
    long exchangeId = 1;
    String tokenId = "999";
    long quant = 1_000L;
    String buyTokenId = "456";

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_SECOND);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.addAssetAmountV2(tokenId.getBytes(), 10000, dbManager );
    Map<String, Long> assetV2Map = accountCapsule.getAssetMapV2();
    Assert.assertEquals(20000_000000L, accountCapsule.getBalance());
    Assert.assertEquals(null, assetV2Map.get(buyTokenId));
    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);

    ExchangeTransactionActuator actuator = new ExchangeTransactionActuator(getContract(
            OWNER_ADDRESS_SECOND, exchangeId, tokenId, quant, 1),
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
   * SameTokenName close,Token balance in exchange is equal with 0, the exchange has been closed"
   */
  @Test
  public void SameTokenNameCloseTokenBalanceZero() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    InitExchangeBeforeSameTokenNameActive();
    long exchangeId = 2;
    String tokenId = "abc";
    long quant = 1_000L;
    String buyTokenId = "def";

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_SECOND);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.addAssetAmount(tokenId.getBytes(), 10000);
    Map<String, Long> assetMap = accountCapsule.getAssetMap();
    Assert.assertEquals(20000_000000L, accountCapsule.getBalance());
    Assert.assertEquals(null, assetMap.get(buyTokenId));
    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);

    ExchangeTransactionActuator actuator = new ExchangeTransactionActuator(getContract(
        OWNER_ADDRESS_SECOND, exchangeId, tokenId, quant, 1),
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
   * SameTokenName open,Token balance in exchange is equal with 0, the exchange has been closed"
   */
  @Test
  public void SameTokenNameOpenTokenBalanceZero() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    InitExchangeSameTokenNameActive();
    long exchangeId = 2;
    String tokenId = "123";
    long quant = 1_000L;
    String buyTokenId = "456";

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_SECOND);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.addAssetAmountV2(tokenId.getBytes(), 10000, dbManager);
    Map<String, Long> assetV2Map = accountCapsule.getAssetMapV2();
    Assert.assertEquals(20000_000000L, accountCapsule.getBalance());
    Assert.assertEquals(null, assetV2Map.get(buyTokenId));
    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);

    ExchangeTransactionActuator actuator = new ExchangeTransactionActuator(getContract(
            OWNER_ADDRESS_SECOND, exchangeId, tokenId, quant, 1),
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
   * SameTokenName close,token quant must greater than zero
   */
  @Test
  public void SameTokenNameCloseTokenQuantLessThanZero() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    InitExchangeBeforeSameTokenNameActive();
    long exchangeId = 2;
    String tokenId = "abc";
    long quant = -1_000L;
    String buyTokenId = "def";

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_SECOND);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.addAssetAmount(tokenId.getBytes(), 10000);
    Map<String, Long> assetMap = accountCapsule.getAssetMap();
    Assert.assertEquals(20000_000000L, accountCapsule.getBalance());
    Assert.assertEquals(null, assetMap.get(buyTokenId));
    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);

    ExchangeTransactionActuator actuator = new ExchangeTransactionActuator(getContract(
        OWNER_ADDRESS_SECOND, exchangeId, tokenId, quant, 1),
        dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("token quant must greater than zero",
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
   * SameTokenName open,token quant must greater than zero
   */
  @Test
  public void SameTokenNameOpenTokenQuantLessThanZero() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    InitExchangeSameTokenNameActive();
    long exchangeId = 2;
    String tokenId = "123";
    long quant = -1_000L;
    String buyTokenId = "456";

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_SECOND);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.addAssetAmountV2(tokenId.getBytes(), 10000, dbManager);
    Map<String, Long> assetV2Map = accountCapsule.getAssetMapV2();
    Assert.assertEquals(20000_000000L, accountCapsule.getBalance());
    Assert.assertEquals(null, assetV2Map.get(buyTokenId));
    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);

    ExchangeTransactionActuator actuator = new ExchangeTransactionActuator(getContract(
            OWNER_ADDRESS_SECOND, exchangeId, tokenId, quant, 1),
            dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("token quant must greater than zero",
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
   * SameTokenName close,token balance must less than balanceLimit
   */
  @Test
  public void SameTokenNameCloseTokenBalanceGreaterThanBalanceLimit() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    InitExchangeBeforeSameTokenNameActive();
    long exchangeId = 2;
    String tokenId = "abc";
    long quant = 1_000_000_000_000_001L;
    String buyTokenId = "def";

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_SECOND);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.addAssetAmount(tokenId.getBytes(), 10000);
    Map<String, Long> assetMap = accountCapsule.getAssetMap();
    Assert.assertEquals(20000_000000L, accountCapsule.getBalance());
    Assert.assertEquals(null, assetMap.get(buyTokenId));
    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);

    ExchangeTransactionActuator actuator = new ExchangeTransactionActuator(getContract(
        OWNER_ADDRESS_SECOND, exchangeId, tokenId, quant, 1),
        dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("token balance must less than 1000000000000000",
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
   * SameTokenName open,token balance must less than balanceLimit
   */
  @Test
  public void SameTokenNameOpenTokenBalanceGreaterThanBalanceLimit() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    InitExchangeSameTokenNameActive();
    long exchangeId = 2;
    String tokenId = "123";
    long quant = 1_000_000_000_000_001L;
    String buyTokenId = "456";

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_SECOND);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.addAssetAmountV2(tokenId.getBytes(), 10000, dbManager);
    Map<String, Long> assetV2Map = accountCapsule.getAssetMapV2();
    Assert.assertEquals(20000_000000L, accountCapsule.getBalance());
    Assert.assertEquals(null, assetV2Map.get(buyTokenId));
    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);

    ExchangeTransactionActuator actuator = new ExchangeTransactionActuator(getContract(
            OWNER_ADDRESS_SECOND, exchangeId, tokenId, quant, 1),
            dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("token balance must less than 1000000000000000",
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
   * SameTokenName close,balance is not enough
   */
  @Test
  public void SameTokenNameCloseBalanceNotEnough() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    InitExchangeBeforeSameTokenNameActive();
    long exchangeId = 1;
    String tokenId = "_";
    long quant = 100_000000L;
    String buyTokenId = "abc";

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_SECOND);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    Map<String, Long> assetMap = accountCapsule.getAssetMap();
    accountCapsule.setBalance(quant - 1);
    Assert.assertEquals(null, assetMap.get(buyTokenId));
    dbManager.getAccountStore().put(ownerAddress, accountCapsule);

    ExchangeTransactionActuator actuator = new ExchangeTransactionActuator(getContract(
        OWNER_ADDRESS_SECOND, exchangeId, tokenId, quant, 1),
        dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("balance is not enough",
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
   * SameTokenName open,balance is not enough
   */
  @Test
  public void SameTokenNameOpenBalanceNotEnough() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    InitExchangeSameTokenNameActive();
    long exchangeId = 1;
    String tokenId = "_";
    long quant = 100_000000L;
    String buyTokenId = "123";

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_SECOND);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    Map<String, Long> assetV2Map = accountCapsule.getAssetMapV2();
    accountCapsule.setBalance(quant - 1);
    Assert.assertEquals(null, assetV2Map.get(buyTokenId));
    dbManager.getAccountStore().put(ownerAddress, accountCapsule);

    ExchangeTransactionActuator actuator = new ExchangeTransactionActuator(getContract(
            OWNER_ADDRESS_SECOND, exchangeId, tokenId, quant, 1),
            dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("balance is not enough",
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
   * SameTokenName close,token balance is not enough
   */
  @Test
  public void SameTokenNameCloseTokenBalanceNotEnough() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    InitExchangeBeforeSameTokenNameActive();
    long exchangeId = 2;
    String tokenId = "abc";
    long quant = 1_000L;
    String buyTokenId = "def";

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_SECOND);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.addAssetAmount(tokenId.getBytes(), quant - 1);
    Map<String, Long> assetMap = accountCapsule.getAssetMap();
    Assert.assertEquals(20000_000000L, accountCapsule.getBalance());
    Assert.assertEquals(null, assetMap.get(buyTokenId));
    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);

    ExchangeTransactionActuator actuator = new ExchangeTransactionActuator(getContract(
        OWNER_ADDRESS_SECOND, exchangeId, tokenId, quant, 1),
        dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("token balance is not enough",
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
   * SameTokenName open,token balance is not enough
   */
  @Test
  public void SameTokenNameOpenTokenBalanceNotEnough() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    InitExchangeSameTokenNameActive();
    long exchangeId = 2;
    String tokenId = "123";
    long quant = 1_000L;
    String buyTokenId = "456";

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_SECOND);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.addAssetAmountV2(tokenId.getBytes(), quant - 1, dbManager);
    Map<String, Long> assetV2Map = accountCapsule.getAssetMapV2();
    Assert.assertEquals(20000_000000L, accountCapsule.getBalance());
    Assert.assertEquals(null, assetV2Map.get(buyTokenId));
    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);

    ExchangeTransactionActuator actuator = new ExchangeTransactionActuator(getContract(
            OWNER_ADDRESS_SECOND, exchangeId, tokenId, quant, 1),
            dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("token balance is not enough",
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
   * SameTokenName close,token required must greater than expected
   */
  @Test
  public void SameTokenNameCloseTokenRequiredNotEnough() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    InitExchangeBeforeSameTokenNameActive();
    long exchangeId = 2;
    String tokenId = "abc";
    long quant = 1_000L;
    String buyTokenId = "def";

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_SECOND);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.addAssetAmount(tokenId.getBytes(), quant);
    Map<String, Long> assetMap = accountCapsule.getAssetMap();
    Assert.assertEquals(20000_000000L, accountCapsule.getBalance());
    Assert.assertEquals(null, assetMap.get(buyTokenId));
    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);

    long expected = 0;
    try {
      ExchangeCapsule exchangeCapsule = dbManager.getExchangeStore()
          .get(ByteArray.fromLong(exchangeId));
      expected = exchangeCapsule.transaction(tokenId.getBytes(), quant);
    } catch (ItemNotFoundException e) {
      fail();
    }

    ExchangeTransactionActuator actuator = new ExchangeTransactionActuator(getContract(
        OWNER_ADDRESS_SECOND, exchangeId, tokenId, quant, expected + 1),
        dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      fail("should not run here");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("token required must greater than expected",
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
   * SameTokenName open,token required must greater than expected
   */
  @Test
  public void SameTokenNameOpenTokenRequiredNotEnough() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    InitExchangeSameTokenNameActive();
    long exchangeId = 2;
    String tokenId = "123";
    long quant = 1_000L;
    String buyTokenId = "456";

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_SECOND);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.addAssetAmountV2(tokenId.getBytes(), quant, dbManager );
    Map<String, Long> assetV2Map = accountCapsule.getAssetMapV2();
    Assert.assertEquals(20000_000000L, accountCapsule.getBalance());
    Assert.assertEquals(null, assetV2Map.get(buyTokenId));
    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);

    long expected = 0;
    try {
      ExchangeCapsule exchangeCapsuleV2 = dbManager.getExchangeV2Store()
              .get(ByteArray.fromLong(exchangeId));
      expected = exchangeCapsuleV2.transaction(tokenId.getBytes(), quant);
    } catch (ItemNotFoundException e) {
      fail();
    }

    ExchangeTransactionActuator actuator = new ExchangeTransactionActuator(getContract(
            OWNER_ADDRESS_SECOND, exchangeId, tokenId, quant, expected + 1),
            dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      fail("should not run here");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("token required must greater than expected",
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
   * SameTokenName open,invalid param
   * "token id is not a valid number"
   * "token expected must greater than zero"
   */
  @Test
  public void SameTokenNameOpenInvalidParam() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    InitExchangeSameTokenNameActive();
    long exchangeId = 1;
    long quant = 100_000_000L; // use 100 TRX to buy abc
    TransactionResultCapsule ret = new TransactionResultCapsule();

    //token id is not a valid number
    ExchangeTransactionActuator actuator = new ExchangeTransactionActuator(getContract(
            OWNER_ADDRESS_SECOND, exchangeId, "abc", quant, 1),
            dbManager);
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("should not run here");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("token id is not a valid number",
              e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

    //token expected must greater than zero
    actuator = new ExchangeTransactionActuator(getContract(
            OWNER_ADDRESS_SECOND, exchangeId, "_", quant, 0),
            dbManager);
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("should not run here");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("token expected must greater than zero",
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