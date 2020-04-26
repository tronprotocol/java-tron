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
import stest.tron.wallet.common.client.utils.PublicMethedForMutiSign;
import stest.tron.wallet.common.client.utils.ShieldAddressInfo;

@Slf4j
public class WalletTestZenToken009 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  Optional<ShieldAddressInfo> shieldAddressInfo;
  String shieldAddress;
  Optional<ShieldAddressInfo> receiverAddressInfo;
  String receiverAddress;
  List<Note> shieldOutList = new ArrayList<>();
  List<Long> shieldInputList = new ArrayList<>();
  DecryptNotes notes;
  String memo;
  Note note;
  String[] permissionKeyString = new String[2];
  String[] ownerKeyString = new String[2];
  String accountPermissionJson = "";
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] manager1Address = ecKey1.getAddress();
  String manager1Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] manager2Address = ecKey2.getAddress();
  String manager2Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  ECKey ecKey3 = new ECKey(Utils.getRandom());
  byte[] ownerAddress = ecKey3.getAddress();
  String ownerKey = ByteArray.toHexString(ecKey3.getPrivKeyBytes());
  ECKey ecKey4 = new ECKey(Utils.getRandom());
  byte[] zenTokenOwnerAddress = ecKey4.getAddress();
  String zenTokenOwnerKey = ByteArray.toHexString(ecKey4.getPrivKeyBytes());
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
  private Long costTokenAmount = 5 * zenTokenFee;
  private Long sendTokenAmount = 3 * zenTokenFee;
  private long multiSignFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.multiSignFee");
  private long updateAccountPermissionFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.updateAccountPermissionFee");


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
    long needCoin = updateAccountPermissionFee * 1 + multiSignFee * 3;
    Assert.assertTrue(
        PublicMethed.sendcoin(zenTokenOwnerAddress, needCoin + 2048000000L, fromAddress, testKey002,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    permissionKeyString[0] = manager1Key;
    permissionKeyString[1] = manager2Key;
    ownerKeyString[0] = zenTokenOwnerKey;
    ownerKeyString[1] = manager1Key;
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":2,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(manager1Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(zenTokenOwnerKey)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":2,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(manager1Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(manager2Key) + "\",\"weight\":1}"
            + "]}]}";

    logger.info(accountPermissionJson);
    Assert.assertTrue(PublicMethedForMutiSign
        .accountPermissionUpdate(accountPermissionJson, zenTokenOwnerAddress, zenTokenOwnerKey,
            blockingStubFull, ownerKeyString));
    Assert.assertTrue(PublicMethed.transferAsset(zenTokenOwnerAddress, tokenId,
        costTokenAmount, foundationZenTokenAddress, foundationZenTokenKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = false,
      description = "Public to shield transaction with mutisign")
  public void test1Public2ShieldTransaction() {
    Args.setFullNodeAllowShieldedTransaction(true);
    shieldAddressInfo = PublicMethed.generateShieldAddress();
    shieldAddress = shieldAddressInfo.get().getAddress();
    logger.info("shieldAddress:" + shieldAddress);
    final Long beforeAssetBalance = PublicMethed.getAssetIssueValue(zenTokenOwnerAddress,
        PublicMethed.queryAccount(foundationZenTokenKey, blockingStubFull).getAssetIssuedID(),
        blockingStubFull);
    final Long beforeBalance = PublicMethed
        .queryAccount(zenTokenOwnerAddress, blockingStubFull).getBalance();
    final Long beforeNetUsed = PublicMethed
        .getAccountResource(zenTokenOwnerAddress, blockingStubFull).getFreeNetUsed();

    memo = "aaaaaaa";

    shieldOutList = PublicMethed.addShieldOutputList(shieldOutList, shieldAddress,
        "" + (sendTokenAmount - zenTokenFee), memo);

    Assert.assertTrue(PublicMethedForMutiSign.sendShieldCoin(
        zenTokenOwnerAddress, sendTokenAmount,
        null, null,
        shieldOutList,
        null, 0,
        zenTokenOwnerKey, blockingStubFull, 0, ownerKeyString));

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long afterAssetBalance = PublicMethed.getAssetIssueValue(zenTokenOwnerAddress,
        PublicMethed.queryAccount(foundationZenTokenKey, blockingStubFull).getAssetIssuedID(),
        blockingStubFull);
    Long afterNetUsed = PublicMethed.getAccountResource(zenTokenOwnerAddress, blockingStubFull)
        .getFreeNetUsed();
    Assert.assertTrue(beforeAssetBalance - afterAssetBalance == sendTokenAmount);
    logger.info("Before net:" + beforeNetUsed);
    logger.info("After net:" + afterNetUsed);
    Assert.assertEquals(beforeNetUsed, afterNetUsed);
    final Long afterBalance = PublicMethed
        .queryAccount(zenTokenOwnerAddress, blockingStubFull).getBalance();
    Assert.assertTrue(beforeBalance - afterBalance == multiSignFee);
    notes = PublicMethed.listShieldNote(shieldAddressInfo, blockingStubFull);
    note = notes.getNoteTxs(0).getNote();
    Long receiverShieldTokenAmount = note.getValue();
    Assert.assertTrue(receiverShieldTokenAmount == sendTokenAmount - zenTokenFee);
    Assert.assertEquals(memo, PublicMethed.getMemo(note));
  }

  @Test(enabled = false,
      description = "When from is shield,sign this transaction is forbidden")
  public void test2ShieldFromShouldNotSign() {
    receiverAddressInfo = PublicMethed.generateShieldAddress();
    receiverAddress = shieldAddressInfo.get().getAddress();
    logger.info("receiver address:" + shieldAddress);

    notes = PublicMethed.listShieldNote(shieldAddressInfo, blockingStubFull);
    note = notes.getNoteTxs(0).getNote();

    shieldOutList.clear();
    shieldOutList = PublicMethed.addShieldOutputList(shieldOutList, receiverAddress,
        "" + (note.getValue() - zenTokenFee), memo);

    Assert.assertFalse(PublicMethedForMutiSign.sendShieldCoin(
        null, 321321,
        shieldAddressInfo.get(), notes.getNoteTxs(0),
        shieldOutList,
        null, 0,
        zenTokenOwnerKey, blockingStubFull, 0, ownerKeyString));

    Assert.assertFalse(PublicMethed.sendShieldCoin(
        null, 321321,
        shieldAddressInfo.get(), notes.getNoteTxs(0),
        shieldOutList,
        null, 0,
        zenTokenOwnerKey, blockingStubFull));

    Assert.assertFalse(PublicMethed.getSpendResult(shieldAddressInfo.get(),
        notes.getNoteTxs(0), blockingStubFull).getResult());


  }

  /**
   * constructor.
   */

  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
    PublicMethedForMutiSign.transferAsset(foundationZenTokenAddress, tokenId,
        PublicMethed.getAssetIssueValue(zenTokenOwnerAddress,
            PublicMethed.queryAccount(foundationZenTokenKey, blockingStubFull).getAssetIssuedID(),
            blockingStubFull), zenTokenOwnerAddress,
        zenTokenOwnerKey, blockingStubFull, ownerKeyString);
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}