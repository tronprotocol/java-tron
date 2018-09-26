package stest.tron.wallet.assetissue;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.math.BigInteger;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.spongycastle.util.encoders.Hex;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.AccountNetMessage;
import org.tron.api.GrpcAPI.NumberMessage;
import org.tron.api.GrpcAPI.Return;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Contract;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Transaction;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.TransactionUtils;

@Slf4j
public class WalletTestAssetIssue007 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);

  private static final long now = System.currentTimeMillis();
  private static String name = "AssetIssue007_" + Long.toString(now);
  private static final long totalSupply = now;
  private static final long sendAmount = 10000000000L;
  private static final long netCostMeasure = 200L;
  private static final Integer trxNum = 1;
  private static final Integer icoNum = 1;

  Long freeAssetNetLimit = 10000L;
  Long publicFreeAssetNetLimit = 10000L;
  String description = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetDescription");
  String url = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetUrl");

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);

  //get account
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] asset007Address = ecKey1.getAddress();
  String testKeyForAssetIssue007 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());


  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] participateAssetAddress = ecKey2.getAddress();
  String participateAssetCreateKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(testKeyForAssetIssue007);
    PublicMethed.printAddress(participateAssetCreateKey);

    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
  }

  @Test(enabled = true)
  public void testParticipateAssetIssueUseParticipaterBandwidth() {
    Assert.assertTrue(PublicMethed
        .sendcoin(asset007Address, sendAmount, fromAddress, testKey002, blockingStubFull));
    //Assert.assertTrue(PublicMethed
    //    .freezeBalance(asset007Address, 100000000L, 3, testKeyForAssetIssue007,
    //       blockingStubFull));
    Long start = System.currentTimeMillis() + 2000;
    Long end = System.currentTimeMillis() + 1000000000;
    Assert.assertTrue(PublicMethed
        .createAssetIssue(asset007Address, name, totalSupply, trxNum, icoNum, start, end, 1,
            description, url, freeAssetNetLimit, publicFreeAssetNetLimit, 1L, 1L,
            testKeyForAssetIssue007, blockingStubFull));

    logger.info(name);
    Assert.assertTrue(PublicMethed.waitProduceNextBlock(blockingStubFull));
    //Assert.assertTrue(PublicMethed.waitProduceNextBlock(blockingStubFull));
    //When no balance, participate an asset issue
    Assert.assertFalse(PublicMethed.participateAssetIssue(asset007Address, name.getBytes(),
        1L, participateAssetAddress, participateAssetCreateKey,blockingStubFull));

    ByteString addressBs = ByteString.copyFrom(asset007Address);
    Account request = Account.newBuilder().setAddress(addressBs).build();
    AccountNetMessage asset007NetMessage = blockingStubFull.getAccountNet(request);
    final Long asset007BeforeFreeNetUsed = asset007NetMessage.getFreeNetUsed();

    //SendCoin to participate account.
    Assert.assertTrue(PublicMethed.sendcoin(participateAssetAddress,10000000L,
        fromAddress,testKey002,blockingStubFull));
    addressBs = ByteString.copyFrom(participateAssetAddress);
    request = Account.newBuilder().setAddress(addressBs).build();
    AccountNetMessage participateAccountNetMessage = blockingStubFull.getAccountNet(request);
    final Long participateAccountBeforeNetUsed = participateAccountNetMessage.getFreeNetUsed();
    Assert.assertTrue(participateAccountBeforeNetUsed == 0);

    //Participate an assetIssue, then query the net information.
    ByteString assetNameBs = ByteString.copyFrom(name.getBytes());
    GrpcAPI.BytesMessage request1 = GrpcAPI.BytesMessage.newBuilder().setValue(assetNameBs).build();
    Contract.AssetIssueContract assetIssueByName = blockingStubFull.getAssetIssueByName(request1);
    logger.info(Long.toString(assetIssueByName.getStartTime()));
    logger.info(Long.toString(System.currentTimeMillis()));
    
    Assert.assertTrue(PublicMethed.participateAssetIssue(asset007Address,name.getBytes(),
        1L,participateAssetAddress,participateAssetCreateKey,blockingStubFull));

    addressBs = ByteString.copyFrom(asset007Address);
    request = Account.newBuilder().setAddress(addressBs).build();
    asset007NetMessage = blockingStubFull.getAccountNet(request);
    final Long asset007AfterFreeNetUsed = asset007NetMessage.getFreeNetUsed();

    addressBs = ByteString.copyFrom(participateAssetAddress);
    request = Account.newBuilder().setAddress(addressBs).build();
    participateAccountNetMessage = blockingStubFull.getAccountNet(request);
    final Long participateAccountAfterNetUsed = participateAccountNetMessage.getFreeNetUsed();

    logger.info(Long.toString(asset007BeforeFreeNetUsed));
    logger.info(Long.toString(asset007AfterFreeNetUsed));
    logger.info(Long.toString(participateAccountBeforeNetUsed));
    logger.info(Long.toString(participateAccountAfterNetUsed));
    Assert.assertTrue(asset007AfterFreeNetUsed <= asset007BeforeFreeNetUsed);
    Assert.assertTrue(participateAccountAfterNetUsed - participateAccountBeforeNetUsed > 150);



    Assert.assertTrue(PublicMethed.participateAssetIssue(asset007Address,name.getBytes(),
        1L,participateAssetAddress,participateAssetCreateKey,blockingStubFull));
    Assert.assertTrue(PublicMethed.participateAssetIssue(asset007Address,name.getBytes(),
        1L,participateAssetAddress,participateAssetCreateKey,blockingStubFull));

    Account participateInfo = PublicMethed.queryAccount(participateAssetCreateKey,blockingStubFull);
    final Long beforeBalance = participateInfo.getBalance();
    Assert.assertTrue(PublicMethed.participateAssetIssue(asset007Address,name.getBytes(),
        1L,participateAssetAddress,participateAssetCreateKey,blockingStubFull));
    participateInfo = PublicMethed.queryAccount(participateAssetCreateKey,blockingStubFull);
    final Long afterBalance = participateInfo.getBalance();

    Assert.assertTrue(beforeBalance  - trxNum * 1 * icoNum  >= afterBalance);
  }

  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


