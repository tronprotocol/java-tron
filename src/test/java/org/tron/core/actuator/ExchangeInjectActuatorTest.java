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

  private Any getContract(String address, long exchangeId, String tokenId, long quant) {
    return Any.pack(
        Contract.ExchangeInjectContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(address)))
            .setExchangeId(exchangeId)
            .setTokenId(ByteString.copyFrom(tokenId.getBytes()))
            .setQuant(quant)
            .build());
  }

  /**
   * first inject Exchange,result is success.
   */
  @Test
  public void successExchangeInject() {
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
   * second inject Exchange,result is success.
   */
  @Test
  public void successExchangeInject2() {
    long exchangeId = 2;
    String firstTokenId = "_";
    long firstTokenQuant = 100_000_000000L;
    String secondTokenId = "def";
    long secondTokenQuant = 4_000_000L;

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
   * use Invalid Address, result is failed, exception is "Invalid address".
   */
  @Test
  public void invalidAddress() {
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
   * use AccountStore not exists, result is failed, exception is "account not exists".
   */
  @Test
  public void noAccount() {
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
   * Exchange not exists
   */
  @Test
  public void exchangeNotExist() {
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
   * account[" + readableOwnerAddress + "] is not creator
   */
  @Test
  public void accountIsNotCreator() {
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
   * token is not in exchange
   */
  @Test
  public void tokenIsNotInExchange() {
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
   * injected token quant must greater than zero
   */
  @Test
  public void tokenQuantLessThanZero() {
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
   * "the calculated token quant  must be greater than 0"
   */
  @Test
  public void calculatedTokenQuantLessThanZero() {
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
   * token balance must less than balanceLimit
   */
  @Test
  public void tokenBalanceGreaterThanBalanceLimit() {
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
   * balance is not enough
   */
  @Test
  public void balanceNotEnough() {
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
   * first token balance is not enough
   */
  @Test
  public void tokenBalanceNotEnough() {
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
   * balance is not enough2
   */
  @Test
  public void balanceNotEnough2() {
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
   * first token balance is not enough
   */
  @Test
  public void anotherTokenBalanceNotEnough() {
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

}