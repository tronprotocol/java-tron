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
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.core.config.args.Args;
import org.tron.protos.contract.ShieldContract.IncrementalMerkleVoucherInfo;
import org.tron.protos.contract.ShieldContract.OutputPoint;
import org.tron.protos.contract.ShieldContract.OutputPointInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.ShieldAddressInfo;


@Slf4j
public class WalletTestZenToken002 {

  private static ByteString assetAccountId = null;
  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  Optional<ShieldAddressInfo> sendShieldAddressInfo;
  Optional<ShieldAddressInfo> receiverShieldAddressInfo;
  String sendShieldAddress;
  String receiverShieldAddress;
  List<Note> shieldOutList = new ArrayList<>();
  DecryptNotes notes;
  String memo;
  Note sendNote;
  Note receiverNote;
  IncrementalMerkleVoucherInfo firstMerkleVoucherInfo;
  IncrementalMerkleVoucherInfo secondMerkleVoucherInfo;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] zenTokenOwnerAddress = ecKey1.getAddress();
  String zenTokenOwnerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private ManagedChannel channelFull = null;
  private ManagedChannel channelSolidity = null;
  private ManagedChannel channelSolidity1 = null;
  private ManagedChannel channelPbft = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity1 = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubPbft = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);
  private String soliditynode1 = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(1);
  private String soliInPbft = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(2);
  private String foundationZenTokenKey = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.zenTokenOwnerKey");
  byte[] foundationZenTokenAddress = PublicMethed.getFinalAddress(foundationZenTokenKey);
  private String zenTokenId = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.zenTokenId");
  private byte[] tokenId = zenTokenId.getBytes();
  private Long zenTokenFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.zenTokenFee");
  private Long costTokenAmount = 10 * zenTokenFee;
  private Long sendTokenAmount = 8 * zenTokenFee;

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
    channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext(true)
        .build();
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);

    channelSolidity1 = ManagedChannelBuilder.forTarget(soliditynode1)
        .usePlaintext(true)
        .build();
    blockingStubSolidity1 = WalletSolidityGrpc.newBlockingStub(channelSolidity1);

    channelPbft = ManagedChannelBuilder.forTarget(soliInPbft)
        .usePlaintext(true)
        .build();
    blockingStubPbft = WalletSolidityGrpc.newBlockingStub(channelPbft);

    Assert.assertTrue(PublicMethed.transferAsset(zenTokenOwnerAddress, tokenId,
        costTokenAmount, foundationZenTokenAddress, foundationZenTokenKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Args.setFullNodeAllowShieldedTransaction(true);
    sendShieldAddressInfo = PublicMethed.generateShieldAddress();
    sendShieldAddress = sendShieldAddressInfo.get().getAddress();
    logger.info("sendShieldAddressInfo:" + sendShieldAddressInfo);
    memo = "Shield memo in" + System.currentTimeMillis();
    shieldOutList = PublicMethed.addShieldOutputList(shieldOutList, sendShieldAddress,
        "" + (sendTokenAmount - zenTokenFee), memo);
    Assert.assertTrue(PublicMethed.sendShieldCoin(zenTokenOwnerAddress, sendTokenAmount, null,
        null, shieldOutList, null, 0, zenTokenOwnerKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    notes = PublicMethed.listShieldNote(sendShieldAddressInfo, blockingStubFull);
    sendNote = notes.getNoteTxs(0).getNote();
  }

  @Test(enabled = false, description = "Get merkle tree voucher info")
  public void test01GetMerkleTreeVoucherInfo() {
    notes = PublicMethed.listShieldNote(sendShieldAddressInfo, blockingStubFull);
    sendNote = notes.getNoteTxs(0).getNote();
    OutputPointInfo.Builder request = OutputPointInfo.newBuilder();

    //ShieldNoteInfo noteInfo = shieldWrapper.getUtxoMapNote().get(shieldInputList.get(i));
    OutputPoint.Builder outPointBuild = OutputPoint.newBuilder();
    outPointBuild.setHash(ByteString.copyFrom(notes.getNoteTxs(0).getTxid().toByteArray()));
    outPointBuild.setIndex(notes.getNoteTxs(0).getIndex());
    request.addOutPoints(outPointBuild.build());
    firstMerkleVoucherInfo = blockingStubFull
        .getMerkleTreeVoucherInfo(request.build());
  }


  @Test(enabled = false, description = "Shield to shield transaction")
  public void test02Shield2ShieldTransaction() {
    receiverShieldAddressInfo = PublicMethed.generateShieldAddress();
    receiverShieldAddress = receiverShieldAddressInfo.get().getAddress();

    shieldOutList.clear();
    ;
    memo = "Send shield to receiver shield memo in" + System.currentTimeMillis();
    shieldOutList = PublicMethed.addShieldOutputList(shieldOutList, receiverShieldAddress,
        "" + (sendNote.getValue() - zenTokenFee), memo);
    Assert.assertTrue(PublicMethed.sendShieldCoin(
        null, 0,
        sendShieldAddressInfo.get(), notes.getNoteTxs(0),
        shieldOutList,
        null, 0,
        zenTokenOwnerKey, blockingStubFull));

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    notes = PublicMethed.listShieldNote(receiverShieldAddressInfo, blockingStubFull);
    receiverNote = notes.getNoteTxs(0).getNote();
    logger.info("Receiver note:" + receiverNote.toString());
    Assert.assertTrue(receiverNote.getValue() == sendNote.getValue() - zenTokenFee);

  }

  /**
   * constructor.
   */
  @Test(enabled = false, description = "Scan note by ivk and scan not by ivk on FullNode")
  public void test03ScanNoteByIvkAndOvk() {
    //Scan sender note by ovk equals scan receiver note by ivk on FullNode
    Note scanNoteByIvk = PublicMethed
        .getShieldNotesByIvk(receiverShieldAddressInfo, blockingStubFull).getNoteTxs(0).getNote();
    Note scanNoteByOvk = PublicMethed
        .getShieldNotesByOvk(sendShieldAddressInfo, blockingStubFull).getNoteTxs(0).getNote();
    Assert.assertEquals(scanNoteByIvk.getValue(), scanNoteByOvk.getValue());
    Assert.assertEquals(scanNoteByIvk.getMemo(), scanNoteByOvk.getMemo());
    Assert.assertEquals(scanNoteByIvk.getRcm(), scanNoteByOvk.getRcm());
    Assert.assertEquals(scanNoteByIvk.getPaymentAddress(), scanNoteByOvk.getPaymentAddress());
  }

  /**
   * constructor.
   */
  @Test(enabled = false, description = "Scan note by ivk and scan not by ivk on solidity")
  public void test04ScanNoteByIvkAndOvkOnSolidityServer() {

    //Scan sender note by ovk equals scan receiver note by ivk in Solidity
    PublicMethed.waitSolidityNodeSynFullNodeData(blockingStubFull, blockingStubSolidity);
    Note scanNoteByIvk = PublicMethed
        .getShieldNotesByIvkOnSolidity(receiverShieldAddressInfo, blockingStubSolidity)
        .getNoteTxs(0).getNote();
    Note scanNoteByOvk = PublicMethed
        .getShieldNotesByOvkOnSolidity(sendShieldAddressInfo, blockingStubSolidity)
        .getNoteTxs(0).getNote();
    Assert.assertEquals(scanNoteByIvk.getValue(), scanNoteByOvk.getValue());
    Assert.assertEquals(scanNoteByIvk.getMemo(), scanNoteByOvk.getMemo());
    Assert.assertEquals(scanNoteByIvk.getRcm(), scanNoteByOvk.getRcm());
    Assert.assertEquals(scanNoteByIvk.getPaymentAddress(), scanNoteByOvk.getPaymentAddress());
  }

  /**
   * constructor.
   */
  @Test(enabled = false, description = "Scan note by ivk and scan not by ivk on Pbft")
  public void test05ScanNoteByIvkAndOvkOnPbftServer() {

    //Scan sender note by ovk equals scan receiver note by ivk in Solidity
    PublicMethed.waitSolidityNodeSynFullNodeData(blockingStubFull, blockingStubSolidity);
    Note scanNoteByIvk = PublicMethed
        .getShieldNotesByIvkOnSolidity(receiverShieldAddressInfo, blockingStubPbft)
        .getNoteTxs(0).getNote();
    Note scanNoteByOvk = PublicMethed
        .getShieldNotesByOvkOnSolidity(sendShieldAddressInfo, blockingStubPbft)
        .getNoteTxs(0).getNote();
    Assert.assertEquals(scanNoteByIvk.getValue(), scanNoteByOvk.getValue());
    Assert.assertEquals(scanNoteByIvk.getMemo(), scanNoteByOvk.getMemo());
    Assert.assertEquals(scanNoteByIvk.getRcm(), scanNoteByOvk.getRcm());
    Assert.assertEquals(scanNoteByIvk.getPaymentAddress(), scanNoteByOvk.getPaymentAddress());
  }


  /**
   * constructor.
   */
  @Test(enabled = false, description = "Scan note by ivk and scan not by ivk on solidity")
  public void test06ScanNoteByIvkAndOvkOnSolidityServer() {
    //Scan sender note by ovk equals scan receiver note by ivk in Solidity
    PublicMethed.waitSolidityNodeSynFullNodeData(blockingStubFull, blockingStubSolidity1);
    Note scanNoteByIvk = PublicMethed
        .getShieldNotesByIvkOnSolidity(receiverShieldAddressInfo, blockingStubSolidity1)
        .getNoteTxs(0).getNote();
    Note scanNoteByOvk = PublicMethed
        .getShieldNotesByOvkOnSolidity(sendShieldAddressInfo, blockingStubSolidity1)

        .getNoteTxs(0).getNote();
    Assert.assertEquals(scanNoteByIvk.getValue(), scanNoteByOvk.getValue());
    Assert.assertEquals(scanNoteByIvk.getMemo(), scanNoteByOvk.getMemo());
    Assert.assertEquals(scanNoteByIvk.getRcm(), scanNoteByOvk.getRcm());
    Assert.assertEquals(scanNoteByIvk.getPaymentAddress(), scanNoteByOvk.getPaymentAddress());
  }

  /**
   * constructor.
   */
  @Test(enabled = false, description = "Query whether note is spend on solidity")
  public void test07QueryNoteIsSpendOnSolidity() {
    notes = PublicMethed.listShieldNote(sendShieldAddressInfo, blockingStubFull);
    //Scan sender note by ovk equals scan receiver note by ivk in Solidity
    Assert.assertTrue(PublicMethed.getSpendResult(sendShieldAddressInfo.get(),
        notes.getNoteTxs(0), blockingStubFull).getResult());
    Assert.assertTrue(PublicMethed.getSpendResultOnSolidity(sendShieldAddressInfo.get(),
        notes.getNoteTxs(0), blockingStubSolidity).getResult());
    Assert.assertTrue(PublicMethed.getSpendResultOnSolidity(sendShieldAddressInfo.get(),
        notes.getNoteTxs(0), blockingStubSolidity1).getResult());
  }

  /**
   * constructor.
   */
  @Test(enabled = false, description = "Query whether note is spend on PBFT")
  public void test08QueryNoteIsSpendOnPbft() {
    notes = PublicMethed.listShieldNote(sendShieldAddressInfo, blockingStubFull);
    //Scan sender note by ovk equals scan receiver note by ivk in Solidity
    Assert.assertTrue(PublicMethed.getSpendResult(sendShieldAddressInfo.get(),
        notes.getNoteTxs(0), blockingStubFull).getResult());
    Assert.assertTrue(PublicMethed.getSpendResultOnSolidity(sendShieldAddressInfo.get(),
        notes.getNoteTxs(0), blockingStubPbft).getResult());
  }


  /**
   * constructor.
   */
  @Test(enabled = false, description = "Query note and spend status on fullnode and solidity")
  public void test09QueryNoteAndSpendStatusOnFullnode() {
    Assert.assertFalse(
        PublicMethed.getShieldNotesAndMarkByIvk(receiverShieldAddressInfo, blockingStubFull)
            .getNoteTxs(0).getIsSpend());
    Note scanNoteByIvk = PublicMethed
        .getShieldNotesByIvk(receiverShieldAddressInfo, blockingStubFull)
        .getNoteTxs(0).getNote();
    Assert.assertEquals(scanNoteByIvk,
        PublicMethed.getShieldNotesAndMarkByIvk(receiverShieldAddressInfo, blockingStubFull)
            .getNoteTxs(0).getNote());

    Assert.assertFalse(PublicMethed
        .getShieldNotesAndMarkByIvkOnSolidity(receiverShieldAddressInfo, blockingStubSolidity)
        .getNoteTxs(0).getIsSpend());
    scanNoteByIvk = PublicMethed
        .getShieldNotesByIvkOnSolidity(receiverShieldAddressInfo, blockingStubSolidity)
        .getNoteTxs(0).getNote();
    Assert.assertEquals(scanNoteByIvk, PublicMethed
        .getShieldNotesAndMarkByIvkOnSolidity(receiverShieldAddressInfo, blockingStubSolidity)
        .getNoteTxs(0).getNote());
    Assert.assertEquals(scanNoteByIvk, PublicMethed
        .getShieldNotesAndMarkByIvkOnSolidity(receiverShieldAddressInfo, blockingStubPbft)
        .getNoteTxs(0).getNote());

    shieldOutList.clear();
    memo = "Query note and spend status on fullnode " + System.currentTimeMillis();
    notes = PublicMethed.listShieldNote(receiverShieldAddressInfo, blockingStubFull);
    shieldOutList = PublicMethed.addShieldOutputList(shieldOutList, sendShieldAddress,
        "" + (notes.getNoteTxs(0).getNote().getValue() - zenTokenFee), memo);
    Assert.assertTrue(PublicMethed.sendShieldCoin(
        null, 0,
        receiverShieldAddressInfo.get(), notes.getNoteTxs(0),
        shieldOutList,
        null, 0,
        zenTokenOwnerKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitSolidityNodeSynFullNodeData(blockingStubFull, blockingStubSolidity);

    Assert.assertTrue(
        PublicMethed.getShieldNotesAndMarkByIvk(receiverShieldAddressInfo, blockingStubFull)
            .getNoteTxs(0).getIsSpend());

    Assert.assertTrue(PublicMethed
        .getShieldNotesAndMarkByIvkOnSolidity(receiverShieldAddressInfo, blockingStubSolidity)
        .getNoteTxs(0).getIsSpend());
  }

  @Test(enabled = false, description = "Get merkle tree voucher info")
  public void test10GetMerkleTreeVoucherInfo() {
    notes = PublicMethed.listShieldNote(sendShieldAddressInfo, blockingStubFull);
    sendNote = notes.getNoteTxs(0).getNote();
    OutputPointInfo.Builder request = OutputPointInfo.newBuilder();

    //ShieldNoteInfo noteInfo = shieldWrapper.getUtxoMapNote().get(shieldInputList.get(i));
    OutputPoint.Builder outPointBuild = OutputPoint.newBuilder();
    outPointBuild.setHash(ByteString.copyFrom(notes.getNoteTxs(0).getTxid().toByteArray()));
    outPointBuild.setIndex(notes.getNoteTxs(0).getIndex());
    request.addOutPoints(outPointBuild.build());
    secondMerkleVoucherInfo = blockingStubFull
        .getMerkleTreeVoucherInfo(request.build());

    Assert.assertEquals(firstMerkleVoucherInfo, secondMerkleVoucherInfo);
  }

  @Test(enabled = false, description = "Get merkle tree voucher info from Solidity")
  public void test11GetMerkleTreeVoucherInfoFromSolidity() {
    notes = PublicMethed.listShieldNote(sendShieldAddressInfo, blockingStubFull);
    sendNote = notes.getNoteTxs(0).getNote();
    OutputPointInfo.Builder request = OutputPointInfo.newBuilder();

    //ShieldNoteInfo noteInfo = shieldWrapper.getUtxoMapNote().get(shieldInputList.get(i));
    OutputPoint.Builder outPointBuild = OutputPoint.newBuilder();
    outPointBuild.setHash(ByteString.copyFrom(notes.getNoteTxs(0).getTxid().toByteArray()));
    outPointBuild.setIndex(notes.getNoteTxs(0).getIndex());
    request.addOutPoints(outPointBuild.build());
    secondMerkleVoucherInfo = blockingStubSolidity
        .getMerkleTreeVoucherInfo(request.build());

    Assert.assertEquals(firstMerkleVoucherInfo, secondMerkleVoucherInfo);
  }

  @Test(enabled = false, description = "Get merkle tree voucher info from Pbft")
  public void test12GetMerkleTreeVoucherInfoFromPbft() {
    notes = PublicMethed.listShieldNote(sendShieldAddressInfo, blockingStubFull);
    sendNote = notes.getNoteTxs(0).getNote();
    OutputPointInfo.Builder request = OutputPointInfo.newBuilder();

    //ShieldNoteInfo noteInfo = shieldWrapper.getUtxoMapNote().get(shieldInputList.get(i));
    OutputPoint.Builder outPointBuild = OutputPoint.newBuilder();
    outPointBuild.setHash(ByteString.copyFrom(notes.getNoteTxs(0).getTxid().toByteArray()));
    outPointBuild.setIndex(notes.getNoteTxs(0).getIndex());
    request.addOutPoints(outPointBuild.build());
    secondMerkleVoucherInfo = blockingStubPbft
        .getMerkleTreeVoucherInfo(request.build());

    Assert.assertEquals(firstMerkleVoucherInfo, secondMerkleVoucherInfo);
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
    if (channelSolidity != null) {
      channelSolidity.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelSolidity1 != null) {
      channelSolidity1.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelPbft != null) {
      channelPbft.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}