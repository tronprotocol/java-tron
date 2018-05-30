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
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.protos.Protocol.Account;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class WalletTestAssetIssue011 {

  //testng001、testng002、testng003、testng004
  private final String testKey002 =
      "FC8BF0238748587B9617EB6D15D47A66C0E07C1A1959033CF249C6532DC29FE6";
  //private final static  String testKeyForAssetIssue011 = 
  // "66BB2BA4233E8DC8CFDBC36D28E0898E7F7A58D8B78061ADB9215B9E7ED5F666";
  //private final static  String transferAssetCreateKey =
  // "895FBF2CEE60509EC4EE6F7D3ACE608FD30AEBD3A95293C46ECE7FD851B3FA72";


  //testng001、testng002、testng003、testng004
  private static final byte[] FROM_ADDRESS = Base58
      .decodeFromBase58Check("27WvzgdLiUvNAStq2BCvA1LZisdD3fBX8jv");
  //private static final byte[] asset011Address =
  // Base58.decodeFromBase58Check("27meuueCQsoqrDBpRM6XBPREvYtxHU5q6TU");
  //private static final byte[] transferAssetCreateAddress =
  // Base58.decodeFromBase58Check("27RC2QnokoC1QBAGc1NMj7FSe2KZ5CTLvfV");


  private static final long now = System.currentTimeMillis();
  private static String name = "testAssetIssue_" + Long.toString(now);
  private static final long totalSupply = now;
  private static final long sendAmount = 10000000000L;
  private static final String updateMostLongName = "w2345678901234567890123456789012";

  Long freeAssetNetLimit = 10000L;
  Long publicFreeAssetNetLimit = 10000L;
  String description = "just-test";
  String url = "https://github.com/tronprotocol/wallet-cli/";


  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);

  //get account
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] asset011Address = ecKey1.getAddress();
  String testKeyForAssetIssue011 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());


  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] transferAssetCreateAddress = ecKey2.getAddress();
  String transferAssetCreateKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());


  @BeforeClass(enabled = true)
  public void beforeClass() {
    logger.info(testKeyForAssetIssue011);
    logger.info(transferAssetCreateKey);

    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    //Sendcoin to this account
    ByteString addressBS1 = ByteString.copyFrom(asset011Address);
    Account request1 = Account.newBuilder().setAddress(addressBS1).build();
    GrpcAPI.AssetIssueList assetIssueList1 = blockingStubFull
        .getAssetIssueByAccount(request1);
    Optional<GrpcAPI.AssetIssueList> queryAssetByAccount = Optional.ofNullable(assetIssueList1);
    if (queryAssetByAccount.get().getAssetIssueCount() == 0) {
      Assert.assertTrue(PublicMethed.freezeBalance(FROM_ADDRESS, 10000000, 3, testKey002,
          blockingStubFull));
      Assert.assertTrue(PublicMethed
          .sendcoin(asset011Address, sendAmount, FROM_ADDRESS, testKey002, blockingStubFull));
      Assert.assertTrue(PublicMethed
          .freezeBalance(asset011Address, 100000000L, 3, testKeyForAssetIssue011,
              blockingStubFull));
      Long start = System.currentTimeMillis() + 2000;
      Long end = System.currentTimeMillis() + 1000000000;
      Assert.assertTrue(PublicMethed
          .createAssetIssue(asset011Address, name, totalSupply, 1, 1, start, end, 1, description,
              url, freeAssetNetLimit, publicFreeAssetNetLimit, 1L, 1L, testKeyForAssetIssue011,
              blockingStubFull));
    } else {
      logger.info("This account already create an assetisue");
      Optional<GrpcAPI.AssetIssueList> queryAssetByAccount1 = Optional.ofNullable(assetIssueList1);
      name = ByteArray.toStr(queryAssetByAccount1.get().getAssetIssue(0).getName().toByteArray());
    }
  }

  @Test(enabled = true)
  public void testTransferAssetCreateAccount() {
    //Transfer asset to create an account.
    Assert.assertTrue(PublicMethed
        .transferAsset(transferAssetCreateAddress, name.getBytes(), 1L, asset011Address,
            testKeyForAssetIssue011, blockingStubFull));

    Account queryTransferAssetAccount = PublicMethed
        .queryAccount(transferAssetCreateKey, blockingStubFull);
    Assert.assertTrue(queryTransferAssetAccount.getAssetCount() == 1);
    Assert.assertTrue(PublicMethed.updateAccount(asset011Address, "wwwxxxeeef"
        .getBytes(), testKeyForAssetIssue011, blockingStubFull));
    Assert.assertTrue(PublicMethed.updateAccount(transferAssetCreateAddress, updateMostLongName
        .getBytes(), transferAssetCreateKey, blockingStubFull));
    queryTransferAssetAccount = PublicMethed.queryAccount(transferAssetCreateKey, blockingStubFull);
    Assert.assertFalse(queryTransferAssetAccount.getAccountName().isEmpty());

  }


  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


