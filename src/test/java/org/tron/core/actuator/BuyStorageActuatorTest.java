package org.tron.core.actuator;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.io.File;
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
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.Parameter.ChainConstant;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Contract;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Transaction.Result.code;

@Slf4j
public class BuyStorageActuatorTest {

  private static Manager dbManager;
  private static final String dbPath = "output_buy_storage_test1";
  private static TronApplicationContext context;
  private static final String OWNER_ADDRESS;
  private static final String OWNER_ADDRESS_INVALID = "aaaa";
  private static final String OWNER_ACCOUNT_INVALID;
  private static final long initBalance = 10_000_000_000_000_000L;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
    OWNER_ACCOUNT_INVALID =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a3456";
  }

  /**
   * Init data.
   */
  @BeforeClass
  public static void init() {
    dbManager = context.getBean(Manager.class);
    //    Args.setParam(new String[]{"--output-directory", dbPath},
    //        "config-junit.conf");
    //    dbManager = new Manager();
    //    dbManager.init();
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
  public void createAccountCapsule() {
    AccountCapsule ownerCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8("owner"),
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
            AccountType.Normal,
            initBalance);
    dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);

    dbManager.getDynamicPropertiesStore().saveTotalStorageReserved(
        128L * 1024 * 1024 * 1024);
    dbManager.getDynamicPropertiesStore().saveTotalStoragePool(100_000_000_000000L);
    dbManager.getDynamicPropertiesStore().saveTotalStorageTax(0);

    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(0);
  }

  private Any getContract(String ownerAddress, long quant) {
    return Any.pack(
        Contract.BuyStorageContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
            .setQuant(quant)
            .build());
  }

  @Test
  public void testBuyStorage() {
    long currentPool = dbManager.getDynamicPropertiesStore().getTotalStoragePool();
    long currentReserved = dbManager.getDynamicPropertiesStore().getTotalStorageReserved();
    Assert.assertEquals(currentPool, 100_000_000_000000L);
    Assert.assertEquals(currentReserved, 128L * 1024 * 1024 * 1024);

    long quant = 2_000_000_000_000L; // 2 million trx
    BuyStorageActuator actuator = new BuyStorageActuator(
        getContract(OWNER_ADDRESS, quant), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));

      Assert.assertEquals(owner.getBalance(), initBalance - quant
          - ChainConstant.TRANSFER_FEE);
      Assert.assertEquals(2694881440L, owner.getStorageLimit());
      Assert.assertEquals(currentReserved - 2694881440L,
          dbManager.getDynamicPropertiesStore().getTotalStorageReserved());
      Assert.assertEquals(currentPool + quant,
          dbManager.getDynamicPropertiesStore().getTotalStoragePool());
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void testBuyStorage2() {
    long currentPool = dbManager.getDynamicPropertiesStore().getTotalStoragePool();
    long currentReserved = dbManager.getDynamicPropertiesStore().getTotalStorageReserved();
    Assert.assertEquals(currentPool, 100_000_000_000000L);
    Assert.assertEquals(currentReserved, 128L * 1024 * 1024 * 1024);

    long quant = 1_000_000_000_000L; // 1 million trx

    BuyStorageActuator actuator = new BuyStorageActuator(
        getContract(OWNER_ADDRESS, quant), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    BuyStorageActuator actuator2 = new BuyStorageActuator(
        getContract(OWNER_ADDRESS, quant), dbManager);
    TransactionResultCapsule ret2 = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      Assert.assertEquals(owner.getBalance(), initBalance - quant
          - ChainConstant.TRANSFER_FEE);
      Assert.assertEquals(1360781717L, owner.getStorageLimit());
      Assert.assertEquals(currentReserved - 1360781717L,
          dbManager.getDynamicPropertiesStore().getTotalStorageReserved());
      Assert.assertEquals(currentPool + quant,
          dbManager.getDynamicPropertiesStore().getTotalStoragePool());

      actuator2.validate();
      actuator2.execute(ret2);
      Assert.assertEquals(ret2.getInstance().getRet(), code.SUCESS);

      owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      Assert.assertEquals(owner.getBalance(), initBalance - 2 * quant
          - ChainConstant.TRANSFER_FEE);
      Assert.assertEquals(2694881439L, owner.getStorageLimit());
      Assert.assertEquals(currentReserved - 2694881439L,
          dbManager.getDynamicPropertiesStore().getTotalStorageReserved());
      Assert.assertEquals(currentPool + 2 * quant,
          dbManager.getDynamicPropertiesStore().getTotalStoragePool());

    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

//  @Test
//  public void testBuyStorageTax() {
//    long currentPool = dbManager.getDynamicPropertiesStore().getTotalStoragePool();
//    long currentReserved = dbManager.getDynamicPropertiesStore().getTotalStorageReserved();
//    Assert.assertEquals(currentPool, 100_000_000_000000L);
//    Assert.assertEquals(currentReserved, 128L * 1024 * 1024 * 1024);
//
//    long quant = 1_000_000_000_000L; // 2 million trx
//
//    BuyStorageActuator actuator = new BuyStorageActuator(
//        getContract(OWNER_ADDRESS, quant), dbManager);
//    TransactionResultCapsule ret = new TransactionResultCapsule();
//
//    BuyStorageActuator actuator2 = new BuyStorageActuator(
//        getContract(OWNER_ADDRESS, quant), dbManager);
//    TransactionResultCapsule ret2 = new TransactionResultCapsule();
//
//    try {
//      actuator.validate();
//      actuator.execute(ret);
//      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
//      AccountCapsule owner =
//          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
//      Assert.assertEquals(owner.getBalance(), initBalance - quant
//          - ChainConstant.TRANSFER_FEE);
//      Assert.assertEquals(1360781717L, owner.getStorageLimit());
//      Assert.assertEquals(currentReserved - 1360781717L,
//          dbManager.getDynamicPropertiesStore().getTotalStorageReserved());
//      Assert.assertEquals(currentPool + quant,
//          dbManager.getDynamicPropertiesStore().getTotalStoragePool());
//
//      dbManager.getDynamicPropertiesStore()
//          .saveLatestBlockHeaderTimestamp(365 * 24 * 3600 * 1000L);
//      actuator2.validate();
//      actuator2.execute(ret);
//      Assert.assertEquals(ret2.getInstance().getRet(), code.SUCESS);
//      owner =
//          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
//      Assert.assertEquals(owner.getBalance(), initBalance - 2 * quant
//          - ChainConstant.TRANSFER_FEE);
//      Assert.assertEquals(2561459696L, owner.getStorageLimit());
//      long tax = 100899100225L;
//      Assert.assertEquals(tax,
//          dbManager.getDynamicPropertiesStore().getTotalStorageTax());
//      Assert.assertEquals(currentReserved - 2561459696L,
//          dbManager.getDynamicPropertiesStore().getTotalStorageReserved());
//      Assert.assertEquals(currentPool + 2 * quant - tax,
//          dbManager.getDynamicPropertiesStore().getTotalStoragePool());
//
//    } catch (ContractValidateException e) {
//      Assert.assertFalse(e instanceof ContractValidateException);
//    } catch (ContractExeException e) {
//      Assert.assertFalse(e instanceof ContractExeException);
//    }
//  }

  @Test
  public void buyLessThanZero() {
    long quant = -1_000_000_000L;
    BuyStorageActuator actuator = new BuyStorageActuator(
        getContract(OWNER_ADDRESS, quant), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.fail("cannot run here.");

    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("quantity must be positive", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void buyLessThan1Trx() {
    long quant = 200_000L;
    BuyStorageActuator actuator = new BuyStorageActuator(
        getContract(OWNER_ADDRESS, quant), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.fail("cannot run here.");

    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("quantity must be larger than 1TRX", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void buyLessThan1Byte() {
    long currentPool = dbManager.getDynamicPropertiesStore().getTotalStoragePool();
    long currentReserved = dbManager.getDynamicPropertiesStore().getTotalStorageReserved();
    Assert.assertEquals(currentPool, 100_000_000_000000L);
    Assert.assertEquals(currentReserved, 128L * 1024 * 1024 * 1024);

    long quant = 9_000_000_000_000_000L; // 9 billion trx

    BuyStorageActuator actuator = new BuyStorageActuator(
        getContract(OWNER_ADDRESS, quant), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    BuyStorageActuator actuator2 = new BuyStorageActuator(
        getContract(OWNER_ADDRESS, 1_000_000), dbManager);
    TransactionResultCapsule ret2 = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      Assert.assertEquals(owner.getBalance(), initBalance - quant
          - ChainConstant.TRANSFER_FEE);
      Assert.assertEquals(135928635301L, owner.getStorageLimit());
      Assert.assertEquals(currentReserved - 135928635301L,
          dbManager.getDynamicPropertiesStore().getTotalStorageReserved());
      Assert.assertEquals(currentPool + quant,
          dbManager.getDynamicPropertiesStore().getTotalStoragePool());

      actuator2.validate();
      actuator2.execute(ret2);
      Assert.fail("cannot run here.");

    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("storage_bytes must be larger than 1,current storage_bytes[0]",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

  }

  @Test
  public void buyMoreThanBalance() {
    long quant = 11_000_000_000_000_000L;
    BuyStorageActuator actuator = new BuyStorageActuator(
        getContract(OWNER_ADDRESS, quant), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.fail("cannot run here.");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("quantity must be less than accountBalance", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void invalidOwnerAddress() {
    long quant = 1_000_000_000L;
    BuyStorageActuator actuator = new BuyStorageActuator(
        getContract(OWNER_ADDRESS_INVALID, quant), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.fail("cannot run here.");

    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);

      Assert.assertEquals("Invalid address", e.getMessage());

    } catch (ContractExeException e) {
      Assert.assertTrue(e instanceof ContractExeException);
    }

  }

  @Test
  public void invalidOwnerAccount() {
    long quant = 1_000_000_000L;
    BuyStorageActuator actuator = new BuyStorageActuator(
        getContract(OWNER_ACCOUNT_INVALID, quant), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.fail("cannot run here.");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Account[" + OWNER_ACCOUNT_INVALID + "] not exists",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

}
