package stest.tron.wallet.dailybuild.delaytransaction;

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
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;

//import org.tron.protos.Protocol.DeferredTransaction;

@Slf4j
public class DelayTransaction003 {

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
  Optional<TransactionInfo> infoById = null;
  //Optional<DeferredTransaction> deferredTransactionById = null;
  Optional<Transaction> getTransactionById = null;
  ECKey ecKey = new ECKey(Utils.getRandom());
  byte[] assetOwnerAddress = ecKey.getAddress();
  String assetOwnerKey = ByteArray.toHexString(ecKey.getPrivKeyBytes());
  ECKey ecKey3 = new ECKey(Utils.getRandom());
  byte[] receiverAssetAddress = ecKey3.getAddress();
  String receiverassetKey = ByteArray.toHexString(ecKey3.getPrivKeyBytes());
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(1);
  private Long delayTransactionFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.delayTransactionFee");
  private Long cancleDelayTransactionFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.cancleDelayTransactionFee");

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

  @Test(enabled = false, description = "Delay transfer asset")
  public void test1DelayTransferAsset() {
    //get account
    ecKey = new ECKey(Utils.getRandom());
    assetOwnerAddress = ecKey.getAddress();
    assetOwnerKey = ByteArray.toHexString(ecKey.getPrivKeyBytes());
    PublicMethed.printAddress(assetOwnerKey);
    ecKey3 = new ECKey(Utils.getRandom());
    receiverAssetAddress = ecKey3.getAddress();
    receiverassetKey = ByteArray.toHexString(ecKey3.getPrivKeyBytes());
    PublicMethed.printAddress(receiverassetKey);

    Assert.assertTrue(PublicMethed.sendcoin(assetOwnerAddress, 2048000000, fromAddress,
        testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    //Create test token.
    Long start = System.currentTimeMillis() + 2000;
    Long end = System.currentTimeMillis() + 1000000000;
    Assert.assertTrue(PublicMethed.createAssetIssue(assetOwnerAddress,
        name, totalSupply, 1, 1, start, end, 1, description, url,
        2000L, 2000L, 100000L, 1L,
        assetOwnerKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Account assetOwnerAccount = PublicMethed.queryAccount(assetOwnerKey, blockingStubFull);
    assetId = assetOwnerAccount.getAssetIssuedID();

    //Delay transfer asset
    Long transferAssetAmount = 1L;
    final Long ownerAssetBalanceOfbeforeTransferAsset = PublicMethed
        .getAssetBalanceByAssetId(assetId, assetOwnerKey, blockingStubFull);
    final Long receiverAssetBalanceOfbeforeTransferAsset = PublicMethed
        .getAssetBalanceByAssetId(assetId, receiverassetKey, blockingStubFull);
    Assert.assertTrue(PublicMethed.transferAssetDelay(receiverAssetAddress, assetId.toByteArray(),
        transferAssetAmount, delaySecond, assetOwnerAddress, assetOwnerKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long ownerAssetBalanceInDelayTransferAsset = PublicMethed
        .getAssetBalanceByAssetId(assetId, assetOwnerKey, blockingStubFull);
    final Long receiverAssetBalanceInDelayTransferAsset = PublicMethed
        .getAssetBalanceByAssetId(assetId, receiverassetKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long ownerAssetBalanceAfterTransferAsset = PublicMethed
        .getAssetBalanceByAssetId(assetId, assetOwnerKey, blockingStubFull);
    Long receiverAssetBalanceAfterTransferAsset = PublicMethed
        .getAssetBalanceByAssetId(assetId, receiverassetKey, blockingStubFull);

    Assert.assertEquals(ownerAssetBalanceOfbeforeTransferAsset,
        ownerAssetBalanceInDelayTransferAsset);
    Assert.assertTrue(receiverAssetBalanceOfbeforeTransferAsset
        == receiverAssetBalanceInDelayTransferAsset);
    Assert.assertTrue(ownerAssetBalanceInDelayTransferAsset - transferAssetAmount
        == ownerAssetBalanceAfterTransferAsset);
    Assert.assertTrue(receiverAssetBalanceAfterTransferAsset == transferAssetAmount);

  }


  @Test(enabled = false, description = "Cancel delay transfer asset")
  public void test2CancelDelayTransferAsset() {

    //Delay transfer asset
    Long transferAssetAmount = 1L;
    final Long ownerAssetBalanceOfbeforeTransferAsset = PublicMethed
        .getAssetBalanceByAssetId(assetId, assetOwnerKey, blockingStubFull);
    final Long receiverAssetBalanceOfbeforeTransferAsset = PublicMethed
        .getAssetBalanceByAssetId(assetId, receiverassetKey, blockingStubFull);

    String txid = PublicMethed.transferAssetDelayGetTxid(receiverAssetAddress,
        assetId.toByteArray(), transferAssetAmount, delaySecond, assetOwnerAddress, assetOwnerKey,
        blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    //Assert.assertFalse(PublicMethed.cancelDeferredTransactionById(txid,receiverAssetAddress,
    // receiverassetKey,blockingStubFull));
    Assert.assertTrue(PublicMethed.cancelDeferredTransactionById(txid, assetOwnerAddress,
        assetOwnerKey, blockingStubFull));
    //Assert.assertFalse(PublicMethed.cancelDeferredTransactionById(txid,assetOwnerAddress,
    // assetOwnerKey,blockingStubFull));

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long ownerAssetBalanceAfterTransferAsset = PublicMethed
        .getAssetBalanceByAssetId(assetId, assetOwnerKey, blockingStubFull);
    Long receiverAssetBalanceAfterTransferAsset = PublicMethed
        .getAssetBalanceByAssetId(assetId, receiverassetKey, blockingStubFull);

    Assert.assertEquals(ownerAssetBalanceOfbeforeTransferAsset, ownerAssetBalanceAfterTransferAsset
    );
    Assert.assertTrue(receiverAssetBalanceAfterTransferAsset
        == receiverAssetBalanceOfbeforeTransferAsset);
    Assert.assertFalse(PublicMethed.cancelDeferredTransactionById(txid, assetOwnerAddress,
        assetOwnerKey, blockingStubFull));

  }

  @Test(enabled = false, description = "Delay unfreeze asset")
  public void test3DelayUnfreezeAsset() {

    final Long ownerAssetBalanceOfbeforeTransferAsset = PublicMethed
        .getAssetBalanceByAssetId(assetId, assetOwnerKey, blockingStubFull);

    String txid = PublicMethed.unfreezeAssetDelayGetTxid(assetOwnerAddress, delaySecond,
        assetOwnerKey, blockingStubFull);


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


