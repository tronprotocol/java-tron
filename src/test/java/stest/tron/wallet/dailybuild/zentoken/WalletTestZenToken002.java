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
import org.tron.api.WalletSolidityGrpc;
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
public class WalletTestZenToken002 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  Optional<ShieldAddressInfo> sendShieldAddressInfo;
  Optional<ShieldAddressInfo> receiverShieldAddressInfo;
  String sendShieldAddress;
  String receiverShieldAddress;
  List<Note> shieldOutList = new ArrayList<>();
  List<Long> shieldInputList = new ArrayList<>();
  DecryptNotes notes;
  String memo;
  Note sendNote;
  Note receiverNote;

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private ManagedChannel channelSolidity = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);
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
    channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext(true)
        .build();
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);
    Assert.assertTrue(PublicMethed.transferAsset(zenTokenOwnerAddress, tokenId,
        costTokenAmount, foundationZenTokenAddress, foundationZenTokenKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Args.getInstance().setFullNodeAllowShieldedTransaction(true);
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

  @Test(enabled = true, description = "Shield to shield transaction")
  public void test1Shield2ShieldTransaction() {
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
  @Test(enabled = true, description = "Scan not by ivk and scan not by ivk on FullNode")
  public void test2ScanNoteByIvkAndOvk() {
    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext(true)
        .build();
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);

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
  @Test(enabled = true, description = "Scan not by ivk and scan not by ivk on solidity")
  public void test3ScanNoteByIvkAndOvkOnSolidityServer() {

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
  @Test(enabled = true, description = "Scan not by ivk and scan not by ivk on solidity")
  public void test4ScanNoteByIvkAndOvkOnSolidityServer() {
    soliditynode = Configuration.getByPath("testng.conf")
        .getStringList("solidityNode.ip.list").get(1);
    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext(true)
        .build();
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);

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
  }
}