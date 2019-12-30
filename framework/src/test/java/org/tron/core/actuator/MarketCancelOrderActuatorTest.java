package org.tron.core.actuator;

import static org.testng.Assert.fail;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.io.File;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.common.zksnark.MarketUtils;
import org.tron.core.ChainBaseManager;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.capsule.MarketAccountOrderCapsule;
import org.tron.core.capsule.MarketOrderCapsule;
import org.tron.core.capsule.MarketOrderIdListCapsule;
import org.tron.core.capsule.MarketPriceListCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.core.store.AccountStore;
import org.tron.core.store.MarketAccountStore;
import org.tron.core.store.MarketOrderStore;
import org.tron.core.store.MarketPairPriceToOrderStore;
import org.tron.core.store.MarketPairToPriceStore;
import org.tron.core.store.MarketPriceStore;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.MarketOrder.State;
import org.tron.protos.Protocol.MarketPrice;
import org.tron.protos.contract.AssetIssueContractOuterClass.AssetIssueContract;
import org.tron.protos.contract.MarketContract.MarketCancelOrderContract;
import org.tron.protos.contract.MarketContract.MarketSellAssetContract;

@Slf4j

public class MarketCancelOrderActuatorTest {

  private static final String dbPath = "output_MarketCancelOrder_test";
  private static final String ACCOUNT_NAME_FIRST = "ownerF";
  private static final String OWNER_ADDRESS_FIRST;
  private static final String ACCOUNT_NAME_SECOND = "ownerS";
  private static final String OWNER_ADDRESS_SECOND;
  private static final String OWNER_ADDRESS_NOT_EXIST;
  private static final String URL = "https://tron.network";
  private static final String OWNER_ADDRESS_INVALID = "aaaa";
  private static final String OWNER_ADDRESS_NOACCOUNT;
  private static final String OWNER_ADDRESS_BALANCENOTSUFFIENT;
  private static final String TOKEN_ID_ONE = String.valueOf(1L);
  private static final String TOKEN_ID_TWO = String.valueOf(2L);
  private static final String TRX = "_";
  private static TronApplicationContext context;
  private static Manager dbManager;

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
    OWNER_ADDRESS_NOT_EXIST =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e06d4271a1c11";
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
    byte[] ownerAddressFirstBytes = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    byte[] ownerAddressSecondBytes = ByteArray.fromHexString(OWNER_ADDRESS_SECOND);

    AccountCapsule ownerAccountFirstCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8(ACCOUNT_NAME_FIRST),
            ByteString.copyFrom(ownerAddressFirstBytes),
            AccountType.Normal,
            10000_000_000L);
    AccountCapsule ownerAccountSecondCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8(ACCOUNT_NAME_SECOND),
            ByteString.copyFrom(ownerAddressSecondBytes),
            AccountType.Normal,
            20000_000_000L);

    dbManager.getAccountStore()
        .put(ownerAccountFirstCapsule.getAddress().toByteArray(), ownerAccountFirstCapsule);
    dbManager.getAccountStore()
        .put(ownerAccountSecondCapsule.getAddress().toByteArray(), ownerAccountSecondCapsule);

    // clean
    cleanMarketOrderByAccount(ownerAddressFirstBytes);
    cleanMarketOrderByAccount(ownerAddressSecondBytes);
    ChainBaseManager chainBaseManager = dbManager.getChainBaseManager();

    chainBaseManager.getMarketAccountStore().delete(ownerAddressFirstBytes);
    chainBaseManager.getMarketAccountStore().delete(ownerAddressSecondBytes);

    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1000000);
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderNumber(10);
    dbManager.getDynamicPropertiesStore().saveNextMaintenanceTime(2000000);
  }

  private void cleanMarketOrderByAccount(byte[] accountAddress) {

    if (accountAddress == null || accountAddress.length == 0) {
      return ;
    }

    MarketAccountOrderCapsule marketAccountOrderCapsule;
    try {
      marketAccountOrderCapsule = dbManager.getChainBaseManager()
          .getMarketAccountStore().get(accountAddress);
    } catch (ItemNotFoundException e) {
      return ;
    }

    MarketOrderStore marketOrderStore = dbManager.getChainBaseManager().getMarketOrderStore();

    List<ByteString> orderIdList = marketAccountOrderCapsule.getOrdersList();
    orderIdList
        .forEach(
            orderId -> marketOrderStore.delete(orderId.toByteArray())
        );
  }

  private Any getContract(String address, String sellTokenId, long sellTokenQuantity
      , String buyTokenId, long buyTokenQuantity) {

    return Any.pack(
        MarketSellAssetContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(address)))
            .setSellTokenId(ByteString.copyFrom(sellTokenId.getBytes()))
            .setSellTokenQuantity(sellTokenQuantity)
            .setBuyTokenId(ByteString.copyFrom(buyTokenId.getBytes()))
            .setBuyTokenQuantity(buyTokenQuantity)
            .build());
  }

  private Any getContract(String address, ByteString orderId) {

    return Any.pack(
        MarketCancelOrderContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(address)))
            .setOrderId(orderId)
            .build());
  }

  private void InitAsset() {
    AssetIssueCapsule assetIssueCapsule1 = new AssetIssueCapsule(
        AssetIssueContract.newBuilder()
            .setName(ByteString.copyFrom("abc".getBytes()))
            .setId(TOKEN_ID_ONE)
            .build());

    AssetIssueCapsule assetIssueCapsule2 = new AssetIssueCapsule(
        AssetIssueContract.newBuilder()
            .setName(ByteString.copyFrom("def".getBytes()))
            .setId(TOKEN_ID_TWO)
            .build());
    dbManager.getAssetIssueV2Store().put(assetIssueCapsule1.createDbV2Key(), assetIssueCapsule1);
    dbManager.getAssetIssueV2Store().put(assetIssueCapsule2.createDbV2Key(), assetIssueCapsule2);

    // clean
    ChainBaseManager chainBaseManager = dbManager.getChainBaseManager();
    chainBaseManager.getMarketPairToPriceStore()
        .delete(MarketUtils.createPairKey(TOKEN_ID_ONE.getBytes(), TOKEN_ID_TWO.getBytes()));
    chainBaseManager.getMarketPairToPriceStore()
        .delete(MarketUtils.createPairKey(TOKEN_ID_TWO.getBytes(), TOKEN_ID_ONE.getBytes()));

    MarketPairPriceToOrderStore pairPriceToOrderStore = chainBaseManager
        .getMarketPairPriceToOrderStore();
    pairPriceToOrderStore.forEach(
        marketOrderIdListCapsuleEntry -> pairPriceToOrderStore
            .delete(marketOrderIdListCapsuleEntry.getKey())
    );
  }

  /**
   * use Invalid Address, result is failed, exception is "Invalid address".
   */
  @Test
  public void invalidOwnerAddress() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    InitAsset();
    ByteString orderId = ByteString.copyFromUtf8("123");

    MarketCancelOrderActuator actuator = new MarketCancelOrderActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(getContract(
        OWNER_ADDRESS_INVALID, orderId));

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
    }
  }

  /**
   * Account not exist , result is failed, exception is "token quantity must greater than zero".
   */
  @Test
  public void notExistAccount() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    InitAsset();
    ByteString orderId = ByteString.copyFromUtf8("123");

    MarketCancelOrderActuator actuator = new MarketCancelOrderActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(getContract(
        OWNER_ADDRESS_NOT_EXIST, orderId));

    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      fail("Account does not exist!");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Account does not exist!", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
    }
  }

  /**
   * orderId not exists, result is failed, exception is "orderId not exists".
   */
  @Test
  public void notExistOrder() {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    InitAsset();
    ByteString orderId = ByteString.copyFromUtf8("123");

    MarketCancelOrderActuator actuator = new MarketCancelOrderActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(getContract(
        OWNER_ADDRESS_FIRST, orderId));

    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      fail("orderId not exists");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("orderId not exists", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
    }
  }

  /**
   * Order is not active!, result is failed, exception is "Order is not active!".
   */
  @Test
  public void orderNotActive() throws Exception {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    InitAsset();

    //prepare env
    addOrder(TOKEN_ID_ONE, 100L, TOKEN_ID_TWO,
        200L, OWNER_ADDRESS_FIRST);

    ChainBaseManager chainBaseManager = dbManager.getChainBaseManager();
    MarketAccountStore marketAccountStore = chainBaseManager.getMarketAccountStore();
    MarketOrderStore orderStore = chainBaseManager.getMarketOrderStore();

    MarketAccountOrderCapsule accountOrderCapsule = marketAccountStore
        .get(ByteArray.fromHexString(OWNER_ADDRESS_FIRST));
    ByteString orderId = accountOrderCapsule.getOrdersList().get(0);
    MarketOrderCapsule orderCapsule = orderStore.get(orderId.toByteArray());
    orderCapsule.setState(State.CANCELED);
    orderStore.put(orderId.toByteArray(), orderCapsule);

    MarketCancelOrderActuator actuator = new MarketCancelOrderActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(getContract(
        OWNER_ADDRESS_FIRST, orderId));

    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      fail("Order is not active!");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Order is not active!", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
    }
  }


  /**
   * Order does not belong to the account!, result is failed, exception is "Order does not belong to
   * the account!".
   */
  @Test
  public void notBelongToTheAccount() throws Exception {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    InitAsset();

    //prepare env
    addOrder(TOKEN_ID_ONE, 100L, TOKEN_ID_TWO,
        200L, OWNER_ADDRESS_FIRST);

    ChainBaseManager chainBaseManager = dbManager.getChainBaseManager();
    MarketAccountStore marketAccountStore = chainBaseManager.getMarketAccountStore();
    MarketOrderStore orderStore = chainBaseManager.getMarketOrderStore();

    MarketAccountOrderCapsule accountOrderCapsule = marketAccountStore
        .get(ByteArray.fromHexString(OWNER_ADDRESS_FIRST));
    ByteString orderId = accountOrderCapsule.getOrdersList().get(0);

    MarketCancelOrderActuator actuator = new MarketCancelOrderActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(getContract(
        OWNER_ADDRESS_SECOND, orderId));

    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      fail("Order does not belong to the account!");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Order does not belong to the account!", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
    }
  }

  /**
   * No enough balance !, result is failed, exception is "No enough balance !".
   */
  @Test
  public void noEnoughBalance() throws Exception {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    InitAsset();

    //prepare env
    addOrder(TOKEN_ID_ONE, 100L, TOKEN_ID_TWO,
        200L, OWNER_ADDRESS_FIRST);

    ChainBaseManager chainBaseManager = dbManager.getChainBaseManager();
    MarketAccountStore marketAccountStore = chainBaseManager.getMarketAccountStore();
    AccountStore accountStore = chainBaseManager.getAccountStore();
    AccountCapsule accountCapsule = accountStore.get(ByteArray.fromHexString(OWNER_ADDRESS_FIRST));
    accountCapsule.setBalance(0L);
    accountStore.put(ByteArray.fromHexString(OWNER_ADDRESS_FIRST), accountCapsule);

    MarketAccountOrderCapsule accountOrderCapsule = marketAccountStore
        .get(ByteArray.fromHexString(OWNER_ADDRESS_FIRST));
    ByteString orderId = accountOrderCapsule.getOrdersList().get(0);

    MarketCancelOrderActuator actuator = new MarketCancelOrderActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(getContract(
        OWNER_ADDRESS_FIRST, orderId));

    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      fail("No enough balance !");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("No enough balance !", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
    }
  }


  /**
   * validate Success, result is Success .
   */
  @Test
  public void validateSuccess() throws Exception {

    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    InitAsset();

    //prepare env
    addOrder(TOKEN_ID_ONE, 100L, TOKEN_ID_TWO,
        200L, OWNER_ADDRESS_FIRST);

    ChainBaseManager chainBaseManager = dbManager.getChainBaseManager();
    MarketAccountStore marketAccountStore = chainBaseManager.getMarketAccountStore();
    AccountStore accountStore = chainBaseManager.getAccountStore();

    MarketAccountOrderCapsule accountOrderCapsule = marketAccountStore
        .get(ByteArray.fromHexString(OWNER_ADDRESS_FIRST));
    ByteString orderId = accountOrderCapsule.getOrdersList().get(0);

    MarketCancelOrderActuator actuator = new MarketCancelOrderActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(getContract(
        OWNER_ADDRESS_FIRST, orderId));

    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      fail("validateSuccess error");
    } finally {
    }
  }

  private void cancelOrder(ByteString orderId) throws Exception {

    MarketCancelOrderActuator actuator = new MarketCancelOrderActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(getContract(
        OWNER_ADDRESS_FIRST, orderId));

    TransactionResultCapsule ret = new TransactionResultCapsule();

    actuator.validate();
    actuator.execute(ret);
  }

  private void addOrder(String sellTokenId, long sellTokenQuant,
      String buyTokenId, long buyTokenQuant, String ownAddress) throws Exception {

    byte[] ownerAddress = ByteArray.fromHexString(ownAddress);
    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    accountCapsule.addAssetAmountV2(sellTokenId.getBytes(), sellTokenQuant,
        dbManager.getDynamicPropertiesStore(), dbManager.getAssetIssueStore());
    dbManager.getAccountStore().put(ownerAddress, accountCapsule);

    // do process
    MarketSellAssetActuator actuator = new MarketSellAssetActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(getContract(
        ownAddress, sellTokenId, sellTokenQuant, buyTokenId, buyTokenQuant));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    actuator.validate();
    actuator.execute(ret);
  }

  // execute:
  // There are multiple orders at this price
  // Only one order at this price
  //    This trading pair has multiple prices
  //    There is only one price for this trading pair

  /**
   * There are multiple orders at this price,return TOKEN
   */
  @Test
  public void multipleOrdersAtThisPrice1() throws Exception {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    InitAsset();

    //get storeDb
    ChainBaseManager chainBaseManager = dbManager.getChainBaseManager();
    MarketAccountStore marketAccountStore = chainBaseManager.getMarketAccountStore();
    AccountStore accountStore = chainBaseManager.getAccountStore();
    MarketOrderStore orderStore = chainBaseManager.getMarketOrderStore();
    MarketPairToPriceStore pairToPriceStore = chainBaseManager.getMarketPairToPriceStore();
    MarketPairPriceToOrderStore pairPriceToOrderStore = chainBaseManager
        .getMarketPairPriceToOrderStore();
    MarketPriceStore marketPriceStore = chainBaseManager.getMarketPriceStore();

    addOrder(TOKEN_ID_ONE, 100L, TOKEN_ID_TWO,
        200L, OWNER_ADDRESS_FIRST);
    addOrder(TOKEN_ID_ONE, 100L, TOKEN_ID_TWO,
        300L, OWNER_ADDRESS_FIRST);
    addOrder(TOKEN_ID_ONE, 100L, TOKEN_ID_TWO,
        300L, OWNER_ADDRESS_FIRST);//cancel this one
    addOrder(TOKEN_ID_ONE, 100L, TOKEN_ID_TWO,
        300L, OWNER_ADDRESS_FIRST);
    addOrder(TOKEN_ID_ONE, 100L, TOKEN_ID_TWO,
        400L, OWNER_ADDRESS_FIRST);

    //record account state
    AccountCapsule accountCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS_FIRST));
    long balanceBefore = accountCapsule.getBalance();

    MarketAccountOrderCapsule accountOrderCapsule = marketAccountStore
        .get(ByteArray.fromHexString(OWNER_ADDRESS_FIRST));
    ByteString orderId = accountOrderCapsule.getOrdersList().get(2);

    // cancel the third order
    cancelOrder(orderId);

    //check balance
    accountCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS_FIRST));

    Assert.assertEquals(balanceBefore,
        dbManager.getDynamicPropertiesStore().getMarketCancelFee() + accountCapsule.getBalance());

    //check accountOrder
    accountOrderCapsule = marketAccountStore.get(ByteArray.fromHexString(OWNER_ADDRESS_FIRST));
    Assert.assertEquals(accountOrderCapsule.getCount(), 5);
    orderId = accountOrderCapsule.getOrdersList().get(2);

    //check order
    MarketOrderCapsule orderCapsule = orderStore.get(orderId.toByteArray());
    Assert.assertEquals(orderCapsule.getSellTokenQuantityRemain(), 0L);
    Assert.assertEquals(orderCapsule.getSellTokenQuantity(), 100L);
    Assert.assertEquals(orderCapsule.getBuyTokenQuantity(), 300L);
    Assert.assertEquals(orderCapsule.getSt(), State.CANCELED);

    //check pairToPrice
    byte[] marketPair = MarketUtils.createPairKey(TOKEN_ID_ONE.getBytes(), TOKEN_ID_TWO.getBytes());
    MarketPriceListCapsule priceListCapsule = pairToPriceStore.get(marketPair);
    Assert.assertEquals(priceListCapsule.getPriceSize(marketPriceStore), 3);

    MarketPrice marketPrice = priceListCapsule.getPriceByIndex(1, marketPriceStore).getInstance();
    Assert.assertEquals(marketPrice.getSellTokenQuantity(), 100L);
    Assert.assertEquals(marketPrice.getBuyTokenQuantity(), 300L);

    //check pairPriceToOrder
    byte[] pairPriceKey = MarketUtils.createPairPriceKey(
        priceListCapsule.getSellTokenId(), priceListCapsule.getBuyTokenId(),
        marketPrice.getSellTokenQuantity(), marketPrice.getBuyTokenQuantity());
    MarketOrderIdListCapsule orderIdListCapsule = pairPriceToOrderStore
        .get(pairPriceKey);
    Assert.assertEquals(orderIdListCapsule.getOrdersList().size(), 2);
  }

  /**
   * There are multiple orders at this price,return TRX
   */
  @Test
  public void multipleOrdersAtThisPrice2() throws Exception {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    InitAsset();

    //get storeDb
    ChainBaseManager chainBaseManager = dbManager.getChainBaseManager();
    MarketAccountStore marketAccountStore = chainBaseManager.getMarketAccountStore();
    AccountStore accountStore = chainBaseManager.getAccountStore();
    MarketOrderStore orderStore = chainBaseManager.getMarketOrderStore();
    MarketPairToPriceStore pairToPriceStore = chainBaseManager.getMarketPairToPriceStore();
    MarketPairPriceToOrderStore pairPriceToOrderStore = chainBaseManager
        .getMarketPairPriceToOrderStore();
    MarketPriceStore marketPriceStore = chainBaseManager.getMarketPriceStore();

    addOrder("_", 100L, TOKEN_ID_TWO,
        200L, OWNER_ADDRESS_FIRST);
    addOrder("_", 100L, TOKEN_ID_TWO,
        300L, OWNER_ADDRESS_FIRST);
    addOrder("_", 100L, TOKEN_ID_TWO,
        300L, OWNER_ADDRESS_FIRST);//cancel this one
    addOrder("_", 100L, TOKEN_ID_TWO,
        300L, OWNER_ADDRESS_FIRST);
    addOrder("_", 100L, TOKEN_ID_TWO,
        400L, OWNER_ADDRESS_FIRST);

    //record account state
    AccountCapsule accountCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS_FIRST));
    long balanceBefore = accountCapsule.getBalance();

    MarketAccountOrderCapsule accountOrderCapsule = marketAccountStore
        .get(ByteArray.fromHexString(OWNER_ADDRESS_FIRST));
    ByteString orderId = accountOrderCapsule.getOrdersList().get(2);

    // cancel the third order
    cancelOrder(orderId);

    //check balance
    accountCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS_FIRST));

    Assert.assertEquals(
        balanceBefore + 100L - dbManager.getDynamicPropertiesStore().getMarketCancelFee(),
        +accountCapsule.getBalance());

    //check accountOrder
    accountOrderCapsule = marketAccountStore.get(ByteArray.fromHexString(OWNER_ADDRESS_FIRST));
    Assert.assertEquals(accountOrderCapsule.getCount(), 5);
    orderId = accountOrderCapsule.getOrdersList().get(2);

    //check order
    MarketOrderCapsule orderCapsule = orderStore.get(orderId.toByteArray());
    Assert.assertEquals(orderCapsule.getSellTokenQuantityRemain(), 0L);
    Assert.assertEquals(orderCapsule.getSellTokenQuantity(), 100L);
    Assert.assertEquals(orderCapsule.getBuyTokenQuantity(), 300L);
    Assert.assertEquals(orderCapsule.getSt(), State.CANCELED);

    //check pairToPrice
    byte[] marketPair = MarketUtils.createPairKey("_".getBytes(), TOKEN_ID_TWO.getBytes());
    MarketPriceListCapsule priceListCapsule = pairToPriceStore.get(marketPair);
    Assert.assertEquals(priceListCapsule.getPriceSize(marketPriceStore), 3);

    MarketPrice marketPrice = priceListCapsule.getPriceByIndex(1, marketPriceStore).getInstance();
    Assert.assertEquals(marketPrice.getSellTokenQuantity(), 100L);
    Assert.assertEquals(marketPrice.getBuyTokenQuantity(), 300L);

    //check pairPriceToOrder
    byte[] pairPriceKey = MarketUtils.createPairPriceKey(
        priceListCapsule.getSellTokenId(), priceListCapsule.getBuyTokenId(),
        marketPrice.getSellTokenQuantity(), marketPrice.getBuyTokenQuantity());
    MarketOrderIdListCapsule orderIdListCapsule = pairPriceToOrderStore
        .get(pairPriceKey);
    Assert.assertEquals(orderIdListCapsule.getOrdersList().size(), 2);
  }


  /**
   * Only one order at this price,and this trading pair has multiple prices
   */
  @Test
  public void onlyOneOrderAtThisPriceAndHasMultiplePrices() throws Exception {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    InitAsset();

    //get storeDb
    ChainBaseManager chainBaseManager = dbManager.getChainBaseManager();
    MarketAccountStore marketAccountStore = chainBaseManager.getMarketAccountStore();
    AccountStore accountStore = chainBaseManager.getAccountStore();
    MarketOrderStore orderStore = chainBaseManager.getMarketOrderStore();
    MarketPairToPriceStore pairToPriceStore = chainBaseManager.getMarketPairToPriceStore();
    MarketPairPriceToOrderStore pairPriceToOrderStore = chainBaseManager
        .getMarketPairPriceToOrderStore();
    MarketPriceStore marketPriceStore = chainBaseManager.getMarketPriceStore();

    addOrder(TOKEN_ID_ONE, 100L, TOKEN_ID_TWO,
        200L, OWNER_ADDRESS_FIRST);
    addOrder(TOKEN_ID_ONE, 100L, TOKEN_ID_TWO,
        300L, OWNER_ADDRESS_FIRST);//cancel this one
    addOrder(TOKEN_ID_ONE, 100L, TOKEN_ID_TWO,
        400L, OWNER_ADDRESS_FIRST);

    //record account state
    AccountCapsule accountCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS_FIRST));
    long balanceBefore = accountCapsule.getBalance();

    MarketAccountOrderCapsule accountOrderCapsule = marketAccountStore
        .get(ByteArray.fromHexString(OWNER_ADDRESS_FIRST));
    ByteString orderId = accountOrderCapsule.getOrdersList().get(1);

    // cancel the second order
    cancelOrder(orderId);

    //check balance
    accountCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS_FIRST));

    Assert.assertEquals(
        balanceBefore - dbManager.getDynamicPropertiesStore().getMarketCancelFee(),
        +accountCapsule.getBalance());

    //check accountOrder
    accountOrderCapsule = marketAccountStore.get(ByteArray.fromHexString(OWNER_ADDRESS_FIRST));
    Assert.assertEquals(accountOrderCapsule.getCount(), 3);
    orderId = accountOrderCapsule.getOrdersList().get(1);

    //check order
    MarketOrderCapsule orderCapsule = orderStore.get(orderId.toByteArray());
    Assert.assertEquals(orderCapsule.getSellTokenQuantityRemain(), 0L);
    Assert.assertEquals(orderCapsule.getSellTokenQuantity(), 100L);
    Assert.assertEquals(orderCapsule.getBuyTokenQuantity(), 300L);
    Assert.assertEquals(orderCapsule.getSt(), State.CANCELED);

    //check pairToPrice
    byte[] marketPair = MarketUtils.createPairKey(TOKEN_ID_ONE.getBytes(), TOKEN_ID_TWO.getBytes());
    MarketPriceListCapsule priceListCapsule = pairToPriceStore.get(marketPair);
    Assert.assertEquals(priceListCapsule.getPriceSize(marketPriceStore), 2);

    MarketPrice marketPrice = priceListCapsule.getBestPrice();
    Assert.assertEquals(marketPrice.getSellTokenQuantity(), 100L);
    Assert.assertEquals(marketPrice.getBuyTokenQuantity(), 200L);

    marketPrice = priceListCapsule.getPriceByIndex(1, marketPriceStore).getInstance();
    Assert.assertEquals(marketPrice.getSellTokenQuantity(), 100L);
    Assert.assertEquals(marketPrice.getBuyTokenQuantity(), 400L);

    //check pairPriceToOrder
    byte[] pairPriceKey = MarketUtils.createPairPriceKey(
        priceListCapsule.getSellTokenId(), priceListCapsule.getBuyTokenId(),
        100L, 300L);
    MarketOrderIdListCapsule orderIdListCapsule = pairPriceToOrderStore
        .getUnchecked(pairPriceKey);
    Assert.assertEquals(orderIdListCapsule, null);
  }


  /**
   * Only one order at this price,and there is only one price for this trading pair
   */
  @Test
  public void onlyOneOrderAtThisPriceAndHasOnlyOnePrice() throws Exception {
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    InitAsset();

    //get storeDb
    ChainBaseManager chainBaseManager = dbManager.getChainBaseManager();
    MarketAccountStore marketAccountStore = chainBaseManager.getMarketAccountStore();
    AccountStore accountStore = chainBaseManager.getAccountStore();
    MarketOrderStore orderStore = chainBaseManager.getMarketOrderStore();
    MarketPairToPriceStore pairToPriceStore = chainBaseManager.getMarketPairToPriceStore();
    MarketPairPriceToOrderStore pairPriceToOrderStore = chainBaseManager
        .getMarketPairPriceToOrderStore();

    addOrder(TOKEN_ID_ONE, 100L, TOKEN_ID_TWO,
        300L, OWNER_ADDRESS_FIRST);//cancel this one

    //record account state
    AccountCapsule accountCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS_FIRST));
    long balanceBefore = accountCapsule.getBalance();

    MarketAccountOrderCapsule accountOrderCapsule = marketAccountStore
        .get(ByteArray.fromHexString(OWNER_ADDRESS_FIRST));
    ByteString orderId = accountOrderCapsule.getOrdersList().get(0);

    // cancel the second order
    cancelOrder(orderId);

    //check balance
    accountCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS_FIRST));

    Assert.assertEquals(
        balanceBefore - dbManager.getDynamicPropertiesStore().getMarketCancelFee(),
        +accountCapsule.getBalance());

    //check accountOrder
    accountOrderCapsule = marketAccountStore.get(ByteArray.fromHexString(OWNER_ADDRESS_FIRST));
    Assert.assertEquals(accountOrderCapsule.getCount(), 1);
    orderId = accountOrderCapsule.getOrdersList().get(0);

    //check order
    MarketOrderCapsule orderCapsule = orderStore.get(orderId.toByteArray());
    Assert.assertEquals(orderCapsule.getSellTokenQuantityRemain(), 0L);
    Assert.assertEquals(orderCapsule.getSellTokenQuantity(), 100L);
    Assert.assertEquals(orderCapsule.getBuyTokenQuantity(), 300L);
    Assert.assertEquals(orderCapsule.getSt(), State.CANCELED);

    //check pairToPrice
    byte[] marketPair = MarketUtils.createPairKey(TOKEN_ID_ONE.getBytes(), TOKEN_ID_TWO.getBytes());
    MarketPriceListCapsule priceListCapsule = pairToPriceStore.getUnchecked(marketPair);

    Assert.assertEquals(priceListCapsule, null);

    //check pairPriceToOrder
    byte[] pairPriceKey = MarketUtils.createPairPriceKey(
        TOKEN_ID_ONE.getBytes(), TOKEN_ID_TWO.getBytes(),
        100L, 300L);
    MarketOrderIdListCapsule orderIdListCapsule = pairPriceToOrderStore
        .getUnchecked(pairPriceKey);
    Assert.assertEquals(orderIdListCapsule, null);
  }
}