package stest.tron.wallet.assetissue;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.AccountNetMessage;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Account;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.WalletClient;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class WalletTestAssetIssue015 {
  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);


  private static final long now = System.currentTimeMillis();
  private static String name = "AssetIssue015_" + Long.toString(now);
  private static final long totalSupply = now;
  private static final long sendAmount = 10000000000L;
  private static final long netCostMeasure = 200L;

  Long freeAssetNetLimit = 30000L;
  Long publicFreeAssetNetLimit = 30000L;
  String description = "for case assetissue015";
  String url = "https://stest.assetissue015.url";


  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);



  //get account
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] asset015Address = ecKey1.getAddress();
  String testKeyForAssetIssue015 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());


  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] transferAssetAddress = ecKey2.getAddress();
  String transferAssetCreateKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

  ECKey ecKey3 = new ECKey(Utils.getRandom());
  byte[] newAddress = ecKey3.getAddress();
  String testKeyForNewAddress = ByteArray.toHexString(ecKey3.getPrivKeyBytes());

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  @BeforeClass(enabled = true)
  public void beforeClass() {
    logger.info(testKeyForAssetIssue015);
    logger.info(transferAssetCreateKey);
    logger.info(testKeyForNewAddress);

    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    //Sendcoin to this account
    ByteString addressBS1 = ByteString.copyFrom(asset015Address);
    Account request1 = Account.newBuilder().setAddress(addressBS1).build();
    GrpcAPI.AssetIssueList assetIssueList1 = blockingStubFull
        .getAssetIssueByAccount(request1);
    Optional<GrpcAPI.AssetIssueList> queryAssetByAccount = Optional.ofNullable(assetIssueList1);
    if (queryAssetByAccount.get().getAssetIssueCount() == 0) {
      //Assert.assertTrue(PublicMethed.freezeBalance(fromAddress,10000000, 3, testKey002,
      //    blockingStubFull));
      Assert.assertTrue(PublicMethed
          .sendcoin(asset015Address, sendAmount, fromAddress, testKey002, blockingStubFull));
      Long start = System.currentTimeMillis() + 2000;
      Long end = System.currentTimeMillis() + 1000000000;
      Assert.assertTrue(PublicMethed
          .createAssetIssue(asset015Address, name, totalSupply, 1, 1, start, end, 1, description,
              url, freeAssetNetLimit, publicFreeAssetNetLimit, 1L, 1L, testKeyForAssetIssue015,
              blockingStubFull));
    } else {
      logger.info("This account already create an assetisue");
      Optional<GrpcAPI.AssetIssueList> queryAssetByAccount1 = Optional.ofNullable(assetIssueList1);
      name = ByteArray.toStr(queryAssetByAccount1.get().getAssetIssue(0).getName().toByteArray());
    }
  }

  @Test(enabled = true)
  public void atestWhenCreatorHasNoEnoughBandwidthUseTransferNet() {
    //Transfer asset to an account.
    Assert.assertTrue(PublicMethed
        .transferAsset(transferAssetAddress, name.getBytes(), 10000000L, asset015Address,
            testKeyForAssetIssue015, blockingStubFull));

    //Before use transfer net, query the net used from creator and transfer.
    AccountNetMessage assetCreatorNet = PublicMethed
        .getAccountNet(asset015Address,blockingStubFull);
    AccountNetMessage assetTransferNet = PublicMethed
        .getAccountNet(transferAssetAddress,blockingStubFull);
    Long creatorBeforeFreeNetUsed = assetCreatorNet.getFreeNetUsed();
    Long transferBeforeFreeNetUsed = assetTransferNet.getFreeNetUsed();
    logger.info(Long.toString(creatorBeforeFreeNetUsed));
    logger.info(Long.toString(transferBeforeFreeNetUsed));

    //Transfer send some asset issue to default account, to test if this
    // transaction use the transaction free net.
    Assert.assertTrue(PublicMethed.transferAsset(toAddress,name.getBytes(),1L,
        transferAssetAddress,transferAssetCreateKey,blockingStubFull));
    assetCreatorNet = PublicMethed
        .getAccountNet(asset015Address,blockingStubFull);
    assetTransferNet = PublicMethed
        .getAccountNet(transferAssetAddress,blockingStubFull);
    Long creatorAfterFreeNetUsed = assetCreatorNet.getFreeNetUsed();
    Long transferAfterFreeNetUsed = assetTransferNet.getFreeNetUsed();
    logger.info(Long.toString(creatorAfterFreeNetUsed));
    logger.info(Long.toString(transferAfterFreeNetUsed));


    Assert.assertTrue(creatorAfterFreeNetUsed - creatorBeforeFreeNetUsed < netCostMeasure);
    Assert.assertTrue(transferAfterFreeNetUsed - transferBeforeFreeNetUsed > netCostMeasure);
  }

  @Test(enabled = true)
  public void btestWhenTransferHasNoEnoughBandwidthUseBalance() {
    Boolean ret = true;
    while (ret) {
      ret = PublicMethed.transferAsset(toAddress,name.getBytes(),1L,
          transferAssetAddress,transferAssetCreateKey,blockingStubFull);
    }
    AccountNetMessage assetTransferNet = PublicMethed
        .getAccountNet(transferAssetAddress,blockingStubFull);
    logger.info(Long.toString(assetTransferNet.getFreeNetUsed()));
    Assert.assertTrue(assetTransferNet.getFreeNetUsed() >= 4700);

    Assert.assertTrue(PublicMethed.sendcoin(transferAssetAddress,
        20000000,fromAddress,testKey002,blockingStubFull));

    Account transferAccount = PublicMethed.queryAccount(transferAssetCreateKey,blockingStubFull);
    Long beforeBalance = transferAccount.getBalance();
    logger.info(Long.toString(beforeBalance));

    Assert.assertTrue(PublicMethed.transferAsset(toAddress,name.getBytes(),1L,
        transferAssetAddress,transferAssetCreateKey,blockingStubFull));

    transferAccount = PublicMethed.queryAccount(transferAssetCreateKey,blockingStubFull);
    Long afterBalance = transferAccount.getBalance();
    logger.info(Long.toString(afterBalance));

    Assert.assertTrue(beforeBalance - afterBalance > 2000);
  }

  @Test(enabled = true)
  public void ctestWhenFreezeBalanceUseNet() {
    Assert.assertTrue(PublicMethed.freezeBalance(transferAssetAddress,5000000,
        3,transferAssetCreateKey,blockingStubFull));
    AccountNetMessage assetTransferNet = PublicMethed
        .getAccountNet(transferAssetAddress,blockingStubFull);
    Account transferAccount = PublicMethed.queryAccount(transferAssetCreateKey,blockingStubFull);

    final Long transferNetUsedBefore = assetTransferNet.getNetUsed();
    final Long transferBalanceBefore = transferAccount.getBalance();
    logger.info("before  " + Long.toString(transferBalanceBefore));

    Assert.assertTrue(PublicMethed.transferAsset(toAddress,name.getBytes(),1L,
        transferAssetAddress,transferAssetCreateKey,blockingStubFull));

    assetTransferNet = PublicMethed
        .getAccountNet(transferAssetAddress,blockingStubFull);
    transferAccount = PublicMethed.queryAccount(transferAssetCreateKey,blockingStubFull);
    final Long transferNetUsedAfter = assetTransferNet.getNetUsed();
    final Long transferBalanceAfter = transferAccount.getBalance();
    logger.info("after " + Long.toString(transferBalanceAfter));


    Assert.assertTrue(transferBalanceAfter - transferBalanceBefore == 0);
    Assert.assertTrue(transferNetUsedAfter - transferNetUsedBefore > 200);


  }


  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


