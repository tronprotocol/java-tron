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
public class WalletTestZenToken004 {

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
  Optional<ShieldAddressInfo> sendShieldAddressInfo;
  String sendshieldAddress;
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
  private Long costTokenAmount = 20 * zenTokenFee;
  private Long zenTokenWhenCreateNewAddress = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.zenTokenWhenCreateNewAddress");

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
    Args.setFullNodeAllowShieldedTransaction(true);
    Assert.assertTrue(PublicMethed.sendcoin(receiverPublicAddress, 1000000L,
        fromAddress, testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = false, description = "Shield to two shield transaction")
  public void test1Shield2TwoShieldTransaction() {
    sendShieldAddressInfo = PublicMethed.generateShieldAddress();
    sendshieldAddress = sendShieldAddressInfo.get().getAddress();
    String memo = "Use to TestZenToken004 shield address";
    shieldOutList = PublicMethed.addShieldOutputList(shieldOutList, sendshieldAddress,
        "" + costTokenAmount, memo);
    Assert.assertTrue(PublicMethed.sendShieldCoin(
        foundationZenTokenAddress, costTokenAmount + zenTokenFee,
        null, null,
        shieldOutList,
        null, 0,
        foundationZenTokenKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    notes = PublicMethed.listShieldNote(sendShieldAddressInfo, blockingStubFull);

    Optional<ShieldAddressInfo> shieldAddressInfo1 = PublicMethed.generateShieldAddress();
    String shieldAddress1 = shieldAddressInfo1.get().getAddress();
    Optional<ShieldAddressInfo> shieldAddressInfo2 = PublicMethed.generateShieldAddress();
    String shieldAddress2 = shieldAddressInfo2.get().getAddress();
    logger.info("shieldAddress1:" + shieldAddress1);
    logger.info("shieldAddress2:" + shieldAddress2);

    Long sendToShiledAddress1Amount = 3 * zenTokenFee;
    Long sendToShiledAddress2Amount = costTokenAmount - sendToShiledAddress1Amount - zenTokenFee;
    String memo1 = "Shield to  shield address1 transaction";
    shieldOutList.clear();
    shieldOutList = PublicMethed.addShieldOutputList(shieldOutList, shieldAddress1,
        "" + sendToShiledAddress1Amount, memo1);
    String memo2 = "Shield to  shield address2 transaction";
    shieldOutList = PublicMethed.addShieldOutputList(shieldOutList, shieldAddress2,
        "" + sendToShiledAddress2Amount, memo2);

    Assert.assertTrue(PublicMethed.sendShieldCoin(
        null, 0,
        sendShieldAddressInfo.get(), notes.getNoteTxs(0),
        shieldOutList,
        null, 0,
        zenTokenOwnerKey, blockingStubFull));

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    notes = PublicMethed.listShieldNote(sendShieldAddressInfo, blockingStubFull);
    Assert.assertTrue(PublicMethed.getSpendResult(sendShieldAddressInfo.get(),
        notes.getNoteTxs(0), blockingStubFull).getResult());

    notes = PublicMethed.listShieldNote(shieldAddressInfo1, blockingStubFull);
    note = notes.getNoteTxs(0).getNote();
    Long receiverShieldTokenAmount1 = note.getValue();
    logger.info("receiverShieldTokenAmount1:" + receiverShieldTokenAmount1);
    logger.info("sendToShiledAddress1Amount:" + sendToShiledAddress1Amount);
    Assert.assertEquals(receiverShieldTokenAmount1, sendToShiledAddress1Amount);
    Assert.assertEquals(memo1, PublicMethed.getMemo(note));

    notes = PublicMethed.listShieldNote(shieldAddressInfo2, blockingStubFull);
    note = notes.getNoteTxs(0).getNote();
    Long receiverShieldTokenAmount2 = note.getValue();
    Assert.assertEquals(receiverShieldTokenAmount2, sendToShiledAddress2Amount);
    Assert.assertEquals(memo2, PublicMethed.getMemo(note));

  }

  @Test(enabled = false,
      description = "Shield to one public and one shield transaction")
  public void test2Shield2OneShieldAndOnePublicTransaction() {
    sendShieldAddressInfo = PublicMethed.generateShieldAddress();
    sendshieldAddress = sendShieldAddressInfo.get().getAddress();
    String memo = "Use to TestZenToken004 shield address";
    shieldOutList.clear();
    shieldOutList = PublicMethed.addShieldOutputList(shieldOutList, sendshieldAddress,
        "" + costTokenAmount, memo);
    Assert.assertTrue(PublicMethed.sendShieldCoin(
        foundationZenTokenAddress, costTokenAmount + zenTokenFee,
        null, null,
        shieldOutList,
        null, 0,
        foundationZenTokenKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    notes = PublicMethed.listShieldNote(sendShieldAddressInfo, blockingStubFull);

    Optional<ShieldAddressInfo> shieldAddressInfo1 = PublicMethed.generateShieldAddress();
    String shieldAddress1 = shieldAddressInfo1.get().getAddress();
    logger.info("shieldAddress1:" + shieldAddress1);

    Long sendToShiledAddress1Amount = 1 * zenTokenFee;
    Long sendToPublicAddressAmount = costTokenAmount - sendToShiledAddress1Amount - zenTokenFee;
    shieldOutList.clear();
    String memo1 = "Shield to  shield address1 transaction";
    shieldOutList = PublicMethed.addShieldOutputList(shieldOutList, shieldAddress1,
        "" + sendToShiledAddress1Amount, memo1);

    Assert.assertTrue(PublicMethed.sendShieldCoin(
        null, 0,
        sendShieldAddressInfo.get(), notes.getNoteTxs(0),
        shieldOutList,
        receiverPublicAddress, sendToPublicAddressAmount,
        zenTokenOwnerKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    notes = PublicMethed.listShieldNote(sendShieldAddressInfo, blockingStubFull);
    Assert.assertTrue(PublicMethed.getSpendResult(sendShieldAddressInfo.get(),
        notes.getNoteTxs(0), blockingStubFull).getResult());

    notes = PublicMethed.listShieldNote(shieldAddressInfo1, blockingStubFull);
    note = notes.getNoteTxs(0).getNote();
    Long receiverShieldTokenAmount1 = note.getValue();
    logger.info("receiverShieldTokenAmount1:" + receiverShieldTokenAmount1);
    logger.info("sendToShiledAddress1Amount:" + sendToShiledAddress1Amount);
    Assert.assertEquals(receiverShieldTokenAmount1, sendToShiledAddress1Amount);
    Assert.assertEquals(memo1, PublicMethed.getMemo(note));

    Long afterReceiverPublicAssetBalance = PublicMethed.getAssetIssueValue(receiverPublicAddress,
        PublicMethed.queryAccount(foundationZenTokenKey, blockingStubFull).getAssetIssuedID(),
        blockingStubFull);
    Assert.assertEquals(afterReceiverPublicAssetBalance, sendToPublicAddressAmount);
  }

  @Test(enabled = false,
      description = "Shield to one public and two shield transaction")
  public void test3Public2OneShieldAndOnePublicTransaction() {
    sendShieldAddressInfo = PublicMethed.generateShieldAddress();
    sendshieldAddress = sendShieldAddressInfo.get().getAddress();
    String memo = "Use to TestZenToken004 shield address";
    shieldOutList.clear();
    shieldOutList = PublicMethed.addShieldOutputList(shieldOutList, sendshieldAddress,
        "" + costTokenAmount, memo);
    Assert.assertTrue(PublicMethed.sendShieldCoin(
        foundationZenTokenAddress, costTokenAmount + zenTokenFee,
        null, null,
        shieldOutList,
        null, 0,
        foundationZenTokenKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    notes = PublicMethed.listShieldNote(sendShieldAddressInfo, blockingStubFull);

    Optional<ShieldAddressInfo> shieldAddressInfo1 = PublicMethed.generateShieldAddress();
    String shieldAddress1 = shieldAddressInfo1.get().getAddress();
    Optional<ShieldAddressInfo> shieldAddressInfo2 = PublicMethed.generateShieldAddress();
    String shieldAddress2 = shieldAddressInfo2.get().getAddress();
    logger.info("shieldAddress1:" + shieldAddress1);
    logger.info("shieldAddress2:" + shieldAddress2);

    Long sendToShiledAddress1Amount = 3 * zenTokenFee;
    Long sendToShiledAddress2Amount = 4 * zenTokenFee;
    final Long sendToPublicAddressAmount = costTokenAmount - sendToShiledAddress1Amount
        - sendToShiledAddress2Amount - zenTokenWhenCreateNewAddress;
    shieldOutList.clear();
    String memo1 = "Shield to  shield address1 transaction";
    shieldOutList = PublicMethed.addShieldOutputList(shieldOutList, shieldAddress1,
        "" + sendToShiledAddress1Amount, memo1);
    String memo2 = "Shield to  shield address2 transaction";
    shieldOutList = PublicMethed.addShieldOutputList(shieldOutList, shieldAddress2,
        "" + sendToShiledAddress2Amount, memo2);
    //When receiver public address don't active,the fee is 1000000
    ECKey ecKey3 = new ECKey(Utils.getRandom());
    byte[] notActivePublicAddress = ecKey3.getAddress();

    Assert.assertTrue(PublicMethed.sendShieldCoin(
        null, 0,
        sendShieldAddressInfo.get(), notes.getNoteTxs(0),
        shieldOutList,
        notActivePublicAddress, sendToPublicAddressAmount,
        zenTokenOwnerKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    notes = PublicMethed.listShieldNote(sendShieldAddressInfo, blockingStubFull);
    Assert.assertTrue(PublicMethed.getSpendResult(sendShieldAddressInfo.get(),
        notes.getNoteTxs(0), blockingStubFull).getResult());

    notes = PublicMethed.listShieldNote(shieldAddressInfo1, blockingStubFull);
    note = notes.getNoteTxs(0).getNote();
    Long receiverShieldTokenAmount1 = note.getValue();
    Assert.assertEquals(receiverShieldTokenAmount1, sendToShiledAddress1Amount);
    Assert.assertEquals(memo1, PublicMethed.getMemo(note));

    notes = PublicMethed.listShieldNote(shieldAddressInfo2, blockingStubFull);
    note = notes.getNoteTxs(0).getNote();
    Long receiverShieldTokenAmount2 = note.getValue();
    Assert.assertEquals(receiverShieldTokenAmount2, sendToShiledAddress2Amount);
    Assert.assertEquals(memo2, PublicMethed.getMemo(note));

    final Long afterNotActivePublicAssetBalance = PublicMethed
        .getAssetIssueValue(notActivePublicAddress,
            PublicMethed.queryAccount(foundationZenTokenKey, blockingStubFull).getAssetIssuedID(),
            blockingStubFull);
    logger.info("afterNotActivePublicAssetBalance:" + afterNotActivePublicAssetBalance);
    logger.info("sendToPublicAddressAmount:" + sendToPublicAddressAmount);
    Assert.assertEquals(afterNotActivePublicAssetBalance, sendToPublicAddressAmount);
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