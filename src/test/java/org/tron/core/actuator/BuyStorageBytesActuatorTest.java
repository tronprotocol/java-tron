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
public class BuyStorageBytesActuatorTest {

  private static Manager dbManager;
  private static final String dbPath = "output_buy_storage_bytes_test";
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

  private Any getContract(String ownerAddress, long bytes) {
    return Any.pack(
        Contract.BuyStorageBytesContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
            .setBytes(bytes)
            .build());
  }

  @Test
  public void testBuyStorageBytes() {
    long currentPool = dbManager.getDynamicPropertiesStore().getTotalStoragePool();
    long currentReserved = dbManager.getDynamicPropertiesStore().getTotalStorageReserved();
    Assert.assertEquals(currentPool, 100_000_000_000000L);
    Assert.assertEquals(currentReserved, 128L * 1024 * 1024 * 1024);

    long bytes = 2694881440L; // 2 million trx
    long quant = 2_000_000_000_000L;
    BuyStorageBytesActuator actuator = new BuyStorageBytesActuator(
        getContract(OWNER_ADDRESS, bytes), dbManager);
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
  public void testBuyStorageBytes2() {
    long currentPool = dbManager.getDynamicPropertiesStore().getTotalStoragePool();
    long currentReserved = dbManager.getDynamicPropertiesStore().getTotalStorageReserved();
    Assert.assertEquals(currentPool, 100_000_000_000000L);
    Assert.assertEquals(currentReserved, 128L * 1024 * 1024 * 1024);

    long quant = 1_000_000_000_000L; // 1 million trx
    long bytes1 = 1360781717L;
    long bytes2 = 2694881439L - bytes1;

    BuyStorageBytesActuator actuator = new BuyStorageBytesActuator(
        getContract(OWNER_ADDRESS, bytes1), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();

    BuyStorageBytesActuator actuator2 = new BuyStorageBytesActuator(
        getContract(OWNER_ADDRESS, bytes2), dbManager);
    TransactionResultCapsule ret2 = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      Assert.assertEquals(owner.getBalance(), initBalance - quant
          - ChainConstant.TRANSFER_FEE);
      Assert.assertEquals(bytes1, owner.getStorageLimit());
      Assert.assertEquals(currentReserved - bytes1,
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
      Assert.assertEquals(bytes1 + bytes2, owner.getStorageLimit());
      Assert.assertEquals(currentReserved - bytes1 - bytes2,
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
    long bytes = -1_000_000_000L;
    BuyStorageBytesActuator actuator = new BuyStorageBytesActuator(
        getContract(OWNER_ADDRESS, bytes), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.fail("cannot run here.");

    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("bytes must be positive", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void buyLessThan1Byte() {
    long bytes = 0L;
    BuyStorageBytesActuator actuator = new BuyStorageBytesActuator(
        getContract(OWNER_ADDRESS, bytes), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.fail("cannot run here.");

    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("bytes must be larger than 1, current storage_bytes[0]", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void buyLessThan1Trx() {
    long bytes = 1L;
    BuyStorageBytesActuator actuator = new BuyStorageBytesActuator(
        getContract(OWNER_ADDRESS, bytes), dbManager);
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
  public void buyMoreThanBalance() {
    long currentPool = dbManager.getDynamicPropertiesStore().getTotalStoragePool();
    long currentReserved = dbManager.getDynamicPropertiesStore().getTotalStorageReserved();
    Assert.assertEquals(currentPool, 100_000_000_000000L);
    Assert.assertEquals(currentReserved, 128L * 1024 * 1024 * 1024);

    long bytes = 136178171754L;

    BuyStorageBytesActuator actuator = new BuyStorageBytesActuator(
        getContract(OWNER_ADDRESS, bytes), dbManager);
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
    long bytes = 1_000_000_000L;
    BuyStorageBytesActuator actuator = new BuyStorageBytesActuator(
        getContract(OWNER_ADDRESS_INVALID, bytes), dbManager);
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
    long bytes = 1_000_000_000L;
    BuyStorageBytesActuator actuator = new BuyStorageBytesActuator(
        getContract(OWNER_ACCOUNT_INVALID, bytes), dbManager);
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
