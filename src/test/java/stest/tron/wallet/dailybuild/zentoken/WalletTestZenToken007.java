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
import org.tron.api.GrpcAPI.BytesMessage;
import org.tron.api.GrpcAPI.DecryptNotes;
import org.tron.api.GrpcAPI.DiversifierMessage;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.GrpcAPI.ExpandedSpendingKeyMessage;
import org.tron.api.GrpcAPI.IncomingViewingKeyDiversifierMessage;
import org.tron.api.GrpcAPI.IncomingViewingKeyMessage;
import org.tron.api.GrpcAPI.Note;
import org.tron.api.GrpcAPI.PaymentAddressMessage;
import org.tron.api.GrpcAPI.ViewingKeyMessage;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.core.config.args.Args;
import org.tron.core.zen.address.DiversifierT;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.ShieldAddressInfo;

@Slf4j
public class WalletTestZenToken007 {

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
  private static ByteString assetAccountId = null;
  BytesMessage ak;
  BytesMessage nk;

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private ManagedChannel channelSolidity = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;
  private ManagedChannel channelSolidity1 = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity1 = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);
  private String soliditynode1 = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(1);
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
  BytesMessage sk;
  ExpandedSpendingKeyMessage expandedSpendingKeyMessage;
  DiversifierMessage diversifierMessage;
  IncomingViewingKeyMessage ivk;
  ShieldAddressInfo addressInfo = new ShieldAddressInfo();
  Optional<ShieldAddressInfo> receiverAddressInfo;

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

    channelSolidity1 = ManagedChannelBuilder.forTarget(soliditynode1)
        .usePlaintext(true)
        .build();
    blockingStubSolidity1 = WalletSolidityGrpc.newBlockingStub(channelSolidity1);

    Assert.assertTrue(PublicMethed.transferAsset(zenTokenOwnerAddress, tokenId,
        costTokenAmount, foundationZenTokenAddress, foundationZenTokenKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Args.getInstance().setFullNodeAllowShieldedTransaction(true);
    sendShieldAddressInfo = PublicMethed.generateShieldAddress();
    sendShieldAddress = sendShieldAddressInfo.get().getAddress();
    logger.info("sendShieldAddressInfo:" + sendShieldAddressInfo);
    memo = "Shield memo in" + System.currentTimeMillis();
    shieldOutList = PublicMethed.addShieldOutputList(shieldOutList,sendShieldAddress,
        "" + (sendTokenAmount - zenTokenFee),memo);
    Assert.assertTrue(PublicMethed.sendShieldCoin(zenTokenOwnerAddress,sendTokenAmount,null,
        null,shieldOutList,null,0,zenTokenOwnerKey,blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    notes = PublicMethed.listShieldNote(sendShieldAddressInfo,blockingStubFull);
    sendNote = notes.getNoteTxs(0).getNote();

  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get spending key")
  public void test01GetSpendingKey() {
    sk = blockingStubFull.getSpendingKey(EmptyMessage.newBuilder().build());
    logger.info("sk: " + ByteArray.toHexString(sk.getValue().toByteArray()));

  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get diversifier")
  public void test02GetDiversifier() {
    diversifierMessage = blockingStubFull.getDiversifier(EmptyMessage.newBuilder().build());
    logger.info("d: " + ByteArray.toHexString(diversifierMessage.getD().toByteArray()));

  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get expanded spending key")
  public void test03GetExpandedSpendingKey() {
    expandedSpendingKeyMessage = blockingStubFull.getExpandedSpendingKey(sk);
    logger.info("ask: " + ByteArray.toHexString(expandedSpendingKeyMessage.getAsk().toByteArray()));
    logger.info("nsk: " + ByteArray.toHexString(expandedSpendingKeyMessage.getNsk().toByteArray()));
    logger.info("ovk: " + ByteArray.toHexString(expandedSpendingKeyMessage.getOvk().toByteArray()));

  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get AK from ASK")
  public void test04GetAkFromAsk() {
    BytesMessage.Builder askBuilder = BytesMessage.newBuilder();
    askBuilder.setValue(expandedSpendingKeyMessage.getAsk());
    ak = blockingStubFull.getAkFromAsk(askBuilder.build());
    logger.info("ak: " + ByteArray.toHexString(ak.getValue().toByteArray()));
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get Nk from Nsk")
  public void test05GetNkFromNsk() {
    BytesMessage.Builder nskBuilder = BytesMessage.newBuilder();
    nskBuilder.setValue(expandedSpendingKeyMessage.getNsk());
    nk = blockingStubFull.getNkFromNsk(nskBuilder.build());
    logger.info("nk: " + ByteArray.toHexString(nk.getValue().toByteArray()));
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get incoming viewing Key")
  public void test06GetIncomingViewingKey() {
    ViewingKeyMessage.Builder viewBuilder = ViewingKeyMessage.newBuilder();
    viewBuilder.setAk(ak.getValue());
    viewBuilder.setNk(nk.getValue());
    ivk = blockingStubFull.getIncomingViewingKey(viewBuilder.build());
    logger.info("ivk: " + ByteArray.toHexString(ivk.getIvk().toByteArray()));
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get Zen Payment Address")
  public void test07GetZenPaymentAddress() {
    IncomingViewingKeyDiversifierMessage.Builder builder =
        IncomingViewingKeyDiversifierMessage.newBuilder();
    builder.setD(diversifierMessage);
    builder.setIvk(ivk);
    PaymentAddressMessage addressMessage = blockingStubFull.getZenPaymentAddress(builder.build());
    System.out.println("pkd: " +  ByteArray.toHexString(addressMessage.getPkD().toByteArray()));
    System.out.println("address: " + addressMessage.getPaymentAddress());
    addressInfo.setSk(sk.getValue().toByteArray());
    addressInfo.setD(new DiversifierT(diversifierMessage.getD().toByteArray()));
    addressInfo.setIvk(ivk.getIvk().toByteArray());
    addressInfo.setOvk(expandedSpendingKeyMessage.getOvk().toByteArray());
    addressInfo.setPkD(addressMessage.getPkD().toByteArray());
    receiverAddressInfo = Optional.of(addressInfo);
  }

  @Test(enabled = true, description = "Shield to shield transaction")
  public void test08Shield2ShieldTransaction() {
    receiverShieldAddress = receiverAddressInfo.get().getAddress();
    shieldOutList.clear();;
    memo = "Send shield to receiver shield memo in" + System.currentTimeMillis();
    shieldOutList = PublicMethed.addShieldOutputList(shieldOutList,receiverShieldAddress,
        "" + (sendNote.getValue() - zenTokenFee),memo);
    Assert.assertTrue(PublicMethed.sendShieldCoin(
        null,0,
        sendShieldAddressInfo.get(), notes.getNoteTxs(0),
        shieldOutList,
        null,0,
        zenTokenOwnerKey,blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    notes = PublicMethed.listShieldNote(receiverAddressInfo,blockingStubFull);
    receiverNote = notes.getNoteTxs(0).getNote();
    logger.info("Receiver note:" + receiverNote.toString());
    Assert.assertTrue(receiverNote.getValue() == sendNote.getValue() - zenTokenFee);

    notes = PublicMethed.getShieldNotesByIvk(receiverAddressInfo,blockingStubFull);
    receiverNote = notes.getNoteTxs(0).getNote();
    logger.info("Receiver note:" + receiverNote.toString());
    Assert.assertTrue(receiverNote.getValue() == sendNote.getValue() - zenTokenFee);
  }

  @Test(enabled = true, description = "Shield to shield transaction without ask")
  public void test09Shield2ShieldTransactionWithoutAsk() {
    sendShieldAddressInfo = PublicMethed.generateShieldAddress();
    sendShieldAddress = sendShieldAddressInfo.get().getAddress();

    notes = PublicMethed.listShieldNote(receiverAddressInfo,blockingStubFull);
    receiverNote = notes.getNoteTxs(0).getNote();
    shieldOutList.clear();;
    memo = "Send shield without ask" + System.currentTimeMillis();
    shieldOutList = PublicMethed.addShieldOutputList(shieldOutList,sendShieldAddress,
        "" + (receiverNote.getValue() - zenTokenFee),memo);

    Assert.assertTrue(PublicMethed.sendShieldCoinWithoutAsk(
        null,0,
        receiverAddressInfo.get(), notes.getNoteTxs(0),
        shieldOutList,
        null,0,
        zenTokenOwnerKey,blockingStubFull));

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    notes = PublicMethed.listShieldNote(sendShieldAddressInfo,blockingStubFull);
    sendNote = notes.getNoteTxs(0).getNote();
    logger.info("Receiver note:" + sendNote.toString());
    Assert.assertTrue(sendNote.getValue() == receiverNote.getValue() - zenTokenFee);

    notes = PublicMethed.getShieldNotesByIvk(sendShieldAddressInfo,blockingStubFull);
    sendNote = notes.getNoteTxs(0).getNote();
    logger.info("Receiver note:" + sendNote.toString());
    Assert.assertTrue(sendNote.getValue() == receiverNote.getValue() - zenTokenFee);
  }

  @Test(enabled = true, description = "Get shield Nulltifier")
  public void test10GetShieldNulltifier() {
    notes = PublicMethed.listShieldNote(sendShieldAddressInfo,blockingStubFull);
    Assert.assertEquals(PublicMethed.getShieldNullifier(sendShieldAddressInfo.get(),
        notes.getNoteTxs(0),blockingStubFull).length(),64);
    notes = PublicMethed.listShieldNote(receiverAddressInfo,blockingStubFull);
    Assert.assertEquals(PublicMethed.getShieldNullifier(receiverAddressInfo.get(),
        notes.getNoteTxs(0),blockingStubFull).length(),64);

    Assert.assertTrue(PublicMethed.getSpendResult(receiverAddressInfo.get(),
        notes.getNoteTxs(0),blockingStubFull).getResult());
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
  }
}