package stest.tron.wallet.dailybuild.assetissue;

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
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.AccountNetMessage;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Account;
import org.tron.protos.contract.AssetIssueContractOuterClass.AssetIssueContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class WalletTestAssetIssue016 {

  private static final long now = System.currentTimeMillis();
  private static final long totalSupply = now;
  private static final long sendAmount = 10000000000L;
  private static final long netCostMeasure = 200L;
  private static String name = "AssetIssue016_" + Long.toString(now);
  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);
  Long freeAssetNetLimit = 30000L;
  Long publicFreeAssetNetLimit = 30000L;
  String description = "for case assetissue016";
  String url = "https://stest.assetissue016.url";
  ByteString assetAccountId;
  //get account
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] asset016Address = ecKey1.getAddress();
  String testKeyForAssetIssue016 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] transferAssetAddress = ecKey2.getAddress();
  String transferAssetCreateKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  private ManagedChannel channelFull = null;
  private ManagedChannel channelSolidity = null;
  private ManagedChannel channelSoliInFull = null;
  private ManagedChannel channelPbft = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSoliInFull = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubPbft = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);
  private String soliInFullnode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(1);
  private String soliInPbft = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(2);

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext(true)
        .build();
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);

    channelSoliInFull = ManagedChannelBuilder.forTarget(soliInFullnode)
        .usePlaintext(true)
        .build();
    blockingStubSoliInFull = WalletSolidityGrpc.newBlockingStub(channelSoliInFull);

    channelPbft = ManagedChannelBuilder.forTarget(soliInPbft)
        .usePlaintext(true)
        .build();
    blockingStubPbft = WalletSolidityGrpc.newBlockingStub(channelPbft);
  }

  @Test(enabled = true, description = "Get asset issue net resource")
  public void test01GetAssetIssueNet() {
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
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Long start = System.currentTimeMillis() + 2000;
    Long end = System.currentTimeMillis() + 1000000000;
    Assert.assertTrue(PublicMethed
        .createAssetIssue(asset016Address, name, totalSupply, 1, 1, start, end, 1, description,
            url, freeAssetNetLimit, publicFreeAssetNetLimit, 1L, 1L, testKeyForAssetIssue016,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Account getAssetIdFromThisAccount;
    getAssetIdFromThisAccount = PublicMethed.queryAccount(asset016Address, blockingStubFull);
    assetAccountId = getAssetIdFromThisAccount.getAssetIssuedID();

    AccountNetMessage assetIssueInfo = PublicMethed
        .getAccountNet(asset016Address, blockingStubFull);
    Assert.assertTrue(assetIssueInfo.getAssetNetLimitCount() == 1);
    Assert.assertTrue(assetIssueInfo.getAssetNetUsedCount() == 1);
    Assert.assertFalse(assetIssueInfo.getAssetNetLimitMap().isEmpty());
    Assert.assertFalse(assetIssueInfo.getAssetNetUsedMap().isEmpty());

    GrpcAPI.BytesMessage request = GrpcAPI.BytesMessage.newBuilder()
        .setValue(assetAccountId).build();
    AssetIssueContract assetIssueByName = blockingStubFull.getAssetIssueByName(request);
    Assert.assertTrue(assetIssueByName.getFreeAssetNetLimit() == freeAssetNetLimit);
    Assert.assertTrue(assetIssueByName.getPublicFreeAssetNetLimit() == publicFreeAssetNetLimit);
    Assert.assertTrue(assetIssueByName.getPublicLatestFreeNetTime() == 0);
    assetIssueInfo.hashCode();
    assetIssueInfo.getSerializedSize();
    assetIssueInfo.equals(assetIssueInfo);

    PublicMethed.transferAsset(transferAssetAddress, assetAccountId.toByteArray(), 1000L,
        asset016Address, testKeyForAssetIssue016, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.transferAsset(toAddress, assetAccountId.toByteArray(), 100L,
        transferAssetAddress, transferAssetCreateKey, blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    assetIssueByName = blockingStubFull.getAssetIssueByName(request);
    Assert.assertTrue(assetIssueByName.getPublicLatestFreeNetTime() == 0);
    Assert.assertTrue(assetIssueByName.getPublicFreeAssetNetUsage() == 0);

    Assert.assertTrue(PublicMethed.freezeBalance(asset016Address, 30000000L,
        3, testKeyForAssetIssue016, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.transferAsset(toAddress, assetAccountId.toByteArray(), 100L,
        transferAssetAddress, transferAssetCreateKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    assetIssueByName = blockingStubFull.getAssetIssueByName(request);
    Assert.assertTrue(assetIssueByName.getPublicLatestFreeNetTime() > 0);
    Assert.assertTrue(assetIssueByName.getPublicFreeAssetNetUsage() > 150);

    PublicMethed
        .freedResource(asset016Address, testKeyForAssetIssue016, fromAddress, blockingStubFull);


  }

  @Test(enabled = true, description = "Get asset issue by name from Solidity")
  public void test02GetAssetIssueByNameFromSolidity() {
    Assert.assertEquals(PublicMethed.getAssetIssueByNameFromSolidity(name,
        blockingStubSolidity).getTotalSupply(), totalSupply);
  }

  @Test(enabled = true, description = "Get asset issue by name from PBFT")
  public void test03GetAssetIssueByNameFromPbft() {
    Assert.assertEquals(PublicMethed.getAssetIssueByNameFromSolidity(name,
        blockingStubPbft).getTotalSupply(), totalSupply);
  }

  @Test(enabled = true, description = "Get asset issue list from PBFT")
  public void test04GetAssetIssueListFromPbft() {
    Assert.assertTrue(PublicMethed.listAssetIssueFromSolidity(
        blockingStubPbft).get().getAssetIssueCount() >= 1);
  }


  @Test(enabled = true, description = "Get asset issue list from Solidity")
  public void test05GetAssetIssueListFromSolidity() {
    Assert.assertTrue(PublicMethed.listAssetIssueFromSolidity(
        blockingStubSoliInFull).get().getAssetIssueCount() >= 1);
    Assert.assertTrue(PublicMethed.listAssetIssueFromSolidity(
        blockingStubSolidity).get().getAssetIssueCount() >= 1);
  }

  @Test(enabled = true, description = "Get asset issue list paginated from PBFT")
  public void test06GetAssetIssetListPaginatedFromPbft() {
    Assert.assertTrue(PublicMethed.listAssetIssuepaginatedFromSolidity(
        blockingStubPbft, 0L, 1L).get().getAssetIssueCount() == 1);
  }


  @Test(enabled = true, description = "Get asset issue list paginated from Solidity")
  public void test05GetAssetIssueListPaginatedFromSolidity() {
    Assert.assertTrue(PublicMethed.listAssetIssuepaginatedFromSolidity(
        blockingStubSolidity, 0L, 1L).get().getAssetIssueCount() == 1);
    Assert.assertTrue(PublicMethed.listAssetIssuepaginatedFromSolidity(
        blockingStubSoliInFull, 0L, 1L).get().getAssetIssueCount() == 1);
  }


  /**
   * constructor.
   */
  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelSolidity != null) {
      channelSolidity.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelPbft != null) {
      channelPbft.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelSoliInFull != null) {
      channelSoliInFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}