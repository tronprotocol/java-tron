package stest.tron.wallet.dailybuild.assetmarket;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.protos.Protocol.MarketOrder;
import org.tron.protos.Protocol.MarketOrderList;
import org.tron.protos.Protocol.MarketOrderPairList;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Result.code;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j


public class MarketSellAsset006 {

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

  private ManagedChannel channelSolidity = null;

  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;

  public ManagedChannel channelPbft = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubRealSolidity = null;
  public WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubPbft = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(1);
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);
  private String realSoliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(1);
  private String soliInPbft = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(2);


  /**
   * constructor.
   */

  @BeforeClass
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext(true)
        .build();
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);

    channelPbft = ManagedChannelBuilder.forTarget(soliInPbft)
        .usePlaintext(true)
        .build();
    blockingStubPbft = WalletSolidityGrpc.newBlockingStub(channelPbft);
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

    assetAccountId001 = PublicMethed.queryAccount(testAddress001, blockingStubFull)
        .getAssetIssuedID().toByteArray();

    assetAccountId002 = PublicMethed.queryAccount(testAddress002, blockingStubFull)
        .getAssetIssuedID().toByteArray();
  }


  @Test(enabled = true, description = "create sellOrder")
  void marketSellAssetTest001() {

    String txid = PublicMethed.marketSellAsset(testAddress001, testKey001, assetAccountId001,
        sellTokenQuantity, assetAccountId002, buyTokenQuantity, blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertNotNull(txid);

    Optional<Transaction> transaction = PublicMethed
        .getTransactionById(txid, blockingStubFull);
    Assert.assertEquals(transaction.get().getRet(0).getRet(), code.SUCESS);

    Optional<MarketOrderList> orderList = PublicMethed
        .getMarketOrderByAccount(testAddress001, blockingStubFull);
    Assert.assertTrue(orderList.get().getOrdersCount() > 0);

    byte[] orderId = orderList.get().getOrders(0).getOrderId().toByteArray();

    MarketOrder order = PublicMethed
        .getMarketOrderById(orderId, blockingStubFull).get();

    Assert.assertEquals(order.getOrderId().toByteArray(), orderId);
    Assert.assertEquals(order.getOwnerAddress().toByteArray(), testAddress001);
    Assert.assertEquals(order.getSellTokenId().toByteArray(), assetAccountId001);
    Assert.assertEquals(order.getSellTokenQuantity(), sellTokenQuantity);
    Assert.assertEquals(order.getBuyTokenId().toByteArray(), assetAccountId002);
    Assert.assertEquals(order.getBuyTokenQuantity(), buyTokenQuantity);

  }

  @Test(enabled = false, description = "getMarketPairList from solidity and pbft")
  void marketSellAssetTest002() {
    Optional<MarketOrderPairList> pairList = PublicMethed
        .getMarketPairList(blockingStubFull);

    PublicMethed.waitSolidityNodeSynFullNodeData(blockingStubFull,blockingStubSolidity);

    Optional<MarketOrderPairList> pairList2 = PublicMethed
        .getMarketPairListSolidity(blockingStubSolidity);


    Optional<MarketOrderPairList> pairList3 = PublicMethed
        .getMarketPairListSolidity(blockingStubPbft);

    Assert.assertEquals(pairList,pairList2);
    Assert.assertEquals(pairList,pairList3);



  }

  @Test(enabled = true, description = "getMarketOrderListByPair from solidity and pbft")
  void marketSellAssetTest003() {
    Optional<MarketOrderList> orderList = PublicMethed
        .getMarketOrderListByPair(assetAccountId001,assetAccountId002,blockingStubFull);

    PublicMethed.waitSolidityNodeSynFullNodeData(blockingStubFull,blockingStubSolidity);

    Optional<MarketOrderList> orderList2 = PublicMethed
        .getMarketOrderListByPairSolidity(assetAccountId001,assetAccountId002,blockingStubSolidity);


    Optional<MarketOrderList> orderList3 = PublicMethed
        .getMarketOrderListByPairSolidity(assetAccountId001,assetAccountId002,blockingStubPbft);

    System.out.println(orderList3);

    Assert.assertEquals(orderList,orderList2);
    Assert.assertEquals(orderList,orderList3);

  }


  @Test(enabled = true, description = "GetMarketOrderById from solidity and pbft")
  void marketSellAssetTest004() {
    Optional<MarketOrderList> orderList = PublicMethed
        .getMarketOrderListByPair(assetAccountId001,assetAccountId002,blockingStubFull);

    PublicMethed.waitSolidityNodeSynFullNodeData(blockingStubFull,blockingStubSolidity);

    Optional<MarketOrderList> orderList2 = PublicMethed
        .getMarketOrderListByPairSolidity(assetAccountId001,assetAccountId002,blockingStubSolidity);


    Optional<MarketOrderList> orderList3 = PublicMethed
        .getMarketOrderListByPairSolidity(assetAccountId001,assetAccountId002,blockingStubPbft);

    System.out.println(orderList3);

    Assert.assertEquals(orderList,orderList2);
    Assert.assertEquals(orderList,orderList3);

  }


  @Test(enabled = true, description = "GetMarketOrderByAccount from solidity and pbft")
  void marketSellAssetTest005() {

    Optional<MarketOrderList> orderList = PublicMethed
        .getMarketOrderByAccount(testAddress001, blockingStubFull);
    Assert.assertTrue(orderList.get().getOrdersCount() > 0);

    PublicMethed.waitSolidityNodeSynFullNodeData(blockingStubFull,blockingStubSolidity);

    Optional<MarketOrderList> orderList2 = PublicMethed
        .getMarketOrderByAccountSolidity(testAddress001, blockingStubSolidity);
    Assert.assertTrue(orderList2.get().getOrdersCount() > 0);


    Optional<MarketOrderList> orderList3 = PublicMethed
        .getMarketOrderByAccountSolidity(testAddress001, blockingStubPbft);
    Assert.assertTrue(orderList3.get().getOrdersCount() > 0);
    Assert.assertEquals(orderList,orderList2);
    Assert.assertEquals(orderList,orderList3);

  }

  @Test(enabled = true, description = "GetMarketOrderById from solidity and pbft")
  void marketSellAssetTest006() {

    Optional<MarketOrderList> orderList = PublicMethed
        .getMarketOrderByAccount(testAddress001, blockingStubFull);

    byte[] orderId = orderList.get().getOrders(0).getOrderId().toByteArray();

    MarketOrder order = PublicMethed
        .getMarketOrderById(orderId, blockingStubFull).get();

    Assert.assertEquals(order.getOrderId().toByteArray(), orderId);
    Assert.assertEquals(order.getOwnerAddress().toByteArray(), testAddress001);
    Assert.assertEquals(order.getSellTokenId().toByteArray(), assetAccountId001);
    Assert.assertEquals(order.getSellTokenQuantity(), sellTokenQuantity);
    Assert.assertEquals(order.getBuyTokenId().toByteArray(), assetAccountId002);
    Assert.assertEquals(order.getBuyTokenQuantity(), buyTokenQuantity);

    MarketOrder order2 = PublicMethed
        .getMarketOrderByIdSolidity(orderId, blockingStubSolidity).get();

    Assert.assertEquals(order2.getOrderId().toByteArray(), orderId);
    Assert.assertEquals(order2.getOwnerAddress().toByteArray(), testAddress001);
    Assert.assertEquals(order2.getSellTokenId().toByteArray(), assetAccountId001);
    Assert.assertEquals(order2.getSellTokenQuantity(), sellTokenQuantity);
    Assert.assertEquals(order2.getBuyTokenId().toByteArray(), assetAccountId002);
    Assert.assertEquals(order2.getBuyTokenQuantity(), buyTokenQuantity);

    MarketOrder order3 = PublicMethed
        .getMarketOrderByIdSolidity(orderId, blockingStubPbft).get();

    Assert.assertEquals(order3.getOrderId().toByteArray(), orderId);
    Assert.assertEquals(order3.getOwnerAddress().toByteArray(), testAddress001);
    Assert.assertEquals(order3.getSellTokenId().toByteArray(), assetAccountId001);
    Assert.assertEquals(order3.getSellTokenQuantity(), sellTokenQuantity);
    Assert.assertEquals(order3.getBuyTokenId().toByteArray(), assetAccountId002);
    Assert.assertEquals(order3.getBuyTokenQuantity(), buyTokenQuantity);


  }





}
