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
import org.tron.api.WalletExtensionGrpc;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Contract;
import org.tron.protos.Protocol.Account;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class WalletTestAssetIssue016 {
  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);


  private static final long now = System.currentTimeMillis();
  private static String name = "AssetIssue016_" + Long.toString(now);
  private static final long totalSupply = now;
  private static final long sendAmount = 10000000000L;
  private static final long netCostMeasure = 200L;

  Long freeAssetNetLimit = 30000L;
  Long publicFreeAssetNetLimit = 30000L;
  String description = "for case assetissue016";
  String url = "https://stest.assetissue016.url";
  ByteString assetAccountId;


  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;

  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);

  //get account
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] asset016Address = ecKey1.getAddress();
  String testKeyForAssetIssue016 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());


  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] transferAssetAddress = ecKey2.getAddress();
  String transferAssetCreateKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  @BeforeClass(enabled = true)
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
  }

  @Test(enabled = true)
  public void testGetAssetIssueNet() {
    //get account
    ecKey1 = new ECKey(Utils.getRandom());
    asset016Address = ecKey1.getAddress();
    testKeyForAssetIssue016 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());


    ecKey2 = new ECKey(Utils.getRandom());
    transferAssetAddress = ecKey2.getAddress();
    transferAssetCreateKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

    PublicMethed.printAddress(testKeyForAssetIssue016);
    PublicMethed.printAddress(transferAssetCreateKey);

    Assert.assertTrue(PublicMethed
        .sendcoin(asset016Address, sendAmount, fromAddress, testKey002, blockingStubFull));
    Long start = System.currentTimeMillis() + 2000;
    Long end = System.currentTimeMillis() + 1000000000;
    Assert.assertTrue(PublicMethed
        .createAssetIssue(asset016Address, name, totalSupply, 1, 1, start, end, 1, description,
            url, freeAssetNetLimit, publicFreeAssetNetLimit, 1L, 1L, testKeyForAssetIssue016,
            blockingStubFull));

    Account getAssetIdFromThisAccount;
    getAssetIdFromThisAccount = PublicMethed.queryAccount(asset016Address,blockingStubFull);
    assetAccountId = getAssetIdFromThisAccount.getAssetIssuedID();
    
    
    AccountNetMessage assetIssueInfo = PublicMethed.getAccountNet(asset016Address,blockingStubFull);
    Assert.assertTrue(assetIssueInfo.getAssetNetLimitCount() == 1);
    Assert.assertTrue(assetIssueInfo.getAssetNetUsedCount() == 1);
    Assert.assertFalse(assetIssueInfo.getAssetNetLimitMap().isEmpty());
    Assert.assertFalse(assetIssueInfo.getAssetNetUsedMap().isEmpty());

    GrpcAPI.BytesMessage request = GrpcAPI.BytesMessage.newBuilder().setValue(assetAccountId).build();
    Contract.AssetIssueContract assetIssueByName = blockingStubFull.getAssetIssueByName(request);
    Assert.assertTrue(assetIssueByName.getFreeAssetNetLimit() == freeAssetNetLimit);
    Assert.assertTrue(assetIssueByName.getPublicFreeAssetNetLimit() == publicFreeAssetNetLimit);
    Assert.assertTrue(assetIssueByName.getPublicLatestFreeNetTime() == 0);
    assetIssueInfo.hashCode();
    assetIssueInfo.getSerializedSize();
    assetIssueInfo.equals(assetIssueInfo);

    PublicMethed.transferAsset(transferAssetAddress,assetAccountId.toByteArray(),1000L,
        asset016Address,testKeyForAssetIssue016,blockingStubFull);

    PublicMethed.transferAsset(toAddress,assetAccountId.toByteArray(),100L,
        transferAssetAddress,transferAssetCreateKey,blockingStubFull);

    assetIssueByName = blockingStubFull.getAssetIssueByName(request);
    Assert.assertTrue(assetIssueByName.getPublicLatestFreeNetTime() == 0);
    Assert.assertTrue(assetIssueByName.getPublicFreeAssetNetUsage() == 0);

    Assert.assertTrue(PublicMethed.freezeBalance(asset016Address,3000000L,
        3,testKeyForAssetIssue016,blockingStubFull));
    PublicMethed.transferAsset(toAddress,assetAccountId.toByteArray(),100L,
        transferAssetAddress,transferAssetCreateKey,blockingStubFull);

    assetIssueByName = blockingStubFull.getAssetIssueByName(request);
    Assert.assertTrue(assetIssueByName.getPublicLatestFreeNetTime() > 0);
    Assert.assertTrue(assetIssueByName.getPublicFreeAssetNetUsage() > 150);


  }

  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


