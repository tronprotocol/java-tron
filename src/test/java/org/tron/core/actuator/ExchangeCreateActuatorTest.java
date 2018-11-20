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

public class ExchangeCreateActuatorTest {

  private static TronApplicationContext context;
  private static Manager dbManager;
  private static final String dbPath = "output_ExchangeCreate_test";
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
    dbManager.getDynamicPropertiesStore().saveLatestExchangeNum(0);

  }

  private Any getContract(String address, String firstTokenId, long firstTokenBalance,
      String secondTokenId, long secondTokenBalance) {
    return Any.pack(
        Contract.ExchangeCreateContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(address)))
            .setFirstTokenId(ByteString.copyFrom(firstTokenId.getBytes()))
            .setFirstTokenBalance(firstTokenBalance)
            .setSecondTokenId(ByteString.copyFrom(secondTokenId.getBytes()))
            .setSecondTokenBalance(secondTokenBalance)
            .build());
  }

  /**
   * SameTokenName close,first createExchange,result is success.
   */
  @Test
  public void sameTokenNameCloseSuccessExchangeCreate() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    String firstTokenId = "abc";
    long firstTokenBalance = 100000000L;
    String secondTokenId = "def";
    long secondTokenBalance = 100000000L;

    AssetIssueCapsule assetIssueCapsule1 =
        new AssetIssueCapsule(
            AssetIssueContract.newBuilder()
                .setName(ByteString.copyFrom(firstTokenId.getBytes()))
                .build());
    assetIssueCapsule1.setId(String.valueOf(1L));

    AssetIssueCapsule assetIssueCapsule2 =
        new AssetIssueCapsule(
            AssetIssueContract.newBuilder()
                .setName(ByteString.copyFrom(secondTokenId.getBytes()))
                .build());
    assetIssueCapsule2.setId(String.valueOf(2L));

    dbManager.getAssetIssueStore()
        .put(assetIssueCapsule1.getName().toByteArray(), assetIssueCapsule1);
    dbManager.getAssetIssueStore()
        .put(assetIssueCapsule2.getName().toByteArray(), assetIssueCapsule2);

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.addAssetAmount(firstTokenId.getBytes(), firstTokenBalance);
    accountCapsule.addAssetAmount(secondTokenId.getBytes(), secondTokenBalance);
    accountCapsule.setBalance(10000_000000L);
    dbManager.getAccountStore().put(ownerAddress, accountCapsule);

    ExchangeCreateActuator actuator = new ExchangeCreateActuator(getContract(
        OWNER_ADDRESS_FIRST, firstTokenId, firstTokenBalance, secondTokenId, secondTokenBalance),
        dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);
    try {
      actuator.validate();
      actuator.execute(ret);

      Assert.assertEquals(ret.getInstance().getExchangeId(), 1L);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      long id = 1;
      Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), id);
      // check old(V1) version
      ExchangeCapsule exchangeCapsule = dbManager.getExchangeStore().get(ByteArray.fromLong(id));
      Assert.assertNotNull(exchangeCapsule);
      Assert.assertEquals(ByteString.copyFrom(ownerAddress), exchangeCapsule.getCreatorAddress());
      Assert.assertEquals(id, exchangeCapsule.getID());
      Assert.assertEquals(1000000, exchangeCapsule.getCreateTime());
      Assert.assertTrue(Arrays.equals(firstTokenId.getBytes(), exchangeCapsule.getFirstTokenId()));
      Assert.assertEquals(firstTokenId, ByteArray.toStr(exchangeCapsule.getFirstTokenId()));
      Assert.assertEquals(firstTokenBalance, exchangeCapsule.getFirstTokenBalance());
      Assert.assertEquals(secondTokenId, ByteArray.toStr(exchangeCapsule.getSecondTokenId()));
      Assert.assertEquals(secondTokenBalance, exchangeCapsule.getSecondTokenBalance());

      accountCapsule = dbManager.getAccountStore().get(ownerAddress);
      Map<String, Long> assetMap = accountCapsule.getAssetMap();
      Assert.assertEquals(10000_000000L - 1024_000000L, accountCapsule.getBalance());
      Assert.assertEquals(0L, assetMap.get(firstTokenId).longValue());
      Assert.assertEquals(0L, assetMap.get(secondTokenId).longValue());

      // check V2 version
      ExchangeCapsule exchangeCapsuleV2 = dbManager.getExchangeV2Store().get(ByteArray.fromLong(id));
      Assert.assertNotNull(exchangeCapsuleV2);
      Assert.assertEquals(ByteString.copyFrom(ownerAddress), exchangeCapsuleV2.getCreatorAddress());
      Assert.assertEquals(id, exchangeCapsuleV2.getID());
      Assert.assertEquals(1000000, exchangeCapsuleV2.getCreateTime());
      // convert
      firstTokenId = dbManager.getAssetIssueStore().get(firstTokenId.getBytes()).getId();
      secondTokenId = dbManager.getAssetIssueStore().get(secondTokenId.getBytes()).getId();
      Assert.assertTrue(Arrays.equals(firstTokenId.getBytes(), exchangeCapsuleV2.getFirstTokenId()));
      Assert.assertEquals(firstTokenId, ByteArray.toStr(exchangeCapsuleV2.getFirstTokenId()));
      Assert.assertEquals(firstTokenBalance, exchangeCapsuleV2.getFirstTokenBalance());
      Assert.assertEquals(secondTokenId, ByteArray.toStr(exchangeCapsuleV2.getSecondTokenId()));
      Assert.assertEquals(secondTokenBalance, exchangeCapsuleV2.getSecondTokenBalance());

      accountCapsule = dbManager.getAccountStore().get(ownerAddress);
      Map<String, Long> getAssetV2Map = accountCapsule.getAssetMapV2();
      Assert.assertEquals(10000_000000L - 1024_000000L, accountCapsule.getBalance());
      Assert.assertEquals(0L, getAssetV2Map.get(firstTokenId).longValue());
      Assert.assertEquals(0L, getAssetV2Map.get(secondTokenId).longValue());


    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } catch (ItemNotFoundException e) {
      Assert.assertFalse(e instanceof ItemNotFoundException);
    }
  }

  /**
   * SameTokenName close,second create Exchange, result is success.
   */
  @Test
  public void sameTokenNameCloseSuccessExchangeCreate2() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    String firstTokenId = "_";
    long firstTokenBalance = 100_000_000_000000L;
    String secondTokenId = "abc";
    long secondTokenBalance = 100_000_000L;

    AssetIssueCapsule assetIssueCapsule =
        new AssetIssueCapsule(
            AssetIssueContract.newBuilder()
                .setName(ByteString.copyFrom(secondTokenId.getBytes()))
                .build());
    assetIssueCapsule.setId(String.valueOf(1L));
    dbManager.getAssetIssueStore()
        .put(assetIssueCapsule.getName().toByteArray(), assetIssueCapsule);

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.setBalance(200_000_000_000000L);
    accountCapsule.addAssetAmount(secondTokenId.getBytes(), 200_000_000L);
    dbManager.getAccountStore().put(ownerAddress, accountCapsule);

    ExchangeCreateActuator actuator = new ExchangeCreateActuator(getContract(
        OWNER_ADDRESS_FIRST, firstTokenId, firstTokenBalance, secondTokenId, secondTokenBalance),
        dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      long id = 1;
      Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), id);
      // check old version
      ExchangeCapsule exchangeCapsule = dbManager.getExchangeStore().get(ByteArray.fromLong(id));
      Assert.assertNotNull(exchangeCapsule);
      Assert.assertEquals(ByteString.copyFrom(ownerAddress), exchangeCapsule.getCreatorAddress());
      Assert.assertEquals(id, exchangeCapsule.getID());
      Assert.assertEquals(1000000, exchangeCapsule.getCreateTime());
      Assert.assertTrue(Arrays.equals(firstTokenId.getBytes(), exchangeCapsule.getFirstTokenId()));
      Assert.assertEquals(firstTokenId, ByteArray.toStr(exchangeCapsule.getFirstTokenId()));
      Assert.assertEquals(firstTokenBalance, exchangeCapsule.getFirstTokenBalance());
      Assert.assertEquals(secondTokenId, ByteArray.toStr(exchangeCapsule.getSecondTokenId()));
      Assert.assertEquals(secondTokenBalance, exchangeCapsule.getSecondTokenBalance());

      accountCapsule = dbManager.getAccountStore().get(ownerAddress);
      Map<String, Long> assetMap = accountCapsule.getAssetMap();
      Assert.assertEquals(200_000_000_000000L - 1024_000000L - firstTokenBalance,
          accountCapsule.getBalance());
      Assert.assertEquals(100_000_000L, assetMap.get(secondTokenId).longValue());

      // check V2 version
      ExchangeCapsule exchangeCapsuleV2 = dbManager.getExchangeV2Store().get(ByteArray.fromLong(id));
      Assert.assertNotNull(exchangeCapsuleV2);
      Assert.assertEquals(ByteString.copyFrom(ownerAddress), exchangeCapsuleV2.getCreatorAddress());
      Assert.assertEquals(id, exchangeCapsuleV2.getID());
      Assert.assertEquals(1000000, exchangeCapsuleV2.getCreateTime());
      secondTokenId = dbManager.getAssetIssueStore().get(secondTokenId.getBytes()).getId();
      Assert.assertTrue(Arrays.equals(firstTokenId.getBytes(), exchangeCapsuleV2.getFirstTokenId()));
      Assert.assertEquals(firstTokenId, ByteArray.toStr(exchangeCapsuleV2.getFirstTokenId()));
      Assert.assertEquals(firstTokenBalance, exchangeCapsuleV2.getFirstTokenBalance());
      Assert.assertEquals(secondTokenId, ByteArray.toStr(exchangeCapsuleV2.getSecondTokenId()));
      Assert.assertEquals(secondTokenBalance, exchangeCapsuleV2.getSecondTokenBalance());

      accountCapsule = dbManager.getAccountStore().get(ownerAddress);
      Map<String, Long> getAssetV2Map = accountCapsule.getAssetMapV2();
      Assert.assertEquals(200_000_000_000000L - 1024_000000L - firstTokenBalance,
              accountCapsule.getBalance());
      Assert.assertEquals(100_000_000L, getAssetV2Map.get(secondTokenId).longValue());

    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } catch (ItemNotFoundException e) {
      Assert.assertFalse(e instanceof ItemNotFoundException);
    }
  }

  /**
   * SameTokenName open,first createExchange,result is success.
   */
  @Test
  public void sameTokenNameOpenSuccessExchangeCreate() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    String firstTokenId = "123";
    long firstTokenBalance = 100000000L;
    String secondTokenId = "456";
    long secondTokenBalance = 100000000L;

    AssetIssueCapsule assetIssueCapsule1 =
            new AssetIssueCapsule(
                    AssetIssueContract.newBuilder()
                            .setName(ByteString.copyFrom(firstTokenId.getBytes()))
                            .build());
    assetIssueCapsule1.setId(String.valueOf(1L));

    AssetIssueCapsule assetIssueCapsule2 =
            new AssetIssueCapsule(
                    AssetIssueContract.newBuilder()
                            .setName(ByteString.copyFrom(secondTokenId.getBytes()))
                            .build());
    assetIssueCapsule2.setId(String.valueOf(2L));

    dbManager.getAssetIssueStore()
            .put(assetIssueCapsule1.getName().toByteArray(), assetIssueCapsule1);
    dbManager.getAssetIssueStore()
            .put(assetIssueCapsule2.getName().toByteArray(), assetIssueCapsule2);

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.addAssetAmountV2(firstTokenId.getBytes(), firstTokenBalance, dbManager);
    accountCapsule.addAssetAmountV2(secondTokenId.getBytes(), secondTokenBalance, dbManager);
    accountCapsule.setBalance(10000_000000L);
    dbManager.getAccountStore().put(ownerAddress, accountCapsule);

    ExchangeCreateActuator actuator = new ExchangeCreateActuator(getContract(
            OWNER_ADDRESS_FIRST, firstTokenId, firstTokenBalance, secondTokenId, secondTokenBalance),
            dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      long id = 1;
      Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), id);

      // check V2 version
      ExchangeCapsule exchangeCapsuleV2 = dbManager.getExchangeV2Store().get(ByteArray.fromLong(id));
      Assert.assertNotNull(exchangeCapsuleV2);
      Assert.assertEquals(ByteString.copyFrom(ownerAddress), exchangeCapsuleV2.getCreatorAddress());
      Assert.assertEquals(id, exchangeCapsuleV2.getID());
      Assert.assertEquals(1000000, exchangeCapsuleV2.getCreateTime());

      Assert.assertTrue(Arrays.equals(firstTokenId.getBytes(), exchangeCapsuleV2.getFirstTokenId()));
      Assert.assertEquals(firstTokenId, ByteArray.toStr(exchangeCapsuleV2.getFirstTokenId()));
      Assert.assertEquals(firstTokenBalance, exchangeCapsuleV2.getFirstTokenBalance());
      Assert.assertEquals(secondTokenId, ByteArray.toStr(exchangeCapsuleV2.getSecondTokenId()));
      Assert.assertEquals(secondTokenBalance, exchangeCapsuleV2.getSecondTokenBalance());

      accountCapsule = dbManager.getAccountStore().get(ownerAddress);
      Map<String, Long> getAssetV2Map = accountCapsule.getAssetMapV2();
      Assert.assertEquals(10000_000000L - 1024_000000L, accountCapsule.getBalance());
      Assert.assertEquals(0L, getAssetV2Map.get(firstTokenId).longValue());
      Assert.assertEquals(0L, getAssetV2Map.get(secondTokenId).longValue());

    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } catch (ItemNotFoundException e) {
      Assert.assertFalse(e instanceof ItemNotFoundException);
    }
  }

  /**
   * SameTokenName close,second create Exchange, result is success.
   */
  @Test
  public void sameTokenNameOpenSuccessExchangeCreate2() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    String firstTokenId = "_";
    long firstTokenBalance = 100_000_000_000000L;
    String secondTokenId = "123";
    long secondTokenBalance = 100_000_000L;

    AssetIssueCapsule assetIssueCapsule =
            new AssetIssueCapsule(
                    AssetIssueContract.newBuilder()
                            .setName(ByteString.copyFrom(secondTokenId.getBytes()))
                            .build());
    assetIssueCapsule.setId(String.valueOf(1L));
    dbManager.getAssetIssueStore()
            .put(assetIssueCapsule.getName().toByteArray(), assetIssueCapsule);

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.setBalance(200_000_000_000000L);
    accountCapsule.addAssetAmountV2(secondTokenId.getBytes(), 200_000_000L, dbManager );
    dbManager.getAccountStore().put(ownerAddress, accountCapsule);

    ExchangeCreateActuator actuator = new ExchangeCreateActuator(getContract(
            OWNER_ADDRESS_FIRST, firstTokenId, firstTokenBalance, secondTokenId, secondTokenBalance),
            dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      long id = 1;
      Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), id);
      // check V2 version
      ExchangeCapsule exchangeCapsuleV2 = dbManager.getExchangeV2Store().get(ByteArray.fromLong(id));
      Assert.assertNotNull(exchangeCapsuleV2);
      Assert.assertEquals(ByteString.copyFrom(ownerAddress), exchangeCapsuleV2.getCreatorAddress());
      Assert.assertEquals(id, exchangeCapsuleV2.getID());
      Assert.assertEquals(1000000, exchangeCapsuleV2.getCreateTime());
      Assert.assertTrue(Arrays.equals(firstTokenId.getBytes(), exchangeCapsuleV2.getFirstTokenId()));
      Assert.assertEquals(firstTokenId, ByteArray.toStr(exchangeCapsuleV2.getFirstTokenId()));
      Assert.assertEquals(firstTokenBalance, exchangeCapsuleV2.getFirstTokenBalance());
      Assert.assertEquals(secondTokenId, ByteArray.toStr(exchangeCapsuleV2.getSecondTokenId()));
      Assert.assertEquals(secondTokenBalance, exchangeCapsuleV2.getSecondTokenBalance());

      accountCapsule = dbManager.getAccountStore().get(ownerAddress);
      Map<String, Long> getAssetV2Map = accountCapsule.getAssetMapV2();
      Assert.assertEquals(200_000_000_000000L - 1024_000000L - firstTokenBalance,
              accountCapsule.getBalance());
      Assert.assertEquals(100_000_000L, getAssetV2Map.get(secondTokenId).longValue());

    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } catch (ItemNotFoundException e) {
      Assert.assertFalse(e instanceof ItemNotFoundException);
    }
  }


  /**
   * SameTokenName open,first createExchange,result is failure.
   */
  @Test
  public void sameTokenNameOpenExchangeCreateFailure() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    String firstTokenId = "abc";
    long firstTokenBalance = 100000000L;
    String secondTokenId = "def";
    long secondTokenBalance = 100000000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.setBalance(10000_000000L);
    dbManager.getAccountStore().put(ownerAddress, accountCapsule);

    ExchangeCreateActuator actuator = new ExchangeCreateActuator(getContract(
            OWNER_ADDRESS_FIRST, firstTokenId, firstTokenBalance, secondTokenId, secondTokenBalance),
            dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);
    try {
      actuator.validate();
      actuator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("first token id is not a valid number", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * SameTokenName open,second create Exchange, result is failure.
   */
  @Test
  public void sameTokenNameOpenExchangeCreateFailure2() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    String firstTokenId = "_";
    long firstTokenBalance = 100_000_000_000000L;
    String secondTokenId = "abc";
    long secondTokenBalance = 100_000_000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.setBalance(200_000_000_000000L);
    dbManager.getAccountStore().put(ownerAddress, accountCapsule);

    ExchangeCreateActuator actuator = new ExchangeCreateActuator(getContract(
            OWNER_ADDRESS_FIRST, firstTokenId, firstTokenBalance, secondTokenId, secondTokenBalance),
            dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);
    try {
      actuator.validate();
      actuator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("second token id is not a valid number", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }


  /**
   * SameTokenName close, use Invalid Address, result is failed, exception is "Invalid address".
   */
  @Test
  public void sameTokenNameCloseInvalidAddress() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    String firstTokenId = "_";
    long firstTokenBalance = 100_000_000_000000L;
    String secondTokenId = "abc";
    long secondTokenBalance = 100_000_000L;

    ExchangeCreateActuator actuator = new ExchangeCreateActuator(getContract(
        OWNER_ADDRESS_INVALID, firstTokenId, firstTokenBalance, secondTokenId, secondTokenBalance),
        dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);

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
  public void sameTokenNameOpenInvalidAddress() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    String firstTokenId = "_";
    long firstTokenBalance = 100_000_000_000000L;
    String secondTokenId = "123";
    long secondTokenBalance = 100_000_000L;

    ExchangeCreateActuator actuator = new ExchangeCreateActuator(getContract(
            OWNER_ADDRESS_INVALID, firstTokenId, firstTokenBalance, secondTokenId, secondTokenBalance),
            dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);

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
   * SameTokenName close, use AccountStore not exists, result is failed, exception is "account not
   * exists".
   */
  @Test
  public void sameTokenNameCloseNoAccount() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    String firstTokenId = "_";
    long firstTokenBalance = 100_000_000_000000L;
    String secondTokenId = "abc";
    long secondTokenBalance = 100_000_000L;

    ExchangeCreateActuator actuator = new ExchangeCreateActuator(getContract(
        OWNER_ADDRESS_NOACCOUNT, firstTokenId, firstTokenBalance, secondTokenId,
        secondTokenBalance),
        dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);

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
   * SameTokenName open, use AccountStore not exists, result is failed, exception is "account not
   * exists".
   */
  @Test
  public void sameTokenNameOpenNoAccount() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    String firstTokenId = "_";
    long firstTokenBalance = 100_000_000_000000L;
    String secondTokenId = "123";
    long secondTokenBalance = 100_000_000L;

    ExchangeCreateActuator actuator = new ExchangeCreateActuator(getContract(
            OWNER_ADDRESS_NOACCOUNT, firstTokenId, firstTokenBalance, secondTokenId,
            secondTokenBalance),
            dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);

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
   * SameTokenName close,No enough balance
   */
  @Test
  public void sameTokenNameCloseNoEnoughBalance() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    String firstTokenId = "abc";
    long firstTokenBalance = 100000000L;
    String secondTokenId = "def";
    long secondTokenBalance = 100000000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.addAssetAmount(firstTokenId.getBytes(), firstTokenBalance);
    accountCapsule.addAssetAmount(secondTokenId.getBytes(), secondTokenBalance);
    accountCapsule.setBalance(1000_000000L);
    dbManager.getAccountStore().put(ownerAddress, accountCapsule);

    ExchangeCreateActuator actuator = new ExchangeCreateActuator(getContract(
        OWNER_ADDRESS_FIRST, firstTokenId, firstTokenBalance, secondTokenId, secondTokenBalance),
        dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);
    try {
      actuator.validate();
      actuator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("No enough balance for exchange create fee!",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }


  /**
   * SameTokenName open,No enough balance
   */
  @Test
  public void sameTokenNameOpenNoEnoughBalance() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    String firstTokenId = "123";
    long firstTokenBalance = 100000000L;
    String secondTokenId = "345";
    long secondTokenBalance = 100000000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.addAssetAmount(firstTokenId.getBytes(), firstTokenBalance);
    accountCapsule.addAssetAmount(secondTokenId.getBytes(), secondTokenBalance);
    accountCapsule.setBalance(1000_000000L);
    dbManager.getAccountStore().put(ownerAddress, accountCapsule);

    ExchangeCreateActuator actuator = new ExchangeCreateActuator(getContract(
            OWNER_ADDRESS_FIRST, firstTokenId, firstTokenBalance, secondTokenId, secondTokenBalance),
            dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);
    try {
      actuator.validate();
      actuator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("No enough balance for exchange create fee!",
              e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * SameTokenName close,exchange same tokens
   */
  @Test
  public void sameTokenNameCloseSameTokens() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    String firstTokenId = "abc";
    long firstTokenBalance = 100000000L;
    String secondTokenId = "abc";
    long secondTokenBalance = 100000000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.addAssetAmount(firstTokenId.getBytes(), firstTokenBalance);
    accountCapsule.addAssetAmount(secondTokenId.getBytes(), secondTokenBalance);
    accountCapsule.setBalance(10000_000000L);
    dbManager.getAccountStore().put(ownerAddress, accountCapsule);

    ExchangeCreateActuator actuator = new ExchangeCreateActuator(getContract(
        OWNER_ADDRESS_FIRST, firstTokenId, firstTokenBalance, secondTokenId, secondTokenBalance),
        dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);
    try {
      actuator.validate();
      actuator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("cannot exchange same tokens",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * SameTokenName open,exchange same tokens
   */
  @Test
  public void sameTokenNameOpenSameTokens() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    String firstTokenId = "123";
    long firstTokenBalance = 100000000L;
    String secondTokenId = "456";
    long secondTokenBalance = 100000000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.addAssetAmount(firstTokenId.getBytes(), firstTokenBalance);
    accountCapsule.addAssetAmount(secondTokenId.getBytes(), secondTokenBalance);
    accountCapsule.setBalance(10000_000000L);
    dbManager.getAccountStore().put(ownerAddress, accountCapsule);

    ExchangeCreateActuator actuator = new ExchangeCreateActuator(getContract(
            OWNER_ADDRESS_FIRST, firstTokenId, firstTokenBalance, secondTokenId, secondTokenBalance),
            dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);
    try {
      actuator.validate();
      actuator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("first token balance is not enough",
              e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * SameTokenName close,token balance less than zero
   */
  @Test
  public void sameTokenNameCloseLessToken() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    String firstTokenId = "abc";
    long firstTokenBalance = 0L;
    String secondTokenId = "def";
    long secondTokenBalance = 0L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.addAssetAmount(firstTokenId.getBytes(), 1000);
    accountCapsule.addAssetAmount(secondTokenId.getBytes(), 1000);
    accountCapsule.setBalance(10000_000000L);
    dbManager.getAccountStore().put(ownerAddress, accountCapsule);

    ExchangeCreateActuator actuator = new ExchangeCreateActuator(getContract(
        OWNER_ADDRESS_FIRST, firstTokenId, firstTokenBalance, secondTokenId, secondTokenBalance),
        dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);
    try {
      actuator.validate();
      actuator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("token balance must greater than zero",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }


  /**
   * SameTokenName open,token balance less than zero
   */
  @Test
  public void sameTokenNameOpenLessToken() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    String firstTokenId = "123";
    long firstTokenBalance = 0L;
    String secondTokenId = "456";
    long secondTokenBalance = 0L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.addAssetAmount(firstTokenId.getBytes(), 1000);
    accountCapsule.addAssetAmount(secondTokenId.getBytes(), 1000);
    accountCapsule.setBalance(10000_000000L);
    dbManager.getAccountStore().put(ownerAddress, accountCapsule);

    ExchangeCreateActuator actuator = new ExchangeCreateActuator(getContract(
            OWNER_ADDRESS_FIRST, firstTokenId, firstTokenBalance, secondTokenId, secondTokenBalance),
            dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);
    try {
      actuator.validate();
      actuator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("token balance must greater than zero",
              e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * SameTokenName close,token balance must less than balanceLimit
   */
  @Test
  public void sameTokenNameCloseMoreThanBalanceLimit() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    String firstTokenId = "abc";
    long firstTokenBalance = 1_000_000_000_000_001L;
    String secondTokenId = "def";
    long secondTokenBalance = 100000000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.addAssetAmount(firstTokenId.getBytes(), firstTokenBalance);
    accountCapsule.addAssetAmount(secondTokenId.getBytes(), secondTokenBalance);
    accountCapsule.setBalance(10000_000000L);
    dbManager.getAccountStore().put(ownerAddress, accountCapsule);

    ExchangeCreateActuator actuator = new ExchangeCreateActuator(getContract(
        OWNER_ADDRESS_FIRST, firstTokenId, firstTokenBalance, secondTokenId, secondTokenBalance),
        dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);
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
   * SameTokenName open,token balance must less than balanceLimit
   */
  @Test
  public void sameTokenNameOpenMoreThanBalanceLimit() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    String firstTokenId = "123";
    long firstTokenBalance = 1_000_000_000_000_001L;
    String secondTokenId = "456";
    long secondTokenBalance = 100000000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.addAssetAmount(firstTokenId.getBytes(), firstTokenBalance);
    accountCapsule.addAssetAmount(secondTokenId.getBytes(), secondTokenBalance);
    accountCapsule.setBalance(10000_000000L);
    dbManager.getAccountStore().put(ownerAddress, accountCapsule);

    ExchangeCreateActuator actuator = new ExchangeCreateActuator(getContract(
            OWNER_ADDRESS_FIRST, firstTokenId, firstTokenBalance, secondTokenId, secondTokenBalance),
            dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);
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
   * SameTokenName close,balance is not enough
   */
  @Test
  public void sameTokenNameCloseBalanceNotEnough() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    String firstTokenId = "_";
    long firstTokenBalance = 100_000_000_000000L;
    String secondTokenId = "abc";
    long secondTokenBalance = 100_000_000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.setBalance(firstTokenBalance + 1000L);
    accountCapsule.addAssetAmount(secondTokenId.getBytes(), 200_000_000L);
    dbManager.getAccountStore().put(ownerAddress, accountCapsule);

    ExchangeCreateActuator actuator = new ExchangeCreateActuator(getContract(
        OWNER_ADDRESS_FIRST, firstTokenId, firstTokenBalance, secondTokenId, secondTokenBalance),
        dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);
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
   * SameTokenName open,balance is not enough
   */
  @Test
  public void sameTokenNameOpenBalanceNotEnough() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    String firstTokenId = "_";
    long firstTokenBalance = 100_000_000_000000L;
    String secondTokenId = "123";
    long secondTokenBalance = 100_000_000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.setBalance(firstTokenBalance + 1000L);
    accountCapsule.addAssetAmount(secondTokenId.getBytes(), 200_000_000L);
    dbManager.getAccountStore().put(ownerAddress, accountCapsule);

    ExchangeCreateActuator actuator = new ExchangeCreateActuator(getContract(
            OWNER_ADDRESS_FIRST, firstTokenId, firstTokenBalance, secondTokenId, secondTokenBalance),
            dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);
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
   * SameTokenName close,first token balance is not enough
   */
  @Test
  public void sameTokenNameCloseFirstTokenBalanceNotEnough() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    String firstTokenId = "abc";
    long firstTokenBalance = 100_000_000_000000L;
    String secondTokenId = "def";
    long secondTokenBalance = 100_000_000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.addAssetAmount(firstTokenId.getBytes(), firstTokenBalance - 1000L);
    accountCapsule.addAssetAmount(secondTokenId.getBytes(), 200_000_000L);
    accountCapsule.setBalance(10000_000000L);
    dbManager.getAccountStore().put(ownerAddress, accountCapsule);

    ExchangeCreateActuator actuator = new ExchangeCreateActuator(getContract(
        OWNER_ADDRESS_FIRST, firstTokenId, firstTokenBalance, secondTokenId, secondTokenBalance),
        dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);
    try {
      actuator.validate();
      actuator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("first token balance is not enough",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }


  /**
   * SameTokenName open,first token balance is not enough
   */
  @Test
  public void sameTokenNameOpenFirstTokenBalanceNotEnough() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    String firstTokenId = "123";
    long firstTokenBalance = 100_000_000_000000L;
    String secondTokenId = "456";
    long secondTokenBalance = 100_000_000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.addAssetAmount(firstTokenId.getBytes(), firstTokenBalance - 1000L);
    accountCapsule.addAssetAmount(secondTokenId.getBytes(), 200_000_000L);
    accountCapsule.setBalance(10000_000000L);
    dbManager.getAccountStore().put(ownerAddress, accountCapsule);

    ExchangeCreateActuator actuator = new ExchangeCreateActuator(getContract(
            OWNER_ADDRESS_FIRST, firstTokenId, firstTokenBalance, secondTokenId, secondTokenBalance),
            dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);
    try {
      actuator.validate();
      actuator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("first token balance is not enough",
              e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * SameTokenName close,balance is not enough
   */
  @Test
  public void sameTokenNameCloseBalanceNotEnough2() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    String firstTokenId = "abc";
    long firstTokenBalance = 100_000_000L;
    String secondTokenId = "_";
    long secondTokenBalance = 100_000_000_000000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.setBalance(secondTokenBalance + 1000L);
    accountCapsule.addAssetAmount(firstTokenId.getBytes(), 200_000_000L);
    dbManager.getAccountStore().put(ownerAddress, accountCapsule);

    ExchangeCreateActuator actuator = new ExchangeCreateActuator(getContract(
        OWNER_ADDRESS_FIRST, firstTokenId, firstTokenBalance, secondTokenId, secondTokenBalance),
        dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);
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
   * SameTokenName open,balance is not enough
   */
  @Test
  public void sameTokenNameOpenBalanceNotEnough2() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    String firstTokenId = "123";
    long firstTokenBalance = 100_000_000L;
    String secondTokenId = "_";
    long secondTokenBalance = 100_000_000_000000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.setBalance(secondTokenBalance + 1000L);
    accountCapsule.addAssetAmount(firstTokenId.getBytes(), 200_000_000L);
    dbManager.getAccountStore().put(ownerAddress, accountCapsule);

    ExchangeCreateActuator actuator = new ExchangeCreateActuator(getContract(
            OWNER_ADDRESS_FIRST, firstTokenId, firstTokenBalance, secondTokenId, secondTokenBalance),
            dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);
    try {
      actuator.validate();
      actuator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("first token balance is not enough",
              e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * SameTokenName close,first token balance is not enough
   */
  @Test
  public void sameTokenNameCloseSecondTokenBalanceNotEnough() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    String firstTokenId = "abc";
    long firstTokenBalance = 100_000_000_000000L;
    String secondTokenId = "def";
    long secondTokenBalance = 100_000_000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.addAssetAmount(firstTokenId.getBytes(), firstTokenBalance);
    accountCapsule.addAssetAmount(secondTokenId.getBytes(), 90_000_000L);
    accountCapsule.setBalance(10000_000000L);
    dbManager.getAccountStore().put(ownerAddress, accountCapsule);

    ExchangeCreateActuator actuator = new ExchangeCreateActuator(getContract(
        OWNER_ADDRESS_FIRST, firstTokenId, firstTokenBalance, secondTokenId, secondTokenBalance),
        dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);
    try {
      actuator.validate();
      actuator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("second token balance is not enough",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }


  /**
   * SameTokenName open,first token balance is not enough
   */
  @Test
  public void sameTokenNameOpenSecondTokenBalanceNotEnough() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    String firstTokenId = "123";
    long firstTokenBalance = 100_000_000_000000L;
    String secondTokenId = "456";
    long secondTokenBalance = 100_000_000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.addAssetAmount(firstTokenId.getBytes(), firstTokenBalance);
    accountCapsule.addAssetAmount(secondTokenId.getBytes(), 90_000_000L);
    accountCapsule.setBalance(10000_000000L);
    dbManager.getAccountStore().put(ownerAddress, accountCapsule);

    ExchangeCreateActuator actuator = new ExchangeCreateActuator(getContract(
            OWNER_ADDRESS_FIRST, firstTokenId, firstTokenBalance, secondTokenId, secondTokenBalance),
            dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);
    try {
      actuator.validate();
      actuator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("first token balance is not enough",
              e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * SameTokenName close,not trx,ont token is ok, but the second one is not exist.
   */
  @Test
  public void sameTokenNameCloseSecondTokenNotExist() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    String firstTokenId = "abc";
    long firstTokenBalance = 100_000_000_000000L;
    String secondTokenId = "def";
    long secondTokenBalance = 100_000_000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.addAssetAmount(firstTokenId.getBytes(), firstTokenBalance);
    accountCapsule.setBalance(10000_000000L);
    dbManager.getAccountStore().put(ownerAddress, accountCapsule);

    ExchangeCreateActuator actuator = new ExchangeCreateActuator(getContract(
        OWNER_ADDRESS_FIRST, firstTokenId, firstTokenBalance, secondTokenId, secondTokenBalance),
        dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);
    try {
      actuator.validate();
      actuator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("second token balance is not enough",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }


  /**
   * SameTokenName open,not trx,ont token is ok, but the second one is not exist.
   */
  @Test
  public void sameTokenNameOpenSecondTokenNotExist() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    String firstTokenId = "123";
    long firstTokenBalance = 100_000_000_000000L;
    String secondTokenId = "456";
    long secondTokenBalance = 100_000_000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.addAssetAmount(firstTokenId.getBytes(), firstTokenBalance);
    accountCapsule.setBalance(10000_000000L);
    dbManager.getAccountStore().put(ownerAddress, accountCapsule);

    ExchangeCreateActuator actuator = new ExchangeCreateActuator(getContract(
            OWNER_ADDRESS_FIRST, firstTokenId, firstTokenBalance, secondTokenId, secondTokenBalance),
            dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);
    try {
      actuator.validate();
      actuator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("first token balance is not enough",
              e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

}