package stest.tron.wallet.dailybuild.assetmarket;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.protos.Protocol.MarketOrderList;
import org.tron.protos.Protocol.Transaction;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j

public class MarketSellAsset003 {

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

  byte [] trx = ByteArray.fromString("_");


  ECKey ecKey001 = new ECKey(Utils.getRandom());
  byte[] testAddress001 = ecKey001.getAddress();
  String testKey001 = ByteArray.toHexString(ecKey001.getPrivKeyBytes());
  byte[] assetAccountId001;

  ECKey ecKey002 = new ECKey(Utils.getRandom());
  byte[] testAddress002 = ecKey002.getAddress();
  String testKey002 = ByteArray.toHexString(ecKey002.getPrivKeyBytes());
  byte[] assetAccountId002;


  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(1);

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
    Assert.assertTrue(PublicMethed.createAssetIssue(testAddress001, name, 10000_000000L,
        1, 1, start, end, 1, description, url, 10000L,
        10000L, 1L, 1L, testKey001, blockingStubFull));

    start = System.currentTimeMillis() + 5000;
    end = System.currentTimeMillis() + 1000000000;
    Assert.assertTrue(PublicMethed.createAssetIssue(testAddress002, name, 10000_000000L,
        1, 1, start, end, 1, description, url, 10000L,
        10000L, 1L, 1L, testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    assetAccountId001 = PublicMethed.queryAccount(testAddress001, blockingStubFull)
        .getAssetIssuedID().toByteArray();

    assetAccountId002 = PublicMethed.queryAccount(testAddress002, blockingStubFull)
        .getAssetIssuedID().toByteArray();
  }


  @Test(enabled = true, description = "CancelOrder")
  void marketCancelAssetTest001() {

    String txid = PublicMethed.marketSellAsset(testAddress001, testKey001, assetAccountId001, 100,
        trx, 50, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Transaction> transaction = PublicMethed
        .getTransactionById(txid, blockingStubFull);
    logger.info("transaction: " + transaction);
    Assert.assertEquals(transaction.get().getRet(0).getRet().toString(), "SUCESS");

    Optional<MarketOrderList> orderList = PublicMethed
        .getMarketOrderByAccount(testAddress001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(orderList.get().getOrdersCount() > 0);
    byte[] orderId = orderList.get().getOrders(0).getOrderId().toByteArray();
    txid = PublicMethed.marketCancelOrder(testAddress001, testKey001, orderId, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    transaction = PublicMethed
        .getTransactionById(txid, blockingStubFull);
    Assert.assertEquals(transaction.get().getRet(0).getRet().toString(), "SUCESS");

    orderList = PublicMethed
        .getMarketOrderByAccount(testAddress001, blockingStubFull);
    Assert.assertTrue(orderList.get().getOrdersCount() == 0);

  }

  @Test(enabled = true, description = "Cancel a cancelled order ")
  void marketCancelAssetTest002() {

    String txid = PublicMethed.marketSellAsset(testAddress001, testKey001, assetAccountId001, 100,
        trx, 50, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Transaction> transaction = PublicMethed
        .getTransactionById(txid, blockingStubFull);
    logger.info("transaction: " + transaction);
    Assert.assertEquals(transaction.get().getRet(0).getRet().toString(), "SUCESS");

    Optional<MarketOrderList> orderList = PublicMethed
        .getMarketOrderByAccount(testAddress001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(orderList.get().getOrdersCount() > 0);
    byte[] orderId = orderList.get().getOrders(0).getOrderId().toByteArray();
    txid = PublicMethed.marketCancelOrder(testAddress001, testKey001, orderId, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    transaction = PublicMethed
        .getTransactionById(txid, blockingStubFull);
    Assert.assertEquals(transaction.get().getRet(0).getRet().toString(), "SUCESS");
    orderList = PublicMethed
        .getMarketOrderByAccount(testAddress001, blockingStubFull);
    Assert.assertTrue(orderList.get().getOrdersCount() == 0);

    Assert.assertEquals(PublicMethed.marketCancelOrder(testAddress001, testKey001, orderId,
        blockingStubFull).toLowerCase(),
        "contract validate error : Order is not active!".toLowerCase());


  }

}
