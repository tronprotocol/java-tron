package stest.tron.wallet.dailybuild.delaytransaction;

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
import org.tron.protos.Protocol.Account;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class DelayTransaction006 {

  private static final long now = System.currentTimeMillis();
  private static final long totalSupply = now;
  private static final String name = "Asset008_" + Long.toString(now);
  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);
  String description = "just-test";
  String url = "https://github.com/tronprotocol/wallet-cli/";
  Long delaySecond = 10L;
  ByteString assetId;
  SmartContract smartContract;
  ECKey ecKey = new ECKey(Utils.getRandom());
  byte[] assetOwnerAddress = ecKey.getAddress();
  String assetOwnerKey = ByteArray.toHexString(ecKey.getPrivKeyBytes());
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(1);
  private Long delayTransactionFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.delayTransactionFee");
  private Long cancleDelayTransactionFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.cancleDelayTransactionFee");
  private byte[] contractAddress = null;

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  /**
   * constructor.
   */

  @BeforeClass(enabled = false)
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
  }

  @Test(enabled = false, description = "Delay update asset contract")
  public void test1DelayUpdateSetting() {
    //get account
    ecKey = new ECKey(Utils.getRandom());
    assetOwnerAddress = ecKey.getAddress();
    assetOwnerKey = ByteArray.toHexString(ecKey.getPrivKeyBytes());
    PublicMethed.printAddress(assetOwnerKey);

    Assert.assertTrue(PublicMethed.sendcoin(assetOwnerAddress, 2048000000, fromAddress,
        testKey002, blockingStubFull));

    final Long oldFreeAssetNetLimit = 2000L;
    Long start = System.currentTimeMillis() + 2000;
    Long end = System.currentTimeMillis() + 1000000000;
    Assert.assertTrue(PublicMethed.createAssetIssue(assetOwnerAddress,
        name, totalSupply, 1, 1, start, end, 1, description, url,
        oldFreeAssetNetLimit, 2000L, 1L, 1L,
        assetOwnerKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Account assetOwnerAccount = PublicMethed.queryAccount(assetOwnerKey, blockingStubFull);
    assetId = assetOwnerAccount.getAssetIssuedID();
    String newAssetUrl = "new.url";
    String newAssetDescription = "new.description";

    final Long newFreeAssetNetLimit = 3333L;
    final String txid = PublicMethed.updateAssetDelay(assetOwnerAddress,
        newAssetDescription.getBytes(), newAssetUrl.getBytes(), newFreeAssetNetLimit,
        23L, delaySecond, assetOwnerKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethed.getAssetIssueById(ByteArray.toStr(assetId
        .toByteArray()), blockingStubFull).getFreeAssetNetLimit() == oldFreeAssetNetLimit);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethed.getAssetIssueById(ByteArray.toStr(assetId
        .toByteArray()), blockingStubFull).getFreeAssetNetLimit() == newFreeAssetNetLimit);
    Long afterNetUsaged = PublicMethed.queryAccount(assetOwnerKey, blockingStubFull)
        .getFreeNetUsage();
    Long netFee = PublicMethed.getTransactionInfoById(txid, blockingStubFull).get()
        .getReceipt().getNetFee();
    Long fee = PublicMethed.getTransactionInfoById(txid, blockingStubFull).get().getFee();
    Long beforeNetUsaged = PublicMethed.queryAccount(assetOwnerKey, blockingStubFull)
        .getFreeNetUsage();
    Long inDelayNetUsaged = PublicMethed.queryAccount(assetOwnerKey, blockingStubFull)
        .getFreeNetUsage();
    Assert.assertTrue(fee - netFee == delayTransactionFee);
    Assert.assertTrue(beforeNetUsaged + 50 < inDelayNetUsaged);
    Assert.assertTrue(inDelayNetUsaged + 50 < afterNetUsaged);

  }

  @Test(enabled = false, description = "Cancel delay asset setting contract")
  public void test2CancelDelayUpdateAsset() {
    //get account
    final Long oldFreeAssetNetLimit = PublicMethed.getAssetIssueById(assetId.toStringUtf8(),
        blockingStubFull).getFreeAssetNetLimit();
    final Long newFreeAssetNetLimit = 461L;

    String newAssetUrl = "new.url";
    String newAssetDescription = "new.description";
    logger.info("Before delay net usage: " + PublicMethed.queryAccount(assetOwnerKey,
        blockingStubFull).getFreeNetUsage());
    String txid = PublicMethed.updateAssetDelay(assetOwnerAddress, newAssetDescription.getBytes(),
        newAssetUrl.getBytes(), newFreeAssetNetLimit, 23L, delaySecond, assetOwnerKey,
        blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    logger.info("In delay net usage: " + PublicMethed.queryAccount(assetOwnerKey, blockingStubFull)
        .getFreeNetUsage());
    Assert.assertFalse(PublicMethed.cancelDeferredTransactionById(txid, fromAddress, testKey002,
        blockingStubFull));
    final String cancelTxid = PublicMethed.cancelDeferredTransactionByIdGetTxid(txid,
        assetOwnerAddress, assetOwnerKey, blockingStubFull);
    Assert.assertFalse(PublicMethed.cancelDeferredTransactionById(txid, assetOwnerAddress,
        assetOwnerKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    logger.info("After cancle net usage: " + PublicMethed.queryAccount(assetOwnerKey,
        blockingStubFull).getFreeNetUsage());

    Assert.assertTrue(PublicMethed.getAssetIssueById(assetId.toStringUtf8(),
        blockingStubFull).getFreeAssetNetLimit() == oldFreeAssetNetLimit);

    final Long netFee = PublicMethed.getTransactionInfoById(cancelTxid, blockingStubFull).get()
        .getReceipt().getNetFee();
    final Long fee = PublicMethed.getTransactionInfoById(cancelTxid, blockingStubFull).get()
        .getFee();
    logger.info("net fee : " + PublicMethed.getTransactionInfoById(cancelTxid, blockingStubFull)
        .get().getReceipt().getNetFee());
    logger.info("Fee : " + PublicMethed.getTransactionInfoById(cancelTxid, blockingStubFull)
        .get().getFee());

    Assert.assertTrue(fee - netFee == cancleDelayTransactionFee);

    Long afterNetUsaged = PublicMethed.queryAccount(assetOwnerKey, blockingStubFull)
        .getFreeNetUsage();
    Long beforeNetUsaged = PublicMethed.queryAccount(assetOwnerKey, blockingStubFull)
        .getFreeNetUsage();

    logger.info("beforeNetUsaged: " + beforeNetUsaged);
    logger.info("afterNetUsaged:  " + afterNetUsaged);
    Assert.assertTrue(beforeNetUsaged >= afterNetUsaged);

  }


  /**
   * constructor.
   */

  @AfterClass(enabled = false)
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


