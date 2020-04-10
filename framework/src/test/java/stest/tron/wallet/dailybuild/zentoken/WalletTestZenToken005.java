package stest.tron.wallet.dailybuild.zentoken;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.DecryptNotes;
import org.tron.api.GrpcAPI.Note;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.core.config.args.Args;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.ShieldAddressInfo;

@Slf4j
public class WalletTestZenToken005 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  List<Note> shieldOutList = new ArrayList<>();
  DecryptNotes notes;
  Note note;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] zenTokenOwnerAddress = ecKey1.getAddress();
  String zenTokenOwnerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] receiverPublicAddress = ecKey2.getAddress();
  String receiverPublicKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private String foundationZenTokenKey = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.zenTokenOwnerKey");
  byte[] foundationZenTokenAddress = PublicMethed.getFinalAddress(foundationZenTokenKey);
  private String zenTokenId = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.zenTokenId");
  private byte[] tokenId = zenTokenId.getBytes();
  private Long zenTokenFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.zenTokenFee");
  private Long costTokenAmount = 10 * zenTokenFee;

  /**
   * constructor.
   */
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
    PublicMethed.printAddress(foundationZenTokenKey);
    PublicMethed.printAddress(zenTokenOwnerKey);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    Assert.assertTrue(PublicMethed.transferAsset(zenTokenOwnerAddress, tokenId,
        costTokenAmount, foundationZenTokenAddress, foundationZenTokenKey, blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoin(receiverPublicAddress, 1000000L,
        fromAddress, testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Args.setFullNodeAllowShieldedTransaction(true);
  }

  @Test(enabled = false,
      description = "The receiver shield address can't more then 2")
  public void test1ReceiverShieldAddressCanNotMoreThenTwo() {
    Optional<ShieldAddressInfo> shieldAddressInfo1 = PublicMethed.generateShieldAddress();
    String shieldAddress1 = shieldAddressInfo1.get().getAddress();
    Optional<ShieldAddressInfo> shieldAddressInfo2 = PublicMethed.generateShieldAddress();
    String shieldAddress2 = shieldAddressInfo2.get().getAddress();
    Optional<ShieldAddressInfo> shieldAddressInfo3 = PublicMethed.generateShieldAddress();
    String shieldAddress3 = shieldAddressInfo3.get().getAddress();
    logger.info("shieldAddress1:" + shieldAddress1);
    logger.info("shieldAddress2:" + shieldAddress2);
    logger.info("shieldAddress3:" + shieldAddress3);

    Long sendToShiledAddress1Amount = 3 * zenTokenFee;
    Long sendToShiledAddress2Amount = 2 * zenTokenFee;
    Long sendToShiledAddress3Amount = costTokenAmount - sendToShiledAddress1Amount
        - sendToShiledAddress2Amount - zenTokenFee;
    String memo1 = "Shield to  shield address1 transaction";
    shieldOutList = PublicMethed.addShieldOutputList(shieldOutList, shieldAddress1,
        "" + sendToShiledAddress1Amount, memo1);
    String memo2 = "Shield to  shield address2 transaction";
    shieldOutList = PublicMethed.addShieldOutputList(shieldOutList, shieldAddress2,
        "" + sendToShiledAddress2Amount, memo2);
    String memo3 = "Shield to  shield address3 transaction";
    shieldOutList = PublicMethed.addShieldOutputList(shieldOutList, shieldAddress3,
        "" + sendToShiledAddress3Amount, memo3);

    Assert.assertFalse(PublicMethed.sendShieldCoin(
        zenTokenOwnerAddress, costTokenAmount,
        null, null,
        shieldOutList,
        null, 0,
        zenTokenOwnerKey, blockingStubFull));
  }

  @Test(enabled = false,
      description = "The receiver can't only one public address")
  public void test2ReceiverPublicCanNotOnlyOnePublic() {
    Assert.assertTrue(PublicMethed.transferAsset(zenTokenOwnerAddress, tokenId,
        costTokenAmount, foundationZenTokenAddress, foundationZenTokenKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    shieldOutList.clear();
    Assert.assertFalse(PublicMethed.sendShieldCoin(
        zenTokenOwnerAddress, costTokenAmount,
        null, null,
        shieldOutList,
        receiverPublicAddress, costTokenAmount - zenTokenFee,
        zenTokenOwnerKey, blockingStubFull));
  }

  @Test(enabled = false,
      description = "Public send amount must equal receiver amount + shieldFee")
  public void test3SendAmountMustEqualReceiverAmountPlusShieldFee() {
    Assert.assertTrue(PublicMethed.transferAsset(zenTokenOwnerAddress, tokenId,
        costTokenAmount, foundationZenTokenAddress, foundationZenTokenKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<ShieldAddressInfo> shieldAddressInfo1 = PublicMethed.generateShieldAddress();
    String shieldAddress1 = shieldAddressInfo1.get().getAddress();
    Optional<ShieldAddressInfo> shieldAddressInfo2 = PublicMethed.generateShieldAddress();
    String shieldAddress2 = shieldAddressInfo2.get().getAddress();
    logger.info("shieldAddress1:" + shieldAddress1);
    logger.info("shieldAddress2:" + shieldAddress2);

    Long sendToShiledAddress1Amount = 1 * zenTokenFee;
    Long sendToShiledAddress2Amount = 2 * zenTokenFee;

    shieldOutList.clear();
    String memo1 = "Public to  shield address1 transaction";
    shieldOutList = PublicMethed.addShieldOutputList(shieldOutList, shieldAddress1,
        "" + sendToShiledAddress1Amount, memo1);
    String memo2 = "Public to  shield address2 transaction";
    shieldOutList = PublicMethed.addShieldOutputList(shieldOutList, shieldAddress2,
        "" + sendToShiledAddress2Amount, memo2);

    //Public receiver amount is wrong
    Long sendToPublicAddressAmount = costTokenAmount - sendToShiledAddress1Amount
        - sendToShiledAddress2Amount - zenTokenFee;
    Assert.assertFalse(PublicMethed.sendShieldCoin(
        zenTokenOwnerAddress, costTokenAmount,
        null, null,
        shieldOutList,
        receiverPublicAddress, sendToPublicAddressAmount - 1,
        zenTokenOwnerKey, blockingStubFull));

    //Shield receiver amount is wrong
    sendToShiledAddress1Amount = 1 * zenTokenFee;
    sendToShiledAddress2Amount = 2 * zenTokenFee;
    sendToPublicAddressAmount = costTokenAmount - sendToShiledAddress1Amount
        - sendToShiledAddress2Amount - zenTokenFee;
    shieldOutList.clear();
    shieldOutList = PublicMethed.addShieldOutputList(shieldOutList, shieldAddress1,
        "" + (sendToShiledAddress1Amount - 1), memo1);
    shieldOutList = PublicMethed.addShieldOutputList(shieldOutList, shieldAddress2,
        "" + sendToShiledAddress2Amount, memo2);
    Assert.assertFalse(PublicMethed.sendShieldCoin(
        zenTokenOwnerAddress, costTokenAmount,
        null, null,
        shieldOutList,
        receiverPublicAddress, sendToPublicAddressAmount,
        zenTokenOwnerKey, blockingStubFull));

    sendToShiledAddress1Amount = 1 * zenTokenFee;
    sendToShiledAddress2Amount = 2 * zenTokenFee;
    sendToPublicAddressAmount = costTokenAmount - sendToShiledAddress1Amount
        - sendToShiledAddress2Amount - zenTokenFee;
    shieldOutList.clear();
    shieldOutList = PublicMethed.addShieldOutputList(shieldOutList, shieldAddress1,
        "" + sendToShiledAddress1Amount, memo1);
    shieldOutList = PublicMethed.addShieldOutputList(shieldOutList, shieldAddress2,
        "" + sendToShiledAddress2Amount, memo2);
    Assert.assertTrue(PublicMethed.sendShieldCoin(
        zenTokenOwnerAddress, costTokenAmount,
        null, null,
        shieldOutList,
        receiverPublicAddress, sendToPublicAddressAmount,
        zenTokenOwnerKey, blockingStubFull));
  }

  /**
   * constructor.
   */

  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
    PublicMethed.transferAsset(foundationZenTokenAddress, tokenId,
        PublicMethed.getAssetIssueValue(zenTokenOwnerAddress,
            PublicMethed.queryAccount(foundationZenTokenKey, blockingStubFull).getAssetIssuedID(),
            blockingStubFull), zenTokenOwnerAddress, zenTokenOwnerKey, blockingStubFull);
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}