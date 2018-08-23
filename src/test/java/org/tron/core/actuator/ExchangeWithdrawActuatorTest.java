package org.tron.core.actuator;

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
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
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

public class ExchangeWithdrawActuatorTest {

  private static AnnotationConfigApplicationContext context;
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
    context = new AnnotationConfigApplicationContext(DefaultConfig.class);
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
    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
    }
    context.destroy();
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
            "abc".getBytes(),
            "def".getBytes());
    exchangeCapsule.setBalance(100000000L, 200000000L);

    dbManager.getAccountStore()
        .put(ownerAccountFirstCapsule.getAddress().toByteArray(), ownerAccountFirstCapsule);
    dbManager.getAccountStore()
        .put(ownerAccountSecondCapsule.getAddress().toByteArray(), ownerAccountSecondCapsule);
    dbManager.getExchangeStore()
        .put(exchangeCapsule.createDbKey(), exchangeCapsule);

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

  /**
   * first withdraw Exchange,result is success.
   */
  @Test
  public void successExchangeWithdraw() {
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

      accountCapsule = dbManager.getAccountStore().get(ownerAddress);
      assetMap = accountCapsule.getAssetMap();
      Assert.assertEquals(10000_000000L, accountCapsule.getBalance());
      Assert.assertEquals(firstTokenQuant, assetMap.get(firstTokenId).longValue());
      Assert.assertEquals(secondTokenQuant, assetMap.get(secondTokenId).longValue());

    } catch (ContractValidateException e) {
      logger.info(e.getMessage());
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } catch (ItemNotFoundException e) {
      Assert.assertFalse(e instanceof ItemNotFoundException);
    }
  }

//  /**
//   * use Invalid Address, result is failed, exception is "Invalid address".
//   */
//  @Test
//  public void invalidAddress() {
//    HashMap<Long, Long> paras = new HashMap<>();
//    paras.put(0L, 10000L);
//    ExchangeCreateActuator actuator =
//        new ExchangeCreateActuator(getContract(OWNER_ADDRESS_INVALID, paras), dbManager);
//    TransactionResultCapsule ret = new TransactionResultCapsule();
//    try {
//      actuator.validate();
//      actuator.execute(ret);
//      fail("Invalid address");
//    } catch (ContractValidateException e) {
//      Assert.assertTrue(e instanceof ContractValidateException);
//      Assert.assertEquals("Invalid address", e.getMessage());
//    } catch (ContractExeException e) {
//      Assert.assertFalse(e instanceof ContractExeException);
//    }
//  }
//
//  /**
//   * use AccountStore not exists, result is failed, exception is "account not exists".
//   */
//  @Test
//  public void noAccount() {
//    HashMap<Long, Long> paras = new HashMap<>();
//    paras.put(0L, 10000L);
//    ExchangeCreateActuator actuator =
//        new ExchangeCreateActuator(getContract(OWNER_ADDRESS_NOACCOUNT, paras), dbManager);
//    TransactionResultCapsule ret = new TransactionResultCapsule();
//    try {
//      actuator.validate();
//      actuator.execute(ret);
//      fail("account[+OWNER_ADDRESS_NOACCOUNT+] not exists");
//    } catch (ContractValidateException e) {
//      Assert.assertTrue(e instanceof ContractValidateException);
//      Assert.assertEquals("account[" + OWNER_ADDRESS_NOACCOUNT + "] not exists",
//          e.getMessage());
//    } catch (ContractExeException e) {
//      Assert.assertFalse(e instanceof ContractExeException);
//    }
//  }
//
//  /**
//   * use WitnessStore not exists Address,result is failed,exception is "witness not exists".
//   */
//  @Test
//  public void noWitness() {
//    HashMap<Long, Long> paras = new HashMap<>();
//    paras.put(0L, 10000L);
//    ExchangeCreateActuator actuator =
//        new ExchangeCreateActuator(getContract(OWNER_ADDRESS_SECOND, paras), dbManager);
//    TransactionResultCapsule ret = new TransactionResultCapsule();
//    try {
//      actuator.validate();
//      actuator.execute(ret);
//      fail("witness[+OWNER_ADDRESS_NOWITNESS+] not exists");
//    } catch (ContractValidateException e) {
//      Assert.assertTrue(e instanceof ContractValidateException);
//      Assert.assertEquals("Witness[" + OWNER_ADDRESS_SECOND + "] not exists",
//          e.getMessage());
//    } catch (ContractExeException e) {
//      Assert.assertFalse(e instanceof ContractExeException);
//    }
//  }
//
//  /**
//   * use invalid parameter, result is failed, exception is "Bad chain parameter id".
//   */
//  @Test
//  public void invalidPara() {
//    HashMap<Long, Long> paras = new HashMap<>();
//    paras.put(17L, 10000L);
//    ExchangeCreateActuator actuator =
//        new ExchangeCreateActuator(getContract(OWNER_ADDRESS_FIRST, paras), dbManager);
//    TransactionResultCapsule ret = new TransactionResultCapsule();
//    try {
//      actuator.validate();
//      actuator.execute(ret);
//      fail("Bad chain parameter id");
//    } catch (ContractValidateException e) {
//      Assert.assertTrue(e instanceof ContractValidateException);
//      Assert.assertEquals("Bad chain parameter id",
//          e.getMessage());
//    } catch (ContractExeException e) {
//      Assert.assertFalse(e instanceof ContractExeException);
//    }
//
//    paras = new HashMap<>();
//    paras.put(3L, 1 + 100_000_000_000_000_000L);
//    actuator =
//        new ExchangeCreateActuator(getContract(OWNER_ADDRESS_FIRST, paras), dbManager);
//    try {
//      actuator.validate();
//      actuator.execute(ret);
//      fail("Bad chain parameter value,valid range is [0,100_000_000_000_000_000L]");
//    } catch (ContractValidateException e) {
//      Assert.assertTrue(e instanceof ContractValidateException);
//      Assert.assertEquals("Bad chain parameter value,valid range is [0,100_000_000_000_000_000L]",
//          e.getMessage());
//    } catch (ContractExeException e) {
//      Assert.assertFalse(e instanceof ContractExeException);
//    }
//
//    paras = new HashMap<>();
//    paras.put(10L, -1L);
//    actuator =
//        new ExchangeCreateActuator(getContract(OWNER_ADDRESS_FIRST, paras), dbManager);
//    dbManager.getDynamicPropertiesStore().saveRemoveThePowerOfTheGr(-1);
//    try {
//      actuator.validate();
//      fail("This exchange has been executed before and is only allowed to be executed once");
//    } catch (ContractValidateException e) {
//      Assert.assertTrue(e instanceof ContractValidateException);
//      Assert.assertEquals(
//          "This exchange has been executed before and is only allowed to be executed once",
//          e.getMessage());
//    }
//
//    paras.put(10L, -1L);
//    dbManager.getDynamicPropertiesStore().saveRemoveThePowerOfTheGr(0);
//    actuator =
//        new ExchangeCreateActuator(getContract(OWNER_ADDRESS_FIRST, paras), dbManager);
//    dbManager.getDynamicPropertiesStore().saveRemoveThePowerOfTheGr(0);
//    try {
//      actuator.validate();
//      fail("This value[REMOVE_THE_POWER_OF_THE_GR] is only allowed to be 1");
//    } catch (ContractValidateException e) {
//      Assert.assertTrue(e instanceof ContractValidateException);
//      Assert.assertEquals("This value[REMOVE_THE_POWER_OF_THE_GR] is only allowed to be 1",
//          e.getMessage());
//    }
//  }

}