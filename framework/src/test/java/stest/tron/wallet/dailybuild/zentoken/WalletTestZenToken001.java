package stest.tron.wallet.dailybuild.zentoken;

import com.google.protobuf.ByteString;
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
import org.tron.api.GrpcAPI.SpendResult;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.core.config.args.Args;
import org.tron.protos.Protocol.Account;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.ShieldAddressInfo;

@Slf4j
public class WalletTestZenToken001 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  Optional<ShieldAddressInfo> shieldAddressInfo;
  String shieldAddress;
  List<Note> shieldOutList = new ArrayList<>();
  List<Long> shieldInputList = new ArrayList<>();
  DecryptNotes notes;
  String memo;
  Note note;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] zenTokenOwnerAddress = ecKey1.getAddress();
  String zenTokenOwnerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
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
  private Long costTokenAmount = 8 * zenTokenFee;
  private Long sendTokenAmount = 3 * zenTokenFee;

  /**
   * constructor.
   */
  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    logger.info("enter this");
    if (PublicMethed.queryAccount(foundationZenTokenKey, blockingStubFull).getCreateTime() == 0) {
      PublicMethed.sendcoin(foundationZenTokenAddress, 20480000000000L, fromAddress,
          testKey002, blockingStubFull);
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      String name = "shieldToken";
      Long start = System.currentTimeMillis() + 20000;
      Long end = System.currentTimeMillis() + 10000000000L;
      Long totalSupply = 15000000000000001L;
      String description = "This asset issue is use for exchange transaction stress";
      String url = "This asset issue is use for exchange transaction stress";
      PublicMethed.createAssetIssue(foundationZenTokenAddress, name, totalSupply, 1, 1,
          start, end, 1, description, url, 1000L, 1000L,
          1L, 1L, foundationZenTokenKey, blockingStubFull);
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      Account getAssetIdFromThisAccount =
          PublicMethed.queryAccount(foundationZenTokenAddress, blockingStubFull);
      ByteString assetAccountId = getAssetIdFromThisAccount.getAssetIssuedID();
      logger.info("AssetId:" + assetAccountId.toString());
    }
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
        costTokenAmount, foundationZenTokenAddress, foundationZenTokenKey,
        blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = false, description = "Public to shield transaction")
  public void test1Public2ShieldTransaction() {
    Args.setFullNodeAllowShieldedTransaction(true);
    shieldAddressInfo = PublicMethed.generateShieldAddress();
    shieldAddress = shieldAddressInfo.get().getAddress();
    logger.info("shieldAddress:" + shieldAddress);
    final Long beforeAssetBalance = PublicMethed.getAssetIssueValue(zenTokenOwnerAddress,
        PublicMethed.queryAccount(foundationZenTokenKey, blockingStubFull).getAssetIssuedID(),
        blockingStubFull);
    final Long beforeNetUsed = PublicMethed
        .getAccountResource(zenTokenOwnerAddress, blockingStubFull).getFreeNetUsed();

    memo = "aaaaaaa";

    shieldOutList = PublicMethed.addShieldOutputList(shieldOutList, shieldAddress,
        "" + (sendTokenAmount - zenTokenFee), memo);

    Assert.assertTrue(PublicMethed.sendShieldCoin(
        zenTokenOwnerAddress, sendTokenAmount,
        null, null,
        shieldOutList,
        null, 0,
        zenTokenOwnerKey, blockingStubFull));

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long afterAssetBalance = PublicMethed.getAssetIssueValue(zenTokenOwnerAddress,
        PublicMethed.queryAccount(foundationZenTokenKey, blockingStubFull).getAssetIssuedID(),
        blockingStubFull);
    Long afterNetUsed = PublicMethed.getAccountResource(zenTokenOwnerAddress, blockingStubFull)
        .getFreeNetUsed();
    Assert.assertTrue(beforeAssetBalance - afterAssetBalance == sendTokenAmount);
    Assert.assertTrue(beforeNetUsed == afterNetUsed);
    notes = PublicMethed.listShieldNote(shieldAddressInfo, blockingStubFull);
    note = notes.getNoteTxs(0).getNote();
    Long receiverShieldTokenAmount = note.getValue();
    Assert.assertTrue(receiverShieldTokenAmount == sendTokenAmount - zenTokenFee);
    Assert.assertEquals(memo, PublicMethed.getMemo(note));
  }

  @Test(enabled = false, description = "Shield to public transaction")
  public void test2Shield2PublicTransaction() {
    note = notes.getNoteTxs(0).getNote();
    SpendResult result = PublicMethed.getSpendResult(shieldAddressInfo.get(),
        notes.getNoteTxs(0), blockingStubFull);
    Assert.assertTrue(!result.getResult());

    shieldOutList.clear();
    final Long beforeAssetBalance = PublicMethed.getAssetIssueValue(zenTokenOwnerAddress,
        PublicMethed.queryAccount(foundationZenTokenKey, blockingStubFull).getAssetIssuedID(),
        blockingStubFull);

    Assert.assertTrue(PublicMethed.sendShieldCoin(
        null, 0,
        shieldAddressInfo.get(), notes.getNoteTxs(0),
        shieldOutList,
        zenTokenOwnerAddress, note.getValue() - zenTokenFee,
        zenTokenOwnerKey, blockingStubFull));

    //When you want to send shield coin to public account,you should add one zero output amount cm
    /*    shieldOutList = PublicMethed.addShieldOutputList(shieldOutList, shieldAddress,
        "0", memo);
    Assert.assertTrue(PublicMethed.sendShieldCoin(
        null, 0,
        shieldAddressInfo.get(), notes.getNoteTxs(0),
        shieldOutList,
        zenTokenOwnerAddress, note.getValue() - zenTokenFee,
        zenTokenOwnerKey, blockingStubFull));*/

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    result = PublicMethed.getSpendResult(shieldAddressInfo.get(), notes.getNoteTxs(0),
        blockingStubFull);
    Assert.assertTrue(result.getResult());
    Long afterAssetBalance = PublicMethed.getAssetIssueValue(zenTokenOwnerAddress,
        PublicMethed.queryAccount(foundationZenTokenKey, blockingStubFull).getAssetIssuedID(),
        blockingStubFull);
    Assert.assertTrue(afterAssetBalance - beforeAssetBalance == note.getValue() - zenTokenFee);
    logger.info("beforeAssetBalance:" + beforeAssetBalance);
    logger.info("afterAssetBalance :" + afterAssetBalance);
  }


  @Test(enabled = false,
      description = "Output amount can't be zero or below zero")
  public void test3Shield2PublicAmountIsZero() {
    shieldAddressInfo = PublicMethed.generateShieldAddress();
    shieldAddress = shieldAddressInfo.get().getAddress();
    memo = "Shield to public amount is zero";
    shieldOutList.clear();
    shieldOutList = PublicMethed.addShieldOutputList(shieldOutList, shieldAddress,
        "" + (sendTokenAmount - zenTokenFee), memo);
    Assert.assertTrue(PublicMethed.sendShieldCoin(
        zenTokenOwnerAddress, sendTokenAmount,
        null, null,
        shieldOutList,
        null, 0,
        zenTokenOwnerKey, blockingStubFull));

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    notes = PublicMethed.listShieldNote(shieldAddressInfo, blockingStubFull);
    note = notes.getNoteTxs(0).getNote();

    shieldOutList.clear();
    shieldOutList = PublicMethed.addShieldOutputList(shieldOutList, shieldAddress,
        "" + (note.getValue() - zenTokenFee - (zenTokenFee - note.getValue())), memo);
    Assert.assertFalse(PublicMethed.sendShieldCoin(
        null, 0,
        shieldAddressInfo.get(), notes.getNoteTxs(0),
        shieldOutList,
        zenTokenOwnerAddress, zenTokenFee - note.getValue(),
        zenTokenOwnerKey, blockingStubFull));

    shieldOutList.clear();
    shieldOutList = PublicMethed.addShieldOutputList(shieldOutList, shieldAddress,
        "" + (note.getValue() - zenTokenFee), memo);

    Assert.assertFalse(PublicMethed.sendShieldCoin(
        null, 0,
        shieldAddressInfo.get(), notes.getNoteTxs(0),
        shieldOutList,
        zenTokenOwnerAddress, 0,
        zenTokenOwnerKey, blockingStubFull));

    shieldOutList.clear();
    shieldOutList = PublicMethed.addShieldOutputList(shieldOutList, shieldAddress,
        "" + (-zenTokenFee), memo);
    Assert.assertFalse(PublicMethed.sendShieldCoin(
        null, 0,
        shieldAddressInfo.get(), notes.getNoteTxs(0),
        shieldOutList,
        zenTokenOwnerAddress, note.getValue(),
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