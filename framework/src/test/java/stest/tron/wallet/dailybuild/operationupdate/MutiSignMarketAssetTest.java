package stest.tron.wallet.dailybuild.operationupdate;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.MarketOrder;
import org.tron.protos.Protocol.MarketOrderList;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.PublicMethedForMutiSign;


@Slf4j
public class MutiSignMarketAssetTest {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);

  ECKey ecKey0 = new ECKey(Utils.getRandom());
  byte[] testAddress001 = ecKey0.getAddress();
  String testKey001 = ByteArray.toHexString(ecKey0.getPrivKeyBytes());

  String[] permissionKeyString = new String[2];
  String[] ownerKeyString = new String[2];
  String accountPermissionJson = "";
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] manager1Address = ecKey1.getAddress();
  String manager1Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] manager2Address = ecKey2.getAddress();
  String manager2Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  private ManagedChannel channelFull = null;
  private ManagedChannel channelSolidity = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  /**
   * constructor.
   */

  @BeforeClass
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext(true).build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    Assert.assertTrue(PublicMethed
        .sendcoin(testAddress001, 20000_000000L, fromAddress, testKey002,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    long start = System.currentTimeMillis() + 2000;
    long end = System.currentTimeMillis() + 1000000000;

    Assert.assertTrue(PublicMethed.createAssetIssue(testAddress001,
        "MarketAsset" + start,
        100_000000L,
        1,1,
        start, end,1,"MarketAsset","MarketAsset.com",10000L,10000L,1L, 1L,testKey001,
        blockingStubFull));
    Long balanceBefore = PublicMethed.queryAccount(testAddress001, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);

    permissionKeyString[0] = manager1Key;
    permissionKeyString[1] = manager2Key;
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    ownerKeyString[0] = testKey001;
    ownerKeyString[1] = testKey002;

    // operation include MarketSellAssetContract(52)
    Integer[] ints = {0, 1, 2, 3, 4, 5, 6, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 30, 31,
        32, 33, 41, 42, 43, 44, 45, 48, 49, 52, 53};
    String operations = PublicMethedForMutiSign.getOperations(ints);

    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":2,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(testKey001) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(testKey002)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":2,"
            + "\"operations\":\"" + operations + "\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(manager1Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(manager2Key) + "\",\"weight\":1}"
            + "]}]}";
    logger.info(accountPermissionJson);
    PublicMethedForMutiSign
        .accountPermissionUpdate(accountPermissionJson, testAddress001, testKey001,
            blockingStubFull, ownerKeyString);

    PublicMethed.waitProduceNextBlock(blockingStubFull);

  }

  @Test(enabled = true, description = "MutiSignForMarketSellAsset with active_permissions")
  public void testMutiSignForMarketSellAsset001() {
    //  MarketSellAsset
    ByteString assetAccountId = PublicMethed
        .queryAccount(testAddress001, blockingStubFull).getAssetIssuedID();

    int marketOrderCountBefore = PublicMethed
        .getMarketOrderByAccount(testAddress001, blockingStubFull).get().getOrdersCount();

    Assert.assertTrue(PublicMethedForMutiSign
        .marketSellAsset(testAddress001,assetAccountId.toByteArray(),10,"_".getBytes(),10,2,
            permissionKeyString,blockingStubFull));

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    MarketOrderList marketOrder = PublicMethed
        .getMarketOrderByAccount(testAddress001, blockingStubFull).get();
    Assert.assertEquals(marketOrderCountBefore + 1, marketOrder.getOrdersCount());
  }

  @Test(enabled = true, description = "MutiSignForMarketSellAsset with owner_permission")
  public void testMutiSignForMarketSellAsset002() {
    //  MarketSellAsset
    ByteString assetAccountId = PublicMethed
        .queryAccount(testAddress001, blockingStubFull).getAssetIssuedID();

    int marketOrderCountBefore = PublicMethed
        .getMarketOrderByAccount(testAddress001, blockingStubFull).get().getOrdersCount();


    Assert.assertTrue(PublicMethedForMutiSign
        .marketSellAsset(testAddress001,assetAccountId.toByteArray(),10,"_".getBytes(),10,0,
            ownerKeyString,blockingStubFull));

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    MarketOrderList marketOrder = PublicMethed
        .getMarketOrderByAccount(testAddress001, blockingStubFull).get();
    Assert.assertEquals(marketOrderCountBefore + 1, marketOrder.getOrdersCount());
  }


  @Test(enabled = true, dependsOnMethods = "testMutiSignForMarketSellAsset001",
      description = "MutiSignForMarketOrderCancel with active_permissions")
  public void testMutiSignForMarketOrderCancel001() {
    // MarketOrderCancel

    ByteString orderId = PublicMethed
        .getMarketOrderByAccount(testAddress001, blockingStubFull).get().getOrders(0).getOrderId();
    int marketOrderCountBefore = PublicMethed
        .getMarketOrderByAccount(testAddress001, blockingStubFull).get().getOrdersCount();


    Assert.assertTrue(PublicMethedForMutiSign.marketCancelOrder(testAddress001,
        orderId.toByteArray(),2,permissionKeyString,blockingStubFull));

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Assert.assertEquals(marketOrderCountBefore - 1, PublicMethed
        .getMarketOrderByAccount(testAddress001, blockingStubFull).get().getOrdersCount());

  }

  @Test(enabled = true, dependsOnMethods = "testMutiSignForMarketSellAsset002",
      description = "MutiSignForMarketOrderCancel with owner_permission")
  public void testMutiSignForMarketOrderCancel002() {
    // MarketOrderCancel

    ByteString orderId = PublicMethed
        .getMarketOrderByAccount(testAddress001, blockingStubFull).get().getOrders(0).getOrderId();
    int marketOrderCountBefore = PublicMethed
        .getMarketOrderByAccount(testAddress001, blockingStubFull).get().getOrdersCount();


    Assert.assertTrue(PublicMethedForMutiSign.marketCancelOrder(testAddress001,
        orderId.toByteArray(),0,ownerKeyString,blockingStubFull));

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Assert.assertEquals(marketOrderCountBefore - 1, PublicMethed
        .getMarketOrderByAccount(testAddress001, blockingStubFull).get().getOrdersCount());

  }



  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelSolidity != null) {
      channelSolidity.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


