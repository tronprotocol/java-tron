package stest.tron.wallet.dailybuild.assetmarket;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.Return;
import org.tron.api.GrpcAPI.Return.response_code;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.protos.Protocol.MarketOrder;
import org.tron.protos.Protocol.MarketOrderList;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Result.code;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class MarketSellAsset002 {

  private static final long now = System.currentTimeMillis();
  private static final String name = "testAssetIssue003_" + Long.toString(now);
  private static final String shortname = "a";
  private final String foundationKey001 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final String foundationKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] foundationAddress001 = PublicMethed.getFinalAddress(foundationKey001);
  private final byte[] foundationAddress002 = PublicMethed.getFinalAddress(foundationKey002);
  String description = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetDescription");
  String url = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetUrl");

  ECKey ecKey001 = new ECKey(Utils.getRandom());
  byte[] testAddress001 = ecKey001.getAddress();
  String testKey001 = ByteArray.toHexString(ecKey001.getPrivKeyBytes());
  byte[] assetAccountId001;

  ECKey ecKey002 = new ECKey(Utils.getRandom());
  byte[] testAddress002 = ecKey002.getAddress();
  String testKey002 = ByteArray.toHexString(ecKey002.getPrivKeyBytes());
  byte[] assetAccountId002;

  long sellTokenQuantity = 100;
  long buyTokenQuantity = 50;

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(1);


  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    PublicMethed.printAddress(testKey001);
    PublicMethed.printAddress(testKey002);

    Assert.assertTrue(PublicMethed.sendcoin(testAddress001, 20000_000000L, foundationAddress001,
        foundationKey001, blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoin(testAddress002, 20000_000000L, foundationAddress001,
        foundationKey001, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Long start = System.currentTimeMillis() + 5000;
    Long end = System.currentTimeMillis() + 1000000000;
    Assert
        .assertTrue(PublicMethed.createAssetIssue(testAddress001, name, 10000_000000L, 1, 1, start,
            end, 1, description, url, 10000L, 10000L, 1L, 1L, testKey001, blockingStubFull));

    start = System.currentTimeMillis() + 5000;
    end = System.currentTimeMillis() + 1000000000;
    Assert
        .assertTrue(PublicMethed.createAssetIssue(testAddress002, name, 10000_000000L, 1, 1, start,
            end, 1, description, url, 10000L, 10000L, 1L, 1L, testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    assetAccountId001 =
        PublicMethed.queryAccount(testAddress001, blockingStubFull).getAssetIssuedID()
            .toByteArray();

    assetAccountId002 =
        PublicMethed.queryAccount(testAddress002, blockingStubFull).getAssetIssuedID()
            .toByteArray();
  }


  @Test(enabled = true, description = "create sellOrder and Match Order")
  void marketSellAssetTest001() {

    Map<String, Long> beforeAsset001 = PublicMethed
        .queryAccount(testAddress001, blockingStubFull).getAssetV2Map();
    Map<String, Long> beforeAsset002 = PublicMethed
        .queryAccount(testAddress002, blockingStubFull).getAssetV2Map();

    logger.info("beforeAsset001: " + beforeAsset001);
    logger.info("beforeAsset002: " + beforeAsset002);

    String txid = PublicMethed
        .marketSellAsset(testAddress001, testKey001, assetAccountId001, sellTokenQuantity,
            assetAccountId002, buyTokenQuantity, blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertNotNull(txid);

    Optional<Transaction> transaction = PublicMethed
        .getTransactionById(txid, blockingStubFull);
    Assert.assertEquals(transaction.get().getRet(0).getRet(), code.SUCESS);

    Optional<MarketOrderList> orderList = PublicMethed
        .getMarketOrderByAccount(testAddress001, blockingStubFull);
    Assert.assertTrue(orderList.get().getOrdersCount() > 0);

    byte[] orderId = orderList.get().getOrders(0).getOrderId().toByteArray();

    String txid2 = PublicMethed.marketSellAsset(testAddress002, testKey002, assetAccountId002,
        buyTokenQuantity, assetAccountId001, sellTokenQuantity, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertNotNull(txid2);

    transaction = PublicMethed
        .getTransactionById(txid2, blockingStubFull);
    Assert.assertEquals(transaction.get().getRet(0).getRet(), code.SUCESS);

    Map<String, Long> afterAsset001 = PublicMethed.queryAccount(testAddress001, blockingStubFull)
        .getAssetV2Map();
    Map<String, Long> afterAsset002 = PublicMethed.queryAccount(testAddress002, blockingStubFull)
        .getAssetV2Map();

    logger.info("afterAsset001: " + afterAsset001);
    logger.info("afterAsset002: " + afterAsset002);

    String assetId001 = ByteArray.toStr(assetAccountId001);
    String assetId002 = ByteArray.toStr(assetAccountId002);
    Assert.assertEquals((beforeAsset001.get(assetId001) - sellTokenQuantity),
        afterAsset001.get(assetId001).longValue());
    Assert.assertEquals((buyTokenQuantity), afterAsset001.get(assetId002).longValue());

    Assert.assertEquals(beforeAsset002.get(assetId002) - buyTokenQuantity,
        afterAsset002.get(assetId002).longValue());
    Assert.assertEquals(sellTokenQuantity, afterAsset002.get(assetId001).longValue());


  }

  @Test(enabled = true, description = "create sellOrder and Match Order twice")
  void marketSellAssetTest002() {
    Map<String, Long> beforeAsset001 = PublicMethed.queryAccount(testAddress001, blockingStubFull)
        .getAssetV2Map();
    Map<String, Long> beforeAsset002 = PublicMethed.queryAccount(testAddress002, blockingStubFull)
        .getAssetV2Map();

    logger.info("beforeAsset001: " + beforeAsset001);
    logger.info("beforeAsset002: " + beforeAsset002);

    String txid = PublicMethed.marketSellAsset(testAddress001, testKey001, assetAccountId001,
        sellTokenQuantity * 2,
        assetAccountId002, buyTokenQuantity * 2, blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertNotNull(txid);

    logger.info("beforeAsset001 :"
        + PublicMethed.queryAccount(testAddress001, blockingStubFull).getAssetV2Map());
    logger.info("beforeAsset002 :"
        + PublicMethed.queryAccount(testAddress002, blockingStubFull).getAssetV2Map());

    Optional<Transaction> transaction = PublicMethed
        .getTransactionById(txid, blockingStubFull);
    Assert.assertEquals(transaction.get().getRet(0).getRet(), code.SUCESS);

    Optional<MarketOrderList> orderList = PublicMethed
        .getMarketOrderByAccount(testAddress001, blockingStubFull);
    Assert.assertTrue(orderList.get().getOrdersCount() > 0);

    byte[] orderId;
    orderId = orderList.get().getOrders(0).getOrderId().toByteArray();

    String txid2 = PublicMethed.marketSellAsset(testAddress002, testKey002, assetAccountId002,
        buyTokenQuantity, assetAccountId001, sellTokenQuantity, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertNotNull(txid2);

    transaction = PublicMethed
        .getTransactionById(txid2, blockingStubFull);
    Assert.assertEquals(transaction.get().getRet(0).getRet(), code.SUCESS);

    // get order Message and RemainSellTokenQuantity
    MarketOrder order001 = PublicMethed
        .getMarketOrderById(orderId, blockingStubFull).get();
    Assert.assertEquals(order001.getSellTokenQuantityRemain(),sellTokenQuantity);

    Map<String, Long> afterAsset001 = PublicMethed.queryAccount(testAddress001, blockingStubFull)
        .getAssetV2Map();
    Map<String, Long> afterAsset002 = PublicMethed.queryAccount(testAddress002, blockingStubFull)
        .getAssetV2Map();

    logger.info("afterAsset001: " + afterAsset001);
    logger.info("afterAsset002: " + afterAsset002);

    String assetId001 = ByteArray.toStr(assetAccountId001);
    String assetId002 = ByteArray.toStr(assetAccountId002);
    Assert.assertEquals((beforeAsset001.get(assetId001) - sellTokenQuantity * 2),
        afterAsset001.get(assetId001).longValue());
    Assert.assertEquals((beforeAsset001.get(assetId002) + buyTokenQuantity),
        afterAsset001.get(assetId002).longValue());

    Assert.assertEquals(beforeAsset002.get(assetId002) - buyTokenQuantity,
        afterAsset002.get(assetId002).longValue());
    Assert.assertEquals(beforeAsset002.get(assetId001) + sellTokenQuantity,
        afterAsset002.get(assetId001).longValue());


    String txid3 = PublicMethed.marketSellAsset(testAddress002, testKey002, assetAccountId002,
        buyTokenQuantity, assetAccountId001, sellTokenQuantity, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertNotNull(txid3);

    transaction = PublicMethed
        .getTransactionById(txid3, blockingStubFull);
    Assert.assertEquals(transaction.get().getRet(0).getRet(), code.SUCESS);

    // get order Message and RemainSellTokenQuantity
    order001 = PublicMethed
        .getMarketOrderById(orderId, blockingStubFull).get();
    Assert.assertEquals(order001.getSellTokenQuantityRemain(),0);

    afterAsset001 = PublicMethed.queryAccount(testAddress001, blockingStubFull)
        .getAssetV2Map();
    afterAsset002 = PublicMethed.queryAccount(testAddress002, blockingStubFull)
        .getAssetV2Map();

    logger.info("afterAsset001: " + afterAsset001);
    logger.info("afterAsset002: " + afterAsset002);

    Assert.assertEquals((beforeAsset001.get(assetId001) - sellTokenQuantity * 2),
        afterAsset001.get(assetId001).longValue());
    Assert.assertEquals((beforeAsset001.get(assetId002) + buyTokenQuantity * 2),
        afterAsset001.get(assetId002).longValue());

    Assert.assertEquals(beforeAsset002.get(assetId002) - buyTokenQuantity * 2,
        afterAsset002.get(assetId002).longValue());
    Assert.assertEquals(beforeAsset002.get(assetId001) + sellTokenQuantity * 2,
        afterAsset002.get(assetId001).longValue());



  }

  @Test(enabled = true, description = "create sellOrder and not Match Order")
  void marketSellAssetTest003() {

    Map<String, Long> beforeAsset001 = PublicMethed
        .queryAccount(testAddress001, blockingStubFull).getAssetV2Map();
    Map<String, Long> beforeAsset002 = PublicMethed
        .queryAccount(testAddress002, blockingStubFull).getAssetV2Map();

    logger.info("beforeAsset001: " + beforeAsset001);
    logger.info("beforeAsset002: " + beforeAsset002);

    String txid = PublicMethed
        .marketSellAsset(testAddress001, testKey001, assetAccountId001, sellTokenQuantity,
            assetAccountId002, buyTokenQuantity, blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertNotNull(txid);

    Optional<Transaction> transaction = PublicMethed
        .getTransactionById(txid, blockingStubFull);
    Assert.assertEquals(transaction.get().getRet(0).getRet(), code.SUCESS);

    Optional<MarketOrderList> orderList = PublicMethed
        .getMarketOrderByAccount(testAddress001, blockingStubFull);
    Assert.assertTrue(orderList.get().getOrdersCount() > 0);

    byte[] orderId = orderList.get().getOrders(0).getOrderId().toByteArray();

    String txid2 = PublicMethed.marketSellAsset(testAddress002, testKey002, assetAccountId002,
        buyTokenQuantity * 2, assetAccountId001, sellTokenQuantity * 5, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertNotNull(txid2);

    transaction = PublicMethed
        .getTransactionById(txid2, blockingStubFull);
    Assert.assertEquals(transaction.get().getRet(0).getRet(), code.SUCESS);

    Map<String, Long> afterAsset001 = PublicMethed.queryAccount(testAddress001, blockingStubFull)
        .getAssetV2Map();
    Map<String, Long> afterAsset002 = PublicMethed.queryAccount(testAddress002, blockingStubFull)
        .getAssetV2Map();

    logger.info("afterAsset001: " + afterAsset001);
    logger.info("afterAsset002: " + afterAsset002);

    String assetId001 = ByteArray.toStr(assetAccountId001);
    String assetId002 = ByteArray.toStr(assetAccountId002);
    Assert.assertEquals((beforeAsset001.get(assetId001) - sellTokenQuantity),
        afterAsset001.get(assetId001).longValue());
    Assert.assertEquals((beforeAsset001.get(assetId002).longValue()),
        afterAsset001.get(assetId002).longValue());

    Assert.assertEquals(beforeAsset002.get(assetId002) - buyTokenQuantity * 2,
        afterAsset002.get(assetId002).longValue());
    Assert.assertEquals(beforeAsset002.get(assetId001).longValue(),
        afterAsset002.get(assetId001).longValue());
  }

  @Test(enabled = true, description = "CancelOrder")
  void marketSellAssetTest004() {

    Map<String, Long> beforeAsset001 = PublicMethed
        .queryAccount(testAddress001, blockingStubFull).getAssetV2Map();

    logger.info("beforeAsset001: " + beforeAsset001);

    Optional<MarketOrderList> orderList = PublicMethed
        .getMarketOrderByAccount(testAddress001, blockingStubFull);
    Assert.assertTrue(orderList.get().getOrdersCount() > 0);

    Long sellTokenQuantity001;
    byte[] tokenId;
    byte[] orderId001;

    orderId001 = orderList.get().getOrders(0).getOrderId().toByteArray();
    tokenId = orderList.get().getOrders(0).getSellTokenId().toByteArray();
    sellTokenQuantity001 = orderList.get().getOrders(0).getSellTokenQuantityRemain();

    String txid = PublicMethed.marketCancelOrder(testAddress001,testKey001,orderId001,
        blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertNotNull(txid);

    Optional<Transaction> transaction = PublicMethed
        .getTransactionById(txid, blockingStubFull);
    Assert.assertEquals(transaction.get().getRet(0).getRet(), code.SUCESS);

    Map<String, Long> afterAsset001 = PublicMethed.queryAccount(testAddress001, blockingStubFull)
        .getAssetV2Map();

    logger.info("afterAsset001: " + afterAsset001);

    String assetId001 = ByteArray.toStr(tokenId);

    Assert.assertEquals(beforeAsset001.get(assetId001) + sellTokenQuantity001,
        afterAsset001.get(assetId001).longValue());

    Return response = PublicMethed
        .marketCancelOrderGetResposne(testAddress001, testKey001, orderId001,
            blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertNotNull(response);
    Assert.assertEquals(response.getCode(), response_code.CONTRACT_VALIDATE_ERROR);
    Assert.assertEquals(ByteArray.toStr(response.getMessage().toByteArray()),
        "contract validate error : Order is not active!");


  }

}
