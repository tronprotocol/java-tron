package stest.tron.wallet.zentoken;

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
public class WalletTestZenToken006 {

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
  private Long sendTokenAmount = 3 * zenTokenFee;

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] zenTokenOwnerAddress = ecKey1.getAddress();
  String zenTokenOwnerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

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
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Args.getInstance().setFullNodeAllowShieldedTransaction(true);
  }

  @Test(enabled = true, description = "Shield note memo is one char")
  public void test1ShieldMemoIsOneChar() {
    shieldAddressInfo = PublicMethed.generateShieldAddress();
    shieldAddress = shieldAddressInfo.get().getAddress();
    logger.info("shieldAddress:" + shieldAddress);

    //One char.
    memo = ".";
    shieldOutList = PublicMethed.addShieldOutputList(shieldOutList, shieldAddress,
        "" + zenTokenFee, memo);
    Assert.assertTrue(PublicMethed.sendShieldCoin(
        zenTokenOwnerAddress, zenTokenFee * 2,
        null, null,
        shieldOutList,
        null, 0,
        zenTokenOwnerKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    notes = PublicMethed.listShieldNote(shieldAddressInfo, blockingStubFull);
    note = notes.getNoteTxs(0).getNote();
    Long receiverShieldTokenAmount = note.getValue();
    Assert.assertEquals(receiverShieldTokenAmount, zenTokenFee);
    Assert.assertEquals(memo, PublicMethed.getMemo(note));
  }

  @Test(enabled = true, description = "Shield note memo is 512 char")
  public void test2ShieldMemoIs512Char() {
    shieldAddressInfo = PublicMethed.generateShieldAddress();
    shieldAddress = shieldAddressInfo.get().getAddress();
    logger.info("shieldAddress:" + shieldAddress);

    //One char.
    memo = "1234567812345678123456781234567812345678123456781234567812345678123456781234567812"
        + "345678123456781234567812345678123456781234567812345678123456781234567812345678123456"
        + "781234567812345678123456781234567812345678123456781234567812345678123456781234567812"
        + "345678123456781234567812345678123456781234567812345678123456781234567812345678123456"
        + "781234567812345678123456781234567812345678123456781234567812345678123456781234567812"
        + "345678123456781234567812345678123456781234567812345678123456781234567812345678123456"
        + "7812345678";
    shieldOutList.clear();
    shieldOutList = PublicMethed.addShieldOutputList(shieldOutList, shieldAddress,
        "" + zenTokenFee, memo);
    Assert.assertTrue(PublicMethed.sendShieldCoin(
        zenTokenOwnerAddress, zenTokenFee * 2,
        null, null,
        shieldOutList,
        null, 0,
        zenTokenOwnerKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    notes = PublicMethed.listShieldNote(shieldAddressInfo, blockingStubFull);
    note = notes.getNoteTxs(0).getNote();
    Long receiverShieldTokenAmount = note.getValue();
    Assert.assertEquals(receiverShieldTokenAmount, zenTokenFee);
    Assert.assertEquals(memo, PublicMethed.getMemo(note));
  }

  @Test(enabled = true, description = "Shield note memo is 513 char")
  public void test3ShieldMemoIs513Char() {
    shieldAddressInfo = PublicMethed.generateShieldAddress();
    shieldAddress = shieldAddressInfo.get().getAddress();
    logger.info("shieldAddress:" + shieldAddress);

    //One char.
    memo = "-1234567812345678123456781234567812345678123456781234567812345678123456781234567812"
        + "3456781234567812345678123456781234567812345678123456781234567812345678123456781234"
        + "5678123456781234567812345678123456781234567812345678123456781234567812345678123456"
        + "7812345678123456781234567812345678123456781234567812345678123456781234567812345678"
        + "1234567812345678123456781234567812345678123456781234567812345678123456781234567812"
        + "3456781234567812345678123456781234567812345678123456781234567812345678123456781234"
        + "56781234567812345678";
    shieldOutList.clear();
    shieldOutList = PublicMethed.addShieldOutputList(shieldOutList, shieldAddress,
        "" + zenTokenFee, memo);
    Assert.assertTrue(PublicMethed.sendShieldCoin(
        zenTokenOwnerAddress, zenTokenFee * 2,
        null, null,
        shieldOutList,
        null, 0,
        zenTokenOwnerKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    notes = PublicMethed.listShieldNote(shieldAddressInfo, blockingStubFull);
    note = notes.getNoteTxs(0).getNote();
    Long receiverShieldTokenAmount = note.getValue();
    Assert.assertEquals(receiverShieldTokenAmount, zenTokenFee);
    Assert.assertEquals(memo, PublicMethed.getMemo(note));
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