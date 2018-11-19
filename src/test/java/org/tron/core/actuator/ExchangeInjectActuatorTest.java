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

public class ExchangeInjectActuatorTest {

  private static TronApplicationContext context;
  private static Manager dbManager;
  private static final String dbPath = "output_ExchangeInject_test";
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
            300_000_000L);
    AccountCapsule ownerAccountSecondCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8(ACCOUNT_NAME_SECOND),
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_SECOND)),
            AccountType.Normal,
            200_000_000_000L);

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
        Contract.ExchangeInjectContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(address)))
            .setExchangeId(exchangeId)
            .setTokenId(ByteString.copyFrom(tokenId.getBytes()))
            .setQuant(quant)
            .build());
  }

  private void InitExchangeBeforeSameTokenNameActive() {

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

    dbManager.getExchangeStore()
            .put(exchangeCapsule.createDbKey(), exchangeCapsule);
    dbManager.getExchangeStore()
            .put(exchangeCapsule2.createDbKey(), exchangeCapsule2);
  }

  private void InitExchangeSameTokenNameActive() {

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

    dbManager.getExchangeV2Store()
            .put(exchangeCapsule.createDbKey(), exchangeCapsule);
    dbManager.getExchangeV2Store()
            .put(exchangeCapsule2.createDbKey(), exchangeCapsule2);
  }

  /**
   * SameTokenName close, first inject Exchange,result is success.
   */
  @Test
  public void SameTokenNameCloseSuccessExchangeInject() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    InitExchangeBeforeSameTokenNameActive();
    long exchangeId = 1;
    String firstTokenId = "abc";
    long firstTokenQuant = 200000000L;
    String secondTokenId = "def";
    long secondTokenQuant = 400000000L;

    AssetIssueCapsule assetIssueCapsule1 =
        new AssetIssueCapsule(
            AssetIssueContract.newBuilder()
                .setName(ByteString.copyFrom(firstTokenId.getBytes()))
                .build());
    assetIssueCapsule1.setId(String.valueOf(1L));
    dbManager.getAssetIssueStore()
        .put(assetIssueCapsule1.getName().toByteArray(), assetIssueCapsule1);
    AssetIssueCapsule assetIssueCapsule2 =
        new AssetIssueCapsule(
            AssetIssueContract.newBuilder()
                .setName(ByteString.copyFrom(secondTokenId.getBytes()))
                .build());
    assetIssueCapsule2.setId(String.valueOf(2L));
    dbManager.getAssetIssueStore()
        .put(assetIssueCapsule2.getName().toByteArray(), assetIssueCapsule2);

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.addAssetAmount(firstTokenId.getBytes(), firstTokenQuant);
    accountCapsule.addAssetAmount(secondTokenId.getBytes(), secondTokenQuant);
    accountCapsule.setBalance(10000_000000L);
    dbManager.getAccountStore().put(ownerAddress, accountCapsule);

    ExchangeInjectActuator actuator = new ExchangeInjectActuator(getContract(
        OWNER_ADDRESS_FIRST, exchangeId, firstTokenId, firstTokenQuant),
        dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      long id = 1;
      ExchangeCapsule exchangeCapsule = dbManager.getExchangeStore().get(ByteArray.fromLong(id));
      Assert.assertNotNull(exchangeCapsule);

      Assert.assertEquals(ByteString.copyFrom(ownerAddress), exchangeCapsule.getCreatorAddress());
      Assert.assertEquals(id, exchangeCapsule.getID());
      Assert.assertEquals(1000000, exchangeCapsule.getCreateTime());
      Assert.assertTrue(Arrays.equals(firstTokenId.getBytes(), exchangeCapsule.getFirstTokenId()));
      Assert.assertEquals(firstTokenId, ByteArray.toStr(exchangeCapsule.getFirstTokenId()));
      Assert.assertEquals(300000000L, exchangeCapsule.getFirstTokenBalance());
      Assert.assertEquals(secondTokenId, ByteArray.toStr(exchangeCapsule.getSecondTokenId()));
      Assert.assertEquals(600000000L, exchangeCapsule.getSecondTokenBalance());

      Assert.assertEquals(secondTokenQuant, ret.getExchangeInjectAnotherAmount());

      accountCapsule = dbManager.getAccountStore().get(ownerAddress);
      Map<String, Long> assetMap = accountCapsule.getAssetMap();
      Assert.assertEquals(10000_000000L, accountCapsule.getBalance());
      Assert.assertEquals(0L, assetMap.get(firstTokenId).longValue());
      Assert.assertEquals(0L, assetMap.get(secondTokenId).longValue());


    } catch (ContractValidateException e) {
      logger.info(e.getMessage());
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } catch (ItemNotFoundException e) {
      Assert.assertFalse(e instanceof ItemNotFoundException);
    }
  }

  /**
   * SameTokenName open, first inject Exchange,result is success.
   */
  @Test
  public void SameTokenNameOpenSuccessExchangeInject() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    InitExchangeSameTokenNameActive();
    long exchangeId = 1;
    String firstTokenId = "123";
    long firstTokenQuant = 200000000L;
    String secondTokenId = "456";
    long secondTokenQuant = 400000000L;

    AssetIssueCapsule assetIssueCapsule1 =
            new AssetIssueCapsule(
                    AssetIssueContract.newBuilder()
                            .setName(ByteString.copyFrom(firstTokenId.getBytes()))
                            .build());
    assetIssueCapsule1.setId(String.valueOf(1L));
    dbManager.getAssetIssueStore()
            .put(assetIssueCapsule1.getName().toByteArray(), assetIssueCapsule1);
    AssetIssueCapsule assetIssueCapsule2 =
            new AssetIssueCapsule(
                    AssetIssueContract.newBuilder()
                            .setName(ByteString.copyFrom(secondTokenId.getBytes()))
                            .build());
    assetIssueCapsule2.setId(String.valueOf(2L));
    dbManager.getAssetIssueStore()
            .put(assetIssueCapsule2.getName().toByteArray(), assetIssueCapsule2);

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.addAssetAmountV2(firstTokenId.getBytes(), firstTokenQuant, dbManager);
    accountCapsule.addAssetAmountV2(secondTokenId.getBytes(), secondTokenQuant, dbManager);
    accountCapsule.setBalance(10000_000000L);
    dbManager.getAccountStore().put(ownerAddress, accountCapsule);

    ExchangeInjectActuator actuator = new ExchangeInjectActuator(getContract(
            OWNER_ADDRESS_FIRST, exchangeId, firstTokenId, firstTokenQuant),
            dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      long id = 1;
      ExchangeCapsule exchangeCapsuleV2 =
              dbManager.getExchangeV2Store().get(ByteArray.fromLong(id));
      Assert.assertNotNull(exchangeCapsuleV2);

      Assert.assertEquals(ByteString.copyFrom(ownerAddress), exchangeCapsuleV2.getCreatorAddress());
      Assert.assertEquals(id, exchangeCapsuleV2.getID());
      Assert.assertEquals(1000000, exchangeCapsuleV2.getCreateTime());
      Assert.assertTrue(Arrays.equals(firstTokenId.getBytes(), exchangeCapsuleV2.getFirstTokenId()));
      Assert.assertEquals(firstTokenId, ByteArray.toStr(exchangeCapsuleV2.getFirstTokenId()));
      Assert.assertEquals(300000000L, exchangeCapsuleV2.getFirstTokenBalance());
      Assert.assertEquals(secondTokenId, ByteArray.toStr(exchangeCapsuleV2.getSecondTokenId()));
      Assert.assertEquals(600000000L, exchangeCapsuleV2.getSecondTokenBalance());

      accountCapsule = dbManager.getAccountStore().get(ownerAddress);
      Map<String, Long> assetV2Map = accountCapsule.getAssetMapV2();
      Assert.assertEquals(10000_000000L, accountCapsule.getBalance());
      Assert.assertEquals(0L, assetV2Map.get(firstTokenId).longValue());
      Assert.assertEquals(0L, assetV2Map.get(secondTokenId).longValue());

    } catch (ContractValidateException e) {
      logger.info(e.getMessage());
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } catch (ItemNotFoundException e) {
      Assert.assertFalse(e instanceof ItemNotFoundException);
    }
  }

  /**
   * SameTokenName close, second inject Exchange,result is success.
   */
  @Test
  public void SameTokenNameCloseSuccessExchangeInject2() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    InitExchangeBeforeSameTokenNameActive();
    long exchangeId = 2;
    String firstTokenId = "_";
    long firstTokenQuant = 100_000_000000L;
    String secondTokenId = "def";
    long secondTokenQuant = 4_000_000L;
    AssetIssueCapsule assetIssueCapsule =
        new AssetIssueCapsule(
            AssetIssueContract.newBuilder()
                .setName(ByteString.copyFrom(secondTokenId.getBytes()))
                .build());
    assetIssueCapsule.setId(String.valueOf(2L));
    dbManager.getAssetIssueStore()
        .put(assetIssueCapsule.getName().toByteArray(), assetIssueCapsule);

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.addAssetAmount(secondTokenId.getBytes(), secondTokenQuant);
    accountCapsule.setBalance(firstTokenQuant);
    dbManager.getAccountStore().put(ownerAddress, accountCapsule);

    ExchangeInjectActuator actuator = new ExchangeInjectActuator(getContract(
        OWNER_ADDRESS_FIRST, exchangeId, firstTokenId, firstTokenQuant),
        dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      ExchangeCapsule exchangeCapsule = dbManager.getExchangeStore()
          .get(ByteArray.fromLong(exchangeId));
      Assert.assertNotNull(exchangeCapsule);

      Assert.assertEquals(ByteString.copyFrom(ownerAddress), exchangeCapsule.getCreatorAddress());
      Assert.assertEquals(exchangeId, exchangeCapsule.getID());
      Assert.assertEquals(1000000, exchangeCapsule.getCreateTime());
      Assert.assertTrue(Arrays.equals(firstTokenId.getBytes(), exchangeCapsule.getFirstTokenId()));
      Assert.assertEquals(firstTokenId, ByteArray.toStr(exchangeCapsule.getFirstTokenId()));
      Assert.assertEquals(1_100_000_000000L, exchangeCapsule.getFirstTokenBalance());
      Assert.assertEquals(secondTokenId, ByteArray.toStr(exchangeCapsule.getSecondTokenId()));
      Assert.assertEquals(11_000_000L, exchangeCapsule.getSecondTokenBalance());

      accountCapsule = dbManager.getAccountStore().get(ownerAddress);
      Map<String, Long> assetMap = accountCapsule.getAssetMap();
      Assert.assertEquals(0L, accountCapsule.getBalance());
      Assert.assertEquals(3_000_000L, assetMap.get(secondTokenId).longValue());

    } catch (ContractValidateException e) {
      logger.info(e.getMessage());
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } catch (ItemNotFoundException e) {
      Assert.assertFalse(e instanceof ItemNotFoundException);
    }
  }

  /**
   * SameTokenName open, second inject Exchange,result is success.
   */
  @Test
  public void SameTokenNameOpenSuccessExchangeInject2() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    InitExchangeSameTokenNameActive();
    long exchangeId = 2;
    String firstTokenId = "_";
    long firstTokenQuant = 100_000_000000L;
    String secondTokenId = "456";
    long secondTokenQuant = 4_000_000L;
    AssetIssueCapsule assetIssueCapsule =
            new AssetIssueCapsule(
                    AssetIssueContract.newBuilder()
                            .setName(ByteString.copyFrom(secondTokenId.getBytes()))
                            .build());
    assetIssueCapsule.setId(String.valueOf(2L));
    dbManager.getAssetIssueV2Store()
            .put(assetIssueCapsule.getName().toByteArray(), assetIssueCapsule);

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.addAssetAmountV2(secondTokenId.getBytes(), secondTokenQuant, dbManager);
    accountCapsule.setBalance(firstTokenQuant);
    dbManager.getAccountStore().put(ownerAddress, accountCapsule);

    ExchangeInjectActuator actuator = new ExchangeInjectActuator(getContract(
            OWNER_ADDRESS_FIRST, exchangeId, firstTokenId, firstTokenQuant),
            dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      ExchangeCapsule exchangeV2Capsule = dbManager.getExchangeV2Store()
              .get(ByteArray.fromLong(exchangeId));
      Assert.assertNotNull(exchangeV2Capsule);

      Assert.assertEquals(ByteString.copyFrom(ownerAddress), exchangeV2Capsule.getCreatorAddress());
      Assert.assertEquals(exchangeId, exchangeV2Capsule.getID());
      Assert.assertEquals(1000000, exchangeV2Capsule.getCreateTime());
      Assert.assertTrue(Arrays.equals(firstTokenId.getBytes(), exchangeV2Capsule.getFirstTokenId()));
      Assert.assertEquals(firstTokenId, ByteArray.toStr(exchangeV2Capsule.getFirstTokenId()));
      Assert.assertEquals(1_100_000_000000L, exchangeV2Capsule.getFirstTokenBalance());
      Assert.assertEquals(secondTokenId, ByteArray.toStr(exchangeV2Capsule.getSecondTokenId()));
      Assert.assertEquals(11_000_000L, exchangeV2Capsule.getSecondTokenBalance());

      accountCapsule = dbManager.getAccountStore().get(ownerAddress);
      Map<String, Long> assetV2Map = accountCapsule.getAssetMapV2();
      Assert.assertEquals(0L, accountCapsule.getBalance());
      Assert.assertEquals(3_000_000L, assetV2Map.get(secondTokenId).longValue());

    } catch (ContractValidateException e) {
      logger.info(e.getMessage());
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } catch (ItemNotFoundException e) {
      Assert.assertFalse(e instanceof ItemNotFoundException);
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
    long firstTokenQuant = 200000000L;

    ExchangeInjectActuator actuator = new ExchangeInjectActuator(getContract(
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
    long firstTokenQuant = 200000000L;

    ExchangeInjectActuator actuator = new ExchangeInjectActuator(getContract(
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
    long firstTokenQuant = 200000000L;

    ExchangeInjectActuator actuator = new ExchangeInjectActuator(getContract(
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
    long firstTokenQuant = 200000000L;

    ExchangeInjectActuator actuator = new ExchangeInjectActuator(getContract(
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
    }
  }

  /**
   * SameTokenName close, Exchange not exists
   */
  @Test
  public void SameTokenNameCloseExchangeNotExist() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    InitExchangeBeforeSameTokenNameActive();
    long exchangeId = 3;
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

    ExchangeInjectActuator actuator = new ExchangeInjectActuator(getContract(
        OWNER_ADDRESS_FIRST, exchangeId, firstTokenId, firstTokenQuant),
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
    }
  }

  /**
   * SameTokenName open, Exchange not exists
   */
  @Test
  public void SameTokenNameOpenExchangeNotExist() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    InitExchangeSameTokenNameActive();
    long exchangeId = 3;
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

    ExchangeInjectActuator actuator = new ExchangeInjectActuator(getContract(
            OWNER_ADDRESS_FIRST, exchangeId, firstTokenId, firstTokenQuant),
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
    }
  }

  /**
   * SameTokenName close, account[" + readableOwnerAddress + "] is not creator
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

    ExchangeInjectActuator actuator = new ExchangeInjectActuator(getContract(
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
    }
  }

  /**
   * SameTokenName open, account[" + readableOwnerAddress + "] is not creator
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
    accountCapsule.addAssetAmountV2(firstTokenId.getBytes(), firstTokenQuant, dbManager );
    accountCapsule.addAssetAmountV2(secondTokenId.getBytes(), secondTokenQuant, dbManager );
    accountCapsule.setBalance(10000_000000L);
    dbManager.getAccountStore().put(ownerAddress, accountCapsule);

    ExchangeInjectActuator actuator = new ExchangeInjectActuator(getContract(
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

    ExchangeInjectActuator actuator = new ExchangeInjectActuator(getContract(
        OWNER_ADDRESS_FIRST, exchangeId, firstTokenId, firstTokenQuant),
        dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("token id is not in exchange",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
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

    ExchangeInjectActuator actuator = new ExchangeInjectActuator(getContract(
            OWNER_ADDRESS_FIRST, exchangeId, firstTokenId, firstTokenQuant),
            dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("token id is not in exchange",
              e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
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

    ExchangeInjectActuator actuator = new ExchangeInjectActuator(getContract(
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

    ExchangeInjectActuator actuator = new ExchangeInjectActuator(getContract(
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
    }
  }

  /**
   * SameTokenName close, injected token quant must greater than zero
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

    ExchangeInjectActuator actuator = new ExchangeInjectActuator(getContract(
        OWNER_ADDRESS_FIRST, exchangeId, firstTokenId, firstTokenQuant),
        dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("injected token quant must greater than zero",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * SameTokenName open, injected token quant must greater than zero
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

    ExchangeInjectActuator actuator = new ExchangeInjectActuator(getContract(
            OWNER_ADDRESS_FIRST, exchangeId, firstTokenId, firstTokenQuant),
            dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("injected token quant must greater than zero",
              e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * SameTokenName close, "the calculated token quant  must be greater than 0"
   */
  @Test
  public void SameTokenNameCloseCalculatedTokenQuantLessThanZero() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    InitExchangeBeforeSameTokenNameActive();
    long exchangeId = 2;
    String firstTokenId = "_";
    long firstTokenQuant = 100L;
    String secondTokenId = "def";
    long secondTokenQuant = 400000000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.addAssetAmount(secondTokenId.getBytes(), secondTokenQuant);
    accountCapsule.setBalance(firstTokenQuant);
    dbManager.getAccountStore().put(ownerAddress, accountCapsule);

    ExchangeInjectActuator actuator = new ExchangeInjectActuator(getContract(
        OWNER_ADDRESS_FIRST, exchangeId, firstTokenId, firstTokenQuant),
        dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("the calculated token quant  must be greater than 0",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * SameTokenName open, "the calculated token quant  must be greater than 0"
   */
  @Test
  public void SameTokenNameOpenCalculatedTokenQuantLessThanZero() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    InitExchangeSameTokenNameActive();
    long exchangeId = 2;
    String firstTokenId = "_";
    long firstTokenQuant = 100L;
    String secondTokenId = "456";
    long secondTokenQuant = 400000000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.addAssetAmountV2(secondTokenId.getBytes(), secondTokenQuant, dbManager );
    accountCapsule.setBalance(firstTokenQuant);
    dbManager.getAccountStore().put(ownerAddress, accountCapsule);

    ExchangeInjectActuator actuator = new ExchangeInjectActuator(getContract(
            OWNER_ADDRESS_FIRST, exchangeId, firstTokenId, firstTokenQuant),
            dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("the calculated token quant  must be greater than 0",
              e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * SameTokenName close, token balance must less than balanceLimit
   */
  @Test
  public void SameTokenNameCloseTokenBalanceGreaterThanBalanceLimit() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    InitExchangeBeforeSameTokenNameActive();
    long exchangeId = 2;
    String firstTokenId = "_";
    long firstTokenQuant = 1_000_000_000_000_001L;
    String secondTokenId = "def";
    long secondTokenQuant = 400000000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.addAssetAmount(secondTokenId.getBytes(), secondTokenQuant);
    accountCapsule.setBalance(firstTokenQuant);
    dbManager.getAccountStore().put(ownerAddress, accountCapsule);

    ExchangeInjectActuator actuator = new ExchangeInjectActuator(getContract(
        OWNER_ADDRESS_FIRST, exchangeId, firstTokenId, firstTokenQuant),
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
    }
  }

  /**
   * SameTokenName open, token balance must less than balanceLimit
   */
  @Test
  public void SameTokenNameOpenTokenBalanceGreaterThanBalanceLimit() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    InitExchangeSameTokenNameActive();
    long exchangeId = 2;
    String firstTokenId = "_";
    long firstTokenQuant = 1_000_000_000_000_001L;
    String secondTokenId = "456";
    long secondTokenQuant = 400000000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.addAssetAmountV2(secondTokenId.getBytes(), secondTokenQuant, dbManager );
    accountCapsule.setBalance(firstTokenQuant);
    dbManager.getAccountStore().put(ownerAddress, accountCapsule);

    ExchangeInjectActuator actuator = new ExchangeInjectActuator(getContract(
            OWNER_ADDRESS_FIRST, exchangeId, firstTokenId, firstTokenQuant),
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
    }
  }

  /**
   * SameTokenName close, balance is not enough
   */
  @Test
  public void SameTokenNameCloseBalanceNotEnough() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    InitExchangeBeforeSameTokenNameActive();
    long exchangeId = 2;
    String firstTokenId = "_";
    long firstTokenQuant = 100_000000L;
    String secondTokenId = "def";
    long secondTokenQuant = 400000000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.addAssetAmount(secondTokenId.getBytes(), secondTokenQuant);
    accountCapsule.setBalance(firstTokenQuant - 1);
    dbManager.getAccountStore().put(ownerAddress, accountCapsule);

    ExchangeInjectActuator actuator = new ExchangeInjectActuator(getContract(
        OWNER_ADDRESS_FIRST, exchangeId, firstTokenId, firstTokenQuant),
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
    }
  }

  /**
   * SameTokenName open, balance is not enough
   */
  @Test
  public void SameTokenNameOpenBalanceNotEnough() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    InitExchangeSameTokenNameActive();
    long exchangeId = 2;
    String firstTokenId = "_";
    long firstTokenQuant = 100_000000L;
    String secondTokenId = "456";
    long secondTokenQuant = 400000000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.addAssetAmountV2(secondTokenId.getBytes(), secondTokenQuant, dbManager );
    accountCapsule.setBalance(firstTokenQuant - 1);
    dbManager.getAccountStore().put(ownerAddress, accountCapsule);

    ExchangeInjectActuator actuator = new ExchangeInjectActuator(getContract(
            OWNER_ADDRESS_FIRST, exchangeId, firstTokenId, firstTokenQuant),
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
    }
  }

  /**
   * SameTokenName close, first token balance is not enough
   */
  @Test
  public void SameTokenNameCloseTokenBalanceNotEnough() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    InitExchangeBeforeSameTokenNameActive();
    long exchangeId = 1;
    String firstTokenId = "abc";
    long firstTokenQuant = 200000000L;
    String secondTokenId = "def";
    long secondTokenQuant = 400000000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.addAssetAmount(firstTokenId.getBytes(), firstTokenQuant - 1);
    accountCapsule.addAssetAmount(secondTokenId.getBytes(), secondTokenQuant);
    accountCapsule.setBalance(10000_000000L);
    dbManager.getAccountStore().put(ownerAddress, accountCapsule);

    ExchangeInjectActuator actuator = new ExchangeInjectActuator(getContract(
        OWNER_ADDRESS_FIRST, exchangeId, firstTokenId, firstTokenQuant),
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
    }
  }

  /**
   * SameTokenName open, first token balance is not enough
   */
  @Test
  public void SameTokenNameOpenTokenBalanceNotEnough() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    InitExchangeSameTokenNameActive();
    long exchangeId = 1;
    String firstTokenId = "123";
    long firstTokenQuant = 200000000L;
    String secondTokenId = "456";
    long secondTokenQuant = 400000000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.addAssetAmountV2(firstTokenId.getBytes(), firstTokenQuant - 1, dbManager );
    accountCapsule.addAssetAmountV2(secondTokenId.getBytes(), secondTokenQuant, dbManager );
    accountCapsule.setBalance(10000_000000L);
    dbManager.getAccountStore().put(ownerAddress, accountCapsule);

    ExchangeInjectActuator actuator = new ExchangeInjectActuator(getContract(
            OWNER_ADDRESS_FIRST, exchangeId, firstTokenId, firstTokenQuant),
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
    }
  }

  /**
   * SameTokenName close, balance is not enough2
   */
  @Test
  public void SameTokenNameCloseBalanceNotEnough2() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    InitExchangeBeforeSameTokenNameActive();
    long exchangeId = 2;
    String secondTokenId = "def";
    long secondTokenQuant = 4000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.addAssetAmount(secondTokenId.getBytes(), secondTokenQuant);
    accountCapsule.setBalance(399_000000L);
    dbManager.getAccountStore().put(ownerAddress, accountCapsule);

    ExchangeInjectActuator actuator = new ExchangeInjectActuator(getContract(
        OWNER_ADDRESS_FIRST, exchangeId, secondTokenId, secondTokenQuant),
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
    }
  }

  /**
   * SameTokenName open, balance is not enough2
   */
  @Test
  public void SameTokenNameOpenBalanceNotEnough2() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    InitExchangeSameTokenNameActive();
    long exchangeId = 2;
    String secondTokenId = "456";
    long secondTokenQuant = 4000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.addAssetAmountV2(secondTokenId.getBytes(), secondTokenQuant, dbManager );
    accountCapsule.setBalance(399_000000L);
    dbManager.getAccountStore().put(ownerAddress, accountCapsule);

    ExchangeInjectActuator actuator = new ExchangeInjectActuator(getContract(
            OWNER_ADDRESS_FIRST, exchangeId, secondTokenId, secondTokenQuant),
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
    }
  }

  /**
   * SameTokenName close, first token balance is not enough
   */
  @Test
  public void SameTokenNameCloseAnotherTokenBalanceNotEnough() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    InitExchangeBeforeSameTokenNameActive();
    long exchangeId = 1;
    String firstTokenId = "abc";
    long firstTokenQuant = 200000000L;
    String secondTokenId = "def";
    long secondTokenQuant = 400000000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.addAssetAmount(firstTokenId.getBytes(), firstTokenQuant - 1);
    accountCapsule.addAssetAmount(secondTokenId.getBytes(), secondTokenQuant);
    accountCapsule.setBalance(10000_000000L);
    dbManager.getAccountStore().put(ownerAddress, accountCapsule);

    ExchangeInjectActuator actuator = new ExchangeInjectActuator(getContract(
        OWNER_ADDRESS_FIRST, exchangeId, secondTokenId, secondTokenQuant),
        dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("another token balance is not enough",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * SameTokenName open, first token balance is not enough
   */
  @Test
  public void SameTokenNameOpenAnotherTokenBalanceNotEnough() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    InitExchangeSameTokenNameActive();
    long exchangeId = 1;
    String firstTokenId = "123";
    long firstTokenQuant = 200000000L;
    String secondTokenId = "456";
    long secondTokenQuant = 400000000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.addAssetAmountV2(firstTokenId.getBytes(), firstTokenQuant - 1, dbManager );
    accountCapsule.addAssetAmountV2(secondTokenId.getBytes(), secondTokenQuant, dbManager );
    accountCapsule.setBalance(10000_000000L);
    dbManager.getAccountStore().put(ownerAddress, accountCapsule);

    ExchangeInjectActuator actuator = new ExchangeInjectActuator(getContract(
            OWNER_ADDRESS_FIRST, exchangeId, secondTokenId, secondTokenQuant),
            dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("another token balance is not enough",
              e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * SameTokenName open, invalid param
   * "token id is not a valid number"
   */
  @Test
  public void SameTokenNameOpenInvalidParam() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    InitExchangeSameTokenNameActive();
    long exchangeId = 1;
    ExchangeInjectActuator actuator = new ExchangeInjectActuator(getContract(
            OWNER_ADDRESS_FIRST, exchangeId, "abc", 1000),
            dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
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
    }
  }

}