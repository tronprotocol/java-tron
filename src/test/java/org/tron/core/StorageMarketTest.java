package org.tron.core;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.Parameter.ChainConstant;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.db.StorageMarket;
import org.tron.protos.Contract;
import org.tron.protos.Protocol.AccountType;

@Slf4j
public class StorageMarketTest {

  private static Manager dbManager;
  private static StorageMarket storageMarket;
  private static final String dbPath = "output_buy_storage_test";
  private static AnnotationConfigApplicationContext context;
  private static final String OWNER_ADDRESS;
  private static final String OWNER_ADDRESS_INVALID = "aaaa";
  private static final String OWNER_ACCOUNT_INVALID;
  private static final long initBalance = 10_000_000_000_000_000L;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath}, Constant.TEST_CONF);
    context = new AnnotationConfigApplicationContext(DefaultConfig.class);
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
    storageMarket = new StorageMarket(dbManager);
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

    AccountCapsule owner =
        dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));

    long quant = 2_000_000_000_000L; // 2 million trx
    storageMarket.buyStorage(owner, quant);

    Assert.assertEquals(owner.getBalance(), initBalance - quant
        - ChainConstant.TRANSFER_FEE);
    Assert.assertEquals(2694881440L, owner.getStorageLimit());
    Assert.assertEquals(currentReserved - 2694881440L,
        dbManager.getDynamicPropertiesStore().getTotalStorageReserved());
    Assert.assertEquals(currentPool + quant,
        dbManager.getDynamicPropertiesStore().getTotalStoragePool());

  }

//  @Test
//  public void testBuyStorageBytes() {
//    long currentPool = dbManager.getDynamicPropertiesStore().getTotalStoragePool();
//    long currentReserved = dbManager.getDynamicPropertiesStore().getTotalStorageReserved();
//    Assert.assertEquals(currentPool, 100_000_000_000000L);
//    Assert.assertEquals(currentReserved, 128L * 1024 * 1024 * 1024);
//
//    AccountCapsule owner =
//        dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
//
//    long bytes = 2694881440L; // 2 million trx
//    storageMarket.buyStorageBytes(owner, bytes);
//
//    Assert.assertEquals(owner.getBalance(), initBalance - 2_000_000_000_000L
//        - ChainConstant.TRANSFER_FEE);
//    Assert.assertEquals(bytes, owner.getStorageLimit());
//    Assert.assertEquals(currentReserved - bytes,
//        dbManager.getDynamicPropertiesStore().getTotalStorageReserved());
//    Assert.assertEquals(currentPool + 2_000_000_000_000L,
//        dbManager.getDynamicPropertiesStore().getTotalStoragePool());
//
//  }

//  @Test
//  public void testBuyStorage2() {
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
//      Assert.assertEquals(ret.getInstance().getRet(), code.SUCCESS);
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
//      actuator2.validate();
//      actuator2.execute(ret);
//      Assert.assertEquals(ret2.getInstance().getRet(), code.SUCCESS);
//      owner =
//          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
//      Assert.assertEquals(owner.getBalance(), initBalance - 2 * quant
//          - ChainConstant.TRANSFER_FEE);
//      Assert.assertEquals(2694881439L, owner.getStorageLimit());
//      long tax = 0L;
//      Assert.assertEquals(tax,
//          dbManager.getDynamicPropertiesStore().getTotalStorageTax());
//      Assert.assertEquals(currentReserved - 2694881439L,
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


}
