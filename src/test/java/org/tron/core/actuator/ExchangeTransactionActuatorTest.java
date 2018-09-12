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
import org.tron.core.capsule.ExchangeCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.protos.Contract;
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

    dbManager.getAccountStore()
        .put(ownerAccountFirstCapsule.getAddress().toByteArray(), ownerAccountFirstCapsule);
    dbManager.getAccountStore()
        .put(ownerAccountSecondCapsule.getAddress().toByteArray(), ownerAccountSecondCapsule);
    dbManager.getExchangeStore()
        .put(exchangeCapsule.createDbKey(), exchangeCapsule);
    dbManager.getExchangeStore()
        .put(exchangeCapsule2.createDbKey(), exchangeCapsule2);

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

  /**
   * first transaction Exchange,result is success.
   */
  @Test
  public void successExchangeTransaction() {
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
      exchangeCapsule = dbManager.getExchangeStore().get(ByteArray.fromLong(exchangeId));
      Assert.assertNotNull(exchangeCapsule);

      Assert.assertEquals(exchangeId, exchangeCapsule.getID());
      Assert.assertEquals(1000000, exchangeCapsule.getCreateTime());
      Assert.assertTrue(Arrays.equals(tokenId.getBytes(), exchangeCapsule.getFirstTokenId()));
      Assert.assertEquals(tokenId, ByteArray.toStr(exchangeCapsule.getFirstTokenId()));
      Assert.assertEquals(firstTokenBalance + quant, exchangeCapsule.getFirstTokenBalance());
      Assert.assertEquals("abc", ByteArray.toStr(exchangeCapsule.getSecondTokenId()));
      Assert.assertEquals(9999001L, exchangeCapsule.getSecondTokenBalance());

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
    }
  }

  /**
   * second transaction Exchange,result is success.
   */
  @Test
  public void successExchangeTransaction2() {
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
      exchangeCapsule = dbManager.getExchangeStore().get(ByteArray.fromLong(exchangeId));
      Assert.assertNotNull(exchangeCapsule);

      Assert.assertEquals(exchangeId, exchangeCapsule.getID());
      Assert.assertEquals(1000000, exchangeCapsule.getCreateTime());
      Assert.assertTrue(Arrays.equals(tokenId.getBytes(), exchangeCapsule.getFirstTokenId()));
      Assert.assertEquals(tokenId, ByteArray.toStr(exchangeCapsule.getFirstTokenId()));
      Assert.assertEquals(firstTokenBalance + quant, exchangeCapsule.getFirstTokenBalance());
      Assert.assertEquals("def", ByteArray.toStr(exchangeCapsule.getSecondTokenId()));
      Assert.assertEquals(199998001L, exchangeCapsule.getSecondTokenBalance());

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
    }
  }

  /**
   * use Invalid Address, result is failed, exception is "Invalid address".
   */
  @Test
  public void invalidAddress() {
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
    }
  }

  /**
   * use AccountStore not exists, result is failed, exception is "account not exists".
   */
  @Test
  public void noAccount() {
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
    }
  }

  /**
   * Exchange not exists
   */
  @Test
  public void exchangeNotExist() {
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
    }
  }

  /**
   * token is not in exchange
   */
  @Test
  public void tokenIsNotInExchange() {
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
    }
  }

  /**
   * Token balance in exchange is equal with 0, the exchange has been closed"
   */
  @Test
  public void tokenBalanceZero() {
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
    }
  }

  /**
   * token quant must greater than zero
   */
  @Test
  public void tokenQuantLessThanZero() {
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
    }
  }

  /**
   * token balance must less than balanceLimit
   */
  @Test
  public void tokenBalanceGreaterThanBalanceLimit() {
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
    }
  }

  /**
   * balance is not enough
   */
  @Test
  public void balanceNotEnough() {
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
    }
  }

  /**
   * token balance is not enough
   */
  @Test
  public void tokenBalanceNotEnough() {
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
    }
  }

  /**
   * token required must greater than expected
   */
  @Test
  public void tokenRequiredNotEnough() {
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
    }
  }
}